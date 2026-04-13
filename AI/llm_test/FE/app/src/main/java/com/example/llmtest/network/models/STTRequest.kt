package com.example.llmtest.network.models

data class STTRequest(
    val model_name: String,
    val message: String,
    val session_id: String
)
