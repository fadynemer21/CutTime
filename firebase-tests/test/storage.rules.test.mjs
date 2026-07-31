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
  deleteObject,
  getBytes,
  ref,
  uploadBytes,
} from "firebase/storage";
import {doc, setDoc} from "firebase/firestore";

const projectId = "cuttime-b1fa1";
const repositoryRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
  "..",
);
const firestoreRules = fs.readFileSync(
  path.join(repositoryRoot, "firestore.rules"),
  "utf8",
);
const storageRules = fs.readFileSync(
  path.join(repositoryRoot, "storage.rules"),
  "utf8",
);

let environment;

before(async () => {
  environment = await initializeTestEnvironment({
    projectId,
    firestore: {rules: firestoreRules},
    storage: {rules: storageRules},
  });
});

beforeEach(async () => {
  await environment.clearFirestore();
  await environment.clearStorage();
  await environment.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "users/barber"), {
      uid: "barber",
      fullName: "Barber Owner",
      email: "barber@example.com",
      role: "BARBER",
      createdAt: new Date(),
    });
    await setDoc(doc(context.firestore(), "users/customer"), {
      uid: "customer",
      fullName: "Customer One",
      email: "customer@example.com",
      role: "CUSTOMER",
      createdAt: new Date(),
    });
  });
});

after(async () => {
  await environment.cleanup();
});

const storageFor = (uid) =>
  environment.authenticatedContext(uid, {
    email: `${uid}@example.com`,
  }).storage();

const imageMetadata = {
  contentType: "image/jpeg",
  customMetadata: {
    barberId: "barber",
    imageId: "image",
  },
};

test("owning barber can upload a supported gallery image", async () => {
  await assertSucceeds(
    uploadBytes(
      ref(storageFor("barber"), "barberGalleries/barber/image"),
      new Uint8Array([1, 2, 3]),
      imageMetadata,
    ),
  );
});

test("customer and other paths cannot upload gallery objects", async () => {
  await assertFails(
    uploadBytes(
      ref(storageFor("customer"), "barberGalleries/barber/image"),
      new Uint8Array([1, 2, 3]),
      imageMetadata,
    ),
  );
  await assertFails(
    uploadBytes(
      ref(storageFor("barber"), "avatars/barber/image"),
      new Uint8Array([1, 2, 3]),
      imageMetadata,
    ),
  );
});

test("unsupported content and mismatched metadata are rejected", async () => {
  await assertFails(
    uploadBytes(
      ref(storageFor("barber"), "barberGalleries/barber/image"),
      new Uint8Array([1, 2, 3]),
      {...imageMetadata, contentType: "text/plain"},
    ),
  );
  await assertFails(
    uploadBytes(
      ref(storageFor("barber"), "barberGalleries/barber/image"),
      new Uint8Array([1, 2, 3]),
      {
        ...imageMetadata,
        customMetadata: {
          barberId: "barber",
          imageId: "different",
        },
      },
    ),
  );
});

test("signed-in user can read but anonymous user cannot", async () => {
  const path = "barberGalleries/barber/image";
  const customerStorage = storageFor("customer");
  const anonymousStorage = environment.unauthenticatedContext().storage();
  await environment.withSecurityRulesDisabled(async (context) => {
    await context.storage().ref(path).put(
      new Uint8Array([1, 2, 3]),
      imageMetadata,
    );
  });
  await assertSucceeds(
    customerStorage.ref(path).getMetadata(),
  );
  await assertFails(
    getBytes(ref(anonymousStorage, path)),
  );
});

test("only owning barber can delete a gallery object", async () => {
  const path = "barberGalleries/barber/image";
  const barberStorage = storageFor("barber");
  const customerStorage = storageFor("customer");
  await environment.withSecurityRulesDisabled(async (context) => {
    await context.storage().ref(path).put(
      new Uint8Array([1, 2, 3]),
      imageMetadata,
    );
  });
  await assertFails(
    customerStorage.ref(path).delete(),
  );
  await assertSucceeds(
    barberStorage.ref(path).delete(),
  );
});
