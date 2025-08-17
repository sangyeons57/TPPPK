import { CallableRequest } from "firebase-functions/v2/https";
import { AppError } from "./errors";

export const validateAuth = (auth: CallableRequest["auth"]): string => {
  if (!auth?.uid) {
    throw new AppError("unauthenticated", "User not authenticated");
  }
  return auth.uid;
};

export const validateAuthWithToken = (auth: CallableRequest["auth"]): { uid: string; token: any } => {
  if (!auth?.uid) {
    throw new AppError("unauthenticated", "User not authenticated");
  }
  if (!auth.token) {
    throw new AppError("unauthenticated", "Invalid authentication token");
  }
  return { uid: auth.uid, token: auth.token };
};

export const isAdmin = (auth: CallableRequest["auth"]): boolean => {
  return auth?.token?.admin === true;
};

export const hasRole = (auth: CallableRequest["auth"], role: string): boolean => {
  return auth?.token?.roles?.includes(role) === true;
};