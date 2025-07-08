import { InviteRepository } from '../../domain/invite/repositories/invite.repository';
import { InviteEntity } from '../../domain/invite/entities/invite.entity';
import { InviteDataSource } from '../datasources/interfaces/invite.datasource';
import { CustomResult, Result } from '../../core/types';

export class InviteRepositoryImpl implements InviteRepository {
  constructor(private readonly dataSource: InviteDataSource) {}

  async create(invite: InviteEntity): Promise<CustomResult<InviteEntity>> {
    try {
      const inviteData = await this.dataSource.create(invite.toData());
      return Result.success(InviteEntity.fromData(inviteData));
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to create invite'));
    }
  }

  async findByCode(inviteCode: string): Promise<CustomResult<InviteEntity | null>> {
    try {
      const inviteData = await this.dataSource.findByCode(inviteCode);
      return Result.success(inviteData ? InviteEntity.fromData(inviteData) : null);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to find invite by code'));
    }
  }

  async findById(id: string): Promise<CustomResult<InviteEntity | null>> {
    try {
      const inviteData = await this.dataSource.findById(id);
      return Result.success(inviteData ? InviteEntity.fromData(inviteData) : null);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to find invite by id'));
    }
  }

  async findActiveByProjectId(projectId: string): Promise<CustomResult<InviteEntity[]>> {
    try {
      const inviteDataList = await this.dataSource.findActiveByProjectId(projectId);
      return Result.success(inviteDataList.map(data => InviteEntity.fromData(data)));
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to find active invites'));
    }
  }

  async update(invite: InviteEntity): Promise<CustomResult<InviteEntity>> {
    try {
      const inviteData = await this.dataSource.update(invite.toData());
      return Result.success(InviteEntity.fromData(inviteData));
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to update invite'));
    }
  }

  async delete(id: string): Promise<CustomResult<void>> {
    try {
      await this.dataSource.delete(id);
      return Result.success(undefined);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to delete invite'));
    }
  }

  async existsByCode(inviteCode: string): Promise<CustomResult<boolean>> {
    try {
      const exists = await this.dataSource.existsByCode(inviteCode);
      return Result.success(exists);
    } catch (error) {
      return Result.failure(error instanceof Error ? error : new Error('Failed to check invite existence'));
    }
  }
}