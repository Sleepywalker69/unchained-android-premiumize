package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.local.PremiumizeStore
import com.github.livingwithhippos.unchained.data.model.APIError
import com.github.livingwithhippos.unchained.data.model.NetworkError
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.model.domain.CacheCheckResult
import com.github.livingwithhippos.unchained.data.model.domain.DebridProvider
import com.github.livingwithhippos.unchained.data.model.domain.UnchainedDownload
import com.github.livingwithhippos.unchained.data.model.domain.UnchainedTransfer
import com.github.livingwithhippos.unchained.data.model.domain.UnchainedUploadResult
import com.github.livingwithhippos.unchained.data.model.domain.UnchainedUser
import com.github.livingwithhippos.unchained.data.model.domain.toUnchainedDownload
import com.github.livingwithhippos.unchained.data.model.domain.toUnchainedTransfer
import com.github.livingwithhippos.unchained.data.model.domain.toUnchainedUploadResult
import com.github.livingwithhippos.unchained.data.model.domain.toUnchainedUser
import com.github.livingwithhippos.unchained.data.model.premiumize.CacheCheckAdapter
import com.github.livingwithhippos.unchained.data.remote.premiumize.PremiumizeAccountApiHelper
import com.github.livingwithhippos.unchained.data.remote.premiumize.PremiumizeCacheApiHelper
import com.github.livingwithhippos.unchained.data.remote.premiumize.PremiumizeFolderApiHelper
import com.github.livingwithhippos.unchained.data.remote.premiumize.PremiumizeTransferApiHelper
import com.github.livingwithhippos.unchained.utilities.EitherResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

