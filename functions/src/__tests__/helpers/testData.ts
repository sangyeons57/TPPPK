import { COLLECTIONS, FRIEND_STATUS, MEMBER_ROLES } from "../../shared/constants";

export const TEST_PROJECT_ID = "test-project-123";
export const TEST_CHANNEL_ID = "test-channel-456";
export const TEST_INVITE_CODE = "TEST1234";

export const TestDataFactory = {
  // User data
  createUser: (overrides: Partial<any> = {}) => ({
    id: "user-123",
    name: "Test User",
    email: "test@example.com",
    createdAt: new Date("2024-01-01"),
    updatedAt: new Date("2024-01-01"),
    isOnline: false,
    ...overrides,
  }),

  // Project data
  createProject: (overrides: Partial<any> = {}) => ({
    id: TEST_PROJECT_ID,
    name: "Test Project",
    description: "A test project",
    createdBy: "user-123",
    createdAt: new Date("2024-01-01"),
    updatedAt: new Date("2024-01-01"),
    ...overrides,
  }),

  // Friend request data
  createFriendRequest: (overrides: Partial<any> = {}) => ({
    id: "friend-request-123",
    userId: "user-123",
    friendId: "user-456",
    status: FRIEND_STATUS.PENDING,
    createdAt: new Date("2024-01-01"),
    updatedAt: new Date("2024-01-01"),
    ...overrides,
  }),

  // DM Channel data
  createDMChannel: (overrides: Partial<any> = {}) => ({
    id: TEST_CHANNEL_ID,
    participants: ["user-123", "user-456"],
    createdAt: new Date("2024-01-01"),
    updatedAt: new Date("2024-01-01"),
    ...overrides,
  }),

  // DM Wrapper data
  createDMWrapper: (overrides: Partial<any> = {}) => ({
    id: "dm-wrapper-123",
    userId: "user-123",
    channelId: TEST_CHANNEL_ID,
    targetUserId: "user-456",
    targetUserName: "Target User",
    isBlocked: false,
    createdAt: new Date("2024-01-01"),
    updatedAt: new Date("2024-01-01"),
    ...overrides,
  }),

  // Member data
  createMember: (overrides: Partial<any> = {}) => ({
    id: "member-123",
    userId: "user-123",
    projectId: TEST_PROJECT_ID,
    role: MEMBER_ROLES.MEMBER,
    joinedAt: new Date("2024-01-01"),
    ...overrides,
  }),

  // Invite data
  createInvite: (overrides: Partial<any> = {}) => ({
    id: "invite-123",
    code: TEST_INVITE_CODE,
    projectId: TEST_PROJECT_ID,
    createdBy: "user-123",
    isActive: true,
    usedCount: 0,
    maxUses: 50,
    createdAt: new Date("2024-01-01"),
    expiresAt: new Date("2024-12-31"),
    ...overrides,
  }),

  // Message data
  createMessage: (overrides: Partial<any> = {}) => ({
    id: "message-123",
    content: "Test message",
    senderId: "user-123",
    channelId: TEST_CHANNEL_ID,
    type: "text",
    createdAt: new Date("2024-01-01"),
    ...overrides,
  }),
};

// Mock Firestore collections with test data
export const setupTestData = () => {
  const testData = {
    [COLLECTIONS.USERS]: {
      "user-123": TestDataFactory.createUser({ id: "user-123", name: "Alice" }),
      "user-456": TestDataFactory.createUser({ id: "user-456", name: "Bob" }),
      "admin-789": TestDataFactory.createUser({ id: "admin-789", name: "Admin" }),
    },
    [COLLECTIONS.PROJECTS]: {
      [TEST_PROJECT_ID]: TestDataFactory.createProject(),
    },
    [COLLECTIONS.FRIENDS]: {
      "friend-request-123": TestDataFactory.createFriendRequest(),
    },
    [COLLECTIONS.DM_CHANNELS]: {
      [TEST_CHANNEL_ID]: TestDataFactory.createDMChannel(),
    },
    [COLLECTIONS.DM_WRAPPERS]: {
      "dm-wrapper-123": TestDataFactory.createDMWrapper(),
    },
    [COLLECTIONS.MEMBERS]: {
      "member-123": TestDataFactory.createMember({ role: MEMBER_ROLES.OWNER }),
      "member-456": TestDataFactory.createMember({ 
        id: "member-456", 
        userId: "user-456", 
        role: MEMBER_ROLES.MEMBER 
      }),
    },
    [COLLECTIONS.INVITES]: {
      "invite-123": TestDataFactory.createInvite(),
    },
  };

  return testData;
};