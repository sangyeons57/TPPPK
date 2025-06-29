/**
 * Firebase Functions entry point
 * Exports all callable functions using the new modular architecture
 */

import * as admin from "firebase-admin";
import { DATABASE_ID } from "./constants";

if (!admin.apps.length) {
  admin.initializeApp();
  // Set Firestore to use the custom database ID (e.g., "default" without parentheses)
  admin.firestore().settings({ databaseId: DATABASE_ID });
}

// System functions
export {helloWorld} from "./functions/system/helloWorldFunction";

// Authentication functions - temporarily disabled due to compilation errors
// export {signUp} from "./functions/auth/signUpFunction";
// export {session} from "./functions/auth/sessionFunction";

// User data related functions
export {onUserProfileImageUpload} from "./functions/user/onUserProfileImageUpload";
export {updateUserProfile} from "./functions/user/updateUserProfileFunction";
