package com.github.livingwithhippos.unchained.data.remote.premiumize

import com.github.livingwithhippos.unchained.data.model.premiumize.PremiumizeAccountInfo
import com.github.livingwithhippos.unchained.data.model.premiumize.PremiumizeCacheCheckRawResponse
import com.github.livingwithhippos.unchained.data.model.premiumize.PremiumizeDeleteResponse
import com.github.livingwithhippos.unchained.data.model.premiumize.PremiumizeFolderListResponse
import com.github.livingwithhippos.unchained.data.model.premiumize.PremiumizeToken
import com.github.livingwithhippos.unchained.data.model.premiumize.PremiumizeTransferCreateResponse
import com.github.livingwithhippos.unchained.data.model.premiumize.PremiumizeTransferListResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface PremiumizeTransferApi {

    @FormUrlEncoded
    @POST("api/transfer/create")
    suspend fun createTransfer(
        @Header("Authorization") token: String,
        @Field("src") src: String,
    ): Response<PremiumizeTransferCreateResponse>

    @Multipart
    @POST("api/transfer/create")
    suspend fun createTransferFromFile(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part,
    ): Response<PremiumizeTransferCreateResponse>

    @GET("api/transfer/list")
    suspend fun getTransferList(
        @Header("Authorization") token: String,
    ): Response<PremiumizeTransferListResponse>

    @FormUrlEncoded
    @POST("api/transfer/delete")
    suspend fun deleteTransfer(
        @Header("Authorization") token: String,
        @Field("id") id: String,
    ): Response<PremiumizeDeleteResponse>
}

interface PremiumizeFolderApi {

    @GET("api/folder/list")
    suspend fun listFolder(
        @Header("Authorization") token: String,
        @Query("id") id: String? = null,
        @Query("includebreadcrumbs") includeBreadcrumbs: String? = null,
    ): Response<PremiumizeFolderListResponse>
}

interface PremiumizeCacheApi {

    @FormUrlEncoded
    @POST("api/cache/check")
    suspend fun checkCache(
        @Header("Authorization") token: String,
        @Field("items[]") items: List<String>,
    ): Response<PremiumizeCacheCheckRawResponse>
}

interface PremiumizeAccountApi {

    @GET("api/account/info")
    suspend fun getAccountInfo(
        @Header("Authorization") token: String,
    ): Response<PremiumizeAccountInfo>
}

interface PremiumizeAuthApi {

    @FormUrlEncoded
    @POST("token")
    suspend fun getToken(
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("code") code: String,
        @Field("client_id") clientId: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("code_verifier") codeVerifier: String,
    ): Response<PremiumizeToken>

    @FormUrlEncoded
    @POST("token")
    suspend fun refreshToken(
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String,
        @Field("client_id") clientId: String,
    ): Response<PremiumizeToken>
}
