package com.incleanhome.mobile.search.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.search.data.Worker
import com.incleanhome.mobile.ui.components.EmptyState
import com.incleanhome.mobile.ui.components.ErrorRetryState
import com.incleanhome.mobile.ui.components.InCleanHomeCard
import com.incleanhome.mobile.ui.components.InCleanHomeTextField
import com.incleanhome.mobile.ui.components.LoadingState
import com.incleanhome.mobile.ui.components.PrimaryButton
import com.incleanhome.mobile.ui.components.ScreenBackground
import com.incleanhome.mobile.ui.components.ScreenHeader
import com.incleanhome.mobile.ui.components.SingleServiceTypeSelector
import com.incleanhome.mobile.ui.format.formatCurrency
import com.incleanhome.mobile.ui.format.formatRating
import com.incleanhome.mobile.ui.format.humanizeIdentifier
import com.incleanhome.mobile.ui.format.presentationValues
import com.incleanhome.mobile.ui.theme.Navy
import com.incleanhome.mobile.ui.theme.PrimaryGreen

@Composable
fun WorkerSearchScreen(
    viewModel: WorkerSearchViewModel,
    onBack: () -> Unit,
    onWorkerClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    ScreenBackground(modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ScreenHeader(stringResource(R.string.search_marketplace_title), onBack)
                Text(
                    text = stringResource(R.string.search_marketplace_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                InCleanHomeCard {
                    Text(
                        text = stringResource(R.string.search_filters),
                        style = MaterialTheme.typography.titleMedium,
                        color = Navy
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.field_service_type), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    SingleServiceTypeSelector(
                        selectedValue = uiState.serviceType,
                        onSelectionChange = viewModel::onServiceTypeChange,
                        allServicesLabel = stringResource(R.string.all_services),
                        enabled = !uiState.isLoading
                    )
                    Spacer(Modifier.height(12.dp))
                    InCleanHomeTextField(
                        value = uiState.zone,
                        onValueChange = viewModel::onZoneChange,
                        label = stringResource(R.string.field_zone),
                        enabled = !uiState.isLoading,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            focusManager.clearFocus()
                            viewModel.search()
                        })
                    )
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton(
                        text = stringResource(R.string.action_search),
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.search()
                        },
                        enabled = !uiState.isLoading,
                        loading = uiState.isLoading
                    )
                }
            }
            item {
                Text(
                    text = stringResource(R.string.search_results),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Navy
                )
            }
            when {
                uiState.isLoading -> item { LoadingState(Modifier.padding(vertical = 32.dp)) }
                uiState.errorMessage != null -> item {
                    ErrorRetryState(uiState.errorMessage.orEmpty(), viewModel::search)
                }
                uiState.hasSearched && uiState.workers.isEmpty() -> item {
                    EmptyState(stringResource(R.string.empty_search_results), Modifier.padding(vertical = 32.dp))
                }
                else -> items(uiState.workers, key = Worker::id) { worker ->
                    WorkerResultCard(worker = worker, onClick = { onWorkerClick(worker.id) })
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun WorkerResultCard(
    worker: Worker,
    onClick: () -> Unit
) {
    InCleanHomeCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(worker.name, style = MaterialTheme.typography.titleLarge, color = Navy)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = PrimaryGreen)
                Text(
                    text = "${formatRating(worker.averageRating)} · " +
                        androidx.compose.ui.res.pluralStringResource(
                            R.plurals.services_completed_short,
                            worker.totalServices,
                            worker.totalServices
                        ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = limitedValues(worker.serviceTypes) { presentationValues(it, " · ") },
                style = MaterialTheme.typography.bodyMedium
            )
            if (worker.zones.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = PrimaryGreen)
                    Text(
                        text = limitedValues(worker.zones) { values ->
                            values.joinToString(" · ") { humanizeIdentifier(it) }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = stringResource(R.string.worker_rate_value, formatCurrency(worker.hourlyRate)),
                style = MaterialTheme.typography.titleSmall,
                color = Navy
            )
            Text(
                text = stringResource(R.string.search_view_profile),
                modifier = Modifier.align(Alignment.End),
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryGreen
            )
        }
    }
}

@Composable
private fun limitedValues(values: List<String>, formatter: @Composable (List<String>) -> String): String {
    val visible = values.take(3)
    val remaining = values.size - visible.size
    return formatter(visible) + if (remaining > 0) stringResource(R.string.search_more_items, remaining) else ""
}
