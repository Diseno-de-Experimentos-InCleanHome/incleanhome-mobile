package com.incleanhome.mobile.iam.presentation

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.incleanhome.mobile.R
import com.incleanhome.mobile.iam.data.MEMBERSHIP_PENDING
import com.incleanhome.mobile.ui.components.InCleanHomeCard
import com.incleanhome.mobile.ui.components.PrimaryButton
import com.incleanhome.mobile.ui.components.ScreenBackground
import com.incleanhome.mobile.ui.components.SecondaryButton
import com.incleanhome.mobile.ui.theme.InCleanHomeDimens
import com.incleanhome.mobile.ui.theme.Navy

@Composable
fun MembershipStatusScreen(
    status: String,
    backendMessage: String?,
    whatsappLink: String?,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPending = status.equals(MEMBERSHIP_PENDING, ignoreCase = true)
    var linkError by remember(whatsappLink) { mutableStateOf(false) }

    BackHandler(onBack = onBackToLogin)
    ScreenBackground(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(InCleanHomeDimens.ScreenPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InCleanHomeCard {
                Text(
                    text = stringResource(
                        if (isPending) R.string.auth_membership_pending_title
                        else R.string.auth_membership_rejected_title
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Navy
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(
                        if (isPending) R.string.auth_membership_pending_body
                        else R.string.auth_membership_rejected_body
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
                if (linkError) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.auth_membership_whatsapp_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.height(24.dp))
                PrimaryButton(
                    text = stringResource(
                        if (isPending) R.string.auth_membership_send_receipt
                        else R.string.auth_membership_contact_whatsapp
                    ),
                    onClick = {
                        linkError = !openExternalLinkSafely(
                            link = whatsappLink,
                            startActivity = context::startActivity
                        )
                    }
                )
                Spacer(Modifier.height(12.dp))
                SecondaryButton(
                    text = stringResource(R.string.auth_back_to_sign_in),
                    onClick = onBackToLogin,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun AdministrativeAccessScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenBackground(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(InCleanHomeDimens.ScreenPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InCleanHomeCard {
                Text(
                    stringResource(R.string.auth_admin_access_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Navy
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.auth_admin_access_body),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(24.dp))
                PrimaryButton(
                    text = stringResource(R.string.auth_sign_out),
                    onClick = onSignOut
                )
            }
        }
    }
}

internal fun openExternalLinkSafely(
    link: String?,
    startActivity: (Intent) -> Unit
): Boolean {
    if (link.isNullOrBlank() || link != link.trim()) return false
    val uri = runCatching { Uri.parse(link) }.getOrNull() ?: return false
    if (uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank()) {
        return false
    }

    return runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
    }.isSuccess
}
