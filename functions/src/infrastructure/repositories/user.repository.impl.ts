import {UserRepository} from "../../domain/user/repositories/user.repository";
import {UserEntity} from "../../domain/user/entities/user.entity";
import {CustomResult, Result} from "../../core/types";
import {DatabaseError} from "../../core/errors";
import {FirestoreUserDataSource} from "../datasources/firestore/user.datasource";

export class UserRepositoryImpl implements UserRepository {
  constructor(private readonly dataSource: FirestoreUserDataSource) {}

  async findById(id: string): Promise<CustomResult<UserEntity | null>> {
    try {
      const result = await this.dataSource.findById(id);
      if (!result.success) {
        return Result.failure(result.error);
      }
      return Result.success(result.data);
    } catch (error) {
      return Result.failure(new DatabaseError("Failed to find user by id", error instanceof Error ? error.message : String(error)));
    }
  }

  async findByUserId(userId: string): Promise<CustomResult<UserEntity | null>> {
    try {
      const result = await this.dataSource.findByUserId(userId);
      if (!result.success) {
        return Result.failure(result.error);
      }
      return Result.success(result.data);
    } catch (error) {
      return Result.failure(new DatabaseError("Failed to find user by userId", error instanceof Error ? error.message : String(error)));
    }
  }

  async findByEmail(email: string): Promise<CustomResult<UserEntity | null>> {
    try {
      const result = await this.dataSource.findByEmail(email);
      if (!result.success) {
        return Result.failure(result.error);
      }
      return Result.success(result.data);
    } catch (error) {
      return Result.failure(new DatabaseError("Failed to find user by email", error instanceof Error ? error.message : String(error)));
    }
  }

  async findByName(name: string): Promise<CustomResult<UserEntity | null>> {
    try {
      const result = await this.dataSource.findByName(name);
      if (!result.success) {
        return Result.failure(result.error);
      }
      return Result.success(result.data);
    } catch (error) {
      return Result.failure(new DatabaseError("Failed to find user by name", error instanceof Error ? error.message : String(error)));
    }
  }

  async save(user: UserEntity): Promise<CustomResult<UserEntity>> {
    try {
      const result = await this.dataSource.save(user);
      if (!result.success) {
        return Result.failure(result.error);
      }
      return Result.success(result.data);
    } catch (error) {
      return Result.failure(new DatabaseError("Failed to save user", error instanceof Error ? error.message : String(error)));
    }
  }

  async update(user: UserEntity): Promise<CustomResult<UserEntity>> {
    try {
      const result = await this.dataSource.update(user);
      if (!result.success) {
        return Result.failure(result.error);
      }
      return Result.success(result.data);
    } catch (error) {
      return Result.failure(new DatabaseError("Failed to update user", error instanceof Error ? error.message : String(error)));
    }
  }

  async delete(id: string): Promise<CustomResult<void>> {
    try {
      const result = await this.dataSource.delete(id);
      if (!result.success) {
        return Result.failure(result.error);
      }
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(new DatabaseError("Failed to delete user", error instanceof Error ? error.message : String(error)));
    }
  }

  async exists(userId: string): Promise<CustomResult<boolean>> {
    try {
      const result = await this.dataSource.exists(userId);
      if (!result.success) {
        return Result.failure(result.error);
      }
      return Result.success(result.data);
    } catch (error) {
      return Result.failure(new DatabaseError("Failed to check user existence", error instanceof Error ? error.message : String(error)));
    }
  }

  async findActiveUsers(limit?: number): Promise<CustomResult<UserEntity[]>> {
    try {
      const result = await this.dataSource.findActiveUsers(limit);
      if (!result.success) {
        return Result.failure(result.error);
      }
      return Result.success(result.data);
    } catch (error) {
      return Result.failure(new DatabaseError("Failed to find active users", error instanceof Error ? error.message : String(error)));
    }
  }

  async removeProjectWrapper(userId: string, projectId: string): Promise<CustomResult<void>> {
    try {
      const result = await this.dataSource.removeProjectWrapper(userId, projectId);
      if (!result.success) {
        return Result.failure(result.error);
      }
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(new DatabaseError("Failed to remove project wrapper", error instanceof Error ? error.message : String(error)));
    }
  }
}