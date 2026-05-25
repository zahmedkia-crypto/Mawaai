# DATA MODEL — Supabase → Room Mapping

Complete port of the Creative Studio Supabase schema into Kotlin Room entities. Use this when implementing EPIC E8 (MT-040, MT-041, MT-042).

---

## 📊 Schema Inventory (Source: Supabase migrations)

| Supabase Table | Room Entity | Purpose | RLS in source |
|---|---|---|---|
| `profiles` | (skip — Android is single-user local) | User identity | self-only |
| `templates` | `TemplateEntity` | Template catalog with surface metadata | public read |
| `projects` | `ProjectEntity` | One per design session | owner-only |
| `product_mockups` | `ProductMockupEntity` | Product scene catalog (Phase 8) | public read |
| (storage: sketches) | `File` in `filesDir/sketches/` | Sketch images | owner-only |
| (storage: renders) | `File` in `filesDir/renders/` | Rendered images | owner-only |
| (storage: exports) | `MediaStore` (gallery) | Final composited exports | gallery API |
| (storage: templates) | `app/src/main/assets/templates/` | Template images (bundled) | n/a |

Two **denormalized JSON sub-documents** that live inside `projects`:
- `analysis` — full `SketchAnalysis` blob (Phase 3 output)
- `suggestions` — full `SuggestionsResponse` blob (Phase 4 output)

These can be stored as `TEXT NOT NULL` columns with Gson type converters, OR split into separate entities. Recommended: keep as JSON-on-disk for now (faster MT-040 delivery; refactor to split tables later if Room queries get awkward).

---

## 🏗 Room Entities

### TemplateEntity

```kotlin
// data/database/entities/TemplateEntity.kt

package com.mawaai.love.app.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val category: String,                  // "henna" | "garment" | "wall" | "ceramic"
    val name: String,
    @ColumnInfo(name = "surface_type")
    val surfaceType: String,               // "skin_palm" | "fabric_abaya" | ...
    val description: String? = null,
    @ColumnInfo(name = "reference_image_asset")
    val referenceImageAsset: String,       // assets/templates/<cat>/<file>
    @ColumnInfo(name = "zones_json")
    val zonesJson: String = "[]",          // serialized List<TemplateZone>
    @ColumnInfo(name = "cultural_rules_json")
    val culturalRulesJson: String = "{}",  // serialized Map<String, Any>
    @ColumnInfo(name = "traditional_palette_json")
    val traditionalPaletteJson: String = "[]",
    @ColumnInfo(name = "lighting_profile_json")
    val lightingProfileJson: String = "{}",
    @ColumnInfo(name = "recommended_complexity")
    val recommendedComplexity: String = "medium",
    @ColumnInfo(name = "max_coverage_pct")
    val maxCoveragePct: Int = 75,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now()
)

/** Strongly-typed model exposed by TemplateRepository (deserialized form). */
data class Template(
    val id: String,
    val category: TemplateCategory,
    val name: String,
    val surfaceType: String,
    val description: String?,
    val referenceImageAsset: String,
    val zones: List<TemplateZone>,
    val culturalRules: CulturalRules,
    val traditionalPalette: List<String>,
    val lightingProfile: LightingProfile,
    val recommendedComplexity: String,
    val maxCoveragePct: Int
)

enum class TemplateCategory { HENNA, GARMENT, WALL, CERAMIC }

data class TemplateZone(
    val id: String,
    val name: String,
    val quad: List<List<Float>>,    // [[x,y]*4] normalized
    val priority: Int = 0,
    val notes: String = ""
)

data class CulturalRules(
    val tradition: String? = null,
    val origin: String? = null,
    val motifs: List<String> = emptyList(),
    val taboos: List<String> = emptyList()
)

data class LightingProfile(
    val primaryLight: String = "soft front",
    val material: String = "",
    val surfaceReflectance: String = "medium"
)
```

### ProjectEntity

