package com.hawatraders.manager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hawatraders.manager.data.db.Category
import com.hawatraders.manager.data.db.ProductEntity
import com.hawatraders.manager.ui.components.CategoryBadge
import com.hawatraders.manager.ui.components.EmptyState
import com.hawatraders.manager.ui.components.ProductImage
import com.hawatraders.manager.ui.theme.PriceHugeTextStyle
import com.hawatraders.manager.ui.theme.PriceTextStyle
import com.hawatraders.manager.ui.theme.extraColors
import com.hawatraders.manager.util.Bn
import com.hawatraders.manager.util.Strings
import com.hawatraders.manager.viewmodel.AppViewModelProvider
import com.hawatraders.manager.viewmodel.SearchViewModel

/** Result of a barcode scan, handed from the navigation host to the search screen. */
sealed interface ScanOutcome {
    data class Found(val productId: Long) : ScanOutcome
    data object NotFound : ScanOutcome
    /** Fallback from the label-text scan: search by this word. */
    data class Query(val text: String) : ScanOutcome
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    scanOutcome: ScanOutcome? = null,
    onScanHandled: () -> Unit = {},
    vm: SearchViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val snackbar = remember { SnackbarHostState() }
    val state by vm.uiState.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val category by vm.category.collectAsStateWithLifecycle()
    val selectedId by vm.selectedId.collectAsStateWithLifecycle()
    val favOnly by vm.favoritesOnly.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    // The title row slides away once the list is scrolled, leaving more room for products.
    val atTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 }
    }

    // Hide the keyboard as soon as the user starts scrolling the results.
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) focusManager.clearFocus()
    }

    LaunchedEffect(scanOutcome) {
        when (scanOutcome) {
            is ScanOutcome.Found -> {
                // Clear filters so the scanned product is certainly in the list, then open it.
                vm.onQueryChange("")
                vm.onCategoryChange(null)
                vm.setFavoritesOnly(false)
                vm.select(scanOutcome.productId)
                onScanHandled()
            }
            is ScanOutcome.Query -> {
                vm.onCategoryChange(null)
                vm.setFavoritesOnly(false)
                vm.onQueryChange(scanOutcome.text)
                onScanHandled()
            }
            ScanOutcome.NotFound -> {
                onScanHandled()
                snackbar.showSnackbar(Strings.scanNotFound)
            }
            null -> Unit
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .fillMaxSize()
        ) {
            AnimatedVisibility(visible = atTop) {
                TitleRow(totalCount = state.totalCount)
            }

            OutlinedTextField(
                value = query,
                onValueChange = vm::onQueryChange,
                placeholder = { Text(Strings.searchHint) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    AnimatedVisibility(visible = query.isNotEmpty()) {
                        IconButton(onClick = { vm.onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = Strings.clearSearch)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    CategoryChip(
                        label = "${Strings.filterAll} · ${Bn.number(state.totalCount)}",
                        selected = category == null && !favOnly,
                    ) {
                        vm.onCategoryChange(null)
                        vm.setFavoritesOnly(false)
                    }
                }
                item {
                    CategoryChip(
                        label = "${Strings.filterFavorites} · ${Bn.number(state.favoriteCount)}",
                        selected = favOnly,
                        icon = Icons.Filled.Star,
                    ) { vm.setFavoritesOnly(!favOnly) }
                }
                items(Category.entries) { c ->
                    CategoryChip(
                        label = "${c.labelBn} · ${Bn.number(state.counts[c] ?: 0)}",
                        selected = category == c,
                    ) { vm.onCategoryChange(if (category == c) null else c) }
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isLoading -> Unit
                    state.totalCount == 0 ->
                        EmptyState(Icons.Outlined.Inventory2, Strings.emptyNoProducts)
                    state.products.isEmpty() ->
                        EmptyState(Icons.Outlined.SearchOff, Strings.emptyNoResults)
                    else -> LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 64.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.products, key = { it.id }) { product ->
                            ProductRow(
                                product = product,
                                onClick = { vm.select(product.id) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }

    val selected = selectedId?.let { id -> state.products.firstOrNull { it.id == id } }
    if (selected != null) {
        ProductDetailSheet(product = selected, onDismiss = { vm.select(null) })
    }
}

/** Slim app title: leaf mark, name and the total number of products. */
@Composable
private fun TitleRow(totalCount: Int) {
    val extra = MaterialTheme.extraColors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(extra.heroStart),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Eco, contentDescription = null, tint = extra.onHero, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                Strings.appName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (totalCount > 0) Strings.resultCount(totalCount) else Strings.heroSubtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1) },
        leadingIcon = if (icon != null) {
            { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        },
        shape = RoundedCornerShape(20.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

/**
 * Compact price-list row: small photo, name + "company · unit" on the left, bold price on the right.
 * An amber dot before the price means the price has not been changed for over 90 days.
 */
@Composable
fun ProductRow(product: ProductEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val stale = product.priceUpdatedAt > 0L && Bn.ageDays(product.priceUpdatedAt) > 90L
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProductImage(
                imageName = product.imagePath,
                contentDescription = Strings.productImage,
                iconSize = 28.dp,
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        product.nameBn,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (product.isFavorite) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = Strings.filterFavorites,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                val meta = listOf(product.company, Bn.digits(product.unit))
                    .filter { it.isNotBlank() }
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
                Spacer(Modifier.height(4.dp))
                CategoryBadge(product.category)
            }
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (stale) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary)
                            .semantics { contentDescription = Strings.priceStale }
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    Bn.price(product.price),
                    style = PriceTextStyle,
                    color = MaterialTheme.extraColors.price,
                    maxLines = 1,
                )
            }
        }
    }
}

/** "দাম হালনাগাদ: ৩ দিন আগে"; amber after 90 days so stale prices stand out. */
@Composable
private fun PriceDateLine(product: ProductEntity, modifier: Modifier = Modifier) {
    if (product.priceUpdatedAt <= 0L) return
    val stale = Bn.ageDays(product.priceUpdatedAt) > 90L
    Text(
        Strings.priceDate(Bn.ageText(product.priceUpdatedAt)),
        style = MaterialTheme.typography.labelSmall,
        color = if (stale) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 2.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductDetailSheet(product: ProductEntity, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProductImage(
                imageName = product.imagePath,
                contentDescription = Strings.productImage,
                iconSize = 64.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(24.dp)),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                product.nameBn,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium, lineHeight = 34.sp),
                textAlign = TextAlign.Center,
            )
            if (product.nameEn.isNotBlank()) {
                Text(
                    product.nameEn,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(product.category)
                Spacer(Modifier.width(10.dp))
                Text(
                    "${Strings.detailUnit}: ${Bn.digits(product.unit)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (product.company.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "${Strings.detailCompany}: ${product.company}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (product.note.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            Strings.sectionNote,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(product.note, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Surface(
                color = MaterialTheme.extraColors.priceContainer,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    Bn.price(product.price),
                    style = PriceHugeTextStyle,
                    color = MaterialTheme.extraColors.price,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(vertical = 16.dp, horizontal = 12.dp)
                        .semantics { contentDescription = Bn.price(product.price) },
                )
            }
            Spacer(Modifier.height(8.dp))
            PriceDateLine(product)
        }
    }
}
