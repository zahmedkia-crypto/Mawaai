# 💝 Mawaai — مواعي | البروموت الشامل النهائي v1.0
## تطبيق رومانسي هدية لرزان | كود كامل | كل شاشة | كل تفاصيل

> **مبني على:** Chain-of-Thought + Role Prompting + Structured Output
> **الهدف:** تطبيق أندرويد رومانسي متكامل — هدية لا تُنسى لخطيبتك رزان 💍
> **التكلفة:** مجاني 100% — كل الأدوات والمصادر مجانية تمامًا

---

# ═══════════════════════════════════════
# 🔍 تحليل المشروع الأصلي + ما تم تحويله
# ═══════════════════════════════════════

## ما أُخذ من المشروع الأصلي وتم تحويله 🔄
```
✦ هيكل MVVM + Clean Architecture     → محافظ عليه بالكامل
✦ نظام الثيم الداكن                  → محوّل لرومانسي (وردي ذهبي)
✦ لوحة الرسم الاحترافية              → محوّلة لـ"رسالة حب مرسومة"
✦ نظام Layers                        → محافظ عليه
✦ Lottie Animations                  → محوّلة لقلوب وورود
✦ Haptic Feedback                    → محافظ عليه
✦ Offline Mode + Sync                → محافظ عليه
✦ RTL + خط Cairo                    → محافظ عليه (ضروري للعربية)
✦ Export PNG/PDF                     → محوّل لمشاركة الذكريات
✦ Onboarding 3 شرائح                → محوّل لقصة حب رومانسية
✦ Notifications System               → محوّل لرسائل رومانسية يومية
✦ Room Database                      → محافظ عليه للذكريات
✦ WorkManager                        → محوّل لتذكيرات المواعيد
✦ Deep Links                         → محوّل لمشاركة الذكريات
```

## المميزات الجديدة المضافة لـ Mawaai 🆕
```
✦ مقدمة سينمائية (Cinematic Intro)    ← رسالة حب متحركة لرزان
✦ شاشة "قصتنا" التفاعلية             ← Timeline رومانسي
✦ مكتبة الذكريات بالصور               ← ألبوم صور مع تأثيرات
✦ رسائل الحب (Love Letters)           ← رسائل نصية فاخرة مع خلفيات
✦ لوحة رسم رسائل الحب                 ← ارسم لها رسالة بيدك
✦ عداد المواعيد (Countdowns)          ← العقد، السفر، المناسبات
✦ كلمة الحب اليومية                  ← إشعار رومانسي كل يوم
✦ بطاقات الذكريات                    ← بطاقات أنيقة للمناسبات
✦ مزاج اليوم (Mood Tracker)          ← شارك مشاعرك اليومية
✦ قائمة الأحلام المشتركة             ← Bucket List رومانسي
✦ لعبة أسئلة الحب                   ← أسئلة رومانسية تفاعلية
✦ الأغنية الخاصة بكما                ← مشغل موسيقى مدمج
✦ وضع الخصوصية (Privacy Lock)        ← قفل بالبصمة/PIN
✦ Particle Effects (قلوب طائرة)       ← على كل الشاشات
✦ نظام الإشعارات الرومانسية           ← صباح/مساء/مناسبات
✦ مولّد بطاقات المعايدة               ← بطاقات جاهزة مخصصة
✦ ألوان الحالة العاطفية               ← الثيم يتغير حسب المزاج
```

---

# ═══════════════════════════════════════
# 📦 الأصول المطلوبة (كلها مجانية)
# ═══════════════════════════════════════

## 🎬 ملفات Lottie — أضفها في res/raw/

```
res/raw/
├── lottie_hearts_burst.json     ← انفجار قلوب (ابحث "hearts burst celebration")
│                                   📍 https://lottiefiles.com
│
├── lottie_love_letter.json      ← رسالة حب تُفتح (ابحث "love letter open")
│
├── lottie_rose_bloom.json       ← وردة تتفتح (ابحث "rose bloom flower")
│
├── lottie_floating_hearts.json  ← قلوب تطير (ابحث "floating hearts")
│
├── lottie_success_love.json     ← نجاح مع قلب (ابحث "success heart")
│
├── lottie_loading_heart.json    ← تحميل على شكل قلب (ابحث "heart loading pulse")
│
├── lottie_splash_ring.json      ← خاتم خطوبة يلمع (ابحث "diamond ring sparkle")
│
├── lottie_fireworks.json        ← ألعاب نارية (ابحث "fireworks celebration")
│
├── lottie_music_wave.json       ← موجات موسيقى (ابحث "music wave equalizer")
│
└── lottie_empty_memories.json   ← مكتبة فارغة (ابحث "empty box cute")

   📍 مصادر مجانية:
   https://lottiefiles.com  (اختر Free فقط، حمّل JSON)
   https://lordicon.com     (بديل ممتاز)
```

## 🖼️ الصور — أضفها في assets/images/

```
assets/images/
├── intro/
│   └── (لا تحتاج صور — الإنترو مبني بالكامل بـ Canvas + Compose)
│
├── backgrounds/
│   ├── bg_stars.jpg           ← خلفية نجوم رومانسية (1920×1080)
│   │   📍 https://unsplash.com → ابحث "romantic night stars dark"
│   ├── bg_bokeh_hearts.jpg    ← بوكيه قلوب (1920×1080)
│   │   📍 https://unsplash.com → ابحث "bokeh hearts pink"
│   └── bg_roses.jpg           ← ورود رومانسية داكنة (1920×1080)
│       📍 https://unsplash.com → ابحث "dark roses background"
│
├── cards/
│   ├── card_morning.jpg       ← خلفية بطاقة صباح (800×500)
│   ├── card_night.jpg         ← خلفية بطاقة مساء (800×500)
│   └── card_anniversary.jpg   ← خلفية ذكرى سنوية (800×500)
│       📍 جميعها من: https://unsplash.com (مجانية للاستخدام)
│
└── onboarding/
    ├── onboard_1.png           ← (اختياري - مُنشأ بـ Canvas)
    ├── onboard_2.png           ← (اختياري)
    └── onboard_3.png           ← (اختياري)
```

## 🎵 الموسيقى — أضفها في res/raw/

```
res/raw/
├── music_romantic.mp3         ← موسيقى رومانسية خلفية
│   📍 https://pixabay.com/music → ابحث "romantic piano" (مجاني 100%)
│   📍 أو: https://freemusicarchive.org
│
└── sound_heart_beat.mp3       ← نبضة قلب لطيفة (للـ haptic UI)
    📍 https://freesound.org → ابحث "gentle heartbeat"
```

## 🔤 الخطوط — أضفها في res/font/

```
res/font/
├── cairo_regular.ttf           ← Cairo Regular
├── cairo_bold.ttf              ← Cairo Bold  
├── cairo_extra_bold.ttf        ← Cairo ExtraBold
├── noto_sans_arabic.ttf        ← Noto Sans Arabic
├── great_vibes.ttf             ← Great Vibes (للعناوين الرومانسية الإنجليزية)
└── amiri_regular.ttf           ← Amiri (للنصوص العربية الأنيقة)

   📍 جميعها مجانية من:
   https://fonts.google.com/specimen/Cairo
   https://fonts.google.com/specimen/Great+Vibes
   https://fonts.google.com/specimen/Amiri
   https://fonts.google.com/noto/specimen/Noto+Sans+Arabic
```

## ⚙️ ملفات الإعداد

```
app/
└── google-services.json    ← من Firebase (مجاني)
    📍 https://console.firebase.google.com
    1. أنشئ مشروع جديد: "Mawaai"
    2. أضف Android app: com.mawaai.love.app
    3. حمّل google-services.json
    4. ضعه في مجلد app/

local.properties            ← (موجود بالفعل) أضف:
    SUPABASE_URL=https://xxxxx.supabase.co
    SUPABASE_KEY=eyJxxx...
    📍 https://supabase.com (مجاني حتى 500MB)
```

