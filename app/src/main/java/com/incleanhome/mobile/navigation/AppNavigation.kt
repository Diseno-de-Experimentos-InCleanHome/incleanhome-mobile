package com.incleanhome.mobile.navigation

import android.net.Uri
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.incleanhome.mobile.booking.presentation.BookingDetailViewModel
import com.incleanhome.mobile.booking.presentation.BookingListViewModel
import com.incleanhome.mobile.booking.presentation.CreateBookingScreen
import com.incleanhome.mobile.booking.presentation.CreateBookingViewModel
import com.incleanhome.mobile.booking.presentation.MyBookingsScreen
import com.incleanhome.mobile.booking.presentation.WorkerBookingDetailScreen
import com.incleanhome.mobile.booking.presentation.WorkerRequestsScreen
import com.incleanhome.mobile.core.session.SessionManager
import com.incleanhome.mobile.core.session.SessionState
import com.incleanhome.mobile.home.presentation.ClientHomeScreen
import com.incleanhome.mobile.home.presentation.WorkerHomeScreen
import com.incleanhome.mobile.events.presentation.*
import com.incleanhome.mobile.iam.data.LoginNextStep
import com.incleanhome.mobile.iam.presentation.LoginScreen
import com.incleanhome.mobile.iam.presentation.LoginViewModel
import com.incleanhome.mobile.iam.presentation.TwoFactorSetupScreen
import com.incleanhome.mobile.iam.presentation.TwoFactorVerifyScreen
import com.incleanhome.mobile.messaging.presentation.ChatScreen
import com.incleanhome.mobile.messaging.presentation.ChatViewModel
import com.incleanhome.mobile.messaging.presentation.ConversationsScreen
import com.incleanhome.mobile.messaging.presentation.ConversationsViewModel
import com.incleanhome.mobile.reviews.presentation.CreateReviewScreen
import com.incleanhome.mobile.reviews.presentation.CreateReviewViewModel
import com.incleanhome.mobile.reviews.presentation.WorkerReviewsScreen
import com.incleanhome.mobile.reviews.presentation.WorkerReviewsViewModel
import com.incleanhome.mobile.search.presentation.WorkerDetailScreen
import com.incleanhome.mobile.search.presentation.WorkerDetailViewModel
import com.incleanhome.mobile.search.presentation.WorkerSearchScreen
import com.incleanhome.mobile.search.presentation.WorkerSearchViewModel
import com.incleanhome.mobile.worker.presentation.WorkerAvailabilityScreen
import com.incleanhome.mobile.worker.presentation.WorkerAvailabilityViewModel
import com.incleanhome.mobile.worker.presentation.WorkerProfileScreen
import com.incleanhome.mobile.worker.presentation.WorkerProfileViewModel
import kotlinx.coroutines.launch

private object Routes {
    const val LOGIN = "login"
    const val TWO_FACTOR_SETUP = "two_factor_setup"
    const val TWO_FACTOR_VERIFY = "two_factor_verify"
    const val CLIENT_HOME = "client_home"
    const val WORKER_HOME = "worker_home"
    const val WORKER_SEARCH = "worker_search"
    const val WORKER_DETAIL = "worker_detail/{workerId}"
    const val WORKER_PROFILE = "worker_profile"
    const val WORKER_AVAILABILITY = "worker_availability"
    const val CREATE_BOOKING = "create_booking/{workerId}"
    const val CLIENT_BOOKINGS = "client_bookings"
    const val WORKER_REQUESTS = "worker_requests"
    const val WORKER_BOOKING_DETAIL = "worker_booking_detail/{bookingId}"
    const val CONVERSATIONS = "conversations"
    const val CHAT = "chat/{userId}?userName={userName}"
    const val CREATE_REVIEW = "create_review/{bookingId}"
    const val WORKER_REVIEWS = "worker_reviews"
    const val CLIENT_EVENTS = "client_events"
    const val CREATE_EVENT = "create_event"
    const val CLIENT_EVENT_DETAIL = "client_event_detail/{eventId}"
    const val EVENT_APPLICATIONS = "event_applications/{eventId}"
    const val WORKER_EVENTS = "worker_events"
    const val WORKER_EVENT_DETAIL = "worker_event_detail/{eventId}"
    const val WORKER_APPLICATIONS = "worker_applications"

