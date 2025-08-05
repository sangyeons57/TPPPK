# WebSocket Chat Server - Cloud Run 배포 가이드

이 가이드는 Cloud Run에서 WebSocket Chat 서비스를 배포하는 방법을 설명합니다.

## 🎯 목표

- Firebase Admin SDK를 Application Default Credentials (ADC)로 초기화
- 전용 서비스 계정으로 최소 권한 원칙 적용
- WebSocket 연결을 위한 적절한 타임아웃 설정
- 실제 Firebase 권한 확인을 통한 헬스체크

## 📋 사전 요구사항

1. **Google Cloud 프로젝트 설정**
   ```bash
   gcloud config set project teamnovaprojectprojecting
   ```

2. **필요한 API 활성화**
   ```bash
   gcloud services enable firebase.googleapis.com
   gcloud services enable firestore.googleapis.com
   gcloud services enable iam.googleapis.com
   gcloud services enable iamcredentials.googleapis.com
   ```

## 🔧 1단계: 서비스 계정 설정

### 자동 설정 (권장)

```bash
cd websocket-chat
./setup-service-account.sh
```

### 수동 설정

```bash
PROJECT_ID="teamnovaprojectprojecting"
SERVICE_ACCOUNT_NAME="websocket-chat-sa"
SERVICE_ACCOUNT_EMAIL="${SERVICE_ACCOUNT_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

# 1. 서비스 계정 생성
gcloud iam service-accounts create ${SERVICE_ACCOUNT_NAME} \
    --display-name="WebSocket Chat Service Account" \
    --description="Service account for WebSocket Chat Server with Firebase permissions" \
    --project=${PROJECT_ID}

# 2. Firestore 권한 부여
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:${SERVICE_ACCOUNT_EMAIL}" \
    --role="roles/datastore.user"

# 3. Firebase Auth 권한 부여
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:${SERVICE_ACCOUNT_EMAIL}" \
    --role="roles/firebaseauth.admin"

# 4. IAM Token Creator 권한 부여 (커스텀 토큰용)
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:${SERVICE_ACCOUNT_EMAIL}" \
    --role="roles/iam.serviceAccountTokenCreator"

# 5. 서비스 계정이 자신에게 토큰 생성 권한을 가질 수 있도록 설정
gcloud iam service-accounts add-iam-policy-binding \
    ${SERVICE_ACCOUNT_EMAIL} \
    --member="serviceAccount:${SERVICE_ACCOUNT_EMAIL}" \
    --role="roles/iam.serviceAccountTokenCreator"
```

## 🚀 2단계: 배포

### Cloud Build를 통한 자동 배포 (권장)

```bash
# 프로젝트 루트에서 실행
gcloud builds submit --config websocket-chat/cloudbuild.yaml
```

### 수동 배포

```bash
cd websocket-chat

# 1. Docker 이미지 빌드
docker build -t gcr.io/teamnovaprojectprojecting/websocket-chat .

# 2. Container Registry에 푸시
docker push gcr.io/teamnovaprojectprojecting/websocket-chat

# 3. Cloud Run에 배포
gcloud run deploy websocket-chat \
  --image gcr.io/teamnovaprojectprojecting/websocket-chat \
  --platform managed \
  --region asia-northeast3 \
  --allow-unauthenticated \
  --service-account websocket-chat-sa@teamnovaprojectprojecting.iam.gserviceaccount.com \
  --timeout 3600 \
  --memory 1Gi \
  --cpu 2 \
  --max-instances 10
```

## ✅ 3단계: 배포 확인

### 1. 서비스 상태 확인

```bash
# 서비스 URL 확인
gcloud run services describe websocket-chat --region=asia-northeast3 --format="value(status.url)"

# 헬스체크
curl https://websocket-chat-xxxxx-xx.a.run.app/health
```

### 2. 예상 응답

정상적인 헬스체크 응답:
```json
{
  "status": "healthy",
  "timestamp": "2025-01-27T10:30:00Z",
  "firebase": {
    "status": "initialized",
    "auth": "accessible",
    "firestore": "accessible"
  },
  "system": {
    "java_version": "17.0.9",
    "os_name": "Linux",
    "memory_total_mb": 1024,
    "memory_used_mb": 256
  },
  "websocket": {
    "endpoint": "/chat",
    "protocol": "ws",
    "authentication": "firebase_jwt"
  }
}
```

### 3. 로그 확인

```bash
# 실시간 로그 확인
gcloud logging tail "resource.type=cloud_run_revision AND resource.labels.service_name=websocket-chat"

# 최근 로그 확인
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=websocket-chat" --limit=50
```

