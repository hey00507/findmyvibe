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

## 개발 프로세스
1. 기능에 대한 명확한 PRD를 먼저 구성한다
2. 중복되는 코드 또는 차용 가능한 기존 코드가 없는지 확인한다
3. 실패하는 테스트를 먼저 작성한다
4. 테스트를 통과하는 최소한의 코드를 구현한다
5. 리팩터링한다

## 테스트 원칙
- BE API 테스트 커버리지 100% 유지
- 3회 반복 → 메서드 추출
- 미사용 자원 즉시 제거

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

## 커밋 메시지
한글 커밋 메시지 사용. 타입 prefix:
- `feat:` 새 기능
- `fix:` 버그 수정
- `refactor:` 리팩토링
- `test:` 테스트
- `docs:` 문서
- `chore:` 빌드/설정

## 참고 문서
- [ADR 목록](docs/adr/) — 아키텍처 의사결정 기록
