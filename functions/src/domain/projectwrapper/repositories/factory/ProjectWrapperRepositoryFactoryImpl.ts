import {ProjectWrapperRepositoryFactory} from "./ProjectWrapperRepositoryFactory";
import {ProjectWrapperRepository} from "../projectwrapper.repository";
import {ProjectWrapperRepositoryFactoryContext} from "./ProjectWrapperRepositoryFactoryContext";
import {ProjectWrapperRepositoryImpl} from "../../../../infrastructure/repositories/projectwrapper.repository.impl";
import {FirestoreProjectWrapperDataSource} from "../../../../infrastructure/datasources/firestore/projectwrapper.datasource";

export class ProjectWrapperRepositoryFactoryImpl implements ProjectWrapperRepositoryFactory {
  create(context?: ProjectWrapperRepositoryFactoryContext): ProjectWrapperRepository {
    const datasource = new FirestoreProjectWrapperDataSource();
    return new ProjectWrapperRepositoryImpl(datasource);
  }
}