@Singleton
class PremiumizeRepositoryImpl
@Inject
constructor(
    private val premiumizeStore: PremiumizeStore,
    private val transferApiHelper: PremiumizeTransferApiHelper,
    private val folderApiHelper: PremiumizeFolderApiHelper,
    private val cacheApiHelper: PremiumizeCacheApiHelper,
    private val accountApiHelper: PremiumizeAccountApiHelper,
) : DebridRepository {

    override val provider: DebridProvider = DebridProvider.PREMIUMIZE

    private suspend fun getToken(): String {
        val creds = premiumizeStore.getCredentials()
        val token = creds.apiKey.ifBlank { creds.accessToken }
        if (token.isBlank()) throw IllegalArgumentException("No Premiumize credentials found")
        return token
    }

    private suspend fun bearerToken(): String = "Bearer ${getToken()}"

    override suspend fun addMagnet(
        magnet: String
    ): EitherResult<UnchainedNetworkException, UnchainedUploadResult> =
        withContext(Dispatchers.IO) {
            try {
                val response = transferApiHelper.createTransfer(bearerToken(), magnet)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.status == "success") {
                        EitherResult.Success(body.toUnchainedUploadResult())
                    } else {
                        EitherResult.Failure(
                            APIError(
                                body?.message ?: "Transfer creation failed",
                                null,
                                null,
                            )
                        )
                    }
                } else {
                    EitherResult.Failure(
                        NetworkError(response.code(), "Premiumize transfer creation failed")
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error creating Premiumize transfer")
                EitherResult.Failure(NetworkError(-1, e.message ?: "Unknown error"))
            }
        }

    override suspend fun addTorrent(
        binaryTorrent: ByteArray
    ): EitherResult<UnchainedNetworkException, UnchainedUploadResult> =
        withContext(Dispatchers.IO) {
            try {
                val requestBody =
                    binaryTorrent.toRequestBody(
                        "application/x-bittorrent".toMediaTypeOrNull(),
                        0,
                        binaryTorrent.size,
                    )
                val part =
                    MultipartBody.Part.createFormData("file", "upload.torrent", requestBody)
                val response = transferApiHelper.createTransferFromFile(bearerToken(), part)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.status == "success") {
                        EitherResult.Success(body.toUnchainedUploadResult())
                    } else {
                        EitherResult.Failure(
                            APIError(
                                body?.message ?: "Torrent upload failed",
                                null,
                                null,
                            )
                        )
                    }
                } else {
                    EitherResult.Failure(
                        NetworkError(response.code(), "Premiumize torrent upload failed")
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error uploading torrent to Premiumize")
                EitherResult.Failure(NetworkError(-1, e.message ?: "Unknown error"))
            }
        }

    override suspend fun getTransferList(
        offset: Int?,
        page: Int?,
        limit: Int?,
    ): List<UnchainedTransfer> =
        withContext(Dispatchers.IO) {
            try {
                val response = transferApiHelper.getTransferList(bearerToken())
                if (response.isSuccessful) {
                    response.body()?.transfers?.map { it.toUnchainedTransfer() } ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching Premiumize transfer list")
                emptyList()
            }
        }

    override suspend fun getTransferInfo(id: String): UnchainedTransfer? =
        getTransferList().find { it.id == id }

    override suspend fun deleteTransfer(
        id: String
    ): EitherResult<UnchainedNetworkException, Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = transferApiHelper.deleteTransfer(bearerToken(), id)
                if (response.isSuccessful && response.body()?.status == "success") {
                    EitherResult.Success(Unit)
                } else {
                    EitherResult.Failure(
                        NetworkError(
                            response.code(),
                            response.body()?.message ?: "Delete failed",
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting Premiumize transfer")
                EitherResult.Failure(NetworkError(-1, e.message ?: "Unknown error"))
            }
        }

    override suspend fun selectFiles(
        id: String,
        files: String,
    ): EitherResult<UnchainedNetworkException, Unit> = EitherResult.Success(Unit)

    override fun supportsFileSelection(): Boolean = false

    override suspend fun getDownloads(
        offset: Int?,
        page: Int?,
        limit: Int?,
    ): List<UnchainedDownload> =
        withContext(Dispatchers.IO) {
            try {
                val response = folderApiHelper.listFolder(bearerToken(), null)
                if (response.isSuccessful) {
                    response
                        .body()
                        ?.content
                        ?.filter { it.type == "file" }
                        ?.map { it.toUnchainedDownload() }
                        ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching Premiumize downloads")
                emptyList()
            }
        }

    override suspend fun unrestrictLink(
        link: String,
        password: String?,
    ): EitherResult<UnchainedNetworkException, UnchainedDownload> =
        when (val result = addMagnet(link)) {
            is EitherResult.Success -> {
                val transfer = getTransferInfo(result.success.id)
                if (transfer != null) {
                    EitherResult.Success(
                        UnchainedDownload(
                            id = transfer.id,
                            filename = transfer.name,
                            mimeType = null,
                            sizeBytes = transfer.sizeBytes,
                            link = link,
                            downloadUrl = link,
                            streamable = false,
                            generatedDate = null,
                            provider = DebridProvider.PREMIUMIZE,
                        )
                    )
                } else {
                    EitherResult.Success(
                        UnchainedDownload(
                            id = result.success.id,
                            filename = "",
                            mimeType = null,
                            sizeBytes = 0,
                            link = link,
                            downloadUrl = link,
                            streamable = false,
                            generatedDate = null,
                            provider = DebridProvider.PREMIUMIZE,
                        )
                    )
                }
            }
            is EitherResult.Failure -> EitherResult.Failure(result.failure)
        }

    override suspend fun deleteDownload(id: String): Unit? {
        deleteTransfer(id)
        return Unit
    }

    override suspend fun getUserInfo(): EitherResult<UnchainedNetworkException, UnchainedUser> =
        withContext(Dispatchers.IO) {
            try {
                val response = accountApiHelper.getAccountInfo(bearerToken())
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.status == "success") {
                        EitherResult.Success(body.toUnchainedUser())
                    } else {
                        EitherResult.Failure(
                            APIError("Failed to fetch account info", null, null)
                        )
                    }
                } else {
                    EitherResult.Failure(
                        NetworkError(response.code(), "Failed to fetch Premiumize account info")
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching Premiumize account info")
                EitherResult.Failure(NetworkError(-1, e.message ?: "Unknown error"))
            }
        }

    override suspend fun checkCache(hashes: List<String>): List<CacheCheckResult> =
        withContext(Dispatchers.IO) {
            try {
                val response = cacheApiHelper.checkCache(bearerToken(), hashes)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.status == "success") {
                        CacheCheckAdapter.parseResponse(body, hashes)
                    } else {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking Premiumize cache")
                emptyList()
            }
        }

    override fun supportsCacheCheck(): Boolean = true

    override suspend fun getHostsRegex(): List<String> = emptyList()
}
