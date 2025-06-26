/**
 * Application constants for Firebase Functions
 */

export const APP_CONSTANTS = {
  // Authentication
  AUTH: {
    TOKEN_EXPIRATION: 3600, // 1 hour in seconds
    REFRESH_TOKEN_EXPIRATION: 2592000, // 30 days in seconds
    MAX_LOGIN_ATTEMPTS: 5,
    LOCKOUT_DURATION: 900, // 15 minutes in seconds
    PASSWORD_RESET_TOKEN_EXPIRATION: 1800, // 30 minutes in seconds
  },

  // Validation
  VALIDATION: {
    EMAIL_MAX_LENGTH: 254,
    PASSWORD_MIN_LENGTH: 8,
    PASSWORD_MAX_LENGTH: 128,
    USERNAME_MIN_LENGTH: 2,
    USERNAME_MAX_LENGTH: 50,
    NAME_MAX_LENGTH: 100,
    DESCRIPTION_MAX_LENGTH: 1000,
  },

  // Pagination
  PAGINATION: {
    DEFAULT_PAGE_SIZE: 20,
    MAX_PAGE_SIZE: 100,
    DEFAULT_PAGE: 1,
  },

  // Rate limiting
  RATE_LIMIT: {
    DEFAULT_MAX_REQUESTS: 100,
    DEFAULT_WINDOW_MS: 900000, // 15 minutes
    AUTH_MAX_REQUESTS: 10,
    AUTH_WINDOW_MS: 300000, // 5 minutes
  },

  // Database
  DATABASE: {
    TRANSACTION_TIMEOUT: 10000, // 10 seconds
    MAX_RETRY_ATTEMPTS: 3,
    BATCH_SIZE: 500,
  },

  // File upload
  FILE_UPLOAD: {
    MAX_FILE_SIZE: 10485760, // 10MB
    ALLOWED_IMAGE_TYPES: ['image/jpeg', 'image/png', 'image/gif', 'image/webp'],
    ALLOWED_DOCUMENT_TYPES: ['application/pdf', 'text/plain', 'application/msword'],
    MAX_FILES_PER_REQUEST: 5,
  },

  // Collections
  COLLECTIONS: {
    USERS: 'users',
    PROJECTS: 'projects',
    CATEGORIES: 'categories',
    CHANNELS: 'channels',
    MESSAGES: 'messages',
    INVITES: 'invites',
    ROLES: 'roles',
    PERMISSIONS: 'permissions',
    SCHEDULES: 'schedules',
    FRIENDS: 'friends',
    DM_CHANNELS: 'dmChannels',
    MESSAGE_ATTACHMENTS: 'messageAttachments',
  },

  // Project settings
  PROJECT: {
    NAME_MIN_LENGTH: 1,
    NAME_MAX_LENGTH: 100,
    DESCRIPTION_MAX_LENGTH: 500,
    MAX_MEMBERS: 1000,
    MAX_CHANNELS: 100,
    MAX_CATEGORIES: 50,
    MAX_ROLES: 20,
  },

  // User settings
  USER: {
    NAME_MIN_LENGTH: 1,
    NAME_MAX_LENGTH: 50,
    MEMO_MAX_LENGTH: 200,
    MAX_FRIENDS: 500,
    PROFILE_IMAGE_MAX_SIZE: 5242880, // 5MB
  },

  // Message settings
  MESSAGE: {
    CONTENT_MAX_LENGTH: 2000,
    MAX_ATTACHMENTS: 10,
    EDIT_TIME_LIMIT: 3600000, // 1 hour in milliseconds
    DELETE_TIME_LIMIT: 86400000, // 24 hours in milliseconds
  },

  // Error codes
  ERROR_CODES: {
    // Authentication errors
    AUTH_INVALID_CREDENTIALS: 'auth/invalid-credentials',
    AUTH_USER_NOT_FOUND: 'auth/user-not-found',
    AUTH_EMAIL_ALREADY_EXISTS: 'auth/email-already-exists',
    AUTH_WEAK_PASSWORD: 'auth/weak-password',
    AUTH_TOO_MANY_REQUESTS: 'auth/too-many-requests',
    AUTH_TOKEN_EXPIRED: 'auth/token-expired',
    AUTH_INSUFFICIENT_PERMISSION: 'auth/insufficient-permission',

    // Validation errors
    VALIDATION_REQUIRED_FIELD: 'validation/required-field',
    VALIDATION_INVALID_FORMAT: 'validation/invalid-format',
    VALIDATION_OUT_OF_RANGE: 'validation/out-of-range',
    VALIDATION_DUPLICATE_VALUE: 'validation/duplicate-value',

    // Business logic errors
    BUSINESS_USER_ALREADY_MEMBER: 'business/user-already-member',
    BUSINESS_PROJECT_NOT_FOUND: 'business/project-not-found',
    BUSINESS_INSUFFICIENT_PERMISSION: 'business/insufficient-permission',
    BUSINESS_RESOURCE_LIMIT_EXCEEDED: 'business/resource-limit-exceeded',

    // System errors
    SYSTEM_DATABASE_ERROR: 'system/database-error',
    SYSTEM_EXTERNAL_SERVICE_ERROR: 'system/external-service-error',
    SYSTEM_TIMEOUT: 'system/timeout',
    SYSTEM_INTERNAL_ERROR: 'system/internal-error',
  },

  // HTTP status codes
  HTTP_STATUS: {
    OK: 200,
    CREATED: 201,
    NO_CONTENT: 204,
    BAD_REQUEST: 400,
    UNAUTHORIZED: 401,
    FORBIDDEN: 403,
    NOT_FOUND: 404,
    CONFLICT: 409,
    UNPROCESSABLE_ENTITY: 422,
    TOO_MANY_REQUESTS: 429,
    INTERNAL_SERVER_ERROR: 500,
    SERVICE_UNAVAILABLE: 503,
  },

  // Logging
  LOGGING: {
    LEVELS: {
      DEBUG: 'debug',
      INFO: 'info',
      WARN: 'warn',
      ERROR: 'error',
    },
    MAX_LOG_SIZE: 1024, // 1KB
  },
} as const;

// Export individual constants for convenience
export const { AUTH, VALIDATION, PAGINATION, RATE_LIMIT, DATABASE, FILE_UPLOAD, COLLECTIONS, PROJECT, USER, MESSAGE, ERROR_CODES, HTTP_STATUS, LOGGING } = APP_CONSTANTS;