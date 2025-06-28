export const FUNCTION_REGION = "asia-northeast3" as const;

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