import { InviteData } from '../../../domain/invite/entities/invite.entity';

export interface InviteDataSource {
  /**
   * Creates a new invite.
   */
  create(invite: InviteData): Promise<InviteData>;

  /**
   * Finds an invite by its unique code.
   */
  findByCode(inviteCode: string): Promise<InviteData | null>;

  /**
   * Finds an invite by its ID.
   */
  findById(id: string): Promise<InviteData | null>;

  /**
   * Finds all active invites for a project.
   */
  findActiveByProjectId(projectId: string): Promise<InviteData[]>;

  /**
   * Updates an existing invite.
   */
  update(invite: InviteData): Promise<InviteData>;

  /**
   * Deletes an invite by its ID.
   */
  delete(id: string): Promise<void>;

  /**
   * Checks if an invite code already exists.
   */
  existsByCode(inviteCode: string): Promise<boolean>;
}