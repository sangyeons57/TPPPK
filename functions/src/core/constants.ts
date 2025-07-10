export const DATABASE_ID = "(default)" as const;

// Storage Configuration
export const STORAGE_BUCKETS = "teamnovaprojectprojecting.firebasestorage.app" as const;

export const STORAGE_ROOT = {
  USER_PROFILE_ORIGIN: "user_profile_images",
  USER_PROFILE_PROCESSED: "user_profiles",
  PROJECT_PROFILE_ORIGIN: "project_profile_images",
  PROJECT_PROFILE_PROCESSED: "project_profiles",
} as const;

export const STORAGE_METADATA = {
  PROFILE_IMAGE: {
    CONTENT_TYPE: "image/webp",
    CACHE_CONTROL: "public, max-age=31536000", // 1년 캐시
  },
} as const;

// Image Processing Configuration
export const IMAGE_PROCESSING = {
  MAX_SIZE: 5 * 1024 * 1024, // 5MB
  SUPPORTED_FORMATS: ["jpg", "jpeg", "png", "webp"],
  THUMBNAIL_SIZE: 300,
  QUALITY: 80,
} as const;

// Runtime Configuration
export const RUNTIME_CONFIG = {
  REGION: "asia-northeast3" as const,
  MEMORY: "512MiB" as const,
  TIMEOUT_SECONDS: 60 as const,
} as const;

export const FUNCTION_MEMORY = {
  SMALL: "256MiB",
  MEDIUM: "512MiB",
  LARGE: "1GiB",
} as const;

export const FUNCTION_TIMEOUT = {
  STANDARD: 30 as const,
  LONG: 60 as const,
  MAX: 540 as const,
} as const;

// Legacy aliases for backward compatibility
export const FUNCTION_REGION = RUNTIME_CONFIG.REGION;

// Validation Rules
export const VALIDATION_RULES = {
  EMAIL_REGEX: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
  PASSWORD_MIN_LENGTH: 8,
  USERNAME_MIN_LENGTH: 3,
  USERNAME_MAX_LENGTH: 30,
} as const;
