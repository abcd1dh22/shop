package com.hawatraders.manager.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hawatraders.manager.data.OwnerSession
import com.hawatraders.manager.data.image.ImageStore
import com.hawatraders.manager.ui.components.FormCard
import com.hawatraders.manager.ui.components.IconLabel
import com.hawatraders.manager.ui.components.LocalContainer
import com.hawatraders.manager.ui.components.ProgressDialog
import com.hawatraders.manager.util.Strings
import com.hawatraders.manager.viewmodel.AppViewModelProvider
import com.hawatraders.manager.viewmodel.ProductImportState
import com.hawatraders.manager.viewmodel.ProductImportViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductImportScreen(
    onBack: () -> Unit,
    vm: ProductImportViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val context = LocalContext.current
    val session = LocalContainer.current.session
    val snackbar = remember { SnackbarHostState() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        session.externalLaunch = false
        if (uri != null) vm.onFilePicked(uri)
    }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }
    LaunchedEffect(vm.shareRequest) {
        vm.shareRequest?.let {
            shareCsv(context, session, it)
            vm.shareRequest = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.importProductsTitle, style = MaterialTheme.typography.titleLarge) },
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FormCard(Strings.importProductsTitle) {
                Text(Strings.importHelp, style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(
                    onClick = vm::makeTemplate,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) { IconLabel(Icons.Filled.Description, Strings.importTemplate) }
                OutlinedButton(
                    onClick = vm::makeExport,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) { IconLabel(Icons.Filled.FileDownload, Strings.importExportCurrent) }
            }
            Button(
                onClick = {
                    session.externalLaunch = true
                    try {
                        picker.launch(arrayOf("*/*"))
                    } catch (e: Exception) {
                        session.externalLaunch = false
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { IconLabel(Icons.Filled.UploadFile, Strings.importPickSheet) }
        }
    }

    when (val s = vm.state) {
        ProductImportState.Idle -> Unit
        is ProductImportState.Working -> ProgressDialog(s.label)
        is ProductImportState.Preview -> {
            val p = s.parsed
            AlertDialog(
                onDismissRequest = vm::cancel,
                title = { Text(Strings.importPreviewTitle) },
                text = {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (p.rows.isEmpty()) {
                            Text(Strings.importNothing, style = MaterialTheme.typography.bodyLarge)
                        } else {
                            Text(
                                Strings.importPlan(p.newCount, p.updateCount),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        if (p.errorCount > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                Strings.importSkipped(p.errorCount),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                            p.errors.take(6).forEach {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = vm::confirm, enabled = p.rows.isNotEmpty()) {
                        Text(Strings.importDo)
                    }
                },
                dismissButton = { TextButton(onClick = vm::cancel) { Text(Strings.cancel) } },
            )
        }
    }
}

/** Hands a CSV in cacheDir/export to the share sheet through the FileProvider. */
private fun shareCsv(context: Context, session: OwnerSession, file: File) {
    val uri = FileProvider.getUriForFile(context, ImageStore.authority(context), file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
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
