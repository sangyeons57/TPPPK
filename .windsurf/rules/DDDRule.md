---
trigger: model_decision
description: DDD, 도메인 모델, 도메인 주도 등의 언급이 나올떄 참조할수 있는 Rule
---

# **Domain-Driven Design (DDD) Rules – Projecting Kotlin**

본 문서는 **Teamnova Personal Projecting Kotlin** 코드베이스에서 DDD 패턴을 적용할 때 반드시 지켜야 할 규칙(Rules)을 정리한 참조 문서이다. 워크플로우가 아닌 *설계 헌장*의 형태로, 코드 리뷰 시 준수 여부를 확인하는 기준으로 활용한다.

## **1\. 계층(Layer) 구조 원칙**

의존성은 **Presentation → Domain → Data** 방향으로만 흐릅니다 (단방향 의존).

1. **Presentation (Application Layer):** ViewModel, UI 등 사용자 인터페이스를 담당합니다. Domain 계층에만 의존할 수 있습니다.  
2. **Domain (Business Layer):** 순수한 비즈니스 로직을 담당합니다. Entity, Value Object, UseCase가 여기에 속하며, 외부 프레임워크나 라이브러리(Android, Firebase 등)를 참조할 수 없습니다.  
3. **Data (Infrastructure Layer):** 데이터베이스, 네트워크 등 외부와의 통신을 담당합니다. Domain 객체를 DTO로 맵핑하여 외부 스토리지(Firebase 등)와 통신하며, Domain 계층에 의존합니다.

## **2\. Entity & Aggregate**

엔티티는 비즈니스 규칙과 자신의 상태를 책임지는 객체입니다.

* **단일 책임:** 비즈니스 규칙과 상태의 불변성(Invariant)을 유지하는 것에만 집중합니다.  
* **불변 생성:** private 생성자와 정적 팩토리 메서드(companion object 내)를 통해 생성합니다.  
* **ID 규칙:** 모든 식별자는 UserId, DocumentId 같은 값 객체(VO)로 래핑하여 타입 안전성을 확보합니다.  
* **도메인 이벤트:** 상태 변경 결과로 발생하는 이벤트는 내부 리스트에 저장하고 pullDomainEvents() 메서드로 외부에 제공합니다.

### **예시: User 애그리게이트 정의**

// 위치: domain/src/main/java/com/example/domain/model/base/User.kt

class User private constructor(  
    uid: DocumentId,  
    email: UserEmail,  
    createdAt: Instant,  
    name: UserName  
    // ...  
) : AggregateRoot {  
    val uid: DocumentId \= uid  
    val email: UserEmail \= email  
    val createdAt: Instant \= createdAt  
    var name: UserName \= name  
        private set  
    // ...

    private val \_domainEvents \= mutableListOf\<DomainEvent\>()  
    override fun pullDomainEvents(): List\<DomainEvent\> { /\* ... \*/ }

    fun changeName(newName: UserName) {  
        this.name \= newName  
        \_domainEvents.add(UserNameChangedEvent(uid))  
    }  
    // ...  
}

## **3\. Value Object (VO)**

단순한 값을 의미 있게 포장한 객체입니다.

1. 모든 프로퍼티는 불변(val-only)으로 선언합니다.  
2. equals()와 hashCode()는 값을 기반으로 구현합니다. (value class 사용을 강력히 권장합니다.)  
3. 단순 기본 타입이라도 특정 의미를 갖는다면(ID, URL, 이메일, 제목 등) 값 객체로 래핑합니다.

### **예시: DocumentId 값 객체 정의**

// 위치: domain/src/main/java/com/example/domain/model/vo/DocumentId.kt

@JvmInline  
value class DocumentId(val value: String) {  
    init {  
        require(value.isNotBlank()) { "Document ID는 비어있을 수 없습니다." }  
    }  
}

## **4\. Domain Event**

도메인에서 발생한 의미 있는 과거의 사건을 나타내는 객체입니다.

* 모든 이벤트는 DomainEvent 인터페이스를 구현하며, 발생 시각(occurredOn)을 포함해야 합니다.  
* 이벤트 클래스는 domain/event/\<aggregate\> 패키지에 위치시킵니다.

### **예시: UserAccountActivatedEvent 정의**

// 위치: domain/src/main/java/com/example/domain/event/user/UserAccountActivatedEvent.kt

data class UserAccountActivatedEvent(  
    val userId: DocumentId,  
    override val occurredOn: Instant \= Instant.now()  
) : DomainEvent

## **5\. Event Dispatcher**

도메인 모델에서 발생한 상태 변경을 시스템의 다른 부분(핸들러)으로 전파하는 중앙 허브 메커니즘입니다.

