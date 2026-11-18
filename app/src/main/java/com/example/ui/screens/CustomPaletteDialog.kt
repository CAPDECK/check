package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.CustomPaletteItem
import com.example.ui.viewmodel.WriterViewModel
import java.util.Locale

// --- Color Conversion Helpers ---

fun colorToHex(color: Color): String {
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    return String.format(Locale.US, "#%02X%02X%02X", r, g, b)
}

private val parsedColorCache = java.util.concurrent.ConcurrentHashMap<String, Color>()

fun parseColorOrNull(hex: String): Color? {
    if (hex.isEmpty()) return null
    parsedColorCache[hex]?.let { return it }
    return try {
        val clean = hex.trim().removePrefix("#")
        val parsed = when (clean.length) {
            6, 8 -> Color(android.graphics.Color.parseColor("#$clean"))
            3 -> {
                val expanded = "${clean[0]}${clean[0]}${clean[1]}${clean[1]}${clean[2]}${clean[2]}"
                Color(android.graphics.Color.parseColor("#$expanded"))
            }
            else -> null
        }
        if (parsed != null && parsedColorCache.size < 512) {
            parsedColorCache[hex] = parsed
        }
        parsed
    } catch (e: Exception) {
        null
    }
}

fun colorToHsv(color: Color): FloatArray {
    val hsv = FloatArray(3)
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    android.graphics.Color.RGBToHSV(r, g, b, hsv)
    return hsv
}

fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val colorInt = android.graphics.Color.HSVToColor(
        floatArrayOf(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
    )
    return Color(colorInt)
}

fun isColorBright(color: Color): Boolean {
    val lum = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return lum > 0.55f
}

/**
 * Iconic Google Material 3 / Material You 3-Tone Split Circle Swatch
 */
@Composable
fun MaterialYouSwatchIcon(
    primaryColor: Color,
    containerColor: Color,
    surfaceColor: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    showTuneIconWhenUnselected: Boolean = false
) {
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 1.8.dp else 0.8.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "swatchBorder"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        label = "swatchBorderColor"
    )
    val dividerColor = MaterialTheme.colorScheme.surfaceContainerLow

    Box(
        modifier = modifier
            .size(size)
            .border(borderWidth, borderColor, CircleShape)
            .padding(if (isSelected) 2.5.dp else 1.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            drawArc(
                color = primaryColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                size = Size(w, h)
            )
            drawArc(
                color = containerColor,
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = true,
                size = Size(w, h)
            )
            drawArc(
                color = surfaceColor,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = true,
                size = Size(w, h)
            )
            val strokePx = 1.dp.toPx()
            drawLine(
                color = dividerColor,
                start = Offset(0f, h / 2f),
                end = Offset(w, h / 2f),
                strokeWidth = strokePx
            )
            drawLine(
                color = dividerColor,
                start = Offset(w / 2f, h / 2f),
                end = Offset(w / 2f, h),
                strokeWidth = strokePx
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(size * 0.46f)
                    .clip(CircleShape)
                    .background(primaryColor)
                    .border(1.dp, dividerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = if (isColorBright(primaryColor)) Color(0xFF141414) else Color.White,
                    modifier = Modifier.size(size * 0.28f)
                )
            }
        } else if (showTuneIconWhenUnselected) {
            Box(
                modifier = Modifier
                    .size(size * 0.44f)
                    .clip(CircleShape)
                    .background(surfaceColor.copy(alpha = 0.88f))
                    .border(0.8.dp, primaryColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Customize",
                    tint = primaryColor,
                    modifier = Modifier.size(size * 0.25f)
                )
            }
        }
    }
}

data class PaletteThemePreset(
    val name: String,
    val nameEn: String,
    val description: String,
    val descriptionEn: String,
    val primaryHex: String,
    val bgHex: String,
    val iconEmoji: String
)

