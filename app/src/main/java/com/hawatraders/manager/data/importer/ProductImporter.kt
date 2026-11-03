package com.hawatraders.manager.data.importer

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.hawatraders.manager.data.db.AppDatabase
import com.hawatraders.manager.data.db.Category
import com.hawatraders.manager.data.db.ProductEntity
import com.hawatraders.manager.util.AppException
import com.hawatraders.manager.util.Bn
import com.hawatraders.manager.util.SearchQuery
import com.hawatraders.manager.util.Strings
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One valid row from the file. A null [category] means the column was missing or blank. */
class ImportRow(
    val line: Int,
    val nameBn: String,
    val nameEn: String,
    val company: String,
    val category: Category?,
    val unit: String,
    val price: Double,
    val note: String,
) {
    val key: String get() = keyOf(nameBn, company)
}

/** [errors] holds only the first few messages; [errorCount] is the real total. */
class ParsedImport(
    val rows: List<ImportRow>,
    val errors: List<String>,
    val errorCount: Int,
    val newCount: Int,
    val updateCount: Int,
)

private enum class Field(val aliases: List<String>) {
    NAME_BN(
        listOf(
            "নাম", "পণ্যের নাম", "পণ্যের নাম (বাংলা)", "বাংলা নাম", "namebn", "name_bn",
            "name", "product", "product name", "item",
        )
    ),
    NAME_EN(listOf("ইংরেজি নাম", "ইংরেজি", "পণ্যের নাম (ইংরেজি)", "nameen", "name_en", "english", "english name")),
    COMPANY(listOf("কোম্পানি", "কোম্পানী", "ব্র্যান্ড", "company", "brand")),
    CATEGORY(listOf("ধরন", "ক্যাটাগরি", "category", "type")),
    UNIT(listOf("একক", "unit", "uom")),
    PRICE(listOf("দাম", "মূল্য", "টাকা", "price", "rate")),
    NOTE(listOf("নির্দেশনা", "ব্যবহারের নির্দেশনা", "নোট", "note", "notes", "usage", "description")),
}

private val HEADER_NOISE = Regex("[\\s_\\-*():.]+")
private val PRICE_NOISE = Regex("(?i)tk\\.?|taka|টাকা|৳|/-")

private fun headerKey(s: String): String = s.lowercase().replace(HEADER_NOISE, "")

private fun keyOf(name: String, company: String): String =
    SearchQuery.normalize(name).lowercase() + "|" + SearchQuery.normalize(company).lowercase()

private val CATEGORY_WORDS: Map<String, Category> = mapOf(
    "pesticide" to Category.PESTICIDE, "insecticide" to Category.PESTICIDE, "কীটনাশক" to Category.PESTICIDE,
    "fungicide" to Category.FUNGICIDE, "ছত্রাকনাশক" to Category.FUNGICIDE,
    "herbicide" to Category.HERBICIDE, "আগাছানাশক" to Category.HERBICIDE,
    "fertilizer" to Category.FERTILIZER, "fertiliser" to Category.FERTILIZER, "সার" to Category.FERTILIZER,
    "seed" to Category.SEED, "seeds" to Category.SEED, "বীজ" to Category.SEED,
    "other" to Category.OTHER, "অন্যান্য" to Category.OTHER,
)

/**
 * Imports products from CSV/.xlsx and writes CSV files (template / current products) that Excel can
 * open and that this importer can read back. Matching is by Bengali name + company: existing
 * products are updated, new ones added, nothing is ever deleted.
 */
