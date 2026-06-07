package com.github.livingwithhippos.unchained.data.model.domain

import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.UploadedTorrent
import com.github.livingwithhippos.unchained.data.model.User

fun TorrentItem.toUnchainedTransfer(): UnchainedTransfer =
    UnchainedTransfer(
        id = id,
        name = filename,
        hash = hash,
        sizeBytes = bytes,
        progress = progress,
        status = mapRealDebridStatus(status),
        addedDate = added,
        completedDate = ended,
        speed = speed,
        seeders = seeders,
        links = links,
        files =
            files?.map { f ->
                UnchainedFile(
                    id = f.id,
                    path = f.path,
                    sizeBytes = f.bytes,
                    selected = f.selected == 1,
                )
            },
        provider = DebridProvider.REAL_DEBRID,
    )

fun DownloadItem.toUnchainedDownload(): UnchainedDownload =
    UnchainedDownload(
        id = id,
        filename = filename,
        mimeType = mimeType,
        sizeBytes = fileSize,
        link = link,
        downloadUrl = download,
        streamable = streamable == 1,
        generatedDate = generated,
        provider = DebridProvider.REAL_DEBRID,
    )

fun User.toUnchainedUser(): UnchainedUser =
    UnchainedUser(
        id = id.toString(),
        username = username,
        email = email,
        isPremium = premium > 0,
        expirationDate = expiration,
        provider = DebridProvider.REAL_DEBRID,
    )

fun UploadedTorrent.toUnchainedUploadResult(): UnchainedUploadResult =
    UnchainedUploadResult(
        id = id,
        provider = DebridProvider.REAL_DEBRID,
    )

private fun mapRealDebridStatus(status: String): TransferStatus =
    when (status) {
        "magnet_error" -> TransferStatus.ERROR
        "magnet_conversion" -> TransferStatus.MAGNET_CONVERSION
        "waiting_files_selection" -> TransferStatus.WAITING_FILES_SELECTION
        "queued" -> TransferStatus.QUEUED
        "downloading" -> TransferStatus.DOWNLOADING
        "downloaded" -> TransferStatus.COMPLETED
        "error" -> TransferStatus.ERROR
        "virus" -> TransferStatus.VIRUS
        "compressing" -> TransferStatus.COMPRESSING
        "uploading" -> TransferStatus.UPLOADING
        "dead" -> TransferStatus.DEAD
        else -> TransferStatus.UNKNOWN
    }
