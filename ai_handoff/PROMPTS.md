# MAWAAI — Ready-To-Paste Prompts For Each Micro-Task

Paste each prompt **verbatim** into the downstream AI agent's chat. Each one is self-contained — the agent has all the context it needs from `MASTER_PLAN.md` (which must be loaded first).

**Workflow:**
1. Load `MASTER_PLAN.md` into the agent (system prompt / first message)
2. Wait for the agent to say "ready"
3. Copy the prompt for the next MT below
4. Paste, review the agent's output
5. Run the verification steps from `VERIFICATION.md`
6. Commit if verification passes
7. Move to next MT

---

## MT-013 — Wire OpenRouter fallback at GeminiClient call sites

**Status:** Ready. Run first.

```text
MT-013: Wire OpenRouter as a transparent fallback to GeminiClient.inspirationPrompts()

CONTEXT
- The repo already has `com.mawaai.love.app.design.ai.openrouter.OpenRouterClient` — a drop-in mirror of GeminiClient.inspirationPrompts(count: Int): List<String>. It was added in commit 940238da on 2026-05-22.
- OpenRouterClient is @Singleton @Inject-injectable and exposes:
    val isConfigured: Boolean
    suspend fun inspirationPrompts(count: Int = 5, model: String = "openrouter/auto"): List<String>
- The Gemini free-tier returns HTTP 429 when quota is exhausted; OpenRouterClient returns emptyList() on any failure (never throws).

YOUR TASK
1. Find the file(s) that CALL `geminiClient.inspirationPrompts(...)`. Do this by grepping ONLY for that exact symbol — do not browse the codebase.
   Suggested:
       grep -rn "inspirationPrompts" app/src/main/java/com/mawaai/love/app/

2. There will be 1–3 call sites (likely a ViewModel and possibly a UseCase).
3. For EACH call site, modify the surrounding code so that:
   - It still asks GeminiClient first.
   - If GeminiClient returns an empty list AND OpenRouterClient.isConfigured == true, it falls back to OpenRouterClient.inspirationPrompts(count) with the SAME count parameter.
4. To inject OpenRouterClient, add it to the constructor parameters of the calling ViewModel/UseCase (Hilt auto-wires).
5. Do NOT touch GeminiClient itself. Do NOT modify any other class.
6. Add a one-line KDoc above the fallback block explaining "MT-013: OpenRouter fallback when Gemini quota is exhausted."

FILES TO READ
- The output of `grep -rn "inspirationPrompts" app/src/main/java/com/mawaai/love/app/`
- Each file the grep returns (typically 1–3 files)

FILES YOU MAY MODIFY
- Only the file(s) returned by the grep. NOTHING ELSE.

ANTI-PATTERNS
- Do NOT modify GeminiClient.kt.
- Do NOT create a new "FallbackPromptClient" abstraction (that's a refactor, MT-013 is wiring).
- Do NOT add retry loops, exponential backoff, or circuit breakers (out of scope).
- Do NOT change the count parameter default or semantics.
- Do NOT log API responses (security gate).

OUTPUT FORMAT
Follow the Required Output Format from MASTER_PLAN.md Section 3 exactly.

VERIFICATION
After your changes, the human will run:
- ./gradlew assembleDebug   (must PASS)
- ./gradlew test            (must PASS)
- git diff                   (must show only files inside MT scope)
- grep "openRouterClient.inspirationPrompts" <modified files>  (must show fallback wiring)

NEXT
After MT-013 lands, next is MT-014 (audit GeminiVisionClient.kt for -latest model names).
```

---

## MT-014 — Audit GeminiVisionClient.kt for deprecated model names

**Status:** Ready. Run after MT-013.

