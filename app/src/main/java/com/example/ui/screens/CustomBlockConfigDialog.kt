package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.BlockStyle
import com.example.data.models.BlockTemplate
import com.example.data.models.EditorBlock
import com.example.ui.viewmodel.WriterViewModel
import kotlin.math.roundToInt

// Curated Material 3 Accent Swatches for Custom Blocks
val CUSTOM_BLOCK_PALETTE = listOf(
    "#E5A93C", // Studio Gold
    "#7B1FA2", // Majestic Purple
    "#0288D1", // Ocean Blue
    "#2E7D32", // Pine Green
    "#E65100", // Sunset Orange
    "#D32F2F", // Crimson Red
    "#00897B", // Deep Teal
    "#3949AB", // Indigo Night
    "#C2185B", // Cinema Rose
    "#0097A7", // Cyber Cyan
    "#F57C00", // Warm Amber
    "#388E3C", // Forest Emerald
    "#5E35B1", // Royal Violet
    "#6D4C41", // Warm Sepia
    "#455A64", // Slate Steel
    "#546E7A"  // Cool Grey
)

data class CustomBlockIconOption(
    val id: String,
    val icon: ImageVector,
    val labelRu: String,
    val labelEn: String
)

val CUSTOM_BLOCK_ICONS = listOf(
    CustomBlockIconOption("tune", Icons.Filled.Tune, "Кастом", "Custom"),
    CustomBlockIconOption("mic", Icons.Filled.Mic, "Голос", "V.O."),
    CustomBlockIconOption("subtitles", Icons.Filled.Subtitles, "Титр", "Title"),
    CustomBlockIconOption("sfx", Icons.AutoMirrored.Filled.VolumeUp, "SFX", "SFX"),
    CustomBlockIconOption("note", Icons.Filled.Lightbulb, "Заметка", "Note"),
    CustomBlockIconOption("flashback", Icons.Filled.History, "Флешбэк", "Past"),
    CustomBlockIconOption("camera", Icons.Filled.Videocam, "Камера", "Cam"),
    CustomBlockIconOption("vfx", Icons.Filled.Bolt, "VFX", "VFX"),
    CustomBlockIconOption("music", Icons.Filled.MusicNote, "Музыка", "Music"),
    CustomBlockIconOption("thought", Icons.Filled.Psychology, "Мысли", "Mind"),
    CustomBlockIconOption("screen", Icons.Filled.Smartphone, "Экран", "SMS"),
    CustomBlockIconOption("location", Icons.Filled.LocationOn, "Место", "Loc"),
    CustomBlockIconOption("bookmark", Icons.Filled.Bookmark, "Сноска", "Mark"),
    CustomBlockIconOption("magic", Icons.Filled.AutoAwesome, "Акцент", "Star")
)

data class QuickBlockPreset(
    val id: String,
    val titleRu: String,
    val titleEn: String,
    val descRu: String,
    val descEn: String,
    val badgeRu: String,
    val badgeEn: String,
    val prefixRu: String,
    val prefixEn: String,
    val colorHex: String,
    val iconId: String,
    val fontId: String,
    val alignment: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false
)

