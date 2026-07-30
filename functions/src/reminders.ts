import {Timestamp} from "firebase-admin/firestore";
import {logger} from "firebase-functions";
import {
  AppointmentRecord,
  reminderLeadMinutes,
} from "./domain.js";
import {reminderContent} from "./content.js";
import {
  database,
  deliverNotification,
  readPreferences,
} from "./delivery.js";

const reminderScanWindowMinutes = 24 * 60;
const schedulerIntervalMinutes = 15;

export async function sendDueReminders(
  nowMillis: number = Date.now(),
): Promise<ReminderRunSummary> {
  const upperBound =
    nowMillis + reminderScanWindowMinutes * 60_000;
  const snapshot = await database
    .collection("appointments")
    .where("status", "==", "UPCOMING")
    .where("startAt", ">=", Timestamp.fromMillis(nowMillis))
    .where("startAt", "<=", Timestamp.fromMillis(upperBound))
    .get();

  const appointments = snapshot.docs.flatMap((document) => {
    const value = document.data();
    return isReminderAppointment(value)
      ? [value as AppointmentRecord]
      : [];
  });

  const recipientResults = await Promise.all(
    appointments.flatMap((appointment) => [
      sendRecipientReminders(
        appointment,
        appointment.customerId,
        false,
        nowMillis,
      ),
      sendRecipientReminders(
        appointment,
        appointment.barberId,
        true,
        nowMillis,
      ),
    ]),
  );
  const results = recipientResults.flat();

  const summary = results.reduce(
    (current, result) => ({
      considered: current.considered + 1,
      due: current.due + (result.due ? 1 : 0),
      created: current.created + (result.created ? 1 : 0),
      pushed: current.pushed + result.pushed,
      failedPushes:
        current.failedPushes + result.failedPushes,
    }),
    emptySummary(),
  );

  logger.info("CuTime reminder scan completed", summary);
  return summary;
}

export interface ReminderRunSummary {
  considered: number;
  due: number;
  created: number;
  pushed: number;
  failedPushes: number;
}

export interface ReminderWindow {
  targetMillis: number;
  opensAtMillis: number;
  closesAtMillis: number;
}

interface RecipientReminderResult {
  due: boolean;
  created: boolean;
  pushed: number;
  failedPushes: number;
}

async function sendRecipientReminders(
  appointment: AppointmentRecord,
  userId: string,
  recipientIsBarber: boolean,
  nowMillis: number,
): Promise<RecipientReminderResult[]> {
  const preferences = await readPreferences(database, userId);
  if (!preferences.remindersEnabled) {
    return reminderLeadMinutes.map(() => skippedRecipient());
  }

  return Promise.all(
    reminderLeadMinutes.map((leadMinutes) =>
      sendRecipientReminder(
        appointment,
        userId,
        recipientIsBarber,
        nowMillis,
        leadMinutes,
      ),
    ),
  );
}

async function sendRecipientReminder(
  appointment: AppointmentRecord,
  userId: string,
  recipientIsBarber: boolean,
  nowMillis: number,
  leadMinutes: number,
): Promise<RecipientReminderResult> {
  const window = calculateReminderWindow(
    appointment.startAt.toMillis(),
    leadMinutes,
  );
  if (!isInsideReminderWindow(nowMillis, window)) {
    return skippedRecipient();
  }

  const eventId =
    `reminder:${appointment.appointmentId}:` +
    `${appointment.startAt.toMillis()}:${userId}:` +
    `${leadMinutes}`;
  const result = await deliverNotification(
    userId,
    reminderContent(appointment, recipientIsBarber),
    {eventId},
  );

  return {
    due: true,
    created: result.notificationCreated,
    pushed: result.successfulPushCount,
    failedPushes: result.failedPushCount,
  };
}

export function calculateReminderWindow(
  appointmentStartMillis: number,
  leadMinutes: number,
): ReminderWindow {
  const targetMillis =
    appointmentStartMillis - leadMinutes * 60_000;
  return {
    targetMillis,
    opensAtMillis: targetMillis,
    closesAtMillis:
      targetMillis + schedulerIntervalMinutes * 60_000 - 1,
  };
}

export function isInsideReminderWindow(
  nowMillis: number,
  window: ReminderWindow,
): boolean {
  return (
    nowMillis >= window.opensAtMillis &&
    nowMillis <= window.closesAtMillis
  );
}

function isReminderAppointment(
  value: FirebaseFirestore.DocumentData,
): boolean {
  return (
    typeof value.appointmentId === "string" &&
    typeof value.customerId === "string" &&
    typeof value.customerName === "string" &&
    typeof value.barberId === "string" &&
    typeof value.barberName === "string" &&
    typeof value.serviceId === "string" &&
    typeof value.serviceName === "string" &&
    typeof value.appointmentDate === "string" &&
    typeof value.appointmentTime === "string" &&
    typeof value.price === "number" &&
    typeof value.durationMinutes === "number" &&
    value.status === "UPCOMING" &&
    value.startAt instanceof Timestamp &&
    value.endAt instanceof Timestamp
  );
}

function skippedRecipient(): RecipientReminderResult {
  return {
    due: false,
    created: false,
    pushed: 0,
    failedPushes: 0,
  };
}

function emptySummary(): ReminderRunSummary {
  return {
    considered: 0,
    due: 0,
    created: 0,
    pushed: 0,
    failedPushes: 0,
  };
}
