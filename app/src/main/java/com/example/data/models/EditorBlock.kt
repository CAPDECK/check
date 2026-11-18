package com.example.data.models

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
data class BlockStyle(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    val textColorHex: String = "#000000",
    val bgColorHex: String = "#00000000",
    val lineSpacing: Float = 1.25f,
    val textIndentDp: Int = 0
)

/**
 * Saved reusable template for custom blocks
 */
@Immutable
data class BlockTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,                    // Human-readable template name (e.g. "Закадровый голос")
    val typeId: String = "custom",       // Base type or "custom"
    val customTypeName: String? = null,  // Badge label (e.g. "З.К.", "ТИТР", "ЗВУК")
    val customColorHex: String? = null,  // Custom accent color
    val customIconName: String? = null,  // Icon descriptor
    val customPrefix: String? = null,    // Optional text prefix (e.g. "[З.К.]", "[ТИТР]")
    val alignment: String = "LEFT",
    val style: BlockStyle = BlockStyle()
)

@Immutable
data class EditorBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: String = "paragraph", // paragraph, h1, h2, h3, quote, bullet_list, ordered_list, image, scene, action, character, parenthetical, dialogue, transition, custom
    val text: String = "",
    val alignment: String = "LEFT", // LEFT, CENTER, RIGHT, JUSTIFY
    val style: BlockStyle = BlockStyle(),
    val imageUrl: String? = null,
    val imageSize: Float = 0.8f, // 0.2 to 1.0 (multiplier of screen width)
    val imageCaption: String? = null,
    // Custom screenplay block & dialogue parameters:
    val customTypeName: String? = null,
    val customColorHex: String? = null,
    val customIconName: String? = null,
    val customPrefix: String? = null,
    val parenthetical: String? = null
)
