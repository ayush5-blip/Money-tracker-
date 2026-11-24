const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validTransactionPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 5000);
  return {
    userId: uid,
    type: "EXPENSE",
    instrument: "DEBIT_CARD",
    amountCents: 4250,
    currency: "USD",
    merchant: "Whole Foods Market",
    category: "Groceries",
    bankName: "Chase",
    accountLast4: "4821",
    source: "SMS_INBOX",
    messageHash: "abc123def456",
    note: "Debited via Debit Card XX4821",
    transactionTimeMillis: Date.now() - 60000,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validPreferencesPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 5000);
  return {
    userId: uid,
    defaultCurrency: "USD",
    monthlyBudgetCents: 250000,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write transactions or settings", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_1")
      .set(validTransactionPayload(ALICE_UID))
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("settings")
      .doc("preferences")
      .set(validPreferencesPayload(ALICE_UID))
  );
});

test("Owner can create, list, get, partially update, and delete transactions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txCol = aliceDb.collection("users").doc(ALICE_UID).collection("transactions");

  // List on empty subcollection must succeed
  await assertSucceeds(txCol.get());

  // Create without @DocumentId "id" field
  await assertSucceeds(txCol.doc("tx_1").set(validTransactionPayload(ALICE_UID)));

  // Get and list after create
  await assertSucceeds(txCol.doc("tx_1").get());
  await assertSucceeds(txCol.get());

  // Partial update on mutable fields
  await assertSucceeds(
    txCol.doc("tx_1").update({
      merchant: "Whole Foods Organic",
      category: "Food & Dining",
      amountCents: 4590,
      note: "Updated receipt total",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Delete
  await assertSucceeds(txCol.doc("tx_1").delete());
});

test("Rejects invalid transaction payloads, @DocumentId key, future timestamps, and immutable edits", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txCol = aliceDb.collection("users").doc(ALICE_UID).collection("transactions");

  // Extra "id" field (@DocumentId conflict)
  await assertFails(
    txCol.doc("tx_bad_id").set(validTransactionPayload(ALICE_UID, { id: "tx_bad_id" }))
  );

  // Ghost field (shadow update / create)
  await assertFails(
    txCol.doc("tx_ghost").set(validTransactionPayload(ALICE_UID, { isAdmin: true }))
  );

  // Non-positive amountCents
  await assertFails(
    txCol.doc("tx_zero").set(validTransactionPayload(ALICE_UID, { amountCents: 0 }))
  );

  // Future timestamp
  await assertFails(
    txCol
      .doc("tx_future")
      .set(validTransactionPayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) }))
  );

  // Create valid doc, then attempt immutable field mutation
  await assertSucceeds(txCol.doc("tx_valid").set(validTransactionPayload(ALICE_UID)));
  await assertFails(
    txCol.doc("tx_valid").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    txCol.doc("tx_valid").update({
      source: "MANUAL",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user access to transactions and settings is strictly denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("transactions")
      .doc("bob_tx_1")
      .set(validTransactionPayload(BOB_UID));
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("settings")
      .doc("preferences")
      .set(validPreferencesPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("transactions").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("transactions").doc("bob_tx_1").get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("transactions")
      .doc("bob_tx_1")
      .update({ merchant: "Hijacked", updatedAt: new Date(Date.now() - 1000) })
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("transactions").doc("bob_tx_1").delete()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("settings").doc("preferences").get()
  );
});

test("Owner can create, read, and update preferences; invalid budget rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const prefRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("settings")
    .doc("preferences");

  await assertSucceeds(prefRef.set(validPreferencesPayload(ALICE_UID)));
  await assertSucceeds(prefRef.get());
  await assertSucceeds(
    prefRef.update({
      defaultCurrency: "INR",
      monthlyBudgetCents: 5000000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    prefRef.update({
      monthlyBudgetCents: -100,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
