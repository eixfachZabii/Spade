# Version 0 · Foundation: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Put WealthWatcher's working process into Spade without changing what the product does. That means: rotated secrets and a clean history, `cv/` brought into the repo, one honest gate plus CI, the docs set, and a board with a seeded backlog.

**Architecture:** Five phases run in order.
- Phase 01 runs directly on master, because it rewrites history.
- Phases 02–05 each run in their own worktree and merge back with `--no-ff`.
- Product code changes only where the spec allows: the evaluator fix (D20, widened by C2), fail-fast config, and dev-only seeding.

**Tech Stack:** Spring Boot 3.4.4 / Java 17 / Maven wrapper · Python 3.12 + uv + ultralytics (cv) · Python ≥3.10 stdlib (scripts) · bash · GitHub Actions · GitHub Projects v2 via `gh` · Git LFS · git-filter-repo

**Spec:** [`V0_2026-10-09_FOUNDATION.md`](V0_2026-10-09_FOUNDATION.md). Read it first. Where the two disagree, this plan's corrections win.

---

## Corrections to the spec (these override it)

Each was measured on 2026-10-09 while this plan was written. Task 0 appends them to the spec as an amendment.

| # | Spec said | Measured | Plan does |
|---|---|---|---|
| C1 | Restore the 22-test `HandEvaluatorTest` from `origin/add_first_game_logic` | That file has **0 assertions** and never calls the evaluator. It only builds card lists | Do not restore it. Task 7 writes a new table-driven `HandEvaluationTest` with 18 cases, each checked against the compiled code. Task 4 archives the branch as a local bundle instead of copying the file |
| C2 | D20: fix the flush bug | The same test also exposes a **full-house bug**. The evaluator takes the *first* matching rank in `HashMap` order, not the highest: `9s 9h 9d Kc Kd Ks` scores as nines full of kings, and `5s 5h 5d Kc Kd 2s 2h` as fives full of twos | Fix both bugs in Task 7. Pre-fix, 8 of the 18 cases fail; post-fix, all 18 pass (verified on a scratch copy) |
| C3 | §9: regenerate the Genius token | `genius.access-token` is read by **no** code. `GLA` scrapes without it | Drop the property and **revoke** the token. Nothing to migrate |
| C4 | §7: owner runs `gh auth refresh -s project` | `gh` already has the `project` scope | Skip |
| C5 | (implicit) git-lfs is available | `git-lfs` is **not installed** | Task 5 runs `brew install git-lfs` |
| C6 | §6: the gate has backend + cv + docs from Phase 03 | `doc_budget.py` needs CLAUDE.md (Phase 04). The scripts tests and the board hint need `check_board.py` (Phase 05) | The gate grows in steps: Task 10 builds the base gate, Task 17 adds the doc budget, Task 19 adds scripts pytest and the board hint |
| C7 | (not mentioned) | Once secrets leave `application-docker.yml`, the compose `app` service has no way to get them | Task 2 adds `env_file: .env` to the compose `app` service |
| C8 | (not mentioned) | The tracked `spadeboot/src/main/resources/info` says secrets live in a Discord channel | Task 2 deletes it; `.env.example` replaces it |

## Global Constraints

- **Versions:** Java 17 (`pom.xml` `java.version`), Spring Boot 3.4.4; cv Python `>=3.12,<3.13` via uv; `scripts/` run on the system `python3` (3.10 here), stdlib only.
- **Names:**
  - Repo: `eixfachZabii/Spade`. `origin` may still say `SpadeBoot.git`, which redirects.
  - Board: user project titled **`Spade`**, owner `eixfachZabii`.
- **Secrets:**
  - **Never print a secret value.** Compare by SHA-256 prefix only. Never `cat` `.env`, `application*.yml` backups or `gurobi.lic*`.
  - **Nothing is pushed until Task 1 is done.** The force-push and branch deletion in Task 4 need the owner's explicit "yes" *at that moment*.
  - Announce every other push in chat before running it.
- **Frozen code:** `client/` and `webapp/` are frozen (spec D7). The only allowed changes there are Task 0's owner commit and Task 3's `git rm --cached`.
- **Where work happens:**
  - Tasks 0–4 run on `master` in the main checkout.
  - Phases 02–05 run in `.worktrees/phase-NN-slug` on branch `phase/NN-slug`, merged with the procedure below.
- **Commits:**
  - Format: `type(scope): outcome (Phase NN, D#|C#, #issue)`.
  - Types: feat · fix · docs · test · refactor · chore. Scopes: backend · cv · docs · devex · legacy · security.
  - No negated close keywords: write "Deferred: #N", never "not fixing #N".
- **Banned:** no `git stash`; no hand-typed counts in CLAUDE.md; no `ROADMAP.md`; screenshots only under `.playwright/screenshots/`.
- **Scratch location:** `$SCRATCH` means `/private/tmp/claude-501/-Users-sebastianrogg-IdeaProjects-SPADE/<session>/scratchpad`, or any fresh `mktemp -d` outside the repo.

### Phase merge procedure (Phases 02–04; Phase 05 uses the `ship-phase` skill it creates)

```bash
# 1. in the phase worktree, everything committed:
git status --short                         # Expected: no output
scripts/gate.sh                            # Expected: GATE GREEN (from Phase 03 on; Phase 02 runs its own tests)
# 2. in the main checkout (/Users/sebastianrogg/IdeaProjects/SPADE), on master:
git merge --no-ff phase/NN-slug -m "Merge phase/NN-slug — <one-sentence outcome> (V0 Phase NN)"
scripts/gate.sh                            # Expected: GATE GREEN on master too
git push origin master                     # announce first
# 3. clean up:
git worktree remove .worktrees/phase-NN-slug && git branch -d phase/NN-slug
git push origin --delete phase/NN-slug 2>/dev/null || true
```

## Review Focus

The five ways V0 is most likely to bite someone that no test would catch unprompted. Each line names the owning task and the check pinned there.

1. **A fresh clone with no `.env`.**
   - A backend started without `SPADE_JWT_SECRET` must refuse to boot with a message naming the variable. Today it boots and fails only at the first login.
   - `run.sh` without `.env` must say what to do.
   - *Pinned by:* `JwtUtilsTest` and Task 2's fresh-clone steps.
2. **The model file is an LFS pointer** (a clone without git-lfs, or CI checkout without `lfs: true`). The smoke test must fail with "run `git lfs pull`", not a pickle stack trace. *Pinned by:* `test_model_file_is_real_not_an_lfs_pointer` in Task 5.
3. **Secrets resurfacing when `application*.yml` is un-ignored.** The local copies still contain secrets until they are overwritten. *Pinned by:* Task 2's order (backup → overwrite → un-ignore) and its hash-based leak scan over every tracked file.
4. **The gate or hook run from somewhere unexpected:**
   - from a worktree;
   - from a subdirectory;
   - with a markdown path under a guarded folder.
   - *Pinned by:* `gate.sh` `cd`s to the repo root (Task 10, step 4 runs it from `spadeboot/`); `test_guard_master.sh` covers worktree, docs and `.md` cases (Task 12).
5. **Hands with duplicate ranks and ace-low edges** (a pair inside a straight, ace-low straight flush, ace-low straight, three pairs). *Pinned by:* the `HandEvaluationTest` rows in Task 7.

## File structure

| Path | Task | Responsibility |
|---|---|---|
| `spadeboot/src/main/resources/application.yml` | 2 | Secret-free base config; every secret is `${ENV}` |
| `spadeboot/src/main/resources/application-docker.yml` | 2 | Secret-free docker profile (values otherwise unchanged) |
| `spadeboot/.env.example`, `spadeboot/run.sh` | 2 | Template of every variable; launcher that loads `.env` |
| `spadeboot/src/main/java/com/spadeboot/security/JwtUtils.java` | 2 | + `validateSecret()`: fail fast on a missing or short secret |
| `spadeboot/src/main/java/com/spadeboot/config/DataInitializer.java` | 2 | `dev` profile only; password from env; never printed |
| `spadeboot/src/main/java/com/spadeboot/domain/game/HandEvaluation.java` | 7 | Suit comparison fix; highest trips and pair for a full house |
| `spadeboot/src/test/java/com/spadeboot/{security/JwtUtilsTest,config/DataInitializerTest,domain/game/HandEvaluationTest}.java` | 2, 7 | New tests |
| `spadeboot/src/test/java/com/spadeboot/{GameServiceTest,SpadebootApplicationTests}.java` | 8, 2 | Fixture repair; test-only JWT secret |
| `.gitignore`, `spadeboot/.gitignore`, `.gitattributes` | 2, 3, 5 | Secrets, tool dirs, LFS |
| `cv/` (`app.py`, `camera.py`, `utils.py`, `models/best_60_23.pt`, `pyproject.toml`, `uv.lock`, `README.md`, `.gitignore`, `tests/test_smoke.py`) | 5 | Card-detection service |
| `scripts/gate.sh`, `check_doc_links.py`, `check_commit_refs.py`, `doc_budget.py`, `doc-budget.json`, `check_board.py`, `tests/test_check_board.py` | 9, 10, 17, 19 | Gate and checks |
| `.github/workflows/gate.yml` | 11 | CI mirror of the gate |
| `.claude/settings.json`, `.claude/hooks/guard-master.sh`, `.claude/hooks/test_guard_master.sh` | 12 | Master-edit guard |
| `.claude/skills/{ship-phase,capture-idea,verify-change}/SKILL.md` | 20 | Repo skills |
| `CLAUDE.md`, `AGENTS.md`, `USAGE.md`, `README.md`, `PRODUCT.md`, `CONTEXT.md` | 16, 17 | Root docs |
| `docs/README.md`, `docs/status-quo/*`, `docs/references/*`, `docs/adr/000{1..4}-*.md`, `docs/handoffs/INDEX.md` | 13–15 | The docs set |
| `docs/handoffs/done/Version0.0/` | 22 | Archive with `CLOSEOUT.md` |

---

# Pre-phase

### Task 0: Commit the owner's pending work; record the corrections

**Files:**
- Commit as-is: `spadeboot/pom.xml`, `spadeboot/src/main/java/com/spadeboot/api/dto/request/ChipInventoryDto.java`, `spadeboot/src/main/java/com/spadeboot/service/spadehub/CheatsheetService.java`, `client/src/layouts/cheatsheet/components/ChipDistributionCard.js`
- Modify: `docs/handoffs/V0_2026-10-09_FOUNDATION.md` (append an amendment)

**Interfaces:**
- Consumes: nothing.
- Produces: a clean tree apart from `client/{cert,key}.pem`, `webapp/{cert,key}.pem` and `spadeboot/.DS_Store`, which Task 3 untracks.

- [ ] **Step 1: Confirm the pending changes are exactly D19's**

Run: `git status --short`
Expected: exactly these 9 lines (the order may differ):
```
 M client/cert.pem
 M client/key.pem
 M client/src/layouts/cheatsheet/components/ChipDistributionCard.js
 M spadeboot/.DS_Store
 M spadeboot/pom.xml
 M spadeboot/src/main/java/com/spadeboot/api/dto/request/ChipInventoryDto.java
 M spadeboot/src/main/java/com/spadeboot/service/spadehub/CheatsheetService.java
 M webapp/cert.pem
 M webapp/key.pem
```
If anything else shows up, stop and ask the owner.

- [ ] **Step 2: Commit the owner's work, never the certificates**

```bash
git add spadeboot/pom.xml \
  spadeboot/src/main/java/com/spadeboot/api/dto/request/ChipInventoryDto.java \
  spadeboot/src/main/java/com/spadeboot/service/spadehub/CheatsheetService.java \
  client/src/layouts/cheatsheet/components/ChipDistributionCard.js
git diff --cached --name-only        # Expected: exactly those 4 paths
git commit -m "feat(backend): \$20 chip in the chip optimiser; H2 for tests; Gurobi reads the local licence file (owner's pending work, V0 D19)"
```

- [ ] **Step 3: Append the amendment to the spec**

Append to the end of `docs/handoffs/V0_2026-10-09_FOUNDATION.md`:

```markdown

## Amendment: 2026-10-09 (planning)

Writing the plan measured eight things the spec had wrong. The plan's "Corrections to the spec" table (C1–C8, [`V0_2026-10-09_FOUNDATION_TASKS.md`](V0_2026-10-09_FOUNDATION_TASKS.md)) overrides §3, §6, §7, §9 and §10 where they disagree. The two that change scope:
- **C1:** the orphan `HandEvaluatorTest` has no assertions, so it is not restored. A new table-driven test replaces it.
- **C2:** D20 is widened to the full-house bug the same test exposes.

## Amendment: 2026-10-09 (owner direction after planning)

> *"I also want to simplify the code. So before trying to fix all this lets take the best parts of it look at wealth watcher for architecture inspiration aswell and just code it fully new where things cant be saved. You have full freedom. I envision to fully redesign the fronted. While keeping the idea of a dashboard and the pages themselves as the concept but code fully from ground up new and better new libaries be creative use design skills […] Same I said for the frontend counts for the webapp. I need to get this webapp on IOS so lets code it up in SWIFT find good libaries for that aswell. But honestly I did kinda like the style and vibe of the webapp that we had we can apply the same style for the frontend."*

| # | Decision | By |
|---|---|---|
| D21 | **Rebuild, don't repair.** V1 takes the best parts of every repo (a salvage map, V0 Task 13) and writes everything else new, with a simpler design that uses WealthWatcher's *architecture* as inspiration. No effort goes into patching code V1 replaces | owner |
| D22 | **The player app is a native iOS app in Swift.** It replaces `webapp/`. Libraries are chosen by research before the V1 grill | owner |
| D23 | **The hub is a new web dashboard.** The old `client/` pages are the concept; all of the code is new, and new libraries are welcome | owner |
| D24 | **Visual direction: the legacy webapp's style and vibe**, refined and applied to the hub (and iOS), designed with the design skills. It is a new style: not WealthWatcher's, and not the Vision UI template's | owner |
| D25 | V0 still fixes the hand evaluator (D20, C2): it is one of the parts worth salvaging, and its 18 test vectors become the rebuild's spec. V0 also repairs `GameServiceTest`, but only so the gate tells the truth. Nothing else in V0 patches code V1 replaces | derived from D21 |

**What this changes in V1's evidence (§11).** The V1 grill starts from five pieces of evidence, not one:
1. the card-detection spike;
2. the salvage map;
3. an audit of WealthWatcher's backend and frontend *architecture* (the kickoff audits covered only its process);
4. research into an iOS stack (SwiftUI, camera, on-device Core ML for hole cards, a STOMP or WebSocket client);
5. research into the web-hub stack, plus a design direction drawn from the webapp's style.
```

- [ ] **Step 4: Commit**

```bash
git add docs/handoffs/V0_2026-10-09_FOUNDATION.md
git commit -m "docs(handoffs): the V0 spec records what planning measured (V0 C1–C8)"
```

---

# Phase 01 · Lockdown (on master)

### Task 1: Rotate the leaked secrets (owner, guided)

**Files:** none in the repo.

**Interfaces:**
- Consumes: spec §9.
- Produces: the leaked Gurobi WLS key is revoked and the Genius token is revoked. Task 2 generates the new JWT secret, DB passwords and seed password.

- [ ] **Step 1: Show the owner which Gurobi key is the leaked one** (the access ID is an identifier, not the secret)

```bash
grep -E '^WLSACCESSID' ~/Downloads/gurobi.lic     # the LEAKED key: delete this one
grep -E '^WLSACCESSID' ~/Downloads/gurobi.lic-2   # the newer key: keep it
```

- [ ] **Step 2: Owner deletes the leaked key**

In the Gurobi Web License Manager (license.gurobi.com → API Keys), delete the key whose access ID matches `~/Downloads/gurobi.lic`. Keep the `-2` key.
Expected: the API Keys list shows only the `-2` access ID.

- [ ] **Step 3: Owner revokes the Genius token** (C3)

genius.com/api-clients → revoke the client access token. Nothing replaces it.

- [ ] **Step 4: Owner tells Luca about the Roboflow key** in `lucabzt/Spade`'s history (`server/src/classifier/inference.py`). It isn't ours to rotate.

- [ ] **Step 5: Remove the leaked licence file from disk**

```bash
rm ~/Downloads/gurobi.lic
ls ~/Downloads/gurobi.lic* ~/gurobi.lic     # Expected: ~/Downloads/gurobi.lic-2 and ~/gurobi.lic only
```

- [ ] **Step 6: Record it.** No commit. Note in chat: "Task 1 done: WLS key <first 8 chars of the leaked access ID> deleted, Genius revoked, Luca told." **From here on, pushing is allowed** (Task 4 still needs its own yes).

### Task 2: Secret-free backend config

**Files:**
- Create: `spadeboot/.env.example`, `spadeboot/run.sh`, `spadeboot/src/test/java/com/spadeboot/security/JwtUtilsTest.java`, `spadeboot/src/test/java/com/spadeboot/config/DataInitializerTest.java`
- Overwrite (they are untracked today): `spadeboot/src/main/resources/application.yml`, `spadeboot/src/main/resources/application-docker.yml`
- Delete: the untracked `spadeboot/src/main/resources/application-dev.yml` and `application-prod.yml` (backed up first); the tracked `spadeboot/src/main/resources/info`
- Modify: `spadeboot/src/main/java/com/spadeboot/security/JwtUtils.java`, `spadeboot/src/main/java/com/spadeboot/config/DataInitializer.java`, `spadeboot/src/test/java/com/spadeboot/SpadebootApplicationTests.java`, `spadeboot/docker-compose.yml`, `spadeboot/.gitignore`
- Write (untracked, never committed): `spadeboot/.env`

**Interfaces:**
- Consumes: Task 1 done.
- Produces:
  - Environment variables `SPADE_JWT_SECRET`, `SPADE_SEED_PASSWORD`, `SPADE_SSL_ENABLED`, `SPADE_SSL_KEYSTORE`, `SPADE_SSL_KEYSTORE_PASSWORD`, `SPADE_DB_URL`, `SPADE_DB_DRIVER`, `SPADE_DB_USER`, `SPADE_DB_PASSWORD`, `SPADE_BIND_ADDRESS`, `SPADE_PORT`, `SPOTIFY_CLIENT_ID`, `SPOTIFY_CLIENT_SECRET`, `SPOTIFY_REDIRECT_URI`, `SPOTIFY_REDIRECT_VIEW`, `MYSQL_ROOT_PASSWORD`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `SPRING_PROFILES_ACTIVE`.
  - `JwtUtils.validateSecret()` (package-private, `@PostConstruct`).
  - `DataInitializer` is active only in profile `dev` and reads `spade.seed.password`.

- [ ] **Step 1: Back up the local configs outside the repo** (they hold secrets; never print them)

```bash
B=~/spade-secrets-backup/2026-10-09 && mkdir -p "$B" && chmod 700 ~/spade-secrets-backup "$B"
cp spadeboot/src/main/resources/application.yml spadeboot/src/main/resources/application-dev.yml \
   spadeboot/src/main/resources/application-docker.yml spadeboot/src/main/resources/application-prod.yml \
   spadeboot/.env "$B"/
ls "$B"       # Expected: .env plus the 4 application*.yml files (`ls -a` shows .env)
```

- [ ] **Step 2: Write the failing fail-fast test**

Create `spadeboot/src/test/java/com/spadeboot/security/JwtUtilsTest.java`:

