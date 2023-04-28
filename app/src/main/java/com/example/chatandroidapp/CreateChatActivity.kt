package com.example.chatandroidapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.chatandroidapp.databinding.ActivityCreateChatBinding
import com.example.chatandroidapp.models.Chat
import com.example.chatandroidapp.models.UserInfo
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class CreateChatActivity : AppCompatActivity() {

    lateinit var binding: ActivityCreateChatBinding
    private val auth = Firebase.auth
    private val fireStore = Firebase.firestore
    private val firebaseRef =
        Firebase.database.getReference("chats")
    private val scope: CoroutineScope = CoroutineScope(Job() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userInfoRefCollections = fireStore.collection("user-info")

        //Create user list adapter
        val adapter = UserInfoListAdapter()
        adapter.itemClick {
            createChat(userInfoRefCollections, it.email)
        }
        binding.userListRecycler.adapter = adapter


        //Insert users from firebase to adapter and update list
        lifecycleScope.launch(Dispatchers.Main) {
            val users: List<UserInfo> = withContext(Dispatchers.IO) {
                userInfoRefCollections.get().await().documents.map {
                    it.toObject(UserInfo::class.java)!!
                }
            }

            adapter.items = users
        }


        binding.button3.setOnClickListener {
            val email = binding.editTextTextEmailAddress2.text.toString()
            createChat(userInfoRefCollections, email)

        }
    }

    private fun createChat(
        collections: CollectionReference,
        email: String,
    ) {
        lifecycleScope.launch {
            val chat = Chat()
            withContext(Dispatchers.IO) {
                try {
                    val task = collections.whereEqualTo("email", email).get().await()
                    if (task.isEmpty) {
                        showToast("User not found!!!")
                    } else {
                        val companion = task.documents.map {
                            it.toObject(UserInfo::class.java)!!
                        }.first()
                        val user = collections.document(auth.currentUser?.uid!!).get().await()
                            .toObject(UserInfo::class.java)!!

                        if (checkChatExist(
                                companion.chats.keys,
                                user.chats.keys
                            )
                        ) throw Exception("Chat with this user already exist!!!")


                        chat.id = firebaseRef.push().key!!
                        firebaseRef.child(chat.id).setValue(chat).await()
                        firebaseRef.child(chat.id).child("messages").setValue(chat.messages)
                            .await()
                        companion.chats[chat.id] = user.id
                        user.chats[chat.id] = companion.id
                        collections.document(companion.id).set(companion).await()
                        collections.document(user.id).set(user).await()
                        goToChat(chat.id)

                    }

                } catch (e: Exception) {
                    showToast(e.message.toString())
                }
            }

        }
    }

    private suspend fun showToast(message: String) = withContext(Dispatchers.Main) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun checkChatExist(
        chatsId: MutableSet<String>,
        chatsId2: MutableSet<String>
    ): Boolean {
        for (i in chatsId) {
            for (j in chatsId2) {
                if (i == j) {
                    if (i == "share-chat") continue
                    return true
                }
            }
        }
        return false
    }

    private suspend fun goToChat(id: String) = withContext(Dispatchers.Main) {
        val intent = Intent(applicationContext, ChatActivity::class.java)
        intent.putExtra("id", id)
        startActivity(intent)
    }

}