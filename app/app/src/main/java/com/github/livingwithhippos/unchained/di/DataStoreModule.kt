package com.github.livingwithhippos.unchained.di

import android.content.Context
import com.github.livingwithhippos.unchained.data.local.PremiumizeStore
import com.github.livingwithhippos.unchained.data.local.PremiumizeStoreImpl
import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.local.ProtoStoreImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the datastore injected with Dagger Hilt */
@InstallIn(SingletonComponent::class)
@Module
object DataStoreModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext appContext: Context): ProtoStore {
        return ProtoStoreImpl(appContext)
    }

    @Provides
    @Singleton
    fun providePremiumizeStore(@ApplicationContext appContext: Context): PremiumizeStore {
        return PremiumizeStoreImpl(appContext)
    }
}
