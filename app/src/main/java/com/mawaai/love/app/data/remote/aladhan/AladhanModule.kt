package com.mawaai.love.app.data.remote.aladhan

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
 * Hilt module for the Aladhan public API. Same shape as the other
 * remote-data modules: dedicated OkHttp + Retrofit + Gson.
 *
 * No API key handling — Aladhan does not require auth.
 */
@Module
@InstallIn(SingletonComponent::class)
object AladhanModule {

    @Provides
    @Singleton
    @Named("aladhan-okhttp")
    fun provideAladhanOkHttp(): OkHttpClient = OkHttpClient.Builder()
        // Aladhan responds in well under a second from its global CDN;
        // 12s is generous for spotty cellular without inviting hangs.
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideAladhanApi(
        @Named("aladhan-okhttp") okHttp: OkHttpClient,
        gson: Gson
    ): AladhanApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(AladhanApi::class.java)

    private const val BASE_URL = "https://api.aladhan.com/"
}
