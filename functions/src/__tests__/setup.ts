import * as admin from "firebase-admin";
import { initializeApp } from "firebase-admin/app";

// Initialize test environment
process.env.FIRESTORE_EMULATOR_HOST = "localhost:8080";
process.env.FIREBASE_AUTH_EMULATOR_HOST = "localhost:9099";
process.env.FIREBASE_STORAGE_EMULATOR_HOST = "localhost:9199";

// Initialize Firebase Admin for testing
if (!admin.apps.length) {
  initializeApp({
    projectId: "test-project",
    storageBucket: "test-project.appspot.com",
  });
}

// Global test setup
beforeAll(() => {
  console.log("🧪 Test environment initialized");
});

afterAll(async () => {
  // Clean up
  await admin.app().delete();
  console.log("🧹 Test environment cleaned up");
});

export default {};