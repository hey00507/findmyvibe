# ADR-002: Java 25 + Virtual Threads, WebFlux 미사용

## 상태
Accepted (2026-03-22)

## 맥락
LLM API 호출(1~10초), DB, Redis 등 블로킹 I/O가 많은 서비스에서 높은 동시성이 필요하다.
두 가지 접근이 있었다:

- **A. Spring WebFlux** (리액티브 스택) — `Mono`, `Flux` 기반 비동기
- **B. Spring MVC + Virtual Threads** (Java 25) — 동기식 코드 + 가상 스레드

## 결정
**B. Spring MVC + Virtual Threads**를 선택한다.

## 근거

### 동시성 성능
- Virtual Threads: OS 스레드 수백 개 → 가상 스레드 수만 개 가능
- 블로킹 I/O 시 OS 스레드를 점유하지 않음 (JVM이 자동 unmount)
- WebFlux와 동등한 처리량을 동기식 코드로 달성

### 코드 복잡도
- WebFlux: `Mono.flatMap().map().switchIfEmpty().onErrorResume()` 체인
- Virtual Threads: `try-catch` 동기 코드. 디버깅 시 스택 트레이스가 명확
- 러닝 커브: 팀원이 리액티브에 익숙하지 않아도 바로 작업 가능

### Java 25 추가 활용
- **Structured Concurrency**: LLM + 크롤링 병렬 실행을 `StructuredTaskScope`로 안전하게 관리
- **Scoped Values**: `ThreadLocal` 대체 — Virtual Thread에 최적화된 traceId 전파
- 두 기능 모두 WebFlux에서는 사용할 수 없음 (리액티브 스택과 비호환)

### RestClient > WebClient
- Spring 6.1 도입 RestClient: 동기식 HTTP 클라이언트, Virtual Threads와 자연스러운 조합
- WebClient: 리액티브 스택 위 동작, MVC + VT 환경에서 오버헤드

## 결과
- `spring.threads.virtual.enabled: true` 설정
- 모든 HTTP 요청 Virtual Thread에서 처리
- Claude API 호출, DB, Redis 모두 동기식 블로킹 코드
- Structured Concurrency로 병렬 작업 관리
- Scoped Values로 요청 컨텍스트 전파
