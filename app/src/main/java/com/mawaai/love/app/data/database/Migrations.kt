package com.mawaai.love.app.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object MawaaiMigrations {
    /**
     * v5 → v6: add the Creative Studio integration tables (templates, projects,
     * product_mockups).
     *
     * P0-A / P0-B (fix branch fix/P0-room-v6-migration):
     *   - Index names use Room's canonical `index_<table>_<columns>` prefix.
     *     The first cut of this migration used `idx_*` which does not match
     *     what Room generates from `@Index(...)` and therefore failed
     *     `RoomOpenHelper.validateMigration` on first launch with an existing
     *     v5 database.
     *   - The foreign key clause now spells out `ON UPDATE NO ACTION` so the
     *     migration mirrors Room's emitted CREATE TABLE byte-for-byte. SQLite
     *     treats omitted `ON UPDATE` as `NO ACTION` anyway, but being explicit
     *     keeps the schema diff clean.
     *
     * `MigrationTest` in `androidTest` exercises this path against the
     * generated `5.json` schema and will fail loudly if the column list,
     * types, NOT NULL flags, primary key, indices, or foreign keys ever drift
     * out of sync with the @Entity definitions again.
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // ---- templates ----
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `templates` (
                    `id` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `surfaceType` TEXT NOT NULL,
                    `description` TEXT,
                    `referenceImageUrl` TEXT,
                    `assetPath` TEXT NOT NULL,
                    `zonesJson` TEXT NOT NULL,
                    `culturalRulesJson` TEXT NOT NULL,
                    `traditionalPaletteJson` TEXT NOT NULL,
                    `primaryLight` TEXT NOT NULL DEFAULT '',
                    `material` TEXT NOT NULL DEFAULT '',
                    `surfaceReflectance` TEXT NOT NULL DEFAULT '',
                    `recommendedComplexity` TEXT NOT NULL DEFAULT 'medium',
                    `maxCoveragePct` INTEGER NOT NULL DEFAULT 75,
                    `sortOrder` INTEGER NOT NULL DEFAULT 0,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            // ---- projects ----
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `projects` (
                    `id` TEXT NOT NULL,
                    `templateId` TEXT NOT NULL,
                    `title` TEXT,
                    `sketchPath` TEXT,
                    `sketchCapturedAt` INTEGER,
                    `analysisJson` TEXT,
                    `suggestionsJson` TEXT,
                    `acceptedSuggestionIds` TEXT NOT NULL,
                    `colorOverride` TEXT,
                    `templateColor` TEXT,
                    `renderedPath` TEXT,
                    `renderPrompt` TEXT,
                    `renderedAt` INTEGER,
                    `renderQualityJson` TEXT,
                    `exportPath` TEXT,
                    `exportedAt` INTEGER,
                    `exportMockupId` TEXT,
                    `isPublic` INTEGER NOT NULL DEFAULT 0,
                    `notes` TEXT,
                    `status` TEXT NOT NULL DEFAULT 'DRAFT',
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`templateId`) REFERENCES `templates`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_templateId` ON `projects` (`templateId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_status` ON `projects` (`status`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_isPublic` ON `projects` (`isPublic`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_createdAt` ON `projects` (`createdAt`)")

            // ---- product_mockups ----
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `product_mockups` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `surfaceMatchCsv` TEXT NOT NULL,
                    `scene` TEXT NOT NULL,
                    `lighting` TEXT NOT NULL,
                    `perspective` TEXT NOT NULL,
                    `accentColor` TEXT NOT NULL,
                    `sortOrder` INTEGER NOT NULL DEFAULT 0,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }
    }

    val ALL = arrayOf(MIGRATION_5_6)
}
