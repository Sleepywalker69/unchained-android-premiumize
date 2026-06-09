package com.github.livingwithhippos.unchained.data.local

import kotlinx.coroutines.flow.Flow

interface PremiumizeStore {

    val credentialsFlow: Flow<PremiumizeCredentials.PremiumizeCredential>

    suspend fun setAccessToken(accessToken: String)

    suspend fun setRefreshToken(refreshToken: String)

    suspend fun setApiKey(apiKey: String)

    suspend fun setCustomerId(customerId: String)

    suspend fun deleteCredentials()

    suspend fun getCredentials(): PremiumizeCredentials.PremiumizeCredential

    suspend fun hasCredentials(): Boolean
}
