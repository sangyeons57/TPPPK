import { helloWorld } from "../http/helloWorld";
import { createMockRequest, TEST_USERS } from "../../../__tests__/helpers";

describe("helloWorld", () => {
  it("should return welcome message for authenticated user", async () => {
    const request = createMockRequest({}, TEST_USERS.ALICE);

    const result = await helloWorld(request);

    expect(result).toEqual({
      message: "Hello from simplified Firebase Functions!",
      timestamp: expect.any(String),
      version: "2.0.0-simplified",
      auth: true,
      region: "asia-northeast3",
      architecture: "domains + shared",
    });
  });

  it("should return welcome message for unauthenticated user", async () => {
    const request = createMockRequest({});

    const result = await helloWorld(request);

    expect(result).toEqual({
      message: "Hello from simplified Firebase Functions!",
      timestamp: expect.any(String),
      version: "2.0.0-simplified",
      auth: false,
      region: "asia-northeast3",
      architecture: "domains + shared",
    });
  });

  it("should include valid timestamp", async () => {
    const request = createMockRequest({});

    const result = await helloWorld(request);

    const timestamp = new Date(result.timestamp);
    expect(timestamp).toBeInstanceOf(Date);
    expect(timestamp.getTime()).toBeGreaterThan(Date.now() - 5000); // Within last 5 seconds
  });

  it("should handle request data", async () => {
    const requestData = { test: "data", number: 123 };
    const request = createMockRequest(requestData, TEST_USERS.ALICE);

    const result = await helloWorld(request);

    expect(result.auth).toBe(true);
    expect(result.message).toContain("Hello from simplified");
  });
});