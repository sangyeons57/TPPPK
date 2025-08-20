import { onProjectDelete } from "../events/onProjectChange";
import { mockFirestore, MockFirestoreHelper, mockBatch } from "../../../__tests__/helpers";

// Mock admin.firestore
jest.mock("firebase-admin", () => ({
  firestore: () => mockFirestore,
  app: () => ({}),
}));

describe("onProjectDelete - wrapper cleanup", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    MockFirestoreHelper.reset();
  });

  it("deletes all user project wrappers via collectionGroup", async () => {
    const projectId = "proj-123";

    // Seed user wrappers at users/{uid}/projects_wrapper/{projectId}
    MockFirestoreHelper.addDocument(`users/userA/projects_wrapper`, projectId, {
      projectName: "Test Project",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
    MockFirestoreHelper.addDocument(`users/userB/projects_wrapper`, projectId, {
      projectName: "Test Project",
      createdAt: new Date(),
      updatedAt: new Date(),
    });

    const event: any = {
      params: { projectId },
    };

    await onProjectDelete(event);

    // Verify batch delete was called for both wrapper docs and committed
    expect(mockBatch.delete).toHaveBeenCalledTimes(2);
    expect(mockBatch.commit).toHaveBeenCalledTimes(1);
  });
});

