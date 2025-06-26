/**
 * User account status enumeration
 */

export enum UserAccountStatus {
  ACTIVE = 'ACTIVE',
  SUSPENDED = 'SUSPENDED',
  WITHDRAWN = 'WITHDRAWN',
  PENDING_VERIFICATION = 'PENDING_VERIFICATION',
  LOCKED = 'LOCKED',
}

export const UserAccountStatusUtils = {
  isActive: (status: UserAccountStatus): boolean => status === UserAccountStatus.ACTIVE,
  isSuspended: (status: UserAccountStatus): boolean => status === UserAccountStatus.SUSPENDED,
  isWithdrawn: (status: UserAccountStatus): boolean => status === UserAccountStatus.WITHDRAWN,
  isPendingVerification: (status: UserAccountStatus): boolean => status === UserAccountStatus.PENDING_VERIFICATION,
  isLocked: (status: UserAccountStatus): boolean => status === UserAccountStatus.LOCKED,
  canLogin: (status: UserAccountStatus): boolean => status === UserAccountStatus.ACTIVE,
  getAllStatuses: (): UserAccountStatus[] => Object.values(UserAccountStatus),
  getDisplayName: (status: UserAccountStatus): string => {
    switch (status) {
      case UserAccountStatus.ACTIVE:
        return 'Active';
      case UserAccountStatus.SUSPENDED:
        return 'Suspended';
      case UserAccountStatus.WITHDRAWN:
        return 'Withdrawn';
      case UserAccountStatus.PENDING_VERIFICATION:
        return 'Pending Verification';
      case UserAccountStatus.LOCKED:
        return 'Locked';
      default:
        return 'Unknown';
    }
  },
};