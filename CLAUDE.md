# SenTic 백엔드 프로젝트 지침

## 프로젝트 개요
AI 영어 소통 학습 앱 SenTic의 백엔드 서버입니다.
음성/채팅으로 AI와 영어 대화를 하며 실생활 영어 표현을 익히는 앱입니다.

## 기술 스택
- **Language**: Java 21
- **Framework**: Spring Boot 3.5.13
- **Database**: MySQL 8.x
- **Build Tool**: Gradle
- **배포**: Cloudtype
- **AI**: OpenAI GPT-4o, Whisper(STT), TTS
- **소셜 로그인**: 카카오, 구글
- **파일 저장**: AWS S3
- **이메일**: Gmail SMTP (개발) / AWS SES (운영)
- **결제**: 토스페이먼츠

## GitHub
- **Organization**: SenTic-DMU
- **백엔드 레포**: SenTic-DMU/Backend
- **프론트엔드 레포**: SenTic-DMU/Frontend

## 패키지 구조
com.project.sentic
├── domain
│   ├── faq
│   ├── feedback
│   ├── message
│   ├── payment
│   ├── room
│   ├── scrap
│   └── user
├── global
│   ├── auth        # JwtTokenProvider, JwtAuthenticationFilter, SecurityConfig
│   ├── common      # ApiResponse, BaseTimeEntity
│   ├── config      # SwaggerConfig, CorsConfig
│   ├── controller  # HealthCheckController
│   └── exception   # ErrorCode, CustomException, GlobalExceptionHandler
└── infra
├── ai          # OpenAI, STT, TTS 연동
├── email       # Gmail SMTP 연동
├── kakao       # 카카오 사용자 정보 API
├── google      # 구글 사용자 정보 API
└── s3 

## 브랜치 전략
main
└── develop
└── feature/기능명

- main 직접 push ❌ 절대 금지
- develop 직접 push ❌ PR로만 머지
- feature 브랜치 네이밍: feature/기능명 (예: feature/auth, feature/room)
- PR 머지 방식: Squash and Merge
- PR 머지 조건: 팀원 1명 이상 리뷰 후 머지

## 커밋 컨벤션 (Conventional Commits)
feat: 새 기능 추가
fix: 버그 수정
chore: 설정, 의존성 변경
refactor: 리팩토링
docs: 문서 수정
test: 테스트 코드
style: 코드 포맷

예시: feat(auth): 카카오 소셜 로그인 API 구현

## 작업 흐름
1. 작업 시작 전 git pull origin develop
2. feature 브랜치 생성 후 작업
3. 작업 완료 후 push
4. GitHub에서 PR 생성 → develop으로 머지
5. Jira 티켓 완료 처리

## 코딩 컨벤션
- 들여쓰기: 4 spaces
- 클래스명: PascalCase (예: UserService)
- 메서드명/변수명: camelCase (예: findUserById)
- 상수명: UPPER_SNAKE_CASE (예: MAX_TOKEN_COUNT)
- 패키지명: 소문자 (예: com.project.sentic)
- 한 줄 최대 길이: 120자

## API 응답 형식
모든 API는 ApiResponse로 통일된 형태로 응답합니다.
{
"success": true,
"message": "success",
"data": {}
}

## 예외 처리
- 모든 예외는 CustomException + ErrorCode로 처리
- 새로운 에러 상황은 ErrorCode enum에 추가
  throw new CustomException(ErrorCode.USER_NOT_FOUND);

## DB 정보
- DB명: sentic
- 테이블 목록: users, user_settings, subscription_plans, payments, rooms, messages, feedbacks, scraps, announcements, faqs
- 스키마 파일: src/main/resources/schema.sql

## feedbacks 테이블 컬럼
- feedback_id, room_id, message_id
- word_errors (JSON)       → 단어 오류 (빨간색 하이라이트)
- grammar_errors (JSON)    → 문법 오류 (형광펜 하이라이트)
- expression_errors (JSON) → 자연스러운 표현 추천
- perfect_sentence (TEXT)  → 종합된 완벽한 문장
- created_at

## JSON 구조 (word_errors, grammar_errors)
[
{
"original": "large",
"corrected": "a large",
"explanation": "관사 누락",
"startIndex": 15,
"endIndex": 20
}
]

## JSON 구조 (expression_errors)
[
{
"original": "I want to order",
"suggested": "I'd like to order",
"explanation": "더 정중하고 자연스러운 표현",
"startIndex": 0,
"endIndex": 15
}
]

## 소셜 로그인 방식
- 방식 B: 프론트에서 SDK로 accessToken 받아서 백엔드로 전달
- POST /api/auth/kakao, POST /api/auth/google
- 백엔드에서 카카오/구글 서버에 재검증 후 SenTic JWT 발급

## AI 아키텍처
### 프롬프트 구조 (3레이어)
[SYSTEM - 고정]
캐릭터 / 상황 / 난이도 / 페르소나 이탈 방지 규칙
PromptBuilder가 자동 생성

[MEMORY BANK - 동적]
중요 정보 추출 저장 (rooms.memory_bank JSON 컬럼)
GPT 호출 1턴당 2회 (대화 + 메모리 추출)

[SLIDING WINDOW - 최근 10개]
최근 대화 기록 그대로 전달

## 환경 설정
- application.yml: 공통 설정 (민감 정보 제외)
- application-dev.yml: 로컬 개발 환경 (gitignore 처리 - 직접 생성 필요)
- application-prod.yml: 운영 환경 (Cloudtype 환경변수 참조)

## application-dev.yml 필수 항목
spring:
datasource:
url: jdbc:mysql://localhost:3306/sentic?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
username: root
password: {MySQL비밀번호}
mail:
host: smtp.gmail.com
port: 587
username: {Gmail주소}
password: {Gmail앱비밀번호}
from: {Gmail주소}

jwt:
secret: {Base64시크릿키}
access-token-expiry: 1800000
refresh-token-expiry: 604800000

ai:
openai:
api-key: {OpenAI키}

oauth2:
kakao:
client-id: {카카오클라이언트ID}
google:
client-id: {구글클라이언트ID}

## Swagger
- 로컬 접속 주소: http://localhost:8080/swagger-ui.html
- 프론트팀 API 명세서 공유용

## 팀원 역할 분배
### 김민정
- Spring Security + JWT ✅
- 회원가입 / 로그인 / 로그아웃 API ✅
- 아이디 찾기 / 비밀번호 재설정 API ✅
- 회원 탈퇴 API ✅
- 카카오 / 구글 소셜 로그인 API ✅
- 마이페이지 API ✅
- OpenAI GPT-4o 채팅 API ✅ (PromptBuilder, Memory Bank, Sliding Window)
- 채팅 피드백 생성 API
- 스크랩 API
- 결제 API (토스페이먼츠)
- 관리자 회원 관리 API

### 김민석
- User 엔티티 + Repository ✅
- Room 엔티티 + Repository ✅
- 방 생성 / 목록 / 삭제 API ✅
- TTS 연동 ✅
- Message 엔티티 + Repository ✅
- 음성 대화 API ✅
- UserSettings API ✅
- FAQ API ✅
- 대화 기록 조회 API
- 음성 피드백 생성 API
- 스크랩 API
- AWS S3 파일 저장
- 공지사항 CRUD API