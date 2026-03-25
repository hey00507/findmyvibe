# ArchUnit — 아키텍처 규칙 테스트 도구

## 한 줄 요약

**ArchUnit은 코드의 패키지 의존성 규칙을 JUnit 테스트로 강제하는 도구다.**
"domain은 infrastructure에 의존하면 안 된다" 같은 규칙을 자동 검증한다.

---

## 왜 필요한가?

### ArchUnit 없이 개발하면 생기는 문제

```
CLAUDE.md에 규칙 정의:
  "domain은 api, infrastructure에 의존하지 않음"

3개월 후...
  누군가 domain/service/AnalysisService.java에서
  infrastructure/claude/ClaudeClient.java를 직접 import

→ 코드 리뷰에서 놓침
→ 규칙이 문서에만 존재하고 코드에서는 깨진 상태
```

### ArchUnit이 해결하는 것

1. **아키텍처 규칙을 코드로 표현** — 문서가 아닌 테스트로 관리
2. **CI에서 자동 검증** — 규칙 위반 시 빌드 실패
3. **빠른 피드백** — 코드 작성 직후 `./gradlew test`로 확인
4. **점진적 적용** — 필요한 규칙만 하나씩 추가 가능

---

## 동작 원리

### 기본 구조

ArchUnit 테스트는 일반 JUnit 테스트와 같은 위치에 작성한다.

```java
@AnalyzeClasses(packages = "com.findmyvibe")
class ArchitectureTest {

    @ArchTest
    static final ArchRule 규칙이름 = 규칙정의;
}
```

- `@AnalyzeClasses` — 분석할 패키지 범위 지정
- `@ArchTest` — 각 규칙을 필드로 선언
- 테스트 실행 시 실제 컴파일된 클래스를 분석하여 규칙 위반 여부를 검사

### 규칙 작성 패턴

ArchUnit은 자연어에 가까운 API를 제공한다:

```java
// "domain 패키지에 있는 클래스는 api 패키지에 의존하면 안 된다"
noClasses()
    .that().resideInAPackage("..domain..")
    .should().dependOnClassesThat().resideInAPackage("..api..")
```

읽는 방법:
```
noClasses()                                    → 어떤 클래스도
    .that().resideInAPackage("..domain..")     → domain 패키지에 있는 것 중
    .should().dependOnClassesThat()            → 의존해서는 안 된다
    .resideInAPackage("..api..")               → api 패키지에 있는 클래스에
```

`..`은 "하위 패키지 포함"을 의미한다.
예: `..domain..` = `com.findmyvibe.domain` 및 그 하위 패키지 전부

---

## 우리 프로젝트에서의 적용

### 지켜야 할 규칙 (CLAUDE.md에 정의됨)

```
com.findmyvibe/
├── api/              ← controller, dto
├── domain/           ← entity, repository, service (핵심 비즈니스)
├── infrastructure/   ← claude client, redis, crawler
└── common/           ← config, exception
```

| # | 규칙 | 이유 |
|---|------|------|
| 1 | **domain → api 의존 금지** | domain은 순수 비즈니스 로직이어야 한다. Controller/DTO를 알면 안 된다. |
| 2 | **domain → infrastructure 의존 금지** | domain이 Claude API나 Redis 구현에 직접 의존하면 교체가 어렵다. |
| 3 | **controller → repository 직접 접근 금지** | controller는 service를 통해서만 데이터에 접근해야 한다. |
| 4 | **infrastructure → api 의존 금지** | infrastructure는 기술 구현만 담당한다. controller를 알 필요가 없다. |

### 실제 테스트 코드

