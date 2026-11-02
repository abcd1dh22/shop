package com.hawatraders.manager.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hawatraders.manager.ui.components.LocalContainer
import com.hawatraders.manager.ui.components.PasswordField
import com.hawatraders.manager.ui.components.ProgressDialog
import com.hawatraders.manager.ui.components.SecureScreenEffect
import com.hawatraders.manager.util.Strings
import com.hawatraders.manager.viewmodel.AppViewModelProvider
import com.hawatraders.manager.viewmodel.ImportState
import com.hawatraders.manager.viewmodel.ImportViewModel
import com.hawatraders.manager.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onAddProduct: () -> Unit,
    onManageProducts: () -> Unit,
    onChangePassword: () -> Unit,
    onBackup: () -> Unit,
    vm: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    importVm: ImportViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    SecureScreenEffect()
    val session = LocalContainer.current.session
    val hasPassword by vm.hasPassword.collectAsStateWithLifecycle()
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(importVm.message) {
        importVm.message?.let {
            snackbar.showSnackbar(it)
            importVm.consumeMessage()
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        session.externalLaunch = false
        if (uri != null) importVm.onFilePicked(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.tabSettings, style = MaterialTheme.typography.titleLarge) },
                actions = {
                    if (unlocked) {
                        IconButton(onClick = vm::lock) {
                            Icon(Icons.Filled.Lock, contentDescription = Strings.lock)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                hasPassword == null -> Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                    CircularProgressIndicator()
                }
                hasPassword == false -> CreatePasswordCard(vm)
                !unlocked -> LoginCard(vm)
                else -> OwnerMenu(onAddProduct, onManageProducts, onChangePassword, onBackup)
            }

            SettingsCard {
                MenuHeader(Icons.Filled.FileUpload, Strings.importTitle, Strings.importSub)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        session.externalLaunch = true
                        try {
                            picker.launch(arrayOf("*/*"))
                        } catch (e: Exception) {
                            session.externalLaunch = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(Strings.pickFile) }
            }
        }
    }

    ImportDialogs(importVm)
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun MenuHeader(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CreatePasswordCard(vm: SettingsViewModel) {
    SettingsCard {
        Text(Strings.createPasswordTitle, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            Strings.createPasswordHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        PasswordField(
            value = vm.password,
            onValueChange = vm::onPasswordChange,
            label = Strings.password,
            error = vm.passwordError,
            enabled = !vm.busy,
        )
        Spacer(Modifier.height(8.dp))
        PasswordField(
            value = vm.confirm,
            onValueChange = vm::onConfirmChange,
            label = Strings.confirmPassword,
            error = vm.confirmError,
            imeAction = ImeAction.Done,
            onImeAction = vm::createPassword,
            enabled = !vm.busy,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = vm::createPassword,
            enabled = !vm.busy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (vm.busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text(Strings.setPassword)
        }
    }
}

@Composable
private fun LoginCard(vm: SettingsViewModel) {
    SettingsCard {
        Text(Strings.ownerArea, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            Strings.enterPasswordHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        PasswordField(
            value = vm.password,
            onValueChange = vm::onPasswordChange,
            label = Strings.password,
            error = if (vm.lockSeconds > 0) Strings.lockedFor(vm.lockSeconds.toLong()) else vm.passwordError,
            imeAction = ImeAction.Done,
            onImeAction = vm::login,
            enabled = !vm.busy && vm.lockSeconds == 0,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = vm::login,
            enabled = !vm.busy && vm.lockSeconds == 0 && vm.password.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (vm.busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text(Strings.unlock)
        }
    }
}

@Composable
private fun OwnerMenu(
    onAddProduct: () -> Unit,
    onManageProducts: () -> Unit,
    onChangePassword: () -> Unit,
    onBackup: () -> Unit,
) {
    MenuItem(Icons.Filled.AddCircle, Strings.addProduct, Strings.addProductSub, onAddProduct)
    MenuItem(Icons.Filled.Inventory2, Strings.manageProducts, Strings.manageProductsSub, onManageProducts)
    MenuItem(Icons.Filled.Key, Strings.changePassword, Strings.changePasswordSub, onChangePassword)
    MenuItem(Icons.Filled.Backup, Strings.backup, Strings.backupSub, onBackup)
}

@Composable
private fun MenuItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(16.dp).height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun ImportDialogs(vm: ImportViewModel) {
    when (val s = vm.state) {
        ImportState.Idle -> Unit
        is ImportState.NeedPassword -> AlertDialog(
            onDismissRequest = vm::cancel,
            title = { Text(Strings.importPasswordTitle) },
            text = {
                PasswordField(
                    value = vm.password,
                    onValueChange = { vm.password = it },
                    label = Strings.backupPassword,
                    error = s.error,
                    imeAction = ImeAction.Done,
                    onImeAction = vm::submitPassword,
                )
            },
            confirmButton = {
                TextButton(onClick = vm::submitPassword, enabled = vm.password.isNotEmpty()) {
                    Text(Strings.importVerify)
                }
            },
            dismissButton = { TextButton(onClick = vm::cancel) { Text(Strings.cancel) } },
        )
        is ImportState.Working -> ProgressDialog(s.label)
        is ImportState.Confirm -> AlertDialog(
            onDismissRequest = vm::cancel,
            title = { Text(Strings.importConfirmTitle) },
            text = {
                Column {
                    Text(Strings.importOverwriteWarning, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        Strings.importCount(s.payload.products.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = vm::confirmRestore) {
                    Text(Strings.importDo, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = vm::cancel) { Text(Strings.cancel) } },
        )
    }
}
