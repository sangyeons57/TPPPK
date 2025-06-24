# Docker WSL 환경 테스트 가이드

## 1. Docker Desktop WSL 연동 설정

### Windows에서 Docker Desktop 설정:
1. Docker Desktop을 실행
2. Settings (톱니바퀴 아이콘) 클릭
3. Resources → WSL Integration 메뉴로 이동
4. "Enable integration with my default WSL distro" 체크
5. Ubuntu (또는 사용 중인 WSL 배포판) 토글을 ON으로 설정
6. "Apply & Restart" 클릭

### WSL에서 확인:
```bash
# WSL 터미널에서 실행
docker --version
docker-compose --version
```

## 2. Docker 빌드 테스트 명령어

### 기본 빌드 테스트:
```bash
# Docker 이미지 빌드
docker build -f Dockerfile.android -t android-kotlin-app .

# 빌드 로그 확인
docker build -f Dockerfile.android -t android-kotlin-app . --no-cache
```

### Docker Compose 사용:
```bash
# 전체 빌드 실행
docker-compose -f docker-compose.android.yml up --build android-build

# 개발 환경 실행
docker-compose -f docker-compose.android.yml up -d android-shell
docker exec -it android-kotlin-shell bash
```

### 컨테이너 내부에서 테스트:
```bash
# 컨테이너 접속 후 실행할 명령어들
./gradlew clean
./gradlew :domain:compileDebugKotlin
./gradlew assembleDebug
```

## 3. 성능 최적화를 위한 .gradle 볼륨 마운트 확인

Docker Compose에서 gradle-cache 볼륨이 정상 마운트되는지 확인:
```bash
docker volume ls | grep gradle
```

## 4. 문제 해결

### 일반적인 문제들:
- **Permission 오류**: `chmod +x ./gradlew` 실행
- **SDK 라이센스**: `yes | sdkmanager --licenses` 재실행  
- **메모리 부족**: Docker Desktop의 Memory 설정 증가 (8GB 이상 권장)

### 로그 확인:
```bash
# Docker 빌드 로그 상세 확인
docker build -f Dockerfile.android -t android-kotlin-app . --progress=plain

# 실행 중인 컨테이너 로그 확인
docker logs android-kotlin-build
```

## 5. 테스트 결과 예상

성공적인 빌드 시 다음과 같은 출력을 볼 수 있습니다:
```
BUILD SUCCESSFUL in XXs
XX actionable tasks: XX executed
```

## 6. Windows PowerShell에서 직접 테스트 (대안)

WSL Docker 연동이 안 될 경우, Windows PowerShell에서:
```powershell
# 프로젝트 디렉토리로 이동
cd D:\repository\repository_java\Android\TeamnovaPersonalProjectProjectingKotlin

# Docker 빌드
docker build -f Dockerfile.android -t android-kotlin-app .

# 빌드 실행
docker run --rm -v ${PWD}:/workspace android-kotlin-app
```