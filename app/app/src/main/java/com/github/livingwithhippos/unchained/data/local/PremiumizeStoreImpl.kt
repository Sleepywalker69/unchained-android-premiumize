package com.github.livingwithhippos.unchained.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

class PremiumizeStoreImpl
@Inject
constructor(@param:ApplicationContext private val context: Context) : PremiumizeStore {

    override val credentialsFlow: Flow<PremiumizeCredentials.PremiumizeCredential> =
        context.premiumizeCredentialsDataStore.data.catch { exception ->
            if (exception is IOException) {
                exception.printStackTrace()
                emit(PremiumizeCredentials.PremiumizeCredential.getDefaultInstance())
            } else {
                throw exception
            }
        }

    override suspend fun setAccessToken(accessToken: String) {
        context.premiumizeCredentialsDataStore.updateData { credentials ->
            credentials.toBuilder().setAccessToken(accessToken).build()
        }
    }

    override suspend fun setRefreshToken(refreshToken: String) {
        context.premiumizeCredentialsDataStore.updateData { credentials ->
            credentials.toBuilder().setRefreshToken(refreshToken).build()
        }
    }

    override suspend fun setApiKey(apiKey: String) {
        context.premiumizeCredentialsDataStore.updateData { credentials ->
            credentials.toBuilder().setApiKey(apiKey).build()
        }
    }

    override suspend fun setCustomerId(customerId: String) {
        context.premiumizeCredentialsDataStore.updateData { credentials ->
            credentials.toBuilder().setCustomerId(customerId).build()
        }
    }

    override suspend fun deleteCredentials() {
        context.premiumizeCredentialsDataStore.updateData { it.toBuilder().clear().build() }
    }

    override suspend fun getCredentials(): PremiumizeCredentials.PremiumizeCredential {
        return try {
            credentialsFlow.first()
        } catch (e: Exception) {
            e.printStackTrace()
            PremiumizeCredentials.PremiumizeCredential.getDefaultInstance()
        }
    }

    override suspend fun hasCredentials(): Boolean {
        val creds = getCredentials()
        return creds.accessToken.isNotBlank() || creds.apiKey.isNotBlank()
    }
}
