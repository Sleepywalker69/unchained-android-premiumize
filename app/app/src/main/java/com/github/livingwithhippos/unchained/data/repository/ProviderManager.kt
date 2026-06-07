package com.github.livingwithhippos.unchained.data.repository

import android.content.SharedPreferences
import com.github.livingwithhippos.unchained.data.model.domain.DebridProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class ProviderManager
@Inject
constructor(
    private val preferences: SharedPreferences,
    private val realDebridRepository: RealDebridRepositoryImpl,
    private val premiumizeRepository: PremiumizeRepositoryImpl,
) {

    private val _providerChanges = MutableSharedFlow<DebridProvider>(extraBufferCapacity = 1)
    val providerChanges: SharedFlow<DebridProvider> = _providerChanges.asSharedFlow()

    fun getActiveProvider(): DebridProvider {
        val providerName = preferences.getString(PREF_KEY_DEBRID_PROVIDER, "real_debrid")
        return when (providerName) {
            "premiumize" -> DebridProvider.PREMIUMIZE
            else -> DebridProvider.REAL_DEBRID
        }
    }

    fun getRepository(): DebridRepository =
        when (getActiveProvider()) {
            DebridProvider.REAL_DEBRID -> realDebridRepository
            DebridProvider.PREMIUMIZE -> premiumizeRepository
        }

    fun setActiveProvider(provider: DebridProvider) {
        val value =
            when (provider) {
                DebridProvider.REAL_DEBRID -> "real_debrid"
                DebridProvider.PREMIUMIZE -> "premiumize"
            }
        preferences.edit().putString(PREF_KEY_DEBRID_PROVIDER, value).apply()
        _providerChanges.tryEmit(provider)
    }

    companion object {
        const val PREF_KEY_DEBRID_PROVIDER = "debrid_provider"
    }
}