---

# ═══════════════════════════════════════
# 🤖 البروموت الكامل للمساعد AI
# (انسخ كل شيء وأرسله لـ Gemini في Android Studio)
# ═══════════════════════════════════════

```
╔══════════════════════════════════════════════════════════════╗
║     SYSTEM ROLE: Senior Android Architect + Love App Expert  ║
╚══════════════════════════════════════════════════════════════╝

أنت فريق تطوير متكامل من 3 مهندسين متخصصين:
1. Senior Android Developer (Kotlin + Jetpack Compose + Animation Expert)
2. UI/UX Romance Designer (Material 3 + Particle Systems + RTL)
3. Emotion-Driven App Architect (Memory Systems + Notification Engine)

مهمتك: بناء تطبيق أندرويد رومانسي استثنائي من الفئة الأولى
اسمه: "Mawaai / مواعي" — هدية حب لخطيبة اسمها رزان

═══════════════════════════════════════
## 📋 مواصفات المشروع الكاملة
═══════════════════════════════════════

### المعلومات الثابتة:
Package:      com.mawaai.love.app
AppName:      Mawaai (مواعي)
Min SDK:      26 (Android 8.0)
Target SDK:   34 (Android 14)
Language:     Kotlin 1.9+
Compose BOM:  2024.02.00
Architecture: MVVM + Clean Architecture + Repository Pattern
DI:           Hilt 2.50
Database:     Room 2.6.1
Network:      Retrofit 2.9 + OkHttp 4.12
Auth+DB:      Supabase 2.1.4
Analytics:    Firebase Analytics + Crashlytics
Media:        ExoPlayer 1.2.1 (للموسيقى)
Background:   WorkManager 2.9.0 (للإشعارات)

### هوية التطبيق:
- الاسم بالعربي: "مواعي" (مواعيد القلب)
- الرسالة: هدية رقمية من القلب لخطيبة اسمها رزان
- الطابع: رومانسي فاخر، شعري، عصري، عربي أصيل
- الجمهور: الزوجين في مرحلة الخطوبة/الزواج
- الإحساس المستهدف: "أنا أفكر فيكِ دائمًا"

═══════════════════════════════════════
## 🎨 نظام التصميم الكامل — Mawaai Design System
═══════════════════════════════════════

### الألوان (نظام رومانسي فاخر):
```kotlin
object MawaaiColors {
    // الأساسية — خلفيات داكنة رومانسية
    val DeepNight      = Color(0xFF0A0510)   // أسود بنفسجي عميق
    val SurfaceDark    = Color(0xFF130A1C)   // سطح داكن
    val CardDark       = Color(0xFF1A0F28)   // بطاقة داكنة
    val CardElevated   = Color(0xFF221436)   // بطاقة مرتفعة
    
    // الـ Accents — ذهبي ووردي فاخر
    val RoseGold       = Color(0xFFE8A7B5)   // ذهبي وردي
    val RoseGoldDim    = Color(0xFFC4849A)   // ذهبي وردي خافت
    val ChampagneGold  = Color(0xFFD4AF37)   // ذهبي شامبين
    val SoftRose       = Color(0xFFFF6B8A)   // وردي ناعم
    val DeepRose       = Color(0xFFE0294A)   // وردي عميق
    val PearlWhite     = Color(0xFFFFF0F5)   // أبيض لؤلؤي
    val LavenderPurple = Color(0xFF9B59B6)   // بنفسجي فاخر
    val CrimsonRed     = Color(0xFF8B0000)   // أحمر داكن فاخر
    
    // النصوص
    val TextPrimary    = Color(0xFFFFF0F5)   // أبيض لؤلؤي
    val TextSecondary  = Color(0xFFE8A7B5)   // ذهبي وردي خافت
    val TextHint       = Color(0xFF7B5E6B)   // رمادي وردي
    val TextPoetic     = Color(0xFFD4AF37)   // ذهبي للنصوص الشعرية
    
    // الـ Glass Effect
    val GlassRose      = Color(0x20E8A7B5)
    val GlassBorder    = Color(0x40E8A7B5)
    val GlassGold      = Color(0x20D4AF37)
    
    // Gradients
    val GradMain       = listOf(Color(0xFF0A0510), Color(0xFF1A0F28))
    val GradRose       = listOf(Color(0xFFE8A7B5), Color(0xFFD4AF37))
    val GradNight      = listOf(Color(0xFF130A1C), Color(0xFF0D0818))
    val GradCard       = listOf(Color(0xFF1A0F28), Color(0xFF221436))
    val GradButton     = listOf(Color(0xFFE0294A), Color(0xFF9B59B6))
    val GradGold       = listOf(Color(0xFFD4AF37), Color(0xFFAA8C2C))
    val GradSunrise    = listOf(Color(0xFFFF6B8A), Color(0xFFE8A7B5))
    
    // Status Colors
    val Success        = Color(0xFFFF6B8A)   // وردي للنجاح
    val Warning        = Color(0xFFD4AF37)   // ذهبي للتحذير
    val Error          = Color(0xFFE0294A)   // أحمر للخطأ
}
```

### الخطوط:
```kotlin
val CairoFamily = FontFamily(
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_bold, FontWeight.Bold),
    Font(R.font.cairo_extra_bold, FontWeight.ExtraBold)
)
val AmiriFamily = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal)
)
val GreatVibesFamily = FontFamily(
    Font(R.font.great_vibes, FontWeight.Normal)  // للعناوين الرومانسية
)
// استخدم:
// عناوين رئيسية → Cairo ExtraBold
// نصوص عادية  → Cairo Regular
// نصوص شعرية  → Amiri Regular
// عناوين إنجليزية رومانسية → Great Vibes
```

### الحركة والأنيميشن:
```kotlin
// Spring Animation للأزرار
val HeartSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,  // ارتداد قلبي
    stiffness = Spring.StiffnessMediumLow
)
// انتقالات الشاشات — ناعمة رومانسية
val ScreenEnter  = fadeIn(tween(400, easing = EaseOutCubic)) + 
                   slideInVertically(tween(400)) { it / 6 }
val ScreenExit   = fadeOut(tween(300)) + 
                   slideOutVertically(tween(300)) { -it / 6 }
