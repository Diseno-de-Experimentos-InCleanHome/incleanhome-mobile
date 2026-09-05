package com.incleanhome.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.incleanhome.mobile.core.network.RetrofitClient
import com.incleanhome.mobile.core.session.SessionManager
import com.incleanhome.mobile.navigation.AppNavigation
import com.incleanhome.mobile.ui.theme.InCleanHomeTheme

class MainActivity : ComponentActivity() {
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(applicationContext)
        RetrofitClient.setTokenProvider(sessionManager::currentToken)
        enableEdgeToEdge()
        setContent {
            InCleanHomeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(
                        sessionManager = sessionManager,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
