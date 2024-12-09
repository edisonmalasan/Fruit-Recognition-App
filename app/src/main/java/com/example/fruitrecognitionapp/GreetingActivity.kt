package com.example.fruitrecognitionapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class GreetingActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_greeting)

        database = FirebaseDatabase.getInstance().getReference("users")
        val userInput: EditText = findViewById(R.id.user_name)
        val submitButton: Button = findViewById(R.id.get_started)

        submitButton.setOnClickListener {
            val userName = userInput.text.toString()
            if (userName.isNotEmpty()) {
                val user = User(userName)
                val userID = database.push().key
                if (userID != null) {
                    database.child(userID).setValue(user).addOnSuccessListener {
                        Toast.makeText(this, "User added successfully!", Toast.LENGTH_SHORT).show()
                        // After adding the user, navigate to MainActivity
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }.addOnFailureListener {
                        Toast.makeText(this, "Failed to add user!", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please enter a name!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
