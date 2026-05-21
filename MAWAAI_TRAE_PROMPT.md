# 💍 MAWAAI — بروموت Trae الشامل النهائي
## تطبيق أندرويد رومانسي احترافي | Kotlin + Jetpack Compose | هدية لرزان

---

> **كيفية الاستخدام في Trae:**
> افتح مشروع Android Studio جديد → افتح Trae Builder → انسخ هذا البروموت كاملاً والصقه
> اتبع المراحل بالترتيب — كل مرحلة مستقلة ويمكن تنفيذها على دفعات

---

## ═══════════════════════════════════════
## 🎭 SYSTEM ROLE — هويتك كـ AI Agent
## ═══════════════════════════════════════

```
أنت فريق تطوير أندرويد متكامل من 3 خبراء:

① Senior Android Engineer  — Kotlin 1.9 · Jetpack Compose · Clean Architecture
② Romance UI/UX Designer   — Material 3 · Animations · RTL Arabic design
③ Mobile Architect         — Hilt DI · Room · WorkManager · Offline-first

مهمتك: بناء تطبيق "مواعي / Mawaai" — أندرويد احترافي من الفئة الأولى
هدف التطبيق: هدية رقمية فاخرة من قلب لخطيبة اسمها رزان

قواعد العمل:
✦ اكتب الكود الكامل دون اختصار أو TODO
✦ كل ملف يكون production-ready
✦ لا تترك أي import مفقود
✦ استخدم Kotlin idioms الحديثة دائماً
✦ كل Composable يحتوي على Preview
✦ RTL أولاً — التطبيق عربي بالكامل
```

---

## ═══════════════════════════════════════
## 📋 مواصفات المشروع — Project Specs
## ═══════════════════════════════════════

```
Package Name  :  com.mawaai.love.app
App Name      :  مأواي (Mawaai)
Min SDK       :  26  (Android 8.0+)
Target SDK    :  34  (Android 14)
Language      :  Kotlin 1.9.22
Compose BOM   :  2024.02.00
Architecture  :  MVVM + Clean Architecture + Repository Pattern
DI Framework  :  Hilt 2.50
Database      :  Room 2.6.1
Navigation    :  Navigation Compose 2.7.7
Async         :  Kotlin Coroutines + Flow
Background    :  WorkManager 2.9.0
Images        :  Coil 2.5.0
Animation     :  Lottie Compose 6.3.0 + Compose Animation
Media         :  ExoPlayer (Media3) 1.2.1
Auth/Cloud    :  Supabase 2.1.4 (اختياري — offline-first)
Security      :  Biometric 1.2.0-alpha05
Storage       :  DataStore Preferences 1.0.0
Build         :  Gradle Kotlin DSL (build.gradle.kts)
```

---

## ═══════════════════════════════════════
## 🚀 STAGE 0 — إعداد المشروع الكامل
## ═══════════════════════════════════════

### المطلوب: اكتب هذه الملفات بالكامل

---

### `build.gradle.kts` (Project level):

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
    id("com.google.gms.google-services") version "4.4.1" apply false
    id("com.google.firebase.crashlytics") version "2.9.9" apply false
}
```

---

### `app/build.gradle.kts` (App level) — الكامل:

المطلوب:
- تعريف namespace = "com.mawaai.love.app"
- minSdk=26, targetSdk=34, compileSdk=34
- versionCode=1, versionName="1.0.0"
- تفعيل buildFeatures { compose=true; buildConfig=true }
- compileOptions: JavaVersion.VERSION_17
- kotlinOptions: jvmTarget="17"
- تعريف buildConfigField للـ SUPABASE_URL و SUPABASE_KEY من local.properties
- تعريف release buildType مع minifyEnabled=true و shrinkResources=true

Dependencies المطلوبة (كلها):
```
// Compose BOM + Material3 + Animation + Foundation + Icons Extended
// Core KTX + Activity Compose + Lifecycle + ViewModel
// Navigation Compose
// Hilt Android + Hilt Compiler (kapt) + Hilt Navigation Compose + Hilt Work
// Room Runtime + Room KTX + Room Compiler (kapt)
// WorkManager KTX
// Supabase BOM + postgrest-kt + auth-kt + storage-kt + ktor-client-android
// Lottie Compose 6.3.0
// Coil Compose 2.5.0
// Media3 ExoPlayer + Media3 UI
// Biometric
// Firebase BOM + crashlytics + analytics + messaging
// Accompanist: systemuicontroller + permissions
// DataStore Preferences
// SplashScreen Core
// Retrofit + OkHttp + Gson Converter + Logging Interceptor
// Palette KTX (لاستخراج ألوان الصور)
// Testing: JUnit + MockK + Coroutines Test + Compose UI Test
```

---

### `AndroidManifest.xml` — الكامل:

Permissions المطلوبة:
- INTERNET, ACCESS_NETWORK_STATE
- CAMERA, READ_MEDIA_IMAGES, READ_EXTERNAL_STORAGE (maxSdk=32), WRITE_EXTERNAL_STORAGE (maxSdk=29)
- POST_NOTIFICATIONS, VIBRATE
- USE_BIOMETRIC, USE_FINGERPRINT
- FOREGROUND_SERVICE, RECEIVE_BOOT_COMPLETED

Application attributes:
- android:name=".MawaaiApp"
- android:supportsRtl="true"
- android:hardwareAccelerated="true"
- android:largeHeap="true"
- android:theme="@style/Theme.Mawaai.Splash"

Activity:
- screenOrientation="portrait"
- windowSoftInputMode="adjustResize"
- configChanges="locale|layoutDirection"
- Intent filter لـ LAUNCHER
- Deep Link filter: scheme="mawaai", host="memory"

Components إضافية:
- BootReceiver للإشعارات بعد إعادة التشغيل
- FileProvider للمشاركة

---

### `core/theme/Color.kt` — نظام الألوان الكامل:

```kotlin
object MawaaiColors {
    val DeepNight      = Color(0xFF0A0510)
    val SurfaceDark    = Color(0xFF130A1C)
    val CardDark       = Color(0xFF1A0F28)
    val CardElevated   = Color(0xFF221436)

    val RoseGold       = Color(0xFFE8A7B5)
    val RoseGoldDim    = Color(0xFFC4849A)
    val ChampagneGold  = Color(0xFFD4AF37)
    val SoftRose       = Color(0xFFFF6B8A)
    val DeepRose       = Color(0xFFE0294A)
    val PearlWhite     = Color(0xFFFFF0F5)
    val LavenderPurple = Color(0xFF9B59B6)
    val CrimsonRed     = Color(0xFF8B0000)

    val TextPrimary    = Color(0xFFFFF0F5)
    val TextSecondary  = Color(0xFFE8A7B5)
    val TextHint       = Color(0xFF7B5E6B)
    val TextPoetic     = Color(0xFFD4AF37)

    val GlassRose      = Color(0x20E8A7B5)
    val GlassBorder    = Color(0x40E8A7B5)
    val GlassGold      = Color(0x20D4AF37)

    // Gradients (as Brush)
    val GradMain       = Brush.verticalGradient(listOf(Color(0xFF0A0510), Color(0xFF1A0F28)))
    val GradCard       = Brush.verticalGradient(listOf(Color(0xFF1A0F28), Color(0xFF221436)))
    val GradButton     = Brush.horizontalGradient(listOf(Color(0xFFE0294A), Color(0xFF9B59B6)))
    val GradGold       = Brush.horizontalGradient(listOf(Color(0xFFD4AF37), Color(0xFFAA8C2C)))
    val GradRose       = Brush.horizontalGradient(listOf(Color(0xFFE8A7B5), Color(0xFFD4AF37)))
}
```

---

### `core/theme/Type.kt` — الخطوط:

```kotlin
val CairoFamily = FontFamily(
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_bold, FontWeight.Bold),
    Font(R.font.cairo_extra_bold, FontWeight.ExtraBold)
)
val AmiriFamily  = FontFamily(Font(R.font.amiri_regular))
val GreatVibesFamily = FontFamily(Font(R.font.great_vibes))

