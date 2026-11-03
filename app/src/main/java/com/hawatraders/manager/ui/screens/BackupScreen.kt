package com.hawatraders.manager.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hawatraders.manager.data.OwnerSession
import com.hawatraders.manager.data.image.ImageStore
import com.hawatraders.manager.ui.components.IconLabel
import com.hawatraders.manager.ui.components.LocalContainer
import com.hawatraders.manager.ui.components.PasswordField
import com.hawatraders.manager.ui.components.ProgressDialog
import com.hawatraders.manager.ui.components.SecureScreenEffect
import com.hawatraders.manager.util.Strings
import com.hawatraders.manager.viewmodel.AppViewModelProvider
import com.hawatraders.manager.viewmodel.BackupViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    vm: BackupViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    SecureScreenEffect()
    val context = LocalContext.current
    val session = LocalContainer.current.session
    val snackbar = remember { SnackbarHostState() }

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        session.externalLaunch = false
        if (uri != null) vm.saveTo(uri)
    }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.backupTitle, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.back)
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(Strings.backupWarning, style = MaterialTheme.typography.bodyLarge)
                }
            }

            PasswordField(
                value = vm.password,
                onValueChange = vm::onPassword,
                label = Strings.backupPassword,
                error = vm.passwordError,
                enabled = !vm.busy,
            )
            PasswordField(
                value = vm.confirm,
                onValueChange = vm::onConfirm,
                label = Strings.confirmBackupPassword,
                error = vm.confirmError,
                imeAction = ImeAction.Done,
                onImeAction = vm::create,
                enabled = !vm.busy,
            )
            Button(
                onClick = vm::create,
                enabled = !vm.busy,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { IconLabel(Icons.Filled.Backup, Strings.createBackup) }

            val file = vm.readyFile
            AnimatedVisibility(visible = file != null) {
                if (file != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(Strings.backupReady, style = MaterialTheme.typography.titleMedium)
                            Text(
                                file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = {
                                    session.externalLaunch = true
                                    try {
                                        createDocument.launch(file.name)
                                    } catch (e: Exception) {
                                        session.externalLaunch = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                            ) {
                                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(Strings.saveToFile)
                            }
                            OutlinedButton(
                                onClick = { shareFile(context, session, file) },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(Strings.share)
                            }
                        }
                    }
                }
            }
        }
    }

    if (vm.busy) ProgressDialog(Strings.creatingBackup)
}

/** Shares the encrypted file (e.g. over WhatsApp) through a FileProvider content URI. */
private fun shareFile(context: Context, session: OwnerSession, file: File) {
    val uri = FileProvider.getUriForFile(context, ImageStore.authority(context), file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    session.externalLaunch = true
    try {
        context.startActivity(Intent.createChooser(intent, Strings.share))
    } catch (e: Exception) {
        session.externalLaunch = false
    }
}
