package com.mawaai.love.app.data.repository

import com.mawaai.love.app.data.dao.ProfileDao
import com.mawaai.love.app.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val dao: ProfileDao
) {
    fun getProfile(): Flow<UserProfile?> = dao.getProfile()
    suspend fun updateProfile(profile: UserProfile) = dao.updateProfile(profile)
}