// MawaaiTypography: استخدم CairoFamily كـ default لكل styles
// headlineLarge: 28sp Bold — عناوين رئيسية
// headlineMedium: 22sp Bold — عناوين شاشات
// titleLarge: 20sp Bold — عناوين البطاقات
// titleMedium: 18sp Medium — عناوين فرعية
// bodyLarge: 16sp Regular — النصوص الأساسية
// bodyMedium: 14sp Regular — النصوص الثانوية
// labelSmall: 11sp Regular — التسميات الصغيرة
```

---

### `core/theme/Theme.kt` — Mawaai Theme:

```kotlin
@Composable
fun MawaaiTheme(content: @Composable () -> Unit) {
    // MaterialTheme داكن دائماً
    // colorScheme من MawaaiColors
    // typography من MawaaiTypography
    // shapes: RoundedCornerShape للمكونات
    // تفعيل LocalLayoutDirection = CompositionLocalProvider(LayoutDirection.Rtl)
    // تفعيل LocalContentColor = MawaaiColors.TextPrimary
}
```

---

### `core/theme/Motion.kt` — الحركة والانتقالات:

```kotlin
// Spring Animations
val HeartSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow
)

// Screen Transitions (Navigation)
val ScreenEnterTransition  // fadeIn(400ms) + slideInVertically(from 1/6)
val ScreenExitTransition   // fadeOut(300ms) + slideOutVertically(to -1/6)

// Shimmer Effect للـ Loading
@Composable fun ShimmerBrush(targetValue: Float = 1000f): Brush
    // InfiniteTransition + translateX animation
    // Brush.linearGradient من شفاف → ذهبي فاتح → شفاف

// Glow Modifier
fun Modifier.goldGlow(radius: Dp = 12.dp): Modifier
    // drawBehind + Paint + BlurMaskFilter
```

---

## ═══════════════════════════════════════
## 🗃️ STAGE 1 — Data Layer الكامل
## ═══════════════════════════════════════

اكتب هذه الملفات كاملة بالكود الكامل:

---

### `data/model/Memory.kt`:
```kotlin
@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val imagePath: String?,        // مسار الصورة المحلي
    val date: Long,                // timestamp
    val category: MemoryCategory,
    val mood: MoodType,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val syncedToCloud: Boolean = false
)

enum class MemoryCategory {
    ROMANTIC, TRAVEL, FOOD, SPECIAL_DAY, GENERAL
}
```

### `data/model/LoveLetter.kt`:
```kotlin
@Entity(tableName = "love_letters")
data class LoveLetter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val backgroundId: Int = 0,   // 0-4 للخلفيات المدمجة
    val fontFamily: String = "Cairo",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
```

### `data/model/Countdown.kt`:
```kotlin
@Entity(tableName = "countdowns")
data class Countdown(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetDate: Long,          // timestamp
    val type: CountdownType,
    val iconRes: Int,
    val notify7Days: Boolean = true,
    val notifyOnDay: Boolean = true,
    val isCompleted: Boolean = false
)

enum class CountdownType {
    ENGAGEMENT, WEDDING, TRAVEL, BIRTHDAY, SPECIAL, RELIGIOUS
}
```

### `data/model/MoodEntry.kt`:
```kotlin
@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mood: MoodType,
    val note: String = "",
    val date: Long = System.currentTimeMillis()
)

enum class MoodType(val emoji: String, val label: String) {
    HAPPY("😊", "سعيد"),
    LOVING("💕", "محب"),
    AMAZED("😍", "مبهور"),
    GRATEFUL("🥰", "ممتنن"),
    EXCITED("💫", "متشوق")
}
```

### `data/model/WishItem.kt`:
```kotlin
@Entity(tableName = "wishes")
data class WishItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: WishCategory,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class WishCategory(val emoji: String, val label: String) {
    TRAVEL("🌍", "سفر"),
    FOOD("🍽️", "مطاعم"),
    EXPERIENCE("🎭", "تجارب"),
    ROMANTIC("💝", "رومانسي"),
    HOME("🏡", "منزل")
}
```

### `data/model/DrawingStroke.kt`:
```kotlin
data class DrawingStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val alpha: Float = 1f,
    val layerIndex: Int = 1
)

data class DrawingState(
    val strokes: List<DrawingStroke> = emptyList(),
    val currentColor: Color = Color(0xFFE8A7B5),
    val currentStrokeWidth: Float = 5f,
    val currentAlpha: Float = 1f,
    val currentLayer: Int = 1,
    val canvasBackground: Color = Color.White
)
```

### `data/model/UserProfile.kt`:
```kotlin
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val userName: String = "",
    val partnerName: String = "رزان",
    val engagementDate: Long? = null,
    val profileImagePath: String? = null,
    val selectedTheme: ThemeVariant = ThemeVariant.ROSE,
    val morningNotifHour: Int = 8,
    val eveningNotifHour: Int = 20,
    val notificationsEnabled: Boolean = true,
    val biometricEnabled: Boolean = false
)