val QUICK_CUSTOM_BLOCK_PRESETS = listOf(
    QuickBlockPreset(
        id = "vo",
        titleRu = "Закадровый голос",
        titleEn = "Voiceover",
        descRu = "Речь рассказчика или мысли за кадром",
        descEn = "Narrator speech or off-screen voice",
        badgeRu = "З.К.",
        badgeEn = "V.O.",
        prefixRu = "[З.К.]",
        prefixEn = "[V.O.]",
        colorHex = "#7B1FA2",
        iconId = "mic",
        fontId = "monospace",
        alignment = "CENTER",
        isItalic = true
    ),
    QuickBlockPreset(
        id = "title",
        titleRu = "Титр на экране",
        titleEn = "On-Screen Title",
        descRu = "Надпись, дата или локация поверх кадра",
        descEn = "Superimposed text, date or location",
        badgeRu = "ТИТР",
        badgeEn = "TITLE",
        prefixRu = "ТИТР:",
        prefixEn = "TITLE:",
        colorHex = "#0288D1",
        iconId = "subtitles",
        fontId = "monospace",
        alignment = "CENTER",
        isBold = true
    ),
    QuickBlockPreset(
        id = "sfx",
        titleRu = "Звуковой эффект",
        titleEn = "Sound Effect",
        descRu = "Акцент на важном звуке или шуме в сцене",
        descEn = "Highlight key sound or audio cue",
        badgeRu = "SFX",
        badgeEn = "SFX",
        prefixRu = "SFX:",
        prefixEn = "SFX:",
        colorHex = "#E65100",
        iconId = "sfx",
        fontId = "monospace",
        alignment = "LEFT",
        isBold = true
    ),
    QuickBlockPreset(
        id = "note",
        titleRu = "Заметка автора",
        titleEn = "Author Note",
        descRu = "Рабочий комментарий или пометка на полях",
        descEn = "Production note or draft comment",
        badgeRu = "ЗАМЕТКА",
        badgeEn = "NOTE",
        prefixRu = "//",
        prefixEn = "//",
        colorHex = "#E5A93C",
        iconId = "note",
        fontId = "sans",
        alignment = "LEFT",
        isItalic = true
    ),
    QuickBlockPreset(
        id = "flashback",
        titleRu = "Флешбэк",
        titleEn = "Flashback",
        descRu = "Переход к воспоминаниям или прошлому",
        descEn = "Scene transition into past memory",
        badgeRu = "ФЛЕШБЭК",
        badgeEn = "FLASHBACK",
        prefixRu = "[ФЛЕШБЭК]",
        prefixEn = "[FLASHBACK]",
        colorHex = "#00897B",
        iconId = "flashback",
        fontId = "serif",
        alignment = "LEFT",
        isBold = true,
        isItalic = true
    ),
    QuickBlockPreset(
        id = "camera",
        titleRu = "Камера / Ракурс",
        titleEn = "Camera Shot",
        descRu = "Крупный план, движение камеры или склейка",
        descEn = "Close-up, tracking shot or cut",
        badgeRu = "КАМЕРА",
        badgeEn = "CAMERA",
        prefixRu = "КАМЕРА:",
        prefixEn = "ANGLE:",
        colorHex = "#3949AB",
        iconId = "camera",
        fontId = "monospace",
        alignment = "RIGHT",
        isBold = true
    ),
    QuickBlockPreset(
        id = "vfx",
        titleRu = "Визуальный эффект",
        titleEn = "Visual VFX",
        descRu = "Компьютерная графика и спецэффекты",
        descEn = "CGI and special visual elements",
        badgeRu = "VFX",
        badgeEn = "VFX",
        prefixRu = "[VFX]",
        prefixEn = "[VFX]",
        colorHex = "#D32F2F",
        iconId = "vfx",
        fontId = "monospace",
        alignment = "LEFT",
        isBold = true
    ),
    QuickBlockPreset(
        id = "music",
        titleRu = "Музыка",
        titleEn = "Music Cue",
        descRu = "Музыкальная тема или песня в сцене",
        descEn = "Musical score or song in the scene",
        badgeRu = "МУЗЫКА",
        badgeEn = "MUSIC",
        prefixRu = "♪",
        prefixEn = "♪",
        colorHex = "#C2185B",
        iconId = "music",
        fontId = "serif",
        alignment = "LEFT",
        isItalic = true
    ),
    QuickBlockPreset(
        id = "thought",
        titleRu = "Внутренний монолог",
        titleEn = "Inner Thought",
        descRu = "Непроизнесённые мысли персонажа",
        descEn = "Unspoken internal character monologue",
        badgeRu = "МЫСЛИ",
        badgeEn = "THOUGHT",
        prefixRu = "(мысленно)",
        prefixEn = "(thinking)",
        colorHex = "#5E35B1",
        iconId = "thought",
        fontId = "serif",
        alignment = "CENTER",
        isItalic = true
    ),
    QuickBlockPreset(
        id = "screen",
        titleRu = "Экран / СМС",
        titleEn = "Screen / SMS",
        descRu = "Переписка в телефоне, чат или монитор",
        descEn = "Phone text message, chat or monitor",
        badgeRu = "ЭКРАН",
        badgeEn = "SCREEN",
        prefixRu = "[СМС]:",
        prefixEn = "[TEXT]:",
        colorHex = "#0097A7",
        iconId = "screen",
        fontId = "monospace",
        alignment = "LEFT",
        isBold = false
    )
)

fun encodeCustomIconAndFont(iconId: String, fontId: String): String {
    val cleanIcon = iconId.ifBlank { "tune" }
    val cleanFont = fontId.ifBlank { "serif" }
    return "$cleanIcon|$cleanFont"
}

fun extractCustomIconId(customIconName: String?): String {
    if (customIconName.isNullOrBlank()) return "tune"
    if (customIconName == "monospace" || customIconName == "normal") return "tune"
    return customIconName.substringBefore("|").ifBlank { "tune" }
}

fun extractCustomFontId(customIconName: String?, alignment: String = "LEFT"): String {
    if (customIconName.isNullOrBlank()) {
        return if (alignment == "CENTER") "monospace" else "serif"
    }
    if (customIconName == "monospace") return "monospace"
    if (customIconName == "normal") return "serif"
    val after = customIconName.substringAfter("|", "")
    return after.ifBlank { "serif" }
}

fun getCustomBlockIconVector(customIconName: String?): ImageVector {
    val iconId = extractCustomIconId(customIconName)
    return CUSTOM_BLOCK_ICONS.find { it.id == iconId }?.icon ?: Icons.Filled.Tune
}

fun getCustomBlockFontFamily(customIconName: String?): FontFamily {
    return when (extractCustomFontId(customIconName)) {
        "monospace" -> FontFamily.Monospace
        "sans" -> FontFamily.SansSerif
        else -> FontFamily.Serif
    }
}

