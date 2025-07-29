package com.example.domain.provider

import com.example.domain.repository.remote.MediaRepository
import com.example.domain.usecase.local.media.DeleteMediaLocalUseCase
import com.example.domain.usecase.local.media.DeleteMediaLocalUseCaseImpl
import com.example.domain.usecase.local.media.UploadMediaLocalUseCase
import com.example.domain.usecase.local.media.UploadMediaLocalUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 미디어 관리 관련 Local UseCase들을 제공하는 Provider
 * 
 * 로컬 저장소를 기반으로 한 미디어 파일 업로드, 삭제 등의 기능을 담당합니다.
 */
@Singleton
class MediaUseCaseProvider @Inject constructor(
    private val mediaLocalRepository: MediaRepository
) {

    /**
     * 미디어 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 미디어 관리 UseCase 그룹
     */
    fun create(): MediaLocalUseCases {
        return MediaLocalUseCases(
            // 미디어 업로드
            uploadMediaLocalUseCase = UploadMediaLocalUseCaseImpl(
                mediaLocalRepository = mediaLocalRepository
            ),
            
            // 미디어 삭제
            deleteMediaLocalUseCase = DeleteMediaLocalUseCaseImpl(
                mediaLocalRepository = mediaLocalRepository
            ),
            
            mediaLocalRepository = mediaLocalRepository
        )
    }
}

/**
 * 미디어 관리 Local UseCase 그룹
 */
data class MediaLocalUseCases(
    // 미디어 업로드
    val uploadMediaLocalUseCase: UploadMediaLocalUseCase,
    
    // 미디어 삭제
    val deleteMediaLocalUseCase: DeleteMediaLocalUseCase,

    val mediaLocalRepository: MediaRepository
) 