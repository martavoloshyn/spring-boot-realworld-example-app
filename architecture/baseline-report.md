# Pre-refactor baseline (hexagonal)

Measured **2026-08-28** at git `30d3c11` (`Add hexagonal refactor plan as agent handoff docs.`).

Purpose: after the work in `architecture/hexagonal-plan.md`, re-run the same commands on the same machine and compare this file (and `architecture/baseline-metrics.json`). Hexagonal refactor is **behavior-preserving**; latency and test counts should stay in the same band unless you change logging, JVM, or data volume.

## How to re-measure (keep these knobs identical)

| Knob | Baseline value |
| --- | --- |
| JDK | Temurin **17.0.20.1** (project `sourceCompatibility` is 11; Gradle 7.4 cannot run on JDK 25) |
| Gradle | Wrapper **7.4**, `./gradlew test bootJar --no-daemon` |
| HTTP | `java -jar build/libs/*SNAPSHOT.jar --server.port=18080 --spring.datasource.url=jdbc:sqlite:/tmp/rw-baseline.db` |
| Dataset | 2 users (alice/bob), alice follows bob, **50** articles, 10 favorites, 5 comments on first article |
| Bench | 20 warmup + **200** sequential requests (login: 5 + 50) |
| Logging | `application.properties` has MyBatis mapper/read DEBUG — **this inflates HTTP times**. Do not change log levels between runs. |

Warm JVM HTTP numbers below are localhost, single process, SQLite file DB, DEBUG SQL. They are for **regression**, not production SLOs.

---

## 1. Build and tests

| Metric | Baseline |
| --- | --- |
| `./gradlew test bootJar --no-daemon` (cold-ish, deps already cached) | **74 s**, `BUILD SUCCESSFUL` |
| JUnit 5 test methods | **68** |
| Failures / errors / skipped | **0 / 0 / 0** |
| Sum of JUnit suite times | **2.619 s** (excludes Spring context start shared across classes) |
| Boot JAR | **68.9 MiB** (`72278681` bytes) |

### Tests by layer (method count / summed method time)

| Layer | Tests | Wall in XML |
| --- | --- | --- |
| `api` (`@WebMvcTest`) | 35 | 1.894 s |
| `application` (MyBatis + SQLite via `DbTestBase`) | 13 | 0.253 s |
| `infrastructure` | 14 | 0.135 s |
| `core` (`ArticleTest` only) | 5 | 0.001 s |
| `RealworldApplicationTests` (`@SpringBootTest` contextLoads) | 1 | 0.296 s |

Slowest tests: `ArticleApiTest#should_read_article_success` **1.04 s** (first WebMvc context), `contextLoads` **0.30 s**, `UsersApiTest#should_login_success` **0.15 s**.

### Coverage gaps (quality, not hexagon)

- **No GraphQL tests** (DGS adapters are untested).
- Almost all REST tests are **WebMvc + mocks**, not HTTP against a real Spring context.
- **No ArchUnit** yet (plan step 1).
- Feed success is mocked in `ListArticleApiTest`; the empty-feed SQL bug below is **not** caught.

---

## 2. HTTP / GraphQL performance

Startup: **Started RealWorldApplication in 3.81–3.93 s** (JVM running ~4.2–4.4 s). Process RSS after bench: **~603 MiB** (`VmRSS` 617192 kB) — typical fat Spring + DGS heap, not app logic.

Seed: create article mean **26.6 ms**, p95 **34.2 ms** (includes BCrypt only on user create, not each article).

Sequential latency (ms), localhost, 50 articles in SQLite:

| Endpoint | n | mean | p50 | p95 | p99 | HTTP |
| --- | --- | --- | --- | --- | --- | --- |
| GET `/tags` | 200 | 3.50 | 3.40 | 5.42 | 7.61 | 200 |
| GET `/articles?limit=20` anon | 200 | 10.37 | 10.31 | 14.66 | 16.95 | 200 |
| GET `/articles?limit=20` auth | 200 | 11.09 | 10.81 | 17.33 | 19.97 | 200 |
| GET `/articles/{slug}` anon | 200 | 3.99 | 4.12 | 5.62 | 5.83 | 200 |
| GET `/articles/{slug}` auth | 200 | 4.85 | 4.51 | 7.49 | 8.09 | 200 |
| GET `/articles/feed` (alice follows bob, bob has 0 articles) | 200 | 9.85 | 9.42 | 13.56 | 17.76 | **500** |
| GET `/profiles/bob` auth | 200 | 4.56 | 4.51 | 6.93 | 7.39 | 200 |
| GET `/user` | 200 | 4.24 | 4.23 | 6.46 | 7.07 | 200 |
| POST `/users/login` | 50 | 66.95 | 70.94 | 74.84 | 75.81 | 200 |
| GET `/articles/{slug}/comments` | 200 | 3.42 | 3.29 | 5.01 | 5.53 | 200 |
| POST `/graphql` `{ tags }` | 200 | 5.54 | 5.12 | 8.05 | 9.43 | 200 |
| POST `/graphql` `articles(first:20)` | 200 | 9.59 | 8.68 | 13.77 | 40.28 | 200 |
| POST `/graphql` `article(slug)` | 200 | 4.71 | 4.36 | 6.94 | 8.20 | 200 |
| POST `/graphql` article + author + comments | 200 | 6.37 | 6.24 | 8.23 | 9.69 | 200 |

Concurrent: **200** GET `/articles?limit=20` with **20** threads: wall **169 ms**, ~**1184 req/s**, p50 **10.2 ms**, p95 **19.2 ms**.

