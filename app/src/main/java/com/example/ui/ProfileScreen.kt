package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.User
import com.example.viewmodel.SageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: SageViewModel
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val isCouple by viewModel.isCoupleMode.collectAsState()
    val users by viewModel.users.collectAsState()
    val settingsState by viewModel.settings.collectAsState()

    var showEditUser1Dialog by remember { mutableStateOf(false) }
    var showEditUser2Dialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Backup states
    var backupJson by remember { mutableStateOf("") }
    var backupStatusText by remember { mutableStateOf("") }

    // Reset verification
    var resetInputText by remember { mutableStateOf("") }

    // Form inputs User 1
    var u1Name by remember { mutableStateOf("") }
    var u1Salary by remember { mutableStateOf("") }
    var u1RentContrib by remember { mutableStateOf("") }

    // Form inputs User 2
    var u2Name by remember { mutableStateOf("") }
    var u2RentContrib by remember { mutableStateOf("") }

    val user1 = users.find { it.id == 1 }
    val user2 = users.find { it.id == 2 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_settings_view"),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "SAGE",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = SageLocalizer.t("app_tagline", lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        // --- SYSTEM MODE (PERSONAL VS COUPLE) ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "System Operations Mode".uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                text = if (isCouple) SageLocalizer.t("couple_mode", lang) else SageLocalizer.t("personal_mode", lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isCouple) "Simultaneous Ledger for Couple Mode." else "Personal Mode hides secondary companion fields.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Switch(
                            checked = isCouple,
                            onCheckedChange = { viewModel.setCoupleModeEnabled(it) },
                            modifier = Modifier.testTag("mode_switch")
                        )
                    }
                }
            }
        }

        // --- COMPANIONS PROFILES ---
        item {
            Text(
                text = "Profiles".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Rama profile card
        if (user1 != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable {
                            u1Name = user1.name
                            u1Salary = user1.incomeAmount.toInt().toString()
                            u1RentContrib = user1.rentContribution.toInt().toString()
                            showEditUser1Dialog = true
                        }
                        .testTag("profile_card_rama"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "User 1 (Primary)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(text = user1.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Contribution to rent: Rp ${user1.rentContribution.toInt()}", style = MaterialTheme.typography.bodySmall)
                        }

                        Icon(Icons.Default.Edit, contentDescription = "Edit User", tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        // Nadiya (active in Couple mode)
        if (isCouple) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable {
                            if (user2 != null) {
                                u2Name = user2.name
                                u2RentContrib = user2.rentContribution.toInt().toString()
                                showEditUser2Dialog = true
                            } else {
                                viewModel.addUser("Nadiya", "Weekly Income", 0.0, 1000000.0)
                            }
                        }
                        .testTag("profile_card_nadiya"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (user2 != null) Icons.Default.Person2 else Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "User 2 (Partner)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(text = user2?.name ?: "No Partner added", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (user2 != null) {
                                Text(text = "Contribution to rent: Rp ${user2.rentContribution.toInt()}", style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text(text = "Tap to create Partner ledger", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Icon(
                            imageVector = if (user2 != null) Icons.Default.Edit else Icons.Default.ArrowForward,
                            contentDescription = "Edit User",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // --- PREFERENCES (LANGUAGES) ---
        item {
            Text(
                text = "Preferences".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = SageLocalizer.t("settings", lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "Select display localizations.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }

                    Row {
                        Button(
                            onClick = { viewModel.setLanguage("en") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (lang == "en") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .testTag("lang_en")
                                .padding(end = 4.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("EN", color = if (lang == "en") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }

                        Button(
                            onClick = { viewModel.setLanguage("id") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (lang == "id") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("lang_id"),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("ID", color = if (lang == "id") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        // Theme setting card
        item {
            val themeM by viewModel.themeMode.collectAsState()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "App Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "Choose your preferred primary interface mode.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { viewModel.setThemeMode("Light") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (themeM == "Light") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f).padding(end = 4.dp).testTag("theme_light"),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("Light", color = if (themeM == "Light") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                        }

                        Button(
                            onClick = { viewModel.setThemeMode("Dark") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (themeM == "Dark") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f).padding(horizontal = 2.dp).testTag("theme_dark"),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("Dark", color = if (themeM == "Dark") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                        }

                        Button(
                            onClick = { viewModel.setThemeMode("System") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (themeM == "System") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1.2f).padding(start = 4.dp).testTag("theme_system"),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("System", color = if (themeM == "System") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // --- NOTIFICATIONS SETTING ---
        item {
            val notificationsEnabled by viewModel.isNotificationsEnabled.collectAsState()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = SageLocalizer.t("notifications", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = SageLocalizer.t("notifications_desc", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setNotificationsEnabled(!notificationsEnabled) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = SageLocalizer.t("enable_notifications", lang),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Checkbox(
                            checked = notificationsEnabled,
                            onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                            modifier = Modifier.testTag("enable_notifications_checkbox")
                        )
                    }
                }
            }
        }

        // --- BACKUP & EMERGENCY MAINTENANCE ---
        item {
            Text(
                text = SageLocalizer.t("backup_maintenance", lang).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Export/Import card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable {
                        backupJson = viewModel.exportBackupJson()
                        backupStatusText = ""
                        showBackupDialog = true
                    }
                    .testTag("action_backup"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Database Backups Manager", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "Export or import custom backup codes easily.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                }
            }
        }

        // Reset database card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .clickable {
                        resetInputText = ""
                        showResetDialog = true
                    }
                    .testTag("action_reset_all"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = SageLocalizer.t("reset_all_data", lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text(text = "Clear and reset all seed templates.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    // Edit User 1 Dialog
    if (showEditUser1Dialog && user1 != null) {
        AlertDialog(
            onDismissRequest = { showEditUser1Dialog = false },
            title = { Text("Edit Space Leader Profile") },
            text = {
                Column {
                    OutlinedTextField(
                        value = u1Name,
                        onValueChange = { u1Name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = u1Salary,
                        onValueChange = { u1Salary = it },
                        label = { Text("Income (Salary)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = u1RentContrib,
                        onValueChange = { u1RentContrib = it },
                        label = { Text("Rent (Kos) Contribution Target") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sal = u1Salary.toDoubleOrNull() ?: user1.incomeAmount
                        val rent = u1RentContrib.toDoubleOrNull() ?: user1.rentContribution
                        if (u1Name.isNotEmpty()) {
                            viewModel.updateUser(
                                user1.copy(
                                    name = u1Name,
                                    incomeAmount = sal,
                                    rentContribution = rent
                                )
                            )
                        }
                        showEditUser1Dialog = false
                    },
                    modifier = Modifier.testTag("btn_save_u1")
                ) {
                    Text(SageLocalizer.t("save", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditUser1Dialog = false }) {
                    Text(SageLocalizer.t("cancel", lang))
                }
            }
        )
    }

    // Edit User 2 Dialog
    if (showEditUser2Dialog && user2 != null) {
        AlertDialog(
            onDismissRequest = { showEditUser2Dialog = false },
            title = { Text("Edit Partner Profile") },
            text = {
                Column {
                    OutlinedTextField(
                        value = u2Name,
                        onValueChange = { u2Name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = u2RentContrib,
                        onValueChange = { u2RentContrib = it },
                        label = { Text("Rent (Kos) Contribution Target") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rent = u2RentContrib.toDoubleOrNull() ?: user2.rentContribution
                        if (u2Name.isNotEmpty()) {
                            viewModel.updateUser(
                                user2.copy(
                                    name = u2Name,
                                    rentContribution = rent
                                )
                            )
                        }
                        showEditUser2Dialog = false
                    },
                    modifier = Modifier.testTag("btn_save_u2")
                ) {
                    Text(SageLocalizer.t("save", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditUser2Dialog = false }) {
                    Text(SageLocalizer.t("cancel", lang))
                }
            }
        )
    }

    // Export/Import Backups Dialog
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Database Backups") },
            text = {
                Column {
                    Text("Copy this code snippet to export or paste a valid JSON string code below to import.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = backupJson,
                        onValueChange = { backupJson = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("backup_snippet_text"),
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    if (backupStatusText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(backupStatusText, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Row {
                    Button(
                        onClick = {
                            val success = viewModel.importBackupJson(backupJson)
                            backupStatusText = if (success) "Backup Code Imported Successfully!" else "Failed to Parse Backup JSON! Check format."
                        },
                        modifier = Modifier.testTag("btn_import_backup")
                    ) {
                        Text("Import Code")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Factory Reset Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Erase App Database?", color = MaterialTheme.colorScheme.error) },
            text = {
                Column {
                    Text(SageLocalizer.t("reset_prompt", lang), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetInputText,
                        onValueChange = { resetInputText = it },
                        placeholder = { Text("RESET") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_input_text")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetInputText.trim() == "RESET") {
                            viewModel.resetAllDataConfirmed()
                            showResetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = resetInputText.trim() == "RESET",
                    modifier = Modifier.testTag("btn_confirm_all_wipe")
                ) {
                    Text(SageLocalizer.t("confirm", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(SageLocalizer.t("cancel", lang))
                }
            }
        )
    }
}
