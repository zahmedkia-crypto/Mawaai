package com.mawaai.love.app.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object MawaaiMigrations {
    /**
     * v5 → v6: add the Creative Studio integration tables.
     * Note: User reported current version is 5.
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Templates table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS templates (
                    id TEXT NOT NULL PRIMARY KEY,
                    category TEXT NOT NULL,
                    name TEXT NOT NULL,
                    surfaceType TEXT NOT NULL,
                    description TEXT,
                    referenceImageUrl TEXT,
                    assetPath TEXT NOT NULL,
                    zonesJson TEXT NOT NULL,
                    culturalRulesJson TEXT NOT NULL,
                    traditionalPaletteJson TEXT NOT NULL,
                    primaryLight TEXT NOT NULL DEFAULT '',
                    material TEXT NOT NULL DEFAULT '',
                    surfaceReflectance TEXT NOT NULL DEFAULT '',
                    recommendedComplexity TEXT NOT NULL DEFAULT 'medium',
                    maxCoveragePct INTEGER NOT NULL DEFAULT 75,
                    sortOrder INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())

            // Projects table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS projects (
                    id TEXT NOT NULL PRIMARY KEY,
                    templateId TEXT NOT NULL,
                    title TEXT,
                    sketchPath TEXT,
                    sketchCapturedAt INTEGER,
                    analysisJson TEXT,
                    suggestionsJson TEXT,
                    acceptedSuggestionIds TEXT NOT NULL,
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

            // Product Mockups table
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

    val ALL = arrayOf(MIGRATION_5_6)
}
