package com.incleanhome.mobile.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.ui.components.InCleanHomeCard
import com.incleanhome.mobile.ui.theme.InCleanHomeDimens
import com.incleanhome.mobile.ui.theme.Navy
import com.incleanhome.mobile.ui.theme.PrimaryGreen
import com.incleanhome.mobile.ui.theme.TextSecondary

@Composable
internal fun HomeGreeting(userName: String?, prompt: String) {
    val displayName = userName?.trim().orEmpty()
    Text(
        text = stringResource(R.string.home_brand),
        color = PrimaryGreen,
        style = MaterialTheme.typography.titleLarge
    )
    Spacer(Modifier.height(24.dp))
    Text(
        text = if (displayName.isBlank()) {
            stringResource(R.string.home_hello)
        } else {
            stringResource(R.string.home_hello_named, displayName)
        },
        color = Navy,
        style = MaterialTheme.typography.headlineMedium
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = prompt,
        color = TextSecondary,
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
internal fun HomeSectionTitle(text: String) {
    Text(text = text, color = Navy, style = MaterialTheme.typography.titleLarge)
}

@Composable
internal fun HomeActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    InCleanHomeCard(modifier = modifier.heightIn(min = 116.dp), onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(InCleanHomeDimens.ContentSpacing)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = PrimaryGreen
            )
            Text(text = title, color = Navy, style = MaterialTheme.typography.titleMedium)
        }
    }
}
