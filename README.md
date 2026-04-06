# 🎙️ AI 영어 소통 학습 앱 - SenTic (Backend)

> 음성 또는 채팅 방식으로 AI와 상황별 영어 대화를 하며 실생활 영어 표현을 익히는 학습 앱

---

## 🛠️ 기술 스택

| 파트 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.x |
| Database | MySQL 8.x |
| 배포 | Cloudtype |
| AI | OpenAI GPT-4o / Whisper (STT) / TTS |
| 인증 | JWT + OAuth2 (카카오, 구글) |
| 파일 저장 | AWS S3 |
| 이메일 | AWS SES |
| 결제 | 토스페이먼츠 |
| API 문서 | Swagger (SpringDoc) |

---

## 📁 패키지 구조

```
com.project.sentic
├── domain
│   ├── admin       # 관리자
│   ├── feedback    # 피드백
│   ├── message     # 대화 메시지
│   ├── payment     # 결제
│   ├── room        # 대화방
│   ├── scrap       # 스크랩
│   └── user        # 회원
├── global
│   ├── auth        # JWT, Security
│   ├── common      # ApiResponse, BaseTimeEntity
│   ├── config      # SwaggerConfig, SecurityConfig
│   ├── controller  # HealthCheckController
│   └── exception   # ErrorCode, CustomException, GlobalExceptionHandler
└── infra
    └── ai          # OpenAI, STT, TTS 연동
```

---

## 🗄️ DB 테이블 목록

| 테이블 | 설명 |
|--------|------|
| users | 회원 정보 |
| user_settings | 사용자 설정 |
| subscription_plans | 이용권 플랜 |
| payments | 결제 정보 |
| rooms | 대화방 |
| messages | 대화 메시지 |
| feedbacks | 피드백 |
| scraps | 스크랩 표현 |
| announcements | 공지사항 |

---

## 🌿 브랜치 전략

```
main
  └── develop
        └── feature/기능명
```

| 브랜치 | 설명 |
|--------|------|
| `main` | 배포 전용. 직접 push 금지 |
| `develop` | 개발 통합. PR로만 머지 |
| `feature/*` | 기능 단위 개발 |

- PR 머지 방식: **Squash and Merge**
- PR 머지 조건: 팀원 1명 이상 리뷰

---

## ✍️ 커밋 컨벤션

```
feat: 새 기능 추가
fix: 버그 수정
chore: 설정, 의존성 변경
refactor: 리팩토링
docs: 문서 수정
test: 테스트 코드
style: 코드 포맷
```

예시: `feat(auth): 카카오 소셜 로그인 API 구현`

---

## ⚙️ 로컬 개발 환경 세팅

### 1. 레포 클론
```bash
git clone https://github.com/SenTic-DMU/Backend.git
cd Backend
```

### 2. application-dev.yml 생성
`src/main/resources/` 에 `application-dev.yml` 파일 생성 후 아래 내용 작성:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/sentic?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
    username: root
    password: 본인_MySQL_비밀번호
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    show-sql: true

jwt:
  secret: 본인_JWT_시크릿_키_Base64인코딩
  access-token-expiry: 1800000
  refresh-token-expiry: 604800000

ai:
  openai:
    api-key: 본인_OpenAI_API_키
    base-url: https://api.openai.com/v1
    chat-model: gpt-4o
    tts-model: tts-1
    stt-model: whisper-1

oauth2:
  kakao:
    client-id: 카카오_클라이언트_ID
    user-info-url: https://kapi.kakao.com/v2/user/me
  google:
    client-id: 구글_클라이언트_ID
    user-info-url: https://www.googleapis.com/oauth2/v3/userinfo
```

### 3. MySQL DB 생성
```sql
CREATE DATABASE sentic CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sentic;
source src/main/resources/schema.sql
```

### 4. 서버 실행
```bash
./gradlew bootRun
```

### 5. Swagger 접속
```
http://localhost:8080/swagger-ui.html
```

---

## 📋 API 응답 형식

```json
{
  "success": true,
  "message": "success",
  "data": {}
}
```

---

## 👥 팀원

| 이름 | 역할 |
|------|------|
| 김민정 | Spring Security/JWT, 회원가입/로그인, 마이페이지, AI 연동, 채팅/피드백/스크랩/결제 API |
| 김민석 | 소셜 로그인, 방 관리, TTS 연동, 대화 기록, AWS S3, 공지사항 API |