enum class ThemeVariant { ROSE, GOLD, PURPLE, RED }
```

---

### `data/database/MawaaiDatabase.kt`:

```kotlin
@Database(
    entities = [Memory::class, LoveLetter::class, Countdown::class,
                MoodEntry::class, WishItem::class, UserProfile::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MawaaiDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun loveLetterDao(): LoveLetterDao
    abstract fun countdownDao(): CountdownDao
    abstract fun moodDao(): MoodDao
    abstract fun wishDao(): WishDao
    abstract fun profileDao(): ProfileDao
}

class Converters {
    // @TypeConverter لـ Color, List<Offset>, MoodType, MemoryCategory...
    // استخدم Gson لتحويل القوائم
    // استخدم ordinal للـ Enums
}
```

---

### DAOs الكاملة:

**MemoryDao.kt:**
```kotlin
@Dao interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY date DESC")
    fun getAllMemories(): Flow<List<Memory>>

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY date DESC")
    fun getMemoriesByCategory(category: MemoryCategory): Flow<List<Memory>>

    @Query("SELECT * FROM memories WHERE isFavorite = 1 ORDER BY date DESC")
    fun getFavoriteMemories(): Flow<List<Memory>>

    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getMemoryById(id: Long): Memory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: Memory): Long

    @Update suspend fun updateMemory(memory: Memory)
    @Delete suspend fun deleteMemory(memory: Memory)

    @Query("SELECT COUNT(*) FROM memories")
    fun getMemoryCount(): Flow<Int>
}
```

**LoveLetterDao, CountdownDao, MoodDao, WishDao, ProfileDao:**
اكتبها بنفس الأسلوب مع الـ queries المناسبة لكل entity.

---

### Repositories الكاملة:

**MemoryRepository.kt:**
```kotlin
@Singleton
class MemoryRepository @Inject constructor(
    private val dao: MemoryDao,
    private val fileUtils: FileUtils
) {
    fun getAllMemories(): Flow<List<Memory>> = dao.getAllMemories()
    fun getFavorites(): Flow<List<Memory>> = dao.getFavoriteMemories()
    fun getByCategory(cat: MemoryCategory) = dao.getMemoriesByCategory(cat)

    suspend fun addMemory(memory: Memory, imageUri: Uri?): Result<Long> {
        return try {
            val localPath = imageUri?.let { fileUtils.copyImageToInternal(it) }
            val id = dao.insertMemory(memory.copy(imagePath = localPath))
            Result.success(id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteMemory(memory: Memory) {
        memory.imagePath?.let { fileUtils.deleteFile(it) }
        dao.deleteMemory(memory)
    }
}
```

اكتب CountdownRepository, LoveLetterRepository, MoodRepository, WishRepository, ProfileRepository بالكامل.

---

## ═══════════════════════════════════════
## 🎬 STAGE 2 — Core Components المشتركة
## ═══════════════════════════════════════

### `core/components/ParticleHeartSystem.kt` — ⭐ مكوّن مهم جداً:

```kotlin
data class HeartParticle(
    val id: Int,
    val startX: Float,      // 0f..1f relative
    val speed: Float,       // animationDuration بالـ ms
    val size: Float,        // حجم dp
    val alpha: Float,       // 0.05f..0.20f
    val drift: Float,       // انحراف أفقي
    val delay: Int          // تأخير البداية بالـ ms
)

@Composable
fun ParticleHeartSystem(
    particleCount: Int = 8,
    modifier: Modifier = Modifier
) {
    // 1. أنشئ القائمة باستخدام remember { List(particleCount) { HeartParticle(...) } }
    // 2. InfiniteTransition لكل جسيم (دورة من 0f إلى 1f)
    // 3. Canvas { للرسم }:
    //    - كل قلب: y = size * (1 - progress) * 1.1f - extraPad
    //    - ارسم القلب بـ Path من cubic beziers (لا Unicode)
    //    - drawPath بـ alpha وcolor RoseGold
    //    - rotation خفيف مع الحركة

    // Heart Path (cubic bezier):
    // moveTo(0f, -size*0.3f)
    // cubicTo(-size*0.5f, -size*0.8f, -size, -size*0.3f, 0f, size*0.4f)
    // cubicTo(size, -size*0.3f, size*0.5f, -size*0.8f, 0f, -size*0.3f)
}
```

---

### `core/components/RoseGlassCard.kt`:
```kotlin
@Composable
fun RoseGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    // Box مع:
    // - background: Brush.verticalGradient من GlassRose إلى شفاف
    // - border: 1dp GlassBorder مع RoundedCornerShape(16.dp)
    // - blur effect: backdropBlur إذا متاح API 31+ وإلا gradient فقط
    // - clickable مع ripple وردي إذا onClick != null
    // - padding داخلي 16.dp
}
```

---

### `core/components/GoldDivider.kt`:
```kotlin
@Composable
fun GoldDivider(
    modifier: Modifier = Modifier,
    width: Dp = 120.dp
) {
    // Canvas رسم خط من المنتصف للخارج مع Brush ذهبي
    // animate width من 0 إلى المطلوب عند أول ظهور
    // نقطة صغيرة ماسية في المنتصف
}
```

---

### `core/components/HeartButton.kt`:
```kotlin
@Composable
fun HeartButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    // Spring animation على الضغط: scale 1.0 → 0.92 → 1.08 → 1.0
    // Background: GradButton brush
    // شكل: RoundedCornerShape(50.dp) — pill shape
    // Haptic feedback: HapticFeedbackType.LongPress
    // إذا isLoading: CircularProgressIndicator صغير بدل النص
    // Disabled state: opacity 0.5
}
```

---

### `core/components/LoadingHeart.kt`:
```kotlin
@Composable
fun LoadingHeart() {
    // قلب يتسع ويصغر بـ InfiniteTransition
    // scale: 0.8f → 1.2f → 0.8f بـ spring
    // لون: SoftRose → DeepRose
    // رسم بـ Canvas مع نفس Path القلب
    // نص "لحظة..." تحته بخط Amiri
}
```

---

### `core/components/RomanticTopBar.kt`:
```kotlin
@Composable
fun RomanticTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    // TopAppBar بـ MawaaiColors.SurfaceDark
    // title: خط Cairo Bold، لون RoseGold
    // navigationIcon: سهم عودة أنيق إذا onBack != null
    // خط ذهبي رفيع في الأسفل
    // elevation: 0.dp (نعتمد على اللون)
}
```

---

### `core/utils/QuoteUtils.kt`:
```kotlin
object QuoteUtils {
    val LOVE_QUOTES = listOf(
        "الحب ليس كلمة تُقال، بل لحظة تُعاش",
        "في عينيكِ وجدت وطني الذي ضللته",
        "كل يوم بجانبكِ هو هدية لا تتكرر",
        "أجمل ما في الحياة أن تُحبّ وتكون محبوبًا",
        "قلبي يسكن حيث تكوني",
        "أنتِ الفكرة التي لا تغادر رأسي",
        "لو كان الحب كلمة، لاخترت اسمك",
        "منذ عرفتكِ، صار العالم أجمل",
        "أنتِ السبب الذي يجعلني أبتسم دون سبب",
        "الحب الحقيقي يسكن في التفاصيل الصغيرة",
        "رزان... كل لحظة معكِ تستحق أن تُحفظ للأبد",
        "أنتِ من جعل المستقبل يستحق الانتظار",
        "حبّكِ يجعل أصعب الأيام محتملة",
        "في كل مكان أذهب إليه، أتمنى لو كنتِ بجانبي",
        "الوقت الذي أمضيه معكِ هو أجمل استثماراتي",
        "لم أفهم معنى البيت حتى قابلتكِ",
        "أنتِ لستِ كل حياتي، أنتِ أجمل جزء منها",
        "الحب الحقيقي لا يُوصف، يُعاش فحسب",
        "أنتِ السر الذي أريد الاعتراف به كل يوم",
        "مع كل غروب شمس، أشكر الله على وجودكِ",
        "أنتِ من أكملت ما كان ناقصًا",
        "قلبي تعلّم اسمكِ قبل أن يتعلم الخوف",
        "كل شيء جميل يذكّرني بكِ",
        "أنتِ من علّمني أن اللحظة الحاضرة هي أثمن هدية",
        "الحب يبدأ حين تصبح سعادة الآخر أهم من سعادتك",
        "أجمل قصة كتبتها الحياة بيني وبينكِ",
        "مع وجودكِ، لا أحتاج شيئًا آخر",
        "أنتِ الحلم الوحيد الذي لا أريد الاستيقاظ منه",
        "لن يكفيني عمر لأشكر الله على هديته... أنتِ",
        "رزان... مواعي قلبي لكِ إلى الأبد 💍"
    )

    fun getDailyQuote(): String {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return LOVE_QUOTES[dayOfYear % LOVE_QUOTES.size]
    }