```kotlin
// data/database/entities/ProjectEntity.kt

@Entity(
    tableName = "projects",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("template_id"), Index("status", "created_at")]
)
data class ProjectEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String? = null,
    @ColumnInfo(name = "template_id")
    val templateId: String,
    @ColumnInfo(name = "sketch_path")
    val sketchPath: String? = null,        // local file path under filesDir/sketches/
    @ColumnInfo(name = "analysis_json")
    val analysisJson: String? = null,      // serialized SketchAnalysis
    @ColumnInfo(name = "suggestions_json")
    val suggestionsJson: String? = null,   // serialized SuggestionsResponse
    @ColumnInfo(name = "accepted_suggestion_ids")
    val acceptedSuggestionIds: String = "", // comma-separated
    @ColumnInfo(name = "color_override")
    val colorOverride: String? = null,
    val notes: String? = null,
    val status: String = "draft",          // draft | analyzing | analyzed | rendering | rendered | failed
    @ColumnInfo(name = "rendered_path")
    val renderedPath: String? = null,
    @ColumnInfo(name = "render_prompt")
    val renderPrompt: String? = null,
    @ColumnInfo(name = "render_quality_json")
    val renderQualityJson: String? = null,
    @ColumnInfo(name = "rendered_at")
    val renderedAt: Instant? = null,
    @ColumnInfo(name = "export_path")
    val exportPath: String? = null,
    @ColumnInfo(name = "export_mockup_id")
    val exportMockupId: String? = null,
    @ColumnInfo(name = "exported_at")
    val exportedAt: Instant? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now()
)

enum class ProjectStatus { DRAFT, ANALYZING, ANALYZED, RENDERING, RENDERED, FAILED }
```

### ProductMockupEntity

```kotlin
// data/database/entities/ProductMockupEntity.kt

@Entity(tableName = "product_mockups")
data class ProductMockupEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String,                   // henna | garment | wall | ceramic
    @ColumnInfo(name = "surface_match_csv")
    val surfaceMatchCsv: String,            // "skin_palm,skin_hand_full"
    val scene: String,
    val lighting: String = "soft natural daylight",
    val perspective: String = "eye-level",
    @ColumnInfo(name = "accent_color")
    val accentColor: String = "#c9a84c",
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now()
)
```

---

## 🔄 Type Converters

```kotlin
// data/database/Converters.kt (extend existing file)

class Converters {
    private val gson = Gson()

    @TypeConverter fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    // The JSON columns are stored as plain TEXT. Repositories handle (de)serialization
    // via Gson rather than Room converters — keeps schemas/ artifacts simple and avoids
    // forcing every consumer through Room.
}
```

---

## 📥 DAOs

