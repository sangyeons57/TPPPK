# Task: Core_Common 모듈 컴파일 오류 해결

- [x] Step 1: ErrorMapper.kt와 DomainError.kt 파일 분석 - protected 생성자 접근 문제 이해
  - DomainError.kt에서 sealed 클래스들(NetworkError, AuthError, DataError, ChatError)의 생성자가 protected여서 ErrorMapper.kt에서 접근할 수 없음
  - ErrorMapper.kt에서 code 필드를 참조하고 있으나 HttpException에는 해당 필드가 없는 것으로 보임
  - ResultExtensions.kt에서 타입 추론 문제가 발생하고 있음
  
- [x] Step 2: DomainError.kt 수정 - 생성자 접근 제한자 수정 또는 팩토리 메서드 구현
  - 모든 sealed 클래스(NetworkError, AuthError, DataError, ChatError)의 생성자에 `public constructor` 키워드 추가
  
- [x] Step 3: ErrorMapper.kt 수정 - 존재하지 않는 'code' 필드 참조 문제 해결
  - Android HttpException과 Retrofit HttpException을 구분하여 처리
  - Android HttpException은 메시지에서 상태 코드를 파싱하도록 수정
  - Retrofit HttpException은 code() 메서드를 사용하도록 수정
  - 중복 코드를 줄이고 다양한 HTTP 예외 유형을 지원하도록 리팩토링
  
- [x] Step 4: ResultExtensions.kt 수정 - 타입 추론 문제 해결
  - `.catch { emit(Result.failure(it).toDomainError(errorMapper)) }`에서 타입 파라미터 명시: `.catch { emit(Result.failure<T>(it).toDomainError(errorMapper)) }`
  
- [x] Step 5: 모든 변경사항 테스트 빌드 및 검증
  - DomainError.kt: sealed 클래스 생성자를 protected로 변경하고 companion object로 팩토리 메서드 추가
  - ErrorMapper.kt: Retrofit 의존성 제거 및 Android HttpException만 처리하도록 수정
  - 모든 sealed 클래스 인스턴스화는 팩토리 메서드를 통해 수행
  - 컴파일에 성공함
  
- [x] Step 6: 변경사항 커밋 및 푸시
  - commit_message.txt 파일 생성: "Fix: Core_Common 모듈 컴파일 오류 해결"
  - 변경된 파일들을 git add로 스테이징
  - git commit으로 변경사항 커밋
  - git push로 원격 리포지토리에 푸시 완료

**태스크 완료** - Core_Common 모듈의 컴파일 오류를 해결하였습니다. 