```java
package com.spadeboot.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilsTest {

    private static JwtUtils withSecret(String secret) {
        JwtUtils utils = new JwtUtils();
        ReflectionTestUtils.setField(utils, "jwtSecret", secret);
        return utils;
    }

    @Test
    void refusesToStartWithoutASecret() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> withSecret("").validateSecret());
        assertTrue(e.getMessage().contains("SPADE_JWT_SECRET"), e.getMessage());
    }

    @Test
    void refusesASecretShorterThan32Bytes() {
        assertThrows(IllegalStateException.class, () -> withSecret("too-short").validateSecret());
    }

    @Test
    void acceptsA32ByteSecret() {
        assertDoesNotThrow(() -> withSecret("x".repeat(32)).validateSecret());
    }
}
```

- [ ] **Step 3: Run it and watch it fail**

Run: `cd spadeboot && ./mvnw -q test -Dtest=JwtUtilsTest; cd ..`
Expected: compilation FAILURE, `cannot find symbol … validateSecret()`.

- [ ] **Step 4: Implement `validateSecret`**

In `spadeboot/src/main/java/com/spadeboot/security/JwtUtils.java`:
- add the import `import jakarta.annotation.PostConstruct;`;
- add this method directly after the `jwtExpirationMs` field:

```java
    /**
     * Fails at startup instead of at the first login: HS256 needs a key of at least 32 bytes,
     * and jjwt only checks that when a token is signed.
     */
    @PostConstruct
    void validateSecret() {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret is missing or shorter than 32 bytes. Set SPADE_JWT_SECRET "
                    + "(see spadeboot/.env.example; generate one with: openssl rand -base64 48)");
        }
    }
```

- [ ] **Step 5: Run it and watch it pass**

Run: `cd spadeboot && ./mvnw -q test -Dtest=JwtUtilsTest; cd ..`
Expected: no output, exit 0. `target/surefire-reports/TEST-com.spadeboot.security.JwtUtilsTest.xml` shows `tests="3" failures="0" errors="0"`.

- [ ] **Step 6: Write the failing seeding test**

Create `spadeboot/src/test/java/com/spadeboot/config/DataInitializerTest.java`:

```java
package com.spadeboot.config;

import com.spadeboot.repository.FriendshipRepository;
import com.spadeboot.repository.UserRepository;
import com.spadeboot.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock private UserRepository userRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserService userService;
    @InjectMocks private DataInitializer initializer;

    @Test
    void onlyRunsInTheDevProfile() {
        Profile profile = DataInitializer.class.getAnnotation(Profile.class);
        assertNotNull(profile, "DataInitializer must carry @Profile(\"dev\")");
        assertArrayEquals(new String[] {"dev"}, profile.value());
    }

    @Test
    void skipsSeedingWhenNoSeedPasswordIsConfigured() {
        ReflectionTestUtils.setField(initializer, "seedPassword", "");
        initializer.run();
        verifyNoInteractions(userRepository, friendshipRepository, passwordEncoder, userService);
    }

    @Test
    void seedsUsersWithTheConfiguredPasswordAndNoHardCodedOne() {
        ReflectionTestUtils.setField(initializer, "seedPassword", "from-env-123");
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("from-env-123")).thenReturn("hashed");
        when(userRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        initializer.run();

        verify(passwordEncoder, atLeastOnce()).encode("from-env-123");
        verify(passwordEncoder, never()).encode("admin123");
        verify(passwordEncoder, never()).encode("password123");
    }
}
```

- [ ] **Step 7: Run it and watch it fail**

Run: `cd spadeboot && ./mvnw -q test -Dtest=DataInitializerTest; cd ..`
Expected: compilation FAILURE: `seedPassword` field not found (ReflectionTestUtils throws), or `onlyRunsInTheDevProfile` fails with "must carry @Profile". Either is a correct RED.

- [ ] **Step 8: Rewrite `DataInitializer`**

Replace the whole of `spadeboot/src/main/java/com/spadeboot/config/DataInitializer.java` with:

```java
package com.spadeboot.config;

import com.spadeboot.domain.user.Friendship;
import com.spadeboot.domain.user.FriendshipStatus;
import com.spadeboot.domain.user.User;
import com.spadeboot.repository.FriendshipRepository;
import com.spadeboot.repository.UserRepository;
import com.spadeboot.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the poker-night regulars as mutual friends, for local development only.
 * Runs only in the {@code dev} profile, and only when SPADE_SEED_PASSWORD is set.
 * Every seed user shares that password, and it is never logged.
 */
@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String ADMIN_NAME = "Hoerter";
    private static final String[] PLAYER_NAMES = {"Sebastian", "Markus", "Matthi", "Luca", "Paul", "Viktor"};

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @Value("${spade.seed.password:}")
    private String seedPassword;

    @Override
    public void run(String... args) {
        if (seedPassword == null || seedPassword.isBlank()) {
            log.warn("SPADE_SEED_PASSWORD is not set; skipping the dev seed users");
            return;
        }

        List<User> users = new ArrayList<>();
        if (userRepository.findByUsername(ADMIN_NAME).isEmpty()) {
            users.add(newUser(ADMIN_NAME, "ROLE_ADMIN", 50000));
        }
        for (String name : PLAYER_NAMES) {
            if (userRepository.findByUsername(name).isEmpty()) {
                users.add(newUser(name, "ROLE_USER", 2000));
            }
        }

        users = userRepository.saveAll(users);
        for (User user : users) {
            userService.createPlayer(user.getId());
            log.info("Seed user created: {}", user.getUsername());
        }

        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < users.size(); i++) {
            for (int j = i + 1; j < users.size(); j++) {
                User a = users.get(i);
                User b = users.get(j);
                if (friendshipRepository.findFriendship(a, b).isEmpty()) {
                    Friendship friendship = new Friendship();
                    friendship.setRequester(a);
                    friendship.setAddressee(b);
                    friendship.setStatus(FriendshipStatus.ACCEPTED);
                    friendship.setCreatedAt(now);
                    friendship.setUpdatedAt(now);
                    friendshipRepository.save(friendship);
                }
            }
        }
    }

    private User newUser(String name, String role, int balance) {
        User user = new User();
        user.setUsername(name);
        user.setEmail(name.toLowerCase() + "@spade.com");
        user.setPassword(passwordEncoder.encode(seedPassword));
        user.setRole(role);
        user.setBalance(balance);
        return user;
    }
}
```

- [ ] **Step 9: Run it and watch it pass**

Run: `cd spadeboot && ./mvnw -q test -Dtest=DataInitializerTest; cd ..`
Expected: exit 0. The surefire report shows `tests="3" failures="0" errors="0"`.

If `findFriendship` doesn't return an `Optional`, Mockito's default return breaks `.isEmpty()`. In that case add `when(friendshipRepository.findFriendship(any(), any())).thenReturn(Optional.empty());` to the seeding test.

- [ ] **Step 10: Write `spadeboot/.env` from the backup plus fresh secrets** (prints key names only)

Create `$SCRATCH/write_env.py` (scratch only, never committed):

```python
"""Write spadeboot/.env from the backed-up local configs + fresh secrets. Prints key NAMES only."""
import base64, os, secrets, sys
from pathlib import Path
import yaml

backup, out = Path(sys.argv[1]), Path(sys.argv[2])
dev = yaml.safe_load((backup / "application-dev.yml").read_text()) or {}
old_env = {}
for line in (backup / ".env").read_text().splitlines():
    if "=" in line and not line.lstrip().startswith("#"):
        k, v = line.split("=", 1)
        old_env[k.strip()] = v.strip()

def get(d, *path):
    for key in path:
        if not isinstance(d, dict) or key not in d:
            return ""
        d = d[key]
    return "" if d is None else str(d)

env = {
    "SPRING_PROFILES_ACTIVE": "dev",
    "SPADE_JWT_SECRET": base64.b64encode(secrets.token_bytes(48)).decode(),
    "SPADE_SEED_PASSWORD": secrets.token_urlsafe(12),
    "SPADE_SSL_ENABLED": "true",
    "SPADE_SSL_KEYSTORE_PASSWORD": get(dev, "server", "ssl", "key-store-password"),
    "SPOTIFY_CLIENT_ID": get(dev, "spotify", "client-id"),
    "SPOTIFY_CLIENT_SECRET": get(dev, "spotify", "client-secret"),
    "SPOTIFY_REDIRECT_URI": get(dev, "spotify", "redirect-uri"),
    "SPOTIFY_REDIRECT_VIEW": get(dev, "spotify", "redirect-view"),
    "MYSQL_ROOT_PASSWORD": secrets.token_urlsafe(24),
    "MYSQL_DATABASE": old_env.get("MYSQL_DATABASE", "SpadeDB"),
    "MYSQL_USER": old_env.get("MYSQL_USER", "spade"),
    "MYSQL_PASSWORD": secrets.token_urlsafe(24),
}

def quote(v):  # single quotes survive both `source` and docker compose env_file
    return "'" + v.replace("'", "'\"'\"'") + "'"

lines = ["# spadeboot/.env: written by V0 Task 2. NEVER commit. Template: .env.example"]
lines += [f"{k}={quote(v)}" for k, v in env.items()]
out.write_text("\n".join(lines) + "\n")
os.chmod(out, 0o600)
print("wrote", out)
print("set:  ", ", ".join(k for k, v in env.items() if v))
print("empty:", ", ".join(k for k, v in env.items() if not v) or "none")
```

Run: `uv run --with pyyaml python3 -I $SCRATCH/write_env.py ~/spade-secrets-backup/2026-10-09 spadeboot/.env`
Expected: `empty: none`, every key listed under `set:`. The MySQL passwords are new, so the old Docker volume no longer matches. Nothing is deployed, so that's fine; `docker compose down -v` resets it.

- [ ] **Step 11: Overwrite `application.yml` with the secret-free base**

Write `spadeboot/src/main/resources/application.yml`:

```yaml
# Secret-free base configuration. Every secret comes from an environment variable;
# spadeboot/run.sh loads them from the untracked spadeboot/.env (template: .env.example).
# With no environment at all this boots on in-memory H2 without SSL, except that
# SPADE_JWT_SECRET is required (JwtUtils.validateSecret fails fast without it).

spring:
  datasource:
    url: ${SPADE_DB_URL:jdbc:h2:mem:spade}
    driver-class-name: ${SPADE_DB_DRIVER:org.h2.Driver}
    username: ${SPADE_DB_USER:sa}
    password: ${SPADE_DB_PASSWORD:}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false

server:
  address: ${SPADE_BIND_ADDRESS:0.0.0.0}
  port: ${SPADE_PORT:8080}
  ssl:
    enabled: ${SPADE_SSL_ENABLED:false}
    key-store: ${SPADE_SSL_KEYSTORE:classpath:keystore.p12}
    key-store-password: ${SPADE_SSL_KEYSTORE_PASSWORD:}
    key-store-type: PKCS12
    key-alias: spadeboot

app:
  jwt:
    secret: ${SPADE_JWT_SECRET:}
    expirationMs: 86400000

spade:
  seed:
    password: ${SPADE_SEED_PASSWORD:}

spotify:
  client-id: ${SPOTIFY_CLIENT_ID:}
  client-secret: ${SPOTIFY_CLIENT_SECRET:}
  redirect-uri: ${SPOTIFY_REDIRECT_URI:https://spade-dev.local:8080/api/spotify/callback}
  redirect-view: ${SPOTIFY_REDIRECT_VIEW:http://localhost:3000}
```

- [ ] **Step 12: Overwrite `application-docker.yml`; delete the other local profiles and `info`**

Write `spadeboot/src/main/resources/application-docker.yml`:

```yaml
# Docker profile: docker-compose.yml sets SPRING_PROFILES_ACTIVE=docker and loads spadeboot/.env.
# Bind address and port are unchanged from before V0. Their mismatch with the compose port mapping
# is a known bug, tracked in the Docker issue (V0 spec §8).

server:
  address: 127.0.0.1
  port: 5467
  ssl:
    enabled: false

spring:
  datasource:
    url: jdbc:mysql://mysql:3306/${MYSQL_DATABASE:SpadeDB}
    username: ${MYSQL_USER:spade}
    password: ${MYSQL_PASSWORD:}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect

spotify:
  redirect-uri: https://hub.poker-spade.de/api/spotify/callback
  redirect-view: ""
```

Then:

```bash
rm spadeboot/src/main/resources/application-dev.yml spadeboot/src/main/resources/application-prod.yml
git rm -q spadeboot/src/main/resources/info
```

- [ ] **Step 13: `.env.example`, `run.sh`, compose `env_file`**

Create `spadeboot/.env.example`:

```bash
# Copy to spadeboot/.env and fill in. Never commit .env (it is gitignored).
# spadeboot/run.sh loads it; docker-compose.yml loads it via env_file.

# dev = seed users + friendships (needs SPADE_SEED_PASSWORD)
SPRING_PROFILES_ACTIVE=dev

# Required, at least 32 bytes. Generate one with: openssl rand -base64 48
SPADE_JWT_SECRET=

# Password shared by the dev seed users (dev profile only). Empty means no seed users.
SPADE_SEED_PASSWORD=

# The legacy frontends call https://…:8080, so dev runs with SSL.
# The keystore is the untracked src/main/resources/keystore.p12.
SPADE_SSL_ENABLED=true
SPADE_SSL_KEYSTORE_PASSWORD=

# Optional overrides (defaults: in-memory H2 on 0.0.0.0:8080)
# SPADE_DB_URL=
# SPADE_PORT=

# Spotify (SpadeHub). Empty means the Spotify page cannot log in.
SPOTIFY_CLIENT_ID=
SPOTIFY_CLIENT_SECRET=
SPOTIFY_REDIRECT_URI=https://spade-dev.local:8080/api/spotify/callback
SPOTIFY_REDIRECT_VIEW=http://localhost:3000

# Docker profile only (MySQL container)
MYSQL_ROOT_PASSWORD=
MYSQL_DATABASE=SpadeDB
MYSQL_USER=spade
MYSQL_PASSWORD=
```

Create `spadeboot/run.sh`, then `chmod +x spadeboot/run.sh`:

```bash
#!/usr/bin/env bash
# Starts the backend with the variables in spadeboot/.env (never committed).
set -euo pipefail
cd "$(dirname "$0")"
if [[ ! -f .env ]]; then
  echo "spadeboot/.env is missing: cp .env.example .env and fill it in (see USAGE.md)." >&2
  exit 1
fi
set -a
# shellcheck disable=SC1091
source .env
set +a
exec ./mvnw spring-boot:run
```

In `spadeboot/docker-compose.yml`, in the `app:` service, insert `env_file` directly above `environment:`:

```yaml
    env_file:
      - .env
    environment:
```

(Use the Edit tool: replace `      dockerfile: Dockerfile\n    environment:` with `      dockerfile: Dockerfile\n    env_file:\n      - .env\n    environment:`.)

- [ ] **Step 14: Give the one Spring context test its own secret**

In `spadeboot/src/test/java/com/spadeboot/SpadebootApplicationTests.java`, replace `@SpringBootTest` with:

```java
@SpringBootTest(properties = "app.jwt.secret=test-only-secret-not-used-anywhere-else-0123456789")
```

- [ ] **Step 15: Un-ignore the now secret-free configs**

In `spadeboot/.gitignore`, replace the block from `# Spring Boot Config (sensible Daten)` to the end of the file (the last line is `.env*`, with no trailing newline) with:

```
# Secrets. Real values live in spadeboot/.env (template: .env.example).
src/main/resources/keystore.p12
src/main/resources/app.key
src/main/resources/app.pub
gurobi.lic*
.env
!.env.example
```

Then run:
```bash
git status --short spadeboot
```
Expected: exactly these lines (any order); **no** `application-dev.yml`, `application-prod.yml` or `.env`:
```
 M spadeboot/.DS_Store
 M spadeboot/.gitignore
 M spadeboot/docker-compose.yml
 M spadeboot/src/main/java/com/spadeboot/config/DataInitializer.java
 M spadeboot/src/main/java/com/spadeboot/security/JwtUtils.java
 M spadeboot/src/test/java/com/spadeboot/SpadebootApplicationTests.java
 D spadeboot/src/main/resources/info
?? spadeboot/.env.example
?? spadeboot/run.sh
?? spadeboot/src/main/resources/application-docker.yml
?? spadeboot/src/main/resources/application.yml
?? spadeboot/src/test/java/com/spadeboot/config/
?? spadeboot/src/test/java/com/spadeboot/security/
```

- [ ] **Step 16: Leak scan: no backed-up secret value appears in anything about to be tracked**

Create `$SCRATCH/leak_scan.py`:

```python
"""Exit 1 if any secret value from the backup appears in a tracked or staged file. Prints hashes only."""
import hashlib, re, subprocess, sys
from pathlib import Path

backup = Path(sys.argv[1])
values = set()
for f in backup.iterdir():
    for m in re.finditer(r'(?m)^\s*[\w.-]*(secret|password|token|key-store-password)[\w.-]*\s*[:=]\s*["\']?([^\s"\'#]{6,})', f.read_text(), re.I):
        values.add(m.group(2))
files = subprocess.run(["git", "ls-files", "--cached", "--others", "--exclude-standard"],
                       capture_output=True, text=True, check=True).stdout.split()
hits = []
for name in files:
    p = Path(name)
    if not p.is_file() or p.stat().st_size > 2_000_000:
        continue
    text = p.read_text(errors="ignore")
    for v in values:
        if v in text:
            hits.append((name, hashlib.sha256(v.encode()).hexdigest()[:10]))
print(f"{len(values)} secret values checked against {len(files)} files")
for name, h in hits:
    print("LEAK", name, h)
sys.exit(1 if hits else 0)
```

Run: `python3 -I $SCRATCH/leak_scan.py ~/spade-secrets-backup/2026-10-09`
Expected: one summary line, no `LEAK` lines, exit 0.

- [ ] **Step 17: Run the Task 2 tests together**

Run: `cd spadeboot && ./mvnw -q test -Dtest='JwtUtilsTest,DataInitializerTest,SpadebootApplicationTests'; cd ..`
Expected: exit 0. `GameServiceTest` is **not** in this run; it is still red until Task 8.

- [ ] **Step 18: Boot it the way the owner will**

```bash
spadeboot/run.sh > $SCRATCH/boot.log 2>&1 &
for i in $(seq 1 60); do grep -q 'Started SpadebootApplication' $SCRATCH/boot.log && break; sleep 2; done
grep -E 'Started SpadebootApplication|profile is active|Seed user created' $SCRATCH/boot.log | head -10
grep -cF "$(sed -n "s/^SPADE_SEED_PASSWORD='\(.*\)'$/\1/p" spadeboot/.env)" $SCRATCH/boot.log   # prints a count only
curl -sk -o /dev/null -w '%{http_code}\n' https://localhost:8080/api/cheatsheet/chips/presets
kill %1; pkill -f SpadebootApplication || true      # spring-boot:run forks a JVM
```
Expected:
- `The following 1 profile is active: "dev"`;
- seven `Seed user created:` lines (Hoerter plus six players);
- the seed-password count is `0`: the password never reaches the log;
- curl prints `200`.

- [ ] **Step 19: Commit**

