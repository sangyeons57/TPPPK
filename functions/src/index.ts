/**
 * Firebase Functions entry point - Simplified Architecture
 * Clean, domain-based structure with minimal abstractions
 */

import * as admin from "firebase-admin";
import { DATABASE_ID, STORAGE_BUCKETS } from "./shared";

// Initialize Firebase Admin
if (!admin.apps.length) {
  admin.initializeApp({
    storageBucket: STORAGE_BUCKETS,
  });
  admin.firestore().settings({ databaseId: DATABASE_ID });
}

// =====================================
// USERS DOMAIN
// =====================================

// HTTP Functions
export { updateUserProfile } from "./domains/users/http/updateProfile";
export { removeUserProfileImage } from "./domains/users/http/removeProfileImage";

// Event Functions  
export { onUserProfileImageUpload } from "./domains/users/events/onProfileImageUpload";

// =====================================
// FRIENDS DOMAIN
// =====================================

// HTTP Functions
export { sendFriendRequest } from "./domains/friends/http/sendRequest";
export { acceptFriendRequest } from "./domains/friends/http/acceptRequest";
export { rejectFriendRequest } from "./domains/friends/http/rejectRequest";
export { removeFriend } from "./domains/friends/http/removeFriend";
export { getFriends } from "./domains/friends/http/getFriends";
export { getFriendRequests } from "./domains/friends/http/getFriendRequests";

// =====================================
// PROJECTS DOMAIN
// =====================================

// HTTP Functions
export { deleteProject } from "./domains/projects/http/deleteProject";
export { removeProjectProfileImage } from "./domains/projects/http/removeProfileImage";

// Event Functions
export { onProjectProfileImageUpload } from "./domains/projects/events/onProfileImageUpload";
export { onProjectChange, onProjectDelete } from "./domains/projects/events/onProjectChange";

// =====================================
// DM DOMAIN
// =====================================

// HTTP Functions
export { createDMChannel } from "./domains/dm/http/createDMChannel";
export { blockDMChannel } from "./domains/dm/http/blockChannel";
export { unblockDMChannel } from "./domains/dm/http/unblockChannel";

// =====================================
// MEMBERS DOMAIN
// =====================================

// HTTP Functions
export { joinProject } from "./domains/members/http/joinProject";
export { leaveProject } from "./domains/members/http/leaveProject";
export { blockMember } from "./domains/members/http/blockMember";
export { unblockMember } from "./domains/members/http/unblockMember";

// =====================================
// SYSTEM DOMAIN  
// =====================================

// HTTP Functions
export { helloWorld } from "./domains/system/http/helloWorld";
export { cleanupIdempotency } from "./domains/system/http/cleanupIdempotency";

// =====================================
// ARCHITECTURE COMPLETE ✅
// - domains/ (clean, domain-based structure)
// - shared/ (utilities, constants, validation)
// - Minimal abstractions, direct Firestore access
// - Structured logging, idempotency, error handling
// =====================================
