# ════════════════════════════════════════════════════════════════
# Mawaai proguard-rules.pro
# Keeps R8 effective while preserving reflective / native call sites.
# ════════════════════════════════════════════════════════════════

# Keep line numbers for crash diagnosis
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ──────────────── Kotlin ────────────────
-keepclassmembers class kotlin.Metadata { *; }
-keep class kotlin.coroutines.Continuation
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ──────────────── Hilt / Dagger ────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep,allowobfuscation @interface dagger.hilt.android.AndroidEntryPoint
-keep,allowobfuscation @interface dagger.hilt.android.HiltAndroidApp
-keep,allowobfuscation @interface dagger.hilt.InstallIn
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltViewModelFactory { *; }

# ──────────────── Room ────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Database class *
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# ──────────────── Compose / Navigation ────────────────
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.tooling.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ──────────────── Gson (used by Gemini DTOs) ────────────────
-keepattributes Signature,*Annotation*
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }
-keep class com.mawaai.love.app.design.ai.gemini.** { *; }

# ──────────────── Retrofit / OkHttp ────────────────
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class retrofit2.** { *; }
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# ──────────────── kotlinx.serialization (Ktor / Supabase) ────────────────
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclasseswithmembers class **$$serializer { *; }
-keepclassmembers class * {
    static **$Companion Companion;
}
-keepclassmembers class * {
    static <clinit>();
}
-keepclassmembers,allowshrinking,allowobfuscation class * {
    *** Companion;
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    static <1>$$serializer INSTANCE;
}
-dontwarn kotlinx.serialization.**

# ──────────────── Supabase / Ktor ────────────────
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn io.github.jan.supabase.**

# ──────────────── ML Kit ────────────────
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-dontwarn com.google.mlkit.**

# ──────────────── OpenCV ────────────────
-keep class org.opencv.** { *; }
-keepclassmembers class org.opencv.** {
    native <methods>;
}
-dontwarn org.opencv.**

# ──────────────── TensorFlow Lite ────────────────
-keep class org.tensorflow.** { *; }
-keepclassmembers class org.tensorflow.** {
    native <methods>;
}
-dontwarn org.tensorflow.**

# ──────────────── Coil ────────────────
-dontwarn coil.**

# ──────────────── Lottie ────────────────
-keep class com.airbnb.lottie.** { *; }

# ──────────────── Media3 / ExoPlayer ────────────────
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ──────────────── Biometric ────────────────
-keep class androidx.biometric.** { *; }

# ──────────────── App domain / data models ────────────────
-keep class com.mawaai.love.app.data.model.** { *; }
-keep class com.mawaai.love.app.design.domain.model.** { *; }

# ──────────────── Parcelable ────────────────
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ──────────────── Misc ────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
