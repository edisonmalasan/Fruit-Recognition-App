package com.example.strawberryripenessdetectionapp
import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class CameraActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val localImageButton: ImageButton = findViewById(R.id.local_imageIB)

        localImageButton.setOnClickListener {
            // Create an Intent to open the local image picker
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*" // Filter for images only
            startActivityForResult(intent, 100) // 100 is the request code
        }

        val shutterButton: ImageButton = findViewById(R.id.shot_cameraIB)

        shutterButton.setOnClickListener {
            // Shrink animation
            val shrinkAnimatorX = ObjectAnimator.ofFloat(shutterButton, "scaleX", 1f, 0.85f).apply {
                duration = 100 // Shrinking duration
            }
            val shrinkAnimatorY = ObjectAnimator.ofFloat(shutterButton, "scaleY", 1f, 0.85f).apply {
                duration = 100
            }

            // Expand animation
            val expandAnimatorX = ObjectAnimator.ofFloat(shutterButton, "scaleX", 0.85f, 1f).apply {
                duration = 100 // Expanding duration
            }
            val expandAnimatorY = ObjectAnimator.ofFloat(shutterButton, "scaleY", 0.85f, 1f).apply {
                duration = 100
            }

            // Start shrink animation
            shrinkAnimatorX.start()
            shrinkAnimatorY.start()

            // When shrink finishes, start expand animation
            shrinkAnimatorX.addListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(animation: Animator) {
                    expandAnimatorX.start()
                    expandAnimatorY.start()
                }

                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            // Get the URI of the selected image
            val selectedImageUri = data.data

            // Perform any action with the selected image, like displaying it in an ImageView
            if (selectedImageUri != null) {
                // Example: Displaying the image in an ImageView
                val imageView: ImageView = findViewById(R.id.local_imageIB) // Ensure this ImageView exists in your layout
                imageView.setImageURI(selectedImageUri)
            }
        }
    }
}