```java
package com.findmyvibe;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
    packages = "com.findmyvibe",
    importOptions = ImportOption.DoNotIncludeTests.class  // 테스트 코드는 분석 대상에서 제외
)
class ArchitectureTest {

    // 규칙 1: domain은 api에 의존하지 않는다
    @ArchTest
    static final ArchRule domain_should_not_depend_on_api =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..api..")
            .because("domain은 순수 비즈니스 로직이며 API 레이어를 알면 안 됩니다");

    // 규칙 2: domain은 infrastructure에 의존하지 않는다
    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .because("domain은 외부 기술 구현에 의존하면 안 됩니다");

    // 규칙 3: controller는 repository에 직접 접근하지 않는다
    @ArchTest
    static final ArchRule controller_should_not_access_repository =
        noClasses()
            .that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAPackage("..domain.repository..")
            .because("controller는 service를 통해서만 데이터에 접근해야 합니다");

    // 규칙 4: infrastructure는 api에 의존하지 않는다
    @ArchTest
    static final ArchRule infrastructure_should_not_depend_on_api =
        noClasses()
            .that().resideInAPackage("..infrastructure..")
            .should().dependOnClassesThat().resideInAPackage("..api..")
            .because("infrastructure는 기술 구현만 담당합니다");
}
```

### 테스트 실행

```bash
# ArchUnit 테스트만 실행
./gradlew test --tests "*ArchitectureTest*"

# 전체 테스트 실행 (ArchUnit 포함)
./gradlew test
```

### 위반 시 출력 예시

만약 `AnalysisService`에서 `SessionController`를 import하면:

```
Architecture Violation [Priority: MEDIUM] - Rule 'domain은 api에 의존하지 않는다' was violated (1 times):
  Class <com.findmyvibe.domain.service.AnalysisService>
  depends on <com.findmyvibe.api.controller.SessionController>
  in (AnalysisService.java:5)

  because domain은 순수 비즈니스 로직이며 API 레이어를 알면 안 됩니다
```

→ 어떤 클래스의 몇 번째 줄에서 위반했는지 정확히 알려준다.

### build.gradle 의존성 (이미 추가됨)

```groovy
testImplementation 'com.tngtech.archunit:archunit-junit5:1.4.0'
```

---

## 자주 쓰는 규칙 패턴

### 패키지 의존 방향 제한

```java
// domain은 api/infrastructure를 모른다
noClasses().that().resideInAPackage("..domain..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("..api..", "..infrastructure..");
```

### 네이밍 규칙 강제

```java
// Controller로 끝나는 클래스는 api.controller 패키지에 있어야 한다
classes().that().haveSimpleNameEndingWith("Controller")
    .should().resideInAPackage("..api.controller..");

// Repository로 끝나는 클래스는 domain.repository 패키지에 있어야 한다
classes().that().haveSimpleNameEndingWith("Repository")
    .should().resideInAPackage("..domain.repository..");
```

### 어노테이션 기반 규칙

```java
// @Entity 클래스는 domain.entity 패키지에 있어야 한다
classes().that().areAnnotatedWith(Entity.class)
    .should().resideInAPackage("..domain.entity..");

// @RestController 클래스는 api.controller에 있어야 한다
classes().that().areAnnotatedWith(RestController.class)
    .should().resideInAPackage("..api.controller..");
```

### 레이어 아키텍처 (한번에 정의)

```java
layeredArchitecture()
    .consideringAllDependencies()
    .layer("API").definedBy("..api..")
    .layer("Domain").definedBy("..domain..")
    .layer("Infrastructure").definedBy("..infrastructure..")
    .layer("Common").definedBy("..common..")

    .whereLayer("API").mayOnlyBeAccessedByLayers("Common")
    .whereLayer("Domain").mayOnlyBeAccessedByLayers("API", "Infrastructure", "Common")
    .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("API", "Common")
    .whereLayer("Common").mayOnlyBeAccessedByLayers("API", "Domain", "Infrastructure");
```

---

## Flyway vs ArchUnit 비교

둘 다 "규칙을 코드로 관리"한다는 공통점이 있지만 대상이 다르다:

| | Flyway | ArchUnit |
|---|---|---|
| 관리 대상 | DB 스키마 (테이블, 컬럼, 인덱스) | Java 코드 구조 (패키지, 의존성) |
| 실행 시점 | 앱 시작 시 | 테스트 실행 시 |
| 파일 형식 | SQL 파일 | Java 테스트 파일 |
| 위치 | `src/main/resources/db/migration/` | `src/test/java/` |
| 역할 | "DB가 이 상태여야 한다" | "코드가 이 규칙을 지켜야 한다" |