```kotlin
// data/dao/TemplateDao.kt

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY category, sort_order, name")
    fun observeAll(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE category = :category ORDER BY sort_order, name")
    fun observeByCategory(category: String): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun byId(id: String): TemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TemplateEntity>)

    @Query("DELETE FROM templates")
    suspend fun deleteAll()
}

// data/dao/ProjectDao.kt

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observe(id: String): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun byId(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ProjectEntity)

    @Query("UPDATE projects SET status = :status, updated_at = :now WHERE id = :id")
    suspend fun setStatus(id: String, status: String, now: Instant = Instant.now())

    @Query("""
        UPDATE projects
        SET analysis_json = :json, status = 'analyzed', updated_at = :now
        WHERE id = :id
    """)
    suspend fun setAnalysis(id: String, json: String, now: Instant = Instant.now())

    @Query("""
        UPDATE projects
        SET suggestions_json = :json, updated_at = :now
        WHERE id = :id
    """)
    suspend fun setSuggestions(id: String, json: String, now: Instant = Instant.now())

    @Query("""
        UPDATE projects
        SET rendered_path = :path, render_prompt = :prompt,
            render_quality_json = :qualityJson, rendered_at = :now,
            status = 'rendered', updated_at = :now
        WHERE id = :id
    """)
    suspend fun setRender(
        id: String,
        path: String,
        prompt: String,
        qualityJson: String,
        now: Instant = Instant.now()
    )

    @Query("UPDATE projects SET color_override = :color, updated_at = :now WHERE id = :id")
    suspend fun setColorOverride(id: String, color: String?, now: Instant = Instant.now())

    @Query("UPDATE projects SET accepted_suggestion_ids = :ids, updated_at = :now WHERE id = :id")
    suspend fun setAcceptedSuggestions(id: String, ids: String, now: Instant = Instant.now())

    @Delete
    suspend fun delete(item: ProjectEntity)
}

// data/dao/ProductMockupDao.kt

@Dao
interface ProductMockupDao {
    @Query("SELECT * FROM product_mockups WHERE category = :category ORDER BY sort_order, name")
    fun observeByCategory(category: String): Flow<List<ProductMockupEntity>>

    @Query("""
        SELECT * FROM product_mockups
        WHERE category = :category
          AND surface_match_csv LIKE '%' || :surface || '%'
        ORDER BY sort_order, name
    """)
    fun observeForSurface(category: String, surface: String): Flow<List<ProductMockupEntity>>

    @Query("SELECT * FROM product_mockups WHERE id = :id")
    suspend fun byId(id: String): ProductMockupEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seed(items: List<ProductMockupEntity>)
}
```

---

## 🌱 Seed Data — 12 Product Mockups

Verbatim port of the Lovable Supabase seed in migration `20260522025843`.

