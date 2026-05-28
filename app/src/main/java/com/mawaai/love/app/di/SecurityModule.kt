package com.mawaai.love.app.di

import com.mawaai.love.app.core.security.EncryptedKeyVault
import com.mawaai.love.app.core.security.KeyVault
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module wiring [KeyVault] to the production [EncryptedKeyVault].
 *
 * Keeping the binding behind an `@Binds` lets tests swap in a fake without
 * touching every client. Once MT-027 migrates clients off direct `BuildConfig`
 * reads, the test path becomes:
 *
 *   @TestInstallIn(replaces = [SecurityModule::class], components = [SingletonComponent::class])
 *   abstract class FakeSecurityModule {
 *       @Binds @Singleton
 *       abstract fun bindFakeVault(impl: FakeKeyVault): KeyVault
 *   }
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindKeyVault(impl: EncryptedKeyVault): KeyVault
}
