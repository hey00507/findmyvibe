# CLAUDE.md — FindMyVibe Backend

## 패키지 구조
```
com.findmyvibe/
├── api/                 # Controller, DTO
├── domain/              # Entity, Repository, Service
├── infrastructure/      # Claude Client, Redis, Crawler
└── common/              # Config, Exception
```

## 의존성 규칙
- domain은 api, infrastructure에 의존하지 않음
- controller는 repository에 직접 접근하지 않음
- infrastructure는 api에 의존하지 않음

## 명령어
- `./gradlew bootRun --args='--spring.profiles.active=local'`
- `./gradlew test`
- `./gradlew test --tests "*UnitTest*"`
- `./gradlew test --tests "*ArchitectureTest*"`

## 개발 프로세스
1. PRD 정의
2. 기존 코드에서 재사용 가능한 부분 확인
3. 실패하는 테스트 작성
4. 테스트 통과하는 최소 코드 구현
5. 리팩토링