val CURATED_THEME_PRESETS = listOf(
    PaletteThemePreset("Золотой век", "Golden Age", "Теплый классический стиль писательской студии", "Warm classic writer studio style", "#E5A93C", "#0E1015", "🌟"),
    PaletteThemePreset("Киберпанк", "Cyberpunk", "Яркий неоновый розовый на глубокой ночи", "Vivid neon pink on midnight backdrop", "#FF007F", "#0A0A12", "⚡"),
    PaletteThemePreset("Изумрудный бор", "Emerald Forest", "Глубокая лесная зелень для концентрации", "Deep pine green for deep focus", "#10B981", "#08140F", "🌲"),
    PaletteThemePreset("Северный океан", "Nordic Ocean", "Ледяной бирюзовый акцент и полуночная синева", "Glacier cyan accent and arctic midnight", "#06B6D4", "#091224", "🌊"),
    PaletteThemePreset("Эспрессо и книги", "Espresso & Books", "Уютный карамельный оттенок кофейни", "Cozy caramel and coffeehouse aesthetic", "#D97706", "#17120F", "☕"),
    PaletteThemePreset("Королевский аметист", "Royal Amethyst", "Благородный фиолетовый для мистических романов", "Noble violet for fantasy and mystery novels", "#A78BFA", "#0D0A18", "👑"),
    PaletteThemePreset("Закат на Ривьере", "Riviera Sunset", "Мягкий кораллово-персиковый градиент", "Warm coral peach sunset gradient", "#FF6B6B", "#161821", "🌅"),
    PaletteThemePreset("Матрица", "Matrix", "Культовый хакерский терминал", "Iconic hacker green terminal", "#22C55E", "#000000", "💻"),
    PaletteThemePreset("Сакура в цвету", "Sakura Bloom", "Нежный романтический розовый бархат", "Gentle romantic floral pink", "#FB7185", "#160F14", "🌸"),
    PaletteThemePreset("Бумажная классика", "Paper Classic", "Традиционная типографская печать на светлом льне", "Traditional typographic ink on bright linen", "#1E40AF", "#F8F8FA", "📜"),
    PaletteThemePreset("Теплый пергамент", "Warm Parchment", "Мягкий винтажный стиль старых рукописей", "Soft vintage look of classic manuscripts", "#B45309", "#FFFDF7", "🌾"),
    PaletteThemePreset("Космический индиго", "Cosmic Indigo", "Глубокий индиго и чернильный космос", "Deep celestial indigo and ink cosmos", "#818CF8", "#0B0F19", "🪐")
)

val CURATED_QUICK_SWATCHES = listOf(
    Color(0xFFD0BCFF), Color(0xFFE5A93C), Color(0xFFF59E0B), Color(0xFFF97316),
    Color(0xFFEF4444), Color(0xFFFF6B6B), Color(0xFFFF007F), Color(0xFFFB7185),
    Color(0xFFD946EF), Color(0xFFA78BFA), Color(0xFF818CF8), Color(0xFF60A5FA),
    Color(0xFF38BDF8), Color(0xFF06B6D4), Color(0xFF2DD4BF), Color(0xFF10B981),
    Color(0xFF22C55E), Color(0xFF84CC16), Color(0xFFEAB308), Color(0xFFFB923C),
    Color(0xFF94A3B8), Color(0xFF64748B), Color(0xFFB45309), Color(0xFF1E40AF)
)

data class BackgroundToneOption(
    val id: String,
    val name: String,
    val nameEn: String,
    val hex: String,
    val color: Color,
    val isDark: Boolean
)

val BACKGROUND_TONE_OPTIONS = listOf(
    BackgroundToneOption("oled", "OLED Черный", "OLED Black", "#000000", Color(0xFF000000), true),
    BackgroundToneOption("obsidian", "Обсидиан", "Obsidian", "#0E1015", Color(0xFF0E1015), true),
    BackgroundToneOption("midnight", "Полуночный индиго", "Midnight Indigo", "#0B132B", Color(0xFF0B132B), true),
    BackgroundToneOption("charcoal", "Графитовый сланец", "Charcoal Slate", "#181A20", Color(0xFF181A20), true),
    BackgroundToneOption("espresso", "Теплый эспрессо", "Warm Espresso", "#1A1615", Color(0xFF1A1615), true),
    BackgroundToneOption("forest", "Изумрудная чаща", "Emerald Forest", "#0A1913", Color(0xFF0A1913), true),
    BackgroundToneOption("white", "Чистый белый", "Pure White", "#FFFFFF", Color(0xFFFFFFFF), false),
    BackgroundToneOption("linen", "Светлый лен", "Light Linen", "#F8F8FA", Color(0xFFF8F8FA), false),
    BackgroundToneOption("parchment", "Теплый пергамент", "Warm Parchment", "#FFFDF7", Color(0xFFFFFDF7), false),
    BackgroundToneOption("mist", "Ледяной туман", "Ice Mist", "#F0F4F8", Color(0xFFF0F4F8), false)
)

