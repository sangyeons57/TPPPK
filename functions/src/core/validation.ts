import {ValidationError} from "./errors";
import {VALIDATION_RULES} from "./constants";

// Type aliases for better type safety
export type UserId = string;
export type FriendId = string;
export type ProjectId = string;

// Email validation
export function validateEmail(email: string): void {
  if (!VALIDATION_RULES.EMAIL_REGEX.test(email)) {
    throw new ValidationError("email", "Invalid email format");
  }
}

export function isValidEmail(email: string): boolean {
  return VALIDATION_RULES.EMAIL_REGEX.test(email);
}

// Username validation
export function validateUsername(name: string): void {
  if (name.length < VALIDATION_RULES.USERNAME_MIN_LENGTH) {
    throw new ValidationError("name", `Username must be at least ${VALIDATION_RULES.USERNAME_MIN_LENGTH} characters`);
  }
  if (name.length > VALIDATION_RULES.USERNAME_MAX_LENGTH) {
    throw new ValidationError("name", `Username must be at most ${VALIDATION_RULES.USERNAME_MAX_LENGTH} characters`);
  }
}

export function isValidUsername(name: string): boolean {
  return name.length >= VALIDATION_RULES.USERNAME_MIN_LENGTH && 
         name.length <= VALIDATION_RULES.USERNAME_MAX_LENGTH;
}

// Project name validation
export function validateProjectName(name: string): void {
  if (name.length < 3) {
    throw new ValidationError("projectName", "Project name must be at least 3 characters");
  }
  if (name.length > 100) {
    throw new ValidationError("projectName", "Project name must be at most 100 characters");
  }
}

export function isValidProjectName(name: string): boolean {
  return name.length >= 3 && name.length <= 100;
}

// Image URL validation
export function validateImageUrl(url: string): void {
  if (!url.startsWith("https://")) {
    throw new ValidationError("imageUrl", "Image must be a valid HTTPS URL");
  }
}

export function isValidImageUrl(url: string): boolean {
  return url.startsWith("https://");
}

// User memo validation
export function validateUserMemo(memo: string): void {
  if (memo.length > 500) {
    throw new ValidationError("memo", "User memo must be at most 500 characters");
  }
}

export function isValidUserMemo(memo: string): boolean {
  return memo.length <= 500;
}

// Project description validation
export function validateProjectDescription(description: string): void {
  if (description.length > 1000) {
    throw new ValidationError("projectDescription", "Project description must be at most 1000 characters");
  }
}

export function isValidProjectDescription(description: string): boolean {
  return description.length <= 1000;
}

// FCM token validation
export function validateFcmToken(token: string): void {
  if (token.trim().length === 0) {
    throw new ValidationError("fcmToken", "FCM token cannot be empty");
  }
}

export function isValidFcmToken(token: string): boolean {
  return token.trim().length > 0;
}

// ID validation (for FriendId, UserId, etc.)
export function validateId(id: string, fieldName: string): void {
  if (!id || id.trim().length === 0) {
    throw new ValidationError(fieldName, `${fieldName} cannot be empty`);
  }
}

export function isValidId(id: string): boolean {
  return !!(id && id.trim().length > 0);
}