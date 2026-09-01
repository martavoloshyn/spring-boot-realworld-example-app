package io.spring.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Target hexagonal rules. Disabled tests cite a violation id in {@code architecture/violations.md}.
 * Enabling a test is the proof that id is gone.
 */
public class HexagonalArchitectureTest {

  private static JavaClasses classes;

  @BeforeAll
  static void importProductionClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.spring");
  }

  @Test
  void core_must_not_depend_on_mybatis() {
    noClasses()
        .that()
        .resideInAPackage("io.spring.core..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.mybatis..")
        .because("domain must not depend on MyBatis")
        .check(classes);
  }

  @Test
  void core_must_not_depend_on_jackson() {
    noClasses()
        .that()
        .resideInAPackage("io.spring.core..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.fasterxml.jackson..")
        .because("domain must not depend on Jackson")
        .check(classes);
  }

  @Test
  void v1_core_must_not_depend_on_spring() {
    noClasses()
        .that()
        .resideInAPackage("io.spring.core..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.springframework..")
        .because("V1 — Spring must not appear on domain ports")
        .check(classes);
  }

  @Test
  void v2_core_must_not_depend_on_root_util() {
    noClasses()
        .that()
        .resideInAPackage("io.spring.core..")
        .should()
        .dependOnClassesThat()
        .haveFullyQualifiedName("io.spring.Util")
        .because("V2 — entities must not import io.spring.Util")
        .check(classes);
  }

  @Disabled("V3 — Lombok on domain entities (optional); see architecture/violations.md")
  @Test
  void v3_core_must_not_depend_on_lombok() {
    noClasses()
        .that()
        .resideInAPackage("io.spring.core..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("lombok..")
        .because("V3 — optional purity: domain entities without Lombok")
        .check(classes);
  }

  @Disabled("V4 — Application → infrastructure; see architecture/violations.md")
  @Test
  void v4_application_must_not_depend_on_infrastructure_or_mybatis() {
    noClasses()
        .that()
        .resideInAPackage("io.spring.application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("io.spring.infrastructure..", "org.mybatis..")
        .because("V4 — query services must depend on ports, not MyBatis mappers")
        .check(classes);
  }

  @Test
  void v5_web_adapters_must_not_depend_on_repositories_or_authorization() {
    noClasses()
        .that()
        .resideInAnyPackage("io.spring.api..", "io.spring.graphql..")
        .and()
        .resideOutsideOfPackage("io.spring.api.security..")
        .should()
        .dependOnClassesThat(
            simpleNameEndingWith("Repository").or(simpleName("AuthorizationService")))
        .because("V5 — controllers/mutations must call inbound ports, not repositories")
        .check(classes);
  }

  @Disabled("V6 — Auth/token treated as domain; see architecture/violations.md")
  @Test
  void v6_jwt_must_not_live_in_core() {
    noClasses()
        .that()
        .resideInAPackage("io.spring.core..")
        .should()
        .haveSimpleName("JwtService")
        .because("V6 — JWT issue/parse belongs in adapter-web, not domain")
        .check(classes);
  }

  @Disabled("V6 — PasswordEncoder in application; see architecture/violations.md")
  @Test
  void v6_application_must_not_depend_on_spring_security() {
    noClasses()
        .that()
        .resideInAPackage("io.spring.application..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.springframework.security..")
        .because("V6 — application use cases must depend on PasswordHasher, not PasswordEncoder")
        .check(classes);
  }

  @Disabled("V7 — Query mappers imported from application; see architecture/violations.md")
  @Test
  void v7_application_must_not_import_mybatis_mappers() {
    noClasses()
        .that()
        .resideInAPackage("io.spring.application..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("io.spring.infrastructure.mybatis..")
        .because("V7 — application must not import persistence mappers")
        .check(classes);
  }
}
