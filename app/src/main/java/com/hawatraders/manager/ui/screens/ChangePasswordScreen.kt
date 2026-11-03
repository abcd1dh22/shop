package com.hawatraders.manager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hawatraders.manager.ui.components.IconLabel
import com.hawatraders.manager.ui.components.PasswordField
import com.hawatraders.manager.ui.components.SecureScreenEffect
import com.hawatraders.manager.util.Strings
import com.hawatraders.manager.viewmodel.AppViewModelProvider
import com.hawatraders.manager.viewmodel.ChangePasswordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    vm: ChangePasswordViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    SecureScreenEffect()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm.done) {
        if (vm.done) {
            snackbar.showSnackbar(Strings.passwordChanged)
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.changePassword, style = MaterialTheme.typography.titleLarge) },
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
            PasswordField(
                value = vm.current,
                onValueChange = vm::onCurrent,
                label = Strings.currentPassword,
                error = vm.currentError,
                enabled = !vm.busy,
            )
            PasswordField(
                value = vm.newPassword,
                onValueChange = vm::onNew,
                label = Strings.newPassword,
                error = vm.newError,
                enabled = !vm.busy,
            )
            PasswordField(
                value = vm.confirm,
                onValueChange = vm::onConfirm,
                label = Strings.confirmNewPassword,
                error = vm.confirmError,
                imeAction = ImeAction.Done,
                onImeAction = vm::submit,
                enabled = !vm.busy,
            )
            Button(
                onClick = vm::submit,
                enabled = !vm.busy,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                if (vm.busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else IconLabel(Icons.Filled.Check, Strings.changeAction)
            }
        }
    }
}
