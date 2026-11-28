package com.incleanhome.mobile.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.core.location.District
import com.incleanhome.mobile.ui.theme.InCleanHomeDimens

/** Closed district list of US32/US33: the API rejects free text that is not in its catalog. */
@Composable
fun DistrictSelector(
    districts: List<District>,
    selectedKey: String?,
    placeholder: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = districts.firstOrNull { it.key == selectedKey }
    Box(modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = InCleanHomeDimens.TouchTarget)
                .semantics { contentDescription = "$placeholder: ${selected?.name.orEmpty()}" }
        ) {
            Text(selected?.name ?: placeholder, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 360.dp)
        ) {
            districts.forEach { district ->
                DropdownMenuItem(
                    text = { Text(district.name) },
                    onClick = {
                        expanded = false
                        onSelect(district.key)
                    }
                )
            }
        }
    }
}