    fun getRandomQuote() = LOVE_QUOTES.random()
}
```

---

### `core/utils/DateUtils.kt`:
```kotlin
object DateUtils {
    fun formatArabicDate(timestamp: Long): String
    fun getTimeGreeting(): String {
        // 6-12: "صباح الورد يا رزان ☀️"
        // 12-17: "وقت الغداء... فكّرت فيكِ 💕"
        // 17-21: "مساء الحب يا رزان 🌙"
        // 21-6:  "تصبحين على خير يا حبيبتي 🌟"
    }
    fun getDaysUntil(targetTimestamp: Long): Long
    fun formatCountdown(millis: Long): CountdownDisplay // days/hours/mins/secs
    fun isSameDay(ts1: Long, ts2: Long): Boolean
}
```

---

### `core/utils/FileUtils.kt`:
```kotlin
@Singleton
class FileUtils @Inject constructor(@ApplicationContext val context: Context) {
    suspend fun copyImageToInternal(uri: Uri): String?
    fun deleteFile(path: String): Boolean
    suspend fun exportCanvasAsBitmap(bitmap: Bitmap): Uri?
    fun getBitmapFromPath(path: String): Bitmap?
    suspend fun createShareableImage(bitmap: Bitmap): Uri
}
```

---

### `core/utils/HapticUtils.kt`:
```kotlin
object HapticUtils {
    fun heartbeat(context: Context)   // نبضة خفيفة 50ms
    fun success(context: Context)     // نبضتان 50-100-50ms
    fun error(context: Context)       // اهتزاز قصير 100ms
    fun click(view: View)             // haptic feedback خفيف
}
```

---

## ═══════════════════════════════════════
## 🎬 STAGE 3 — Splash + Intro + Onboarding
## ═══════════════════════════════════════

### `ui/splash/SplashScreen.kt` — الكامل:

```kotlin
@Composable
fun SplashScreen(
    onNavigateToIntro: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    // خلفية: MawaaiColors.DeepNight
    // 1. أيقونة القلب مع الخاتم: ارسمها بـ Canvas (لا ImageVector خارجي)
    //    - قلب كبير لون DeepRose
    //    - حلقة ذهبية صغيرة على يمين القلب
    //    - scale animation: 0f → 1.1f → 1.0f بـ spring
    //    - glow effect ذهبي حول الأيقونة
    // 2. "مواعي" — Cairo ExtraBold — 38sp — RoseGold — fadeIn بعد 500ms
    // 3. "Mawaai" — GreatVibes — 22sp — ChampagneGold — fadeIn بعد 800ms
    // 4. بعد 2 ثانية: viewModel.checkFirstLaunch() → navigate
    // 5. ParticleHeartSystem(particleCount=5) في الخلفية بشفافية عالية

    LaunchedEffect(Unit) {
        delay(2000)
        viewModel.checkFirstLaunch()
    }

    val isFirstLaunch by viewModel.isFirstLaunch.collectAsStateWithLifecycle()
    LaunchedEffect(isFirstLaunch) {
        isFirstLaunch?.let { first ->
            if (first) onNavigateToIntro() else onNavigateToHome()
        }
    }
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {
    private val FIRST_LAUNCH_KEY = booleanPreferencesKey("first_launch")
    val isFirstLaunch = MutableStateFlow<Boolean?>(null)

    fun checkFirstLaunch() = viewModelScope.launch {
        val isFirst = dataStore.data.first()[FIRST_LAUNCH_KEY] != false
        isFirstLaunch.value = isFirst
        if (isFirst) dataStore.edit { it[FIRST_LAUNCH_KEY] = false }
    }
}
```

---

### `ui/intro/IntroScreen.kt` — ⭐ أهم شاشة في التطبيق:

```
اكتب IntroScreen.kt الكامل بهذه المراحل الدقيقة:

المدة الإجمالية: 8 ثوانٍ | زر تخطي بعد ثانيتين

المرحلة 1 (0-2 ثانية):
- خلفية: Gradient من #0A0510 إلى #1A0F28
- نجوم صغيرة (15 نقطة) تتلألأ بـ InfiniteTransition
- خاتم ماسي في المنتصف: ارسمه بـ Canvas
  • دائرة خارجية ذهبية stroke 3dp
  • ماسة صغيرة فوقه (Path مثلثي مع fill ذهبي)
  • scale من 0f → 1.1f → 1.0f بـ spring bouncy
  • shimmer دائري يدور حوله

المرحلة 2 (2-5 ثانية):
- الخاتم ينتقل للأعلى بـ animateDpAsState
- نصوص تظهر بالتسلسل بـ typewriter effect:
  • "رزان..." خط Amiri 38sp لون #E8A7B5 — shimmer effect
  • GoldDivider يمتد من المنتصف
  • "في زحمة الدنيا" — fadeIn من أسفل تأخير 200ms
  • "وجدتك أنتِ" — نفس الأسلوب تأخير 600ms
  • "هذا التطبيق" — تأخير 1000ms
  • "ليس كودًا وبيانات" — تأخير 1400ms
  • "هو مواعيد قلبي لكِ 💍" — scale 0.8→1.0 مع goldGlow تأخير 1800ms

typewriter effect: استخدم var displayedChars by remember { mutableStateOf(0) }
                   LaunchedEffect(text) { repeat(text.length) { delay(40); displayedChars++ } }

المرحلة 3 (6-8 ثانية):
- انبثاق 15-20 قلباً من أسفل الشاشة بـ LaunchEffect
  • كل قلب: angle عشوائي ±45° من المنتصف
  • velocity عشوائية مختلفة
  • alpha يبدأ 1f وينتهي 0f خلال 1.5 ثانية
  • ألوان: SoftRose, RoseGold, ChampagneGold
- كل الشاشة تتلاشى
- "مواعي 💍" بـ scale من 0.5f → 1.0f
- الانتقال للـ Onboarding
```

---

### `ui/onboarding/OnboardingScreen.kt` — 3 شرائح:

```kotlin
// HorizontalPager مع 3 صفحات:

// الشريحة 1 — ذكرياتنا:
// - رسم أيقونة البوم بـ Canvas (لا ImageVector)
// - قلوب صغيرة تطير حول الأيقونة
// - عنوان: "ذكرياتنا" 28sp Bold RoseGold
// - نص: "احفظي كل لحظة جميلة... لأن بعض اللحظات تستحق أن تخلد 💕"

// الشريحة 2 — رسائلي لكِ:
// - رسم ظرف بريد يتفتح بـ Canvas + animation
// - عنوان: "رسائلي لكِ"
// - نص: "كل ما يصعب قوله بالكلمات، يُكتب بالحروف 💌"

// الشريحة 3 — مواعيدنا:
// - رسم ساعة رومانسية بـ Canvas مع عقارب تدور
// - عنوان: "مواعيدنا"
// - نص: "كل لقاء يستحق احتفالاً... لأنكِ تستحقين العالم ⏳"

// مشتركات:
// - نقاط التنقل في الأسفل (animated)
// - زر "التالي" / "ابدأ" في الأسفل
// - ParticleHeartSystem في الخلفية
// - Pager indicator مع animation
```

---

## ═══════════════════════════════════════
## 🏠 STAGE 4 — HomeScreen الرئيسية
## ═══════════════════════════════════════

### `ui/home/HomeScreen.kt` + `HomeViewModel.kt` — الكاملين:

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val memoryRepo: MemoryRepository,
    private val countdownRepo: CountdownRepository,
    private val profileRepo: ProfileRepository
) : ViewModel() {
    val dailyQuote = QuoteUtils.getDailyQuote()
    val greeting = DateUtils.getTimeGreeting()
    val nextCountdown = countdownRepo.getNextUpcoming().stateIn(...)
    val recentMemory = memoryRepo.getAllMemories().map { it.firstOrNull() }.stateIn(...)
    val todayMood = moodRepo.getTodayMood().stateIn(...)
    val profile = profileRepo.getProfile().stateIn(...)
}

@Composable
fun HomeScreen(
    onNavigateToMemories: () -> Unit,
    onNavigateToLetters: () -> Unit,
    onNavigateToDrawing: () -> Unit,
    onNavigateToCountdowns: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    // الهيكل:
    // Scaffold {
    //   topBar = RomanticTopBar("مواعي 💍")
    //   bottomBar = MawaaiBottomNavBar(...)
    //   content = LazyColumn {
    //     item { WelcomeCard(greeting) }
    //     item { DailyQuoteCard(quote) }
    //     item { NextCountdownCard(countdown) }
    //     item { RecentMemoryCard(memory) }
    //     item { MoodWidget(todayMood) { mood -> viewModel.saveMood(mood) } }
    //   }
    //   ParticleHeartSystem(8) -- خلفية شفافة
    // }
}
```

### `ui/home/components/BottomNavBar.kt`:
```kotlin
// 5 أيقونات: البيت | ذكريات | رسائل | مواعيد | ارسم
// (الإعدادات في TopBar)
// كل أيقونة: Vector Icon + نص عربي صغير
// الـ active: لون RoseGold + نقطة ذهبية متحركة تحت الأيقونة
// الخلفية: glass effect فوق المحتوى
// انتقال: spring animation على التغيير
```

### `ui/home/components/DailyQuoteCard.kt`:
```kotlin
// RoseGlassCard مع:
// - أيقونة اقتباس ذهبية في الأعلى
// - النص بخط Amiri 16sp مع TextAlign.Center
// - سطر الـ shimmer effect على النص
// - زر "مشاركة" أسفل البطاقة
// - animation: ظهور البطاقة بـ slideIn من اليمين عند أول تحميل
```

### `ui/home/components/NextCountdownCard.kt`:
```kotlin
// RoseGlassCard مع:
// - اسم الموعد بالأعلى
// - 4 أرقام كبيرة: أيام | ساعات | دقائق | ثوانٍ
// - كل رقم في مربع صغير مع اسمه تحته
// - LaunchedEffect لتحديث الثواني كل ثانية
// - FlipAnimation للأرقام عند التغيير (مثل ساعة الـ flip)
```

### `ui/home/components/MoodWidget.kt`:
```kotlin
// Row من 5 إيموجي (كـ Text Composables)
// كل إيموجي: دائرة مع تأثير عند الضغط
// المحدد: scale أكبر + border ذهبي
// animation: bouncy spring عند الاختيار
```

---

## ═══════════════════════════════════════
## 📸 STAGE 5 — MemoriesScreen
## ═══════════════════════════════════════

### `ui/memories/MemoriesScreen.kt`:
```kotlin
@Composable
fun MemoriesScreen(onMemoryClick: (Long) -> Unit, onAddMemory: () -> Unit) {
    // Scaffold + RomanticTopBar("ذكرياتنا 📸")
    // FilterChips: كل | رومانسي | سفر | أكل | يوم خاص
    // LazyVerticalStaggeredGrid(columns=2, verticalItemSpacing=8.dp) {
    //   items(memories) { memory -> MemoryCard(memory, onClick) }
    // }
    // FAB: HeartButton("+ ذكرى جديدة")
    // EmptyState: LottieAnimation + "أضيفي أولى ذكرياتكما 💕"
}
```

### `ui/memories/components/MemoryCard.kt`:
```kotlin
// Card مع:
// - AsyncImage من Coil بـ ContentScale.Crop
// - Gradient overlay في الأسفل: شفاف → SurfaceDark
// - التاريخ بخط Cairo 11sp في الأسفل
// - أيقونة قلب للمفضلة في الأعلى اليسار
// - تأثير الظهور: fadeIn + scale من 0.8 عند أول ظهور
// - Shimmer placeholder أثناء تحميل الصورة
```

### `ui/memories/AddMemoryScreen.kt`:
```kotlin
// الحقول:
// 1. ImagePicker: مستطيل كبير للضغط واختيار صورة
//    - إذا تم اختيار صورة: عرضها مع زر X للحذف
//    - إذا لا: أيقونة كاميرا + "اضغطي لإضافة صورة 📷"
// 2. DatePicker: حقل تاريخ بتصميم مخصص (MaterialDatePicker)
// 3. TextField للعنوان — خط Cairo Bold
// 4. TextField للوصف — multiline — خط Cairo Regular
// 5. Category Chips — اختيار واحد فقط
// 6. Mood Picker — نفس MoodWidget
// 7. HeartButton("حفظ الذكرى 💕")
//    - عند الحفظ الناجح: Lottie success + navigate back
```

### `ui/memories/MemoryDetailScreen.kt`:
```kotlin
// FullScreen Layout:
// - صورة الذكرى بـ parallax effect: تتحرك بسرعة أبطأ من السحب
// - gradient overlay من أسفل: شفاف → DeepNight
// - CollapsingToolbar behavior
// - التاريخ: Amiri font — ذهبي
// - العنوان: Cairo Bold — PearlWhite
// - الوصف: Cairo Regular — TextSecondary
// - Action buttons: تعديل | مشاركة | حذف
// - Palette API: استخدم ألوان الصورة لتلوين الـ gradient
```

---

## ═══════════════════════════════════════
## 💌 STAGE 6 — LettersScreen
## ═══════════════════════════════════════

### `ui/letters/LettersScreen.kt`:
```kotlin
// قائمة الرسائل:
// كل بطاقة تشبه ورقة مطوية:
// - لون كريمي #FFF8F0 على خلفية داكنة
// - seal قلب ذهبي في أعلى اليمين
// - العنوان + أول سطرين
// - التاريخ في الأسفل

// TabRow: "رسائلي لكِ" | "المفضلة"
// FAB: "كتابة رسالة جديدة 💌"
```

### `ui/letters/ComposeLetterScreen.kt`:
```kotlin
// خلفية: لون #FFF8F0 (ورق كريمي)
// texture: ارسمه بـ Canvas (خطوط رفيعة أفقية فاتحة جداً)
// Toolbar للتنسيق:
//   - Bold | Italic | Underline | حجم الخط (14-22sp)
// اختيار خلفية الرسالة:
//   - 5 خيارات: كريمي | ورود | نجوم | ذهبي | بنفسجي
//   - كلها مبنية بـ Canvas (بدون صور خارجية)
// TextField للعنوان + TextField للجسم (Amiri font)
// زر إرسال: animation رسالة طائرة بـ Canvas
```

### `ui/letters/LetterDetailScreen.kt`:
```kotlin
// الرسالة تُعرض على خلفيتها المختارة
// الخط: Amiri للجسم
// أسفل الرسالة: "من قلبي لكِ دائمًا 💍" — GreatVibes font
// أزرار: تعديل | PDF export | حذف
// haptic feedback خفيف عند الفتح (heartbeat)
```

---

## ═══════════════════════════════════════
## ✏️ STAGE 7 — DrawingScreen — لوحة الرسم
## ═══════════════════════════════════════

### `ui/drawing/DrawingCanvasView.kt` — ⭐ مكون حيوي:

```kotlin
@Composable
fun DrawingCanvas(
    drawingState: DrawingState,
    onStrokeAdded: (DrawingStroke) -> Unit,
    modifier: Modifier = Modifier
) {
    // Canvas { } مع Modifier.pointerInput(Unit) {
    //   detectDragGestures(
    //     onDragStart = { offset -> currentPath = Path(); currentPath.moveTo(offset.x, offset.y) }
    //     onDrag = { change, _ ->
    //       val o = change.position
    //       // quadraticBezierTo للحصول على حركة ناعمة:
    //       val mid = Offset((prevPoint.x + o.x)/2f, (prevPoint.y + o.y)/2f)
    //       currentPath.quadraticBezierTo(prevPoint.x, prevPoint.y, mid.x, mid.y)
    //       prevPoint = o
    //     }
    //     onDragEnd = { onStrokeAdded(DrawingStroke(points, color, width)) }
    //   )
    //   detectTransformGestures { _, pan, zoom, _ ->
    //     scale = (scale * zoom).coerceIn(0.5f, 4f)
    //     offset = offset + pan
    //   }
    // }
    //
    // في الرسم:
    // scale(scale, scale, Offset.Zero) {
    //   translate(offset.x, offset.y) {
    //     // ارسم كل strokes السابقة
    //     // ارسم الـ stroke الحالي
    //   }
    // }
}
```

### `ui/drawing/DrawingScreen.kt`:
```kotlin
// Scaffold:
// TopBar: "رسالتي لكِ ✏️" + حفظ + مشاركة
// Content: DrawingCanvas يأخذ كل المساحة
// BottomBar: DrawingBottomBar
//   - أدوات: قلم | فرشاة | ممحاة | نص | أشكال (قلب، نجمة، وردة)
//   - ألوان: 12 دائرة ملونة + color picker
//   - سمك الخط: Slider
// Undo / Redo في TopBar
// حفظ: Bitmap.createBitmap من Canvas + تصدير PNG
```

### `ui/drawing/DrawingViewModel.kt`:
```kotlin
// undoStack: ArrayDeque<DrawingState>
// redoStack: ArrayDeque<DrawingState>
// currentState: StateFlow<DrawingState>

// fun addStroke(stroke: DrawingStroke):
//   pushToUndo()
//   currentState = currentState.copy(strokes = strokes + stroke)
//   redoStack.clear()

// fun undo(): if (undoStack.isNotEmpty()) { redoStack.push(current); current = undoStack.pop() }
// fun redo(): if (redoStack.isNotEmpty()) { undoStack.push(current); current = redoStack.pop() }

// fun exportAsBitmap(canvasSize: IntSize): Bitmap
//   Picture + PictureRecordingCanvas + drawStrokes + Picture.toBitmap
```

---

## ═══════════════════════════════════════
## ⏳ STAGE 8 — CountdownsScreen
## ═══════════════════════════════════════

### `ui/countdowns/CountdownsScreen.kt`:
```kotlin
// LazyColumn من Countdown cards
// كل بطاقة تعرض:
//   - اسم الموعد + أيقونته
//   - 4 أرقام: DD | HH | MM | SS
//   - تحديث تلقائي كل ثانية بـ LaunchedEffect + delay(1000)
//   - إذا الموعد وصل: animation confetti صغير + رسالة "وصل اليوم! 🎉"
// FAB: "موعد جديد +"
// EmptyState: أيقونة ساعة + "أضيفي أول موعد لكما ⏳"
```

### `ui/countdowns/AddCountdownScreen.kt`:
```kotlin
// حقول:
// - TextField: اسم الموعد
// - DateTimePicker: تاريخ الموعد
// - CountdownType Chips: 💍 خطوبة | 💒 زفاف | ✈️ سفر | 🎂 عيد ميلاد | 📅 موعد خاص
// - Toggle: تذكير 7 أيام قبل
// - Toggle: تذكير يوم الموعد
// - HeartButton("إضافة الموعد")
// عند الحفظ: جدولة WorkManager
```

---

## ═══════════════════════════════════════
## 📖 STAGE 9 — OurStoryScreen
## ═══════════════════════════════════════

### `ui/story/OurStoryScreen.kt`:
```kotlin
// Timeline رومانسي عمودي:
// - خط ذهبي عمودي في المنتصف بـ Canvas
// - بطاقات تتناوب يمين/يسار (index % 2)
// - كل بطاقة: تاريخ + صورة مصغرة + وصف
// - عند الضغط: expandAnimation لعرض التفاصيل الكاملة
// - نقطة ذهبية على الخط عند كل حدث
//
// الأحداث الافتراضية:
// "أول مرة قابلتكِ" | "يوم الخطوبة 💍" | "أول رحلة سوا" | "يضاف بواسطة المستخدم"
//
// FAB: "أضف حدثاً جديداً +"
// LazyColumn بـ animateItemPlacement()
```

---

## ═══════════════════════════════════════
## 🌠 STAGE 10 — Wishes + Mood + Quiz
## ═══════════════════════════════════════

### `ui/wishes/WishesScreen.kt`:
```kotlin
// Bucket List رومانسي:
// LazyColumn + SwipeToDismiss للحذف
// كل Wish: Checkbox مخصص + النص + الفئة
// عند الإنجاز: StrikeThrough animation + confetti صغير
// فلتر بالفئات (CategoryChips أعلى الشاشة)
// FAB: "أضف أمنية جديدة 🌠"
```

### `ui/mood/MoodScreen.kt`:
```kotlin
// 5 إيموجي كبيرة في المنتصف مع تأثير اختيار
// عند الاختيار:
//   😊 سعيد:   نجوم تتساقط
//   💕 محب:    قلوب تطير
//   😍 مبهور:  أضواء مبهرة تومض
//   🥰 ممتنن:  موجات دوائر ذهبية
//   💫 متشوق:  نجوم دوارة
// رسم بياني (LineChart بـ Canvas) آخر 7 أيام
// رسالة تشجيعية مخصصة لكل مزاج
```

### `ui/quiz/LoveQuizScreen.kt`:
```kotlin
// 20 سؤال محفوظة locally
// تقدم: ProgressBar أعلى الشاشة (animated)
// كل سؤال:
//   - النص بخط Cairo Bold 20sp
//   - 4 خيارات كـ Cards
//   - عند الاختيار: تأثير اختيار (scale + border ذهبي)
// في النهاية:
//   - نتيجة من 20 مع نجوم
//   - رسالة رومانسية بناءً على النتيجة
//   - زر "إعادة اللعب"

val QUIZ_QUESTIONS = listOf(
    QuizQuestion("ما أجمل شيء في رزان؟",
        listOf("ابتسامتها", "عيونها", "طيبة قلبها", "أسلوبها"),
        correctIndex = -1),  // كل الإجابات صحيحة 💕
    QuizQuestion("أين تتمنى تقضي إجازتكما الأولى؟",
        listOf("تركيا 🇹🇷", "المالديف 🏝️", "باريس 🇫🇷", "دبي 🇦🇪"),
        correctIndex = -1),
    // ... 18 سؤالاً آخرين
)
```

---

## ═══════════════════════════════════════
## 🎵 STAGE 11 — Music + Cards
## ═══════════════════════════════════════

### `ui/music/MusicScreen.kt`:
```kotlin
// ExoPlayer integration:
// - يحمل الملف من res/raw/music_romantic.mp3 كـ default
// - zر "اختر أغنية" بـ ActivityResultContracts.OpenDocument

// الواجهة:
// - دائرة كبيرة في المنتصف مع نوتة موسيقية مرسومة بـ Canvas
// - اسم الأغنية + المغني
// - Seek bar مخصص بلون ذهبي
// - أزرار: ⏮ | ⏯ (مع animation) | ⏭
// - Visualizer: 20 عمود يتحرك بـ InfiniteTransition
//   (محاكاة — لا يستلزم Real AudioVisualizer)

// ExoPlayer setup:
// val player = remember { ExoPlayer.Builder(context).build() }
// DisposableEffect(Unit) { onDispose { player.release() } }
```

### `ui/cards/CardsScreen.kt`:
```kotlin
// 5 قوالب بطاقات مبنية بـ Canvas:
// 1. صباحية: gradient وردي مع شمس صغيرة
// 2. مسائية: gradient ليلي مع قمر ونجوم
// 3. عيد ميلاد: بالونات ملونة مع كيكة
// 4. عيد الحب: قلوب كثيرة مع ورود
// 5. مناسبة: إطار ذهبي فاخر

// تخصيص:
// - TextField: الاسم
// - TextField: الرسالة
// - اختيار القالب

// معاينة حية: Canvas يحدث مع كل تغيير
// تصدير PNG: Bitmap من Canvas
```

---

## ═══════════════════════════════════════
## ⚙️ STAGE 12 — Settings + Privacy
## ═══════════════════════════════════════

### `ui/settings/SettingsScreen.kt`:
```kotlin
// LazyColumn من Sections:

// Section 1 - الملف الشخصي:
//   - صورة البروفايل (دائرة كبيرة قابلة للتغيير)
//   - اسمك + اسم رزان (TextFields)
//   - تاريخ الخطوبة (DatePicker)

// Section 2 - الإشعارات:
//   - Switch: تفعيل الإشعارات
//   - TimePicker: وقت الإشعار الصباحي (default 8:00)
//   - TimePicker: وقت الإشعار المسائي (default 20:00)

// Section 3 - الخصوصية:
//   - Switch: قفل بالبصمة — BiometricPrompt
//   - إذا فعّل: اختبار البصمة فوراً
//   - PIN backup إذا البصمة غير متاحة

// Section 4 - المظهر:
//   - 4 دوائر ملونة: وردي | ذهبي | بنفسجي | أحمر
//   - ThemeVariant يُحفظ في DataStore
//   - التطبيق يعيد بناء الـ Theme فوراً

// Section 5 - النسخ الاحتياطي:
//   - زر: "رفع الذكريات للسحابة"
//   - زر: "استعادة من السحابة"
//   - آخر نسخة احتياطية: التاريخ والوقت

// Section 6 - عن التطبيق:
//   - "مواعي — مواعيد القلب"
//   - "صُنع بـ 💕 هدية لرزان"
//   - "الإصدار 1.0.0"
```

---

## ═══════════════════════════════════════
## 🔔 STAGE 13 — نظام الإشعارات
## ═══════════════════════════════════════

### `workers/DailyQuoteWorker.kt`:
```kotlin
@HiltWorker
class DailyQuoteWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val quote = QuoteUtils.getRandomQuote()
        showNotification(
            title = "💕 مواعي — لحظة حب لكِ",
            body = quote,
            channelId = CHANNEL_LOVE_QUOTES
        )
        return Result.success()
    }
}

// جدولة في AppModule:
// PeriodicWorkRequestBuilder<DailyQuoteWorker>(1, TimeUnit.DAYS)
//   .setInitialDelay(/* حتى الوقت المحدد */)
//   .build()
```

### `workers/CountdownWorker.kt`:
```kotlin
// يعمل يومياً
// يقرأ كل الـ Countdowns من Room
// لكل countdown:
//   - إذا باقي 7 أيام: إشعار "⏳ تبقى 7 أيام على [اسم الموعد]"
//   - إذا اليوم هو الموعد: إشعار "🎉 اليوم هو [اسم الموعد]! احتفلي بهذه اللحظة 💕"
```

### `core/notifications/NotificationManager.kt`:
```kotlin
// Channels:
// CHANNEL_LOVE_QUOTES: "رسائل الحب اليومية"
// CHANNEL_COUNTDOWNS: "تذكيرات المواعيد"

// createNotificationChannels() يُستدعى في Application.onCreate()

// buildNotification(title, body, channelId, deepLink?):
//   - LargeIcon: قلب مرسوم كـ Bitmap
//   - color: MawaaiColors.SoftRose
//   - priority: HIGH
//   - PendingIntent للـ Deep Link
```

---

## ═══════════════════════════════════════
## 🧭 STAGE 14 — Navigation الكامل
## ═══════════════════════════════════════

### `navigation/Screen.kt`:
```kotlin
sealed class Screen(val route: String) {
    object Splash      : Screen("splash")
    object Intro       : Screen("intro")
    object Onboarding  : Screen("onboarding")
    object Home        : Screen("home")
    object Memories    : Screen("memories")
    object AddMemory   : Screen("add_memory")
    object MemoryDetail: Screen("memory_detail/{id}") {
        fun createRoute(id: Long) = "memory_detail/$id"
    }
    object Letters     : Screen("letters")
    object ComposeLetter: Screen("compose_letter?id={id}") {
        fun createRoute(id: Long? = null) = "compose_letter?id=$id"
    }
    object LetterDetail: Screen("letter_detail/{id}") {
        fun createRoute(id: Long) = "letter_detail/$id"
    }
    object Drawing     : Screen("drawing")
    object Countdowns  : Screen("countdowns")
    object AddCountdown: Screen("add_countdown")
    object OurStory    : Screen("our_story")
    object Mood        : Screen("mood")
    object Wishes      : Screen("wishes")
    object Quiz        : Screen("quiz")
    object Music       : Screen("music")
    object Cards       : Screen("cards")
    object Settings    : Screen("settings")
    object Privacy     : Screen("privacy")
}
```

### `navigation/AppNavigation.kt`:
```kotlin
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = { ScreenEnterTransition },
        exitTransition = { ScreenExitTransition }
    ) {
        // كل شاشة composable { ... }
        // مع arguments لشاشات التفاصيل
        // مع deepLinks لـ mawaai://memory/{id}
    }
}
```

---

## ═══════════════════════════════════════
## 💉 STAGE 15 — Dependency Injection
## ═══════════════════════════════════════

### `di/AppModule.kt`:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): MawaaiDatabase =
        Room.databaseBuilder(ctx, MawaaiDatabase::class.java, "mawaai.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton fun provideMemoryDao(db: MawaaiDatabase) = db.memoryDao()
    @Provides @Singleton fun provideLetterDao(db: MawaaiDatabase) = db.loveLetterDao()
    @Provides @Singleton fun provideCountdownDao(db: MawaaiDatabase) = db.countdownDao()
    @Provides @Singleton fun provideMoodDao(db: MawaaiDatabase) = db.moodDao()
    @Provides @Singleton fun provideWishDao(db: MawaaiDatabase) = db.wishDao()
    @Provides @Singleton fun provideProfileDao(db: MawaaiDatabase) = db.profileDao()

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { ctx.preferencesDataStoreFile("mawaai_prefs") }

    @Provides @Singleton
    fun provideWorkManager(@ApplicationContext ctx: Context): WorkManager =
        WorkManager.getInstance(ctx)

    @Provides
    fun provideFileUtils(@ApplicationContext ctx: Context) = FileUtils(ctx)
}
```

### `MawaaiApp.kt`:
```kotlin
@HiltAndroidApp
class MawaaiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        scheduleDailyWorkers()
    }

    private fun createNotificationChannels() { ... }
    private fun scheduleDailyWorkers() {
        // DailyQuoteWorker: PeriodicWorkRequest يومي
        // CountdownWorker: PeriodicWorkRequest يومي
    }
}
```

---

## ═══════════════════════════════════════
## 🔐 STAGE 16 — Privacy Lock
## ═══════════════════════════════════════

### `ui/privacy/BiometricHelper.kt`:
```kotlin
class BiometricHelper(private val activity: FragmentActivity) {

