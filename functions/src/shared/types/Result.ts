/**
 * Result wrapper type for consistent error handling across the Firebase Functions.
 * Inspired by the Android CustomResult implementation.
 */

export abstract class Result<TSuccess, TError> {
  abstract readonly isSuccess: boolean;
  abstract readonly isFailure: boolean;
  abstract readonly isLoading: boolean;
  abstract readonly isInitial: boolean;
  abstract readonly isProgress: boolean;

  static success<TSuccess, TError>(data: TSuccess): Result<TSuccess, TError> {
    return new Success(data);
  }

  static failure<TSuccess, TError>(error: TError): Result<TSuccess, TError> {
    return new Failure(error);
  }

  static loading<TSuccess, TError>(): Result<TSuccess, TError> {
    return new Loading();
  }

  static initial<TSuccess, TError>(): Result<TSuccess, TError> {
    return new Initial();
  }

  static progress<TSuccess, TError>(progress?: number): Result<TSuccess, TError> {
    return new Progress(progress);
  }

  onSuccess(callback: (data: TSuccess) => void): Result<TSuccess, TError> {
    if (this.isSuccess) {
      callback((this as Success<TSuccess, TError>).data);
    }
    return this;
  }

  onFailure(callback: (error: TError) => void): Result<TSuccess, TError> {
    if (this.isFailure) {
      callback((this as Failure<TSuccess, TError>).error);
    }
    return this;
  }

  map<TNewSuccess>(
    transform: (data: TSuccess) => TNewSuccess
  ): Result<TNewSuccess, TError> {
    if (this.isSuccess) {
      return Result.success(transform((this as Success<TSuccess, TError>).data));
    }
    if (this.isFailure) {
      return Result.failure((this as Failure<TSuccess, TError>).error);
    }
    if (this.isLoading) {
      return Result.loading();
    }
    if (this.isProgress) {
      return Result.progress((this as Progress<TSuccess, TError>).progress);
    }
    return Result.initial();
  }

  mapError<TNewError>(
    transform: (error: TError) => TNewError
  ): Result<TSuccess, TNewError> {
    if (this.isSuccess) {
      return Result.success((this as Success<TSuccess, TError>).data);
    }
    if (this.isFailure) {
      return Result.failure(transform((this as Failure<TSuccess, TError>).error));
    }
    if (this.isLoading) {
      return Result.loading();
    }
    if (this.isProgress) {
      return Result.progress((this as Progress<TSuccess, TError>).progress);
    }
    return Result.initial();
  }

  async flatMap<TNewSuccess>(
    transform: (data: TSuccess) => Promise<Result<TNewSuccess, TError>>
  ): Promise<Result<TNewSuccess, TError>> {
    if (this.isSuccess) {
      return await transform((this as Success<TSuccess, TError>).data);
    }
    if (this.isFailure) {
      return Result.failure((this as Failure<TSuccess, TError>).error);
    }
    if (this.isLoading) {
      return Result.loading();
    }
    if (this.isProgress) {
      return Result.progress((this as Progress<TSuccess, TError>).progress);
    }
    return Result.initial();
  }

  getOrNull(): TSuccess | null {
    return this.isSuccess ? (this as Success<TSuccess, TError>).data : null;
  }

  getOrDefault(defaultValue: TSuccess): TSuccess {
    return this.isSuccess ? (this as Success<TSuccess, TError>).data : defaultValue;
  }

  getOrThrow(): TSuccess {
    if (this.isSuccess) {
      return (this as Success<TSuccess, TError>).data;
    }
    if (this.isFailure) {
      const error = (this as Failure<TSuccess, TError>).error;
      throw error instanceof Error ? error : new Error(String(error));
    }
    throw new Error('Result is not in success state');
  }
}

export class Success<TSuccess, TError> extends Result<TSuccess, TError> {
  readonly isSuccess = true;
  readonly isFailure = false;
  readonly isLoading = false;
  readonly isInitial = false;
  readonly isProgress = false;

  constructor(public readonly data: TSuccess) {
    super();
  }

  toString(): string {
    return `Success(${this.data})`;
  }
}

export class Failure<TSuccess, TError> extends Result<TSuccess, TError> {
  readonly isSuccess = false;
  readonly isFailure = true;
  readonly isLoading = false;
  readonly isInitial = false;
  readonly isProgress = false;

  constructor(public readonly error: TError) {
    super();
  }

  toString(): string {
    return `Failure(${this.error})`;
  }
}

export class Loading<TSuccess, TError> extends Result<TSuccess, TError> {
  readonly isSuccess = false;
  readonly isFailure = false;
  readonly isLoading = true;
  readonly isInitial = false;
  readonly isProgress = false;

  toString(): string {
    return 'Loading';
  }
}

export class Initial<TSuccess, TError> extends Result<TSuccess, TError> {
  readonly isSuccess = false;
  readonly isFailure = false;
  readonly isLoading = false;
  readonly isInitial = true;
  readonly isProgress = false;

  toString(): string {
    return 'Initial';
  }
}

export class Progress<TSuccess, TError> extends Result<TSuccess, TError> {
  readonly isSuccess = false;
  readonly isFailure = false;
  readonly isLoading = false;
  readonly isInitial = false;
  readonly isProgress = true;

  constructor(public readonly progress?: number) {
    super();
  }

  toString(): string {
    return `Progress(${this.progress ?? 'unknown'})`;
  }
}