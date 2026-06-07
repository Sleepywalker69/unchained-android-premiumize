package com.github.livingwithhippos.unchained.data.local

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.github.livingwithhippos.unchained.data.local.PremiumizeCredentials.PremiumizeCredential
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object PremiumizeCredentialSerializer : Serializer<PremiumizeCredential> {
    override val defaultValue: PremiumizeCredential = PremiumizeCredential.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): PremiumizeCredential {
        try {
            return PremiumizeCredential.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: PremiumizeCredential, output: OutputStream) = t.writeTo(output)
}

val Context.premiumizeCredentialsDataStore: DataStore<PremiumizeCredential> by
    dataStore(
        fileName = "premiumize_credentials.pb",
        serializer = PremiumizeCredentialSerializer,
    )
