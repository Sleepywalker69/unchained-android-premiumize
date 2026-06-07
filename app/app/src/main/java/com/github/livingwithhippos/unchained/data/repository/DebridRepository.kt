package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.model.domain.CacheCheckResult
import com.github.livingwithhippos.unchained.data.model.domain.DebridProvider
import com.github.livingwithhippos.unchained.data.model.domain.UnchainedDownload
import com.github.livingwithhippos.unchained.data.model.domain.UnchainedTransfer
import com.github.livingwithhippos.unchained.data.model.domain.UnchainedUploadResult
import com.github.livingwithhippos.unchained.data.model.domain.UnchainedUser
import com.github.livingwithhippos.unchained.utilities.EitherResult

interface DebridRepository {

    val provider: DebridProvider

    // Transfers (torrents/magnets)

    suspend fun addMagnet(magnet: String): EitherResult<UnchainedNetworkException, UnchainedUploadResult>

    suspend fun addTorrent(binaryTorrent: ByteArray): EitherResult<UnchainedNetworkException, UnchainedUploadResult>

    suspend fun getTransferList(
        offset: Int? = null,
        page: Int? = null,
        limit: Int? = null,
    ): List<UnchainedTransfer>

    suspend fun getTransferInfo(id: String): UnchainedTransfer?

    suspend fun deleteTransfer(id: String): EitherResult<UnchainedNetworkException, Unit>

    suspend fun selectFiles(
        id: String,
        files: String = "all",
    ): EitherResult<UnchainedNetworkException, Unit>

    fun supportsFileSelection(): Boolean

    // Downloads / Unrestrict

    suspend fun getDownloads(
        offset: Int? = null,
        page: Int? = null,
        limit: Int? = null,
    ): List<UnchainedDownload>

    suspend fun unrestrictLink(
        link: String,
        password: String? = null,
    ): EitherResult<UnchainedNetworkException, UnchainedDownload>

    suspend fun deleteDownload(id: String): Unit?

    // User

    suspend fun getUserInfo(): EitherResult<UnchainedNetworkException, UnchainedUser>

    // Cache

    suspend fun checkCache(hashes: List<String>): List<CacheCheckResult>

    fun supportsCacheCheck(): Boolean

    // Hosts

    suspend fun getHostsRegex(): List<String>
}
