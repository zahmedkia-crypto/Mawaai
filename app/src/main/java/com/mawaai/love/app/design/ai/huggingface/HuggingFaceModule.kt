package com.mawaai.love.app.design.ai.huggingface

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
 * Hilt module providing the HuggingFace Inference API stack. Kept in a
 * separate module from [com.mawaai.love.app.design.di.DesignModule]
 * because:
 *  - The OkHttp client wants a longer read timeout (60s) than Gemini's
 *    20s — diffusion models can take a while to render even after the
 *    initial cold-start retry. A shared client would force the lower
 *    bound on Gemini calls.
 *  - The `@Named("hf-okhttp")` qualifier mirrors the existing pattern
 *    [DesignModule] uses for any future per-service OkHttp configuration
 *    and keeps the DI graph readable.
 *
 * Auth and the API key are NOT injected here — `HuggingFaceClient`
 * reads `BuildConfig.HUGGINGFACE_API_KEY` at call time so a build-time
 * key flip doesn't require restarting the DI graph.
 */
@Module
@InstallIn(SingletonComponent::class)
object HuggingFaceModule {

    @Provides
    @Singleton
    @Named("hf-okhttp")
    fun provideHuggingFaceOkHttp(): OkHttpClient = OkHttpClient.Builder()
        // Connect quickly — HF's CDN is geo-distributed and rarely takes
        // more than a few seconds to handshake.
        .connectTimeout(15, TimeUnit.SECONDS)
        // Long read timeout to tolerate diffusion latency. Cold-start
        // retry inside the client adds up to 30s on top of this if the
        // server reports `estimated_time`.
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideHuggingFaceApi(
        @Named("hf-okhttp") okHttp: OkHttpClient,
        gson: Gson
    ): HuggingFaceApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(HuggingFaceApi::class.java)

    private const val BASE_URL = "https://api-inference.huggingface.co/"
}
