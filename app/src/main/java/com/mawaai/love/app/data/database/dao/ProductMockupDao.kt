package com.mawaai.love.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mawaai.love.app.data.database.entities.ProductMockupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductMockupDao {
    @Query("SELECT * FROM product_mockups ORDER BY sortOrder ASC")
    fun getAllMockups(): Flow<List<ProductMockupEntity>>

    @Query("SELECT * FROM product_mockups WHERE category = :category ORDER BY sortOrder ASC")
    fun getMockupsByCategory(category: String): Flow<List<ProductMockupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMockups(mockups: List<ProductMockupEntity>)

    @Query("SELECT COUNT(*) FROM product_mockups")
    suspend fun getMockupCount(): Int
}
