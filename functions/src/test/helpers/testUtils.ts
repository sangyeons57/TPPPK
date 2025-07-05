import { Result, CustomResult } from '../../core/types';

export class TestUtils {
  static createSuccessResult<T>(data: T): CustomResult<T, Error> {
    return Result.success(data);
  }

  static createFailureResult<E>(error: E): CustomResult<never, E> {
    return Result.failure(error);
  }

  static expectSuccess<T, E>(result: CustomResult<T, E>): T {
    if (!result.success) {
      throw new Error(`Expected success but got failure: ${JSON.stringify((result as any).error)}`);
    }
    return result.data;
  }

  static expectFailure<T, E>(result: CustomResult<T, E>): E {
    if (result.success) {
      throw new Error(`Expected failure but got success: ${JSON.stringify(result.data)}`);
    }
    return (result as any).error;
  }

  static mockDateNow(date: Date): jest.SpyInstance {
    return jest.spyOn(Date, 'now').mockReturnValue(date.getTime());
  }

  static mockConsoleError(): jest.SpyInstance {
    return jest.spyOn(console, 'error').mockImplementation(() => {});
  }

  static restoreAllMocks(): void {
    jest.restoreAllMocks();
  }

  static createMockDate(isoString: string = '2023-01-01T00:00:00.000Z'): Date {
    return new Date(isoString);
  }

  static createFutureDate(hoursFromNow: number = 24): Date {
    return new Date(Date.now() + hoursFromNow * 60 * 60 * 1000);
  }

  static createPastDate(hoursAgo: number = 24): Date {
    return new Date(Date.now() - hoursAgo * 60 * 60 * 1000);
  }
}

export const delay = (ms: number): Promise<void> => new Promise(resolve => setTimeout(resolve, ms));

export const generateRandomId = (prefix = 'test'): string => {
  return `${prefix}_${Math.random().toString(36).substr(2, 9)}`;
};

export const generateRandomEmail = (): string => {
  return `test_${Math.random().toString(36).substr(2, 9)}@example.com`;
};