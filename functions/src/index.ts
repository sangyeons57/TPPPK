/**
 * Firebase Functions entry point
 * Exports all callable functions using the new DDD architecture
 */

import * as admin from "firebase-admin";
import {DATABASE_ID} from "./core/constants";
import {DependencyConfig} from "./config/dependencies";

if (!admin.apps.length) {
  admin.initializeApp({
    storageBucket: "teamnovaprojectprojecting",
  });
  // Set Firestore to use the custom database ID (e.g., "default" without parentheses)
  admin.firestore().settings({databaseId: DATABASE_ID});
}

// Initialize dependency injection container
DependencyConfig.initialize();


// User management functions
export {updateUserProfileFunction as updateUserProfile} from "./triggers/user/userProfile.trigger";
export {onUserProfileImageUpload} from "./triggers/user/userImage.trigger";

// Project management functions
export {onProjectImageUpload} from "./triggers/project/projectImage.trigger";

// System functions
export {helloWorldFunction as helloWorld} from "./triggers/system/helloWorld.trigger";

// Friend management functions
export {sendFriendRequestFunction as sendFriendRequest} from "./triggers/friend/friendManagement.trigger";
export {acceptFriendRequestFunction as acceptFriendRequest} from "./triggers/friend/friendManagement.trigger";
export {rejectFriendRequestFunction as rejectFriendRequest} from "./triggers/friend/friendManagement.trigger";
export {removeFriendFunction as removeFriend} from "./triggers/friend/friendManagement.trigger";
export {getFriendsFunction as getFriends} from "./triggers/friend/friendManagement.trigger";
export {getFriendRequestsFunction as getFriendRequests} from "./triggers/friend/friendManagement.trigger";
