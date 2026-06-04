# SenTic 백엔드 프로젝트 지침

## 프로젝트 개요
AI 영어 소통 학습 앱 SenTic의 백엔드 서버입니다.
음성/채팅으로 AI와 영어 대화를 하며 실생활 영어 표현을 익히는 앱입니다.

## 기술 스택
- **Language**: Java 21
- **Framework**: Spring Boot 3.5.x
- **Database**: MySQL 8.x
- **Build Tool**: Gradle
- **배포**: Cloudtype
- **AI**: OpenAI GPT-4o, Whisper(STT), TTS
- **소셜 로그인**: 카카오, 구글 OAuth2
- **파일 저장**: AWS S3
- **이메일**: AWS SES
- **결제**: 토스페이먼츠

## 패키지 구조
```
com.project.sentic
├── domain
│   ├── admin
│   ├── feedback
│   ├── message
│   ├── payment
│   ├── room
│   ├── scrap
│   └── user
├── global
│   ├── auth        # JwtTokenProvider, JwtAuthenticationFilter
│   ├── common      # ApiResponse, BaseTimeEntity
│   ├── config      # SwaggerConfig, SecurityConfig
│   ├── controller  # HealthCheckController
│   └── exception   # ErrorCode, CustomException, GlobalExceptionHandler
└── infra
    └── ai          # OpenAI, STT, TTS 연동
```

## 브랜치 전략
```
main
  └── develop
        └── feature/기능명
```
- `main` 직접 push 절대 금지
- `develop` 직접 push 금지 → PR로만 머지
- feature 브랜치 네이밍: `feature/기능명` (예: feature/auth, feature/room)
- PR 머지 방식: Squash and Merge
- PR 머지 조건: 팀원 1명 이상 리뷰 후 머지

## 커밋 컨벤션 (Conventional Commits)
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

## API 응답 형식
모든 API는 ApiResponse로 통일된 형태로 응답합니다.
```json
{
  "success": true,
  "message": "success",
  "data": {}
}
```

## 예외 처리 규칙
- 모든 예외는 CustomException + ErrorCode로 처리
- 새로운 에러 상황은 ErrorCode enum에 추가
```java
throw new CustomException(ErrorCode.USER_NOT_FOUND);
```

## 코딩 컨벤션
- 들여쓰기: 4 spaces
- 클래스명: PascalCase
- 메서드명/변수명: camelCase
- 상수명: UPPER_SNAKE_CASE
- 패키지명: 소문자
- 한 줄 최대 길이: 120자

---

## User 엔티티 핵심 정보
```java
// com.project.sentic.domain.user.entity.User
@Column(name = "user_id") private Long id;  // PK
private String email;
private String password;       // 소셜 로그인은 NULL
private String nickname;
private String profileImage;   // S3 URL
Provider provider;             // LOCAL / KAKAO / GOOGLE
String providerId;
Role role;                     // USER / ADMIN
Status status;                 // ACTIVE / INACTIVE / BANNED
boolean emailVerified;
LocalDateTime deletedAt;

// 정적 생성 메서드
public static User createLocalUser(String email, String encodedPassword, String nickname)
// 수정 메서드
public void updateNickname(String nickname)
public void updateProfileImage(String profileImage)
public void updatePassword(String encodedPassword)
public void verifyEmail()
public void deactivate()
```

## UserRepository 메서드
```java
Optional<User> findByEmail(String email);
Optional<User> findByProviderAndProviderId(Provider provider, String providerId);
boolean existsByEmail(String email);
boolean existsByNickname(String nickname);
```

## DB 테이블 목록
- users, user_settings, subscription_plans, payments
- rooms, messages, feedbacks, scraps, announcements

## rooms 테이블 주요 컬럼
```sql
room_id, user_id, room_type(VOICE/CHAT), room_name,
character_name, participant_count, situation, difficulty(BEGINNER/INTERMEDIATE/ADVANCED),
is_random, last_active_at, is_deleted, created_at
```

## messages 테이블 주요 컬럼
```sql
message_id, room_id, sender_type(USER/AI),
content_text, audio_url, sequence_no, created_at
```