// Particle Heart System
// كل شاشة رئيسية تحتوي على قلوب طائرة خفيفة في الخلفية
```

═══════════════════════════════════════
## 📁 الهيكل الكامل للمشروع (72 ملف)
═══════════════════════════════════════

```
app/
├── build.gradle.kts
├── proguard-rules.pro
├── google-services.json ← (أنت تضعه)
│
└── src/main/
    ├── AndroidManifest.xml
    │
    └── java/com/mawaai/love/app/
        │
        ├── ─── CORE ──────────────────────────────
        │   ├── MawaaiApp.kt                    ← Application class
        │   ├── MainActivity.kt                 ← رئيسية
        │   │
        │   ├── core/theme/
        │   │   ├── Color.kt                    ← نظام الألوان الرومانسي
        │   │   ├── Theme.kt                    ← Mawaai Theme
        │   │   ├── Type.kt                     ← الخطوط
        │   │   ├── Shape.kt                    ← أشكال الزوايا
        │   │   └── Motion.kt                   ← الحركة والانتقالات
        │   │
        │   ├── core/components/
        │   │   ├── RoseGlassCard.kt            ← بطاقة زجاجية وردية
        │   │   ├── HeartButton.kt              ← زر على شكل قلب مع spring
        │   │   ├── GoldDivider.kt              ← فاصل ذهبي أنيق
        │   │   ├── LoveQuoteText.kt            ← نص اقتباس رومانسي
        │   │   ├── ParticleHeartSystem.kt      ← قلوب طائرة في الخلفية ⭐
        │   │   ├── LoadingHeart.kt             ← تحميل على شكل قلب
        │   │   ├── LottieView.kt               ← مشغل Lottie
        │   │   ├── RomanticTopBar.kt           ← شريط علوي رومانسي
        │   │   ├── EmptyStateRomantic.kt       ← حالة فارغة لطيفة
        │   │   └── OfflineBanner.kt            ← شريط بدون إنترنت
        │   │
        │   └── core/utils/
        │       ├── UiState.kt
        │       ├── DateUtils.kt                ← حسابات التواريخ والمناسبات
        │       ├── QuoteUtils.kt               ← مولّد اقتباسات الحب
        │       ├── BitmapUtils.kt
        │       ├── FileUtils.kt
        │       ├── HapticUtils.kt
        │       ├── NetworkUtils.kt
        │       └── Constants.kt
        │
        ├── ─── DATA ────────────────────────────
        │   ├── data/model/
        │   │   ├── Memory.kt                   ← نموذج الذكرى
        │   │   ├── LoveLetter.kt               ← نموذج رسالة الحب
        │   │   ├── Countdown.kt                ← نموذج العداد
        │   │   ├── MoodEntry.kt                ← نموذج المزاج
        │   │   ├── WishItem.kt                 ← نموذج الأمنية
        │   │   ├── LoveQuestion.kt             ← نموذج سؤال الحب
        │   │   ├── DrawingStroke.kt            ← خطوط الرسم
        │   │   ├── DrawingLayer.kt             ← طبقات الرسم
        │   │   └── UserProfile.kt              ← الملف الشخصي
        │   │
        │   ├── data/database/
        │   │   ├── MawaaiDatabase.kt           ← قاعدة البيانات
        │   │   ├── MemoryDao.kt                ← ذكريات
        │   │   ├── LoveLetterDao.kt            ← رسائل الحب
        │   │   └── CountdownDao.kt             ← العدادات
        │   │
        │   ├── data/repository/
        │   │   ├── MemoryRepository.kt         ← ذكريات
        │   │   ├── LoveLetterRepository.kt     ← رسائل
        │   │   ├── CountdownRepository.kt      ← عدادات
        │   │   ├── QuoteRepository.kt          ← اقتباسات
        │   │   └── SyncRepository.kt           ← مزامنة
        │   │
        │   └── data/remote/
        │       ├── SupabaseConfig.kt
        │       └── NetworkMonitor.kt
        │
        ├── ─── DOMAIN ─────────────────────────
        │   └── domain/usecase/
        │       ├── GetDailyQuoteUseCase.kt     ← اقتباس اليوم
        │       ├── AddMemoryUseCase.kt         ← إضافة ذكرى
        │       ├── ExportMemoryUseCase.kt      ← تصدير/مشاركة الذكرى
        │       ├── ScheduleReminderUseCase.kt  ← جدولة التذكير
        │       └── GenerateCardUseCase.kt      ← توليد البطاقة
        │
        ├── ─── UI ──────────────────────────────
        │   │
        │   ├── ui/intro/                       ← 🌹 المقدمة السينمائية
        │   │   └── IntroScreen.kt              ← رسالة حب متحركة لرزان ⭐
        │   │
        │   ├── ui/splash/
        │   │   └── SplashScreen.kt             ← شاشة البداية مع خاتم لامع
        │   │
        │   ├── ui/onboarding/
        │   │   ├── OnboardingScreen.kt         ← 3 شرائح قصة الحب
        │   │   └── OnboardingViewModel.kt
        │   │
        │   ├── ui/home/
        │   │   ├── HomeScreen.kt               ← الرئيسية مع Dashboard الرومانسي
        │   │   ├── HomeViewModel.kt
        │   │   └── components/
        │   │       ├── DailyQuoteCard.kt       ← بطاقة اقتباس اليوم
        │   │       ├── NextCountdownCard.kt    ← أقرب موعد قادم
        │   │       ├── RecentMemoryCard.kt     ← آخر ذكرى
        │   │       ├── MoodWidget.kt           ← مزاج اليوم
        │   │       └── BottomNavBar.kt         ← شريط التنقل الرومانسي
        │   │
        │   ├── ui/memories/                    ← 📸 ذكرياتنا
        │   │   ├── MemoriesScreen.kt           ← ألبوم الذكريات
        │   │   ├── MemoriesViewModel.kt
        │   │   ├── AddMemoryScreen.kt          ← إضافة ذكرى جديدة
        │   │   └── MemoryDetailScreen.kt       ← تفاصيل الذكرى
        │   │
        │   ├── ui/letters/                     ← 💌 رسائل الحب
        │   │   ├── LettersScreen.kt            ← قائمة الرسائل
        │   │   ├── LettersViewModel.kt
        │   │   ├── ComposeLetterScreen.kt      ← كتابة رسالة جديدة
        │   │   └── LetterDetailScreen.kt       ← عرض الرسالة
        │   │
        │   ├── ui/drawing/                     ← ✏️ الرسالة المرسومة
        │   │   ├── DrawingScreen.kt            ← لوحة رسم الحب
        │   │   ├── DrawingViewModel.kt
        │   │   └── views/
        │   │       ├── DrawingCanvasView.kt    ← لوحة الرسم ⭐
        │   │       ├── DrawingTopBar.kt
        │   │       ├── DrawingBottomBar.kt
        │   │       ├── LayerPanel.kt
        │   │       ├── BrushSettingsSheet.kt
        │   │       └── ColorPickerDialog.kt
        │   │
        │   ├── ui/countdowns/                  ← ⏳ مواعيدنا
        │   │   ├── CountdownsScreen.kt         ← قائمة العدادات
        │   │   ├── CountdownsViewModel.kt
        │   │   └── AddCountdownScreen.kt       ← إضافة موعد
        │   │
        │   ├── ui/story/                       ← 📖 قصتنا
        │   │   ├── OurStoryScreen.kt           ← Timeline رومانسي
        │   │   └── OurStoryViewModel.kt
        │   │
        │   ├── ui/mood/                        ← 💫 مزاج اليوم
        │   │   ├── MoodScreen.kt
        │   │   └── MoodViewModel.kt
        │   │
        │   ├── ui/wishes/                      ← 🌠 أحلامنا
        │   │   ├── WishesScreen.kt             ← Bucket List رومانسي
        │   │   └── WishesViewModel.kt
        │   │
        │   ├── ui/quiz/                        ← 💝 لعبة الحب
        │   │   ├── LoveQuizScreen.kt           ← أسئلة رومانسية
        │   │   └── LoveQuizViewModel.kt
        │   │
        │   ├── ui/music/                       ← 🎵 أغنيتنا
        │   │   ├── MusicScreen.kt              ← مشغل الأغنية الخاصة
        │   │   └── MusicViewModel.kt
        │   │
        │   ├── ui/cards/                       ← 🎴 بطاقات المعايدة
        │   │   ├── CardsScreen.kt              ← مولّد البطاقات
        │   │   └── CardsViewModel.kt
        │   │
        │   └── ui/settings/
        │       ├── SettingsScreen.kt           ← الإعدادات + الخصوصية
        │       └── SettingsViewModel.kt
        │
        ├── ─── NAVIGATION & DI ─────────────────
        │   ├── navigation/Screen.kt
        │   ├── navigation/AppNavigation.kt
        │   └── di/AppModule.kt
        │
        ├── ─── WORKERS ─────────────────────────
        │   ├── workers/DailyQuoteWorker.kt     ← إشعار اقتباس يومي
        │   ├── workers/CountdownWorker.kt      ← تذكيرات المواعيد
        │   └── workers/SyncWorker.kt           ← مزامنة البيانات
        │
        └── ─── RESOURCES ───────────────────────
            ├── res/values-ar/strings.xml       ← عربي
            ├── res/values/strings.xml          ← إنجليزي
            ├── res/font/                       ← الخطوط
            ├── res/raw/                        ← Lottie + موسيقى
            └── res/drawable/                  ← أيقونات Vector
