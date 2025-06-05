FIrebase Storage에 저장될 데이터

실제 아레 3개 요소가  어떻게 참조,업로드, 다운로드, 보안규칙, 계층 구조, 통합 등을 실행 하는지 자세히 작성성

1. 사용자 프로필 이미지 데이터  
2. 프로젝트 프로필 이미지 데이터  
3. 체팅 메시지에서 전송하게될 이미지 및 파일 데이터  
   1. 이미지, 영상, 그리고 각종 파일

### **2\. 참조 (Reference) 📌**

* 파일에 접근하려면 먼저 해당 파일에 대한 **참조**를 만들어야 합니다.  
* 참조는 특정 파일 또는 디렉토리(실제로는 객체 경로의 접두사)를 가리키는 포인터와 같습니다. 예를 들어, `images/my_image.jpg`와 같은 경로를 사용하여 참조를 생성할 수 있습니다.

실제 사용시 

- 경로: user\_profile\_images/{userId}/{fileName}  
  - 용도: 사용자 프로필 이미지 저장  
  - 사용자 아이디/ 파일 이름  
- project\_profile\_images/{projectId}/{fileName}  
  - 용도: 프로젝트 프로필 이미지 저장  
  - 프로젝트 ID/ 파일 이름  
- dm\_channel\_files/{dmChannelId}/{messageId}/{fileType}/{fileName}  
  - 용도: (파일, 이미지) 등이 할당된 메시지 저장  
  - DM체널 ID/메시지ID/파일 이름  
- project\_channel\_files/{projectChannelId}/{messageId}/{fileType}/{fileName}  
  - 용도: (파일, 이미지) 등이 할당된 메시지 저장  
  - DM체널 ID/메시지ID/파일 이름  
  - 특이사항: Category는 저장하지 않음 구지 필요가없고 ProjectChannelId만으로 식별 가능하기 때문

필수정보

- 사용자ID \= userId  
  - 회원 가입시 생성됨  
  - 로그인해야 각 계정에서 사용가능  
    - Firebase Authentication \- uid

### **3\. 업로드 (Upload) ⬆️**

* 클라이언트 앱(iOS, Android, 웹) 또는 서버 환경(Admin SDK 사용)에서 파일이나 바이트 데이터를 Firebase Storage 참조로 **업로드**할 수 있습니다.  
* SDK는 파일 전송, 네트워크 문제 처리, 진행률 모니터링 등의 복잡한 작업을 자동으로 처리합니다. 데이터는 바이트 스트림 형태로 전송됩니다.

실제 사용시

- 경로: user\_profile\_images/{userId}/{fileName}  
  - 필요정보  
    - userId  
    - 사용자가 선택한 이미지의 Uri  
- project\_profile\_images/{projectId}/{fileName}  
  - 필요정보  
    - projectId  
    - 사용자가 선택한 이미지의 Uri  
    - userId(프로젝트 오너 인증용)  
- dm\_channel\_files/{dmChannelId}/{messageId}/{fileType}/{fileName}  
  - 필요정보  
    - dmChannelId (체널 ID)   
    - messageId (메시지 ID)  
    - 파일 타입 (이미지, 영상, 문서 등등)  
- project\_channel\_files/{projectChannelId}/{messageId}/{fileType}/{fileName}  
  - 필요정보  
    - projectChannelId(체널 ID)   
    - messageId (메시지 ID)  
    - 파일 타입 (이미지, 영상, 문서 등등)

### **5\. 보안 규칙 (Security Rules) 🛡️**

* Firebase Storage 보안 규칙을 사용하여 파일에 대한 **접근을 제어**합니다.  
* 사용자의 인증 상태, 파일 메타데이터(크기, 콘텐츠 유형 등), 경로 등을 기반으로 읽기 및 쓰기 권한을 세밀하게 설정할 수 있습니다. 이를 통해 특정 사용자만 파일을 업로드하거나 다운로드하도록 제한할 수 있습니다.  
* 예를 들어, 인증된 사용자만 자신의 프로필 이미지를 업로드하고 읽을 수 있도록 규칙을 설정할 수 있습니다.

실제 사용시

- 경로: user\_profile\_images/{userId}/{fileName}  
  - Create  
    - (로그인)인증된 본인  
  - Read  
    - (로그인)인증된 모두  
  - Update  
    - (로그인)인증된 본인  
  - Delete  
    - (로그인)인증된 본인  
- project\_profile\_images/{projectId}/{fileName}  
  - Create  
    - 프로젝트 오너  
  - Read  
    - (로그인)인증된 모두  
  - Update  
    - 프로젝트 오너  
  - Delete  
    - 프로젝트 오너  
- dm\_channel\_files/{dmChannelId}/{messageId}/{fileType}/{fileName}  
  - Create  
    - 작성자  
  - Read  
    - DM상대 \+ 작성자  
  - Update  
    - 작성자  
  - Delete  
    - 작성자  
- project\_channel\_files/{projectChannelId}/{messageId}/{fileType}/{fileName}  
  - Create  
    - 작성자  
  - Read  
    - 읽기 권한이 있는 (로그인)인증된 맴버  
  - Update  
    - 작성자  
  - Delete  
    - 작성자  
    - 프로젝트 오너  
    - 제거권한이 있는 (로그인)인증된 멤버

직접적인 update는 불가능 하나 데신 다음과 같은 방식이 있음

- overwrite : 같은 경로 로 덮어써서 작성  
- 제거 후 작성

\#프로젝트 오너 \= “프로젝트 생성한 사람 혹은 그후에 해당 권한을 받은 사람”

### **7\. Cloud Functions와의 통합 ⚙️**

* Firebase Storage의 파일 변경(업로드, 삭제, 메타데이터 업데이트 등)을 트리거로 사용하여 **Cloud Functions를 실행**할 수 있습니다.  
* 예를 들어, 이미지 파일이 업로드되면 자동으로 썸네일을 생성하거나, 파일 내용을 분석하는 등의 작업을 수행할 수 있습니다.

현제는 사용처가 안보이나  
만약 사용할 필요가 있는경우 기획서 수정 필요  