class ProductImporter(
    private val context: Context,
    private val db: AppDatabase,
) {
    private val dao = db.productDao()
    private val exportDir get() = File(context.cacheDir, "export")

    suspend fun parse(uri: Uri): ParsedImport = withContext(Dispatchers.IO) {
        try {
            val table = TableParser.parse(readLimited(uri))
            buildParsed(table, dao.getAll())
        } catch (e: AppException) {
            throw e
        } catch (e: OutOfMemoryError) {
            throw AppException(Strings.errFileTooLarge)
        } catch (e: Exception) {
            throw AppException(Strings.errReadFile, e)
        }
    }

    /** Inserts new products and updates matching ones inside one Room transaction. */
    suspend fun apply(parsed: ParsedImport) = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            db.withTransaction {
                val existing = HashMap<String, ProductEntity>()
                for (p in dao.getAll()) {
                    val k = keyOf(p.nameBn, p.company)
                    if (!existing.containsKey(k)) existing[k] = p
                }
                val inserts = ArrayList<ProductEntity>()
                val updates = ArrayList<ProductEntity>()
                for (r in parsed.rows) {
                    val old = existing[r.key]
                    if (old == null) {
                        inserts.add(
                            ProductEntity(
                                nameBn = r.nameBn,
                                nameEn = r.nameEn,
                                company = r.company,
                                category = r.category ?: Category.OTHER,
                                unit = r.unit.ifBlank { DEFAULT_UNIT },
                                price = r.price,
                                updatedAt = now,
                                priceUpdatedAt = now,
                                note = r.note,
                            )
                        )
                    } else {
                        updates.add(
                            old.copy(
                                nameEn = r.nameEn.ifBlank { old.nameEn },
                                category = r.category ?: old.category,
                                unit = r.unit.ifBlank { old.unit },
                                price = r.price,
                                note = r.note.ifBlank { old.note },
                                updatedAt = now,
                                priceUpdatedAt = if (old.price != r.price) now else old.priceUpdatedAt,
                            )
                        )
                    }
                }
                dao.updateAll(updates)
                dao.insertAll(inserts)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw AppException(Strings.saveFailed, e)
        }
    }

    /** A CSV with the right header and two sample rows. */
    suspend fun writeTemplate(): File = withContext(Dispatchers.IO) {
        writeCsv(
            "hawa-products-template.csv",
            listOf(
                listOf("নমুনা কীটনাশক", "Sample Pesticide", "নমুনা কোম্পানি", "কীটনাশক", "লিটার", "850", "প্রতি লিটার পানিতে ২ মি.লি. মেশান"),
                listOf("নমুনা সার", "Sample Fertilizer", "", "সার", "কেজি", "32", ""),
            ),
        )
    }

    /** All current products as CSV: edit the prices in Excel and import the file back. */
    suspend fun writeExport(): File = withContext(Dispatchers.IO) {
        val rows = dao.getAll().map { p ->
            listOf(p.nameBn, p.nameEn, p.company, p.category.labelBn, p.unit, Bn.editable(p.price), p.note)
        }
        writeCsv("hawa-products-${Bn.todayStamp()}.csv", rows)
    }

    // ------------------------------------------------------------ internals

    private fun buildParsed(table: List<List<String>>, existing: List<ProductEntity>): ParsedImport {
        if (table.isEmpty()) throw AppException(Strings.errEmptyFile)
        if (table.size - 1 > MAX_ROWS) throw AppException(Strings.errTooManyRows)

        val header = table[0].map { headerKey(it) }
        fun column(field: Field): Int {
            val keys = field.aliases.map { headerKey(it) }
            return header.indexOfFirst { it in keys }
        }
        val iName = column(Field.NAME_BN)
        val iPrice = column(Field.PRICE)
        if (iName < 0 || iPrice < 0) throw AppException(Strings.errMissingColumns)
        val iNameEn = column(Field.NAME_EN)
        val iCompany = column(Field.COMPANY)
        val iCategory = column(Field.CATEGORY)
        val iUnit = column(Field.UNIT)
        val iNote = column(Field.NOTE)

        val existingKeys = HashSet<String>()
        for (p in existing) existingKeys.add(keyOf(p.nameBn, p.company))

        val seen = HashSet<String>()
        val rows = ArrayList<ImportRow>()
        val errors = ArrayList<String>()
        var errorCount = 0
        var newCount = 0
        var updateCount = 0

        fun fail(line: Int, message: String) {
            errorCount++
            if (errors.size < MAX_ERROR_LINES) errors.add(Strings.importLine(line, message))
        }

        for (r in 1 until table.size) {
            val line = r + 1
            val cells = table[r]
            fun cell(i: Int): String = if (i >= 0 && i < cells.size) cells[i].trim() else ""

            val name = cell(iName)
            if (name.isEmpty()) {
                fail(line, Strings.rowNoName)
                continue
            }
            val price = parsePrice(cell(iPrice))
            if (price == null) {
                fail(line, Strings.rowBadPrice)
                continue
            }
            val company = cell(iCompany)
            val row = ImportRow(
                line = line,
                nameBn = name,
                nameEn = cell(iNameEn),
                company = company,
                category = parseCategory(cell(iCategory)),
                unit = cell(iUnit),
                price = price,
                note = cell(iNote).take(2000),
            )
            if (!seen.add(row.key)) {
                fail(line, Strings.rowDuplicate)
                continue
            }
            rows.add(row)
            if (existingKeys.contains(row.key)) updateCount++ else newCount++
        }
        return ParsedImport(rows, errors, errorCount, newCount, updateCount)
    }

    private fun parsePrice(raw: String): Double? {
        val v = Bn.parsePrice(PRICE_NOISE.replace(raw, "").trim()) ?: return null
        return if (v > 0.0) v else null
    }

    private fun parseCategory(raw: String): Category? {
        val k = headerKey(raw)
        if (k.isEmpty()) return null
        return CATEGORY_WORDS[k] ?: Category.OTHER
    }

    private fun readLimited(uri: Uri): ByteArray {
        val input = context.contentResolver.openInputStream(uri) ?: throw AppException(Strings.errReadFile)
        input.use {
            val out = ByteArrayOutputStream()
            val buf = ByteArray(32 * 1024)
            var total = 0L
            while (true) {
                val n = it.read(buf)
                if (n < 0) break
                total += n
                if (total > MAX_FILE_BYTES) throw AppException(Strings.errFileTooLarge)
                out.write(buf, 0, n)
            }
            return out.toByteArray()
        }
    }

    private fun writeCsv(fileName: String, rows: List<List<String>>): File {
        exportDir.mkdirs()
        exportDir.listFiles()?.forEach { it.delete() }
        val sb = StringBuilder("﻿") // BOM so Excel opens Bengali text correctly
        sb.append(HEADER.joinToString(",") { csv(it) }).append("\r\n")
        for (r in rows) sb.append(r.joinToString(",") { csv(it) }).append("\r\n")
        val file = File(exportDir, fileName)
        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }

    private fun csv(s: String): String =
        if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else {
            s
        }

    private companion object {
        const val MAX_FILE_BYTES = 10L * 1024 * 1024
        const val MAX_ROWS = 20_000
        const val MAX_ERROR_LINES = 20
        const val DEFAULT_UNIT = "পিস"
        val HEADER = listOf("নাম", "ইংরেজি নাম", "কোম্পানি", "ধরন", "একক", "দাম", "নির্দেশনা")
    }
}
