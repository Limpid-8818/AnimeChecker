package com.animechecker.local

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.animechecker.local.ui.AppRoot
import com.animechecker.local.ui.theme.AnimeCheckerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 容器由 AnimeCheckerApp 创建并持有.
        val container = (application as AnimeCheckerApp).container
        setContent {
            AnimeCheckerTheme {
                AppRoot(container)
            }
        }
    }
}