/**
 * Sleek, self-sizing centered Color Palette Studio dialog.
 * Eliminates dead vertical space, duplicate header bars, and double-track sliders.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPaletteDialog(
    viewModel: WriterViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    val currentPrimaryHex by viewModel.customPrimaryColor.collectAsStateWithLifecycle()
    val currentBgHex by viewModel.customBgColor.collectAsStateWithLifecycle()
    val savedPalettes by viewModel.savedCustomPalettes.collectAsStateWithLifecycle()

    val defaultBgForMode = when (themeMode) {
        "LIGHT" -> "#F8F8FA"
        "BLACK" -> "#000000"
        else -> "#0E1015"
    }

    val initialPrimaryColor = remember(currentPrimaryHex) {
        parseColorOrNull(currentPrimaryHex) ?: Color(0xFFE5A93C)
    }
    val initialHsv = remember(initialPrimaryColor) {
        val hsv = colorToHsv(initialPrimaryColor)
        // If starting color is pure white/gray (saturation == 0), default hue to warm gold (38°) so spectrum isn't bright red
        if (hsv[1] < 0.02f) floatArrayOf(38f, hsv[1], hsv[2]) else hsv
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    val activeColor = remember(hue, saturation, value) {
        hsvToColor(hue, saturation, value)
    }

    var hexText by remember { mutableStateOf(colorToHex(activeColor)) }
    var selectedBgHex by remember { mutableStateOf(if (currentBgHex.isNotEmpty()) currentBgHex else defaultBgForMode) }
    var customBgInput by remember { mutableStateOf(if (currentBgHex.isNotEmpty()) currentBgHex else defaultBgForMode) }
    var isCustomBgActive by remember {
        mutableStateOf(
            currentBgHex.isNotEmpty() &&
                BACKGROUND_TONE_OPTIONS.none { it.hex.equals(currentBgHex, ignoreCase = true) }
        )
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showSaveNameDialog by remember { mutableStateOf(false) }
    var newPaletteName by remember { mutableStateOf("") }

    LaunchedEffect(activeColor) {
        val calculatedHex = colorToHex(activeColor)
        if (!hexText.equals(calculatedHex, ignoreCase = true)) {
            hexText = calculatedHex
        }
    }

    val activeBgColor = remember(selectedBgHex, isCustomBgActive, customBgInput, defaultBgForMode) {
        if (isCustomBgActive) {
            parseColorOrNull(customBgInput) ?: Color(0xFF0E1015)
        } else if (selectedBgHex.isNotEmpty()) {
            parseColorOrNull(selectedBgHex) ?: Color(0xFF0E1015)
        } else {
            parseColorOrNull(defaultBgForMode) ?: Color(0xFF0E1015)
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
                    .heightIn(max = 535.dp)
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
                    // 1. Unified Header + Live Preview Banner
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
                                MaterialYouSwatchIcon(
                                    primaryColor = activeColor,
                                    containerColor = lerp(activeBgColor, activeColor, 0.25f),
                                    surfaceColor = activeBgColor,
                                    isSelected = false,
                                    size = 28.dp
                                )
                                Column {
                                    Text(
                                        text = l("palette_studio", appLanguage),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = tr("Настройка акцента и холста", "Accent & canvas customization", appLanguage),
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
                                        viewModel.resetPaletteToDefault()
                                        onDismiss()
                                        Toast.makeText(context, l("reset_palette", appLanguage), Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(30.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = l("reset_palette", appLanguage),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                FilledTonalIconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(30.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = l("close", appLanguage),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        // Always-visible Live Mini Theme Preview Card
                        CompactLiveThemeStrip(
                            activeColor = activeColor,
                            activeBgColor = activeBgColor,
                            appLanguage = appLanguage
                        )

                        // Sleek Segmented Tab Pill Row
                        val tabs = listOf(
                            Triple(0, tr("Акцент", "Accent", appLanguage), Icons.Outlined.Palette),
                            Triple(1, tr("Фон", "Canvas", appLanguage), Icons.Outlined.FormatPaint),
                            Triple(2, tr("Темы", "Themes", appLanguage), Icons.Outlined.AutoAwesome),
                            Triple(3, tr("Мои (${savedPalettes.size})", "Saved (${savedPalettes.size})", appLanguage), Icons.Outlined.Bookmarks)
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
                                    val isSel = selectedTab == idx
                                    val tabBg by animateColorAsState(
                                        targetValue = if (isSel) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                        label = "tabBg"
                                    )
                                    val tabContent by animateColorAsState(
                                        targetValue = if (isSel) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        label = "tabContent"
                                    )

                                    Surface(
                                        onClick = { selectedTab = idx },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(30.dp),
                                        shape = RoundedCornerShape(9.dp),
                                        color = tabBg
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

                    // 2. Self-Sizing Content Area (no dead space below content!)
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        when (selectedTab) {
                            0 -> PrimaryAccentConfigTabM3(
                                activeColor = activeColor,
                                hue = hue,
                                saturation = saturation,
                                value = value,
                                hexText = hexText,
                                onHueChange = { hue = it },
                                onSaturationChange = { saturation = it },
                                onValueChange = { value = it },
                                onHsvChange = { newH, newS, newV ->
                                    hue = newH
                                    saturation = newS
                                    value = newV
                                },
                                onHexChange = { newHex ->
                                    hexText = newHex
                                    val parsed = parseColorOrNull(newHex)
                                    if (parsed != null) {
                                        val hsv = colorToHsv(parsed)
                                        if (hsv[1] > 0.01f) hue = hsv[0]
                                        saturation = hsv[1]
                                        value = hsv[2]
                                    }
                                },
                                onQuickSwatchSelected = { color ->
                                    val hsv = colorToHsv(color)
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    value = hsv[2]
                                    hexText = colorToHex(color)
                                },
                                appLanguage = appLanguage
                            )

                            1 -> BackgroundCanvasTabM3(
                                activeAccentColor = activeColor,
                                selectedBgHex = selectedBgHex,
                                isCustomBgActive = isCustomBgActive,
                                customBgInput = customBgInput,
                                onSelectPreset = { hex ->
                                    selectedBgHex = hex
                                    customBgInput = hex
                                    isCustomBgActive = false
                                },
                                onEnableCustomBg = {
                                    isCustomBgActive = !isCustomBgActive
                                },
                                onCustomBgInputChange = { newHex ->
                                    customBgInput = newHex
                                    isCustomBgActive = true
                                },
                                appLanguage = appLanguage
                            )

                            2 -> PresetsGalleryTabM3(
                                activePrimaryHex = colorToHex(activeColor),
                                activeBgHex = if (isCustomBgActive) customBgInput else selectedBgHex,
                                appLanguage = appLanguage,
                                onLoadPreset = { preset ->
                                    val parsedPrimary = parseColorOrNull(preset.primaryHex)
                                    if (parsedPrimary != null) {
                                        val hsv = colorToHsv(parsedPrimary)
                                        hue = hsv[0]
                                        saturation = hsv[1]
                                        value = hsv[2]
                                        hexText = preset.primaryHex
                                    }
                                    selectedBgHex = preset.bgHex
                                    customBgInput = preset.bgHex
                                    isCustomBgActive = false
                                    val pName = com.example.util.LocalizationManager.translateThemePresetName(preset.name, preset.nameEn, appLanguage)
                                    val msg = tr("Тема «$pName» выбрана", "Theme \"$pName\" selected", appLanguage)
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                onApplyImmediately = { preset ->
                                    viewModel.applyCustomPalette(preset.primaryHex, preset.bgHex)
                                    onDismiss()
                                    Toast.makeText(context, l("palette_applied", appLanguage), Toast.LENGTH_SHORT).show()
                                }
                            )

                            3 -> SavedPalettesTabM3(
                                savedPalettes = savedPalettes,
                                onSaveCurrent = { showSaveNameDialog = true },
                                onLoad = { item ->
                                    val parsedPrimary = parseColorOrNull(item.primaryHex)
                                    if (parsedPrimary != null) {
                                        val hsv = colorToHsv(parsedPrimary)
                                        hue = hsv[0]
                                        saturation = hsv[1]
                                        value = hsv[2]
                                        hexText = item.primaryHex
                                    }
                                    if (item.bgHex.isNotEmpty()) {
                                        selectedBgHex = item.bgHex
                                        customBgInput = item.bgHex
                                        isCustomBgActive = BACKGROUND_TONE_OPTIONS.none { it.hex.equals(item.bgHex, ignoreCase = true) }
                                    }
                                    val displayName = com.example.util.LocalizationManager.translatePaletteName(item.name, appLanguage)
                                    val msg = tr("«$displayName» загружена", "\"$displayName\" loaded", appLanguage)
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                onApplyNow = { item ->
                                    viewModel.applyCustomPalette(item.primaryHex, item.bgHex)
                                    onDismiss()
                                    Toast.makeText(context, l("palette_applied", appLanguage), Toast.LENGTH_SHORT).show()
                                },
                                onDelete = { id ->
                                    viewModel.deleteSavedCustomPalette(id)
                                    val msg = tr("Палитра удалена", "Palette deleted", appLanguage)
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                appLanguage = appLanguage
                            )
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
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = l("cancel", appLanguage),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        FilledTonalButton(
                            onClick = { showSaveNameDialog = true },
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = l("save", appLanguage),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = {
                                val finalPrimaryHex = colorToHex(activeColor)
                                val finalBgHex = if (isCustomBgActive) customBgInput else selectedBgHex
                                viewModel.applyCustomPalette(finalPrimaryHex, finalBgHex)
                                onDismiss()
                                Toast.makeText(context, l("palette_applied", appLanguage), Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = activeColor,
                                contentColor = if (isColorBright(activeColor)) Color(0xFF141414) else Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = tr("Применить", "Apply", appLanguage),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSaveNameDialog) {
        AlertDialog(
            onDismissRequest = { showSaveNameDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            icon = {
                Icon(
                    imageVector = Icons.Default.BookmarkAdd,
                    contentDescription = null,
                    tint = activeColor
                )
            },
            title = {
                Text(
                    text = l("enter_palette_name", appLanguage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = newPaletteName,
                    onValueChange = { newPaletteName = it },
                    placeholder = { Text(tr("Например: Полуночный чернильный", "e.g. Midnight Inkwell", appLanguage)) },
                    label = { Text(l("enter_palette_name", appLanguage)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newPaletteName.ifBlank {
                            com.example.util.LocalizationManager.translatePaletteName("Palette #${savedPalettes.size + 1}", appLanguage)
                        }
                        val finalPrimaryHex = colorToHex(activeColor)
                        val finalBgHex = if (isCustomBgActive) customBgInput else selectedBgHex
                        viewModel.saveAndApplyCustomPalette(name, finalPrimaryHex, finalBgHex)
                        showSaveNameDialog = false
                        newPaletteName = ""
                        onDismiss()
                        val msg = tr("«$name» сохранена и применена!", "\"$name\" saved and applied!", appLanguage)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeColor,
                        contentColor = if (isColorBright(activeColor)) Color(0xFF141414) else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(l("save", appLanguage), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveNameDialog = false }) {
                    Text(l("cancel", appLanguage))
                }
            }
        )
    }
}

/**
 * Always-live compact theme preview strip inside the header.
 * Immediately visualizes the selected Accent + Canvas without needing an expand button.
 */
