package com.example.fruitrecognitionapp

import NutritionAdapter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
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
import org.w3c.dom.Text
import java.io.File
import java.io.InputStream

class FruitDetailsActivity : AppCompatActivity() {

    private lateinit var fruitImageView: ImageView
    private lateinit var predictionResultTextView: TextView
    private lateinit var model: Module
    private lateinit var nutritionRecyclerView: RecyclerView
    private lateinit var healthRecyclerView: RecyclerView
    private lateinit var predictedClass: String

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

        // load the model
        try {
            val modelFilePath = assetFilePath("IdentiFruit_Model_cpu_V2.pt")
            Log.d("FruitDetailsActivity", "Model file path: $modelFilePath, Exists: ${File(modelFilePath).exists()}")

            model = Module.load(modelFilePath)  // Ensure correct extension
            Log.d("FruitDetailsActivity", "Model loaded successfully")
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

        val fruitName = findViewById<TextView>(R.id.fruit)
        fruitName.text = predictedClass
        fetchFruitData(fruitName.text.toString())
        getReference(predictedClass)
    }

    private fun predictFruit(imageUri: Uri) {
        val image = loadImageFromUri(imageUri)

        if (image != null) {
            // preProcess the Uri  image
            val inputTensor = preprocessImage(image)

            // perform inference to the model
            val outputTensor = try {
                model.forward(IValue.from(inputTensor)).toTensor()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Inference failed", Toast.LENGTH_SHORT).show()
                return
            }

            // Get prediction result
            val result = outputTensor.dataAsFloatArray
            predictedClass = getClassFromOutput(result)

            // display the result
            predictionResultTextView.text = "Predicted Fruit: $predictedClass"

            // fetch fruit data from firebase
            fetchFruitData(predictedClass.lowercase())
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

    // WAG NYO TANGGALIN TO PARA MABALIKAN ANG MISTAKES
//    private fun preprocessImage(image: Bitmap): Tensor {
//        // Resize to 256x256 first
//        val resizedImage = Bitmap.createScaledBitmap(image, 256, 256, true)
//
//        // Center crop to 224x224
//        val xOffset = (resizedImage.width - 224) / 2
//        val yOffset = (resizedImage.height - 224) / 2
//        val croppedImage = Bitmap.createBitmap(resizedImage, xOffset, yOffset, 224, 224)
//
//        // Normalize and convert to CHW format
//        val floatArray = FloatArray(224 * 224 * 3)
//        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
//        val std = floatArrayOf(0.229f, 0.224f, 0.225f)
//
//        for (y in 0 until 224) {
//            for (x in 0 until 224) {
//                val pixel = croppedImage.getPixel(x, y)
//                val r = (pixel shr 16) and 0xFF
//                val g = (pixel shr 8) and 0xFF
//                val b = pixel and 0xFF
//
//                // Normalize pixel values
//                floatArray[(y * 224 + x) * 3 + 0] = (r / 255.0f - mean[0]) / std[0]
//                floatArray[(y * 224 + x) * 3 + 1] = (g / 255.0f - mean[1]) / std[1]
//                floatArray[(y * 224 + x) * 3 + 2] = (b / 255.0f - mean[2]) / std[2]
//            }
//        }
//
//        return Tensor.fromBlob(floatArray, longArrayOf(1, 3, 224, 224))
//    }

    private fun preprocessImage(image: Bitmap): Tensor {
        // resize to 256x256 first as i did when training the model
        val resizedImage = Bitmap.createScaledBitmap(image, 256, 256, true)

        // then crop to 224x224 as its the needed/recommended size for Tensor
        val xOffset = (resizedImage.width - 224) / 2
        val yOffset = (resizedImage.height - 224) / 2
        val croppedImage = Bitmap.createBitmap(resizedImage, xOffset, yOffset, 224, 224)

        // convert to hwc format
        val floatArray = FloatArray(224 * 224 * 3)
        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
        val std = floatArrayOf(0.229f, 0.224f, 0.225f)

        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = croppedImage.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                // pixel values
                floatArray[(y * 224 + x) * 3 + 0] = (r / 255.0f - mean[0]) / std[0]
                floatArray[(y * 224 + x) * 3 + 1] = (g / 255.0f - mean[1]) / std[1]
                floatArray[(y * 224 + x) * 3 + 2] = (b / 255.0f - mean[2]) / std[2]
            }
        }

        // convert the img to chw format
        val chwArray = FloatArray(224 * 224 * 3)
        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val r = floatArray[(y * 224 + x) * 3 + 0]
                val g = floatArray[(y * 224 + x) * 3 + 1]
                val b = floatArray[(y * 224 + x) * 3 + 2]

                // rerange to chw format
                chwArray[x + (y * 224)] = r
                chwArray[x + (y * 224) + 224 * 224] = g
                chwArray[x + (y * 224) + 2 * 224 * 224] = b
            }
        }

        // return tensor to chw format
        return Tensor.fromBlob(chwArray, longArrayOf(1, 3, 224, 224))
    }

    private fun getClassFromOutput(output: FloatArray): String {
        val maxIndex = output.indices.maxByOrNull { output[it] } ?: -1
        if (maxIndex == -1) return "Unknown"

        val confidence = output[maxIndex] * 100
        Log.d("FruitDetailsActivity", "Confidence: $confidence%")

        if (confidence < 200) { // 200 or 225
            return "Unknown"
        }

        // list of dataset that was used in training the model
        val classNames = listOf(
            "Apple", "Banana", "Bell Pepper", "Chilli Pepper", "Corn", "Eggplant",
            "Grapes", "Jalapeno", "Kiwi", "Lemon", "Mango", "Onion", "Orange",
            "Paprika", "Pear", "Pineapple", "Pomegranate", "Sweetcorn", "Tomato", "Watermelon"
        )
        return classNames[maxIndex]
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

    /**
     * get necessary reference of the predicted fruit
     */
    private fun getReference(predicted_class: String) {
        val reference = findViewById<TextView>(R.id.fruit_reference)
        when (predicted_class.lowercase()) {
            "apple" -> reference.text = "https://www.healthline.com/nutrition/foods/apples"
            "banana" -> reference.text = "https://www.healthline.com/nutrition/foods/bananas"
            "bell pepper" -> reference.text = "https://www.healthline.com/nutrition/foods/bell-peppers"
            "chilli pepper" -> reference.text = "https://www.healthline.com/nutrition/foods/chili-peppers"
            "corn" -> reference.text = "https://www.healthline.com/nutrition/foods/corn"
            "eggplant" -> reference.text = "https://www.healthline.com/nutrition/foods/eggplant-benefits"
            "grapes" -> reference.text = "https://www.healthline.com/nutrition/foods/benefits-of-grapes"
            "jalapeno" -> reference.text = "https://www.healthline.com/nutrition/foods/jalapeno-health-benefits"
            "kiwi" -> reference.text = "https://www.healthline.com/nutrition/foods/kiwi-benefits"
            "lemon" -> reference.text = "https://www.healthline.com/nutrition/foods/lemons"
            "mango" -> reference.text = "https://www.healthline.com/nutrition/foods/mango"
            "onion" -> reference.text = "https://www.healthline.com/nutrition/foods/onions"
            "orange" -> reference.text = "https://www.healthline.com/nutrition/foods/oranges"
            "paprika" -> reference.text = "https://www.healthline.com/nutrition/foods/paprika-benefits"
            "pear" -> reference.text = "https://www.healthline.com/nutrition/foods/benefits-of-pears"
            "pineapple" -> reference.text = "https://www.healthline.com/nutrition/foods/benefits-of-pineapple"
            "pomegranate" -> reference.text = "https://www.healthline.com/nutrition/foods/12-proven-benefits-of-pomegranate"
            "sweetcorn" -> reference.text = "https://www.healthline.com/nutrition/foods/corn"
            "tomato" -> reference.text = "https://www.healthline.com/nutrition/foods/tomatoes"
            "watermelon" -> reference.text = "https://www.healthline.com/nutrition/foods/watermelon"
            else -> reference.text = "No available reference link for this image"
        }
        reference.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun updateUI(nutritionalValues: List<ListItem>, healthBenefits: List<ListItem>) {
        Log.d("FruitDetailsActivity", "Updating UI with nutritional and health data.")

        val nutritionAdapter = NutritionAdapter(nutritionalValues)
        nutritionRecyclerView.adapter = nutritionAdapter

        val healthAdapter = NutritionAdapter(healthBenefits)
        healthRecyclerView.adapter = healthAdapter
    }

}
