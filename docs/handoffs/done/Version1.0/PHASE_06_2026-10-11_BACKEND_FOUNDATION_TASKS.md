# Phase 06 · Backend foundation: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `backend/` is a new Spring Boot 4.1 / Java 21 service where players register with the group's invite code, log in once per device, open and join tables with buy-ins in points, and the hand evaluator passes its vectors in the model's card notation. ArchUnit, Flyway on PostgreSQL, a committed OpenAPI contract and a testworld profile guard it.

**Architecture:** Packages are `api` (controllers, `api.dto` records) → `service` → `repository` (JPA entities), plus a pure `domain` and the `config`/`security` wiring, all under `com.spade`. ArchUnit enforces the layers from the first task.
- **Errors:** every error is an RFC 9457 `ProblemDetail`.
- **Auth:** an opaque per-device session token whose SHA-256 is stored (ADR 0011).
- **Tests:** they run against a throwaway PostgreSQL in Docker (Testcontainers), never H2.

**Tech Stack:**
- Spring Boot **4.1.1**. The parent manages Spring Framework 7, Spring Security 7.1.1, Hibernate 7.4.5, Jackson 3.1.5, Flyway 12.4.0, PostgreSQL JDBC 42.7.13 and Testcontainers 2.0.5.
- Java 21; ArchUnit **1.5.1**; springdoc-openapi **3.1.1**; JUnit 5; MockMvc.
- Checked against Maven Central and the Boot 4.1 docs on 2026-10-11:
  - Boot 4 splits the starters (`spring-boot-starter-webmvc`, `-flyway`, `-webmvc-test`, `-security-test`);
  - `@AutoConfigureMockMvc` lives in `org.springframework.boot.webmvc.test.autoconfigure`;
  - Testcontainers 2's class is `org.testcontainers.postgresql.PostgreSQLContainer`;
  - the generated-user auto-configuration is `org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration`.

**Spec:** [PHASE_06_2026-10-11_BACKEND_FOUNDATION.md](PHASE_06_2026-10-11_BACKEND_FOUNDATION.md) · [V1 spec](../../V1_2026-10-11_DEALER.md) · ADRs [0006](../../../adr/0006-physical-chips-are-the-truth-spade-mirrors-them.md), [0010](../../../adr/0010-one-card-notation-the-models-labels.md), [0011](../../../adr/0011-device-sessions-not-jwts-the-hub-pairs.md), [0012](../../../adr/0012-v1-stacks-spring-boot-4-react-hub-swiftui-app.md)

**Before Task 1:** start Docker Desktop on the Mac (`docker info` must succeed). Every backend test starts a `postgres:18-alpine` container. Work only in `.worktrees/phase-06-backend-foundation` (branch `phase/06-backend-foundation`). All `./mvnw` commands run in `backend/`.

## Global Constraints
- **Versions:** Java 21. Spring Boot parent `4.1.1`. Don't pin versions the parent manages; `flyway-database-postgresql` uses `${flyway.version}`.
- **Layout:** folder `backend/`, package root `com.spade` (phase D1).
- **Card notation:** rank `2`–`10`, `J`, `Q`, `K`, `A` + suit `C`, `D`, `H`, `S`, upper case, as one string (`"10H"`, `"AS"`). Anything else is rejected (ADR 0010).
- **Amounts:**
  - whole points as `long` (phase D3);
  - every amount put on the table is a positive multiple of **5** (the smallest chip) and at most **1,000,000**;
  - a cash-out may be **0** (ADR 0006).
- **Invite code:** registration needs the group's invite code, compared as SHA-256 hex; only the hash is configured (phase D2).
- **Session tokens:** 32 random bytes, base64url without padding. Only their SHA-256 hex is stored. A token is never logged and never written to a file (ADR 0011, CLAUDE.md).
- **Errors:**
  - every non-2xx response is `application/problem+json`;
  - the catch-all never echoes exception text;
  - an error is never answered with 200.
- **Database:** tests use Testcontainers PostgreSQL `postgres:18-alpine`, never H2 (phase D7). Schema changes go through Flyway only; `ddl-auto: validate`.
- **Copy:** English; messages say what went wrong and how to fix it.
- **Off limits:** never edit `client/` or `webapp/`; don't delete `spadeboot/` (that happens at V1 close); never `git stash`.
- **Commits:** `type(scope): outcome (Phase 06, D#, #issue)`, scope `backend`, `devex` or `docs`.

## Review Focus
1. **Usernames typed with different capitals** ("Ana", then "ANA") belong to one account. Registering "ana" again is refused with 409, and logging in as "ANA" works. → Task 7 and Task 8 tests.
2. **Behind the Cloudflare Tunnel (ADR 0007) every request arrives from the tunnel's address.** The login throttle must key on `CF-Connecting-IP`, or one person's typos lock the whole group out. → Task 9 test.
3. **A revoked, unknown or malformed token** on any protected endpoint answers `401 application/problem+json`, never 200, 403 or 500. → Task 8 test.
4. **Amounts no stack of chips can hold** are refused with a message naming the smallest chip: 0 (except a cash-out), negative, not a multiple of 5, or absurdly large. → Task 10 tests.
5. **A password over BCrypt's 72-byte limit made of multibyte characters** (40 × "ü" = 80 bytes) is refused with 400, not a 500. → Task 7 test.

## File structure
```
backend/
  pom.xml · mvnw · mvnw.cmd · .mvn/wrapper/maven-wrapper.properties   (wrapper copied from spadeboot/)
  run.sh · .env.example
  src/main/java/com/spade/
    SpadeApplication.java
    api/            PlayerController, SessionController, TableController, ApiExceptionHandler, ClientAddress
    api/dto/        request and response records
    service/        AccountService, SessionService, LoginThrottle, TableService, Hashing, SessionTokens,
                    SpadeProperties, SpadePrincipal, PlayerView, SessionView, LoginResult, TableView, SeatView
    repository/     *Entity + *Repository for player, device_session, poker_table, seat, buy_in, cash_out
    domain/         DomainException, Chips
    domain/card/    Rank, Suit, Card
    domain/eval/    Category, HandRank, HandEvaluator, Showdown
    config/         ClockConfig
    security/       SecurityConfig, BearerSessionFilter, ProblemAuthenticationEntryPoint
    testworld/      TestworldClock, TestworldSeed, TestworldController   (profile "testworld" only)
  src/main/resources/
    application.yml · application-testworld.yml
    db/migration/V1__accounts.sql · V2__tables.sql
  src/test/java/com/spade/
    TestcontainersConfiguration, SpadeIntegrationTest, TestworldTest, TestSpadeApplication, MutableClock
    MigrationsTest, ArchitectureTest
    domain/card/CardTest · domain/eval/HandEvaluatorTest · domain/eval/ShowdownTest
    api/ApiErrorsTest · api/PlayerApiTest · api/SessionApiTest · api/LoginThrottleApiTest · api/TableApiTest
    api/TestPlayers · api/ContractTest
    service/LoginThrottleTest · service/SpadePropertiesTest · testworld/TestworldModeTest
  src/test/java/probe/ErrorProbeController.java   (outside com.spade: never component-scanned)
api/openapi.json · api/fixtures/*.json            (repo root: the contract of all three apps, phase D6)
scripts/gate.sh · .github/workflows/gate.yml · CLAUDE.md · USAGE.md · docs/status-quo/backend.md
```

---

### Task 1: A project that boots on PostgreSQL

**Files:**
- Create: `backend/pom.xml`, `backend/src/main/java/com/spade/SpadeApplication.java`, `backend/src/main/resources/application.yml`, `backend/src/main/resources/db/migration/V1__accounts.sql`
- Copy: `spadeboot/mvnw`, `spadeboot/mvnw.cmd`, `spadeboot/.mvn/wrapper/maven-wrapper.properties` → `backend/` (same relative paths)
- Test: `backend/src/test/java/com/spade/TestcontainersConfiguration.java`, `SpadeIntegrationTest.java`, `TestSpadeApplication.java`, `MigrationsTest.java`

**Interfaces:**
- Produces:
  - `@SpadeIntegrationTest`: a full Spring context with Testcontainers PostgreSQL and MockMvc. The invite code is `letmein`, whose SHA-256 is `1c8bfe8f801d79745c4631d09fff36c82aa37fc4cce4fc946683d7b336b63032`.
  - Tables `player` and `device_session`.

- [ ] **Step 1: Copy the Maven wrapper and write the POM**

```bash
mkdir -p backend/.mvn/wrapper && cp spadeboot/mvnw spadeboot/mvnw.cmd backend/ && cp spadeboot/.mvn/wrapper/maven-wrapper.properties backend/.mvn/wrapper/
```

`backend/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.1.1</version>
        <relativePath/>
    </parent>
    <groupId>com.spade</groupId>
    <artifactId>backend</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>spade-backend</name>
    <description>Spade V1 backend: accounts, tables, the hand engine (ADR 0012)</description>

    <properties>
        <java.version>21</java.version>
        <archunit.version>1.5.1</archunit.version>
        <springdoc.version>3.1.1</springdoc.version>
    </properties>

    <dependencies>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-webmvc</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-flyway</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
            <version>${flyway.version}</version>
        </dependency>
        <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>

        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-webmvc-test</artifactId><scope>test</scope></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security-test</artifactId><scope>test</scope></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-testcontainers</artifactId><scope>test</scope></dependency>
        <dependency><groupId>org.testcontainers</groupId><artifactId>testcontainers-junit-jupiter</artifactId><scope>test</scope></dependency>
        <dependency><groupId>org.testcontainers</groupId><artifactId>testcontainers-postgresql</artifactId><scope>test</scope></dependency>
        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
            <version>${archunit.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Write the failing test and the test scaffolding**

`backend/src/test/java/com/spade/TestcontainersConfiguration.java`:
```java
package com.spade;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** A throwaway PostgreSQL for every test context (phase D7: the database that runs on the night, not H2). */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:18-alpine");
    }
}
```

`backend/src/test/java/com/spade/SpadeIntegrationTest.java`:
```java
package com.spade;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/**
 * The full app on a throwaway PostgreSQL, with MockMvc.
 * The tests' invite code is "letmein"; the property is its SHA-256.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest(properties = "spade.invite-code-sha256=1c8bfe8f801d79745c4631d09fff36c82aa37fc4cce4fc946683d7b336b63032")
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
public @interface SpadeIntegrationTest {
}
```

`backend/src/test/java/com/spade/TestSpadeApplication.java`:
```java
package com.spade;

import org.springframework.boot.SpringApplication;

/** Runs the app on a throwaway PostgreSQL: ./mvnw spring-boot:test-run -Dspring-boot.run.profiles=testworld */
public class TestSpadeApplication {