/**
 * Sleek, self-sizing centered Custom Block Studio dialog.
 * Eliminates duplicate top/preview headers, oversized stacked text fields, and clipped horizontal icon cards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomBlockConfigDialog(
    block: EditorBlock,
    viewModel: WriterViewModel,
    onSaveBlock: (EditorBlock) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val blockTemplates by viewModel.blockTemplates.collectAsStateWithLifecycle()

    var customTypeName by remember(block.id, appLanguage) {
        mutableStateOf(
            com.example.util.LocalizationManager.translateBlockBadge(
                block.customTypeName?.ifBlank { null } ?: "CUSTOM",
                appLanguage
            )
        )
    }
    var customPrefix by remember(block.id, appLanguage) {
        mutableStateOf(
            com.example.util.LocalizationManager.translateBlockPrefix(
                block.customPrefix ?: "",
                appLanguage
            )
        )
    }
    var customColorHex by remember { mutableStateOf(block.customColorHex ?: "#E5A93C") }
    var alignment by remember { mutableStateOf(block.alignment.ifBlank { "LEFT" }) }
    var blockText by remember { mutableStateOf(block.text) }

    var selectedIconId by remember { mutableStateOf(extractCustomIconId(block.customIconName)) }
    var selectedFontId by remember { mutableStateOf(extractCustomFontId(block.customIconName, block.alignment)) }

    var isBold by remember { mutableStateOf(block.style.isBold) }
    var isItalic by remember { mutableStateOf(block.style.isItalic) }
    var isUnderline by remember { mutableStateOf(block.style.isUnderline) }
    var isStrikethrough by remember { mutableStateOf(block.style.isStrikethrough) }

    var activeTab by remember { mutableIntStateOf(0) }
    var showCustomColorSliders by remember { mutableStateOf(false) }
    var templateNameInput by remember { mutableStateOf("") }

    val activeColor = remember(customColorHex) {
        parseColorOrNull(customColorHex) ?: Color(0xFFE5A93C)
    }
    val initialHsv = remember(activeColor) { colorToHsv(activeColor) }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var valueBrightness by remember { mutableFloatStateOf(initialHsv[2]) }
    var hexInputText by remember(customColorHex) { mutableStateOf(customColorHex.removePrefix("#").uppercase()) }

    fun updateFromHsv(h: Float, s: Float, v: Float) {
        hue = h.coerceIn(0f, 360f)
        saturation = s.coerceIn(0f, 1f)
        valueBrightness = v.coerceIn(0f, 1f)
        val c = hsvToColor(hue, saturation, valueBrightness)
        val newHex = colorToHex(c)
        customColorHex = newHex
        hexInputText = newHex.removePrefix("#")
    }

    fun selectHexColor(hex: String) {
        val c = parseColorOrNull(hex) ?: return
        val norm = colorToHex(c)
        customColorHex = norm
        hexInputText = norm.removePrefix("#")
        val hsv = colorToHsv(c)
        hue = hsv[0]
        saturation = hsv[1]
        valueBrightness = hsv[2]
    }

    val activeIconVector = remember(selectedIconId) {
        CUSTOM_BLOCK_ICONS.find { it.id == selectedIconId }?.icon ?: Icons.Filled.Tune
    }
    val activeFontFamily = remember(selectedFontId) {
        when (selectedFontId) {
            "monospace" -> FontFamily.Monospace
            "sans" -> FontFamily.SansSerif
            else -> FontFamily.Serif
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 390.dp)
                    .fillMaxWidth()
                    .heightIn(max = 545.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    // 1. Unified Header + Integrated Live Block Preview + Tab Row
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = lerp(MaterialTheme.colorScheme.surfaceContainerHigh, activeColor, 0.25f),
                                    border = BorderStroke(1.dp, activeColor.copy(alpha = 0.55f)),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = activeIconVector,
                                            contentDescription = null,
                                            tint = activeColor,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = tr("Конструктор блока", "Custom Block Studio", appLanguage),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = tr("Пресеты, метка, стиль и шаблоны", "Presets, badge, style & templates", appLanguage),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalIconButton(
                                    onClick = {
                                        customTypeName = com.example.util.LocalizationManager.translateBlockBadge("CUSTOM", appLanguage)
                                        customPrefix = ""
                                        selectHexColor("#E5A93C")
                                        selectedIconId = "tune"
                                        selectedFontId = "serif"
                                        alignment = "LEFT"
                                        isBold = false
                                        isItalic = false
                                        isUnderline = false
                                        isStrikethrough = false
                                    },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = l("reset", appLanguage),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                FilledTonalIconButton(
                                    onClick = onDismiss,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = l("close", appLanguage),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        // Integrated Live Screenplay Block Preview (Single clean bar, no duplicate headers!)
                        CompactBlockPreviewBanner(
                            appLanguage = appLanguage,
                            customTypeName = customTypeName,
                            customPrefix = customPrefix,
                            blockText = blockText,
                            activeColor = activeColor,
                            activeIcon = activeIconVector,
                            activeFontFamily = activeFontFamily,
                            alignment = alignment,
                            isBold = isBold,
                            isItalic = isItalic,
                            isUnderline = isUnderline,
                            isStrikethrough = isStrikethrough,
                            selectedFontId = selectedFontId
                        )

                        // Sleek Segmented Navigation Tabs
                        val tabs = listOf(
                            Triple(0, tr("Основа", "Identity", appLanguage), Icons.Filled.AutoAwesomeMosaic),
                            Triple(1, tr("Стиль и цвет", "Style", appLanguage), Icons.Filled.Palette),
                            Triple(2, tr("Шаблоны (${blockTemplates.size})", "Templates (${blockTemplates.size})", appLanguage), Icons.Filled.Bookmarks)
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                tabs.forEach { (idx, title, icon) ->
                                    val isSel = activeTab == idx
                                    val tabBg by animateColorAsState(
                                        targetValue = if (isSel) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                        label = "customBlockTabBg"
                                    )
                                    val tabContent by animateColorAsState(
                                        targetValue = if (isSel) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        label = "customBlockTabContent"
                                    )

                                    Surface(
                                        onClick = { activeTab = idx },
                                        shape = RoundedCornerShape(9.dp),
                                        color = tabBg,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(30.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = tabContent,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = title,
                                                fontSize = 10.5.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = tabContent,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                    // 2. Self-Sizing Content Body (weight(1f, fill = false))
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (activeTab) {
                                0 -> CustomBlockIdentityTab(
                                    appLanguage = appLanguage,
                                    customTypeName = customTypeName,
                                    onTypeNameChange = { customTypeName = it },
                                    customPrefix = customPrefix,
                                    onPrefixChange = { customPrefix = it },
                                    selectedIconId = selectedIconId,
                                    onIconSelect = { selectedIconId = it },
                                    blockText = blockText,
                                    onBlockTextChange = { blockText = it },
                                    activeColor = activeColor,
                                    onApplyQuickPreset = { preset ->
                                        customTypeName = com.example.util.LocalizationManager.translateBlockBadge(preset.badgeEn, appLanguage)
                                        customPrefix = com.example.util.LocalizationManager.translateBlockPrefix(preset.prefixEn, appLanguage)
                                        selectHexColor(preset.colorHex)
                                        selectedIconId = preset.iconId
                                        selectedFontId = preset.fontId
                                        alignment = preset.alignment
                                        isBold = preset.isBold
                                        isItalic = preset.isItalic
                                        isUnderline = preset.isUnderline
                                        isStrikethrough = false
                                    }
                                )

                                1 -> CustomBlockStyleAndColorTab(
                                    appLanguage = appLanguage,
                                    context = context,
                                    activeColor = activeColor,
                                    customColorHex = customColorHex,
                                    onSelectHex = { selectHexColor(it) },
                                    showCustomColorSliders = showCustomColorSliders,
                                    onToggleCustomColorSliders = { showCustomColorSliders = !showCustomColorSliders },
                                    hue = hue,
                                    saturation = saturation,
                                    valueBrightness = valueBrightness,
                                    onUpdateHsv = { h, s, v -> updateFromHsv(h, s, v) },
                                    hexInputText = hexInputText,
                                    onHexInputChange = { raw ->
                                        val filtered = raw.filter { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }.take(6).uppercase()
                                        hexInputText = filtered
                                        if (filtered.length == 6) {
                                            selectHexColor("#$filtered")
                                        }
                                    },
                                    alignment = alignment,
                                    onAlignmentChange = { alignment = it },
                                    selectedFontId = selectedFontId,
                                    onFontSelect = { selectedFontId = it },
                                    isBold = isBold,
                                    onToggleBold = { isBold = !isBold },
                                    isItalic = isItalic,
                                    onToggleItalic = { isItalic = !isItalic },
                                    isUnderline = isUnderline,
                                    onToggleUnderline = { isUnderline = !isUnderline },
                                    isStrikethrough = isStrikethrough,
                                    onToggleStrikethrough = { isStrikethrough = !isStrikethrough }
                                )

                                2 -> CustomBlockTemplatesTab(
                                    appLanguage = appLanguage,
                                    context = context,
                                    blockTemplates = blockTemplates,
                                    templateNameInput = templateNameInput,
                                    onTemplateNameInputChange = { templateNameInput = it },
                                    currentTypeName = customTypeName,
                                    onSaveNewTemplate = { name ->
                                        val encodedIconFont = encodeCustomIconAndFont(selectedIconId, selectedFontId)
                                        val fallbackBadge = com.example.util.LocalizationManager.translateBlockBadge("CUSTOM", appLanguage)
                                        val newTmpl = BlockTemplate(
                                            name = name,
                                            typeId = "custom",
                                            customTypeName = customTypeName.ifBlank { fallbackBadge },
                                            customColorHex = customColorHex,
                                            customIconName = encodedIconFont,
                                            customPrefix = customPrefix.ifBlank { null },
                                            alignment = alignment,
                                            style = BlockStyle(
                                                isBold = isBold,
                                                isItalic = isItalic,
                                                isUnderline = isUnderline,
                                                isStrikethrough = isStrikethrough
                                            )
                                        )
                                        viewModel.saveBlockTemplate(newTmpl)
                                        templateNameInput = ""
                                    },
                                    onLoadTemplate = { tmpl ->
                                        val fallbackBadge = com.example.util.LocalizationManager.translateBlockBadge("CUSTOM", appLanguage)
                                        customTypeName = com.example.util.LocalizationManager.translateBlockBadge(
                                            tmpl.customTypeName ?: fallbackBadge,
                                            appLanguage
                                        )
                                        customPrefix = com.example.util.LocalizationManager.translateBlockPrefix(
                                            tmpl.customPrefix ?: "",
                                            appLanguage
                                        )
                                        selectHexColor(tmpl.customColorHex ?: "#E5A93C")
                                        selectedIconId = extractCustomIconId(tmpl.customIconName)
                                        selectedFontId = extractCustomFontId(tmpl.customIconName, tmpl.alignment)
                                        alignment = tmpl.alignment
                                        isBold = tmpl.style.isBold
                                        isItalic = tmpl.style.isItalic
                                        isUnderline = tmpl.style.isUnderline
                                        isStrikethrough = tmpl.style.isStrikethrough
                                    },
                                    onApplyTemplateImmediately = { tmpl ->
                                        val encodedIconFont = tmpl.customIconName ?: encodeCustomIconAndFont("tune", "serif")
                                        val updatedBlock = block.copy(
                                            type = "custom",
                                            text = blockText,
                                            customTypeName = com.example.util.LocalizationManager.translateBlockBadge(
                                                tmpl.customTypeName ?: tmpl.name,
                                                appLanguage
                                            ),
                                            customPrefix = tmpl.customPrefix?.let {
                                                com.example.util.LocalizationManager.translateBlockPrefix(it, appLanguage)
                                            },
                                            customColorHex = tmpl.customColorHex ?: "#E5A93C",
                                            customIconName = encodedIconFont,
                                            alignment = tmpl.alignment,
                                            style = tmpl.style
                                        )
                                        onSaveBlock(updatedBlock)
                                        onDismiss()
                                    },
                                    onDeleteTemplate = { id ->
                                        viewModel.deleteBlockTemplate(id)
                                    },
                                    onRestoreDefaultTemplates = {
                                        QUICK_CUSTOM_BLOCK_PRESETS.take(4).reversed().forEach { preset ->
                                            val tmpl = BlockTemplate(
                                                name = com.example.util.LocalizationManager.translatePresetTitle(
                                                    preset.id,
                                                    preset.titleRu,
                                                    preset.titleEn,
                                                    appLanguage
                                                ),
                                                typeId = "custom",
                                                customTypeName = com.example.util.LocalizationManager.translateBlockBadge(preset.badgeEn, appLanguage),
                                                customColorHex = preset.colorHex,
                                                customIconName = encodeCustomIconAndFont(preset.iconId, preset.fontId),
                                                customPrefix = com.example.util.LocalizationManager.translateBlockPrefix(preset.prefixEn, appLanguage),
                                                alignment = preset.alignment,
                                                style = BlockStyle(
                                                    isBold = preset.isBold,
                                                    isItalic = preset.isItalic,
                                                    isUnderline = preset.isUnderline
                                                )
                                            )
                                            viewModel.saveBlockTemplate(tmpl)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                    // 3. Sleek Bottom Action Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = l("cancel", appLanguage),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                val fallbackTmpl = com.example.util.LocalizationManager.translateTemplateName("My Template", appLanguage)
                                val name = templateNameInput.ifBlank { customTypeName.ifBlank { fallbackTmpl } }
                                val encodedIconFont = encodeCustomIconAndFont(selectedIconId, selectedFontId)
                                val fallbackBadge = com.example.util.LocalizationManager.translateBlockBadge("CUSTOM", appLanguage)
                                val newTmpl = BlockTemplate(
                                    name = name,
                                    typeId = "custom",
                                    customTypeName = customTypeName.ifBlank { fallbackBadge },
                                    customColorHex = customColorHex,
                                    customIconName = encodedIconFont,
                                    customPrefix = customPrefix.ifBlank { null },
                                    alignment = alignment,
                                    style = BlockStyle(
                                        isBold = isBold,
                                        isItalic = isItalic,
                                        isUnderline = isUnderline,
                                        isStrikethrough = isStrikethrough
                                    )
                                )
                                viewModel.saveBlockTemplate(newTmpl)
                                templateNameInput = ""
                                val savedMsg = "${l("save", appLanguage)}: $name"
                                Toast.makeText(context, savedMsg, Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tr("В шаблоны", "Template", appLanguage),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = {
                                val fallbackName = com.example.util.LocalizationManager.translateBlockBadge("CUSTOM", appLanguage)
                                val encodedIconFont = encodeCustomIconAndFont(selectedIconId, selectedFontId)
                                val updatedBlock = block.copy(
                                    type = "custom",
                                    text = blockText,
                                    customTypeName = customTypeName.trim().ifBlank { fallbackName },
                                    customPrefix = customPrefix.trim().ifBlank { null },
                                    customColorHex = customColorHex,
                                    customIconName = encodedIconFont,
                                    alignment = alignment,
                                    style = block.style.copy(
                                        isBold = isBold,
                                        isItalic = isItalic,
                                        isUnderline = isUnderline,
                                        isStrikethrough = isStrikethrough
                                    )
                                )
                                onSaveBlock(updatedBlock)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = activeColor,
                                contentColor = if (isColorBright(activeColor)) Color(0xFF141414) else Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("apply_custom_block_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tr("Применить", "Apply Block", appLanguage),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean, single-strip live screenplay block preview inside the header.
 * Shows the exact badge, prefix, font, alignment, and accent bar without duplicate chrome.
 */