    fun workerDetail(workerId: Int): String = "worker_detail/$workerId"
    fun createBooking(workerId: Int): String = "create_booking/$workerId"
    fun workerBookingDetail(bookingId: Int): String = "worker_booking_detail/$bookingId"
    fun chat(userId: Int, userName: String): String =
        "chat/$userId?userName=${Uri.encode(userName)}"
    fun createReview(bookingId: Int): String = "create_review/$bookingId"
    fun clientEventDetail(id:Int) = "client_event_detail/$id"
    fun eventApplications(id:Int) = "event_applications/$id"
    fun workerEventDetail(id:Int) = "worker_event_detail/$id"
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
            ClientHomeScreen(
                onSearchWorkers = { navController.navigate(Routes.WORKER_SEARCH) },
                onBookings = { navController.navigate(Routes.CLIENT_BOOKINGS) },
                onMessages = { navController.navigate(Routes.CONVERSATIONS) },
                onEvents = { navController.navigate(Routes.CLIENT_EVENTS) },
                onLogout = logout
            )
        }
        composable(Routes.WORKER_HOME) {
            WorkerHomeScreen(
                onProfile = { navController.navigate(Routes.WORKER_PROFILE) },
                onAvailability = { navController.navigate(Routes.WORKER_AVAILABILITY) },
                onRequests = { navController.navigate(Routes.WORKER_REQUESTS) },
                onMessages = { navController.navigate(Routes.CONVERSATIONS) },
                onReviews = { navController.navigate(Routes.WORKER_REVIEWS) },
                onEvents = { navController.navigate(Routes.WORKER_EVENTS) },
                onEventApplications = { navController.navigate(Routes.WORKER_APPLICATIONS) },
                onLogout = logout
            )
        }
        composable(Routes.WORKER_SEARCH) {
            val workerSearchViewModel: WorkerSearchViewModel = viewModel(
                factory = WorkerSearchViewModel.Factory
            )
            WorkerSearchScreen(
                viewModel = workerSearchViewModel,
                onBack = navController::popBackStack,
                onWorkerClick = { workerId ->
                    navController.navigate(Routes.workerDetail(workerId))
                }
            )
        }
        composable(
            route = Routes.WORKER_DETAIL,
            arguments = listOf(
                navArgument("workerId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val workerId = backStackEntry.arguments?.getInt("workerId") ?: return@composable
            val workerDetailViewModel: WorkerDetailViewModel = viewModel(
                factory = WorkerDetailViewModel.Factory(workerId)
            )
            WorkerDetailScreen(
                viewModel = workerDetailViewModel,
                onBack = navController::popBackStack,
                onBook = { id -> navController.navigate(Routes.createBooking(id)) },
                onContact = { id, name -> navController.navigate(Routes.chat(id, name)) }
            )
        }
        composable(
            route = Routes.CREATE_BOOKING,
            arguments = listOf(navArgument("workerId") { type = NavType.IntType })
        ) { backStackEntry ->
            val workerId = backStackEntry.arguments?.getInt("workerId") ?: return@composable
            val bookingViewModel: CreateBookingViewModel = viewModel(
                factory = CreateBookingViewModel.Factory(workerId)
            )
            CreateBookingScreen(
                viewModel = bookingViewModel,
                onBack = navController::popBackStack,
                onViewBookings = {
                    navController.navigate(Routes.CLIENT_BOOKINGS) {
                        popUpTo(Routes.WORKER_SEARCH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.CLIENT_BOOKINGS) { backStackEntry ->
            val bookingsViewModel: BookingListViewModel = viewModel(
                factory = BookingListViewModel.Factory
            )
            val reviewCreated by backStackEntry.savedStateHandle
                .getStateFlow("review_created", false)
                .collectAsState()
            LaunchedEffect(reviewCreated) {
                if (reviewCreated) {
                    bookingsViewModel.refresh()
                    backStackEntry.savedStateHandle["review_created"] = false
                }
            }
            MyBookingsScreen(
                viewModel = bookingsViewModel,
                onBack = navController::popBackStack,
                onReviewClick = { bookingId ->
                    navController.navigate(Routes.createReview(bookingId))
                }
            )
        }
        composable(
            route = Routes.CREATE_REVIEW,
            arguments = listOf(navArgument("bookingId") { type = NavType.IntType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getInt("bookingId") ?: return@composable
            val reviewViewModel: CreateReviewViewModel = viewModel(
                factory = CreateReviewViewModel.Factory(bookingId)
            )
            CreateReviewScreen(
                viewModel = reviewViewModel,
                onBack = navController::popBackStack,
                onDone = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("review_created", true)
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.WORKER_REVIEWS) {
            val workerId = (sessionState as? SessionState.Authenticated)
                ?.session
                ?.userId
                ?: return@composable
            val reviewsViewModel: WorkerReviewsViewModel = viewModel(
                factory = WorkerReviewsViewModel.Factory(workerId)
            )
            WorkerReviewsScreen(
                viewModel = reviewsViewModel,
                onBack = navController::popBackStack
            )
        }
        composable(Routes.CLIENT_EVENTS) { backStackEntry ->
            val eventsViewModel: EventsViewModel = viewModel(factory = EventsViewModel.Factory(true))
            val eventCreated by backStackEntry.savedStateHandle
                .getStateFlow("event_created", false).collectAsState()
            LaunchedEffect(eventCreated) {
                if (eventCreated) {
                    eventsViewModel.refresh()
                    backStackEntry.savedStateHandle["event_created"] = false
                }
            }
            EventsScreen(
                viewModel = eventsViewModel,
                clientView = true,
                onBack = navController::popBackStack,
                onCreate = { navController.navigate(Routes.CREATE_EVENT) },
                onEvent = { navController.navigate(Routes.clientEventDetail(it)) }
            )
        }
        composable(Routes.CREATE_EVENT) {
            val eventViewModel: CreateEventViewModel = viewModel(factory = CreateEventViewModel.Factory)
            CreateEventScreen(
                viewModel = eventViewModel,
                onBack = navController::popBackStack,
                onDone = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("event_created", true)
                    navController.popBackStack()
                }
            )
        }
        composable(
            Routes.CLIENT_EVENT_DETAIL,
            arguments = listOf(navArgument("eventId") { type = NavType.IntType })
        ) { entry ->
            val id = entry.arguments?.getInt("eventId") ?: return@composable
            val vm: EventDetailViewModel = viewModel(factory = EventDetailViewModel.Factory(id))
            EventDetailScreen(vm, false, navController::popBackStack, {
                navController.navigate(Routes.eventApplications(it))
            })
        }
        composable(
            Routes.EVENT_APPLICATIONS,
            arguments = listOf(navArgument("eventId") { type = NavType.IntType })
        ) { entry ->
            val id = entry.arguments?.getInt("eventId") ?: return@composable
            val vm: ApplicationsViewModel = viewModel(factory = ApplicationsViewModel.Factory(id))
            ApplicationsScreen(vm, navController::popBackStack)
        }
        composable(Routes.WORKER_EVENTS) {
            val vm: EventsViewModel = viewModel(factory = EventsViewModel.Factory(false))
            EventsScreen(
                viewModel = vm,
                clientView = false,
                onBack = navController::popBackStack,
                onCreate = {},
                onEvent = { navController.navigate(Routes.workerEventDetail(it)) }
            )
        }
        composable(
            Routes.WORKER_EVENT_DETAIL,
            arguments = listOf(navArgument("eventId") { type = NavType.IntType })
        ) { entry ->
            val id = entry.arguments?.getInt("eventId") ?: return@composable
            val vm: EventDetailViewModel = viewModel(factory = EventDetailViewModel.Factory(id))
            EventDetailScreen(vm, true, navController::popBackStack, {})
        }
        composable(Routes.WORKER_APPLICATIONS) {
            val vm: MyApplicationsViewModel = viewModel(factory = MyApplicationsViewModel.Factory)
            MyApplicationsScreen(
                viewModel = vm,
                onBack = navController::popBackStack,
                onEvent = { navController.navigate(Routes.workerEventDetail(it)) }
            )
        }
        composable(Routes.WORKER_REQUESTS) {
            val bookingsViewModel: BookingListViewModel = viewModel(
                factory = BookingListViewModel.Factory
            )
            WorkerRequestsScreen(
                viewModel = bookingsViewModel,
                onBack = navController::popBackStack,
                onBookingClick = { id -> navController.navigate(Routes.workerBookingDetail(id)) }
            )
        }
        composable(
            route = Routes.WORKER_BOOKING_DETAIL,
            arguments = listOf(navArgument("bookingId") { type = NavType.IntType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getInt("bookingId") ?: return@composable
            val bookingViewModel: BookingDetailViewModel = viewModel(
                factory = BookingDetailViewModel.Factory(bookingId)
            )
            WorkerBookingDetailScreen(
                viewModel = bookingViewModel,
                onBack = navController::popBackStack
            )
        }
        composable(Routes.CONVERSATIONS) {
            val conversationsViewModel: ConversationsViewModel = viewModel(
                factory = ConversationsViewModel.Factory
            )
            ConversationsScreen(
                viewModel = conversationsViewModel,
                onBack = navController::popBackStack,
                onConversationClick = { conversation ->
                    navController.navigate(Routes.chat(conversation.userId, conversation.userName))
                }
            )
        }
        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("userId") { type = NavType.IntType },
                navArgument("userName") {
                    type = NavType.StringType
                    defaultValue = "Conversación"
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getInt("userId") ?: return@composable
            val currentUserId = (sessionState as? SessionState.Authenticated)
                ?.session
                ?.userId
                ?: return@composable
            val userName = backStackEntry.arguments?.getString("userName") ?: "Conversación"
            val chatViewModel: ChatViewModel = viewModel(
                factory = ChatViewModel.Factory(userId)
            )
            ChatScreen(
                viewModel = chatViewModel,
                currentUserId = currentUserId,
                otherUserName = userName,
                onBack = navController::popBackStack
            )
        }
        composable(Routes.WORKER_PROFILE) {
            val workerProfileViewModel: WorkerProfileViewModel = viewModel(
                factory = WorkerProfileViewModel.Factory
            )
            WorkerProfileScreen(
                viewModel = workerProfileViewModel,
                onBack = navController::popBackStack
            )
        }
        composable(Routes.WORKER_AVAILABILITY) {
            val workerId = (sessionState as? SessionState.Authenticated)
                ?.session
                ?.userId
                ?: return@composable
            val workerAvailabilityViewModel: WorkerAvailabilityViewModel = viewModel(
                factory = WorkerAvailabilityViewModel.Factory(workerId)
            )
            WorkerAvailabilityScreen(
                viewModel = workerAvailabilityViewModel,
                onBack = navController::popBackStack
            )
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
