# Mawaai database migrations

This project uses Room with **schema export enabled** (`room { schemaDirectory("$projectDir/schemas") }` in `app/build.gradle.kts`). Every `@Database(version = N)` produces a `app/schemas/com.mawaai.love.app.data.database.MawaaiDatabase/N.json` file describing the exact tables, columns, indices, and foreign keys Room expects at that version.

The schema files are **checked into source**. CI must fail if a developer changes an `@Entity` without committing the regenerated schema.

## Adding a new schema version

When you change any `@Entity`, the database version must increment and a `Migration(prev, next)` must be supplied. The workflow:

1. **Bump the version** in `MawaaiDatabase.kt`:
   ```kotlin
   @Database(entities = [...], version = N+1, exportSchema = true)
   ```

2. **Add a migration** in `data/database/Migrations.kt`:
   ```kotlin
   val MIGRATION_N_M = object : Migration(N, N+1) {
       override fun migrate(db: SupportSQLiteDatabase) {
           // ALTER TABLE / CREATE TABLE statements that match what Room
           // emits for the new entity layout. See the gotchas below.
       }
   }

   val ALL = arrayOf(MIGRATION_5_6, MIGRATION_N_M)
   ```

3. **Run a debug build** so the Room KSP processor regenerates the schema:
   ```bash
   ./gradlew :app:assembleDebug
   ```
   This produces `app/schemas/.../{N+1}.json`. **Commit it.**

4. **Run the migration test** on an emulator/device:
   ```bash
   ./gradlew :app:connectedDebugAndroidTest --tests com.mawaai.love.app.data.database.MigrationTest
   ```
   `MigrationTestHelper.runMigrationsAndValidate` will compare the migrated DB against the new schema and throw if anything differs.

## Gotchas (lessons from the v5→v6 migration)

The first cut of `MIGRATION_5_6` shipped with two issues that `MigrationTest` now catches automatically. Be aware:

- **Index names use the `index_` prefix, not `idx_`.** Room generates index names as `index_<tableName>_<columnNames>`. If your migration uses any other prefix, `validateMigration` throws on first launch.

- **Foreign keys are emitted with `ON UPDATE NO ACTION` even if `@ForeignKey` does not specify `onUpdate`.** Room includes the explicit clause; mirror it in migrations to keep the schema diff byte-for-byte identical (SQLite treats absent `ON UPDATE` as `NO ACTION` anyway, so functionally either is fine, but explicit avoids future confusion).

- **`Boolean` columns are stored as `INTEGER`.** `val isPublic: Boolean = false` becomes `INTEGER NOT NULL DEFAULT 0`.

- **`val x: Type = literal` Kotlin defaults do NOT produce SQL `DEFAULT` clauses.** Room only emits SQL defaults when you write `@ColumnInfo(defaultValue = "...")`. The Kotlin default still applies at the Kotlin layer when inserting through the DAO. Including a SQL `DEFAULT` in the migration is fine (Room skips the default-value check when its own column has no explicit `defaultValue`), but matching what Room does keeps the schema cleaner.

- **Column declaration order in `CREATE TABLE` should match the entity's primary-constructor order.** Room compares column lists positionally during validation. If you reorder fields in the entity, regenerate the schema.

## Never use `fallbackToDestructiveMigration()` in release

`DatabaseModule.provideDatabase` deliberately does **not** call `fallbackToDestructiveMigration()`. A real user upgrading the app must not lose their memories, letters, mood entries, or projects. If a migration is missing, the app should crash loudly in `MigrationTest` *before* the release reaches a phone.
