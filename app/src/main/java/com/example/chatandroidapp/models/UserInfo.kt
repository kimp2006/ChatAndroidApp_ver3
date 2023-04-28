package com.example.chatandroidapp.models

data class UserInfo(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val chats: HashMap<String, String> = hashMapOf()
)
