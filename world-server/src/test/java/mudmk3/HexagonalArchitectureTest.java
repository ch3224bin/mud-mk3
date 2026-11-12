package mudmk3;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "mudmk3", importOptions = DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule hexagonalArchitecture = layeredArchitecture()
            .consideringAllDependencies()
            .withOptionalLayers(true)
            .layer("Domain").definedBy("mudmk3.domain..")
            .layer("Application").definedBy("mudmk3.application..")
            .layer("Adapter").definedBy("mudmk3.adapter..")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapter")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter")
            .whereLayer("Adapter").mayNotBeAccessedByAnyLayer()
            // support 와 루트(WorldServerApplication)는 레이어 밖이라 레이어를 참조해도 위반이 아니다
            .ignoreDependency(resideInAnyPackage("mudmk3", "mudmk3.support.."), alwaysTrue());

    @ArchTest
    static final ArchRule domainSlicesFreeOfCycles = slices()
            .matching("mudmk3.domain.(*)..")
            .should().beFreeOfCycles()
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule applicationSlicesFreeOfCycles = slices()
            .matching("mudmk3.application.(*)..")
            .should().beFreeOfCycles()
            .allowEmptyShould(true);
}