```text
MT-014: Audit `app/src/main/java/com/mawaai/love/app/design/ai/gemini/GeminiVisionClient.kt` for any reference to deprecated Gemini model names.

CONTEXT
- The Gemini API has deprecated the `-latest` suffix on model names. Calling `gemini-1.5-flash-latest:generateContent` returns HTTP 404 as of 2026-05.
- GeminiClient.kt already uses the canonical name "gemini-1.5-flash" (verified in commit 940238da context).
- GeminiVisionClient.kt has not been audited yet.
- Stable model names verified live on 2026-05-22 via ListModels:
    gemini-2.5-flash
    gemini-2.0-flash
    gemini-2.0-flash-001
    gemini-2.0-flash-lite
    gemini-2.0-flash-lite-001
- For vision, prefer gemini-1.5-flash (still supported) or gemini-2.0-flash (newer, slightly higher cost).

YOUR TASK
1. Read GeminiVisionClient.kt in full. Summarize its current state in 5–10 bullets BEFORE producing any diff (per Hard Rule #3).
2. Find every string literal or constant that names a Gemini model.
3. For each:
   - If it uses `-latest` suffix → replace with the canonical name (drop the suffix).
   - If it uses `gemini-pro-vision` → replace with `gemini-1.5-flash` (vision is now multimodal in flash).
   - If it already uses a canonical name → leave unchanged, note "OK" in your summary.
4. Add an inline comment near each MODEL constant noting the audit date and source:
       // Audited 2026-05-22 against ListModels API. Canonical name (no -latest alias).
5. Do NOT change behaviour, signatures, or call patterns.

FILES TO READ
- app/src/main/java/com/mawaai/love/app/design/ai/gemini/GeminiVisionClient.kt

FILES YOU MAY MODIFY
- ONLY GeminiVisionClient.kt.

ANTI-PATTERNS
- Do NOT swap to a different model family (e.g., gemini-1.5-pro) — that changes cost/latency contract.
- Do NOT add new API parameters (e.g., safetySettings, generationConfig) unless replacing an explicit `-latest` reference.
- Do NOT refactor the file structure.

OUTPUT FORMAT
Follow Required Output Format from MASTER_PLAN.md Section 3.

VERIFICATION
- ./gradlew assembleDebug   (must PASS)
- ./gradlew test            (must PASS)
- grep -n "gemini-" GeminiVisionClient.kt   (must show only canonical names, no -latest)

NEXT
MT-006: scaffold thob_sudani template metadata.
```

---

## MT-006 — thob_sudani template metadata scaffold

**Status:** Ready. Independent of MT-013/014.

```text
MT-006: Scaffold `app/src/main/assets/templates/thob_sudani/templates.json` so all 5 placeholder template images have category-default placement metadata, mirroring the structure used for abaya (commit 7354ffec) and henna.

CONTEXT
- 5 thob_sudani images exist in `app/src/main/assets/templates/thob_sudani/`. They are placeholders per the existing templates.json `_doc` field, but they are loaded by TemplateAssetManager and will appear in the UI.
- The current templates.json has `_doc` describing the schema but the `templates` array is incomplete.
- Other categories' templates.json (abaya, henna, walls) use this exact schema:
    {
      "_doc": "...",
      "_authoring_status_legend": {...},
      "_category_defaults": "..." or "_surface_defaults": {...},
      "templates": [
        {
          "id": "<filename_without_extension>",
          "quad": [[x0,y0],[x1,y1],[x2,y2],[x3,y3]],   // top-left, top-right, bottom-right, bottom-left in [0..1]
          "blend": "NORMAL" | "MULTIPLY" | "OVERLAY" | "SCREEN" | "FABRIC_REALISTIC",
          "alpha": 0.0..1.0,
          "authoring_status": "default_estimate" | "authored" | "masked"
        }
      ]
    }
- Sudanese thobs are loose flowing garments. The design placement zone is typically the chest/torso panel, similar to abaya "classic" but slightly wider.

YOUR TASK
1. List the 5 template image filenames in `app/src/main/assets/templates/thob_sudani/` (one `cat` / `ls` of that directory only).
2. For EACH image (5 total), create an entry in the `templates` array using these defaults:
       quad:  [[0.26, 0.30], [0.74, 0.30], [0.76, 0.72], [0.24, 0.72]]
       blend: "FABRIC_REALISTIC"
       alpha: 0.82
       authoring_status: "default_estimate"
3. Update the `_doc` field to match the abaya/henna pattern.
4. Add a `_category_defaults` block describing the zone: "Chest/torso panel for loose thob silhouette."
5. Add a `_authoring_status_legend` block (copy from abaya/templates.json verbatim).

FILES TO READ
- app/src/main/assets/templates/thob_sudani/templates.json   (current state)
- A directory listing of app/src/main/assets/templates/thob_sudani/   (to get the 5 filenames)
- app/src/main/assets/templates/abaya/templates.json   (reference schema only)

FILES YOU MAY MODIFY
- ONLY app/src/main/assets/templates/thob_sudani/templates.json

ANTI-PATTERNS
- Do NOT add masks (no .mask.png files exist yet).
- Do NOT pixel-tune quads — they are deliberately default_estimate.
- Do NOT remove the existing `_doc` "generated placeholders" note — append to it.
- Do NOT touch any Kotlin code (this is a pure data MT).

OUTPUT FORMAT
Follow Required Output Format from MASTER_PLAN.md Section 3.
Include the FULL new templates.json content in a single JSON code block.

VERIFICATION
- ./gradlew assembleDebug   (must PASS — JSON syntax check via build)
- ./gradlew test            (must PASS)
- python -c "import json; json.load(open('app/src/main/assets/templates/thob_sudani/templates.json'))"   (must not raise)
- jq '.templates | length' app/src/main/assets/templates/thob_sudani/templates.json   (must return 5)

NEXT
MT-007: API key hygiene audit.
```

