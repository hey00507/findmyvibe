# FindMyVibe

> 간단한 질문에 답하면 AI가 성향을 분석하고, 나에게 맞는 취미/원데이클래스를 추천해주는 웹앱

1인 개발 사이드 프로젝트. App Store/Play Store 배포 없이, 웹으로 서비스.

## 프로젝트 구조

```
findmyvibe/
├── be/          # Spring Boot 백엔드 (Java 25)
├── fe/          # React 프론트엔드 (Phase 3에서 구현)
└── docs/        # 공통 문서 (ADR, 설계)
```

각 디렉토리별 상세 실행 방법은 해당 README 참고:
- [Backend README](be/README.md)
- [Frontend README](fe/README.md)

## 핵심 기능

1. **성향 분석 질의응답** — 기본 질문 7개 + AI 꼬리질문 3~5개 (하이브리드)
2. **성향 프로필 생성** — 타입 라벨 + 레이더 차트 + 키워드
3. **맞춤 추천** — 성향 기반 5개 추천 (매칭 점수 + 실제 클래스 링크)

## 기술 스택

| 영역 | 기술 |
|------|------|
| Backend | Java 25 + Spring Boot 3.5 + Virtual Threads |
| Database | PostgreSQL 17 + Redis 7 |
| AI | Claude API (Anthropic Java SDK) |
| Frontend | React 19 + TypeScript + Vite + Tailwind CSS 4 |
| Infra | BE: AWS ECS Fargate / FE: Vercel |

## 아키텍처

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

## 마일스톤

- **Phase 1** (1주): MVP — API + LLM 연동 + Redis + 테스트 60개+
- **Phase 2** (1주): 크롤링 + 모니터링 (Prometheus/Grafana)
- **Phase 3** (1주): FE 구현 + AWS 배포
- **Phase 4** (선택): 회원 시스템 + 소셜 로그인

## 의사결정 기록 (ADR)

| # | 제목 | 상태 |
|---|------|------|
| [001](docs/adr/001-single-module-decision.md) | 싱글 모듈 + 패키지 분리 선택 | Accepted |
| [002](docs/adr/002-java25-virtual-threads.md) | Java 25 + Virtual Threads, WebFlux 미사용 | Accepted |
| [003](docs/adr/003-fe-be-separation.md) | FE/BE 분리 배포 (Vercel + AWS) | Accepted |

## 라이선스

MIT
