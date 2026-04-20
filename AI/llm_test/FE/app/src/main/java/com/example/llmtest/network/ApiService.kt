package com.example.llmtest.network

import com.example.llmtest.network.models.LLMResponse
import com.example.llmtest.network.models.STTRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {

    @POST("/api/chat/android_stt")
    suspend fun chatWithAndroidSTT(
        @Body request: STTRequest
    ): LLMResponse

    @Multipart
    @POST("/api/chat/whisper_stt")
    suspend fun chatWithWhisperSTT(
        @Part audio: MultipartBody.Part,
        @Part("model_name") modelName: RequestBody,
        @Part("conversation_state") conversationState: RequestBody
    ): LLMResponse
}
