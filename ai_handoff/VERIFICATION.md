# MAWAAI — Human Verification Guide

You (Ahmed) are the **gatekeeper**. The downstream AI produces output; you decide whether to commit. This guide tells you exactly what to check for each micro-task and exactly how.

**Golden rule:** if any check below fails, do NOT commit. Either re-prompt the agent with the specific failure, or `git restore .` and try again.

---

## 🛠 One-Time Setup (Before You Run Any MT)

Before starting, verify these once:

```bash
cd ~/path/to/Mawaai
git status                         # should be clean
git pull origin master             # sync
./gradlew clean
./gradlew assembleDebug             # baseline: must PASS
./gradlew test                      # baseline: must PASS
git log --oneline -5                # note current HEAD sha
```

If `assembleDebug` or `test` fails right now, **fix that first** — do not start the backlog on a broken build.

Save the current `git log` commit SHA. If anything goes wrong, you can `git reset --hard <sha>` back here.

---

## 📋 Universal Per-MT Checklist (Apply To Every Single MT)

After the AI produces output and you apply it, run **all of these in order**:

### Step 1 — Build gates (no human judgment needed)
```bash
./gradlew clean
./gradlew assembleDebug           # MUST PASS
./gradlew test                     # MUST PASS
```
- ❌ If either fails → `git restore .` and re-prompt with the error output.
- ✅ If both pass → continue to Step 2.

### Step 2 — Diff sanity (visual review)
```bash
git diff --stat                    # see file count
git diff                            # full diff
```
Check by eye:
- Is every file listed in the diff also in the MT's "Files you may modify" list from `PROMPTS.md`?
- Is there any file you don't recognize from the MT scope? → ❌ reject.

### Step 3 — Secret scan
```bash
git diff | grep -E "AIza[0-9A-Za-z_-]{20,}|hf_[A-Za-z0-9]{30,}|sk-or-v1-[a-f0-9]{40,}|cfut_[A-Za-z0-9]{30,}|vxcLa[A-Za-z0-9]+"
```
- ❌ If this prints any line → reject the diff. The agent leaked a key.
- ✅ If empty → continue.

### Step 4 — Strongly-typed gate
```bash
git diff | grep -E "Map<String,\s*Any>"
```
- ❌ If this prints anything in template/AI/repository/DI code → reject.
- ✅ Empty → continue.

### Step 5 — Output format compliance
Read the agent's chat response. Verify it contains, in order:
- [ ] `## Phase Header` section
- [ ] `## Context Budget` section listing files read
- [ ] `## Diagnostic Summary` section
- [ ] `## Diff / Files` section
- [ ] `## Verification Plan` section
- [ ] `## Risks + Rollback` section
- [ ] `## Commit Message` section in the exact template
- [ ] `## Next Micro-Task` section

If any section is missing → reject. The agent violated the protocol.

### Step 6 — Commit
If Steps 1–5 all passed:
```bash
git add -A
git commit -m "$(cat <<'EOF'
<paste the agent's "## Commit Message" block here, verbatim>
EOF
)"
git push origin master
```

Record the commit SHA in your notes. Move to the next MT.

---

## 🧪 MT-Specific Verification

### MT-013 (OpenRouter fallback wiring)

After Step 6, run these additional checks:

```bash
# Confirm fallback was wired
grep -rn "openRouterClient.inspirationPrompts" app/src/main/java/com/mawaai/love/app/
# → Must print at least 1 line

# Confirm GeminiClient was NOT modified
git diff HEAD~1 -- app/src/main/java/com/mawaai/love/app/design/ai/gemini/GeminiClient.kt
# → Must be empty (no change to GeminiClient itself)
```

Functional smoke test (optional, requires OPENROUTER_API_KEY in `local.properties`):
```bash
MAWAAI_RUN_LIVE_API_TESTS=1 ./gradlew :app:test --tests com.mawaai.love.app.ApiHealthSmokeTest
# → Should still pass; new wiring doesn't break tests
```

---

### MT-014 (GeminiVisionClient model name audit)

```bash
# Confirm no -latest aliases remain
grep -n "gemini-.*-latest" app/src/main/java/com/mawaai/love/app/design/ai/gemini/GeminiVisionClient.kt
# → MUST return empty

# Confirm only canonical models are referenced
grep -n "gemini-" app/src/main/java/com/mawaai/love/app/design/ai/gemini/GeminiVisionClient.kt
# → Should only show gemini-1.5-flash, gemini-2.0-flash, or gemini-2.5-flash (no -latest)
```

---

### MT-006 (thob_sudani template scaffold)

```bash
# Validate JSON syntax
python3 -c "import json; data = json.load(open('app/src/main/assets/templates/thob_sudani/templates.json')); print('templates:', len(data['templates']))"
# → Must print "templates: 5"

# Confirm all 5 entries are default_estimate
python3 -c "import json; data = json.load(open('app/src/main/assets/templates/thob_sudani/templates.json')); print('default_estimate:', sum(1 for t in data['templates'] if t['authoring_status'] == 'default_estimate'))"
# → Must print "default_estimate: 5"

# Confirm schema fields present in every entry
python3 -c "
import json
data = json.load(open('app/src/main/assets/templates/thob_sudani/templates.json'))
required = {'id', 'quad', 'blend', 'alpha', 'authoring_status'}
for t in data['templates']:
    missing = required - set(t.keys())
    assert not missing, f'{t[\"id\"]} missing: {missing}'
print('All entries OK')
"
# → Must print "All entries OK"
```

---

### MT-007 (API key hygiene audit)

