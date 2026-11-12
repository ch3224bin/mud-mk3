package mudmk3;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
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

    /** 애플리케이션 서비스는 다른 슬라이스의 provided(포트·DTO)만 쓴다 */
    @ArchTest
    static final ArchRule applicationUsesOnlyProvidedOfOtherSlices = classes()
            .that().resideInAPackage("mudmk3.application..")
            .should(useOnlyProvidedOfOtherApplicationSlices());

    private static final Pattern APPLICATION_SLICE = Pattern.compile("^mudmk3\\.application\\.([^.]+)(\\..*)?$");

    private static ArchCondition<JavaClass> useOnlyProvidedOfOtherApplicationSlices() {
        return new ArchCondition<>("use only provided packages of other application slices") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                Optional<String> origin = sliceOf(javaClass.getPackageName());
                if (origin.isEmpty()) return;
                for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                    String targetPackage = dependency.getTargetClass().getPackageName();
                    Optional<String> target = sliceOf(targetPackage);
                    boolean otherSlice = target.isPresent() && !target.get().equals(origin.get());
                    if (otherSlice && !targetPackage.equals("mudmk3.application." + target.get() + ".provided")) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    private static Optional<String> sliceOf(String packageName) {
        Matcher matcher = APPLICATION_SLICE.matcher(packageName);
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