@Composable
private fun CompactLiveThemeStrip(
    activeColor: Color,
    activeBgColor: Color,
    appLanguage: String
) {
    val animActiveColor by animateColorAsState(targetValue = activeColor, label = "animActiveColor")
    val animActiveBgColor by animateColorAsState(targetValue = activeBgColor, label = "animActiveBgColor")

    val isBgLight = isColorBright(animActiveBgColor)
    val previewSurface = if (isBgLight) lerp(animActiveBgColor, Color.White, 0.65f) else lerp(animActiveBgColor, Color(0xFF282B3A), 0.38f)
    val previewPrimaryContainer = lerp(previewSurface, animActiveColor, if (isBgLight) 0.18f else 0.28f)
    val previewOnPrimaryContainer = if (isBgLight) lerp(animActiveColor, Color.Black, 0.35f) else lerp(animActiveColor, Color.White, 0.45f)
    val previewOnSurface = if (isBgLight) Color(0xFF14151A) else Color(0xFFF5F4F8)
    val previewOnSurfaceVariant = if (isBgLight) Color(0xFF525665) else Color(0xFFB8B6C4)
    val previewOnPrimary = if (isColorBright(animActiveColor)) Color(0xFF141414) else Color.White

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = animActiveBgColor,
        border = BorderStroke(1.dp, animActiveColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(previewPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoStories,
                    contentDescription = null,
                    tint = previewOnPrimaryContainer,
                    modifier = Modifier.size(15.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = l("sample_book_title", appLanguage),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = previewOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${colorToHex(animActiveColor)}  •  ${colorToHex(animActiveBgColor)}",
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = previewOnSurfaceVariant,
                    maxLines = 1
                )
            }

            Surface(
                shape = RoundedCornerShape(7.dp),
                color = animActiveColor
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = previewOnPrimary,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = tr("Стиль", "Live", appLanguage),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = previewOnPrimary
                    )
                }
            }
        }
    }
}

