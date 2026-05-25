# DATA MODEL — Supabase → Room Mapping

Direct mapping of every Creative Studio table to a Room entity. Use for EPIC E8 (MT-040 / 041 / 042).

---

## 📋 Table-by-Table Mapping

### `template_category` ENUM → Kotlin enum

```kotlin
enum class TemplateCategory { HENNA, GARMENT, WALL, CERAMIC }
```

### `project_status` ENUM → Kotlin enum

```kotlin
enum class ProjectStatus { DRAFT, ANALYZING, ANALYZED, FAILED }
```

### `profiles` table — Skip
Not needed; the Android app already has its own auth/profile flow.

---

### `templates` table → `TemplateEntity`

```kotlin
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,                    // UUID
    val category: String,                           // TemplateCategory.name
    val name: String,
    val surfaceType: String,                        // e.g. "skin_palm"
    val description: String?,
    val referenceImageUrl: String?,                 // null on Android — assets/templates/...
    val assetPath: String,                          // NEW for Android: assets path
    val zonesJson: String,                          // serialized List<TemplateZone>
    val culturalRulesJson: String,                  // serialized Map<String, String>
    val traditionalPaletteJson: String,             // serialized List<String> (hex colors)
    val lightingProfile: LightingProfile,           // embedded
    val recommendedComplexity: String = "medium",
    val maxCoveragePct: Int = 75,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class LightingProfile(
    val primaryLight: String = "",
    val material: String = "",
    val surfaceReflectance: String = ""
)

data class TemplateZone(
    val id: String,
    val name: String,
    val quad: List<List<Float>>,                    // 4 [x,y] pairs normalized [0..1]
    val blend: String = "FABRIC_REALISTIC",
    val alpha: Float = 0.8f
)
```

---

### `projects` table → `ProjectEntity`

The TS schema has accumulated multiple migrations. Final state:

