# feature_project 모듈 오류 상세 분석

## 개요
feature_project 모듈에서는 프로젝트 관리, 멤버 관리, 역할 관리 등의 기능을 구현하는 코드에 다수의 컴파일 오류가 발생하고 있습니다. 주요 오류는 도메인 모델 참조 불일치, UI 모델 누락, 타입 추론 실패 등으로 구분됩니다.

## 파일별 오류 분석

### EditMemberScreen.kt

1. **속성 참조 오류**:
   - 134줄: `profileImageUrl` 속성 참조 실패
   - 135줄: `userName` 속성 참조 실패
   - 140줄: `userName` 속성 참조 실패

2. **타입 불일치 및 타입 추론 오류**:
   - 223줄, 224줄: 타입 추론 실패, `List<T>` 타입이 `Map<String, Boolean>` 으로 기대되는 곳에 사용됨
   - 223줄, 224줄: `RolePermission` 참조 실패
   - 223줄, 224줄: `memberCount` 매개변수 없음

3. **생성자 호출 오류**:
   - 225줄: Instant? 타입 기대되는 곳에 String 타입 사용
   - 225줄: List<String> 타입에 null 전달
   - 225줄: Member 생성자에 너무 많은 인수 전달

### MemberListScreen.kt

1. **속성 참조 오류**:
   - 112줄: `userName` 속성 참조 실패
   - 224줄: `profileImageUrl` 속성 참조 실패 
   - 225줄: `userName` 속성 참조 실패
   - 233줄: `userName` 속성 참조 실패
   - 239줄, 241줄: `roles` 속성 참조 실패

### EditMemberViewModel.kt

1. **when 문 완전성 오류**:
   - 80줄: 'when' 표현식이 모든 조건을 포함하지 않음, 'Initial', 'Loading', 'is Progress' 분기 또는 'else' 분기 누락

2. **참조 및 타입 오류**:
   - 85줄: `roles` 속성 참조 실패
   - 85줄: 여러 위치에서 타입 추론 실패
   - 85줄: `id` 속성 참조 실패
   - 90줄: `exceptionOrNull` 참조 실패
   - 168줄: `exceptionOrNull` 참조 실패

### MemberListViewModel.kt

1. **타입 추론 및 함수 호출 오류**:
   - 106줄, 107줄: 타입 추론 실패
   - 107줄: Flow<T>에서 CustomResult 타입으로 변환 시 타입 불일치
   - 107줄: filter 함수 사용 시 수신자 타입 불일치
   - 108줄: `userName` 속성 참조 실패
   - 111줄: 타입 추론 실패, 타입 인자 T에 충분한 정보가 없음
   - 127줄: `fetchProjectMembersUseCase` 참조 실패
   - 185줄: `userName` 속성 참조 실패

### EditRoleScreen.kt

1. **RolePermission 관련 오류**:
   - 20줄, 148줄, 178줄, 227줄, 263줄: `RolePermission` 참조 실패
   - 178줄, 263줄, 278줄, 283줄: 타입 추론 실패

2. **Composable 컨텍스트 오류**:
   - 179줄: Composable 함수가 @Composable 함수 컨텍스트 외부에서 호출됨

3. **속성 참조 및 타입 불일치 오류**:
   - 241줄: `name` 속성 참조 실패
   - 275줄, 276줄, 277줄: 인수 타입 불일치 (String? vs String)
   - 278줄, 279-281줄, 283-286줄: Map 타입 관련 불일치 및 RolePermission 참조 오류
   - 288줄: Boolean 타입 인수 불일치

### EditRoleViewModel.kt

1. **RolePermission 관련 오류**:
   - 9줄, 25줄, 28줄, 87줄, 134줄: `RolePermission` 참조 실패

2. **함수 호출 및 타입 오류**:
   - 85줄: `getOrNull` 참조 실패
   - 87줄: `permissions` 속성 참조 실패
   - 91줄, 92줄: `name` 속성 참조 실패
   - 95줄, 96줄: `isDefault` 속성 참조 실패
   - 108줄, 209줄, 244줄: `exceptionOrNull` 참조 실패
   - 136줄, 187줄: Map 관련 타입 추론 및 함수 호출 오류
   - 195줄: 인수 타입 불일치 및 함수 호출 인자 개수 오류

### ProjectSettingScreen.kt

1. **속성 참조 오류**:
   - 245줄: `channels` 속성 참조 실패
   - 246줄, 250줄: `id` 속성 참조 실패
   - 388줄: `name` 속성 참조 실패

2. **Composable 컨텍스트 오류**:
   - 248줄: Composable 함수가 @Composable 함수 컨텍스트 외부에서 호출됨

### ProjectSettingViewModel.kt

