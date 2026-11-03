package com.hawatraders.manager.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hawatraders.manager.data.db.Category
import com.hawatraders.manager.ui.components.FormCard
import com.hawatraders.manager.util.Bn
import com.hawatraders.manager.util.Strings
import com.hawatraders.manager.viewmodel.AppViewModelProvider
import com.hawatraders.manager.viewmodel.BulkPriceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkPriceScreen(
    onBack: () -> Unit,
    vm: BulkPriceViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val snackbar = remember { SnackbarHostState() }
    var confirm by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.message = null
        }
    }

    val affected = vm.affected()
    val amount = vm.amount
    val canApply = amount != null && affected.isNotEmpty() && !vm.busy

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.bulkTitle, style = MaterialTheme.typography.titleLarge) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FormCard(Strings.bulkWhich) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = vm.categoryKey == null,
                        onClick = { vm.categoryKey = null },
                        label = { Text(Strings.filterAll) },
                    )
                    Category.entries.forEach { c ->
                        FilterChip(
                            selected = vm.categoryKey == c.key,
                            onClick = { vm.categoryKey = c.key },
                            label = { Text(c.labelBn) },
                        )
                    }
                }
                OutlinedTextField(
                    value = vm.company,
                    onValueChange = { vm.company = it },
                    label = { Text(Strings.bulkCompanyHint) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            FormCard(Strings.bulkHow) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = vm.increase,
                        onClick = { vm.increase = true },
                        label = { Text(Strings.bulkIncrease) },
                    )
                    FilterChip(
                        selected = !vm.increase,
                        onClick = { vm.increase = false },
                        label = { Text(Strings.bulkDecrease) },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = vm.byPercent,
                        onClick = { vm.byPercent = true },
                        label = { Text(Strings.bulkPercent) },
                    )
                    FilterChip(
                        selected = !vm.byPercent,
                        onClick = { vm.byPercent = false },
                        label = { Text(Strings.bulkTaka) },
                    )
                }
                OutlinedTextField(
                    value = vm.amountText,
                    onValueChange = { vm.amountText = it },
                    label = { Text(Strings.bulkAmount) },
                    suffix = { Text(if (vm.byPercent) "%" else "৳") },
                    isError = vm.amountText.isNotBlank() && amount == null,
                    supportingText = if (vm.amountText.isNotBlank() && amount == null) {
                        { Text(Strings.bulkAmountInvalid) }
                    } else {
                        null
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    Strings.bulkRoundNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FormCard(Strings.bulkPreview) {
                Text(
                    if (affected.isEmpty()) Strings.bulkNone else Strings.bulkAffected(affected.size),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (amount != null) {
                    affected.take(5).forEach { p ->
                        val np = vm.newPrice(p) ?: p.price
                        Text(
                            "${p.nameBn}: ${Bn.price(p.price)} → ${Bn.price(np)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (affected.size > 5) {
                        Text(
                            Strings.bulkMore(affected.size - 5),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Button(
                onClick = { confirm = true },
                enabled = canApply,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text(Strings.bulkApply, style = MaterialTheme.typography.titleMedium) }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(Strings.bulkConfirmTitle) },
            text = { Text(Strings.bulkConfirmBody(affected.size)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    vm.apply()
                }) { Text(Strings.bulkApply, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(Strings.cancel) } },
        )
    }
}
