import {RepositoryFactory} from "../../../shared/RepositoryFactory";
import {FriendRepositoryFactoryContext} from "./FriendRepositoryFactoryContext";
import {FriendRepository} from "../friend.repository";
import {FriendRepositoryImpl} from "../../../../infrastructure/repositories/friend.repository.impl";
import {FirestoreFriendDataSource} from "../../../../infrastructure/datasources/firestore/friend.datasource";

/**
 * Factory for creating friend repositories
 */
export class FriendRepositoryFactory implements RepositoryFactory<FriendRepository, FriendRepositoryFactoryContext> {
  /**
   * Creates a friend repository instance
   * @param {FriendRepositoryFactoryContext} [context] - Optional context for friend repository creation
   * @return {FriendRepository} FriendRepository instance
   */
  create(context?: FriendRepositoryFactoryContext): FriendRepository {
    const dataSource = new FirestoreFriendDataSource();
    return new FriendRepositoryImpl(dataSource);
  }
}
