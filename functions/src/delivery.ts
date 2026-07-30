import {createHash} from "node:crypto";
import {getApps, initializeApp} from "firebase-admin/app";
import {
  FieldValue,
  Firestore,
  getFirestore,
} from "firebase-admin/firestore";
import {
  BatchResponse,
  getMessaging,
  MulticastMessage,
} from "firebase-admin/messaging";
import {logger} from "firebase-functions";
import {
  asPreferences,
  NotificationContent,
  NotificationPreferences,
} from "./domain.js";
import {shouldSendForPreferences} from "./content.js";

if (getApps().length === 0) {
  initializeApp();
}

export const database = getFirestore();

export interface DeliveryOptions {
  eventId: string;
  forceInApp?: boolean;
}

export interface DeliveryResult {
  notificationCreated: boolean;
  pushAttempted: boolean;
  successfulPushCount: number;
  failedPushCount: number;
}

export async function deliverNotification(
  userId: string,
  content: NotificationContent,
  options: DeliveryOptions,
): Promise<DeliveryResult> {
  const preferences = await readPreferences(database, userId);
  const enabled = shouldSendForPreferences(content, preferences);

  if (!enabled && !options.forceInApp) {
    return emptyDeliveryResult();
  }

  const notificationId = deterministicNotificationId(
    options.eventId,
    userId,
    content.type,
  );
  const notificationReference = database
    .collection("users")
    .doc(userId)
    .collection("notifications")
    .doc(notificationId);

  const created = await database.runTransaction(
    async (transaction) => {
      const existing = await transaction.get(notificationReference);
      if (existing.exists) {
        return false;
      }

      transaction.create(notificationReference, {
        notificationId,
        userId,
        type: content.type,
        title: content.title,
        message: content.message,
        appointmentId: content.appointmentId ?? null,
        barberId: content.barberId ?? null,
        isRead: false,
        createdAt: FieldValue.serverTimestamp(),
      });
      return true;
    },
  );

  if (
    !created ||
    !enabled ||
    !preferences.pushEnabled
  ) {
    return {
      notificationCreated: created,
      pushAttempted: false,
      successfulPushCount: 0,
      failedPushCount: 0,
    };
  }

  const devices = await database
    .collection("users")
    .doc(userId)
    .collection("devices")
    .get();
  const tokens = devices.docs
    .map((document) => document.get("token"))
    .filter(
      (token): token is string =>
        typeof token === "string" && token.length > 0,
    );

  if (tokens.length === 0) {
    return {
      notificationCreated: true,
      pushAttempted: false,
      successfulPushCount: 0,
      failedPushCount: 0,
    };
  }

  const route = notificationRoute(content, userId);
  const response = await getMessaging().sendEachForMulticast(
    pushMessage(tokens, content, route, notificationId),
  );

  await removeInvalidTokens(
    database,
    userId,
    devices.docs,
    response,
  );

  return {
    notificationCreated: true,
    pushAttempted: true,
    successfulPushCount: response.successCount,
    failedPushCount: response.failureCount,
  };
}

export async function readPreferences(
  firestore: Firestore,
  userId: string,
): Promise<NotificationPreferences> {
  const snapshot = await firestore
    .collection("users")
    .doc(userId)
    .collection("settings")
    .doc("notifications")
    .get();
  return asPreferences(snapshot.data());
}

export function deterministicNotificationId(
  eventId: string,
  userId: string,
  type: string,
): string {
  const input = `${eventId}|${userId}|${type}`;
  return createHash("sha256").update(input).digest("hex").slice(0, 40);
}

export function notificationRoute(
  content: NotificationContent,
  recipientUserId?: string,
): string {
  if (content.appointmentId) {
    const routePrefix =
      recipientUserId &&
      content.barberId === recipientUserId
        ? "barber_appointment"
        : "appointment";
    return (
      `${routePrefix}/` +
      encodeURIComponent(content.appointmentId)
    );
  }

  if (content.barberId) {
    return `barber_profile/${encodeURIComponent(content.barberId)}`;
  }

  return "home";
}

export function pushMessage(
  tokens: string[],
  content: NotificationContent,
  route: string,
  notificationId: string,
): MulticastMessage {
  return {
    tokens,
    notification: {
      title: content.title,
      body: content.message,
    },
    data: {
      notificationId,
      type: content.type,
      route,
      appointmentId: content.appointmentId ?? "",
      barberId: content.barberId ?? "",
    },
    android: {
      priority: "high",
      notification: {
        channelId: "cutime_appointments",
        tag: notificationId,
      },
    },
  };
}

async function removeInvalidTokens(
  firestore: Firestore,
  userId: string,
  deviceDocuments:
    FirebaseFirestore.QueryDocumentSnapshot[],
  response: BatchResponse,
): Promise<void> {
  const invalidCodes = new Set([
    "messaging/invalid-registration-token",
    "messaging/registration-token-not-registered",
  ]);
  const invalidReferences =
    response.responses.flatMap((sendResponse, index) => {
      const document = deviceDocuments[index];
      const code = sendResponse.error?.code;
      return (
        !sendResponse.success &&
        document &&
        code &&
        invalidCodes.has(code)
      )
        ? [document.ref]
        : [];
    });

  if (invalidReferences.length === 0) {
    return;
  }

  const batch = firestore.batch();
  invalidReferences.forEach((reference) => batch.delete(reference));
  await batch.commit();
  logger.info("Removed stale notification device tokens", {
    userId,
    count: invalidReferences.length,
  });
}

function emptyDeliveryResult(): DeliveryResult {
  return {
    notificationCreated: false,
    pushAttempted: false,
    successfulPushCount: 0,
    failedPushCount: 0,
  };
}
