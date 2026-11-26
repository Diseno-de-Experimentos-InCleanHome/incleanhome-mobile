package com.incleanhome.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.incleanhome.mobile.core.network.RetrofitClient
import com.incleanhome.mobile.core.session.SessionManager
import com.incleanhome.mobile.experiments.Experimentation
import com.incleanhome.mobile.experiments.tracking.TrackingUser
import com.incleanhome.mobile.navigation.AppNavigation
import com.incleanhome.mobile.ui.theme.InCleanHomeTheme

class MainActivity : ComponentActivity() {
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(applicationContext)
        RetrofitClient.setTokenProvider(sessionManager::currentToken)
        Experimentation.initialize(applicationContext) {
            sessionManager.currentSession()?.let { TrackingUser(it.userId, it.role) }
        }
        enableEdgeToEdge()
        setContent {
            InCleanHomeTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    AppNavigation(
                        sessionManager = sessionManager,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // US43/US42: pick up remote experiment settings and upload the event outbox when enabled.
        lifecycleScope.launch { Experimentation.synchronize() }
    }
}
