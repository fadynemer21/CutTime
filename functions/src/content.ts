import {
  AppointmentRecord,
  NotificationContent,
  NotificationTarget,
  RatingRecord,
} from "./domain.js";

export function bookingCreatedTargets(
  appointment: AppointmentRecord,
): NotificationTarget[] {
  return [
    {
      userId: appointment.barberId,
      content: {
        type: "APPOINTMENT_BOOKED",
        title: "New appointment",
        message:
          `${appointment.customerName} booked ` +
          `${appointment.serviceName} for ` +
          `${humanAppointmentTime(appointment)}.`,
        appointmentId: appointment.appointmentId,
        barberId: appointment.barberId,
      },
    },
  ];
}

export function appointmentUpdatedTargets(
  before: AppointmentRecord,
  after: AppointmentRecord,
): NotificationTarget[] {
  if (
    before.status === "UPCOMING" &&
    after.status === "CANCELLED"
  ) {
    return cancellationTargets(after);
  }

  if (
    before.status === "UPCOMING" &&
    after.status === "COMPLETED"
  ) {
    return completionTargets(after);
  }

  if (
    before.startAt.toMillis() !== after.startAt.toMillis() &&
    after.status === "UPCOMING"
  ) {
    return rescheduleTargets(after);
  }

  return [];
}

export function cancellationTargets(
  appointment: AppointmentRecord,
): NotificationTarget[] {
  const message =
    `${appointment.serviceName} on ` +
    `${humanAppointmentTime(appointment)} was cancelled.`;

  return [
    {
      userId: appointment.customerId,
      content: {
        type: "APPOINTMENT_CANCELLED",
        title: "Appointment cancelled",
        message,
        appointmentId: appointment.appointmentId,
        barberId: appointment.barberId,
      },
    },
    {
      userId: appointment.barberId,
      content: {
        type: "APPOINTMENT_CANCELLED",
        title: "Appointment cancelled",
        message:
          `${appointment.customerName}'s ${message.toLowerCase()}`,
        appointmentId: appointment.appointmentId,
        barberId: appointment.barberId,
      },
    },
  ];
}

export function rescheduleTargets(
  appointment: AppointmentRecord,
): NotificationTarget[] {
  const when = humanAppointmentTime(appointment);

  return [
    {
      userId: appointment.barberId,
      content: {
        type: "APPOINTMENT_RESCHEDULED",
        title: "Booking time changed",
        message:
          `${appointment.customerName}'s appointment moved to ${when}.`,
        appointmentId: appointment.appointmentId,
        barberId: appointment.barberId,
      },
    },
  ];
}

export function completionTargets(
  appointment: AppointmentRecord,
): NotificationTarget[] {
  return [
    {
      userId: appointment.barberId,
      content: {
        type: "APPOINTMENT_COMPLETED",
        title: "Appointment completed",
        message:
          `${appointment.customerName}'s ${appointment.serviceName} ` +
          "appointment is complete.",
        appointmentId: appointment.appointmentId,
        barberId: appointment.barberId,
      },
    },
  ];
}

export function reminderContent(
  appointment: AppointmentRecord,
  recipientIsBarber: boolean,
): NotificationContent {
  if (recipientIsBarber) {
    return {
      type: "APPOINTMENT_REMINDER",
      title: "Upcoming customer",
      message:
        `${appointment.customerName} is booked for ` +
        `${appointment.serviceName} at ${appointment.appointmentTime}.`,
      appointmentId: appointment.appointmentId,
      barberId: appointment.barberId,
    };
  }

  return {
    type: "APPOINTMENT_REMINDER",
    title: "Your appointment is coming up",
    message:
      `${appointment.serviceName} with ${appointment.barberName} ` +
      `starts at ${appointment.appointmentTime}.`,
    appointmentId: appointment.appointmentId,
    barberId: appointment.barberId,
  };
}

export function ratingCreatedTarget(
  rating: RatingRecord,
  customerName: string,
): NotificationTarget {
  const starLabel = rating.stars === 1 ? "star" : "stars";
  const reviewSuffix =
    rating.review.trim().length > 0
      ? " Open CuTime to read the review."
      : "";

  return {
    userId: rating.barberId,
    content: {
      type: "GENERAL",
      title: "New customer rating",
      message:
        `${customerName} left ${rating.stars} ${starLabel}.` +
        reviewSuffix,
      appointmentId: rating.appointmentId,
      barberId: rating.barberId,
    },
  };
}

export function humanAppointmentTime(
  appointment: AppointmentRecord,
): string {
  return `${appointment.appointmentDate} at ${appointment.appointmentTime}`;
}

export function shouldSendForPreferences(
  content: NotificationContent,
  preferences: {
    appointmentUpdatesEnabled: boolean;
    remindersEnabled: boolean;
    reviewPromptsEnabled: boolean;
  },
): boolean {
  switch (content.type) {
    case "APPOINTMENT_REMINDER":
      return preferences.remindersEnabled;
    case "REVIEW_REQUEST":
      return preferences.reviewPromptsEnabled;
    case "APPOINTMENT_BOOKED":
    case "APPOINTMENT_CANCELLED":
    case "APPOINTMENT_RESCHEDULED":
    case "APPOINTMENT_COMPLETED":
      return preferences.appointmentUpdatesEnabled;
    case "GENERAL":
      return true;
  }
}
