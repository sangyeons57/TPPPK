import { UserProfileRepository } from '../../domain/user/userProfile.repository';
import { FriendRepository } from '../../domain/friend/friend.repository';
import { SessionRepository } from '../../domain/auth/session.repository';
import { ProjectRepository } from '../../domain/project/project.repository';
import { ImageRepository } from '../../domain/image/image.repository';

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
  findByUserId: jest.fn(),
  findFriendship: jest.fn(),
  findFriendsByUserId: jest.fn(),
  findFriendRequestsByUserId: jest.fn(),
  findPendingRequestsToUser: jest.fn(),
  save: jest.fn(),
  update: jest.fn(),
  delete: jest.fn(),
  exists: jest.fn(),
});

export const createMockSessionRepository = (): jest.Mocked<SessionRepository> => ({
  findById: jest.fn(),
  findByToken: jest.fn(),
  findByRefreshToken: jest.fn(),
  findActiveSessionsByUserId: jest.fn(),
  save: jest.fn(),
  update: jest.fn(),
  delete: jest.fn(),
  exists: jest.fn(),
  revokeAllByUserId: jest.fn(),
  cleanupExpiredSessions: jest.fn(),
});

export const createMockProjectRepository = (): jest.Mocked<ProjectRepository> => ({
  findById: jest.fn(),
  findByOwnerId: jest.fn(),
  findActiveProjects: jest.fn(),
  save: jest.fn(),
  update: jest.fn(),
  delete: jest.fn(),
  exists: jest.fn(),
});

export const createMockImageRepository = (): jest.Mocked<ImageRepository> => ({
  findById: jest.fn(),
  findByUrl: jest.fn(),
  save: jest.fn(),
  update: jest.fn(),
  delete: jest.fn(),
  exists: jest.fn(),
  findUnprocessedImages: jest.fn(),
});