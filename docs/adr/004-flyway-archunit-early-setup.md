# ADR-004: Flyway + ArchUnit 조기 세팅

## 상태

수용 (Accepted)

## 맥락

Phase 1 Step 1에서 도메인 레이어(Entity 5개, Repository 5개, Enum 2개)가 완성되었다.
다음 단계(Step 2: API 레이어)로 넘어가기 전에 Flyway 마이그레이션과 ArchUnit 테스트를 먼저 세팅할지 결정이 필요했다.

**현실적으로 둘 다 지금 꼭 필요하지는 않다:**

- **Flyway**: 로컬/테스트 환경은 H2 + `ddl-auto: create-drop`으로 잘 동작한다. Flyway는 운영 PostgreSQL 배포 시 필요하며, Step 6(마무리) 때 해도 충분하다.
- **ArchUnit**: 현재 domain 패키지만 존재하고 api/infrastructure가 비어있어 검사할 의존성 위반이 없다. API 레이어 코드가 생긴 뒤에 추가해야 실질적 효과가 있다.

## 결정

**Flyway와 ArchUnit을 Step 1 단계에서 조기 세팅한다.**

## 이유

### 1. 인프라는 나중에 끼워넣기 어렵다

코드가 쌓인 뒤에 Flyway를 도입하면 기존 Entity와 SQL의 불일치를 한꺼번에 해결해야 한다.
지금 Entity가 5개일 때 맞춰두면, 이후 스키마 변경은 `V2`, `V3`로 자연스럽게 이어진다.

### 2. PRD SQL과 Entity 간 불일치를 조기 발견

실제 세팅 과정에서 PRD의 SQL과 Entity 코드 사이의 차이를 발견했다:

- PRD SQL에는 `BaseEntity`의 Auditing 컬럼(`created_by`, `modified_at`, `modified_by`)이 누락
- PRD의 `answers.answered_at`은 Entity에 존재하지 않음 (`BaseEntity.createdAt`이 대체)

이런 불일치는 나중에 발견할수록 수정 범위가 커진다.

### 3. ArchUnit은 "가드레일 먼저 세우기"

빈 도로에 가드레일을 세우는 게 차가 다닌 뒤에 세우는 것보다 쉽다.
Step 2에서 Controller → Repository 직접 접근 같은 실수를 하면, ArchUnit이 즉시 잡아준다.
1인 개발에서 셀프 리뷰의 한계를 보완하는 안전장치다.

### 4. 학습 목적

Flyway와 ArchUnit을 처음 사용하는 상황이다.
코드가 단순한 지금 세팅해보면서 도구의 동작을 이해하고, 이후 복잡한 변경에 대비한다.

## 결과

### Flyway

- `V1__create_tables.sql` 작성 (5개 테이블 + 인덱스 3개)
- PRD SQL 대비 BaseEntity 공통 컬럼 4개 추가 (`created_at`, `created_by`, `modified_at`, `modified_by`)
- 환경별 Flyway 설정:
  - `local`: `flyway.enabled: false` (H2 + ddl-auto)
  - `test`: `flyway.enabled: true` (Testcontainers PostgreSQL + ddl-auto: validate)
  - `prod`: `flyway.enabled: true` (PostgreSQL + ddl-auto: validate)

### ArchUnit

- `ArchitectureTest.java` 작성 (4개 규칙)
- CLAUDE.md의 의존성 규칙을 코드로 강제:
  1. `domain` → `api` 의존 금지
  2. `domain` → `infrastructure` 의존 금지
  3. `api` → `domain.repository` 직접 접근 금지
  4. `infrastructure` → `api` 의존 금지

## 트레이드오프

| 장점 | 단점 |
|------|------|
| 스키마 불일치 조기 발견 | 아직 운영 배포 전이라 Flyway 실효성 제한적 |
| 아키텍처 위반 자동 감지 | 현재는 검사 대상 코드가 적어 위반 자체가 없음 |
| 학습 + 경험 축적 | 당장 Step 2 진행에는 불필요한 작업 |
