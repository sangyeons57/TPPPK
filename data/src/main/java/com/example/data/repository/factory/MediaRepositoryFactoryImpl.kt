package com.example.data.repository.factory

import android.content.Context
import com.example.data.repository.base.MediaRepositoryImpl
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.MediaRepository
import com.example.domain.repository.factory.context.MediaRepositoryFactoryContext
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MediaRepositoryFactoryImpl @Inject constructor(
    private val firebaseStorage: FirebaseStorage,
    @ApplicationContext private val context: Context
) : RepositoryFactory<MediaRepositoryFactoryContext, MediaRepository> {

    override fun create(input: MediaRepositoryFactoryContext): MediaRepository {
        return MediaRepositoryImpl(
            firebaseStorage = firebaseStorage,
            context = context
        )
    }
}
