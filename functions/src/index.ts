import {logger} from "firebase-functions";
import {
  onDocumentCreated,
  onDocumentUpdated,
} from "firebase-functions/v2/firestore";
import {onSchedule} from "firebase-functions/v2/scheduler";
import {
  appointmentUpdatedTargets,
  bookingCreatedTargets,
  ratingCreatedTarget,
} from "./content.js";
import {database, deliverNotification} from "./delivery.js";
import {asAppointment, asRating} from "./domain.js";
import {sendDueReminders} from "./reminders.js";

const functionRegion = "europe-west1";

export const onAppointmentCreated = onDocumentCreated(
  {
    document: "appointments/{appointmentId}",
    region: functionRegion,
    retry: true,
  },
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
      logger.warn("Appointment creation event had no document", {
        eventId: event.id,
      });
      return;
    }

    const appointment = asAppointment(snapshot.data());
    if (!appointment) {
      logger.error("Appointment document failed validation", {
        eventId: event.id,
        appointmentId: event.params.appointmentId,
      });
      return;
    }

    const results = await Promise.all(
      bookingCreatedTargets(appointment).map((target) =>
        deliverNotification(target.userId, target.content, {
          eventId: event.id,
        }),
      ),
    );
    logger.info("Appointment creation notifications handled", {
      eventId: event.id,
      appointmentId: appointment.appointmentId,
      results,
    });
  },
);

export const onAppointmentUpdated = onDocumentUpdated(
  {
    document: "appointments/{appointmentId}",
    region: functionRegion,
    retry: true,
  },
  async (event) => {
    const beforeSnapshot = event.data?.before;
    const afterSnapshot = event.data?.after;
    if (!beforeSnapshot || !afterSnapshot) {
      logger.warn("Appointment update event had incomplete data", {
        eventId: event.id,
      });
      return;
    }

    const before = asAppointment(beforeSnapshot.data());
    const after = asAppointment(afterSnapshot.data());
    if (!before || !after) {
      logger.error("Updated appointment failed validation", {
        eventId: event.id,
        appointmentId: event.params.appointmentId,
      });
      return;
    }

    const targets = appointmentUpdatedTargets(before, after);
    if (targets.length === 0) {
      logger.debug("Appointment update has no notification effect", {
        eventId: event.id,
        appointmentId: after.appointmentId,
      });
      return;
    }

    const results = await Promise.all(
      targets.map((target) =>
        deliverNotification(target.userId, target.content, {
          eventId: event.id,
        }),
      ),
    );
    logger.info("Appointment update notifications handled", {
      eventId: event.id,
      appointmentId: after.appointmentId,
      results,
    });
  },
);

export const onRatingCreated = onDocumentCreated(
  {
    document: "ratings/{ratingId}",
    region: functionRegion,
    retry: true,
  },
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
      return;
    }

    const rating = asRating(snapshot.data());
    if (!rating) {
      logger.error("Rating document failed validation", {
        eventId: event.id,
        ratingId: event.params.ratingId,
      });
      return;
    }

    const customerSnapshot = await database
      .collection("users")
      .doc(rating.customerId)
      .get();
    const customerName =
      customerSnapshot.get("fullName") ?? "A customer";
    const target = ratingCreatedTarget(
      rating,
      typeof customerName === "string"
        ? customerName
        : "A customer",
    );
    const result = await deliverNotification(
      target.userId,
      target.content,
      {eventId: event.id},
    );
    logger.info("Rating notification handled", {
      eventId: event.id,
      ratingId: rating.ratingId,
      result,
    });
  },
);

export const sendAppointmentReminders = onSchedule(
  {
    schedule: "every 15 minutes",
    timeZone: "UTC",
    region: functionRegion,
    retryCount: 2,
  },
  async () => {
    await sendDueReminders();
  },
);
