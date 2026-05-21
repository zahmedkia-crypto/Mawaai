package com.mawaai.love.app.design.ai.removebg

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module for the remove.bg HTTP API. Mirrors the layout of
 * `HuggingFaceModule`: dedicated OkHttp client tuned for image upload
 * latency, Retrofit + Gson for the (rare) JSON error parsing.
 *
 * The API key is NOT injected here — `RemoveBgClient` reads
 * `BuildConfig.REMOVE_BG_API_KEY` at call time so a build-time key flip
 * doesn't require restarting the DI graph.
 */
@Module
@InstallIn(SingletonComponent::class)
object RemoveBgModule {

    @Provides
    @Singleton
    @Named("removebg-okhttp")
    fun provideRemoveBgOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // remove.bg typically responds within 3-5s for preview-size
        // requests but full-size renders can take ~20s. Keep some
        // headroom without blocking the UI forever.
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRemoveBgApi(
        @Named("removebg-okhttp") okHttp: OkHttpClient,
        gson: Gson
    ): RemoveBgApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(RemoveBgApi::class.java)

    private const val BASE_URL = "https://api.remove.bg/"
}
