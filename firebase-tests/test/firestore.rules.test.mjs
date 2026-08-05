import fs from "node:fs";
import assert from "node:assert/strict";
import path from "node:path";
import {fileURLToPath} from "node:url";
import test, {after, before, beforeEach} from "node:test";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  collection,
  deleteField,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
  writeBatch,
} from "firebase/firestore";

const projectId = "cuttime-b1fa1";
const repositoryRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
  "..",
);
const rules = fs.readFileSync(
  path.join(repositoryRoot, "firestore.rules"),
  "utf8",
);

let environment;

before(async () => {
  environment = await initializeTestEnvironment({
    projectId,
    firestore: {rules},
  });
});

beforeEach(async () => {
  await environment.clearFirestore();
  await environment.withSecurityRulesDisabled(async (context) => {
    const database = context.firestore();
    await setDoc(doc(database, "users/customer"), {
      uid: "customer",
      fullName: "Customer One",
      email: "customer@example.com",
      role: "CUSTOMER",
      createdAt: new Date(),
    });
    await setDoc(doc(database, "users/other"), {
      uid: "other",
      fullName: "Other Customer",
      email: "other@example.com",
      role: "CUSTOMER",
      createdAt: new Date(),
    });
    await setDoc(doc(database, "users/barber"), {
      uid: "barber",
      fullName: "Barber Owner",
      email: "barber@example.com",
      role: "BARBER",
      createdAt: new Date(),
    });
    await setDoc(doc(database, "barberProfiles/barber"), {
      uid: "barber",
      shopName: "Test Studio",
      description: "A complete test barber profile.",
      ratingCount: 0,
      ratingSum: 0,
      ratingAverage: 0,
      updatedAt: new Date(),
    });
    await setDoc(
      doc(database, "barberProfiles/barber/services/service"),
      {
        serviceId: "service",
        barberId: "barber",
        name: "Haircut",
        price: 50,
        durationMinutes: 30,
        updatedAt: new Date(),
      },
    );
    await setDoc(doc(database, "barberAvailability/barber"), {
      barberId: "barber",
      days: [],
      blockedDates: [],
      updatedAt: new Date(),
    });
    await setDoc(doc(database, "appointments/cancelled"), {
      appointmentId: "cancelled",
      customerId: "customer",
      customerName: "Customer One",
      customerEmail: "customer@example.com",
      barberId: "barber",
      barberName: "Test Studio",
      serviceId: "service",
      serviceName: "Haircut",
      price: 50,
      durationMinutes: 30,
      appointmentDate: "2099-08-01",
      appointmentTime: "10:00",
      startAt: new Date("2099-08-01T07:00:00Z"),
      endAt: new Date("2099-08-01T07:30:00Z"),
      slotIds: ["barber_2099-08-01_10:00"],
      status: "CANCELLED",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
    await setDoc(
      doc(database, "users/customer/notifications/notice"),
      {
        notificationId: "notice",
        userId: "customer",
        type: "GENERAL",
        title: "Hello",
        message: "Test",
        isRead: false,
        createdAt: new Date(),
      },
    );
  });
});

after(async () => {
  await environment.cleanup();
});

const customerDatabase = () =>
  environment.authenticatedContext("customer", {
    email: "customer@example.com",
  }).firestore();
const otherDatabase = () =>
  environment.authenticatedContext("other", {
    email: "other@example.com",
  }).firestore();
const barberDatabase = () =>
  environment.authenticatedContext("barber", {
    email: "barber@example.com",
  }).firestore();
const anonymousDatabase = () =>
  environment.unauthenticatedContext().firestore();

const multiPeriodWeek = () => [
  ["Sunday", true],
  ["Monday", true],
  ["Tuesday", true],
  ["Wednesday", true],
  ["Thursday", true],
  ["Friday", true],
  ["Saturday", false],
].map(([day, isOpen]) => ({
  day,
  isOpen,
  startTime: "09:00",
  endTime: "12:00",
  workingPeriods: [
    {startTime: "09:00", endTime: "12:00"},
    {startTime: "14:00", endTime: "16:00"},
    {startTime: "16:30", endTime: "19:00"},
  ],
}));

test("users can read only their own private profile", async () => {
  await assertSucceeds(
    getDoc(doc(customerDatabase(), "users/customer")),
  );
  await assertFails(
    getDoc(doc(customerDatabase(), "users/other")),
  );
  await assertFails(
    getDoc(doc(anonymousDatabase(), "users/customer")),
  );
});

test("profile edits cannot change immutable identity or role", async () => {
  await assertSucceeds(
    updateDoc(doc(customerDatabase(), "users/customer"), {
      fullName: "Customer Updated",
      updatedAt: serverTimestamp(),
    }),
  );
  await assertFails(
    updateDoc(doc(customerDatabase(), "users/customer"), {
      role: "BARBER",
      updatedAt: serverTimestamp(),
    }),
  );
  await assertFails(
    updateDoc(doc(customerDatabase(), "users/customer"), {
      email: "attacker@example.com",
      updatedAt: serverTimestamp(),
    }),
  );
});

test("customer can create and read own account deletion request", async () => {
  const reference = doc(
    customerDatabase(),
    "accountDeletionRequests/customer",
  );
  await assertSucceeds(setDoc(reference, {
    userId: "customer",
    email: "customer@example.com",
    role: "CUSTOMER",
    status: "PENDING",
    requestedAt: serverTimestamp(),
  }));
  await assertSucceeds(getDoc(reference));
});

test("account deletion requests reject impersonation and foreign reads", async () => {
  await assertFails(setDoc(
    doc(customerDatabase(), "accountDeletionRequests/other"),
    {
      userId: "other",
      email: "other@example.com",
      role: "CUSTOMER",
      status: "PENDING",
      requestedAt: serverTimestamp(),
    },
  ));
  await assertFails(getDoc(
    doc(otherDatabase(), "accountDeletionRequests/customer"),
  ));
});

test("client cannot change account deletion processing status", async () => {
  const reference = doc(
    customerDatabase(),
    "accountDeletionRequests/customer",
  );
  await assertSucceeds(setDoc(reference, {
    userId: "customer",
    email: "customer@example.com",
    role: "CUSTOMER",
    status: "PENDING",
    requestedAt: serverTimestamp(),
  }));
  await assertFails(updateDoc(reference, {status: "COMPLETED"}));
});

test("signed-in users can read public barber data", async () => {
  await assertSucceeds(
    getDoc(doc(customerDatabase(), "barberProfiles/barber")),
  );
  await assertSucceeds(
    getDoc(
      doc(
        customerDatabase(),
        "barberProfiles/barber/services/service",
      ),
    ),
  );
  await assertFails(
    getDoc(doc(anonymousDatabase(), "barberProfiles/barber")),
  );
});

test("owning barber can save version two multi-period availability", async () => {
  await assertSucceeds(
    setDoc(doc(barberDatabase(), "barberAvailability/barber"), {
      barberId: "barber",
      schemaVersion: 2,
      days: multiPeriodWeek(),
      blockedDates: [],
      updatedAt: serverTimestamp(),
    }),
  );
});

test("customers cannot alter a barber's availability", async () => {
  await assertFails(
    setDoc(doc(customerDatabase(), "barberAvailability/barber"), {
      barberId: "barber",
      schemaVersion: 2,
      days: multiPeriodWeek(),
      blockedDates: [],
      updatedAt: serverTimestamp(),
    }),
  );
});

test("availability rejects unknown fields and old schema writes", async () => {
  const reference = doc(
    barberDatabase(),
    "barberAvailability/barber",
  );
  await assertFails(setDoc(reference, {
    barberId: "barber",
    schemaVersion: 1,
    days: multiPeriodWeek(),
    blockedDates: [],
    updatedAt: serverTimestamp(),
  }));
  await assertFails(setDoc(reference, {
    barberId: "barber",
    schemaVersion: 2,
    days: multiPeriodWeek(),
    blockedDates: [],
    unexpected: true,
    updatedAt: serverTimestamp(),
  }));
});

test("only owning barber can edit profile and services", async () => {
  await assertSucceeds(
    updateDoc(doc(barberDatabase(), "barberProfiles/barber"), {
      shopName: "Updated Studio",
      description: "A complete updated barber description.",
      updatedAt: serverTimestamp(),
    }),
  );
  await assertFails(
    updateDoc(doc(customerDatabase(), "barberProfiles/barber"), {
      shopName: "Hijacked Studio",
      description: "A complete attacker description.",
      updatedAt: serverTimestamp(),
    }),
  );
  await assertFails(
    deleteDoc(
      doc(
        customerDatabase(),
        "barberProfiles/barber/services/service",
      ),
    ),
  );
});

test("barber account can book its own shop in Customer Mode", async () => {
  await assertSucceeds(
    setDoc(doc(barberDatabase(), "appointments/self-booking"), {
      appointmentId: "self-booking",
      customerId: "barber",
      customerName: "Barber Owner",
      customerEmail: "barber@example.com",
      barberId: "barber",
      barberName: "Test Studio",
      serviceId: "service",
      serviceName: "Haircut",
      price: 50,
      durationMinutes: 30,
      appointmentDate: "2099-08-01",
      appointmentTime: "11:00",
      startAt: new Date("2099-08-01T08:00:00Z"),
      endAt: new Date("2099-08-01T08:30:00Z"),
      slotIds: ["barber_2099-08-01_11:00"],
      status: "UPCOMING",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    }),
  );
  await assertSucceeds(
    setDoc(
      doc(barberDatabase(), "bookingSlots/barber_2099-08-01_11:00"),
      {
        appointmentId: "self-booking",
        barberId: "barber",
        appointmentDate: "2099-08-01",
        appointmentTime: "11:00",
        startAt: new Date("2099-08-01T08:00:00Z"),
        createdAt: serverTimestamp(),
      },
    ),
  );
});

test("owning barber can complete a future upcoming appointment", async () => {
  await environment.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "appointments/future"), {
      appointmentId: "future",
      customerId: "customer",
      customerName: "Customer One",
      customerEmail: "customer@example.com",
      barberId: "barber",
      barberName: "Test Studio",
      serviceId: "service",
      serviceName: "Haircut",
      price: 50,
      durationMinutes: 30,
      appointmentDate: "2099-08-01",
      appointmentTime: "12:00",
      startAt: new Date("2099-08-01T09:00:00Z"),
      endAt: new Date("2099-08-01T09:30:00Z"),
      slotIds: ["barber_2099-08-01_12:00"],
      status: "UPCOMING",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
    await setDoc(
      doc(context.firestore(), "bookingSlots/future_slot"),
      {
        appointmentId: "future",
        barberId: "barber",
        appointmentDate: "2099-08-01",
        appointmentTime: "12:00",
        startAt: new Date("2099-08-01T09:00:00Z"),
        createdAt: new Date(),
      },
    );
  });

  await assertFails(
    updateDoc(doc(customerDatabase(), "appointments/future"), {
      status: "COMPLETED",
      updatedAt: serverTimestamp(),
    }),
  );
  const database = barberDatabase();
  const completion = writeBatch(database);
  completion.update(doc(database, "appointments/future"), {
    status: "COMPLETED",
    updatedAt: serverTimestamp(),
  });
  completion.delete(doc(database, "bookingSlots/future_slot"));
  await assertSucceeds(completion.commit());
});

