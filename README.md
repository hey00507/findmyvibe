# FindMyVibe

> 간단한 질문에 답하면 AI가 성향을 분석하고, 나에게 맞는 취미/원데이클래스를 추천해주는 웹앱

## 기술 스택

### Backend
- **Java 25** + **Spring Boot 3.5**
- Spring MVC + Virtual Threads (WebFlux 미사용 — [ADR-002](docs/adr/002-java25-virtual-threads.md))
- PostgreSQL 17 + Spring Data JPA + Flyway
- Redis 7 (캐시 + Rate Limit)
- Claude API (Anthropic Java SDK)
- Resilience4j (Circuit Breaker + Retry + Bulkhead)
- springdoc-openapi (Swagger)

### Frontend
- React 19 + TypeScript + Vite
- Tailwind CSS 4
- TanStack Query + React Hook Form

### Infra
- BE: Docker + AWS ECS Fargate + ALB + RDS + ElastiCache
- FE: Vercel
- CI/CD: GitHub Actions (BE) + Vercel (FE)

## 아키텍처

### 시스템 구성도

```
  ┌───────────────────┐
  │  React (TS) SPA   │
  │  Vercel CDN       │
  │  findmyvibe.com   │
  └────────┬──────────┘
           │ HTTPS (CORS)
  ┌────────▼──────────┐
  │  AWS ALB          │
  │  api.findmyvibe.com
  └────────┬──────────┘
           │
  ┌────────▼──────────┐
  │  ECS Fargate      │
  │  Spring Boot 3.5  │
  │  Java 25 + VT     │
  └──┬──┬──┬──────────┘
     │  │  │
     │  │  └───────────────┐
     │  └────────┐         │
     ▼           ▼         ▼
┌──────────┐ ┌────────┐ ┌──────────┐
│PostgreSQL│ │ Redis  │ │Claude API│
│ 17 (RDS) │ │(Elasti)│ │(External)│
└──────────┘ └────────┘ └──────────┘
```

### 패키지 구조 (싱글 모듈)

```
com.findmyvibe/
├── api/                 # Controller, DTO
├── domain/              # Entity, Repository, Service
├── infrastructure/      # Claude Client, Redis, Crawler
└── common/              # Config, Exception
```

> 싱글 모듈 선택 근거: [ADR-001](docs/adr/001-single-module-decision.md)

### 요청 흐름

```
Client
  → Controller (api)
    → Service (domain)
      → Repository (domain) → PostgreSQL
      → ClaudeClient (infrastructure) → Claude API
      → RedisCache (infrastructure) → Redis
    ← 응답 조립
  ← DTO 변환 → JSON Response
```

### 장애 대응

```
Claude API 호출
  ├─ 성공 → 응답 + Redis 캐시 저장
  └─ 실패 → Retry (3회, exponential backoff)
              └─ 실패 지속 → Circuit Breaker OPEN
                              └─ Fallback: 유사 성향 캐시 추천 반환
```

## 핵심 기능

1. **성향 분석 질의응답** — 기본 질문 7개 + AI 꼬리질문 3~5개 (하이브리드)
2. **성향 프로필 생성** — 타입 라벨 + 레이더 차트 + 키워드
3. **맞춤 추천** — 성향 기반 5개 추천 (매칭 점수 + 실제 클래스 링크)

## API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/sessions` | 세션 시작 → 기본 질문 반환 |
| POST | `/api/v1/sessions/{id}/answers` | 기본 답변 → 꼬리질문 반환 |
| POST | `/api/v1/sessions/{id}/follow-up` | 꼬리질문 답변 → 프로필 + 추천 반환 |
| GET | `/api/v1/sessions/{id}/profile` | 성향 프로필 조회 |
| GET | `/api/v1/sessions/{id}/recommendations` | 추천 결과 조회 |

> Swagger UI: `http://localhost:8080/swagger-ui.html`

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

## 의사결정 기록 (ADR)

| # | 제목 | 상태 |
|---|------|------|
| [001](docs/adr/001-single-module-decision.md) | 싱글 모듈 + 패키지 분리 선택 | Accepted |
| [002](docs/adr/002-java25-virtual-threads.md) | Java 25 + Virtual Threads, WebFlux 미사용 | Accepted |
| [003](docs/adr/003-fe-be-separation.md) | FE/BE 분리 배포 (Vercel + AWS) | Accepted |

## 마일스톤

- **Phase 1** (1주): MVP — API + LLM 연동 + Redis + 테스트 60개+
- **Phase 2** (1주): 크롤링 + 모니터링 (Prometheus/Grafana)
- **Phase 3** (1주): FE 연동 + AWS 배포
- **Phase 4** (선택): 회원 시스템 + 소셜 로그인

## 라이선스

MIT
