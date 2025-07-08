import { InviteEntity } from '../entities/invite.entity';
import { CustomResult } from '../../../core/types';

export interface InviteRepository {
  /**
   * Creates a new invite.
   */
  create(invite: InviteEntity): Promise<CustomResult<InviteEntity>>;

  /**
   * Finds an invite by its unique code.
   */
  findByCode(inviteCode: string): Promise<CustomResult<InviteEntity | null>>;

  /**
   * Finds an invite by its ID.
   */
  findById(id: string): Promise<CustomResult<InviteEntity | null>>;

  /**
   * Finds all active invites for a project.
   */
  findActiveByProjectId(projectId: string): Promise<CustomResult<InviteEntity[]>>;

  /**
   * Updates an existing invite.
   */
  update(invite: InviteEntity): Promise<CustomResult<InviteEntity>>;

  /**
   * Deletes an invite by its ID.
   */
  delete(id: string): Promise<CustomResult<void>>;

  /**
   * Checks if an invite code already exists.
   */
  existsByCode(inviteCode: string): Promise<CustomResult<boolean>>;
}