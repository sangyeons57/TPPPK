package com.example.data.datasource.remote

import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.example.data.model.remote.RoleDTO
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

interface RoleRemoteDataSource : DefaultDatasource {

}

@Singleton
class RoleRemoteDataSourceImpl @Inject constructor(
    val firestore: FirebaseFirestore,
) : DefaultDatasourceImpl<RoleDTO>(firestore, RoleDTO::class.java), RoleRemoteDataSource {

}

