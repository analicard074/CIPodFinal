package com.example.cipod_final.navigationBar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    accessibilityEnabled: Boolean,
    onAccessibilityChange: (Boolean) -> Unit,
    onSignOut: suspend () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var isSigningOut by remember { mutableStateOf(false) }
    var signOutFailed by remember { mutableStateOf(false) }
    var showSignOutConfirmation by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() }
        )
        Accessibility(
            enabled = accessibilityEnabled,
            onEnabledChange = onAccessibilityChange,
            modifier = Modifier
                .padding(top = 24.dp)
        )
        Button(
            enabled = !isSigningOut,
            modifier = Modifier.padding(top = 24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.error,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            onClick = { showSignOutConfirmation = true },
        ) { Text(if (isSigningOut) "Signing out" else "Sign Out") }
        if (signOutFailed) Text(
            "Unable to sign out. Please try again.",
            color = MaterialTheme.colorScheme.error
        )
    }

    if (showSignOutConfirmation) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmation = false },
            title = { Text("Sign out?") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    onClick = {
                        showSignOutConfirmation = false
                        isSigningOut = true
                        signOutFailed = false
                        scope.launch {
                            try {
                                onSignOut()
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                signOutFailed = true
                            } finally {
                                isSigningOut = false
                            }
                        }
                    },
                ) { Text("Sign Out") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirmation = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
internal fun Accessibility(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = enabled,
                role = Role.Switch,
                onValueChange = onEnabledChange
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Accessibility", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (enabled) "On" else "Off",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Switch(checked = enabled, onCheckedChange = null)
    }
}
