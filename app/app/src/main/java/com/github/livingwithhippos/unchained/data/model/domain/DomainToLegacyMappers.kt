package com.github.livingwithhippos.unchained.data.model.domain

import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.InnerTorrentFile
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.UploadedTorrent
import com.github.livingwithhippos.unchained.data.model.User

/**
 * Mappers from the provider-agnostic domain models back to the legacy Real Debrid DTOs.
 *
 * The UI layer (adapters, navigation Safe Args, data binding layouts) is still built around
 * [TorrentItem] and [DownloadItem]. Mapping the domain models back at the ViewModel boundary lets
 * every screen work with any provider without touching the view layer. The status strings produced
 * here follow the Real Debrid vocabulary that the UI already understands.
 */
fun UnchainedTransfer.toTorrentItem(): TorrentItem =
    TorrentItem(
        id = id,
        filename = name,
        originalFilename = null,
        hash = hash ?: "",
        bytes = sizeBytes,
        originalBytes = null,
        host = providerHost(provider),
        split = 0,
        progress = progress,
        status = status.toLegacyStatus(),
        added = addedDate ?: "",
        files =
            files?.map { f ->
                InnerTorrentFile(
                    id = f.id,
                    path = f.path,
                    bytes = f.sizeBytes,
                    selected = if (f.selected) 1 else 0,
                )
            },
        links = links,
        ended = completedDate,
        speed = speed,
        seeders = seeders,
    )

fun UnchainedDownload.toDownloadItem(): DownloadItem =
    DownloadItem(
        id = id,
        filename = filename,
        mimeType = mimeType,
        fileSize = sizeBytes,
        link = link,
        host = providerHost(provider),
        hostIcon = null,
        chunks = 0,
        crc = null,
        download = downloadUrl,
        streamable = if (streamable) 1 else 0,
        generated = generatedDate,
        type = null,
        alternative = null,
    )

fun UnchainedUploadResult.toUploadedTorrent(): UploadedTorrent =
    UploadedTorrent(id = id, uri = "")

fun UnchainedUser.toUser(): User =
    User(
        id = id.toIntOrNull() ?: 0,
        username = username,
        email = email ?: "",
        points = 0,
        locale = "",
        avatar = "",
        type = if (isPremium) "premium" else "free",
        premium = if (isPremium) 1 else 0,
        expiration = expirationDate ?: "",
    )

fun TransferStatus.toLegacyStatus(): String =
    when (this) {
        TransferStatus.QUEUED -> "queued"
        TransferStatus.DOWNLOADING -> "downloading"
        TransferStatus.COMPLETED -> "downloaded"
        TransferStatus.ERROR -> "error"
        TransferStatus.WAITING_FILES_SELECTION -> "waiting_files_selection"
        TransferStatus.MAGNET_CONVERSION -> "magnet_conversion"
        TransferStatus.COMPRESSING -> "compressing"
        TransferStatus.UPLOADING -> "uploading"
        TransferStatus.DEAD -> "dead"
        TransferStatus.VIRUS -> "virus"
        TransferStatus.UNKNOWN -> "unknown"
    }

private fun providerHost(provider: DebridProvider): String =
    when (provider) {
        DebridProvider.REAL_DEBRID -> "real-debrid.com"
        DebridProvider.PREMIUMIZE -> "premiumize.me"
    }
