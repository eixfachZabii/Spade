---
name: verify-change
description: Use before claiming a Spade change works, is fixed or is complete, and before any commit that touches the engine, a DTO, a WebSocket payload, auth or cv/. Encodes the traps that make a green test lie.
---

# Verify a change

"Tests pass" is not "it works". Run the checks your change touches, then say what you ran and what it printed.

## 1. A passing test may be pinning the bug
For a fix, prove the test can fail:
```bash
P="$(mktemp -d)/fix.patch"; git diff -- <fixed file> > "$P"; echo "patch saved: $P"
test -s "$P" && git checkout -- <fixed file>          # only revert once the patch is safely saved
(cd backend && ./mvnw -q test -Dtest=<TestClass>)     # expect RED (Docker must run)
git apply "$P"
(cd backend && ./mvnw -q test -Dtest=<TestClass>)     # expect GREEN
```
A test that passes both ways guards nothing. (Never `git stash` for this; it is shared by every worktree.)

## 2. The test harness is not the running app
Mocks are not the app, and neither is MockMvc: the container's error dispatch, the security firewall and the real PostgreSQL only exist in a running server. Start it on a throwaway database (Docker must run) and hit the real thing:
```bash
S="$(mktemp -d)"                                     # log file; nothing secret is expected in it, but scan it
export JAVA_HOME=$(/usr/libexec/java_home -v 21)     # the shell's default JDK may be newer
(cd backend && nohup ./mvnw -q spring-boot:test-run -Dspring-boot.run.profiles=testworld > "$S/app.log" 2>&1 &)
for i in $(seq 1 40); do curl -sf -m 2 http://localhost:8080/actuator/health >/dev/null && break; sleep 3; done   # first run downloads
curl -s http://localhost:8080/actuator/health        # {"groups":[...],"status":"UP"}
curl -s -H 'content-type: application/json' http://localhost:8080/api/v1/<endpoint> | jq .   # plain http; testworld: players ana…fay, password testworld, invite code testworld
(cd cv && uv run python app.py)                      # for cv changes
```
Look at what you did not ask for too: an error path (a bad token, a request the firewall rejects such as `/api/v1/players;x`) must answer `application/problem+json`, and `grep -ai 'password\|token\|invite' "$S/app.log"` must find nothing but startup notices. Keep a token in a shell variable; never echo it.

Stop what you started, and check:
```bash
pkill -f 'spring-boot:test-run'; sleep 5             # stops Maven and the forked app; the Testcontainers database goes with it
lsof -nP -iTCP:8080 -sTCP:LISTEN                     # must print nothing
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
