import { validateAuth, validateAuthWithToken, isAdmin, hasRole } from "../auth";
import { AppError } from "../errors";
import { createMockAuth, TEST_USERS } from "../../__tests__/helpers";

describe("Auth Module", () => {
  describe("validateAuth", () => {
    it("should return user ID when authenticated", () => {
      const auth = createMockAuth(TEST_USERS.ALICE);
      const userId = validateAuth(auth);
      expect(userId).toBe(TEST_USERS.ALICE.uid);
    });

    it("should throw AppError when not authenticated", () => {
      expect(() => validateAuth(undefined)).toThrow(AppError);
      expect(() => validateAuth(undefined)).toThrow("User not authenticated");
    });

    it("should throw AppError when auth has no uid", () => {
      const auth = { uid: "", token: {} } as any;
      expect(() => validateAuth(auth)).toThrow(AppError);
    });
  });

  describe("validateAuthWithToken", () => {
    it("should return uid and token when authenticated", () => {
      const auth = createMockAuth(TEST_USERS.ALICE);
      const result = validateAuthWithToken(auth);
      
      expect(result.uid).toBe(TEST_USERS.ALICE.uid);
      expect(result.token).toBeDefined();
    });

    it("should throw AppError when not authenticated", () => {
      expect(() => validateAuthWithToken(undefined)).toThrow(AppError);
    });

    it("should throw AppError when no token", () => {
      const auth = { uid: "test-uid", token: undefined } as any;
      expect(() => validateAuthWithToken(auth)).toThrow("Invalid authentication token");
    });
  });

  describe("isAdmin", () => {
    it("should return true for admin user", () => {
      const auth = createMockAuth(TEST_USERS.ADMIN);
      expect(isAdmin(auth)).toBe(true);
    });

    it("should return false for non-admin user", () => {
      const auth = createMockAuth(TEST_USERS.ALICE);
      expect(isAdmin(auth)).toBe(false);
    });

    it("should return false when not authenticated", () => {
      expect(isAdmin(undefined)).toBe(false);
    });
  });

  describe("hasRole", () => {
    it("should return true when user has role", () => {
      const auth = createMockAuth({
        ...TEST_USERS.ALICE,
        roles: ["moderator", "editor"],
      });
      expect(hasRole(auth, "moderator")).toBe(true);
      expect(hasRole(auth, "editor")).toBe(true);
    });

    it("should return false when user doesn't have role", () => {
      const auth = createMockAuth({
        ...TEST_USERS.ALICE,
        roles: ["viewer"],
      });
      expect(hasRole(auth, "admin")).toBe(false);
    });

    it("should return false when not authenticated", () => {
      expect(hasRole(undefined, "admin")).toBe(false);
    });

    it("should return false when no roles defined", () => {
      const auth = createMockAuth(TEST_USERS.ALICE);
      expect(hasRole(auth, "admin")).toBe(false);
    });
  });
});