package com.hawatraders.manager

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hawatraders.manager.ui.navigation.HawaTradersApp
import com.hawatraders.manager.ui.theme.HawaTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** FragmentActivity (a ComponentActivity subclass) is required by BiometricPrompt. */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as HawaApp).container
        // One tiny DataStore read so the first frame already has the right text size (no resize flash).
        val initialLarge = runBlocking { container.settingsRepository.largeText.first() }
        setContent {
            val large by container.settingsRepository.largeText
                .collectAsStateWithLifecycle(initialValue = initialLarge)
            HawaTheme(largeText = large) {
                HawaTradersApp(container)
            }
        }
    }
}
