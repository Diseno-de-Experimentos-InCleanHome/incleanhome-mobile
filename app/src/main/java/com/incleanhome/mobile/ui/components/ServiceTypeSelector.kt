package com.incleanhome.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.ui.format.CanonicalServiceTypes
import com.incleanhome.mobile.ui.format.presentationValue

@Composable
fun ServiceTypeSelector(
    selectedValues: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CanonicalServiceTypes.forEach { value ->
            FilterChip(
                selected = value in selectedValues,
                onClick = {
                    onSelectionChange(
                        if (value in selectedValues) {
                            selectedValues.filterNot { it == value }
                        } else {
                            selectedValues + value
                        }
                    )
                },
                label = { Text(presentationValue(value)) },
                enabled = enabled
            )
        }
    }
}

@Composable
fun SingleServiceTypeSelector(
    selectedValue: String,
    onSelectionChange: (String) -> Unit,
    allServicesLabel: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterChip(
            selected = selectedValue.isBlank(),
            onClick = { onSelectionChange("") },
            label = { Text(allServicesLabel) },
            enabled = enabled
        )
        CanonicalServiceTypes.forEach { value ->
            FilterChip(
                selected = selectedValue == value,
                onClick = { onSelectionChange(value) },
                label = { Text(presentationValue(value)) },
                enabled = enabled
            )
        }
    }
}
