package com.hawatraders.manager

import android.content.Context
import com.hawatraders.manager.data.OwnerSession
import com.hawatraders.manager.data.backup.BackupManager
import com.hawatraders.manager.data.db.AppDatabase
import com.hawatraders.manager.data.image.ImageStore
import com.hawatraders.manager.data.ocr.TextScanner
import com.hawatraders.manager.data.importer.ProductImporter
import com.hawatraders.manager.data.repository.ProductRepository
import com.hawatraders.manager.data.repository.SettingsRepository

/** Manual dependency container (no Hilt) created once by [HawaApp]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.create(appContext) }
    val imageStore = ImageStore(appContext)
    val settingsRepository = SettingsRepository(appContext)
    val productRepository by lazy { ProductRepository(database.productDao(), imageStore) }
    val backupManager by lazy { BackupManager(appContext, database, imageStore) }
    val productImporter by lazy { ProductImporter(appContext, database) }
    val textScanner by lazy { TextScanner(appContext) }
    val session = OwnerSession()
}
