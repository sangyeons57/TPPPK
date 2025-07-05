export interface BaseEntity {
  id: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface ValueObject<T> {
  readonly value: T;
  equals(other: ValueObject<T>): boolean;
}

export type CustomResult<T, E = Error> = {
  success: true;
  data: T;
} | {
  success: false;
  error: E;
};

export class Result {
  static success<T>(data: T): CustomResult<T> {
    return {success: true, data};
  }

  static failure<T, E = Error>(error: E): CustomResult<T, E> {
    return {success: false, error};
  }
}