    fun authenticate(
        title: String = "مواعي 💍",
        subtitle: String = "التحقق من هويتكِ للدخول",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit
    ) {
        val biometricPrompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { onSuccess() }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { onError(errString.toString()) }
                override fun onAuthenticationFailed() { onFailed() }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("إلغاء")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()
        biometricPrompt.authenticate(info)
    }
}
```

---

## ═══════════════════════════════════════
## 🎨 STAGE 17 — Resources
## ═══════════════════════════════════════

### `res/values-ar/strings.xml` — كل النصوص العربية:
```xml
<resources>
    <string name="app_name">مواعي</string>
    <string name="home">البيت</string>
    <string name="memories">ذكرياتنا</string>
    <string name="letters">رسائلي لكِ</string>
    <string name="drawing">ارسمي</string>
    <string name="countdowns">مواعيدنا</string>
    <string name="settings">الإعدادات</string>
    <string name="our_story">قصتنا</string>
    <string name="wishes">أحلامنا</string>
    <string name="mood_today">مزاج اليوم</string>
    <string name="love_quiz">لعبة الحب</string>
    <string name="our_song">أغنيتنا</string>
    <string name="greeting_cards">بطاقات</string>
    <string name="save">حفظ</string>
    <string name="cancel">إلغاء</string>
    <string name="delete">حذف</string>
    <string name="share">مشاركة</string>
    <string name="add_new">إضافة جديدة</string>
    <string name="empty_memories">لا يوجد ذكريات بعد... أضيفي أولى لحظاتكما 💕</string>
    <string name="empty_letters">لا يوجد رسائل... اكتبي أول رسالة 💌</string>
    <string name="empty_countdowns">لا يوجد مواعيد... أضيفي أول موعد ⏳</string>
    <!-- ... بقية النصوص -->
