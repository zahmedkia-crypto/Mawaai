# MAWAAI — One-Page Cheatsheet

Print this. Tape it next to your monitor.

---

## 🟢 Start A Session

```
1. cd ~/path/to/Mawaai && git pull && git status         (must be clean)
2. ./gradlew clean assembleDebug test                    (baseline must pass)
3. Open downstream AI chat → paste KICKOFF.md verbatim
4. Wait for "Ready — paste the next MT prompt from PROMPTS.md."
```

---

## 🔁 Per-MT Loop

```
1. Open PROMPTS.md → copy the next MT block
2. Paste into AI chat
3. Read the AI's response — check it has all 8 required sections
4. Apply the diff (copy file contents into your editor; or `git apply` if it gives a patch)
5. Run verification (VERIFICATION.md step-by-step):
       ./gradlew clean assembleDebug test
       git diff --stat
       git diff | grep -E "AIza|hf_|sk-or-v1|cfut_|vxcLa"   (must be empty)
       git diff | grep "Map<String,\s*Any>"                  (must be empty in template/AI/repo)
6. If ALL pass → commit with AI's "## Commit Message" block, then git push
7. If ANY fail → `git restore .` and re-prompt with the failure
```

---

## 🛑 Reject Output If…

- Missing any of the 8 output sections
- Touched a file not in MT's "Files you may modify"
- Added a new dependency (libs.versions.toml or build.gradle.kts deps block)
- Used `Map<String, Any>` in template/AI/repository code
- Added `@OptIn(...)` to silence a deprecation in MT-008
- Says "while I'm here" or "I also fixed..."
- Includes a real API key value anywhere

---

## 🎯 Execution Order (Recommended)

```
MT-013   →  MT-014   →  MT-006   →  MT-007   →  MT-008   →  MT-010
fallback   model audit  thob data   key hygiene  Compose    on-device QA
wire                                              deprecation
15 min     10 min       10 min       20 min       30 min     manual
```

Total agent time: ~90 min. Total your verification time: ~30 min. Plus MT-010 manual device QA.

---

## 📞 If Things Go Sideways

```bash
# AI gave half-broken output:
git restore . && git clean -fd

# You committed something bad:
git revert HEAD && git push

# AI is hallucinating:
# → Open new chat. Re-paste KICKOFF.md. Re-paste MT prompt.
# → Never continue a broken conversation.

# You don't know what failed:
git diff --stat HEAD~1                    # what changed in last commit
./gradlew assembleDebug 2>&1 | tail -50   # last 50 lines of build output
```

---

## ✅ When You're Done

After MT-010 Phase 2 commits cleanly:

```bash
git tag -a v1.0.0-rc1 -m "All 2026-05-22 handoff MTs complete"
git push origin v1.0.0-rc1
```

Project is now at v1.0.0 release-candidate. Next EPIC: signed release build + Play Store assets.

---

**Hard rules at a glance:**

> 1. Only read what the MT lists.
> 2. One MT per response.
> 3. Diagnose, then code.
> 4. Stability > features.
> 5. Build + test pass = gate.
> 6. Strongly typed only.
> 7. Background ≠ content (edge-to-edge).
> 8. No speculative refactors.

That's it. Trust the gates. Ship boring, verified commits.
