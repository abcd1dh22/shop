package com.hawatraders.manager.viewmodel

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.createSavedStateHandle
import com.hawatraders.manager.AppContainer
import com.hawatraders.manager.HawaApp

/** One factory for every ViewModel; dependencies come from the manual [AppContainer]. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { SearchViewModel(container().productRepository, createSavedStateHandle()) }
        initializer { SettingsViewModel(container().settingsRepository, container().session) }
        initializer {
            ProductEditViewModel(createSavedStateHandle(), container().productRepository, container().imageStore)
        }
        initializer { ProductManageViewModel(container().productRepository) }
        initializer { ChangePasswordViewModel(container().settingsRepository) }
        initializer { BackupViewModel(container().backupManager) }
        initializer { ImportViewModel(container().backupManager) }
        initializer { BulkPriceViewModel(container().productRepository) }
        initializer { ProductImportViewModel(container().productImporter) }
        initializer { TextScanViewModel(container().textScanner, container().productRepository) }
    }

    private fun CreationExtras.container(): AppContainer =
        (this[APPLICATION_KEY] as HawaApp).container
}
