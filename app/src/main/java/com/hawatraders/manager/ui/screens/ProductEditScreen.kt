package com.hawatraders.manager.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hawatraders.manager.data.db.Category
import com.hawatraders.manager.ui.components.FileImage
import com.hawatraders.manager.ui.components.LocalContainer
import com.hawatraders.manager.util.Strings
import com.hawatraders.manager.viewmodel.AppViewModelProvider
import com.hawatraders.manager.viewmodel.ProductEditViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditScreen(
    onBack: () -> Unit,
    vm: ProductEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val container = LocalContainer.current
    val session = container.session
    val snackbar = remember { SnackbarHostState() }
    // Survives process death while the camera app is in the foreground.
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        session.externalLaunch = false
        val uri = cameraUri
        cameraUri = null
        if (ok && uri != null) vm.onImagePicked(uri) else container.imageStore.clearCameraTemp()
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        session.externalLaunch = false
        if (uri != null) vm.onImagePicked(uri)
    }

    LaunchedEffect(vm.saved) { if (vm.saved) onBack() }
    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.message = null
        }
    }

    val imagesDir = container.imageStore.imagesDir
    val previewFile: File? = when {
        vm.stagedImagePath.isNotBlank() -> File(vm.stagedImagePath)
        !vm.removeExisting && vm.existingImage.isNotBlank() -> File(imagesDir, vm.existingImage)
        else -> null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (vm.isNew) Strings.newProduct else Strings.editProduct,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
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
        if (vm.loading) {
            Box(Modifier.padding(padding).fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
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
            // --- Photo ---
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    FileImage(
                        file = previewFile,
                        contentDescription = Strings.productImage,
                        iconSize = 48.dp,
                        modifier = Modifier.size(160.dp).clip(RoundedCornerShape(20.dp)),
                    )
                    if (vm.imageBusy) CircularProgressIndicator()
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val uri = container.imageStore.newCameraUri()
                            cameraUri = uri
                            session.externalLaunch = true
                            try {
                                cameraLauncher.launch(uri)
                            } catch (e: Exception) {
                                session.externalLaunch = false
                            }
                        },
                        enabled = !vm.imageBusy,
                        modifier = Modifier.height(48.dp),
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(Strings.takePhoto)
                    }
                    OutlinedButton(
                        onClick = {
                            session.externalLaunch = true
                            try {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } catch (e: Exception) {
                                session.externalLaunch = false
                            }
                        },
                        enabled = !vm.imageBusy,
                        modifier = Modifier.height(48.dp),
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(Strings.pickPhoto)
                    }
                    if (previewFile != null) {
                        IconButton(onClick = vm::removeImage, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = Strings.removePhoto)
                        }
                    }
                }
            }

            // --- Text fields ---
            OutlinedTextField(
                value = vm.nameBn,
                onValueChange = vm::onNameBn,
                label = { Text(Strings.nameBn) },
                isError = vm.errors.nameBn != null,
                supportingText = if (vm.errors.nameBn != null) {
                    { Text(vm.errors.nameBn!!) }
                } else {
                    null
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = vm.nameEn,
                onValueChange = { vm.nameEn = it },
                label = { Text(Strings.nameEn) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = vm.company,
                onValueChange = { vm.company = it },
                label = { Text(Strings.company) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = vm.category.labelBn,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(Strings.category) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    Category.entries.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c.labelBn) },
                            onClick = {
                                vm.categoryKey = c.key
                                expanded = false
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = vm.unit,
                onValueChange = vm::onUnit,
                label = { Text(Strings.unit) },
                isError = vm.errors.unit != null,
                supportingText = if (vm.errors.unit != null) {
                    { Text(vm.errors.unit!!) }
                } else {
                    null
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Strings.unitSuggestions.forEach { s ->
                    FilterChip(
                        selected = vm.unit == s,
                        onClick = { vm.onUnit(s) },
                        label = { Text(s) },
                    )
                }
            }

            OutlinedTextField(
                value = vm.priceText,
                onValueChange = vm::onPrice,
                label = { Text(Strings.price) },
                prefix = { Text("৳ ") },
                isError = vm.errors.price != null,
                supportingText = if (vm.errors.price != null) {
                    { Text(vm.errors.price!!) }
                } else {
                    null
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = vm::save,
                enabled = !vm.saving && !vm.imageBusy,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                if (vm.saving) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text(Strings.save, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
