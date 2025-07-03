package com.example.trabajoenclase2

import com.google.gson.Gson

data class Message(
    val username: String,
    val content: String,
    val timestamp: Long
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun fromJson(json: String): Message {
            return Gson().fromJson(json, Message::class.java)
        }
    }
}