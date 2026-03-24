# PRD: Phase 1 — MVP

> AI 성향 분석 + 취미/원데이클래스 추천 API

## 목표

사용자가 질문에 답하면 Claude API로 성향을 분석하고, 맞춤 취미/원데이클래스를 추천하는 BE API를 완성한다.
Phase 1은 **API 단독 동작** 가능한 수준까지 구현한다 (FE 없이 Swagger로 테스트 가능).

---

## 핵심 플로우

```
[1] 세션 시작
    POST /api/v1/sessions
    → 기본 질문 7개 반환

[2] 기본 답변 제출
    POST /api/v1/sessions/{id}/answers
    → Claude API: 답변 분석 → 꼬리질문 3~5개 생성
    → 꼬리질문 반환

[3] 꼬리질문 답변 제출
    POST /api/v1/sessions/{id}/follow-up
    → Claude API: 전체 답변 종합 분석
    → 성향 프로필 + 추천 5개 생성 및 저장
    → 프로필 + 추천 반환

[4] 결과 조회 (캐싱)
    GET /api/v1/sessions/{id}/profile
    GET /api/v1/sessions/{id}/recommendations
```

---

## 도메인 모델

### 1. Session (세션)

분석 한 건의 단위. 사용자의 전체 질의응답 흐름을 추적한다.

| 필드 | 타입 | 설명 |
|------|------|------|
| id | UUID | PK |
| status | Enum | `CREATED` → `BASIC_ANSWERED` → `COMPLETED` |
| createdAt | LocalDateTime | 세션 시작 시각 |
| completedAt | LocalDateTime | 분석 완료 시각 (nullable) |

**상태 전이:**
```
CREATED → (기본 답변 제출) → BASIC_ANSWERED → (꼬리질문 답변 제출) → COMPLETED
```

### 2. Question (질문)

기본 질문 7개는 시스템에 고정 (DB 저장 또는 상수).
꼬리질문은 Claude API가 동적 생성.

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| sessionId | UUID | FK → Session |
| type | Enum | `BASIC` / `FOLLOW_UP` |
| content | String | 질문 내용 |
| orderIndex | int | 질문 순서 |

**기본 질문 예시 (7개, 고정):**
1. 혼자 시간을 보낼 때 주로 뭘 하나요?
2. 새로운 걸 배울 때 어떤 방식을 선호하나요? (영상 / 책 / 직접 해보기)
3. 실내 vs 야외, 어디서 활동하는 게 더 편한가요?
4. 사람들과 함께하는 활동 vs 혼자 집중하는 활동, 어느 쪽을 선호하나요?
5. 손으로 만드는 걸 좋아하나요? (요리, 공예, 그림 등)
6. 운동이나 신체 활동에 관심이 있나요? 있다면 어떤 종류?
7. 최근에 "이거 해보고 싶다"고 떠오른 게 있나요?

### 3. Answer (답변)

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| sessionId | UUID | FK → Session |
| questionId | Long | FK → Question |
| content | String | 사용자 답변 텍스트 |
| answeredAt | LocalDateTime | 답변 시각 |

### 4. Profile (성향 프로필)

Claude API가 전체 답변을 분석하여 생성한 결과.

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| sessionId | UUID | FK → Session (1:1) |
| typeLabel | String | 성향 타입 라벨 (예: "감성적 탐험가") |
| description | String | 성향 설명 (2~3문장) |
| keywords | List\<String\> | 키워드 태그 (예: ["창의적", "야외활동", "사교적"]) |
| traits | JSON | 레이더 차트 데이터 (5축) |
| createdAt | LocalDateTime | 생성 시각 |

**traits 구조 (레이더 차트 5축):**
```json
{
  "creativity": 85,
  "sociability": 60,
  "activity": 70,
  "exploration": 90,
  "focus": 45
}
```

| 축 | 설명 |
|-----|------|
| creativity | 창의성 — 만들기/표현 선호도 |
| sociability | 사교성 — 함께 vs 혼자 |
| activity | 활동성 — 신체 활동 선호도 |
| exploration | 탐험성 — 새로운 경험 추구 |
| focus | 집중력 — 몰입/정밀 작업 선호도 |

### 5. Recommendation (추천)

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| sessionId | UUID | FK → Session |
| hobbyName | String | 취미/클래스 이름 |
| category | String | 카테고리 (예: "공예", "운동", "음악") |
| matchScore | int | 매칭 점수 (0~100) |
| reason | String | 추천 이유 (1~2문장) |
| classUrl | String | 실제 클래스 링크 (nullable, Phase 2 크롤링) |
| orderIndex | int | 추천 순서 |

---

## API 상세

### POST /api/v1/sessions

세션을 생성하고 기본 질문 7개를 반환한다.