@Composable
private fun CompactBlockPreviewBanner(
    appLanguage: String,
    customTypeName: String,
    customPrefix: String,
    blockText: String,
    activeColor: Color,
    activeIcon: ImageVector,
    activeFontFamily: FontFamily,
    alignment: String,
    isBold: Boolean,
    isItalic: Boolean,
    isUnderline: Boolean,
    isStrikethrough: Boolean,
    selectedFontId: String
) {
    val animActiveColor by animateColorAsState(targetValue = activeColor, label = "animBlockColor")
    val sampleText = blockText.ifBlank {
        tr("Пример текста вашего блока в сценарии...", "Sample text of your screenplay block...", appLanguage)
    }
    val textDecoration = when {
        isUnderline && isStrikethrough -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
        isUnderline -> TextDecoration.Underline
        isStrikethrough -> TextDecoration.LineThrough
        else -> TextDecoration.None
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = lerp(MaterialTheme.colorScheme.surfaceContainerLowest, animActiveColor, 0.07f),
        border = BorderStroke(1.dp, animActiveColor.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left screenplay block accent bar
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(animActiveColor)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = activeIcon,
                            contentDescription = null,
                            tint = animActiveColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = animActiveColor.copy(alpha = 0.18f)
                        ) {
                            val fallbackBadge = com.example.util.LocalizationManager.translateBlockBadge("CUSTOM", appLanguage)
                            Text(
                                text = customTypeName.ifBlank { fallbackBadge }.uppercase(),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = animActiveColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        if (customPrefix.isNotBlank()) {
                            Text(
                                text = customPrefix,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = animActiveColor
                            )
                        }
                    }

                    val alignTranslated = com.example.util.LocalizationManager.translateAlignment(alignment, appLanguage)
                    val fontTranslated = com.example.util.LocalizationManager.translateFontName(selectedFontId, appLanguage)
                    Text(
                        text = "$alignTranslated • $fontTranslated",
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = sampleText,
                    style = TextStyle(
                        fontSize = 11.5.sp,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = textDecoration,
                        fontFamily = activeFontFamily,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = when (alignment) {
                            "CENTER" -> TextAlign.Center
                            "RIGHT" -> TextAlign.Right
                            else -> TextAlign.Left
                        }
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// --- Tab 1: Identity & Presets (Redesigned for compact, zero-overflow ergonomics) ---

@Composable
private fun CustomBlockIdentityTab(
    appLanguage: String,
    customTypeName: String,
    onTypeNameChange: (String) -> Unit,
    customPrefix: String,
    onPrefixChange: (String) -> Unit,
    selectedIconId: String,
    onIconSelect: (String) -> Unit,
    blockText: String,
    onBlockTextChange: (String) -> Unit,
    activeColor: Color,
    onApplyQuickPreset: (QuickBlockPreset) -> Unit
) {
    // 1. Sleek Horizontal Screenplay Preset Pills (34dp height instead of 85dp bulky boxes)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = tr("Сценарные пресеты", "Quick Screenplay Presets", appLanguage),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QUICK_CUSTOM_BLOCK_PRESETS.forEach { preset ->
                    val pColor = parseColorOrNull(preset.colorHex) ?: MaterialTheme.colorScheme.primary
                    val pIcon = CUSTOM_BLOCK_ICONS.find { it.id == preset.iconId }?.icon ?: Icons.Filled.Tune
                    val presetBadge = com.example.util.LocalizationManager.translateBlockBadge(preset.badgeEn, appLanguage)
                    val presetTitle = com.example.util.LocalizationManager.translatePresetTitle(
                        preset.id,
                        preset.titleRu,
                        preset.titleEn,
                        appLanguage
                    )
                    val isCurrentlyMatched = customTypeName.equals(presetBadge, ignoreCase = true)

                    Surface(
                        onClick = { onApplyQuickPreset(preset) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCurrentlyMatched) {
                            lerp(MaterialTheme.colorScheme.surfaceContainerHighest, pColor, 0.24f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        border = BorderStroke(
                            width = if (isCurrentlyMatched) 1.4.dp else 0.7.dp,
                            color = if (isCurrentlyMatched) pColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(pColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = pIcon,
                                    contentDescription = null,
                                    tint = pColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }

                            Text(
                                text = presetTitle,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = pColor.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = presetBadge,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = pColor,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 2. Side-by-Side Badge Label & Prefix Marker + Quick Prefix Chips
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = tr("Метка и префикс", "Badge Label & Prefix", appLanguage),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Side-by-side compact inputs (36dp height instead of two stacked 58dp OutlinedTextFields!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Block Label Input
                CompactStudioTextField(
                    value = customTypeName,
                    onValueChange = onTypeNameChange,
                    placeholder = tr("Метка (З.К.)", "Label (V.O.)", appLanguage),
                    leadingIcon = Icons.AutoMirrored.Filled.Label,
                    leadingTint = activeColor,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("custom_block_type_name_input")
                )

                // Prefix Marker Input
                CompactStudioTextField(
                    value = customPrefix,
                    onValueChange = onPrefixChange,
                    placeholder = tr("Префикс ([З.К.])", "Prefix ([V.O.])", appLanguage),
                    leadingIcon = Icons.AutoMirrored.Filled.ShortText,
                    leadingTint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("custom_block_prefix_input")
                )
            }

            // Sleek Quick Prefix Pills
            val prefixSuggestions = remember(appLanguage) {
                listOf("[V.O.]", "TITLE:", "SFX:", "//", "[FLASHBACK]", "♪", "ANGLE:", "(thinking)", "[TEXT]:")
                    .map { com.example.util.LocalizationManager.translateBlockPrefix(it, appLanguage) }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                prefixSuggestions.forEach { pref ->
                    val isSel = customPrefix == pref
                    Surface(
                        onClick = { onPrefixChange(if (isSel) "" else pref) },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSel) activeColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            width = if (isSel) 1.2.dp else 0.6.dp,
                            color = if (isSel) activeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Text(
                            text = pref,
                            fontSize = 9.5.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp)
                        )
                    }
                }
            }
        }
    }

    // 3. 2x7 All-Visible Block Icon Grid (No clipped cards on the right edge!)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = tr("Иконка блока", "Block Icon", appLanguage),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            CUSTOM_BLOCK_ICONS.chunked(7).forEach { rowIcons ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rowIcons.forEach { item ->
                        val isSel = selectedIconId == item.id
                        val iconLabel = com.example.util.LocalizationManager.translateIconLabel(
                            item.id,
                            item.labelRu,
                            item.labelEn,
                            appLanguage
                        )
                        Surface(
                            onClick = { onIconSelect(item.id) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) {
                                lerp(MaterialTheme.colorScheme.surfaceContainerHighest, activeColor, 0.25f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            border = BorderStroke(
                                width = if (isSel) 1.4.dp else 0.6.dp,
                                color = if (isSel) activeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 1.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(1.5.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = iconLabel,
                                    tint = if (isSel) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = iconLabel,
                                    fontSize = 7.5.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 4. Compact Quick Block Text Entry
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = tr("Текст блока", "Block Content", appLanguage),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            CompactStudioTextField(
                value = blockText,
                onValueChange = onBlockTextChange,
                placeholder = tr("Введите текст блока...", "Type block text...", appLanguage),
                leadingIcon = Icons.Outlined.EditNote,
                leadingTint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_block_content_input")
            )
        }
    }
}

/**
 * Compact 34dp Studio Input Field built on BasicTextField to avoid M3 OutlinedTextField's mandatory 56dp height.
 */
@Composable
private fun CompactStudioTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    leadingTint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = leadingTint,
                modifier = Modifier.size(14.dp)
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    textStyle = TextStyle(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (value.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .clickable { onValueChange("") }
                )
            }
        }
    }
}

// --- Tab 2: Style & Color ---

@Composable
private fun CustomBlockStyleAndColorTab(
    appLanguage: String,
    context: Context,
    activeColor: Color,
    customColorHex: String,
    onSelectHex: (String) -> Unit,
    showCustomColorSliders: Boolean,
    onToggleCustomColorSliders: () -> Unit,
    hue: Float,
    saturation: Float,
    valueBrightness: Float,
    onUpdateHsv: (Float, Float, Float) -> Unit,
    hexInputText: String,
    onHexInputChange: (String) -> Unit,
    alignment: String,
    onAlignmentChange: (String) -> Unit,
    selectedFontId: String,
    onFontSelect: (String) -> Unit,
    isBold: Boolean,
    onToggleBold: () -> Unit,
    isItalic: Boolean,
    onToggleItalic: () -> Unit,
    isUnderline: Boolean,
    onToggleUnderline: () -> Unit,
    isStrikethrough: Boolean,
    onToggleStrikethrough: () -> Unit
) {
    // 1. Typography, Alignment & Formatting Toolbar Card
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text = tr("Шрифт и форматирование", "Font & Formatting", appLanguage),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 3 Compact Font Family Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val fonts = listOf(
                    Triple("serif", tr("С засечками", "Serif", appLanguage), FontFamily.Serif),
                    Triple("sans", tr("Без засечек", "Sans", appLanguage), FontFamily.SansSerif),
                    Triple("monospace", tr("Сценарный", "Mono", appLanguage), FontFamily.Monospace)
                )
                fonts.forEach { (fontId, label, family) ->
                    val isSel = selectedFontId == fontId
                    Surface(
                        onClick = { onFontSelect(fontId) },
                        shape = RoundedCornerShape(9.dp),
                        color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            width = if (isSel) 1.4.dp else 0.6.dp,
                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Aa",
                                fontFamily = family,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Unified Single-Row Alignment + Text Style (B / I / U / S) Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "LEFT" to Icons.AutoMirrored.Filled.FormatAlignLeft,
                    "CENTER" to Icons.Default.FormatAlignCenter,
                    "RIGHT" to Icons.AutoMirrored.Filled.FormatAlignRight
                ).forEach { (alignKey, icon) ->
                    val isSelected = alignment == alignKey
                    Surface(
                        onClick = { onAlignmentChange(alignKey) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            width = if (isSelected) 1.2.dp else 0.6.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = alignKey,
                                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(2.dp))

                listOf(
                    Triple(isBold, onToggleBold, Icons.Default.FormatBold),
                    Triple(isItalic, onToggleItalic, Icons.Default.FormatItalic),
                    Triple(isUnderline, onToggleUnderline, Icons.Default.FormatUnderlined),
                    Triple(isStrikethrough, onToggleStrikethrough, Icons.Default.StrikethroughS)
                ).forEach { (active, onToggle, icon) ->
                    Surface(
                        onClick = onToggle,
                        shape = RoundedCornerShape(8.dp),
                        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            width = if (active) 1.2.dp else 0.6.dp,
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // 2. Marker Accent Color + Single-Track Sliders
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tr("Цвет маркера", "Marker Color", appLanguage),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = customColorHex,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    onClick = onToggleCustomColorSliders,
                    shape = RoundedCornerShape(7.dp),
                    color = if (showCustomColorSliders) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (showCustomColorSliders) Icons.Default.ExpandLess else Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = tr("Спектр", "Sliders", appLanguage),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CUSTOM_BLOCK_PALETTE.chunked(8).forEach { rowHexes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowHexes.forEach { hex ->
                            val swatchColor = parseColorOrNull(hex) ?: Color(0xFFE5A93C)
                            val isSelected = hex.equals(customColorHex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(27.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.8.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Black.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    )
                                    .clickable { onSelectHex(hex) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (isColorBright(swatchColor)) Color.Black else Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showCustomColorSliders,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    BlockGradientSliderRow(
                        label = l("hue", appLanguage),
                        valueText = "${hue.roundToInt()}°",
                        value = hue,
                        valueRange = 0f..360f,
                        gradientColors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
                        thumbColor = hsvToColor(hue, 1f, 1f),
                        onValueChange = { onUpdateHsv(it, saturation, valueBrightness) }
                    )

                    BlockGradientSliderRow(
                        label = l("saturation", appLanguage),
                        valueText = "${(saturation * 100).roundToInt()}%",
                        value = saturation,
                        valueRange = 0f..1f,
                        gradientColors = listOf(hsvToColor(hue, 0f, valueBrightness), hsvToColor(hue, 1f, valueBrightness)),
                        thumbColor = activeColor,
                        onValueChange = { onUpdateHsv(hue, it, valueBrightness) }
                    )

                    BlockGradientSliderRow(
                        label = l("brightness", appLanguage),
                        valueText = "${(valueBrightness * 100).roundToInt()}%",
                        value = valueBrightness,
                        valueRange = 0.1f..1f,
                        gradientColors = listOf(Color.Black, hsvToColor(hue, saturation, 1f)),
                        thumbColor = activeColor,
                        onValueChange = { onUpdateHsv(hue, saturation, it) }
                    )

                    // Compact 32dp HEX Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(activeColor)
                                .border(0.8.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        )

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("#", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                BasicTextField(
                                    value = hexInputText,
                                    onValueChange = onHexInputChange,
                                    singleLine = true,
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    textStyle = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        FilledTonalIconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Block HEX", customColorHex))
                                Toast.makeText(
                                    context,
                                    tr("Скопировано: $customColorHex", "Copied: $customColorHex", appLanguage),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.size(32.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy HEX", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

// --- Tab 3: Templates ---

@Composable
private fun CustomBlockTemplatesTab(
    appLanguage: String,
    context: Context,
    blockTemplates: List<BlockTemplate>,
    templateNameInput: String,
    onTemplateNameInputChange: (String) -> Unit,
    currentTypeName: String,
    onSaveNewTemplate: (String) -> Unit,
    onLoadTemplate: (BlockTemplate) -> Unit,
    onApplyTemplateImmediately: (BlockTemplate) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onRestoreDefaultTemplates: () -> Unit
) {
    // 1. Compact Save Current as Template Row
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = tr("Сохранить текущий вид как шаблон", "Save Current Block as Template", appLanguage),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val sampleTmplName = currentTypeName.ifBlank {
                    com.example.util.LocalizationManager.translateTemplateName("Voiceover", appLanguage)
                }
                CompactStudioTextField(
                    value = templateNameInput,
                    onValueChange = onTemplateNameInputChange,
                    placeholder = "${l("template_name", appLanguage)} ($sampleTmplName)",
                    leadingIcon = Icons.Filled.BookmarkBorder,
                    leadingTint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        val fallbackTmpl = com.example.util.LocalizationManager.translateTemplateName("My Template", appLanguage)
                        val finalName = templateNameInput.trim().ifBlank { currentTypeName.trim().ifBlank { fallbackTmpl } }
                        onSaveNewTemplate(finalName)
                        val savedMsg = "${l("save", appLanguage)}: $finalName"
                        Toast.makeText(context, savedMsg, Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(l("save", appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // 2. Saved Templates List
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = tr("Мои шаблоны (${blockTemplates.size})", "Saved Templates (${blockTemplates.size})", appLanguage),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    onClick = {
                        onRestoreDefaultTemplates()
                        Toast.makeText(
                            context,
                            tr("Базовые шаблоны добавлены", "Default templates restored", appLanguage),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(11.dp))
                        Text(
                            text = tr("Базовые", "Defaults", appLanguage),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (blockTemplates.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bookmarks,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = tr("Нет сохранённых шаблонов", "No saved templates yet", appLanguage),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                blockTemplates.forEach { tmpl ->
                    val tmplColor = parseColorOrNull(tmpl.customColorHex ?: "#E5A93C") ?: Color(0xFFE5A93C)
                    val tmplIcon = getCustomBlockIconVector(tmpl.customIconName)
                    val tmplFontId = extractCustomFontId(tmpl.customIconName, tmpl.alignment)
                    val localizedTmplName = com.example.util.LocalizationManager.translateTemplateName(tmpl.name, appLanguage)
                    val localizedBadge = tmpl.customTypeName?.let {
                        com.example.util.LocalizationManager.translateBlockBadge(it, appLanguage)
                    }

                    Surface(
                        onClick = {
                            onLoadTemplate(tmpl)
                            Toast.makeText(context, localizedTmplName, Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(0.8.dp, tmplColor.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(tmplColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = tmplIcon,
                                    contentDescription = null,
                                    tint = tmplColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = localizedTmplName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!localizedBadge.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(3.dp),
                                            color = tmplColor.copy(alpha = 0.16f)
                                        ) {
                                            Text(
                                                text = localizedBadge.uppercase(),
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = tmplColor,
                                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                            )
                                        }
                                    }
                                }

                                val styleSummary = buildList {
                                    if (!tmpl.customPrefix.isNullOrBlank()) {
                                        add(com.example.util.LocalizationManager.translateBlockPrefix(tmpl.customPrefix, appLanguage))
                                    }
                                    add(com.example.util.LocalizationManager.translateAlignment(tmpl.alignment, appLanguage))
                                    add(com.example.util.LocalizationManager.translateFontName(tmplFontId, appLanguage))
                                    if (tmpl.style.isBold) add("B")
                                    if (tmpl.style.isItalic) add("I")
                                    if (tmpl.style.isUnderline) add("U")
                                }.joinToString(" • ")

                                Text(
                                    text = styleSummary,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }

                            FilledTonalIconButton(
                                onClick = { onApplyTemplateImmediately(tmpl) },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = l("apply", appLanguage),
                                    modifier = Modifier.size(13.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    onDeleteTemplate(tmpl.id)
                                    Toast.makeText(context, l("delete", appLanguage), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = l("delete", appLanguage),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockGradientSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    gradientColors: List<Color>,
    thumbColor: Color,
    onValueChange: (Float) -> Unit
) {
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = valueText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .pointerInput(valueRange) {
                    detectTapGestures { offset ->
                        val newFrac = (offset.x / size.width).coerceIn(0f, 1f)
                        val newVal = valueRange.start + newFrac * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newVal)
                    }
                }
                .pointerInput(valueRange) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val newFrac = (change.position.x / size.width).coerceIn(0f, 1f)
                        val newVal = valueRange.start + newFrac * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newVal)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .border(0.6.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(5.dp))
            ) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(gradientColors),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val radiusPx = 7.5.dp.toPx()
                val thumbX = (size.width * fraction).coerceIn(radiusPx, size.width - radiusPx)
                val center = Offset(thumbX, size.height / 2f)
                drawCircle(
                    color = Color.Black.copy(alpha = 0.35f),
                    radius = radiusPx + 1.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = Color.White,
                    radius = radiusPx,
                    center = center
                )
                drawCircle(
                    color = thumbColor,
                    radius = radiusPx - 2.2.dp.toPx(),
                    center = center
                )
            }
        }
    }
}
