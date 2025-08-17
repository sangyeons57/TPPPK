import {
  validateEmail,
  validateRequired,
  validateUserId,
  validateProjectId,
  validateUsername,
  validatePassword,
  validateImageUrl,
  validateInviteCode,
} from "../validation";
import { AppError } from "../errors";

describe("Validation Module", () => {
  describe("validateEmail", () => {
    it("should pass for valid emails", () => {
      const validEmails = [
        "test@example.com",
        "user.name@domain.co.uk",
        "123@test.org",
      ];
      
      validEmails.forEach(email => {
        expect(() => validateEmail(email)).not.toThrow();
      });
    });

    it("should throw AppError for invalid emails", () => {
      const invalidEmails = [
        "invalid",
        "@example.com",
        "test@",
        "test.example.com",
        "",
      ];

      invalidEmails.forEach(email => {
        expect(() => validateEmail(email)).toThrow(AppError);
        expect(() => validateEmail(email)).toThrow("Invalid email format");
      });
    });
  });

  describe("validateRequired", () => {
    it("should return value when provided", () => {
      expect(validateRequired("test", "field")).toBe("test");
      expect(validateRequired(123, "number")).toBe(123);
      expect(validateRequired(false, "boolean")).toBe(false);
    });

    it("should throw AppError for null/undefined/empty values", () => {
      const invalidValues = [null, undefined, ""];
      
      invalidValues.forEach(value => {
        expect(() => validateRequired(value, "testField")).toThrow(AppError);
        expect(() => validateRequired(value, "testField")).toThrow("testField is required");
      });
    });
  });

  describe("validateUserId", () => {
    it("should pass for valid user IDs", () => {
      const validIds = ["user123", "user-456", "user_789"];
      
      validIds.forEach(id => {
        expect(() => validateUserId(id)).not.toThrow();
      });
    });

    it("should throw AppError for invalid user IDs", () => {
      const invalidIds = ["", "   ", null as any, undefined as any];
      
      invalidIds.forEach(id => {
        expect(() => validateUserId(id)).toThrow(AppError);
        expect(() => validateUserId(id)).toThrow("Invalid user ID");
      });
    });
  });

  describe("validateProjectId", () => {
    it("should pass for valid project IDs", () => {
      const validIds = ["project123", "project-456", "project_789"];
      
      validIds.forEach(id => {
        expect(() => validateProjectId(id)).not.toThrow();
      });
    });

    it("should throw AppError for invalid project IDs", () => {
      const invalidIds = ["", "   ", null as any, undefined as any];
      
      invalidIds.forEach(id => {
        expect(() => validateProjectId(id)).toThrow(AppError);
        expect(() => validateProjectId(id)).toThrow("Invalid project ID");
      });
    });
  });

  describe("validateUsername", () => {
    it("should pass for valid usernames", () => {
      const validUsernames = ["user", "user123", "user-name", "user_name", "ab"];
      
      validUsernames.forEach(username => {
        expect(() => validateUsername(username)).not.toThrow();
      });
    });

    it("should throw AppError for too short usernames", () => {
      const shortUsernames = ["", "a"];
      
      shortUsernames.forEach(username => {
        expect(() => validateUsername(username)).toThrow("Username must be at least 2 characters");
      });
    });

    it("should throw AppError for too long usernames", () => {
      const longUsername = "a".repeat(51);
      expect(() => validateUsername(longUsername)).toThrow("Username must be less than 50 characters");
    });

    it("should throw AppError for invalid characters", () => {
      const invalidUsernames = ["user@name", "user name", "user!", "user#"];
      
      invalidUsernames.forEach(username => {
        expect(() => validateUsername(username)).toThrow("Username can only contain letters, numbers, hyphens, and underscores");
      });
    });
  });

  describe("validatePassword", () => {
    it("should pass for valid passwords", () => {
      const validPasswords = ["password123", "secure@pass", "a".repeat(6)];
      
      validPasswords.forEach(password => {
        expect(() => validatePassword(password)).not.toThrow();
      });
    });

    it("should throw AppError for too short passwords", () => {
      const shortPasswords = ["", "12345"];
      
      shortPasswords.forEach(password => {
        expect(() => validatePassword(password)).toThrow("Password must be at least 6 characters");
      });
    });

    it("should throw AppError for too long passwords", () => {
      const longPassword = "a".repeat(129);
      expect(() => validatePassword(longPassword)).toThrow("Password must be less than 128 characters");
    });
  });

  describe("validateImageUrl", () => {
    it("should pass for valid image URLs", () => {
      const validUrls = [
        "https://example.com/image.jpg",
        "http://test.com/photo.png",
        "https://storage.googleapis.com/bucket/image.webp",
        "https://example.com/path/image.gif",
      ];
      
      validUrls.forEach(url => {
        expect(() => validateImageUrl(url)).not.toThrow();
      });
    });

    it("should throw AppError for invalid URLs", () => {
      const invalidUrls = ["not-a-url", ""];
      
      invalidUrls.forEach(url => {
        expect(() => validateImageUrl(url)).toThrow("Invalid image URL format");
      });
    });

    it("should throw AppError for non-image URLs", () => {
      const nonImageUrls = [
        "https://example.com/document.pdf",
        "https://example.com/video.mp4",
        "https://example.com/file.txt",
      ];
      
      nonImageUrls.forEach(url => {
        expect(() => validateImageUrl(url)).toThrow("Image URL must have a valid image extension");
      });
    });
  });

  describe("validateInviteCode", () => {
    it("should pass for valid invite codes", () => {
      const validCodes = ["ABCD1234", "TEST5678", "INVITE99"];
      
      validCodes.forEach(code => {
        expect(() => validateInviteCode(code)).not.toThrow();
      });
    });

    it("should throw AppError for empty codes", () => {
      const emptyCodes = ["", "   ", null as any, undefined as any];
      
      emptyCodes.forEach(code => {
        expect(() => validateInviteCode(code)).toThrow("Invite code is required");
      });
    });

    it("should throw AppError for wrong length codes", () => {
      const wrongLengthCodes = ["ABC123", "TOOLONGCODE"];
      
      wrongLengthCodes.forEach(code => {
        expect(() => validateInviteCode(code)).toThrow("Invite code must be 8 characters");
      });
    });

    it("should throw AppError for invalid format codes", () => {
      const invalidCodes = ["abcd1234", "TEST@123", "test-123"];
      
      invalidCodes.forEach(code => {
        expect(() => validateInviteCode(code)).toThrow("Invalid invite code format");
      });
    });
  });
});