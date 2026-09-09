# Hexagonal dependency-direction report (step 7)

This is the compile-time proof that the `io.spring.core` → `io.spring.domain` rename and the Gradle split were not cosmetic. `:domain` cannot see Spring, MyBatis, Jackson, or Security on its classpath. ArchUnit enforces the same directions on bytecode.

## How to run ArchUnit

Use JDK 11 or 17 (Gradle 7.4 cannot run on the host JDK 25):

```bash
export JAVA_HOME=/tmp/jdk17-temurin
./gradlew :bootstrap:test --tests io.spring.architecture.HexagonalArchitectureTest
```

The same rules run in CI because `./gradlew test` includes `:bootstrap:test`.

Rules live in `bootstrap/src/test/java/io/spring/architecture/HexagonalArchitectureTest.java`. They import production classes from `io.spring` (excluding tests). V3 (Lombok on entities) stays `@Disabled` — optional, not required by the assignment.

Enabled directions:

| Layer | Must not depend on |
| --- | --- |
| `io.spring.domain..` | `org.springframework..`, `org.springframework.security..`, `org.mybatis..`, `com.fasterxml..`, `javax.servlet..`, `io.spring.application..`, `io.spring.infrastructure..`, `io.spring.api..`, `io.spring.graphql..`, root `io.spring.Util` |
| `io.spring.application..` | `org.mybatis..`, `io.spring.infrastructure..`, `io.spring.api..`, `io.spring.graphql..`, `org.springframework.security..` |

Adapters (`io.spring.api..`, `io.spring.graphql..`, `io.spring.infrastructure..`) may depend inward (application, domain). Domain must not depend outward.

## Gradle: `:domain` has no framework compile deps

```bash
export JAVA_HOME=/tmp/jdk17-temurin
./gradlew :domain:dependencies --configuration compileClasspath
```

Expected `compileClasspath`: **joda-time** and Lombok (`compileOnly`). There is no Spring, MyBatis, Jackson, or Spring Security (`domain/build.gradle`). Lombok is not a runtime dependency.

Contrast:

| Module | May see (compile) |
| --- | --- |
| `:domain` | joda-time |
| `:application` | `:domain`, Spring validation/web, Jackson annotations |
| `:adapter-persistence` | `:domain`, `:application`, MyBatis |
| `:adapter-web` | `:application` (and thus `:domain`), Spring Web/Security, DGS, JJWT |
| `:bootstrap` | all modules + Flyway + SQLite |

## Ports → adapters

### Domain write ports (`io.spring.domain`) → persistence

| Port | Adapter |
| --- | --- |
| `ArticleRepository` | `MyBatisArticleRepository` |
| `CommentRepository` | `MyBatisCommentRepository` |
| `ArticleFavoriteRepository` | `MyBatisArticleFavoriteRepository` |
| `UserRepository` | `MyBatisUserRepository` |

MyBatis mapper XML lives in `:adapter-persistence`. Write-side entities stay annotation-free.

### Domain hashing port → web adapter

| Port | Adapter |
| --- | --- |
| `PasswordHasher` | `BCryptPasswordHasher` (`adapter-web`, wraps Spring `BCryptPasswordEncoder`) |

JWT is not a domain port. `JwtService` / `DefaultJwtService` live in `io.spring.api.security` and are used by `JwtTokenFilter` and login REST/GraphQL adapters.

### Application query ports (`io.spring.application.port.out`) → MyBatis read mappers

Mappers **extend** the ports (same bean; XML namespaces unchanged):

| Query port | Adapter (`@Mapper`) |
| --- | --- |
| `ArticleReadPort` | `ArticleReadService` |
| `ArticleFavoritesReadPort` | `ArticleFavoritesReadService` |
| `CommentReadPort` | `CommentReadService` |
| `UserReadPort` | `UserReadService` |
| `TagReadPort` | `TagReadService` |
| `UserRelationshipQueryPort` | `UserRelationshipQueryService` |

XML may still materialize `io.spring.application.data.*`. Application code must not import mapper types (V7).

### Application inbound ports (`io.spring.application.port.in`) → use cases

| Inbound port | Implementation | Called from |
| --- | --- | --- |
| `UserPort` | `UserService` | `UsersApi`, `UserMutation`, `ProfileApi`, `RelationMutation` |
| `ArticlePort` | `ArticleService` | `ArticleApi`, `ArticleFavoriteApi`, `ArticleMutation` |
| `CommentPort` | `CommentService` | `CommentsApi`, `CommentMutation` |

Query facades (`ArticleQueryService`, `CommentQueryService`, `ProfileQueryService`, `UserQueryService`, `TagsQueryService`) stay in application and inject query ports, not mappers.

Domain `AuthorizationService` is called from use cases, not from REST/GraphQL.

Current user stays in the inbound adapter (`JwtTokenFilter`, `@AuthenticationPrincipal`, GraphQL `SecurityUtil`). There is no domain ThreadLocal.