```kotlin
// data/seed/MockupSeed.kt

object MockupSeed {
    fun build(): List<ProductMockupEntity> = listOf(
        // ─── Henna ───
        ProductMockupEntity(
            name = "Bridal palm",
            category = "henna",
            surfaceMatchCsv = "skin_palm,skin_hand_full",
            scene = "A bride's open palm and fingers rested on a silk cushion, gold bangles at the wrist, soft bokeh of marigold petals behind",
            lighting = "warm golden-hour window light",
            perspective = "overhead 3/4",
            accentColor = "#b86b3a",
            sortOrder = 10
        ),
        ProductMockupEntity(
            name = "Foot pose on rug",
            category = "henna",
            surfaceMatchCsv = "skin_foot",
            scene = "A bare foot with anklet resting on a deep red Persian rug, traditional setting",
            lighting = "soft morning daylight",
            perspective = "side eye-level",
            accentColor = "#7a2b1f",
            sortOrder = 20
        ),
        ProductMockupEntity(
            name = "Henna close-up",
            category = "henna",
            surfaceMatchCsv = "skin_palm,skin_hand_full,skin_foot",
            scene = "Tight macro of freshly applied henna with subtle paste sheen on skin",
            lighting = "soft diffused studio light",
            perspective = "macro overhead",
            accentColor = "#6b3a2a",
            sortOrder = 30
        ),
        // ─── Garment ───
        ProductMockupEntity(
            name = "Flat-lay abaya",
            category = "garment",
            surfaceMatchCsv = "fabric_abaya",
            scene = "A flowing black abaya laid flat on a marble surface with gold thread shimmering, perfume bottle and pearls nearby",
            lighting = "soft north-window light",
            perspective = "overhead",
            accentColor = "#d4af37",
            sortOrder = 10
        ),
        ProductMockupEntity(
            name = "Thobe on hanger",
            category = "garment",
            surfaceMatchCsv = "fabric_thobe",
            scene = "A crisp white thobe on a wooden hanger against a warm sandstone wall",
            lighting = "warm late-afternoon light",
            perspective = "front eye-level",
            accentColor = "#c9a87a",
            sortOrder = 20
        ),
        ProductMockupEntity(
            name = "Toub on model shoulder",
            category = "garment",
            surfaceMatchCsv = "fabric_toub",
            scene = "A translucent Sudanese toub draped over a model's shoulder, soft folds catching light",
            lighting = "soft window light",
            perspective = "3/4 portrait",
            accentColor = "#b87a4a",
            sortOrder = 30
        ),
        // ─── Wall ───
        ProductMockupEntity(
            name = "Majlis wall",
            category = "wall",
            surfaceMatchCsv = "wall_plaster,wall_stone",
            scene = "An interior majlis with low cushions, brass lantern, and a feature wall ready for ornament",
            lighting = "warm interior lantern + daylight mix",
            perspective = "wide eye-level",
            accentColor = "#a87a3a",
            sortOrder = 10
        ),
        ProductMockupEntity(
            name = "Carved arch niche",
            category = "wall",
            surfaceMatchCsv = "wall_arch,wall_stone",
            scene = "A pointed arch niche in a sandstone wall with soft side shadow",
            lighting = "golden-hour raking light",
            perspective = "straight-on eye-level",
            accentColor = "#c08a4a",
            sortOrder = 20
        ),
        ProductMockupEntity(
            name = "Gallery plaster wall",
            category = "wall",
            surfaceMatchCsv = "wall_plaster",
            scene = "A minimalist gallery wall in white lime plaster with subtle texture",
            lighting = "soft skylight",
            perspective = "eye-level",
            accentColor = "#d8c7a8",
            sortOrder = 30
        ),
        // ─── Ceramic ───
        ProductMockupEntity(
            name = "Stoneware mug on linen",
            category = "ceramic",
            surfaceMatchCsv = "ceramic_mug",
            scene = "A stoneware mug on cream linen next to a sprig of dried thyme",
            lighting = "soft diffused morning light",
            perspective = "3/4 product",
            accentColor = "#3a5a4a",
            sortOrder = 10
        ),
        ProductMockupEntity(
            name = "Tile in grid",
            category = "ceramic",
            surfaceMatchCsv = "ceramic_tile",
            scene = "A single hand-painted tile centered in a quiet grid of plain tiles on a wall",
            lighting = "soft side light",
            perspective = "straight-on",
            accentColor = "#2d5f7a",
            sortOrder = 20
        ),
        ProductMockupEntity(
            name = "Display plate on table",
            category = "ceramic",
            surfaceMatchCsv = "ceramic_plate",
            scene = "A round display plate on a dark walnut table with brass accents",
            lighting = "warm spot + soft fill",
            perspective = "overhead",
            accentColor = "#8a4a2d",
            sortOrder = 30
        )
    )
}
```

---

## 🛣 Migration Strategy (MT-041)

The existing `MawaaiDatabase` has version N. After E8 it bumps to N+1.

```kotlin
// data/database/Migrations.kt

object Migrations {
    val MIGRATION_N_TO_NPLUS1 = object : Migration(N, N + 1) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS templates (
                    id TEXT NOT NULL PRIMARY KEY,
                    category TEXT NOT NULL,
                    name TEXT NOT NULL,
                    surface_type TEXT NOT NULL,
                    description TEXT,
                    reference_image_asset TEXT NOT NULL,
                    zones_json TEXT NOT NULL DEFAULT '[]',
                    cultural_rules_json TEXT NOT NULL DEFAULT '{}',
                    traditional_palette_json TEXT NOT NULL DEFAULT '[]',
                    lighting_profile_json TEXT NOT NULL DEFAULT '{}',
                    recommended_complexity TEXT NOT NULL DEFAULT 'medium',
                    max_coverage_pct INTEGER NOT NULL DEFAULT 75,
                    sort_order INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL
                )
            """)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS projects (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT,
                    template_id TEXT NOT NULL REFERENCES templates(id) ON DELETE RESTRICT,
                    sketch_path TEXT,
                    analysis_json TEXT,
                    suggestions_json TEXT,
                    accepted_suggestion_ids TEXT NOT NULL DEFAULT '',
                    color_override TEXT,
                    notes TEXT,
                    status TEXT NOT NULL DEFAULT 'draft',
                    rendered_path TEXT,
                    render_prompt TEXT,
                    render_quality_json TEXT,
                    rendered_at INTEGER,
                    export_path TEXT,
                    export_mockup_id TEXT,
                    exported_at INTEGER,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_projects_template ON projects(template_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_projects_status ON projects(status, created_at)")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS product_mockups (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    surface_match_csv TEXT NOT NULL,
                    scene TEXT NOT NULL,
                    lighting TEXT NOT NULL DEFAULT 'soft natural daylight',
                    perspective TEXT NOT NULL DEFAULT 'eye-level',
                    accent_color TEXT NOT NULL DEFAULT '#c9a84c',
                    sort_order INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL
                )
            """)
        }
    }
}
```

