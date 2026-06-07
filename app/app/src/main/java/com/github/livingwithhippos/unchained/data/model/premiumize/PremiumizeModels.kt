package com.github.livingwithhippos.unchained.data.model.premiumize

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PremiumizeTransferCreateResponse(
    @Json(name = "status") val status: String,
    @Json(name = "id") val id: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "type") val type: String?,
    @Json(name = "message") val message: String?,
)

@JsonClass(generateAdapter = true)
data class PremiumizeTransfer(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String?,
    @Json(name = "message") val message: String?,
    @Json(name = "status") val status: String?,
    @Json(name = "progress") val progress: Float?,
    @Json(name = "src") val src: String?,
    @Json(name = "file_id") val fileId: String?,
    @Json(name = "folder_id") val folderId: String?,
)

@JsonClass(generateAdapter = true)
data class PremiumizeTransferListResponse(
    @Json(name = "status") val status: String,
    @Json(name = "transfers") val transfers: List<PremiumizeTransfer>?,
)

@JsonClass(generateAdapter = true)
data class PremiumizeFolderItem(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "type") val type: String,
    @Json(name = "size") val size: Long?,
    @Json(name = "link") val link: String?,
    @Json(name = "stream_link") val streamLink: String?,
    @Json(name = "mime_type") val mimeType: String?,
    @Json(name = "created_at") val createdAt: Long?,
)

@JsonClass(generateAdapter = true)
data class PremiumizeFolderListResponse(
    @Json(name = "status") val status: String,
    @Json(name = "content") val content: List<PremiumizeFolderItem>?,
    @Json(name = "name") val name: String?,
    @Json(name = "parent_id") val parentId: String?,
)

@JsonClass(generateAdapter = true)
data class PremiumizeAccountInfo(
    @Json(name = "status") val status: String,
    @Json(name = "customer_id") val customerId: String?,
    @Json(name = "premium_until") val premiumUntil: Long?,
    @Json(name = "limit_used") val limitUsed: Double?,
    @Json(name = "space_used") val spaceUsed: Double?,
)

@JsonClass(generateAdapter = true)
data class PremiumizeToken(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String?,
    @Json(name = "expires_in") val expiresIn: Int?,
    @Json(name = "refresh_token") val refreshToken: String?,
)

@JsonClass(generateAdapter = true)
data class PremiumizeDeleteResponse(
    @Json(name = "status") val status: String,
    @Json(name = "message") val message: String?,
)

@JsonClass(generateAdapter = true)
data class PremiumizeCacheCheckRawResponse(
    @Json(name = "status") val status: String,
    @Json(name = "response") val response: List<Boolean>?,
    @Json(name = "filename") val filename: List<String?>?,
    @Json(name = "filesize") val filesize: List<String?>?,
)
