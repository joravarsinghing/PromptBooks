package com.example.promptbooks

data class GPTRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = false  // 👈 required by OpenRouter
)

data class Message(
    val role: String,
    val content: String
)
