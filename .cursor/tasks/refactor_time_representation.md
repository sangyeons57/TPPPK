# Task: 시간 데이터 표현 리팩토링 (Instant 내부 사용, UI만 LocalDateTime)

## 배경
현재 코드베이스는 `LocalDateTime`을 도메인 모델에서 사용하고 있으나, 더 나은 시간대 처리와 일관성을 위해 내부적으로는 `Instant`(UTC)를 사용하고 UI 레이어에서만 `LocalDateTime`으로 변환하는 방식으로 리팩토링이 필요합니다.

## 목표
- 모든 내부 데이터 처리(도메인 모델, 리포지토리, 데이터소스)에서 `Instant` 사용
- 데이터를 저장/조회할 때 시간대에 독립적인 표현 사용
- UI 직전에만 사용자 시간대에 맞는 `LocalDateTime`으로 변환

## 작업 단계

- [x] 1단계: 도메인 모델에서 시간 표현 사용 현황 파악
  - domain/src/main/java/com/example/domain/model 디렉토리의 모든 모델 클래스에서 `LocalDateTime` 사용 식별
  - data/src/main/java/com/example/data/model/remote 디렉토리의 모든 모델 클래스에서 시간 관련 필드 확인

  ### 분석 결과
  - **도메인 모델에서 LocalDateTime 사용 클래스:**
    - `Channel`: `lastMessageTimestamp`, `createdAt`, `updatedAt` 필드
    - `ChatMessage`: `timestamp` 필드
    - `DmConversation`: `lastMessageTimestamp`, `createdAt` 필드
    - `Schedule`: `startTime`, `endTime` 필드
    - `Invite`: `expiresAt`, `createdAt` 필드
  
  - **데이터 모델(DTO)에서 시간 표현:**
    - `ChatMessageDto`: `sentAt` 필드 (Long 타입 타임스탬프)
    - `ChannelDto`: `createdAt` 필드 (Firebase Timestamp 타입)

  - **현재 변환 패턴:**
    - Remote 데이터 계층(Firestore): `Timestamp` 또는 `Date` 사용
    - DTO에서는 `Timestamp` 또는 Long 값 사용
    - 도메인 모델에서는 `LocalDateTime` 사용

- [x] 2단계: 도메인 모델 클래스 수정
  - 모든 `LocalDateTime` 필드를 `Instant`로 변환
  - 필요한 경우 생성자와 메서드 시그니처도 업데이트

  ### 수정 결과
  다음 도메인 모델 클래스들이 수정되었습니다:
  - `Channel`: `lastMessageTimestamp`, `createdAt`, `updatedAt` 필드를 `Instant`로 변경
  - `ChatMessage`: `timestamp` 필드를 `Instant`로 변경
  - `DmConversation`: `lastMessageTimestamp`, `createdAt` 필드를 `Instant`로 변경
  - `Schedule`: `startTime`, `endTime` 필드를 `Instant`로 변경
  - `Invite`: `expiresAt`, `createdAt` 필드를 `Instant`로 변경

  각 클래스에 UI 표시를 위한 변환 헬퍼 메서드도 추가했습니다:
  - `getCreatedAtLocal()`, `getUpdatedAtLocal()`, `getLastMessageTimestampLocal()` 등
  - 모든 메서드는 `ZoneId` 파라미터를 받아 사용자 시간대에 맞게 변환 가능

- [x] 3단계: 데이터 소스 레이어 수정 (진행 중)
  - Remote 데이터 소스에서 `Date`/`Timestamp`를 `Instant`로 변환하는 로직 업데이트 
  - Local 데이터 소스에서 시간 저장 형식 업데이트

  ### 진행 상황
  1. 시간 관련 유틸리티 클래스 구현:
     - `DateTimeUtil`에 `Instant` 관련 메서드 추가
     - `Date`, `Timestamp`, `LocalDateTime`, `Instant` 간 변환 메서드 구현
     - 시간 포맷팅 유틸리티 메서드 추가
  
  2. 데이터 소스 클래스 수정 (진행 중):
     - `ProjectStructureRemoteDataSourceImpl` 클래스를 수정하여 `Instant` 사용
     - `LocalDateTime.now()` 대신 `DateTimeUtil.nowInstant()` 사용
     - 날짜 변환 로직에서 `DateTimeUtil` 유틸리티 사용
  
  3. 남은 데이터 소스 작업:
     - 다른 Remote 데이터 소스 클래스들도 동일한 방식으로 수정 필요
     - Local 데이터 소스에서 시간 저장 방식도 업데이트 필요

- [ ] 4단계: 리포지토리 구현체 수정
  - 리포지토리에서 시간 변환 로직 업데이트
  - Firestore 변환 메서드 수정 (Date ↔ Instant)

- [ ] 5단계: 유틸리티 클래스 생성
  - `Instant`와 `LocalDateTime` 간 변환을 도와주는 유틸리티 클래스 생성
  - 시간대(TimeZone) 처리 논리 통합

- [ ] 6단계: UI 레이어 / 뷰모델 수정
  - 뷰모델에서 `Instant`를 UI 표시용 `LocalDateTime`으로 변환하는 로직 추가
  - UI 컴포넌트에서 시간 표시 방식 업데이트

- [ ] 7단계: 테스트 및 검증
  - 여러 시간대에서 앱 동작 테스트
  - 데이터 일관성 검증
  - 기존 기능 정상 작동 확인 