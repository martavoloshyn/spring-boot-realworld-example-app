# Architecture violations (step 1)

Inventory of hexagonal dependency-rule breaks. **Do not “fix” them in this step.**

Shared memory with `architecture/hexagonal-plan.md`. Later agents mark an id **fixed** only when the matching ArchUnit test in `src/test/java/io/spring/architecture/HexagonalArchitectureTest.java` is **enabled** (`@Disabled` removed) and `./gradlew test` is green.

Status values: `open` · `optional` · `fixed`.

---

## V1 — Spring on domain ports

**Status:** fixed (step 2)  
**ArchUnit:** `v1_core_must_not_depend_on_spring` — **enabled**

Spring stereotypes on `io.spring.core` interfaces:

| Type | Annotation | File | Fix |
| --- | --- | --- | --- |
| `JwtService` | `@Service` | `src/main/java/io/spring/core/service/JwtService.java` | annotation removed; bean still comes from `@Component DefaultJwtService` |
| `UserRepository` | `@Repository` | `src/main/java/io/spring/core/user/UserRepository.java` | annotation removed; bean still comes from `@Repository MyBatisUserRepository` |

Already annotation-free (not this id): `ArticleRepository`, `CommentRepository`, `ArticleFavoriteRepository`.

`io.spring.core..` no longer depends on `org.springframework..`. `JwtService` still *lives* in `core` — that is V6, closed in step 4.

---

## V2 — Domain depends on non-domain helper

**Status:** fixed (step 2)  
**ArchUnit:** `v2_core_must_not_depend_on_root_util` — **enabled**

`User` and `Article` imported root `io.spring.Util`.

`isEmpty` moved into the domain as `io.spring.core.shared.Strings`; `User` and `Article` call `Strings.isEmpty`. `io.spring.Util` had no other callers and was deleted, so the bootstrap package no longer holds domain logic.

---

## V3 — Lombok on domain entities (optional)

**Status:** optional — not taken in step 2, do not block later steps  
**ArchUnit:** `v3_core_must_not_depend_on_lombok` — still `@Disabled`

Assignment does not require removing Lombok. Lombok annotations are source-retention, so ArchUnit on class files may not see them; the rule still documents the purity target.

| Entity | File |
| --- | --- |
| `User` | `src/main/java/io/spring/core/user/User.java` |
| `FollowRelation` | `src/main/java/io/spring/core/user/FollowRelation.java` |
| `Article` | `src/main/java/io/spring/core/article/Article.java` |
| `Tag` | `src/main/java/io/spring/core/article/Tag.java` |
| `Comment` | `src/main/java/io/spring/core/comment/Comment.java` |
| `ArticleFavorite` | `src/main/java/io/spring/core/favorite/ArticleFavorite.java` |

---

## V4 — Application → infrastructure (CQRS reads are not ports)

**Status:** open  
**ArchUnit:** `v4_application_must_not_depend_on_infrastructure_or_mybatis`

`*QueryService` types inject MyBatis `@Mapper`s from `io.spring.infrastructure.mybatis.readservice`. This is the “left JPA/MyBatis coupling intact” failure if you only rename packages.

| Application type | MyBatis mappers |
| --- | --- |
| `ArticleQueryService` | `ArticleReadService`, `ArticleFavoritesReadService`, `UserRelationshipQueryService` |
| `CommentQueryService` | `CommentReadService`, `UserRelationshipQueryService` |
| `ProfileQueryService` | `UserReadService`, `UserRelationshipQueryService` |
| `UserQueryService` | `UserReadService` |
| `TagsQueryService` | `TagReadService` |

Target (enabled in step 4): `io.spring.application..` must not depend on `io.spring.infrastructure..` or `org.mybatis..`.

---

## V5 — Adapters orchestrate writes (no inbound ports)

**Status:** open  
**ArchUnit:** `v5_web_adapters_must_not_depend_on_repositories_or_authorization`

REST/GraphQL call repositories and build aggregates instead of inbound ports. Authorization is invoked in the adapter, not a use case.

| Flow | REST | GraphQL |
| --- | --- | --- |
| Favorite | `ArticleFavoriteApi` | `ArticleMutation` |
| Comments | `CommentsApi` | `CommentMutation` |
| Follow | `ProfileApi` | `RelationMutation` |
| Article update/delete | `ArticleApi` uses `ArticleRepository` + `AuthorizationService` | `ArticleMutation` |
| Login | `UsersApi` uses `UserRepository` + `PasswordEncoder` + `JwtService` | `UserMutation` |

Also (same rule): `ArticleDatafetcher` injects write-side `UserRepository` for profile feed lookup.

`JwtTokenFilter` using `UserRepository` is **not** this id (inbound adapter loading the principal; allowed in step 5). The ArchUnit rule excludes `io.spring.api.security..`.

Target (step 3): no `*Repository` / `AuthorizationService` on controllers and mutations.

---

## V6 — Auth/token treated as domain

**Status:** open  
**ArchUnit:** `v6_jwt_must_not_live_in_core`, `v6_application_must_not_depend_on_spring_security`

- `JwtService` still lives in `core` (`src/main/java/io/spring/core/service/JwtService.java`); its `@Service` annotation is gone since step 2 (V1), but the type itself must move to adapter-web. Implementation: `io.spring.infrastructure.service.DefaultJwtService`.
- `PasswordEncoder` (Spring Security) is injected into application `UserService`.
- Domain `User` is the Spring Security **principal**. That is OK only if Security types stay in adapters (`@AuthenticationPrincipal`, `JwtTokenFilter`, `SecurityUtil`). `User` itself has no Security imports.

Target (steps 4–5): JWT issue/parse in adapter-web; `PasswordHasher` port in domain; no Spring Security types in `core` or application use cases.

---

## V7 — Reverse edge: persistence knows application DTOs

**Status:** open (direction to invert; mapping to DTOs stays)  
**ArchUnit:** `v7_application_must_not_import_mybatis_mappers`

MyBatis read XML / `@Mapper`s map into `io.spring.application.data.*` (and paging types in `io.spring.application`):

- Mapper interfaces: `io.spring.infrastructure.mybatis.readservice.*`
- XML: `src/main/resources/mapper/TransferData.xml`, `UserReadService.xml`, `ArticleReadService.xml`, …

**Acceptable later:** query ports live in **application** and persistence **implements** them (XML may still materialize `ArticleData` / `CommentData` / …).

**Not acceptable:** those mappers imported *from* application (today’s V4). Closing V7 means the application → mapper import is gone; it does **not** mean XML must stop mapping to application DTOs.

---

## Not violations (do not “fix” by inventing ThreadLocal)

- Application methods taking `User` as a parameter
- REST `@AuthenticationPrincipal User`
- GraphQL `SecurityUtil` reading `SecurityContextHolder`

---

## Enabled ArchUnit rules

The step-2 target for `io.spring.core..` is fully enforced:

- `core_must_not_depend_on_mybatis` (held before step 2)
- `core_must_not_depend_on_jackson` (held before step 2)
- `v1_core_must_not_depend_on_spring` (step 2)
- `v2_core_must_not_depend_on_root_util` (step 2)

Still `@Disabled`, each citing its id: V3 (optional), V4, V5, V6 (×2), V7.

---

## How to close an id

1. Implement the matching hexagonal-plan step (behavior-preserving).
2. Remove `@Disabled` from that id’s `@ArchTest`.
3. `./gradlew test` green.
4. Set **Status:** fixed here.
