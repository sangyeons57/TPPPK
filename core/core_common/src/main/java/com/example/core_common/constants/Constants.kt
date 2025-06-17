package com.example.core_common.constants

/**
 * 프로젝트 전반에서 사용되는 상수 값들을 정의합니다.
 */
object Constants {
    const val NO_CATEGORY_ID = "NoCategory" // Changed to Double
    /**
     * "카테고리 없음"으로 표시될 기본 카테고리의 이름입니다.
     * 채널이 특정 카테고리에 속하지 않을 때 이 카테고리에 할당됩니다.
     */
    const val NO_CATEGORY_NAME = "카테고리 없음"

    /**
     * "카테고리 없음" 카테고리의 기본 순서 값입니다.
     * 일반적으로 가장 낮은 순서 값을 가져 목록의 처음이나 마지막에 정렬되도록 합니다.
     */
    const val NO_CATEGORY_ORDER = 0.0 // Changed to Double

    /**
     * 채널을 추가할 수 있는 최대 개수입니다. (소수점 두 자리로 표현되므로 00-99까지 100개)
     * 한 카테고리 내에서 이 개수를 초과하여 채널을 생성할 수 없습니다.
     */
    const val MAX_CHANNELS_PER_CATEGORY = 100

    /**
     * Represents the role of an owner, typically for project membership or resource ownership.
     */
    const val OWNER = "OWNER"
}
