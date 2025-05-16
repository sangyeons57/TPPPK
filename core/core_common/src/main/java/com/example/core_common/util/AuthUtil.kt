package com.example.core_common.util

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/**
 * 인증 관련 유틸리티 기능을 제공하는 클래스입니다.
 * 앱 전체에서 현재 사용자 ID 접근 등의 인증 관련 공통 기능을 제공합니다.
 */
object AuthUtil {
    private val FirebaseAuthInstance: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * 현재 로그인된 사용자의 ID를 반환합니다.
     * 로그인되지 않은 경우 빈 문자열을 반환합니다.
     * 
     * @return 현재 사용자 ID 또는 빈 문자열
     */
    fun getCurrentUserId(): String {
        return FirebaseAuthInstance.currentUser?.uid ?: ""
    }
    
    /**
     * 현재 로그인된 사용자 객체를 반환합니다.
     * 로그인되지 않은 경우 null을 반환합니다.
     * 
     * @return 현재 FirebaseUser 객체 또는 null
     */
    fun getCurrentUser(): FirebaseUser? {
        return FirebaseAuthInstance.currentUser
    }
    
    /**
     * 사용자가 로그인 상태인지 확인합니다.
     * 
     * @return 로그인 상태면 true, 아니면 false
     */
    fun isUserLoggedIn(): Boolean {
        return FirebaseAuthInstance.currentUser != null
    }

    /**
     * 현재 사용자의 이메일을 반환합니다.
     * 로그인되지 않았거나 이메일이 없는 경우 null을 반환합니다.
     * 
     * @return 현재 사용자 이메일 또는 null
     */
    fun getCurrentUserEmail(): String? {
        return FirebaseAuthInstance.currentUser?.email
    }
    
    /**
     * 현재 사용자의 표시 이름을 반환합니다.
     * 로그인되지 않았거나 이름이 없는 경우 null을 반환합니다.
     * 
     * @return 현재 사용자 표시 이름 또는 null
     */
    fun getCurrentUserDisplayName(): String? {
        return FirebaseAuthInstance.currentUser?.displayName
    }

    /**
     * FirebaseAuth 인스턴스를 반환합니다.
     * 
     * @return FirebaseAuth 인스턴스
     */
    fun getFirebaseAuth(): FirebaseAuth {
        return FirebaseAuthInstance
    }
} 