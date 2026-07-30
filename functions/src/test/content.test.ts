import assert from "node:assert/strict";
import {describe, it} from "node:test";
import {Timestamp} from "firebase-admin/firestore";
import {
  appointmentUpdatedTargets,
  bookingCreatedTargets,
  cancellationTargets,
  completionTargets,
  ratingCreatedTarget,
  reminderContent,
  rescheduleTargets,
  shouldSendForPreferences,
} from "../content.js";
import {
  AppointmentRecord,
  NotificationContent,
  reminderLeadMinutes,
} from "../domain.js";
import {
  calculateReminderWindow,
  isInsideReminderWindow,
} from "../reminders.js";
import {
  deterministicNotificationId,
  notificationRoute,
  pushMessage,
} from "../delivery.js";

function appointment(
  overrides: Partial<AppointmentRecord> = {},
): AppointmentRecord {
  return {
    appointmentId: "appointment-1",
    customerId: "customer-1",
    customerName: "Maya Customer",
    barberId: "barber-1",
    barberName: "North Chair",
    serviceId: "service-1",
    serviceName: "Haircut",
    appointmentDate: "August 2, 2026",
    appointmentTime: "14:30",
    startAt: Timestamp.fromMillis(2_000_000),
    endAt: Timestamp.fromMillis(3_800_000),
    status: "UPCOMING",
    price: 80,
    durationMinutes: 30,
    ...overrides,
  };
}

describe("notification content", () => {
  it("creates customer and barber booking targets", () => {
    const targets = bookingCreatedTargets(appointment());

    assert.equal(targets.length, 2);
    assert.equal(targets[0]?.userId, "customer-1");
    assert.equal(
      targets[0]?.content.type,
      "APPOINTMENT_BOOKED",
    );
    assert.match(targets[0]?.content.message ?? "", /North Chair/);
    assert.equal(targets[1]?.userId, "barber-1");
    assert.match(
      targets[1]?.content.message ?? "",
      /Maya Customer/,
    );
  });

  it("creates two cancellation targets", () => {
    const targets = cancellationTargets(
      appointment({status: "CANCELLED"}),
    );

    assert.equal(targets.length, 2);
    assert.ok(
      targets.every(
        (target) =>
          target.content.type === "APPOINTMENT_CANCELLED",
      ),
    );
  });

  it("creates reschedule messages using the new date", () => {
    const targets = rescheduleTargets(
      appointment({
        appointmentDate: "August 3, 2026",
        appointmentTime: "09:00",
      }),
    );

    assert.equal(targets.length, 2);
    assert.ok(
      targets.every((target) =>
        target.content.message.includes(
          "August 3, 2026 at 09:00",
        ),
      ),
    );
  });

  it("creates a review prompt after completion", () => {
    const targets = completionTargets(
      appointment({status: "COMPLETED"}),
    );

    assert.equal(targets[0]?.content.type, "REVIEW_REQUEST");
    assert.equal(
      targets[1]?.content.type,
      "APPOINTMENT_COMPLETED",
    );
  });

  it("detects cancellation transitions", () => {
    const targets = appointmentUpdatedTargets(
      appointment(),
      appointment({status: "CANCELLED"}),
    );

    assert.equal(targets.length, 2);
    assert.equal(
      targets[0]?.content.type,
      "APPOINTMENT_CANCELLED",
    );
  });

  it("detects completion transitions", () => {
    const targets = appointmentUpdatedTargets(
      appointment(),
      appointment({status: "COMPLETED"}),
    );

    assert.equal(targets.length, 2);
    assert.equal(targets[0]?.content.type, "REVIEW_REQUEST");
  });

  it("detects rescheduled start times", () => {
    const targets = appointmentUpdatedTargets(
      appointment(),
      appointment({startAt: Timestamp.fromMillis(2_900_000)}),
    );

    assert.equal(targets.length, 2);
    assert.equal(
      targets[0]?.content.type,
      "APPOINTMENT_RESCHEDULED",
    );
  });

  it("ignores updates without lifecycle meaning", () => {
    const targets = appointmentUpdatedTargets(
      appointment(),
      appointment({price: 90}),
    );

    assert.deepEqual(targets, []);
  });

  it("builds different customer and barber reminders", () => {
    const customer = reminderContent(appointment(), false);
    const barber = reminderContent(appointment(), true);

    assert.match(customer.message, /North Chair/);
    assert.match(barber.message, /Maya Customer/);
    assert.equal(customer.type, "APPOINTMENT_REMINDER");
    assert.equal(barber.type, "APPOINTMENT_REMINDER");
  });

  it("builds a rating notification with singular grammar", () => {
    const target = ratingCreatedTarget(
      {
        ratingId: "rating-1",
        appointmentId: "appointment-1",
        customerId: "customer-1",
        barberId: "barber-1",
        stars: 1,
        review: "",
      },
      "Maya",
    );

    assert.equal(target.userId, "barber-1");
    assert.match(target.content.message, /1 star\./);
  });

  it("mentions an attached written review", () => {
    const target = ratingCreatedTarget(
      {
        ratingId: "rating-1",
        appointmentId: "appointment-1",
        customerId: "customer-1",
        barberId: "barber-1",
        stars: 5,
        review: "Excellent",
      },
      "Maya",
    );

    assert.match(target.content.message, /read the review/);
  });
});

