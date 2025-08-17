import { createLogger, getOrCreateRequestId, withTracing } from "../logger";

// Mock console methods
const mockConsole = {
  log: jest.fn(),
  error: jest.fn(),
  warn: jest.fn(),
  debug: jest.fn(),
};

// Replace console methods
Object.assign(console, mockConsole);

describe("Logger Module", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("createLogger", () => {
    it("should create logger with default context", () => {
      const logger = createLogger({});
      expect(logger).toHaveProperty("info");
      expect(logger).toHaveProperty("error");
      expect(logger).toHaveProperty("warn");
      expect(logger).toHaveProperty("debug");
    });

    it("should log info messages with structured format", () => {
      const logger = createLogger({
        domain: "test",
        operation: "testOp",
        requestId: "req-123",
      });

      logger.info("Test message", { key: "value" });

      expect(mockConsole.log).toHaveBeenCalledWith(
        expect.stringContaining('"level":"INFO"')
      );
      expect(mockConsole.log).toHaveBeenCalledWith(
        expect.stringContaining('"message":"Test message"')
      );
      expect(mockConsole.log).toHaveBeenCalledWith(
        expect.stringContaining('"domain":"test"')
      );
      expect(mockConsole.log).toHaveBeenCalledWith(
        expect.stringContaining('"requestId":"req-123"')
      );
    });

    it("should log error messages", () => {
      const logger = createLogger({ domain: "test" });
      const error = new Error("Test error");

      logger.error("Error occurred", error);

      expect(mockConsole.error).toHaveBeenCalledWith(
        expect.stringContaining('"level":"ERROR"')
      );
      expect(mockConsole.error).toHaveBeenCalledWith(
        expect.stringContaining('"message":"Error occurred"')
      );
    });

    it("should include duration in logs", () => {
      const startTime = Date.now() - 1000; // 1 second ago
      const logger = createLogger({ startTime });

      logger.info("Test message");

      expect(mockConsole.log).toHaveBeenCalledWith(
        expect.stringContaining('"duration":')
      );
    });

    it("should include userId when provided", () => {
      const logger = createLogger({
        userId: "user-123",
        domain: "test",
      });

      logger.info("Test message");

      expect(mockConsole.log).toHaveBeenCalledWith(
        expect.stringContaining('"userId":"user-123"')
      );
    });
  });

  describe("getOrCreateRequestId", () => {
    it("should return request ID from headers", () => {
      const request = {
        headers: { "x-request-id": "existing-id" },
      };

      const requestId = getOrCreateRequestId(request);
      expect(requestId).toBe("existing-id");
    });

    it("should generate new request ID when not in headers", () => {
      const request = { headers: {} };

      const requestId = getOrCreateRequestId(request);
      expect(requestId).toMatch(/^[0-9a-f-]+$/); // UUID format
    });

    it("should handle undefined request", () => {
      const requestId = getOrCreateRequestId(undefined);
      expect(requestId).toMatch(/^[0-9a-f-]+$/); // UUID format
    });
  });

  describe("withTracing", () => {
    it("should trace successful operations", async () => {
      const context = {
        requestId: "req-123",
        domain: "test",
        operation: "testOp",
        startTime: Date.now(),
      };

      const operation = jest.fn().mockResolvedValue("success");

      const result = await withTracing(context, operation);

      expect(result).toBe("success");
      expect(operation).toHaveBeenCalled();

      // Check that start and completion logs were made
      expect(mockConsole.log).toHaveBeenCalledWith(
        expect.stringContaining("Starting operation: testOp")
      );
      expect(mockConsole.log).toHaveBeenCalledWith(
        expect.stringContaining("Operation completed successfully: testOp")
      );
    });

    it("should trace failed operations", async () => {
      const context = {
        requestId: "req-123",
        domain: "test",
        operation: "testOp",
        startTime: Date.now(),
      };

      const error = new Error("Operation failed");
      const operation = jest.fn().mockRejectedValue(error);

      await expect(withTracing(context, operation)).rejects.toThrow("Operation failed");

      expect(mockConsole.log).toHaveBeenCalledWith(
        expect.stringContaining("Starting operation: testOp")
      );
      expect(mockConsole.error).toHaveBeenCalledWith(
        expect.stringContaining("Operation failed: testOp")
      );
    });

    it("should include operation duration", async () => {
      const context = {
        requestId: "req-123",
        domain: "test",
        operation: "testOp",
        startTime: Date.now() - 100, // Started 100ms ago
      };

      const operation = jest.fn().mockResolvedValue("success");

      await withTracing(context, operation);

      // Check that duration is included in logs
      expect(mockConsole.log).toHaveBeenCalledWith(
        expect.stringContaining('"duration":')
      );
    });
  });
});