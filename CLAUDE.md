# CLAUDE.md — FindMyVibe

## 프로젝트 개요
Spring Boot 3.5 + Java 25 기반 AI 성향 분석 취미 추천 API 서버.
Claude API로 사용자 성향을 분석하고 맞춤 취미/원데이클래스를 추천한다.

## 기술 스택
- Java 25, Spring Boot 3.5, Spring MVC + Virtual Threads
- PostgreSQL 17 + JPA + Flyway, H2 (로컬), Redis 7
- Claude API (Anthropic Java SDK), Resilience4j, springdoc-openapi
- Gradle (Groovy DSL), Docker, AWS ECS Fargate

## 패키지 구조
```
com.findmyvibe/
├── api/                 # Controller, DTO (표현 계층)
├── domain/              # Entity, Repository, Service (비즈니스 핵심)
├── infrastructure/      # Claude Client, Redis, Crawler (외부 연동)
└── common/              # Config, Exception (횡단 관심사)
```

## 의존성 규칙 (ArchUnit으로 강제)
- `domain` → `api` 의존 금지
- `domain` → `infrastructure` 의존 금지
- `api.controller` → `domain.repository` 직접 접근 금지
- `infrastructure` → `api` 의존 금지
- 흐름: Controller(api) → Service(domain) → Repository(domain) / Client(infrastructure)

## 빌드 & 실행
```bash
./gradlew bootRun --args='--spring.profiles.active=local'   # 로컬 실행 (H2)
./gradlew test                                               # 전체 테스트
./gradlew bootJar                                            # JAR 빌드
```

## 프로필
- `local`: H2 인메모리 DB, 로컬 Redis
- `test`: Testcontainers (PostgreSQL + Redis)
- `prod`: AWS RDS + ElastiCache

## 코드 컨벤션
- TDD: 테스트 먼저 작성 → 구현 → 리팩터
- 3회 반복 → 메서드 추출
- 미사용 자원 즉시 제거
- API 키/시크릿은 환경변수 (.env gitignore)
- Entity를 API 응답으로 직접 노출하지 않음 → DTO 변환 필수
- 에러 응답: RFC 7807 Problem Details 형식

## Java 25 활용
- Virtual Threads: `spring.threads.virtual.enabled=true`
- Structured Concurrency: 병렬 작업 (LLM + 크롤링)
- Scoped Values: traceId 전파 (ThreadLocal 대체)
- Record, Sealed Interface, Pattern Matching 적극 활용

## 외부 API 장애 대응
- Resilience4j: Circuit Breaker + Retry + Bulkhead
- Fallback: Claude API 장애 시 Redis 캐시에서 유사 프로필 추천 반환
- Rate Limit: IP당 하루 3회, 회원 5회

## 테스트 전략
- Unit: JUnit 5 + Mockito (Service, Domain)
- Integration: Testcontainers (PostgreSQL, Redis)
- API: MockMvc + RestAssured (Controller E2E)
- Architecture: ArchUnit (레이어 의존성 규칙)
- LLM Mock: WireMock (Claude API 응답 시뮬레이션)

## 커밋 메시지
한글 커밋 메시지 사용. 타입 prefix:
- `feat:` 새 기능
- `fix:` 버그 수정
- `refactor:` 리팩토링
- `test:` 테스트
- `docs:` 문서
- `chore:` 빌드/설정

## 참고 문서
- [PRD (BE)](https://github.com/hey00507/findmyvibe/blob/main/docs/PRD-BE.md) — 상세 설계
- [ADR 목록](docs/adr/) — 아키텍처 의사결정 기록
