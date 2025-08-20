import { blockMember } from "../http/blockMember";
import { COLLECTIONS } from "../../../shared";
import { testCallable, cleanupFirestore, setupMockAuth } from "../../../__tests__/helpers";
import { TEST_USERS, TestDataFactory } from "../../../__tests__/helpers/testData";

describe("blockMember", () => {
  beforeEach(async () => {
    await cleanupFirestore();
  });

  afterAll(async () => {
    await cleanupFirestore();
  });

  it("should successfully block a member when called by project owner", async () => {
    // Setup project with owner
    const projectId = "test-project-1";
    const ownerId = TEST_USERS.ALICE.uid;
    const targetUserId = TEST_USERS.BOB.uid;

    // Mock project
    await TestDataFactory.setDocument(COLLECTIONS.PROJECTS, projectId, {
      ownerId,
      name: { value: "Test Project" },
      createdAt: new Date(),
      updatedAt: new Date(),
    });

    // Mock existing member
    await TestDataFactory.setDocument(
      `${COLLECTIONS.PROJECTS}/${projectId}/${COLLECTIONS.MEMBERS}`,
      targetUserId,
      {
        roleIds: [],
        status: "active",
        createdAt: new Date(),
        updatedAt: new Date(),
      }
    );

    // Setup auth as owner
    setupMockAuth(ownerId);

    const request = {
      projectId,
      targetUserId,
      blockType: "blocked" as const,
    };

    const response = await testCallable(blockMember, request);

    expect(response.success).toBe(true);
    expect(response.message).toBe("Member blocked successfully");

    // Verify member status was updated
    const memberDoc = await TestDataFactory.getDocument(
      `${COLLECTIONS.PROJECTS}/${projectId}/${COLLECTIONS.MEMBERS}`,
      targetUserId
    );
    expect(memberDoc.status).toBe("blocked");
    expect(memberDoc.blockedBy).toBe(ownerId);
    expect(memberDoc.blockedAt).toBeDefined();
  });

  it("should successfully ban a member when called by project owner", async () => {
    // Setup project with owner
    const projectId = "test-project-2";
    const ownerId = TEST_USERS.ALICE.uid;
    const targetUserId = TEST_USERS.BOB.uid;

    // Mock project
    await TestDataFactory.setDocument(COLLECTIONS.PROJECTS, projectId, {
      ownerId,
      name: { value: "Test Project" },
      createdAt: new Date(),
      updatedAt: new Date(),
    });

    // Mock existing member
    await TestDataFactory.setDocument(
      `${COLLECTIONS.PROJECTS}/${projectId}/${COLLECTIONS.MEMBERS}`,
      targetUserId,
      {
        roleIds: [],
        status: "active",
        createdAt: new Date(),
        updatedAt: new Date(),
      }
    );

    // Setup auth as owner
    setupMockAuth(ownerId);

    const request = {
      projectId,
      targetUserId,
      blockType: "banned" as const,
    };

    const response = await testCallable(blockMember, request);

    expect(response.success).toBe(true);
    expect(response.message).toBe("Member banned successfully");

    // Verify member status was updated
    const memberDoc = await TestDataFactory.getDocument(
      `${COLLECTIONS.PROJECTS}/${projectId}/${COLLECTIONS.MEMBERS}`,
      targetUserId
    );
    expect(memberDoc.status).toBe("banned");
    expect(memberDoc.blockedBy).toBe(ownerId);
  });

  it("should throw error when non-owner tries to block member", async () => {
    // Setup project with different owner
    const projectId = "test-project-3";
    const ownerId = TEST_USERS.ALICE.uid;
    const targetUserId = TEST_USERS.BOB.uid;
    const nonOwnerId = TEST_USERS.CHARLIE.uid;

    // Mock project
    await TestDataFactory.setDocument(COLLECTIONS.PROJECTS, projectId, {
      ownerId,
      name: { value: "Test Project" },
      createdAt: new Date(),
      updatedAt: new Date(),
    });

    // Mock existing member
    await TestDataFactory.setDocument(
      `${COLLECTIONS.PROJECTS}/${projectId}/${COLLECTIONS.MEMBERS}`,
      targetUserId,
      {
        roleIds: [],
        status: "active",
        createdAt: new Date(),
        updatedAt: new Date(),
      }
    );

    // Setup auth as non-owner
    setupMockAuth(nonOwnerId);

    const request = {
      projectId,
      targetUserId,
      blockType: "blocked" as const,
    };

    await expect(testCallable(blockMember, request)).rejects.toThrow(
      "Only project owner can block members"
    );
  });

  it("should throw error when owner tries to block themselves", async () => {
    // Setup project with owner
    const projectId = "test-project-4";
    const ownerId = TEST_USERS.ALICE.uid;

    // Mock project
    await TestDataFactory.setDocument(COLLECTIONS.PROJECTS, projectId, {
      ownerId,
      name: { value: "Test Project" },
      createdAt: new Date(),
      updatedAt: new Date(),
    });

    // Mock owner as member
    await TestDataFactory.setDocument(
      `${COLLECTIONS.PROJECTS}/${projectId}/${COLLECTIONS.MEMBERS}`,
      ownerId,
      {
        roleIds: [],
        status: "active",
        createdAt: new Date(),
        updatedAt: new Date(),
      }
    );

    // Setup auth as owner
    setupMockAuth(ownerId);

    const request = {
      projectId,
      targetUserId: ownerId, // Owner trying to block themselves
      blockType: "blocked" as const,
    };

    await expect(testCallable(blockMember, request)).rejects.toThrow(
      "Owner cannot block themselves"
    );
  });

  it("should throw error when target user is not a member", async () => {
    // Setup project with owner
    const projectId = "test-project-5";
    const ownerId = TEST_USERS.ALICE.uid;
    const nonMemberUserId = TEST_USERS.BOB.uid;

    // Mock project
    await TestDataFactory.setDocument(COLLECTIONS.PROJECTS, projectId, {
      ownerId,
      name: { value: "Test Project" },
      createdAt: new Date(),
      updatedAt: new Date(),
    });

    // No member document for target user

    // Setup auth as owner
    setupMockAuth(ownerId);

    const request = {
      projectId,
      targetUserId: nonMemberUserId,
      blockType: "blocked" as const,
    };

    await expect(testCallable(blockMember, request)).rejects.toThrow(
      "Target user is not a member of this project"
    );
  });

  it("should throw error when project doesn't exist", async () => {
    const nonExistentProjectId = "non-existent-project";
    const ownerId = TEST_USERS.ALICE.uid;
    const targetUserId = TEST_USERS.BOB.uid;

    // Setup auth as owner
    setupMockAuth(ownerId);

    const request = {
      projectId: nonExistentProjectId,
      targetUserId,
      blockType: "blocked" as const,
    };

    await expect(testCallable(blockMember, request)).rejects.toThrow("Project not found");
  });

  it("should throw error with invalid blockType", async () => {
    const projectId = "test-project-6";
    const ownerId = TEST_USERS.ALICE.uid;
    const targetUserId = TEST_USERS.BOB.uid;

    // Mock project
    await TestDataFactory.setDocument(COLLECTIONS.PROJECTS, projectId, {
      ownerId,
      name: { value: "Test Project" },
      createdAt: new Date(),
      updatedAt: new Date(),
    });

    // Setup auth as owner
    setupMockAuth(ownerId);

    const request = {
      projectId,
      targetUserId,
      blockType: "invalid" as any,
    };

    await expect(testCallable(blockMember, request)).rejects.toThrow(
      "blockType must be 'blocked' or 'banned'"
    );
  });
});