```bash
# .gitignore must include local.properties
grep "local.properties" .gitignore
# → Must print at least one matching line

# No real key material in tracked files (besides google-services.json which is expected)
git ls-files | xargs grep -lE "AIza[0-9A-Za-z_-]{30,}|hf_[A-Za-z0-9]{30,}|sk-or-v1-[a-f0-9]{40,}" 2>/dev/null | grep -v "google-services.json"
# → MUST return empty

# local.properties is NOT tracked
git ls-files | grep "local.properties"
# → MUST return empty

# Audit report exists
ls -la API_KEY_HYGIENE_2026-05-22.md
# → Must exist

# Audit report contains NO key material
grep -E "AIza[0-9A-Za-z_-]{20,}|hf_[A-Za-z0-9]{30,}|sk-or-v1-[a-f0-9]{40,}|cfut_[A-Za-z0-9]{30,}|vxcLa[A-Za-z0-9]+" API_KEY_HYGIENE_2026-05-22.md
# → MUST return empty
```

---

### MT-008 (Compose deprecation pass)

```bash
# Count deprecation warnings before and after
./gradlew compileDebugKotlin 2>&1 | grep -E "warning:.*deprecat" | wc -l
# → Must be LOWER than the baseline you recorded
```

Spot-check 2–3 of the modified files manually:
- Open the file in the IDE
- Confirm the layout still looks correct in the Compose preview
- Confirm no new `@OptIn(...)` annotations were added to silence the warning (the goal is to fix, not silence)

---

### MT-010 Phase 1 (Generate checklist)

```bash
# Checklist file exists with 28 rows
ls -la TEMPLATE_QA_CHECKLIST.md
grep -E "^\| (abaya|henna)_" TEMPLATE_QA_CHECKLIST.md | wc -l
# → Must print at least 28
```

### MT-010 Phase 2 (Apply checklist results)

After you fill in the checklist by running templates on an emulator/device:

```bash
# Count of default_estimate must be LOWER than before
python3 -c "
import json
for cat in ['abaya', 'henna']:
    data = json.load(open(f'app/src/main/assets/templates/{cat}/templates.json'))
    de = sum(1 for t in data['templates'] if t['authoring_status'] == 'default_estimate')
    auth = sum(1 for t in data['templates'] if t['authoring_status'] == 'authored')
    masked = sum(1 for t in data['templates'] if t['authoring_status'] == 'masked')
    print(f'{cat}: default_estimate={de}, authored={auth}, masked={masked}')
"
# → Total default_estimate should decrease; authored + masked should increase by the same amount

./gradlew assembleDebug
./gradlew test
# → Both must PASS
```

---

## 🚩 Red Flags — When To Reject Without Mercy

Reject and re-prompt if the AI does ANY of these:

1. **"I'll also fix..." / "While I'm here..."** — out-of-scope refactor. Reject.
2. Touches `GeminiClient.kt` during MT-013 (the prompt explicitly forbids this).
3. Adds a new dependency to `build.gradle.kts` or version catalog without explicit MT authorization.
4. Adds `@OptIn(...)` annotations instead of fixing deprecations in MT-008.
5. Invents a Gemini model name not in the verified stable list.
6. Posts a "summary of changes" without a per-section output format.
7. Says "you should also..." for a different problem — that belongs in a new MT, not this one.
8. Modifies `settings.gradle.kts`, `gradle.properties`, or `gradle/libs.versions.toml` outside MT-007's `.gitignore`-only authorization.
9. Includes any real API key in any committed file.
10. Doesn't run the build + test verification in its output (or doesn't explicitly ask you to).

---

## 🧯 If Something Goes Wrong

### The AI gave a half-broken diff
```bash
git restore .
git clean -fd
```
Re-prompt the agent with: "Verification gate failed on Step X — [paste the failing output]. Try again with the same MT prompt."

### The build was green, you committed, but the app crashes on launch
```bash
git revert HEAD       # creates a new commit that undoes the bad one
git push origin master
```
Then write a new "fix MT-XXX regression" MT prompt with the crash log.

### The AI is in a loop / hallucinating
- Open a fresh chat window
- Re-load `MASTER_PLAN.md` from scratch
- Re-paste the MT prompt from `PROMPTS.md`
- Do NOT continue the broken conversation — bad context is contagious

---

## 📈 Tracking Progress

Maintain a simple log somewhere (notebook, sticky note, an `EXECUTION_LOG.md` if you want):

```
MT-013 | sha=abc123 | gates: build✅ test✅ secrets✅ scope✅ | date: 2026-05-22
MT-014 | sha=def456 | gates: build✅ test✅ secrets✅ scope✅ | date: 2026-05-22
MT-006 | sha=...    | ...
MT-007 | sha=...    | ...
MT-008 | sha=...    | ...
MT-010 | sha=...    | (multi-stage: phase 1 + phase 2)
```

When all 6 MTs are checkmarks, the handoff is complete. Tag the repo:
```bash
git tag -a v1.0.0-rc1 -m "v1.0.0 release candidate — all MTs in 2026-05-22 handoff complete"
git push origin v1.0.0-rc1
```

---

## 📞 Escalation

If you hit a situation this guide does not cover:

1. Capture the AI's last 3 messages + the failing command output.
2. Either:
   - Open a fresh conversation with the orchestrator (the agent that built this handoff package) and paste the capture.
   - Or, manually fix the immediate problem and write a new MT prompt to formalize the fix.

Do not invent ad-hoc instructions for the downstream agent that contradict `MASTER_PLAN.md`. If the rules need updating, update `MASTER_PLAN.md` first, then re-prompt.

---

**End of Verification Guide.** Trust the process. The gates exist because every commit should leave the repo strictly better than it found it.