```bash
git add spadeboot/.gitignore spadeboot/docker-compose.yml spadeboot/.env.example spadeboot/run.sh \
  spadeboot/src/main/resources/application.yml spadeboot/src/main/resources/application-docker.yml \
  spadeboot/src/main/java/com/spadeboot/config/DataInitializer.java \
  spadeboot/src/main/java/com/spadeboot/security/JwtUtils.java \
  spadeboot/src/test/java/com/spadeboot/SpadebootApplicationTests.java \
  spadeboot/src/test/java/com/spadeboot/config/DataInitializerTest.java \
  spadeboot/src/test/java/com/spadeboot/security/JwtUtilsTest.java
git diff --cached --name-only | grep -E '(^|/)\.env$|\.pem$|gurobi|application-(dev|prod)\.yml' ; echo "exit=$? (expect 1: nothing matched)"
git commit -m "fix(security): secrets leave the repo — env-based config, fail-fast JWT secret, dev-only seeding without hard-coded passwords (V0 Phase 01, D15, C7, C8)"
```

- [ ] **Step 20: Fresh-clone check** (Review Focus 1)

```bash
rm -rf $SCRATCH/fresh && git clone -q . $SCRATCH/fresh && cd $SCRATCH/fresh/spadeboot
./run.sh; echo "exit=$?"
# Expected: "spadeboot/.env is missing: cp .env.example .env …", exit=1
./mvnw -q spring-boot:run > $SCRATCH/fresh-nosecret.log 2>&1; grep -m1 -o 'SPADE_JWT_SECRET[^"]*' $SCRATCH/fresh-nosecret.log
# Expected: the validateSecret message naming SPADE_JWT_SECRET; the process exits non-zero
SPADE_JWT_SECRET="$(openssl rand -base64 48)" ./mvnw -q spring-boot:run > $SCRATCH/fresh.log 2>&1 &
for i in $(seq 1 60); do grep -q 'Started SpadebootApplication' $SCRATCH/fresh.log && break; sleep 2; done
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/cheatsheet/chips/presets   # Expected: 200 (no SSL, no profile, no seed)
kill %1; pkill -f SpadebootApplication || true; cd /Users/sebastianrogg/IdeaProjects/SPADE
```

### Task 3: Untrack the keys and `.DS_Store`; tidy the root `.gitignore`

**Files:**
- Modify: `.gitignore`.
- Untrack: `client/{cert,key}.pem`, `webapp/{cert,key}.pem`, every `.DS_Store`.

**Interfaces:**
- Consumes: Task 2 committed.
- Produces: `.worktrees/`, `.claude/*` (with exceptions), `.playwright/`, `.superpowers/` and the Python caches are ignored. Phases 02–05 rely on `.worktrees/` being ignored.

- [ ] **Step 1: Untrack, keeping the files on disk**

```bash
git rm -q --cached client/cert.pem client/key.pem webapp/cert.pem webapp/key.pem
git ls-files -z '*.DS_Store' | xargs -0 git rm -q --cached
git ls-files | grep -E '\.pem$|DS_Store' ; echo "exit=$? (expect 1)"
ls client/key.pem webapp/key.pem    # Expected: both still exist on disk
```

- [ ] **Step 2: Edit the root `.gitignore`**

Delete the block that starts `# Spring Boot Config (sensible Daten)` and runs to the end of the file. Its root-relative paths never matched `spadeboot/…`, so it was a no-op. Append:

```
# Secrets: never tracked (spadeboot/.env.example is the template)
*.p12
gurobi.lic*

# Python (cv/, scripts/)
__pycache__/
.venv/
.pytest_cache/
.ruff_cache/

# Agent tooling: only the committed parts of .claude/ are tracked
.claude/*
!.claude/settings.json
!.claude/hooks/
!.claude/skills/
.worktrees/
.playwright/
.superpowers/
```

- [ ] **Step 3: Nothing new became visible**

Run: `git status --short`
Expected: 11 `D ` lines (4 `.pem`, 7 `.DS_Store`) and ` M .gitignore`. No `??` lines at all.

- [ ] **Step 4: Commit**

```bash
git add .gitignore
git commit -m "chore(security): stop tracking TLS keys and .DS_Store; ignore agent scratch dirs and worktrees (V0 Phase 01, D15)"
```

### Task 4: Rewrite history (needs the owner's explicit yes)

**Files:** none (history only).

**Interfaces:**
- Consumes: Tasks 1–3.
- Produces:
  - a `master` with no `gurobi.lic`, `*.pem` or `.DS_Store` in any commit;
  - no `add_first_game_logic` branch on GitHub;
  - `origin` pointing at `https://github.com/eixfachZabii/Spade.git`.
  - **Every SHA changes.** Docs must not cite pre-rewrite SHAs from here on.

- [ ] **Step 1: Ask, and wait for a literal yes**

Say, in chat: *"Task 4 rewrites master's history and force-pushes it, and deletes the `add_first_game_logic` branch on GitHub. 0 forks, only you. The branch is archived locally first. Go?"* Do nothing further without a yes.

- [ ] **Step 2: Archive the orphan branch outside the repo** (it contains secrets; local only)

```bash
mkdir -p ~/spade-archive && chmod 700 ~/spade-archive
git bundle create ~/spade-archive/add_first_game_logic-2026-10-09.bundle origin/add_first_game_logic
git bundle verify ~/spade-archive/add_first_game_logic-2026-10-09.bundle    # Expected: "… is okay"
```

- [ ] **Step 3: Preconditions**

```bash
git status --short          # Expected: no output (ignored files are fine)
git worktree list           # Expected: only the main checkout
git branch                  # Expected: only master
```

- [ ] **Step 4: Rewrite**

```bash
git filter-repo --force --invert-paths \
  --path spadeboot/gurobi.lic \
  --path client/cert.pem --path client/key.pem \
  --path webapp/cert.pem --path webapp/key.pem \
  --path-glob '*.DS_Store'
```
Expected: it ends with "Completely finished after …". filter-repo removes `origin` on purpose.

- [ ] **Step 5: Verify that no reachable object holds them**

```bash
git rev-list --all --objects | grep -E 'gurobi\.lic|\.pem$|\.DS_Store$' ; echo "exit=$? (expect 1)"
git log --oneline | wc -l          # Expected: the same commit count as before the rewrite, give or take empty commits filter-repo prunes
ls client/key.pem spadeboot/.env   # Expected: still on disk (untracked)
```

- [ ] **Step 6: Push, after the yes from step 1**

```bash
git remote add origin https://github.com/eixfachZabii/Spade.git
git push origin --delete add_first_game_logic
git push --force origin master
git branch -u origin/master master
gh api repos/eixfachZabii/Spade/branches --jq '.[].name'   # Expected: master
```

- [ ] **Step 7: Note the leftover risk.** GitHub may still serve old commits by SHA until it garbage-collects them; rotation (Task 1) is what protects you. Optional: ask GitHub Support to purge cached views of `eixfachZabii/Spade`.

**Phase 01 done when:**
- a fresh clone boots (Task 2, step 20);
- `git rev-list --all --objects` contains no secret file;
- GitHub shows only `master`.

---

# Phase 02 · Shape (worktree `phase/02-shape`)

```bash
git worktree add .worktrees/phase-02-shape -b phase/02-shape && cd .worktrees/phase-02-shape
```

### Task 5: Import `cv/` from `lucabzt/spadeAI@9e4ec5e`

**Files:**
- Create: `cv/app.py`, `cv/camera.py`, `cv/utils.py` (copies with 3 edits), `cv/models/best_60_23.pt` (LFS), `cv/pyproject.toml`, `cv/uv.lock` (generated), `cv/.gitignore`, `cv/README.md`, `cv/tests/test_smoke.py`
- Create or modify: `.gitattributes`

**Interfaces:**
- Consumes: nothing from the backend.
- Produces:
  - `cv/utils.py` exposes `process_raw_image(bytes) -> np.ndarray`, `get_n_cards(model, image, n) -> list[str]` and `get_comm_cards(model, n) -> list[str]` (still a stub);
  - socket.io events `frame`, `comm_cards`, `getFrame`, `recalibrate` on `:5001`;
  - card labels are rank+suit (`2C` … `10S` … `AS`).

- [ ] **Step 1: Install git-lfs and track the model**

```bash
brew install git-lfs && git lfs install
git lfs track "cv/models/*.pt"
cat .gitattributes          # Expected: cv/models/*.pt filter=lfs diff=lfs merge=lfs -text
```

- [ ] **Step 2: Write the failing smoke tests**

Create `cv/tests/test_smoke.py`:

```python
"""Smoke tests: the model is real and knows the 52 cards; the pure helpers behave.

These do NOT measure card-reading accuracy (the gate says so in its "not proven" block).
"""
from pathlib import Path

import cv2
import numpy as np

from utils import get_n_cards, process_raw_image

MODEL = Path(__file__).resolve().parent.parent / "models" / "best_60_23.pt"
RANKS = ["2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"]
EXPECTED_LABELS = {rank + suit for rank in RANKS for suit in "CDHS"}


def test_model_file_is_real_not_an_lfs_pointer():
    assert MODEL.is_file(), f"{MODEL} is missing"
    assert MODEL.stat().st_size > 1_000_000, (
        f"{MODEL} is {MODEL.stat().st_size} bytes, so it is a Git LFS pointer: run `git lfs pull`"
    )


def test_model_knows_exactly_the_52_cards():
    from ultralytics import YOLO

    model = YOLO(str(MODEL))
    assert set(model.names.values()) == EXPECTED_LABELS


class _Box:
    def __init__(self, cls):
        self.cls = cls


class _Result:
    def __init__(self, classes):
        self.boxes = [_Box(c) for c in classes]


class _FakeModel:
    names = {0: "AS", 1: "10H", 2: "KD"}

    def __init__(self, classes):
        self._classes = classes

    def __call__(self, image):
        return [_Result(self._classes)]


def test_get_n_cards_dedupes_corner_detections_and_caps_at_n():
    # One physical card is usually detected twice, once per printed corner.
    model = _FakeModel([0, 0, 1, 1, 2])
    assert get_n_cards(model, image=None, n=2) == ["AS", "10H"]


def test_process_raw_image_round_trips_a_jpeg():
    image = np.zeros((8, 8, 3), dtype=np.uint8)
    image[:, :, 2] = 255
    ok, buffer = cv2.imencode(".jpg", image)
    assert ok
    assert process_raw_image(buffer.tobytes()).shape == (8, 8, 3)
```

- [ ] **Step 3: `pyproject.toml`, `.gitignore`; watch the tests fail**

Create `cv/pyproject.toml`:

```toml
[project]
name = "spade-cv"
version = "0.1.0"
description = "Spade card detection: YOLOv8 reads hole cards from phone frames; table-camera feed over socket.io"
requires-python = ">=3.12,<3.13"
dependencies = [
    "eventlet>=0.36",
    "flask>=3.0",
    "flask-cors>=4.0",
    "flask-socketio>=5.3",
    "numpy>=1.26",
    "opencv-python>=4.9",
    "torch>=2.2",
    "torchvision>=0.17",
    "ultralytics>=8.3.78",
]

[dependency-groups]
dev = ["pytest>=8", "ruff>=0.6"]

# CI (Linux) gets CPU-only torch wheels instead of the multi-GB CUDA ones; macOS keeps PyPI's (MPS).
[tool.uv.sources]
torch = [{ index = "pytorch-cpu", marker = "sys_platform == 'linux'" }]
torchvision = [{ index = "pytorch-cpu", marker = "sys_platform == 'linux'" }]

[[tool.uv.index]]
name = "pytorch-cpu"
url = "https://download.pytorch.org/whl/cpu"
explicit = true

[tool.pytest.ini_options]
testpaths = ["tests"]
pythonpath = ["."]

[tool.ruff]
target-version = "py312"
line-length = 120

[tool.ruff.lint]
# Pinned explicitly so a user-level ruff config cannot change what the gate checks.
select = ["E4", "E7", "E9", "F"]
```

Create `cv/.gitignore`:

```
.venv/
__pycache__/
.pytest_cache/
.ruff_cache/
certificates/
runs/
```

Run: `cd cv && uv sync && uv run pytest -q; cd ..`
Expected: `ModuleNotFoundError: No module named 'utils'` (collection error). The first `uv sync` downloads torch, which takes a few minutes.

- [ ] **Step 4: Copy the code and the model**

```bash
SRC=~/PycharmProjects/spadeAI
git -C $SRC rev-parse --short HEAD        # Expected: 9e4ec5e
mkdir -p cv/models
cp $SRC/app.py $SRC/camera.py $SRC/utils.py cv/
cp $SRC/models/best_60_23.pt cv/models/
# handy/cams.py is not copied: every line of it is commented out.
```

- [ ] **Step 5: Three minimal edits**

1. `cv/app.py`: delete the line `import ssl` (unused). Replace `MODEL_PATH = "./models/best_60_23.pt"` with:
   ```python
   MODEL_PATH = str(Path(__file__).resolve().parent / "models" / "best_60_23.pt")
   ```
   and add `from pathlib import Path` under `import os`. The model then loads from any working directory.
2. `cv/camera.py`: delete the line `import matplotlib.pyplot as plt` (unused, and not a dependency). Replace `        if cv2.waitKey(1) == ord('q'): break` with:
   ```python
           if cv2.waitKey(1) == ord('q'):
               break
   ```
3. No edits to `cv/utils.py`.

- [ ] **Step 6: Lock, lint, test: green**

```bash
cd cv
uv lock && uv sync --locked
uv run --locked ruff check .        # Expected: All checks passed!
uv run --locked pytest -q           # Expected: 4 passed
cd ..
```

- [ ] **Step 7: The service starts** (the camera is opened only under `__main__`; this only checks import and model load)

```bash
cd cv && uv run --locked python -c "import app; print(type(app.model).__name__, len(app.model.names))"; cd ..
```
Expected: `YOLO 52`.

- [ ] **Step 8: `cv/README.md`**

```markdown
# cv: Spade card detection

Spade's eyes. A Flask-SocketIO service that reads playing cards with a YOLOv8 model.

**Provenance.** Snapshot of [`lucabzt/spadeAI@9e4ec5e`](https://github.com/lucabzt/spadeAI/tree/9e4ec5e) (2025-03-24), written by Luca Bozzetti and Sebastian Rogg. Copied without history (V0 spec D4). Three changes at import: the model path is resolved relative to this file, two unused imports were removed, and a one-line `if … : break` was split.

## Run
    uv sync                      # first run downloads torch
    uv run python app.py         # socket.io on 0.0.0.0:5001, opens camera index 0
    uv run pytest -q             # smoke tests (no camera needed)

The model `models/best_60_23.pt` is stored in Git LFS. If tests say "LFS pointer", run `git lfs pull`.

## Contract (socket.io, answers via the ack callback)
| Event | Request | Ack |
|---|---|---|
| `frame` | `{n: int, image: ArrayBuffer (JPEG)}` | `{predictions: [label…], found: bool}`: duplicates removed, cut to `n` |
| `comm_cards` | `{n: int}` | **stub**: always `["QS","AS","KS"]` |
| `getFrame` | `{tableId}` | `{success, image: JPEG bytes}` (cropped table frame, spades boxed) |
| `recalibrate` | `{tableId}` | `{success, message?}` |

Labels are rank+suit: `2C` … `10S` … `JD`, `QH`, `KS`, `AC` (52 classes).

## Known gaps (see the issues labelled `area:cv`)
- Community-card detection was never built (`get_comm_cards` is a stub). This is the V1 spike.
- Serves plain HTTP. Phones need HTTPS for the camera.
- The legacy `webapp/` connects socket.io to the backend's port, not to `:5001`, so it never reaches this service.
- The card notation differs from the backend (`ACEH`-style) and the legacy client.
```

- [ ] **Step 9: Commit**

```bash
git add .gitattributes cv/
git lfs ls-files                    # Expected: one line ending "cv/models/best_60_23.pt"
git commit -m "feat(cv): import the card-detection service from lucabzt/spadeAI@9e4ec5e — uv project, model in LFS, smoke tests (V0 Phase 02, D4)"
```

### Task 6: Move the ledger spreadsheet to references

**Files:** Move: `spadeboot/src/main/resources/Poker_Chip_Tracker.xlsx` → `docs/references/poker-chip-tracker.xlsx`

**Interfaces:**
- Consumes: nothing.
- Produces: `docs/references/poker-chip-tracker.xlsx`, which Task 14 indexes.

- [ ] **Step 1: Prove no backend code reads it**

