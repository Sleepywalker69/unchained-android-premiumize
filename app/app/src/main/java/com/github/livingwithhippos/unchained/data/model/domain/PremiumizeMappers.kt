package com.github.livingwithhippos.unchained.data.model.domain

import com.github.livingwithhippos.unchained.data.model.premiumize.PremiumizeAccountInfo
import com.github.livingwithhippos.unchained.data.model.premiumize.PremiumizeFolderItem
import com.github.livingwithhippos.unchained.data.model.premiumize.PremiumizeTransfer
import com.github.livingwithhippos.unchained.data.model.premiumize.PremiumizeTransferCreateResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun PremiumizeTransfer.toUnchainedTransfer(): UnchainedTransfer =
    UnchainedTransfer(
        id = id,
        name = name ?: "",
        hash = null,
        sizeBytes = 0L,
        progress = (progress ?: 0f) * 100f,
        status = mapPremiumizeStatus(status),
        addedDate = null,
        completedDate = null,
        speed = null,
        seeders = null,
        links = emptyList(),
        files = null,
        provider = DebridProvider.PREMIUMIZE,
    )

fun PremiumizeTransferCreateResponse.toUnchainedUploadResult(): UnchainedUploadResult =
    UnchainedUploadResult(
        id = id ?: "",
        provider = DebridProvider.PREMIUMIZE,
    )

fun PremiumizeFolderItem.toUnchainedDownload(): UnchainedDownload =
    UnchainedDownload(
        id = id,
        filename = name,
        mimeType = mimeType,
        sizeBytes = size ?: 0L,
        link = link ?: "",
        downloadUrl = link ?: streamLink ?: "",
        streamable = streamLink != null,
        generatedDate =
            createdAt?.let {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date(it * 1000))
            },
        provider = DebridProvider.PREMIUMIZE,
    )

fun PremiumizeAccountInfo.toUnchainedUser(): UnchainedUser =
    UnchainedUser(
        id = customerId ?: "",
        username = customerId ?: "",
        email = null,
        isPremium = premiumUntil != null && premiumUntil > System.currentTimeMillis() / 1000,
        expirationDate =
            premiumUntil?.let {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date(it * 1000))
            },
        provider = DebridProvider.PREMIUMIZE,
    )

private fun mapPremiumizeStatus(status: String?): TransferStatus =
    when (status) {
        "waiting" -> TransferStatus.QUEUED
        "running" -> TransferStatus.DOWNLOADING
        "finished" -> TransferStatus.COMPLETED
        "error" -> TransferStatus.ERROR
        "deleted" -> TransferStatus.DEAD
        "banned" -> TransferStatus.ERROR
        "timeout" -> TransferStatus.ERROR
        "seeding" -> TransferStatus.COMPLETED
        "queued" -> TransferStatus.QUEUED
        else -> TransferStatus.UNKNOWN
    }
