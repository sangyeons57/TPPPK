import { RepositoryFactory } from '../../../shared/RepositoryFactory';
import { MemberRepository } from '../member.repository';
import { MemberRepositoryFactoryContext } from './MemberRepositoryFactoryContext';

export type MemberRepositoryFactory = RepositoryFactory<MemberRepository, MemberRepositoryFactoryContext>;