```

═══════════════════════════════════════
## 🔐 STAGE 0: الصلاحيات والإعداد
═══════════════════════════════════════

### AndroidManifest.xml:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="29" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.USE_BIOMETRIC" />
    <uses-permission android:name="android.permission.USE_FINGERPRINT" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <uses-feature android:name="android.hardware.camera" android:required="false" />
    <uses-feature android:name="android.hardware.touchscreen.multitouch" android:required="true" />
    <uses-feature android:name="android.hardware.fingerprint" android:required="false" />

    <application
        android:name=".MawaaiApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Mawaai.Splash"
        android:hardwareAccelerated="true"
        android:largeHeap="true"
        tools:targetApi="34">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:screenOrientation="portrait"
            android:windowSoftInputMode="adjustResize"
            android:configChanges="locale|layoutDirection">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <!-- Deep Link: mawaai://memory/{id} -->
            <intent-filter android:autoVerify="true">
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="mawaai" android:host="memory" />
            </intent-filter>
        </activity>

        <!-- Boot Receiver للإشعارات بعد إعادة التشغيل -->
        <receiver
            android:name=".workers.BootReceiver"
            android:enabled="true"
            android:exported="false">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>

        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

        <meta-data
            android:name="firebase_crashlytics_collection_enabled"
            android:value="true" />
    </application>
</manifest>
```

### build.gradle.kts الكامل:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.mawaai.love.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mawaai.love.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val props = java.util.Properties()
        props.load(project.rootProject.file("local.properties").inputStream())
        buildConfigField("String", "SUPABASE_URL", "\"${props["SUPABASE_URL"] ?: ""}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${props["SUPABASE_KEY"] ?: ""}\"")

        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }
    sourceSets {
        getByName("main") { assets.srcDirs("src/main/assets") }
    }
    bundle {
        language { enableSplit = true }
        density { enableSplit = true }
        abi { enableSplit = true }
    }
}