**Response 200:**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "questions": [
    { "id": 1, "content": "혼자 시간을 보낼 때 주로 뭘 하나요?", "orderIndex": 1 },
    ...
  ]
}
```

### POST /api/v1/sessions/{id}/answers

기본 질문 7개에 대한 답변을 제출한다. Claude API가 분석하여 꼬리질문을 생성한다.

**Request:**
```json
{
  "answers": [
    { "questionId": 1, "content": "넷플릭스 보거나 산책해요" },
    { "questionId": 2, "content": "직접 해보면서 배우는 편이에요" },
    ...
  ]
}
```

**Response 200:**
```json
{
  "followUpQuestions": [
    { "id": 8, "content": "산책할 때 주로 어디를 걷나요? 자연 속? 도심?", "orderIndex": 1 },
    { "id": 9, "content": "넷플릭스에서 주로 어떤 장르를 보나요?", "orderIndex": 2 },
    ...
  ]
}
```

**Validation:**
- 세션 상태가 `CREATED`여야 함
- 답변 개수 = 기본 질문 수 (7개)
- 각 답변 content는 비어있지 않음

### POST /api/v1/sessions/{id}/follow-up

꼬리질문 답변을 제출하면, 전체 답변을 종합하여 프로필 + 추천을 생성한다.

**Request:**
```json
{
  "answers": [
    { "questionId": 8, "content": "한강 공원이나 숲길을 좋아해요" },
    ...
  ]
}
```

**Response 200:**
```json
{
  "profile": {
    "typeLabel": "감성적 탐험가",
    "description": "자연 속에서 새로운 경험을 즐기며...",
    "keywords": ["자연", "탐험", "감성"],
    "traits": {
      "creativity": 85,
      "sociability": 60,
      "activity": 70,
      "exploration": 90,
      "focus": 45
    }
  },
  "recommendations": [
    {
      "hobbyName": "도시 스케칭",
      "category": "예술",
      "matchScore": 92,
      "reason": "산책을 좋아하고 감성적인 성향에 잘 맞습니다",
      "orderIndex": 1
    },
    ...
  ]
}
```

**Validation:**
- 세션 상태가 `BASIC_ANSWERED`여야 함
- 답변 개수 = 꼬리질문 수

### GET /api/v1/sessions/{id}/profile

완료된 세션의 프로필을 조회한다. Redis 캐시 적용.

**Response 200:** profile 객체
**Response 404:** 세션 없음 또는 미완료

### GET /api/v1/sessions/{id}/recommendations

완료된 세션의 추천 목록을 조회한다. Redis 캐시 적용.

**Response 200:** recommendations 배열
**Response 404:** 세션 없음 또는 미완료

---

## 공통 에러 응답

RFC 7807 Problem Details 형식:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "세션 상태가 올바르지 않습니다. 현재: COMPLETED, 필요: BASIC_ANSWERED",
  "instance": "/api/v1/sessions/550e.../follow-up"
}
```

| 상황 | Status | Detail |
|------|--------|--------|
| 세션 없음 | 404 | 세션을 찾을 수 없습니다 |
| 잘못된 상태 전이 | 400 | 세션 상태가 올바르지 않습니다 |
| 답변 수 불일치 | 400 | 답변 개수가 질문 수와 일치하지 않습니다 |
| Claude API 장애 | 503 | AI 서비스가 일시적으로 불가합니다 |
| Rate Limit | 429 | 요청이 너무 많습니다 |

---

## Claude API 연동

### 프롬프트 전략

**꼬리질문 생성 (Step 2):**
- Input: 기본 질문 7개 + 답변
- Output: 꼬리질문 3~5개 (JSON)
- 프롬프트: 답변에서 모호하거나 더 탐색할 부분을 파고드는 질문 생성

**프로필 + 추천 생성 (Step 3):**
- Input: 기본 질문 + 답변 + 꼬리질문 + 답변 전체
- Output: 프로필(타입/설명/키워드/traits) + 추천 5개 (JSON)
- 프롬프트: 성향 분석 후 구체적이고 실현 가능한 취미/클래스 추천

### Resilience 전략

```
Claude API 호출
├─ Timeout: 30초
├─ Retry: 최대 3회 (exponential backoff: 1s → 2s → 4s)
├─ Circuit Breaker: 5회 연속 실패 → OPEN (30초 후 HALF_OPEN)
└─ Fallback: 유사 성향 캐시된 결과 반환 (Redis)
```

---

## 캐싱 전략 (Redis)

| 대상 | Key 패턴 | TTL | 용도 |
|------|----------|-----|------|
| 프로필 조회 | `profile:{sessionId}` | 24시간 | GET 결과 캐싱 |
| 추천 조회 | `recommendations:{sessionId}` | 24시간 | GET 결과 캐싱 |
| Rate Limit | `ratelimit:{ip}` | 1분 | IP당 분당 10회 제한 |

---

## 패키지 구조 (구현 대상)

