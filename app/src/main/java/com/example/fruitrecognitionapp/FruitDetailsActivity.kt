package com.example.fruitrecognitionapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.nio.FloatBuffer

class FruitDetailsActivity : AppCompatActivity() {

    private lateinit var fruitImageView: ImageView
    private lateinit var predictionResultTextView: TextView
    private lateinit var model: Module

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fruit_details_activity)

        // Initialize views
        fruitImageView = findViewById(R.id.fruitImageView)
        predictionResultTextView = findViewById(R.id.predictionResultTextView)

        // Load the pre-trained model from assets
        try {
            model = Module.load(assetFilePath("IdentiFruit.pt"))  // Ensure correct extension
        } catch (e: Exception) {
            Log.e("FruitDetailsActivity", "Model loading failed: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Model loading failed", Toast.LENGTH_SHORT).show()
            return
        }

        // Retrieve the image URI from the intent
        val selectedImageUri = intent.getStringExtra("selectedImageUri")

        if (selectedImageUri != null) {
            try {
                // Display the image using Picasso
                Picasso.get()
                    .load(selectedImageUri) // URI of the selected image
                    .placeholder(R.drawable.placeholder_image) // Placeholder image while loading
                    .error(R.drawable.error_image)             // Error image if loading fails
                    .into(fruitImageView)                      // The ImageView to load into

                // Predict the fruit based on the selected image
                predictFruit(Uri.parse(selectedImageUri))
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to process image URI", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show()
        }
    }

    private fun predictFruit(imageUri: Uri) {
        val image = loadImageFromUri(imageUri)

        if (image != null) {
            // Preprocess the image for the model
            val inputTensor = preprocessImage(image)

            // Perform inference
            val outputTensor = try {
                model.forward(IValue.from(inputTensor)).toTensor()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Inference failed", Toast.LENGTH_SHORT).show()
                return
            }

            // Get prediction result
            val result = outputTensor.dataAsFloatArray
            val predictedClass = getClassFromOutput(result)

            // Display the result
            predictionResultTextView.text = "Predicted Fruit: $predictedClass"
        } else {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadImageFromUri(imageUri: Uri): Bitmap? {
        return try {
            contentResolver.openInputStream(imageUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun preprocessImage(image: Bitmap): Tensor {
        val resizedImage = Bitmap.createScaledBitmap(image, 224, 224, true)

        val floatBuffer = FloatBuffer.allocate(224 * 224 * 3)
        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
        val std = floatArrayOf(0.229f, 0.224f, 0.225f)

        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = resizedImage.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                floatBuffer.put((r / 255.0f - mean[0]) / std[0])
                floatBuffer.put((g / 255.0f - mean[1]) / std[1])
                floatBuffer.put((b / 255.0f - mean[2]) / std[2])
            }
        }
        return Tensor.fromBlob(floatBuffer, longArrayOf(1, 3, 224, 224))  // Channels-first format
    }

    private fun getClassFromOutput(output: FloatArray): String {
        val maxIndex = output.indices.maxByOrNull { output[it] } ?: -1
        val classNames = listOf("Apple", "Banana", "Bell Pepper", "Chilli Pepper", "Corn", "Eggplant", "Grapes", "Jalapeno", "Kiwi",
            "Lemon", "Mango", "Onion", "Orange", "Paprika", "Pear", "Pineapple", "Pomegranate", "Sweetcorn", "Tomato", "Watermelon") // Adjust this based on your model's classes
        return if (maxIndex != -1) classNames[maxIndex] else "Unknown"
    }

    private fun assetFilePath(assetName: String): String {
        val file = File(cacheDir, assetName)

        // log the file path before attempting to copy
        Log.d("FruitDetailsActivity", "Model file path: ${file.absolutePath}")

        if (!file.exists()) {
            try {
                assets.open(assetName).use { inputStream ->
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Log.d("FruitDetailsActivity", "Model file copied to cache: ${file.absolutePath}")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("FruitDetailsActivity", "Failed to copy model file: ${e.message}")
                Toast.makeText(this, "Error loading model", Toast.LENGTH_SHORT).show()
            }
        }
        return file.absolutePath
    }
}
