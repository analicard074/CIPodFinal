package com.example.cipod_final

import android.os.Bundle
import com.example.cipod_final.indoor.IndoorEditorPolicy
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(indoorEditorEnabled = IndoorEditorPolicy.allowed(BuildConfig.DEBUG, BuildConfig.INDOOR_EDITOR_REQUESTED))
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}