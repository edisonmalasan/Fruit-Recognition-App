package com.example.fruitrecognitionapp

import NutritionAdapter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.io.InputStream

class FruitDetailsActivity : AppCompatActivity() {

    private lateinit var fruitImageView: ImageView
    private lateinit var predictionResultTextView: TextView
    private lateinit var model: Module
    private lateinit var nutritionRecyclerView: RecyclerView
    private lateinit var healthRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fruit_details_activity)

        // Initialize views
        fruitImageView = findViewById(R.id.fruitImageView)
        predictionResultTextView = findViewById(R.id.predictionResultTextView)

        nutritionRecyclerView = findViewById(R.id.nutritionRecyclerView)
        healthRecyclerView = findViewById(R.id.healthRecyclerView)
        nutritionRecyclerView.layoutManager = LinearLayoutManager(this)
        healthRecyclerView.layoutManager = LinearLayoutManager(this)

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
        Log.d("FruitDetailsActivity", "Image URI: $selectedImageUri")

        if (selectedImageUri != null) {
            try {
                // Display the image using Picasso
                Picasso.get()
                    .load(selectedImageUri)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(fruitImageView)

                // Predict the fruit based on the selected image
                predictFruit(Uri.parse(selectedImageUri))
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to process image URI", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show()
        }

        fetchFruitData("banana")
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

            // Fetch fruit data from Firebase
            fetchFruitData(predictedClass)
        } else {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadImageFromUri(imageUri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
            if (inputStream != null) {
                BitmapFactory.decodeStream(inputStream)
            } else {
                Toast.makeText(this, "Error loading image: InputStream is null", Toast.LENGTH_SHORT).show()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun preprocessImage(image: Bitmap): Tensor {
        // Resize the image to 224x224 (as required by most models)
        val resizedImage = Bitmap.createScaledBitmap(image, 224, 224, true)

        // Convert the resized image to a float array (normalized RGB values)
        val floatArray = FloatArray(224 * 224 * 3)

        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
        val std = floatArrayOf(0.229f, 0.224f, 0.225f)

        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = resizedImage.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                // Normalize the pixel values to match the model's expected range
                floatArray[(y * 224 + x) * 3 + 0] = (r / 255.0f - mean[0]) / std[0]
                floatArray[(y * 224 + x) * 3 + 1] = (g / 255.0f - mean[1]) / std[1]
                floatArray[(y * 224 + x) * 3 + 2] = (b / 255.0f - mean[2]) / std[2]
            }
        }

        // Create and return the Tensor
        return Tensor.fromBlob(floatArray, longArrayOf(1, 3, 224, 224))
    }

    private fun getClassFromOutput(output: FloatArray): String {
        val maxIndex = output.indices.maxByOrNull { output[it] } ?: -1
        val classNames = listOf("Apple", "Banana", "Bell Pepper", "Chilli Pepper", "Corn", "Eggplant", "Grapes", "Jalapeno", "Kiwi",
            "Lemon", "Mango", "Onion", "Orange", "Paprika", "Pear", "Pineapple", "Pomegranate", "Sweetcorn", "Tomato", "Watermelon")
        return if (maxIndex != -1) classNames[maxIndex] else "Unknown"
    }

    private fun assetFilePath(assetName: String): String {
        val file = File(cacheDir, assetName)

        if (!file.exists()) {
            try {
                assets.open(assetName).use { inputStream ->
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error loading model", Toast.LENGTH_SHORT).show()
            }
        }
        return file.absolutePath
    }

    private fun fetchFruitData(name: String) {
        val database = FirebaseDatabase.getInstance().getReference("fruits").child(name)

        database.get().addOnSuccessListener { data ->
            if (data.exists()) {
                Log.d("FruitDetailsActivity", "Data exists for $name")
                val nutritionalValues = mutableListOf<ListItem>()
                val healthBenefits = mutableListOf<ListItem>()

                val nutritionalData = data.child("nutritional_content")
                nutritionalData.children.forEach {
                    nutritionalValues.add(ListItem(it.value.toString()))
                }

                val healthBenefitsData = data.child("health_benefits")
                healthBenefitsData.children.forEach {
                    healthBenefits.add(ListItem(it.value.toString()))
                }

                updateUI(nutritionalValues, healthBenefits)
            } else {
                Log.d("FruitDetailsActivity", "No data found for $name")
                Toast.makeText(this, "No data found for $name", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Log.e("FruitDetailsActivity", "Error fetching data: ${e.message}")
            Toast.makeText(this, "Error fetching fruit data", Toast.LENGTH_SHORT).show()
        }
    }


    private fun updateUI(nutritionalValues: List<ListItem>, healthBenefits: List<ListItem>) {
        Log.d("FruitDetailsActivity", "Updating UI with nutritional and health data.")

        val nutritionAdapter = NutritionAdapter(nutritionalValues)
        nutritionRecyclerView.adapter = nutritionAdapter

        val healthAdapter = NutritionAdapter(healthBenefits)
        healthRecyclerView.adapter = healthAdapter
    }

}
