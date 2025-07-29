package com.example.domain.provider

import com.example.domain.repository.local.LocalMessageAttachmentRepository
import com.example.domain.repository.remote.FileRepository
import com.example.domain.usecase.local.file.CheckFileExistenceLocalUseCase
import com.example.domain.usecase.local.file.CheckFileExistenceLocalUseCaseImpl
import com.example.domain.usecase.local.file.DeleteFileLocalUseCase
import com.example.domain.usecase.local.file.DeleteFileLocalUseCaseImpl
import com.example.domain.usecase.local.file.UploadFileLocalUseCase
import com.example.domain.usecase.local.file.UploadFileLocalUseCaseImpl
import com.example.domain.usecase.local.file.UploadMessageAttachmentLocalUseCase
import com.example.domain.usecase.local.file.UploadMessageAttachmentLocalUseCaseImpl
import com.example.domain.usecase.local.file.ValidateFileLocalUseCase
import com.example.domain.usecase.local.file.ValidateFileLocalUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 파일 관리 관련 Local UseCase들을 제공하는 Provider
 * 
 * 로컬 저장소를 기반으로 한 파일 업로드, 다운로드, 검증 등의 기능을 담당합니다.
 */
@Singleton
class FileUseCaseProvider @Inject constructor(
    private val fileLocalRepository: FileRepository,
    private val messageAttachmentLocalRepository: LocalMessageAttachmentRepository
) {

    /**
     * 파일 기본 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 파일 기본 관리 UseCase 그룹
     */
    fun createBasicUseCases(): FileLocalBasicUseCases {
        return FileLocalBasicUseCases(
            // 파일 존재 확인
            checkFileExistenceLocalUseCase = CheckFileExistenceLocalUseCaseImpl(
                fileLocalRepository = fileLocalRepository
            ),
            
            // 파일 업로드/삭제
            uploadFileLocalUseCase = UploadFileLocalUseCaseImpl(
                fileLocalRepository = fileLocalRepository
            ),
            
            deleteFileLocalUseCase = DeleteFileLocalUseCaseImpl(
                fileLocalRepository = fileLocalRepository
            ),
            
            // 파일 검증
            validateFileLocalUseCase = ValidateFileLocalUseCaseImpl(
                fileLocalRepository = fileLocalRepository
            ),
            
            fileLocalRepository = fileLocalRepository
        )
    }

    /**
     * 메시지 첨부파일 관련 UseCase들을 생성합니다.
     * 
     * @return 메시지 첨부파일 관리 UseCase 그룹
     */
    fun createMessageAttachmentUseCases(): FileLocalMessageAttachmentUseCases {
        return FileLocalMessageAttachmentUseCases(
            // 메시지 첨부파일 업로드
            uploadMessageAttachmentLocalUseCase = UploadMessageAttachmentLocalUseCaseImpl(
                messageAttachmentLocalRepository = messageAttachmentLocalRepository,
                validateFileLocalUseCase = ValidateFileLocalUseCaseImpl(
                    fileLocalRepository = fileLocalRepository
                )
            ),
            
            // 파일 검증 (첨부파일용)
            validateFileLocalUseCase = ValidateFileLocalUseCaseImpl(
                fileLocalRepository = fileLocalRepository
            ),
            
            messageAttachmentLocalRepository = messageAttachmentLocalRepository,
            fileLocalRepository = fileLocalRepository
        )
    }
}

/**
 * 파일 기본 관리 Local UseCase 그룹
 */
data class FileLocalBasicUseCases(
    // 파일 존재 확인
    val checkFileExistenceLocalUseCase: CheckFileExistenceLocalUseCase,
    
    // 파일 업로드/삭제
    val uploadFileLocalUseCase: UploadFileLocalUseCase,
    val deleteFileLocalUseCase: DeleteFileLocalUseCase,
    
    // 파일 검증
    val validateFileLocalUseCase: ValidateFileLocalUseCase,

    val fileLocalRepository: FileRepository
)

/**
 * 메시지 첨부파일 관리 Local UseCase 그룹
 */
data class FileLocalMessageAttachmentUseCases(
    // 메시지 첨부파일 업로드
    val uploadMessageAttachmentLocalUseCase: UploadMessageAttachmentLocalUseCase,
    
    // 파일 검증
    val validateFileLocalUseCase: ValidateFileLocalUseCase,

    val messageAttachmentLocalRepository: LocalMessageAttachmentRepository,
    val fileLocalRepository: FileRepository
) 