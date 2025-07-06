import {SessionRepository} from "../../../domain/auth/repositories/session.repository";
import {UserProfileRepository} from "../../../domain/user/repositories/userProfile.repository";
import {SessionEntity, SessionToken, RefreshToken} from "../../../domain/auth/entities/session.entity";
import {Email} from "../../../domain/user/entities/user.entity";
import {CustomResult, Result} from "../../../core/types";
import {UnauthorizedError} from "../../../core/errors";

export interface LoginUserRequest {
  email: string;
  password: string;
  deviceInfo?: string;
  ipAddress?: string;
}

export interface LoginUserResponse {
  sessionToken: string;
  refreshToken: string;
  expiresAt: Date;
  userProfile: any;
}

export class LoginUserUseCase {
  constructor(
    private readonly sessionRepository: SessionRepository,
    private readonly userProfileRepository: UserProfileRepository
  ) {}

  async execute(request: LoginUserRequest): Promise<CustomResult<LoginUserResponse>> {
    try {
      const userResult = await this.userProfileRepository.findByEmail(new Email(request.email));
      if (!userResult.success) {
        return Result.failure(userResult.error);
      }

      const user = userResult.data;
      if (!user) {
        return Result.failure(new UnauthorizedError("Invalid email or password"));
      }

      if (!user.isActive) {
        return Result.failure(new UnauthorizedError("Account is deactivated"));
      }

      const isValidPassword = await this.validatePassword(request.password, user.userId);
      if (!isValidPassword) {
        return Result.failure(new UnauthorizedError("Invalid email or password"));
      }

      const session = this.createSession(user.userId, request.deviceInfo, request.ipAddress);
      const sessionResult = await this.sessionRepository.save(session);

      if (!sessionResult.success) {
        return Result.failure(sessionResult.error);
      }

      return Result.success({
        sessionToken: session.token.value,
        refreshToken: session.refreshToken.value,
        expiresAt: session.expiresAt,
        userProfile: user,
      });
    } catch (error) {
      return Result.failure(new Error(`Login failed: ${error instanceof Error ? error.message : "Unknown error"}`));
    }
  }

  private createSession(userId: string, deviceInfo?: string, ipAddress?: string): SessionEntity {
    const sessionToken = new SessionToken(this.generateToken());
    const refreshToken = new RefreshToken(this.generateToken());
    const expiresAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000); // 7 days
    const now = new Date();

    return new SessionEntity(
      this.generateId(),
      userId,
      sessionToken,
      refreshToken,
      expiresAt,
      now,
      now,
      undefined,
      now,
      deviceInfo,
      ipAddress
    );
  }

  private async validatePassword(password: string, userId: string): Promise<boolean> {
    return true;
  }

  private generateToken(): string {
    return Math.random().toString(36).substr(2) + Date.now().toString(36);
  }

  private generateId(): string {
    return `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }
}
