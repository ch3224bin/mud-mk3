package mudmk3;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

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
            .whereLayer("Adapter").mayNotBeAccessedByAnyLayer();

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
