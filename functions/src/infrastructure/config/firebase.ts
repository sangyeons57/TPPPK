/**
 * Firebase configuration and initialization
 */

import * as admin from 'firebase-admin';
import { getApps } from 'firebase-admin/app';

// Initialize Firebase Admin SDK if not already initialized
if (getApps().length === 0) {
  admin.initializeApp();
}

export const firestore = admin.firestore();
export const auth = admin.auth();
export const storage = admin.storage();

// Configure Firestore settings
firestore.settings({
  ignoreUndefinedProperties: true,
});

export default admin;