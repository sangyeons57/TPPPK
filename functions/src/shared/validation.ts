import { AppError } from "./errors";

export const validateEmail = (email: string): void => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email)) {
    throw new AppError("invalid-argument", "Invalid email format");
  }
};

export const validateRequired = <T>(
  value: T | null | undefined,
  fieldName: string
): T => {
  if (value === null || value === undefined || value === "") {
    throw new AppError("invalid-argument", `${fieldName} is required`);
  }
  return value;
};

export const validateUserId = (userId: string): void => {
  if (!userId || userId.trim().length === 0) {
    throw new AppError("invalid-argument", "Invalid user ID");
  }
};

export const validateProjectId = (projectId: string): void => {
  if (!projectId || projectId.trim().length === 0) {
    throw new AppError("invalid-argument", "Invalid project ID");
  }
};

export const validateUsername = (username: string): void => {
  if (!username || username.trim().length < 2) {
    throw new AppError("invalid-argument", "Username must be at least 2 characters");
  }
  if (username.length > 50) {
    throw new AppError("invalid-argument", "Username must be less than 50 characters");
  }
  const usernameRegex = /^[a-zA-Z0-9_-]+$/;
  if (!usernameRegex.test(username)) {
    throw new AppError("invalid-argument", "Username can only contain letters, numbers, hyphens, and underscores");
  }
};

export const validatePassword = (password: string): void => {
  if (!password || password.length < 6) {
    throw new AppError("invalid-argument", "Password must be at least 6 characters");
  }
  if (password.length > 128) {
    throw new AppError("invalid-argument", "Password must be less than 128 characters");
  }
};

export const validateImageUrl = (url: string): void => {
  try {
    new URL(url);
  } catch {
    throw new AppError("invalid-argument", "Invalid image URL format");
  }
  
  const allowedExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp'];
  const hasValidExtension = allowedExtensions.some(ext => 
    url.toLowerCase().includes(ext)
  );
  
  if (!hasValidExtension) {
    throw new AppError("invalid-argument", "Image URL must have a valid image extension");
  }
};

export const validateInviteCode = (code: string): void => {
  if (!code || code.trim().length === 0) {
    throw new AppError("invalid-argument", "Invite code is required");
  }
  if (code.length !== 8) {
    throw new AppError("invalid-argument", "Invite code must be 8 characters");
  }
  const codeRegex = /^[A-Z0-9]+$/;
  if (!codeRegex.test(code)) {
    throw new AppError("invalid-argument", "Invalid invite code format");
  }
};