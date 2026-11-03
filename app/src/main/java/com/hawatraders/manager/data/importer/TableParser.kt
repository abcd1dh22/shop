package com.hawatraders.manager.data.importer

import android.util.Xml
import com.hawatraders.manager.util.AppException
import com.hawatraders.manager.util.Strings
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.TreeMap
import java.util.zip.ZipInputStream
import org.xmlpull.v1.XmlPullParser

/**
 * Turns a CSV or .xlsx file into rows of cell text, with no third-party libraries.
 * An .xlsx is a ZIP of XML files; only the first worksheet and the shared-strings table are read.
 */
object TableParser {
    private const val MAX_ENTRY_BYTES = 40 * 1024 * 1024

    fun parse(bytes: ByteArray): List<List<String>> {
        // Legacy binary .xls (OLE2 header D0 CF 11 E0) is not supported.
        if (bytes.size >= 4 && bytes[0] == 0xD0.toByte() && bytes[1] == 0xCF.toByte()) {
            throw AppException(Strings.errXls)
        }
        val isZip = bytes.size >= 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()
        return if (isZip) parseXlsx(bytes) else parseCsv(bytes)
    }

    // ---------------------------------------------------------------- CSV

    private fun parseCsv(bytes: ByteArray): List<List<String>> {
        var text = String(bytes, Charsets.UTF_8)
        if (text.startsWith("﻿")) text = text.substring(1)
        val delim = detectDelimiter(text)

        val rows = ArrayList<List<String>>()
        val row = ArrayList<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        cell.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    cell.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    delim -> {
                        row.add(cell.toString())
                        cell.setLength(0)
                    }
                    '\r' -> Unit
                    '\n' -> {
                        row.add(cell.toString())
                        cell.setLength(0)
                        rows.add(ArrayList(row))
                        row.clear()
                    }
                    else -> cell.append(c)
                }
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row.add(cell.toString())
            rows.add(ArrayList(row))
        }
        return rows.filter { r -> r.any { it.isNotBlank() } }
    }

    /** Excel in some locales writes ';' or tab instead of ','. Pick the most frequent on line 1. */
    private fun detectDelimiter(text: String): Char {
        val firstLine = text.substringBefore('\n')
        val candidates = charArrayOf(',', ';', '\t')
        var best = ','
        var bestCount = 0
        for (d in candidates) {
            val n = firstLine.count { it == d }
            if (n > bestCount) {
                best = d
                bestCount = n
            }
        }
        return best
    }

    // --------------------------------------------------------------- XLSX

    private fun parseXlsx(bytes: ByteArray): List<List<String>> {
        var sharedXml: ByteArray? = null
        val sheets = TreeMap<String, ByteArray>()
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name == "xl/sharedStrings.xml") {
                        sharedXml = readEntry(zis)
                    } else if (name.startsWith("xl/worksheets/") && name.endsWith(".xml") &&
                        !name.contains("/_rels/")
                    ) {
                        sheets[name] = readEntry(zis)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw AppException(Strings.errReadFile, e)
        }
        val sheet = sheets.firstEntry()?.value ?: throw AppException(Strings.errEmptyFile)
        val shared = sharedXml?.let { parseSharedStrings(it) } ?: emptyList()
        return parseSheet(sheet, shared)
    }

    private fun readEntry(zis: ZipInputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val n = zis.read(buf)
            if (n < 0) break
            total += n
            if (total > MAX_ENTRY_BYTES) throw AppException(Strings.errFileTooLarge)
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    private fun parseSharedStrings(xml: ByteArray): List<String> {
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(xml), "UTF-8")
        val list = ArrayList<String>()
        val sb = StringBuilder()
        var inSi = false
        var inT = false
        var inPhonetic = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "si" -> {
                        inSi = true
                        sb.setLength(0)
                    }
                    "rPh" -> inPhonetic = true
                    "t" -> inT = inSi && !inPhonetic
                }
                XmlPullParser.TEXT -> if (inT) sb.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "t" -> inT = false
                    "rPh" -> inPhonetic = false
                    "si" -> {
                        list.add(sb.toString())
                        inSi = false
                    }
                }
            }
            event = parser.next()
        }
        return list
    }

    private fun parseSheet(xml: ByteArray, shared: List<String>): List<List<String>> {
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(xml), "UTF-8")
        val rows = ArrayList<List<String>>()
        var current: MutableList<String>? = null
        var colIdx = 0
        var cellType = ""
        var inV = false
        var inT = false
        val value = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> current = ArrayList()
                    "c" -> {
                        cellType = parser.getAttributeValue(null, "t") ?: ""
                        val ref = parser.getAttributeValue(null, "r")
                        colIdx = if (ref != null) columnIndex(ref) else (current?.size ?: 0)
                        value.setLength(0)
                    }
                    "v" -> inV = true
                    "t" -> inT = true
                }
                XmlPullParser.TEXT -> if (inV || inT) value.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "v" -> inV = false
                    "t" -> inT = false
                    "c" -> {
                        val text = if (cellType == "s") {
                            shared.getOrElse(value.toString().trim().toIntOrNull() ?: -1) { "" }
                        } else {
                            value.toString()
                        }
                        val row = current
                        if (row != null && colIdx >= 0) {
                            while (row.size < colIdx) row.add("")
                            if (row.size == colIdx) row.add(text) else row[colIdx] = text
                        }
                    }
                    "row" -> {
                        current?.let { r -> if (r.any { it.isNotBlank() }) rows.add(r) }
                        current = null
                    }
                }
            }
            event = parser.next()
        }
        return rows
    }

    /** "A1" -> 0, "B7" -> 1, "AA3" -> 26. */
    private fun columnIndex(ref: String): Int {
        var n = 0
        for (ch in ref) {
            n = when (ch) {
                in 'A'..'Z' -> n * 26 + (ch - 'A' + 1)
                in 'a'..'z' -> n * 26 + (ch - 'a' + 1)
                else -> return n - 1
            }
        }
        return n - 1
    }
}
