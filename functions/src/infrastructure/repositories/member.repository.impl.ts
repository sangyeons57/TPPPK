import { MemberRepository } from '../../domain/member/repositories/member.repository';
import { MemberEntity } from '../../domain/member/entities/member.entity';
import { MemberDataSource } from '../datasources/interfaces/member.datasource';
import { CustomResult, Result } from '../../core/types';

export class MemberRepositoryImpl implements MemberRepository {
  constructor(
    private readonly memberDataSource: MemberDataSource,
    private readonly projectId: string
  ) {}

  async findById(memberId: string): Promise<CustomResult<MemberEntity>> {
    try {
      const result = await this.memberDataSource.findById(this.projectId, memberId);
      
      if (!result.success) {
        return Result.failure(result.error);
      }

      const memberEntity = MemberEntity.fromData(result.data);
      return Result.success(memberEntity);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to find member by ID'));
    }
  }

  async findByUserId(userId: string): Promise<CustomResult<MemberEntity>> {
    try {
      const result = await this.memberDataSource.findByUserId(this.projectId, userId);
      
      if (!result.success) {
        return Result.failure(result.error);
      }

      const memberEntity = MemberEntity.fromData(result.data);
      return Result.success(memberEntity);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to find member by user ID'));
    }
  }

  async findAll(): Promise<CustomResult<MemberEntity[]>> {
    try {
      const result = await this.memberDataSource.findAll(this.projectId);
      
      if (!result.success) {
        return Result.failure(result.error);
      }

      const memberEntities = result.data.map(data => MemberEntity.fromData(data));
      return Result.success(memberEntities);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to find all members'));
    }
  }

  async findAllActive(): Promise<CustomResult<MemberEntity[]>> {
    try {
      const result = await this.memberDataSource.findAllActive(this.projectId);
      
      if (!result.success) {
        return Result.failure(result.error);
      }

      const memberEntities = result.data.map(data => MemberEntity.fromData(data));
      return Result.success(memberEntities);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to find active members'));
    }
  }

  async save(member: MemberEntity): Promise<CustomResult<MemberEntity>> {
    try {
      const memberData = member.toData();
      const result = await this.memberDataSource.save(this.projectId, memberData);
      
      if (!result.success) {
        return Result.failure(result.error);
      }

      const savedMemberEntity = MemberEntity.fromData(result.data);
      return Result.success(savedMemberEntity);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to save member'));
    }
  }

  async delete(memberId: string): Promise<CustomResult<void>> {
    try {
      const result = await this.memberDataSource.delete(this.projectId, memberId);
      
      if (!result.success) {
        return Result.failure(result.error);
      }

      return Result.success(undefined);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to delete member'));
    }
  }

  async deleteByUserId(userId: string): Promise<CustomResult<void>> {
    try {
      const result = await this.memberDataSource.deleteByUserId(this.projectId, userId);
      
      if (!result.success) {
        return Result.failure(result.error);
      }

      return Result.success(undefined);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to delete member by user ID'));
    }
  }

  async deleteAll(): Promise<CustomResult<void>> {
    try {
      const result = await this.memberDataSource.deleteAll(this.projectId);
      
      if (!result.success) {
        return Result.failure(result.error);
      }

      return Result.success(undefined);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to delete all members'));
    }
  }

  async exists(userId: string): Promise<CustomResult<boolean>> {
    try {
      const result = await this.memberDataSource.exists(this.projectId, userId);
      
      if (!result.success) {
        return Result.failure(result.error);
      }

      return Result.success(result.data);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to check member existence'));
    }
  }

  async count(): Promise<CustomResult<number>> {
    try {
      const result = await this.memberDataSource.count(this.projectId);
      
      if (!result.success) {
        return Result.failure(result.error);
      }

      return Result.success(result.data);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to count members'));
    }
  }

  async countActive(): Promise<CustomResult<number>> {
    try {
      const result = await this.memberDataSource.countActive(this.projectId);
      
      if (!result.success) {
        return Result.failure(result.error);
      }

      return Result.success(result.data);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to count active members'));
    }
  }
}