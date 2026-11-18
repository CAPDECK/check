package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.models.EditorBlock
import java.io.InputStream
import java.io.OutputStream

object FormatExporter {

    // --- TXT ---
    fun exportToTxt(blocks: List<EditorBlock>, outputStream: OutputStream) {
        outputStream.bufferedWriter().use { writer ->
            for (block in blocks) {
                if (block.type == "image") {
                    writer.write("[Изображение: ${block.imageCaption ?: "без названия"}]\n")
                } else {
                    writer.write(block.text)
                    writer.write("\n")
                }
            }
        }
    }

    fun importFromTxt(inputStream: InputStream): List<EditorBlock> {
        val blocks = mutableListOf<EditorBlock>()
        inputStream.bufferedReader().use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val cleanLine = line?.trim() ?: ""
                if (cleanLine.isNotEmpty()) {
                    val type = if (cleanLine.startsWith("ИНТ.") || cleanLine.startsWith("НАТ.") || cleanLine.startsWith("INT.") || cleanLine.startsWith("EXT.")) "scene" else "action"
                    blocks.add(EditorBlock(type = type, text = cleanLine))
                }
            }
        }
        return if (blocks.isEmpty()) listOf(EditorBlock(type = "scene", text = "")) else blocks
    }

    // --- RTF ---
    fun exportToRtf(blocks: List<EditorBlock>, outputStream: OutputStream) {
        outputStream.bufferedWriter().use { writer ->
            writer.write("{\\rtf1\\ansi\\deff0\n")
            writer.write("{\\fonttbl{\\f0\\froman\\fcharset204 Times New Roman;}{\\f1\\fswiss\\fcharset204 Arial;}}\n")
            writer.write("\\viewkind4\\uc1\\f0\\fs24\n") // Set default Roman font size 12pt (24 in RTF half-points)
            
            for (block in blocks) {
                val fontTag = if (block.type in listOf("scene", "character", "dialogue")) "\\f1" else "\\f0"
                val sizeTag = "\\fs24 "
                
                var alignmentTag = "\\ql "
                if (block.type == "character" || block.alignment == "CENTER") alignmentTag = "\\qc "
                if (block.alignment == "RIGHT") alignmentTag = "\\qr "
                
                // If dialogue has a parenthetical remark, write it first
                if (block.type == "dialogue" && !block.parenthetical.isNullOrBlank()) {
                    var remarkText = "(${block.parenthetical})"
                        .replace("\\", "\\\\")
                        .replace("{", "\\{")
                        .replace("}", "\\}")
                    writer.write("\\qc \\f1\\fs22\\i $remarkText\\i0\n\\par\n")
                }

                writer.write("$alignmentTag$fontTag$sizeTag")
                
                var rawContent = if (block.type == "scene" || block.type == "character") {
                    block.text.uppercase()
                } else if (block.type == "custom" && !block.customPrefix.isNullOrBlank()) {
                    "${block.customPrefix} ${block.text}"
                } else {
                    block.text
                }

                var blockText = rawContent
                    .replace("\\", "\\\\")
                    .replace("{", "\\{")
                    .replace("}", "\\}")
                
                // Bold/Italic details inside block style
                if (block.type == "scene" || block.type == "character" || block.style.isBold) {
                    blockText = "\\b $blockText\\b0 "
                }
                if (block.style.isItalic) {
                    blockText = "\\i $blockText\\i0 "
                }
                if (block.style.isUnderline) {
                    blockText = "\\ul $blockText\\ulnone "
                }
                if (block.style.isStrikethrough) {
                    blockText = "\\strike $blockText\\striked0 "
                }
                
                writer.write(blockText)
                writer.write("\n\\par\n")
            }
            writer.write("}")
        }
    }

    // --- DOCX (Stored with HTML markup styled beautifully for instant Office loading) ---
    fun exportToDocx(title: String, blocks: List<EditorBlock>, outputStream: OutputStream) {
        outputStream.bufferedWriter().use { writer ->
            writer.write("""
                <html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:w='urn:schemas-microsoft-com:office:word' xmlns='http://www.w3.org/TR/REC-html40'>
                <head>
                <title>$title</title>
                <style>
                    body { font-family: 'Courier New', Courier, monospace; font-size: 12pt; line-height: 1.5; margin: 1in; }
                    .scene { font-weight: bold; text-transform: uppercase; margin-top: 18pt; margin-bottom: 6pt; }
                    .action { font-family: 'Times New Roman', serif; margin-top: 6pt; margin-bottom: 6pt; }
                    .character { font-weight: bold; text-align: center; text-transform: uppercase; margin-top: 12pt; margin-bottom: 2pt; margin-left: 1.5in; margin-right: 1.5in; }
                    .parenthetical { font-style: italic; text-align: center; margin-top: 1pt; margin-bottom: 2pt; color: #444; }
                    .dialogue { margin-left: 1.2in; margin-right: 1.0in; margin-bottom: 8pt; }
                    .custom { margin-top: 6pt; margin-bottom: 6pt; }
                    .center { text-align: center; }
                    .right { text-align: right; }
                    .left { text-align: left; }
                </style>
                </head>
                <body>
            """.trimIndent())

            for (block in blocks) {
                var rawContent = when (block.type) {
                    "scene", "character" -> block.text.uppercase()
                    "custom" -> if (!block.customPrefix.isNullOrBlank()) "${block.customPrefix} ${block.text}" else block.text
                    else -> block.text
                }
                var formattedText = rawContent
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")

                if (block.type == "scene" || block.type == "character" || block.style.isBold) {
                    formattedText = "<strong>$formattedText</strong>"
                }
                if (block.style.isItalic) {
                    formattedText = "<em>$formattedText</em>"
                }
                if (block.style.isUnderline) {
                    formattedText = "<u>$formattedText</u>"
                }
                if (block.style.isStrikethrough) {
                    formattedText = "<strike>$formattedText</strike>"
                }

                if (block.type == "dialogue" && !block.parenthetical.isNullOrBlank()) {
                    val pText = block.parenthetical.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    writer.write("<p class='parenthetical'>($pText)</p>\n")
                }

                val blockClass = when (block.type) {
                    "scene" -> "scene"
                    "action" -> "action"
                    "character" -> "character"
                    "dialogue" -> "dialogue"
                    "custom" -> "custom"
                    else -> "action"
                }
                writer.write("<p class='$blockClass'>$formattedText</p>\n")
            }

            writer.write("</body></html>")
        }
    }

    fun importFromDocx(inputStream: InputStream): List<EditorBlock> {
        val blocks = mutableListOf<EditorBlock>()
        inputStream.bufferedReader().use { reader ->
            var content = reader.readText()
            val paragraphRegex = "<p[^>]*>(.*?)</p>|<h1[^>]*>(.*?)</h1>|<h2[^>]*>(.*?)</h2>".toRegex(RegexOption.IGNORE_CASE)
            val matches = paragraphRegex.findAll(content)
            for (match in matches) {
                val text = match.groupValues[1].ifEmpty { match.groupValues[2] }.ifEmpty { match.groupValues[3] }
                val cleanText = text.replace("<[^>]*>".toRegex(), "").trim()
                if (cleanText.isNotEmpty()) {
                    val type = if (cleanText.startsWith("ИНТ.") || cleanText.startsWith("НАТ.") || cleanText.startsWith("INT.") || cleanText.startsWith("EXT.")) "scene" else "action"
                    blocks.add(EditorBlock(type = type, text = cleanText))
                }
            }
            if (blocks.isEmpty()) {
                content.split("\n").forEach { line ->
                    val cl = line.trim()
                    if (cl.isNotEmpty() && !cl.startsWith("<")) {
                        val type = if (cl.startsWith("ИНТ.") || cl.startsWith("НАТ.") || cl.startsWith("INT.") || cl.startsWith("EXT.")) "scene" else "action"
                        blocks.add(EditorBlock(type = type, text = cl))
                    }
                }
            }
        }
        return if (blocks.isEmpty()) listOf(EditorBlock(type = "scene", text = "")) else blocks
    }

    // --- PDF EXPORT (A4/Letter page sizing natively compiled using Canvas layout) ---
    fun exportToPdf(
        title: String,
        blocks: List<EditorBlock>,
        outputStream: OutputStream,
        pageSizeName: String = "A4", // "A4", "LETTER"
        fontPreference: String = "SERIF", // "SERIF", "SANS_SERIF", "MONOSPACE"
        lineSpacingMultiplier: Float = 1.15f,
        includePageNumbers: Boolean = true
    ) {
        val pdfDocument = PdfDocument()
        
        // Sizing
        val (pageWidth, pageHeight) = if (pageSizeName.uppercase() == "LETTER") {
            612 to 792 // Letter: 8.5" x 11" @ 72 points/inch
        } else {
            595 to 842 // A4: 595 x 842
        }
        val margin = 54 // 0.75 in margin
        val contentWidth = pageWidth - (margin * 2)

        var pageNumber = 1
        var myPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var currentPage = pdfDocument.startPage(myPageInfo)
        var canvas = currentPage.canvas

        var currentY = margin.toFloat()

        // Configure default paints
        val textPaint = TextPaint().apply {
            color = android.graphics.Color.BLACK
            isAntiAlias = true
            textSize = 12f
        }

        val titlePaint = TextPaint().apply {
            color = android.graphics.Color.BLACK
            isAntiAlias = true
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        // Draw Document Title
        canvas.drawText(title, margin.toFloat(), currentY + 30, titlePaint)
        currentY += 60f

        val defaultTypeface = when (fontPreference.uppercase()) {
            "MONOSPACE" -> Typeface.MONOSPACE
            "SANS_SERIF" -> Typeface.SANS_SERIF
            else -> Typeface.SERIF
        }

        fun drawPageNumber(canvas: Canvas, pageNum: Int, width: Int, height: Int, m: Int) {
            val paint = Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 9f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val text = "$pageNum"
            val textWidth = paint.measureText(text)
            canvas.drawText(text, (width - textWidth) / 2f, height - (m / 2f), paint)
        }

        for (block in blocks) {
            if (block.type == "image") {
                // If it is an image, draw a placeholder layout with caption offline
                if (currentY + 120 > pageHeight - margin) {
                    if (includePageNumbers) {
                        drawPageNumber(canvas, pageNumber, pageWidth, pageHeight, margin)
                    }
                    pdfDocument.finishPage(currentPage)
                    pageNumber++
                    myPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    currentPage = pdfDocument.startPage(myPageInfo)
                    canvas = currentPage.canvas
                    currentY = margin.toFloat()
                }

                val rectPaint = Paint().apply {
                    color = android.graphics.Color.LTGRAY
                    style = Paint.Style.FILL
                }
                val borderPaint = Paint().apply {
                    color = android.graphics.Color.GRAY
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                canvas.drawRect(margin.toFloat(), currentY, margin.toFloat() + contentWidth, currentY + 80f, rectPaint)
                canvas.drawRect(margin.toFloat(), currentY, margin.toFloat() + contentWidth, currentY + 80f, borderPaint)

                val imageLabelPaint = TextPaint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 10f
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
                }
                val captionText = block.imageCaption ?: "Изображение"
                canvas.drawText(captionText, margin + 20f, currentY + 45f, imageLabelPaint)
                currentY += 100f
                continue
            }

            // Apply style parameters dynamically
            val blockType = block.type
            val isScreenplayFont = blockType in listOf("character", "dialogue", "parenthetical", "scene", "transition")
            
            textPaint.textSize = when (blockType) {
                "h1" -> 20f
                "h2" -> 16f
                "h3" -> 14f
                "quote" -> 11f
                "character" -> 12f
                "scene" -> 12f
                "transition" -> 12f
                else -> 12f
            }

            val isBold = when (blockType) {
                "h1", "h2", "h3", "character", "scene" -> true
                else -> block.style.isBold
            }

            val isItalic = when (blockType) {
                "quote", "parenthetical" -> true
                else -> block.style.isItalic
            }

            val styleFlag = when {
                isBold && isItalic -> Typeface.BOLD_ITALIC
                isBold -> Typeface.BOLD
                isItalic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }

            textPaint.typeface = if (isScreenplayFont) {
                Typeface.create(Typeface.MONOSPACE, styleFlag)
            } else {
                Typeface.create(defaultTypeface, styleFlag)
            }

            if (block.style.textColorHex != "#000000") {
                try {
                    textPaint.color = android.graphics.Color.parseColor(block.style.textColorHex)
                } catch (e: Exception) {
                    textPaint.color = android.graphics.Color.BLACK
                }
            } else {
                textPaint.color = android.graphics.Color.BLACK
            }

            // Alignment mapping
            val layoutAlignment = when {
                blockType == "character" || blockType == "parenthetical" -> Layout.Alignment.ALIGN_CENTER
                block.alignment == "CENTER" -> Layout.Alignment.ALIGN_CENTER
                block.alignment == "RIGHT" || blockType == "transition" -> Layout.Alignment.ALIGN_OPPOSITE
                else -> Layout.Alignment.ALIGN_NORMAL
            }

            // Margin and width adjusting for screenplay formatting
            var blockLeftMargin = margin.toFloat()
            var blockContentWidth = contentWidth

            when (blockType) {
                "dialogue" -> {
                    blockLeftMargin = margin.toFloat() + 60f
                    blockContentWidth = contentWidth - 120
                }
                "parenthetical" -> {
                    blockLeftMargin = margin.toFloat() + 80f
                    blockContentWidth = contentWidth - 160
                }
                "character" -> {
                    blockLeftMargin = margin.toFloat() + 100f
                    blockContentWidth = contentWidth - 200
                }
                "transition" -> {
                    blockLeftMargin = margin.toFloat()
                    blockContentWidth = contentWidth
                }
            }

            // Handle dialogue parenthetical remark if present
            if (block.type == "dialogue" && !block.parenthetical.isNullOrBlank()) {
                val pText = "(${block.parenthetical})"
                val pPaint = TextPaint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 10f
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.ITALIC)
                }
                val pLayout = StaticLayout.Builder.obtain(
                    pText,
                    0,
                    pText.length,
                    pPaint,
                    contentWidth - 140
                )
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1f)
                .setIncludePad(true)
                .build()

                if (currentY + pLayout.height > pageHeight - margin) {
                    if (includePageNumbers) {
                        drawPageNumber(canvas, pageNumber, pageWidth, pageHeight, margin)
                    }
                    pdfDocument.finishPage(currentPage)
                    pageNumber++
                    myPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    currentPage = pdfDocument.startPage(myPageInfo)
                    canvas = currentPage.canvas
                    currentY = margin.toFloat()
                }

                canvas.save()
                canvas.translate(margin.toFloat() + 70f, currentY)
                pLayout.draw(canvas)
                canvas.restore()
                currentY += pLayout.height + 4f
            }

            val textToRender = when (block.type) {
                "scene", "character" -> block.text.uppercase()
                "custom" -> if (!block.customPrefix.isNullOrBlank()) "${block.customPrefix} ${block.text}" else block.text
                else -> block.text
            }

            // Generate StaticLayout (supports multi-line auto breaking wrapping)
            val staticLayout = StaticLayout.Builder.obtain(
                textToRender,
                0,
                textToRender.length,
                textPaint,
                blockContentWidth
            )
            .setAlignment(layoutAlignment)
            .setLineSpacing(0f, lineSpacingMultiplier * block.style.lineSpacing)
            .setIncludePad(true)
            .build()

            val blockHeight = staticLayout.height

            // Page overflow check
            if (currentY + blockHeight > pageHeight - margin) {
                if (includePageNumbers) {
                    drawPageNumber(canvas, pageNumber, pageWidth, pageHeight, margin)
                }
                pdfDocument.finishPage(currentPage)
                pageNumber++
                myPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                currentPage = pdfDocument.startPage(myPageInfo)
                canvas = currentPage.canvas
                currentY = margin.toFloat()
            }

            canvas.save()
            canvas.translate(blockLeftMargin, currentY)
            staticLayout.draw(canvas)
            canvas.restore()

            currentY += blockHeight + 15f // Space between paragraphs
        }

        if (includePageNumbers) {
            drawPageNumber(canvas, pageNumber, pageWidth, pageHeight, margin)
        }
        pdfDocument.finishPage(currentPage)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }
}
