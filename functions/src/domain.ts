export const notificationTypes = [
  "APPOINTMENT_BOOKED",
  "APPOINTMENT_CANCELLED",
  "APPOINTMENT_RESCHEDULED",
  "APPOINTMENT_COMPLETED",
  "APPOINTMENT_REMINDER",
  "REVIEW_REQUEST",
  "GENERAL",
] as const;

export type NotificationType =
  (typeof notificationTypes)[number];

export type AppointmentStatus =
  | "UPCOMING"
  | "COMPLETED"
  | "CANCELLED";

export interface AppointmentRecord {
  appointmentId: string;
  customerId: string;
  customerName: string;
  barberId: string;
  barberName: string;
  serviceId: string;
  serviceName: string;
  appointmentDate: string;
  appointmentTime: string;
  startAt: FirebaseFirestore.Timestamp;
  endAt: FirebaseFirestore.Timestamp;
  status: AppointmentStatus;
  price: number;
  durationMinutes: number;
}

export interface RatingRecord {
  ratingId: string;
  appointmentId: string;
  customerId: string;
  barberId: string;
  stars: number;
  review: string;
}

export interface NotificationPreferences {
  pushEnabled: boolean;
  remindersEnabled: boolean;
  appointmentUpdatesEnabled: boolean;
  reviewPromptsEnabled: boolean;
}

export interface NotificationContent {
  type: NotificationType;
  title: string;
  message: string;
  appointmentId?: string;
  barberId?: string;
}

export interface NotificationTarget {
  userId: string;
  content: NotificationContent;
}

export interface DeviceRecord {
  deviceId: string;
  userId: string;
  token: string;
  platform: "ANDROID";
  appVersion: string;
  deviceModel: string;
}

export const defaultPreferences: NotificationPreferences = {
  pushEnabled: true,
  remindersEnabled: true,
  appointmentUpdatesEnabled: true,
  reviewPromptsEnabled: true,
};

export const reminderLeadMinutes = [120, 30] as const;

export function asAppointment(
  value: FirebaseFirestore.DocumentData,
): AppointmentRecord | null {
  const status = value.status;
  const startAt = value.startAt;
  const endAt = value.endAt;

  if (
    typeof value.appointmentId !== "string" ||
    typeof value.customerId !== "string" ||
    typeof value.customerName !== "string" ||
    typeof value.barberId !== "string" ||
    typeof value.barberName !== "string" ||
    typeof value.serviceId !== "string" ||
    typeof value.serviceName !== "string" ||
    typeof value.appointmentDate !== "string" ||
    typeof value.appointmentTime !== "string" ||
    typeof value.price !== "number" ||
    typeof value.durationMinutes !== "number" ||
    !isTimestamp(startAt) ||
    !isTimestamp(endAt) ||
    !isAppointmentStatus(status)
  ) {
    return null;
  }

  return {
    appointmentId: value.appointmentId,
    customerId: value.customerId,
    customerName: value.customerName,
    barberId: value.barberId,
    barberName: value.barberName,
    serviceId: value.serviceId,
    serviceName: value.serviceName,
    appointmentDate: value.appointmentDate,
    appointmentTime: value.appointmentTime,
    startAt,
    endAt,
    status,
    price: value.price,
    durationMinutes: value.durationMinutes,
  };
}

export function asRating(
  value: FirebaseFirestore.DocumentData,
): RatingRecord | null {
  if (
    typeof value.ratingId !== "string" ||
    typeof value.appointmentId !== "string" ||
    typeof value.customerId !== "string" ||
    typeof value.barberId !== "string" ||
    typeof value.stars !== "number" ||
    typeof value.review !== "string"
  ) {
    return null;
  }

  return {
    ratingId: value.ratingId,
    appointmentId: value.appointmentId,
    customerId: value.customerId,
    barberId: value.barberId,
    stars: value.stars,
    review: value.review,
  };
}

export function asPreferences(
  value: FirebaseFirestore.DocumentData | undefined,
): NotificationPreferences {
  if (!value) {
    return defaultPreferences;
  }

  return {
    pushEnabled:
      typeof value.pushEnabled === "boolean"
        ? value.pushEnabled
        : defaultPreferences.pushEnabled,
    remindersEnabled:
      typeof value.remindersEnabled === "boolean"
        ? value.remindersEnabled
        : defaultPreferences.remindersEnabled,
    appointmentUpdatesEnabled:
      typeof value.appointmentUpdatesEnabled === "boolean"
        ? value.appointmentUpdatesEnabled
        : defaultPreferences.appointmentUpdatesEnabled,
    reviewPromptsEnabled:
      typeof value.reviewPromptsEnabled === "boolean"
        ? value.reviewPromptsEnabled
        : defaultPreferences.reviewPromptsEnabled,
  };
}

export function isTimestamp(
  value: unknown,
): value is FirebaseFirestore.Timestamp {
  return (
    typeof value === "object" &&
    value !== null &&
    "toMillis" in value &&
    typeof (value as {toMillis?: unknown}).toMillis === "function"
  );
}

export function isAppointmentStatus(
  value: unknown,
): value is AppointmentStatus {
  return (
    value === "UPCOMING" ||
    value === "COMPLETED" ||
    value === "CANCELLED"
  );
}
