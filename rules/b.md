# build-fixer-ultra-detailed.md
# ══════════════════════════════════════════════════════════
# SAVE LOCATION: Same as main-developer-role (see that file)
# PURPOSE: Fix Gradle/Android Studio build errors systematically
# ══════════════════════════════════════════════════════════

---

## YOUR ONLY JOB: Fix Gradle and Android Studio build errors.

> **Read the ENTIRE error before touching any file. One wrong fix creates three new errors.**

---

## STEP 1: READ AND PARSE THE ERROR LOG

**DO THIS EXACTLY:**

1. Copy the ENTIRE error output from the `Build` tab or terminal
2. Find the first line that contains: `FAILURE:` or `error:` or `ERROR`
   - **Start from the FIRST error — later errors are often caused by the first**
3. Extract this information:
   - **Error type** (Resource / Dependency / Sync / Kotlin / ProGuard / Manifest)
   - **File path** where the error occurs (e.g., `app/build.gradle.kts:14`)
   - **Exact error message** (copy word-for-word)
4. CHECK what DSL is being used:
   - File ends in `.kts` → **Kotlin DSL** → syntax: `key = "value"` with `=`
   - File ends in `.gradle` → **Groovy DSL** → syntax: `key "value"` without `=`

---

## STEP 2: CLASSIFY THE PROBLEM

**USE THIS TABLE — Find your error text on the left:**

| ERROR TEXT CONTAINS | PROBLEM TYPE | LIKELY ROOT CAUSE |
|---|---|---|
| `resource linking failed` | Resource | Missing string key in one language folder |
| `cannot resolve symbol` | Dependency | Library not declared or wrong version |
| `Duplicate class` | Dependency Conflict | Two libraries include the same class |
| `minSdk` / `uses-sdk` | SDK Mismatch | Library requires higher minSdk than declared |
| `R.java not generated` | Resource Syntax | XML file has a syntax error |
| `Unresolved reference` | Kotlin Compile | Missing import OR wrong dependency |
| `Expected performance` | Kotlin DSL Error | Groovy syntax written inside `.kts` file |
| `Could not resolve` | Network/Cache | Dependency can't be downloaded |
| `Manifest merger failed` | Manifest Conflict | Two manifests declare conflicting attributes |
| `Build was configured...` | Gradle Config | `buildFeatures` or option used incorrectly |
| `java.lang.OutOfMemoryError` | Memory | Gradle heap too small |
| `No signature of method` | Wrong DSL | Kotlin syntax used in Groovy file or vice versa |
| `ksp` error | Annotation Processor | KSP version mismatch with Kotlin version |
| `AGP requires...` | Plugin Version | Android Gradle Plugin too old for this Kotlin |

---

## STEP 3: EXECUTE THE FIX

### FIX A: Resource Missing in a Language Folder
**Symptom:** `resource linking failed` or `string/my_key not found`

```
STEPS:
1. Open: app/src/main/res/
2. CHECK which folders exist: values/, values-ar/, values-fr/, values-es/
   → Note: values/ is English (DEFAULT). Do NOT create values-en/ — it's redundant.
3. Open values/strings.xml → find the missing key
4. Open values-ar/strings.xml → add the Arabic translation for that key
5. Repeat for values-fr/ and values-es/
6. RULE: Every key in values/strings.xml MUST exist in ALL language folders
```

**VERIFICATION:**
```bash
./gradlew clean assembleDebug
# Must show: BUILD SUCCESSFUL
```

---

### FIX B: Dependency Version Conflict or Unresolved
**Symptom:** `cannot resolve symbol` or `Duplicate class` or `Could not resolve`

```kotlin
// ✅ CORRECT — Use BOM for Compose (manages ALL compose versions together)
// In app/build.gradle.kts:
dependencies {
    val composeBom = platform(libs.compose.bom)  // if using version catalog
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")           // NO version — BOM controls it
    implementation("androidx.compose.material3:material3")  // NO version
}

// ❌ WRONG — Hardcoding individual Compose versions causes conflicts:
implementation("androidx.compose.ui:ui:1.6.4")  // never mix manual versions with BOM
```

**For Duplicate class conflict:**
```kotlin
// Exclude the conflicting transitive dependency:
implementation("com.squareup.retrofit2:retrofit:2.11.0") {
    exclude(group = "com.squareup.okhttp3", module = "okhttp")
}
implementation("com.squareup.okhttp3:okhttp:4.12.0")  // use explicit version
```

**STEPS:**
1. Open `gradle/libs.versions.toml` (preferred) OR `app/build.gradle.kts`
2. Update the version to the latest stable
3. Press "Sync Now" in the yellow bar
4. Run `./gradlew dependencies` to see the full dependency tree if conflict persists

---

### FIX C: Kotlin DSL Syntax Error (Most Common Mistake)
**Symptom:** `No signature of method` or `Expected performance` or `Unresolved reference`

