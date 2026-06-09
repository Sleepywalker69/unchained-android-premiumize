package com.github.livingwithhippos.unchained.data.model.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class DebridProvider {
    REAL_DEBRID,
    PREMIUMIZE,
}

enum class TransferStatus {
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    ERROR,
    WAITING_FILES_SELECTION,
    MAGNET_CONVERSION,
    COMPRESSING,
    UPLOADING,
    DEAD,
    VIRUS,
    UNKNOWN,
}

@Parcelize
data class UnchainedTransfer(
    val id: String,
    val name: String,
    val hash: String?,
    val sizeBytes: Long,
    val progress: Float,
    val status: TransferStatus,
    val addedDate: String?,
    val completedDate: String?,
    val speed: Int?,
    val seeders: Int?,
    val links: List<String>,
    val files: List<UnchainedFile>?,
    val provider: DebridProvider,
) : Parcelable

@Parcelize
data class UnchainedFile(
    val id: Int,
    val path: String,
    val sizeBytes: Long,
    val selected: Boolean,
) : Parcelable

@Parcelize
data class UnchainedDownload(
    val id: String,
    val filename: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val link: String,
    val downloadUrl: String,
    val streamable: Boolean,
    val generatedDate: String?,
    val provider: DebridProvider,
) : Parcelable

@Parcelize
data class UnchainedUser(
    val id: String,
    val username: String,
    val email: String?,
    val isPremium: Boolean,
    val expirationDate: String?,
    val provider: DebridProvider,
) : Parcelable

@Parcelize
data class UnchainedUploadResult(
    val id: String,
    val provider: DebridProvider,
) : Parcelable

data class CacheCheckResult(
    val hash: String,
    val isCached: Boolean,
    val filename: String?,
    val filesize: Long?,
)
