package com.spade;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import jakarta.persistence.Entity;
import org.springframework.context.annotation.Profile;

/**
 * The layers and rules of ADR 0012, from the first commit (WealthWatcher retrofitted its layers and spent
 * phases on the exemptions). Every rule was shown to fail once on a planted violation: see "What shipping it actually found" in
 * docs/handoffs/PHASE_06_2026-10-11_BACKEND_FOUNDATION.md.
 */
@AnalyzeClasses(packages = "com.spade", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layers = layeredArchitecture().consideringOnlyDependenciesInLayers()
            .layer("Api").definedBy("com.spade.api..")
            .layer("Service").definedBy("com.spade.service..")
            .layer("Repository").definedBy("com.spade.repository..")
            .layer("Domain").definedBy("com.spade.domain..")
            .layer("Config").definedBy("com.spade.config..", "com.spade.security..")
            .whereLayer("Api").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Api", "Config")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Api", "Service", "Repository")
            .whereLayer("Config").mayNotBeAccessedByAnyLayer()
            .withOptionalLayers(true);

    @ArchTest
    static final ArchRule domainIsPure = noClasses().that().resideInAPackage("com.spade.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "org.hibernate..",
                    "com.fasterxml.jackson..", "tools.jackson..",
                    "com.spade.api..", "com.spade.service..", "com.spade.repository..",
                    "com.spade.config..", "com.spade.security..");

    @ArchTest
    static final ArchRule apiNeverSeesEntities = noClasses().that().resideInAPackage("com.spade.api..")
            .should().dependOnClassesThat().areAnnotatedWith(Entity.class);

    @ArchTest
    static final ArchRule dtosAreRecords = classes().that().resideInAPackage("com.spade.api.dto..")
            .should().beRecords();

    /**
     * PRODUCT.md: hole cards stay private until showdown. A public view can't even name them (phase 07).
     * "Public view" covers both naming styles, {@code Public*View} (Phase 07's {@code PublicTableView}) and
     * {@code *PublicView}, and every type nested inside one ({@code PublicTableView$Seat}).
     * The private type is any class with the simple name {@code HoleCards}.
     */
    @ArchTest
    static final ArchRule publicViewsNeverNameHoleCards = noClasses()
            .that().haveNameMatching(".*\\.(Public\\w*View|\\w*PublicView)(\\$.*)?")
            .should().dependOnClassesThat().haveSimpleName("HoleCards")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule noFieldInjection = GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

    @ArchTest
    static final ArchRule noStandardStreams = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    /** The testworld has an unauthenticated wipe endpoint: it must exist only under exactly {@code @Profile("testworld")}. */
    @ArchTest
    static final ArchRule testworldOnlyUnderItsProfile = classes().that().resideInAPackage("com.spade.testworld..")
            .should().beAnnotatedWith(DescribedPredicate.describe(
                    "@Profile with exactly the value \"testworld\" (not negated, not combined with other profiles)",
                    (JavaAnnotation<?> a) -> a.getRawType().isEquivalentTo(Profile.class)
                            && a.get("value").map(v -> v instanceof Object[] values
                                    && values.length == 1 && "testworld".equals(values[0])).orElse(false)));
}
