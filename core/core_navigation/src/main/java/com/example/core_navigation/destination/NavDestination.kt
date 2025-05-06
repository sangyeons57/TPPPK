package com.example.core_navigation.destination

/**
 * 네비게이션 목적지를 정의하는 인터페이스
 * 모든 화면 목적지 객체가 구현해야 합니다.
 */
interface NavDestination {
    /**
     * 이 목적지의 고유 경로
     */
    val route: String
    
    /**
     * 네비게이션 인자들의 목록
     */
    val arguments: List<String>
        get() = emptyList()
    
    /**
     * 인자들을 포함한 전체 경로 생성
     * 
     * @param args 경로에 추가할 인자 맵
     * @return 인자를 포함한 완전한 경로 문자열
     */
    fun createRoute(args: Map<String, Any> = emptyMap()): String {
        if (args.isEmpty()) return route
        
        // 기본 경로에 인자 추가
        val argValues = arguments.map { argName ->
            args[argName]?.toString() ?: "null"
        }
        
        return buildRouteWithArgs(argValues)
    }
    
    /**
     * 인자 값들을 사용하여 경로 생성
     * 
     * @param argValues 인자 값 목록
     * @return 인자 값을 포함한 경로
     */
    fun buildRouteWithArgs(argValues: List<String>): String {
        if (argValues.isEmpty()) return route
        
        return route + argValues.joinToString(prefix = "/", separator = "/")
    }
} 