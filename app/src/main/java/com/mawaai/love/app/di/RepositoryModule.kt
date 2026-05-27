package com.mawaai.love.app.di

import com.mawaai.love.app.data.repository.ProjectRepository
import com.mawaai.love.app.data.repository.TemplateRepository
import com.mawaai.love.app.data.database.dao.ProjectDao
import com.mawaai.love.app.data.database.dao.TemplateDao
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideTemplateRepository(dao: TemplateDao): TemplateRepository {
        return TemplateRepository(dao)
    }

    @Provides
    @Singleton
    fun provideProjectRepository(dao: ProjectDao, gson: Gson): ProjectRepository {
        return ProjectRepository(dao, gson)
    }
}
