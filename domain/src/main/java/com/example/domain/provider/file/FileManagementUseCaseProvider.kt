package com.example.domain.provider.file

import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.FileRepository
import com.example.domain.repository.base.MediaRepository
import com.example.domain.repository.factory.context.FileRepositoryFactoryContext
import com.example.domain.repository.factory.context.MediaRepositoryFactoryContext
import com.example.domain.usecase.file.DeleteFileUseCase
import com.example.domain.usecase.file.DeleteFileUseCaseImpl
import com.example.domain.usecase.file.DownloadFileUseCase
import com.example.domain.usecase.file.DownloadFileUseCaseImpl
import com.example.domain.usecase.file.GetFileUrlUseCase
import com.example.domain.usecase.file.GetFileUrlUseCaseImpl
import com.example.domain.usecase.file.UploadFileUseCase
import com.example.domain.usecase.file.UploadFileUseCaseImpl
import com.example.domain.usecase.media.DeleteMediaUseCase
import com.example.domain.usecase.media.DeleteMediaUseCaseImpl
import com.example.domain.usecase.media.UploadMediaUseCase
import com.example.domain.usecase.media.UploadMediaUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FileManagementUseCaseProvider
 *
 * 파일 및 미디어 관련 UseCase들을 제공하는 Provider
 * 파일 업로드, 다운로드, 삭제 등의 파일 관리 기능을 제공합니다.
 */
@Singleton
class FileManagementUseCaseProvider @Inject constructor(
    private val fileRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<FileRepositoryFactoryContext, FileRepository>,
    private val mediaRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<MediaRepositoryFactoryContext, MediaRepository>
) {

    /**
     * 파일 관리 관련 UseCase들을 생성합니다.
     */
    fun create(): FileManagementUseCases {
        val fileRepository = fileRepositoryFactory.create(FileRepositoryFactoryContext())
        val mediaRepository = mediaRepositoryFactory.create(MediaRepositoryFactoryContext())

        return FileManagementUseCases(
            uploadFileUseCase = UploadFileUseCaseImpl(fileRepository),
            deleteFileUseCase = DeleteFileUseCaseImpl(fileRepository),
            getFileUrlUseCase = GetFileUrlUseCaseImpl(fileRepository),
            downloadFileUseCase = DownloadFileUseCaseImpl(fileRepository),
            uploadMediaUseCase = UploadMediaUseCaseImpl(mediaRepository),
            deleteMediaUseCase = DeleteMediaUseCaseImpl(mediaRepository),
            fileRepository = fileRepository,
            mediaRepository = mediaRepository
        )
    }
}

/**
 * 파일 관리 관련 UseCase들을 그룹화한 데이터 클래스
 */
data class FileManagementUseCases(
    val uploadFileUseCase: UploadFileUseCase,
    val deleteFileUseCase: DeleteFileUseCase,
    val getFileUrlUseCase: GetFileUrlUseCase,
    val downloadFileUseCase: DownloadFileUseCase,
    val uploadMediaUseCase: UploadMediaUseCase,
    val deleteMediaUseCase: DeleteMediaUseCase,
    val fileRepository: FileRepository,
    val mediaRepository: MediaRepository
)