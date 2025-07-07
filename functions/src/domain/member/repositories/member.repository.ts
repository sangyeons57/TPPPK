import { MemberEntity } from '../entities/member.entity';
import { CustomResult } from '../../../core/types';

export interface MemberRepository {
  /**
   * Finds a member by their ID within a project.
   */
  findById(memberId: string): Promise<CustomResult<MemberEntity>>;

  /**
   * Finds a member by user ID within a project.
   */
  findByUserId(userId: string): Promise<CustomResult<MemberEntity>>;

  /**
   * Finds all members in a project.
   */
  findAll(): Promise<CustomResult<MemberEntity[]>>;

  /**
   * Finds all active members in a project.
   */
  findAllActive(): Promise<CustomResult<MemberEntity[]>>;

  /**
   * Saves a member entity.
   */
  save(member: MemberEntity): Promise<CustomResult<MemberEntity>>;

  /**
   * Deletes a member by their ID.
   */
  delete(memberId: string): Promise<CustomResult<void>>;

  /**
   * Deletes a member by user ID.
   */
  deleteByUserId(userId: string): Promise<CustomResult<void>>;

  /**
   * Deletes all members in a project.
   */
  deleteAll(): Promise<CustomResult<void>>;

  /**
   * Checks if a user is a member of the project.
   */
  exists(userId: string): Promise<CustomResult<boolean>>;

  /**
   * Counts the total number of members in a project.
   */
  count(): Promise<CustomResult<number>>;

  /**
   * Counts the number of active members in a project.
   */
  countActive(): Promise<CustomResult<number>>;
}