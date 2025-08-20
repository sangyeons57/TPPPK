import { getFriends } from "../http/getFriends";
import { mockFirestore, MockFirestoreHelper, createMockRequest, TEST_USERS, testCallable } from "../../../__tests__/helpers";
import { HttpsError } from "firebase-functions/v2/https";
import { FRIEND_SUBCOLLECTION_STATUS } from "../../../shared";

// Mock admin.firestore
jest.mock("firebase-admin", () => ({
  firestore: () => mockFirestore,
  app: () => ({}),
}));

describe("getFriends", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    MockFirestoreHelper.reset();
  });

  describe("Authentication", () => {
    it("should throw error when user is not authenticated", async () => {
      const request = createMockRequest({});

      await expect(testCallable(getFriends, request)).rejects.toThrow(HttpsError);
      await expect(testCallable(getFriends, request)).rejects.toThrow("User not authenticated");
    });
  });

  describe("Successful Retrieval", () => {
    it("should return friends list from subcollection successfully", async () => {
      // Mock ACCEPTED friends in user's subcollection
      const mockFriendsQuery = {
        empty: false,
        docs: [
          {
            id: TEST_USERS.BOB.uid,
            data: () => ({
              name: TEST_USERS.BOB.name,
              profileImageUrl: TEST_USERS.BOB.profileImageUrl,
              status: FRIEND_SUBCOLLECTION_STATUS.ACCEPTED,
              acceptedAt: new Date("2024-01-01"),
              createdAt: new Date("2024-01-01"),
            }),
          },
          {
            id: TEST_USERS.CHARLIE.uid,
            data: () => ({
              name: TEST_USERS.CHARLIE.name,
              profileImageUrl: TEST_USERS.CHARLIE.profileImageUrl,
              status: FRIEND_SUBCOLLECTION_STATUS.ACCEPTED,
              acceptedAt: new Date("2024-01-02"),
              createdAt: new Date("2024-01-02"),
            }),
          },
        ],
      };

      const mockCollection = mockFirestore.collection(`users/${TEST_USERS.ALICE.uid}/friends`);
      (mockCollection.where as any).mockReturnValue(mockCollection);
      (mockCollection.get as any).mockResolvedValueOnce(mockFriendsQuery);

      const request = createMockRequest({}, TEST_USERS.ALICE);
      const result = await testCallable(getFriends, request);

      expect(result).toEqual({
        friends: [
          {
            id: TEST_USERS.BOB.uid,
            name: TEST_USERS.BOB.name,
            profileImageUrl: TEST_USERS.BOB.profileImageUrl,
            isOnline: false,
          },
          {
            id: TEST_USERS.CHARLIE.uid,
            name: TEST_USERS.CHARLIE.name,
            profileImageUrl: TEST_USERS.CHARLIE.profileImageUrl,
            isOnline: false,
          },
        ],
      });

      // Verify subcollection query was called correctly
      expect(mockFirestore.collection).toHaveBeenCalledWith(`users/${TEST_USERS.ALICE.uid}/friends`);
      expect(mockCollection.where).toHaveBeenCalledWith("status", "==", FRIEND_SUBCOLLECTION_STATUS.ACCEPTED);
    });

    it("should return empty list when no friends found", async () => {
      const mockEmptyQuery = {
        empty: true,
        docs: [],
      };

      const mockCollection = mockFirestore.collection(`users/${TEST_USERS.ALICE.uid}/friends`);
      (mockCollection.where as any).mockReturnValue(mockCollection);
      (mockCollection.get as any).mockResolvedValueOnce(mockEmptyQuery);

      const request = createMockRequest({}, TEST_USERS.ALICE);
      const result = await testCallable(getFriends, request);

      expect(result).toEqual({
        friends: [],
      });
    });

    it("should handle corrupted friend data gracefully", async () => {
      const mockFriendsQuery = {
        empty: false,
        docs: [
          {
            id: TEST_USERS.BOB.uid,
            data: () => ({
              name: TEST_USERS.BOB.name,
              profileImageUrl: TEST_USERS.BOB.profileImageUrl,
              status: FRIEND_SUBCOLLECTION_STATUS.ACCEPTED,
              acceptedAt: new Date("2024-01-01"),
            }),
          },
          {
            id: "corrupted-doc",
            data: () => {
              throw new Error("Corrupted data");
            },
          },
        ],
      };

      const mockCollection = mockFirestore.collection(`users/${TEST_USERS.ALICE.uid}/friends`);
      (mockCollection.where as any).mockReturnValue(mockCollection);
      (mockCollection.get as any).mockResolvedValueOnce(mockFriendsQuery);

      const request = createMockRequest({}, TEST_USERS.ALICE);
      const result = await testCallable(getFriends, request);

      // Should return only valid friends, filtering out corrupted data
      expect(result).toEqual({
        friends: [
          {
            id: TEST_USERS.BOB.uid,
            name: TEST_USERS.BOB.name,
            profileImageUrl: TEST_USERS.BOB.profileImageUrl,
            isOnline: false,
          },
        ],
      });
    });
  });

  describe("Error Handling", () => {
    it("should handle Firestore query errors", async () => {
      const mockCollection = mockFirestore.collection(`users/${TEST_USERS.ALICE.uid}/friends`);
      (mockCollection.where as any).mockReturnValue(mockCollection);
      (mockCollection.get as any).mockRejectedValueOnce(new Error("Firestore query failed"));

      const request = createMockRequest({}, TEST_USERS.ALICE);

      await expect(testCallable(getFriends, request)).rejects.toThrow("Firestore query failed");
    });
  });
});