Login (~67 ms) is dominated by **BCrypt**, not routing. GraphQL list p99 (~40 ms) is a tail (GC / SQLite / DEBUG logging), not a different algorithm vs REST list.

Raw JSON: `architecture/baseline-metrics.json`.

---

## 3. Read-path cost (why list is slower than slug)

Anonymous `GET /articles` (DEBUG logs):

1. `queryArticles` — `DISTINCT` ids with **five** `LEFT JOIN`s (tags, favorites, two user aliases), then `LIMIT`.
2. `countArticle` — same join shape, `count(DISTINCT A.id)`.
3. `findArticles` — article + tags + author; 20 ids produced **30** rows (tag fan-out).

Authenticated list adds `UserMapper.findById` in `JwtTokenFilter` plus batched extras in `ArticleQueryService.fillExtraInfo` (favorite counts, user favorites, following authors). Auth vs anon list: **~0.7 ms** mean — extra queries, not N+1 per article.

`GET /articles/feed` with follows but **zero** matching articles: `fillExtraInfo` still runs `articlesFavoriteCount` with an empty id list → invalid `IN ()` SQL → **500**. Empty follow list short-circuits and returns 200. **Do not “fix” this during hexagonal steps** unless you treat it as out of scope; it is current behavior for the follow-but-empty-feed case.

Schema: 7 tables, **no indexes** except UNIQUE on `users.username`, `users.email`, `articles.slug`. Fine at this size; list SQL will dominate if data grows.

---

## 4. Size and shape

Non-comment lines of code (NLOC) by tree (generated DGS Java under `build/` excluded):

| Area | Files | Lines | NLOC |
| --- | --- | --- | --- |
| `io.spring.api` | 18 | 921 | 824 |
| `io.spring.application` | 31 | 1000 | 856 |
| `io.spring.core` | 12 | 284 | 228 |
| `io.spring.graphql` | 12 | 1134 | 1045 |
| `io.spring.infrastructure` | 16 | 478 | 380 |
| `io.spring` bootstrap | 4 | 71 | 58 |
| MyBatis XML | 11 | 506 | 493 |
| GraphQL schema | 1 | 177 | 150 |
| Flyway SQL | 1 | 49 | 43 |
| Tests | 23 | 2082 | 1785 |

**93** main Java types. Stereotypes (files containing): `@RestController` 9, `@Service` 7, `@Mapper` 10, `@Repository` 4, `@Component` 4, `@Configuration` 3, `@Transactional` 1.

REST handlers: **21** mappings. GraphQL `@Dgs*` methods: **~30** (queries, mutations, field resolvers).

Domain (`core`) is small: `User`, `Article`, `Comment`, `Tag`, `ArticleFavorite`, `FollowRelation`, four repository interfaces, `AuthorizationService`, `JwtService`.

---

## 5. Coupling (hexagonal health)

Target from the plan: domain/application do not import Spring/MyBatis/adapters; adapters call inbound ports only.

### Cross-package imports (unique main files)

| From | To | Files |
| --- | --- | --- |
| api | application | 8 |
| api | core | 8 |
| application | core | 9 |
| application | **infrastructure** | **5** (V4) |
| graphql | application | 9 |
| graphql | core | 9 |
| graphql | **api** | **8** (shared exceptions) |
| infrastructure | core | 10 |
| infrastructure | **application** | **4** (read mappers → `application.data`, V7) |
| core | bootstrap (`io.spring.Util`) | **2** (V2) |

### Framework on “inner” packages

- **core + Spring:** `JwtService` (`@Service`), `UserRepository` (`@Repository`) — V1.
- **application + Spring Security:** `UserService` → `PasswordEncoder` — V6.
- **application + MyBatis types:** query services inject `*ReadService` `@Mapper`s — V4.
- **api/graphql write orchestration:** controllers/mutations use `*Repository` + `AuthorizationService` — V5.

After refactor, expect: those five application→infrastructure files **0**; core Spring annotations **0**; REST/GQL without repository fields; ArchUnit green.

---

## 6. Other characteristics worth tracking

| Characteristic | Baseline | After refactor, watch |
| --- | --- | --- |
| Gradle modules | **1** | 5 (`domain`, `application`, `adapter-*`, `bootstrap`) — compile isolation |
| ArchUnit | none | all rules enabled (plan step 7) |
| JWT | HS512, secret in `application.properties` | same header `Authorization: Token …` |
| Password | `BCryptPasswordEncoder` default | login p50 still ~70 ms |
| CORS | `*` origins, credentials false | matcher behavior frozen |
| CSRF | disabled | frozen |
| Session | `STATELESS` | frozen |
| Unused Boot auto-config | logs **generated security password** (`UserDetailsServiceAutoConfiguration`) | noise only |
| Time | Joda-Time on `Article`/`Comment` | do not switch without checking JSON |
| Persistence | MyBatis 2.2.2 + SQLite + Flyway 8 | **do not migrate to JPA** |
| GraphQL | DGS 4.9.21, `/graphql` permitAll | same schema |

---

## 7. Suggested comparison checklist after step 7

1. `./gradlew test` — still **68+** tests, **0** failures (ArchUnit may **add** tests).
2. Boot JAR size — ± a few MB is noise; large jump means extra deps.
3. Repeat HTTP table with the same seed and DEBUG flags — p50 within ~20% on this machine is “same”.
4. `GET /articles/feed` with follow-and-empty-articles still **500** unless you explicitly fix it.
5. Coupling table: application→infrastructure files = 0; core Spring files = 0.
6. Startup still ~4 s class of number on this host.

Do not treat hexagonal package moves as a performance project. If p95 list latency moves a lot, check log level, pool size, and SQL shape first.