</resources>
```

### `res/values/themes.xml`:
```xml
<style name="Theme.Mawaai" parent="Theme.Material3.Dark.NoActionBar">
    <item name="android:statusBarColor">@android:color/transparent</item>
    <item name="android:navigationBarColor">@android:color/transparent</item>
    <item name="android:windowLightStatusBar">false</item>
</style>

<style name="Theme.Mawaai.Splash" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">#0A0510</item>
    <item name="postSplashScreenTheme">@style/Theme.Mawaai</item>
</style>
```

---

## ═══════════════════════════════════════
## ✅ STAGE 18 — MainActivity + Final Setup
## ═══════════════════════════════════════

### `MainActivity.kt`:
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var biometricHelper: BiometricHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        biometricHelper = BiometricHelper(this)

        enableEdgeToEdge()

        setContent {
            MawaaiTheme {
                // فحص Privacy Lock
                var isAuthenticated by remember { mutableStateOf(false) }
                val prefs = // read biometric setting from DataStore

                LaunchedEffect(Unit) {
                    if (biometricEnabled) {
                        biometricHelper.authenticate(
                            onSuccess = { isAuthenticated = true },
                            onError = { isAuthenticated = true }, // fallback
                            onFailed = {}
                        )
                    } else {
                        isAuthenticated = true
                    }
                }

                if (isAuthenticated) {
                    AppNavigation()
                } else {
                    LockScreen() // شاشة قفل انتظار البصمة
                }
            }
        }
    }
}
```

