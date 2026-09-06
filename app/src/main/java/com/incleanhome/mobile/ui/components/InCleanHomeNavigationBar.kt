package com.incleanhome.mobile.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.incleanhome.mobile.ui.theme.Border
import com.incleanhome.mobile.ui.theme.GreenLight
import com.incleanhome.mobile.ui.theme.PrimaryGreen
import com.incleanhome.mobile.ui.theme.Surface
import com.incleanhome.mobile.ui.theme.TextSecondary

data class BottomNavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun InCleanHomeNavigationBar(
    items: List<BottomNavigationItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Column {
        HorizontalDivider(color = Border)
        NavigationBar(
            containerColor = Surface,
            tonalElevation = androidx.compose.ui.unit.Dp.Unspecified
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(item.route) },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label
                        )
                    },
                    label = {
                        Text(item.label, style = MaterialTheme.typography.labelSmall)
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryGreen,
                        selectedTextColor = PrimaryGreen,
                        indicatorColor = GreenLight,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
            }
        }
    }
}
