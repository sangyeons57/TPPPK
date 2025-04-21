package com.example.teamnovapersonalprojectprojectingkotlin.domain.model
// SignUpViewModel.kt 파일 상단 또는 domain/model 등에 정의

enum class SignUpFormFocusTarget {
    EMAIL, // 이메일 입력 필드
    PASSWORD, // 비밀번호 입력 필드
    PASSWORD_CONFIRM, // 비밀번호 확인 입력 필드
    NAME // 이름 입력 필드
}