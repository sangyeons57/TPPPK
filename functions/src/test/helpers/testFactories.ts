import { UserProfileEntity, Email, Username } from '../../domain/user/entities/user.entity';
import { FriendEntity, FriendStatus, UserId, FriendId } from '../../domain/friend/entities/friend.entity';
import { SessionEntity, SessionToken, RefreshToken, SessionStatus } from '../../domain/auth/entities/session.entity';
import { ProjectEntity, ProjectName, ProjectDescription, ProjectImage, ProjectStatus } from '../../domain/project/entities/project.entity';

export class TestFactories {
  static createUserProfile(overrides: Partial<{
    id: string;
    userId: string;
    username: string;
    email: string;
    bio?: string;
    profileImageUrl?: string;
    isActive: boolean;
    canReceiveFriendRequests: boolean;
  }> = {}): UserProfileEntity {
    const defaults = {
      id: 'profile_123',
      userId: 'user_123',
      username: 'testuser',
      email: 'test@example.com',
      isActive: true,
      canReceiveFriendRequests: true,
    };
    
    const data = { ...defaults, ...overrides };
    
    return new UserProfileEntity(
      data.id,
      data.userId,
      new Username(data.username),
      new Email(data.email),
      new Date('2023-01-01T00:00:00Z'),
      new Date('2023-01-01T00:00:00Z'),
      data.isActive,
      undefined, // profileImage
      data.bio,
      undefined, // displayName
      undefined, // friendCount
      data.canReceiveFriendRequests
    );
  }

  static createFriend(overrides: Partial<{
    id: string;
    userId: string;
    friendUserId: string;
    status: FriendStatus;
    requestedAt: Date;
    respondedAt?: Date;
  }> = {}): FriendEntity {
    const defaults = {
      id: 'friend_123',
      userId: 'user_123',
      friendUserId: 'user_456',
      status: FriendStatus.ACCEPTED,
      requestedAt: new Date('2023-01-01T00:00:00Z'),
      respondedAt: new Date('2023-01-02T00:00:00Z'),
    };
    
    const data = { ...defaults, ...overrides };
    
    return new FriendEntity(
      new FriendId(data.id),
      new UserId(data.userId),
      new UserId(data.friendUserId),
      data.status,
      data.requestedAt,
      data.respondedAt,
      new Date('2023-01-01T00:00:00Z'),
      new Date('2023-01-02T00:00:00Z')
    );
  }

  static createSession(overrides: Partial<{
    id: string;
    userId: string;
    token: string;
    refreshToken: string;
    expiresAt: Date;
    status: SessionStatus;
    deviceInfo?: string;
    ipAddress?: string;
  }> = {}): SessionEntity {
    const defaults = {
      id: 'session_123',
      userId: 'user_123',
      token: 'session_token_1234567890',
      refreshToken: 'refresh_token_1234567890',
      expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000), // 24 hours from now
      status: SessionStatus.ACTIVE,
      deviceInfo: 'Mozilla/5.0',
      ipAddress: '192.168.1.1',
    };
    
    const data = { ...defaults, ...overrides };
    
    return new SessionEntity(
      data.id,
      data.userId,
      new SessionToken(data.token),
      new RefreshToken(data.refreshToken),
      data.expiresAt,
      new Date('2023-01-01T00:00:00Z'),
      new Date('2023-01-01T00:00:00Z'),
      data.status,
      new Date('2023-01-01T00:00:00Z'),
      data.deviceInfo,
      data.ipAddress
    );
  }

  static createProject(overrides: Partial<{
    id: string;
    name: string;
    ownerId: string;
    description?: string;
    image?: string;
    status: ProjectStatus;
    memberCount: number;
  }> = {}): ProjectEntity {
    const defaults = {
      id: 'project_123',
      name: 'Test Project',
      ownerId: 'user_123',
      status: ProjectStatus.ACTIVE,
      memberCount: 1,
    };
    
    const data = { ...defaults, ...overrides };
    
    return new ProjectEntity(
      data.id,
      new ProjectName(data.name),
      data.ownerId,
      new Date('2023-01-01T00:00:00Z'),
      new Date('2023-01-01T00:00:00Z'),
      data.status,
      data.memberCount,
      data.description ? new ProjectDescription(data.description) : undefined,
      data.image ? new ProjectImage(data.image) : undefined
    );
  }
}