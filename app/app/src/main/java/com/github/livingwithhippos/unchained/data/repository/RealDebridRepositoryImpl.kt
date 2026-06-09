package com.github.livingwithhippos.unchained.data.repository

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
import com.github.livingwithhippos.unchained.utilities.EitherResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealDebridRepositoryImpl
@Inject
constructor(
    private val torrentsRepository: TorrentsRepository,
    private val downloadRepository: DownloadRepository,
    private val unrestrictRepository: UnrestrictRepository,
    private val userRepository: UserRepository,
    private val hostsRepository: HostsRepository,
) : DebridRepository {

    override val provider: DebridProvider = DebridProvider.REAL_DEBRID

    override suspend fun addMagnet(
        magnet: String
    ): EitherResult<UnchainedNetworkException, UnchainedUploadResult> {
        val hosts = torrentsRepository.getAvailableHosts()
        val host = hosts?.firstOrNull()?.host ?: ""
        return when (val result = torrentsRepository.addMagnet(magnet, host)) {
            is EitherResult.Success ->
                EitherResult.Success(result.success.toUnchainedUploadResult())
            is EitherResult.Failure -> EitherResult.Failure(result.failure)
        }
    }

    override suspend fun addTorrent(
        binaryTorrent: ByteArray
    ): EitherResult<UnchainedNetworkException, UnchainedUploadResult> {
        val hosts = torrentsRepository.getAvailableHosts()
        val host = hosts?.firstOrNull()?.host ?: ""
        return when (val result = torrentsRepository.addTorrent(binaryTorrent, host)) {
            is EitherResult.Success ->
                EitherResult.Success(result.success.toUnchainedUploadResult())
            is EitherResult.Failure -> EitherResult.Failure(result.failure)
        }
    }

    override suspend fun getTransferList(
        offset: Int?,
        page: Int?,
        limit: Int?,
    ): List<UnchainedTransfer> =
        torrentsRepository.getTorrentsList(offset, page, limit).map { it.toUnchainedTransfer() }

    override suspend fun getTransferInfo(id: String): UnchainedTransfer? =
        torrentsRepository.getTorrentInfo(id)?.toUnchainedTransfer()

    override suspend fun deleteTransfer(
        id: String
    ): EitherResult<UnchainedNetworkException, Unit> = torrentsRepository.deleteTorrent(id)

    override suspend fun selectFiles(
        id: String,
        files: String,
    ): EitherResult<UnchainedNetworkException, Unit> =
        torrentsRepository.selectFiles(id, files)

    override fun supportsFileSelection(): Boolean = true

    override suspend fun getDownloads(
        offset: Int?,
        page: Int?,
        limit: Int?,
    ): List<UnchainedDownload> =
        downloadRepository
            .getDownloads(offset, page ?: 1, limit ?: 50)
            .map { it.toUnchainedDownload() }

    override suspend fun unrestrictLink(
        link: String,
        password: String?,
    ): EitherResult<UnchainedNetworkException, UnchainedDownload> =
        when (val result = unrestrictRepository.getEitherUnrestrictedLink(link, password)) {
            is EitherResult.Success ->
                EitherResult.Success(result.success.toUnchainedDownload())
            is EitherResult.Failure -> EitherResult.Failure(result.failure)
        }

    override suspend fun deleteDownload(id: String): Unit? =
        downloadRepository.deleteDownload(id)

    override suspend fun getUserInfo(): EitherResult<UnchainedNetworkException, UnchainedUser> {
        val token = torrentsRepository.getToken()
        return when (val result = userRepository.getUserOrError(token)) {
            is EitherResult.Success ->
                EitherResult.Success(result.success.toUnchainedUser())
            is EitherResult.Failure -> EitherResult.Failure(result.failure)
        }
    }

    override suspend fun checkCache(hashes: List<String>): List<CacheCheckResult> = emptyList()

    override fun supportsCacheCheck(): Boolean = false

    override suspend fun getHostsRegex(): List<String> =
        hostsRepository.getHostsRegex().map { it.regex }
}
