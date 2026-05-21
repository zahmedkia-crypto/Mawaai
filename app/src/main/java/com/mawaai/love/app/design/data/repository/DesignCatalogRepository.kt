package com.mawaai.love.app.design.data.repository

import android.content.Context
import com.google.gson.Gson
import com.mawaai.love.app.design.domain.model.DesignCatalog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DesignCatalogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private var cached: DesignCatalog? = null

    suspend fun load(): DesignCatalog = withContext(Dispatchers.IO) {
        cached ?: run {
            val json = context.assets.open("data/design_categories.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            val parsed = gson.fromJson(json, DesignCatalog::class.java)
            cached = parsed
            parsed
        }
    }
}