**Critical rules:**
- ❌ Never use `fallbackToDestructiveMigration()` — destroys user data.
- ✅ Each new column on an existing table = `ALTER TABLE ADD COLUMN` with a default.
- ✅ Each new table = `CREATE TABLE IF NOT EXISTS`.
- ✅ Indices added separately so the migration can be replayed safely.

---

## 📂 File Storage Layout

| Bucket | Android equivalent | Path |
|---|---|---|
| `sketches` | App-private internal storage | `context.filesDir/sketches/<projectId>.png` |
| `renders` | App-private internal storage | `context.filesDir/renders/<projectId>-<timestamp>.png` |
| `exports` | Public via MediaStore | `MediaStore.Images.Media.RELATIVE_PATH=Pictures/Mawaai/` |
| `templates` | Bundled assets | `assets/templates/<category>/<file>` |

Helper:

```kotlin
// data/storage/ProjectFileStorage.kt

class ProjectFileStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sketchesDir by lazy { File(context.filesDir, "sketches").apply { mkdirs() } }
    private val rendersDir by lazy { File(context.filesDir, "renders").apply { mkdirs() } }

    fun sketchFile(projectId: String) = File(sketchesDir, "$projectId.png")
    fun renderFile(projectId: String, timestamp: Long = System.currentTimeMillis()) =
        File(rendersDir, "$projectId-$timestamp.png")

    fun pathToFile(path: String): File =
        if (File(path).isAbsolute) File(path)
        else File(context.filesDir, path)
}
```

---

## 🧪 Repository Contract

```kotlin
// data/repository/ProjectRepository.kt

interface ProjectRepository {
    fun observe(id: String): Flow<Project?>
    fun observeAll(): Flow<List<Project>>
    suspend fun create(templateId: String, title: String?): Project
    suspend fun saveSketch(projectId: String, sketch: Bitmap)
    suspend fun saveAnalysis(projectId: String, analysis: SketchAnalysis)
    suspend fun saveSuggestions(projectId: String, suggestions: SuggestionsResponse)
    suspend fun acceptSuggestions(projectId: String, ids: List<String>)
    suspend fun setColorOverride(projectId: String, hex: String?)
    suspend fun saveRender(
        projectId: String,
        bitmap: Bitmap,
        prompt: String,
        quality: RenderQuality
    )
    suspend fun saveExport(projectId: String, exportedFile: File, mockupId: String)
    suspend fun delete(projectId: String)
}
```

The implementation goes through DAO + ProjectFileStorage; no direct SQLite access outside the DAO, no direct file access outside the storage helper.

---

## 🔒 Privacy Notes

- All sketches, renders, and exports are local to the device. No cloud sync (Supabase was the source — we intentionally drop it per F-002 deferral in `ai_handoff/MASTER_PLAN.md`).
- Exports written via MediaStore are visible to the user in Gallery and survive uninstall. Sketches/renders in `filesDir` are deleted on uninstall.
- No user PII stored. `userId` from the Supabase schema doesn't translate — this is a single-user offline app.