---

## ═══════════════════════════════════════
## 📁 هيكل المجلدات الكامل للمرجع
## ═══════════════════════════════════════

```
app/src/main/java/com/mawaai/love/app/
│
├── MawaaiApp.kt
├── MainActivity.kt
│
├── core/
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   ├── Type.kt
│   │   ├── Shape.kt
│   │   └── Motion.kt
│   │
│   ├── components/
│   │   ├── ParticleHeartSystem.kt   ⭐
│   │   ├── RoseGlassCard.kt
│   │   ├── HeartButton.kt
│   │   ├── GoldDivider.kt
│   │   ├── LoadingHeart.kt
│   │   ├── RomanticTopBar.kt
│   │   ├── MawaaiBottomNavBar.kt
│   │   └── EmptyStateRomantic.kt
│   │
│   ├── utils/
│   │   ├── DateUtils.kt
│   │   ├── QuoteUtils.kt
│   │   ├── FileUtils.kt
│   │   ├── HapticUtils.kt
│   │   ├── BitmapUtils.kt
│   │   └── Constants.kt
│   │
│   └── notifications/
│       └── NotificationManager.kt
│
├── data/
│   ├── model/
│   │   ├── Memory.kt
│   │   ├── LoveLetter.kt
│   │   ├── Countdown.kt
│   │   ├── MoodEntry.kt
│   │   ├── WishItem.kt
│   │   ├── DrawingStroke.kt
│   │   └── UserProfile.kt
│   │
│   ├── database/
│   │   ├── MawaaiDatabase.kt
│   │   ├── Converters.kt
│   │   ├── MemoryDao.kt
│   │   ├── LoveLetterDao.kt
│   │   ├── CountdownDao.kt
│   │   ├── MoodDao.kt
│   │   ├── WishDao.kt
│   │   └── ProfileDao.kt
│   │
│   └── repository/
│       ├── MemoryRepository.kt
│       ├── LoveLetterRepository.kt
│       ├── CountdownRepository.kt
│       ├── MoodRepository.kt
│       ├── WishRepository.kt
│       └── ProfileRepository.kt
│
├── ui/
│   ├── splash/   (SplashScreen.kt + SplashViewModel.kt)
│   ├── intro/    (IntroScreen.kt + IntroViewModel.kt)
│   ├── onboarding/ (OnboardingScreen.kt + ViewModel)
│   ├── home/     (HomeScreen.kt + ViewModel + components/)
│   ├── memories/ (4 ملفات + components/)
│   ├── letters/  (3 ملفات)
│   ├── drawing/  (DrawingScreen.kt + ViewModel + DrawingCanvas.kt)
│   ├── countdowns/ (3 ملفات)
│   ├── story/    (OurStoryScreen.kt + ViewModel)
│   ├── mood/     (MoodScreen.kt + ViewModel)
│   ├── wishes/   (WishesScreen.kt + ViewModel)
│   ├── quiz/     (LoveQuizScreen.kt + ViewModel)
│   ├── music/    (MusicScreen.kt + ViewModel)
│   ├── cards/    (CardsScreen.kt + ViewModel)
│   ├── settings/ (SettingsScreen.kt + ViewModel)
│   └── privacy/  (BiometricHelper.kt + LockScreen.kt)
│
├── workers/
│   ├── DailyQuoteWorker.kt
│   ├── CountdownWorker.kt
│   └── BootReceiver.kt
│
├── navigation/
│   ├── Screen.kt
│   └── AppNavigation.kt
│
└── di/
    └── AppModule.kt

res/
├── font/ (cairo_regular, cairo_bold, cairo_extra_bold, amiri_regular, great_vibes)
├── raw/  (lottie files + music)
├── values/ + values-ar/ (strings, themes, colors)
└── xml/  (file_paths.xml, backup_rules.xml)
```

