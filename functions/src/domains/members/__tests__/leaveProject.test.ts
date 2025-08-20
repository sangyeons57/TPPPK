import { leaveProject } from "../http/leaveProject";
import { COLLECTIONS } from "../../../shared";
import { testCallable, cleanupFirestore, setupMockAuth } from "../../../__tests__/helpers";
import { TEST_USERS, TestDataFactory } from "../../../__tests__/helpers/testData";

describe("leaveProject", () => {
  beforeEach(async () => {
    await cleanupFirestore();
  });

  afterAll(async () => {
    await cleanupFirestore();
  });

  describe("Self Leave", () => {
    it("should successfully allow member to leave project", async () => {
      // Setup project
      const projectId = "test-project-1";
      const userId = TEST_USERS.BOB.uid;

      // Mock project
      await TestDataFactory.setDocument(COLLECTIONS.PROJECTS, projectId, {
        ownerId: TEST_USERS.ALICE.uid,
        name: { value: "Test Project" },
        createdAt: new Date(),
        updatedAt: new Date(),
      });

      // Mock member
      await TestDataFactory.setDocument(
        `${COLLECTIONS.PROJECTS}/${projectId}/${COLLECTIONS.MEMBERS}`,
        userId,
        {
          roleIds: [],
          status: "active",
          createdAt: new Date(),
          updatedAt: new Date(),
        }
      );

      // Mock project wrapper
      await TestDataFactory.setDocument(
        `${COLLECTIONS.USERS}/${userId}/${COLLECTIONS.PROJECT_WRAPPERS}`,
        projectId,
        {
          order: 1,
          projectName: "Test Project",
          createdAt: new Date(),
          updatedAt: new Date(),
        }
      );

      // Setup auth as member
      setupMockAuth(userId);

      const request = { projectId };
      const response = await testCallable(leaveProject, request);

      expect(response.success).toBe(true);

      // Verify member was removed
      const memberExists = await TestDataFactory.documentExists(
        `${COLLECTIONS.PROJECTS}/${projectId}/${COLLECTIONS.MEMBERS}`,
        userId
      );
      expect(memberExists).toBe(false);

      // Verify project wrapper was removed
      const wrapperExists = await TestDataFactory.documentExists(
        `${COLLECTIONS.USERS}/${userId}/${COLLECTIONS.PROJECT_WRAPPERS}`,
        projectId
      );
      expect(wrapperExists).toBe(false);
    });
  });

  describe("Owner Remove Member", () => {
    it("should successfully allow owner to remove another member", async () => {
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

      // Mock target member
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

      // Mock project wrapper for target user
      await TestDataFactory.setDocument(
        `${COLLECTIONS.USERS}/${targetUserId}/${COLLECTIONS.PROJECT_WRAPPERS}`,
        projectId,
        {
          order: 1,
          projectName: "Test Project",
          createdAt: new Date(),
          updatedAt: new Date(),
        }
      );

      // Setup auth as owner
      setupMockAuth(ownerId);

      const request = { 
        projectId,
        targetUserId 
      };
      const response = await testCallable(leaveProject, request);

      expect(response.success).toBe(true);

      // Verify target member was removed
      const memberExists = await TestDataFactory.documentExists(
        `${COLLECTIONS.PROJECTS}/${projectId}/${COLLECTIONS.MEMBERS}`,
        targetUserId
      );
      expect(memberExists).toBe(false);

      // Verify project wrapper was removed for target user
      const wrapperExists = await TestDataFactory.documentExists(
        `${COLLECTIONS.USERS}/${targetUserId}/${COLLECTIONS.PROJECT_WRAPPERS}`,
        projectId
      );
      expect(wrapperExists).toBe(false);
    });

    it("should throw error when non-owner tries to remove another member", async () => {
      // Setup project with owner
      const projectId = "test-project-3";
      const ownerId = TEST_USERS.ALICE.uid;
      const nonOwnerId = TEST_USERS.CHARLIE.uid;
      const targetUserId = TEST_USERS.BOB.uid;

      // Mock project
      await TestDataFactory.setDocument(COLLECTIONS.PROJECTS, projectId, {
        ownerId,
        name: { value: "Test Project" },
        createdAt: new Date(),
        updatedAt: new Date(),
      });

      // Mock target member
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
        targetUserId 
      };

      await expect(testCallable(leaveProject, request)).rejects.toThrow(
        "Only project owner can remove other members"
      );
    });

    it("should throw error when owner tries to remove themselves via targetUserId", async () => {
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
        targetUserId: ownerId // Owner trying to remove themselves
      };

      await expect(testCallable(leaveProject, request)).rejects.toThrow(
        "Owner cannot be removed from project"
      );
    });
  });

  describe("Error Cases", () => {
    it("should throw error when project doesn't exist", async () => {
      const nonExistentProjectId = "non-existent-project";
      const userId = TEST_USERS.BOB.uid;

      // Setup auth
      setupMockAuth(userId);

      const request = { projectId: nonExistentProjectId };

      await expect(testCallable(leaveProject, request)).rejects.toThrow("Project not found");
    });

    it("should throw error when missing required projectId", async () => {
      const userId = TEST_USERS.BOB.uid;

      // Setup auth
      setupMockAuth(userId);

      const request = {}; // Missing projectId

      await expect(testCallable(leaveProject, request)).rejects.toThrow();
    });
  });
});