1. **인수 타입 및 함수 호출 오류**:
   - 83줄: List<CategoryCollection> 타입이 List<Category> 타입으로 기대되는 곳에 사용됨
   - 116줄, 144줄: 필수 매개변수 `categoryId` 누락
   - 144줄: 필수 매개변수 `channelId` 누락
   - 122줄: `exceptionOrNull` 참조 실패

### JoinProjectScreen.kt

1. **참조 오류**:
   - 50줄: `NavDestination` 참조 실패

### ProjectDetailScreen.kt

1. **UI 모델 참조 오류**:
   - 69줄, 101줄, 102줄, 133줄, 134줄: `ChannelUiModel`, `CategoryUiModel` 참조 실패

2. **속성 참조 오류**:
   - 72줄, 73줄: `isDirect`, `categoryId`, `id` 속성 참조 실패
   - 86줄, 88줄: `createChannelDialogData` 속성 참조 실패
   - 107줄: Collection.isNotEmpty() 메소드 호출 시 수신자 타입 불일치
   - 139줄, 141줄: `name` 속성 참조 실패
   - 145줄, 149줄: `channels` 속성 참조 실패

3. **Composable 컨텍스트 오류**:
   - 112줄, 121줄, 126줄: Composable 함수가 @Composable 함수 컨텍스트 외부에서 호출됨

## 주요 문제점 분류

### 1. 누락된 도메인 모델 및 UI 모델
- `RolePermission` 열거형이 존재하지 않거나, 참조 경로가 변경됨
- `Member` 클래스 구조가 변경되어 `userName`, `profileImageUrl` 등의 속성이 누락되거나 경로가 변경됨
- `CategoryUiModel`, `ChannelUiModel` 클래스가 존재하지 않거나 참조 경로가 변경됨

### 2. 타입 시스템 불일치
- Map<RolePermission, Boolean> 타입과 List 타입 간의 변환 누락
- 함수 호출 시 인수 타입 불일치 (String vs String?, Instant vs String 등)
- 타입 추론 실패로 인한 컴파일러 오류

### 3. Result 패턴 사용 오류
- `exceptionOrNull()`, `getOrNull()` 등 확장 함수 참조 실패
- Result 타입과 다른 타입 간의 변환 오류

### 4. Composable 컨텍스트 위반
- @Composable 함수를 부적절한 컨텍스트(예: 일반 함수 내부)에서 호출

## 해결 방향

### 1. 모델 클래스 재정의
- `RolePermission` 열거형 재정의 또는 import 경로 수정
- `Member` 클래스 구조 조정 및 필요한 속성 추가
- UI 모델 클래스 (`CategoryUiModel`, `ChannelUiModel` 등) 구현

### 2. 타입 변환 함수 구현
- 도메인 모델과 UI 모델 간의 변환 로직 구현
- List와 Map 간의 변환 함수 구현

### 3. Result 패턴 지원 추가
- Result 클래스를 위한 확장 함수 추가 (`exceptionOrNull`, `getOrNull` 등)
- CustomResult 타입에 대한 지원 개선

### 4. Composable 함수 호출 구조 수정
- Composable 함수 호출이 올바른 컨텍스트에서 이루어지도록 코드 구조 재조정

## 추가 고려사항

1. **Clean Architecture 준수**: 모델 클래스 수정 시 Clean Architecture의 계층 분리 원칙을 준수해야 함
2. **일관된 네이밍 규칙**: 모델 클래스와 속성 간의 일관된 네이밍을 유지
3. **타입 안전성**: 타입 변환 및 null 처리에 있어 안전한 패턴 사용
4. **확장성 고려**: 향후 추가될 수 있는 기능에 대응할 수 있는 유연한 설계

## 연관된 이전 작업

1. **SearchScreen.kt 및 SearchViewModel.kt 오류 수정**:
   - 이전 세션에서 해결했던 네비게이션 이벤트 플로우 관련 오류와 유사한 패턴이 발견됨
   - 모델 참조 경로 변경에 따른 동일한 유형의 오류 발생

2. **NavDestination 클래스 구현**:
   - 네비게이션 관련 오류는 이전에 구현한 NavDestination 클래스를 활용하여 해결 가능

## 다음 작업 계획

1. **도메인 모델 클래스 확인 및 수정**: 누락된 모델 클래스 및 속성 구현
2. **UI 모델 클래스 확인 및 수정**: UI 관련 모델 클래스 구현 및 속성 매핑
3. **ViewModel 로직 수정**: 타입 추론 및 Result 패턴 관련 오류 해결
4. **UI 컴포넌트 수정**: Composable 함수 호출 구조 및 컨텍스트 조정
5. **테스트 및 검증**: 각 화면별 기능 작동 확인 및 추가 오류 해결

이 문서는 feature_project 모듈의 오류를 체계적으로 해결하기 위한 상세 분석을 제공합니다. 실제 수정 작업은 위에 명시된 계획에 따라 단계별로 진행되어야 합니다.
