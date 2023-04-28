package com.example.chatandroidapp

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.chatandroidapp.databinding.ActivityRegistrationBinding
import com.example.chatandroidapp.models.UserInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class RegistrationActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private var binding: ActivityRegistrationBinding? = null
    private var auth: FirebaseAuth = Firebase.auth
    private var fireStore = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        progressBar = binding?.progressBar2!!
        progressBar.visibility = View.INVISIBLE

        binding?.button?.setOnClickListener {

            val email = binding?.editTextTextEmailAddress?.text.toString()
            val password = binding?.editTextTextPassword?.text.toString()
            val name = binding?.editTextTextPersonName?.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty()){
                it.visibility = View.INVISIBLE
                progressBar.visibility = View.VISIBLE
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        try {
                            val user = registration(email, password)
                            val tasks = listOf(
                                launch { updateProfile(name) },
                                launch { createUserInfo(user.uid, email, name) },
                            )
                            tasks.joinAll()

                        } catch (e: Exception) {
                            it.visibility = View.VISIBLE
                            progressBar.visibility = View.INVISIBLE
                            showToast(e.message.toString())
                        }
                    }
                    startActivity(Intent(applicationContext, MainActivity::class.java))
                }
            }
            else{
                showToast("Field is empty")
            }

        }
    }



    private suspend fun registration(email: String, password: String) = withContext(Dispatchers.IO){
        auth.createUserWithEmailAndPassword(email, password).await().user!!
    }


    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }


    private suspend fun updateProfile(name: String) = withContext(Dispatchers.IO) {
        auth.currentUser!!.updateProfile(userProfileChangeRequest {
            displayName = name
            photoUri = Uri.EMPTY
        })
    }

    private suspend fun createUserInfo(id: String, email: String, name: String) =
        withContext(Dispatchers.IO) {
            val docRef = fireStore.collection("user-info").document(id)
            val userInfo = UserInfo(id, email, name)
            userInfo.chats["share-chat"] = "all"
            docRef.set(userInfo)
        }


    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}