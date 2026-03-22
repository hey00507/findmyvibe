# ADR-001: 싱글 모듈 + 패키지 분리 선택

## 상태
Accepted (2026-03-22)

## 맥락
FindMyVibe는 Spring Boot 기반 API 서버로, 헥사고날 아키텍처를 적용한다.
구조 설계 시 두 가지 선택지가 있었다:

- **A. 멀티 모듈** (api / application / domain / infrastructure / common 각각 독립 모듈)
- **B. 싱글 모듈 + 패키지 분리** (하나의 모듈 안에서 패키지로 레이어 구분)

## 결정
**B. 싱글 모듈 + 패키지 분리**를 선택한다.

## 근거

### 프로젝트 규모
- BE 개발자 1명
- API 엔드포인트 5~6개 (Phase 1 기준)
- 도메인 모델 4개 (Session, Answer, Profile, Recommendation)
- 이 규모에서 5개 모듈은 과도한 설계

### 멀티 모듈의 비용
- 모듈별 `build.gradle` 관리 오버헤드
- 모듈 간 의존성 설정 시행착오
- IDE 빌드/리프레시 속도 저하
- 디버깅 시 모듈 경계 넘나드는 복잡도

### 싱글 모듈로도 달성 가능한 것
- 패키지 레벨 분리로 레이어 구조 표현 가능
- **ArchUnit 테스트**로 의존성 규칙을 CI에서 강제 가능
  - `domain` → `infrastructure` 의존 금지
  - `domain` → `api` 의존 금지
  - `controller` → `repository` 직접 접근 금지
- 헥사고날 아키텍처의 핵심(의존성 방향 제어)은 동일하게 유지

### 면접 관점
"프로젝트 규모를 고려해 싱글 모듈을 선택하되, ArchUnit으로 레이어 규칙을 테스트로 강제했다"는 **적절한 판단력**을 보여줌.
오히려 소규모 프로젝트에 멀티 모듈을 적용하면 "왜 이 규모에 멀티 모듈인가?" 질문에 답이 궁색해질 수 있음.

## 결과

### 패키지 구조
```
com.findmyvibe/
├── api/                 # Controller, DTO
├── domain/              # Entity, Repository, Service
├── infrastructure/      # Claude Client, Redis, Crawler
└── common/              # Config, Exception
```

### 의존성 규칙 (ArchUnit으로 강제)
```
api → domain ← infrastructure
        ↑
      common
```

### 확장 전략
프로젝트가 성장하여 다음 조건을 충족하면 멀티 모듈 전환을 검토한다:
- 개발자 2명 이상 + 레이어별 담당 분리 필요
- 도메인 모델 10개 이상
- 독립 배포가 필요한 모듈 발생 (예: 크롤링 배치를 별도 서비스로 분리)
