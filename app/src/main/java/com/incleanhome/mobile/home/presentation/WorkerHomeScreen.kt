package com.incleanhome.mobile.home.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.ui.components.PrimaryButton
import com.incleanhome.mobile.ui.components.ScreenBackground
import com.incleanhome.mobile.ui.components.SecondaryButton
import com.incleanhome.mobile.ui.theme.InCleanHomeDimens

@Composable
fun WorkerHomeScreen(
    userName: String?,
    onProfile: () -> Unit,
    onAvailability: () -> Unit,
    onRequests: () -> Unit,
    onMessages: () -> Unit,
    onReviews: () -> Unit,
    onEvents: () -> Unit,
    onEventApplications: () -> Unit,
    onStats: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenBackground(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            HomeGreeting(userName, stringResource(R.string.home_worker_prompt))
            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                text = stringResource(R.string.home_view_requests),
                onClick = onRequests,
                leadingIcon = { androidx.compose.material3.Icon(Icons.Default.Assignment, null) }
            )
            Spacer(Modifier.height(32.dp))
            HomeSectionTitle(stringResource(R.string.home_quick_actions))
            Spacer(Modifier.height(InCleanHomeDimens.ContentSpacing))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                HomeActionCard(stringResource(R.string.title_my_availability), Icons.Default.Schedule, onAvailability, Modifier.weight(1f))
                HomeActionCard(stringResource(R.string.title_statistics), Icons.Default.BarChart, onStats, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                HomeActionCard(stringResource(R.string.nav_events), Icons.Default.Event, onEvents, Modifier.weight(1f))
                HomeActionCard(stringResource(R.string.nav_messages), Icons.Default.Email, onMessages, Modifier.weight(1f))
            }
            Spacer(Modifier.height(28.dp))
            HomeSectionTitle(stringResource(R.string.home_more_options))
            Spacer(Modifier.height(InCleanHomeDimens.ContentSpacing))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                HomeActionCard(stringResource(R.string.home_reviews), Icons.Default.Star, onReviews, Modifier.weight(1f))
                HomeActionCard(stringResource(R.string.home_applications), Icons.Default.HowToReg, onEventApplications, Modifier.weight(1f))
            }
            Spacer(Modifier.height(28.dp))
            SecondaryButton(
                text = stringResource(R.string.home_logout),
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
