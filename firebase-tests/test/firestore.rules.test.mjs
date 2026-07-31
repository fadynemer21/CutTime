import fs from "node:fs";
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
