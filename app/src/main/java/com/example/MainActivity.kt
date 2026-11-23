package com.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import com.example.ui.AccendViewModel
import com.example.ui.nav.AccendNavHost
import com.example.ui.theme.AccendTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AccendViewModel by viewModels()
    private val openDailyDeepLink = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            AccendTheme {
                AccendNavHost(
                    viewModel = viewModel,
                    openDailyDeepLink = openDailyDeepLink.value,
                    onConsumedDeepLink = { openDailyDeepLink.value = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResumeApp()
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "accend" && data.host == "daily") {
            openDailyDeepLink.value = true
        }
    }
}
