package com.mawaai.love.app.data.remote.zenquotes

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
 * Hilt module for the ZenQuotes public API. Same shape as the cloud
 * AI modules: dedicated OkHttp client + Retrofit + Gson.
 *
 * No API key handling here — ZenQuotes does not require auth.
 */
@Module
@InstallIn(SingletonComponent::class)
object ZenQuotesModule {

    @Provides
    @Singleton
    @Named("zenquotes-okhttp")
    fun provideZenQuotesOkHttp(): OkHttpClient = OkHttpClient.Builder()
        // Quotes API responds in under a second from the global CDN;
        // 10s timeouts are generous enough for cellular hiccups.
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideZenQuotesApi(
        @Named("zenquotes-okhttp") okHttp: OkHttpClient,
        gson: Gson
    ): ZenQuotesApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(ZenQuotesApi::class.java)

    private const val BASE_URL = "https://zenquotes.io/"
}