---

## MT-007 — API key hygiene audit + `.gitignore` review

**Status:** Ready. Independent.

```text
MT-007: Audit the repo for API key hygiene risks and write a sanitized audit report. Make minimal corrective changes if and only if a hygiene violation is found.

CONTEXT
- The app uses BuildConfig fields populated from `local.properties` for: GEMINI_API_KEY, PEXELS_API_KEY (unused), HUGGINGFACE_API_KEY, REMOVE_BG_API_KEY, CLOUDFLARE_ACCOUNT_ID, CLOUDFLARE_API_TOKEN, OPENROUTER_API_KEY.
- `local.properties` MUST be in `.gitignore`. If not, that's a P0 finding.
- `app/google-services.json` is currently tracked. Firebase API keys in google-services.json are not technically "secrets" but should be restricted in the Google Cloud console.
- No real key value should appear in `git log -p` or in any tracked file.

YOUR TASK
1. Read `.gitignore` and verify each of these is ignored:
   - `local.properties`
   - `*.keystore`, `*.jks`
   - `.env`, `.env.*`
   - `app/release/`
   - `build/`
   - `.gradle/`
2. Read the root and `app/` directory listings to confirm no `local.properties` file is tracked.
3. Read `app/google-services.json` and report (a) which Firebase project ID it points to and (b) whether the API key field present is the public web/Android client key (not a server-side admin secret).
4. Run `grep -rn "AIza\|hf_\|sk-or-v1\|cfut_\|vxcLa1" --include="*.kt" --include="*.kts" --include="*.md" --include="*.json" --include="*.gradle*" .` from the repo root. Report any matches. (Note: matching the Firebase web client key in google-services.json is expected; flag anything else.)
5. Verify the repo has a `KEY_HYGIENE.md` at the root. If not, CREATE one (this is the only file you may add). Content template:
   - "This repo follows the Mawaai key hygiene protocol."
   - List of ignored files and the reason
   - Instructions for adding a new BuildConfig key (1. local.properties; 2. app/build.gradle.kts; 3. BuildConfig consumer)
   - Rotation procedure if a key is ever exposed in a public commit (`git rev-list` to find blast radius; `bfg-repo-cleaner` to scrub; rotate key in provider console)
6. If `.gitignore` is missing any of the ignores in step 1, append them (this is the only authorized .gitignore modification).
7. Write a sanitized audit report at root: `API_KEY_HYGIENE_2026-05-22.md`. NO key material in this file. Document findings from steps 1–4.

FILES TO READ
- .gitignore
- app/google-services.json
- Output of the grep in step 4

FILES YOU MAY MODIFY
- .gitignore (only if a step-1 ignore is missing)
- (optionally create) KEY_HYGIENE.md
- (create) API_KEY_HYGIENE_2026-05-22.md

ANTI-PATTERNS
- Do NOT include any real key value in any committed file.
- Do NOT delete google-services.json (it's required for Firebase).
- Do NOT modify any Kotlin code.
- Do NOT add `bfg` as a project dependency — only mention it in the .md as a tool to use when needed.

OUTPUT FORMAT
Follow Required Output Format. Include the full content of any .md you create.

VERIFICATION
- grep -rn "AIza\|hf_\|sk-or-v1\|cfut_\|vxcLa1" --include="*.kt" --include="*.kts" --include="*.md" .  (only google-services.json match expected)
- git ls-files | grep -E "local\.properties|\.env"   (must return empty)
- cat .gitignore | grep -E "local.properties"        (must return one match)

NEXT
MT-008: Compose deprecation pass.
```