* **AggregateRoot 인터페이스:** 이벤트를 발생시키는 애그리게이트가 구현하며, pullDomainEvents() 메서드를 제공합니다.  
* **EventHandler 인터페이스:** 특정 이벤트를 수신하여 처리하는 로직을 구현합니다.  
* **발행 책임:** 유스케이스는 애그리게이트의 상태를 저장한 후, EventDispatcher.publish(aggregate)를 호출하여 이벤트를 발행합니다.

### **예시: 관련 인터페이스 정의**

// 위치: domain/src/main/java/com/example/domain/event/

interface DomainEvent { val occurredOn: Instant }  
interface AggregateRoot { fun pullDomainEvents(): List\<DomainEvent\> }  
fun interface EventHandler\<E : DomainEvent\> { fun handle(event: E) }  
object EventDispatcher { /\* ... \*/ }

## **6\. Repository 인터페이스 (Domain 계층)**

데이터베이스와의 통신을 위한 계약(규칙)을 정의합니다. 구현은 Data 계층에서 담당합니다.

* **save(entity)**: Upsert (없으면 생성, 있으면 병합)  
* **findById(id)**: ID를 이용한 단건 조회  
* **observe(id)**: 실시간 변경 감지  
* **delete(id)**: 삭제  
* **Query 메서드**: findBy\* 접두사 사용. 존재 여부 확인도 findBy\*를 통해 수행합니다.

### **예시: UserRepository 인터페이스 정의**

// 위치: domain/src/main/java/com/example/domain/repository/UserRepository.kt

interface UserRepository {  
    suspend fun save(user: User): CustomResult\<Unit, Exception\>  
    suspend fun findById(id: DocumentId): CustomResult\<User?, Exception\>  
    fun observeUser(id: DocumentId): Flow\<CustomResult\<User?, Exception\>\>  
    suspend fun findByEmail(email: UserEmail): CustomResult\<User?, Exception\>  
}

## **7\. DTO (Data Transfer Object)**

외부 시스템(Firestore, API 등)과 데이터를 주고받기 위한 순수한 데이터 컨테이너입니다.

1. 모든 프로퍼티는 외부에 노출되어야 하며, 기본 생성자를 가질 수 있습니다.  
2. **도메인 객체와의 변환 책임은 DTO가 가집니다.** toDomain() 메서드나 fromDomain() 팩토리 메서드를 companion object 내에 정의합니다.

### **예시: UserDTO 정의**

// 위치: data/src/main/java/com/example/data/model/remote/UserDTO.kt

data class UserDTO(  
    val uid: String \= "",  
    val email: String \= "",  
    val name: String \= "",  
    val createdAt: Timestamp \= Timestamp.now()  
    // ...  
) {  
    fun toDomain(): User {  
        return User.fromDataSource( // Entity의 팩토리 메서드 사용  
            uid \= DocumentId(this.uid),  
            email \= UserEmail(this.email),  
            name \= UserName(this.name),  
            createdAt \= this.createdAt.toInstant()  
            // ...  
        )  
    }

    companion object {  
        fun fromDomain(user: User): UserDTO {  
            return UserDTO(  
                uid \= user.uid.value,  
                email \= user.email.value,  
                name \= user.name.value,  
                createdAt \= Timestamp.from(user.createdAt)  
                // ...  
            )  
        }  
    }  
}

## **8\. DataSource (Data 계층)**

Firestore와 같은 외부 데이터 소스와 **직접 통신**하는 가장 낮은 수준의 인터페이스 및 구현체입니다.

1. DataSource는 DTO를 반환하거나 파라미터로 받습니다.  
2. Firestore 저장 시 set(dto, SetOptions.merge())를 사용하여 Upsert 동작을 구현합니다.  
3. 캐싱 전략은 Firebase의 오프라인 캐시 기능을 우선적으로 활용합니다.

### **예시: UserRemoteDataSource 정의**

// 위치: data/src/main/java/com/example/data/datasource/remote/UserRemoteDataSource.kt

interface UserRemoteDataSource {  
    suspend fun save(userDto: UserDTO): CustomResult\<Unit, Exception\>  
    suspend fun findById(uid: String): CustomResult\<UserDTO?, Exception\>  
}

class UserRemoteDataSourceImpl(  
    private val firestore: FirebaseFirestore  
) : UserRemoteDataSource {  
    override suspend fun save(userDto: UserDTO): CustomResult\<Unit, Exception\> {  
        // Firestore SDK를 직접 사용하여 DTO를 저장 (Upsert)  
        firestore.collection("users").document(userDto.uid)  
            .set(userDto, SetOptions.merge())  
        // ...  
    }  
    // ...  
}

## **9\. RepositoryImpl (Data 계층)**

domain 계층의 Repository 인터페이스를 구현합니다.

1. DataSource에 의존하며, 데이터 요청을 위임합니다.  
2. DataSource로부터 받은 DTO를 도메인 객체로 변환하여 상위 계층(UseCase)에 반환합니다.

### **예시: UserRepositoryImpl 구조**

// 위치: data/src/main/java/com/example/data/repository/UserRepositoryImpl.kt

