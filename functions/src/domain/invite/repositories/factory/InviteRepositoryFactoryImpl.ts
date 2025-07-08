import { InviteRepositoryFactory } from './InviteRepositoryFactory';
import { InviteRepositoryFactoryContext } from './InviteRepositoryFactoryContext';
import { InviteRepository } from '../invite.repository';
import { InviteRepositoryImpl } from '../../../../infrastructure/repositories/invite.repository.impl';
import { FirestoreInviteDataSource } from '../../../../infrastructure/datasources/firestore/invite.datasource';

export class InviteRepositoryFactoryImpl implements InviteRepositoryFactory {
  create(context?: InviteRepositoryFactoryContext): InviteRepository {
    const dataSource = new FirestoreInviteDataSource();
    return new InviteRepositoryImpl(dataSource);
  }
}