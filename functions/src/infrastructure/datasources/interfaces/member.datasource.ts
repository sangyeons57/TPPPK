import { MemberData } from '../../../domain/member/entities/member.entity';
import { CustomResult } from '../../../core/types';

export interface MemberDataSource {
  /**
   * Finds a member by their ID within a project.
   */
  findById(projectId: string, memberId: string): Promise<CustomResult<MemberData>>;

  /**
   * Finds a member by user ID within a project.
   */
  findByUserId(projectId: string, userId: string): Promise<CustomResult<MemberData>>;

  /**
   * Finds all members in a project.
   */
  findAll(projectId: string): Promise<CustomResult<MemberData[]>>;

  /**
   * Finds all active members in a project.
   */
  findAllActive(projectId: string): Promise<CustomResult<MemberData[]>>;

  /**
   * Saves a member.
   */
  save(projectId: string, member: MemberData): Promise<CustomResult<MemberData>>;

  /**
   * Deletes a member by their ID.
   */
  delete(projectId: string, memberId: string): Promise<CustomResult<void>>;

  /**
   * Deletes a member by user ID.
   */
  deleteByUserId(projectId: string, userId: string): Promise<CustomResult<void>>;

  /**
   * Deletes all members in a project.
   */
  deleteAll(projectId: string): Promise<CustomResult<void>>;

  /**
   * Checks if a user is a member of the project.
   */
  exists(projectId: string, userId: string): Promise<CustomResult<boolean>>;

  /**
   * Counts the total number of members in a project.
   */
  count(projectId: string): Promise<CustomResult<number>>;

  /**
   * Counts the number of active members in a project.
   */
  countActive(projectId: string): Promise<CustomResult<number>>;
}