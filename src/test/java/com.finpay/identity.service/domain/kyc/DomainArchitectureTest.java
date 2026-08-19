package com.finpay.identity.service.domain.kyc;

import com.finpay.common.test.ArchitectureRules;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Enforces AGENTS.md rule 4: the domain layer stays free of infrastructure
 * frameworks (Spring/JPA/Kafka). Uses the shared rules from com.finpay:common-test.
 */
@AnalyzeClasses(packages = "com.finpay.identity.service.domain.kyc",
        importOptions = ImportOption.DoNotIncludeTests.class)
class DomainArchitectureTest {

    @ArchTest
    static final ArchRule domain_is_independent_of_infrastructure =
            ArchitectureRules.domainIsIndependentOfInfrastructure();

    @ArchTest
    static final ArchRule domain_depends_only_on_jdk_and_domain =
            classes()
                    .that().resideInAPackage("..domain..")
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage("java..", "..domain..");
}