describe("preference routing", () => {
  const allowed = {
    appointmentUpdatesEnabled: true,
    remindersEnabled: true,
    reviewPromptsEnabled: true,
  };

  function content(type: NotificationContent["type"]) {
    return {type, title: "Title", message: "Message"};
  }

  it("allows every type with default preferences", () => {
    const types: NotificationContent["type"][] = [
      "APPOINTMENT_BOOKED",
      "APPOINTMENT_CANCELLED",
      "APPOINTMENT_RESCHEDULED",
      "APPOINTMENT_COMPLETED",
      "APPOINTMENT_REMINDER",
      "REVIEW_REQUEST",
      "GENERAL",
    ];

    assert.ok(
      types.every((type) =>
        shouldSendForPreferences(content(type), allowed),
      ),
    );
  });

  it("blocks appointment updates as a category", () => {
    const preferences = {
      ...allowed,
      appointmentUpdatesEnabled: false,
    };

    assert.equal(
      shouldSendForPreferences(
        content("APPOINTMENT_CANCELLED"),
        preferences,
      ),
      false,
    );
    assert.equal(
      shouldSendForPreferences(
        content("APPOINTMENT_REMINDER"),
        preferences,
      ),
      true,
    );
  });

  it("blocks reminders independently", () => {
    assert.equal(
      shouldSendForPreferences(
        content("APPOINTMENT_REMINDER"),
        {...allowed, remindersEnabled: false},
      ),
      false,
    );
  });

  it("blocks review requests independently", () => {
    assert.equal(
      shouldSendForPreferences(
        content("REVIEW_REQUEST"),
        {...allowed, reviewPromptsEnabled: false},
      ),
      false,
    );
  });

  it("always allows general account messages", () => {
    assert.equal(
      shouldSendForPreferences(content("GENERAL"), {
        appointmentUpdatesEnabled: false,
        remindersEnabled: false,
        reviewPromptsEnabled: false,
      }),
      true,
    );
  });
});

describe("delivery helpers", () => {
  it("creates stable event IDs", () => {
    const first = deterministicNotificationId(
      "event-1",
      "user-1",
      "GENERAL",
    );
    const second = deterministicNotificationId(
      "event-1",
      "user-1",
      "GENERAL",
    );

    assert.equal(first, second);
    assert.equal(first.length, 40);
  });

  it("changes IDs when the recipient changes", () => {
    assert.notEqual(
      deterministicNotificationId(
        "event-1",
        "user-1",
        "GENERAL",
      ),
      deterministicNotificationId(
        "event-1",
        "user-2",
        "GENERAL",
      ),
    );
  });

  it("routes appointment content to details", () => {
    assert.equal(
      notificationRoute({
        type: "GENERAL",
        title: "Title",
        message: "Message",
        appointmentId: "appointment / one",
      }),
      "appointment/appointment%20%2F%20one",
    );
  });

  it("routes the owning barber to barber appointment details", () => {
    assert.equal(
      notificationRoute(
        {
          type: "APPOINTMENT_BOOKED",
          title: "New booking",
          message: "A customer booked.",
          appointmentId: "appointment-1",
          barberId: "barber-1",
        },
        "barber-1",
      ),
      "barber_appointment/appointment-1",
    );
  });

  it("keeps a customer-mode barber on customer details", () => {
    assert.equal(
      notificationRoute(
        {
          type: "APPOINTMENT_BOOKED",
          title: "Confirmed",
          message: "Your appointment is booked.",
          appointmentId: "appointment-1",
          barberId: "servicing-barber",
        },
        "customer-who-is-also-a-barber",
      ),
      "appointment/appointment-1",
    );
  });

  it("routes barber content to a barber profile", () => {
    assert.equal(
      notificationRoute({
        type: "GENERAL",
        title: "Title",
        message: "Message",
        barberId: "barber one",
      }),
      "barber_profile/barber%20one",
    );
  });

  it("falls back to home", () => {
    assert.equal(
      notificationRoute({
        type: "GENERAL",
        title: "Title",
        message: "Message",
      }),
      "home",
    );
  });

  it("builds a high-priority Android message", () => {
    const message = pushMessage(
      ["token-a", "token-b"],
      {
        type: "APPOINTMENT_BOOKED",
        title: "Confirmed",
        message: "Your appointment is confirmed.",
        appointmentId: "appointment-1",
      },
      "appointment/appointment-1",
      "notification-1",
    );

    assert.deepEqual(message.tokens, ["token-a", "token-b"]);
    assert.equal(message.android?.priority, "high");
    assert.equal(
      message.android?.notification?.channelId,
      "cutime_appointments",
    );
    assert.equal(message.data?.notificationId, "notification-1");
  });
});

describe("reminder windows", () => {
  it("uses both required appointment reminder times", () => {
    assert.deepEqual(reminderLeadMinutes, [120, 30]);
  });

  it("opens exactly at the configured lead time", () => {
    const start = 10_000_000;
    const window = calculateReminderWindow(start, 60);

    assert.equal(window.targetMillis, 6_400_000);
    assert.equal(window.opensAtMillis, 6_400_000);
  });

  it("stays open for one scheduler interval", () => {
    const window = calculateReminderWindow(10_000_000, 30);

    assert.equal(
      window.closesAtMillis - window.opensAtMillis,
      15 * 60_000 - 1,
    );
  });

  it("includes both window boundaries", () => {
    const window = calculateReminderWindow(10_000_000, 30);

    assert.equal(
      isInsideReminderWindow(window.opensAtMillis, window),
      true,
    );
    assert.equal(
      isInsideReminderWindow(window.closesAtMillis, window),
      true,
    );
  });

  it("rejects times outside the window", () => {
    const window = calculateReminderWindow(10_000_000, 30);

    assert.equal(
      isInsideReminderWindow(window.opensAtMillis - 1, window),
      false,
    );
    assert.equal(
      isInsideReminderWindow(window.closesAtMillis + 1, window),
      false,
    );
  });
});
