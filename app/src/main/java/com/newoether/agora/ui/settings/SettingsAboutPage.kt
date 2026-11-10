package com.newoether.agora.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.newoether.agora.R
import com.newoether.agora.ui.common.LocalAgoraHaptics
import com.newoether.agora.viewmodel.ChatViewModel

@Composable
fun SettingsAboutPage(viewModel: ChatViewModel, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val packageInfo = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0) } catch (_: Exception) { null }
    }
    val versionName = packageInfo?.versionName ?: "?"
    @Suppress("DEPRECATION")
    val versionCode = packageInfo?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode else it.versionCode.toLong()
    } ?: 0L
    val developerOptionsEnabled by viewModel.settings.developerOptionsEnabled.collectAsState()
    var developerTapCount by rememberSaveable { mutableIntStateOf(0) }
    val haptics = LocalAgoraHaptics.current
    val developerTapsRemainingFormat = stringResource(R.string.developer_options_taps_remaining)
    val developerEnabledMessage = stringResource(R.string.developer_options_enabled_message)
    val developerAlreadyEnabledMessage = stringResource(R.string.developer_options_already_enabled_message)
    var showLicenseDialog by remember { mutableStateOf(false) }

    if (showLicenseDialog) LicenseDialog(onDismiss = { showLicenseDialog = false })

    CollapsingSettingsScaffold(title = stringResource(R.string.about_title), onBack = onBack) {
        SettingsGroupColumn {
            SettingsGroup(title = stringResource(R.string.about_info), items = listOf({
                SettingsItem(
                    headlineContent = { Text(stringResource(R.string.about_developer)) },
                    supportingContent = { Text(stringResource(R.string.about_developer_name)) },
                    leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                )
            }, {
                SettingsItem(
                    headlineContent = { Text(stringResource(R.string.about_version)) },
                    supportingContent = { Text("v$versionName ($versionCode)") },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                    modifier = Modifier.clickable {
                        val result = DeveloperModeUnlockPolicy.advance(developerTapCount, developerOptionsEnabled)
                        developerTapCount = result.tapCount
                        when (result.feedback) {
                            DeveloperUnlockFeedback.NONE -> haptics.selection()
                            DeveloperUnlockFeedback.REMAINING_TAPS -> {
                                haptics.selection()
                                viewModel.emitSnackbar(String.format(developerTapsRemainingFormat, result.remainingTaps))
                            }
                            DeveloperUnlockFeedback.ENABLED -> {
                                haptics.confirm()
                                viewModel.settings.setDeveloperOptionsEnabled(true)
                                viewModel.emitSnackbar(developerEnabledMessage)
                            }
                            DeveloperUnlockFeedback.ALREADY_ENABLED -> {
                                haptics.selection()
                                viewModel.emitSnackbar(developerAlreadyEnabledMessage)
                            }
                        }
                    },
                )
            }))
            SettingsGroup(title = stringResource(R.string.about_links), items = listOf({
                SettingsItem(
                    headlineContent = { Text(stringResource(R.string.about_license)) },
                    supportingContent = { Text(stringResource(R.string.about_license_desc)) },
                    leadingContent = { Icon(Icons.Default.Policy, contentDescription = null) },
                    modifier = Modifier.clickable { showLicenseDialog = true },
                )
            }))
        }
    }
}
