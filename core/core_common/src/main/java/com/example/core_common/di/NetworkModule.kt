package com.example.core_common.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.core_common.cache.ChatImageCache
import com.example.core_common.cache.GlobalImageUrlCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 네트워크 관련 의존성 주입을 위한 Hilt 모듈
 * HTTP 캐시 및 이미지 로딩 최적화 설정
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * HTTP 캐시가 활성화된 OkHttpClient 제공
     * 이미지 및 API 응답의 HTTP 레벨 캐싱을 위함
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context
    ): OkHttpClient {
        // HTTP 캐시 디렉토리 (30MB - cost optimized)
        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDir, 30L * 1024L * 1024L) // 30MB

        return OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Cache-Control 헤더를 존중하는 인터셉터
            .addNetworkInterceptor { chain ->
                val originalResponse = chain.proceed(chain.request())
                
                // Firebase Storage 이미지에 대해서는 Cache-Control 헤더가 설정되어 있으므로
                // 그대로 사용하되, 헤더가 없는 경우 기본 캐시 정책 적용
                if (originalResponse.header("Cache-Control").isNullOrEmpty()) {
                    originalResponse.newBuilder()
                        .header("Cache-Control", "public, max-age=3600") // 1시간 기본 캐시 (개선된 캐싱)
                        .build()
                } else {
                    originalResponse
                }
            }
            .build()
    }

    /**
     * HTTP 캐시가 활성화된 Coil ImageLoader 제공
     * Firebase Storage 이미지의 효율적인 캐싱을 위함
     */
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.15) // 15% of available memory (memory optimized)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, "image_cache"))
                    .maxSizeBytes(100L * 1024L * 1024L) // 100MB (enhanced caching for better performance)
                    .build()
            }
            .respectCacheHeaders(true) // HTTP Cache-Control 헤더 존중
            // 디버그 로깅은 필요시에만 활성화
            .build()
    }

    /**
     * 글로벌 이미지 URL 캐시 제공
     * Firebase Storage URL을 앱 전체에서 공유하여 중복 호출 방지
     */
    @Provides
    @Singleton
    fun provideGlobalImageUrlCache(): GlobalImageUrlCache {
        return GlobalImageUrlCache()
    }

    /**
     * 채팅 이미지 캐시 제공
     * 채팅에서 주고받는 이미지들의 효율적인 캐싱 및 썸네일 생성
     */
    @Provides
    @Singleton
    fun provideChatImageCache(
        @ApplicationContext context: Context,
        imageLoader: ImageLoader
    ): ChatImageCache {
        return ChatImageCache(context, imageLoader)
    }

}