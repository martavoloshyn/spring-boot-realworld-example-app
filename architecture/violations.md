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

**Status:** fixed (step 4)  
**ArchUnit:** `v4_application_must_not_depend_on_infrastructure_or_mybatis` — **enabled**

Query services inject application query ports (`io.spring.application.port.out`). MyBatis `@Mapper`s in `io.spring.infrastructure.mybatis.readservice` implement those ports and keep `@Param` plus XML namespaces.

| Application type | Query port |
| --- | --- |
| `ArticleQueryService` | `ArticleReadPort`, `ArticleFavoritesReadPort`, `UserRelationshipQueryPort` |
| `CommentQueryService` | `CommentReadPort`, `UserRelationshipQueryPort` |
| `ProfileQueryService` | `UserReadPort`, `UserRelationshipQueryPort` |
| `UserQueryService` | `UserReadPort` |
| `TagsQueryService` | `TagReadPort` |

`io.spring.application..` no longer depends on `io.spring.infrastructure..` or `org.mybatis..`.

---

## V5 — Adapters orchestrate writes (no inbound ports)

**Status:** fixed (step 3)  
**ArchUnit:** `v5_web_adapters_must_not_depend_on_repositories_or_authorization` — **enabled**

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

**Status:** fixed (step 4)  
**ArchUnit:** `v6_jwt_must_not_live_in_core`, `v6_application_must_not_depend_on_spring_security` — **enabled**

- `JwtService` and `DefaultJwtService` live in adapter-web (`io.spring.api.security`). Domain has no token types.
- `PasswordHasher` is a domain outbound port (`io.spring.core.user.PasswordHasher`). `BCryptPasswordHasher` wraps `PasswordEncoder` in adapter-web. `UserService` depends on the port.
- Domain `User` is still the Spring Security principal. Security types stay in adapters (`@AuthenticationPrincipal`, `JwtTokenFilter`, `SecurityUtil`). Step 5 still owns filter/principal wiring.

---

## V7 — Reverse edge: persistence knows application DTOs

**Status:** fixed (step 4)  
**ArchUnit:** `v7_application_must_not_import_mybatis_mappers` — **enabled**

Query ports live in application. Persistence implements them. XML may still materialize `ArticleData` / `CommentData` / paging types. Application no longer imports mapper types.

---

## Not violations (do not “fix” by inventing ThreadLocal)

- Application methods taking `User` as a parameter
- REST `@AuthenticationPrincipal User`
- GraphQL `SecurityUtil` reading `SecurityContextHolder`

---

## Enabled ArchUnit rules

- `core_must_not_depend_on_mybatis` (held before step 2)
- `core_must_not_depend_on_jackson` (held before step 2)
- `v1_core_must_not_depend_on_spring` (step 2)
- `v2_core_must_not_depend_on_root_util` (step 2)
- `v4_application_must_not_depend_on_infrastructure_or_mybatis` (step 4)
- `v5_web_adapters_must_not_depend_on_repositories_or_authorization` (step 3)
- `v6_jwt_must_not_live_in_core` (step 4)
- `v6_application_must_not_depend_on_spring_security` (step 4)
- `v7_application_must_not_import_mybatis_mappers` (step 4)

Still `@Disabled`, citing its id: V3 (optional).

---

## How to close an id

1. Implement the matching hexagonal-plan step (behavior-preserving).
2. Remove `@Disabled` from that id’s `@ArchTest`.
3. `./gradlew test` green.
4. Set **Status:** fixed here.