---

## ═══════════════════════════════════════
## 🎯 تعليمات التنفيذ في Trae
## ═══════════════════════════════════════

```
▶ الخطوة 1: أنشئ مشروع Android جديد في Android Studio
  - Empty Activity (Compose)
  - Package: com.mawaai.love.app
  - Min SDK: 26

▶ الخطوة 2: في Trae، ابدأ بـ STAGE 0 (build.gradle.kts + Manifest)
  - اطلب من Trae كتابة كل ملف على حدة
  - تأكد من Sync قبل الانتقال للـ STAGE التالي

▶ الخطوة 3: Data Layer أولاً (STAGE 1)
  - Database → DAOs → Repositories
  - تأكد من Compile بدون أخطاء

▶ الخطوة 4: Core Components (STAGE 2)
  - ParticleHeartSystem أهمها ⭐

▶ الخطوة 5: Screens بالترتيب (STAGE 3-12)
  - ابدأ بـ Splash → Intro → Home → بقية الشاشات
  - اختبر كل شاشة قبل الانتقال للتالية

▶ الخطوة 6: Navigation + DI (STAGE 14-15)

▶ الخطوة 7: Workers + Notifications (STAGE 13)

▶ الخطوة 8: الموارد + Themes (STAGE 17)

▶ الخطوة 9: MainActivity + Final (STAGE 18)

▶ ملاحظة مهمة:
  كل مرة تطلب من Trae ملفاً، قل له:
  "اكتب [اسم الملف].kt الكامل بدون اختصار،
   استخدم MawaaiColors وMawaaiTheme من الـ core/theme
   اتبع مواصفات البروموت"
```

---

> **صُنع بكل الحب لأجمل هدية — رزان 💍**
> *هذا البروموت شامل لكل تفاصيل تطبيق مواعي — اتبع المراحل بالترتيب وستحصل على تطبيق احترافي من الفئة الأولى*
