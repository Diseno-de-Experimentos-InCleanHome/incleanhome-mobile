package com.incleanhome.mobile.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.incleanhome.mobile.core.session.SessionManager
import com.incleanhome.mobile.core.session.SessionState
import com.incleanhome.mobile.home.presentation.ClientHomeScreen
import com.incleanhome.mobile.home.presentation.WorkerHomeScreen
import com.incleanhome.mobile.iam.data.LoginNextStep
import com.incleanhome.mobile.iam.presentation.LoginScreen
import com.incleanhome.mobile.iam.presentation.LoginViewModel
import com.incleanhome.mobile.iam.presentation.TwoFactorSetupScreen
import com.incleanhome.mobile.iam.presentation.TwoFactorVerifyScreen
import kotlinx.coroutines.launch

private object Routes {
    const val LOGIN = "login"
    const val TWO_FACTOR_SETUP = "two_factor_setup"
    const val TWO_FACTOR_VERIFY = "two_factor_verify"
    const val CLIENT_HOME = "client_home"
    const val WORKER_HOME = "worker_home"
}

@Composable
fun AppNavigation(
    sessionManager: SessionManager,
    modifier: Modifier = Modifier
) {
    val sessionState by sessionManager.sessionState.collectAsState()

    LaunchedEffect(sessionManager) {
        sessionManager.restoreSession()
    }

    if (sessionState == SessionState.Loading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = viewModel(
        factory = LoginViewModel.Factory(sessionManager)
    )
    val loginUiState by loginViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val startDestination = remember {
        destinationForSession(sessionState)
    }

    LaunchedEffect(loginUiState.nextStep) {
        when (loginUiState.nextStep) {
            LoginNextStep.TWO_FA_SETUP -> navController.navigate(Routes.TWO_FACTOR_SETUP) {
                launchSingleTop = true
            }

            LoginNextStep.TWO_FA_VERIFY -> navController.navigate(Routes.TWO_FACTOR_VERIFY) {
                launchSingleTop = true
            }

            LoginNextStep.TERMS, null -> Unit
        }
    }

    LaunchedEffect(sessionState) {
        val destination = destinationForSession(sessionState)
        if (navController.currentDestination?.route != destination) {
            navController.navigateAndClearBackStack(destination)
        }
    }

    val logout: () -> Unit = {
        loginViewModel.clearAuthenticationState()
        coroutineScope.launch {
            sessionManager.logout()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(loginViewModel = loginViewModel)
        }
        composable(Routes.TWO_FACTOR_SETUP) {
            TwoFactorSetupScreen(loginViewModel = loginViewModel)
        }
        composable(Routes.TWO_FACTOR_VERIFY) {
            TwoFactorVerifyScreen(loginViewModel = loginViewModel)
        }
        composable(Routes.CLIENT_HOME) {
            ClientHomeScreen(onLogout = logout)
        }
        composable(Routes.WORKER_HOME) {
            WorkerHomeScreen(onLogout = logout)
        }
    }
}

private fun destinationForSession(sessionState: SessionState): String {
    return when (sessionState) {
        is SessionState.Authenticated -> when {
            sessionState.session.role.equals(SessionManager.CLIENT_ROLE, ignoreCase = true) -> {
                Routes.CLIENT_HOME
            }

            sessionState.session.role.equals(SessionManager.WORKER_ROLE, ignoreCase = true) -> {
                Routes.WORKER_HOME
            }

            else -> Routes.LOGIN
        }

        SessionState.Loading, SessionState.LoggedOut -> Routes.LOGIN
    }
}

private fun NavHostController.navigateAndClearBackStack(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) {
            inclusive = true
        }
        launchSingleTop = true
    }
}