    public static void main(String[] args) {
        SpringApplication.from(SpadeApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
```

`backend/src/test/java/com/spade/MigrationsTest.java`:
```java
package com.spade;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@SpadeIntegrationTest
class MigrationsTest {

    @Autowired
    Flyway flyway;

    @Test
    void everyMigrationAppliesToAnEmptyDatabase() {
        MigrationInfo[] applied = flyway.info().applied();
        assertThat(applied).isNotEmpty();
        assertThat(applied).allSatisfy(m -> assertThat(m.getState().isApplied()).as(m.getScript()).isTrue());
        assertThat(flyway.info().pending()).isEmpty();
    }
}
```

- [ ] **Step 3: Run it and watch it fail**

Run: `cd backend && ./mvnw -q test -Dtest=MigrationsTest`
Expected: COMPILATION ERROR, `cannot find symbol: class SpadeApplication`.

- [ ] **Step 4: Write the application, its config and the first migration**

`backend/src/main/java/com/spade/SpadeApplication.java`:
```java
package com.spade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/**
 * Spade's backend (ADR 0012). The generated-user auto-configuration is excluded: it would print a
 * password to the log, and Spade's sessions are its own (ADR 0011).
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class SpadeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpadeApplication.class, args);
    }
}
```

`backend/src/main/resources/application.yml`:
```yaml
spring:
  application:
    name: spade
  datasource:
    url: ${SPADE_DB_URL:jdbc:postgresql://localhost:5432/spade}
    username: ${SPADE_DB_USER:spade}
    password: ${SPADE_DB_PASSWORD:}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
  flyway:
    locations: classpath:db/migration

server:
  error:
    include-message: never
    include-stacktrace: never
    include-exception: false

management:
  endpoints:
    web:
      exposure:
        include: health
```

`backend/src/main/resources/db/migration/V1__accounts.sql`:
```sql
-- Players and their device sessions (ADR 0011). A session token is stored only as its SHA-256.
create table player (
    id            bigint generated by default as identity primary key,
    username      varchar(32)  not null unique,
    display_name  varchar(40)  not null,
    password_hash varchar(100) not null,
    created_at    timestamptz  not null
);

create table device_session (
    id           bigint generated by default as identity primary key,
    player_id    bigint       not null references player (id),
    token_sha256 varchar(64)  not null unique,
    device_name  varchar(60)  not null,
    created_at   timestamptz  not null,
    last_used_at timestamptz  not null,
    revoked_at   timestamptz
);

create index device_session_player on device_session (player_id);
```

- [ ] **Step 5: Run it and watch it pass**

Run: `cd backend && ./mvnw -q test -Dtest=MigrationsTest`
Expected: PASS (`Tests run: 1, Failures: 0`). The first run pulls `postgres:18-alpine`.

- [ ] **Step 6: Commit**

```bash
git add backend/
git commit -m "feat(backend): backend/ boots on Spring Boot 4.1 and PostgreSQL; migrations apply to an empty database (Phase 06, D1, D7)"
```

---

### Task 2: The architecture rules, each proven by a planted violation

**Files:**
- Test: `backend/src/test/java/com/spade/ArchitectureTest.java`

**Interfaces:**
- Produces: the rules every later task must pass.
  - Packages `com.spade.{api, api.dto, service, repository, domain, config, security, testworld}`.
  - `config` and `security` together form the "Config" layer.
  - `testworld` is outside the layers, and every class in it carries `@Profile`.

- [ ] **Step 1: Write the rules**

`backend/src/test/java/com/spade/ArchitectureTest.java`:
```java
package com.spade;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import jakarta.persistence.Entity;
import org.springframework.context.annotation.Profile;

/**
 * The layers and rules of ADR 0012, from the first commit (WealthWatcher retrofitted its layers and spent
 * phases on the exemptions). Every rule was shown to fail once on a planted violation: see Task 2, Step 3.
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
                    "com.spade.config..", "com.spade.security..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule apiNeverSeesEntities = noClasses().that().resideInAPackage("com.spade.api..")
            .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule dtosAreRecords = classes().that().resideInAPackage("com.spade.api.dto..")
            .should().beRecords()
            .allowEmptyShould(true);

    /** PRODUCT.md: hole cards stay private until showdown. A public view can't even name them (phase 07). */
    @ArchTest
    static final ArchRule publicViewsNeverNameHoleCards = noClasses().that().haveSimpleNameEndingWith("PublicView")
            .should().dependOnClassesThat().haveSimpleName("HoleCards")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule noFieldInjection = GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

    @ArchTest
    static final ArchRule noStandardStreams = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    @ArchTest
    static final ArchRule testworldOnlyUnderItsProfile = classes().that().resideInAPackage("com.spade.testworld..")
            .should().beAnnotatedWith(Profile.class)
            .allowEmptyShould(true);
}
```

- [ ] **Step 2: Run it on the clean tree**

Run: `cd backend && ./mvnw -q test -Dtest=ArchitectureTest`
Expected: PASS (8 tests).

- [ ] **Step 3: Prove every rule by planting one violation per rule (temporary files, never committed)**

Create these files in `backend/src/main/java/`:
```java
// com/spade/repository/PlantedEntity.java
package com.spade.repository;
@jakarta.persistence.Entity public class PlantedEntity { @jakarta.persistence.Id Long id; }

// com/spade/api/PlantedApi.java  (violates: layers, apiNeverSeesEntities)
package com.spade.api;
public class PlantedApi { public com.spade.repository.PlantedEntity leak() { return null; } }

// com/spade/api/dto/PlantedDto.java  (violates: dtosAreRecords)
package com.spade.api.dto;
public class PlantedDto { }

// com/spade/domain/PlantedDomain.java  (violates: domainIsPure)
package com.spade.domain;
public class PlantedDomain { boolean x() { return org.springframework.util.StringUtils.hasText("x"); } }

// com/spade/domain/HoleCards.java and com/spade/domain/PlantedPublicView.java  (violates: publicViewsNeverNameHoleCards)
package com.spade.domain;
public class HoleCards { }
package com.spade.domain;
public class PlantedPublicView { HoleCards cards; }

// com/spade/service/PlantedService.java  (violates: noFieldInjection, noStandardStreams)
package com.spade.service;
public class PlantedService {
    @org.springframework.beans.factory.annotation.Autowired private java.time.Clock clock;
    void shout() { System.out.println("x"); }
}

// com/spade/testworld/PlantedTestworld.java  (violates: testworldOnlyUnderItsProfile)
package com.spade.testworld;
public class PlantedTestworld { }
```

Run: `cd backend && ./mvnw -q test -Dtest=ArchitectureTest`
Expected: FAIL, with each of the 8 rules reported as violated: `layers`, `domainIsPure`, `apiNeverSeesEntities`, `dtosAreRecords`, `publicViewsNeverNameHoleCards`, `noFieldInjection`, `noStandardStreams`, `testworldOnlyUnderItsProfile`. If a rule passes, it guards nothing: fix the rule before going on.

Delete the planted files and re-run:
```bash
rm backend/src/main/java/com/spade/repository/PlantedEntity.java backend/src/main/java/com/spade/api/PlantedApi.java \
   backend/src/main/java/com/spade/api/dto/PlantedDto.java backend/src/main/java/com/spade/domain/PlantedDomain.java \
   backend/src/main/java/com/spade/domain/HoleCards.java backend/src/main/java/com/spade/domain/PlantedPublicView.java \
   backend/src/main/java/com/spade/service/PlantedService.java backend/src/main/java/com/spade/testworld/PlantedTestworld.java
cd backend && ./mvnw -q test -Dtest=ArchitectureTest
```
Expected: PASS. In the phase doc's "What shipping it actually found", note one line per rule saying it failed on its plant.

- [ ] **Step 4: Commit**

```bash
git status --short   # only ArchitectureTest.java is new; no Planted* file
git add backend/src/test/java/com/spade/ArchitectureTest.java
git commit -m "test(backend): ArchUnit layers, domain purity, record DTOs, no hole cards in public views — each rule failed once on a planted violation (Phase 06, ADR 0012)"
```

---

### Task 3: Cards in the model's notation

**Files:**
- Create: `backend/src/main/java/com/spade/domain/DomainException.java`, `domain/card/Rank.java`, `domain/card/Suit.java`, `domain/card/Card.java`
- Test: `backend/src/test/java/com/spade/domain/card/CardTest.java`

**Interfaces:**
- Produces:
  - `DomainException(Kind kind, String message)` with `Kind { INVALID, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, CONFLICT, TOO_MANY_REQUESTS }` and `kind()`;
  - `Rank` with `value()` (2–14) and `label()`;
  - `Suit` with `label()` ('C','D','H','S');
  - `record Card(Rank rank, Suit suit)` with `static Card parse(String)`, `static List<Card> parseAll(String)`, `String label()`, `static List<Card> deck()`.

- [ ] **Step 1: Write the failing test**

`backend/src/test/java/com/spade/domain/card/CardTest.java`:
```java
package com.spade.domain.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.spade.domain.DomainException;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CardTest {

    @Test
    void everyCardOfTheDeckRoundTripsThroughItsLabel() {
        List<Card> deck = Card.deck();
        assertThat(deck).hasSize(52);
        assertThat(new HashSet<>(deck)).hasSize(52);
        for (Card card : deck) {
            assertThat(Card.parse(card.label())).isEqualTo(card);
        }
    }

    @Test
    void readsTheModelsLabels() {
        assertThat(Card.parse("AS")).isEqualTo(new Card(Rank.ACE, Suit.SPADES));
        assertThat(Card.parse("10H")).isEqualTo(new Card(Rank.TEN, Suit.HEARTS));
        assertThat(Card.parse("JD").label()).isEqualTo("JD");
        assertThat(Card.parseAll(" 10H  AS QC ")).extracting(Card::label).containsExactly("10H", "AS", "QC");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"as", "1H", "TH", "11H", "10", "AX", "AAS", " AS", "AS ", "10h", "ACEH", "THREES"})
    void rejectsAnythingThatIsNotAModelLabel(String label) {
        assertThatThrownBy(() -> Card.parse(label))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Not a card")
                .extracting(e -> ((DomainException) e).kind()).isEqualTo(DomainException.Kind.INVALID);
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

Run: `cd backend && ./mvnw -q test -Dtest=CardTest`
Expected: COMPILATION ERROR, `package com.spade.domain does not exist`.

- [ ] **Step 3: Write the domain types**

`backend/src/main/java/com/spade/domain/DomainException.java`:
```java
package com.spade.domain;

/**
 * A rule of the game or the app was broken. The message is written for the person at the table and is
 * shown to them as-is; never put internals or secrets in it. {@link Kind} picks the HTTP status in api.
 */
public class DomainException extends RuntimeException {

    public enum Kind { INVALID, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, CONFLICT, TOO_MANY_REQUESTS }

    private final Kind kind;

    public DomainException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
```

`backend/src/main/java/com/spade/domain/card/Rank.java`:
```java
package com.spade.domain.card;

/** Card ranks with their value (ace high = 14) and their label in the model's notation (ADR 0010). */
public enum Rank {
    TWO(2, "2"), THREE(3, "3"), FOUR(4, "4"), FIVE(5, "5"), SIX(6, "6"), SEVEN(7, "7"), EIGHT(8, "8"),
    NINE(9, "9"), TEN(10, "10"), JACK(11, "J"), QUEEN(12, "Q"), KING(13, "K"), ACE(14, "A");

    private final int value;
    private final String label;

    Rank(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int value() {
        return value;
    }

    public String label() {
        return label;
    }

    static Rank fromLabel(String label) {
        for (Rank rank : values()) {
            if (rank.label.equals(label)) {
                return rank;
            }
        }
        throw new IllegalArgumentException("unknown rank label " + label);
    }
}
```

`backend/src/main/java/com/spade/domain/card/Suit.java`:
```java
package com.spade.domain.card;

/** Suits with their one-letter label in the model's notation (ADR 0010). */
public enum Suit {
    CLUBS('C'), DIAMONDS('D'), HEARTS('H'), SPADES('S');

    private final char label;

    Suit(char label) {
        this.label = label;
    }

    public char label() {
        return label;
    }

    static Suit fromLabel(char label) {
        for (Suit suit : values()) {
            if (suit.label == label) {
                return suit;
            }
        }
        throw new IllegalArgumentException("unknown suit label " + label);
    }
}
```

`backend/src/main/java/com/spade/domain/card/Card.java`:
```java
package com.spade.domain.card;

import com.spade.domain.DomainException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** One playing card. Written everywhere as the model writes it: "AS", "10H" (ADR 0010). */
public record Card(Rank rank, Suit suit) {

    private static final Pattern LABEL = Pattern.compile("(10|[2-9JQKA])([CDHS])");

    public Card {
        Objects.requireNonNull(rank, "rank");
        Objects.requireNonNull(suit, "suit");
    }

    public static Card parse(String label) {
        Matcher m = label == null ? null : LABEL.matcher(label);
        if (m == null || !m.matches()) {
            throw new DomainException(DomainException.Kind.INVALID,
                    "Not a card: '" + label + "'. Write the rank, then the suit, in capitals: AS, 10H, QD.");
        }
        return new Card(Rank.fromLabel(m.group(1)), Suit.fromLabel(m.group(2).charAt(0)));
    }

    /** Parses space-separated labels, e.g. "10H AS QC". */
    public static List<Card> parseAll(String labels) {
        return Arrays.stream(labels.strip().split("\\s+")).map(Card::parse).toList();
    }

    public static List<Card> deck() {
        List<Card> deck = new ArrayList<>(52);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                deck.add(new Card(rank, suit));
            }
        }
        return List.copyOf(deck);
    }

    public String label() {
        return rank.label() + suit.label();
    }

    @Override
    public String toString() {
        return label();
    }
}
```

- [ ] **Step 4: Run it and watch it pass**

Run: `cd backend && ./mvnw -q test -Dtest='CardTest,ArchitectureTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/spade/domain backend/src/test/java/com/spade/domain
git commit -m "feat(backend): cards read and write the model's labels (AS, 10H) and reject every other notation (Phase 06, ADR 0010, #5)"
```

---

### Task 4: The hand evaluator and its vectors

**Files:**
- Create: `backend/src/main/java/com/spade/domain/eval/Category.java`, `HandRank.java`, `HandEvaluator.java`
- Test: `backend/src/test/java/com/spade/domain/eval/HandEvaluatorTest.java`

**Interfaces:**
- Consumes: `Card`, `Rank.value()`, `Suit`, `DomainException` (Task 3).
- Produces:
  - `enum Category { HIGH_CARD(1) … STRAIGHT_FLUSH(9) }` with `code()`;
  - `record HandRank(Category category, List<Integer> deciders) implements Comparable<HandRank>`, with `long value()` = category × 100⁵ + the five deciders (two digits each, the encoding of the 18 V0 vectors);
  - `HandEvaluator.evaluate(Collection<Card>) → HandRank` for 5–7 distinct cards. It never modifies its argument.

- [ ] **Step 1: Write the failing test: the 18 V0 vectors in model notation, plus the predecessor's asserting analyzer cases**

`backend/src/test/java/com/spade/domain/eval/HandEvaluatorTest.java`:
```java
package com.spade.domain.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.spade.domain.DomainException;
import com.spade.domain.card.Card;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Rank number = category * 100^5 + the five deciding values, two digits each. Categories: 9 straight flush,
 * 8 quads, 7 full house, 6 flush, 5 straight, 4 trips, 3 two pair, 2 pair, 1 high card. Ace = 14; in an
 * ace-low straight the ace moves to the end, still 14, so 5-4-3-2-A ranks below 6-high.
 * The first suite is V0 Phase 03's 18 vectors (spadeboot HandEvaluationTest), rewritten in the model's notation.
 */
class HandEvaluatorTest {

    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource(delimiter = '|', value = {
        "royal flush                      | 10H JH QH KH AH 9D 8C  | 91413121110",
        "straight flush, 9 high           | 5C 6C 7C 8C 9C KD AH   | 90908070605",
        "ace-low straight flush           | AD 2D 3D 4D 5D KC QH   | 90504030214",
        "four of a kind                   | AS AH AD AC KD 2C 3H   | 81414141413",
        "full house                       | KS KH KD 9C 9D 2S 3H   | 71313130909",
        "full house from two trips        | 9S 9H 9D KC KD KS 2H   | 71313130909",
        "full house: trips + two pairs    | 5S 5H 5D KC KD 2S 2H   | 70505051313",
        "flush                            | 2H 7H 9H JH KH 3D 4C   | 61311090702",
        "flush of six takes the top five  | 2H 7H 9H JH KH AH 4C   | 61413110907",
        "flush beats the straight inside  | 4H 5H 6H 7D 8H KH 2C   | 61308060504",
        "straight                         | 6H 7D 8C 9S 10D 2C 2H  | 51009080706",
        "ace-low straight                 | AH 2D 3C 4S 5H 9D KC   | 50504030214",
        "straight with a paired card      | 5H 6D 6C 7S 8D 9C KH   | 50908070605",
        "three of a kind                  | 7S 7H 7D AC KD 2S 3H   | 40707071413",
        "two pair, ace kicker             | JS JH 4D 4C AD 2S 3H   | 31111040414",
        "three pairs: best two + kicker   | AS AH KD KC QS QH 2D   | 31414131312",
        "pair                             | 10S 10H 4D 8C AD 2S 3H | 21010140804",
        "high card                        | 2S 4H 7D 9C JD QS AH   | 11412110907",
    })
    void ranksTheV0Vectors(String description, String hand, long expected) {
        assertThat(HandEvaluator.evaluate(Card.parseAll(hand)).value()).isEqualTo(expected);
    }

    /**
     * lucabzt/Spade@1510db9 server/src/tests/test_analysis/test_poker_hand_analyzer.py, every test that asserts
     * something. Six of its tests (royal_flush_invalid, royal_flush_missing_card, straight_flush_invalid,
     * four_of_a_kind_invalid, full_house_invalid, flush_invalid) compare against a tuple shape get_best_hand
     * never returns, so they can't fail; they are not ported (V0 lesson C1).
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "royal_flush                 | 10H JH QH KH AH 9D 8C    | STRAIGHT_FLUSH | 14",
        "royal_flush_edge_case       | AH KH QH JH 10H AD AC    | STRAIGHT_FLUSH | 14",
        "straight_flush              | 9H 10H JH QH KH AD AC    | STRAIGHT_FLUSH | 13",
        "straight_flush_with_ace_low | AH 2H 3H 4H 5H 6D 7C     | STRAIGHT_FLUSH | 5",
        "straight_flush_with_flush   | 9H 10H JH QH KH AD AC    | STRAIGHT_FLUSH | 13",
        "four_of_a_kind              | 10H 10D 10C 10S AH KD QC | FOUR_OF_A_KIND | 10",
        "four_of_a_kind_with_pair    | 10H 10D 10C 10S AH AD QC | FOUR_OF_A_KIND | 10",
        "full_house                  | 10H 10D 10C 9S 9H AD QC  | FULL_HOUSE     | 10",
        "flush                       | 10H JH QC KH AH 9D 8H    | FLUSH          | 14",
        "flush_with_straight         | 10H JH QH KH 9D 2H AS    | FLUSH          | 13",
        "straight                    | 9H 10D JC QS KH 2D 8C    | STRAIGHT       | 13",
        "straight_with_ace_high      | 10H AH QC KS 2D 9D JC    | STRAIGHT       | 14",
        "straight_High               | 10H JH QH KH AD 9S 8C    | STRAIGHT       | 14",
    })
    void agreesWithThePredecessorsAnalyzer(String name, String hand, Category category, int topDecider) {
        HandRank rank = HandEvaluator.evaluate(Card.parseAll(hand));
        assertThat(rank.category()).isEqualTo(category);
        assertThat(rank.deciders().getFirst()).isEqualTo(topDecider);
    }

    @Test
    void neverReordersTheCallersList() {
        List<Card> hand = new ArrayList<>(Card.parseAll("2S 4H 7D 9C JD QS AH"));
        List<Card> before = List.copyOf(hand);
        HandEvaluator.evaluate(hand);
        assertThat(hand).isEqualTo(before);
    }

    @Test
    void comparesHandsByStrength() {
        HandRank wheel = HandEvaluator.evaluate(Card.parseAll("AH 2D 3C 4S 5H 9D KC"));
        HandRank sixHigh = HandEvaluator.evaluate(Card.parseAll("6H 2D 3C 4S 5H 9D KC"));
        assertThat(sixHigh).isGreaterThan(wheel);
    }

    @Test
    void refusesTooFewTooManyOrRepeatedCards() {
        assertThatThrownBy(() -> HandEvaluator.evaluate(Card.parseAll("AS KS QS JS")))
                .isInstanceOf(DomainException.class).hasMessageContaining("5 to 7 cards");
        assertThatThrownBy(() -> HandEvaluator.evaluate(Card.parseAll("AS KS QS JS 10S 9S 8S 7S")))
                .isInstanceOf(DomainException.class).hasMessageContaining("5 to 7 cards");
        assertThatThrownBy(() -> HandEvaluator.evaluate(Card.parseAll("AS AS QS JS 10S")))
                .isInstanceOf(DomainException.class).hasMessageContaining("twice");
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

Run: `cd backend && ./mvnw -q test -Dtest=HandEvaluatorTest`
Expected: COMPILATION ERROR, `cannot find symbol: class HandEvaluator`.

- [ ] **Step 3: Write the evaluator**

`backend/src/main/java/com/spade/domain/eval/Category.java`:
```java
package com.spade.domain.eval;

/** Hand categories, weakest first; {@link #code()} is the leading digit of a rank number. */
public enum Category {
    HIGH_CARD(1), PAIR(2), TWO_PAIR(3), THREE_OF_A_KIND(4), STRAIGHT(5),
    FLUSH(6), FULL_HOUSE(7), FOUR_OF_A_KIND(8), STRAIGHT_FLUSH(9);

    private final int code;

    Category(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
```

`backend/src/main/java/com/spade/domain/eval/HandRank.java`:
```java
package com.spade.domain.eval;

import java.util.List;

/** A hand's strength: its category and the five card values that decide ties, strongest first. */
public record HandRank(Category category, List<Integer> deciders) implements Comparable<HandRank> {

    public HandRank {
        deciders = List.copyOf(deciders);
        if (deciders.size() != 5) {
            throw new IllegalArgumentException("a hand rank has five deciders, got " + deciders);
        }
    }

    /** category * 100^5 + the deciders, two digits each (the encoding of the V0 vectors). */
    public long value() {
        long value = category.code();
        for (int decider : deciders) {
            value = value * 100 + decider;
        }
        return value;
    }

    @Override
    public int compareTo(HandRank other) {
        return Long.compare(value(), other.value());
    }
}
```

`backend/src/main/java/com/spade/domain/eval/HandEvaluator.java`:
```java
package com.spade.domain.eval;

import com.spade.domain.DomainException;
import com.spade.domain.card.Card;
import com.spade.domain.card.Suit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The best five-card hand out of 5 to 7 cards. A pure function: it never touches its argument. */
public final class HandEvaluator {

    private HandEvaluator() {
    }

    public static HandRank evaluate(Collection<Card> cards) {
        List<Card> hand = List.copyOf(cards);
        if (hand.size() < 5 || hand.size() > 7) {
            throw new DomainException(DomainException.Kind.INVALID,
                    "A hand is judged from 5 to 7 cards, got " + hand.size() + ".");
        }
        if (new HashSet<>(hand).size() != hand.size()) {
            throw new DomainException(DomainException.Kind.INVALID, "A card appears twice in " + hand + ".");
        }

        Map<Suit, List<Integer>> bySuit = new EnumMap<>(Suit.class);
        int[] count = new int[15];
        for (Card card : hand) {
            bySuit.computeIfAbsent(card.suit(), s -> new ArrayList<>()).add(card.rank().value());
            count[card.rank().value()]++;
        }

        List<Integer> flush = null;
        for (List<Integer> suited : bySuit.values()) {
            if (suited.size() >= 5) {
                flush = suited.stream().sorted(Comparator.reverseOrder()).toList();
            }
        }
        if (flush != null) {
            int high = straightHigh(flush);
            if (high > 0) {
                return new HandRank(Category.STRAIGHT_FLUSH, straight(high));
            }
        }

        List<Integer> quads = atLeast(count, 4);
        if (!quads.isEmpty()) {
            int q = quads.getFirst();
            return new HandRank(Category.FOUR_OF_A_KIND, List.of(q, q, q, q, kickers(count, Set.of(q), 1).getFirst()));
        }

        List<Integer> trips = atLeast(count, 3);
        if (!trips.isEmpty()) {
            int t = trips.getFirst();
            for (int v = 14; v >= 2; v--) {
                if (v != t && count[v] >= 2) {
                    return new HandRank(Category.FULL_HOUSE, List.of(t, t, t, v, v));
                }
            }
        }

        if (flush != null) {
            return new HandRank(Category.FLUSH, flush.subList(0, 5));
        }

        int straightHigh = straightHigh(distinctValues(count));
        if (straightHigh > 0) {
            return new HandRank(Category.STRAIGHT, straight(straightHigh));
        }

        if (!trips.isEmpty()) {
            int t = trips.getFirst();
            List<Integer> k = kickers(count, Set.of(t), 2);
            return new HandRank(Category.THREE_OF_A_KIND, List.of(t, t, t, k.get(0), k.get(1)));
        }

        List<Integer> pairs = atLeast(count, 2);
        if (pairs.size() >= 2) {
            int a = pairs.get(0);
            int b = pairs.get(1);
            return new HandRank(Category.TWO_PAIR, List.of(a, a, b, b, kickers(count, Set.of(a, b), 1).getFirst()));
        }
        if (pairs.size() == 1) {
            int p = pairs.getFirst();
            List<Integer> k = kickers(count, Set.of(p), 3);
            return new HandRank(Category.PAIR, List.of(p, p, k.get(0), k.get(1), k.get(2)));
        }
        return new HandRank(Category.HIGH_CARD, kickers(count, Set.of(), 5));
    }

    /** Values held at least n times, highest first. */
    private static List<Integer> atLeast(int[] count, int n) {
        List<Integer> values = new ArrayList<>();
        for (int v = 14; v >= 2; v--) {
            if (count[v] >= n) {
                values.add(v);
            }
        }
        return values;
    }

    /** The n highest distinct values not in {@code exclude}. */
    private static List<Integer> kickers(int[] count, Set<Integer> exclude, int n) {
        List<Integer> values = new ArrayList<>();
        for (int v = 14; v >= 2 && values.size() < n; v--) {
            if (count[v] > 0 && !exclude.contains(v)) {
                values.add(v);
            }
        }
        return values;
    }

    private static List<Integer> distinctValues(int[] count) {
        return atLeast(count, 1);
    }

    /** The high card of the best straight in these values (5 for A-2-3-4-5), or 0 if there is none. */
    private static int straightHigh(List<Integer> values) {
        boolean[] present = new boolean[15];
        for (int v : values) {
            present[v] = true;
        }
        present[1] = present[14];
        for (int high = 14; high >= 5; high--) {
            if (present[high] && present[high - 1] && present[high - 2] && present[high - 3] && present[high - 4]) {
                return high;
            }
        }
        return 0;
    }

    /** Deciders of a straight; in A-2-3-4-5 the ace goes last and stays 14 (the V0 vectors' encoding). */
    private static List<Integer> straight(int high) {
        return high == 5 ? List.of(5, 4, 3, 2, 14) : List.of(high, high - 1, high - 2, high - 3, high - 4);
    }
}
```

- [ ] **Step 4: Run it and watch it pass**

Run: `cd backend && ./mvnw -q test -Dtest='HandEvaluatorTest,ArchitectureTest'`
Expected: PASS, including all 18 V0 vectors and the 13 analyzer rows.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/spade/domain/eval backend/src/test/java/com/spade/domain/eval
git commit -m "feat(backend): the hand evaluator as a pure function, passing V0's 18 vectors and the predecessor's analyzer cases in model notation (Phase 06, #1)"
```

---

### Task 5: Showdown: who wins, splits and kickers

**Files:**
- Create: `backend/src/main/java/com/spade/domain/eval/Showdown.java`
- Test: `backend/src/test/java/com/spade/domain/eval/ShowdownTest.java`

**Interfaces:**
- Consumes: `HandEvaluator.evaluate`, `HandRank` (Task 4), `Card` (Task 3).
- Produces: `Showdown.winners(List<Card> board, Map<K, List<Card>> holeCards) → Set<K>`.
  - Ties produce several winners.
  - The result keeps the map's iteration order.
  - It refuses a board that isn't 5 cards, a hand that isn't 2 cards, and any card dealt twice.

- [ ] **Step 1: Write the failing test: the predecessor's winner suite as rows**

`backend/src/test/java/com/spade/domain/eval/ShowdownTest.java`:
```java
package com.spade.domain.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.spade.domain.DomainException;
import com.spade.domain.card.Card;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * lucabzt/Spade@1510db9 server/src/tests/test_analysis/test_poker_winner_analyzer.py, one row per test.
 * One row was corrected: one_pair_three_winners dealt 10S to Alice and Charlie; Charlie holds 10D here,
 * and the original deal is kept below as a test that a card dealt twice is refused.
 */
class ShowdownTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "royal_flush_vs_four_of_a_kind    | QS JS 10S 4D 4S  | Alice=AS KS; Bob=4H 4C                                   | Alice",
        "full_house_split_pot             | KS KH KD 2C 4S   | Alice=3S 3H; Bob=3D 3C                                   | Alice,Bob",
        "straight_flush_vs_flush          | 7C 8C 9C 2H 3H   | Alice=6C 5C; Bob=KH 9H                                   | Alice",
        "high_card_split_pot              | 4H 5C 6D 7C 8H   | Alice=AH 2C; Bob=AS 3D                                   | Alice,Bob",
        "flush_vs_two_pair                | KH QH JH 2C 3S   | Alice=AH 5H; Bob=10S 10C                                 | Alice",
        "full_house_vs_straight           | KD 9S 9H 8C 7H   | Alice=KS KH; Bob=9C 10S                                  | Alice",
        "three_of_a_kind_vs_two_pair      | QS 10D 10H 2C 3S | Alice=QH QD; Bob=KS KC                                   | Alice",
        "full_house_vs_flush              | JC 10C 7C 9H 9S  | Alice=JS JH; Bob=9C 3C                                   | Alice",
        "straight_vs_three_of_a_kind      | 3D 5H 4C 7S 8H   | Alice=5S 6C; Bob=4H 4S                                   | Alice",
        "one_pair_vs_high_card            | 3H 7D 9C JS QH   | Alice=10S 10D; Bob=AC KS                                 | Alice",
        "two_pair_split_pot               | 10S 10C 3H 4S 6C | Alice=JS 5H; Bob=JC 5D                                   | Alice,Bob",
        "straight_vs_two_pair             | 8C 5S 4D 7C 10H  | Alice=6H 7S; Bob=9C 9D                                   | Alice",
        "low_straight_split_pot           | 3S 4H 5D 7C 9H   | Alice=AS 2H; Bob=AD 2C                                   | Alice,Bob",
        "four_way_split_pot_with_straight | 5S 6H 7C 8D 9H   | Alice=2S 3H; Bob=2C 3D; Charlie=4S 4C; Diana=6C 6D       | Alice,Bob,Charlie,Diana",
        "one_pair_single_winner           | AH JH 7C 5D 2H   | Alice=AS KH; Bob=KC QD; Charlie=JS 10C; Diana=9C 8D      | Alice",
        "one_pair_two_winners             | 9H 5H 10C JD QH  | Alice=9S 3H; Bob=9C 4D; Charlie=5S 2C; Diana=6C 2D       | Alice,Bob",
        "one_pair_three_winners           | 9S 10H JC AD KH  | Alice=10S 4H; Bob=10C 2D; Charlie=10D 3C; Diana=6C 8D    | Alice,Bob,Charlie",
        "one_pair_kicker_decides          | 2S QH 3C 9D 8H   | Alice=QS AH; Bob=QC AD; Charlie=AS 5C; Diana=2C 6D       | Alice,Bob",
        "one_pair_kicker_two_players      | 10S 7H 8C 9D 5H  | Alice=7S 10H; Bob=7C 10D; Charlie=AS 2C; Diana=3C 4D     | Alice,Bob",
        "flush_split                      | 10S 7S 8S 9S KS  | Alice=2S 5S; Bob=4S 3S                                   | Alice,Bob",
        "high_split                       | QC 7C 6S 9S KH   | Alice=2S 8S; Bob=4S 8D                                   | Alice,Bob",
    })
    void decidesThePredecessorsShowdowns(String name, String board, String seats, String winners) {
        assertThat(Showdown.winners(Card.parseAll(board), hands(seats)))
                .containsExactly(winners.split(","));
    }

    @Test
    void refusesACardDealtTwice() {
        // the predecessor's original one_pair_three_winners: Alice and Charlie both hold 10S
        assertThatThrownBy(() -> Showdown.winners(Card.parseAll("9S 10H JC AD KH"),
                hands("Alice=10S 4H; Bob=10C 2D; Charlie=10S 3C; Diana=6C 8D")))
                .isInstanceOf(DomainException.class).hasMessageContaining("10S");
    }

    @Test
    void refusesAnIncompleteBoardOrHand() {
        assertThatThrownBy(() -> Showdown.winners(Card.parseAll("QS JS 10S 4D"), hands("Alice=AS KS; Bob=4H 4C")))
                .isInstanceOf(DomainException.class).hasMessageContaining("5 board cards");
        assertThatThrownBy(() -> Showdown.winners(Card.parseAll("QS JS 10S 4D 4S"), hands("Alice=AS; Bob=4H 4C")))
                .isInstanceOf(DomainException.class).hasMessageContaining("2 hole cards");
    }

    private static Map<String, List<Card>> hands(String seats) {
        Map<String, List<Card>> hands = new LinkedHashMap<>();
        Arrays.stream(seats.split(";")).map(String::strip).forEach(seat -> {
            String[] parts = seat.split("=");
            hands.put(parts[0].strip(), Card.parseAll(parts[1]));
        });
        return hands;
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

Run: `cd backend && ./mvnw -q test -Dtest=ShowdownTest`
Expected: COMPILATION ERROR, `cannot find symbol: class Showdown`.

- [ ] **Step 3: Write Showdown**

`backend/src/main/java/com/spade/domain/eval/Showdown.java`:
```java
package com.spade.domain.eval;

import com.spade.domain.DomainException;
import com.spade.domain.card.Card;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Who wins one pot: the best hand out of the board plus each player's two hole cards. Ties split. */
public final class Showdown {

    private Showdown() {
    }

    public static <K> Set<K> winners(List<Card> board, Map<K, List<Card>> holeCards) {
        if (board.size() != 5) {
            throw new DomainException(DomainException.Kind.INVALID,
                    "A showdown needs all 5 board cards, got " + board.size() + ".");
        }
        if (holeCards.isEmpty()) {
            throw new DomainException(DomainException.Kind.INVALID, "A showdown needs at least one hand.");
        }
        Set<Card> dealt = new HashSet<>();
        for (Card card : board) {
            requireFirstTime(dealt, card);
        }
        Map<K, HandRank> ranks = new LinkedHashMap<>();
        for (Map.Entry<K, List<Card>> hand : holeCards.entrySet()) {
            if (hand.getValue().size() != 2) {
                throw new DomainException(DomainException.Kind.INVALID,
                        hand.getKey() + " needs 2 hole cards, got " + hand.getValue().size() + ".");
            }
            for (Card card : hand.getValue()) {
                requireFirstTime(dealt, card);
            }
            List<Card> seven = new ArrayList<>(board);
            seven.addAll(hand.getValue());
            ranks.put(hand.getKey(), HandEvaluator.evaluate(seven));
        }
        long best = ranks.values().stream().mapToLong(HandRank::value).max().orElseThrow();
        Set<K> winners = new LinkedHashSet<>();
        ranks.forEach((key, rank) -> {
            if (rank.value() == best) {
                winners.add(key);
            }
        });
        return winners;
    }

    private static void requireFirstTime(Set<Card> dealt, Card card) {
        if (!dealt.add(card)) {
            throw new DomainException(DomainException.Kind.INVALID, card + " is dealt twice. Correct the cards first.");
        }
    }
}
```

- [ ] **Step 4: Run it and watch it pass**

Run: `cd backend && ./mvnw -q test -Dtest='ShowdownTest,ArchitectureTest'`
Expected: PASS (21 rows + 2 refusals).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/spade/domain/eval/Showdown.java backend/src/test/java/com/spade/domain/eval/ShowdownTest.java
git commit -m "feat(backend): showdown decides winners, splits and kickers; passes the predecessor's winner suite (one impossible deal corrected) (Phase 06, #1)"
```

---

### Task 6: Problem-details errors, settings that fail fast, a clock

**Files:**
- Create: `backend/src/main/java/com/spade/api/ApiExceptionHandler.java`, `backend/src/main/java/com/spade/service/SpadeProperties.java`, `backend/src/main/java/com/spade/config/ClockConfig.java`
- Modify: `backend/src/main/resources/application.yml` (add the `spade:` block)
- Test: `backend/src/test/java/probe/ErrorProbeController.java`, `backend/src/test/java/com/spade/api/ApiErrorsTest.java`, `backend/src/test/java/com/spade/service/SpadePropertiesTest.java`

**Interfaces:**
- Consumes: `DomainException` (Task 3).
- Produces:
  - `ApiExceptionHandler` maps `DomainException.Kind` to 400/401/403/404/409/429, and `DataIntegrityViolationException` to 409 with a generic text. Anything else becomes 500 with "Something went wrong on our side.".
  - `SpadeProperties(String inviteCodeSha256, Login login)` with `record Login(int maxFailures, Duration lockout)`, prefix `spade`, validated at startup.
  - A `Clock` bean (UTC) outside the testworld profile.

- [ ] **Step 1: Write the failing tests**

`backend/src/test/java/probe/ErrorProbeController.java` (outside `com.spade`, so component scanning never finds it; only `ApiErrorsTest` imports it):
```java
package probe;

import com.spade.domain.DomainException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Throws on purpose so ApiErrorsTest can look at what a client receives. Test-only. */
@RestController
public class ErrorProbeController {

    public record Named(@NotBlank String name) {
    }

    @GetMapping("/probe/boom")
    public String boom() {
        throw new IllegalStateException("secret-db-password=hunter2 at com.spade.Internal.line(42)");
    }

    @GetMapping("/probe/conflict")
    public String conflict() {
        throw new DomainException(DomainException.Kind.CONFLICT, "The table is full.");
    }

    @GetMapping("/probe/duplicate")
    public String duplicate() {
        throw new DataIntegrityViolationException("duplicate key value violates unique constraint \"seat_taken\"");
    }

    @PostMapping("/probe/named")
    public String named(@Valid @RequestBody Named named) {
        return named.name();
    }
}
```

`backend/src/test/java/com/spade/api/ApiErrorsTest.java`:
```java
package com.spade.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.spade.SpadeIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import probe.ErrorProbeController;

/** Every error a client sees is application/problem+json, and internals never leak (#2). */
@SpadeIntegrationTest
@Import(ErrorProbeController.class)
@WithMockUser
class ApiErrorsTest {

    @Autowired
    MockMvc mvc;

    @Test
    void anUnexpectedErrorIsA500ProblemWithoutItsMessage() throws Exception {
        mvc.perform(get("/probe/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Something went wrong on our side."))
                .andExpect(content().string(not(containsString("hunter2"))))
                .andExpect(content().string(not(containsString("com.spade"))));
    }

    @Test
    void aDomainRuleIsAProblemWithItsMessage() throws Exception {
        mvc.perform(get("/probe/conflict"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("The table is full."));
    }

    @Test
    void aDatabaseClashIsA409WithoutTheSql() throws Exception {
        mvc.perform(get("/probe/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(content().string(not(containsString("seat_taken"))));
    }

    @Test
    void invalidInputIsA400Problem() throws Exception {
        mvc.perform(post("/probe/named").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))   // csrf(): Boot's default chain is still active in this task
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void anUnknownPathIsA404Problem() throws Exception {
        mvc.perform(get("/api/v1/nowhere"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
```

`backend/src/test/java/com/spade/service/SpadePropertiesTest.java`:
```java
package com.spade.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** A missing or malformed invite-code hash stops the app at startup, on purpose. */
class SpadePropertiesTest {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SpadeProperties.class)
    static class Props {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Props.class)
            .withPropertyValues("spade.login.max-failures=5", "spade.login.lockout=PT5M");

    @Test
    void refusesToStartWithoutTheInviteCodeHash() {
        runner.run(context -> assertThat(context).hasFailed()
                .getFailure().hasStackTraceContaining("inviteCodeSha256"));
    }

    @Test
    void refusesAHashThatIsNotSha256Hex() {
        runner.withPropertyValues("spade.invite-code-sha256=letmein")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasStackTraceContaining("SHA-256"));
    }

    @Test
    void bindsAValidConfiguration() {
        runner.withPropertyValues("spade.invite-code-sha256=1c8bfe8f801d79745c4631d09fff36c82aa37fc4cce4fc946683d7b336b63032")
                .run(context -> {
                    SpadeProperties props = context.getBean(SpadeProperties.class);
                    assertThat(props.login().maxFailures()).isEqualTo(5);
                    assertThat(props.login().lockout()).isEqualTo(Duration.ofMinutes(5));
                });
    }
}
```

- [ ] **Step 2: Run them and watch them fail**

Run: `cd backend && ./mvnw -q test -Dtest='ApiErrorsTest,SpadePropertiesTest'`
Expected: COMPILATION ERROR, `cannot find symbol: class SpadeProperties`. Once that compiles, `ApiErrorsTest` fails on content type and message without the handler.

- [ ] **Step 3: Write the handler, the settings and the clock**

`backend/src/main/java/com/spade/api/ApiExceptionHandler.java`:
```java
package com.spade.api;

import com.spade.domain.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * One error envelope for every endpoint: RFC 9457 problem details (#2: raw exception messages reached
 * clients, some errors returned 200). Spring MVC's own errors (404, validation, bad JSON) come from the parent.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ProblemDetail domainRule(DomainException e) {
        return ProblemDetail.forStatusAndDetail(status(e.kind()), e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail clash(DataIntegrityViolationException e) {
        log.info("database refused a write: {}", e.getMostSpecificCause().getClass().getSimpleName());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "That clashes with something that already exists. Reload and try again.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpected(Exception e) {
        log.error("unexpected error", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side.");
    }

    static HttpStatus status(DomainException.Kind kind) {
        return switch (kind) {
            case INVALID -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case TOO_MANY_REQUESTS -> HttpStatus.TOO_MANY_REQUESTS;
        };
    }
}
```

`backend/src/main/java/com/spade/service/SpadeProperties.java`:
```java
package com.spade.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Spade's own settings, checked at startup. The invite code itself is never configured, only its SHA-256
 * (CLAUDE.md: compare secrets by hash). Make it with: printf '%s' 'the code' | shasum -a 256
 */
@Validated
@ConfigurationProperties("spade")
public record SpadeProperties(
        @NotBlank(message = "set SPADE_INVITE_CODE_SHA256 (the SHA-256 hex of the group's invite code)")
        @Pattern(regexp = "[0-9a-f]{64}", message = "must be the SHA-256 hex of the invite code (64 lowercase hex digits)")
        String inviteCodeSha256,
        @NotNull @Valid Login login) {

    public record Login(@Min(1) int maxFailures, @NotNull Duration lockout) {
    }
}
```

`backend/src/main/java/com/spade/config/ClockConfig.java`:
```java
package com.spade.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Every timestamp comes from this clock, so tests and the testworld can fix it. */
@Configuration(proxyBeanMethods = false)
class ClockConfig {

    @Bean
    @Profile("!testworld")
    Clock clock() {
        return Clock.systemUTC();
    }
}
```

Append to `backend/src/main/resources/application.yml`:
```yaml
spade:
  invite-code-sha256: ${SPADE_INVITE_CODE_SHA256:}
  login:
    max-failures: 5
    lockout: PT5M
```

- [ ] **Step 4: Run the tests and watch them pass**

Run: `cd backend && ./mvnw -q test -Dtest='ApiErrorsTest,SpadePropertiesTest,MigrationsTest,ArchitectureTest'`
Expected: PASS. `MigrationsTest` still starts, because `@SpadeIntegrationTest` supplies the hash.

- [ ] **Step 5: Commit**

```bash
git add backend/
git commit -m "feat(backend): every error is problem+json without internals; the invite-code hash is required at startup; one injectable clock (Phase 06, D2, #2)"
```

---

### Task 7: Registration with the group's invite code

**Files:**
- Create:
  - `backend/src/main/java/com/spade/repository/PlayerEntity.java`, `PlayerRepository.java`
  - `backend/src/main/java/com/spade/service/Hashing.java`, `PlayerView.java`, `AccountService.java`
  - `backend/src/main/java/com/spade/api/PlayerController.java`, `api/dto/RegisterPlayerRequest.java`, `api/dto/PlayerResponse.java`
  - `backend/src/main/java/com/spade/security/SecurityConfig.java`, `security/ProblemAuthenticationEntryPoint.java`
- Test: `backend/src/test/java/com/spade/api/PlayerApiTest.java`, `backend/src/test/java/com/spade/api/TestPlayers.java`

**Interfaces:**
- Consumes: `SpadeProperties.inviteCodeSha256()`, `Clock`, `DomainException`.
- Produces:
  - `Hashing.sha256Hex(String)` and `Hashing.matchesSha256(String value, String expectedHex)`;
  - `AccountService.register(username, displayName, password, inviteCode) → PlayerView`, `AccountService.get(long playerId) → PlayerView`, `AccountService.normalize(String) → String` (package-private static);
  - `record PlayerView(long id, String username, String displayName)`;
  - `PlayerRepository.existsByUsername(String)`, `findByUsername(String)`;
  - `POST /api/v1/players` → 201 `PlayerResponse(id, username, displayName)`;
  - a `SecurityFilterChain`: stateless, CSRF off, 401 problem+json for a missing session.

- [ ] **Step 1: Write the failing test and the shared helper**

`backend/src/test/java/com/spade/api/TestPlayers.java`:
```java
package com.spade.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Registers and logs in players through the API, with names unique per test run. Invite code: letmein. */
final class TestPlayers {

    static final String PASSWORD = "correct-horse";
    private final MockMvc mvc;

    TestPlayers(MockMvc mvc) {
        this.mvc = mvc;
    }

    static String unique(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    String register(String username) throws Exception {
        mvc.perform(post("/api/v1/players").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","displayName":"%s","password":"%s","inviteCode":"letmein"}"""
                .formatted(username, username, PASSWORD))).andExpect(status().isCreated());
        return username;
    }

    String login(String username) throws Exception {
        String body = mvc.perform(post("/api/v1/sessions").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","password":"%s","deviceName":"test phone"}""".formatted(username, PASSWORD)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    String newPlayerToken(String prefix) throws Exception {
        return login(register(unique(prefix)));
    }

    static String bearer(String token) {
        return "Bearer " + token;
    }
}
```

`backend/src/test/java/com/spade/api/PlayerApiTest.java`:
```java
package com.spade.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.spade.SpadeIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpadeIntegrationTest
class PlayerApiTest {

    @Autowired
    MockMvc mvc;

    private ResultActions register(String username, String password, String inviteCode) throws Exception {
        return mvc.perform(post("/api/v1/players").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","displayName":"Ana","password":"%s","inviteCode":"%s"}"""
                .formatted(username, password, inviteCode)));
    }

    @Test
    void registersWithTheGroupsInviteCode() throws Exception {
        String name = TestPlayers.unique("ana");
        register(name, "correct-horse", "letmein")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(name))
                .andExpect(jsonPath("$.displayName").value("Ana"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(content().string(not(containsString("correct-horse"))))
                .andExpect(content().string(not(containsString("password"))));
    }

    @Test
    void refusesAWrongInviteCode() throws Exception {
        register(TestPlayers.unique("eve"), "correct-horse", "guess")
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(containsString("invite code")));
    }

    @Test
    void usernamesIgnoreCapitals() throws Exception {
        String name = TestPlayers.unique("Ana");
        register(name, "correct-horse", "letmein")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(name.toLowerCase()));
        register(name.toUpperCase(), "correct-horse", "letmein").andExpect(status().isConflict());
    }

    @Test
    void refusesUsernamesWithSpacesOrSymbols() throws Exception {
        register("ana smith", "correct-horse", "letmein").andExpect(status().isBadRequest());
        register("an", "correct-horse", "letmein").andExpect(status().isBadRequest());
    }

    @Test
    void refusesPasswordsOutsideEightTo72Bytes() throws Exception {
        register(TestPlayers.unique("short"), "seven77", "letmein").andExpect(status().isBadRequest());
        register(TestPlayers.unique("long"), "ü".repeat(40), "letmein")   // 40 characters, 80 bytes
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("72 bytes")));
    }

    @Test
    void protectedEndpointsNeedASession() throws Exception {
        mvc.perform(get("/api/v1/players/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

Run: `cd backend && ./mvnw -q test -Dtest=PlayerApiTest`
Expected: FAIL. `TestPlayers` compiles, but `POST /api/v1/players` gets 401 or 404 from the default security chain, and there is no handler.

- [ ] **Step 3: Write the account slice and the security baseline**

`backend/src/main/java/com/spade/repository/PlayerEntity.java`:
```java
package com.spade.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "player")
public class PlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    @Column(name = "display_name", nullable = false, length = 40)
    private String displayName;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PlayerEntity() {
    }

    public PlayerEntity(String username, String displayName, String passwordHash, Instant createdAt) {
        this.username = username;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
}
```

`backend/src/main/java/com/spade/repository/PlayerRepository.java`:
```java
package com.spade.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {

    boolean existsByUsername(String username);

    Optional<PlayerEntity> findByUsername(String username);
}
```

`backend/src/main/java/com/spade/service/Hashing.java`:
```java
package com.spade.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256 for secrets that are compared, never stored in clear (invite code, session tokens). */
public final class Hashing {

    private Hashing() {
    }

    public static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("this JVM has no SHA-256", e);
        }
    }

    /** Constant-time comparison of a value's SHA-256 with a stored hex digest. */
    public static boolean matchesSha256(String value, String expectedHex) {
        return MessageDigest.isEqual(sha256Hex(value).getBytes(StandardCharsets.US_ASCII),
                expectedHex.getBytes(StandardCharsets.US_ASCII));
    }
}
```

`backend/src/main/java/com/spade/service/PlayerView.java`:
```java
package com.spade.service;

import com.spade.repository.PlayerEntity;

public record PlayerView(long id, String username, String displayName) {

    static PlayerView of(PlayerEntity player) {
        return new PlayerView(player.getId(), player.getUsername(), player.getDisplayName());
    }
}
```

`backend/src/main/java/com/spade/service/AccountService.java`:
```java
package com.spade.service;

import com.spade.domain.DomainException;
import com.spade.repository.PlayerEntity;
import com.spade.repository.PlayerRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Accounts: one per person, opened with the group's invite code (phase D2). */
@Service
public class AccountService {

    private static final int BCRYPT_MAX_BYTES = 72;

    private final PlayerRepository players;
    private final PasswordEncoder passwords;
    private final SpadeProperties settings;
    private final Clock clock;

    public AccountService(PlayerRepository players, PasswordEncoder passwords, SpadeProperties settings, Clock clock) {
        this.players = players;
        this.passwords = passwords;
        this.settings = settings;
        this.clock = clock;
    }

    @Transactional
    public PlayerView register(String username, String displayName, String password, String inviteCode) {
        if (!Hashing.matchesSha256(inviteCode, settings.inviteCodeSha256())) {
            throw new DomainException(DomainException.Kind.FORBIDDEN,
                    "That invite code isn't ours. Ask the host for the group's code.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new DomainException(DomainException.Kind.INVALID,
                    "Passwords can be at most 72 bytes long. Choose a shorter one.");
        }
        String name = normalize(username);
        if (players.existsByUsername(name)) {
            throw new DomainException(DomainException.Kind.CONFLICT, "The username '" + name + "' is taken.");
        }
        PlayerEntity saved = players.save(
                new PlayerEntity(name, displayName.strip(), passwords.encode(password), clock.instant()));
        return PlayerView.of(saved);
    }

    @Transactional(readOnly = true)
    public PlayerView get(long playerId) {
        return players.findById(playerId).map(PlayerView::of)
                .orElseThrow(() -> new DomainException(DomainException.Kind.NOT_FOUND, "No such player."));
    }

    static String normalize(String username) {
        return username.strip().toLowerCase(Locale.ROOT);
    }
}
```

`backend/src/main/java/com/spade/api/dto/RegisterPlayerRequest.java`:
```java
package com.spade.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterPlayerRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_.-]{3,32}", message = "use 3 to 32 letters, digits, '.', '_' or '-'")
        String username,
        @NotBlank @Size(max = 40) String displayName,
        @NotNull @Size(min = 8, max = 72, message = "use 8 to 72 characters") String password,
        @NotBlank String inviteCode) {
}
```

`backend/src/main/java/com/spade/api/dto/PlayerResponse.java`:
```java
package com.spade.api.dto;

