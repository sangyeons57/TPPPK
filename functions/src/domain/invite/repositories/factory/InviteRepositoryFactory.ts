import { InviteRepository } from '../invite.repository';
import { InviteRepositoryFactoryContext } from './InviteRepositoryFactoryContext';

export interface InviteRepositoryFactory {
  create(context?: InviteRepositoryFactoryContext): InviteRepository;
}