---

## MT-008 — Compose deprecation cleanup pass

**Status:** Ready. Independent.

```text
MT-008: Find and replace deprecated Compose / Material 3 APIs without changing UI behaviour. Scope is limited.

CONTEXT
- `./gradlew test` reports deprecation warnings including (at minimum) Material 3 text fields and `Icons.Filled.Undo`. The PROJECT_SCAN_2026-05-22.md catalogued this as MT-008.
- This is a cosmetic / lint-debt cleanup — NOT a behavior change.
- Compose BOM is pinned via the version catalog (`libs.androidx.compose.bom`).

YOUR TASK
1. Run: `./gradlew compileDebugKotlin 2>&1 | grep -E "warning: 'Icons\.Filled\." | head -30`
   (If the user cannot run this, they will paste the warning output. If they cannot paste, ask for it explicitly — do not browse the codebase.)
2. For EACH deprecation warning, identify the deprecated symbol and the recommended replacement. Common ones in Compose Material 3:
       Icons.Filled.Undo            → Icons.AutoMirrored.Filled.Undo
       Icons.Filled.ArrowBack       → Icons.AutoMirrored.Filled.ArrowBack
       Icons.Filled.List            → keep (not deprecated)
       OutlinedTextField + KeyboardActions deprecated text-field params → use the (text, onValueChange) overload
       Divider                       → HorizontalDivider
3. Group all warnings by file. For each file, propose ONE diff containing all the replacements in that file.
4. For each replacement, verify the new symbol exists by checking the import. If the import is missing, add it.
5. Do NOT change any layout structure, any modifier order, any state management.

FILES TO READ
- Each file containing a deprecation warning (typically ≤10 files)

FILES YOU MAY MODIFY
- ONLY files containing the warnings — do not touch any other Compose file

ANTI-PATTERNS
- Do NOT migrate Material 2 → Material 3 (out of scope).
- Do NOT change colors, typography, or theme references.
- Do NOT add `@OptIn(...)` annotations to suppress (the goal is to fix, not silence).
- Do NOT touch any business-logic code, ViewModels, or non-Compose files.

OUTPUT FORMAT
Follow Required Output Format. Use one ## File: section per file modified.

VERIFICATION
- ./gradlew assembleDebug   (must PASS)
- ./gradlew test            (must PASS)
- ./gradlew compileDebugKotlin 2>&1 | grep -c "warning: 'Icons.Filled\."   (count must be lower than baseline)

NEXT
MT-010: on-device template QA (requires emulator/device — human-driven).
```

---

## MT-010 — On-device template QA for 28 default_estimate entries

**Status:** Ready. Requires human + device. AI assists with batching.

