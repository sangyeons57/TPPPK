/**
 * Firebase Functions entry point
 * Exports all callable functions using the new modular architecture
 */

// System functions
export { helloWorld } from './functions/system/helloWorldFunction';

// Authentication functions
export { signUp } from './functions/auth/signUpFunction';
export { session } from './functions/auth/sessionFunction';