import com.spade.service.PlayerView;

public record PlayerResponse(long id, String username, String displayName) {

    public static PlayerResponse from(PlayerView player) {
        return new PlayerResponse(player.id(), player.username(), player.displayName());
    }
}
```

`backend/src/main/java/com/spade/api/PlayerController.java`:
```java
package com.spade.api;

import com.spade.api.dto.PlayerResponse;
import com.spade.api.dto.RegisterPlayerRequest;
import com.spade.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/players")
public class PlayerController {

    private final AccountService accounts;

    public PlayerController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerResponse register(@Valid @RequestBody RegisterPlayerRequest request) {
        return PlayerResponse.from(accounts.register(
                request.username(), request.displayName(), request.password(), request.inviteCode()));
    }
}
```

`backend/src/main/java/com/spade/security/ProblemAuthenticationEntryPoint.java`:
```java
package com.spade.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** A request without a valid session gets the same problem+json envelope as every other error. */
@Component
class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
                {"type":"about:blank","title":"Unauthorized","status":401,\
                "detail":"Log in first: this needs a valid session."}""");
    }
}
```

`backend/src/main/java/com/spade/security/SecurityConfig.java`:
```java
package com.spade.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/** Stateless: every request carries its device session (ADR 0011). No cookies, no CSRF, no form login. */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    @Bean
    SecurityFilterChain api(HttpSecurity http, ProblemAuthenticationEntryPoint entryPoint) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST, "/api/v1/players", "/api/v1/sessions").permitAll()
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/actuator/health", "/error").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 4: Run the tests and watch them pass**

Run: `cd backend && ./mvnw -q test -Dtest='PlayerApiTest,ApiErrorsTest,ArchitectureTest,MigrationsTest'`
Expected: PASS. `ApiErrorsTest` still passes: `@WithMockUser` authenticates the probe calls.

- [ ] **Step 5: Commit**

```bash
git add backend/
git commit -m "feat(backend): register with the group's invite code; usernames ignore capitals; passwords 8–72 bytes; no session means 401 problem+json (Phase 06, D2, #2)"
```

---

### Task 8: Device sessions: log in once per device, `/me`, devices and revoke

**Files:**
- Create:
  - `backend/src/main/java/com/spade/repository/DeviceSessionEntity.java`, `DeviceSessionRepository.java`
  - `backend/src/main/java/com/spade/service/SessionTokens.java`, `SpadePrincipal.java`, `SessionView.java`, `LoginResult.java`, `SessionService.java`
  - `backend/src/main/java/com/spade/security/BearerSessionFilter.java`
  - `backend/src/main/java/com/spade/api/SessionController.java`
  - `backend/src/main/java/com/spade/api/dto/LoginRequest.java`, `SessionResponse.java`, `DeviceResponse.java`
- Modify: `backend/src/main/java/com/spade/security/SecurityConfig.java` (add the filter), `backend/src/main/java/com/spade/api/PlayerController.java` (`/me`, devices)
- Test: `backend/src/test/java/com/spade/api/SessionApiTest.java`

**Interfaces:**
- Consumes: `PlayerRepository.findByUsername`, `AccountService.normalize`, `AccountService.get`, `Hashing.sha256Hex`, `PasswordEncoder`, `Clock`.
- Produces:
  - `record SpadePrincipal(long playerId, long sessionId)`, the authenticated principal, read with `@AuthenticationPrincipal`;
  - `SessionService.login(username, password, deviceName) → LoginResult(String token, SessionView session)`;
  - `SessionService.authenticate(String token) → Optional<SpadePrincipal>`;
  - `SessionService.logout(SpadePrincipal)`;
  - `SessionService.devices(long playerId) → List<SessionView>`;
  - `SessionService.revoke(long playerId, long sessionId)`;
  - `record SessionView(long id, String deviceName, Instant createdAt, Instant lastUsedAt)`;
  - endpoints `POST /api/v1/sessions` → 201 `{token, device}`, `DELETE /api/v1/sessions/current` → 204, `GET /api/v1/players/me`, `GET /api/v1/players/me/devices`, `DELETE /api/v1/players/me/devices/{sessionId}` → 204.

- [ ] **Step 1: Write the failing test**

`backend/src/test/java/com/spade/api/SessionApiTest.java`:
```java
package com.spade.api;

import static com.spade.api.TestPlayers.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.spade.SpadeIntegrationTest;
import com.spade.service.Hashing;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpadeIntegrationTest
class SessionApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    TestPlayers players;

    @BeforeEach
    void setUp() {
        players = new TestPlayers(mvc);
    }

    @Test
    void logsInOnceAndTheTokenOpensMe() throws Exception {
        String name = players.register(TestPlayers.unique("ana"));
        String body = mvc.perform(post("/api/v1/sessions").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","password":"correct-horse","deviceName":"Ana's iPhone"}""".formatted(name.toUpperCase())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.device.deviceName").value("Ana's iPhone"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");
        assertThat(token).hasSize(43);   // 32 random bytes, base64url without padding

        mvc.perform(get("/api/v1/players/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(name));
    }

    @Test
    void storesOnlyTheTokensHash() throws Exception {
        String name = players.register(TestPlayers.unique("ben"));
        String token = players.login(name);
        List<String> stored = jdbc.queryForList(
                "select s.token_sha256 from device_session s join player p on p.id = s.player_id where p.username = ?",
                String.class, name);
        assertThat(stored).containsExactly(Hashing.sha256Hex(token));
    }

    @Test
    void aWrongPasswordAndAnUnknownUserLookTheSame() throws Exception {
        String name = players.register(TestPlayers.unique("cleo"));
        String wrongPassword = mvc.perform(post("/api/v1/sessions").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","password":"nope-nope","deviceName":"x"}""".formatted(name)))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String unknownUser = mvc.perform(post("/api/v1/sessions").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","password":"nope-nope","deviceName":"x"}""".formatted(TestPlayers.unique("ghost"))))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertThat(wrongPassword).isEqualTo(unknownUser);
    }

    @Test
    void anUnknownOrMalformedTokenIsA401Problem() throws Exception {
        for (String header : List.of("Bearer not-a-real-token", "Bearer ", "Basic YWJjOmRlZg==", "garbage")) {
            mvc.perform(get("/api/v1/players/me").header("Authorization", header))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }
    }

    @Test
    void logoutEndsThatDevicesSessionOnly() throws Exception {
        String name = players.register(TestPlayers.unique("dev"));
        String phone = players.login(name);
        String laptop = players.login(name);
        mvc.perform(delete("/api/v1/sessions/current").header("Authorization", bearer(phone)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/players/me").header("Authorization", bearer(phone))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/players/me").header("Authorization", bearer(laptop))).andExpect(status().isOk());
    }

    @Test
    void listsDevicesAndRevokesAnotherOneAtOnce() throws Exception {
        String name = players.register(TestPlayers.unique("eli"));
        String phone = players.login(name);
        String lost = players.login(name);

        String devices = mvc.perform(get("/api/v1/players/me/devices").header("Authorization", bearer(phone)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        List<Integer> current = JsonPath.read(devices, "$[?(@.current == true)].id");
        List<Integer> others = JsonPath.read(devices, "$[?(@.current == false)].id");
        assertThat(current).hasSize(1);

        mvc.perform(delete("/api/v1/players/me/devices/{id}", others.getFirst()).header("Authorization", bearer(phone)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/players/me").header("Authorization", bearer(lost))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/players/me").header("Authorization", bearer(phone))).andExpect(status().isOk());
    }

    @Test
    void cannotRevokeSomeoneElsesDevice() throws Exception {
        String mine = players.newPlayerToken("fay");
        String theirs = players.newPlayerToken("gus");
        String theirDevices = mvc.perform(get("/api/v1/players/me/devices").header("Authorization", bearer(theirs)))
                .andReturn().getResponse().getContentAsString();
        Integer theirSession = JsonPath.<List<Integer>>read(theirDevices, "$[*].id").getFirst();
        mvc.perform(delete("/api/v1/players/me/devices/{id}", theirSession).header("Authorization", bearer(mine)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/players/me").header("Authorization", bearer(theirs))).andExpect(status().isOk());
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

Run: `cd backend && ./mvnw -q test -Dtest=SessionApiTest`
Expected: FAIL. `POST /api/v1/sessions` returns 404 (no handler) and `TestPlayers.login` fails with "Status expected:<201>".

- [ ] **Step 3: Write the session slice**

`backend/src/main/java/com/spade/repository/DeviceSessionEntity.java`:
```java
package com.spade.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One logged-in device. Only the token's SHA-256 is kept (ADR 0011). */
@Entity
@Table(name = "device_session")
public class DeviceSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private long playerId;

    @Column(name = "token_sha256", nullable = false, unique = true, length = 64)
    private String tokenSha256;

    @Column(name = "device_name", nullable = false, length = 60)
    private String deviceName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected DeviceSessionEntity() {
    }

    public DeviceSessionEntity(long playerId, String tokenSha256, String deviceName, Instant now) {
        this.playerId = playerId;
        this.tokenSha256 = tokenSha256;
        this.deviceName = deviceName;
        this.createdAt = now;
        this.lastUsedAt = now;
    }

    /** Marks the session used; written at most once a minute to keep requests cheap. */
    public void touch(Instant now) {
        if (lastUsedAt.isBefore(now.minusSeconds(60))) {
            lastUsedAt = now;
        }
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    public Long getId() { return id; }
    public long getPlayerId() { return playerId; }
    public String getDeviceName() { return deviceName; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
}
```

`backend/src/main/java/com/spade/repository/DeviceSessionRepository.java`:
```java
package com.spade.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceSessionRepository extends JpaRepository<DeviceSessionEntity, Long> {

    Optional<DeviceSessionEntity> findByTokenSha256AndRevokedAtIsNull(String tokenSha256);

    List<DeviceSessionEntity> findByPlayerIdAndRevokedAtIsNullOrderByCreatedAtAsc(long playerId);

    Optional<DeviceSessionEntity> findByIdAndPlayerIdAndRevokedAtIsNull(long id, long playerId);
}
```

`backend/src/main/java/com/spade/service/SessionTokens.java`:
```java
package com.spade.service;

import java.security.SecureRandom;
import java.util.Base64;

/** 256-bit opaque session tokens (ADR 0011). Never log one. */
final class SessionTokens {

    private static final SecureRandom RANDOM = new SecureRandom();

    private SessionTokens() {
    }

    static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
```

`backend/src/main/java/com/spade/service/SpadePrincipal.java`:
```java
package com.spade.service;

/** Who is asking: the player and the device session the request came with. */
public record SpadePrincipal(long playerId, long sessionId) {
}
```

`backend/src/main/java/com/spade/service/SessionView.java`:
```java
package com.spade.service;

import com.spade.repository.DeviceSessionEntity;
import java.time.Instant;

public record SessionView(long id, String deviceName, Instant createdAt, Instant lastUsedAt) {

    static SessionView of(DeviceSessionEntity session) {
        return new SessionView(session.getId(), session.getDeviceName(), session.getCreatedAt(), session.getLastUsedAt());
    }
}
```

`backend/src/main/java/com/spade/service/LoginResult.java`:
```java
package com.spade.service;

/** The token is returned exactly once, at login; Spade keeps only its hash. */
public record LoginResult(String token, SessionView session) {
}
```

`backend/src/main/java/com/spade/service/SessionService.java`:
```java
package com.spade.service;

import com.spade.domain.DomainException;
import com.spade.repository.DeviceSessionEntity;
import com.spade.repository.DeviceSessionRepository;
import com.spade.repository.PlayerEntity;
import com.spade.repository.PlayerRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Device sessions: log in once per device, revoke at once (ADR 0011). */
@Service
public class SessionService {

    private final PlayerRepository players;
    private final DeviceSessionRepository sessions;
    private final PasswordEncoder passwords;
    private final Clock clock;
    private final String decoyHash;

    public SessionService(PlayerRepository players, DeviceSessionRepository sessions, PasswordEncoder passwords, Clock clock) {
        this.players = players;
        this.sessions = sessions;
        this.passwords = passwords;
        this.clock = clock;
        this.decoyHash = passwords.encode("decoy-for-unknown-usernames");
    }

    @Transactional
    public LoginResult login(String username, String password, String deviceName) {
        Optional<PlayerEntity> player = players.findByUsername(AccountService.normalize(username));
        // Check a decoy hash for unknown names too, so timing doesn't reveal who has an account.
        boolean passwordMatches = passwords.matches(password, player.map(PlayerEntity::getPasswordHash).orElse(decoyHash));
        if (player.isEmpty() || !passwordMatches) {
            throw new DomainException(DomainException.Kind.UNAUTHORIZED, "Wrong username or password.");
        }
        String token = SessionTokens.newToken();
        DeviceSessionEntity session = sessions.save(new DeviceSessionEntity(
                player.get().getId(), Hashing.sha256Hex(token), deviceName.strip(), clock.instant()));
        return new LoginResult(token, SessionView.of(session));
    }

    @Transactional
    public Optional<SpadePrincipal> authenticate(String token) {
        return sessions.findByTokenSha256AndRevokedAtIsNull(Hashing.sha256Hex(token)).map(session -> {
            session.touch(clock.instant());
            return new SpadePrincipal(session.getPlayerId(), session.getId());
        });
    }

    @Transactional
    public void logout(SpadePrincipal me) {
        revoke(me.playerId(), me.sessionId());
    }

    @Transactional(readOnly = true)
    public List<SessionView> devices(long playerId) {
        return sessions.findByPlayerIdAndRevokedAtIsNullOrderByCreatedAtAsc(playerId).stream().map(SessionView::of).toList();
    }

    @Transactional
    public void revoke(long playerId, long sessionId) {
        Instant now = clock.instant();
        sessions.findByIdAndPlayerIdAndRevokedAtIsNull(sessionId, playerId)
                .orElseThrow(() -> new DomainException(DomainException.Kind.NOT_FOUND, "You have no such device."))
                .revoke(now);
    }
}
```

`backend/src/main/java/com/spade/security/BearerSessionFilter.java`:
```java
package com.spade.security;

import com.spade.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Turns "Authorization: Bearer <token>" into a SpadePrincipal. A bad token simply stays anonymous (then 401). */
final class BearerSessionFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";
    private final SessionService sessions;

    BearerSessionFilter(SessionService sessions) {
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIX) && header.length() > PREFIX.length()) {
            sessions.authenticate(header.substring(PREFIX.length()).strip()).ifPresent(principal -> {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_PLAYER"))));
                SecurityContextHolder.setContext(context);
            });
        }
        chain.doFilter(request, response);
    }
}
```

In `SecurityConfig.api(...)`, add the parameter `com.spade.service.SessionService sessions` and this line before `.authorizeHttpRequests(...)`:
```java
                .addFilterBefore(new BearerSessionFilter(sessions),
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
```
The resulting signature is `SecurityFilterChain api(HttpSecurity http, ProblemAuthenticationEntryPoint entryPoint, SessionService sessions)`.

`backend/src/main/java/com/spade/api/dto/LoginRequest.java`:
```java
package com.spade.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(@NotBlank String username, @NotBlank String password,
                           @NotBlank @Size(max = 60) String deviceName) {
}
```

`backend/src/main/java/com/spade/api/dto/DeviceResponse.java`:
```java
package com.spade.api.dto;

import com.spade.service.SessionView;
import java.time.Instant;

public record DeviceResponse(long id, String deviceName, Instant createdAt, Instant lastUsedAt, boolean current) {

    public static DeviceResponse from(SessionView session, long currentSessionId) {
        return new DeviceResponse(session.id(), session.deviceName(), session.createdAt(), session.lastUsedAt(),
                session.id() == currentSessionId);
    }
}
```

`backend/src/main/java/com/spade/api/dto/SessionResponse.java`:
```java
package com.spade.api.dto;

import com.spade.service.LoginResult;

/** The only response that ever carries a token. */
public record SessionResponse(String token, DeviceResponse device) {

    public static SessionResponse from(LoginResult login) {
        return new SessionResponse(login.token(), DeviceResponse.from(login.session(), login.session().id()));
    }
}
```

`backend/src/main/java/com/spade/api/SessionController.java`:
```java
package com.spade.api;

import com.spade.api.dto.LoginRequest;
import com.spade.api.dto.SessionResponse;
import com.spade.service.SessionService;
import com.spade.service.SpadePrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final SessionService sessions;

    public SessionController(SessionService sessions) {
        this.sessions = sessions;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse login(@Valid @RequestBody LoginRequest request) {
        return SessionResponse.from(sessions.login(request.username(), request.password(), request.deviceName()));
    }

    @DeleteMapping("/current")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal SpadePrincipal me) {
        sessions.logout(me);
    }
}
```

Add to `PlayerController` a constructor parameter `SessionService sessions` (stored in a field) and these methods:
```java
    @GetMapping("/me")
    public PlayerResponse me(@AuthenticationPrincipal SpadePrincipal me) {
        return PlayerResponse.from(accounts.get(me.playerId()));
    }

    @GetMapping("/me/devices")
    public List<DeviceResponse> devices(@AuthenticationPrincipal SpadePrincipal me) {
        return sessions.devices(me.playerId()).stream().map(d -> DeviceResponse.from(d, me.sessionId())).toList();
    }

    @DeleteMapping("/me/devices/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal SpadePrincipal me, @PathVariable long sessionId) {
        sessions.revoke(me.playerId(), sessionId);
    }
```
with imports `com.spade.api.dto.DeviceResponse`, `com.spade.service.SessionService`, `com.spade.service.SpadePrincipal`, `java.util.List`, `org.springframework.security.core.annotation.AuthenticationPrincipal`, `org.springframework.web.bind.annotation.{GetMapping, DeleteMapping, PathVariable}`.

- [ ] **Step 4: Run the tests and watch them pass**

Run: `cd backend && ./mvnw -q test -Dtest='SessionApiTest,PlayerApiTest,ArchitectureTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/
git commit -m "feat(backend): device sessions — log in once per device, only the token's hash is stored, devices listed and revoked at once (Phase 06, ADR 0011, #2)"
```

---

### Task 9: Login throttling that works behind the tunnel

**Files:**
- Create: `backend/src/main/java/com/spade/service/LoginThrottle.java`, `backend/src/main/java/com/spade/api/ClientAddress.java`
- Modify:
  - `backend/src/main/java/com/spade/service/SessionService.java` (`login` takes an address and consults the throttle);
  - `backend/src/main/java/com/spade/api/SessionController.java` (passes `ClientAddress.of(request)`).
- Test: `backend/src/test/java/com/spade/MutableClock.java`, `backend/src/test/java/com/spade/service/LoginThrottleTest.java`, `backend/src/test/java/com/spade/api/LoginThrottleApiTest.java`

**Interfaces:**
- Consumes: `SpadeProperties.login()` (maxFailures 5, lockout PT5M), `Clock`.
- Produces:
  - `LoginThrottle.check(username, address)`, which throws `TOO_MANY_REQUESTS`;
  - `LoginThrottle.failure(username, address)`, `success(username, address)`, `forgetAll()`;
  - `SessionService.login(String username, String password, String deviceName, String address)`, which replaces the 3-argument form;
  - `ClientAddress.of(HttpServletRequest)`: the `CF-Connecting-IP` header if present, else the remote address.

- [ ] **Step 1: Write the failing tests**

`backend/src/test/java/com/spade/MutableClock.java`:
```java
package com.spade;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock a test can move forward. */
public final class MutableClock extends Clock {

    private Instant now;

    public MutableClock(Instant start) {
        this.now = start;
    }

    public void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }
    @Override public Instant instant() { return now; }
}
```

`backend/src/test/java/com/spade/service/LoginThrottleTest.java`:
```java
package com.spade.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.spade.MutableClock;
import com.spade.domain.DomainException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LoginThrottleTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-09T19:00:00Z"));
    private final LoginThrottle throttle = new LoginThrottle(
            new SpadeProperties("0".repeat(64), new SpadeProperties.Login(5, Duration.ofMinutes(5))), clock);

    private void fail(int times, String user, String address) {
        for (int i = 0; i < times; i++) {
            throttle.failure(user, address);
        }
    }

    @Test
    void locksAUsernameAfterFiveFailuresForFiveMinutes() {
        fail(5, "ana", "1.1.1.1");
        assertThatThrownBy(() -> throttle.check("ana", "9.9.9.9"))
                .isInstanceOf(DomainException.class).hasMessageContaining("5 min");
        clock.advance(Duration.ofMinutes(5));
        assertThatCode(() -> throttle.check("ana", "9.9.9.9")).doesNotThrowAnyException();
    }

    @Test
    void locksAnAddressThatGuessesManyUsernames() {
        fail(1, "a", "6.6.6.6");
        fail(1, "b", "6.6.6.6");
        fail(1, "c", "6.6.6.6");
        fail(1, "d", "6.6.6.6");
        fail(1, "e", "6.6.6.6");
        assertThatThrownBy(() -> throttle.check("f", "6.6.6.6")).isInstanceOf(DomainException.class);
        assertThatCode(() -> throttle.check("f", "7.7.7.7")).doesNotThrowAnyException();
    }

    @Test
    void aSuccessfulLoginClearsTheStrikes() {
        fail(4, "ben", "2.2.2.2");
        throttle.success("ben", "2.2.2.2");
        fail(4, "ben", "2.2.2.2");
        assertThatCode(() -> throttle.check("ben", "2.2.2.2")).doesNotThrowAnyException();
    }
}
```

`backend/src/test/java/com/spade/api/LoginThrottleApiTest.java`:
```java
package com.spade.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.spade.SpadeIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Behind the Cloudflare Tunnel every request has the same remote address; the throttle keys on CF-Connecting-IP. */
@SpadeIntegrationTest
class LoginThrottleApiTest {

    @Autowired
    MockMvc mvc;

    private ResultActions login(String user, String password, String ip) throws Exception {
        return mvc.perform(post("/api/v1/sessions").header("CF-Connecting-IP", ip)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"%s","password":"%s","deviceName":"x"}""".formatted(user, password)));
    }

    @Test
    void oneTypistDoesNotLockOutTheGroup() throws Exception {
        TestPlayers players = new TestPlayers(mvc);
        String clumsy = players.register(TestPlayers.unique("clumsy"));
        String friend = players.register(TestPlayers.unique("friend"));

        for (int i = 0; i < 5; i++) {
            login(clumsy, "wrong-password", "203.0.113.7").andExpect(status().isUnauthorized());
        }
        login(clumsy, TestPlayers.PASSWORD, "203.0.113.7")
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        login(friend, TestPlayers.PASSWORD, "198.51.100.23").andExpect(status().isCreated());
    }
}
```

- [ ] **Step 2: Run them and watch them fail**

Run: `cd backend && ./mvnw -q test -Dtest='LoginThrottleTest,LoginThrottleApiTest'`
Expected: COMPILATION ERROR, `cannot find symbol: class LoginThrottle`.

- [ ] **Step 3: Write the throttle and wire it in**

`backend/src/main/java/com/spade/service/LoginThrottle.java`:
```java
package com.spade.service;

import com.spade.domain.DomainException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Slows down password guessing: after {@code maxFailures} wrong logins, a username and the address they came
 * from are locked for {@code lockout}. In memory; a restart forgets (fine for one PC, ADR 0007).
 */
@Component
public class LoginThrottle {

    private record Strikes(int failures, Instant lockedUntil) {
    }

    private final Map<String, Strikes> strikes = new ConcurrentHashMap<>();
    private final SpadeProperties settings;
    private final Clock clock;

    public LoginThrottle(SpadeProperties settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
    }

    public void check(String username, String address) {
        Instant now = clock.instant();
        for (String key : keys(username, address)) {
            Strikes s = strikes.get(key);
            if (s != null && s.lockedUntil() != null && now.isBefore(s.lockedUntil())) {
                long minutes = (Duration.between(now, s.lockedUntil()).toSeconds() + 59) / 60;
                throw new DomainException(DomainException.Kind.TOO_MANY_REQUESTS,
                        "Too many wrong logins. Try again in " + minutes + " min.");
            }
        }
    }

    public void failure(String username, String address) {
        Instant now = clock.instant();
        for (String key : keys(username, address)) {
            strikes.compute(key, (k, s) -> {
                boolean fresh = s == null || (s.lockedUntil() != null && !now.isBefore(s.lockedUntil()));
                int failures = fresh ? 1 : s.failures() + 1;
                return failures >= settings.login().maxFailures()
                        ? new Strikes(0, now.plus(settings.login().lockout()))
                        : new Strikes(failures, null);
            });
        }
    }

    public void success(String username, String address) {
        keys(username, address).forEach(strikes::remove);
    }

    /** For the testworld's reset only. */
    public void forgetAll() {
        strikes.clear();
    }

    private static List<String> keys(String username, String address) {
        return List.of("user:" + username, "addr:" + address);
    }
}
```

`backend/src/main/java/com/spade/api/ClientAddress.java`:
```java
package com.spade.api;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Where a request really came from. Behind the Cloudflare Tunnel (ADR 0007) the remote address is always
 * cloudflared's, and Cloudflare puts the caller in CF-Connecting-IP.
 */
final class ClientAddress {

    private ClientAddress() {
    }

    static String of(HttpServletRequest request) {
        String forwarded = request.getHeader("CF-Connecting-IP");
        return forwarded != null && !forwarded.isBlank() ? forwarded.strip() : request.getRemoteAddr();
    }
}
```

In `SessionService`, add a `LoginThrottle throttle` constructor parameter and field, and replace `login` with:
```java
    @Transactional
    public LoginResult login(String username, String password, String deviceName, String address) {
        String name = AccountService.normalize(username);
        throttle.check(name, address);
        Optional<PlayerEntity> player = players.findByUsername(name);
        // Check a decoy hash for unknown names too, so timing doesn't reveal who has an account.
        boolean passwordMatches = passwords.matches(password, player.map(PlayerEntity::getPasswordHash).orElse(decoyHash));
        if (player.isEmpty() || !passwordMatches) {
            throttle.failure(name, address);
            throw new DomainException(DomainException.Kind.UNAUTHORIZED, "Wrong username or password.");
        }
        throttle.success(name, address);
        String token = SessionTokens.newToken();
        DeviceSessionEntity session = sessions.save(new DeviceSessionEntity(
                player.get().getId(), Hashing.sha256Hex(token), deviceName.strip(), clock.instant()));
        return new LoginResult(token, SessionView.of(session));
    }
```
Caution: the 401 is thrown inside a `@Transactional` method. That is fine, because `login` writes nothing before throwing.

In `SessionController.login`, add the parameter `jakarta.servlet.http.HttpServletRequest request` and call:
```java
        return SessionResponse.from(sessions.login(request.username(), request.password(), request.deviceName(),
                ClientAddress.of(httpRequest)));
```
Rename the DTO parameter to `request` and the servlet one to `httpRequest`:
`public SessionResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest)`.

- [ ] **Step 4: Run the tests and watch them pass**

Run: `cd backend && ./mvnw -q test -Dtest='LoginThrottleTest,LoginThrottleApiTest,SessionApiTest,ArchitectureTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/
git commit -m "feat(backend): login throttling per username and per caller, keyed on CF-Connecting-IP so one typist can't lock out the group (Phase 06, #2)"
```

---

### Task 10: Tables, seats, buy-ins and counted cash-outs

**Files:**
- Create:
  - `backend/src/main/resources/db/migration/V2__tables.sql`
  - `backend/src/main/java/com/spade/domain/Chips.java`
  - `backend/src/main/java/com/spade/repository/{TableEntity, TableRepository, SeatEntity, SeatRepository, BuyInEntity, BuyInRepository, CashOutEntity, CashOutRepository}.java`
  - `backend/src/main/java/com/spade/service/{TableView, SeatView, TableService}.java`
  - `backend/src/main/java/com/spade/api/TableController.java`
  - `backend/src/main/java/com/spade/api/dto/{OpenTableRequest, JoinTableRequest, PointsRequest, TableResponse, SeatResponse}.java`
- Test: `backend/src/test/java/com/spade/api/TableApiTest.java`

**Interfaces:**
- Consumes: `SpadePrincipal`, `PlayerRepository`, `Clock`, `DomainException`.
- Produces:
  - `Chips.SMALLEST = 5`, `Chips.MAX = 1_000_000`, `Chips.requireStake(long points, String what)` (> 0, multiple of 5, ≤ MAX), `Chips.requireCount(long points, String what)` (≥ 0, multiple of 5, ≤ MAX);
  - `TableService.open(me, name, smallBlind, bigBlind, maxSeats)`, `openTables()`, `get(id)`, `join(me, id, buyIn)`, `topUp(me, id, points)`, `cashOut(me, id, countedPoints)`, `close(me, id)`, each returning `TableView`;
  - `record TableView(long id, String name, long hostId, long smallBlind, long bigBlind, int maxSeats, boolean open, List<SeatView> seats)`, `record SeatView(int seatNumber, long playerId, String displayName, long stack)`;
  - endpoints `POST /api/v1/tables`, `GET /api/v1/tables`, `GET /api/v1/tables/{id}`, `POST /api/v1/tables/{id}/seats`, `POST /api/v1/tables/{id}/seats/me/buy-ins`, `POST /api/v1/tables/{id}/seats/me/cash-out`, `POST /api/v1/tables/{id}/close`.

- [ ] **Step 1: Write the failing test**

`backend/src/test/java/com/spade/api/TableApiTest.java`:
```java
package com.spade.api;

import static com.spade.api.TestPlayers.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.spade.SpadeIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpadeIntegrationTest
class TableApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    TestPlayers players;

    @BeforeEach
    void setUp() {
        players = new TestPlayers(mvc);
    }

    private ResultActions postJson(String path, String token, String json) throws Exception {
        return mvc.perform(post(path).header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long openTable(String hostToken, int maxSeats) throws Exception {
        String body = postJson("/api/v1/tables", hostToken, """
                {"name":"Friday","smallBlind":5,"bigBlind":10,"maxSeats":%d}""".formatted(maxSeats))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions join(long table, String token, long buyIn) throws Exception {
        return postJson("/api/v1/tables/" + table + "/seats", token, "{\"buyIn\":" + buyIn + "}");
    }

    @Test
    void aHostOpensATableAndPlayersSitDownWithBuyIns() throws Exception {
        String host = players.newPlayerToken("host");
        String ana = players.newPlayerToken("ana");
        long table = openTable(host, 6);

        join(table, ana, 1000)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seats[0].seatNumber").value(1))
                .andExpect(jsonPath("$.seats[0].stack").value(1000));
        join(table, host, 1000).andExpect(jsonPath("$.seats[1].seatNumber").value(2));
        mvc.perform(get("/api/v1/tables/" + table).header("Authorization", bearer(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(true))
                .andExpect(jsonPath("$.seats.length()").value(2));
    }

    @Test
    void amountsMustBeCountableInChips() throws Exception {
        String host = players.newPlayerToken("chips");
        postJson("/api/v1/tables", host, """
                {"name":"Odd","smallBlind":7,"bigBlind":10,"maxSeats":6}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("steps of 5")));
        long table = openTable(host, 6);
        join(table, host, 0).andExpect(status().isBadRequest());
        join(table, host, -100).andExpect(status().isBadRequest());
        join(table, host, 1003).andExpect(status().isBadRequest());
        join(table, host, 2_000_000)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("typo")));
    }

    @Test
    void onePlayerSitsAtOneTableAtATime() throws Exception {
        String host = players.newPlayerToken("h1");
        String other = players.newPlayerToken("h2");
        String ben = players.newPlayerToken("ben");
        long first = openTable(host, 6);
        long second = openTable(other, 6);
        join(first, ben, 500).andExpect(status().isCreated());
        join(first, ben, 500).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("already sitting at this table")));
        join(second, ben, 500).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("another table")));
        postJson("/api/v1/tables", host, """
                {"name":"Second","smallBlind":5,"bigBlind":10,"maxSeats":6}""").andExpect(status().isConflict());
    }

    @Test
    void aFullTableRefusesTheNextPlayer() throws Exception {
        String host = players.newPlayerToken("full");
        long table = openTable(host, 2);
        join(table, players.newPlayerToken("p1"), 500).andExpect(status().isCreated());
        join(table, players.newPlayerToken("p2"), 500).andExpect(status().isCreated());
        join(table, players.newPlayerToken("p3"), 500).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The table is full."));
    }

    @Test
    void topUpsAddAndACountedCashOutFreesTheSeat() throws Exception {
        String host = players.newPlayerToken("tu");
        String cleo = players.newPlayerToken("cleo");
        long table = openTable(host, 6);
        join(table, cleo, 1000);
        postJson("/api/v1/tables/" + table + "/seats/me/buy-ins", cleo, "{\"points\":500}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.seats[0].stack").value(1500));
        postJson("/api/v1/tables/" + table + "/seats/me/cash-out", cleo, "{\"points\":1730}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.seats.length()").value(0));

        Long bought = jdbc.queryForObject("""
                select sum(b.points) from buy_in b join seat s on s.id = b.seat_id where s.table_id = ?""", Long.class, table);
        Long counted = jdbc.queryForObject("""
                select c.points from cash_out c join seat s on s.id = c.seat_id where s.table_id = ?""", Long.class, table);
        assertThat(bought).isEqualTo(1500L);
        assertThat(counted).isEqualTo(1730L);

        String dev = players.newPlayerToken("dev");
        join(table, dev, 1000).andExpect(jsonPath("$.seats[0].seatNumber").value(1));   // seat 1 is free again
    }

    @Test
    void aCashOutOfNothingIsAllowed() throws Exception {
        String host = players.newPlayerToken("zero");
        long table = openTable(host, 6);
        join(table, host, 1000);
        postJson("/api/v1/tables/" + table + "/seats/me/cash-out", host, "{\"points\":0}").andExpect(status().isOk());
    }

    @Test
    void onlyTheHostClosesAndOnlyAnEmptyTable() throws Exception {
        String host = players.newPlayerToken("closer");
        String eli = players.newPlayerToken("eli");
        long table = openTable(host, 6);
        join(table, eli, 1000);
        postJson("/api/v1/tables/" + table + "/close", eli, "{}").andExpect(status().isForbidden());
        postJson("/api/v1/tables/" + table + "/close", host, "{}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("still seated")));
        postJson("/api/v1/tables/" + table + "/seats/me/cash-out", eli, "{\"points\":1000}");
        postJson("/api/v1/tables/" + table + "/close", host, "{}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.open").value(false));
        join(table, eli, 1000).andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(containsString("closed")));
        String list = mvc.perform(get("/api/v1/tables").header("Authorization", bearer(eli)))
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<java.util.List<Integer>>read(list, "$[*].id")).doesNotContain((int) table);
    }

    @Test
    void theDatabaseItselfRefusesTwoOpenSeatsForOnePlayer() throws Exception {
        String host = players.newPlayerToken("db");
        long table = openTable(host, 6);
        join(table, host, 500);
        Long playerId = jdbc.queryForObject("select player_id from seat where table_id = ?", Long.class, table);
        assertThatThrownBy(() -> jdbc.update("""
                insert into seat (table_id, player_id, seat_number, stack, joined_at) values (?, ?, 2, 500, now())""",
                table, playerId)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

Run: `cd backend && ./mvnw -q test -Dtest=TableApiTest`
Expected: FAIL. `POST /api/v1/tables` returns 404.

- [ ] **Step 3: Write the migration, the domain rule, and the table slice**

`backend/src/main/resources/db/migration/V2__tables.sql`:
```sql
-- Tables, seats, and the buy-ins and counted cash-outs the ledger will read (phase D4, ADR 0006).
create table poker_table (
    id          bigint generated by default as identity primary key,
    name        varchar(40) not null,
    host_id     bigint      not null references player (id),
    small_blind bigint      not null check (small_blind > 0),
    big_blind   bigint      not null check (big_blind >= small_blind),
    max_seats   integer     not null check (max_seats between 2 and 10),
    opened_at   timestamptz not null,
    closed_at   timestamptz
);

create table seat (
    id          bigint generated by default as identity primary key,
    table_id    bigint      not null references poker_table (id),
    player_id   bigint      not null references player (id),
    seat_number integer     not null check (seat_number between 1 and 10),
    stack       bigint      not null check (stack >= 0),
    joined_at   timestamptz not null,
    left_at     timestamptz
);
create unique index seat_taken on seat (table_id, seat_number) where left_at is null;
create unique index one_open_seat_per_player on seat (player_id) where left_at is null;

create table buy_in (
    id      bigint generated by default as identity primary key,
    seat_id bigint      not null references seat (id),
    points  bigint      not null check (points > 0),
    at      timestamptz not null
);

create table cash_out (
    id      bigint generated by default as identity primary key,
    seat_id bigint      not null unique references seat (id),
    points  bigint      not null check (points >= 0),
    at      timestamptz not null
);
```

`backend/src/main/java/com/spade/domain/Chips.java`:
```java
package com.spade.domain;

/** Amounts at the table are points a stack of real chips can hold: the smallest chip is 5 (ADR 0006). */
public final class Chips {

    public static final long SMALLEST = 5;
    public static final long MAX = 1_000_000;

    private Chips() {
    }

    /** A buy-in, a top-up or a blind: more than nothing. */
    public static long requireStake(long points, String what) {
        if (points <= 0) {
            throw invalid(what + " must be more than 0 points.");
        }
        return requireCountable(points, what);
    }

    /** A counted cash-out: may be nothing. */
    public static long requireCount(long points, String what) {
        if (points < 0) {
            throw invalid(what + " can't be negative.");
        }
        return requireCountable(points, what);
    }

    private static long requireCountable(long points, String what) {
        if (points % SMALLEST != 0) {
            throw invalid(what + " must be in steps of " + SMALLEST + " points (the smallest chip), got " + points + ".");
        }
        if (points > MAX) {
            throw invalid(what + " of " + points + " points is more than " + MAX + " (€10,000). A typo?");
        }
        return points;
    }

    private static DomainException invalid(String message) {
        return new DomainException(DomainException.Kind.INVALID, message);
    }
}
```

`backend/src/main/java/com/spade/repository/TableEntity.java`:
```java
package com.spade.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "poker_table")
public class TableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(name = "host_id", nullable = false)
    private long hostId;

    @Column(name = "small_blind", nullable = false)
    private long smallBlind;

    @Column(name = "big_blind", nullable = false)
    private long bigBlind;

    @Column(name = "max_seats", nullable = false)
    private int maxSeats;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    protected TableEntity() {
    }

    public TableEntity(String name, long hostId, long smallBlind, long bigBlind, int maxSeats, Instant openedAt) {
        this.name = name;
        this.hostId = hostId;
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
        this.maxSeats = maxSeats;
        this.openedAt = openedAt;
    }

    public void close(Instant now) {
        this.closedAt = now;
    }

    public boolean isOpen() { return closedAt == null; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public long getHostId() { return hostId; }
    public long getSmallBlind() { return smallBlind; }
    public long getBigBlind() { return bigBlind; }
    public int getMaxSeats() { return maxSeats; }
}
```

`backend/src/main/java/com/spade/repository/TableRepository.java`:
```java
package com.spade.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TableRepository extends JpaRepository<TableEntity, Long> {

    List<TableEntity> findByClosedAtIsNullOrderByOpenedAtAsc();

    boolean existsByHostIdAndClosedAtIsNull(long hostId);
}
```

`backend/src/main/java/com/spade/repository/SeatEntity.java`:
```java
package com.spade.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A player's place at a table. The stack mirrors the chips in front of them (ADR 0006). */
@Entity
@Table(name = "seat")
public class SeatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_id", nullable = false)
    private long tableId;

    @Column(name = "player_id", nullable = false)
    private long playerId;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Column(nullable = false)
    private long stack;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    protected SeatEntity() {
    }

    public SeatEntity(long tableId, long playerId, int seatNumber, long stack, Instant joinedAt) {
        this.tableId = tableId;
        this.playerId = playerId;
        this.seatNumber = seatNumber;
        this.stack = stack;
        this.joinedAt = joinedAt;
    }

    public void addToStack(long points) {
        stack += points;
    }

    public void leave(Instant now) {
        leftAt = now;
        stack = 0;
    }

    public Long getId() { return id; }
    public long getTableId() { return tableId; }
    public long getPlayerId() { return playerId; }
    public int getSeatNumber() { return seatNumber; }
    public long getStack() { return stack; }
}
```

`backend/src/main/java/com/spade/repository/SeatRepository.java`:
```java
package com.spade.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<SeatEntity, Long> {

    List<SeatEntity> findByTableIdAndLeftAtIsNullOrderBySeatNumberAsc(long tableId);

    Optional<SeatEntity> findByPlayerIdAndLeftAtIsNull(long playerId);

    Optional<SeatEntity> findByTableIdAndPlayerIdAndLeftAtIsNull(long tableId, long playerId);
}
```

`backend/src/main/java/com/spade/repository/BuyInEntity.java`:
```java
package com.spade.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Money turned into chips: at sitting down or topping up. Append-only (phase D4). */
@Entity
@Table(name = "buy_in")
public class BuyInEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seat_id", nullable = false)
    private long seatId;

    @Column(nullable = false)
    private long points;

    @Column(nullable = false)
    private Instant at;

    protected BuyInEntity() {
    }

    public BuyInEntity(long seatId, long points, Instant at) {
        this.seatId = seatId;
        this.points = points;
        this.at = at;
    }
}
```

`backend/src/main/java/com/spade/repository/BuyInRepository.java`:
```java
package com.spade.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BuyInRepository extends JpaRepository<BuyInEntity, Long> {
}
```

`backend/src/main/java/com/spade/repository/CashOutEntity.java`:
```java
package com.spade.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** The chips a player counted when leaving. The count, not the mirror, is what the ledger keeps (ADR 0006). */
@Entity
@Table(name = "cash_out")
public class CashOutEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seat_id", nullable = false, unique = true)
    private long seatId;

    @Column(nullable = false)
    private long points;

    @Column(nullable = false)
    private Instant at;

    protected CashOutEntity() {
    }

    public CashOutEntity(long seatId, long points, Instant at) {
        this.seatId = seatId;
        this.points = points;
        this.at = at;
    }
}
```

`backend/src/main/java/com/spade/repository/CashOutRepository.java`:
```java
package com.spade.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CashOutRepository extends JpaRepository<CashOutEntity, Long> {
}
```

`backend/src/main/java/com/spade/service/SeatView.java`:
```java
package com.spade.service;

public record SeatView(int seatNumber, long playerId, String displayName, long stack) {
}
```

`backend/src/main/java/com/spade/service/TableView.java`:
```java
package com.spade.service;

import java.util.List;

public record TableView(long id, String name, long hostId, long smallBlind, long bigBlind, int maxSeats,
                        boolean open, List<SeatView> seats) {
}
```

`backend/src/main/java/com/spade/service/TableService.java`:
```java
package com.spade.service;

import com.spade.domain.Chips;
import com.spade.domain.DomainException;
import com.spade.repository.BuyInEntity;
import com.spade.repository.BuyInRepository;
import com.spade.repository.CashOutEntity;
import com.spade.repository.CashOutRepository;
import com.spade.repository.PlayerEntity;
import com.spade.repository.PlayerRepository;
import com.spade.repository.SeatEntity;
import com.spade.repository.SeatRepository;
import com.spade.repository.TableEntity;
import com.spade.repository.TableRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The lobby's rules (salvage map: concept only), rebuilt: host, seats, buy-ins, counted cash-outs. */
@Service
public class TableService {

    private final TableRepository tables;
    private final SeatRepository seats;
    private final BuyInRepository buyIns;
    private final CashOutRepository cashOuts;
    private final PlayerRepository players;
    private final Clock clock;

    public TableService(TableRepository tables, SeatRepository seats, BuyInRepository buyIns,
                        CashOutRepository cashOuts, PlayerRepository players, Clock clock) {
        this.tables = tables;
        this.seats = seats;
        this.buyIns = buyIns;
        this.cashOuts = cashOuts;
        this.players = players;
        this.clock = clock;
    }

    @Transactional
    public TableView open(SpadePrincipal me, String name, long smallBlind, long bigBlind, int maxSeats) {
        Chips.requireStake(smallBlind, "The small blind");
        Chips.requireStake(bigBlind, "The big blind");
        if (bigBlind < smallBlind) {
            throw invalid("The big blind can't be smaller than the small blind.");
        }
        if (maxSeats < 2 || maxSeats > 10) {
            throw invalid("A table has 2 to 10 seats.");
        }
        if (tables.existsByHostIdAndClosedAtIsNull(me.playerId())) {
            throw conflict("You're already hosting an open table. Close it first.");
        }
        return view(tables.save(new TableEntity(name.strip(), me.playerId(), smallBlind, bigBlind, maxSeats, clock.instant())));
    }

    @Transactional(readOnly = true)
    public List<TableView> openTables() {
        return tables.findByClosedAtIsNullOrderByOpenedAtAsc().stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public TableView get(long tableId) {
        return view(table(tableId));
    }

    @Transactional
    public TableView join(SpadePrincipal me, long tableId, long buyIn) {
        TableEntity table = openTable(tableId);
        Chips.requireStake(buyIn, "A buy-in");
        seats.findByPlayerIdAndLeftAtIsNull(me.playerId()).ifPresent(seat -> {
            throw conflict(seat.getTableId() == tableId
                    ? "You're already sitting at this table."
                    : "You're sitting at another table. Cash out there first.");
        });
        List<SeatEntity> taken = seats.findByTableIdAndLeftAtIsNullOrderBySeatNumberAsc(tableId);
        if (taken.size() >= table.getMaxSeats()) {
            throw conflict("The table is full.");
        }
        Instant now = clock.instant();
        SeatEntity seat = seats.save(new SeatEntity(tableId, me.playerId(), lowestFreeSeat(taken), buyIn, now));
        buyIns.save(new BuyInEntity(seat.getId(), buyIn, now));
        return view(table);
    }

    @Transactional
    public TableView topUp(SpadePrincipal me, long tableId, long points) {
        TableEntity table = openTable(tableId);
        Chips.requireStake(points, "A top-up");
        SeatEntity seat = mySeat(me, tableId);
        seat.addToStack(points);
        buyIns.save(new BuyInEntity(seat.getId(), points, clock.instant()));
        return view(table);
    }

    @Transactional
    public TableView cashOut(SpadePrincipal me, long tableId, long countedPoints) {
        TableEntity table = openTable(tableId);
        Chips.requireCount(countedPoints, "A cash-out");
        SeatEntity seat = mySeat(me, tableId);
        Instant now = clock.instant();
        cashOuts.save(new CashOutEntity(seat.getId(), countedPoints, now));
        seat.leave(now);
        return view(table);
    }

    @Transactional
    public TableView close(SpadePrincipal me, long tableId) {
        TableEntity table = openTable(tableId);
        if (table.getHostId() != me.playerId()) {
            throw new DomainException(DomainException.Kind.FORBIDDEN, "Only the host can close this table.");
        }
        int seated = seats.findByTableIdAndLeftAtIsNullOrderBySeatNumberAsc(tableId).size();
        if (seated > 0) {
            throw conflict(seated + " player(s) are still seated. Everyone cashes out first.");
        }
        table.close(clock.instant());
        return view(table);
    }

    private TableEntity table(long tableId) {
        return tables.findById(tableId)
                .orElseThrow(() -> new DomainException(DomainException.Kind.NOT_FOUND, "No such table."));
    }

    private TableEntity openTable(long tableId) {
        TableEntity table = table(tableId);
        if (!table.isOpen()) {
            throw conflict("This table is closed.");
        }
        return table;
    }

    private SeatEntity mySeat(SpadePrincipal me, long tableId) {
        return seats.findByTableIdAndPlayerIdAndLeftAtIsNull(tableId, me.playerId())
                .orElseThrow(() -> conflict("You're not sitting at this table."));
    }

    private static int lowestFreeSeat(List<SeatEntity> taken) {
        int number = 1;
        for (SeatEntity seat : taken) {
            if (seat.getSeatNumber() != number) {
                break;
            }
            number++;
        }
        return number;
    }

    private TableView view(TableEntity table) {
        List<SeatEntity> open = table.isOpen()
                ? seats.findByTableIdAndLeftAtIsNullOrderBySeatNumberAsc(table.getId())
                : List.of();
        Map<Long, String> names = players.findAllById(open.stream().map(SeatEntity::getPlayerId).toList()).stream()
                .collect(Collectors.toMap(PlayerEntity::getId, PlayerEntity::getDisplayName, (a, b) -> a));
        List<SeatView> seatViews = open.stream()
                .map(s -> new SeatView(s.getSeatNumber(), s.getPlayerId(), names.get(s.getPlayerId()), s.getStack()))
                .toList();
        return new TableView(table.getId(), table.getName(), table.getHostId(), table.getSmallBlind(),
                table.getBigBlind(), table.getMaxSeats(), table.isOpen(), seatViews);
    }

    private static DomainException invalid(String message) {
        return new DomainException(DomainException.Kind.INVALID, message);
    }

    private static DomainException conflict(String message) {
        return new DomainException(DomainException.Kind.CONFLICT, message);
    }
}
```

`backend/src/main/java/com/spade/api/dto/OpenTableRequest.java`:
```java
package com.spade.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Blinds are in points (CONTEXT.md: Point). Their chip rules are checked by the service, with a clear message. */
public record OpenTableRequest(@NotBlank @Size(max = 40) String name, long smallBlind, long bigBlind,
                               @Min(2) @Max(10) int maxSeats) {
}
```

`backend/src/main/java/com/spade/api/dto/JoinTableRequest.java`:
```java
package com.spade.api.dto;

public record JoinTableRequest(long buyIn) {
}
```

`backend/src/main/java/com/spade/api/dto/PointsRequest.java`:
```java
package com.spade.api.dto;

/** A top-up or a counted cash-out, in points. */
public record PointsRequest(long points) {
}
```

`backend/src/main/java/com/spade/api/dto/SeatResponse.java`:
```java
package com.spade.api.dto;

import com.spade.service.SeatView;

public record SeatResponse(int seatNumber, long playerId, String displayName, long stack) {

    public static SeatResponse from(SeatView seat) {
        return new SeatResponse(seat.seatNumber(), seat.playerId(), seat.displayName(), seat.stack());
    }
}
```

`backend/src/main/java/com/spade/api/dto/TableResponse.java`:
```java
package com.spade.api.dto;

import com.spade.service.TableView;
import java.util.List;

public record TableResponse(long id, String name, long hostId, long smallBlind, long bigBlind, int maxSeats,
                            boolean open, List<SeatResponse> seats) {

    public static TableResponse from(TableView table) {
        return new TableResponse(table.id(), table.name(), table.hostId(), table.smallBlind(), table.bigBlind(),
                table.maxSeats(), table.open(), table.seats().stream().map(SeatResponse::from).toList());
    }
}
```

`backend/src/main/java/com/spade/api/TableController.java`:
```java
package com.spade.api;

import com.spade.api.dto.JoinTableRequest;
import com.spade.api.dto.OpenTableRequest;
import com.spade.api.dto.PointsRequest;
import com.spade.api.dto.TableResponse;
import com.spade.service.SpadePrincipal;
import com.spade.service.TableService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tables")
public class TableController {

    private final TableService tables;

    public TableController(TableService tables) {
        this.tables = tables;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TableResponse open(@AuthenticationPrincipal SpadePrincipal me, @Valid @RequestBody OpenTableRequest r) {
        return TableResponse.from(tables.open(me, r.name(), r.smallBlind(), r.bigBlind(), r.maxSeats()));
    }

    @GetMapping
    public List<TableResponse> openTables() {
        return tables.openTables().stream().map(TableResponse::from).toList();
    }

    @GetMapping("/{tableId}")
    public TableResponse get(@PathVariable long tableId) {
        return TableResponse.from(tables.get(tableId));
    }

    @PostMapping("/{tableId}/seats")
    @ResponseStatus(HttpStatus.CREATED)
    public TableResponse join(@AuthenticationPrincipal SpadePrincipal me, @PathVariable long tableId,
                              @RequestBody JoinTableRequest r) {
        return TableResponse.from(tables.join(me, tableId, r.buyIn()));
    }

    @PostMapping("/{tableId}/seats/me/buy-ins")
    public TableResponse topUp(@AuthenticationPrincipal SpadePrincipal me, @PathVariable long tableId,
                               @RequestBody PointsRequest r) {
        return TableResponse.from(tables.topUp(me, tableId, r.points()));
    }

    @PostMapping("/{tableId}/seats/me/cash-out")
    public TableResponse cashOut(@AuthenticationPrincipal SpadePrincipal me, @PathVariable long tableId,
                                 @RequestBody PointsRequest r) {
        return TableResponse.from(tables.cashOut(me, tableId, r.points()));
    }

    @PostMapping("/{tableId}/close")
    public TableResponse close(@AuthenticationPrincipal SpadePrincipal me, @PathVariable long tableId) {
        return TableResponse.from(tables.close(me, tableId));
    }
}
```

- [ ] **Step 4: Run the tests and watch them pass**

Run: `cd backend && ./mvnw -q test -Dtest='TableApiTest,MigrationsTest,ArchitectureTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/
git commit -m "feat(backend): tables — the host opens, players sit down with a buy-in, top up and leave with a counted cash-out; amounts must be countable in chips (Phase 06, D3, D4, ADR 0006)"
```

---

### Task 11: The testworld

**Files:**
- Create:
  - `backend/src/main/resources/application-testworld.yml`
  - `backend/src/main/java/com/spade/testworld/TestworldClock.java`, `TestworldSeed.java`, `TestworldController.java`
- Modify: `backend/src/main/java/com/spade/security/SecurityConfig.java` (permit `/api/v1/testworld/**`)
- Test: `backend/src/test/java/com/spade/TestworldTest.java` (meta-annotation), `backend/src/test/java/com/spade/testworld/TestworldModeTest.java`

**Interfaces:**
- Consumes: `AccountService.register`, `TableService.open`, `LoginThrottle.forgetAll`, `SpadePrincipal`, `JdbcTemplate`.
- Produces:
  - profile `testworld` with invite code `testworld` (its SHA-256 is `78a44d39f13f23c81a94245583bf9ba68ac2776c06231b7fd787e28d0e77537c`);
  - players `ana ben cleo dev eli fay`, password `testworld`;
  - one table "Testworld table": host `ana`, blinds 5/10, 6 seats;
  - a fixed clock at `2026-10-09T19:00:00Z`;
  - `POST /api/v1/testworld/reset` → 204, which rebuilds exactly that world with ids from 1;
  - `@TestworldTest`, a meta-annotation for tests that run the testworld.

- [ ] **Step 1: Write the failing test**

`backend/src/test/java/com/spade/TestworldTest.java`:
```java
package com.spade;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** The app in its testworld profile (seeded players and table, fixed clock) on a throwaway PostgreSQL. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@ActiveProfiles("testworld")
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
public @interface TestworldTest {
}
```

`backend/src/test/java/com/spade/testworld/TestworldModeTest.java`:
```java
package com.spade.testworld;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.spade.SpadeIntegrationTest;
import com.spade.TestworldTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class TestworldModeTest {

    static String login(MockMvc mvc, String name) throws Exception {
        String body = mvc.perform(post("/api/v1/sessions").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","password":"testworld","deviceName":"testworld"}""".formatted(name)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    @Nested
    @TestworldTest
    class InTheTestworld {

        @Autowired
        MockMvc mvc;

        @Test
        void hasItsPlayersTableAndClock() throws Exception {
            mvc.perform(post("/api/v1/testworld/reset")).andExpect(status().isNoContent());
            String ana = login(mvc, "ana");
            mvc.perform(get("/api/v1/tables").header("Authorization", "Bearer " + ana))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("Testworld table"))
                    .andExpect(jsonPath("$[0].smallBlind").value(5));
            mvc.perform(get("/api/v1/players/me/devices").header("Authorization", "Bearer " + ana))
                    .andExpect(jsonPath("$[0].createdAt").value("2026-10-09T19:00:00Z"));
        }

        @Test
        void resetForgetsEverySession() throws Exception {
            String ben = login(mvc, "ben");
            mvc.perform(post("/api/v1/testworld/reset")).andExpect(status().isNoContent());
            mvc.perform(get("/api/v1/players/me").header("Authorization", "Bearer " + ben))
                    .andExpect(status().isUnauthorized());
            login(mvc, "ben");
        }
    }

    @Nested
    @SpadeIntegrationTest
    class OutsideTheTestworld {

        @Autowired
        MockMvc mvc;

        @Test
        void theResetDoesNotExist() throws Exception {
            mvc.perform(post("/api/v1/testworld/reset")).andExpect(status().isNotFound());
        }
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

Run: `cd backend && ./mvnw -q test -Dtest=TestworldModeTest`
Expected: FAIL. The testworld context won't start (`spade.invite-code-sha256` must be set: no testworld profile file yet), or the reset returns 404/401.

- [ ] **Step 3: Write the testworld**

`backend/src/main/resources/application-testworld.yml`:
```yaml
# The testworld: seeded players and table, a fixed clock (phase 06). Never use it with real data.
spade:
  invite-code-sha256: 78a44d39f13f23c81a94245583bf9ba68ac2776c06231b7fd787e28d0e77537c   # SHA-256 of "testworld"
```

`backend/src/main/java/com/spade/testworld/TestworldClock.java`:
```java
package com.spade.testworld;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** The testworld's time stands still, so its responses are the same on every run (the contract fixtures). */
@Configuration(proxyBeanMethods = false)
@Profile("testworld")
class TestworldClock {

    static final Instant NOW = Instant.parse("2026-10-09T19:00:00Z");

    @Bean
    Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}
```

`backend/src/main/java/com/spade/testworld/TestworldSeed.java`:
```java
package com.spade.testworld;

import com.spade.service.AccountService;
import com.spade.service.PlayerView;
import com.spade.service.SpadePrincipal;
import com.spade.service.TableService;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Seeds six players (password "testworld") and one table hosted by ana, blinds 5/10. */
@Component
@Profile("testworld")
class TestworldSeed implements ApplicationRunner {

    static final List<String> PLAYERS = List.of("ana", "ben", "cleo", "dev", "eli", "fay");

    private final AccountService accounts;
    private final TableService tables;
    private final JdbcTemplate jdbc;

    TestworldSeed(AccountService accounts, TableService tables, JdbcTemplate jdbc) {
        this.accounts = accounts;
        this.tables = tables;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer existing = jdbc.queryForObject("select count(*) from player", Integer.class);
        if (existing == null || existing == 0) {
            seed();
        }
    }

    void seed() {
        PlayerView host = null;
        for (String name : PLAYERS) {
            PlayerView player = accounts.register(name, Character.toUpperCase(name.charAt(0)) + name.substring(1),
                    "testworld", "testworld");
            if (host == null) {
                host = player;
            }
        }
        tables.open(new SpadePrincipal(host.id(), 0), "Testworld table", 5, 10, 6);
    }
}
```

`backend/src/main/java/com/spade/testworld/TestworldController.java`:
```java
package com.spade.testworld;

import com.spade.service.LoginThrottle;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Exists only in the testworld; hidden from the OpenAPI contract. Puts the world back to its seed. */
@Hidden
@RestController
@Profile("testworld")
@RequestMapping("/api/v1/testworld")
class TestworldController {

    private final JdbcTemplate jdbc;
    private final TestworldSeed seed;
    private final LoginThrottle throttle;

    TestworldController(JdbcTemplate jdbc, TestworldSeed seed, LoginThrottle throttle) {
        this.jdbc = jdbc;
        this.seed = seed;
        this.throttle = throttle;
    }

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset() {
        jdbc.execute("truncate table cash_out, buy_in, seat, poker_table, device_session, player restart identity cascade");
        throttle.forgetAll();
        seed.seed();
    }
}
```
`io.swagger.v3.oas.annotations.Hidden` comes with springdoc, which Task 12 adds. Do Task 12, Step 1's pom change **now**, so this compiles:
```xml
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-api</artifactId>
            <version>${springdoc.version}</version>
        </dependency>
```

In `SecurityConfig`, add `"/api/v1/testworld/**"` to the `permitAll()` matchers. Outside the testworld, nothing answers there, so it stays a 404.

- [ ] **Step 4: Run the tests and watch them pass**

Run: `cd backend && ./mvnw -q test -Dtest='TestworldModeTest,ArchitectureTest'`
Expected: PASS (`testworldOnlyUnderItsProfile` now has 3 classes to check).

- [ ] **Step 5: Commit**

```bash
git add backend/
git commit -m "feat(backend): the testworld profile — six seeded players, one table, a fixed clock and a reset that exists only there (Phase 06)"
```

---

### Task 12: The contract: `api/openapi.json` and fixtures, checked in the gate

**Files:**
- Modify: `backend/pom.xml` (springdoc, if Task 11 didn't add it yet), `backend/src/main/resources/application.yml`
- Create: `api/openapi.json`, `api/fixtures/{player-me,devices,tables,table}.json` (generated by the test)
- Test: `backend/src/test/java/com/spade/api/ContractTest.java`

**Interfaces:**
- Consumes: the testworld reset and seed (Task 11).
- Produces: the committed contract. Any API change fails the backend build until someone runs `SPADE_CONTRACT_UPDATE=1 ./mvnw test -Dtest=ContractTest` and commits `api/`. Phases 08 and 09 decode `api/fixtures/*.json`.

- [ ] **Step 1: Configure springdoc for stable output**

Append to `backend/src/main/resources/application.yml`:
```yaml
springdoc:
  writer-with-order-by-keys: true
  writer-with-default-pretty-printer: true
```

- [ ] **Step 2: Write the contract test**

`backend/src/test/java/com/spade/api/ContractTest.java`:
```java
package com.spade.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.spade.TestworldTest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The API is a contract between backend, hub and app (ADR 0012, phase D6). A changed response fails here
 * until it is regenerated on purpose:  SPADE_CONTRACT_UPDATE=1 ./mvnw test -Dtest=ContractTest  — then commit api/.
 */
@TestworldTest
class ContractTest {

    private static final Path API = Path.of("..", "api");
    private static final boolean UPDATE = "1".equals(System.getenv("SPADE_CONTRACT_UPDATE"));
    private static final JsonMapper PRETTY = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    @Autowired
    MockMvc mvc;

    @BeforeEach
    void freshWorld() throws Exception {
        mvc.perform(post("/api/v1/testworld/reset")).andExpect(status().isNoContent());
    }

    @Test
    void theOpenApiSpecMatchesTheCommittedOne() throws Exception {
        String spec = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        compare("openapi.json", spec);
    }

    @Test
    void theFixturesMatchTheCommittedOnes() throws Exception {
        String ana = login("ana");
        String ben = login("ben");
        sitDown(ana, 1000);
        sitDown(ben, 1000);
        compare("fixtures/player-me.json", pretty(getAs(ana, "/api/v1/players/me")));
        compare("fixtures/devices.json", pretty(getAs(ana, "/api/v1/players/me/devices")));
        compare("fixtures/tables.json", pretty(getAs(ana, "/api/v1/tables")));
        compare("fixtures/table.json", pretty(getAs(ana, "/api/v1/tables/1")));
    }

    private String login(String name) throws Exception {
        String body = mvc.perform(post("/api/v1/sessions").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","password":"testworld","deviceName":"%s's iPhone"}""".formatted(name, name)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private void sitDown(String token, long buyIn) throws Exception {
        mvc.perform(post("/api/v1/tables/1/seats").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"buyIn\":" + buyIn + "}"))
                .andExpect(status().isCreated());
    }

    private String getAs(String token, String path) throws Exception {
        return mvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static String pretty(String json) {
        return PRETTY.writeValueAsString(PRETTY.readTree(json));
    }

    private static void compare(String name, String body) throws Exception {
        Path file = API.resolve(name);
        String normalized = body.strip() + "\n";
        if (UPDATE) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, normalized);
            return;
        }
        assertThat(file).as("api/%s is missing. Generate it: SPADE_CONTRACT_UPDATE=1 ./mvnw test -Dtest=ContractTest", name)
                .exists();
        assertThat(normalized)
                .as("The API changed (api/%s). If that was on purpose: SPADE_CONTRACT_UPDATE=1 ./mvnw test -Dtest=ContractTest, review the diff, commit api/", name)
                .isEqualTo(Files.readString(file));
    }
}
```

- [ ] **Step 3: Run it and watch it fail**

Run: `cd backend && ./mvnw -q test -Dtest=ContractTest`
Expected: FAIL with "api/openapi.json is missing".

- [ ] **Step 4: Generate the contract, read it, and run it again**

```bash
cd backend && SPADE_CONTRACT_UPDATE=1 ./mvnw -q test -Dtest=ContractTest && cd ..
ls api api/fixtures
grep -c '"/api/v1/' api/openapi.json              # every endpoint of Tasks 7–10 is in the spec
grep -n "testworld" api/openapi.json || echo "testworld hidden: ok"
grep -n -i "token\|password_hash\|passwordHash" api/fixtures/*.json || echo "no secrets in fixtures: ok"
cd backend && ./mvnw -q test -Dtest=ContractTest
```
Expected:
- the spec lists the player, session and table endpoints;
- no testworld path appears;
- no token or password appears in the fixtures;
- the last run PASSes.

Run it once more to prove the output is stable: `./mvnw -q test -Dtest=ContractTest` PASS again, and `git status --short api/` shows nothing new.

- [ ] **Step 5: Commit**

```bash
git add backend/ api/
git commit -m "feat(backend): the API contract — api/openapi.json and testworld fixtures, regenerated only on purpose (Phase 06, D6)"
```

---

### Task 13: The gate, CI and docs move to `backend/`; verify against the running app

**Files:**
- Create: `backend/run.sh`, `backend/.env.example`
- Modify: `scripts/gate.sh`, `.github/workflows/gate.yml`, `CLAUDE.md`, `USAGE.md`, `docs/status-quo/backend.md` (Changed-since line), `docs/handoffs/PHASE_06_2026-10-11_BACKEND_FOUNDATION.md` ("What shipping found" notes)

**Interfaces:**
- Consumes: everything above.
- Produces: `scripts/gate.sh --backend` builds `backend/`, so CI no longer builds `spadeboot/`.

- [ ] **Step 1: Point the gate at `backend/`**

In `scripts/gate.sh`, replace the backend block:
```bash
if want backend; then
  step "backend"
  run "spadeboot: mvn verify" sh -c 'cd spadeboot && ./mvnw -q -B verify'
fi
```
with:
```bash
if want backend; then
  step "backend"
  if docker info >/dev/null 2>&1; then
    run "backend: mvn verify (on a throwaway PostgreSQL)" sh -c 'cd backend && ./mvnw -q -B verify'
  else
    bad "backend: Docker is not running; its tests need a throwaway PostgreSQL. Start Docker Desktop and re-run"
  fi
fi
```
In the "not proven" block, after the `client/ and webapp/` line, add:
```bash
printf '  %s· spadeboot/ (frozen since Phase 06, deleted when V1 ships) is not built or tested%s\n' "$D" "$N"
```

- [ ] **Step 2: Point CI at `backend/`**

In `.github/workflows/gate.yml`, make the `backend` job read:
```yaml
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - name: mvn verify (Testcontainers PostgreSQL; Docker is on the runner)
        working-directory: backend
        run: ./mvnw -q -B verify
```

- [ ] **Step 3: The local run script and the env template**

`backend/.env.example`:
```bash
# backend/.env — copy to .env and fill in. The filled-in copy is git-ignored; never commit it.
SPADE_DB_URL=jdbc:postgresql://localhost:5432/spade
SPADE_DB_USER=spade
SPADE_DB_PASSWORD=
# SHA-256 hex of the group's invite code:  printf '%s' 'the code' | shasum -a 256
SPADE_INVITE_CODE_SHA256=
```

`backend/run.sh` (then `chmod +x backend/run.sh`):
```bash
#!/usr/bin/env bash
# Runs the backend against the PostgreSQL in backend/.env.
# A throwaway database instead (Docker running):
#   (cd backend && ./mvnw spring-boot:test-run -Dspring-boot.run.profiles=testworld)
set -euo pipefail
cd "$(dirname "$0")"
[[ -f .env ]] || { echo "backend/.env is missing: cp .env.example .env and fill it in" >&2; exit 1; }
set -a; . ./.env; set +a
exec ./mvnw -q spring-boot:run
```

- [ ] **Step 4: Docs**
- **`CLAUDE.md`:**
  - In "The repo", add a row ``| `backend/` | Spring Boot 4.1 / Java 21: accounts, device sessions, tables; the hand engine from phase 07 | in use (ADR 0012) |`` and change the `spadeboot/` row's state to ``**frozen** since Phase 06; deleted when V1 ships``.
  - In "Secrets", `backend/.env` (template `backend/.env.example`) replaces `spadeboot/.env`: `SPADE_DB_PASSWORD`, `SPADE_INVITE_CODE_SHA256` (a missing hash stops the backend at startup, on purpose).
  - In "The gate", "`./mvnw verify` passed" becomes "`backend`'s `./mvnw verify` passed (needs Docker)".
  - Then `python3 scripts/doc_budget.py check`. If it grew, run `python3 scripts/doc_budget.py update` and say why in the commit.
- **`USAGE.md`:** replace the "Backend (`spadeboot/`)" section with:
  ````markdown
  ## Backend (`backend/`)
  ```bash
  (cd backend && ./mvnw spring-boot:test-run -Dspring-boot.run.profiles=testworld)   # throwaway DB; players ana…fay, password testworld
  cp backend/.env.example backend/.env && backend/run.sh                             # against a real PostgreSQL
  (cd backend && ./mvnw -q verify)                                                    # tests (Docker must run)
  (cd backend && SPADE_CONTRACT_UPDATE=1 ./mvnw -q test -Dtest=ContractTest)         # after a deliberate API change; commit api/
  ```
  The legacy `spadeboot/` is frozen; its commands are in git history.
  ````
- **`docs/status-quo/backend.md`:** in the "Changed since" box, add `Phase 06: frozen; the gate and CI build backend/ instead (ADR 0012).`

- [ ] **Step 5: Run the gate**

Run: `scripts/gate.sh`
Expected: `GATE GREEN`. The "not proven" block now says `spadeboot/` isn't built.

- [ ] **Step 6: `verify-change` against the running app (testworld)**

Start it in one terminal: `(cd backend && ./mvnw spring-boot:test-run -Dspring-boot.run.profiles=testworld)`. Then:
```bash
B=http://localhost:8080/api/v1
j() { curl -s -w '  [%{http_code}]\n' -H 'content-type: application/json' "$@"; }
j -X POST $B/players -d '{"username":"zoe","displayName":"Zoe","password":"correct-horse","inviteCode":"wrong"}'   # 403 problem
j -X POST $B/players -d '{"username":"zoe","displayName":"Zoe","password":"correct-horse","inviteCode":"testworld"}'   # 201, no password in body
T=$(curl -s -H 'content-type: application/json' -X POST $B/sessions -d '{"username":"ZOE","password":"correct-horse","deviceName":"curl"}' | jq -r .token)
j $B/players/me -H "Authorization: Bearer $T"                                   # 200 zoe
j $B/players/me -H "Authorization: Bearer nope"                                 # 401 problem
j -X POST $B/tables/1/seats -H "Authorization: Bearer $T" -d '{"buyIn":1003}'   # 400, steps of 5
j -X POST $B/tables/1/seats -H "Authorization: Bearer $T" -d '{"buyIn":1000}'   # 201, seat 1
j -X POST $B/tables/1/seats/me/buy-ins -H "Authorization: Bearer $T" -d '{"points":500}'    # 200, stack 1500
j -X POST $B/tables/1/seats/me/cash-out -H "Authorization: Bearer $T" -d '{"points":1730}'  # 200, seat gone
D=$(curl -s $B/players/me/devices -H "Authorization: Bearer $T" | jq '.[0].id')
j -X DELETE $B/players/me/devices/$D -H "Authorization: Bearer $T"              # 204
j $B/players/me -H "Authorization: Bearer $T"                                   # 401: revoked at once
curl -s http://localhost:8080/actuator/health                                    # {"status":"UP"}
```
Also check the app's console output: no generated password, no token, no invite code. Stop the app (Ctrl-C).

Write the results (each command and its status) into the phase doc's "What shipped, against what was scoped". Also add the Task 2 planted-violation results and the predecessor-suite findings to "What shipping it actually found":
- 6 vacuous analyzer tests (Task 4);
- 1 impossible deal corrected (Task 5).

- [ ] **Step 7: Commit**

```bash
git status --short                    # nothing unexpected; never backend/.env
git add scripts/gate.sh .github/workflows/gate.yml backend/run.sh backend/.env.example CLAUDE.md USAGE.md \
        docs/status-quo/backend.md docs/handoffs/PHASE_06_2026-10-11_BACKEND_FOUNDATION.md doc-budget.json
git diff --cached --name-only | grep -E '(^|/)\.env$|\.pem$' && echo "STOP: a secret file is staged"   # must print nothing
git commit -m "chore(devex): the gate and CI build backend/; spadeboot/ is frozen; run script, env template, docs (Phase 06, D5)"
```

Then ship with the `ship-phase` skill (gate, truth-up, INDEX, archive, merge, push, board). #5 closes with this phase; #1 and #2 stay open for phase 07.
