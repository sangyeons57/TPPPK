/**
 * Re-export all infrastructure components for easy importing
 */

// Configuration
export { default as firebase, firestore, auth, storage } from './config/firebase';

// Repository implementations
export { AuthRepositoryImpl } from './repositories/AuthRepositoryImpl';
export { UserRepositoryImpl } from './repositories/UserRepositoryImpl';
export { FunctionsRepositoryImpl } from './repositories/FunctionsRepositoryImpl';

// Factory implementations
export * from './factories';