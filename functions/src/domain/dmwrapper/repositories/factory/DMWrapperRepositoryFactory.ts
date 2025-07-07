import {RepositoryFactory} from "../../../shared/RepositoryFactory";
import {DMWrapperRepositoryFactoryContext} from "./DMWrapperRepositoryFactoryContext";
import {DMWrapperRepository} from "../dmwrapper.repository";
import {DMWrapperRepositoryImpl} from "../../../../infrastructure/repositories/dmwrapper.repository.impl";
import {FirestoreDMWrapperDataSource} from "../../../../infrastructure/datasources/firestore/dmwrapper.datasource";

/**
 * Factory for creating DM wrapper repositories
 */
export class DMWrapperRepositoryFactory implements RepositoryFactory<DMWrapperRepository, DMWrapperRepositoryFactoryContext> {
  /**
   * Creates a DM wrapper repository instance
   * @param {DMWrapperRepositoryFactoryContext} [context] - Optional context for DM wrapper repository creation
   * @return {DMWrapperRepository} DMWrapperRepository instance
   */
  create(context?: DMWrapperRepositoryFactoryContext): DMWrapperRepository {
    const dataSource = new FirestoreDMWrapperDataSource();
    return new DMWrapperRepositoryImpl(dataSource);
  }
}