package com.hawatraders.manager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hawatraders.manager.data.db.ProductEntity
import com.hawatraders.manager.ui.components.CategoryBadge
import com.hawatraders.manager.ui.components.EmptyState
import com.hawatraders.manager.ui.components.ProductImage
import com.hawatraders.manager.ui.theme.extraColors
import com.hawatraders.manager.util.Bn
import com.hawatraders.manager.util.Strings
import com.hawatraders.manager.viewmodel.AppViewModelProvider
import com.hawatraders.manager.viewmodel.ProductManageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManageScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    vm: ProductManageViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val products by vm.products.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var priceId by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.manageTitle, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(Strings.newProductFab) },
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
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::onQueryChange,
                placeholder = { Text(Strings.searchProducts) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = Strings.clearSearch)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            val list = products
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    list == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                    list.isEmpty() && query.isBlank() ->
                        EmptyState(Icons.Outlined.Inventory2, Strings.emptyNoProducts)
                    list.isEmpty() -> EmptyState(Icons.Outlined.SearchOff, Strings.emptyNoResults)
                    else -> LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(list, key = { it.id }) { p ->
                            ManageRow(
                                product = p,
                                onEdit = { onEdit(p.id) },
                                onFavorite = { vm.toggleFavorite(p) },
                                onPrice = { priceId = p.id },
                                onDelete = { deleteId = p.id },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }

    val deleteTarget = products?.firstOrNull { it.id == deleteId }
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text(Strings.confirmDeleteTitle) },
            text = { Text(Strings.confirmDeleteBody(deleteTarget.nameBn)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(deleteTarget)
                    deleteId = null
                }) { Text(Strings.delete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text(Strings.cancel) } },
        )
    }

    val priceTarget = products?.firstOrNull { it.id == priceId }
    if (priceTarget != null) {
        QuickPriceDialog(
            product = priceTarget,
            onDismiss = { priceId = null },
            onConfirm = { price ->
                vm.updatePrice(priceTarget, price)
                priceId = null
            },
        )
    }
}

@Composable
private fun ManageRow(
    product: ProductEntity,
    onEdit: () -> Unit,
    onFavorite: () -> Unit,
    onPrice: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductImage(
                    imageName = product.imagePath,
                    contentDescription = Strings.productImage,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        product.nameBn,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryBadge(product.category)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            Bn.digits(product.unit),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    Bn.price(product.price),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.extraColors.price,
                    maxLines = 1,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onFavorite, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = if (product.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (product.isFavorite) Strings.favoriteRemove else Strings.favoriteAdd,
                        tint = if (product.isFavorite) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onEdit, modifier = Modifier.height(48.dp)) { Text(Strings.edit) }
                TextButton(onClick = onPrice, modifier = Modifier.height(48.dp)) { Text(Strings.quickPrice) }
                TextButton(onClick = onDelete, modifier = Modifier.height(48.dp)) {
                    Text(Strings.delete, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun QuickPriceDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var text by rememberSaveable(product.id) { mutableStateOf(Bn.editable(product.price)) }
    val parsed = Bn.parsePrice(text)
    val valid = parsed != null && parsed > 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(product.nameBn, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(Strings.newPrice) },
                prefix = { Text("৳ ") },
                isError = !valid,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (valid) onConfirm(parsed!!) }, enabled = valid) { Text(Strings.update) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } },
    )
}
