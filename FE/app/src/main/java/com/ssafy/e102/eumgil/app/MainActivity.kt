package com.ssafy.e102.eumgil.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ssafy.e102.eumgil.app.navigation.AppNavHost
import com.ssafy.e102.eumgil.core.designsystem.theme.BusanEumgilTheme

class MainActivity : ComponentActivity() {
    private var keepSplashOnScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BusanEumgilTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNavHost(
                        onAppReady = { isReady ->
                            keepSplashOnScreen = !isReady
                        },
                    )
                }
            }
        }
    }
}
