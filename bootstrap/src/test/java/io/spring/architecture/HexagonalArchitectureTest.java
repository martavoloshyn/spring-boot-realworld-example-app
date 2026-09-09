package io.spring.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** ArchUnit rules that keep hexagonal dependency direction. */
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
  void domain_must_not_depend_on_mybatis() {
    noClasses()
        .that()
        .resideInAPackage("io.spring.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.mybatis..")
        .because("domain must not depend on MyBatis")
        .check(classes);
  }

  @Test
  void domain_must_not_depend_on_jackson() {
    noClasses()
        .that()
        .resideInAPackage("io.spring.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.fasterxml.jackson..")
        .because("domain must not depend on Jackson")
        .check(classes);
  }

  @Test
  void domain_must_not_depend_on_spring() {
    noClasses()
        .that()
        .resideInAPackage("io.spring.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.springframework..")
        .because("Spring must not appear on domain ports")
        .check(classes);
  }

  @Test
  void domain_must_not_depend_on_root_util() {
    noClasses()
        .that()
        .resideInAPackage("io.spring.domain..")
        .should()
        .dependOnClassesThat()
        .haveFullyQualifiedName("io.spring.Util")
        .because("entities must not import io.spring.Util")
        .check(classes);
  }

  @Test
  void application_must_not_depend_on_infrastructure_or_mybatis() {
    noClasses()
        .that()
        .resideInAPackage("io.spring.application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("io.spring.infrastructure..", "org.mybatis..")
        .because("query services must depend on ports, not MyBatis mappers")
        .check(classes);
  }

  @Test
  void web_adapters_must_not_depend_on_repositories_or_authorization() {
    noClasses()
        .that()
        .resideInAnyPackage("io.spring.api..", "io.spring.graphql..")
        .and()
        .resideOutsideOfPackage("io.spring.api.security..")
        .should()
        .dependOnClassesThat(
            simpleNameEndingWith("Repository").or(simpleName("AuthorizationService")))
        .because("controllers/mutations must call inbound ports, not repositories")
        .check(classes);
  }

  @Test
  void jwt_must_not_live_in_domain() {
    noClasses()
        .that()
        .resideInAPackage("io.spring.domain..")
        .should()
        .haveSimpleName("JwtService")
        .because("JWT issue/parse belongs in adapter-web, not domain")
        .check(classes);
  }

  @Test
  void application_must_not_depend_on_spring_security() {
    noClasses()
        .that()
        .resideInAPackage("io.spring.application..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.springframework.security..")
        .because("application use cases must depend on PasswordHasher, not PasswordEncoder")
        .check(classes);
  }

  @Test
  void security_types_only_in_inbound_adapters() {
    noClasses()
        .that()
        .resideOutsideOfPackage("io.spring.api..")
        .and()
        .resideOutsideOfPackage("io.spring.graphql..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.springframework.security..")
        .because("Spring Security types belong only in inbound adapters")
        .check(classes);
  }

  @Test
  void application_must_not_import_mybatis_mappers() {
    noClasses()
        .that()
        .resideInAPackage("io.spring.application..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("io.spring.infrastructure.mybatis..")
        .because("application must not import persistence mappers")
        .check(classes);
  }

  @Test
  void domain_must_not_depend_on_servlet() {
    noClasses()
        .that()
        .resideInAPackage("io.spring.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("javax.servlet..")
        .because("domain must not depend on the servlet API")
        .check(classes);
  }

  @Test
  void domain_must_not_depend_on_spring_security() {
    noClasses()
        .that()
        .resideInAPackage("io.spring.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.springframework.security..")
        .because("domain must not depend on Spring Security")
        .check(classes);
  }

  @Test
  void domain_must_not_depend_on_fasterxml() {
    noClasses()
        .that()
        .resideInAPackage("io.spring.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.fasterxml..")
        .because("domain must not depend on Jackson / FasterXML")
        .check(classes);
  }

  @Test
  void domain_must_not_depend_outward() {
    noClasses()
        .that()
        .resideInAPackage("io.spring.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "io.spring.application..",
            "io.spring.infrastructure..",
            "io.spring.api..",
            "io.spring.graphql..")
        .because("domain must not depend on application or adapters")
        .check(classes);
  }

  @Test
  void application_must_not_depend_on_adapter_packages() {
    noClasses()
        .that()
        .resideInAPackage("io.spring.application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("io.spring.api..", "io.spring.graphql..", "io.spring.infrastructure..")
        .because("application must not depend on adapter packages")
        .check(classes);
  }
}