---

## 현재 완료된 작업
- [x] BE-1: 공통 클래스 (ApiResponse, BaseTimeEntity, ErrorCode, CustomException, GlobalExceptionHandler)
- [x] BE-2: application.yml 설정
- [x] BE-3: MySQL DB 생성 (sentic DB, 9개 테이블)
- [x] BE-4: User 엔티티 + UserRepository (팀원 김민석)
- [x] BE-5: Spring Security + JWT (JwtTokenProvider, JwtAuthenticationFilter, SecurityConfig)
- [x] BE-6: 회원가입 / 로그인 / 로그아웃 API (AuthService, AuthController, DTOs)

## 다음 작업 목록
- [ ] BE-7: 카카오 소셜 로그인 API (담당: 김민석)
- [ ] BE-8: 구글 소셜 로그인 API (담당: 김민석)
- [ ] BE-9: 아이디 찾기 / 비밀번호 재설정 API (담당: 김민정)
- [ ] BE-10: 회원 탈퇴 API (담당: 김민정)
- [ ] BE-11: 마이페이지 조회 API (담당: 김민정)
- [ ] BE-12: 마이페이지 수정 API (담당: 김민정)
- [ ] BE-13: 학습 레벨 설정 API (담당: 김민정)
- [ ] BE-14: 방 생성 / 목록 API (담당: 김민석)
- [ ] BE-15: 방 삭제 API (담당: 김민석)

---

## AI 아키텍처 결정사항

### 페르소나 유지 전략
**Sliding Window + Memory Bank** 채택:
- 고정 시스템 프롬프트: 캐릭터/상황/난이도 절대 고정, 매 API 호출 시 항상 맨 위에 포함
- Memory Bank: rooms 테이블 memory_bank JSON 컬럼에 저장 (중요 정보 영구 보존)
- Sliding Window: 최근 10개 대화만 전달

### 프롬프트 구조
```
[SYSTEM - 항상 고정]
캐릭터 / 상황 / 난이도 / 페르소나 이탈 방지 규칙

[MEMORY BANK - 동적 업데이트]
중요 정보 추출 저장 (사용자 이름, 주문 내역, 미완료 사항 등)

[SLIDING WINDOW - 최근 10개]
최근 대화 기록

[CURRENT]
현재 메시지
```

### 응답 속도 대책
- Spring @Async 비동기 병렬 처리 (GPT 호출 + DB 저장 동시)
- GPT-4o 스트리밍 응답 적용
- 토큰 최적화 (최근 10개 제한)
- 단계별 로딩 UX

### 난이도 기준
- BEGINNER: 짧고 쉬운 단어, 기초 표현
- INTERMEDIATE: 일상 대화 수준
- ADVANCED: 관용어, 슬랭, 복잡한 문법
- 난이도-상황 충돌 시: 경고 메시지 + AI 자동 언어 수준 조절

### 다중 캐릭터 구현 방법
- 하나의 GPT-4o가 여러 역할 담당 (방법 A)
- rooms 테이블 characters JSON 컬럼 사용
- 응답 형식: [CharacterName]: 내용

---

## 환경 설정
- `application.yml`: 공통 설정
- `application-dev.yml`: 로컬 개발 환경 (gitignore - 직접 생성 필요)
- `application-prod.yml`: 운영 환경 (Cloudtype 환경변수)

### application-dev.yml 형식
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/sentic?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
    username: root
    password: 본인_비밀번호
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    show-sql: true

jwt:
  secret: Base64인코딩된_시크릿키
  access-token-expiry: 1800000
  refresh-token-expiry: 604800000

ai:
  openai:
    api-key: OpenAI_API_키
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

## Swagger
- 로컬: http://localhost:8080/swagger-ui.html

## 팀원 역할
- **김민정(BE)**: Security/JWT, 회원가입/로그인, 마이페이지, AI 연동, 채팅/피드백/스크랩/결제 API
- **김민석(BE)**: 소셜 로그인, 방 관리, TTS, AWS S3, 대화 기록, 공지사항 API
