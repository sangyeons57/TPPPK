import { Email, Username, UserProfileEntity } from '../../domain/user/user.entity';
import { ValidationError } from '../../core/errors';

describe('User Domain Entities', () => {
  describe('Email Value Object', () => {
    it('should create valid email', () => {
      const email = new Email('test@example.com');
      expect(email.value).toBe('test@example.com');
    });

    it('should throw ValidationError for invalid email', () => {
      expect(() => new Email('invalid-email')).toThrow(ValidationError);
    });

    it('should check equality correctly', () => {
      const email1 = new Email('test@example.com');
      const email2 = new Email('test@example.com');
      const email3 = new Email('other@example.com');

      expect(email1.equals(email2)).toBe(true);
      expect(email1.equals(email3)).toBe(false);
    });
  });

  describe('Username Value Object', () => {
    it('should create valid username', () => {
      const username = new Username('testuser');
      expect(username.value).toBe('testuser');
    });

    it('should throw ValidationError for short username', () => {
      expect(() => new Username('ab')).toThrow(ValidationError);
    });

    it('should throw ValidationError for long username', () => {
      const longUsername = 'a'.repeat(31);
      expect(() => new Username(longUsername)).toThrow(ValidationError);
    });
  });

  describe('UserProfileEntity', () => {
    it('should create user profile entity', () => {
      const email = new Email('test@example.com');
      const username = new Username('testuser');
      const now = new Date();

      const userProfile = new UserProfileEntity(
        'profile1',
        'user1',
        username,
        email,
        now,
        now,
        true
      );

      expect(userProfile.id).toBe('profile1');
      expect(userProfile.userId).toBe('user1');
      expect(userProfile.username.value).toBe('testuser');
      expect(userProfile.email.value).toBe('test@example.com');
      expect(userProfile.isActive).toBe(true);
    });

    it('should update profile correctly', () => {
      const email = new Email('test@example.com');
      const username = new Username('testuser');
      const newUsername = new Username('newuser');
      const now = new Date();

      const userProfile = new UserProfileEntity(
        'profile1',
        'user1',
        username,
        email,
        now,
        now,
        true
      );

      const updatedProfile = userProfile.updateProfile({
        username: newUsername,
        bio: 'New bio'
      });

      expect(updatedProfile.username.value).toBe('newuser');
      expect(updatedProfile.bio).toBe('New bio');
      expect(updatedProfile.email).toBe(email);
    });

    it('should deactivate profile correctly', () => {
      const email = new Email('test@example.com');
      const username = new Username('testuser');
      const now = new Date();

      const userProfile = new UserProfileEntity(
        'profile1',
        'user1',
        username,
        email,
        now,
        now,
        true
      );

      const deactivatedProfile = userProfile.deactivate();
      expect(deactivatedProfile.isActive).toBe(false);
    });
  });
});