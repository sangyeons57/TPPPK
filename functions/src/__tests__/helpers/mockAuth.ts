import { CallableRequest } from "firebase-functions/v2/https";

export interface MockAuthUser {
  uid: string;
  email?: string;
  name?: string;
  roles?: string[];
  admin?: boolean;
}

export const createMockAuth = (user: MockAuthUser): CallableRequest["auth"] => {
  return {
    uid: user.uid,
    token: {
      email: user.email,
      name: user.name,
      roles: user.roles || [],
      admin: user.admin || false,
      aud: "test-audience",
      auth_time: Math.floor(Date.now() / 1000),
      exp: Math.floor(Date.now() / 1000) + 3600,
      firebase: {
        identities: {},
        sign_in_provider: "custom",
      },
      iat: Math.floor(Date.now() / 1000),
      iss: "test-issuer",
      sub: user.uid,
    } as any,
  };
};

export const createMockRequest = <T = any>(
  data: T,
  auth?: MockAuthUser,
  headers?: Record<string, string>
): CallableRequest<T> => {
  return {
    data,
    auth: auth ? createMockAuth(auth) : undefined,
    acceptsStreaming: false,
    rawRequest: {
      headers: {
        "x-request-id": `test-${Date.now()}`,
        ...headers,
      },
    } as any,
  };
};

// Test users
export const TEST_USERS = {
  ALICE: {
    uid: "alice-123",
    email: "alice@test.com",
    name: "Alice Test",
  },
  BOB: {
    uid: "bob-456",
    email: "bob@test.com", 
    name: "Bob Test",
  },
  CHARLIE: {
    uid: "charlie-789",
    email: "charlie@test.com",
    name: "Charlie Test",
  },
  ADMIN: {
    uid: "admin-999",
    email: "admin@test.com",
    name: "Admin User",
    admin: true,
    roles: ["admin"],
  },
  UNAUTHENTICATED: undefined,
};