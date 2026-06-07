package com.github.livingwithhippos.unchained.data.remote.premiumize

import com.github.livingwithhippos.unchained.data.model.premiumize.PremiumizeAccountInfo
import com.github.livingwithhippos.unchained.data.model.premiumize.PremiumizeCacheCheckRawResponse
import com.github.livingwithhippos.unchained.data.model.premiumize.PremiumizeDeleteResponse
import com.github.livingwithhippos.unchained.data.model.premiumize.PremiumizeFolderListResponse
import com.github.livingwithhippos.unchained.data.model.premiumize.PremiumizeTransferCreateResponse
import com.github.livingwithhippos.unchained.data.model.premiumize.PremiumizeTransferListResponse
import okhttp3.MultipartBody
import retrofit2.Response
import javax.inject.Inject

// Transfer API Helper

interface PremiumizeTransferApiHelper {
    suspend fun createTransfer(token: String, src: String): Response<PremiumizeTransferCreateResponse>
    suspend fun createTransferFromFile(token: String, file: MultipartBody.Part): Response<PremiumizeTransferCreateResponse>
    suspend fun getTransferList(token: String): Response<PremiumizeTransferListResponse>
    suspend fun deleteTransfer(token: String, id: String): Response<PremiumizeDeleteResponse>
}

class PremiumizeTransferApiHelperImpl
@Inject
constructor(private val api: PremiumizeTransferApi) : PremiumizeTransferApiHelper {
    override suspend fun createTransfer(token: String, src: String) =
        api.createTransfer(token, src)
    override suspend fun createTransferFromFile(token: String, file: MultipartBody.Part) =
        api.createTransferFromFile(token, file)
    override suspend fun getTransferList(token: String) = api.getTransferList(token)
    override suspend fun deleteTransfer(token: String, id: String) =
        api.deleteTransfer(token, id)
}

// Folder API Helper

interface PremiumizeFolderApiHelper {
    suspend fun listFolder(token: String, id: String?): Response<PremiumizeFolderListResponse>
}

class PremiumizeFolderApiHelperImpl
@Inject
constructor(private val api: PremiumizeFolderApi) : PremiumizeFolderApiHelper {
    override suspend fun listFolder(token: String, id: String?) = api.listFolder(token, id)
}

// Cache API Helper

interface PremiumizeCacheApiHelper {
    suspend fun checkCache(token: String, items: List<String>): Response<PremiumizeCacheCheckRawResponse>
}

class PremiumizeCacheApiHelperImpl
@Inject
constructor(private val api: PremiumizeCacheApi) : PremiumizeCacheApiHelper {
    override suspend fun checkCache(token: String, items: List<String>) =
        api.checkCache(token, items)
}

// Account API Helper

interface PremiumizeAccountApiHelper {
    suspend fun getAccountInfo(token: String): Response<PremiumizeAccountInfo>
}

class PremiumizeAccountApiHelperImpl
@Inject
constructor(private val api: PremiumizeAccountApi) : PremiumizeAccountApiHelper {
    override suspend fun getAccountInfo(token: String) = api.getAccountInfo(token)
}
