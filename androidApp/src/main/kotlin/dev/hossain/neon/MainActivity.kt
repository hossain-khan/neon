package dev.hossain.neon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.hossain.neon.engine.highlightjs.HljsConfig
import dev.hossain.neon.engine.highlightjs.android

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(
                defaultHljsConfig = HljsConfig.android(applicationContext),
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