```text
MT-010: For each of the 28 default_estimate template entries (19 abaya + 9 henna), verify on-device that the placement quad lands the design inside the fabric/skin region. Update authoring_status to "authored" + correct the quad if needed.

CONTEXT
- After MT-004/005 (commit 7354ffec), 28 templates have category-default placement quads marked authoring_status="default_estimate" in their templates.json.
- Defaults are sensible but unverified against actual image geometry.
- Templates with masks should also have a matching <id>.mask.png — these get authoring_status="masked".

EXECUTION MODE
This MT cannot be done by an AI alone — it requires looking at rendered output on a device. The AI's role here is to:
1. Generate a per-template checklist (28 rows)
2. After the human reports results, batch the JSON updates into one commit

YOUR TASK (Phase 1 — checklist generation)
1. Read app/src/main/assets/templates/abaya/templates.json and app/src/main/assets/templates/henna/templates.json.
2. Build a Markdown checklist `TEMPLATE_QA_CHECKLIST.md` at the repo root with one row per default_estimate entry:
       | id | current_quad | new_quad (filled in by human) | status (default_estimate / authored / masked) | notes |
3. Add a brief "How to QA" section at the top of the checklist:
       - Load the template in the design studio
       - Run a known-good design through the pipeline
       - Eyeball-check: does the design land inside the fabric/skin region with no clipping?
       - If yes: change status to "authored" and leave quad unchanged.
       - If no: open the image in any editor, identify the 4 corners of the actual fabric/skin region, divide each (x,y) by image width/height, paste the new normalized quad in the table.

YOUR TASK (Phase 2 — JSON update, after human reports)
4. Once the human fills in the table and returns it:
   - Parse the table.
   - Generate the updated abaya/templates.json and henna/templates.json.
   - Preserve all other fields (_doc, _category_defaults, etc.).
   - Bump authoring_status per row.
5. Commit with message "MT-010: on-device template QA pass — N templates authored, M masked".

FILES TO READ (Phase 1)
- app/src/main/assets/templates/abaya/templates.json
- app/src/main/assets/templates/henna/templates.json

FILES YOU MAY MODIFY (Phase 1)
- (create) TEMPLATE_QA_CHECKLIST.md

FILES YOU MAY MODIFY (Phase 2)
- app/src/main/assets/templates/abaya/templates.json
- app/src/main/assets/templates/henna/templates.json
- (mark complete) TEMPLATE_QA_CHECKLIST.md

ANTI-PATTERNS
- Do NOT invent QA results — only update JSON after the human reports actual on-device observations.
- Do NOT delete entries that the human left as "default_estimate" — those just stay flagged for the next QA pass.
- Do NOT touch any Kotlin code or any other category's templates.

OUTPUT FORMAT (Phase 1)
Follow Required Output Format. Include the full TEMPLATE_QA_CHECKLIST.md content.

OUTPUT FORMAT (Phase 2)
Follow Required Output Format. Include both updated templates.json files in full.

VERIFICATION (Phase 1)
- jq '.templates | map(select(.authoring_status == "default_estimate")) | length' app/src/main/assets/templates/abaya/templates.json
  → 19 (before this MT)
- (TEMPLATE_QA_CHECKLIST.md exists and has 28 rows)

VERIFICATION (Phase 2)
- jq '.templates | map(select(.authoring_status == "default_estimate")) | length' app/src/main/assets/templates/abaya/templates.json
  → ≤19 (lower than before; equal to count of rows the human left as default_estimate)
- ./gradlew assembleDebug   (must PASS — JSON syntax check)
- Manual review: each "authored" row in TEMPLATE_QA_CHECKLIST.md must correspond to an entry in the JSON with status="authored".

NEXT
None — this is the final MT in the current handoff. After MT-010, the project is at v1.0.0 readiness. Plan a release (signed build, version bump, tag, Play Store assets) as the next EPIC.
```

---

## END OF PROMPTS

After all 6 MTs are committed and verified, the project is at production-ready for v1.0.0.

The downstream agent should refuse any task not in this file unless the human explicitly opens a new EPIC. If asked to do something off-plan, the agent should respond: "That is outside the current handoff scope. Confirm you want a new EPIC plan first."