Run: `grep -rn "Poker_Chip_Tracker" spadeboot/src`
Expected: no output. (`client/src/layouts/analytics/data/ExtractExcel.js` mentions it, but that's a legacy Node script and is not run.)

- [ ] **Step 2: Move it, then check the build**

```bash
mkdir -p docs/references
git mv spadeboot/src/main/resources/Poker_Chip_Tracker.xlsx docs/references/poker-chip-tracker.xlsx
(cd spadeboot && ./mvnw -q -o compile)    # Expected: exit 0
git commit -m "docs(references): the 20-night ledger spreadsheet leaves the backend's resources — no code reads it (V0 Phase 02)"
```

**Phase 02 done when** `cv/` tests pass from a clean `uv sync --locked`. Merge with the Phase merge procedure; the gate doesn't exist yet, so run the cv tests in its place.

---

# Phase 03 · Gate (worktree `phase/03-gate`)

```bash
git worktree add .worktrees/phase-03-gate -b phase/03-gate && cd .worktrees/phase-03-gate
```

### Task 7: Table-driven hand evaluator test; fix flush and full house

**Files:**
- Create: `spadeboot/src/test/java/com/spadeboot/domain/game/HandEvaluationTest.java`
- Modify: `spadeboot/src/main/java/com/spadeboot/domain/game/HandEvaluation.java`

**Interfaces:**
- Consumes: `HandEvaluation.cardsToRankNumber(List<Card>) -> long` (unchanged signature).
- Produces: an evaluator that is correct on the 18 pinned cases.
  - Rank number = `category × 100⁵ + five deciding values` (two digits each).
  - Categories: 9 straight flush · 8 quads · 7 full house · 6 flush · 5 straight · 4 trips · 3 two pair · 2 pair · 1 high card.

- [ ] **Step 1: Write the test**

Create `spadeboot/src/test/java/com/spadeboot/domain/game/HandEvaluationTest.java`:

```java
package com.spadeboot.domain.game;

import com.spadeboot.domain.card.Card;
import com.spadeboot.domain.card.Suit;
import com.spadeboot.domain.card.Value;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Seven-card hands against their rank number:
 * category * 100^5 + the five deciding card values, two digits each.
 * Categories: 9 straight flush, 8 quads, 7 full house, 6 flush, 5 straight,
 * 4 trips, 3 two pair, 2 pair, 1 high card. Ace = 14. In an ace-low straight the
 * ace moves to the end, still written 14 (so 5-4-3-2-A ranks below 6-high).
 * Card notation here: rank 2-9 T J Q K A + suit h d c s.
 */
class HandEvaluationTest {

    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource(delimiter = '|', value = {
        "royal flush                      | Th Jh Qh Kh Ah 9d 8c | 91413121110",
        "straight flush, 9 high           | 5c 6c 7c 8c 9c Kd Ah | 90908070605",
        "ace-low straight flush           | Ad 2d 3d 4d 5d Kc Qh | 90504030214",
        "four of a kind                   | As Ah Ad Ac Kd 2c 3h | 81414141413",
        "full house                       | Ks Kh Kd 9c 9d 2s 3h | 71313130909",
        "full house from two trips        | 9s 9h 9d Kc Kd Ks 2h | 71313130909",
        "full house: trips + two pairs    | 5s 5h 5d Kc Kd 2s 2h | 70505051313",
        "flush                            | 2h 7h 9h Jh Kh 3d 4c | 61311090702",
        "flush of six takes the top five  | 2h 7h 9h Jh Kh Ah 4c | 61413110907",
        "flush beats the straight inside  | 4h 5h 6h 7d 8h Kh 2c | 61308060504",
        "straight                         | 6h 7d 8c 9s Td 2c 2h | 51009080706",
        "ace-low straight                 | Ah 2d 3c 4s 5h 9d Kc | 50504030214",
        "straight with a paired card      | 5h 6d 6c 7s 8d 9c Kh | 50908070605",
        "three of a kind                  | 7s 7h 7d Ac Kd 2s 3h | 40707071413",
        "two pair, ace kicker             | Js Jh 4d 4c Ad 2s 3h | 31111040414",
        "three pairs: best two + kicker   | As Ah Kd Kc Qs Qh 2d | 31414131312",
        "pair                             | Ts Th 4d 8c Ad 2s 3h | 21010140804",
        "high card                        | 2s 4h 7d 9c Jd Qs Ah | 11412110907",
    })
    void ranksSevenCardHands(String description, String hand, long expected) {
        assertEquals(expected, HandEvaluation.cardsToRankNumber(cards(hand)));
    }

    static List<Card> cards(String hand) {
        List<Card> cards = new ArrayList<>();
        for (String token : hand.trim().split("\\s+")) {
            Card card = new Card();
            card.setValue(value(token.substring(0, token.length() - 1)));
            card.setSuit(suit(token.charAt(token.length() - 1)));
            cards.add(card);
        }
        return cards;
    }

    private static Value value(String rank) {
        return switch (rank) {
            case "T" -> Value.TEN;
            case "J" -> Value.JACK;
            case "Q" -> Value.QUEEN;
            case "K" -> Value.KING;
            case "A" -> Value.ACE;
            default -> Value.values()[Integer.parseInt(rank) - 2];
        };
    }

    private static Suit suit(char suit) {
        return switch (suit) {
            case 'h' -> Suit.HEARTS;
            case 'd' -> Suit.DIAMONDS;
            case 'c' -> Suit.CLUBS;
            case 's' -> Suit.SPADES;
            default -> throw new IllegalArgumentException("unknown suit " + suit);
        };
    }
}
```

- [ ] **Step 2: Run it: exactly these 8 fail**

Run: `cd spadeboot && ./mvnw -q test -Dtest=HandEvaluationTest; cd ..`
Expected: `Tests run: 18, Failures: 8`. The failing rows are royal flush (got `51413121110`), straight flush 9 high, ace-low straight flush, full house from two trips (got `70909091313`), full house trips + two pairs (got `70505050202`), flush (got `11311090704`), flush of six, and flush beats the straight inside. If a *different* set fails, stop: the evaluator isn't what this plan measured.

- [ ] **Step 3: Fix the suit comparison**

In `HandEvaluation.java`, `filterBySuit` compares the `Suit` enum to a `String`, which is never equal. Change the signature and the comparison:

```java
    private static List<Card> filterBySuit(List<Card> cards, Suit suit) {
        List<Card> result = new ArrayList<>();
        for (Card card : cards) {
            if (card.getSuit() == suit) {
                result.add(card);
            }
        }
        return result;
    }
```

and its four callers at the top of `cardsToRankNumber`:
- `filterBySuit(cards, "S")` → `filterBySuit(cards, Suit.SPADES)`
- `"H"` → `Suit.HEARTS`
- `"C"` → `Suit.CLUBS`
- `"D"` → `Suit.DIAMONDS`

- [ ] **Step 4: Fix the full house: the highest trips, then the highest other pair**

Replace:

```java
        Integer threeCard = getKeyWithCount(cardCounts, 3);
        if (threeCard != null) {
            Integer twoCard = getKeyWithCountExcluding(cardCounts, 2, threeCard);
```

with:

```java
        // HashMap order is not rank order: take the HIGHEST triple, then the highest
        // other rank holding at least two cards (a second triple counts as the pair).
        Integer threeCard = getHighestKeyWithCount(cardCounts, 3, null);
        if (threeCard != null) {
            Integer twoCard = getHighestKeyWithCount(cardCounts, 2, threeCard);
```

Delete the now-unused `getKeyWithCountExcluding` method (with its Javadoc), and add in its place:

```java
    /**
     * Returns the highest key whose count is at least targetCount, skipping {@code exclude},
     * or null if there is none.
     */
    private static Integer getHighestKeyWithCount(Map<Integer, Integer> counts, int targetCount, Integer exclude) {
        Integer best = null;
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            int key = entry.getKey();
            if (exclude != null && key == exclude) {
                continue;
            }
            if (entry.getValue() >= targetCount && (best == null || key > best)) {
                best = key;
            }
        }
        return best;
    }
```

- [ ] **Step 5: Run it: all green**

Run: `cd spadeboot && ./mvnw -q test -Dtest=HandEvaluationTest; cd ..`
Expected: exit 0; the surefire report shows `tests="18" failures="0"`.

- [ ] **Step 6: Revert-and-observe (`verify-change` trap 1)**

```bash
git diff -- spadeboot/src/main/java/com/spadeboot/domain/game/HandEvaluation.java > $SCRATCH/eval-fix.patch
git checkout -- spadeboot/src/main/java/com/spadeboot/domain/game/HandEvaluation.java
(cd spadeboot && ./mvnw -q test -Dtest=HandEvaluationTest) ; echo "exit=$? (expect non-zero: 8 failures)"
git apply $SCRATCH/eval-fix.patch
(cd spadeboot && ./mvnw -q test -Dtest=HandEvaluationTest) ; echo "exit=$? (expect 0)"
```

- [ ] **Step 7: Commit**

```bash
git add spadeboot/src/test/java/com/spadeboot/domain/game/HandEvaluationTest.java spadeboot/src/main/java/com/spadeboot/domain/game/HandEvaluation.java
git commit -m "fix(backend): flushes and straight flushes are detected again; a full house takes the highest trips and pair (V0 Phase 03, D20, C1, C2)"
```

### Task 8: Repair the `GameServiceTest` fixtures

**Files:** Modify: `spadeboot/src/test/java/com/spadeboot/GameServiceTest.java`

**Interfaces:**
- Consumes: `Player.setUser(User)`, `User.setId(Long)`, `User.setUsername(String)` (Lombok setters).
- Produces: a fully green `./mvnw verify`.

- [ ] **Step 1: See the RED**

Run: `cd spadeboot && ./mvnw -q test -Dtest=GameServiceTest; cd ..`
Expected: 9 tests, 5 failing or erroring, each with `NullPointerException` at `Player.getUserId`.

- [ ] **Step 2: Give each fixture player a user**

Add `import com.spadeboot.domain.user.User;` after the `Player` import. Insert this helper directly above `@BeforeEach`:

```java
    private static Player player(long playerId, long userId) {
        User user = new User();
        user.setId(userId);
        user.setUsername("user" + userId);
        Player player = new Player();
        player.setId(playerId);
        player.setUser(user);
        player.setChips(1000);
        return player;
    }
```

In `setUp()`, replace the three blocks from `testOwner = new Player();` through `testPlayer2.setChips(1000);` with:

```java
        testOwner = player(1L, 1L);
        testPlayer1 = player(2L, 2L);
        testPlayer2 = player(3L, 3L);
```

- [ ] **Step 3: GREEN, then the whole backend**

```bash
(cd spadeboot && ./mvnw -q test -Dtest=GameServiceTest)   # Expected: exit 0, tests="9" failures="0" errors="0"
(cd spadeboot && ./mvnw -q -B verify)                     # Expected: exit 0
```
(This repair was trialled on a scratch copy on 2026-10-09: 9/9 pass.)

- [ ] **Step 4: Commit**

```bash
git add spadeboot/src/test/java/com/spadeboot/GameServiceTest.java
git commit -m "test(backend): GameServiceTest fixtures give each player a user — 5 stale NPEs gone, verify is green (V0 Phase 03)"
```

### Task 9: Copy the generic doc checks from WealthWatcher

**Files:** Create: `scripts/check_doc_links.py`, `scripts/check_commit_refs.py`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `python3 scripts/check_doc_links.py [--list]`: exit 1 on a broken relative link;
  - `python3 scripts/check_commit_refs.py [--range A..B|--all]`: exit 1 on a negated close keyword in unpushed commits.

- [ ] **Step 1: Copy**

```bash
WW="/Users/sebastianrogg/PycharmProjects/Hackathons & Projekte/WealthWatchter/scripts"
mkdir -p scripts && cp "$WW/check_doc_links.py" "$WW/check_commit_refs.py" scripts/ && chmod +x scripts/*.py
```

- [ ] **Step 2: Adapt three things**

1. `scripts/check_doc_links.py`: replace the `SKIP_DIRS = {…}` line with
   ```python
   SKIP_DIRS = {".git", "node_modules", ".venv", ".worktrees", ".impeccable", ".superpowers", "target", "build", ".pytest_cache"}
   ```
   (`target` and `build` hold Maven and CRA output.) In the docstring, change "`grounded_news` emits" to "WealthWatcher's `grounded_news` emits", so the story stays true as a provenance note.
2. `scripts/check_commit_refs.py`: change `https://github.com/eixfachZabii/WealthWatchter/issues/18` in the docstring to `https://github.com/eixfachZabii/Spade/issues/18`, and the sentinels `<|ww-field|>` / `<|ww-commit|>` to `<|spade-field|>` / `<|spade-commit|>`.
3. At the top of each docstring, add the line: `Copied from WealthWatcher (eixfachZabii/WealthWatchter) on 2026-10-09; its incident notes are WealthWatcher's.`

- [ ] **Step 3: Run both**

```bash
python3 scripts/check_doc_links.py --list     # Expected: "✓ N relative links resolve"
python3 scripts/check_commit_refs.py          # Expected: exit 0, no negated references
python3 scripts/check_commit_refs.py --all    # Expected: exit 0 (whole history)
```

- [ ] **Step 4: Commit**

```bash
git add scripts/check_doc_links.py scripts/check_commit_refs.py
git commit -m "chore(devex): doc-link and negated-close-keyword checks, copied from WealthWatcher (V0 Phase 03, D11)"
```

### Task 10: `scripts/gate.sh`

**Files:** Create: `scripts/gate.sh`

**Interfaces:**
- Consumes: Tasks 5, 8, 9.
- Produces: `scripts/gate.sh [--backend|--cv|--docs]`, which exits 0 only when every block it ran is green and prints `GATE GREEN` / `GATE RED`. Tasks 17 and 19 append lines to its `docs` block and its "not proven" block.

- [ ] **Step 1: Write it**

Create `scripts/gate.sh`, then `chmod +x scripts/gate.sh`:

```bash
#!/usr/bin/env bash
#
# The gate. One command: backend, cv, docs, and then what it did NOT prove.
#
#   scripts/gate.sh             everything (first cv run downloads torch)
#   scripts/gate.sh --backend   spadeboot only
#   scripts/gate.sh --cv        cv only
#   scripts/gate.sh --docs      docs and devex checks only
#
# Shape and the "not proven" block copied from WealthWatcher's gate (2026-10-09).
set -uo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO"

ONLY="${1:-all}"
case "$ONLY" in
  all|--backend|--cv|--docs) ;;
  --help|-h) sed -n '2,9p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
  *) echo "unknown option: $ONLY (try --help)" >&2; exit 2 ;;
esac
want() { [[ "$ONLY" == all || "$ONLY" == "--$1" ]]; }

if [[ -t 1 ]]; then R=$'\e[31m'; G=$'\e[32m'; Y=$'\e[33m'; D=$'\e[2m'; N=$'\e[0m'
else R=; G=; Y=; D=; N=; fi

FAILED=()
step() { printf '%s\n' "${D}── $1${N}"; }
ok()   { printf '  %s✓%s %s\n' "$G" "$N" "$1"; }
bad()  { printf '  %s✗%s %s\n' "$R" "$N" "$1"; FAILED+=("$1"); }
run()  { # run <label> <cmd...>
  local label="$1"; shift
  local out; out="$("$@" 2>&1)"; local rc=$?
  if [[ $rc -eq 0 ]]; then ok "$label"
  else bad "$label"; printf '%s\n' "$out" | tail -30 | sed 's/^/      /'; fi
}

printf '\n%sSpade gate%s  %s%s%s\n\n' "$Y" "$N" "$D" "$(git rev-parse --abbrev-ref HEAD) @ $(git rev-parse --short HEAD)" "$N"

if want backend; then
  step "backend"
  run "spadeboot: mvn verify" sh -c 'cd spadeboot && ./mvnw -q -B verify'
fi

if want cv; then
  step "cv"
  run "cv: uv sync (locked)" sh -c 'cd cv && uv sync --locked --quiet'
  run "cv: ruff" sh -c 'cd cv && uv run --locked ruff check .'
  run "cv: pytest" sh -c 'cd cv && uv run --locked pytest -q'
fi

if want docs; then
  step "docs & devex"
  run "relative links resolve" python3 scripts/check_doc_links.py
  run "no negated close-keyword in unpushed commits" python3 scripts/check_commit_refs.py
fi

step "not proven by this gate"
DIRT="$(git status --porcelain)"
if [[ -n "$DIRT" ]]; then
  printf '  %s!%s working tree is dirty: a green gate proves this working copy, not your branch\n' "$Y" "$N"
  printf '%s\n' "$DIRT" | head -12 | sed 's/^/      /'
else
  ok "working tree clean"
fi
printf '  %s· client/ and webapp/ (frozen legacy) are not built or tested%s\n' "$D" "$N"
printf '  %s· cv: only that the model loads and knows 52 labels; card-reading ACCURACY is not measured%s\n' "$D" "$N"
printf '  %s· no real phone, real camera or real poker night was involved%s\n' "$D" "$N"
printf '  %s· the Docker image was not built%s\n' "$D" "$N"
[[ "$ONLY" != all ]] && printf '  %s!%s partial run (%s): the other blocks did not run\n' "$Y" "$N" "$ONLY"

echo
if [[ ${#FAILED[@]} -eq 0 ]]; then
  printf '%sGATE GREEN%s\n' "$G" "$N"; exit 0
else
  printf '%sGATE RED%s: %d failed\n' "$R" "$N" "${#FAILED[@]}"
  printf '  - %s\n' "${FAILED[@]}"; exit 1
fi
```

- [ ] **Step 2: Prove it can go RED**

```bash
echo "[broken](does-not-exist.md)" > docs/_gate_probe.md
scripts/gate.sh --docs; echo "exit=$?"     # Expected: ✗ relative links resolve … GATE RED, exit=1
rm docs/_gate_probe.md
```

- [ ] **Step 3: GREEN**

Run: `scripts/gate.sh`
Expected: `✓` on every line in backend, cv and docs, the not-proven block, then `GATE GREEN` and exit 0. The tree is dirty until you commit; that's expected and reported.

- [ ] **Step 4: From a subdirectory** (Review Focus 4)

Run: `(cd spadeboot && ../scripts/gate.sh --docs)`
Expected: `GATE GREEN`. The script `cd`s to the repo root itself.

- [ ] **Step 5: Commit**

```bash
git add scripts/gate.sh
git commit -m "chore(devex): scripts/gate.sh — backend, cv and docs in one command, plus what it did not prove (V0 Phase 03, D11)"
```

### Task 11: CI mirror of the gate

**Files:** Create: `.github/workflows/gate.yml`

**Interfaces:**
- Consumes: Task 10's commands.
- Produces: GitHub Actions workflow `gate`, run on every push to every branch.

- [ ] **Step 1: Write it**

```yaml
name: gate

on:
  push:
    branches: ['**']
  workflow_dispatch:

concurrency:
  group: gate-${{ github.ref }}
  cancel-in-progress: true

permissions:
  contents: read

jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
          cache: maven
      - name: mvn verify
        working-directory: spadeboot
        run: ./mvnw -q -B verify

  cv:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      # LFS bandwidth is metered: cache the objects by the list of LFS file ids.
      - name: list LFS objects
        run: git lfs ls-files -l | cut -d' ' -f1 | sort > .lfs-assets-id
      - uses: actions/cache@v4
        with:
          path: .git/lfs
          key: lfs-${{ hashFiles('.lfs-assets-id') }}
      - name: fetch LFS objects
        run: git lfs pull
      - name: system libs for opencv
        run: sudo apt-get update -qq && sudo apt-get install -y -qq libgl1 libglib2.0-0
      - uses: astral-sh/setup-uv@v6
        with:
          enable-cache: true
          cache-dependency-glob: cv/uv.lock
      - name: uv sync
        working-directory: cv
        run: uv sync --locked
      - name: ruff
        working-directory: cv
        run: uv run --locked ruff check .
      - name: pytest
        working-directory: cv
        run: uv run --locked pytest -q

  docs:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: '3.12'
      - name: relative links resolve
        run: python3 scripts/check_doc_links.py
```

(`check_commit_refs.py` stays local-only. By the time CI runs, the commits are pushed and any issue has already been closed.)

- [ ] **Step 2: Commit and push the branch** (announce: "pushing phase/03-gate so CI runs")

```bash
git add .github/workflows/gate.yml
git commit -m "chore(devex): CI runs the gate's backend, cv and docs blocks on every push (V0 Phase 03, D11)"
git push -u origin phase/03-gate
```

- [ ] **Step 3: Watch it**

Run: `gh run watch --repo eixfachZabii/Spade --exit-status $(gh run list --repo eixfachZabii/Spade --branch phase/03-gate --limit 1 --json databaseId --jq '.[0].databaseId')`
Expected: the `backend`, `cv` and `docs` jobs all ✓.
- If `cv` fails at `import cv2` with a missing `.so`, add that library to the apt line.
- If it fails with "LFS pointer", the cache or pull step is wrong.

### Task 12: The master-edit guard hook

**Files:** Create: `.claude/settings.json`, `.claude/hooks/guard-master.sh`, `.claude/hooks/test_guard_master.sh`

**Interfaces:**
- Consumes: Claude Code's PreToolUse JSON on stdin (`tool_input.file_path`).
- Produces:
  - exit 2 plus a message on stderr for a code edit on `master` in the main checkout;
  - exit 0 otherwise;
  - bypass with `SPADE_ALLOW_MASTER_EDIT=1`.

- [ ] **Step 1: Write the test first**

Create `.claude/hooks/test_guard_master.sh`, then `chmod +x` it:

```bash
#!/usr/bin/env bash
# Tests for guard-master.sh. Run from anywhere: .claude/hooks/test_guard_master.sh
set -uo pipefail
HOOK="$(cd "$(dirname "$0")" && pwd)/guard-master.sh"
MAIN="$(git -C "$(dirname "$0")" worktree list --porcelain | sed -n '1s/^worktree //p')"
fails=0
check() { # check <expected-exit> <description> <path> [env]
  local want="$1" desc="$2" path="$3"; shift 3
  printf '{"tool_input":{"file_path":"%s"}}' "$path" | env "$@" "$HOOK" >/dev/null 2>&1
  local got=$?
  if [[ $got -eq $want ]]; then echo "ok   $desc"; else echo "FAIL $desc (exit $got, want $want)"; fails=$((fails+1)); fi
}
BRANCH="$(git -C "$MAIN" rev-parse --abbrev-ref HEAD)"
if [[ "$BRANCH" == master ]]; then
  check 2 "backend code on master in main tree is blocked" "$MAIN/spadeboot/src/main/java/X.java"
  check 2 "cv code on master in main tree is blocked"      "$MAIN/cv/app.py"
  check 2 "legacy client code is blocked"                  "$MAIN/client/src/App.js"
  check 0 "a markdown file under cv/ passes"               "$MAIN/cv/README.md"
  check 0 "docs pass"                                      "$MAIN/docs/handoffs/INDEX.md"
  check 0 "the bypass variable passes"                     "$MAIN/cv/app.py" SPADE_ALLOW_MASTER_EDIT=1
else
  echo "skip main-tree cases: main checkout is on '$BRANCH', not master"
fi
WT="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
if [[ -f "$WT/.git" ]]; then
  check 0 "code inside a linked worktree passes" "$WT/spadeboot/src/main/java/X.java"
fi
check 0 "an empty payload passes" ""
exit $fails
```

Run: `.claude/hooks/test_guard_master.sh`
Expected: FAIL lines or "No such file" for the hook (RED).

- [ ] **Step 2: Write the hook**

Create `.claude/hooks/guard-master.sh`, then `chmod +x` it:

```bash
#!/usr/bin/env bash
#
# PreToolUse guard: no code edits on `master` in the main working tree.
# Adapted from WealthWatcher's hook (2026-10-09). Docs and markdown stay allowed;
# code goes through a worktree per phase (CLAUDE.md, "Working in this repo").
#
# Escape hatch, for a deliberate one-liner:  SPADE_ALLOW_MASTER_EDIT=1
#
set -uo pipefail

[[ -n "${SPADE_ALLOW_MASTER_EDIT:-}" ]] && exit 0

payload="$(cat)"
path="$(printf '%s' "$payload" | python3 -c 'import json,sys
try: print(json.load(sys.stdin).get("tool_input",{}).get("file_path",""))
except Exception: print("")' 2>/dev/null)"

[[ -z "$path" ]] && exit 0
[[ "$path" == *.md ]] && exit 0

case "$path" in
  *"/spadeboot/src/"*|*"/cv/"*|*"/client/src/"*|*"/webapp/src/"*|*"/scripts/"*) ;;
  *) exit 0 ;;
esac

repo_root="$(cd "$(dirname "$path")" 2>/dev/null && git rev-parse --show-toplevel 2>/dev/null)" || exit 0
[[ -z "$repo_root" ]] && exit 0
[[ -f "$repo_root/.git" ]] && exit 0          # a linked worktree has .git as a FILE

branch="$(git -C "$repo_root" rev-parse --abbrev-ref HEAD 2>/dev/null)"
[[ "$branch" != "master" ]] && exit 0

cat >&2 <<MSG
BLOCKED: code edit on 'master' in the main working tree.

  file: ${path#"$repo_root"/}

Make a worktree first:

  git worktree add .worktrees/<phase-or-fix> -b phase/<NN-slug>
  cd .worktrees/<phase-or-fix>

Then merge back with the ship-phase skill.
(Deliberate one-liner? Re-run with SPADE_ALLOW_MASTER_EDIT=1.)
MSG
exit 2
```

Note: the guard's directory test (`/cv/` etc.) uses the *edited file's* path. Paths whose directory doesn't exist yet fall through `cd … || exit 0` and pass. That's acceptable; the guard is a seatbelt, not a wall.

- [ ] **Step 3: `.claude/settings.json`**

```json
{
  "$comment": [
    "Committed, shared hook wiring (V0 Phase 03, D11). Personal permissions go in settings.local.json, which is gitignored.",
    "guard-master.sh blocks code edits on master in the main checkout; see CLAUDE.md 'Working in this repo'."
  ],
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "^(Edit|Write|MultiEdit|NotebookEdit)$",
        "hooks": [
          {
            "type": "command",
            "command": "\"$CLAUDE_PROJECT_DIR/.claude/hooks/guard-master.sh\"",
            "timeout": 5,
            "statusMessage": "Checking worktree"
          }
        ]
      }
    ]
  }
}
```

- [ ] **Step 4: GREEN**

Run: `.claude/hooks/test_guard_master.sh; echo "exit=$?"`
Expected: every line `ok`, exit 0. From inside the worktree, the main-tree cases run against `$MAIN`, which is on master.

- [ ] **Step 5: Commit, then merge Phase 03**

```bash
git add .claude/settings.json .claude/hooks/guard-master.sh .claude/hooks/test_guard_master.sh
git commit -m "chore(devex): guard hook — no code edits on master in the main checkout (V0 Phase 03, D11)"
```
Run the Phase merge procedure. **Phase 03 done when** the gate is GREEN locally on master **and** the `gate` workflow is green for the master push (`gh run list --branch master --limit 1`).

---

# Phase 04 · Docs (worktree `phase/04-docs`)

```bash
git worktree add .worktrees/phase-04-docs -b phase/04-docs && cd .worktrees/phase-04-docs
```

**Rule for every doc in this phase:**
- **Verify each claim against the code at HEAD, not against the audits or this plan.** The audits are dated evidence, and Phases 01–03 changed things (config, evaluator, tests, cv).
- A doc that lists API endpoints gets them from the code: `grep -rn "@\(Get\|Post\|Put\|Delete\)Mapping\|@RequestMapping\|@MessageMapping" spadeboot/src/main/java`.
- No hand-typed counts in CLAUDE.md. Elsewhere, a number must say "as of <date>" and how it was derived.

### Task 13: `docs/status-quo/`

**Files:** Create `docs/status-quo/README.md`, `backend.md`, `frontends.md`, `cv.md`, `lineage.md`, `salvage.md` (D21).

**Interfaces:**
- Consumes: `docs/handoffs/V0_audits/*.md` and the code at HEAD.
- Produces: the dated "before" picture that CLAUDE.md and the skills point to.

- [ ] **Step 1: Write each file with this skeleton**

```markdown
# <Area>: status quo, 2026-10-09

> Dated snapshot, not maintained (V0 spec §5). It describes Spade as V0 found it.
> **Changed since:** <one line per later change that makes a statement below untrue, with the phase>.
> Evidence: [audit](../handoffs/V0_audits/audit-<area>.md).
```

Sections per file:

| File | Sections (in order) | Source |
|---|---|---|
| `backend.md` | Stack · Package map · Domain model (the audit's mermaid `erDiagram`, re-checked against `@Entity` classes) · API surface (REST + STOMP tables, re-derived with the grep above) · Feature status (✅ / 🟡 / ❌) · Engine defects (the checklist that feeds the engine issue) · Security findings (**kinds only**: "hole cards reach every client", "password hash in a DTO"…, no secret file paths) · Tests | `audit-backend.md` §1–8 |
| `frontends.md` | The two apps and their roles · Route tables · End-to-end flow · API contract with backend match (✅ / dead) · Known legacy bugs (Jacks render as Aces, empty Win/Loss chart, socket.io aimed at the wrong port…) · Worth keeping / throw away · Brand assets (paths) | `audit-frontends.md` |
| `cv.md` | What `cv/` does · socket.io contract (link to `cv/README.md`, don't copy it) · Model (labels, size, LFS) · What was never built (community cards) · How the legacy webapp calls it, and why that fails | `audit-lineage.md` §C + `cv/` |
| `lineage.md` | Timeline table (repos, dates, authors by handle) · What survived · What was lost · Where it lives now (local paths + GitHub) | `audit-lineage.md` §A, D |
| `salvage.md` | **The salvage map (D21): what V1 takes from the old code.** One table per source (backend, hub `client/`, phone `webapp/`, `cv/`, predecessor repos). Each row is a component with a path, a verdict and a reason. Verdicts: **keep as-is** · **keep, adapt** · **concept only** (the idea or flow survives, the code doesn't) · **rewrite** · **drop**. Rows must cover at least: `HandEvaluation` (+ its 18 vectors), the seat-oval maths `positionUtils.js`, the chip colour scale, the card PNG deck and brand assets, the heatmap data, the chip optimiser, the Spotify flow, the JWT/auth flow, the lobby domain rules (bankroll → buy-in → stack; owner start/end/delete), the webapp's hole-card privacy pattern (hidden until tapped), the webapp's visual style (D24), `cv/` YOLO model + `get_n_cards`, the camera calibration idea, the 436 voice clips, the ledger spreadsheet's formulas | all audits + the code |
| `README.md` | One page: a works / partial / dead table across all areas, the top 5 risks, links to the five files | the five files |

"Changed since" lines that are true today:
- backend: "Phase 01: secrets moved to env, seeding dev-only. Phase 03: flush, straight flush and full house fixed; GameServiceTest repaired."
- cv: "Phase 02: imported into `cv/`."

- [ ] **Step 2: Check them**

```bash
python3 scripts/check_doc_links.py      # Expected: ✓
grep -rn -E 'gurobi\.lic|key\.pem|3f25aee|0d50468' docs/status-quo/ ; echo "exit=$? (expect 1: no leak pointers)"
```

- [ ] **Step 3: Commit**

```bash
git add docs/status-quo && git commit -m "docs(status-quo): the dated before-picture of backend, frontends, cv and lineage, and the salvage map V1 builds from (V0 Phase 04, D9, D21)"
```

### Task 14: `docs/references/`

**Files:** Create `docs/references/README.md`, `legacy-api.md`, `user-stories.md`, `uml/spade-uml.puml`, `uml/zuml.puml`, `hand-eval-vectors.md`, `voice-clips.md`, `webapp-style.md` (D24). `poker-chip-tracker.xlsx` already exists from Task 6.

**Interfaces:**
- Consumes: the predecessor repos (read-only):
  - `~/PycharmProjects/Spade` @ `1510db9`
  - `~/PycharmProjects/spadeAI` @ `9e4ec5e`
- Produces: the index that the CLAUDE.md "does it already exist?" step points to.

- [ ] **Step 1: Copy the UML sources**

```bash
mkdir -p docs/references/uml
git -C ~/PycharmProjects/Spade rev-parse --short HEAD          # Expected: 1510db9
cp ~/PycharmProjects/Spade/UML docs/references/uml/spade-uml.puml
cp ~/PycharmProjects/Spade/zUML.txt docs/references/uml/zuml.puml
plantuml -checkonly docs/references/uml/*.puml; echo "exit=$?"   # record the result in README; don't fix the diagrams
```

- [ ] **Step 2: Generate the voice-clip manifest** (no clips copied; they contain friends' names)

```bash
cd ~/PycharmProjects/Spade/server/assets/sounds && for d in */; do printf '| `%s` | %s |\n' "${d%/}" "$(find "$d" -name '*.mp3' | wc -l | tr -d ' ')"; done; ls *.mp3; cd -
```

Write `docs/references/voice-clips.md` with:
- a header: origin `lucabzt/Spade@1510db9:server/assets/sounds/`, voice "ElevenLabs Daniel", "manifest as of 2026-10-09; clips are not in this repo (they use friends' names)";
- the folder table above;
- the list of loose `* TODO.mp3` placeholders;
- one line: "Two-pair announcements were never recorded (audit-lineage §D)."

- [ ] **Step 3: `user-stories.md`**

Rewrite the 8 stories from `~/PycharmProjects/Spade/zDone` lines 469–476 in our words, as a numbered list. Each gets a status:
- **kept**: login with balance, join with a buy-in, bet/fold/raise/check, leave with chips back to balance;
- **open, needs-grill**: spectate, log every game for replay;
- **replaced**: invite players → friends;
- **reframed by D2**: create a game.

Add a note: "Source is a SiemensGPT chat transcript; only the stories are carried over, never its code."

- [ ] **Step 4: `hand-eval-vectors.md`**

- One section points at `spadeboot/src/test/java/com/spadeboot/domain/game/HandEvaluationTest.java` as the living vectors.
- One section lists the Python suites as the reference for the V1 engine:
  - `lucabzt/Spade@1510db9:server/src/tests/test_analysis/test_poker_hand_analyzer.py` (hand categories)
  - `…/test_poker_winner_analyzer.py` (winner comparison: kickers, splits)
  - with the test-name list produced by `grep -h 'def test' ~/PycharmProjects/Spade/server/src/tests/test_analysis/*.py`.
- Note the counts "as of 2026-10-09 (`grep -c 'def test'`)".
- Note: "The orphan Java `HandEvaluatorTest` (22 methods) had no assertions and was dropped (V0 C1); it is archived in `~/spade-archive/`."

- [ ] **Step 5: `legacy-api.md`**

Three sections:
1. The Python Flask routes (`audit-lineage.md` §B table).
2. The Spring REST and STOMP surface **at HEAD**, re-derived with the grep in the phase rule. Mark the frontend calls that have no backend (`/my-cards`, `/cards/scan`, `/history`, `/ai-hint`, `/statistics/*`, `/tournaments*`, `GET /players`).
3. The spadeAI socket.io contract (link `cv/README.md`).

End with "Card notation today: `AS`/`10H` (cv), `ACEH`-style `Value.name()+suit initial` (backend), `THREES`-style (legacy client)."

- [ ] **Step 5b: `webapp-style.md`, the style the owner liked (D24)**

Extract the legacy webapp's visual language from its CSS. Copy values exactly; don't redesign anything.

```bash
ls webapp/src/styles/
grep -h -o -E -- '--[a-z0-9-]+:\s*[^;]+' webapp/src/styles/*.css | sort -u            # CSS custom properties
grep -h -o -E '#[0-9a-fA-F]{6}\b|#[0-9a-fA-F]{3}\b|rgba?\([^)]*\)|linear-gradient\([^;]*\)' webapp/src/styles/*.css | sort | uniq -c | sort -rn | head -40
grep -h -o -E 'font-family:[^;]+|border-radius:[^;]+|box-shadow:[^;]+' webapp/src/styles/*.css | sort | uniq -c | sort -rn | head -30
```

Write `docs/references/webapp-style.md` with these sections:
- **Origin:** `webapp/src/styles/*.css` at the V0 HEAD sha.
- **Colour tokens:** dark and light themes as tables (token, value, where it is used), including the purple→blue gradient (`#6a11cb → #2575fc`) and the gray scale.
- **Typography.**
- **Radii and shadows.**
- **Component patterns**, one line each with its CSS file: table card, action buttons, the tap-to-reveal hole cards, modals, the header with theme toggle.
- **What to keep vs refine:** the owner's words, quoted. Mark it as input for `/impeccable` in V1, not a design system.

Screenshots of the running legacy app are left to the V1 design phase, because starting it needs certificates and `npm install`.

- [ ] **Step 6: `README.md` (the index)**

```markdown
# References

Assets carried forward from earlier Spade repos. Each row names its origin so it can be re-checked. These are inputs, not specs: an ADR or a phase doc decides what is built.

| Asset | Origin | Used for | Caveats |
|---|---|---|---|
| [poker-chip-tracker.xlsx](poker-chip-tracker.xlsx) | the group's ledger, 2024-10-13 → 2025-05-21 (was `spadeboot/src/main/resources/`) | ledger feature; real P&L history of 20 nights | friends' names and money |
| [legacy-api.md](legacy-api.md) | `lucabzt/Spade@1510db9` Flask, this repo's Spring code, `lucabzt/spadeAI@9e4ec5e` | what the rebuild must keep or drop | the Spring part reflects V0's HEAD |
| [user-stories.md](user-stories.md) | `lucabzt/Spade@1510db9:zDone` | requirements input for the V1 grill | rewritten; the source transcript is not reused |
| [uml/](uml/) | `lucabzt/Spade@1510db9:UML`, `zUML.txt` | the 2025 domain model and layering | <plantuml -checkonly result> |
| [hand-eval-vectors.md](hand-eval-vectors.md) | this repo's tests + `lucabzt/Spade@1510db9` Python suites | engine correctness | |
| [voice-clips.md](voice-clips.md) | `lucabzt/Spade@1510db9:server/assets/sounds/` | the voice-dealer idea | clips not in repo |
| [webapp-style.md](webapp-style.md) | `webapp/src/styles/` (legacy phone app) | the starting point for the new visual style (D24) | extracted values, not a design system |

Not here on purpose:
- the table reference photos in `lucabzt/Spade:server/src/classifier/table/images/` (they include personal photo thumbnails);
- the YOLO model (it lives in `cv/models/`).
```

Fill `<plantuml -checkonly result>` with what Step 1 printed (for example "both parse" or "zuml.puml: syntax error line N").

- [ ] **Step 7: Check and commit**

```bash
python3 scripts/check_doc_links.py      # Expected: ✓
git add docs/references && git commit -m "docs(references): index of what earlier Spade repos already solved — API contracts, stories, UML, vectors, voice manifest, the webapp's style (V0 Phase 04, D9, D24)"
```

### Task 15: ADRs, `INDEX.md`, `docs/README.md`

**Files:** Create `docs/adr/0001-one-repo-spadeboot-cv-frontend.md`, `docs/adr/0002-physical-cards-camera-reads-manual-betting.md`, `docs/adr/0003-issues-are-the-inbox-index-is-the-roadmap.md`, `docs/adr/0004-solo-worktrees-local-merges.md`, `docs/adr/0005-rebuild-dont-repair.md`, `docs/handoffs/INDEX.md`, `docs/README.md`.

**Interfaces:**
- Consumes: spec D1–D10, D18, D21–D25.
- Produces: ADR numbers 0001–0005 (the next free one is 0006); INDEX as the only roadmap.

- [ ] **Step 1: The four ADRs**

`docs/adr/0001-one-repo-spadeboot-cv-frontend.md`:

```markdown
# 1. Spade is one repo: spadeboot, cv and the frontend live together

Date: 2026-10-09
Status: Accepted
Relates to: [V0 spec](../handoffs/V0_2026-10-09_FOUNDATION.md) D4

## Context
Card detection lived in a separate repo (`lucabzt/spadeAI`), and its contract drifted from its only caller: the legacy webapp sends socket.io frames to the backend's port, so they never reach it. A feature that crosses services (a scanned card reaching the engine) needs one commit, one gate and one board.

## Decision
`cv/` (Python, uv) sits next to `spadeboot/` (Spring Boot) and the future frontend. It was imported as a snapshot of `spadeAI@9e4ec5e`, without history. The model weights are in Git LFS. One `scripts/gate.sh` and one CI workflow cover every service.

## Consequences
- A cross-service change lands atomically and is gated once.
- The repo needs git-lfs. CI pulls the model and caches it, because LFS bandwidth is metered.
- Python and Java tooling sit side by side; each service keeps its own build file.
```

`docs/adr/0002-physical-cards-camera-reads-manual-betting.md`:

```markdown
# 2. Real cards are dealt by a person; cameras read them; bets are typed on the phone

Date: 2026-10-09
Status: Accepted
Relates to: [V0 spec](../handoffs/V0_2026-10-09_FOUNDATION.md) D1–D3 · [PRODUCT.md](../../PRODUCT.md)

## Context
Spade exists for our own poker nights (D1). The 2025 code assumed two different products at once: the README promised a camera-driven dealer for a physical table, while the engine shuffled and dealt its own virtual deck. The owner settled it: *"phone camera recognizes your own player cards. table cam reads community cards. chips like raise are entered manually. I want the card detection."*

## Decision
- A person shuffles and deals physical cards. Spade never deals.
- Each player's phone camera reads that player's hole cards. The table camera reads the board.
- Bets, calls, raises and folds are entered by hand on the phone.
- Spade runs the hand, holds the pot and the stacks, decides the showdown, announces the result and records it.
- Card detection is core scope.

## Consequences
- The engine becomes a state machine that takes in card reads, instead of a dealer (the V1 engine issue).
- Every camera read needs a manual correction path, because the night must not stop for Spade (PRODUCT.md).
- Community-card detection was never built, which makes it V1's biggest risk; a spike measures it first.
- Physical chips are not the source of truth (assumption A1, to confirm in the V1 grill).
```

`docs/adr/0003-issues-are-the-inbox-index-is-the-roadmap.md`:

```markdown
# 3. Issues are the inbox; docs/handoffs/INDEX.md is the only roadmap

Date: 2026-10-09
Status: Accepted
Relates to: [V0 spec](../handoffs/V0_2026-10-09_FOUNDATION.md) D10, D13 · adopted from WealthWatcher ADR 0032

## Context
WealthWatcher kept ROADMAP files next to its phase docs. When it measured them, 84% of their content duplicated phase docs, and two of their open checkboxes had already shipped. A list with no state goes stale.

## Decision
- Ideas, bugs and complaints are GitHub issues on the "Spade" board (Inbox → Grilling → Ready → Building → Shipped · WontDo), filed with the `capture-idea` skill.
- `docs/handoffs/INDEX.md` lists versions and phases. It is the only roadmap.
- An issue becomes a phase when an INDEX phase entry links it.
- No `ROADMAP.md`, and no checklist of future work in any doc.

## Consequences
- Every item has a state and a home.
- `scripts/check_board.py --fix` keeps the board honest, because GitHub's "item closed" automation proved unreliable in WealthWatcher.
- The owner's global "we are done" checklist mentions ROADMAP files; in this repo the `ship-phase` skill replaces it.
```

`docs/adr/0004-solo-worktrees-local-merges.md`:

```markdown
# 4. Solo with AI: one worktree per phase, local --no-ff merges, no pull requests

Date: 2026-10-09
Status: Accepted
Relates to: [V0 spec](../handoffs/V0_2026-10-09_FOUNDATION.md) D5, D18

## Context
One person builds Spade, with AI agents, sometimes several sessions at once. Pull requests would add ceremony without a second reviewer. Sessions sharing one working tree overwrite each other's files.

## Decision
- Each phase is built in `.worktrees/phase-NN-slug` on `phase/NN-slug`.
- It merges into `master` locally with `--no-ff` through the `ship-phase` skill, and the gate runs before and after the merge.
- `.claude/hooks/guard-master.sh` blocks code edits on master in the main checkout.
- Phase numbers are global and never reset. V0 is phases 01–05.

## Consequences
- History reads as one merge commit per phase.
- Review happens in the session (the `verify-change` skill and the gate), not on GitHub.
- If a second contributor joins, this ADR is superseded by one that introduces PRs.
```

`docs/adr/0005-rebuild-dont-repair.md`:

```markdown
# 5. Rebuild, don't repair: a new web hub, a native iOS player app, a simpler backend

Date: 2026-10-09
Status: Accepted
Relates to: [V0 spec](../handoffs/V0_2026-10-09_FOUNDATION.md) D21–D25 · [salvage map](../status-quo/salvage.md) · [webapp style](../references/webapp-style.md)

## Context
The 2025 code works in places and is broken in many others (see status-quo). The hub `client/` is a Vision UI template with no style of its own. The phone app `webapp/` needs to be on iOS. The owner: *"take the best parts of it, look at wealth watcher for architecture inspiration as well and just code it fully new where things can't be saved."*

## Decision
- **Salvage first.** The [salvage map](../status-quo/salvage.md) names what is kept, adapted, kept only as a concept, rewritten or dropped. Nothing is patched in place for its own sake.
- **Backend:** rebuilt simpler where it can't be saved, borrowing WealthWatcher's layering (thin routes → services → repositories, a pure domain, explicit API schemas).
- **Hub:** a new web dashboard. The old pages are the concept; all of the code and libraries are new.
- **Player app:** native iOS in Swift. It replaces `webapp/`.
- **Look:** the legacy webapp's style and vibe, refined into a design system with the design skills. A new style; not WealthWatcher's.
- **Not decided here:** stacks, libraries and project names. They come from V1's research and grill.

## Consequences
- `client/` and `webapp/` stay frozen until their replacements ship, then they are deleted.
- iOS needs Xcode and macOS CI runners, which are slower and pricier than Linux. The gate grows an iOS block.
- On-device hole-card detection (the YOLO model exported to Core ML) becomes possible: hole-card images would never leave the phone.
- The V1 grill needs five pieces of evidence before it can decide anything (see INDEX).
```

- [ ] **Step 2: `docs/handoffs/INDEX.md`**

Fill the `✅` dates and merge SHAs from `git log --merges --format='%h %ad %s' --date=short master`.

```markdown
# Handoff Index

The only roadmap ([ADR 0003](../adr/0003-issues-are-the-inbox-index-is-the-roadmap.md)). Ideas live as issues on the "Spade" board; a version appears here once it is scoped. Versions newest first. Linking an issue inside a phase entry promotes it to `type:phase`.

## 🔲 Version 1 — queued · name chosen in its grill

> **Rebuild, don't repair** ([ADR 0005](../adr/0005-rebuild-dont-repair.md)): a new web hub, a native iOS player app, a simpler backend, all built from the [salvage map](../status-quo/salvage.md).
> **Evidence before the grill:**
> 1. the card-detection spike (can the table camera read the board? how reliably do phones read hole cards?)
> 2. the salvage map ✅ (V0)
> 3. an audit of WealthWatcher's backend and frontend architecture
> 4. iOS stack research (SwiftUI, camera, Core ML on-device detection, a realtime client)
> 5. web-hub stack research, plus a design direction from the [webapp's style](../references/webapp-style.md)
>
> Then `/grill-with-docs`. No phase numbers or docs yet; its phases start at 06.

## 🔲 Version 0 — building · **Foundation**

> **The version is one sentence:** Spade gets a working process before it gets new code.
> Spec: [V0_2026-10-09_FOUNDATION.md](V0_2026-10-09_FOUNDATION.md) · Plan: [V0_2026-10-09_FOUNDATION_TASKS.md](V0_2026-10-09_FOUNDATION_TASKS.md) · Evidence: [V0_audits/](V0_audits/)

- **01 — Lockdown** — ✅ <date> — secrets rotated and out of the repo, history rewritten, a fresh clone boots
- **02 — Shape** — ✅ merged <date> (`<sha>`) — `cv/` imported, model in LFS, the ledger moved to references
- **03 — Gate** — ✅ merged <date> (`<sha>`) — `scripts/gate.sh` and CI green; evaluator fixed; guard hook
- **04 — Docs** — 🔲 building — status quo, references, ADRs, PRODUCT, CONTEXT, CLAUDE.md
- **05 — Tracker** — 🔲 — labels, board, skills, seeded backlog
```

- [ ] **Step 3: `docs/README.md`**

```markdown
# docs/

| Path | What | Read it when |
|---|---|---|
| [../PRODUCT.md](../PRODUCT.md) | who Spade is for, how a night works, principles | deciding what to build |
| [../CONTEXT.md](../CONTEXT.md) | the glossary | naming anything |
| [../CLAUDE.md](../CLAUDE.md) | rules for agents | every session (loaded automatically) |
| [../USAGE.md](../USAGE.md) | commands | running anything |
| [handoffs/INDEX.md](handoffs/INDEX.md) | the roadmap: versions and phases | planning |
| `handoffs/PHASE_NN_*.md` | one doc per phase: scope, decisions, what shipped | building that phase |
| [handoffs/done/](handoffs/done/) | shipped phases, by version | archaeology |
| [adr/](adr/) | decisions and why | before contradicting one |
| [status-quo/](status-quo/README.md) | how Spade looked on 2026-10-09 | touching an old area |
| [references/](references/README.md) | assets from earlier Spade repos | before building something that may already exist |
```

(`handoffs/done/` doesn't exist until Task 22. Leave that row's link text in backticks, `` `handoffs/done/` ``, until then; Task 22 turns it into a link.)

- [ ] **Step 4: Check and commit**

```bash
python3 scripts/check_doc_links.py      # Expected: ✓
git add docs/adr docs/handoffs/INDEX.md docs/README.md
git commit -m "docs(adr): ADRs 0001–0005, INDEX as the only roadmap, and a map of docs/ (V0 Phase 04, D10, D18, D21–D24)"
```

### Task 16: `PRODUCT.md` and `CONTEXT.md`

**Files:** Create `PRODUCT.md`, `CONTEXT.md`.

**Interfaces:**
- Consumes: spec D1–D3, A1–A3.
- Produces: the principles that issues and ADRs cite by name, and the glossary terms.

- [ ] **Step 1: `PRODUCT.md`**

```markdown
# Spade: product

## One sentence
Spade is the dealer's brain for **our own poker nights**: real cards on a real table, while Spade reads them, runs the hand, calls the winner and keeps the books.

## Who it is for
One friend group, physically at one table, playing Texas Hold'em together. Success is simple: **we use it every poker night** ([ADR 0002](docs/adr/0002-physical-cards-camera-reads-manual-betting.md)). That is not a product for strangers, and not a demo.

## How a night works
1. Everyone joins the table in the Spade iPhone app, with a buy-in from their bankroll.
2. A person shuffles and deals real cards.
3. Each player scans their two hole cards with the app. Only they see them.
4. The table camera reads the flop, turn and river.
5. Players tap fold / check / call / raise; Spade tracks the pot and every stack.
6. At showdown Spade knows every hand: it decides the winner (side pots included), announces it, pays out and records the result.
7. The big screen shows the table: board, pot, whose turn it is, the announcements.
8. At the end, cash-outs land in the ledger. No more spreadsheet.

## Principles
These are proposals until the V1 grill confirms them (V0 spec A2).
- **The night never stops for Spade.** Every camera read can be corrected by hand in seconds. A misread is an inconvenience, never a dead end.
- **Hole cards stay private until showdown.** No screen and no API response shows another player's cards earlier.
- **Reliability beats magic.** A plain feature that always works beats a clever one that works most nights.
- **Setup in minutes.** From "cards are out" to the first hand in under five minutes, with no laptop fiddling.
- **The iPhone app is the only device per player** ([ADR 0005](docs/adr/0005-rebuild-dont-repair.md)). Install it once, then no account juggling at the table.

## What Spade is not
- Not online poker: everyone is at the same table.
- Not a real-money gambling platform: stacks are the group's own bookkeeping.
- Not a public product: one group, our nights.

## Open questions (V1 grill)
- Are physical chips still on the table, or are Spade's stacks the only truth? (A1)
- Where does Spade run: a laptop at the table, or `hub.poker-spade.de`? (A3)
- Which extras earn their place: voice dealer, ledger history, Spotify, cheatsheet, win probability?
- Does everyone at the table have an iPhone? If not, what does a non-iPhone player use?
```

- [ ] **Step 2: `CONTEXT.md`**

```markdown
# Glossary

How Spade's words are used in code, docs and issues. Format: **Term**: definition. _Avoid_: words that cause confusion.

- **Poker night**: one evening of play by the group, from the first buy-in to the last cash-out. The unit the ledger records. _Avoid_: session (the code's `GameSession` is a different thing).
- **Table**: one physical table and its Spade state: seats, the current hand, the pot. _Avoid_: room, lobby.
- **Seat**: a player's place at the table, holding their stack. _Avoid_: slot.
- **Hand**: one deal, from the blinds to the showdown or the last fold. _Avoid_: round (the code's `Round` means a hand; new code says hand).
- **Street**: one betting round inside a hand: pre-flop, flop, turn, river. _Avoid_: stage (the code's `Stage`).
- **Hole cards**: a player's two private cards. _Avoid_: player cards, hand cards.
- **Board**: the up to five shared cards in the middle. _Avoid_: community cards in UI copy (fine in code), table cards.
- **Scan**: one camera read of cards: a phone reading hole cards, or the table camera reading the board. _Avoid_: detection (that is the model's output), recognition.
- **Correction**: a person overriding a scan by hand. Always possible (PRODUCT.md). _Avoid_: manual mode.
- **Stack**: the chips a player has at the table right now. _Avoid_: chips (ambiguous with physical chips), balance.
- **Bankroll**: a player's money outside any table; buy-ins come from it, cash-outs go back to it. _Avoid_: balance (the code's `User.balance`), wallet.
- **Buy-in**: moving money from bankroll to a new stack when sitting down.
- **Cash-out**: moving a stack back to the bankroll when leaving.
- **Ledger**: the record of buy-ins, cash-outs and results per poker night; it replaces the spreadsheet.
- **Pot**: the chips bet in the current hand. **Side pot**: a pot only some players can win, created when someone is all-in for less.
- **Showdown**: the end of a hand where the remaining hole cards are compared and the pot is paid out.
- **Hub**: the web dashboard on the big shared screen: the table, plus the night's pages (ledger, cheatsheet, music). _Avoid_: TV app, client (the legacy `client/`).
- **Player app**: the native iPhone app each player uses at the table: join, scan, bet. _Avoid_: webapp (the legacy `webapp/` it replaces), phone app.
```

- [ ] **Step 3: Check and commit**

```bash
python3 scripts/check_doc_links.py      # Expected: ✓
git add PRODUCT.md CONTEXT.md
git commit -m "docs: PRODUCT.md (how a night works, principles) and the CONTEXT.md glossary (V0 Phase 04, D1–D3)"
```

### Task 17: `CLAUDE.md`, `AGENTS.md`, `USAGE.md`, `README.md`, doc budget

**Files:** Create `CLAUDE.md`, `AGENTS.md`, `USAGE.md`, `scripts/doc_budget.py`, `scripts/doc-budget.json`. Overwrite `README.md`. Modify `scripts/gate.sh` (one line).

**Interfaces:**
- Consumes: every command from Tasks 2–12.
- Produces: `python3 scripts/doc_budget.py check|update` (tracks `CLAUDE.md`), plus its line in the gate's docs block.

- [ ] **Step 1: `CLAUDE.md`**

````markdown
# Spade: rules for agents

Spade is an AI poker dealer for **our own poker nights**: real cards on the table, each phone reads its owner's hole cards, a table camera reads the board, bets are tapped in, and Spade runs the hand, decides the showdown and keeps the ledger. Goals and principles: [PRODUCT.md](PRODUCT.md). Words: [CONTEXT.md](CONTEXT.md). Every doc: [docs/README.md](docs/README.md). Commands: [USAGE.md](USAGE.md).

## Before you write code: does it already exist?
1. **Issues and the roadmap.** `gh issue list --repo eixfachZabii/Spade --state all --search "<words>"` and [docs/handoffs/INDEX.md](docs/handoffs/INDEX.md).
2. **The status quo.** [docs/status-quo/](docs/status-quo/README.md) is the dated before-picture: what works, what is partial, what is dead. Read the file for your area first.
3. **The references.** [docs/references/](docs/references/README.md) holds what earlier Spade repos already solved: legacy API contracts, user stories, hand-evaluation vectors, the ledger, the voice-clip manifest.
4. **The decisions.** [docs/adr/](docs/adr/). Propose a new ADR rather than silently contradicting one.

## Where the work lives
| What | Where |
|---|---|
| Ideas, bugs, the backlog | GitHub issues on the **Spade** board, filed with the `capture-idea` skill |
| Scoped work | [docs/handoffs/INDEX.md](docs/handoffs/INDEX.md), then one `PHASE_NN_YYYY-MM-DD_SLUG.md` per phase |
| Shipped work | `docs/handoffs/done/VersionN.0/` |
| Why it is this way | [docs/adr/](docs/adr/) |

## The repo
| Path | What | State |
|---|---|---|
| `spadeboot/` | Spring Boot 3 / Java 17: auth, lobby, tables, Hold'em engine, cheatsheet, Spotify | in use; cleanup and engine rebuild in V1 |
| `cv/` | Python card detection (YOLOv8, socket.io), from `lucabzt/spadeAI` | in use; community cards are a stub |
| `client/`, `webapp/` | legacy React apps (hub, phone) | **frozen**: never edit; replaced in V1 by a new web hub and a native iOS player app ([ADR 0005](docs/adr/0005-rebuild-dont-repair.md)) |
| `scripts/` | the gate and its checks | |
| `docs/` | everything else | |

## Working in this repo: always in a worktree
Code edits on `master` in the main checkout are blocked by `.claude/hooks/guard-master.sh`; docs pass. One worktree per phase or fix:
```bash
git worktree add .worktrees/phase-NN-slug -b phase/NN-slug && cd .worktrees/phase-NN-slug
```
Merge back with the `ship-phase` skill. Deliberate one-liner on master: `SPADE_ALLOW_MASTER_EDIT=1`.

## The gate
```bash
scripts/gate.sh            # backend + cv + docs, then what it did NOT prove
scripts/gate.sh --backend  # or --cv, --docs
```
Green means:
- `./mvnw verify` passed;
- cv's ruff and pytest passed;
- every relative doc link resolves;
- CLAUDE.md is within its byte budget;
- no unpushed commit uses a negated close keyword.

**Read the "not proven" block every time.** The legacy apps, card-reading accuracy, real phones and cameras, and Docker are never covered. CI (`.github/workflows/gate.yml`) runs the same blocks on every push. Before claiming anything works: the `verify-change` skill.

## Secrets
They live only in the untracked `spadeboot/.env` (template `spadeboot/.env.example`), which `spadeboot/run.sh` loads. A missing `SPADE_JWT_SECRET` stops the backend at startup, on purpose.

## Conventions
- **Commits:** `type(scope): outcome (Phase NN, D#, #issue)`.
  - Types: feat · fix · docs · test · refactor · chore.
  - Scopes: backend · cv · docs · devex · legacy · security.
- **Branches:** `phase/NN-slug`, `fix/<issue>-slug`.
- **Decisions:** a phase doc numbers them D1…; commits cite them; lasting ones become ADRs.
- **Card notation:** three formats exist (cv `AS`/`10H`, backend `ACEH`-style, legacy client `THREES`-style). Don't add a fourth; see the `area:cv` notation issue.
- **Phase docs:** status line, "What shipped against what was scoped", "Not proven by this phase", "What shipping it actually found".

## Do not
- **Don't hand-type counts** (endpoints, tests, files) into any always-loaded doc. They go stale within a phase. Say how to derive them.
- **Don't create a `ROADMAP.md`**, or a checklist of future work in any doc. Future work is issues ([ADR 0003](docs/adr/0003-issues-are-the-inbox-index-is-the-roadmap.md)).
- **Don't put a secret** in a tracked file, commit message, issue or log line. Compare secrets by hash; never print them.
- **Don't edit `client/` or `webapp/`.**
- **Don't use `git stash`.** It is shared by every worktree.
- **Don't write a negated close keyword:** "not fixing #12" still closes #12. Write "Deferred: #12".
- **Don't save screenshots outside `.playwright/screenshots/`.**
- **Don't trust a plan over the code.** Plans go stale mid-phase; verify every claim against the code.
````

- [ ] **Step 2: `AGENTS.md`**

```markdown
# AGENTS.md

The instructions for agents live in [CLAUDE.md](CLAUDE.md). Read that; this file is only a pointer, so there is one source.
```

- [ ] **Step 3: `USAGE.md`**

````markdown
# Usage

Commands only. Rules and reasons: [CLAUDE.md](CLAUDE.md).

## Backend (`spadeboot/`)
```bash
cp spadeboot/.env.example spadeboot/.env      # once; fill it in (openssl rand -base64 48 for SPADE_JWT_SECRET)
spadeboot/run.sh                              # dev profile: in-memory H2, seed users if SPADE_SEED_PASSWORD is set
(cd spadeboot && ./mvnw -q verify)            # tests
(cd spadeboot && docker compose up --build)   # Docker: known broken, see the Docker issue
```

## Card detection (`cv/`)
```bash
git lfs install && git lfs pull               # once per clone; the model is in LFS
(cd cv && uv sync)                            # first run downloads torch
(cd cv && uv run python app.py)               # socket.io on :5001; opens camera 0
(cd cv && uv run pytest -q)
```

## Legacy apps (frozen; reference only)
Their dev certificates are untracked. Make them once:
```bash
for app in client webapp; do openssl req -x509 -newkey rsa:2048 -nodes -days 365 -subj "/CN=localhost" -keyout $app/key.pem -out $app/cert.pem; done
(cd client && npm install && ./run.sh)        # hub, https://localhost:3000
(cd webapp && npm install && ./run.sh)        # phone app; reachable on the LAN
```

## Gate and CI
```bash
scripts/gate.sh [--backend|--cv|--docs]
gh run list --repo eixfachZabii/Spade --limit 5
```

## Backlog and board
```bash
gh issue list --repo eixfachZabii/Spade --label needs-grill
python3 scripts/check_board.py [--fix]
```

## Skills
| When | Skill |
|---|---|
| an idea, a bug, a "wouldn't it be nice" | `capture-idea` |
| before saying something works | `verify-change` |
| "we are done", "ship it", "push" | `ship-phase` |
| before building a version or a big phase | `/grill-with-docs` |
| turning a grilled design into tasks | `superpowers:writing-plans` |
````

(The board and skill rows describe what Tasks 19 and 20 create. They land one phase later, so for the length of one phase USAGE.md describes them early. The link checker ignores code spans.)

- [ ] **Step 4: `README.md`** (overwrite)

````markdown
# Spade: the AI poker dealer for our poker nights

![Spade](client/src/assets/images/spade-logo/spade_logo_rectangle.png)

Real cards on a real table. Each player's phone reads their own hole cards, a table camera reads the board, bets are tapped in, and Spade runs the hand, calls the winner and keeps the books. How a night works and what Spade is (and isn't): [PRODUCT.md](PRODUCT.md).

**Status (October 2026):** being rebuilt. Version 0 sets up the working process. Version 1 rebuilds Spade around card detection: a simpler backend, a new web hub and a native iOS player app. Roadmap: [docs/handoffs/INDEX.md](docs/handoffs/INDEX.md).

| Part | What |
|---|---|
| [`spadeboot/`](spadeboot/) | Spring Boot backend |
| [`cv/`](cv/README.md) | card detection (Python, YOLOv8) |
| `client/`, `webapp/` | the 2025 React apps (frozen, being replaced) |

Run it: [USAGE.md](USAGE.md). Docs map: [docs/README.md](docs/README.md).

Built by Sebastian Rogg, with Luca Bozzetti and Markus on the 2024–25 versions ([lineage](docs/status-quo/lineage.md)).
````

- [ ] **Step 5: Doc budget**

```bash
WW="/Users/sebastianrogg/PycharmProjects/Hackathons & Projekte/WealthWatchter/scripts"
cp "$WW/doc_budget.py" scripts/ && chmod +x scripts/doc_budget.py
python3 scripts/doc_budget.py update     # Expected: "CLAUDE.md: N bytes (new)" and "Wrote scripts/doc-budget.json"
python3 scripts/doc_budget.py check      # Expected: exit 0
```
In `scripts/doc_budget.py`'s docstring, add the line `Copied from WealthWatcher on 2026-10-09; its incident notes are WealthWatcher's.`
Expected size: CLAUDE.md about 6–8 KB. If it is over 10 KB, cut it before recording the budget.

In `scripts/gate.sh`, in the docs block, insert after the "relative links resolve" line:

```bash
  run "CLAUDE.md within its byte budget" python3 scripts/doc_budget.py check
```

- [ ] **Step 6: Gate, commit, merge Phase 04**

```bash
scripts/gate.sh                          # Expected: GATE GREEN
git add CLAUDE.md AGENTS.md USAGE.md README.md scripts/doc_budget.py scripts/doc-budget.json scripts/gate.sh
git commit -m "docs: CLAUDE.md rulebook (byte-budgeted), AGENTS pointer, USAGE commands, a new README (V0 Phase 04, D9, C6)"
```
Run the Phase merge procedure. **Phase 04 done when** links are ✓, the budget is set, and every status-quo claim cites a path or an audit section.

---

# Phase 05 · Tracker (worktree `phase/05-tracker`)

```bash
git worktree add .worktrees/phase-05-tracker -b phase/05-tracker && cd .worktrees/phase-05-tracker
```

### Task 18: Labels

**Files:** none (GitHub state).

**Interfaces:**
- Produces labels: `area:{backend,cv,frontend,devex,docs,security}`, `type:{bug,idea,phase,chore}`, `version:{v0,v1,later}`, `needs-grill`, `already-shipped`.

- [ ] **Step 1: Delete the 9 defaults** (announce first: "replacing GitHub's default labels")

```bash
R=eixfachZabii/Spade
for l in bug documentation duplicate enhancement "good first issue" "help wanted" invalid question wontfix; do
  gh label delete "$l" --repo $R --yes; done
```

- [ ] **Step 2: Create the scheme**

```bash
R=eixfachZabii/Spade
mk() { gh label create "$1" --repo $R --color "$2" --description "$3"; }
mk area:backend  1D76DB "spadeboot/ (Spring Boot)"
mk area:cv       5319E7 "cv/ (card detection)"
mk area:frontend 0E8A16 "the app players and the hub use"
mk area:devex    BFD4F2 "gate, CI, tooling, Docker"
mk area:docs     C5DEF5 "docs, ADRs, references"
mk area:security B60205 "secrets, auth, privacy"
mk type:bug      D73A4A "Something is wrong"
mk type:idea     FBCA04 "Captured, not yet scoped"
mk type:phase    0052CC "Scoped: linked from a phase entry in INDEX.md"
mk type:chore    EDEDED "Maintenance with no behaviour change"
mk version:v0    E4E669 "Version 0 · Foundation"
mk version:v1    F9D0C4 "Version 1 (named in its grill)"
mk version:later D4C5F9 "Not scheduled"
mk needs-grill   C2E0C6 "Must go through /grill-with-docs before building"
mk already-shipped 006B75 "Filed for the record; it already exists"
gh label list --repo $R --limit 50 | wc -l     # Expected: 15
```

### Task 19: The board and `scripts/check_board.py`

**Files:** Create `scripts/check_board.py`, `scripts/tests/test_check_board.py`. Modify `scripts/gate.sh`.

**Interfaces:**
- Consumes: `gh` JSON shapes, measured 2026-10-09:
  - `gh project item-list` → `.items[] = {id, status, content: {type, number, repository, url}}`
  - `gh project field-list` → `.fields[] = {id, name, type, options[]: {id, name}}`
  - `gh issue list --json number,url,state,stateReason` → `state` is `OPEN`/`CLOSED`; `stateReason` is `COMPLETED`/`NOT_PLANNED`/`DUPLICATE`/`REOPENED`/`""`.
- Produces: `plan(issues, items) -> list[Action]` (pure) and the CLI `python3 scripts/check_board.py [--fix]` (exit 1 when something is off and `--fix` wasn't given).

- [ ] **Step 1: Create the project** (announce first)

```bash
gh project create --owner eixfachZabii --title Spade --format json | jq '{number, id, url}'
N=<number from above>
gh project link $N --owner eixfachZabii --repo eixfachZabii/Spade
FIELD=$(gh project field-list $N --owner eixfachZabii --format json | jq -r '.fields[] | select(.name=="Status") | .id')
gh api graphql -f field="$FIELD" -f query='
mutation($field: ID!) {
  updateProjectV2Field(input: {fieldId: $field, singleSelectOptions: [
    {name: "Inbox",    color: GRAY,   description: "Captured, not triaged"},
    {name: "Grilling", color: PURPLE, description: "Being grilled with /grill-with-docs"},
    {name: "Ready",    color: BLUE,   description: "Scoped: linked from a phase entry in INDEX.md"},
    {name: "Building", color: YELLOW, description: "In a worktree now"},
    {name: "Shipped",  color: GREEN,  description: "Closed as completed"},
    {name: "WontDo",   color: RED,    description: "Closed as not planned"}
  ]}) { projectV2Field { ... on ProjectV2SingleSelectField { options { name } } } }
}'
```
Expected: the mutation echoes the six option names. If the API rejects `updateProjectV2Field` with `singleSelectOptions`, create a field named `Status` from the web UI instead, with the same six options, and continue.

- [ ] **Step 2: The owner switches on the two settings with no API**

In the project's web UI (`gh project view $N --owner eixfachZabii --web`):
- ⋯ → **Workflows** → **Auto-add to project** → filter `is:issue`, repository `Spade` → enable.
- The default view: **Layout → Board**, grouped by **Status**.

- [ ] **Step 3: Write the failing tests**

Create `scripts/tests/test_check_board.py`:

```python
"""Tests for the pure planning half of scripts/check_board.py."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from check_board import INBOX, SHIPPED, WONTDO, Action, Issue, Item, plan  # noqa: E402

URL = "https://github.com/eixfachZabii/Spade/issues/{}"


def issue(n, open_=True, reason=None):
    return Issue(number=n, url=URL.format(n), open=open_, state_reason=reason)


def test_open_issue_missing_from_board_is_added_to_inbox():
    assert plan([issue(1)], []) == [Action("add", 1, INBOX, url=URL.format(1))]


def test_open_issue_on_board_without_status_goes_to_inbox():
    assert plan([issue(2)], [Item("I2", 2, None)]) == [Action("set", 2, INBOX, item_id="I2")]


def test_open_issue_in_a_working_column_is_left_alone():
    assert plan([issue(3)], [Item("I3", 3, "Ready")]) == []


def test_reopened_issue_sitting_in_shipped_goes_back_to_inbox():
    assert plan([issue(4, reason="REOPENED")], [Item("I4", 4, SHIPPED)]) == [Action("set", 4, INBOX, item_id="I4")]


def test_closed_as_completed_goes_to_shipped():
    assert plan([issue(5, open_=False, reason="COMPLETED")], [Item("I5", 5, "Building")]) == [
        Action("set", 5, SHIPPED, item_id="I5")
    ]


def test_closed_as_not_planned_or_duplicate_goes_to_wontdo():
    actions = plan(
        [issue(6, open_=False, reason="NOT_PLANNED"), issue(7, open_=False, reason="DUPLICATE")],
        [Item("I6", 6, "Inbox"), Item("I7", 7, "Inbox")],
    )
    assert actions == [Action("set", 6, WONTDO, item_id="I6"), Action("set", 7, WONTDO, item_id="I7")]


def test_closed_issue_missing_from_board_is_added_in_its_final_column():
    assert plan([issue(8, open_=False, reason="COMPLETED")], []) == [Action("add", 8, SHIPPED, url=URL.format(8))]


def test_closed_issue_already_in_the_right_column_needs_nothing():
    assert plan([issue(9, open_=False, reason="COMPLETED")], [Item("I9", 9, SHIPPED)]) == []


def test_empty_state_reason_on_a_closed_issue_counts_as_completed():
    assert plan([issue(10, open_=False, reason="")], [Item("I10", 10, "Ready")]) == [
        Action("set", 10, SHIPPED, item_id="I10")
    ]
```

Run: `uvx --quiet pytest -q scripts/tests`
Expected: `ModuleNotFoundError: No module named 'check_board'`.

- [ ] **Step 4: Write `scripts/check_board.py`**

```python
#!/usr/bin/env python3
"""Every issue is on the "Spade" board, in the column its state says it belongs in.

GitHub's built-in "auto-add" workflow puts new issues on the board. Moving closed
issues is done here instead: WealthWatcher measured GitHub's "item closed" automation
firing zero times in eight closes (ADR 0003).

Rules:
  open issue not on the board                -> add it, Status = Inbox
  open issue with no Status, or in Shipped / WontDo (reopened) -> Inbox
  open issue in Grilling / Ready / Building  -> left alone (a person put it there)
  closed as completed (or no reason)         -> Shipped
  closed as not planned / duplicate          -> WontDo

    python3 scripts/check_board.py          # report; exit 1 if anything is off
    python3 scripts/check_board.py --fix    # repair it
"""
from __future__ import annotations

import json
import subprocess
import sys
from dataclasses import dataclass

OWNER = "eixfachZabii"
REPO = "eixfachZabii/Spade"
PROJECT_TITLE = "Spade"
INBOX, SHIPPED, WONTDO = "Inbox", "Shipped", "WontDo"
NOT_DONE = {"NOT_PLANNED", "DUPLICATE"}


@dataclass(frozen=True)
class Issue:
    number: int
    url: str
    open: bool
    state_reason: str | None


@dataclass(frozen=True)
class Item:
    item_id: str
    number: int
    status: str | None


@dataclass(frozen=True)
class Action:
    kind: str  # "add" or "set"
    number: int
    status: str
    item_id: str | None = None
    url: str | None = None


def desired_status(issue: Issue, current: str | None) -> str | None:
    """The column the issue must be in, or None when any working column is fine."""
    if not issue.open:
        return WONTDO if (issue.state_reason or "").upper() in NOT_DONE else SHIPPED
    if current in (None, SHIPPED, WONTDO):
        return INBOX
    return None


def plan(issues: list[Issue], items: list[Item]) -> list[Action]:
    on_board = {item.number: item for item in items}
    actions: list[Action] = []
    for issue in sorted(issues, key=lambda i: i.number):
        item = on_board.get(issue.number)
        target = desired_status(issue, item.status if item else None)
        if item is None:
            actions.append(Action("add", issue.number, target or INBOX, url=issue.url))
        elif target is not None and target != item.status:
            actions.append(Action("set", issue.number, target, item_id=item.item_id))
    return actions


# ---------------------------------------------------------------- GitHub I/O


def gh(*args: str) -> str:
    return subprocess.run(["gh", *args], capture_output=True, text=True, check=True).stdout


def load_issues() -> list[Issue]:
    raw = json.loads(gh("issue", "list", "--repo", REPO, "--state", "all", "--limit", "1000",
                        "--json", "number,url,state,stateReason"))
    return [Issue(r["number"], r["url"], r["state"] == "OPEN", r.get("stateReason") or None) for r in raw]


def find_project() -> dict:
    for project in json.loads(gh("project", "list", "--owner", OWNER, "--format", "json"))["projects"]:
        if project["title"] == PROJECT_TITLE:
            return project
    raise SystemExit(f"check_board: no project titled {PROJECT_TITLE!r} for {OWNER}")


def load_items(number: int) -> list[Item]:
    raw = json.loads(gh("project", "item-list", str(number), "--owner", OWNER, "--format", "json", "--limit", "1000"))
    items = []
    for it in raw["items"]:
        content = it.get("content") or {}
        if content.get("type") == "Issue" and content.get("repository") == REPO:
            items.append(Item(it["id"], content["number"], it.get("status") or None))
    return items


def status_field(number: int) -> tuple[str, dict[str, str]]:
    for field in json.loads(gh("project", "field-list", str(number), "--owner", OWNER, "--format", "json"))["fields"]:
        if field["name"] == "Status":
            return field["id"], {o["name"]: o["id"] for o in field.get("options", [])}
    raise SystemExit("check_board: the project has no Status field")


def apply(actions: list[Action], project: dict, field_id: str, options: dict[str, str]) -> None:
    for action in actions:
        item_id = action.item_id
        if action.kind == "add":
            added = json.loads(gh("project", "item-add", str(project["number"]), "--owner", OWNER,
                                  "--url", action.url, "--format", "json"))
            item_id = added["id"]
        gh("project", "item-edit", "--id", item_id, "--project-id", project["id"],
           "--field-id", field_id, "--single-select-option-id", options[action.status])


def main() -> int:
    fix = "--fix" in sys.argv
    project = find_project()
    actions = plan(load_issues(), load_items(project["number"]))
    if not actions:
        print("  ✓ every issue is on the board in the right column")
        return 0
    for a in actions:
        print(f"  {'fixing' if fix else '✗'} #{a.number}: {a.kind} → {a.status}")
    if not fix:
        print("\n  run: python3 scripts/check_board.py --fix")
        return 1
    field_id, options = status_field(project["number"])
    missing = {a.status for a in actions} - options.keys()
    if missing:
        raise SystemExit(f"check_board: Status field lacks options {sorted(missing)}")
    apply(actions, project, field_id, options)
    print(f"  ✓ fixed {len(actions)} item(s)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
```

`chmod +x scripts/check_board.py`

- [ ] **Step 5: GREEN, then against the real board**

```bash
uvx --quiet pytest -q scripts/tests          # Expected: 9 passed
python3 scripts/check_board.py; echo "exit=$?"   # Expected: "✓ every issue is on the board…", exit=0 (there are no issues yet)
```

- [ ] **Step 6: Add the gate lines**

In `scripts/gate.sh`, docs block, append after the commit-refs line:

```bash
  run "scripts: pytest" uvx --quiet pytest -q scripts/tests
```

In the "not proven" block, after the Docker line:

```bash
printf '  %s· filed or closed an issue? the board does not know: python3 scripts/check_board.py --fix%s\n' "$D" "$N"
```

In `.github/workflows/gate.yml`, docs job, add after the links step:

```yaml
      - uses: astral-sh/setup-uv@v6
      - name: scripts pytest
        run: uvx pytest -q scripts/tests
```

Run: `scripts/gate.sh` → Expected: GATE GREEN.

- [ ] **Step 7: Commit**

```bash
git add scripts/check_board.py scripts/tests/test_check_board.py scripts/gate.sh .github/workflows/gate.yml
git commit -m "chore(devex): check_board.py keeps every issue in the right column; gate and CI run the scripts' tests (V0 Phase 05, D13)"
```

### Task 20: The three repo skills

**Files:** Create `.claude/skills/ship-phase/SKILL.md`, `.claude/skills/capture-idea/SKILL.md`, `.claude/skills/verify-change/SKILL.md`.

**Interfaces:**
- Consumes: `scripts/gate.sh`, `check_doc_links.py`, `doc_budget.py`, `check_commit_refs.py`, `check_board.py`.
- Produces: the skill names `ship-phase`, `capture-idea` and `verify-change`, which USAGE.md and CLAUDE.md refer to.

- [ ] **Step 1: `ship-phase`**

````markdown
---
name: ship-phase
description: Use when a Spade phase or fix branch is finished and ready to integrate — "we are done", "push", "ship it", "merge this", or before any merge to master. Runs the gate, truths up the docs against the code, archives the phase docs, tidies the board, merges and pushes. Replaces the owner's global "we are done" checklist in this repo.
---

# Ship a phase

Work left on a branch is not shipped, and a doc that describes the plan instead of the code is a trap for the next session. This is the whole close-out, in order. (The owner's global checklist mentions ROADMAP files; Spade has none. See ADR 0003.)

## 1. Gate, from inside the worktree
```bash
scripts/gate.sh
```
Green is necessary, not sufficient. Read the **not proven** block and act on it:
- **Dirty tree?** The gate proved the worktree, not the branch. Commit or delete, then re-run.
- **Touched the engine, a game DTO, `/api/games/**` or a STOMP topic?** Run the `verify-change` privacy check.
- **Touched `cv/`?** Accuracy isn't measured. Measure it, or say so in the phase doc's "Not proven".

## 2. Truth up the docs against the code
Check each claim against the code at HEAD, never against the phase doc (plans go stale mid-phase):
- `docs/status-quo/` is a dated snapshot: don't rewrite it. If this phase made one of its statements untrue, add a line to that file's **Changed since** box.
- `CLAUDE.md`: new rules, commands, paths; never a count. Then `python3 scripts/doc_budget.py check`. If it grew on purpose, `python3 scripts/doc_budget.py update` and say why in the commit.
- `CONTEXT.md` for new or changed terms. `PRODUCT.md` only if a principle changed, which needs the owner.
- A decision that outlives the phase gets a new ADR: `docs/adr/NNNN-claim-as-title.md`, next free number.
- The phase doc: status `✅ shipped YYYY-MM-DD`, plus "What shipped, against what was scoped", "Not proven by this phase" and "What shipping it actually found".

## 3. Mark it in INDEX
In `docs/handoffs/INDEX.md`, the phase entry becomes `✅ merged YYYY-MM-DD (`<sha>`) — <outcome>`. If it was the version's last phase, the heading becomes `## ✅ Version N — shipped YYYY-MM-DD`. Never create a ROADMAP.md.

## 4. Archive the phase docs, all of them
```bash
mkdir -p docs/handoffs/done/VersionN.0
git mv docs/handoffs/PHASE_NN_* docs/handoffs/done/VersionN.0/
```
Include the tasks/plan doc and any audits folder. Then fix links **in both directions**:
- links *inside* the moved files gain one `../` per level (`../adr/` → `../../../adr/`);
- every link *to* them (INDEX, other phase docs, ADRs, status-quo) gets the new path.
```bash
python3 scripts/check_doc_links.py        # must print ✓
```
At a version's end also write `done/VersionN.0/CLOSEOUT.md`: what it found · what moved that the owner can see · what left the codebase · deferred, with reasons and issue links · process notes worth keeping.

## 5. Commit, merge, push
```bash
git status --short                          # nothing unexpected; never .env, *.pem, gurobi.lic
git add <the files you mean> && git commit -m "docs(handoffs): ship Phase NN — <outcome> (Phase NN)"
python3 scripts/check_commit_refs.py        # no negated close keyword
cd /Users/sebastianrogg/IdeaProjects/SPADE  # the main checkout, on master
git merge --no-ff phase/NN-slug -m "Merge phase/NN-slug — <one-sentence outcome> (Phase NN)"
scripts/gate.sh                             # again, on master
git push origin master                      # tell the owner you are pushing
```

## 6. Tidy the board
- Close every issue the phase completed, now that the merge sha is real:
  `gh issue close N --repo eixfachZabii/Spade --reason completed --comment "Shipped in Version N Phase NN (merge <sha>). <what changed>. Guarded by: <tests>."`
- File what you found and are not fixing, with `capture-idea`.
- `python3 scripts/check_board.py --fix`, then `python3 scripts/check_board.py` prints ✓.

## 7. Leave nothing behind
```bash
git worktree remove .worktrees/phase-NN-slug && git branch -d phase/NN-slug
git push origin --delete phase/NN-slug 2>/dev/null || true
git worktree list && git status --short      # one worktree, clean tree
```

## Never
- **`git stash`**: it is shared by every worktree.
- **Commit a secret**: `git diff --cached --name-only | grep -E '(^|/)\.env$|\.pem$|gurobi'` must print nothing.
- **"not fixing #N"** in a message: it closes #N. Write "Deferred: #N".
- **Leave a closed issue in Building**: run `check_board.py --fix`.
````

- [ ] **Step 2: `capture-idea`**

````markdown
---
name: capture-idea
description: Use when the owner has an idea, feature request, bug, complaint or "wouldn't it be nice if" for Spade — anything that would otherwise be said in chat and lost. Checks whether it already exists, files it as a GitHub issue with the repo context attached, and puts it on the board.
---

# Capture an idea

An idea said in chat has no state; an issue does. WealthWatcher measured the cost: when its chat backlog moved to issues, five of eleven ideas had already shipped. So step 1 is the point.

## 1. Does it already exist?
```bash
gh issue list --repo eixfachZabii/Spade --state all --search "<two or three words>"
grep -n -i "<word>" docs/handoffs/INDEX.md
grep -rn -i "<word>" spadeboot/src cv docs/status-quo docs/references | head
```
- **An open issue covers it**: comment there with the owner's words and the date. No duplicate.
- **Already shipped**: file it anyway, then close it at once with the label `already-shipped` and a comment saying where it lives.
- **Partly there**: file it open, and say which parts exist, with paths.
- **New**: file it open.

## 2. Write it so it can be built later
```markdown
**Owner, <YYYY-MM-DD>:** *"<their words, verbatim>"*
**What exists.** <files, endpoints, docs it builds on — paths in backticks>
**The problem / idea.** <behaviour, with evidence: a command and its output, or file:line>
**Constraints.** <PRODUCT.md principles and ADRs it must respect>
**Open questions.** <what a grill has to settle>
**Not this issue:** <neighbouring issues, by URL>
```
Found by an agent rather than said by the owner? The first line becomes `**Found by:** <doc, audit section or command>`.

## 3. Label it and put it on the board
```bash
body=$(mktemp) && $EDITOR "$body"    # or write the body with the Write tool
gh issue create --repo eixfachZabii/Spade --title "<outcome-shaped title>" \
  --label "area:<backend|cv|frontend|devex|docs|security>,type:<bug|idea|chore>,version:<v1|later>" \
  --body-file "$body"
python3 scripts/check_board.py --fix
```
Add `needs-grill` when the design is open. `type:phase` is set only by promotion (step 4).

## 4. Do not start a second roadmap
Issues are the inbox; `docs/handoffs/INDEX.md` is the roadmap (ADR 0003). An issue becomes a phase when an INDEX phase entry links it. At that point it gets `type:phase`, moves to Ready, and stays open until the phase ships. Never write a "future work" checklist in a doc.
````

- [ ] **Step 3: `verify-change`**

````markdown
---
name: verify-change
description: Use before claiming a Spade change works, is fixed or is complete, and before any commit that touches the engine, a DTO, a WebSocket payload, auth or cv/. Encodes the traps that make a green test lie.
---

# Verify a change

"Tests pass" is not "it works". Run the checks your change touches, then say what you ran and what it printed.

## 1. A passing test may be pinning the bug
For a fix, prove the test can fail:
```bash
git diff -- <fixed file> > /tmp/fix.patch
git checkout -- <fixed file>
(cd spadeboot && ./mvnw -q test -Dtest=<TestClass>)   # expect RED
git apply /tmp/fix.patch
(cd spadeboot && ./mvnw -q test -Dtest=<TestClass>)   # expect GREEN
```
A test that passes both ways guards nothing. (Never `git stash` for this; it is shared by every worktree.)

## 2. The test harness is not the running app
Mocks and H2 are not the app. Start it and hit the real thing:
```bash
spadeboot/run.sh &                                   # then:
curl -sk https://localhost:8080/api/<endpoint> | jq .
(cd cv && uv run python app.py)                      # for cv changes
```

## 3. Privacy: look at what the other player receives
Hole cards are private until showdown (PRODUCT.md). For any change to the engine, a game DTO, `/api/games/**` or a STOMP topic:
1. Log in as two users at one table.
2. Act as one of them.
3. Read the *other's* REST responses and STOMP frames.

If one player can see another's hole cards before showdown, the change is not done.

## 4. A card-detection claim needs a number
"cv works" means: N named photos under the table's real light, X of them read correctly. The gate only proves the model loads.

## 5. Then, and only then
State what you ran and what it printed. "Should work" is not a result.
````

- [ ] **Step 4: Check and commit**

```bash
python3 scripts/check_doc_links.py           # Expected: ✓
git add .claude/skills && git commit -m "docs(devex): ship-phase, capture-idea and verify-change skills for Spade (V0 Phase 05, D12)"
```

### Task 21: The seeded backlog, filed through `capture-idea`

**Files:** none (GitHub state).

**Interfaces:**
- Consumes:
  - the `capture-idea` skill;
  - `docs/status-quo/*` (the "Found by" links use `https://github.com/eixfachZabii/Spade/blob/master/docs/status-quo/<file>.md` once merged; until then, the relative path in backticks);
  - the audits.
- Produces: 16 issues on the board, every one in Inbox.

- [ ] **Step 1: File issue 1 by following `capture-idea` literally, end to end** (the "done when" check for the skill)

Title: **Backend rebuild: a simpler spadeboot, with the engine as a state machine fed by camera reads**. Labels: `area:backend,type:idea,version:v1,needs-grill`.
Body, in the skill's format:
- **Owner, 2026-10-09:** quote D21 from the spec amendment.
- **What exists:** the salvage map's backend table (`docs/status-quo/salvage.md`); `spadeboot/src/main/java/com/spadeboot/session/{GameSession,RoundSession,SessionManager}.java`; `domain/game/HandEvaluation.java` (fixed and pinned in V0).
- **The problem:** the engine deals its own virtual deck on raw threads, and the layers leak (entities inside DTOs, in-memory "entities"). ADR 0002 needs an engine that takes in card reads; ADR 0005 asks for a rebuild, not a repair.
- **Constraints:** PRODUCT.md "the night never stops" (correction path) and "hole cards stay private"; ADR 0002 and 0005; WealthWatcher-style layering.
- **Open questions:** salvage vs rewrite per package; state machine shape; persisting results; how a correction re-enters the flow; Flyway migrations instead of `ddl-auto: update`.
- Then a checklist of every known defect the rebuild must not repeat:
  - [ ] no side pots or odd-chip handling
  - [ ] chip results never saved
  - [ ] turn / winner / board events never sent
  - [ ] minimum raise equals the big blind
  - [ ] the action response is built before the action applies
  - [ ] an exception in the round thread kills the hand silently
  - [ ] a table can be deleted mid-game
  - [ ] thread-per-game concurrency
  - [ ] dead code and unused dependencies (`archiv/`, QueryDSL, Thymeleaf, jsoup, both OAuth2 starters, …)
  - [ ] no DB migrations (`ddl-auto: update`)

Then `python3 scripts/check_board.py` → Expected: ✓, with the issue in Inbox.

- [ ] **Step 2: File the rest the same way** (search first each time, per the skill)

D21 (rebuild, don't repair) means defects in code V1 replaces are filed as **checklists inside the rebuild issues**, not as separate bugs. That is D14's reasoning applied to everything.

| # | Title | Labels |
|---|---|---|
| 2 | Security requirements the rebuilt backend must meet. Checklist: hole cards reach every client before showdown (REST `/status` and the table topic) · `GET /api/players/me` returns the password hash · STOMP CONNECT without a token is accepted and SUBSCRIBE is not authorised · the chip-optimiser endpoint is public (up to 50 solver calls per request) · `/users/{id}` and `/friends/{id}` expose other users' data · Spotify OAuth `state` isn't validated and tokens travel in the redirect URL | area:security, type:bug, version:v1 |
| 3 | Spike: can the table camera read the board, and how well do phones read hole cards? | area:cv, type:idea, version:v1, needs-grill |
| 4 | On-device hole-card detection on iPhone (export the YOLO model to Core ML) | area:cv, type:idea, version:v1, needs-grill |
| 5 | One card notation across cv, backend, hub and iOS | area:cv, type:idea, version:v1, needs-grill |
| 6 | Hub rebuild: a new web dashboard (the old pages as the concept, new code, the webapp's look). Carries the `client/` keep/drop list and gaps: showdown screen, what the hub may show | area:frontend, type:idea, version:v1, needs-grill |
| 7 | iOS player app in Swift, replacing `webapp/`. Carries the webapp's flows and gaps: all-in, configurable blinds, card correction, tap-to-reveal privacy | area:frontend, type:idea, version:v1, needs-grill |
| 8 | Hosting and the local network: a laptop at the table or `hub.poker-spade.de`; transport security between the app, hub, backend and cv (iOS App Transport Security) | area:devex, type:idea, version:v1, needs-grill |
| 9 | Docker: bind/port mismatch, ARM-only Gurobi, MySQL exposed, WLS licence via env | area:devex, type:bug, version:later |
| 10 | Voice dealer: recorded clips or live TTS? | area:frontend, type:idea, version:later, needs-grill |
| 11 | Ledger: replace the spreadsheet and import the 20 nights | area:backend, type:idea, version:later, needs-grill |
| 12 | Win probability on the hub | area:backend, type:idea, version:later, needs-grill |
| 13 | Spotify and lyrics: keep or drop? | area:frontend, type:idea, version:later, needs-grill |
| 14 | Cheatsheet and chip optimiser without Gurobi | area:backend, type:idea, version:later, needs-grill |
| 15 | Friends: build a UI or drop the feature? | area:backend, type:idea, version:later, needs-grill |
| 16 | Hand history and stats | area:backend, type:idea, version:later, needs-grill |

**Rules for every body:**
- Each body uses the skill template.
- **Found by** cites the status-quo section, re-verified against HEAD: Phases 01–03 changed config, evaluator and tests, so don't copy audit line numbers blindly.
- **Never name a secret's location** (the leaks are fixed, and issues are public).
- The "seed users / known admin password" finding is **not** filed: Task 2 fixed it. If anything, file it and close it as `already-shipped`.

- [ ] **Step 3: Board check**

```bash
python3 scripts/check_board.py; echo "exit=$?"                  # Expected: ✓, exit=0
gh project item-list <N> --owner eixfachZabii --format json --limit 100 | jq '[.items[].status] | group_by(.) | map({(.[0]): length}) | add'
# Expected: {"Inbox": 16}
```

- [ ] **Step 4: Ship Phase 05 with the skill it created.** Follow `.claude/skills/ship-phase/SKILL.md` steps 1–7. No issue closes in Phase 05; V0's work isn't tracked in issues.

**Phase 05 done when:**
- every issue is on the board with a status;
- issue 1 went through `capture-idea` end to end;
- Phase 05 was merged by following `ship-phase`.

---

# Closeout

### Task 22: Close Version 0 with its own ritual

**Files:**
- Create: `docs/handoffs/done/Version0.0/CLOSEOUT.md`.
- Move: `docs/handoffs/V0_2026-10-09_FOUNDATION.md`, `V0_2026-10-09_FOUNDATION_TASKS.md` and `V0_audits/` → `docs/handoffs/done/Version0.0/`.
- Modify: `docs/handoffs/INDEX.md`, `docs/README.md`, plus every file that links to the moved docs.

**Interfaces:**
- Consumes: everything above.
- Produces: V0 shipped; INDEX shows `✅ Version 0`; V1 is next.

- [ ] **Step 1: Worktree**

```bash
git worktree add .worktrees/v0-closeout -b phase/v0-closeout && cd .worktrees/v0-closeout
```

- [ ] **Step 2: `CLOSEOUT.md`** (WealthWatcher's closeout shape, kept short)

Sections:
- **One sentence** plus dates (2026-10-09 → merge date) and the base and final SHAs.
- **What it found:** the five audits in two lines each, plus C1–C8.
- **What moved that the owner can see:** before → after table:
  - secrets in repo → env only;
  - history with leaks → clean;
  - no gate → one command plus CI;
  - flushes undetected → detected;
  - 5/10 tests red → all green;
  - no docs → the set;
  - no backlog → 16 issues on a board.
- **What left the codebase:** `info`, the dev/prod profiles, the TLS keys and `.DS_Store` from history, the orphan branch (archived locally).
- **Deferred, with reasons:** each row links its issue number.
- **Process notes worth keeping:** for example, "an audit's test count is not a test: check for assertions" (C1); "measure secrets by hash".

- [ ] **Step 3: INDEX and archive**

- In `docs/handoffs/INDEX.md`: the heading becomes `## ✅ Version 0 — shipped <date> · **Foundation**`; phases 04 and 05 get `✅ merged <date> (`<sha>`)`; the spec, plan and audit links move to `done/Version0.0/…`.
- Then:

```bash
mkdir -p docs/handoffs/done/Version0.0
git mv docs/handoffs/V0_2026-10-09_FOUNDATION.md docs/handoffs/V0_2026-10-09_FOUNDATION_TASKS.md docs/handoffs/V0_audits docs/handoffs/done/Version0.0/
python3 scripts/check_doc_links.py          # Expected: ✗ with a list; fix every one, both directions
```

**Links that will break:**
- **Inside the moved files:** the spec, the plan and the audits link mostly to each other. Those links move together and still resolve; the checker lists any that don't.
- **Pointing at them:** INDEX, `docs/status-quo/*.md` (the "Evidence: audit" lines), the ADRs' "Relates to" lines (`../handoffs/V0_…` becomes `../handoffs/done/Version0.0/V0_…`), and `docs/README.md` (turn the `handoffs/done/` row into a real link).

Re-run until `✓`.

- [ ] **Step 4: Ship it**

Follow `.claude/skills/ship-phase/SKILL.md` steps 1–7. INDEX already has Version 1 queued.

- [ ] **Step 5: Tell the owner** what shipped, the CI run URL, and the next step: *the V1 spike needs your deck, the mat, the overhead camera and the poker-night lighting.*

---

## Self-review (done while writing; kept for the executor)

- **Spec coverage:** every row has a task.
  - §3 D1–D20: D19 → T0; D15 → T1–4; D4 → T5; D9 → T13–17; D10 → T15; D11 → T9–12, 17, 19; D12 → T20; D13 → T18–19; D14 → T21; D20 → T7.
  - §4 layout → T5, 6, 9–12, 17.
  - §5 docs → T13–17.
  - §6 → T7–12, 17, 19, 20.
  - §7 → T18, 19.
  - §8 → T21.
  - §9 → T1, T2.
  - §10 → the phase headers.
  - §11 → INDEX (T15), T22.
  - §12 → nothing (it's the not-done list).
- **Placeholders:** the remaining `<…>` are values that only exist at execution time (dates, merge SHAs, project number N, an issue's own words), each with the command that produces it. The doc tasks (T13, T14) specify sections, sources and checks instead of verbatim prose, on purpose: the content must be verified against code that Phases 01–03 changed.
- **Names consistent across tasks:**
  - `validateSecret`, `seedPassword`, `spade.seed.password`/`SPADE_SEED_PASSWORD`;
  - `getHighestKeyWithCount`;
  - `plan` / `Issue` / `Item` / `Action` / `INBOX` / `SHIPPED` / `WONTDO`;
  - `SPADE_ALLOW_MASTER_EDIT`;
  - `phase/NN-slug` and `.worktrees/phase-NN-slug`.
- **Review Focus coverage:**
  - 1 → T2 steps 2–5 and 20;
  - 2 → T5 `test_model_file_is_real_not_an_lfs_pointer`;
  - 3 → T2 steps 1, 12–16;
  - 4 → T10 step 4 and T12 tests;
  - 5 → T7 rows.