// --- Tab 1: Primary Accent Configuration ---

@Composable
fun PrimaryAccentConfigTabM3(
    activeColor: Color,
    hue: Float,
    saturation: Float,
    value: Float,
    hexText: String,
    onHueChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onValueChange: (Float) -> Unit,
    onHsvChange: (Float, Float, Float) -> Unit,
    onHexChange: (String) -> Unit,
    onQuickSwatchSelected: (Color) -> Unit,
    appLanguage: String
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. 2D Spectrum + Subtle Tone Chips + Single-Track Sliders in one cohesive studio surface
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 2D Saturation (X) vs Brightness (Y) Canvas
                val pureHueColor = remember(hue) { hsvToColor(hue, 1f, 1f) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .pointerInput(hue) {
                            detectTapGestures { offset ->
                                val newS = (offset.x / size.width).coerceIn(0f, 1f)
                                val newV = (1f - (offset.y / size.height)).coerceIn(0.05f, 1f)
                                onHsvChange(hue, newS, newV)
                            }
                        }
                        .pointerInput(hue) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val newS = (change.position.x / size.width).coerceIn(0f, 1f)
                                val newV = (1f - (change.position.y / size.height)).coerceIn(0.05f, 1f)
                                onHsvChange(hue, newS, newV)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(brush = Brush.horizontalGradient(colors = listOf(Color.White, pureHueColor)))
                        drawRect(brush = Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black)))

                        val thumbX = (saturation.coerceIn(0f, 1f) * size.width).coerceIn(8.dp.toPx(), size.width - 8.dp.toPx())
                        val thumbY = ((1f - value.coerceIn(0f, 1f)) * size.height).coerceIn(8.dp.toPx(), size.height - 8.dp.toPx())
                        val center = Offset(thumbX, thumbY)

                        drawCircle(
                            color = Color.Black.copy(alpha = 0.45f),
                            radius = 7.5.dp.toPx(),
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawCircle(
                            color = activeColor,
                            radius = 4.5.dp.toPx(),
                            center = center
                        )
                    }
                }

                // Refined Tone Chips (Tonal background + colored indicator dot instead of loud solid blocks)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val harmonies = listOf(
                        Triple(tr("Пастель", "Pastel", appLanguage), 0.38f, 0.98f),
                        Triple(tr("Мягкий", "Soft", appLanguage), 0.60f, 0.94f),
                        Triple(tr("Яркий", "Vivid", appLanguage), 0.84f, 0.96f),
                        Triple(tr("Неон", "Neon", appLanguage), 1.0f, 1.0f),
                        Triple(tr("Глубокий", "Deep", appLanguage), 0.88f, 0.72f)
                    )
                    harmonies.forEach { (hLabel, hSat, hVal) ->
                        val hColor = hsvToColor(hue, hSat, hVal)
                        val isNear = kotlin.math.abs(saturation - hSat) < 0.06f && kotlin.math.abs(value - hVal) < 0.06f
                        Surface(
                            onClick = { onHsvChange(hue, hSat, hVal) },
                            modifier = Modifier
                                .weight(1f)
                                .height(25.dp),
                            shape = RoundedCornerShape(7.dp),
                            color = if (isNear) lerp(MaterialTheme.colorScheme.surfaceContainerHighest, hColor, 0.28f)
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(
                                width = if (isNear) 1.2.dp else 0.6.dp,
                                color = if (isNear) hColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(hColor)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = hLabel,
                                    fontSize = 8.5.sp,
                                    fontWeight = if (isNear) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                // Single-Track Custom Canvas Sliders (No broken double tracks!)
                SingleTrackGradientSlider(
                    label = l("hue", appLanguage),
                    valueText = "${hue.toInt()}°",
                    value = hue,
                    valueRange = 0f..360f,
                    gradientColors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
                    thumbColor = pureHueColor,
                    onValueChange = onHueChange
                )

                SingleTrackGradientSlider(
                    label = l("saturation", appLanguage),
                    valueText = "${(saturation * 100).toInt()}%",
                    value = saturation,
                    valueRange = 0f..1f,
                    gradientColors = listOf(hsvToColor(hue, 0f, value), hsvToColor(hue, 1f, value)),
                    thumbColor = activeColor,
                    onValueChange = onSaturationChange
                )

                SingleTrackGradientSlider(
                    label = l("brightness", appLanguage),
                    valueText = "${(value * 100).toInt()}%",
                    value = value,
                    valueRange = 0.08f..1f,
                    gradientColors = listOf(Color.Black, hsvToColor(hue, saturation, 1f)),
                    thumbColor = activeColor,
                    onValueChange = onValueChange
                )
            }
        }

        // 2. Compact Quick Swatches + Sleek HEX Bar in one card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = l("quick_swatches", appLanguage),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "HSV ${hue.toInt()}° • ${(saturation * 100).toInt()}% • ${(value * 100).toInt()}%",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 2 rows of 8 proportional 26dp swatches
                CURATED_QUICK_SWATCHES.take(16).chunked(8).forEach { rowSwatches ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowSwatches.forEach { swatch ->
                            val isSelected = colorToHex(swatch).equals(hexText, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(27.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.8.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Black.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    )
                                    .clickable { onQuickSwatchSelected(swatch) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (isColorBright(swatch)) Color.Black else Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                // Sleek 34dp HEX Input Row (replaces the oversized 58dp OutlinedTextField)
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
                            .border(0.8.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Colorize,
                            contentDescription = null,
                            tint = if (isColorBright(activeColor)) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "HEX",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            BasicTextField(
                                value = hexText,
                                onValueChange = onHexChange,
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                textStyle = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    imeAction = ImeAction.Done
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    FilledTonalIconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(hexText))
                            val msg = tr("$hexText скопирован", "$hexText copied", appLanguage)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy HEX", modifier = Modifier.size(14.dp))
                    }

                    FilledTonalIconButton(
                        onClick = {
                            val clip = clipboardManager.getText()?.text?.trim() ?: ""
                            val parsed = parseColorOrNull(clip)
                            if (parsed != null) {
                                onHexChange(colorToHex(parsed))
                            } else {
                                val msg = tr("Буфер не содержит HEX-код", "Clipboard does not contain a valid HEX color", appLanguage)
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste HEX", modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

/**
 * Clean single-track gradient slider drawn with Canvas.
 * Replaces the broken double-bar (thin Box + thick M3 Slider) from the screenshot.
 */
@Composable
fun SingleTrackGradientSlider(
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
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
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

// --- Tab 2: Background & Canvas Configuration ---

@Composable
fun BackgroundCanvasTabM3(
    activeAccentColor: Color,
    selectedBgHex: String,
    isCustomBgActive: Boolean,
    customBgInput: String,
    onSelectPreset: (String) -> Unit,
    onEnableCustomBg: () -> Unit,
    onCustomBgInputChange: (String) -> Unit,
    appLanguage: String
) {
    val darkOptions = remember { BACKGROUND_TONE_OPTIONS.filter { it.isDark } }
    val lightOptions = remember { BACKGROUND_TONE_OPTIONS.filter { !it.isDark } }

    val customParsedBg = remember(customBgInput) { parseColorOrNull(customBgInput) ?: Color(0xFF0E1015) }
    val customBgHsv = remember(customParsedBg) { colorToHsv(customParsedBg) }
    var bgHue by remember(customParsedBg) { mutableFloatStateOf(customBgHsv[0]) }
    var bgSat by remember(customParsedBg) { mutableFloatStateOf(customBgHsv[1]) }
    var bgVal by remember(customParsedBg) { mutableFloatStateOf(customBgHsv[2]) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = tr("Тёмные и OLED покрытия", "Dark & OLED Surfaces", appLanguage),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        darkOptions.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { item ->
                    val isSelected = !isCustomBgActive && selectedBgHex.equals(item.hex, ignoreCase = true)
                    CanvasToneCard(
                        item = item,
                        activeAccentColor = activeAccentColor,
                        isSelected = isSelected,
                        appLanguage = appLanguage,
                        onClick = { onSelectPreset(item.hex) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = tr("Светлые и бумажные покрытия", "Light & Paper Surfaces", appLanguage),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        lightOptions.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { item ->
                    val isSelected = !isCustomBgActive && selectedBgHex.equals(item.hex, ignoreCase = true)
                    CanvasToneCard(
                        item = item,
                        activeAccentColor = activeAccentColor,
                        isSelected = isSelected,
                        appLanguage = appLanguage,
                        onClick = { onSelectPreset(item.hex) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Custom Background Tone Builder
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(
                width = if (isCustomBgActive) 1.4.dp else 0.8.dp,
                color = if (isCustomBgActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEnableCustomBg() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = l("custom_bg_option", appLanguage),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = tr("Произвольный оттенок фона", "Custom canvas tone", appLanguage),
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isCustomBgActive,
                        onCheckedChange = { onEnableCustomBg() },
                        modifier = Modifier.scale(0.75f)
                    )
                }

                SingleTrackGradientSlider(
                    label = tr("Оттенок фона", "Canvas Hue", appLanguage),
                    valueText = "${bgHue.toInt()}°",
                    value = bgHue,
                    valueRange = 0f..360f,
                    gradientColors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
                    thumbColor = customParsedBg,
                    onValueChange = { newH ->
                        bgHue = newH
                        onCustomBgInputChange(colorToHex(hsvToColor(newH, bgSat, bgVal)))
                    }
                )

                SingleTrackGradientSlider(
                    label = tr("Яркость фона", "Canvas Lightness", appLanguage),
                    valueText = "${(bgVal * 100).toInt()}%",
                    value = bgVal,
                    valueRange = 0f..1f,
                    gradientColors = listOf(Color.Black, hsvToColor(bgHue, bgSat.coerceAtLeast(0.25f), 0.5f), Color.White),
                    thumbColor = activeAccentColor,
                    onValueChange = { newV ->
                        bgVal = newV
                        if (bgSat < 0.05f && newV in 0.04f..0.94f) bgSat = 0.25f
                        onCustomBgInputChange(colorToHex(hsvToColor(bgHue, bgSat, newV)))
                    }
                )
            }
        }
    }
}

@Composable
private fun CanvasToneCard(
    item: BackgroundToneOption,
    activeAccentColor: Color,
    isSelected: Boolean,
    appLanguage: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBright = isColorBright(item.color)
    val textColor = if (isBright) Color(0xFF14151A) else Color(0xFFF5F4F8)
    val subColor = if (isBright) Color(0xFF525665) else Color(0xFFB5B4C2)

    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        color = item.color,
        border = BorderStroke(
            width = if (isSelected) 1.6.dp else 0.8.dp,
            color = if (isSelected) activeAccentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(lerp(item.color, activeAccentColor, 0.28f))
                    .border(1.dp, activeAccentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(activeAccentColor)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = com.example.util.LocalizationManager.translateBgToneName(item.id, item.name, item.nameEn, appLanguage),
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.hex,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = subColor,
                    maxLines = 1
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = activeAccentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// --- Tab 3: Presets Gallery ---

@Composable
fun PresetsGalleryTabM3(
    activePrimaryHex: String,
    activeBgHex: String,
    appLanguage: String = "ru",
    onLoadPreset: (PaletteThemePreset) -> Unit,
    onApplyImmediately: (PaletteThemePreset) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 310.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(CURATED_THEME_PRESETS, key = { it.nameEn }) { preset ->
            val primary = parseColorOrNull(preset.primaryHex) ?: Color(0xFFE5A93C)
            val bg = parseColorOrNull(preset.bgHex) ?: Color(0xFF0E1015)
            val container = lerp(bg, primary, if (isColorBright(bg)) 0.18f else 0.28f)
            val pName = com.example.util.LocalizationManager.translateThemePresetName(preset.name, preset.nameEn, appLanguage)
            val pDesc = tr(preset.description, preset.descriptionEn, appLanguage)
            val isLoaded = activePrimaryHex.equals(preset.primaryHex, ignoreCase = true) &&
                activeBgHex.equals(preset.bgHex, ignoreCase = true)

            Surface(
                onClick = { onLoadPreset(preset) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (isLoaded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(
                    width = if (isLoaded) 1.5.dp else 0.8.dp,
                    color = if (isLoaded) primary else primary.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MaterialYouSwatchIcon(
                        primaryColor = primary,
                        containerColor = container,
                        surfaceColor = bg,
                        isSelected = isLoaded,
                        size = 28.dp
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = "${preset.iconEmoji} $pName",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = pDesc,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    FilledTonalButton(
                        onClick = { onApplyImmediately(preset) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = primary,
                            contentColor = if (isColorBright(primary)) Color(0xFF141414) else Color.White
                        )
                    ) {
                        Text(
                            text = tr("Выбрать", "Use", appLanguage),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// --- Tab 4: Saved Palettes ---

@Composable
fun SavedPalettesTabM3(
    savedPalettes: List<CustomPaletteItem>,
    onSaveCurrent: () -> Unit,
    onLoad: (CustomPaletteItem) -> Unit,
    onApplyNow: (CustomPaletteItem) -> Unit,
    onDelete: (String) -> Unit,
    appLanguage: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 310.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(
            onClick = onSaveCurrent,
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = l("save_as_new", appLanguage),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (savedPalettes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = tr("Нет сохраненных палитр", "No saved palettes yet", appLanguage),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = tr("Настройте акцент и фон, затем сохраните тему.", "Customize colors and save your theme.", appLanguage),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(savedPalettes, key = { it.id }) { item ->
                    val primary = parseColorOrNull(item.primaryHex) ?: Color(0xFFE5A93C)
                    val bg = if (item.bgHex.isNotEmpty()) parseColorOrNull(item.bgHex) ?: Color(0xFF0E1015) else Color(0xFF0E1015)
                    val container = lerp(bg, primary, 0.25f)
                    val displayName = com.example.util.LocalizationManager.translatePaletteName(item.name, appLanguage)

                    Surface(
                        onClick = { onLoad(item) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(0.8.dp, primary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MaterialYouSwatchIcon(
                                primaryColor = primary,
                                containerColor = container,
                                surfaceColor = bg,
                                isSelected = false,
                                size = 26.dp
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.primaryHex} • ${item.bgHex.ifEmpty { l("default_str", appLanguage) }}",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            FilledTonalIconButton(
                                onClick = { onApplyNow(item) },
                                modifier = Modifier.size(28.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = primary,
                                    contentColor = if (isColorBright(primary)) Color(0xFF141414) else Color.White
                                )
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Apply", modifier = Modifier.size(14.dp))
                            }

                            IconButton(
                                onClick = { onDelete(item.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
