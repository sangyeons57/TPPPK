import { joinProject } from "../http/joinProject";
import { 
  mockFirestore, 
  MockFirestoreHelper, 
  createMockRequest, 
  TEST_USERS, 
  testCallable 
} from "../../../__tests__/helpers";
import { HttpsError } from "firebase-functions/v2/https";
import { COLLECTIONS } from "../../../shared";

// Mock admin.firestore
jest.mock("firebase-admin", () => ({
  firestore: () => mockFirestore,
  app: () => ({}),
}));

describe("joinProject (projectId-based)", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    MockFirestoreHelper.reset();
  });

  it("throws when unauthenticated", async () => {
    const request = createMockRequest({ projectId: "project-1" });
    await expect(testCallable(joinProject, request)).rejects.toThrow(HttpsError);
    await expect(testCallable(joinProject, request)).rejects.toThrow("User not authenticated");
  });

  it("throws when project not found", async () => {
    const request = createMockRequest({ projectId: "missing" }, TEST_USERS.ALICE);
    await expect(testCallable(joinProject, request)).rejects.toThrow(HttpsError);
  });

  it("joins project successfully via projectId", async () => {
    // Mock project
    MockFirestoreHelper.mockCollection(COLLECTIONS.PROJECTS, {
      "project-1": {
        id: "project-1",
        name: "Project One",
        imageUrl: "https://example.com/p.png",
      },
    });

    const request = createMockRequest({ projectId: "project-1" }, TEST_USERS.ALICE);
    const result = await testCallable<any, any>(joinProject, request);

    expect(result.success).toBe(true);
    expect(result.projectId).toBe("project-1");
    expect(result.role).toBe("member");
  });

  it("throws when user is banned from project", async () => {
    // Mock project
    MockFirestoreHelper.mockCollection(COLLECTIONS.PROJECTS, {
      "project-1": {
        id: "project-1",
        name: "Project One",
        imageUrl: "https://example.com/p.png",
      },
    });

    // Mock banned member
    MockFirestoreHelper.mockSubcollection(
      COLLECTIONS.PROJECTS,
      "project-1",
      COLLECTIONS.MEMBERS,
      {
        [TEST_USERS.ALICE.uid]: {
          id: TEST_USERS.ALICE.uid,
          roleIds: [],
          status: "banned",
          blockedAt: new Date(),
          blockedBy: "owner-id",
        },
      }
    );

    const request = createMockRequest({ projectId: "project-1" }, TEST_USERS.ALICE);
    await expect(testCallable(joinProject, request)).rejects.toThrow(
      "User is permanently banned from this project"
    );
  });

  it("throws when user is blocked from project", async () => {
    // Mock project
    MockFirestoreHelper.mockCollection(COLLECTIONS.PROJECTS, {
      "project-1": {
        id: "project-1",
        name: "Project One",
        imageUrl: "https://example.com/p.png",
      },
    });

    // Mock blocked member
    MockFirestoreHelper.mockSubcollection(
      COLLECTIONS.PROJECTS,
      "project-1",
      COLLECTIONS.MEMBERS,
      {
        [TEST_USERS.ALICE.uid]: {
          id: TEST_USERS.ALICE.uid,
          roleIds: [],
          status: "blocked",
          blockedAt: new Date(),
          blockedBy: "owner-id",
        },
      }
    );

    const request = createMockRequest({ projectId: "project-1" }, TEST_USERS.ALICE);
    await expect(testCallable(joinProject, request)).rejects.toThrow(
      "User is blocked from this project"
    );
  });

  it("returns success when user is already an active member", async () => {
    // Mock project
    MockFirestoreHelper.mockCollection(COLLECTIONS.PROJECTS, {
      "project-1": {
        id: "project-1",
        name: "Project One",
        imageUrl: "https://example.com/p.png",
      },
    });

    // Mock active member
    MockFirestoreHelper.mockSubcollection(
      COLLECTIONS.PROJECTS,
      "project-1",
      COLLECTIONS.MEMBERS,
      {
        [TEST_USERS.ALICE.uid]: {
          id: TEST_USERS.ALICE.uid,
          roleIds: [],
          status: "active",
          createdAt: new Date(),
          updatedAt: new Date(),
        },
      }
    );

    const request = createMockRequest({ projectId: "project-1" }, TEST_USERS.ALICE);
    const result = await testCallable<any, any>(joinProject, request);

    expect(result.success).toBe(true);
    expect(result.projectId).toBe("project-1");
    expect(result.role).toBe("member");
  });
});

