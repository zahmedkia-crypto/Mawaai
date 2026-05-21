package com.mawaai.love.app.design.ai.cloudflare

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
 * Hilt module for Cloudflare Workers AI. Same layout as the HuggingFace
 * and remove.bg modules: dedicated OkHttp client tuned for diffusion
 * latency, Retrofit + Gson for the JSON request envelope.
 *
 * The account ID + API token are NOT injected here — `CloudflareWorkersAiClient`
 * reads them from `BuildConfig` at call time so a build-time key flip
 * doesn't require restarting the DI graph.
 */
@Module
@InstallIn(SingletonComponent::class)
object CloudflareModule {

    @Provides
    @Singleton
    @Named("cf-okhttp")
    fun provideCloudflareOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // SDXL on CF takes ~6-12s; FLUX / Lightning take <3s. 60s
        // covers the worst case (cold worker + large prompt) without
        // pinning the UI thread indefinitely if the service stalls.
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideCloudflareApi(
        @Named("cf-okhttp") okHttp: OkHttpClient,
        gson: Gson
    ): CloudflareWorkersAiApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(CloudflareWorkersAiApi::class.java)

    // Cloudflare's API root; the account ID + model live in the per-call
    // path so a single Retrofit instance handles every Workers AI model.
    private const val BASE_URL = "https://api.cloudflare.com/"
}
