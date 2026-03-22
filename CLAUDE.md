# CLAUDE.md — FindMyVibe

## 패키지 구조
```
com.findmyvibe/
├── api/                 # Controller, DTO
├── domain/              # Entity, Repository, Service
├── infrastructure/      # Claude Client, Redis, Crawler
└── common/              # Config, Exception
```

## 의존성 규칙
- domain → api, infrastructure 의존 금지
- controller → repository 직접 접근 금지
- infrastructure → api 의존 금지

## 개발 프로세스
1. PRD 먼저 구성
2. 기존 코드에서 차용 가능한 부분 확인
3. 실패하는 테스트 작성
4. 테스트 통과하는 최소 코드 구현
5. 리팩터링

## 원칙
- API 테스트 커버리지 100%
- 3회 반복 → 메서드 추출
- 미사용 자원 즉시 제거

## 커밋
한글. `feat:` / `fix:` / `refactor:` / `test:` / `docs:` / `chore:`
