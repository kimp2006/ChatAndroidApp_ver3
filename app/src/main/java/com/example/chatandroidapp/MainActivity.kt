package com.example.chatandroidapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.chatandroidapp.databinding.ActivityMainBinding
import com.example.chatandroidapp.models.Chat
import com.example.chatandroidapp.models.Message
import com.example.chatandroidapp.models.UserInfo
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth = Firebase.auth
    private val fireStore = Firebase.firestore
    private val firebaseChatsRef = Firebase.database.getReference("chats")
    private lateinit var adapter: ChatListAdapter

    private var chats: MutableList<Chat> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.floatingActionButton.visibility = View.INVISIBLE
        initFloatingButton()
        adapter = ChatListAdapter()

        adapter.itemClick {
            val intent = Intent(applicationContext, ChatActivity::class.java)
            intent.putExtra("id", it.id)
            startActivity(intent)
        }

        binding.recyclerView.adapter = adapter


        lifecycleScope.launch(Dispatchers.Main) {
            initChats()
            adapter.items = chats
            binding.progressBar.visibility = View.INVISIBLE
            binding.floatingActionButton.visibility = View.VISIBLE

        }


        binding.logoutContainer.setOnClickListener {
            auth.signOut()
            startActivity(Intent(applicationContext, LoginActivity::class.java))
        }

    }


    private suspend fun initChats() = withContext(Dispatchers.Default){
                val user = getUser()
                for (c in user.chats) {
                    val chat = getChat(c.key)
                    val companion = getCompanion(c.value)
                    chat.companion = companion?.email ?: "Share chat"
                    chats.add(chat)
                }
    }

    private suspend fun getCompanion(userId: String): UserInfo? = withContext(Dispatchers.IO) {
        val companionDocument = fireStore.collection("user-info").document(userId)
        val companion = companionDocument.get().await().toObject<UserInfo>()
        companion

    }

    private suspend fun getChat(c: String) = withContext(Dispatchers.IO) {
        firebaseChatsRef.child(c)
            .get()
            .await()
            .getValue(Chat::class.java) ?: Chat()
    }


    private suspend fun getUser() = withContext(Dispatchers.IO) {
        fireStore
            .collection("user-info")
            .document(auth.currentUser?.uid!!)
            .get()
            .await()
            .toObject(UserInfo::class.java)!!
    }


    private fun initFloatingButton() {
        binding.floatingActionButton.setOnClickListener {
            startActivity(Intent(applicationContext, CreateChatActivity::class.java))
        }
    }


}