# FindMyVibe — Backend

Spring Boot 기반 AI 성향 분석 + 추천 API 서버

## 기술 스택

| 항목 | 기술 |
|------|------|
| Runtime | Java 25 + Virtual Threads |
| Framework | Spring Boot 3.5 + Spring MVC |
| Database | PostgreSQL 17 + Spring Data JPA + Flyway |
| Cache | Redis 7 (캐시 + Rate Limit) |
| AI | Claude API (Anthropic Java SDK) |
| Resilience | Resilience4j (Circuit Breaker + Retry + Bulkhead) |
| API Docs | springdoc-openapi (Swagger) |
| Monitoring | Micrometer + Prometheus |

## 패키지 구조

```
com.findmyvibe/
├── api/                 # Controller, DTO
├── domain/              # Entity, Repository, Service
├── infrastructure/      # Claude Client, Redis, Crawler
└── common/              # Config, Exception
```

## 로컬 실행

```bash
# 1. Docker 의존성 기동 (PostgreSQL + Redis)
docker compose up -d

# 2. 환경변수 설정
cp .env.example .env
# ANTHROPIC_API_KEY=sk-ant-... 설정

# 3. 애플리케이션 실행
./gradlew bootRun --args='--spring.profiles.active=local'

# 4. Swagger 확인
open http://localhost:8080/swagger-ui.html
```

## 테스트

```bash
# 전체 테스트 (Testcontainers 필요 → Docker 실행 상태여야 함)
./gradlew test

# 단위 테스트만
./gradlew test --tests "*UnitTest*"

# ArchUnit 아키텍처 규칙 테스트만
./gradlew test --tests "*ArchitectureTest*"
```

## API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/sessions` | 세션 시작 → 기본 질문 반환 |
| POST | `/api/v1/sessions/{id}/answers` | 기본 답변 → 꼬리질문 반환 |
| POST | `/api/v1/sessions/{id}/follow-up` | 꼬리질문 답변 → 프로필 + 추천 반환 |
| GET | `/api/v1/sessions/{id}/profile` | 성향 프로필 조회 |
| GET | `/api/v1/sessions/{id}/recommendations` | 추천 결과 조회 |

## 장애 대응

```
Claude API 호출
  ├─ 성공 → 응답 + Redis 캐시 저장
  └─ 실패 → Retry (3회, exponential backoff)
              └─ 실패 지속 → Circuit Breaker OPEN
                              └─ Fallback: 유사 성향 캐시 추천 반환
```

## 배포

AWS ECS Fargate + ALB → `api.findmyvibe.com`
