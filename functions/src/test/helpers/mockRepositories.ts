import { UserProfileRepository } from '../../domain/user/repositories/userProfile.repository';
import { FriendRepository } from '../../domain/friend/repositories/friend.repository';
import { SessionRepository } from '../../domain/auth/repositories/session.repository';
import { ProjectRepository } from '../../domain/project/repositories/project.repository';
import { ImageRepository } from '../../core/services/imageProcessing.service';

export const createMockUserProfileRepository = (): jest.Mocked<UserProfileRepository> => ({
  findById: jest.fn(),
  findByUserId: jest.fn(),
  findByEmail: jest.fn(),
  findByUsername: jest.fn(),
  save: jest.fn(),
  update: jest.fn(),
  delete: jest.fn(),
  exists: jest.fn(),
  findActiveProfiles: jest.fn(),
});

export const createMockFriendRepository = (): jest.Mocked<FriendRepository> => ({
  findById: jest.fn(),
  findByUserIds: jest.fn(),
  findFriendsByUserId: jest.fn(),
  findReceivedFriendRequests: jest.fn(),
  findSentFriendRequests: jest.fn(),
  areUsersFriends: jest.fn(),
  friendRequestExists: jest.fn(),
  save: jest.fn(),
  update: jest.fn(),
  delete: jest.fn(),
  deleteByUserIds: jest.fn(),
  deleteAllByUserId: jest.fn(),
  findByCriteria: jest.fn(),
  countFriendsByUserId: jest.fn(),
  countPendingRequestsByUserId: jest.fn(),
});

export const createMockSessionRepository = (): jest.Mocked<SessionRepository> => ({
  findById: jest.fn(),
  findByToken: jest.fn(),
  findByRefreshToken: jest.fn(),
  findByUserId: jest.fn(),
  findActiveSessionsByUserId: jest.fn(),
  findByStatus: jest.fn(),
  save: jest.fn(),
  update: jest.fn(),
  delete: jest.fn(),
  deleteByUserId: jest.fn(),
  exists: jest.fn(),
  revokeAllUserSessions: jest.fn(),
  cleanupExpiredSessions: jest.fn(),
});

export const createMockProjectRepository = (): jest.Mocked<ProjectRepository> => ({
  findById: jest.fn(),
  findByOwnerId: jest.fn(),
  findByName: jest.fn(),
  findByStatus: jest.fn(),
  save: jest.fn(),
  update: jest.fn(),
  delete: jest.fn(),
  exists: jest.fn(),
  findActiveProjects: jest.fn(),
  findProjectsByMemberId: jest.fn(),
  updateMemberCount: jest.fn(),
});

export const createMockImageRepository = (): jest.Mocked<ImageRepository> => ({
  findById: jest.fn(),
  findByOwnerId: jest.fn(),
  findByType: jest.fn(),
  findByOwnerIdAndType: jest.fn(),
  save: jest.fn(),
  update: jest.fn(),
  delete: jest.fn(),
  exists: jest.fn(),
  deleteByUrl: jest.fn(),
});