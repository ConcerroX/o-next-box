package com.mi.onextbox.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mi.onextbox.MainActivity
import com.mi.onextbox.R
import com.mi.onextbox.ui.common.CouiConfirmDialog
import com.mi.onextbox.ui.onboarding.OnboardingPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun DeveloperActivationEntry() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirm by rememberSaveable { mutableStateOf(false) }
    var opening by remember { mutableStateOf(false) }
    SettingsSection(title = stringResource(R.string.developer_activation_section))
    SettingsGroup {
        SettingsCardRow(
            title = stringResource(R.string.developer_activation_title),
            summary = stringResource(R.string.developer_activation_summary),
            showArrow = true,
            onClick = { if (!opening) confirm = true },
        )
    }
    CouiConfirmDialog(
        show = confirm,
        title = stringResource(R.string.developer_activation_title),
        summary = stringResource(R.string.developer_activation_confirm),
        negativeText = stringResource(android.R.string.cancel),
        positiveText = stringResource(android.R.string.ok),
        onDismissRequest = { confirm = false },
        onPositive = {
            if (!opening) {
                confirm = false
                opening = true
                scope.launch {
                    try {
                        check(withContext(Dispatchers.IO) { OnboardingPreferences.reset(context) })
                        UpdateNotificationScheduler.cancel(context)
                        // A fresh task drops rememberSaveable(completed=true). recreate()
                        // would otherwise restore the old completed gate over the reset store.
                        context.startActivity(Intent(context, MainActivity::class.java).addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (_: Exception) {
                        Toast.makeText(context, R.string.developer_activation_failed, Toast.LENGTH_LONG).show()
                    } finally { opening = false }
                }
            }
        },
    )
}
