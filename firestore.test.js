const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const databaseId = "ai-studio-android-myjannus-24766e3f-8f58-47bd-bc0d-2dd156b7dc2d";

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: "nusrat-ai-test",
    firestore: {
      host: "127.0.0.1",
      port: 8085,
      rules: fs.readFileSync("firestore.rules", "utf8"),
    },
  });
});

after(async () => {
  await testEnv.cleanup();
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

test("Users can read their own profile", async () => {
  const alice = testEnv.authenticatedContext("alice");
  const aliceDoc = alice.firestore(databaseId).collection("users").doc("alice");
  await assertSucceeds(aliceDoc.get());
});

test("Users cannot read other profiles", async () => {
  const alice = testEnv.authenticatedContext("alice");
  const bobDoc = alice.firestore(databaseId).collection("users").doc("bob");
  await assertFails(bobDoc.get());
});

test("Users can create their own profile with valid data", async () => {
  const alice = testEnv.authenticatedContext("alice");
  const aliceDoc = alice.firestore(databaseId).collection("users").doc("alice");
  await assertSucceeds(aliceDoc.set({
    uid: "alice",
    email: "alice@example.com",
    displayName: "Alice",
    subscriptionTier: "basic",
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("Unauthenticated users cannot read profiles", async () => {
  const unauth = testEnv.unauthenticatedContext();
  const aliceDoc = unauth.firestore(databaseId).collection("users").doc("alice");
  await assertFails(aliceDoc.get());
});
