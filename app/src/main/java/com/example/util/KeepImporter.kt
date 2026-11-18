package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.models.BlockStyle
import com.example.data.models.EditorBlock
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

data class ParsedKeepNote(
    val title: String,
    val blocks: List<EditorBlock>,
    val labels: List<String> = emptyList(),
    val isPlainText: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

object KeepImporter {

    /**
     * Reads a list of URIs (ZIP archives from Google Takeout, JSON files, HTML files, or TXT/MD files)
     * and returns a list of ParsedKeepNote items.
     */
    fun parseUris(context: Context, uris: List<Uri>): List<ParsedKeepNote> {
        val results = mutableListOf<ParsedKeepNote>()
        for (uri in uris) {
            try {
                val mimeType = context.contentResolver.getType(uri) ?: ""
                val fileName = getFileName(context, uri) ?: "KeepNote"
                
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    if (fileName.endsWith(".zip", ignoreCase = true) || mimeType.contains("zip")) {
                        results.addAll(parseZipStream(inputStream))
                    } else if (fileName.endsWith(".json", ignoreCase = true) || mimeType.contains("json")) {
                        val content = inputStream.bufferedReader().use { it.readText() }
                        parseJsonContent(content, fileName.removeSuffix(".json"))?.let { results.add(it) }
                    } else if (fileName.endsWith(".html", ignoreCase = true) || fileName.endsWith(".htm", ignoreCase = true) || mimeType.contains("html")) {
                        val content = inputStream.bufferedReader().use { it.readText() }
                        results.add(parseHtmlContent(content, fileName.substringBeforeLast(".")))
                    } else {
                        // Plain text or markdown
                        val content = inputStream.bufferedReader().use { it.readText() }
                        results.add(parseTextContent(content, fileName.substringBeforeLast(".")))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return results
    }

    /**
     * Parses raw text or raw JSON entered directly by the user.
     */
    fun parseRawText(rawText: String): List<ParsedKeepNote> {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return emptyList()

        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            try {
                if (trimmed.startsWith("[")) {
                    val array = JSONArray(trimmed)
                    val notes = mutableListOf<ParsedKeepNote>()
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        parseJsonContent(obj.toString(), "Note ${i + 1}")?.let { notes.add(it) }
                    }
                    if (notes.isNotEmpty()) return notes
                } else {
                    parseJsonContent(trimmed, "Keep Note")?.let { return listOf(it) }
                }
            } catch (e: Exception) {
                // Fall back to plain text
            }
        }

        // Split by double newline or delimiter if multiple notes are pasted
        if (trimmed.contains("\n---\n") || trimmed.contains("\n***\n")) {
            val parts = trimmed.split(Regex("\n(---|\\*\\*\\*)\n"))
            return parts.mapIndexed { index, part -> parseTextContent(part, "Keep Note ${index + 1}") }
        }

        return listOf(parseTextContent(trimmed, "Keep Note"))
    }

    private fun parseZipStream(inputStream: InputStream): List<ParsedKeepNote> {
        val notes = mutableListOf<ParsedKeepNote>()
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val entryName = entry.name
                    val lower = entryName.lowercase()
                    if (!lower.contains("__macosx") && !lower.endsWith(".ds_store")) {
                        if (lower.endsWith(".json")) {
                            val content = BufferedReader(InputStreamReader(zis)).readText()
                            val baseName = File(entryName).nameWithoutExtension
                            parseJsonContent(content, baseName)?.let { notes.add(it) }
                        } else if (lower.endsWith(".html") || lower.endsWith(".htm")) {
                            val content = BufferedReader(InputStreamReader(zis)).readText()
                            val baseName = File(entryName).nameWithoutExtension
                            notes.add(parseHtmlContent(content, baseName))
                        } else if (lower.endsWith(".txt") || lower.endsWith(".md")) {
                            val content = BufferedReader(InputStreamReader(zis)).readText()
                            val baseName = File(entryName).nameWithoutExtension
                            notes.add(parseTextContent(content, baseName))
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return notes
    }

    fun parseJsonContent(jsonStr: String, fallbackTitle: String): ParsedKeepNote? {
        return try {
            val json = JSONObject(jsonStr)
            
            val rawTitle = json.optString("title", "").ifEmpty { fallbackTitle }
            val textContent = json.optString("textContent", "")
            val listContent = json.optJSONArray("listContent")
            val labelsArray = json.optJSONArray("labels")
            val timestampUsec = json.optLong("userEditedTimestampUsec", json.optLong("createdTimestampUsec", 0L))
            val createdAt = if (timestampUsec > 0) timestampUsec / 1000 else System.currentTimeMillis()

            val labels = mutableListOf<String>()
            if (labelsArray != null) {
                for (i in 0 until labelsArray.length()) {
                    val labelObj = labelsArray.optJSONObject(i)
                    val lbl = labelObj?.optString("name") ?: labelsArray.optString(i, "")
                    if (lbl.isNotEmpty()) labels.add(lbl)
                }
            }

            val blocks = mutableListOf<EditorBlock>()

            if (textContent.isNotEmpty()) {
                val lines = textContent.split("\n")
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        blocks.add(EditorBlock(type = "paragraph", text = line))
                    }
                }
            }

            if (listContent != null) {
                for (i in 0 until listContent.length()) {
                    val item = listContent.optJSONObject(i) ?: continue
                    val itemText = item.optString("text", "")
                    val isChecked = item.optBoolean("isChecked", false)
                    val prefix = if (isChecked) "☑ " else "☐ "
                    blocks.add(EditorBlock(
                        type = "bullet_list",
                        text = "$prefix$itemText",
                        style = BlockStyle(isStrikethrough = isChecked)
                    ))
                }
            }

            if (blocks.isEmpty()) {
                val altText = json.optString("text", json.optString("content", json.optString("body", "")))
                if (altText.isNotEmpty()) {
                    altText.split("\n").forEach { line ->
                        if (line.isNotBlank()) {
                            blocks.add(EditorBlock(type = "paragraph", text = line))
                        }
                    }
                }
            }

            if (blocks.isEmpty()) {
                blocks.add(EditorBlock(type = "paragraph", text = ""))
            }

            ParsedKeepNote(
                title = rawTitle.ifBlank { "Google Keep Note" },
                blocks = blocks,
                labels = labels,
                createdAt = createdAt
            )
        } catch (e: Exception) {
            null
        }
    }

    fun parseHtmlContent(htmlStr: String, fallbackTitle: String): ParsedKeepNote {
        val titleMatcher = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE)
        val extractedTitle = titleMatcher.find(htmlStr)?.groupValues?.get(1)?.trim()
        val title = extractedTitle?.ifEmpty { null } ?: fallbackTitle

        val h1Matcher = Regex("<h1>(.*?)</h1>", RegexOption.IGNORE_CASE)
        val h1Title = h1Matcher.find(htmlStr)?.groupValues?.get(1)?.trim()

        var cleaned = htmlStr
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>"), "\n")
            .replace(Regex("(?i)</div>"), "\n")
            .replace(Regex("(?i)</li>"), "\n")
            .replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")

        val lines = cleaned.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val blocks = mutableListOf<EditorBlock>()

        for (line in lines) {
            if (line == title || line == h1Title) continue
            if (line.startsWith("☐") || line.startsWith("☑") || line.startsWith("-") || line.startsWith("*")) {
                blocks.add(EditorBlock(type = "bullet_list", text = line))
            } else {
                blocks.add(EditorBlock(type = "paragraph", text = line))
            }
        }

        if (blocks.isEmpty()) {
            blocks.add(EditorBlock(type = "paragraph", text = ""))
        }

        return ParsedKeepNote(
            title = (h1Title ?: title).ifBlank { "Keep Note" },
            blocks = blocks
        )
    }

    fun parseTextContent(textStr: String, fallbackTitle: String): ParsedKeepNote {
        val lines = textStr.split("\n").map { it.trimEnd() }
        val firstLine = lines.firstOrNull { it.isNotBlank() } ?: fallbackTitle
        val title = if (firstLine.length <= 60 && !firstLine.contains(".")) firstLine else fallbackTitle

        val blocks = mutableListOf<EditorBlock>()
        var skipFirst = title == firstLine

        for (line in lines) {
            if (skipFirst && line.trim() == firstLine) {
                skipFirst = false
                continue
            }
            if (line.isNotBlank()) {
                val trimmed = line.trim()
                if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")) {
                    blocks.add(EditorBlock(type = "bullet_list", text = trimmed.substring(2)))
                } else {
                    blocks.add(EditorBlock(type = "paragraph", text = line))
                }
            }
        }

        if (blocks.isEmpty()) {
            blocks.add(EditorBlock(type = "paragraph", text = textStr))
        }

        return ParsedKeepNote(
            title = title,
            blocks = blocks
        )
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}
