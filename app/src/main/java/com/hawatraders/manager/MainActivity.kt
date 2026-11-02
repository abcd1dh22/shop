package com.hawatraders.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hawatraders.manager.ui.navigation.HawaTradersApp
import com.hawatraders.manager.ui.theme.HawaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as HawaApp).container
        setContent {
            HawaTheme {
                HawaTradersApp(container)
            }
        }
    }
}