class UserRepositoryImpl(  
    private val remoteDataSource: UserRemoteDataSource  
) : UserRepository {

    override suspend fun save(user: User): CustomResult\<Unit, Exception\> {  
        // Domain 객체를 DTO로 변환하여 DataSource에 전달  
        val userDto \= UserDTO.fromDomain(user)  
        return remoteDataSource.save(userDto)  
    }

    override suspend fun findById(id: DocumentId): CustomResult\<User?, Exception\> {  
        return when (val result \= remoteDataSource.findById(id.value)) {  
            is CustomResult.Success \-\> {  
                // DTO를 Domain 객체로 변환하여 반환  
                val user \= result.data?.toDomain()  
                CustomResult.Success(user)  
            }  
            is CustomResult.Failure \-\> result  
            // ...  
        }  
    }  
    // ...  
}

## **10\. UseCase**

하나의 비즈니스 시나리오를 책임지는 클래스입니다. Interface와 Impl을 한 파일에 함께 정의합니다.

### **예시: SignUpUseCase 정의**

// 위치: domain/src/main/java/com/example/domain/usecase/auth/SignUpUseCase.kt

interface SignUpUseCase {  
    suspend operator fun invoke(email: UserEmail, name: UserName): CustomResult\<User, Exception\>  
}

class SignUpUseCaseImpl(  
    private val userRepository: UserRepository  
) : SignUpUseCase {  
    override suspend fun invoke(email: UserEmail, name: UserName): CustomResult\<User, Exception\> {  
        // ... 유스케이스 로직 구현 ...  
    }  
}

## **11\. Presentation (ViewModel/UI)**

1. ViewModel은 UI 상태(State)와 단발성 UI 이벤트(UIEvent)만을 관리하는 데 집중합니다.  
2. 액션 성격이 강한 UseCase는 해당 기능이 주로 사용되는 전용 화면의 ViewModel에서 호출합니다.

## **12\. Naming & Packaging 규칙**

* **Aggregate (Entity)**  
  * **위치:** domain/src/main/java/com/example/domain/model/base/  
  * **예시:** User.kt, Schedule.kt  
* **Value Object (VO)**  
  * **위치:** domain/src/main/java/com/example/domain/model/vo/\<aggregate\>/  
  * **예시:** domain/model/vo/user/UserName.kt  
* **Enum**  
  * **위치:** domain/src/main/java/com/example/domain/model/enum/  
  * **예시:** UserStatus.kt  
* **Domain Event & Handlers**  
  * **위치:** domain/src/main/java/com/example/domain/event/  
  * **세부:** 이벤트 클래스는 \<aggregate\> 하위 패키지에 위치 (domain/event/user/)  
  * **예시:** EventDispatcher.kt, user/UserCreatedEvent.kt  
* **DTO (Data Transfer Object)**  
  * **위치:** data/src/main/java/com/example/data/model/remote/  
  * **예시:** UserDTO.kt  
* **DataSource (Interface & Impl)**  
  * **위치:** data/src/main/java/com/example/data/datasource/remote/  
  * **예시:** UserRemoteDataSource.kt  
* **Repository Interface**  
  * **위치:** domain/src/main/java/com/example/domain/repository/  
  * **예시:** UserRepository.kt  
* **Repository Implementation**  
  * **위치:** data/src/main/java/com/example/data/repository/  
  * **예시:** UserRepositoryImpl.kt  
* **UseCase (Interface & Impl)**  
  * **위치:** domain/src/main/java/com/example/domain/usecase/\<aggregate\>/  
  * **예시:** domain/usecase/user/UpdateUserNameUseCase.kt

## **13\. 시간(Time) 처리**

1. 모든 시간 데이터는 **java.time.Instant** 타입을 사용하여 UTC 기준으로 다룹니다.  
2. UI에 표시할 때만 ViewModel/UI 계층에서 사용자의 시간대(ZoneId)를 고려하여 ZonedDateTime이나 LocalDateTime으로 변환합니다.  
3. 시간 관련 변환 및 계산은 DateTimeUtil 유틸리티 클래스를 사용하여 통일합니다.

## **체크리스트**

* \[ \] 계층 간 순환 의존성이 없는가?  
* \[ \] Repository API 네이밍 규칙을 준수하였는가?  
* \[ \] 의미 있는 단위에 대해 VO, Entity, DomainEvent, KDoc 작성을 완료하였는가?  
* \[ \] save() 메서드의 Upsert 동작이 구현되었는가?  
* \[ \] 조회 목적의 쿼리 메서드는 findBy\* 접두사를 사용하는가?  
* \[ \] 작성한 로직에 대한 단위 테스트 케이스가 작성되었는가?

위 규칙을 위반할 경우 코드 리뷰에서 변경 요청(Request changes)될 수 있다. 규칙 변경이 필요할 경우, 본 문서를 먼저 업데이트하고 팀 전체에 공지한다.