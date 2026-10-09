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
(cd spadeboot && ./mvnw -q test -Dtest=<TestClass>)   # expect RED
git apply "$P"
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
Stop what you started (`pkill -f SpadebootApplication`).

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
