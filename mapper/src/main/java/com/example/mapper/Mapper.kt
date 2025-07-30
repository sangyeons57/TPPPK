package com.example.mapper

/**
 * Entity, Domain Model, DTO 간의 매핑을 담당하는 기본 인터페이스
 * Clean Architecture 원칙에 따라 JSON 의존성 없이 순수한 객체 변환만 담당
 *
 * @param Entity Room Entity 타입 (로컬 저장용)
 * @param Domain Domain Model 타입 (비즈니스 로직용)
 * @param DTO Data Transfer Object 타입 (원격 API용)
 */
interface Mapper<Entity, Domain, DTO> {

    /**
     * Entity를 Domain Model로 변환
     * @param entity 변환할 Entity 객체
     * @return Domain Model 객체
     */
    fun entityToDomain(entity: Entity): Domain

    /**
     * Domain Model을 Entity로 변환
     * @param domain 변환할 Domain Model 객체
     * @return Entity 객체
     */
    fun domainToEntity(domain: Domain): Entity

    /**
     * DTO를 Domain Model로 변환
     * @param dto 변환할 DTO 객체
     * @return Domain Model 객체
     */
    fun dtoToDomain(dto: DTO): Domain

    /**
     * Domain Model을 DTO로 변환
     * @param domain 변환할 Domain Model 객체
     * @return DTO 객체
     */
    fun domainToDto(domain: Domain): DTO
}

/**
 * Entity가 없는 Domain Model을 위한 Mapper 인터페이스
 * DTO ↔ Domain 변환만 제공
 *
 * @param Domain Domain Model 타입
 * @param DTO Data Transfer Object 타입
 */
interface DtoMapper<Domain, DTO> {

    /**
     * DTO를 Domain Model로 변환
     * @param dto 변환할 DTO 객체
     * @return Domain Model 객체
     */
    fun dtoToDomain(dto: DTO): Domain

    /**
     * Domain Model을 DTO로 변환
     * @param domain 변환할 Domain Model 객체
     * @return DTO 객체
     */
    fun domainToDto(domain: Domain): DTO
}