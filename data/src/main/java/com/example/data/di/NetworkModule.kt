package com.example.data.di

import com.example.core_logging.SentryUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 네트워크 관련 의존성을 제공하는 Hilt 모듈
 * 
 * Retrofit, OkHttp 클라이언트 및 관련 설정을 제공합니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Sentry 네트워크 요청 추적을 위한 인터셉터
     * 
     * 모든 네트워크 요청을 Sentry에 기록합니다.
     */
    class SentryNetworkInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val requestUrl = request.url.toString()
            val requestMethod = request.method
            
            // 요청 시작 기록
            SentryUtil.addBreadcrumb("network", "API 요청: $requestMethod $requestUrl")
            
            // 요청에 태그 추가
            SentryUtil.setCustomTag("network.url", requestUrl)
            SentryUtil.setCustomTag("network.method", requestMethod)
            
            try {
                val response = chain.proceed(request)
                
                // 응답 기록
                val statusCode = response.code
                SentryUtil.addBreadcrumb("network", "API 응답: $statusCode - $requestMethod $requestUrl")
                SentryUtil.setCustomTag("network.status_code", statusCode.toString())
                
                return response
            } catch (e: Exception) {
                // 실패한 요청 기록
                SentryUtil.captureError(
                    throwable = e,
                    message = "API 요청 실패: $requestMethod $requestUrl",
                    tags = mapOf(
                        "network.url" to requestUrl,
                        "network.method" to requestMethod
                    )
                )
                throw e
            }
        }
    }
    
    /**
     * Sentry 네트워크 인터셉터 제공
     */
    @Provides
    @Singleton
    fun provideSentryInterceptor(): SentryNetworkInterceptor {
        return SentryNetworkInterceptor()
    }

    /**
     * OkHttpClient 제공
     * 
     * 타임아웃 및 Sentry 인터셉터가 설정된 HTTP 클라이언트를 제공합니다.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(sentryInterceptor: SentryNetworkInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(sentryInterceptor)
            .build()
    }

    /**
     * Retrofit 인스턴스 제공
     * 
     * REST API 호출을 위한 Retrofit 인스턴스를 제공합니다.
     * 기본 URL은 필요에 따라 수정하세요.
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.example.com/") // 실제 API 서버 URL로 변경 필요
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
} 