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