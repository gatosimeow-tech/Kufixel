package com.example.kufixel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            // Handle selected image URI here (e.g., import into editor)
            android.util.Log.d("PhotoPicker", "Selected URI: $uri")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize the Android Image Saver with activity context
        (getImageSaver() as? AndroidImageSaver)?.context = this

        setContent {
            App(onOpenStorage = {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear context to avoid memory leaks
        (getImageSaver() as? AndroidImageSaver)?.context = null
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}