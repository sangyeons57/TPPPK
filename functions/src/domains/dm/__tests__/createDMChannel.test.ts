import { createDMChannel } from "../http/createDMChannel";
import { mockFirestore, MockFirestoreHelper, createMockRequest, TEST_USERS, testCallable } from "../../../__tests__/helpers";
import { HttpsError } from "firebase-functions/v2/https";

// Mock admin.firestore
jest.mock("firebase-admin", () => ({
  firestore: () => mockFirestore,
  app: () => ({}),
}));

describe("createDMChannel (simplified)", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    MockFirestoreHelper.reset();
  });

  it("requires authentication", async () => {
    const request = createMockRequest({ targetUserId: TEST_USERS.BOB.uid });
    await expect(testCallable(createDMChannel, request)).rejects.toThrow(HttpsError);
  });

  it("validates targetUserId and prevents self DM", async () => {
    const missing = createMockRequest({}, TEST_USERS.ALICE);
    await expect(testCallable(createDMChannel, missing)).rejects.toThrow(HttpsError);

    const selfReq = createMockRequest({ targetUserId: TEST_USERS.ALICE.uid }, TEST_USERS.ALICE);
    await expect(testCallable(createDMChannel, selfReq)).rejects.toThrow(HttpsError);
  });

  it("creates channel and both wrappers on first call", async () => {
    // Mock target user exists
    (mockFirestore.collection().doc().get as any)
      .mockResolvedValueOnce({ exists: true, data: () => ({ name: TEST_USERS.ALICE.name }) }) // current user
      .mockResolvedValueOnce({ exists: true, data: () => ({ name: TEST_USERS.BOB.name }) }) // target user
      .mockResolvedValueOnce({ exists: false }); // channel

    const req = createMockRequest({ targetUserId: TEST_USERS.BOB.uid }, TEST_USERS.ALICE);
    const result = await testCallable(createDMChannel, req);

    expect(result.success).toBe(true);
    expect(result.channelId).toMatch(/^dm_/);

    // One batch commit, at least three set calls (channel + two wrappers)
    const batch = (mockFirestore.batch as any).mock.results[0].value;
    expect(batch.set).toHaveBeenCalled();
    expect(batch.commit).toHaveBeenCalledTimes(1);
  });

  it("reuses existing channel and still upserts wrappers", async () => {
    // Mock target user exists
    (mockFirestore.collection().doc().get as any)
      .mockResolvedValueOnce({ exists: true, data: () => ({ name: TEST_USERS.ALICE.name }) }) // current user
      .mockResolvedValueOnce({ exists: true, data: () => ({ name: TEST_USERS.BOB.name }) }) // target user
      .mockResolvedValueOnce({ exists: true, data: () => ({ id: "existing" }) }); // channel exists

    const req = createMockRequest({ targetUserId: TEST_USERS.BOB.uid }, TEST_USERS.ALICE);
    const result = await testCallable(createDMChannel, req);

    expect(result.success).toBe(true);
    const batch = (mockFirestore.batch as any).mock.results[0].value;
    expect(batch.set).toHaveBeenCalled();
    expect(batch.commit).toHaveBeenCalledTimes(1);
  });
});

