package com.soundlog.app.data.remote.telegram

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Url

interface TelegramBotApi {

    @FormUrlEncoded
    @POST
    suspend fun sendMessage(
        @Url url: String,
        @Field("chat_id") chatId: String,
        @Field("text") text: String,
        @Field("parse_mode") parseMode: String? = "HTML"
    ): Response<TelegramMessageResponse>

    @Multipart
    @POST
    suspend fun sendPhoto(
        @Url url: String,
        @Part("chat_id") chatId: RequestBody,
        @Part photo: MultipartBody.Part,
        @Part("caption") caption: RequestBody? = null,
        @Part("parse_mode") parseMode: RequestBody? = null
    ): Response<TelegramMessageResponse>

    @FormUrlEncoded
    @POST
    suspend fun sendPhotoUrl(
        @Url url: String,
        @Field("chat_id") chatId: String,
        @Field("photo") photoUrl: String,
        @Field("caption") caption: String? = null,
        @Field("parse_mode") parseMode: String? = "HTML"
    ): Response<TelegramMessageResponse>
}

data class TelegramMessageResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("result") val result: TelegramMessageResult?,
    @SerializedName("description") val description: String?
)

data class TelegramMessageResult(
    @SerializedName("message_id") val messageId: Long
)
