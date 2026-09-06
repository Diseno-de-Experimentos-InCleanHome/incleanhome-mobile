package com.incleanhome.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onBack, modifier = Modifier.sizeIn(minHeight = 48.dp)) {
            Text(stringResource(R.string.action_back))
        }
        Text(title, Modifier.padding(start = 16.dp), style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.loading), Modifier.padding(start = 12.dp))
    }
}

@Composable
fun ErrorRetryState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.error_with_message, message), color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRetry, modifier = Modifier.sizeIn(minHeight = 48.dp)) {
            Text(stringResource(R.string.action_retry))
        }
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAction, modifier = Modifier.sizeIn(minHeight = 48.dp)) { Text(actionLabel) }
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
        enabled = enabled && !loading
    ) {
        if (loading) CircularProgressIndicator(Modifier.sizeIn(maxWidth = 24.dp, maxHeight = 24.dp), strokeWidth = 2.dp)
        else Text(text)
    }
}
