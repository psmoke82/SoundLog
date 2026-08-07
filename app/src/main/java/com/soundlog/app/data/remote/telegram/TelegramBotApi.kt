package com.soundlog.app.data.remote.telegram

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
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
}

data class TelegramMessageResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("result") val result: TelegramMessageResult?,
    @SerializedName("description") val description: String?
)

data class TelegramMessageResult(
    @SerializedName("message_id") val messageId: Long
)
