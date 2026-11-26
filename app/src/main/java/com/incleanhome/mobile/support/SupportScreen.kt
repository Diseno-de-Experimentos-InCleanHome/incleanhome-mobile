package com.incleanhome.mobile.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.ui.components.InCleanHomeCard
import com.incleanhome.mobile.ui.components.PrimaryButton
import com.incleanhome.mobile.ui.components.ScreenBackground
import com.incleanhome.mobile.ui.components.ScreenHeader
import com.incleanhome.mobile.ui.components.SecondaryButton
import com.incleanhome.mobile.ui.theme.InCleanHomeDimens
import com.incleanhome.mobile.ui.theme.Navy
import com.incleanhome.mobile.ui.theme.PrimaryGreen

/** Frequent problems before registering a claim (US38). */
@Composable
fun SupportScreen(
    viewModel: SupportViewModel,
    onRegisterClaim: (categoryLabel: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    ScreenBackground(modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(InCleanHomeDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenHeader(stringResource(R.string.support_title), onBack)
            Text(stringResource(R.string.support_intro), style = MaterialTheme.typography.bodyLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FaqCategory.entries.forEach { category ->
                    FilterChip(
                        selected = state.category == category,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(stringResource(category.label)) }
                    )
                }
            }
            val categoryLabel = stringResource(state.category.label)
            FaqCatalog.byCategory(state.category).forEach { item ->
                InCleanHomeCard(onClick = { viewModel.toggle(item) }) {
                    Text(stringResource(item.question), style = MaterialTheme.typography.titleMedium, color = Navy)
                    if (state.expandedFaqId == item.id) {
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(item.answer), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        if (item.id in state.resolvedFaqIds) {
                            Text(stringResource(R.string.support_resolved_thanks), color = PrimaryGreen)
                        } else {
                            Text(stringResource(R.string.support_question_solved), style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(8.dp))
                            PrimaryButton(stringResource(R.string.support_yes_solved), { viewModel.markResolved(item) })
                            Spacer(Modifier.height(8.dp))
                            SecondaryButton(
                                stringResource(R.string.support_not_solved),
                                { onRegisterClaim(categoryLabel) },
                                Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            SecondaryButton(
                stringResource(R.string.support_direct_claim),
                { onRegisterClaim("") },
                Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
