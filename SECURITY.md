# Security Policy

## Supported versions

Mawaai ships from `master`. Only the most recent release on the Play Store
receives security fixes; older versions should be considered unsupported.

| Version | Supported |
|---|---|
| Latest Play Store release | ✅ |
| Previous Play Store release | ⚠️ — critical CVEs only, no feature work |
| Any prior version | ❌ |

## Reporting a vulnerability

**Do not** open a public GitHub issue or pull request for security-sensitive
problems.

Instead, email the maintainer at the address listed on the project owner's
GitHub profile, or use GitHub's
[private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing-information-about-vulnerabilities/privately-reporting-a-security-vulnerability)
feature on this repository.

In your report, please include:

- The affected version (Play Store version code, or commit SHA if testing a
  build from source).
- A description of the issue, including the impact and any preconditions
  needed to exploit it.
- Step-by-step reproduction instructions. A minimal proof-of-concept is
  always welcome.
- Your name / handle for credit in the advisory, or your preference to remain
  anonymous.

You will receive an acknowledgement within **72 hours**. We aim to ship a
patch within **30 days** for critical vulnerabilities and **90 days** for
medium-severity issues.

## Scope

This policy covers:

- The Mawaai Android app and any released APK / AAB artifact.
- Configuration files committed to this repository (`AndroidManifest.xml`,
  `app/build.gradle.kts`, `app/google-services.json`).
- Server-side endpoints owned by the project, if any are introduced in the
  future.

This policy does **not** cover:

- Third-party services the app talks to (Gemini, HuggingFace, Cloudflare
  Workers AI, Remove.bg, OpenRouter, Groq, Pexels, Firebase). Report
  vulnerabilities in those directly to the provider.
- Issues that require physical access to an unlocked, rooted device — those
  are outside our threat model.

## What we treat as a vulnerability

Examples (non-exhaustive):

- Extracting any of the AI provider keys from a released APK in a way that
  bypasses the provider-side restrictions documented in
  [`docs/security-runbook.md`](docs/security-runbook.md). **Mere extraction of
  the BuildConfig key string is not a vulnerability** if the provider has
  package + SHA-1 restrictions in place — that is by design (defence in
  depth, see § 2 of the runbook).
- Bypassing the biometric lock when `profile.biometricEnabled = true`.
- Reading the contents of `EncryptedSharedPreferences` (used by `KeyVault`,
  PR #11) without the Android Keystore key on a non-rooted device.
- Persisting an attacker-controlled override into `KeyVault` from a different
  app on the same device.
- Local data exfiltration: any other app reading
  `/data/data/com.mawaai.love.app/`.
- SSRF, RCE, or any classic web-style flaw if a backend service is added.

## Acknowledgements

Researchers who submit valid, in-scope reports will be credited in:

- The GitHub Security Advisory for the corresponding CVE (if one is opened).
- The Play Store release notes for the patched version.
- A `docs/security/acknowledgements.md` file (created on first credit).

Thank you.
