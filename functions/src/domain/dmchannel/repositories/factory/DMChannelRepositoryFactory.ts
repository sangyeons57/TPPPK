import {RepositoryFactory} from "../../../shared/RepositoryFactory";
import {DMChannelRepositoryFactoryContext} from "./DMChannelRepositoryFactoryContext";
import {DMChannelRepository} from "../dmchannel.repository";
import {DMChannelRepositoryImpl} from "../../../../infrastructure/repositories/dmchannel.repository.impl";
import {FirestoreDMChannelDataSource} from "../../../../infrastructure/datasources/firestore/dmchannel.datasource";

/**
 * Factory for creating DM channel repositories
 */
export class DMChannelRepositoryFactory implements RepositoryFactory<DMChannelRepository, DMChannelRepositoryFactoryContext> {
  /**
   * Creates a DM channel repository instance
   * @param {DMChannelRepositoryFactoryContext} [context] - Optional context for DM channel repository creation
   * @return {DMChannelRepository} DMChannelRepository instance
   */
  create(context?: DMChannelRepositoryFactoryContext): DMChannelRepository {
    const dataSource = new FirestoreDMChannelDataSource();
    return new DMChannelRepositoryImpl(dataSource);
  }
}