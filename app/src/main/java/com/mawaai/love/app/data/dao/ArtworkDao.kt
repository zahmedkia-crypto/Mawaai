package com.mawaai.love.app.data.dao

import androidx.room.*
import com.mawaai.love.app.data.model.Artwork
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtworkDao {
    @Query("SELECT * FROM artworks ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Artwork>>

    @Query("SELECT * FROM artworks WHERE categoryId = :categoryId ORDER BY createdAt DESC")
    fun getByCategory(categoryId: String): Flow<List<Artwork>>

    @Query("SELECT * FROM artworks WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavorites(): Flow<List<Artwork>>

    @Query("SELECT * FROM artworks WHERE id = :id")
    suspend fun getById(id: Long): Artwork?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(artwork: Artwork): Long

    @Update
    suspend fun update(artwork: Artwork)

    @Delete
    suspend fun delete(artwork: Artwork)

    @Query("SELECT COUNT(*) FROM artworks")
    fun count(): Flow<Int>
}