```kotlin
// ═══════════════════════════════════════════════════════
// KOTLIN DSL (.kts files) — CORRECT SYNTAX:
// ═══════════════════════════════════════════════════════

android {
    compileSdk = 35                          // ← USE = assignment
    
    defaultConfig {
        minSdk = 24                          // ← USE = assignment
        targetSdk = 35                       // ← USE = assignment
        versionCode = 1                      // ← USE = assignment
        versionName = "1.0.0"               // ← DOUBLE quotes only
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true          // ← Boolean prefix: is
            isShrinkResources = true        // ← Boolean prefix: is
        }
    }
    
    // ✅ For Kotlin 2.0+ with kotlin.compose plugin (modern approach):
    // Just add the plugin — no composeOptions block needed:
    // alias(libs.plugins.kotlin.compose)
    
    // For older Kotlin < 2.0 only:
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"  // ← USE = and DOUBLE quotes
    }
}

// ═══════════════════════════════════════════════════════
// GROOVY DSL (.gradle files) — CORRECT SYNTAX:
// ═══════════════════════════════════════════════════════

android {
    compileSdk 35                            // ← NO = sign

    defaultConfig {
        minSdk 24                            // ← NO = sign
        targetSdk 35                         // ← NO = sign
        versionCode 1                        // ← NO = sign
        versionName '1.0.0'                 // ← single OR double quotes
    }
    
    buildTypes {
        release {
            minifyEnabled true               // ← no "is" prefix
            shrinkResources true             // ← no "is" prefix
        }
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion '1.5.14'  // ← NO = sign
    }
}
```

---

### FIX D: Sync Failed / Cache Corrupted
**Symptom:** Sync button keeps spinning, or `Could not resolve` for known libraries

```
STEPS IN ORDER:
1. File → Invalidate Caches → check ALL boxes → "Invalidate and Restart"
2. Wait for Android Studio to restart completely
3. If still failing:
   - Close Android Studio
   - Delete folder: ~/.gradle/caches/  (Mac/Linux) or C:\Users\YOU\.gradle\caches\ (Windows)
   - Reopen Android Studio → Sync
4. If still failing:
   - Check internet connection
   - Verify repositories in settings.gradle.kts:
```

```kotlin
// settings.gradle.kts — verify these repositories exist:
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()           // ← MUST be present
        mavenCentral()     // ← MUST be present
        // Add only if specific library needs it:
        // maven { url = uri("https://jitpack.io") }
    }
}
```

---

### FIX E: Manifest Merger Failed
**Symptom:** `Manifest merger failed with multiple errors`

```xml
<!-- In YOUR AndroidManifest.xml, override the conflict: -->
<manifest xmlns:tools="http://schemas.android.com/tools">
    <application
        tools:replace="android:allowBackup,android:theme"
        android:allowBackup="false">
    </application>
</manifest>

<!-- OR suppress a specific library's attribute: -->
<uses-sdk tools:overrideLibrary="com.conflicting.library" />
```

---

### FIX F: ProGuard / R8 Stripping Code
**Symptom:** App crashes in release build but works in debug

```proguard
# ══════════════════════════════════════════════════
# proguard-rules.pro — CORRECT RULES
# SAVE AT: app/proguard-rules.pro
# ══════════════════════════════════════════════════

# ❌ NEVER USE THIS — keeps EVERYTHING, defeats R8 completely:
# -keep class ** { *; }

# ✅ CORRECT — Keep only what's needed:

# Retrofit models (data classes used in API responses)
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Hilt generated code
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep your own model classes from obfuscation
-keep class com.yourpackage.data.model.** { *; }
-keep class com.yourpackage.domain.model.** { *; }
```

---

### FIX G: KSP / Annotation Processor Mismatch
**Symptom:** `error: [ksp]` or Room/Hilt annotation errors

```toml
# In gradle/libs.versions.toml — versions MUST be compatible:
[versions]
kotlin = "2.0.0"
ksp = "2.0.0-1.0.21"    # ← KSP version format: {kotlin}-{ksp}
# Visit: https://github.com/google/ksp/releases to find matching version
```

---

### FIX H: Out of Memory During Build
**Symptom:** `java.lang.OutOfMemoryError: Java heap space`

```properties
# In gradle.properties (at project ROOT) — add or increase:
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
kotlin.incremental=true
```

---

## STEP 4: VERIFY THE FIX

**ALL THREE MUST SUCCEED:**

```bash
./gradlew clean              # Clean build cache
./gradlew assembleDebug      # Full debug build
./gradlew testDebugUnitTest  # All unit tests
```

**Expected output for each:**
```
BUILD SUCCESSFUL in Xs
```

**If build fails again:**
- Return to Step 1
- Look for a DIFFERENT first error (fixing one error may reveal another)
- Do NOT apply multiple fixes at once — fix one, verify, then fix next

---

## QUICK REFERENCE: SDK VERSION REQUIREMENTS (2025/2026)

| Setting | Minimum | Recommended |
|---|---|---|
| `compileSdk` | 34 | **35** |
| `targetSdk` | 34 | **35** |
| `minSdk` | 21 | **24** (drops 5% of devices, gains modern APIs) |
| Android Gradle Plugin | 8.3.0 | **8.5.0+** |
| Kotlin | 1.9.0 | **2.0.0** |
| Compose BOM | 2024.01.00 | **2024.06.00+** |