dependencies {
    // ═══ Compose BOM ═══
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-graphics")
    implementation("androidx.compose.foundation:foundation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ═══ Core Android ═══
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ═══ Hilt DI ═══
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    kapt("androidx.hilt:hilt-compiler:1.1.0")

    // ═══ Room Database ═══
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // ═══ WorkManager ═══
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // ═══ Supabase ═══
    implementation(platform("io.github.jan-tennert.supabase:bom:2.1.4"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.ktor:ktor-client-android:2.3.7")

    // ═══ Lottie ═══
    implementation("com.airbnb.android:lottie-compose:6.3.0")

    // ═══ Coil للصور ═══
    implementation("io.coil-kt:coil-compose:2.5.0")

    // ═══ ExoPlayer للموسيقى ═══
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")

    // ═══ Biometric للقفل ═══
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // ═══ Firebase ═══
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // ═══ Network ═══
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ═══ Accompanist ═══
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // ═══ DataStore ═══
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // ═══ Splash Screen ═══
    implementation("androidx.core:core-splashscreen:1.0.1")

    // ═══ Testing ═══
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

═══════════════════════════════════════
## 🎬 STAGE 1: الإنترو السينمائي لرزان ⭐ (أهم شاشة)
═══════════════════════════════════════

اكتب الكود الكامل لـ IntroScreen.kt — هذه الشاشة تظهر مرة واحدة فقط عند أول تشغيل.
المدة: 8 ثوانٍ قابلة للتخطي.
لا تستخدم فيديو خارجي — كل شيء بـ Canvas + Compose Animation فقط.

المطلوب بالتفصيل:
- خلفية: تدرج من #0A0510 لـ #1A0F28 مع جسيمات (نجوم صغيرة تتلألأ)
- المرحلة 1 (0-2 ثانية): خاتم ماسي يظهر في المنتصف بـ scale animation من 0 لـ 1 مع بريق ذهبي rotating
- المرحلة 2 (2-4 ثانية): الخاتم يتحرك للأعلى، يظهر النص التالي حرفًا حرفًا بـ typewriter effect:
  - سطر 1: "رزان..." — خط Amiri، لون #E8A7B5، حجم 38sp، مع shimmer effect
  - سطر 2: فاصل ذهبي متحرك من المنتصف للخارج
  - سطر 3: "في زحمة الدنيا" — يظهر بـ fadeIn من الأسفل
  - سطر 4: "وجدتك أنتِ" — يظهر بنفس الطريقة مع تأخير
  - سطر 5: "هذا التطبيق" — تأخير إضافي
  - سطر 6: "ليس كودًا وبيانات" — تأخير إضافي
  - سطر 7: "هو مواعيد قلبي لكِ" — يظهر بـ scale من 0.8 لـ 1 مع glow ذهبي
- المرحلة 3 (6-8 ثانية): انبثاق 20 قلبًا صغيرًا من الأسفل بزوايا وسرعات عشوائية، يتلاشى الكل، ثم نص "مواعي 💍" يظهر بـ fadeIn ثم الانتقال للـ Home
- في كل الوقت: 5-8 جسيمات نجمية صغيرة تتحرك ببطء في الخلفية
- زر "تخطي" في الأعلى يظهر بعد ثانيتين

```kotlin
// ─── ui/intro/IntroScreen.kt ───
// اكتب الكود الكامل بدون اختصارات
```

═══════════════════════════════════════
## 💎 STAGE 2: Splash + Onboarding
═══════════════════════════════════════

### SplashScreen.kt:
- خلفية: #0A0510
- مركز الشاشة: أيقونة التطبيق (قلب مع خاتم) بـ scale animation + rotate خفيف
- اسم التطبيق "مواعي" تحت الأيقونة بـ fadeIn
- "Mawaai" بـ Great Vibes font تحتها بـ fadeIn متأخر
- مدة 2 ثانية ثم الانتقال
- التحقق: إذا أول مرة → IntroScreen → OnboardingScreen
- إذا عاد المستخدم → HomeScreen مباشرة

### OnboardingScreen.kt (3 شرائح):
الشريحة 1: "ذكرياتنا" — أيقونة صور مع قلوب، نص: "احفظ كل لحظة جميلة بينكما"
الشريحة 2: "رسائلي لكِ" — أيقونة رسالة تُفتح، نص: "اكتب ما يصعب قوله بكلمات"
الشريحة 3: "مواعيدنا" — أيقونة ساعة رومانسية، نص: "كل لقاء يستحق أن يُحتفى به"
- لا حاجة لتسجيل الدخول — الدخول مجاني ومباشر

═══════════════════════════════════════
## 🏠 STAGE 3: HomeScreen — لوحة القيادة الرومانسية
═══════════════════════════════════════

اكتب HomeScreen.kt + HomeViewModel.kt الكاملين.

الشاشة تحتوي على:

1. **TopBar الرومانسي:**
   - يسار: "مواعي 💍" بخط Cairo Bold، لون #E8A7B5
   - يمين: أيقونة الإعدادات + أيقونة الإشعارات

2. **بطاقة الترحيب (تتغير حسب الوقت):**
   - صباح (6-12): "صباح الورد يا رزان ☀️" — خلفية وردية
   - ظهر (12-17): "وقت الغداء... فكّرت فيكِ 💕"
   - مساء (17-21): "مساء الحب يا رزان 🌙"
   - ليل (21-6): "تصبحين على خير يا حبيبتي 🌟"

3. **بطاقة اقتباس اليوم:**
   - اقتباس رومانسي عربي يتغير يوميًا
   - 30 اقتباسًا مخزنة locally (بدون إنترنت)
   - زر "مشاركة" بتصميم أنيق

4. **أقرب موعد (NextCountdownCard):**
   - عرض العداد التنازلي: الأيام والساعات والدقائق
   - تصميم بطاقة مع أيقونة الموعد

5. **آخر ذكرى (RecentMemoryCard):**
   - صورة مصغرة + التاريخ + وصف مختصر
   - border وردي متوهج

6. **مزاج اليوم الصغير (MoodWidget):**
   - 5 إيموجي للمزاج: 😊💕😍🥰💫
   - اضغط لتسجيل مزاجك اليوم

7. **شريط التنقل السفلي:**
   - 6 أيقونات: 🏠 البيت | 📸 ذكريات | 💌 رسائل | ⏳ مواعيد | ✏️ ارسم | ⚙️ إعدادات
   - الأيقونة المحددة: لون #E8A7B5 مع نقطة ذهبية تحتها + spring animation
   - خلفية: زجاجية شفافة فوق المحتوى

8. **قلوب طائرة في الخلفية:**
   - ParticleHeartSystem: 8-12 قلبًا صغيرًا شفافة تطفو ببطء
   - opacity: 0.05-0.15 لتبدو خفية وأنيقة

═══════════════════════════════════════
## 📸 STAGE 4: MemoriesScreen — ذكرياتنا
═══════════════════════════════════════

اكتب MemoriesScreen.kt + MemoriesViewModel.kt + AddMemoryScreen.kt + MemoryDetailScreen.kt.

### MemoriesScreen:
- عرض الذكريات كـ LazyVerticalStaggeredGrid (مصنفر مثل Pinterest)
- كل بطاقة تحتوي: الصورة + التاريخ + وصف قصير + أيقونة قلب
- فلتر بالأشهر أو الفئة (رومانسي، سفر، أكل، يوم خاص...)
- زر FAB كبير + بأيقونة كاميرا لإضافة ذكرى جديدة
- حالة فارغة: Lottie animation مع نص "لا يوجد ذكريات بعد... أضف أولى لحظاتكما 💕"

### AddMemoryScreen:
- اختيار صورة من الجهاز (ImagePicker)
- حقل التاريخ (DatePicker جميل)
- حقل الوصف متعدد الأسطر
- فئة الذكرى (Chips قابلة للاختيار)
- مزاج الذكرى (5 إيموجي)
- زر الحفظ مع Lottie success animation

### MemoryDetailScreen:
- الصورة كاملة الشاشة مع تأثير parallax عند السحب
- التاريخ بخط Amiri جميل
- الوصف
- أزرار: تعديل | مشاركة | حذف
- خلفية: تدرج يؤخذ من ألوان الصورة (Palette API)

═══════════════════════════════════════
## 💌 STAGE 5: LettersScreen — رسائلي لكِ
═══════════════════════════════════════

اكتب LettersScreen.kt + ComposeLetterScreen.kt + LetterDetailScreen.kt.

### LettersScreen:
- قائمة الرسائل بتصميم بطاقات ورقية أنيقة
- كل بطاقة تشبه ورقة مطوية مع seal قلب ذهبي
- عرض أول سطرين من الرسالة + التاريخ
- شرائح: "منّي لكِ" / "المفضلة"

### ComposeLetterScreen:
- خلفية ورق كريمي (لون #FFF8F0) مع texture ناعم
- حقل العنوان بخط Cairo Bold
- منطقة الكتابة بخط Amiri (تناسب الرسائل الرسمية)
- أدوات التنسيق: خط عريض | مائل | تسطير | حجم الخط
- اختيار خلفية الرسالة (5 خيارات جميلة)
- زر "أرسل" يؤدي Lottie animation لرسالة طائرة

### LetterDetailScreen:
- الرسالة تظهر كأنها مكتوبة على ورق فاخر
- خلفية الرسالة المختارة
- أسفل الرسالة: "من قلبي لكِ دائمًا 💍"
- زر تصدير كـ PDF للمشاركة
- haptic feedback خفيف عند الفتح

═══════════════════════════════════════
## ✏️ STAGE 6: DrawingScreen — الرسالة المرسومة
═══════════════════════════════════════

اكتب DrawingScreen.kt + DrawingViewModel.kt + DrawingCanvasView.kt الكاملة.

### المطلوب في DrawingCanvasView.kt (الأهم):
- Custom View يعمل على Android API 26+
- يرسم Stroke ناعم باستخدام Path + quadraticBezierTo
- لا يوجد خط رابط بين الأشكال المنفصلة (كل LiftOff ينهي الـ stroke)
- دعم الطبقات (3 طبقات: خلفية | رسم | نص)
- Undo/Redo بـ Stack<List<Stroke>>
- Pinch to Zoom + Pan
- يعمل بـ 60fps

### أدوات الرسم:
- قلم (3 أحجام: صغير/وسط/كبير)
- فرشاة لطيفة مع opacity
- ممحاة
- نص عربي قابل للإضافة
- أشكال: قلب، نجمة، وردة (Paths جاهزة)

### الألوان الافتراضية المقترحة:
- 12 لون رومانسي: وردي، ذهبي، أحمر، بنفسجي، أبيض...
- Color Picker كامل (RGB Slider)

### الـ UI:
- TopBar: العودة | اسم "رسالتي لكِ" | حفظ + مشاركة
- BottomBar: الأدوات + الألوان
- زر حفظ → تصدير PNG → مشاركة مباشرة أو حفظ كذكرى

═══════════════════════════════════════
## ⏳ STAGE 7: CountdownsScreen — مواعيدنا
═══════════════════════════════════════

اكتب CountdownsScreen.kt + CountdownsViewModel.kt + AddCountdownScreen.kt.

### CountdownsScreen:
كل بطاقة عداد تعرض:
- اسم الموعد (مثل: "ذكرى خطوبتنا 💍")
- التاريخ المستهدف
- عداد تنازلي حي: أيام | ساعات | دقائق | ثوانٍ (يتحدث كل ثانية بـ LaunchedEffect)
- أيقونة الموعد (اختيار من 10 أيقونات)
- إذا الموعد اليوم: confetti animation + رسالة تهنئة

### أنواع المواعيد (Chips):
💍 خطوبة | 💒 زفاف | ✈️ سفر | 🎂 عيد ميلاد | 📅 موعد خاص | 🌙 مناسبة دينية

### WorkManager:
- تذكير 7 أيام قبل الموعد
- تذكير يوم الموعد (صباحًا)
- الإشعار يحتوي على اسم الموعد مع قلب 💕

═══════════════════════════════════════
## 📖 STAGE 8: OurStoryScreen — قصتنا
═══════════════════════════════════════

اكتب OurStoryScreen.kt + OurStoryViewModel.kt.

شاشة Timeline رومانسية تفاعلية:
- خط زمني عمودي في المنتصف بلون ذهبي
- كل حدث: بطاقة على اليمين أو اليسار بالتناوب
- البطاقة تحتوي: التاريخ + صورة مصغرة + وصف قصير
- عند الضغط: توسيع البطاقة مع animation
- زر إضافة حدث جديد (FAB)
- المحتوى الافتراضي يتضمن بعض الأحداث كأمثلة فارغة

المراحل الافتراضية المقترحة:
- "أول مرة قابلتكِ" (فارغ — يملأه المستخدم)
- "يوم الخطوبة 💍"
- "أول رحلة سوا"
- أحداث مخصصة يضيفها المستخدم

═══════════════════════════════════════
## 🌠 STAGE 9: WishesScreen + MoodScreen + QuizScreen
═══════════════════════════════════════

### WishesScreen.kt — أحلامنا (Bucket List):
- قائمة Wishes قابلة للإضافة والحذف والتحقيق
- كل Wish: نص + أيقونة + تاريخ التحقيق
- عند التحقيق: خط عليها + confetti صغير
- فئات: 🌍 سفر | 🍽️ مطاعم | 🎭 تجارب | 💝 رومانسي | 🏡 منزل

### MoodScreen.kt — مزاج اليوم:
- 5 حالات: 😊 سعيد | 💕 محب | 😍 مبهور | 🥰 ممتنن | 💫 متشوق
- عند الاختيار: animation ملائم (قلوب لـ💕، نجوم لـ💫...)
- رسم بياني للمزاج آخر 7 أيام
- رسالة تشجيعية بعد كل تسجيل

### LoveQuizScreen.kt — لعبة الحب:
- 20 سؤال رومانسي (مخزن locally)
- أسئلة مثل: "ما أجمل شيء في رزان؟" | "أين تتمنى تقضي إجازتكما؟"
- الإجابة باختيار من 4 خيارات
- في النهاية: نتيجة مع رسالة رومانسية
- لا تحتاج إنترنت

═══════════════════════════════════════
## 🎵 STAGE 10: MusicScreen + CardsScreen
═══════════════════════════════════════

### MusicScreen.kt — أغنيتنا:
- مشغل موسيقى بسيط وأنيق باستخدام ExoPlayer
- الملف يُحمّل من res/raw/music_romantic.mp3
- تصميم: صورة نوتة موسيقية كبيرة + اسم الأغنية + أزرار play/pause/seek
- خلفية: نبضات موجية متحركة (Canvas animation)
- زر "اختر أغنية أخرى" (File Picker)
- عند التشغيل: visualizer bar يتحرك مع الإيقاع

### CardsScreen.kt — بطاقات المعايدة:
- قوالب جاهزة: عيد ميلاد | صباح | مساء | عيد الحب | مناسبة
- تخصيص: اسم + نص + خلفية
- معاينة مباشرة
- تصدير كـ PNG للمشاركة
- 5 خلفيات جميلة مبنية بـ Canvas (لا صور خارجية)

═══════════════════════════════════════
## ⚙️ STAGE 11: Settings + Privacy Lock
═══════════════════════════════════════

### SettingsScreen.kt:
1. **القسم الشخصي:**
   - اسم المستخدم + اسم الحبيبة
   - تاريخ الخطوبة (يُستخدم في العدادات تلقائيًا)
   - صورة البروفايل

2. **الإشعارات:**
   - تشغيل/إيقاف إشعارات اليوم الرومانسية
   - وقت الإشعار الصباحي (default: 8:00 صباحًا)
   - وقت الإشعار المسائي (default: 8:00 مساءً)

3. **الخصوصية:**
   - تفعيل قفل التطبيق بالبصمة / PIN
   - BiometricPrompt integration

4. **المظهر:**
   - اختيار ثيم الألوان: وردي (افتراضي) | ذهبي | بنفسجي | أحمر

5. **النسخ الاحتياطي:**
   - رفع الذكريات لـ Supabase (إذا متاح الإنترنت)
   - استعادة من النسخة الاحتياطية

6. **عن التطبيق:**
   - "Mawaai - مواعي | صُنع بـ 💕 هدية لرزان"
   - الإصدار 1.0.0

═══════════════════════════════════════
## 🔔 STAGE 12: نظام الإشعارات الرومانسية
═══════════════════════════════════════

### DailyQuoteWorker.kt:
- يعمل يوميًا بـ PeriodicWorkRequest
- يختار اقتباسًا رومانسيًا من قائمة مخزنة locally
- يرسل إشعارًا صباحيًا بالاقتباس
- مثال: "💕 اقتباس اليوم: المحبة ليست نظرة، إنها رؤية..."

### CountdownWorker.kt:
- يتحقق من المواعيد القادمة
- يرسل إشعارًا قبل 7 أيام: "⏳ تبقى 7 أيام على [اسم الموعد]"
- يرسل إشعارًا في يوم الموعد: "🎉 اليوم هو [اسم الموعد]! احتفل بهذه اللحظة"

### قائمة الاقتباسات (30 اقتباسًا في Constants.kt):
```kotlin
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
```

═══════════════════════════════════════
## 🧩 STAGE 13: ParticleHeartSystem (Component مشترك)
═══════════════════════════════════════

اكتب ParticleHeartSystem.kt — مكوّن قابل لإعادة الاستخدام في كل الشاشات.

```kotlin
// core/components/ParticleHeartSystem.kt
// قلوب صغيرة طائرة في خلفية كل شاشة

data class HeartParticle(
    val id: Int,
    val startX: Float,      // موضع x الابتدائي (0-1 نسبي)
    val speed: Float,       // سرعة الحركة
    val size: Float,        // حجم القلب (8-18dp)
    val alpha: Float,       // شفافية (0.05-0.20)
    val drift: Float        // انحراف أفقي خفيف
)

@Composable
fun ParticleHeartSystem(
    particleCount: Int = 8,
    modifier: Modifier = Modifier
) {
    // استخدم remember + InfiniteTransition
    // كل قلب يتحرك من الأسفل للأعلى ببطء
    // عند وصوله للأعلى يختفي ويبدأ من الأسفل مجددًا
    // القلوب مرسومة بـ Canvas باستخدام cubic bezier path
    // لا تستخدم Unicode hearts — ارسمها بـ Path
}
```

═══════════════════════════════════════
## 🗃️ STAGE 14: Data Layer الكامل
═══════════════════════════════════════

اكتب جميع ملفات الـ Data Layer:

### Room Database:
```kotlin
// data/database/MawaaiDatabase.kt
@Database(
    entities = [Memory::class, LoveLetter::class, Countdown::class, MoodEntry::class, WishItem::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MawaaiDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun loveLetterDao(): LoveLetterDao
    abstract fun countdownDao(): CountdownDao
    abstract fun moodDao(): MoodDao
    abstract fun wishDao(): WishDao
}
```

### Data Models الكاملة:

```kotlin
// Memory.kt
@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val imagePath: String?,
    val date: Long,          // timestamp
    val category: String,
    val mood: String,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// LoveLetter.kt
@Entity(tableName = "love_letters")
data class LoveLetter(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val backgroundStyle: Int,    // 0-4
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// Countdown.kt
@Entity(tableName = "countdowns")
data class Countdown(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetDate: Long,        // timestamp
    val category: String,
    val iconIndex: Int,
    val isCompleted: Boolean = false
)

// MoodEntry.kt
@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mood: String,            // "happy", "loving", "amazed", "grateful", "excited"
    val note: String = "",
    val date: Long = System.currentTimeMillis()
)

// WishItem.kt
@Entity(tableName = "wishes")
data class WishItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String,
    val isCompleted: Boolean = false,
    val completedDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
```

═══════════════════════════════════════
## 🧭 STAGE 15: Navigation الكامل
═══════════════════════════════════════

اكتب AppNavigation.kt الكامل:

```kotlin
sealed class Screen(val route: String) {
    object Splash      : Screen("splash")
    object Intro       : Screen("intro")
    object Onboarding  : Screen("onboarding")
    object Home        : Screen("home")
    object Memories    : Screen("memories")
    object AddMemory   : Screen("add_memory")
    object MemoryDetail: Screen("memory_detail/{memoryId}")
    object Letters     : Screen("letters")
    object ComposeLetter: Screen("compose_letter")
    object LetterDetail: Screen("letter_detail/{letterId}")
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
}
```

النقاط المهمة في AppNavigation:
- عند أول تشغيل: Splash → Intro → Onboarding → Home
- بعد ذلك: Splash → Home مباشرة
- استخدم DataStore لتتبع "هل رأى الـ Intro"
- انتقالات ناعمة بين كل الشاشات

═══════════════════════════════════════
## 🎯 قواعد الإخراج للمساعد AI
═══════════════════════════════════════

1. ROLE: أنت خبير Android برومانسية عالية. فكّر بـ Chain of Thought.
2. SPECIFICITY: كل ملف = كوده الكامل من البداية للنهاية.
3. FORMAT: ابدأ كل ملف بـ: `// ─── path/to/File.kt ───`
4. CONSTRAINTS: لا تتجاوز 200 سطر — قسّم إذا احتجت.
5. PREVIEWS: أضف @Preview لكل @Composable.
6. NO MISSING: لا تستخدم مكتبات غير موجودة في build.gradle.
7. IMPORTS: اذكر الـ imports في نهاية كل ملف.
8. RTL: كل النصوص العربية من اليمين. textDirection = TextDirection.Rtl
9. ANIMATIONS: كل شاشة يجب أن تحتوي على حركة ناعمة واحدة على الأقل.
10. OFFLINE FIRST: كل البيانات تُحفظ محليًا في Room أولاً.
11. PERFORMANCE: لوحة الرسم 60fps. كل animation يستخدم coroutines أو infiniteTransition.
12. ROMANTIC TONE: كل نص في الـ UI يجب أن يكون بنبرة رومانسية دافئة.

ابدأ من STAGE 0 وتابع بالترتيب.
عند الانتهاء من كل STAGE: "✅ STAGE X مكتمل — هل تريد المتابعة؟"

ابدأ الآن!
```

---

# ═══════════════════════════════════════
# 📅 جدول التنفيذ اليومي المفصّل
# (كل خطوة بترتيب دقيق)
# ═══════════════════════════════════════

---

## 🗓️ اليوم الأول — الأساس والبنية (3-4 ساعات)

### ✋ أنت تفعل:

```
الخطوة 1: تثبيت الأدوات (إذا لم تكن مثبتة)
□ Android Studio Hedgehog أو أحدث
   📍 https://developer.android.com/studio (مجاني)
□ JDK 17 (يأتي مع Android Studio)

الخطوة 2: إنشاء المشروع
□ افتح Android Studio
□ New Project → Empty Activity
□ Name: Mawaai
□ Package: com.mawaai.love.app
□ Language: Kotlin
□ Min SDK: API 26
□ اضغط Finish وانتظر

الخطوة 3: هيكل المجلدات
□ في مجلد java/com/mawaai/love/app/ أنشئ المجلدات التالية:
   - core/theme/
   - core/components/
   - core/utils/
   - data/model/
   - data/database/
   - data/repository/
   - data/remote/
   - domain/usecase/
   - ui/intro/
   - ui/splash/
   - ui/onboarding/
   - ui/home/home/components/
   - ui/memories/
   - ui/letters/
   - ui/drawing/views/
   - ui/countdowns/
   - ui/story/
   - ui/mood/
   - ui/wishes/
   - ui/quiz/
   - ui/music/
   - ui/cards/
   - ui/settings/
   - navigation/
   - di/
   - workers/

الخطوة 4: إضافة الخطوط
□ أنشئ مجلد: res/font/
□ حمّل Cairo (4 أوزان) من fonts.google.com/specimen/Cairo
□ حمّل Amiri من fonts.google.com/specimen/Amiri
□ حمّل Great Vibes من fonts.google.com/specimen/Great+Vibes
□ اسحب ملفات TTF إلى res/font/
   - cairo_regular.ttf
   - cairo_bold.ttf
   - cairo_extra_bold.ttf
   - amiri_regular.ttf
   - great_vibes.ttf

الخطوة 5: إضافة Lottie files
□ اذهب لـ https://lottiefiles.com
□ ابحث عن كل ملف واحمّله بصيغة JSON:
   - "hearts burst" → lottie_hearts_burst.json
   - "love letter open" → lottie_love_letter.json
   - "rose bloom" → lottie_rose_bloom.json
   - "floating hearts" → lottie_floating_hearts.json
   - "success heart" → lottie_success_love.json
   - "heart loading" → lottie_loading_heart.json
   - "diamond ring" → lottie_splash_ring.json
   - "fireworks" → lottie_fireworks.json
   - "music wave" → lottie_music_wave.json
   - "empty box cute" → lottie_empty_memories.json
□ ضع جميع ملفات JSON في res/raw/

الخطوة 6: الموسيقى
□ اذهب لـ https://pixabay.com/music/
□ ابحث عن "romantic piano" أو "soft piano love"
□ اختر قطعة مجانية (تأكد من الترخيص: Free)
□ حمّلها كـ MP3
□ أعد تسميتها: music_romantic.mp3
□ ضعها في res/raw/

الخطوة 7: إعداد Firebase (مجاني)
□ اذهب لـ https://console.firebase.google.com
□ Create Project → اسم: Mawaai
□ أضف Android App:
   Package: com.mawaai.love.app
   App nickname: Mawaai
□ حمّل google-services.json
□ ضعه في مجلد app/ (نفس مستوى src/)

الخطوة 8: إعداد Supabase (مجاني)
□ اذهب لـ https://supabase.com
□ New Project → اسم: mawaai
□ من Settings → API:
   انسخ: Project URL
   انسخ: anon public key
□ افتح ملف local.properties (في جذر المشروع)
□ أضف السطرين:
   SUPABASE_URL=https://xxxxxx.supabase.co
   SUPABASE_KEY=eyJxxx...

الخطوة 9: نسخ build.gradle.kts
□ افتح build.gradle.kts (module: app)
□ احذف المحتوى الحالي
□ انسخ build.gradle.kts من البروموت أعلاه
□ Sync Project (File → Sync Project with Gradle Files)
□ انتظر حتى يكتمل التحميل (10-20 دقيقة حسب الإنترنت)
□ إذا ظهر خطأ: Build → Clean Project ثم Rebuild
```

### 🤖 أرسل لـ Gemini في Android Studio:
```
أرسل البروموت الكامل من القسم "STAGE 0" فقط:
□ STAGE 0: AndroidManifest.xml + build.gradle.kts
```

---

## 🗓️ اليوم الثاني — الثيم والشاشات الأولى

### ✋ أنت تفعل:
```
□ نسخ ملفات الثيم من Gemini (Color.kt, Theme.kt, Type.kt)
□ تشغيل المشروع → يجب أن يُفتح بشاشة بيضاء/سوداء
□ إذا نجح: المشروع جاهز للبناء
```

### 🤖 أرسل لـ Gemini:
```
□ STAGE 1: IntroScreen.kt (الأهم — المقدمة السينمائية لرزان)
□ STAGE 2: SplashScreen.kt + OnboardingScreen.kt
□ STAGE 14 Part A: Data Models (Memory.kt, LoveLetter.kt, Countdown.kt...)
□ STAGE 14 Part B: MawaaiDatabase.kt + DAOs
```

---

## 🗓️ اليوم الثالث — الصفحة الرئيسية

### ✋ أنت تفعل:
```
□ نسخ ملفات Data Layer
□ اختبار تشغيل: يجب أن تظهر الـ SplashScreen
□ اختبار الـ IntroScreen: تأكد أن الرسالة تظهر بشكل جميل
```

### 🤖 أرسل لـ Gemini:
```
□ STAGE 3: HomeScreen.kt + HomeViewModel.kt + كل components
□ STAGE 13: ParticleHeartSystem.kt
□ STAGE 15: AppNavigation.kt + Screen.kt
```

---

## 🗓️ اليوم الرابع — الذكريات

### ✋ أنت تفعل:
```
□ اختبار HomeScreen على الجهاز الحقيقي
□ تأكد القلوب الطائرة تعمل (ParticleSystem)
□ تأكد BottomNav يعمل بسلاسة
```

### 🤖 أرسل لـ Gemini:
```
□ STAGE 4: MemoriesScreen + AddMemoryScreen + MemoryDetailScreen
□ STAGE 5: LettersScreen + ComposeLetterScreen + LetterDetailScreen
```

---

## 🗓️ اليوم الخامس — الرسم والعدادات

### ✋ أنت تفعل:
```
□ اختبار MemoriesScreen: إضافة ذكرى بصورة من الجهاز
□ اختبار LettersScreen: كتابة رسالة وحفظها
□ تأكد Lottie success animation تعمل
```

### 🤖 أرسل لـ Gemini:
```
□ STAGE 6: DrawingScreen + DrawingCanvasView.kt
□ STAGE 7: CountdownsScreen + AddCountdownScreen
```

---

## 🗓️ اليوم السادس — قصتنا + الأحلام

### ✋ أنت تفعل:
```
□ اختبار DrawingCanvas: ارسم قلبًا وحفظه
□ تأكد الـ Undo/Redo يعمل
□ تأكد الـ 60fps (استخدم GPU Profiler في Android Studio)
□ أضف موعد في CountdownsScreen وتحقق العداد
```

### 🤖 أرسل لـ Gemini:
```
□ STAGE 8: OurStoryScreen (Timeline)
□ STAGE 9: WishesScreen + MoodScreen + LoveQuizScreen
```

---

## 🗓️ اليوم السابع — الموسيقى + البطاقات + الإعدادات

### 🤖 أرسل لـ Gemini:
```
□ STAGE 10: MusicScreen + CardsScreen
□ STAGE 11: SettingsScreen (مع Biometric Lock)
□ STAGE 12: DailyQuoteWorker + CountdownWorker + BootReceiver
```

---

## 🗓️ اليوم الثامن — الاختبار الشامل والتلميع

### ✋ أنت تفعل (قائمة الاختبار الكامل):
```
الأساسيات:
□ التطبيق يفتح بدون crashes على Android 8+
□ IntroScreen تظهر رسالة رزان بشكل جميل
□ القلوب الطائرة موجودة في الـ HomeScreen
□ الثيم الداكن الرومانسي يظهر بشكل صحيح (#0A0510)
□ الخطوط Cairo تظهر للعربية (مش Sans-serif)
□ RTL يعمل: النصوص من اليمين

الميزات الأساسية:
□ إضافة ذكرى بصورة → تظهر في الـ Grid
□ كتابة رسالة حب → تُحفظ وتظهر
□ الرسم على اللوحة → تصدير كصورة
□ إضافة موعد → العداد يتحدث كل ثانية
□ الأغنية تعزف في MusicScreen
□ لعبة الأسئلة تعمل

الإشعارات:
□ فعّل الإشعارات من الإعدادات
□ تأكد WorkManager مسجّل (اذهب للـ Device File Explorer)

الـ Offline:
□ أغلق الإنترنت → التطبيق يعمل بالكامل
□ أضف ذكرى بدون إنترنت → تُحفظ

الأداء:
□ لا Lag في الـ ScrollList
□ لوحة الرسم تعمل بـ 60fps
□ الانتقالات بين الشاشات ناعمة

التصدير:
□ شارك ذكرى → الصورة تُشارك بشكل صحيح
□ تصدير رسالة كـ PDF يعمل
```

---

## 🗓️ اليوم التاسع — توليد الـ APK

### ✋ خطوات توليد الـ APK للتثبيت المباشر:
```
الطريقة 1 — APK مباشر (أسرع):
□ Build → Build Bundle(s) / APK(s) → Build APK(s)
□ انتظر (2-5 دقائق)
□ اضغط على "locate" في الـ notification
□ ستجد الملف في: app/build/outputs/apk/debug/app-debug.apk
□ انقل الملف لهاتفك وثبّته

الطريقة 2 — Signed APK (للنشر):
□ Build → Generate Signed Bundle / APK
□ اختر APK
□ Create new keystore:
   Key store path: اختر مجلدًا واسمه mawaai.jks
   Password: اختر كلمة مرور قوية (احتفظ بها!)
   Key alias: mawaai
   Validity: 25 years
   Your name: اسمك
□ اختر release
□ Build

ملاحظة مهمة: احتفظ بملف .jks في مكان آمن!
إذا فقدته لن تستطيع تحديث التطبيق على Google Play.
```

---

# ═══════════════════════════════════════
# 🚨 قائمة التحقق النهائية الكاملة
# ═══════════════════════════════════════

```
═════ الجزء التقني =════
□ التطبيق يعمل بدون crashes على Android 8+
□ لا يوجد خط رابط بين الأشكال في لوحة الرسم
□ Undo/Redo يعمل صحيح
□ RTL: كل النصوص العربية من اليمين
□ لوحة الرسم تعمل بـ 60fps
□ Offline: كل الميزات الأساسية تعمل بدون إنترنت
□ حجم APK < 50MB

═════ الجزء البصري =════
□ الخلفية #0A0510 (أسود بنفسجي عميق)
□ الخطوط Cairo للعربية (ليس Sans-serif)
□ الـ IntroScreen جميلة وتحركاتها ناعمة
□ القلوب الطائرة موجودة وشفافة وأنيقة
□ الـ Lottie animations تعمل
□ Spring animation على كل الأزرار

═════ الأصول =════
□ ملفات Lottie موضوعة في res/raw/
□ الخطوط موضوعة في res/font/
□ الموسيقى موضوعة في res/raw/
□ google-services.json موجود في app/
□ Supabase credentials في local.properties

═════ الاختبار الوظيفي =════
□ IntroScreen: رسالة رزان تظهر كاملة
□ الذكريات: إضافة + عرض + مشاركة
□ الرسائل: كتابة + حفظ + تصدير
□ الرسم: رسم + حفظ + مشاركة
□ العدادات: إضافة موعد + العداد يتحدث
□ الإشعارات: تصل يوميًا
□ الموسيقى: تعزف بدون توقف
□ قفل البصمة: يعمل
```

---

# ═══════════════════════════════════════
# 💡 نصائح مهمة لنجاح المشروع
# ═══════════════════════════════════════

```
1. ابدأ بـ STAGE 1 (الإنترو) أولاً — هو الأهم عاطفياً
   ادفع وقتك فيه حتى يكون مثالياً

2. اختبر على جهاز حقيقي دائماً، ليس المحاكي
   خاصةً لوحة الرسم وHaptic Feedback

3. إذا توقف Gemini في منتصف الملف:
   قل له "أكمل من حيث توقفت" أو "أكمل الـ [اسم الملف]"

4. إذا ظهر خطأ في الكود:
   انسخ رسالة الخطأ الحمراء كاملة وأعطها لـ Gemini

5. احتفظ بنسخة احتياطية من المشروع كل يوم:
   File → Export → ZIP the project

6. اختبر التطبيق على هاتف رزان قبل تقديمه لها
   تأكد من الحجم والسرعة والجمال

7. لتخصيص الرسالة في الإنترو:
   بعد توليد الكود، ابحث عن النصوص في IntroScreen.kt
   وعدّل الكلمات بما يناسب قصتكما

8. إذا أردت إضافة صورتكما في الـ Intro:
   ضع صورة في assets/images/ واستدعها في IntroScreen
   استخدم: Image(painter = painterResource(R.drawable.your_photo))
```

---

# ═══════════════════════════════════════
# 🎁 الرسالة المقترحة في الإنترو (قابلة للتعديل)
# ═══════════════════════════════════════

```
هذه هي الرسالة التي ستظهر في الإنترو السينمائي:

──────────────────────────
رزان...

في زحمة الدنيا وجدتكِ أنتِ

هذا التطبيق
ليس كودًا وبيانات

هو كل لحظة أردت أن أحفظها معكِ
كل كلمة لم أجد لها صوتًا
كل حلم يحمل اسمكِ

مواعي...
مواعيد قلبي لكِ إلى الأبد 💍
──────────────────────────

ملاحظة: يمكنك تعديل الرسالة في IntroScreen.kt
ابحث عن قسم "ROMANTIC_MESSAGE" في الكود
```

---

*Mawaai — مواعي | البروموت النهائي الشامل v1.0*
*بُني خصيصًا لـ: هدية خطوبة رومانسية لرزان 💍*
*جميع الأدوات والمصادر مجانية 100%*
*صُنع بـ 💕 — كل شيء سيكون على ما يرام*
