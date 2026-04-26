package com.example.promptbooks

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface GPTService {
    @Headers(
        "Content-Type: application/json",
        "HTTP-Referer: https://promptbooks.local",
        "X-Title: PromptBooks"
    )
    @POST("api/v1/chat/completions")
    fun sendPrompt(
        @Header("Authorization") authorization: String,
        @Body body: GPTRequest
    ): Call<GPTResponse>
}
