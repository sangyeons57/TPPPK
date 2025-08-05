#!/bin/bash

# WebSocket Chat Service Account Setup Script
# 이 스크립트는 Cloud Run WebSocket 서비스를 위한 전용 서비스 계정을 생성하고 필요한 권한을 부여합니다.

set -e

PROJECT_ID="teamnovaprojectprojecting"
SERVICE_ACCOUNT_NAME="websocket-chat-sa"
SERVICE_ACCOUNT_EMAIL="${SERVICE_ACCOUNT_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

echo "🔧 Setting up service account for WebSocket Chat Service..."
echo "📋 Project ID: ${PROJECT_ID}"
echo "👤 Service Account: ${SERVICE_ACCOUNT_EMAIL}"

# 1. 서비스 계정 생성
echo "🔧 Creating service account..."
gcloud iam service-accounts create ${SERVICE_ACCOUNT_NAME} \
    --display-name="WebSocket Chat Service Account" \
    --description="Service account for WebSocket Chat Server with Firebase permissions" \
    --project=${PROJECT_ID}

# 2. Firestore 권한 부여 (Datastore User 역할)
echo "🔥 Granting Firestore permissions..."
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:${SERVICE_ACCOUNT_EMAIL}" \
    --role="roles/datastore.user"

# 3. Firebase Auth 권한 부여
echo "🔐 Granting Firebase Auth permissions..."
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:${SERVICE_ACCOUNT_EMAIL}" \
    --role="roles/firebaseauth.admin"

# 4. FCM 권한 부여 (선택사항)
echo "📱 Granting Firebase Cloud Messaging permissions..."
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:${SERVICE_ACCOUNT_EMAIL}" \
    --role="roles/firebasecloudmessaging.admin"

# 5. 커스텀 토큰 생성을 위한 IAM 권한 부여
echo "🎫 Granting IAM Service Account Token Creator permissions..."
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:${SERVICE_ACCOUNT_EMAIL}" \
    --role="roles/iam.serviceAccountTokenCreator"

# 6. 서비스 계정이 자신에게 토큰 생성 권한을 가질 수 있도록 설정
echo "🔑 Setting up self-signing permissions..."
gcloud iam service-accounts add-iam-policy-binding \
    ${SERVICE_ACCOUNT_EMAIL} \
    --member="serviceAccount:${SERVICE_ACCOUNT_EMAIL}" \
    --role="roles/iam.serviceAccountTokenCreator"

echo "✅ Service account setup completed!"
echo "📋 Summary:"
echo "   - Service Account: ${SERVICE_ACCOUNT_EMAIL}"
echo "   - Firestore: ✅ (datastore.user)"
echo "   - Firebase Auth: ✅ (firebaseauth.admin)"
echo "   - FCM: ✅ (firebasecloudmessaging.admin)"
echo "   - IAM Token Creator: ✅ (iam.serviceAccountTokenCreator)"
echo ""
echo "🚀 Next steps:"
echo "   1. Deploy the service using the updated cloudbuild.yaml"
echo "   2. The service will automatically use this service account"
echo "   3. Check /health endpoint for Firebase status" 