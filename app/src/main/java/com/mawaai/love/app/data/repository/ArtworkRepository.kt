package com.mawaai.love.app.data.repository

import com.mawaai.love.app.data.dao.ArtworkDao
import com.mawaai.love.app.data.model.Artwork
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtworkRepository @Inject constructor(
    private val dao: ArtworkDao
) {
    fun getAll(): Flow<List<Artwork>> = dao.getAll()
    fun getByCategory(categoryId: String): Flow<List<Artwork>> = dao.getByCategory(categoryId)
    fun getFavorites(): Flow<List<Artwork>> = dao.getFavorites()
    suspend fun getById(id: Long): Artwork? = dao.getById(id)
    suspend fun save(artwork: Artwork): Long = dao.insert(artwork)
    suspend fun update(artwork: Artwork) = dao.update(artwork)
    suspend fun delete(artwork: Artwork) = dao.delete(artwork)
    fun count(): Flow<Int> = dao.count()
}
