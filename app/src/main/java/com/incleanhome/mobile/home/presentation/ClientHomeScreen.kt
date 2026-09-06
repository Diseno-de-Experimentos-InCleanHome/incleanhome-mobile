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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
fun ClientHomeScreen(
    userName: String?,
    onSearchWorkers: () -> Unit,
    onBookings: () -> Unit,
    onMessages: () -> Unit,
    onEvents: () -> Unit,
    onProfile: () -> Unit,
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
            HomeGreeting(userName, stringResource(R.string.home_client_prompt))
            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                text = stringResource(R.string.home_search_staff),
                onClick = onSearchWorkers,
                leadingIcon = { Icons.Default.Search.let { androidx.compose.material3.Icon(it, null) } }
            )
            Spacer(Modifier.height(32.dp))
            HomeSectionTitle(stringResource(R.string.home_quick_actions))
            Spacer(Modifier.height(InCleanHomeDimens.ContentSpacing))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                HomeActionCard(stringResource(R.string.title_my_bookings), Icons.Default.DateRange, onBookings, Modifier.weight(1f))
                HomeActionCard(stringResource(R.string.nav_events), Icons.Default.Event, onEvents, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                HomeActionCard(stringResource(R.string.nav_messages), Icons.Default.Email, onMessages, Modifier.weight(1f))
                HomeActionCard(stringResource(R.string.title_my_profile), Icons.Default.Person, onProfile, Modifier.weight(1f))
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