## 🔍 4단계: 문제 해결

### Firebase 초기화 실패

**증상**: `firebase.status: "not_initialized"`

**해결 방법**:

1. **서비스 계정 권한 확인**
   ```bash
   gcloud projects get-iam-policy teamnovaprojectprojecting \
     --flatten="bindings[].members" \
     --format="table(bindings.role)" \
     --filter="bindings.members:websocket-chat-sa@teamnovaprojectprojecting.iam.gserviceaccount.com"
   ```

2. **API 활성화 확인**
   ```bash
   gcloud services list --enabled --filter="name:firebase"
   ```

3. **서비스 계정 재설정**
   ```bash
   # 기존 서비스 계정 삭제
   gcloud iam service-accounts delete websocket-chat-sa@teamnovaprojectprojecting.iam.gserviceaccount.com
   
   # 다시 생성
   ./setup-service-account.sh
   ```

### 권한 오류

**증상**: `firebase.auth: "error: ..."` 또는 `firebase.firestore: "error: ..."`

**해결 방법**:

1. **필요한 권한 추가**
   ```bash
   # FCM 권한 추가 (필요한 경우)
   gcloud projects add-iam-policy-binding teamnovaprojectprojecting \
     --member="serviceAccount:websocket-chat-sa@teamnovaprojectprojecting.iam.gserviceaccount.com" \
     --role="roles/firebasecloudmessaging.admin"
   ```

2. **Firebase 프로젝트 설정 확인**
   ```bash
   # Firebase 프로젝트 확인
   firebase projects:list
   
   # Firestore 데이터베이스 확인
   gcloud firestore databases list
   ```

### WebSocket 연결 문제

**증상**: 클라이언트에서 연결 실패

**해결 방법**:

1. **타임아웃 설정 확인**
   ```bash
   # 서비스 타임아웃 확인
   gcloud run services describe websocket-chat --region=asia-northeast3 --format="value(spec.template.spec.timeoutSeconds)"
   ```

2. **클라이언트 ping/pong 설정**
   - 클라이언트에서 3분마다 ping 전송
   - 서버에서 4분 타임아웃 설정 (이미 적용됨)

## 📊 5단계: 모니터링

### Cloud Run 메트릭

```bash
# 서비스 메트릭 확인
gcloud run services describe websocket-chat --region=asia-northeast3
```

### 로그 분석

```bash
# Firebase 관련 로그
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=websocket-chat AND textPayload:Firebase" --limit=20

# 에러 로그
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=websocket-chat AND severity>=ERROR" --limit=20
```

## 🔄 업데이트

### 코드 변경 후 재배포

```bash
# Cloud Build로 자동 재배포
gcloud builds submit --config websocket-chat/cloudbuild.yaml

# 또는 수동 재배포
cd websocket-chat
docker build -t gcr.io/teamnovaprojectprojecting/websocket-chat .
docker push gcr.io/teamnovaprojectprojecting/websocket-chat
gcloud run deploy websocket-chat --image gcr.io/teamnovaprojectprojecting/websocket-chat --region=asia-northeast3
```

### 서비스 계정 권한 변경

```bash
# 권한 추가
gcloud projects add-iam-policy-binding teamnovaprojectprojecting \
  --member="serviceAccount:websocket-chat-sa@teamnovaprojectprojecting.iam.gserviceaccount.com" \
  --role="NEW_ROLE"

# 권한 제거
gcloud projects remove-iam-policy-binding teamnovaprojectprojecting \
  --member="serviceAccount:websocket-chat-sa@teamnovaprojectprojecting.iam.gserviceaccount.com" \
  --role="ROLE_TO_REMOVE"
```

## 📝 체크리스트

배포 전 확인사항:

- [ ] Google Cloud 프로젝트 설정 완료
- [ ] 필요한 API 활성화 완료
- [ ] 서비스 계정 생성 및 권한 부여 완료
- [ ] Firebase 프로젝트 설정 확인
- [ ] 코드에서 ADC 사용 확인
- [ ] Cloud Run 타임아웃 설정 (3600초)
- [ ] 헬스체크 엔드포인트 동작 확인
- [ ] WebSocket 연결 테스트 완료

## 🆘 지원

문제가 발생하면 다음을 확인하세요:

1. **로그 확인**: `gcloud logging tail`
2. **헬스체크**: `/health` 엔드포인트
3. **권한 확인**: IAM 정책 및 서비스 계정
4. **API 활성화**: Firebase 관련 API들

추가 지원이 필요하면 프로젝트 로그와 함께 문의해주세요. 