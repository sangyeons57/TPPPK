/**
 * Firebase Functions entry point
 * Exports all callable functions using the new DDD architecture
 */

import * as admin from "firebase-admin";
import {DATABASE_ID, STORAGE_BUCKETS} from "./core/constants";
import {DependencyConfig} from "./config/dependencies";

if (!admin.apps.length) {
  // Initialize Firebase with the **correct** default Storage bucket
  // Must include the ".appspot.com" suffix, otherwise Storage operations will fail
  admin.initializeApp({
    storageBucket: STORAGE_BUCKETS,
  });
  // Set Firestore to use the custom database ID (e.g., "default" without parentheses)
  admin.firestore().settings({databaseId: DATABASE_ID});
}

// Initialize dependency injection container
DependencyConfig.initialize();


// User management functions
export {updateUserProfileFunction as updateUserProfile} from "./triggers/user/userProfile.trigger";
export {removeUserProfileImageFunction as removeUserProfileImage} from "./triggers/user/removeUserProfileImage.trigger";
export {onUserProfileImageUpload} from "./triggers/user/userImage.trigger";

// Project management functions
export {onProjectProfileImageUpload} from "./triggers/project/projectImage.trigger";
export {removeProjectProfileImageFunction as removeProjectProfileImage} from "./triggers/project/removeProjectProfileImage.trigger";
export {onProjectChange, onProjectDelete} from "./triggers/project/projectSync.trigger";

// System functions
export {helloWorldFunction as helloWorld} from "./triggers/system/helloWorld.trigger";
export {cleanupTempFiles, cleanupTempFilesNow} from "./triggers/system/cleanupTempFiles.trigger";

// Friend management functions
export {sendFriendRequestFunction as sendFriendRequest} from "./triggers/friend/friendManagement.trigger";
export {acceptFriendRequestFunction as acceptFriendRequest} from "./triggers/friend/friendManagement.trigger";
export {rejectFriendRequestFunction as rejectFriendRequest} from "./triggers/friend/friendManagement.trigger";
export {removeFriendFunction as removeFriend} from "./triggers/friend/friendManagement.trigger";
export {getFriendsFunction as getFriends} from "./triggers/friend/friendManagement.trigger";
export {getFriendRequestsFunction as getFriendRequests} from "./triggers/friend/friendManagement.trigger";

// DM management functions
export {createDMChannelFunction as createDMChannel} from "./triggers/dm/dmManagement.trigger";
export {blockDMChannelFunction as blockDMChannel} from "./triggers/dm/dmManagement.trigger";
export {unblockDMChannelFunction as unblockDMChannel} from "./triggers/dm/dmManagement.trigger";
export {unblockDMChannelByUserNameFunction as unblockDMChannelByUserName} from "./triggers/dm/dmManagement.trigger";

// Member management functions
export {removeMemberFunction as removeMember} from "./triggers/member/memberManagement.trigger";
export {blockMemberFunction as blockMember} from "./triggers/member/memberManagement.trigger";
export {leaveMemberFunction as leaveMember} from "./triggers/member/memberManagement.trigger";
export {deleteProjectFunction as deleteProject} from "./triggers/member/memberManagement.trigger";

// Invite management functions
export {generateInviteLinkFunction as generateInviteLink} from "./triggers/member/memberManagement.trigger";
export {validateInviteCodeFunction as validateInviteCode} from "./triggers/member/memberManagement.trigger";
export {joinProjectWithInviteFunction as joinProjectWithInvite} from "./triggers/member/memberManagement.trigger";

// Project member management functions
export {leaveProjectFunction as leaveProject} from "./triggers/member/memberManagement.trigger";
