package com.hawatraders.manager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hawatraders.manager.data.db.ProductEntity
import com.hawatraders.manager.ui.theme.extraColors
import com.hawatraders.manager.util.Bn
import com.hawatraders.manager.util.ProductMatcher
import com.hawatraders.manager.util.Strings
import com.hawatraders.manager.viewmodel.TextScanState

/** Bottom sheet opened by the centre scan button: barcode or label text. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanMenuSheet(onBarcode: () -> Unit, onText: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(Strings.scanMenuTitle, style = MaterialTheme.typography.titleLarge)
            ScanOption(Icons.Filled.QrCodeScanner, Strings.scanBarcodeTitle, Strings.scanBarcodeDesc, onBarcode)
            ScanOption(Icons.Filled.DocumentScanner, Strings.scanTextTitle, Strings.scanTextDesc, onText)
        }
    }
}

@Composable
private fun ScanOption(icon: ImageVector, title: String, desc: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Progress, failure and result dialogs of the label-text scan. */
@Composable
fun TextScanDialogs(
    state: TextScanState,
    onPick: (Long) -> Unit,
    onSearchText: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    when (state) {
        TextScanState.Idle -> Unit
        TextScanState.Working -> ProgressDialog(Strings.ocrWorking)
        is TextScanState.Failed -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(Strings.ocrFailedTitle) },
            text = { Text(state.message, style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { TextButton(onClick = onDismiss) { Text(Strings.close) } },
        )
        is TextScanState.Result -> {
            // The most distinctive word (the longest) makes the best fallback search term.
            val term = ProductMatcher.tokens(state.text).maxByOrNull { it.length }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(Strings.ocrResultTitle) },
                text = {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (state.matches.isEmpty()) {
                            Text(Strings.ocrNoMatch, style = MaterialTheme.typography.bodyLarge)
                        }
                        state.matches.forEach { MatchRow(it) { onPick(it.id) } }
                    }
                },
                confirmButton = {
                    if (term != null) {
                        TextButton(onClick = { onSearchText(term) }) { Text(Strings.ocrSearchWithText) }
                    }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.close) } },
            )
        }
    }
}

@Composable
private fun MatchRow(product: ProductEntity, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    product.nameBn,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = listOf(product.company, Bn.digits(product.unit)).filter { it.isNotBlank() }
                    .joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                Bn.price(product.price),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.extraColors.price,
                maxLines = 1,
            )
        }
    }
}
