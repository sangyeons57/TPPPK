package com.example.domain_usecase.provider.file

import com.example.domain_repository.base.FileRepository
import com.example.domain_repository.base.MediaRepository
import com.example.domain_usecase.usecase.file.DeleteFileUseCase
import com.example.domain_usecase.usecase.file.DeleteFileUseCaseImpl
import com.example.domain_usecase.usecase.file.DownloadFileUseCase
import com.example.domain_usecase.usecase.file.DownloadFileUseCaseImpl
import com.example.domain_usecase.usecase.file.GetFileUrlUseCase
import com.example.domain_usecase.usecase.file.GetFileUrlUseCaseImpl
import com.example.domain_usecase.usecase.file.UploadFileUseCase
import com.example.domain_usecase.usecase.file.UploadFileUseCaseImpl
import com.example.domain_usecase.usecase.media.DeleteMediaUseCase
import com.example.domain_usecase.usecase.media.DeleteMediaUseCaseImpl
import com.example.domain_usecase.usecase.media.UploadMediaUseCase
import com.example.domain_usecase.usecase.media.UploadMediaUseCaseImpl
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
    private val fileRepository: FileRepository,
    private val mediaRepository: MediaRepository
) {

    /**
     * 파일 관리 관련 UseCase들을 생성합니다.
     */
    fun create(): FileManagementUseCases {
        return FileManagementUseCases(
            uploadFileUseCase = UploadFileUseCaseImpl(this.fileRepository),
            deleteFileUseCase = DeleteFileUseCaseImpl(this.fileRepository),
            getFileUrlUseCase = GetFileUrlUseCaseImpl(this.fileRepository),
            downloadFileUseCase = DownloadFileUseCaseImpl(this.fileRepository),
            uploadMediaUseCase = UploadMediaUseCaseImpl(this.mediaRepository),
            deleteMediaUseCase = DeleteMediaUseCaseImpl(this.mediaRepository),
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
)