test("barber cancellation creates an unread customer notification", async () => {
  await environment.withSecurityRulesDisabled(async (context) => {
    const database = context.firestore();
    await setDoc(doc(database, "appointments/future-cancel"), {
      appointmentId: "future-cancel",
      customerId: "customer",
      customerName: "Customer One",
      customerEmail: "customer@example.com",
      barberId: "barber",
      barberName: "Test Studio",
      serviceId: "service",
      serviceName: "Haircut",
      price: 50,
      durationMinutes: 30,
      appointmentDate: "2099-08-01",
      appointmentTime: "13:00",
      startAt: new Date("2099-08-01T10:00:00Z"),
      endAt: new Date("2099-08-01T10:30:00Z"),
      slotIds: ["future-cancel_slot"],
      status: "UPCOMING",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
    await setDoc(doc(database, "bookingSlots/future-cancel_slot"), {
      appointmentId: "future-cancel",
      barberId: "barber",
      appointmentDate: "2099-08-01",
      appointmentTime: "13:00",
      startAt: new Date("2099-08-01T10:00:00Z"),
      createdAt: new Date(),
    });
  });

  const database = barberDatabase();
  const cancellation = writeBatch(database);
  cancellation.update(doc(database, "appointments/future-cancel"), {
    status: "CANCELLED",
    updatedAt: serverTimestamp(),
  });
  cancellation.delete(
    doc(database, "bookingSlots/future-cancel_slot"),
  );
  cancellation.set(
    doc(
      database,
      "users/customer/notifications/cancelled_future-cancel",
    ),
    {
      notificationId: "cancelled_future-cancel",
      userId: "customer",
      type: "APPOINTMENT_CANCELLED",
      title: "Appointment cancelled",
      message: "Cancelled by Test Studio.",
      appointmentId: "future-cancel",
      barberId: "barber",
      isRead: false,
      createdAt: serverTimestamp(),
    },
  );
  await assertSucceeds(cancellation.commit());

  const notification = await getDoc(
    doc(
      customerDatabase(),
      "users/customer/notifications/cancelled_future-cancel",
    ),
  );
  assert.equal(notification.exists(), true);
  assert.equal(notification.data().isRead, false);
});
test("appointment detail is private to customer and barber", async () => {
  await assertSucceeds(
    getDoc(doc(customerDatabase(), "appointments/cancelled")),
  );
  await assertSucceeds(
    getDoc(doc(barberDatabase(), "appointments/cancelled")),
  );
  await assertFails(
    getDoc(doc(otherDatabase(), "appointments/cancelled")),
  );
});

test("customer can hide only own cancelled history", async () => {
  await assertSucceeds(
    updateDoc(doc(customerDatabase(), "appointments/cancelled"), {
      hiddenFromCustomer: true,
      updatedAt: serverTimestamp(),
    }),
  );
  await assertFails(
    updateDoc(doc(barberDatabase(), "appointments/cancelled"), {
      hiddenFromCustomer: true,
      updatedAt: serverTimestamp(),
    }),
  );
});

test("notification inbox cannot be read by another user", async () => {
  await assertSucceeds(
    getDoc(
      doc(
        customerDatabase(),
        "users/customer/notifications/notice",
      ),
    ),
  );
  await assertFails(
    getDoc(
      doc(
        otherDatabase(),
        "users/customer/notifications/notice",
      ),
    ),
  );
});

test("customer can submit a rating using the saved username", async () => {
  await environment.withSecurityRulesDisabled(async (context) => {
    const database = context.firestore();
    await setDoc(doc(database, "appointments/completed-rating"), {
      appointmentId: "completed-rating",
      customerId: "customer",
      customerName: "Customer One",
      customerEmail: "customer@example.com",
      barberId: "barber",
      barberName: "Test Studio",
      serviceId: "service",
      serviceName: "Haircut",
      price: 50,
      durationMinutes: 30,
      appointmentDate: "2026-08-01",
      appointmentTime: "10:00",
      startAt: new Date("2026-08-01T07:00:00Z"),
      endAt: new Date("2026-08-01T07:30:00Z"),
      slotIds: [],
      status: "COMPLETED",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const database = customerDatabase();
  const rating = writeBatch(database);
  rating.set(doc(database, "ratings/completed-rating"), {
    ratingId: "completed-rating",
    appointmentId: "completed-rating",
    customerId: "customer",
    barberId: "barber",
    customerName: "Customer One",
    notificationId: "review-completed-rating",
    stars: 5,
    review: "Great cut",
    createdAt: serverTimestamp(),
  });
  rating.set(
    doc(
      database,
      "users/barber/notifications/review-completed-rating",
    ),
    {
      notificationId: "review-completed-rating",
      userId: "barber",
      type: "GENERAL",
      title: "New review",
      message: "Customer One left a review.",
      appointmentId: "completed-rating",
      barberId: "barber",
      isRead: false,
      createdAt: serverTimestamp(),
    },
  );
  await assertSucceeds(rating.commit());

  const reviewNotification = await getDoc(
    doc(
      barberDatabase(),
      "users/barber/notifications/review-completed-rating",
    ),
  );
  assert.equal(reviewNotification.exists(), true);
  assert.equal(reviewNotification.data().isRead, false);

  await environment.withSecurityRulesDisabled(async (context) => {
    await updateDoc(
      doc(context.firestore(), "ratings/completed-rating"),
      {customerName: "Customer"},
    );
  });
  await assertSucceeds(
    updateDoc(doc(barberDatabase(), "ratings/completed-rating"), {
      customerName: "Customer One",
    }),
  );

  await assertSucceeds(
    updateDoc(doc(database, "appointments/completed-rating"), {
      ratingId: "completed-rating",
      updatedAt: serverTimestamp(),
    }),
  );

  await assertSucceeds(
    updateDoc(doc(database, "appointments/completed-rating"), {
      hiddenFromCustomer: true,
      updatedAt: serverTimestamp(),
    }),
  );
  await assertSucceeds(
    updateDoc(doc(barberDatabase(), "appointments/completed-rating"), {
      hiddenFromBarber: true,
      updatedAt: serverTimestamp(),
    }),
  );

  const savedRating = await getDoc(
    doc(database, "ratings/completed-rating"),
  );
  assert.equal(savedRating.exists(), true);
  assert.equal(savedRating.data().customerName, "Customer One");
  assert.equal(savedRating.data().review, "Great cut");

  await assertFails(
    deleteDoc(doc(barberDatabase(), "ratings/completed-rating")),
  );
  await assertSucceeds(
    deleteDoc(doc(database, "ratings/completed-rating")),
  );
  await assertSucceeds(
    updateDoc(doc(database, "appointments/completed-rating"), {
      ratingId: deleteField(),
      updatedAt: serverTimestamp(),
    }),
  );
  const deletedRating = await getDoc(
    doc(database, "ratings/completed-rating"),
  );
  assert.equal(deletedRating.exists(), false);
});

test("barber accounts cannot rate even while using customer features", async () => {
  await environment.withSecurityRulesDisabled(async (context) => {
    const database = context.firestore();
    await setDoc(doc(database, "appointments/barber-completed"), {
      appointmentId: "barber-completed",
      customerId: "barber",
      customerName: "Barber Owner",
      customerEmail: "barber@example.com",
      barberId: "barber",
      barberName: "Test Studio",
      serviceId: "service",
      serviceName: "Haircut",
      price: 50,
      durationMinutes: 30,
      appointmentDate: "2026-08-01",
      appointmentTime: "10:00",
      startAt: new Date("2026-08-01T07:00:00Z"),
      endAt: new Date("2026-08-01T07:30:00Z"),
      slotIds: [],
      status: "COMPLETED",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const database = barberDatabase();
  const rating = writeBatch(database);
  rating.set(doc(database, "ratings/barber-completed"), {
    ratingId: "barber-completed",
    appointmentId: "barber-completed",
    customerId: "barber",
    barberId: "barber",
    customerName: "Barber Owner",
    stars: 5,
    review: "Self review",
    createdAt: serverTimestamp(),
  });
  await assertFails(rating.commit());
});

test("customer and barber can hide completed history independently", async () => {
  await assertSucceeds(
    updateDoc(
      doc(customerDatabase(), "appointments/cancelled"),
      {
        hiddenFromCustomer: true,
        updatedAt: serverTimestamp(),
      },
    ),
  );
  await assertSucceeds(
    updateDoc(
      doc(barberDatabase(), "appointments/cancelled"),
      {
        hiddenFromBarber: true,
        updatedAt: serverTimestamp(),
      },
    ),
  );
});

test("customer can hide an expired upcoming appointment shown in completed history", async () => {
  await environment.withSecurityRulesDisabled(async (context) => {
    const database = context.firestore();
    await setDoc(doc(database, "appointments/expired-upcoming"), {
      appointmentId: "expired-upcoming",
      customerId: "customer",
      customerName: "Customer One",
      customerEmail: "customer@example.com",
      barberId: "barber",
      barberName: "Test Studio",
      serviceId: "service",
      serviceName: "Haircut",
      price: 50,
      durationMinutes: 30,
      appointmentDate: "2026-08-01",
      appointmentTime: "10:00",
      startAt: new Date("2026-08-01T07:00:00Z"),
      endAt: new Date("2026-08-01T07:30:00Z"),
      slotIds: [],
      status: "UPCOMING",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  await assertSucceeds(
    updateDoc(
      doc(customerDatabase(), "appointments/expired-upcoming"),
      {
        hiddenFromCustomer: true,
        updatedAt: serverTimestamp(),
      },
    ),
  );
});

test("clients cannot create trusted notification records", async () => {
  await assertFails(
    setDoc(
      doc(
        customerDatabase(),
        "users/customer/notifications/forged",
      ),
      {
        notificationId: "forged",
        userId: "customer",
        type: "GENERAL",
        title: "Forged",
        message: "Forged",
        isRead: false,
        createdAt: serverTimestamp(),
      },
    ),
  );
});

test("notification settings require the fixed reminder schedule", async () => {
  const validSettings = {
    userId: "customer",
    pushEnabled: true,
    remindersEnabled: true,
    appointmentUpdatesEnabled: true,
    reviewPromptsEnabled: true,
    reminderLeadMinutes: [120, 30],
    updatedAt: serverTimestamp(),
  };
  await assertSucceeds(
    setDoc(
      doc(customerDatabase(), "users/customer/settings/notifications"),
      validSettings,
    ),
  );
  await assertFails(
    setDoc(
      doc(customerDatabase(), "users/customer/settings/notifications"),
      {
        ...validSettings,
        reminderLeadMinutes: [5],
      },
    ),
  );
});

test("private collection query is constrained by rules", async () => {
  await assertSucceeds(
    getDocs(
      query(
        collection(customerDatabase(), "appointments"),
        where("customerId", "==", "customer"),
      ),
    ),
  );
  await assertFails(
    getDocs(collection(customerDatabase(), "appointments")),
  );
});
