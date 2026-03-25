# Flyway — DB 마이그레이션 도구

## 한 줄 요약

**Flyway는 DB 스키마 변경을 SQL 파일로 관리하는 버전 관리 도구다.**
Git이 코드 변경 이력을 추적하듯, Flyway는 DB 테이블 변경 이력을 추적한다.

---

## 왜 필요한가?

### Flyway 없이 개발하면 생기는 문제

```
개발자 A: "나 sessions 테이블에 컬럼 추가했어"
개발자 B: "어떤 컬럼? 내 로컬 DB엔 없는데?"
개발자 A: "아 직접 ALTER TABLE 날려야 해"
서버 배포: "운영 DB에도 누가 수동으로 ALTER TABLE 해야 하나?"
```

→ **수동 DB 관리는 실수가 생기고, 환경마다 스키마가 달라지는 문제가 발생한다.**

### Flyway가 해결하는 것

1. **DB 스키마를 코드로 관리** — SQL 파일을 Git에 커밋
2. **자동 실행** — 앱 시작 시 아직 적용 안 된 마이그레이션을 자동 실행
3. **환경 일관성** — 로컬/스테이징/운영 DB가 항상 같은 상태
4. **이력 추적** — 어떤 변경이 언제 적용됐는지 기록

---

## 동작 원리

### 1. 마이그레이션 파일 네이밍 규칙

```
V{버전}__{설명}.sql
```

| 예시 | 설명 |
|------|------|
| `V1__create_tables.sql` | 최초 테이블 생성 |
| `V2__add_session_ip_column.sql` | sessions에 ip 컬럼 추가 |
| `V3__create_tags_table.sql` | 새 테이블 추가 |

- `V` 접두사 + 숫자 = 버전 (순서대로 실행)
- `__` (언더스코어 2개) = 버전과 설명 구분자
- 설명은 사람이 읽기 위한 것 (Flyway는 버전 숫자만 본다)

### 2. 실행 흐름

```
앱 시작
  ↓
Flyway: "flyway_schema_history 테이블 확인"
  ↓
이미 적용된 버전: V1, V2
아직 미적용: V3
  ↓
V3__create_tags_table.sql 실행
  ↓
flyway_schema_history에 V3 기록
  ↓
완료 — 앱 정상 시작
```

### 3. flyway_schema_history 테이블

Flyway가 자동 생성하는 메타 테이블. 어떤 마이그레이션이 언제 적용됐는지 기록한다.

```
| version | description     | installed_on        | success |
|---------|-----------------|---------------------|---------|
| 1       | create tables   | 2026-03-25 09:00:00 | true    |
| 2       | add ip column   | 2026-03-26 10:00:00 | true    |
```

---

## 우리 프로젝트에서의 적용

### 파일 위치

```
be/src/main/resources/db/migration/
└── V1__create_tables.sql    ← 여기에 작성
```

Spring Boot는 이 경로를 자동으로 스캔한다 (별도 설정 불필요).

### V1__create_tables.sql 내용

PRD에 정의된 5개 Entity를 테이블로 생성하는 SQL:

```sql
-- Session: 분석 한 건의 단위
CREATE TABLE sessions (
    id           UUID PRIMARY KEY,
    status       VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP
);

-- Question: 기본 질문 7개 + Claude가 생성하는 꼬리질문
CREATE TABLE questions (
    id           BIGSERIAL PRIMARY KEY,
    session_id   UUID NOT NULL REFERENCES sessions(id),
    type         VARCHAR(10) NOT NULL,   -- BASIC / FOLLOW_UP
    content      TEXT NOT NULL,
    order_index  INT NOT NULL
);

-- Answer: 사용자의 답변
CREATE TABLE answers (
    id           BIGSERIAL PRIMARY KEY,
    session_id   UUID NOT NULL REFERENCES sessions(id),
    question_id  BIGINT NOT NULL REFERENCES questions(id),
    content      TEXT NOT NULL,
    answered_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Profile: Claude가 분석한 성향 프로필 (Session과 1:1)
CREATE TABLE profiles (
    id           BIGSERIAL PRIMARY KEY,
    session_id   UUID NOT NULL UNIQUE REFERENCES sessions(id),
    type_label   VARCHAR(50) NOT NULL,
    description  TEXT NOT NULL,
    keywords     JSONB NOT NULL DEFAULT '[]',
    traits       JSONB NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Recommendation: 추천 취미/클래스 (Session당 5개)
CREATE TABLE recommendations (
    id           BIGSERIAL PRIMARY KEY,
    session_id   UUID NOT NULL REFERENCES sessions(id),
    hobby_name   VARCHAR(100) NOT NULL,
    category     VARCHAR(50) NOT NULL,
    match_score  INT NOT NULL CHECK (match_score BETWEEN 0 AND 100),
    reason       TEXT NOT NULL,
    class_url    VARCHAR(500),
    order_index  INT NOT NULL
);

-- 조회 성능을 위한 인덱스
CREATE INDEX idx_questions_session ON questions(session_id);
CREATE INDEX idx_answers_session ON answers(session_id);
CREATE INDEX idx_recommendations_session ON recommendations(session_id);
```

### Entity와 SQL의 관계

| Java Entity | DB Table | 매핑 방식 |
|-------------|----------|-----------|
| `Session.java` (`@Table(name = "sessions")`) | `sessions` | UUID PK, status는 Enum→VARCHAR |
| `Question.java` | `questions` | session_id FK, BIGSERIAL = auto increment |
| `Answer.java` | `answers` | question_id FK |
| `Profile.java` | `profiles` | UNIQUE(session_id) = 1:1 관계 |
| `Recommendation.java` | `recommendations` | CHECK 제약조건 = matchScore 0~100 |

### build.gradle 의존성 (이미 추가됨)

```groovy
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-database-postgresql'
```

---

## JPA `ddl-auto`와의 차이

| | `ddl-auto=update` | Flyway |
|---|---|---|
| 용도 | 개발 편의 (프로토타이핑) | 운영 환경 스키마 관리 |
| 동작 | Entity 보고 테이블 자동 생성/변경 | SQL 파일을 순서대로 실행 |
| 컬럼 삭제 | 안 함 (위험 방지) | SQL에 명시하면 실행 |
| 데이터 안전성 | 낮음 (예측 불가) | 높음 (SQL 리뷰 가능) |
| 이력 관리 | 없음 | `flyway_schema_history`에 기록 |

→ **개발 중에도 Flyway를 쓰면 "내 로컬에서는 되는데?" 문제를 예방할 수 있다.**

---

## 핵심 규칙

1. **한번 적용된 마이그레이션 파일은 수정하지 않는다** — 체크섬이 달라지면 Flyway가 에러를 낸다
2. **새로운 변경은 새 버전 파일로** — `V2__xxx.sql`, `V3__xxx.sql` ...
3. **롤백은 별도 마이그레이션으로** — `V4__rollback_v3.sql` (Flyway 커뮤니티 버전은 자동 롤백 미지원)