```kotlin
@Entity(
    tableName = "projects",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("templateId"),
        Index("status"),
        Index("isPublic"),
        Index("createdAt")
    ]
)
data class ProjectEntity(
    @PrimaryKey val id: String,                    // UUID
    val templateId: String,
    val title: String?,
    val sketchPath: String?,                        // local file path (vs Supabase storage URL)
    val sketchCapturedAt: Long?,

    // Phase 3 output
    val analysisJson: String?,                      // serialized SketchAnalysis
    // Phase 4 output
    val suggestionsJson: String?,                   // serialized List<Suggestion>
    val acceptedSuggestionIds: String,              // CSV (TEXT[] equivalent)

    // Phase 7
    val colorOverride: String?,                     // hex
    val templateColor: String?,                     // legacy alias

    // Phase 5 + 6 output
    val renderedPath: String?,
    val renderPrompt: String?,
    val renderedAt: Long?,
    val renderQualityJson: String?,                 // serialized RenderQuality

    // Phase 8 output
    val exportPath: String?,
    val exportedAt: Long?,
    val exportMockupId: String?,

    // Sharing (deferred — keep column for future)
    val isPublic: Boolean = false,

    val notes: String?,
    val status: String = "DRAFT",                   // ProjectStatus.name
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

---

### `product_mockups` table → `ProductMockupEntity`

```kotlin
@Entity(tableName = "product_mockups")
data class ProductMockupEntity(
    @PrimaryKey val id: String,                    // UUID
    val name: String,
    val category: String,                          // TemplateCategory.name
    val surfaceMatchCsv: String,                   // CSV of surface ids: "skin_palm,skin_hand_full"
    val scene: String,                             // descriptive scene prompt
    val lighting: String,
    val perspective: String,
    val accentColor: String,                       // hex
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
```

---

## 🛠 Type Converters

Single converter class registered on the database.

```kotlin
@ProvidedTypeConverter
class MawaaiTypeConverters @Inject constructor(
    private val gson: Gson
) {
    @TypeConverter fun lightingProfileToJson(v: LightingProfile): String = gson.toJson(v)
    @TypeConverter fun jsonToLightingProfile(s: String): LightingProfile =
        gson.fromJson(s, LightingProfile::class.java) ?: LightingProfile()
}
```

(Embed via `@Embedded` for `LightingProfile` if you prefer column-per-field instead of JSON.)

---

## 🗂 Seed Data

The Lovable seed includes **12 product mockups** that ship with the app. Embed them in a `MockupSeed.kt`:

```kotlin
object MockupSeed {
    val ALL: List<ProductMockupEntity> = listOf(
        // ─── Henna ───
        ProductMockupEntity(
            id = "mockup-henna-bridal-palm",
            name = "Bridal palm",
            category = "HENNA",
            surfaceMatchCsv = "skin_palm,skin_hand_full",
            scene = "A bride's open palm and fingers rested on a silk cushion, gold bangles at the wrist, soft bokeh of marigold petals behind",
            lighting = "warm golden-hour window light",
            perspective = "overhead 3/4",
            accentColor = "#b86b3a",
            sortOrder = 10
        ),
        ProductMockupEntity(
            id = "mockup-henna-foot-rug",
            name = "Foot pose on rug",
            category = "HENNA",
            surfaceMatchCsv = "skin_foot",
            scene = "A bare foot with anklet resting on a deep red Persian rug, traditional setting",
            lighting = "soft morning daylight",
            perspective = "side eye-level",
            accentColor = "#7a2b1f",
            sortOrder = 20
        ),
        ProductMockupEntity(
            id = "mockup-henna-closeup",
            name = "Henna close-up",
            category = "HENNA",
            surfaceMatchCsv = "skin_palm,skin_hand_full,skin_foot",
            scene = "Tight macro of freshly applied henna with subtle paste sheen on skin",
            lighting = "soft diffused studio light",
            perspective = "macro overhead",
            accentColor = "#6b3a2a",
            sortOrder = 30
        ),
        // ─── Garments ───
        ProductMockupEntity(
            id = "mockup-garment-abaya-flatlay",
            name = "Flat-lay abaya",
            category = "GARMENT",
            surfaceMatchCsv = "fabric_abaya",
            scene = "A flowing black abaya laid flat on a marble surface with gold thread shimmering, perfume bottle and pearls nearby",
            lighting = "soft north-window light",
            perspective = "overhead",
            accentColor = "#d4af37",
            sortOrder = 10
        ),
        ProductMockupEntity(
            id = "mockup-garment-thobe-hanger",
            name = "Thobe on hanger",
            category = "GARMENT",
            surfaceMatchCsv = "fabric_thobe",
            scene = "A crisp white thobe on a wooden hanger against a warm sandstone wall",
            lighting = "warm late-afternoon light",
            perspective = "front eye-level",
            accentColor = "#c9a87a",
            sortOrder = 20
        ),
        ProductMockupEntity(
            id = "mockup-garment-toub-shoulder",
            name = "Toub on model shoulder",
            category = "GARMENT",
            surfaceMatchCsv = "fabric_toub",
            scene = "A translucent Sudanese toub draped over a model's shoulder, soft folds catching light",
            lighting = "soft window light",
            perspective = "3/4 portrait",
            accentColor = "#b87a4a",
            sortOrder = 30
        ),
        // ─── Walls ───
        ProductMockupEntity(
            id = "mockup-wall-majlis",
            name = "Majlis wall",
            category = "WALL",
            surfaceMatchCsv = "wall_plaster,wall_stone",
            scene = "An interior majlis with low cushions, brass lantern, and a feature wall ready for ornament",
            lighting = "warm interior lantern + daylight mix",
            perspective = "wide eye-level",
            accentColor = "#a87a3a",
            sortOrder = 10
        ),
        ProductMockupEntity(
            id = "mockup-wall-arch-niche",
            name = "Carved arch niche",
            category = "WALL",
            surfaceMatchCsv = "wall_arch,wall_stone",
            scene = "A pointed arch niche in a sandstone wall with soft side shadow",
            lighting = "golden-hour raking light",
            perspective = "straight-on eye-level",
            accentColor = "#c08a4a",
            sortOrder = 20
        ),
        ProductMockupEntity(
            id = "mockup-wall-gallery",
            name = "Gallery plaster wall",
            category = "WALL",
            surfaceMatchCsv = "wall_plaster",
            scene = "A minimalist gallery wall in white lime plaster with subtle texture",
            lighting = "soft skylight",
            perspective = "eye-level",
            accentColor = "#d8c7a8",
            sortOrder = 30
        ),
        // ─── Ceramics ───
        ProductMockupEntity(
            id = "mockup-ceramic-mug",
            name = "Stoneware mug on linen",
            category = "CERAMIC",
            surfaceMatchCsv = "ceramic_mug",
            scene = "A stoneware mug on cream linen next to a sprig of dried thyme",
            lighting = "soft diffused morning light",
            perspective = "3/4 product",
            accentColor = "#3a5a4a",
            sortOrder = 10
        ),
        ProductMockupEntity(
            id = "mockup-ceramic-tile",
            name = "Tile in grid",
            category = "CERAMIC",
            surfaceMatchCsv = "ceramic_tile",
            scene = "A single hand-painted tile centered in a quiet grid of plain tiles on a wall",
            lighting = "soft side light",
            perspective = "straight-on",
            accentColor = "#2d5f7a",
            sortOrder = 20
        ),
        ProductMockupEntity(
            id = "mockup-ceramic-plate",
            name = "Display plate on table",
            category = "CERAMIC",
            surfaceMatchCsv = "ceramic_plate",
            scene = "A round display plate on a dark walnut table with brass accents",
            lighting = "warm spot + soft fill",
            perspective = "overhead",
            accentColor = "#8a4a2d",
            sortOrder = 30
        ),
    )
}
```

---

## 🔁 Migrations

```kotlin
// data/database/Migrations.kt

object MawaaiMigrations {
    /**
     * v1 → v2: add the Creative Studio integration tables + new project columns.
     * NON-DESTRUCTIVE — all existing user data preserved.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // New tables
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS templates (
                    id TEXT NOT NULL PRIMARY KEY,
                    category TEXT NOT NULL,
                    name TEXT NOT NULL,
                    surfaceType TEXT NOT NULL,
                    description TEXT,
                    referenceImageUrl TEXT,
                    assetPath TEXT NOT NULL,
                    zonesJson TEXT NOT NULL DEFAULT '[]',
                    culturalRulesJson TEXT NOT NULL DEFAULT '{}',
                    traditionalPaletteJson TEXT NOT NULL DEFAULT '[]',
                    primaryLight TEXT NOT NULL DEFAULT '',
                    material TEXT NOT NULL DEFAULT '',
                    surfaceReflectance TEXT NOT NULL DEFAULT '',
                    recommendedComplexity TEXT NOT NULL DEFAULT 'medium',
                    maxCoveragePct INTEGER NOT NULL DEFAULT 75,
                    sortOrder INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS projects (
                    id TEXT NOT NULL PRIMARY KEY,
                    templateId TEXT NOT NULL,
                    title TEXT,
                    sketchPath TEXT,
                    sketchCapturedAt INTEGER,
                    analysisJson TEXT,
                    suggestionsJson TEXT,
                    acceptedSuggestionIds TEXT NOT NULL DEFAULT '',
                    colorOverride TEXT,
                    templateColor TEXT,
                    renderedPath TEXT,
                    renderPrompt TEXT,
                    renderedAt INTEGER,
                    renderQualityJson TEXT,
                    exportPath TEXT,
                    exportedAt INTEGER,
                    exportMockupId TEXT,
                    isPublic INTEGER NOT NULL DEFAULT 0,
                    notes TEXT,
                    status TEXT NOT NULL DEFAULT 'DRAFT',
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(templateId) REFERENCES templates(id) ON DELETE RESTRICT
                )
            """.trimIndent())

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_projects_templateId ON projects(templateId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_projects_status ON projects(status)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_projects_isPublic ON projects(isPublic)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_projects_createdAt ON projects(createdAt)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS product_mockups (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    surfaceMatchCsv TEXT NOT NULL,
                    scene TEXT NOT NULL,
                    lighting TEXT NOT NULL,
                    perspective TEXT NOT NULL,
                    accentColor TEXT NOT NULL,
                    sortOrder INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    val ALL = arrayOf(MIGRATION_1_2)
}
```

Register on database:
```kotlin
Room.databaseBuilder(...)
    .addMigrations(*MawaaiMigrations.ALL)
    .addTypeConverter(typeConverters)
    .build()
```

---

## 📂 Storage Mapping (Supabase Buckets → Android File Paths)

Supabase has 4 storage buckets. On Android, use scoped storage equivalents:

| Bucket | Supabase access | Android equivalent |
|---|---|---|
| `templates` (public read) | Pre-bundled in `app/src/main/assets/templates/` | Already in your repo |
| `sketches` (private, owner-scoped) | `context.filesDir / "sketches"` | New |
| `renders` (private, owner-scoped) | `context.filesDir / "renders"` | New |
| `exports` (private, owner-scoped) | `MediaStore.Downloads` (so user can share/find) | New |

```kotlin
@Singleton
class MawaaiStorage @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    val sketches: File by lazy { File(ctx.filesDir, "sketches").apply { mkdirs() } }
    val renders: File by lazy { File(ctx.filesDir, "renders").apply { mkdirs() } }
    val tempMockups: File by lazy { File(ctx.cacheDir, "mockups").apply { mkdirs() } }

    fun sketchFor(projectId: String) = File(sketches, "$projectId.png")
    fun renderFor(projectId: String, ext: String = "png") = File(renders, "$projectId.$ext")
}
```

---

## ✅ Implementation Checklist for E8 (MT-040/041/042)

- [ ] All 3 entities have `@Entity`, `@PrimaryKey`, type converters where needed
- [ ] `MawaaiDatabase` `version = 2`, includes new entities, registers `MawaaiTypeConverters` and `MawaaiMigrations.ALL`
- [ ] `DatabaseModule` provides new DAOs
- [ ] Migration tested by writing test data in v1, upgrading, asserting data preserved
- [ ] `schemas/com.mawaai.love.app.data.database.MawaaiDatabase/2.json` is generated
- [ ] Each repository (Template, Project, ProductMockup) exposes Flow + suspend mutators only
- [ ] No raw SQLite access outside DAOs
- [ ] `MockupSeed.ALL` is inserted once on first launch (via `RoomDatabase.Callback.onCreate` or a `DataStore`-gated init worker)
