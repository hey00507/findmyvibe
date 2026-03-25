package com.findmyvibe;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "com.findmyvibe",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain은_api에_의존하지_않는다 =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..api..")
                    .because("domain은 순수 비즈니스 로직이며 API 레이어를 알면 안 됩니다");

    @ArchTest
    static final ArchRule domain은_infrastructure에_의존하지_않는다 =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .because("domain은 외부 기술 구현에 의존하면 안 됩니다");

    // api, infrastructure 패키지에 클래스가 아직 없으므로 allowEmptyShould 허용
    // Step 2에서 Controller/Service 구현 후 실질적 검증이 시작된다
    @ArchTest
    static final ArchRule controller는_repository에_직접_접근하지_않는다 =
            noClasses()
                    .that().resideInAPackage("..api..")
                    .should().dependOnClassesThat().resideInAPackage("..domain.repository..")
                    .allowEmptyShould(true)
                    .because("controller는 service를 통해서만 데이터에 접근해야 합니다");

    @ArchTest
    static final ArchRule infrastructure는_api에_의존하지_않는다 =
            noClasses()
                    .that().resideInAPackage("..infrastructure..")
                    .should().dependOnClassesThat().resideInAPackage("..api..")
                    .allowEmptyShould(true)
                    .because("infrastructure는 기술 구현만 담당합니다");
}