```
com.findmyvibe/
├── api/
│   ├── controller/
│   │   └── SessionController.java
│   ├── dto/
│   │   ├── request/
│   │   │   ├── SubmitAnswersRequest.java
│   │   │   └── SubmitFollowUpRequest.java
│   │   └── response/
│   │       ├── CreateSessionResponse.java
│   │       ├── FollowUpQuestionsResponse.java
│   │       ├── ProfileResponse.java
│   │       └── RecommendationsResponse.java
│   └── GlobalExceptionHandler.java
│
├── domain/
│   ├── entity/
│   │   ├── Session.java
│   │   ├── Question.java
│   │   ├── Answer.java
│   │   ├── Profile.java
│   │   └── Recommendation.java
│   ├── repository/
│   │   ├── SessionRepository.java
│   │   ├── QuestionRepository.java
│   │   ├── AnswerRepository.java
│   │   ├── ProfileRepository.java
│   │   └── RecommendationRepository.java
│   ├── service/
│   │   ├── SessionService.java
│   │   └── AnalysisService.java
│   └── enums/
│       ├── SessionStatus.java
│       └── QuestionType.java
│
├── infrastructure/
│   ├── claude/
│   │   ├── ClaudeClient.java
│   │   ├── ClaudePromptBuilder.java
│   │   └── ClaudeResponseParser.java
│   └── redis/
│       └── RedisCacheService.java
│
└── common/
    ├── config/
    │   ├── WebConfig.java          (CORS)
    │   ├── RedisConfig.java
    │   └── ResilienceConfig.java
    └── exception/
        ├── SessionNotFoundException.java
        ├── InvalidSessionStateException.java
        └── ClaudeApiException.java
```

---

## DB 마이그레이션 (Flyway)

### V1__create_tables.sql

```sql
CREATE TABLE sessions (
    id          UUID PRIMARY KEY,
    status      VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP
);

CREATE TABLE questions (
    id          BIGSERIAL PRIMARY KEY,
    session_id  UUID NOT NULL REFERENCES sessions(id),
    type        VARCHAR(10) NOT NULL,  -- BASIC / FOLLOW_UP
    content     TEXT NOT NULL,
    order_index INT NOT NULL
);

CREATE TABLE answers (
    id          BIGSERIAL PRIMARY KEY,
    session_id  UUID NOT NULL REFERENCES sessions(id),
    question_id BIGINT NOT NULL REFERENCES questions(id),
    content     TEXT NOT NULL,
    answered_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE profiles (
    id          BIGSERIAL PRIMARY KEY,
    session_id  UUID NOT NULL UNIQUE REFERENCES sessions(id),
    type_label  VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    keywords    JSONB NOT NULL DEFAULT '[]',
    traits      JSONB NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE recommendations (
    id          BIGSERIAL PRIMARY KEY,
    session_id  UUID NOT NULL REFERENCES sessions(id),
    hobby_name  VARCHAR(100) NOT NULL,
    category    VARCHAR(50) NOT NULL,
    match_score INT NOT NULL CHECK (match_score BETWEEN 0 AND 100),
    reason      TEXT NOT NULL,
    class_url   VARCHAR(500),
    order_index INT NOT NULL
);

CREATE INDEX idx_questions_session ON questions(session_id);
CREATE INDEX idx_answers_session ON answers(session_id);
CREATE INDEX idx_recommendations_session ON recommendations(session_id);
```

---

## 테스트 전략

| 레이어 | 테스트 종류 | 도구 | 목표 |
|--------|------------|------|------|
| Controller | 통합 테스트 | MockMvc + TestContainers | 전체 API 흐름 검증 |
| Service | 단위 테스트 | JUnit 5 + Mockito | 비즈니스 로직 |
| Claude Client | 단위 테스트 | WireMock | API 응답 모킹 |
| Repository | 슬라이스 테스트 | @DataJpaTest + H2 | 쿼리 검증 |
| Architecture | ArchUnit | ArchUnit | 의존성 규칙 강제 |
| Redis | 통합 테스트 | TestContainers | 캐시 동작 검증 |

**목표: 테스트 60개+**

---

## 구현 순서 (제안)

### Step 1: 도메인 + DB (Day 1)
- Entity 5개 + Enum 2개
- Repository 5개
- Flyway 마이그레이션
- ArchUnit 의존성 규칙 테스트

### Step 2: 세션 시작 API (Day 1~2)
- `POST /api/v1/sessions`
- SessionService + SessionController
- 기본 질문 7개 상수 정의
- GlobalExceptionHandler (RFC 7807)
- 통합 테스트

### Step 3: Claude API 연동 (Day 2~3)
- ClaudeClient + RestClient 기반 호출
- ClaudePromptBuilder (프롬프트 템플릿)
- ClaudeResponseParser (JSON 파싱)
- WireMock 기반 단위 테스트
- Resilience4j 설정 (Retry + Circuit Breaker)

### Step 4: 답변 제출 + 분석 API (Day 3~4)
- `POST /api/v1/sessions/{id}/answers` → 꼬리질문 생성
- `POST /api/v1/sessions/{id}/follow-up` → 프로필 + 추천 생성
- AnalysisService
- 상태 전이 검증
- 통합 테스트

### Step 5: 조회 API + 캐싱 (Day 4~5)
- `GET /api/v1/sessions/{id}/profile`
- `GET /api/v1/sessions/{id}/recommendations`
- Redis 캐시 적용
- Rate Limiting (IP 기반)
- 통합 테스트

### Step 6: 마무리 (Day 5)
- Swagger 문서 정리 (springdoc 어노테이션)
- docker-compose.yml (PostgreSQL + Redis)
- CORS 설정
- 전체 테스트 60개+ 확인
