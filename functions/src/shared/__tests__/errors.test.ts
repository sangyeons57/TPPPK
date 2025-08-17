import { AppError, handleError } from "../errors";
import { HttpsError } from "firebase-functions/v2/https";

describe("Errors Module", () => {
  describe("AppError", () => {
    it("should create AppError with code and message", () => {
      const error = new AppError("test-code", "Test message");
      
      expect(error).toBeInstanceOf(Error);
      expect(error).toBeInstanceOf(AppError);
      expect(error.code).toBe("test-code");
      expect(error.message).toBe("Test message");
      expect(error.name).toBe("AppError");
    });

    it("should create AppError with details", () => {
      const details = { field: "value", nested: { key: "data" } };
      const error = new AppError("test-code", "Test message", details);
      
      expect(error.details).toEqual(details);
    });
  });

  describe("handleError", () => {
    it("should convert AppError to HttpsError", () => {
      const appError = new AppError("invalid-argument", "Invalid input", { field: "name" });
      const result = handleError(appError, "testFunction");
      
      expect(result).toBeInstanceOf(HttpsError);
      expect(result.code).toBe("invalid-argument");
      expect(result.message).toBe("Invalid input");
    });

    it("should pass through HttpsError unchanged", () => {
      const httpsError = new HttpsError("permission-denied", "Access denied");
      const result = handleError(httpsError, "testFunction");
      
      expect(result).toBe(httpsError);
    });

    it("should convert Error to internal HttpsError", () => {
      const error = new Error("Generic error");
      const result = handleError(error, "testFunction");
      
      expect(result).toBeInstanceOf(HttpsError);
      expect(result.code).toBe("internal");
      expect(result.message).toBe("Generic error");
    });

    it("should convert unknown error to internal HttpsError", () => {
      const unknownError = "String error";
      const result = handleError(unknownError, "testFunction");
      
      expect(result).toBeInstanceOf(HttpsError);
      expect(result.code).toBe("internal");
      expect(result.message).toBe("Unknown error occurred");
    });

    it("should convert null/undefined to internal HttpsError", () => {
      const nullError = null;
      const result = handleError(nullError, "testFunction");
      
      expect(result).toBeInstanceOf(HttpsError);
      expect(result.code).toBe("internal");
      expect(result.message).toBe("Unknown error occurred");
    });

    it("should log error with context", () => {
      const consoleSpy = jest.spyOn(console, "error").mockImplementation();
      
      const error = new Error("Test error");
      handleError(error, "testFunction");
      
      expect(consoleSpy).toHaveBeenCalledWith("Error in testFunction:", error);
      
      consoleSpy.mockRestore();
    });

    it("should handle complex error objects", () => {
      const complexError = {
        message: "Complex error",
        code: 500,
        data: { nested: "value" },
      };
      
      const result = handleError(complexError, "testFunction");
      
      expect(result).toBeInstanceOf(HttpsError);
      expect(result.code).toBe("internal");
      expect(result.message).toBe("Unknown error occurred");
    });
  });
});