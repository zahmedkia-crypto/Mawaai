package com.mawaai.love.app.data.repository

import com.mawaai.love.app.data.dao.LoveLetterDao
import com.mawaai.love.app.data.model.LoveLetter
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoveLetterRepository @Inject constructor(
    private val dao: LoveLetterDao
) {
    fun getAllLetters(): Flow<List<LoveLetter>> = dao.getAllLetters()
    fun getFavorites(): Flow<List<LoveLetter>> = dao.getFavoriteLetters()
    suspend fun getLetterById(id: Long) = dao.getLetterById(id)

    suspend fun addLetter(letter: LoveLetter): Long = dao.insertLetter(letter)
    suspend fun updateLetter(letter: LoveLetter) = dao.updateLetter(letter)
    suspend fun deleteLetter(letter: LoveLetter) = dao.deleteLetter(letter)
}
