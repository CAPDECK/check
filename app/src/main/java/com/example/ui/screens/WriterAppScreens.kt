package com.example.ui.screens

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.composed
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.entity.*
import com.example.data.models.BlockStyle
import com.example.data.models.EditorBlock
import com.example.ui.viewmodel.DocumentMetrics
import com.example.ui.viewmodel.WriterViewModel
import com.example.util.ImageStorageUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

fun isRu(lang: String): Boolean = com.example.util.LocalizationManager.isRussian(lang)
fun tr(ruText: String, enText: String, lang: String): String = com.example.util.LocalizationManager.tr(ruText, enText, lang)

fun Modifier.bounceClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleState = animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "bounceScale"
    )
    this
        .graphicsLayer {
            val s = scaleState.value
            scaleX = s
            scaleY = s
        }
        .clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.foundation.LocalIndication.current,
            enabled = enabled,
            onClick = onClick
        )
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bounceCombinedClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleState = animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "bounceScale"
    )
    this
        .graphicsLayer {
            scaleX = scaleState.value
            scaleY = scaleState.value
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = androidx.compose.foundation.LocalIndication.current,
            enabled = enabled,
            onClick = onClick,
            onLongClick = onLongClick
        )
}

// --- Theme Tones ---
object WriterThemeColors {
    // Elegant Dark (Inkwell Pro Theme)
    val DarkBg = Color(0xFF11121C)
    val DarkSurface = Color(0xFF1B1C26)
    val DarkText = Color(0xFFF7F5F0)
    val PrimaryAmber = Color(0xFFD0BCFF)
    
    // Light Pure White Theme
    val LightBg = Color(0xFFFFFFFF)
    val LightSurface = Color(0xFFFFFFFF)
    val LightText = Color(0xFF111111)
    val SecondarySlate = Color(0xFF4B6584)
}

@Composable
fun WriterAppMainLayout(viewModel: WriterViewModel) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val colorPalette by viewModel.colorPalette.collectAsStateWithLifecycle()
    val customPrimaryHex by viewModel.customPrimaryColor.collectAsStateWithLifecycle()
    val customBgHex by viewModel.customBgColor.collectAsStateWithLifecycle()
    val customSurfaceHex by viewModel.customSurfaceColor.collectAsStateWithLifecycle()

    val isLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val isPinConfigured by viewModel.isPinConfigured.collectAsStateWithLifecycle()
    val selectedProj by viewModel.selectedProject.collectAsStateWithLifecycle()
    val activeConflict by viewModel.activeConflict.collectAsStateWithLifecycle()

    val isCustom = colorPalette == "CUSTOM"
    val parsedCustomPrimary = parseColorOrNull(customPrimaryHex) ?: WriterThemeColors.PrimaryAmber
    val parsedCustomBg = if (customBgHex.isNotEmpty()) parseColorOrNull(customBgHex) else null

    val targetBg = when {
        isCustom && parsedCustomBg != null -> parsedCustomBg
        themeMode == "LIGHT" -> Color(0xFFF8F9FC)
        themeMode == "BLACK" -> Color(0xFF000000)
        else -> WriterThemeColors.DarkBg
    }

    val isBgLight = isColorBright(targetBg)
    val targetTextColors = if (isBgLight) WriterThemeColors.LightText else WriterThemeColors.DarkText

    val targetPrimaryColor = when {
        isCustom -> parsedCustomPrimary
        colorPalette == "BLUE" -> if (!isBgLight) Color(0xFF8AB4F8) else Color(0xFF165EC0)
        colorPalette == "GREEN" -> if (!isBgLight) Color(0xFF81C995) else Color(0xFF0F6D2E)
        colorPalette == "ORANGE" -> if (!isBgLight) Color(0xFFFFB066) else Color(0xFFD35400)
        colorPalette == "RED" -> if (!isBgLight) Color(0xFFFF8A80) else Color(0xFF962D22)
        colorPalette == "CORAL" -> if (!isBgLight) Color(0xFFFE8B77) else Color(0xFFD85D4E)
        colorPalette == "GREY" -> if (!isBgLight) Color(0xFF94A3B8) else Color(0xFF475569)
        colorPalette == "YELLOW" -> if (!isBgLight) Color(0xFFFAD02C) else Color(0xFFB57C00)
        colorPalette == "PINK" -> if (!isBgLight) Color(0xFFFF80AC) else Color(0xFFD81B60)
        else -> if (!isBgLight) WriterThemeColors.PrimaryAmber else Color(0xFF5D3FD3)
    }

    val targetOnPrimaryColor = if (isColorBright(targetPrimaryColor)) Color(0xFF141414) else Color.White

    val targetSurface = when {
        isCustom && customSurfaceHex.isNotEmpty() -> parseColorOrNull(customSurfaceHex) ?: if (isBgLight) WriterThemeColors.LightSurface else WriterThemeColors.DarkSurface
        isCustom && parsedCustomBg != null -> {
            if (isBgLight) Color.White else Color(
                (targetBg.red * 1.25f).coerceAtMost(1f),
                (targetBg.green * 1.25f).coerceAtMost(1f),
                (targetBg.blue * 1.25f).coerceAtMost(1f),
                1f
            )
        }
        themeMode == "BLACK" -> Color(0xFF0A0A0D)
        themeMode == "LIGHT" -> Color(0xFFFFFFFF)
        else -> WriterThemeColors.DarkSurface
    }

    val targetSurfaceContainerLowest = when {
        isBgLight -> Color(0xFFFFFFFF)
        themeMode == "BLACK" -> Color(0xFF000000)
        else -> Color(0xFF0C0D14)
    }
    val targetSurfaceContainerLow = when {
        isBgLight -> Color(0xFFF2F4F8)
        themeMode == "BLACK" -> Color(0xFF101116)
        else -> Color(0xFF171822)
    }
    val targetSurfaceContainer = when {
        isBgLight -> Color(0xFFECEEF4)
        themeMode == "BLACK" -> Color(0xFF16171E)
        else -> Color(0xFF1E202C)
    }
    val targetSurfaceContainerHigh = when {
        isBgLight -> Color(0xFFE5E8F0)
        themeMode == "BLACK" -> Color(0xFF1F2029)
        else -> Color(0xFF262837)
    }
    val targetSurfaceContainerHighest = when {
        isBgLight -> Color(0xFFDEE2EC)
        themeMode == "BLACK" -> Color(0xFF282A35)
        else -> Color(0xFF303345)
    }

    val colorAnimSpec = spring<Color>(stiffness = Spring.StiffnessMediumLow)
    val currentBg by animateColorAsState(targetBg, colorAnimSpec, label = "bgColor")
    val currentSurface by animateColorAsState(targetSurface, colorAnimSpec, label = "surfaceColor")
    val activePrimaryColor by animateColorAsState(targetPrimaryColor, colorAnimSpec, label = "primaryColor")
    val dynamicOnPrimaryColor by animateColorAsState(targetOnPrimaryColor, colorAnimSpec, label = "onPrimaryColor")
    val currentTextColors by animateColorAsState(targetTextColors, colorAnimSpec, label = "textColor")
    val surfaceContainerLowest by animateColorAsState(targetSurfaceContainerLowest, colorAnimSpec, label = "scLowest")
    val surfaceContainerLow by animateColorAsState(targetSurfaceContainerLow, colorAnimSpec, label = "scLow")
    val surfaceContainer by animateColorAsState(targetSurfaceContainer, colorAnimSpec, label = "sc")
    val surfaceContainerHigh by animateColorAsState(targetSurfaceContainerHigh, colorAnimSpec, label = "scHigh")
    val surfaceContainerHighest by animateColorAsState(targetSurfaceContainerHighest, colorAnimSpec, label = "scHighest")

    val opaquePrimaryContainer = if (!isBgLight) {
        lerp(currentSurface, activePrimaryColor, 0.24f)
    } else {
        lerp(currentSurface, activePrimaryColor, 0.15f)
    }
    val opaqueOnPrimaryContainer = if (!isBgLight) {
        lerp(activePrimaryColor, Color.White, 0.38f)
    } else {
        lerp(activePrimaryColor, Color.Black, 0.35f)
    }
    val opaqueSecondaryContainer = if (!isBgLight) {
        lerp(surfaceContainerHigh, activePrimaryColor, 0.22f)
    } else {
        lerp(surfaceContainerHigh, activePrimaryColor, 0.16f)
    }
    val opaqueOnSecondaryContainer = if (!isBgLight) {
        lerp(activePrimaryColor, Color.White, 0.45f)
    } else {
        lerp(activePrimaryColor, Color.Black, 0.35f)
    }
    val opaqueTertiaryContainer = if (!isBgLight) {
        lerp(currentSurface, activePrimaryColor, 0.18f)
    } else {
        lerp(currentSurface, activePrimaryColor, 0.12f)
    }

    MaterialTheme(
        colorScheme = if (!isBgLight) {
            darkColorScheme(
                primary = activePrimaryColor,
                onPrimary = dynamicOnPrimaryColor,
                background = currentBg,
                onBackground = currentTextColors,
                surface = currentSurface,
                onSurface = currentTextColors,
                primaryContainer = opaquePrimaryContainer,
                onPrimaryContainer = opaqueOnPrimaryContainer,
                secondary = activePrimaryColor,
                onSecondary = dynamicOnPrimaryColor,
                secondaryContainer = opaqueSecondaryContainer,
                onSecondaryContainer = opaqueOnSecondaryContainer,
                tertiary = activePrimaryColor,
                tertiaryContainer = opaqueTertiaryContainer,
                onTertiaryContainer = opaqueOnPrimaryContainer,
                surfaceVariant = surfaceContainerHigh,
                onSurfaceVariant = Color(0xFFC8C5D0),
                surfaceContainerLowest = surfaceContainerLowest,
                surfaceContainerLow = surfaceContainerLow,
                surfaceContainer = surfaceContainer,
                surfaceContainerHigh = surfaceContainerHigh,
                surfaceContainerHighest = surfaceContainerHighest,
                outline = if (themeMode == "BLACK") Color(0xFF383942) else Color(0xFF49454F),
                outlineVariant = if (themeMode == "BLACK") Color(0xFF24252C) else Color(0xFF323442)
            )
        } else {
            lightColorScheme(
                primary = activePrimaryColor,
                onPrimary = dynamicOnPrimaryColor,
                background = currentBg,
                surface = currentSurface,
                onBackground = currentTextColors,
                onSurface = currentTextColors,
                primaryContainer = opaquePrimaryContainer,
                onPrimaryContainer = opaqueOnPrimaryContainer,
                secondary = activePrimaryColor,
                onSecondary = dynamicOnPrimaryColor,
                secondaryContainer = opaqueSecondaryContainer,
                onSecondaryContainer = opaqueOnSecondaryContainer,
                tertiary = activePrimaryColor,
                tertiaryContainer = opaqueTertiaryContainer,
                onTertiaryContainer = opaqueOnPrimaryContainer,
                surfaceVariant = Color(0xFFE8ECF4),
                onSurfaceVariant = Color(0xFF4E5362),
                surfaceContainerLowest = surfaceContainerLowest,
                surfaceContainerLow = surfaceContainerLow,
                surfaceContainer = surfaceContainer,
                surfaceContainerHigh = surfaceContainerHigh,
                surfaceContainerHighest = surfaceContainerHighest,
                outline = Color(0xFF757986),
                outlineVariant = Color(0xFFD5DAE4)
            )
        },
        typography = com.example.ui.theme.Typography
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = currentBg
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLocked && isPinConfigured) {
                    AppLockScreen(viewModel)
                } else {
                    NavigationScaffold(viewModel, themeMode, onThemeChange = { viewModel.setThemeMode(it) })
                }

                activeConflict?.let { conflict ->
                    AlertDialog(
                        onDismissRequest = { /* Modal: force resolution selection */ },
                        title = {
                            Text(
                                text = tr("Конфликт синхронизации", "Sync Conflict", viewModel.appLanguage.value),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = tr(
                                        "Проект \"${conflict.localTitle}\" был изменен локально и в облаке с момента последней синхронизации. Пожалуйста, выберите действие:",
                                        "The project \"${conflict.localTitle}\" was modified both locally and in the cloud since last sync. Please choose how to resolve:",
                                        viewModel.appLanguage.value
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tr(
                                        "• Локальная версия: ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(conflict.localUpdatedAt))}",
                                        "• Local version: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(conflict.localUpdatedAt))}",
                                        viewModel.appLanguage.value
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = tr(
                                        "• Облачная версия: ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(conflict.remoteModifiedTime))}",
                                        "• Cloud version: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(conflict.remoteModifiedTime))}",
                                        viewModel.appLanguage.value
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        confirmButton = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.resolveConflictWithLocal(conflict, context) },
                                    modifier = Modifier.fillMaxWidth().testTag("resolve_keep_local_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = activePrimaryColor)
                                ) {
                                    Text(
                                        text = tr("Оставить локальную версию", "Keep Local Version", viewModel.appLanguage.value),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                Button(
                                    onClick = { viewModel.resolveConflictWithCloud(conflict, context) },
                                    modifier = Modifier.fillMaxWidth().testTag("resolve_keep_cloud_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = activePrimaryColor)
                                ) {
                                    Text(
                                        text = tr("Оставить облачную версию", "Keep Cloud Version", viewModel.appLanguage.value),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                Button(
                                    onClick = { viewModel.resolveConflictWithMerge(conflict, context) },
                                    modifier = Modifier.fillMaxWidth().testTag("resolve_merge_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text(
                                        text = tr("Создать объединенную копию", "Create Merged Copy", viewModel.appLanguage.value),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        },
                        dismissButton = null,
                        modifier = Modifier.testTag("sync_conflict_dialog")
                    )
                }
            }
        }
    }
}

// --- Lock Screen ---
@Composable
fun AppLockScreen(viewModel: WriterViewModel) {
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    var enteredPin by remember { mutableStateOf("") }
    val shakeAnim = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(enteredPin) {
        if (enteredPin.length == 4) {
            val success = viewModel.verifyAppPin(enteredPin)
            if (!success) {
                shakeAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 380
                        -14f at 40
                        14f at 90
                        -10f at 140
                        10f at 190
                        -6f at 240
                        6f at 290
                        -2f at 340
                        0f at 380
                    }
                )
                enteredPin = ""
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(84.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "App Locked",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = tr("Вход в Writer's Studio", "Welcome to Writer's Studio", appLanguage),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tr("Введите 4-значный PIN-код защиты", "Enter 4-digit security PIN", appLanguage),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(28.dp))

        // Dots indicating length
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .offset(x = shakeAnim.value.dp)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..4) {
                val filled = enteredPin.length >= i
                val dotColor by animateColorAsState(
                    targetValue = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "pinDotColor_$i"
                )
                val dotScale by animateFloatAsState(
                    targetValue = if (filled) 1.25f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "pinDotScale_$i"
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer {
                            scaleX = dotScale
                            scaleY = dotScale
                        }
                        .clip(CircleShape)
                        .background(dotColor)
                        .border(1.dp, if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Material 3 Numpad
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "⌫")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (row in keys) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (key in row) {
                        val isActionKey = key in listOf("C", "⌫")
                        Button(
                            onClick = {
                                when (key) {
                                    "C" -> enteredPin = ""
                                    "⌫" -> if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                    else -> if (enteredPin.length < 4) enteredPin += key
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isActionKey) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = if (isActionKey) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(72.dp)
                                .bounceClickable {
                                    when (key) {
                                        "C" -> enteredPin = ""
                                        "⌫" -> if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                        else -> if (enteredPin.length < 4) enteredPin += key
                                    }
                                }
                                .testTag("pin_key_$key")
                        ) {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isActionKey) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Navigation Controller Layout ---
@Composable
fun NavigationScaffold(
    viewModel: WriterViewModel,
    currTheme: String,
    onThemeChange: (String) -> Unit
) {
    val tabStack = remember { mutableStateListOf("PROJECTS") }
    val activeTab = tabStack.lastOrNull() ?: "PROJECTS"
    
    val selectedProj by viewModel.selectedProject.collectAsStateWithLifecycle()
    val activeDoc by viewModel.activeDocument.collectAsStateWithLifecycle()
    val isPrompterMode = remember { mutableStateOf(false) }
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val bottomBarStyle by viewModel.bottomBarStyle.collectAsStateWithLifecycle()
    val activeFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
    val folders by viewModel.currentFolders.collectAsStateWithLifecycle()

    fun selectTab(tab: String) {
        if (tabStack.lastOrNull() != tab) {
            tabStack.remove(tab)
            tabStack.add(tab)
        }
    }

    // Dynamic clean BackHandler routing
    if (isPrompterMode.value && activeDoc != null) {
        BackHandler {
            isPrompterMode.value = false
        }
    } else if (activeDoc != null) {
        BackHandler {
            viewModel.selectDocument(null)
        }
    } else if (selectedProj != null) {
        BackHandler {
            if (activeFolder != null) {
                viewModel.navigateBackFolder()
            } else {
                viewModel.selectProject(null)
            }
        }
    } else {
        BackHandler(enabled = tabStack.size > 1) {
            tabStack.removeAt(tabStack.lastIndex)
        }
    }

    val screenHierarchy = when {
        isPrompterMode.value && activeDoc != null -> 4
        activeDoc != null -> 3
        selectedProj != null -> 2
        else -> 1
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = screenHierarchy,
            transitionSpec = {
                val smoothCurve = androidx.compose.animation.core.FastOutSlowInEasing
                val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(240, easing = smoothCurve)
                val fadeEnter = fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing))
                val fadeExit = fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing))
                if (targetState > initialState) {
                    (slideInHorizontally(animationSpec = slideSpec, initialOffsetX = { (it * 0.25f).toInt() }) + fadeEnter)
                        .togetherWith(slideOutHorizontally(animationSpec = slideSpec, targetOffsetX = { (-it * 0.15f).toInt() }) + fadeExit)
                } else {
                    (slideInHorizontally(animationSpec = slideSpec, initialOffsetX = { (-it * 0.20f).toInt() }) + fadeEnter)
                        .togetherWith(slideOutHorizontally(animationSpec = slideSpec, targetOffsetX = { (it * 0.25f).toInt() }) + fadeExit)
                }
            },
            label = "ScreenTransition"
        ) { targetHierarchy ->
            when (targetHierarchy) {
                4 -> {
                    PrompterScreen(viewModel = viewModel, onClose = { isPrompterMode.value = false })
                }
                3 -> {
                    DocumentEditorScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.selectDocument(null) },
                        onLaunchPrompter = { isPrompterMode.value = true }
                    )
                }
                2 -> {
                    ProjectWorkspaceScreen(viewModel = viewModel, onBack = { viewModel.selectProject(null) })
                }
                else -> {
                    val tabs = listOf(
                        Triple("PROJECTS", Icons.Outlined.Book, l("cabinet", appLanguage)),
                        Triple("STATS", Icons.Outlined.BarChart, l("progress", appLanguage)),
                        Triple("SETTINGS", Icons.Outlined.Settings, l("options", appLanguage))
                    )

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isExpandedScreen = maxWidth >= 600.dp

                        Scaffold(
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            bottomBar = {
                                if (!isExpandedScreen) {
                                    AnimatedContent(
                                        targetState = bottomBarStyle,
                                        transitionSpec = {
                                            val smoothCurve = androidx.compose.animation.core.FastOutSlowInEasing
                                            val enterAnim = slideInVertically(
                                                animationSpec = tween(320, easing = smoothCurve),
                                                initialOffsetY = { fullHeight -> fullHeight }
                                            ) + fadeIn(animationSpec = tween(260, easing = smoothCurve))

                                            val exitAnim = slideOutVertically(
                                                animationSpec = tween(280, easing = smoothCurve),
                                                targetOffsetY = { fullHeight -> fullHeight }
                                            ) + fadeOut(animationSpec = tween(200, easing = smoothCurve))

                                            enterAnim.togetherWith(exitAnim)
                                        },
                                        label = "BottomBarStyleTransition"
                                    ) { targetStyle ->
                                        when (targetStyle) {
                                            "CAPSULE" -> {
                                                // Floating Rounded Capsule Bar (M3 Surface Container High)
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Surface(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(32.dp)),
                                                        shape = RoundedCornerShape(32.dp),
                                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                        tonalElevation = 3.dp,
                                                        shadowElevation = 6.dp,
                                                        border = BorderStroke(
                                                            1.dp,
                                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                                        )
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                                            horizontalArrangement = Arrangement.SpaceAround,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            tabs.forEach { (tabKey, icon, label) ->
                                                                val isSelected = activeTab == tabKey
                                                                Surface(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .clip(RoundedCornerShape(24.dp))
                                                                        .bounceClickable { selectTab(tabKey) }
                                                                        .testTag("nav_tab_${tabKey.lowercase()}"),
                                                                    shape = RoundedCornerShape(24.dp),
                                                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                                                ) {
                                                                    Row(
                                                                        modifier = Modifier
                                                                            .fillMaxWidth()
                                                                            .padding(vertical = 10.dp),
                                                                        horizontalArrangement = Arrangement.Center,
                                                                        verticalAlignment = Alignment.CenterVertically
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = icon,
                                                                            contentDescription = label,
                                                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                            modifier = Modifier.size(20.dp)
                                                                        )
                                                                        if (isSelected) {
                                                                            Spacer(modifier = Modifier.width(6.dp))
                                                                            Text(
                                                                                text = label,
                                                                                style = MaterialTheme.typography.labelMedium,
                                                                                fontWeight = FontWeight.Bold,
                                                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                                maxLines = 1
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            "SEGMENTED" -> {
                                                // Floating Segmented Discrete Capsules Bar (M3)
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        tabs.forEach { (tabKey, icon, label) ->
                                                            val isSelected = activeTab == tabKey
                                                            Surface(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clip(RoundedCornerShape(20.dp))
                                                                    .bounceClickable { selectTab(tabKey) }
                                                                    .testTag("nav_tab_${tabKey.lowercase()}"),
                                                                shape = RoundedCornerShape(20.dp),
                                                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                                        else MaterialTheme.colorScheme.surfaceContainer,
                                                                tonalElevation = if (isSelected) 4.dp else 1.dp,
                                                                shadowElevation = if (isSelected) 4.dp else 1.dp,
                                                                border = BorderStroke(
                                                                    1.dp,
                                                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                                                )
                                                            ) {
                                                                Column(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .padding(vertical = 8.dp),
                                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                                    verticalArrangement = Arrangement.Center
                                                                ) {
                                                                    Icon(
                                                                        imageVector = icon,
                                                                        contentDescription = label,
                                                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        modifier = Modifier.size(20.dp)
                                                                    )
                                                                    Spacer(modifier = Modifier.height(3.dp))
                                                                    Text(
                                                                        text = label,
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        maxLines = 1
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            else -> {
                                                // STANDARD Material 3 NavigationBar
                                                NavigationBar(
                                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                                    windowInsets = WindowInsets.navigationBars
                                                ) {
                                                    tabs.forEach { (tabKey, icon, label) ->
                                                        NavigationBarItem(
                                                            selected = activeTab == tabKey,
                                                            onClick = { selectTab(tabKey) },
                                                            icon = { Icon(icon, label) },
                                                            label = { Text(label) },
                                                            modifier = Modifier.testTag("nav_tab_${tabKey.lowercase()}")
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = innerPadding.calculateBottomPadding())
                            ) {
                                if (isExpandedScreen) {
                                    NavigationRail(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                        header = {
                                            Surface(
                                                modifier = Modifier
                                                    .padding(top = 12.dp, bottom = 8.dp)
                                                    .size(44.dp),
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Create,
                                                        contentDescription = "Writer's Studio",
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        tabs.forEach { (tabKey, icon, label) ->
                                            NavigationRailItem(
                                                selected = activeTab == tabKey,
                                                onClick = { selectTab(tabKey) },
                                                icon = { Icon(icon, label) },
                                                label = { Text(label) },
                                                modifier = Modifier
                                                    .padding(vertical = 4.dp)
                                                    .testTag("nav_tab_${tabKey.lowercase()}")
                                            )
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                    VerticalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    val tabIndex = when (activeTab) {
                                        "PROJECTS" -> 1
                                        "STATS" -> 2
                                        else -> 3
                                    }
                                    AnimatedContent(
                                        targetState = tabIndex,
                                        transitionSpec = {
                                            val smoothCurve = androidx.compose.animation.core.CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
                                            val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(300, easing = smoothCurve)
                                            val tabFadeIn = fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing))
                                            val tabFadeOut = fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
                                            if (targetState > initialState) {
                                                (slideInHorizontally(animationSpec = slideSpec, initialOffsetX = { (it * 0.12f).toInt() }) + tabFadeIn)
                                                    .togetherWith(slideOutHorizontally(animationSpec = slideSpec, targetOffsetX = { (-it * 0.12f).toInt() }) + tabFadeOut)
                                            } else {
                                                (slideInHorizontally(animationSpec = slideSpec, initialOffsetX = { (-it * 0.12f).toInt() }) + tabFadeIn)
                                                    .togetherWith(slideOutHorizontally(animationSpec = slideSpec, targetOffsetX = { (it * 0.12f).toInt() }) + tabFadeOut)
                                            }
                                        },
                                        label = "TabTransition",
                                        modifier = Modifier.fillMaxSize()
                                    ) { targetIndex ->
                                        when (targetIndex) {
                                            1 -> ProjectsDashboardScreen(viewModel)
                                            2 -> WriterStatsScreen(viewModel)
                                            else -> AppSettingsScreen(viewModel, currTheme, onThemeChange)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- PROJECTS SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
/** Subtle translucent surface for the projects home screen. */
@Composable
private fun Modifier.homeLiquidGlass(selected: Boolean = false): Modifier {
    val light = isColorBright(MaterialTheme.colorScheme.background)
    val tint = if (light) Color.White else Color(0xFFB9C9EE)
    val edge = if (light) Color.White else Color(0xFFE1E9FF)
    return this
        .clip(RoundedCornerShape(26.dp))
        .background(
            Brush.verticalGradient(
                listOf(
                    tint.copy(alpha = if (selected) 0.34f else 0.19f),
                    MaterialTheme.colorScheme.surface.copy(alpha = if (selected) 0.64f else 0.38f)
                )
            )
        )
        .border(
            1.dp,
            Brush.verticalGradient(
                listOf(edge.copy(alpha = 0.72f), edge.copy(alpha = 0.08f))
            ),
            RoundedCornerShape(26.dp)
        )
}

@Composable
fun ProjectsDashboardScreen(viewModel: WriterViewModel) {
    val activeProjs by viewModel.activeProjects.collectAsStateWithLifecycle()
    val archivedProjs by viewModel.archivedProjects.collectAsStateWithLifecycle()
    val trashProjs by viewModel.trashProjects.collectAsStateWithLifecycle()

    val searchQuery by viewModel.projectSearchQuery.collectAsStateWithLifecycle()
    val sortBy by viewModel.projectSortBy.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var currentSubTab by remember { mutableStateOf("ACTIVE") } // ACTIVE, FAVORITES, ARCHIVE, TRASH

    var isSearchPanelVisible by remember { mutableStateOf(false) }
    var searchCategoryFilter by remember { mutableStateOf("ALL") } // ALL, BOOK, SCREENPLAY, STORY, TEXT

    val unlockedIds by viewModel.unlockedProjectIds.collectAsStateWithLifecycle()
    var passwordProjectToUnlock by remember { mutableStateOf<WorkspaceProject?>(null) }
    var unlockingPassword by remember { mutableStateOf("") }
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val interfaceStyle by viewModel.interfaceStyle.collectAsStateWithLifecycle()

    var showAccountDialog by remember { mutableStateOf(false) }
    val authorName by viewModel.authorName.collectAsStateWithLifecycle()
    val authorBio by viewModel.authorBio.collectAsStateWithLifecycle()
    val authorAvatar by viewModel.authorAvatar.collectAsStateWithLifecycle()
    val statsList by viewModel.statistics.collectAsStateWithLifecycle()

    var projectToRename by remember { mutableStateOf<WorkspaceProject?>(null) }
    var longPressedProject by remember { mutableStateOf<WorkspaceProject?>(null) }
    var projectToSendToTrash by remember { mutableStateOf<WorkspaceProject?>(null) }
    var projectToDeletePermanently by remember { mutableStateOf<WorkspaceProject?>(null) }

    // Unified List Provider with category filtration
    val filteredList = remember(activeProjs, archivedProjs, trashProjs, currentSubTab, searchQuery, sortBy, searchCategoryFilter) {
        val rawList = when (currentSubTab) {
            "ARCHIVE" -> archivedProjs
            "TRASH" -> trashProjs
            "FAVORITES" -> activeProjs.filter { it.isFavorite }
            else -> activeProjs
        }
        val categoryFiltered = if (searchCategoryFilter == "ALL") rawList else {
            rawList.filter { it.type == searchCategoryFilter }
        }
        val searched = if (searchQuery.isEmpty()) categoryFiltered else {
            categoryFiltered.filter { it.title.contains(searchQuery, ignoreCase = true) || it.type.contains(searchQuery, ignoreCase = true) }
        }
        when (sortBy) {
            "NAME" -> searched.sortedBy { it.title }
            "CREATED" -> searched.sortedBy { it.createdAt }
            "UPDATED" -> searched.sortedByDescending { it.updatedAt }
            else -> searched // MANUAL (default query order: sortOrder ASC, updatedAt DESC)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
                navigationIcon = {
                    val iconVector = Icons.Outlined.AccountCircle
                    val contentDescription = "Profile Settings"
                    
                    if (interfaceStyle == "PIXEL") {
                        IconButton(
                            onClick = { showAccountDialog = true },
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(48.dp)
                                .testTag("account_icon_toggle_pixel")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (authorAvatar.isNotEmpty()) {
                                    AsyncImage(
                                        model = authorAvatar,
                                        contentDescription = contentDescription,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = contentDescription,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        IconButton(
                            onClick = { showAccountDialog = true },
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(48.dp)
                                .testTag("account_icon_toggle_classic")
                        ) {
                            if (authorAvatar.isNotEmpty()) {
                                AsyncImage(
                                    model = authorAvatar,
                                    contentDescription = contentDescription,
                                    modifier = Modifier.size(32.dp).clip(CircleShape).border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = contentDescription,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = "Feather Quill Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Writer's Studio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (interfaceStyle == "PIXEL") {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSearchPanelVisible) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .bounceClickable { isSearchPanelVisible = !isSearchPanelVisible },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = l("search", appLanguage),
                                tint = if (isSearchPanelVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        IconButton(onClick = { isSearchPanelVisible = !isSearchPanelVisible }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = l("search", appLanguage),
                                tint = if (isSearchPanelVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentSubTab == "ACTIVE" || currentSubTab == "FAVORITES") {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    icon = { Icon(Icons.Filled.Add, l("new_project", appLanguage)) },
                    text = { Text(l("new_project", appLanguage), fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.76f),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .homeLiquidGlass(selected = true)
                        .testTag("create_project_fab")
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // SEARCH & FILTER PANEL (opens when magnifier icon is clicked)
            AnimatedVisibility(
                visible = isSearchPanelVisible,
                enter = expandVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(220)),
                exit = shrinkVertically(animationSpec = tween(240, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(180))
            ) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = ShapeDefaults.ExtraLarge,
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.32f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = l("search_filter", appLanguage),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Input query
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setProjectSearchQuery(it) },
                            placeholder = { Text(l("search_placeholder", appLanguage)) },
                            leadingIcon = { Icon(Icons.Default.Search, "Search Icon", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setProjectSearchQuery("") }) {
                                        Icon(Icons.Default.Close, "Clear", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("project_search_input"),
                            singleLine = true,
                            shape = ShapeDefaults.Large
                        )

                        // Sorters selection
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(l("sort_by", appLanguage), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(76.dp))
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(
                                    "MANUAL" to tr("Порядок", "Manual", appLanguage),
                                    "UPDATED" to l("sort_changed", appLanguage),
                                    "NAME" to l("sort_name", appLanguage),
                                    "CREATED" to l("sort_created", appLanguage)
                                ).forEach { (sortId, label) ->
                                    val isSelected = sortBy == sortId
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setProjectSortBy(sortId) },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }

                        // Categories filtering UI (All, Book, Screenplay, Story, Text)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(l("category", appLanguage), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(76.dp))
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(
                                    "ALL" to l("cat_all", appLanguage),
                                    "BOOK" to l("cat_book", appLanguage),
                                    "SCREENPLAY" to l("cat_screenplay", appLanguage),
                                    "STORY" to l("cat_story", appLanguage),
                                    "TEXT" to l("cat_text", appLanguage)
                                ).forEach { (typeId, label) ->
                                    val isSelected = searchCategoryFilter == typeId
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { searchCategoryFilter = typeId },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Liquid Glass segmented project navigation.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .homeLiquidGlass()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                listOf(
                    Triple("ACTIVE", l("projects", appLanguage), Icons.Outlined.FolderOpen),
                    Triple("FAVORITES", l("favorites", appLanguage), Icons.Outlined.StarBorder),
                    Triple("ARCHIVE", l("archive", appLanguage), Icons.Outlined.Archive),
                    Triple("TRASH", l("trash", appLanguage), Icons.Outlined.Delete)
                ).forEach { (tabId, label, icon) ->
                    DashboardTabItem(
                        label = label,
                        icon = icon,
                        isSelected = currentSubTab == tabId,
                        onClick = { currentSubTab = tabId },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            AnimatedContent(
                targetState = currentSubTab,
                transitionSpec = {
                    val enterAnim = fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { (it * 0.12f).toInt() }
                    val exitAnim = fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                        slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { (-it * 0.12f).toInt() }
                    enterAnim.togetherWith(exitAnim)
                },
                label = "ProjectSubTabContentTransition",
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { targetTab ->
                val tabFilteredList = remember(filteredList, targetTab) { filteredList }
                if (tabFilteredList.isEmpty()) {
                    if (interfaceStyle == "PIXEL") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 960.dp)
                                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                            shape = RoundedCornerShape(32.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                EmptyStateIllustration()
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = l("no_projects_yet", appLanguage),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = l("create_first_project_desc", appLanguage),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "Empty",
                                    modifier = Modifier.size(72.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = l("no_projects_yet", appLanguage),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 960.dp)
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(tabFilteredList, key = { it.id }) { proj ->
                            ProjectItemRow(
                                project = proj,
                                modifier = Modifier.animateItem(),
                                onExplore = {
                                    if (proj.passwordHash.isNullOrEmpty() || unlockedIds.contains(proj.id)) {
                                        viewModel.selectProject(proj)
                                    } else {
                                        passwordProjectToUnlock = proj
                                        unlockingPassword = ""
                                    }
                                },
                                onFavoriteToggle = { viewModel.toggleFavoriteProject(proj) },
                                onArchiveToggle = { viewModel.toggleArchiveProject(proj) },
                                onTrashToggle = { projectToSendToTrash = proj },
                                onRestore = { viewModel.restoreFromTrash(proj) },
                                onDeletePermanent = { projectToDeletePermanently = proj },
                                isTrashMode = targetTab == "TRASH",
                                appLanguage = appLanguage,
                                onLongClick = { longPressedProject = proj },
                                onRenameToggle = { projectToRename = proj }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Create Project Dialog ---
    if (showCreateDialog) {
        CreateProjectDialog(
            viewModel = viewModel,
            onDismiss = { showCreateDialog = false }
        )
    }

    // --- Account Profile Dialog ---
    if (showAccountDialog) {
        AccountProfileDialog(
            viewModel = viewModel,
            onDismiss = { showAccountDialog = false }
        )
    }
    // --- Password Verification Dialog ---
    if (passwordProjectToUnlock != null) {
        AlertDialog(
            onDismissRequest = { passwordProjectToUnlock = null },
            title = { Text(l("protected_project", appLanguage)) },
            text = {
                Column {
                    Text(l("project_locked_hint", appLanguage))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = unlockingPassword,
                        onValueChange = { unlockingPassword = it },
                        label = { Text(l("password", appLanguage)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val proj = passwordProjectToUnlock ?: return@Button
                        val matched = viewModel.verifyProjectPassword(proj, unlockingPassword)
                        if (matched) {
                             viewModel.selectProject(proj)
                             passwordProjectToUnlock = null
                        } else {
                             // Incorrect visual feedback
                        }
                    }
                ) { Text(l("enter_btn", appLanguage)) }
            },
            dismissButton = {
                TextButton(onClick = { passwordProjectToUnlock = null }) { Text(l("cancel", appLanguage)) }
            }
        )
    }

    if (projectToRename != null) {
        val proj = projectToRename!!
        var newTitle by remember(proj.title) { mutableStateOf(proj.title) }
        AlertDialog(
            onDismissRequest = { projectToRename = null },
            title = {
                Text(
                    text = tr("Переименовать проект", "Rename Project", appLanguage),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = tr("Введите новое название проекта:", "Enter new project title:", appLanguage),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rename_project_title_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameProject(proj, newTitle)
                        projectToRename = null
                    },
                    enabled = newTitle.isNotBlank(),
                    modifier = Modifier.testTag("confirm_rename_project_btn")
                ) {
                    Text(l("manual_save", appLanguage))
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToRename = null }) {
                    Text(l("cancel", appLanguage))
                }
            }
        )
    }

    if (longPressedProject != null) {
        val proj = longPressedProject!!
        AlertDialog(
            onDismissRequest = { longPressedProject = null },
            title = {
                Text(
                    text = proj.title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = tr("Действия проекта:", "Project actions:", appLanguage),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    // Rename Option
                    Button(
                        onClick = {
                            projectToRename = proj
                            longPressedProject = null
                        },
                        modifier = Modifier.fillMaxWidth().testTag("long_press_action_rename"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                            Text(tr("Переименовать", "Rename", appLanguage), fontSize = 14.sp)
                        }
                    }

                    // Move Up (Переместить выше) Option
                    val currentList = filteredList
                    val index = currentList.indexOfFirst { it.id == proj.id }
                    val canMoveUp = index > 0
                    
                    Button(
                        onClick = {
                            viewModel.moveProject(proj, up = true, currentSubTab = currentSubTab)
                            longPressedProject = null
                        },
                        enabled = canMoveUp,
                        modifier = Modifier.fillMaxWidth().testTag("long_press_action_move_up"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(18.dp))
                            Text(tr("Переместить выше", "Move Up", appLanguage), fontSize = 14.sp)
                        }
                    }

                    // Move Down (Переместить ниже) Option
                    val canMoveDown = index != -1 && index < currentList.size - 1
                    
                    Button(
                        onClick = {
                            viewModel.moveProject(proj, up = false, currentSubTab = currentSubTab)
                            longPressedProject = null
                        },
                        enabled = canMoveDown,
                        modifier = Modifier.fillMaxWidth().testTag("long_press_action_move_down"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(18.dp))
                            Text(tr("Переместить ниже", "Move Down", appLanguage), fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { longPressedProject = null }) {
                    Text(l("close", appLanguage))
                }
            }
        )
    }

    if (projectToSendToTrash != null) {
        val proj = projectToSendToTrash!!
        AlertDialog(
            onDismissRequest = { projectToSendToTrash = null },
            title = {
                Text(
                    text = tr("Удалить проект?", "Delete Project?", appLanguage),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = tr(
                        "Вы действительно хотите переместить проект \"${proj.title}\" в корзину?",
                        "Are you sure you want to move the project \"${proj.title}\" to trash?",
                        appLanguage
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendToTrash(proj)
                        projectToSendToTrash = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_send_to_trash_btn")
                ) {
                    Text(l("delete", appLanguage))
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToSendToTrash = null }) {
                    Text(l("cancel", appLanguage))
                }
            }
        )
    }

    if (projectToDeletePermanently != null) {
        val proj = projectToDeletePermanently!!
        AlertDialog(
            onDismissRequest = { projectToDeletePermanently = null },
            title = {
                Text(
                    text = tr("Удалить проект навсегда?", "Delete Project Permanently?", appLanguage),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = tr(
                        "Вы действительно хотите безвозвратно удалить проект \"${proj.title}\"? Это действие нельзя отменить!",
                        "Are you sure you want to permanently delete the project \"${proj.title}\"? This action cannot be undone!",
                        appLanguage
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.permanentDeleteProject(proj)
                        projectToDeletePermanently = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_hard_delete_btn")
                ) {
                    Text(tr("Стереть", "Erase", appLanguage))
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDeletePermanently = null }) {
                    Text(l("cancel", appLanguage))
                }
            }
        )
    }
}

// --- PROJECT ROW ITEM ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectItemRow(
    project: WorkspaceProject,
    onExplore: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onArchiveToggle: () -> Unit,
    onTrashToggle: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanent: () -> Unit,
    isTrashMode: Boolean,
    appLanguage: String,
    onLongClick: () -> Unit,
    onRenameToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val projectColor = remember(project.colorHex) {
        try { Color(android.graphics.Color.parseColor(project.colorHex)) } catch (e: Exception) { Color(0xFF6200EE) }
    }

    val typeLabel = when (project.type) {
        "SCREENPLAY" -> l("cat_screenplay", appLanguage)
        "STORY" -> l("cat_story", appLanguage)
        "TEXT" -> l("cat_text", appLanguage)
        else -> l("cat_book", appLanguage)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("project_card_${project.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = ShapeDefaults.Large,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .bounceCombinedClickable(
                    onClick = onExplore,
                    onLongClick = onLongClick
                )
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left color sign
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(projectColor)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!project.passwordHash.isNullOrEmpty()) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = l("protected_project", appLanguage),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = projectColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = typeLabel.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = projectColor,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${l("modified", appLanguage)}: ${SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()).format(Date(project.updatedAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isTrashMode) {
                IconButton(onClick = onRestore, modifier = Modifier.testTag("restore_project_btn")) {
                    Icon(Icons.Outlined.Restore, l("restore", appLanguage), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDeletePermanent, modifier = Modifier.testTag("hard_delete_project_btn")) {
                    Icon(Icons.Outlined.DeleteForever, l("delete_permanent", appLanguage), tint = MaterialTheme.colorScheme.error)
                }
            } else {
                val starScale by animateFloatAsState(
                    targetValue = if (project.isFavorite) 1.25f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "favStarScale"
                )
                val starTint by animateColorAsState(
                    targetValue = if (project.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(220),
                    label = "favStarTint"
                )
                val archiveTint by animateColorAsState(
                    targetValue = if (project.isArchived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(220),
                    label = "archiveTint"
                )

                IconButton(onClick = onRenameToggle, modifier = Modifier.testTag("rename_project_btn")) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename Project",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onFavoriteToggle, modifier = Modifier.testTag("fav_project_btn")) {
                    Icon(
                        imageVector = if (project.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = l("favorites", appLanguage),
                        tint = starTint,
                        modifier = Modifier.graphicsLayer {
                            scaleX = starScale
                            scaleY = starScale
                        }
                    )
                }
                IconButton(onClick = onArchiveToggle, modifier = Modifier.testTag("archive_project_btn")) {
                    Icon(
                        imageVector = if (project.isArchived) Icons.Filled.Unarchive else Icons.Outlined.Archive,
                        contentDescription = l("archive", appLanguage),
                        tint = archiveTint
                    )
                }
                IconButton(onClick = onTrashToggle, modifier = Modifier.testTag("trash_project_btn")) {
                    Icon(Icons.Outlined.Delete, l("to_trash", appLanguage), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// --- FOLDER HIERARCHY HELPERS ---
fun getFolderFullPath(folderId: Long?, allFolders: List<Folder>): String {
    if (folderId == null) return ""
    val names = mutableListOf<String>()
    var curr = allFolders.find { it.id == folderId }
    while (curr != null) {
        names.add(0, curr.name)
        curr = curr.parentFolderId?.let { pid -> allFolders.find { it.id == pid } }
    }
    return names.joinToString(" / ")
}

fun getFolderDescendantIds(folderId: Long, allFolders: List<Folder>): Set<Long> {
    val descendants = mutableSetOf<Long>()
    val queue = mutableListOf(folderId)
    while (queue.isNotEmpty()) {
        val curr = queue.removeAt(0)
        descendants.add(curr)
        val children = allFolders.filter { it.parentFolderId == curr }.map { it.id }
        for (cid in children) {
            if (!descendants.contains(cid)) {
                queue.add(cid)
            }
        }
    }
    return descendants
}

// --- PROJECT WORKSPACE SCREEN (Folders & Documents) ---
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ProjectWorkspaceScreen(viewModel: WriterViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val project by viewModel.selectedProject.collectAsStateWithLifecycle()
    val folders by viewModel.currentFolders.collectAsStateWithLifecycle()
    val activeFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()

    val rootDocs by viewModel.currentRootDocuments.collectAsStateWithLifecycle()
    val folderDocs by viewModel.currentFolderDocuments.collectAsStateWithLifecycle()

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateDocumentDialog by remember { mutableStateOf(false) }
    var documentToDelete by remember { mutableStateOf<Document?>(null) }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    val workspaceSortBy by viewModel.workspaceSortBy.collectAsStateWithLifecycle()

    var showRenameFolderDialog by remember { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf<Folder?>(null) }
    var showRenameDocumentDialog by remember { mutableStateOf(false) }
    var documentToRename by remember { mutableStateOf<Document?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    var folderToMove by remember { mutableStateOf<Folder?>(null) }
    var documentToMove by remember { mutableStateOf<Document?>(null) }
    var quickDropDocument by remember { mutableStateOf<Document?>(null) }
    var quickDropFolder by remember { mutableStateOf<Folder?>(null) }

    val breadcrumbTrail = remember(activeFolder, folders) {
        val trail = mutableListOf<Folder>()
        var curr = activeFolder
        while (curr != null) {
            trail.add(0, curr)
            curr = curr.parentFolderId?.let { pid -> folders.find { it.id == pid } }
        }
        trail
    }

    val rawLevelFolders = remember(folders, activeFolder) {
        folders.filter { it.parentFolderId == activeFolder?.id }
    }

    val rawLevelDocuments = if (activeFolder == null) rootDocs else folderDocs

    val currentLevelFolders = remember(rawLevelFolders, workspaceSortBy) {
        when (workspaceSortBy) {
            "NAME_ASC" -> rawLevelFolders.sortedBy { it.name.lowercase() }
            "NAME_DESC" -> rawLevelFolders.sortedByDescending { it.name.lowercase() }
            "DATE_CREATED_DESC" -> rawLevelFolders.sortedByDescending { it.createdAt }
            "DATE_CREATED_ASC" -> rawLevelFolders.sortedBy { it.createdAt }
            "DATE_MODIFIED_DESC" -> rawLevelFolders.sortedByDescending { it.createdAt }
            else -> rawLevelFolders.sortedBy { it.sortOrder }
        }
    }

    val currentLevelDocuments = remember(rawLevelDocuments, workspaceSortBy) {
        when (workspaceSortBy) {
            "NAME_ASC" -> rawLevelDocuments.sortedBy { it.title.lowercase() }
            "NAME_DESC" -> rawLevelDocuments.sortedByDescending { it.title.lowercase() }
            "DATE_CREATED_DESC" -> rawLevelDocuments.sortedByDescending { it.createdAt }
            "DATE_CREATED_ASC" -> rawLevelDocuments.sortedBy { it.createdAt }
            "DATE_MODIFIED_DESC" -> rawLevelDocuments.sortedByDescending { it.updatedAt }
            else -> rawLevelDocuments.sortedBy { it.sortOrder }
        }
    }

    val projectColor = MaterialTheme.colorScheme.primary

    val projectDotColor = remember(project) {
        val hex = project?.colorHex ?: "#6200EE"
        try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            Color(0xFF6200EE)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = project?.title ?: l("project_docs", appLanguage),
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(projectDotColor)
                            )
                            val catLabel = l("cat_${(project?.type ?: "BOOK").lowercase()}", appLanguage)
                            Text(
                                text = catLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (activeFolder != null) {
                            viewModel.navigateBackFolder()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, l("close", appLanguage))
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateFolderDialog = true }) {
                        Icon(Icons.Filled.CreateNewFolder, l("create_folder_title", appLanguage), tint = projectColor)
                    }
                    IconButton(onClick = { showCreateDocumentDialog = true }, modifier = Modifier.testTag("workspace_create_doc_btn")) {
                        Icon(Icons.AutoMirrored.Filled.NoteAdd, l("create_doc_title", appLanguage), tint = projectColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            // Elegant breadcrumbs navigation bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isAtRoot = activeFolder == null
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .bounceClickable { viewModel.selectFolder(null) }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = l("project_root", appLanguage),
                            modifier = Modifier.size(16.dp),
                            tint = if (isAtRoot) projectColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = l("project_root", appLanguage),
                            fontSize = 12.sp,
                            fontWeight = if (isAtRoot) FontWeight.Bold else FontWeight.Medium,
                            color = if (isAtRoot) projectColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    breadcrumbTrail.forEachIndexed { index, crumbFolder ->
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "nav_arrow",
                            modifier = Modifier.size(16.dp).padding(horizontal = 2.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        val isLast = index == breadcrumbTrail.lastIndex
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isLast) projectColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isLast) projectColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .bounceClickable {
                                    if (!isLast) viewModel.selectFolder(crumbFolder)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isLast) Icons.Default.FolderOpen else Icons.Default.Folder,
                                    contentDescription = "Folder",
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isLast) projectColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = crumbFolder.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isLast) projectColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Quick Drop banner when in transfer mode
            AnimatedVisibility(visible = quickDropDocument != null || quickDropFolder != null) {
                val movingItemName = quickDropDocument?.title ?: quickDropFolder?.name ?: ""
                val isMovingDoc = quickDropDocument != null
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr("Режим: перекинуть в папку", "Mode: drop into folder", appLanguage),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = tr(
                                        "Выберите папку для «$movingItemName»",
                                        "Select folder for \"$movingItemName\"",
                                        appLanguage
                                    ),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = {
                                    quickDropDocument = null
                                    quickDropFolder = null
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (activeFolder != null) {
                                FilledTonalButton(
                                    onClick = {
                                        if (quickDropDocument != null) {
                                            val d = quickDropDocument!!
                                            viewModel.moveDocumentToFolder(d.id, null)
                                            val msg = tr("«${d.title}» перекинут в корень проекта", "\"${d.title}\" moved to project root", appLanguage)
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            quickDropDocument = null
                                        } else if (quickDropFolder != null) {
                                            val f = quickDropFolder!!
                                            viewModel.moveFolderToFolder(f.id, null)
                                            val msg = tr("Папка «${f.name}» перекинута в корень проекта", "Folder \"${f.name}\" moved to project root", appLanguage)
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            quickDropFolder = null
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Home, null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(tr("В корень проекта", "To Root", appLanguage), fontSize = 11.sp)
                                }
                            }
                            Button(
                                onClick = {
                                    if (quickDropDocument != null) {
                                        documentToMove = quickDropDocument
                                    } else if (quickDropFolder != null) {
                                        folderToMove = quickDropFolder
                                    }
                                    quickDropDocument = null
                                    quickDropFolder = null
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(tr("Все папки (список)...", "All folders...", appLanguage), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Sort selection row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Sort Icon",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = l("sort_by", appLanguage),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val sModes = listOf(
                        "MANUAL" to tr("Порядок", "Manual", appLanguage),
                        "NAME_ASC" to tr("А-Я", "A-Z", appLanguage),
                        "NAME_DESC" to tr("Я-А", "Z-A", appLanguage),
                        "DATE_CREATED_DESC" to tr("Новые", "Newest", appLanguage),
                        "DATE_CREATED_ASC" to tr("Старые", "Oldest", appLanguage),
                        "DATE_MODIFIED_DESC" to tr("Недавние", "Recent", appLanguage)
                    )
                    items(sModes) { (sortId, label) ->
                        val isSelected = workspaceSortBy == sortId
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setWorkspaceSortBy(sortId) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }

            if (currentLevelFolders.isEmpty() && currentLevelDocuments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Graphic Canvas empty state illustration with subtle glowing aura
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(140.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                            colors = listOf(
                                                projectColor.copy(alpha = 0.22f),
                                                projectColor.copy(alpha = 0.05f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                            androidx.compose.foundation.Canvas(modifier = Modifier.size(116.dp)) {
                                drawCircle(
                                    color = projectColor.copy(alpha = 0.15f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 1.5.dp.toPx(),
                                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                            floatArrayOf(15f, 15f), 0f
                                        )
                                    )
                                )
                            }
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 4.dp,
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, projectColor.copy(alpha = 0.35f)),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.EditNote,
                                        contentDescription = "Empty Folder",
                                        modifier = Modifier.size(38.dp),
                                        tint = projectColor
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = l("create_work_hint", appLanguage),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        Text(
                            text = tr("Создайте папки для организации ваших идей и пишите в отдельных документах!", "Structure your project layout using folders and draft documents for any specific scene!", appLanguage),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            ),
                            modifier = Modifier.padding(top = 6.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
                        )

                        // Elegant Empty-state Context Actions directly on screen
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showCreateFolderDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, projectColor.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = projectColor
                                )
                            ) {
                                Icon(Icons.Filled.CreateNewFolder, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(l("create_folder_title", appLanguage), fontSize = 11.sp, maxLines = 1)
                            }
                            
                            Button(
                                onClick = { showCreateDocumentDialog = true },
                                modifier = Modifier.weight(1f).testTag("workspace_create_doc_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = projectColor,
                                    contentColor = if (((projectColor.red * 0.299f + projectColor.green * 0.587f + projectColor.blue * 0.114f) > 0.5f)) Color.Black else Color.White
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.NoteAdd, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(l("create_doc_title", appLanguage), fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    // List Folders first
                    items(currentLevelFolders, key = { "fold_${it.id}" }) { fold ->
                        var showFolderMenu by remember { mutableStateOf(false) }
                        val isDropTargetForMovingItem = (quickDropDocument != null) || (quickDropFolder != null && quickDropFolder!!.id != fold.id && !getFolderDescendantIds(quickDropFolder!!.id, folders).contains(fold.id))

                        Card(
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .combinedClickable(
                                    onClick = {
                                        if (quickDropDocument != null) {
                                            val d = quickDropDocument!!
                                            viewModel.moveDocumentToFolder(d.id, fold.id)
                                            val msg = tr("«${d.title}» перекинут в «${fold.name}»", "\"${d.title}\" moved to \"${fold.name}\"", appLanguage)
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            quickDropDocument = null
                                        } else if (quickDropFolder != null && isDropTargetForMovingItem) {
                                            val f = quickDropFolder!!
                                            viewModel.moveFolderToFolder(f.id, fold.id)
                                            val msg = tr("Папка «${f.name}» перекинута в «${fold.name}»", "Folder \"${f.name}\" moved to \"${fold.name}\"", appLanguage)
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            quickDropFolder = null
                                        } else {
                                            viewModel.selectFolder(fold)
                                        }
                                    },
                                    onLongClick = {
                                        folderToMove = fold
                                    }
                                ),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDropTargetForMovingItem) projectColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isDropTargetForMovingItem) 2.dp else 1.dp,
                                if (isDropTargetForMovingItem) projectColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = projectColor.copy(alpha = 0.15f),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = l("folders", appLanguage),
                                                tint = projectColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = fold.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = l("project_folder", appLanguage),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (workspaceSortBy == "MANUAL") {
                                            IconButton(
                                                onClick = { viewModel.moveFolder(fold, up = true) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowUpward,
                                                    contentDescription = "Move Up",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.moveFolder(fold, up = false) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDownward,
                                                    contentDescription = "Move Down",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        // Dedicated Move Button
                                        FilledTonalIconButton(
                                            onClick = { folderToMove = fold },
                                            modifier = Modifier.size(32.dp),
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = projectColor.copy(alpha = 0.12f),
                                                contentColor = projectColor
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                                contentDescription = tr("Перекинуть папку", "Move folder", appLanguage),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Overflow Menu
                                        Box {
                                            IconButton(
                                                onClick = { showFolderMenu = true },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "More",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            DropdownMenu(
                                                expanded = showFolderMenu,
                                                onDismissRequest = { showFolderMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text(tr("Перекинуть в папку...", "Move to folder...", appLanguage)) },
                                                    leadingIcon = {
                                                        Icon(Icons.AutoMirrored.Filled.DriveFileMove, null, tint = projectColor)
                                                    },
                                                    onClick = {
                                                        showFolderMenu = false
                                                        folderToMove = fold
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(tr("Быстро перекинуть", "Quick drop mode", appLanguage)) },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.MoveToInbox, null, tint = projectColor)
                                                    },
                                                    onClick = {
                                                        showFolderMenu = false
                                                        quickDropFolder = fold
                                                        quickDropDocument = null
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(tr("Переименовать", "Rename", appLanguage)) },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Edit, null)
                                                    },
                                                    onClick = {
                                                        showFolderMenu = false
                                                        folderToRename = fold
                                                        renameInputText = fold.name
                                                        showRenameFolderDialog = true
                                                    }
                                                )
                                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                                DropdownMenuItem(
                                                    text = { Text(l("delete", appLanguage), color = MaterialTheme.colorScheme.error) },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                                    },
                                                    onClick = {
                                                        showFolderMenu = false
                                                        folderToDelete = fold
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isDropTargetForMovingItem) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = projectColor.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, projectColor),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MoveToInbox,
                                                contentDescription = null,
                                                tint = projectColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = tr("Нажмите, чтобы перекинуть в «${fold.name}»", "Tap to drop into \"${fold.name}\"", appLanguage),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = projectColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // List documents
                    items(currentLevelDocuments, key = { "doc_${it.id}" }) { doc ->
                        var showDocMenu by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .testTag("document_card_${doc.id}")
                                .clip(RoundedCornerShape(16.dp))
                                .combinedClickable(
                                    onClick = { viewModel.selectDocument(doc) },
                                    onLongClick = { documentToMove = doc }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .width(4.dp)
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                                        .background(projectColor)
                                )
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (doc.isPlainText) Icons.AutoMirrored.Filled.Notes else Icons.Default.EditNote,
                                                contentDescription = l("document", appLanguage),
                                                tint = projectColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = doc.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1.5f, fill = false),
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                            
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (doc.isPlainText) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            ) {
                                                Text(
                                                    text = if (doc.isPlainText) "Basic TXT" else "Pro Block",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (doc.isPlainText) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(2.dp))
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Schedule,
                                                contentDescription = "Time",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "${l("modified", appLanguage)}: ${SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()).format(Date(doc.updatedAt))}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                    
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (workspaceSortBy == "MANUAL") {
                                            IconButton(
                                                onClick = { viewModel.moveDocument(doc, up = true) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowUpward,
                                                    contentDescription = "Move Up",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.moveDocument(doc, up = false) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDownward,
                                                    contentDescription = "Move Down",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        // Dedicated Move Button
                                        FilledTonalIconButton(
                                            onClick = { documentToMove = doc },
                                            modifier = Modifier.size(32.dp),
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = projectColor.copy(alpha = 0.12f),
                                                contentColor = projectColor
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                                contentDescription = tr("Перекинуть документ", "Move document", appLanguage),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Overflow Menu
                                        Box {
                                            IconButton(
                                                onClick = { showDocMenu = true },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "More",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            DropdownMenu(
                                                expanded = showDocMenu,
                                                onDismissRequest = { showDocMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text(tr("Перекинуть в папку...", "Move to folder...", appLanguage)) },
                                                    leadingIcon = {
                                                        Icon(Icons.AutoMirrored.Filled.DriveFileMove, null, tint = projectColor)
                                                    },
                                                    onClick = {
                                                        showDocMenu = false
                                                        documentToMove = doc
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(tr("Быстро перекинуть", "Quick drop mode", appLanguage)) },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.MoveToInbox, null, tint = projectColor)
                                                    },
                                                    onClick = {
                                                        showDocMenu = false
                                                        quickDropDocument = doc
                                                        quickDropFolder = null
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(tr("Переименовать", "Rename", appLanguage)) },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Edit, null)
                                                    },
                                                    onClick = {
                                                        showDocMenu = false
                                                        documentToRename = doc
                                                        renameInputText = doc.title
                                                        showRenameDocumentDialog = true
                                                    }
                                                )
                                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                                DropdownMenuItem(
                                                    text = { Text(l("delete", appLanguage), color = MaterialTheme.colorScheme.error) },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                                    },
                                                    onClick = {
                                                        showDocMenu = false
                                                        documentToDelete = doc
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // CREATE FOLDER DIALOG
    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = l("create_folder_title", appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(l("folder_name", appLanguage)) },
                    modifier = Modifier.fillMaxWidth().testTag("new_folder_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.isNotEmpty()) {
                            viewModel.createFolder(folderName)
                            showCreateFolderDialog = false
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(l("create", appLanguage))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text(l("cancel", appLanguage))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // CREATE DOCUMENT DIALOG
    if (showCreateDocumentDialog) {
        val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
        var docName by remember { mutableStateOf("") }
        var isPlainText by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showCreateDocumentDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = l("create_doc_title", appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = docName,
                        onValueChange = { docName = it },
                        label = { Text(l("doc_name", appLanguage)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_document_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        text = l("select_editor_mode", appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { isPlainText = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isPlainText) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!isPlainText) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(l("type_doc", appLanguage), fontSize = 11.sp, maxLines = 1)
                        }

                        Button(
                            onClick = { isPlainText = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPlainText) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isPlainText) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(l("type_txt", appLanguage), fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    Text(
                        text = if (isPlainText) l("basic_mode_desc", appLanguage) else l("pro_mode_desc", appLanguage),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (docName.isNotEmpty()) {
                            viewModel.createDocument(docName, isPlainText)
                            showCreateDocumentDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_document_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(l("create", appLanguage))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDocumentDialog = false }) {
                    Text(l("cancel", appLanguage))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // DELETE DOCUMENT DIALOG
    if (documentToDelete != null) {
        val doc = documentToDelete!!
        AlertDialog(
            onDismissRequest = { documentToDelete = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = tr("Удалить документ?", "Delete Document?", appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            text = {
                Text(
                    text = tr(
                        "Вы действительно хотите удалить документ \"${doc.title}\"? Это действие нельзя отменить.",
                        "Are you sure you want to delete the document \"${doc.title}\"? This action cannot be undone.",
                        appLanguage
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDocument(doc.id)
                        documentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(tr("Удалить", "Delete", appLanguage))
                }
            },
            dismissButton = {
                TextButton(onClick = { documentToDelete = null }) {
                    Text(l("cancel", appLanguage))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // DELETE FOLDER DIALOG
    if (folderToDelete != null) {
        val fold = folderToDelete!!
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = tr("Удалить папку?", "Delete Folder?", appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            text = {
                Text(
                    text = tr(
                        "Вы действительно хотите удалить папку \"${fold.name}\" и все её содержимое? Это действие нельзя отменить.",
                        "Are you sure you want to delete the folder \"${fold.name}\" and all its contents? This action cannot be undone.",
                        appLanguage
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFolder(fold.id)
                        folderToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_delete_folder_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(tr("Удалить", "Delete", appLanguage))
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text(l("cancel", appLanguage))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // RENAME FOLDER DIALOG
    if (showRenameFolderDialog && folderToRename != null) {
        var folderName by remember { mutableStateOf(renameInputText) }
        AlertDialog(
            onDismissRequest = { showRenameFolderDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = tr("Переименовать папку", "Rename Folder", appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(l("folder_name", appLanguage)) },
                    modifier = Modifier.fillMaxWidth().testTag("rename_folder_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.isNotEmpty()) {
                            viewModel.renameFolder(folderToRename!!, folderName)
                            showRenameFolderDialog = false
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(tr("Сохранить", "Save", appLanguage))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameFolderDialog = false }) {
                    Text(l("cancel", appLanguage))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // RENAME DOCUMENT DIALOG
    if (showRenameDocumentDialog && documentToRename != null) {
        var docName by remember { mutableStateOf(renameInputText) }
        AlertDialog(
            onDismissRequest = { showRenameDocumentDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = tr("Переименовать файл", "Rename Document", appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                OutlinedTextField(
                    value = docName,
                    onValueChange = { docName = it },
                    label = { Text(l("doc_name", appLanguage)) },
                    modifier = Modifier.fillMaxWidth().testTag("rename_document_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (docName.isNotEmpty()) {
                            viewModel.renameDocumentInWorkspace(documentToRename!!, docName)
                            showRenameDocumentDialog = false
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(tr("Сохранить", "Save", appLanguage))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDocumentDialog = false }) {
                    Text(l("cancel", appLanguage))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // MOVE DOCUMENT DIALOG
    if (documentToMove != null) {
        MoveDocumentDialog(
            doc = documentToMove!!,
            folders = folders,
            appLanguage = appLanguage,
            projectColor = projectColor,
            onDismiss = { documentToMove = null },
            onConfirmMove = { targetId, targetName ->
                val movingDoc = documentToMove!!
                viewModel.moveDocumentToFolder(movingDoc.id, targetId)
                val msg = tr("Документ «${movingDoc.title}» перекинут в «$targetName»", "Document \"${movingDoc.title}\" moved to \"$targetName\"", appLanguage)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                documentToMove = null
            },
            onCreateNewFolder = { name ->
                viewModel.createFolder(name)
            }
        )
    }

    // MOVE FOLDER DIALOG
    if (folderToMove != null) {
        MoveFolderDialog(
            fold = folderToMove!!,
            folders = folders,
            appLanguage = appLanguage,
            projectColor = projectColor,
            onDismiss = { folderToMove = null },
            onConfirmMove = { targetId, targetName ->
                val movingFold = folderToMove!!
                viewModel.moveFolderToFolder(movingFold.id, targetId)
                val msg = tr("Папка «${movingFold.name}» перекинута в «$targetName»", "Folder \"${movingFold.name}\" moved to \"$targetName\"", appLanguage)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                folderToMove = null
            },
            onCreateNewFolder = { name ->
                viewModel.createFolder(name)
            }
        )
    }
}

// --- REUSABLE HIERARCHICAL MOVE DIALOGS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveDocumentDialog(
    doc: Document,
    folders: List<Folder>,
    appLanguage: String,
    projectColor: Color,
    onDismiss: () -> Unit,
    onConfirmMove: (targetFolderId: Long?, targetFolderName: String) -> Unit,
    onCreateNewFolder: ((name: String) -> Unit)? = null
) {
    var selectedTargetFolderId by remember { mutableStateOf<Long?>(doc.folderId) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateFolderField by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    val filteredFolders = remember(folders, searchQuery) {
        if (searchQuery.isBlank()) folders
        else folders.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(projectColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                        contentDescription = null,
                        tint = projectColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = tr("Перекинуть документ", "Move Document", appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = doc.title,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = tr("Выберите целевую папку назначения:", "Select destination folder:", appLanguage),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (folders.size > 2) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(tr("Поиск папки...", "Search folder...", appLanguage), fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Clear, null, modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                if (onCreateNewFolder != null) {
                    if (showCreateFolderField) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, projectColor.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = newFolderName,
                                    onValueChange = { newFolderName = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text(tr("Название новой папки", "New folder name", appLanguage), fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Button(
                                    onClick = {
                                        if (newFolderName.isNotBlank()) {
                                            onCreateNewFolder(newFolderName.trim())
                                            newFolderName = ""
                                            showCreateFolderField = false
                                        }
                                    },
                                    enabled = newFolderName.isNotBlank(),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(tr("Создать", "Create", appLanguage), fontSize = 11.sp)
                                }
                                IconButton(onClick = { showCreateFolderField = false }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showCreateFolderField = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CreateNewFolder, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("+ Создать новую папку", "+ Create New Folder", appLanguage), fontSize = 12.sp)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Option 1: Root level
                    item(key = "root") {
                        val isCurrentLocation = doc.folderId == null
                        val isChosen = selectedTargetFolderId == null
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .bounceClickable {
                                    selectedTargetFolderId = null
                                },
                            color = if (isChosen) projectColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                1.dp,
                                if (isChosen) projectColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = if (isChosen) projectColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tr("Корень проекта (без папки)", "Project Root (no folder)", appLanguage),
                                        fontSize = 13.sp,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isCurrentLocation) {
                                        Text(
                                            text = tr("Текущее расположение", "Current location", appLanguage),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                if (isChosen) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = projectColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Available Folders
                    items(filteredFolders, key = { "doc_target_${it.id}" }) { targetFold ->
                        val isCurrentLocation = doc.folderId == targetFold.id
                        val isChosen = selectedTargetFolderId == targetFold.id
                        val fullPath = getFolderFullPath(targetFold.id, folders)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .bounceClickable {
                                    selectedTargetFolderId = targetFold.id
                                },
                            color = if (isChosen) projectColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                1.dp,
                                if (isChosen) projectColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (isChosen) projectColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = targetFold.name,
                                        fontSize = 13.sp,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (fullPath != targetFold.name) {
                                        Text(
                                            text = "📁 $fullPath",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                    if (isCurrentLocation) {
                                        Text(
                                            text = tr("Текущее расположение", "Current location", appLanguage),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                if (isChosen) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = projectColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val isAlreadyThere = selectedTargetFolderId == doc.folderId
            Button(
                onClick = {
                    val targetId = selectedTargetFolderId
                    val targetName = if (targetId == null) {
                        l("project_root", appLanguage)
                    } else {
                        folders.find { it.id == targetId }?.name ?: ""
                    }
                    onConfirmMove(targetId, targetName)
                },
                enabled = !isAlreadyThere,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.DriveFileMove, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isAlreadyThere) {
                        tr("Уже здесь", "Already here", appLanguage)
                    } else {
                        tr("Перекинуть", "Move", appLanguage)
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Отмена", "Cancel", appLanguage))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveFolderDialog(
    fold: Folder,
    folders: List<Folder>,
    appLanguage: String,
    projectColor: Color,
    onDismiss: () -> Unit,
    onConfirmMove: (targetParentId: Long?, targetParentName: String) -> Unit,
    onCreateNewFolder: ((name: String) -> Unit)? = null
) {
    var selectedTargetParentId by remember { mutableStateOf<Long?>(fold.parentFolderId) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateFolderField by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    val invalidTargetIds = remember(fold, folders) {
        getFolderDescendantIds(fold.id, folders)
    }

    val availableFolders = remember(folders, invalidTargetIds, searchQuery) {
        val valid = folders.filter { !invalidTargetIds.contains(it.id) }
        if (searchQuery.isBlank()) valid
        else valid.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(projectColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                        contentDescription = null,
                        tint = projectColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = tr("Перекинуть папку", "Move Folder", appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = fold.name,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = tr("Выберите родительскую папку назначения:", "Select destination parent folder:", appLanguage),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (folders.size > 2) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(tr("Поиск папки...", "Search folder...", appLanguage), fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Clear, null, modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                if (onCreateNewFolder != null) {
                    if (showCreateFolderField) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, projectColor.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = newFolderName,
                                    onValueChange = { newFolderName = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text(tr("Название новой папки", "New folder name", appLanguage), fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Button(
                                    onClick = {
                                        if (newFolderName.isNotBlank()) {
                                            onCreateNewFolder(newFolderName.trim())
                                            newFolderName = ""
                                            showCreateFolderField = false
                                        }
                                    },
                                    enabled = newFolderName.isNotBlank(),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(tr("Создать", "Create", appLanguage), fontSize = 11.sp)
                                }
                                IconButton(onClick = { showCreateFolderField = false }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showCreateFolderField = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CreateNewFolder, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("+ Создать новую папку", "+ Create New Folder", appLanguage), fontSize = 12.sp)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Option 1: Root level
                    item(key = "root_parent") {
                        val isCurrentLocation = fold.parentFolderId == null
                        val isChosen = selectedTargetParentId == null
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .bounceClickable {
                                    selectedTargetParentId = null
                                },
                            color = if (isChosen) projectColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                1.dp,
                                if (isChosen) projectColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = if (isChosen) projectColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tr("Корень проекта (верхний уровень)", "Project Root (top level)", appLanguage),
                                        fontSize = 13.sp,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isCurrentLocation) {
                                        Text(
                                            text = tr("Текущее расположение", "Current location", appLanguage),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                if (isChosen) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = projectColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Available Target Folders
                    items(availableFolders, key = { "pf_${it.id}" }) { targetFold ->
                        val isCurrentLocation = fold.parentFolderId == targetFold.id
                        val isChosen = selectedTargetParentId == targetFold.id
                        val fullPath = getFolderFullPath(targetFold.id, folders)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .bounceClickable {
                                    selectedTargetParentId = targetFold.id
                                },
                            color = if (isChosen) projectColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                1.dp,
                                if (isChosen) projectColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (isChosen) projectColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = targetFold.name,
                                        fontSize = 13.sp,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (fullPath != targetFold.name) {
                                        Text(
                                            text = "📁 $fullPath",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                    if (isCurrentLocation) {
                                        Text(
                                            text = tr("Текущее расположение", "Current location", appLanguage),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                if (isChosen) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = projectColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val isAlreadyThere = selectedTargetParentId == fold.parentFolderId
            Button(
                onClick = {
                    val targetId = selectedTargetParentId
                    val targetName = if (targetId == null) {
                        l("project_root", appLanguage)
                    } else {
                        folders.find { it.id == targetId }?.name ?: ""
                    }
                    onConfirmMove(targetId, targetName)
                },
                enabled = !isAlreadyThere,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.DriveFileMove, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isAlreadyThere) {
                        tr("Уже здесь", "Already here", appLanguage)
                    } else {
                        tr("Перекинуть", "Move", appLanguage)
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Отмена", "Cancel", appLanguage))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

// --- DOCUMENT RICH TEXT EDITOR SCREEN ---
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DocumentEditorScreen(
    viewModel: WriterViewModel,
    onBack: () -> Unit,
    onLaunchPrompter: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val document by viewModel.activeDocument.collectAsStateWithLifecycle()
    val blocks by viewModel.editorBlocks.collectAsStateWithLifecycle()
    val history by viewModel.activeDocumentHistory.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val blockTemplates by viewModel.blockTemplates.collectAsStateWithLifecycle()


    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    var showGeminiAssistant by remember { mutableStateOf(false) }
    var geminiRequestType by remember { mutableStateOf("BLOCK") } // "BLOCK", "DOCUMENT"
    var geminiPromptText by remember { mutableStateOf("") }
    var geminiResultText by remember { mutableStateOf("") }
    var isGeminiGenerating by remember { mutableStateOf(false) }
    var geminiErrorMessage by remember { mutableStateOf<String?>(null) }
    var isTranslitEnabled by remember { mutableStateOf(false) }
    var isVirtualKeyboardVisible by remember { mutableStateOf(false) }
    var activeBlockIndex by remember { mutableStateOf(0) }
    val metrics = remember(blocks) { viewModel.getDocumentMetrics() }

    // Search and Replace States
    var isSearchPanelVisible by remember { mutableStateOf(false) }
    var searchQueryText by remember { mutableStateOf("") }
    var replaceQueryText by remember { mutableStateOf("") }

    // Version snapshots pane
    var isHistoryPanelVisible by remember { mutableStateOf(false) }

    // Custom tabs and reading setup
    var editorTab by remember { mutableStateOf("BLOCKS") } // "BLOCKS", "DOCUMENT"
    var isReadingMode by remember { mutableStateOf(false) }
    var readFontSizeScale by remember { mutableStateOf(16f) }
    var showAddBlockBottomSheet by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf("PDF") } // "PDF", "DOCX", "RTF", "TXT"
    var exportPageSize by remember { mutableStateOf("A4") } // "A4", "LETTER"
    var exportFontFamily by remember { mutableStateOf("SERIF") } // "SERIF", "SANS_SERIF", "MONOSPACE"
    var exportLineSpacing by remember { mutableStateOf(1.15f) } // 1.0f, 1.15f, 1.5f, 2.0f
    var exportIncludePageNumbers by remember { mutableStateOf(true) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameDocName by remember(document?.title, showRenameDialog) { mutableStateOf(document?.title ?: "") }
    var configuringCustomBlock by remember { mutableStateOf<EditorBlock?>(null) }
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var createDocName by remember { mutableStateOf("") }
    var createDocIsPlainText by remember { mutableStateOf(false) }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoreActionsBottomSheet by remember { mutableStateOf(false) }
    var showMoveDocDialog by remember { mutableStateOf(false) }
    val collapsedBlocks = remember { mutableStateOf(setOf<String>()) }
    val isKeyboardVisible = WindowInsets.isImeVisible

    // Auto-save logic on changes (3 mins interval or 3 seconds debounce on manual update types)
    LaunchedEffect(blocks) {
        if (blocks.isNotEmpty()) {
            delay(3000)
            viewModel.saveActiveDocumentImmediate()
        }
    }

    val getBlockAccentColor = { type: String ->
        when (type) {
            "scene" -> Color(0xFF2B5C8F)          // Deep Premium Blue
            "action" -> Color(0xFF455A64)         // Slate Steel Grey
            "character" -> Color(0xFF7B1FA2)      // Majestic Purple
            "dialogue" -> Color(0xFF2E7D32)       // Warm Pine Green
            "custom" -> Color(0xFFE5A93C)         // Studio Gold
            else -> Color(0xFF455A64)             // Slate Steel Grey
        }
    }

    val getBlockIcon = { type: String ->
        when (type) {
            "scene" -> Icons.Default.Movie
            "action" -> Icons.AutoMirrored.Filled.Notes
            "character" -> Icons.Default.Person
            "dialogue" -> Icons.Default.ChatBubble
            "custom" -> Icons.Default.Tune
            else -> Icons.AutoMirrored.Filled.Notes
        }
    }

    val proLazyListState = rememberLazyListState()

    LaunchedEffect(activeBlockIndex) {
        if (activeBlockIndex in 0 until blocks.size) {
            proLazyListState.animateScrollToItem(activeBlockIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = tr("Экспорт документа", "Export Document", appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // FORMAT SELECTOR
                    Text(
                        text = tr("Формат файла", "File Format", appLanguage),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val formats = listOf("PDF", "DOCX", "RTF", "TXT")
                        formats.forEach { fmt ->
                            val isSel = exportFormat == fmt
                            Button(
                                onClick = { exportFormat = fmt },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f).testTag("export_format_$fmt"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(fmt, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // PDF-Specific Layout Customizations
                    if (exportFormat == "PDF") {
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // PAGE SIZE
                        Text(
                            text = tr("Размер страницы", "Page Size", appLanguage),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val sizes = listOf("A4", "LETTER")
                            sizes.forEach { sz ->
                                val isSel = exportPageSize == sz
                                OutlinedButton(
                                    onClick = { exportPageSize = sz },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        contentColor = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.weight(1f).testTag("export_size_$sz"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (sz == "LETTER") tr("Letter (США)", "Letter (US)", appLanguage) else "A4",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // FONT PREFERENCE
                        Text(
                            text = tr("Основной шрифт документа", "Document Primary Font", appLanguage),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val fonts = listOf(
                                "SERIF" to tr("С засечками", "Serif", appLanguage),
                                "SANS_SERIF" to tr("Без засечек", "Sans-Serif", appLanguage),
                                "MONOSPACE" to tr("Моноширинный", "Monospace", appLanguage)
                            )
                            fonts.forEach { (key, label) ->
                                val isSel = exportFontFamily == key
                                OutlinedButton(
                                    onClick = { exportFontFamily = key },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        contentColor = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.weight(1f).testTag("export_font_$key"),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(label, fontSize = 11.sp)
                                }
                            }
                        }

                        // LINE SPACING MULTIPLIER
                        Text(
                            text = tr("Межстрочный интервал", "Line Spacing", appLanguage),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val spacingOptions = listOf(
                                1.0f to "1.0",
                                1.15f to "1.15",
                                1.5f to "1.5",
                                2.0f to "2.0"
                            )
                            spacingOptions.forEach { (multiplier, label) ->
                                val isSel = exportLineSpacing == multiplier
                                OutlinedButton(
                                    onClick = { exportLineSpacing = multiplier },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        contentColor = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.weight(1f).testTag("export_spacing_$label"),
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(label, fontSize = 11.sp)
                                }
                            }
                        }

                        // PAGE NUMBERS
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { exportIncludePageNumbers = !exportIncludePageNumbers }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val labelTxt = tr("Нумерация страниц", "Include Page Numbers", appLanguage)
                                val descTxt = tr("Печатать номера страниц внизу по центру", "Draw page index centered in footer", appLanguage)
                                Text(
                                    text = labelTxt,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = descTxt,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            androidx.compose.material3.Switch(
                                checked = exportIncludePageNumbers,
                                onCheckedChange = { exportIncludePageNumbers = it },
                                modifier = Modifier.testTag("export_page_numbers_toggle")
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportDialog = false
                        val file = viewModel.exportCurrentDocument(
                            context = context,
                            format = exportFormat,
                            pageSizeName = exportPageSize,
                            fontPreference = exportFontFamily,
                            lineSpacingMultiplier = exportLineSpacing,
                            includePageNumbers = exportIncludePageNumbers
                        )
                        if (file != null) {
                            try {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = when (exportFormat) {
                                        "PDF" -> "application/pdf"
                                        "DOCX" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                        "RTF" -> "application/rtf"
                                        else -> "text/plain"
                                    }
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        intent,
                                        tr("Экспортировать через", "Export via", appLanguage)
                                    )
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(
                                    context,
                                    tr("Ошибка отправки файла", "Failed to send file", appLanguage),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                context,
                                tr("Ошибка генерации файла", "Failed to generate file", appLanguage),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr("Экспорт", "Export File", appLanguage),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(l("cancel", appLanguage))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Unified Gemini Assistant Dialog for both BLOCK & DOCUMENT mode
    if (showGeminiAssistant) {
        var isBlockDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isGeminiGenerating) showGeminiAssistant = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Gemini",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (document?.isPlainText == true) {
                            tr("Gemini: Помощник по тексту", "Gemini: Text Assistant", appLanguage)
                        } else if (geminiRequestType == "BLOCK") {
                            tr("Gemini: Помощник по блокам", "Gemini: Block Assistant", appLanguage)
                        } else {
                            tr("Gemini: Анализ Сценария", "Gemini: Document Assistant", appLanguage)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val activeBlock = if (document?.isPlainText == true) blocks.getOrNull(0) else blocks.getOrNull(activeBlockIndex)
                    val blockText = activeBlock?.text ?: ""

                    if (document?.isPlainText == true) {
                        Text(
                            text = tr("Исходный текст документа:", "Original document text:", appLanguage),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 120.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = blockText.ifBlank { tr("<пусто>", "<empty>", appLanguage) },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (geminiRequestType == "BLOCK") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = tr("Выбранный блок:", "Selected block:", appLanguage),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                                val currentType = activeBlock?.type?.uppercase() ?: "---"
                                val truncatedText = if (blockText.isBlank()) {
                                    tr("<пустой>", "<empty>", appLanguage)
                                } else {
                                    if (blockText.length > 20) blockText.take(20) + "..." else blockText
                                }
                                TextButton(
                                    onClick = { isBlockDropdownExpanded = true },
                                    modifier = Modifier.testTag("gemini_block_selector_btn")
                                ) {
                                    Text(
                                        text = "#${activeBlockIndex + 1} [$currentType] $truncatedText ▾",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = isBlockDropdownExpanded,
                                    onDismissRequest = { isBlockDropdownExpanded = false },
                                    modifier = Modifier.heightIn(max = 280.dp)
                                ) {
                                    blocks.forEachIndexed { idx, b ->
                                        val itemText = b.text.ifBlank { tr("<пустой>", "<empty>", appLanguage) }
                                        val labelText = "#${idx + 1} [${b.type.uppercase()}] ${if (itemText.length > 25) itemText.take(25) + "..." else itemText}"
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = labelText,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (idx == activeBlockIndex) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (idx == activeBlockIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            onClick = {
                                                activeBlockIndex = idx
                                                isBlockDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = if (blockText.isBlank()) tr("<пусто>", "<empty>", appLanguage) else if (blockText.length > 150) blockText.take(150) + "..." else blockText,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val totalWords = metrics.words
                        Text(
                            text = tr("Целый документ ($totalWords слов):", "Active document ($totalWords words):", appLanguage),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = tr("Быстрые действия AI:", "AI Quick Actions:", appLanguage),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val promptChips = if (document?.isPlainText == true || geminiRequestType == "BLOCK") {
                            listOf(
                                Pair(
                                    tr("💡 Продолжить", "💡 Continue", appLanguage),
                                    tr("Продолжи писать этот абзац, сохранив интонацию и стиль автора.", "Continue writing this text block, preserving the author's tone and style.", appLanguage)
                                ),
                                Pair(
                                    tr("✍️ Переписать", "✍️ Rewrite", appLanguage),
                                    tr("Перепиши этот фрагмент текста более красноречиво, художественно и увлекательно.", "Rewrite this text fragment to make it more expressive, artistic, and engaging.", appLanguage)
                                ),
                                Pair(
                                    tr("📈 Расширить", "📈 Expand", appLanguage),
                                    tr("Сделай этот фрагмент текста более развернутым, добавь кинематографических деталей и красок.", "Expand this text fragment, adding cinematic detail and vivid imagery.", appLanguage)
                                ),
                                Pair(
                                    tr("📉 Сократить", "📉 Shorten", appLanguage),
                                    tr("Сократи этот фрагмент сценария до самой сути, убрав лишнюю воду.", "Shorten this script fragment to its absolute core essence, removing excess filler.", appLanguage)
                                ),
                                Pair(
                                    tr("🌐 На Английский", "🌐 To English", appLanguage),
                                    tr("Переведи этот фрагмент сценария на английский язык.", "Translate this screenplay fragment into English.", appLanguage)
                                )
                            )
                        } else {
                            listOf(
                                Pair(
                                    tr("📝 Краткий обзор", "📝 Summarize", appLanguage),
                                    tr("Сделай подробное краткое изложение этого сценария с ключевыми сюжетными линиями и конфликтом.", "Write a comprehensive summary of this script, outlining key plot lines and main conflict.", appLanguage)
                                ),
                                Pair(
                                    tr("📊 Анализ сюжета", "📊 Analyze Plot", appLanguage),
                                    tr("Проведи подробный критический разбор сюжета, темпоритма, укажи сильные места и что можно улучшить.", "Perform a detailed evaluation of plot dynamics and pacing, highlight strengths and areas for improvement.", appLanguage)
                                ),
                                Pair(
                                    tr("👥 Характеры", "👥 Characters", appLanguage),
                                    tr("Проанализируй персонажей сценария и составь психологический профиль для каждого.", "Analyze characters within the text and compile a psychological profile summary for active ones.", appLanguage)
                                ),
                                Pair(
                                    tr("👁️ План сцен", "👁️ Outline Scenes", appLanguage),
                                    tr("Сопоставь сюжет с классической схемой кинопроизводства и распиши побитный план сцен.", "Map this script against industry standard storytelling structures and outline scene beats.", appLanguage)
                                ),
                                Pair(
                                    tr("🔮 Идеи продолжения", "🔮 Predict ideas", appLanguage),
                                    tr("Предложи 3 неожиданных и креативных сюжетных поворота для дальнейшего развития истории.", "Propose 3 unexpected and highly creative plot twists for continuing this script story.", appLanguage)
                                )
                            )
                        }

                        promptChips.forEach { (label, actionPrompt) ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    geminiPromptText = actionPrompt
                                },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = geminiPromptText,
                        onValueChange = { geminiPromptText = it },
                        label = { Text(tr("Опишите запрос или выберите быстрый выше", "Write custom instructions or select preset actions", appLanguage)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (isGeminiGenerating) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = tr("Gemini анализирует...", "Gemini is writing...", appLanguage),
                                fontSize = 11.sp,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                if (geminiPromptText.isNotBlank()) {
                                    isGeminiGenerating = true
                                    geminiErrorMessage = null
                                    
                                    coroutineScope.launch {
                                        val fullPrompt = if (document?.isPlainText == true) {
                                            val text = blocks.firstOrNull()?.text ?: ""
                                            "You are an expert Hollywood writer and screenplay consultant.\n\n" +
                                            "Context - original document text:\n\"$text\"\n\n" +
                                            "Action required:\n$geminiPromptText\n\n" +
                                            "Return ONLY the updated block text, without markdown wrappers (like ```txt or ```), without intro/outro commentary. Start directly with the text."
                                        } else if (geminiRequestType == "BLOCK") {
                                            val activeBlock = blocks.getOrNull(activeBlockIndex)
                                            val text = activeBlock?.text ?: ""
                                            "You are an expert Hollywood co-writer.\n\n" +
                                            "Context - active block text:\n\"$text\"\n\n" +
                                            "Action required:\n$geminiPromptText\n\n" +
                                            "Return ONLY the updated block text, without markdown wrappers (like ```txt or ```), without intro/outro commentary. Start directly with the text."
                                        } else {
                                            val concatText = blocks.joinToString("\n\n") { b ->
                                                "[Type: ${b.type.uppercase()}]\n${b.text}"
                                            }
                                            "You are a professional script analyzer and critic.\n\n" +
                                            "Screenplay content:\n$concatText\n\n" +
                                            "User instructions / task:\n$geminiPromptText\n\n" +
                                            "Deliver a highly polished response with clear layout headings and structured bullets."
                                        }

                                        val systemInstruction = if (document?.isPlainText == true) {
                                            "You are a helpful co-writer and screenplay assistant."
                                        } else if (geminiRequestType == "BLOCK") {
                                            "You are a helpful co-writer. You respond only with revised screenplay lines."
                                        } else {
                                            "You are a skilled screenplay consultant."
                                        }

                                        val res = com.example.util.GeminiService.generateContent(fullPrompt, systemInstruction)
                                        if (res.isSuccess) {
                                            geminiResultText = res.getOrThrow()
                                        } else {
                                            val err = res.exceptionOrNull()
                                            geminiErrorMessage = err?.message ?: "Internet call failed"
                                        }
                                        isGeminiGenerating = false
                                    }
                                }
                            },
                            enabled = geminiPromptText.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("Сгенерировать в Gemini", "Process in Gemini", appLanguage))
                        }
                    }

                    if (geminiErrorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ " + (geminiErrorMessage ?: ""),
                                modifier = Modifier.padding(10.dp),
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (geminiResultText.isNotBlank()) {
                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        Text(
                            text = tr("Результат Gemini:", "Gemini response:", appLanguage),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            androidx.compose.foundation.text.selection.SelectionContainer {
                                Text(
                                    text = geminiResultText,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }

                        Text(
                            text = tr("Применить результат:", "Apply action:", appLanguage),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (document?.isPlainText == true) {
                                Button(
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    onClick = {
                                        val b = blocks.firstOrNull() ?: EditorBlock(type = "paragraph", text = "")
                                        viewModel.updateEditorBlocks(listOf(b.copy(text = geminiResultText)))
                                        showGeminiAssistant = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = tr("Заменить", "Replace", appLanguage),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }

                                Button(
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    onClick = {
                                        val b = blocks.firstOrNull() ?: EditorBlock(type = "paragraph", text = "")
                                        val updatedText = if (b.text.isEmpty()) geminiResultText else b.text + "\n\n" + geminiResultText
                                        viewModel.updateEditorBlocks(listOf(b.copy(text = updatedText)))
                                        showGeminiAssistant = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = tr("Добавить", "Append", appLanguage),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            } else if (geminiRequestType == "BLOCK") {
                                Button(
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    onClick = {
                                        val activeBlock = blocks.getOrNull(activeBlockIndex)
                                        if (activeBlock != null) {
                                            val updated = blocks.mapIndexed { idx, b ->
                                                if (idx == activeBlockIndex) b.copy(text = geminiResultText) else b
                                            }
                                            viewModel.updateEditorBlocks(updated)
                                            showGeminiAssistant = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = tr("Заменить", "Replace", appLanguage),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }

                                Button(
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    onClick = {
                                        val activeBlock = blocks.getOrNull(activeBlockIndex)
                                        if (activeBlock != null) {
                                            val list = blocks.toMutableList()
                                            val nextType = if (activeBlock.type == "character") "dialogue" else activeBlock.type
                                            val newB = EditorBlock(type = nextType, text = geminiResultText)
                                            list.add(activeBlockIndex + 1, newB)
                                            viewModel.updateEditorBlocks(list)
                                            activeBlockIndex = activeBlockIndex + 1
                                            showGeminiAssistant = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = tr("Вставить", "Insert", appLanguage),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            } else {
                                Button(
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    onClick = {
                                        val list = blocks.toMutableList()
                                        val newB = EditorBlock(type = "paragraph", text = "--- AI ANALYSIS ---\n\n$geminiResultText")
                                        list.add(newB)
                                        viewModel.updateEditorBlocks(list)
                                        activeBlockIndex = list.size - 1
                                        showGeminiAssistant = false
                                        editorTab = "BLOCKS"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = tr("В конец файла", "Append text", appLanguage),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Gemini result", geminiResultText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, tr("Скопировано!", "Copied!", appLanguage), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .size(40.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { if (!isGeminiGenerating) showGeminiAssistant = false }) {
                    Text(l("close", appLanguage))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Unified Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = tr("Переименовать документ", "Rename Document", appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                OutlinedTextField(
                    value = renameDocName,
                    onValueChange = { renameDocName = it },
                    label = { Text(tr("Новое название", "New Title", appLanguage)) },
                    modifier = Modifier.fillMaxWidth().testTag("rename_document_name_input_editor"),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameDocName.isNotBlank()) {
                            viewModel.updateDocumentTitle(renameDocName)
                            showRenameDialog = false
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(l("manual_save", appLanguage))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(l("cancel", appLanguage))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Unified Create Dialog representing cohesive file operations
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = tr("Создать документ", "Create Document", appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = createDocName,
                        onValueChange = { createDocName = it },
                        label = { Text(tr("Название документа", "Document Title", appLanguage)) },
                        modifier = Modifier.fillMaxWidth().testTag("new_document_name_input_editor"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        text = tr("Выберите режим:", "Select format:", appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { createDocIsPlainText = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!createDocIsPlainText) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!createDocIsPlainText) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(tr("Блочный (Pro)", "Block (Pro)", appLanguage), fontSize = 11.sp, maxLines = 1)
                        }
                        Button(
                            onClick = { createDocIsPlainText = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (createDocIsPlainText) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (createDocIsPlainText) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(tr("Простой (Basic)", "Text (Basic)", appLanguage), fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (createDocName.isNotBlank()) {
                            viewModel.createDocument(createDocName, createDocIsPlainText)
                            showCreateDialog = false
                            createDocName = ""
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(l("create", appLanguage))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(l("cancel", appLanguage))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Unified Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = tr("Удалить документ?", "Delete Document?", appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            text = {
                Text(
                    text = tr(
                        "Вы действительно хотите удалить документ \"${document?.title}\"? Это действие нельзя отменить.",
                        "Are you sure you want to delete the document \"${document?.title}\"? This action cannot be undone.",
                        appLanguage
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        document?.let { doc ->
                            viewModel.deleteDocument(doc.id)
                        }
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(l("delete", appLanguage))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(l("cancel", appLanguage))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showMoveDocDialog && document != null) {
        val allFolders by viewModel.currentFolders.collectAsStateWithLifecycle()
        val currentProject by viewModel.selectedProject.collectAsStateWithLifecycle()
        val projectColor = (currentProject?.colorHex?.let { parseColorOrNull(it) }) ?: MaterialTheme.colorScheme.primary
        MoveDocumentDialog(
            doc = document!!,
            folders = allFolders,
            appLanguage = appLanguage,
            projectColor = projectColor,
            onDismiss = { showMoveDocDialog = false },
            onConfirmMove = { targetId, targetName ->
                val movingDoc = document!!
                viewModel.moveDocumentToFolder(movingDoc.id, targetId)
                val msg = tr("Документ «${movingDoc.title}» перекинут в «$targetName»", "Document \"${movingDoc.title}\" moved to \"$targetName\"", appLanguage)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                showMoveDocDialog = false
            },
            onCreateNewFolder = { name ->
                viewModel.createFolder(name)
            }
        )
    }

    if (showMoreActionsBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreActionsBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 20.dp, end = 20.dp, bottom = 24.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (document?.isPlainText == true) Icons.AutoMirrored.Filled.Notes else Icons.Default.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = document?.title ?: "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (document?.isPlainText == true) {
                                tr("Формат: Простой текст (Basic)", "Format: Plain Text (Basic)", appLanguage)
                            } else {
                                tr("Формат: Блочный редактор (Pro)", "Format: Block Editor (Pro)", appLanguage)
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                val menuItems = listOf(
                    Triple(
                        tr("Перекинуть в папку", "Move to Folder", appLanguage),
                        tr("Переместить этот документ в другую папку или в корень", "Move this document into another folder or root", appLanguage),
                        Icons.AutoMirrored.Filled.DriveFileMove
                    ),
                    Triple(
                        tr("Переименовать", "Rename Document", appLanguage),
                        tr("Изменить название текущего документа", "Change the title of this document", appLanguage),
                        Icons.Default.Edit
                    ),
                    Triple(
                        tr("Создать документ", "Create Document", appLanguage),
                        tr("Создать новый пустой форматный файл", "Start a new blank basic or pro file", appLanguage),
                        Icons.Default.Add
                    ),
                    Triple(
                        tr("Экспорт в PDF", "Export to PDF", appLanguage),
                        tr("Сохранить документ в файл формата PDF", "Generate and share high-quality PDF", appLanguage),
                        Icons.Filled.PictureAsPdf
                    )
                )

                menuItems.forEach { (title, subtitle, icon) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showMoreActionsBottomSheet = false
                                when (icon) {
                                    Icons.AutoMirrored.Filled.DriveFileMove -> showMoveDocDialog = true
                                    Icons.Default.Edit -> showRenameDialog = true
                                    Icons.Default.Add -> showCreateDialog = true
                                    Icons.Filled.PictureAsPdf -> showExportDialog = true
                                }
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = subtitle,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showMoreActionsBottomSheet = false
                            showDeleteDialog = true
                        },
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = l("delete", appLanguage),
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("Удалить", "Delete Document", appLanguage),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = tr("Безвозвратно удалить файл из хранилища", "Permanently delete this document", appLanguage),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (document?.isPlainText == true) {
        // --- BASIC MODE / SIMPLE TXT WRITER ---
        val textFileContent = remember(blocks) {
            if (blocks.size > 1) {
                blocks.joinToString("\n") { it.text }
            } else {
                blocks.firstOrNull()?.text ?: ""
            }
        }

        var textFontSize by remember { mutableStateOf(16) }
        var textLineHeight by remember { mutableStateOf(24) }
        var textFontFamilyName by remember { mutableStateOf("SansSerif") }
        var isFormatSettingsVisible by remember { mutableStateOf(false) }

        val basicScrollState = rememberScrollState()
        var localTextValue by remember(document?.id) { mutableStateOf(TextFieldValue(textFileContent)) }
        if (localTextValue.text != textFileContent) {
            localTextValue = localTextValue.copy(
                text = textFileContent,
                selection = if (textFileContent.startsWith("- ") && localTextValue.text.isEmpty()) {
                    TextRange(2)
                } else {
                    TextRange(textFileContent.length)
                }
            )
        }

        val activeFontFamily = when (textFontFamilyName) {
            "Monospace" -> FontFamily.Monospace
            "Serif" -> FontFamily.Serif
            else -> FontFamily.SansSerif
        }

        val fontLabel = tr("Шрифт", "Font", appLanguage)
        val sizeLabel = tr("Размер", "Size", appLanguage)
        val spacingLabel = tr("Интервал", "Spacing", appLanguage)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = document?.title ?: "",
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.saveActiveDocumentImmediate()
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, l("close", appLanguage))
                        }
                    },
                    actions = {
                        IconButton(onClick = { isFormatSettingsVisible = !isFormatSettingsVisible }) {
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = "Font Settings",
                                tint = if (isFormatSettingsVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = {
                                geminiRequestType = "BLOCK"
                                activeBlockIndex = 0
                                geminiPromptText = ""
                                geminiResultText = ""
                                geminiErrorMessage = null
                                showGeminiAssistant = true
                            },
                            modifier = Modifier.testTag("top_gemini_assist_btn_basic")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemini AI",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { isSearchPanelVisible = !isSearchPanelVisible }) {
                            Icon(Icons.Filled.Search, l("search", appLanguage))
                        }
                        IconButton(onClick = { isHistoryPanelVisible = !isHistoryPanelVisible }) {
                            Icon(Icons.Filled.History, l("versions", appLanguage))
                        }
                        FilledIconButton(
                            onClick = onLaunchPrompter,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("editor_prompter_btn")
                                .bounceClickable { onLaunchPrompter() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = l("prompter", appLanguage),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(onClick = { showMoreActionsBottomSheet = true }, modifier = Modifier.testTag("editor_more_options_btn")) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = tr("Еще", "More options", appLanguage),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 2.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Word Count Item
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${l("words", appLanguage)}: ${metrics.words}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Char Count Item
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TextFormat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${l("chars", appLanguage)}: ${metrics.characters}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Reading Time Item
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "~${metrics.readingTimeMinutes} ${l("min", appLanguage)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            val isKeyboardVisible = WindowInsets.isImeVisible
            val bottomPadding = if (isKeyboardVisible) 0.dp else paddingValues.calculateBottomPadding()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = paddingValues.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        top = paddingValues.calculateTopPadding(),
                        end = paddingValues.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        bottom = bottomPadding
                    )
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Search panel if toggled
                    AnimatedVisibility(
                        visible = isSearchPanelVisible,
                        enter = expandVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(220)),
                        exit = shrinkVertically(animationSpec = tween(240, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(180))
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row {
                                    OutlinedTextField(
                                        value = searchQueryText,
                                        onValueChange = { searchQueryText = it },
                                        label = { Text(l("find_replace", appLanguage)) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = replaceQueryText,
                                        onValueChange = { replaceQueryText = it },
                                        label = { Text(l("chars", appLanguage)) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    TextButton(
                                        onClick = {
                                            if (searchQueryText.isNotEmpty()) {
                                                val b = blocks.firstOrNull() ?: EditorBlock(type = "paragraph", text = "")
                                                val updatedText = b.text.replace(searchQueryText, replaceQueryText, ignoreCase = true)
                                                viewModel.updateEditorBlocks(listOf(b.copy(text = updatedText)))
                                                Toast.makeText(context, tr("Текст заменен", "Text replaced", appLanguage), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) { Text("OK") }
                                }
                            }
                        }
                    }

                    // Format Settings panel if toggled
                    AnimatedVisibility(
                        visible = isFormatSettingsVisible,
                        enter = expandVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(220)),
                        exit = shrinkVertically(animationSpec = tween(240, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(180))
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Font family switcher
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = fontLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("SansSerif", "Serif", "Monospace").forEach { family ->
                                            val isSelected = textFontFamilyName == family
                                            val displayName = when (family) {
                                                "SansSerif" -> "Sans"
                                                "Serif" -> "Serif"
                                                else -> "Mono"
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                                    .bounceClickable { textFontFamilyName = family }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = displayName,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                )

                                // Font size controls
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = sizeLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (textFontSize > 12) {
                                                    textFontSize -= 2
                                                    textLineHeight = (textFontSize * 1.5).toInt()
                                                }
                                            },
                                            modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f), androidx.compose.foundation.shape.CircleShape)
                                        ) {
                                            Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }
                                        Text("${textFontSize}sp", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        IconButton(
                                            onClick = {
                                                if (textFontSize < 32) {
                                                    textFontSize += 2
                                                    textLineHeight = (textFontSize * 1.5).toInt()
                                                }
                                            },
                                            modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f), androidx.compose.foundation.shape.CircleShape)
                                        ) {
                                            Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                )

                                // Line spacing controls
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = spacingLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(1.2f, 1.5f, 1.8f).forEach { scale ->
                                            val isSelected = Math.abs(textLineHeight.toFloat() / textFontSize.toFloat() - scale) < 0.15f
                                            val labelText = when(scale) {
                                                1.2f -> tr("Узкий", "Compact", appLanguage)
                                                1.5f -> tr("Средний", "Medium", appLanguage)
                                                else -> tr("Широкий", "Wide", appLanguage)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                                    .bounceClickable { textLineHeight = (textFontSize * scale).toInt() }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = labelText,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Text Area Editor Workspace Card Sheet
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    ) {
                        var containerHeightPx by remember { mutableStateOf(0) }
                        var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                        LaunchedEffect(localTextValue.selection, textLayoutResult, containerHeightPx) {
                            val layout = textLayoutResult
                            val containerHeight = containerHeightPx
                            if (layout != null && containerHeight > 0) {
                                val cursor = localTextValue.selection.start
                                val safeCursor = cursor.coerceIn(0, layout.layoutInput.text.length)
                                val line = layout.getLineForOffset(safeCursor)
                                val lineTop = layout.getLineTop(line).toInt()
                                val lineBottom = layout.getLineBottom(line).toInt()
                                
                                val currentScroll = basicScrollState.value
                                val visibleMin = currentScroll
                                val visibleMax = currentScroll + containerHeight
                                
                                val padding = 40
                                if (lineTop - padding < visibleMin) {
                                    basicScrollState.scrollTo((lineTop - padding).coerceAtLeast(0))
                                } else if (lineBottom + padding > visibleMax) {
                                    basicScrollState.scrollTo((lineBottom + padding - containerHeight).coerceAtLeast(0))
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            BasicTextField(
                                value = localTextValue,
                                onValueChange = { newVal ->
                                    val processedVal = processTextFieldValueSmartEditor(localTextValue, newVal)
                                    localTextValue = processedVal
                                    val activeBlock = blocks.firstOrNull() ?: EditorBlock(type = "paragraph", text = "")
                                    viewModel.updateEditorBlocks(
                                        listOf(activeBlock.copy(text = processedVal.text)),
                                        updateHistory = false
                                    )
                                },
                                textStyle = TextStyle(
                                    fontSize = textFontSize.sp,
                                    fontFamily = activeFontFamily,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = textLineHeight.sp
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onGloballyPositioned { containerHeightPx = it.size.height }
                                    .verticalScroll(basicScrollState)
                                    .testTag("basic_text_editor_input"),
                                onTextLayout = { textLayoutResult = it }
                            )

                            if (textFileContent.isEmpty()) {
                                Text(
                                    text = l("empty_text", appLanguage),
                                    style = TextStyle(
                                        fontSize = textFontSize.sp,
                                        fontFamily = activeFontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                )
                            }
                        }
                    }

                    // Keyboard auxiliary toolbar
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.undo() },
                            enabled = canUndo,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                tint = if (canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.redo() },
                            enabled = canRedo,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "Redo",
                                tint = if (canRedo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        )

                        val helperSymbols = listOf("—", "«", "»", "“", "”", "•", "¶", "Tab")
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(helperSymbols) { symbol ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        .bounceClickable {
                                            val oldText = localTextValue.text
                                            val selectionStart = localTextValue.selection.start.coerceIn(0, oldText.length)
                                            val selectionEnd = localTextValue.selection.end.coerceIn(0, oldText.length)
                                            val appendText = if (symbol == "Tab") "    " else symbol
                                            val newText = oldText.substring(0, selectionStart) + appendText + oldText.substring(selectionEnd)
                                            val newSelection = TextRange(selectionStart + appendText.length)
                                            localTextValue = TextFieldValue(text = newText, selection = newSelection)

                                            val activeBlock = blocks.firstOrNull() ?: EditorBlock(type = "paragraph", text = "")
                                            viewModel.updateEditorBlocks(
                                                listOf(activeBlock.copy(text = newText)),
                                                updateHistory = true
                                            )
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = symbol,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // SNAPSHOTS PANEL in basic mode
                AnimatedVisibility(
                    visible = isHistoryPanelVisible,
                    enter = slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessLow), initialOffsetX = { it }) + fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)),
                    exit = slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessLow), targetOffsetX = { it }) + fadeOut(animationSpec = tween(260, easing = FastOutSlowInEasing)),
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(280.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        .align(Alignment.TopEnd)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(l("versions_subtitle", appLanguage), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        var snapLabel by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = snapLabel,
                            onValueChange = { snapLabel = it },
                            label = { Text(l("save_snapshot", appLanguage)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (snapLabel.isNotEmpty()) {
                                    viewModel.createManualSnapshot(snapLabel)
                                    snapLabel = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) { Text("OK") }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(l("saved_versions", appLanguage), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                            items(history, key = { it.id }) { hist ->
                                OutlinedCard(
                                    modifier = Modifier.animateItem().fillMaxWidth().bounceClickable { viewModel.restoreSnapshot(hist) },
                                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(hist.snapshotName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text(SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(hist.timestamp)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {

    // Image Picker Result launcher
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && document != null) {
            val copiedPath = ImageStorageUtil.copyImageToProject(context, uri)
            if (copiedPath != null) {
                // Insert a new Image block in the editor blocks
                val blocksList = blocks.toMutableList()
                val newBlock = EditorBlock(
                    type = "image",
                    imageUrl = copiedPath,
                    imageCaption = tr("Изображение в документе", "Document image", appLanguage)
                )
                // Add next to active block or at bottom
                if (activeBlockIndex in 0 until blocks.size) {
                    blocksList.add(activeBlockIndex + 1, newBlock)
                    activeBlockIndex++
                } else {
                    blocksList.add(newBlock)
                    activeBlockIndex = blocksList.size - 1
                }
                viewModel.updateEditorBlocks(blocksList)
            } else {
                Toast.makeText(context, tr("Ошибка импорта изображения", "Image import error", appLanguage), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            if (!isReadingMode) {
                Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
                    TopAppBar(
                        title = {
                            var editingTitle by remember(document?.title) { mutableStateOf(document?.title ?: "") }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                BasicTextField(
                                    value = editingTitle,
                                    onValueChange = {
                                        editingTitle = it
                                        viewModel.updateDocumentTitle(it)
                                    },
                                    singleLine = true,
                                    maxLines = 1,
                                    textStyle = TextStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                viewModel.saveActiveDocumentImmediate()
                                onBack()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, l("close", appLanguage))
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                geminiRequestType = if (editorTab == "BLOCKS") "BLOCK" else "DOCUMENT"
                                geminiPromptText = ""
                                geminiResultText = ""
                                geminiErrorMessage = null
                                showGeminiAssistant = true
                            }, modifier = Modifier.testTag("top_gemini_assist_btn")) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = tr("Gemini AI Помощник", "Gemini AI Assistant", appLanguage),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { isSearchPanelVisible = !isSearchPanelVisible }) {
                                Icon(Icons.Filled.Search, l("find_replace", appLanguage))
                            }
                            IconButton(onClick = { isHistoryPanelVisible = !isHistoryPanelVisible }) {
                                Icon(Icons.Filled.History, l("versions", appLanguage))
                            }
                            IconButton(onClick = onLaunchPrompter, modifier = Modifier.testTag("editor_prompter_btn")) {
                                Icon(Icons.Filled.PlayArrow, l("prompter", appLanguage))
                            }

                             IconButton(onClick = { showMoreActionsBottomSheet = true }, modifier = Modifier.testTag("editor_more_options_btn_pro")) {
                                 Icon(
                                     imageVector = Icons.Default.MoreVert,
                                     contentDescription = tr("Еще", "More options", appLanguage),
                                     tint = MaterialTheme.colorScheme.onSurface
                                 )
                             }
                        }
                    )

                    // Navigation Tabs Row
                    TabRow(
                        selectedTabIndex = if (editorTab == "BLOCKS") 0 else 1,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[if (editorTab == "BLOCKS") 0 else 1]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        Tab(
                            selected = editorTab == "BLOCKS",
                            onClick = { editorTab = "BLOCKS" },
                            text = { Text(tr("Блоки", "Blocks", appLanguage), fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.GridView, contentDescription = null) }
                        )
                        Tab(
                            selected = editorTab == "DOCUMENT",
                            onClick = { editorTab = "DOCUMENT" },
                            text = { Text(tr("Документ", "Document", appLanguage), fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) }
                        )
                    }
                }
            }
        },
        bottomBar = {
            val isKeyboardVisible = WindowInsets.isImeVisible
            if (!isKeyboardVisible && !isReadingMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(8.dp)
                ) {
                    // Script stats metadata row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${l("words", appLanguage)}: ${metrics.words}  |  ${l("chars", appLanguage)}: ${metrics.characters}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${l("pages", appLanguage)}: ${metrics.pages}  |  ${l("reading_time", appLanguage)}: ~${metrics.readingTimeMinutes} ${l("min", appLanguage)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (editorTab == "BLOCKS") {
                        // FORMATTING HORIZONTAL BAR
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activeBlock = blocks.getOrNull(activeBlockIndex)
                            
                            // Undo/Redo Controls
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.undo() },
                                    enabled = canUndo,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = l("undo", appLanguage),
                                        modifier = Modifier.size(20.dp),
                                        tint = if (canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.redo() },
                                    enabled = canRedo,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Redo,
                                        contentDescription = l("redo", appLanguage),
                                        modifier = Modifier.size(20.dp),
                                        tint = if (canRedo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            )

                            // Text Style Options
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isBold = activeBlock?.style?.isBold == true
                                IconButton(
                                    onClick = {
                                        if (activeBlock != null) {
                                            val updated = blocks.mapIndexed { index, b ->
                                                if (index == activeBlockIndex) b.copy(style = b.style.copy(isBold = !isBold)) else b
                                            }
                                            viewModel.updateEditorBlocks(updated)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("format_bold")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isBold) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "B",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp,
                                            color = if (isBold) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                val isItalic = activeBlock?.style?.isItalic == true
                                IconButton(
                                    onClick = {
                                        if (activeBlock != null) {
                                            val updated = blocks.mapIndexed { index, b ->
                                                if (index == activeBlockIndex) b.copy(style = b.style.copy(isItalic = !isItalic)) else b
                                            }
                                            viewModel.updateEditorBlocks(updated)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("format_italic")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isItalic) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "I",
                                            fontWeight = FontWeight.Bold,
                                            fontStyle = FontStyle.Italic,
                                            fontSize = 15.sp,
                                            color = if (isItalic) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                val isUnderline = activeBlock?.style?.isUnderline == true
                                IconButton(
                                    onClick = {
                                        if (activeBlock != null) {
                                          val updated = blocks.mapIndexed { index, b ->
                                              if (index == activeBlockIndex) b.copy(style = b.style.copy(isUnderline = !isUnderline)) else b
                                          }
                                          viewModel.updateEditorBlocks(updated)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isUnderline) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "U",
                                            style = TextStyle(textDecoration = TextDecoration.Underline),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isUnderline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            )

                            // Media picker action
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        geminiRequestType = "BLOCK"
                                        geminiPromptText = ""
                                        geminiResultText = ""
                                        geminiErrorMessage = null
                                        showGeminiAssistant = true
                                    },
                                    modifier = Modifier.size(38.dp).testTag("editor_gemini_block")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Gemini",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val active = blocks.getOrNull(activeBlockIndex)
                                        if (active?.type == "custom") {
                                            configuringCustomBlock = active
                                        } else {
                                            showAddBlockBottomSheet = true
                                        }
                                    },
                                    modifier = Modifier.size(38.dp).testTag("editor_custom_block_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Tune,
                                        contentDescription = tr("Кастомный блок", "Custom block", appLanguage),
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    } else {
                        // DOCUMENT MODE READING BAR CONTROLS
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr("Режим чтения", "Reading Mode", appLanguage),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Scaling down font action
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { if (readFontSizeScale > 10f) readFontSizeScale -= 1f },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("A-", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                
                                Text(
                                    text = "${(readFontSizeScale / 16f * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                // Scaling up font action
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { if (readFontSizeScale < 28f) readFontSizeScale += 1f },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("A+", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                
                                Spacer(modifier = Modifier.width(6.dp))
                                
                                // Launch Fullscreen Button
                                Button(
                                    onClick = { isReadingMode = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text(tr("На весь экран", "Fullscreen", appLanguage), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (editorTab == "BLOCKS" && !isKeyboardVisible && !isReadingMode) {
                ExtendedFloatingActionButton(
                    onClick = { showAddBlockBottomSheet = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = l("add_block", appLanguage)) },
                    text = { Text(l("add_block", appLanguage)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("fab_add_block")
                )
            } else if (editorTab == "DOCUMENT" && !isKeyboardVisible && !isReadingMode) {
                ExtendedFloatingActionButton(
                    onClick = {
                        geminiRequestType = "DOCUMENT"
                        geminiPromptText = ""
                        geminiResultText = ""
                        geminiErrorMessage = null
                        showGeminiAssistant = true
                    },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Gemini", tint = MaterialTheme.colorScheme.primary) },
                    text = { Text(tr("Анализ Gemini", "Gemini Analysis", appLanguage)) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("fab_gemini_doc")
                )
            }
        }
    ) { paddingValues ->
        val isKeyboardVisible = WindowInsets.isImeVisible
        val bottomPadding = if (isKeyboardVisible) 0.dp else paddingValues.calculateBottomPadding()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = paddingValues.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    top = paddingValues.calculateTopPadding(),
                    end = paddingValues.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    bottom = bottomPadding
                )
                .imePadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // SEARCH & REPLACE PANEL
                AnimatedVisibility(
                    visible = isSearchPanelVisible,
                    enter = expandVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(220)),
                    exit = shrinkVertically(animationSpec = tween(240, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(180))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row {
                                OutlinedTextField(
                                    value = searchQueryText,
                                    onValueChange = { searchQueryText = it },
                                    label = { Text(l("find_text", appLanguage)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = replaceQueryText,
                                    onValueChange = { replaceQueryText = it },
                                    label = { Text(l("replace_with", appLanguage)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(
                                    onClick = {
                                        if (searchQueryText.isNotEmpty()) {
                                            val updated = blocks.map { b ->
                                                if (b.text.contains(searchQueryText, ignoreCase = true)) {
                                                    b.copy(text = b.text.replace(searchQueryText, replaceQueryText, ignoreCase = true))
                                                } else b
                                            }
                                            viewModel.updateEditorBlocks(updated)
                                            Toast.makeText(context, l("text_replaced", appLanguage), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) { Text(l("replace_all", appLanguage)) }
                            }
                        }
                    }
                }

                // SCREENPLAY FORMAT PRESETS EXTRAS FOR WRITERS
                if (!isKeyboardVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val activeBlock = blocks.getOrNull(activeBlockIndex)
                        
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState())
                                .padding(end = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val presets = listOf(
                                Triple("scene", tr("Сцена", "Scene", appLanguage), Icons.Filled.Movie),
                                Triple("action", tr("Действие", "Action", appLanguage), Icons.AutoMirrored.Filled.Notes),
                                Triple("character", tr("Персонаж", "Character", appLanguage), Icons.Filled.Person),
                                Triple("dialogue", tr("Диалог", "Dialogue", appLanguage), Icons.AutoMirrored.Filled.Chat),
                                Triple("custom", tr("Кастом", "Custom", appLanguage), Icons.Filled.Tune)
                            )
                            presets.forEach { (typeId, label, icon) ->
                                val isCurr = activeBlock?.type == typeId
                                val animContainerColor by animateColorAsState(
                                    targetValue = if (isCurr) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "presetContainerColor_$typeId"
                                )
                                val animContentColor by animateColorAsState(
                                    targetValue = if (isCurr) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "presetContentColor_$typeId"
                                )
                                val animScale by animateFloatAsState(
                                    targetValue = if (isCurr) 1.04f else 1.0f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                    label = "presetScale_$typeId"
                                )

                                Row(
                                    modifier = Modifier
                                        .graphicsLayer {
                                            scaleX = animScale
                                            scaleY = animScale
                                        }
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(animContainerColor)
                                        .bounceClickable {
                                            if (isCurr && typeId == "custom") {
                                                configuringCustomBlock = activeBlock
                                                return@bounceClickable
                                            }
                                            val updated = blocks.mapIndexed { idx, block ->
                                                if (idx == activeBlockIndex) {
                                                    val formattedText = when (typeId) {
                                                        "scene", "character" -> block.text.uppercase()
                                                        "action", "dialogue" -> {
                                                            if (block.text.isNotEmpty()) {
                                                                block.text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                                            } else {
                                                                block.text
                                                            }
                                                        }
                                                        else -> block.text
                                                    }
                                                    block.copy(
                                                        type = typeId,
                                                        text = formattedText,
                                                        alignment = when (typeId) {
                                                            "character", "dialogue" -> "CENTER"
                                                            else -> "LEFT"
                                                        }
                                                    )
                                                } else block
                                            }
                                            viewModel.updateEditorBlocks(updated)
                                            if (typeId == "custom") {
                                                configuringCustomBlock = updated.getOrNull(activeBlockIndex)
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        modifier = Modifier.size(16.dp),
                                        tint = animContentColor
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = animContentColor
                                    )
                                }
                            }
                            if (activeBlock?.type == "custom") {
                                IconButton(
                                    onClick = { configuringCustomBlock = activeBlock },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = tr("Настройка кастомного блока", "Configure custom block", appLanguage),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Trash Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF381416)) // Burgundy red
                                .bounceClickable {
                                    val list = blocks.toMutableList()
                                    if (activeBlockIndex in 0 until list.size) {
                                        list.removeAt(activeBlockIndex)
                                        if (list.isEmpty()) list.add(EditorBlock(type = "scene", text = ""))
                                        activeBlockIndex = Math.max(0, activeBlockIndex - 1)
                                        viewModel.updateEditorBlocks(list)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = tr("Стереть блок", "Delete block", appLanguage),
                                tint = Color(0xFFF2B8B5),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                AnimatedContent(
                    targetState = editorTab,
                    transitionSpec = {
                        val isDoc = targetState == "DOCUMENT"
                        val enterAnim = fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                            slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { if (isDoc) (it * 0.12f).toInt() else (-it * 0.12f).toInt() }
                        val exitAnim = fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                            slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { if (isDoc) (-it * 0.12f).toInt() else (it * 0.12f).toInt() }
                        enterAnim.togetherWith(exitAnim)
                    },
                    label = "EditorTabContentTransition",
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) { targetTab ->
                    if (targetTab == "BLOCKS") {
                        // MAIN REDESIGNED M3 BLOCKS LIST
                        LazyColumn(
                            state = proLazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                        itemsIndexed(blocks, key = { _, block -> block.id }) { index, block ->
                            val isSelected = index == activeBlockIndex
                            val focusRequester = remember { FocusRequester() }
                            val isCollapsed = collapsedBlocks.value.contains(block.id)

                            LaunchedEffect(isSelected) {
                                if (isSelected) {
                                    try {
                                        focusRequester.requestFocus()
                                    } catch (e: Exception) {
                                        // ignore
                                    }
                                }
                            }

                            val accentColor = if (block.type == "custom") {
                                (block.customColorHex?.let { parseColorOrNull(it) }) ?: Color(0xFFE5A93C)
                            } else {
                                getBlockAccentColor(block.type)
                            }
                            val containerBg by animateColorAsState(
                                targetValue = if (isSelected) accentColor.copy(alpha = 0.08f) else accentColor.copy(alpha = 0.03f),
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "blockContainerBg"
                            )
                            val borderCol by animateColorAsState(
                                targetValue = if (isSelected) accentColor.copy(alpha = 0.45f) else accentColor.copy(alpha = 0.12f),
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "blockBorderCol"
                            )
                            val borderWidth by animateDpAsState(
                                targetValue = if (isSelected) 1.5.dp else 1.dp,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "blockBorderWidth"
                            )
                            val animStripeWidth by animateDpAsState(
                                targetValue = if (isSelected) 6.dp else 4.dp,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "blockStripeWidth"
                            )
                            val animElevation by animateDpAsState(
                                targetValue = if (isSelected) 3.dp else 0.dp,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "blockElevation"
                            )

                            // Beautiful Card with 20dp corners and left vertical stripe accent
                            Box(
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        shadowElevation = animElevation.toPx()
                                        shape = RoundedCornerShape(20.dp)
                                        clip = true
                                    }
                                    .background(containerBg)
                                    .border(borderWidth, borderCol, RoundedCornerShape(20.dp))
                                    .bounceClickable { activeBlockIndex = index }
                            ) {
                                // Left accent stripe
                                Box(
                                    modifier = Modifier
                                        .width(animStripeWidth)
                                        .background(
                                            accentColor,
                                            shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                                        )
                                        .align(Alignment.CenterStart)
                                        .height(IntrinsicSize.Max)
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 20.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)
                                ) {
                                    // Card Heading Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(accentColor.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (block.type == "custom") getCustomBlockIconVector(block.customIconName) else getBlockIcon(block.type),
                                                    contentDescription = null,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = when (block.type) {
                                                    "scene" -> l("scene", appLanguage).uppercase()
                                                    "action" -> l("action", appLanguage).uppercase()
                                                    "character" -> l("character", appLanguage).uppercase()
                                                    "dialogue" -> l("dialogue", appLanguage).uppercase()
                                                    "custom" -> com.example.util.LocalizationManager.translateBlockBadge(
                                                        block.customTypeName?.ifBlank { null } ?: "CUSTOM",
                                                        appLanguage
                                                    ).uppercase()
                                                    else -> l("action", appLanguage).uppercase()
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = accentColor,
                                                letterSpacing = 0.5.sp
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // Custom block configure button
                                            if (block.type == "custom") {
                                                IconButton(
                                                    onClick = { configuringCustomBlock = block },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Tune,
                                                        contentDescription = tr("Настроить кастомный блок", "Configure custom block", appLanguage),
                                                        tint = accentColor,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }

                                            // Reorder - Shift Block UP
                                            if (index > 0) {
                                                IconButton(
                                                    onClick = {
                                                        val list = blocks.toMutableList()
                                                        val temp = list[index]
                                                        list[index] = list[index - 1]
                                                        list[index - 1] = temp
                                                        viewModel.updateEditorBlocks(list)
                                                        activeBlockIndex = index - 1
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ArrowUpward,
                                                        contentDescription = "Move Up",
                                                        tint = accentColor.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }

                                            // Reorder - Shift Block DOWN
                                            if (index < blocks.size - 1) {
                                                IconButton(
                                                    onClick = {
                                                        val list = blocks.toMutableList()
                                                        val temp = list[index]
                                                        list[index] = list[index + 1]
                                                        list[index + 1] = temp
                                                        viewModel.updateEditorBlocks(list)
                                                        activeBlockIndex = index + 1
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ArrowDownward,
                                                        contentDescription = "Move Down",
                                                        tint = accentColor.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }

                                            // Collapse / Expand Toggle chevron
                                            IconButton(
                                                onClick = {
                                                    if (isCollapsed) {
                                                        collapsedBlocks.value = collapsedBlocks.value - block.id
                                                    } else {
                                                        collapsedBlocks.value = collapsedBlocks.value + block.id
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                                    contentDescription = "Collapse/Expand",
                                                    tint = accentColor.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            // Block item deletion
                                            IconButton(
                                                onClick = {
                                                    val list = blocks.toMutableList()
                                                    list.removeAt(index)
                                                    if (list.isEmpty()) {
                                                        list.add(EditorBlock(type = "scene", text = ""))
                                                    }
                                                    viewModel.updateEditorBlocks(list)
                                                    activeBlockIndex = Math.max(0, index - 1)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete block",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (isCollapsed) {
                                        // Compact text preview for collapsed state
                                        val preview = block.text.ifBlank { tr("<Пустой блок>", "<Empty block>", appLanguage) }
                                        Text(
                                            text = preview,
                                            fontSize = 13.sp,
                                            fontStyle = FontStyle.Italic,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                        )
                                    } else {
                                        // Quick assistance chips for Scene Heading
                                        if (block.type == "scene") {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState())
                                                    .padding(bottom = 6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                val sceneChips = com.example.util.LocalizationManager.getSceneChips(appLanguage)
                                                sceneChips.forEach { chipText ->
                                                    SuggestionChip(
                                                        onClick = {
                                                            val currentText = block.text.trim()
                                                            val newText = when {
                                                                currentText.isEmpty() -> chipText
                                                                chipText.startsWith("—") && !currentText.contains("—") -> "$currentText $chipText"
                                                                chipText.endsWith(".") && !currentText.startsWith(chipText) -> "$chipText $currentText"
                                                                else -> if (currentText.isEmpty()) chipText else "$currentText $chipText"
                                                            }
                                                            val updated = blocks.mapIndexed { idx, b ->
                                                                if (idx == index) b.copy(text = newText.uppercase()) else b
                                                            }
                                                            viewModel.updateEditorBlocks(updated)
                                                        },
                                                        label = { Text(chipText, fontSize = 9.sp, fontWeight = FontWeight.SemiBold) }
                                                    )
                                                }
                                            }
                                        }

                                        // Custom block prefix badge
                                        if (block.type == "custom" && !block.customPrefix.isNullOrBlank()) {
                                            Surface(
                                                color = accentColor.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            ) {
                                                Text(
                                                    text = com.example.util.LocalizationManager.translateBlockPrefix(block.customPrefix, appLanguage),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = accentColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        // Dialogue Parenthetical Remark section
                                        if (block.type == "dialogue") {
                                            if (block.parenthetical != null) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("(", fontSize = 13.sp, fontStyle = FontStyle.Italic, color = accentColor)
                                                    BasicTextField(
                                                        value = block.parenthetical ?: "",
                                                        onValueChange = { newRem ->
                                                            val updated = blocks.mapIndexed { idx, b ->
                                                                if (idx == index) b.copy(parenthetical = newRem) else b
                                                            }
                                                            viewModel.updateEditorBlocks(updated, updateHistory = false)
                                                        },
                                                        textStyle = TextStyle(
                                                            fontSize = 13.sp,
                                                            fontStyle = FontStyle.Italic,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = accentColor,
                                                            textAlign = TextAlign.Center
                                                        ),
                                                        modifier = Modifier.widthIn(min = 60.dp, max = 220.dp)
                                                    )
                                                    Text(")", fontSize = 13.sp, fontStyle = FontStyle.Italic, color = accentColor)
                                                    IconButton(
                                                        onClick = {
                                                            val updated = blocks.mapIndexed { idx, b ->
                                                                if (idx == index) b.copy(parenthetical = null) else b
                                                            }
                                                            viewModel.updateEditorBlocks(updated)
                                                        },
                                                        modifier = Modifier.size(20.dp).padding(start = 2.dp)
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = tr("Удалить ремарку", "Remove parenthetical", appLanguage), modifier = Modifier.size(12.dp), tint = Color.Gray)
                                                    }
                                                }
                                            } else {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    SuggestionChip(
                                                        onClick = {
                                                            val updated = blocks.mapIndexed { idx, b ->
                                                                if (idx == index) b.copy(parenthetical = tr("шепотом", "whispering", appLanguage)) else b
                                                            }
                                                            viewModel.updateEditorBlocks(updated)
                                                        },
                                                        label = { Text(tr("+ Добавить ремарку", "+ Add parenthetical", appLanguage), fontSize = 10.sp) },
                                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                        ),
                                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                                    )
                                                }
                                            }
                                        }

                                        val calculatedPadding = when (block.type) {
                                            "character" -> PaddingValues(horizontal = 24.dp, vertical = 2.dp)
                                            "dialogue" -> PaddingValues(horizontal = 16.dp, vertical = 2.dp)
                                            else -> PaddingValues(0.dp)
                                        }

                                        val textStyle = TextStyle(
                                            fontSize = when (block.type) {
                                                "scene" -> 15.sp
                                                "character" -> 15.sp
                                                "dialogue" -> 14.5.sp
                                                else -> 14.sp
                                            },
                                            fontWeight = when (block.type) {
                                                "scene", "character" -> FontWeight.Bold
                                                "custom" -> if (block.style.isBold) FontWeight.Bold else FontWeight.Normal
                                                else -> if (block.style.isBold) FontWeight.Bold else FontWeight.Normal
                                            },
                                            fontStyle = when (block.type) {
                                                "custom" -> if (block.style.isItalic) FontStyle.Italic else FontStyle.Normal
                                                else -> if (block.style.isItalic) FontStyle.Italic else FontStyle.Normal
                                            },
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontFamily = when (block.type) {
                                                "scene", "character", "dialogue" -> FontFamily.Monospace
                                                "custom" -> getCustomBlockFontFamily(block.customIconName)
                                                else -> FontFamily.Serif
                                            }
                                        ).copy(
                                            textDecoration = when {
                                                block.style.isUnderline && block.style.isStrikethrough -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                                                block.style.isUnderline -> TextDecoration.Underline
                                                block.style.isStrikethrough -> TextDecoration.LineThrough
                                                else -> TextDecoration.None
                                            },
                                            textAlign = when (block.type) {
                                                "character", "dialogue" -> TextAlign.Center
                                                "custom" -> when (block.alignment) {
                                                    "CENTER" -> TextAlign.Center
                                                    "RIGHT" -> TextAlign.Right
                                                    else -> TextAlign.Left
                                                }
                                                else -> when (block.alignment) {
                                                    "CENTER" -> TextAlign.Center
                                                    "RIGHT" -> TextAlign.Right
                                                    "JUSTIFY" -> TextAlign.Justify
                                                    else -> TextAlign.Left
                                                }
                                            }
                                        )

                                        Box(modifier = Modifier.padding(calculatedPadding)) {
                                            var blockTextValue by remember(block.id) {
                                                mutableStateOf(
                                                    TextFieldValue(
                                                        text = block.text,
                                                        selection = if (block.text.startsWith("- ")) TextRange(2) else TextRange(block.text.length)
                                                    )
                                                )
                                            }
                                            if (blockTextValue.text != block.text) {
                                                blockTextValue = blockTextValue.copy(
                                                    text = block.text,
                                                    selection = if (blockTextValue.selection.start <= block.text.length && blockTextValue.selection.end <= block.text.length) {
                                                        blockTextValue.selection
                                                    } else {
                                                        TextRange(block.text.length)
                                                    }
                                                )
                                            }

                                            BasicTextField(
                                                value = blockTextValue,
                                                onValueChange = label@{ newVal ->
                                                     blockTextValue = newVal
                                                     val rawText = newVal.text
                                                     
                                                     // Slash command live converter
                                                     if (rawText.startsWith("/")) {
                                                         val lowerCmd = rawText.lowercase()
                                                         val convertedType = when {
                                                             lowerCmd.startsWith("/scene") || lowerCmd.startsWith("/сцена") -> "scene"
                                                             lowerCmd.startsWith("/action") || lowerCmd.startsWith("/действие") -> "action"
                                                             lowerCmd.startsWith("/character") || lowerCmd.startsWith("/персонаж") -> "character"
                                                             lowerCmd.startsWith("/dialogue") || lowerCmd.startsWith("/диалог") -> "dialogue"
                                                             lowerCmd.startsWith("/custom") || lowerCmd.startsWith("/кастом") -> "custom"
                                                             else -> null
                                                         }
                                                         if (convertedType != null) {
                                                             val firstSpace = rawText.indexOf(" ")
                                                             val remainingText = if (firstSpace != -1) rawText.substring(firstSpace + 1) else ""
                                                             val updated = blocks.mapIndexed { idx, b ->
                                                                 if (idx == index) b.copy(
                                                                     type = convertedType,
                                                                     text = remainingText,
                                                                     alignment = when (convertedType) {
                                                                         "character", "dialogue" -> "CENTER"
                                                                         else -> "LEFT"
                                                                     }
                                                                 ) else b
                                                             }
                                                             viewModel.updateEditorBlocks(updated)
                                                             blockTextValue = TextFieldValue(text = remainingText, selection = TextRange(remainingText.length))
                                                             if (convertedType == "custom") {
                                                                 configuringCustomBlock = updated[index]
                                                             }
                                                             return@label
                                                         }
                                                     }

                                                     // Fast screenplay prefixes:
                                                     if (rawText.startsWith("ИНТ. ") || rawText.startsWith("НАТ. ") || rawText.startsWith("INT. ") || rawText.startsWith("EXT. ")) {
                                                         if (block.type != "scene") {
                                                             val updated = blocks.mapIndexed { idx, b ->
                                                                 if (idx == index) b.copy(type = "scene", text = rawText.uppercase(), alignment = "LEFT") else b
                                                             }
                                                             viewModel.updateEditorBlocks(updated)
                                                             blockTextValue = TextFieldValue(text = rawText.uppercase(), selection = TextRange(rawText.length))
                                                             return@label
                                                         }
                                                     }
                                                     if (rawText.startsWith("//")) {
                                                         if (block.type != "custom") {
                                                             val rem = rawText.removePrefix("//").trimStart()
                                                             val updated = blocks.mapIndexed { idx, b ->
                                                                 if (idx == index) b.copy(
                                                                     type = "custom",
                                                                     customTypeName = com.example.util.LocalizationManager.translateBlockBadge("NOTE", appLanguage),
                                                                     customPrefix = "//",
                                                                     text = rem
                                                                 ) else b
                                                             }
                                                             viewModel.updateEditorBlocks(updated)
                                                             blockTextValue = TextFieldValue(text = rem, selection = TextRange(rem.length))
                                                             return@label
                                                         }
                                                     }
                                                     
                                                     // Backspace on empty block: delete block and move focus to previous block
                                                     if (rawText.isEmpty() && block.text.isEmpty() && index > 0) {
                                                         val list = blocks.toMutableList()
                                                         list.removeAt(index)
                                                         viewModel.updateEditorBlocks(list)
                                                         activeBlockIndex = index - 1
                                                         return@label
                                                     }
                                                     
                                                     if (rawText.contains("\n")) {
                                                         val parts = rawText.split("\n", limit = 2)
                                                         val firstPart = parts.getOrNull(0) ?: ""
                                                         val secondPart = parts.getOrNull(1) ?: ""
                                                         val list = blocks.toMutableList()
                                                         list[index] = block.copy(text = firstPart)
                                                         val nextType = when (block.type) {
                                                             "character" -> "dialogue"
                                                             "dialogue" -> "action"
                                                             "scene" -> "action"
                                                             "action" -> "action"
                                                             "custom" -> "custom"
                                                             else -> "action"
                                                         }
                                                         val newBlock = if (nextType == "custom") {
                                                             block.copy(
                                                                 id = java.util.UUID.randomUUID().toString(),
                                                                 text = if (firstPart.trimStart().startsWith("- ")) "- " + secondPart else secondPart
                                                             )
                                                         } else {
                                                             EditorBlock(
                                                                 type = nextType,
                                                                 text = if (firstPart.trimStart().startsWith("- ")) "- " + secondPart else secondPart,
                                                                 alignment = when (nextType) {
                                                                     "character", "dialogue" -> "CENTER"
                                                                     else -> "LEFT"
                                                                 }
                                                             )
                                                         }
                                                         list.add(index + 1, newBlock)
                                                         viewModel.updateEditorBlocks(list)
                                                         activeBlockIndex = index + 1
                                                         return@label
                                                     }
                                                    var updatedValue = if (isTranslitEnabled && rawText.length > block.text.length) {
                                                        val addedText = rawText.substring(block.text.length)
                                                        block.text + transliterateEnToRu(addedText)
                                                    } else rawText

                                                    updatedValue = when (block.type) {
                                                        "scene", "character" -> updatedValue.uppercase()
                                                        "action", "dialogue" -> {
                                                            if (block.text.isEmpty() && updatedValue.isNotEmpty()) {
                                                                updatedValue.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                                            } else {
                                                                updatedValue
                                                            }
                                                        }
                                                        else -> updatedValue
                                                    }

                                                    val updated = blocks.mapIndexed { idx, b ->
                                                        if (idx == index) b.copy(text = if (block.text == "- " && updatedValue == "-") "" else updatedValue) else b
                                                    }
                                                    viewModel.updateEditorBlocks(updated, updateHistory = false)
                                                },
                                                textStyle = textStyle,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .focusRequester(focusRequester)
                                                    .onFocusChanged { if (it.isFocused) { activeBlockIndex = index } }
                                                    .testTag("editor_text_field_$index")
                                            )
                                            if (block.text.isEmpty()) {
                                                Text(
                                                    text = when (block.type) {
                                                        "scene" -> com.example.util.LocalizationManager.getDefaultSceneHeading(appLanguage)
                                                        "action" -> tr("Краткий рассказ о том, что происходит в кадре...", "Action description...", appLanguage)
                                                        "character" -> tr("ИМЯ ПЕРСОНАЖА", "CHARACTER NAME", appLanguage)
                                                        "dialogue" -> tr("Слова действующего лица...", "Dialogue lines...", appLanguage)
                                                        "custom" -> tr("Текст кастомного блока...", "Custom block content...", appLanguage)
                                                        else -> l("empty_text", appLanguage)
                                                    },
                                                    style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
                                                )
                                            }
                                            if (block.text.startsWith("/")) {
                                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                                    Text(
                                                        text = tr("Нажмите, чтобы применить тип блока:", "Tap to change block type:", appLanguage),
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(
                                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        val suggestions = listOf(
                                                            "scene" to l("scene", appLanguage),
                                                            "action" to l("action", appLanguage),
                                                            "character" to l("character", appLanguage),
                                                            "dialogue" to l("dialogue", appLanguage),
                                                            "custom" to l("custom", appLanguage)
                                                        )
                                                        suggestions.forEach { (typeKey, label) ->
                                                            Surface(
                                                                onClick = {
                                                                    val updated = blocks.mapIndexed { idx, b ->
                                                                        if (idx == index) b.copy(
                                                                            type = typeKey,
                                                                            text = "",
                                                                            alignment = when (typeKey) {
                                                                                "character", "dialogue" -> "CENTER"
                                                                                else -> "LEFT"
                                                                            }
                                                                        ) else b
                                                                    }
                                                                    viewModel.updateEditorBlocks(updated)
                                                                    blockTextValue = TextFieldValue(text = "", selection = TextRange(0))
                                                                    if (typeKey == "custom") {
                                                                        configuringCustomBlock = updated[index]
                                                                    }
                                                                },
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                                            ) {
                                                                Text(
                                                                    text = label,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // ==========================================
                        // --- DOCUMENT PREVIEW / SCRIPT READING MODE ---
                        // ==========================================
                    val readingLazyState = rememberLazyListState()
                    val readerScope = rememberCoroutineScope()
                    
                    // Filter blocks for active Scene Heading records for navigation jump chips
                    val scenesList = remember(blocks) {
                        blocks.filter { b -> b.type == "scene" && b.text.isNotBlank() }
                    }

                    if (isReadingMode) {
                        // Semitransparent floating button to close distraction-free mode
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                                    .clickable { isReadingMode = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Fullscreen", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    if (!isReadingMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(vertical = 6.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr("Выбран блок:", "Selected block:", appLanguage),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                
                                Box {
                                    var isBlockSelectorExpanded by remember { mutableStateOf(false) }
                                    val activeBlock = blocks.getOrNull(activeBlockIndex)
                                    val currentType = activeBlock?.type?.uppercase() ?: "---"
                                    val blockText = activeBlock?.text ?: ""
                                    val truncatedText = if (blockText.isBlank()) {
                                        tr("<пустой>", "<empty>", appLanguage)
                                    } else {
                                        if (blockText.length > 25) blockText.take(25) + "..." else blockText
                                    }
                                    
                                    TextButton(
                                        onClick = { isBlockSelectorExpanded = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp).testTag("action_bar_block_selector_btn")
                                    ) {
                                        Text(
                                            text = "#${activeBlockIndex + 1} [$currentType] $truncatedText ▾",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    DropdownMenu(
                                        expanded = isBlockSelectorExpanded,
                                        onDismissRequest = { isBlockSelectorExpanded = false },
                                        modifier = Modifier.heightIn(max = 280.dp)
                                    ) {
                                        blocks.forEachIndexed { idx, b ->
                                            val itemText = b.text.ifBlank { tr("<пустой>", "<empty>", appLanguage) }
                                            val labelText = "#${idx + 1} [${b.type.uppercase()}] ${if (itemText.length > 25) itemText.take(25) + "..." else itemText}"
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = labelText,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (idx == activeBlockIndex) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (idx == activeBlockIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                onClick = {
                                                    activeBlockIndex = idx
                                                    isBlockSelectorExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = {
                                        geminiRequestType = "BLOCK"
                                        geminiPromptText = ""
                                        geminiResultText = ""
                                        geminiErrorMessage = null
                                        showGeminiAssistant = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.height(28.dp).testTag("action_bar_gemini_block_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Gemini",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = tr("Gemini блока", "Block Gemini", appLanguage),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Scene quick jump index chips bar
                    if (scenesList.isNotEmpty() && !isReadingMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr("Сцены:", "Scenes:", appLanguage),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                scenesList.forEach { sBlock ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                                            .clickable {
                                                val sIdx = blocks.indexOf(sBlock)
                                                if (sIdx != -1) {
                                                    readerScope.launch {
                                                        readingLazyState.animateScrollToItem(sIdx)
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = sBlock.text.take(30) + if (sBlock.text.length > 30) "..." else "",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Ivory/White script sheet layout body
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        tonalElevation = 1.dp
                    ) {
                        if (blocks.isEmpty() || (blocks.size == 1 && blocks[0].text.isBlank())) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tr("Сценарий пуст", "Script is empty", appLanguage),
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                state = readingLazyState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp),
                                contentPadding = PaddingValues(vertical = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(blocks, key = { _, b -> b.id }) { index, rBlock ->
                                    val formattedContent = rBlock.text
                                    val isSelected = index == activeBlockIndex
                                    val accentColor = getBlockAccentColor(rBlock.type)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) accentColor.copy(alpha = 0.08f) else Color.Transparent)
                                            .border(
                                                width = if (isSelected) 1.dp else 0.dp,
                                                color = if (isSelected) accentColor.copy(alpha = 0.3f) else Color.Transparent,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { activeBlockIndex = index }
                                            .padding(if (isSelected) 8.dp else 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                    
                                    when (rBlock.type) {
                                        "scene" -> {
                                            Text(
                                                text = formattedContent.uppercase(),
                                                fontSize = (readFontSizeScale + 1).sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = getBlockAccentColor("scene"),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 16.dp, bottom = 4.dp)
                                            )
                                        }
                                        "character" -> {
                                            Text(
                                                text = formattedContent.uppercase(),
                                                fontSize = readFontSizeScale.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = getBlockAccentColor("character"),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 48.dp, end = 48.dp, top = 8.dp, bottom = 2.dp)
                                            )
                                        }
                                        "dialogue" -> {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 32.dp, end = 32.dp, top = 2.dp, bottom = 8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                if (!rBlock.parenthetical.isNullOrBlank()) {
                                                    Text(
                                                        text = "(${rBlock.parenthetical})",
                                                        fontSize = (readFontSizeScale - 1.5).sp,
                                                        fontStyle = FontStyle.Italic,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = getBlockAccentColor("dialogue"),
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.padding(bottom = 2.dp)
                                                    )
                                                }
                                                Text(
                                                    text = formattedContent,
                                                    fontSize = readFontSizeScale.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                        "custom" -> {
                                            val customColor = (rBlock.customColorHex?.let { parseColorOrNull(it) }) ?: getBlockAccentColor("custom")
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                            ) {
                                                if (!rBlock.customTypeName.isNullOrBlank()) {
                                                    Surface(
                                                        color = customColor.copy(alpha = 0.15f),
                                                        shape = RoundedCornerShape(4.dp),
                                                        modifier = Modifier.padding(bottom = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = rBlock.customTypeName.uppercase(),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = customColor,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                val displayCustomText = buildString {
                                                    if (!rBlock.customPrefix.isNullOrBlank()) append("${rBlock.customPrefix} ")
                                                    append(formattedContent)
                                                }
                                                Text(
                                                    text = displayCustomText,
                                                    fontSize = readFontSizeScale.sp,
                                                    fontWeight = if (rBlock.style.isBold) FontWeight.Bold else FontWeight.Normal,
                                                    fontStyle = if (rBlock.style.isItalic) FontStyle.Italic else FontStyle.Normal,
                                                    textDecoration = when {
                                                        rBlock.style.isUnderline && rBlock.style.isStrikethrough -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                                                        rBlock.style.isUnderline -> TextDecoration.Underline
                                                        rBlock.style.isStrikethrough -> TextDecoration.LineThrough
                                                        else -> TextDecoration.None
                                                    },
                                                    fontFamily = getCustomBlockFontFamily(rBlock.customIconName),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    textAlign = when (rBlock.alignment) {
                                                        "CENTER" -> TextAlign.Center
                                                        "RIGHT" -> TextAlign.Right
                                                        else -> TextAlign.Left
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                        else -> { // "action"
                                            Text(
                                                text = formattedContent,
                                                fontSize = readFontSizeScale.sp,
                                                fontFamily = FontFamily.Serif,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                style = TextStyle(lineHeight = (readFontSizeScale * 1.4f).sp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                    }

                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                geminiRequestType = "BLOCK"
                                                geminiPromptText = ""
                                                geminiResultText = ""
                                                geminiErrorMessage = null
                                                showGeminiAssistant = true
                                            },
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                                .testTag("doc_gemini_assist_row_${index}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = "Gemini",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    }
                                }
                            }
                        }
                    }
                    }
                }
                }
                
                // Add the keyboard accessory toolbar at the bottom of Column if keyboard is visible!
                if (isKeyboardVisible && editorTab == "BLOCKS" && !isReadingMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                    // 1. Screenplay Format Presets (Scrollable chips)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val activeBlock = blocks.getOrNull(activeBlockIndex)
                        
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val presets = listOf(
                                Triple("scene", tr("Сцена", "Scene", appLanguage), Icons.Filled.Movie),
                                Triple("action", tr("Действие", "Action", appLanguage), Icons.AutoMirrored.Filled.Notes),
                                Triple("character", tr("Персонаж", "Character", appLanguage), Icons.Filled.Person),
                                Triple("dialogue", tr("Диалог", "Dialogue", appLanguage), Icons.AutoMirrored.Filled.Chat),
                                Triple("custom", tr("Кастом", "Custom", appLanguage), Icons.Filled.Tune)
                            )
                            presets.forEach { (typeId, label, icon) ->
                                val isCurr = activeBlock?.type == typeId
                                val chipScale by animateFloatAsState(
                                    targetValue = if (isCurr) 1.05f else 1.0f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                    label = "chipScale_$typeId"
                                )
                                ElevatedFilterChip(
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = chipScale
                                        scaleY = chipScale
                                    },
                                    selected = isCurr,
                                    onClick = {
                                        if (isCurr && typeId == "custom") {
                                            configuringCustomBlock = activeBlock
                                            return@ElevatedFilterChip
                                        }
                                        val updated = blocks.mapIndexed { idx, block ->
                                            if (idx == activeBlockIndex) {
                                                val formattedText = when (typeId) {
                                                    "scene", "character" -> block.text.uppercase()
                                                    "action", "dialogue" -> {
                                                        if (block.text.isNotEmpty()) {
                                                            block.text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                                        } else {
                                                            block.text
                                                        }
                                                    }
                                                    else -> block.text
                                                }
                                                block.copy(
                                                    type = typeId,
                                                    text = formattedText,
                                                    alignment = when (typeId) {
                                                        "character", "dialogue" -> "CENTER"
                                                        else -> "LEFT"
                                                    }
                                                )
                                            } else block
                                        }
                                        viewModel.updateEditorBlocks(updated)
                                        if (typeId == "custom") {
                                            configuringCustomBlock = updated.getOrNull(activeBlockIndex)
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            modifier = Modifier.size(12.dp),
                                            tint = if (isCurr) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    },
                                    label = { 
                                        Text(
                                            text = label, 
                                            fontSize = 10.sp, 
                                            fontWeight = if (isCurr) FontWeight.Bold else FontWeight.Medium
                                        ) 
                                    },
                                    colors = FilterChipDefaults.elevatedFilterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                            if (activeBlock?.type == "custom") {
                                IconButton(
                                    onClick = { configuringCustomBlock = activeBlock },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = tr("Настройка кастомного блока", "Configure custom block", appLanguage),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        )

                        IconButton(
                            onClick = {
                                val list = blocks.toMutableList()
                                if (activeBlockIndex in 0 until list.size) {
                                    list.removeAt(activeBlockIndex)
                                    if (list.isEmpty()) list.add(EditorBlock(type = "scene", text = ""))
                                    activeBlockIndex = Math.max(0, activeBlockIndex - 1)
                                    viewModel.updateEditorBlocks(list)
                                }
                            },
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete, 
                                contentDescription = tr("Стереть блок", "Delete block", appLanguage), 
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 2. Formatting Toolbar (Undo/Redo, B, I, U, Image, Add Block)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val activeBlock = blocks.getOrNull(activeBlockIndex)
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.undo() },
                                enabled = canUndo,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = l("undo", appLanguage),
                                    modifier = Modifier.size(18.dp),
                                    tint = if (canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.redo() },
                                enabled = canRedo,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Redo,
                                    contentDescription = l("redo", appLanguage),
                                    modifier = Modifier.size(18.dp),
                                    tint = if (canRedo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                )
                            }

                            IconButton(
                                onClick = { if (activeBlockIndex > 0) activeBlockIndex-- },
                                enabled = activeBlockIndex > 0,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = tr("Предыдущий блок", "Previous block", appLanguage),
                                    modifier = Modifier.size(24.dp),
                                    tint = if (activeBlockIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                )
                            }

                            IconButton(
                                onClick = { if (activeBlockIndex < blocks.size - 1) activeBlockIndex++ },
                                enabled = activeBlockIndex < blocks.size - 1,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = tr("Следующий блок", "Next block", appLanguage),
                                    modifier = Modifier.size(24.dp),
                                    tint = if (activeBlockIndex < blocks.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isBold = activeBlock?.style?.isBold == true
                            IconButton(
                                onClick = {
                                    if (activeBlock != null) {
                                        val updated = blocks.mapIndexed { index, b ->
                                            if (index == activeBlockIndex) b.copy(style = b.style.copy(isBold = !isBold)) else b
                                        }
                                        viewModel.updateEditorBlocks(updated)
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isBold) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "B",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = if (isBold) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            val isItalic = activeBlock?.style?.isItalic == true
                            IconButton(
                                onClick = {
                                    if (activeBlock != null) {
                                        val updated = blocks.mapIndexed { index, b ->
                                            if (index == activeBlockIndex) b.copy(style = b.style.copy(isItalic = !isItalic)) else b
                                        }
                                        viewModel.updateEditorBlocks(updated)
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isItalic) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "I",
                                        fontWeight = FontWeight.Bold,
                                        fontStyle = FontStyle.Italic,
                                        fontSize = 14.sp,
                                        color = if (isItalic) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            val isUnderline = activeBlock?.style?.isUnderline == true
                            IconButton(
                                onClick = {
                                    if (activeBlock != null) {
                                      val updated = blocks.mapIndexed { index, b ->
                                          if (index == activeBlockIndex) b.copy(style = b.style.copy(isUnderline = !isUnderline)) else b
                                      }
                                      viewModel.updateEditorBlocks(updated)
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isUnderline) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "U",
                                        style = TextStyle(textDecoration = TextDecoration.Underline),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isUnderline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    geminiRequestType = "BLOCK"
                                    geminiPromptText = ""
                                    geminiResultText = ""
                                    geminiErrorMessage = null
                                    showGeminiAssistant = true
                                },
                                modifier = Modifier.size(36.dp).testTag("keyboard_gemini_block")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Gemini",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (activeBlock?.type == "custom") {
                                IconButton(
                                    onClick = { configuringCustomBlock = activeBlock },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Tune,
                                        contentDescription = tr("Настройка кастомного блока", "Configure custom block", appLanguage),
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    showAddBlockBottomSheet = true
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddCircle,
                                    contentDescription = tr("Вставить блок", "Insert block", appLanguage),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    }
                }
            }

            // SNAPSHOT HISTORY SIDE BAR DRAWER POPUP OVERLAY
            AnimatedVisibility(
                visible = isHistoryPanelVisible,
                enter = slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessLow), initialOffsetX = { it }) + fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)),
                exit = slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessLow), targetOffsetX = { it }) + fadeOut(animationSpec = tween(260, easing = FastOutSlowInEasing)),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(280.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    .align(Alignment.TopEnd)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(l("versions_revisions", appLanguage), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    var snapLabel by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = snapLabel,
                        onValueChange = { snapLabel = it },
                        label = { Text(l("save_snapshot", appLanguage)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (snapLabel.isNotEmpty()) {
                                viewModel.createManualSnapshot(snapLabel)
                                snapLabel = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).testTag("save_snapshot_btn")
                    ) { Text(l("take_snapshot", appLanguage)) }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(l("saved_revisions", appLanguage), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(history, key = { it.id }) { snap ->
                            OutlinedCard(
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                onClick = { viewModel.restoreSnapshot(snap) },
                                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(snap.snapshotName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        text = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(snap.timestamp)),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Button(onClick = { isHistoryPanelVisible = false }, modifier = Modifier.fillMaxWidth()) {
                        Text(l("close", appLanguage))
                    }
                }
            }

            if (showAddBlockBottomSheet) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { showAddBlockBottomSheet = false },
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.94f)
                            .heightIn(max = 680.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 6.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Dialog Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.AddCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = tr("Добавить блок в сценарий", "Add Screenplay Block", appLanguage),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = tr("Стандартный элемент, кастомный блок или шаблон", "Standard element, custom block or template", appLanguage),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                FilledTonalIconButton(
                                    onClick = { showAddBlockBottomSheet = false },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                                }
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                // 1. HERO CARD: CREATE CUSTOM BLOCK (STUDIO)
                                val customAccent = Color(0xFFE5A93C)
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = lerp(MaterialTheme.colorScheme.surfaceContainer, customAccent, 0.14f),
                                    border = BorderStroke(1.5.dp, customAccent.copy(alpha = 0.55f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(customAccent.copy(alpha = 0.22f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Tune,
                                                        contentDescription = null,
                                                        tint = customAccent,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                                Column {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = tr("Кастомный блок", "Custom Block", appLanguage),
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = customAccent.copy(alpha = 0.2f)
                                                        ) {
                                                            Text(
                                                                text = "STUDIO",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = customAccent,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = tr("Свой бейджик, префикс, иконка, шрифт, цвет и шаблоны", "Custom badge, prefix, icon, font, color & templates", appLanguage),
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        // Quick preset chips inside Hero Card
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            QUICK_CUSTOM_BLOCK_PRESETS.take(6).forEach { preset ->
                                                val pColor = parseColorOrNull(preset.colorHex) ?: customAccent
                                                val pIcon = CUSTOM_BLOCK_ICONS.find { it.id == preset.iconId }?.icon ?: Icons.Filled.Tune
                                                val badge = tr(preset.badgeRu, preset.badgeEn, appLanguage)
                                                Surface(
                                                    onClick = {
                                                        val list = blocks.toMutableList()
                                                        val insertIdx = if (activeBlockIndex in 0 until list.size) activeBlockIndex + 1 else list.size
                                                        val newBlk = EditorBlock(
                                                            type = "custom",
                                                            text = "",
                                                            customTypeName = badge,
                                                            customPrefix = tr(preset.prefixRu, preset.prefixEn, appLanguage),
                                                            customColorHex = preset.colorHex,
                                                            customIconName = encodeCustomIconAndFont(preset.iconId, preset.fontId),
                                                            alignment = preset.alignment,
                                                            style = com.example.data.models.BlockStyle(
                                                                isBold = preset.isBold,
                                                                isItalic = preset.isItalic,
                                                                isUnderline = preset.isUnderline
                                                            )
                                                        )
                                                        list.add(insertIdx, newBlk)
                                                        viewModel.updateEditorBlocks(list)
                                                        activeBlockIndex = insertIdx
                                                        showAddBlockBottomSheet = false
                                                        configuringCustomBlock = newBlk
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                    border = BorderStroke(1.dp, pColor.copy(alpha = 0.45f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(pIcon, contentDescription = null, tint = pColor, modifier = Modifier.size(13.dp))
                                                        Text(
                                                            text = badge,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                val list = blocks.toMutableList()
                                                val insertIdx = if (activeBlockIndex in 0 until list.size) activeBlockIndex + 1 else list.size
                                                val newBlk = EditorBlock(
                                                    type = "custom",
                                                    text = "",
                                                    customTypeName = com.example.util.LocalizationManager.translateBlockBadge("custom", appLanguage),
                                                    customColorHex = "#E5A93C",
                                                    customIconName = encodeCustomIconAndFont("tune", "serif")
                                                )
                                                list.add(insertIdx, newBlk)
                                                viewModel.updateEditorBlocks(list)
                                                activeBlockIndex = insertIdx
                                                showAddBlockBottomSheet = false
                                                configuringCustomBlock = newBlk
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = customAccent,
                                                contentColor = Color.Black
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(42.dp)
                                                .testTag("open_custom_block_studio_btn")
                                        ) {
                                            Icon(Icons.Default.AutoAwesomeMosaic, contentDescription = null, modifier = Modifier.size(17.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = tr("Открыть конструктор блока", "Open Custom Block Studio", appLanguage),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }

                                // 2. SAVED TEMPLATES SECTION (if any)
                                if (blockTemplates.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = tr("Ваши шаблоны блоков (${blockTemplates.size}):", "Saved Block Templates (${blockTemplates.size}):", appLanguage),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    blockTemplates.forEach { template ->
                                        val tColor = template.customColorHex?.let { parseColorOrNull(it) } ?: Color(0xFFE5A93C)
                                        val tIcon = getCustomBlockIconVector(template.customIconName)
                                        Surface(
                                            onClick = {
                                                val list = blocks.toMutableList()
                                                val insertIdx = if (activeBlockIndex in 0 until list.size) activeBlockIndex + 1 else list.size
                                                val newBlk = EditorBlock(
                                                    type = "custom",
                                                    text = "",
                                                    customTypeName = template.customTypeName ?: template.name,
                                                    customColorHex = template.customColorHex,
                                                    customIconName = template.customIconName ?: encodeCustomIconAndFont("tune", "serif"),
                                                    customPrefix = template.customPrefix,
                                                    alignment = template.alignment,
                                                    style = template.style
                                                )
                                                list.add(insertIdx, newBlk)
                                                viewModel.updateEditorBlocks(list)
                                                activeBlockIndex = insertIdx
                                                showAddBlockBottomSheet = false
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainer,
                                            border = BorderStroke(1.dp, tColor.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(tColor.copy(alpha = 0.18f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = tIcon,
                                                        contentDescription = null,
                                                        tint = tColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = template.name,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        if (!template.customTypeName.isNullOrBlank()) {
                                                            Surface(
                                                                shape = RoundedCornerShape(5.dp),
                                                                color = tColor.copy(alpha = 0.16f)
                                                            ) {
                                                                Text(
                                                                    text = template.customTypeName.uppercase(),
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = tColor,
                                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    if (!template.customPrefix.isNullOrBlank()) {
                                                        Text(
                                                            text = "${tr("Префикс", "Prefix", appLanguage)}: ${template.customPrefix}",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                // Customize template before/after inserting
                                                IconButton(
                                                    onClick = {
                                                        val list = blocks.toMutableList()
                                                        val insertIdx = if (activeBlockIndex in 0 until list.size) activeBlockIndex + 1 else list.size
                                                        val newBlk = EditorBlock(
                                                            type = "custom",
                                                            text = "",
                                                            customTypeName = template.customTypeName ?: template.name,
                                                            customColorHex = template.customColorHex,
                                                            customIconName = template.customIconName ?: encodeCustomIconAndFont("tune", "serif"),
                                                            customPrefix = template.customPrefix,
                                                            alignment = template.alignment,
                                                            style = template.style
                                                        )
                                                        list.add(insertIdx, newBlk)
                                                        viewModel.updateEditorBlocks(list)
                                                        activeBlockIndex = insertIdx
                                                        showAddBlockBottomSheet = false
                                                        configuringCustomBlock = newBlk
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Tune,
                                                        contentDescription = tr("Настроить", "Customize", appLanguage),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 3. STANDARD SCREENPLAY BLOCKS
                                Text(
                                    text = tr("Базовые блоки сценария:", "Standard Screenplay Blocks:", appLanguage),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                val mainBlocks = listOf(
                                    Triple(
                                        "scene",
                                        tr("Заголовок сцены", "Scene Heading", appLanguage),
                                        tr("Место и время действия (ИНТ. КОМНАТА — ДЕНЬ)", "Location and time of action (INT. ROOM - DAY)", appLanguage)
                                    ) to Triple(Icons.Filled.Movie, Color(0xFF2B5C8F), "/scene"),
                                    Triple(
                                        "action",
                                        tr("Описание действия", "Action Description", appLanguage),
                                        tr("Что происходит в кадре, действия героев и обстановка", "Narrative of what happens in the shot and setting", appLanguage)
                                    ) to Triple(Icons.AutoMirrored.Filled.Notes, Color(0xFF388E3C), "/action"),
                                    Triple(
                                        "character",
                                        tr("Имя персонажа (атрибуция)", "Character Name", appLanguage),
                                        tr("Пишется по центру перед репликой героя", "Centered before dialogue to identify speaker", appLanguage)
                                    ) to Triple(Icons.Filled.Person, Color(0xFF7B1FA2), "/character"),
                                    Triple(
                                        "dialogue",
                                        tr("Диалог и ремарка", "Dialogue", appLanguage),
                                        tr("Речь действующих лиц с поддержкой ремарок", "Character speech with optional parentheticals", appLanguage)
                                    ) to Triple(Icons.AutoMirrored.Filled.Chat, Color(0xFFD84315), "/dialogue")
                                )

                                mainBlocks.forEach { (info, styling) ->
                                    val (typeKey, title, description) = info
                                    val (icon, accentColor, slashCmd) = styling
                                    Surface(
                                        onClick = {
                                            val list = blocks.toMutableList()
                                            val insertIdx = if (activeBlockIndex in 0 until list.size) activeBlockIndex + 1 else list.size
                                            val initialText = if (typeKey == "scene") tr("ИНТ. ", "INT. ", appLanguage) else ""
                                            val newBlk = EditorBlock(
                                                type = typeKey,
                                                text = initialText,
                                                alignment = when (typeKey) {
                                                    "character", "dialogue" -> "CENTER"
                                                    else -> "LEFT"
                                                }
                                            )
                                            list.add(insertIdx, newBlk)
                                            viewModel.updateEditorBlocks(list)
                                            activeBlockIndex = insertIdx
                                            showAddBlockBottomSheet = false
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(accentColor.copy(alpha = 0.18f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = title,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(5.dp),
                                                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                                                    ) {
                                                        Text(
                                                            text = slashCmd,
                                                            fontSize = 9.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = description,
                                                    fontSize = 11.sp,
                                                    lineHeight = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val currentBlockToConfig = configuringCustomBlock
            if (currentBlockToConfig != null) {
                CustomBlockConfigDialog(
                    block = currentBlockToConfig,
                    viewModel = viewModel,
                    onSaveBlock = { updatedBlock ->
                        val updatedList = blocks.map { b ->
                            if (b.id == updatedBlock.id) updatedBlock else b
                        }
                        viewModel.updateEditorBlocks(updatedList)
                        configuringCustomBlock = null
                    },
                    onDismiss = {
                        configuringCustomBlock = null
                    }
                )
            }
        }
    }
}
}
}

// --- IMMERSIVE TELEPROMPTER MODE SCREEN ---
@Composable
fun PrompterScreen(viewModel: WriterViewModel, onClose: () -> Unit) {
    val activeDoc by viewModel.activeDocument.collectAsStateWithLifecycle()
    val blocks by viewModel.editorBlocks.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    var isMirrorH by remember { mutableStateOf(false) }
    var isMirrorV by remember { mutableStateOf(false) }
    val animMirrorV by animateFloatAsState(targetValue = if (isMirrorV) 180f else 0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "animMirrorV")
    val animMirrorH by animateFloatAsState(targetValue = if (isMirrorH) 180f else 0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "animMirrorH")
    var scrollSpeed by remember { mutableStateOf(10f) } // 1 to 50
    var isScrolling by remember { mutableStateOf(false) }
    var textFontSize by remember { mutableStateOf(26f) }
    var prompterFont by remember { mutableStateOf("SERIF") } // SERIF, SANS_SERIF, MONO
    var isSettingsCollapsed by remember { mutableStateOf(true) }
    var dragOffset by remember { mutableStateOf(0f) }

    val prompterTheme = remember { mutableStateOf("DARK") } // DARK (Black-White), AMBER (Black-Amber), BRIGHT (White-Black)

    val prompterBg = when (prompterTheme.value) {
        "AMBER" -> Color(0xFF0F0B00)
        "BRIGHT" -> Color.White
        else -> Color.Black
    }

    val prompterText = when (prompterTheme.value) {
        "AMBER" -> Color(0xFFFFA500)
        "BRIGHT" -> Color.Black
        else -> Color.White
    }

    val fullManuscriptText = remember(blocks) {
        blocks.filter { b -> b.type != "image" }.joinToString("\n\n") { it.text }
    }

    val scrollState = rememberScrollState()

    // Autoscroller Routine Continuous Ticker Thread - fully frame-rate independent and synchronized with VSync for 120Hz screens
    LaunchedEffect(isScrolling, scrollSpeed) {
        if (isScrolling) {
            var lastTimeMillis = System.currentTimeMillis()
            while (true) {
                androidx.compose.runtime.withFrameMillis { } // Suspend until next VSync V-blank ticks
                val currentTime = System.currentTimeMillis()
                val elapsedSeconds = (currentTime - lastTimeMillis) / 1000f
                lastTimeMillis = currentTime
                
                val safeElapsed = elapsedSeconds.coerceAtMost(0.05f)
                val step = scrollSpeed * 9.375f * safeElapsed
                scrollState.scrollBy(step)
            }
        }
    }

    // Speech estimation timers
    val wordsCount = remember(fullManuscriptText) { fullManuscriptText.split("\\s+".toRegex()).size }
    val remainingTimeMin = remember(scrollState.value, scrollState.maxValue) {
        val pct = if (scrollState.maxValue > 0) 1f - (scrollState.value.toFloat() / scrollState.maxValue) else 1f
        val estDurationMin = wordsCount / 150f // standard speech minutes
        (estDurationMin * pct).toInt()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(prompterBg)
    ) {
        // SCROLL CONTENT CONTAINER WITH horizontal/vertical GPU graphics transforms
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { isScrolling = !isScrolling } // Instant single-tap pause/play
                .graphicsLayer {
                    rotationX = animMirrorV
                    rotationY = animMirrorH
                }
                .padding(horizontal = 32.dp, vertical = 72.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = fullManuscriptText,
                    fontSize = textFontSize.sp,
                    color = prompterText,
                    lineHeight = (textFontSize * 1.5).sp,
                    fontFamily = when (prompterFont) {
                        "SANS_SERIF" -> FontFamily.SansSerif
                        "MONO" -> FontFamily.Monospace
                        else -> FontFamily.Serif
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(200.dp)) // padding so reader can finish reading
            }
        }

        // Cinematic gradient ambient fading overlays (top and bottom) that blend beautifully
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(prompterBg, Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, prompterBg)
                    )
                )
        )

        // Reading guide horizontal indicator line & left/right arrows that pulsate dynamically
        val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f * pulseScale),
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
            )
            // Thin elegant glowing horizontal guide line
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
                    .height(1.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent, 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), 
                                Color.Transparent
                            )
                        )
                    )
            )
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f * pulseScale),
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        rotationZ = 180f
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
            )
        }

        // Elegant animated slide input toggle drawer controls
        AnimatedContent(
            targetState = isSettingsCollapsed,
            transitionSpec = {
                if (targetState) {
                    (slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(animationSpec = tween(220)))
                        .togetherWith(slideOutVertically(targetOffsetY = { it }) + fadeOut(animationSpec = tween(220)))
                } else {
                    (slideInVertically(initialOffsetY = { it }) + fadeIn(animationSpec = tween(280)))
                        .togetherWith(slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(animationSpec = tween(220)))
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            label = "PrompterSettingsTransition"
        ) { collapsed ->
            if (collapsed) {
                Surface(
                    modifier = Modifier
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(percent = 50),
                    color = if (prompterTheme.value == "BRIGHT") {
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
                    } else {
                        Color(0xFF161618).copy(alpha = 0.9f)
                    },
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "Settings" Button to expand settings back
                        IconButton(
                            onClick = { isSettingsCollapsed = false },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = tr("Настройки", "Settings", appLanguage),
                                tint = if (prompterTheme.value == "BRIGHT") MaterialTheme.colorScheme.onSurface else Color.LightGray,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Play/Pause circular button (Start)
                        FilledIconButton(
                            onClick = { isScrolling = !isScrolling },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("prompter_play_pause"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            AnimatedContent(
                                targetState = isScrolling,
                                transitionSpec = {
                                    (scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn())
                                        .togetherWith(scaleOut(animationSpec = spring(stiffness = Spring.StiffnessHigh)) + fadeOut())
                                },
                                label = "PlayPauseIconMini"
                            ) { scrolling ->
                                Icon(
                                    imageVector = if (scrolling) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = l("prompter", appLanguage),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Exit IconButton
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = l("exit", appLanguage),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            } else {
                // CONTROL OVERLAY CONTAINER (Material 3 Styled Bottom Sheet style overlay)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = if (prompterTheme.value == "BRIGHT") {
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f)
                    } else {
                        Color(0xFF161618).copy(alpha = 0.95f)
                    },
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                var accumulatedDrag = 0f
                                detectVerticalDragGestures(
                                    onDragStart = { accumulatedDrag = 0f },
                                    onDragEnd = { accumulatedDrag = 0f },
                                    onDragCancel = { accumulatedDrag = 0f },
                                    onVerticalDrag = { change, dragAmount ->
                                        accumulatedDrag += dragAmount
                                        if (accumulatedDrag > 45f) { // Swipe down trigger
                                            isSettingsCollapsed = true
                                            accumulatedDrag = 0f
                                        }
                                    }
                                )
                            }
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Tiny drag handle and swipe-to-collapse row
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp, 4.dp)
                                    .background(
                                        color = (if (prompterTheme.value == "BRIGHT") MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray).copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                            
                            Text(
                                text = tr("Проведите пальцем вниз, чтобы свернуть настройки", "Swipe down to collapse settings", appLanguage),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = (if (prompterTheme.value == "BRIGHT") MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray).copy(alpha = 0.6f)
                            )
                        }

                        // Row 1: Estimated Time, Words & Play/Pause Circular FAB
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${l("speech_live", appLanguage)}${remainingTimeMin}${l("minutes_left", appLanguage)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (prompterTheme.value == "BRIGHT") MaterialTheme.colorScheme.onSurface else Color.LightGray
                                )
                                Text(
                                    text = tr("Всего: $wordsCount слов(а)", "Total: $wordsCount words", appLanguage),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (prompterTheme.value == "BRIGHT") MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                )
                            }

                            // Floating Action Indicator Play / Pause
                            FilledIconButton(
                                onClick = { isScrolling = !isScrolling },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("prompter_play_pause"),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                AnimatedContent(
                                    targetState = isScrolling,
                                    transitionSpec = {
                                        (scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn())
                                            .togetherWith(scaleOut(animationSpec = spring(stiffness = Spring.StiffnessHigh)) + fadeOut())
                                    },
                                    label = "PlayPauseIconExpanded"
                                ) { scrolling ->
                                    Icon(
                                        imageVector = if (scrolling) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = l("prompter", appLanguage),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            color = (if (prompterTheme.value == "BRIGHT") MaterialTheme.colorScheme.outlineVariant else Color.DarkGray).copy(alpha = 0.4f),
                            thickness = 0.5.dp
                        )

                        // Row 2: Contrast & Theme Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = l("prompter_theme", appLanguage),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (prompterTheme.value == "BRIGHT") MaterialTheme.colorScheme.onSurfaceVariant else Color.LightGray,
                                modifier = Modifier.width(64.dp)
                            )
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "DARK" to l("theme_dark_prompter", appLanguage),
                                    "AMBER" to l("theme_amber_prompter", appLanguage),
                                    "BRIGHT" to l("theme_bright_prompter", appLanguage)
                                ).forEach { (themeId, label) ->
                                    val isSel = prompterTheme.value == themeId
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { prompterTheme.value = themeId },
                                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                                    )
                                }
                            }
                        }

                        // Row 3: Font Selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr("Шрифт:", "Font:", appLanguage),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (prompterTheme.value == "BRIGHT") MaterialTheme.colorScheme.onSurfaceVariant else Color.LightGray,
                                modifier = Modifier.width(64.dp)
                            )
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "SERIF" to tr("Засечки", "Serif", appLanguage),
                                    "SANS_SERIF" to tr("Без засечек", "Sans", appLanguage),
                                    "MONO" to tr("Моно", "Mono", appLanguage)
                                ).forEach { (fontId, label) ->
                                    val isSel = prompterFont == fontId
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { prompterFont = fontId },
                                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            color = (if (prompterTheme.value == "BRIGHT") MaterialTheme.colorScheme.outlineVariant else Color.DarkGray).copy(alpha = 0.3f),
                            thickness = 0.5.dp
                        )

                        // Row 4: Speed Slider Control
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = l("speed", appLanguage),
                                tint = if (prompterTheme.value == "BRIGHT") MaterialTheme.colorScheme.onSurfaceVariant else Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = tr("Скорость", "Speed", appLanguage),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (prompterTheme.value == "BRIGHT") MaterialTheme.colorScheme.onSurface else Color.LightGray,
                                modifier = Modifier.width(84.dp)
                            )
                            Slider(
                                value = scrollSpeed,
                                onValueChange = { scrollSpeed = it },
                                valueRange = 1f..50f,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${scrollSpeed.toInt()}",
                                color = if (prompterTheme.value == "BRIGHT") MaterialTheme.colorScheme.onSurface else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(28.dp)
                            )
                        }

                        // Row 5: Text Size Slider Control
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = "Text Size",
                                tint = if (prompterTheme.value == "BRIGHT") MaterialTheme.colorScheme.onSurfaceVariant else Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = tr("Размер", "Text Size", appLanguage),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (prompterTheme.value == "BRIGHT") MaterialTheme.colorScheme.onSurface else Color.LightGray,
                                modifier = Modifier.width(84.dp)
                            )
                            Slider(
                                value = textFontSize,
                                onValueChange = { textFontSize = it },
                                valueRange = 16f..72f,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${textFontSize.toInt()}sp",
                                color = if (prompterTheme.value == "BRIGHT") MaterialTheme.colorScheme.onSurface else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(44.dp)
                            )
                        }

                        HorizontalDivider(
                            color = (if (prompterTheme.value == "BRIGHT") MaterialTheme.colorScheme.outlineVariant else Color.DarkGray).copy(alpha = 0.3f),
                            thickness = 0.5.dp
                        )

                        // Row 6: Mirror Controls & Close Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tr("Зеркало:", "Mirror:", appLanguage),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (prompterTheme.value == "BRIGHT") MaterialTheme.colorScheme.onSurfaceVariant else Color.LightGray
                                )
                                
                                FilterChip(
                                    selected = isMirrorH,
                                    onClick = { isMirrorH = !isMirrorH },
                                    label = { Text(l("mirror_h", appLanguage), fontSize = 11.sp) },
                                    modifier = Modifier.testTag("prompter_mirror_h")
                                )

                                FilterChip(
                                    selected = isMirrorV,
                                    onClick = { isMirrorV = !isMirrorV },
                                    label = { Text(l("mirror_v", appLanguage), fontSize = 11.sp) }
                                )
                            }

                            Button(
                                onClick = onClose,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, "Exit", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(l("exit", appLanguage), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- PRODUCTIVITY STATS SCREEN ---
@Composable
fun WriterStatsScreen(viewModel: WriterViewModel) {
    val statsList by viewModel.statistics.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val interfaceStyle by viewModel.interfaceStyle.collectAsStateWithLifecycle()

    val totalWords = remember(statsList) { statsList.sumOf { it.wordsCount } }
    val totalChars = remember(statsList) { statsList.sumOf { it.charsCount } }

    val currentStreak = remember(statsList) {
        var streak = 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val activeDates = statsList.filter { it.wordsCount > 0 }.map { it.dateString }.toSet()
        
        val today = Calendar.getInstance()
        val todayStr = sdf.format(today.time)
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = sdf.format(yesterday.time)
        
        val startCal = when {
            activeDates.contains(todayStr) -> today
            activeDates.contains(yesterdayStr) -> yesterday
            else -> null
        }
        
        if (startCal != null) {
            val checkCal = startCal.clone() as Calendar
            val limitSafety = activeDates.size + 2 // Infinite loop guard
            var count = 0
            while (count < limitSafety) {
                val dateStr = sdf.format(checkCal.time)
                if (activeDates.contains(dateStr)) {
                    streak++
                    checkCal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
                count++
            }
        }
        streak
    }

    if (interfaceStyle == "PIXEL") {
        val streakUnit = l("days_short", appLanguage)

        val wordsSuffix = l("words_suffix", appLanguage)

        val charsSuffix = l("chars_suffix", appLanguage)

        val cleanStreakLabel = remember(appLanguage) {
            l("stats_streak", appLanguage).substringBefore(" (").replace(":", "").trim()
        }
        val cleanWordsLabel = remember(appLanguage) {
            l("stats_words", appLanguage).replace(":", "").trim()
        }
        val cleanCharsLabel = remember(appLanguage) {
            l("stats_chars", appLanguage).replace(":", "").trim()
        }

        val last7DaysStats = remember(statsList) {
            val list = mutableListOf<ProductivityStat>()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -6)
            for (i in 0 until 7) {
                val dateStr = sdf.format(cal.time)
                val match = statsList.find { it.dateString == dateStr }
                list.add(match ?: ProductivityStat(dateString = dateStr, wordsCount = 0, charsCount = 0))
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            list
        }

        val maxItemOriginal = remember(last7DaysStats) { last7DaysStats.maxOfOrNull { it.wordsCount } ?: 0 }
        val maxItem = if (maxItemOriginal > 0) maxItemOriginal else 100
        val midValue = maxItem / 2

        val statsInfiniteTransition = rememberInfiniteTransition(label = "statsInfinite")
        val flamePulseScale by statsInfiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "flamePulse"
        )
        val animTotalWords by animateIntAsState(
            targetValue = totalWords,
            animationSpec = tween(900, easing = FastOutSlowInEasing),
            label = "animWords"
        )
        val animTotalChars by animateIntAsState(
            targetValue = totalChars,
            animationSpec = tween(900, easing = FastOutSlowInEasing),
            label = "animChars"
        )
        val chartBarProgress by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(1000, easing = FastOutSlowInEasing),
            label = "chartProgress"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Screen Title
                Text(
                    text = l("stats_title", appLanguage),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 1. Streak Card
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .graphicsLayer {
                                    scaleX = flamePulseScale
                                    scaleY = flamePulseScale
                                }
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Whatshot,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = cleanStreakLabel,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$currentStreak $streakUnit",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Text("🔥", fontSize = 26.sp)
                    }
                }

                // 2. Total Words & Chars Combined Card
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
                ) {
                    Column {
                        // Words Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Notes,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = cleanWordsLabel,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = "$animTotalWords $wordsSuffix",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.testTag("stats_total_words")
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )

                        // Characters Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Title,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = cleanCharsLabel,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = "$animTotalChars $charsSuffix",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.testTag("stats_total_chars")
                            )
                        }
                    }
                }

                // 3. Productivity Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = tr("Продуктивность", "Productivity", appLanguage),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = tr("(последние 7 дней)", "(Last 7 days)", appLanguage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 4. Productivity Graph Card
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Left Column (Y Axis)
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(end = 12.dp, bottom = 6.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(text = "$maxItem", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "$midValue", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "0", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            // Right Graph Canvas
                            val primaryColor = MaterialTheme.colorScheme.primary
                            val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            Canvas(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(bottom = 6.dp)
                            ) {
                                // Draw horizontal dashed lines
                                val pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                val gridLineYPositions = listOf(0.05f * size.height, 0.5f * size.height, 0.95f * size.height)
                                gridLineYPositions.forEach { yPos ->
                                    drawLine(
                                        color = gridColor,
                                        start = androidx.compose.ui.geometry.Offset(0f, yPos),
                                        end = androidx.compose.ui.geometry.Offset(size.width, yPos),
                                        pathEffect = pathEffect,
                                        strokeWidth = 2f
                                    )
                                }

                                // Bars
                                val blockWidth = size.width / 7
                                val barWidth = blockWidth * 0.45f

                                last7DaysStats.forEachIndexed { idx, stat ->
                                    val pct = stat.wordsCount.toFloat() / maxItem
                                    val maxBarHeight = size.height * 0.9f
                                    val barHeight = maxBarHeight * pct * chartBarProgress

                                    if (barHeight > 0f) {
                                        val left = idx * blockWidth + (blockWidth - barWidth) / 2
                                        val top = size.height * 0.95f - barHeight

                                        drawRoundRect(
                                            color = primaryColor,
                                            topLeft = androidx.compose.ui.geometry.Offset(left, top),
                                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Labels below bar chart
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            last7DaysStats.forEachIndexed { idx, _ ->
                                val daysAgo = 6 - idx
                                val label = if (daysAgo == 0) {
                                    l("today", appLanguage)
                                } else {
                                    "$daysAgo ${l("days_short", appLanguage)}"
                                }
                                Text(
                                    text = label,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // 5. Keep it up card
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = tr("Так держать!", "Keep it up!", appLanguage),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tr("Постоянство — ключ к великим историям.", "Consistency is the key to great stories.", appLanguage),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Fallback / Classic Stats Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(l("stats_title", appLanguage), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)

                // Metrics Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(l("stats_streak", appLanguage), fontWeight = FontWeight.SemiBold)
                            Text("$currentStreak ${l("days_short", appLanguage)} 🔥", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(l("stats_words", appLanguage))
                            Text("$totalWords ${l("words_suffix", appLanguage)}", fontWeight = FontWeight.Bold, modifier = Modifier.testTag("stats_total_words"))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(l("stats_chars", appLanguage))
                            Text("$totalChars ${l("chars_suffix", appLanguage)}", fontWeight = FontWeight.Bold, modifier = Modifier.testTag("stats_total_chars"))
                        }
                    }
                }

                // Productivity Graph placeholder using custom CANVAS
                Text(l("productivity_title", appLanguage), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val primaryColor = MaterialTheme.colorScheme.primary
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        ) {
                            // Draw a simple bar chart
                            val itemsToDraw = statsList.take(7).reversed()
                            if (itemsToDraw.isEmpty()) {
                                return@Canvas
                            }
                            val maxItem = itemsToDraw.maxOfOrNull { it.wordsCount } ?: 1
                            val blockWidth = size.width / 7
                            itemsToDraw.forEachIndexed { idx, stat ->
                                val pct = stat.wordsCount.toFloat() / maxItem
                                val barHeight = size.height * 0.7f * pct
                                drawRoundRect(
                                    color = primaryColor,
                                    topLeft = androidx.compose.ui.geometry.Offset(
                                        x = idx * blockWidth + (blockWidth * 0.2f),
                                        y = size.height - barHeight - 20f
                                    ),
                                    size = androidx.compose.ui.geometry.Size(
                                        width = blockWidth * 0.6f,
                                        height = barHeight
                                    ),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(l("earlier", appLanguage), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(l("today", appLanguage), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// --- SYSTEM PREFERENCES SETTINGS SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Content
            content()
        }
    }
}

@Composable
fun AppSettingsScreen(
    viewModel: WriterViewModel,
    currTheme: String,
    onThemeChange: (String) -> Unit
) {
    val context = LocalContext.current
    var inputPinValue by remember { mutableStateOf("") }
    val isPinConfigured by viewModel.isPinConfigured.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val interfaceStyle by viewModel.interfaceStyle.collectAsStateWithLifecycle()
    val bottomBarStyle by viewModel.bottomBarStyle.collectAsStateWithLifecycle()
    val activeProjects by viewModel.activeProjects.collectAsStateWithLifecycle()

    var backupPayloadLocal by remember { mutableStateOf("") }

    var showKeepImportDialog by remember { mutableStateOf(false) }
    var showKeepRawTextDialog by remember { mutableStateOf(false) }
    var pendingKeepUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var keepRawInputText by remember { mutableStateOf("") }

    var keepTargetProjectId by remember { mutableStateOf<Long?>(null) }
    var keepNewProjectName by remember(appLanguage) { mutableStateOf(tr("Заметки Google Keep", "Google Keep Notes", appLanguage)) }
    var keepImportIsPlainText by remember { mutableStateOf(true) }
    var isKeepImporting by remember { mutableStateOf(false) }

    val keepFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            pendingKeepUris = uris
            showKeepImportDialog = true
        }
    }

    val backupRestorerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val text = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                viewModel.runRestore(context, text) { success ->
                    if (success) {
                        Toast.makeText(context, l("backup_success", appLanguage), Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, l("backup_error", appLanguage), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, l("read_error", appLanguage), Toast.LENGTH_SHORT).show()
            }
        }
    }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var languageSearchQuery by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
    Column(
        modifier = Modifier
            .widthIn(max = 840.dp)
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Redesigned bold Title
        Text(
            text = l("sys_settings", appLanguage), 
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold, 
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )

        // 1. Language settings
        val allLangs = com.example.util.LocalizationManager.languages
        val activeLangObj = allLangs.firstOrNull { it.code == appLanguage }
        val isSystem = appLanguage == "system"
        val detectedSysName = com.example.util.LocalizationManager.getDetectedSystemLanguageName()

        SettingsSectionCard(
            icon = Icons.Outlined.Language,
            title = l("app_language", appLanguage),
            subtitle = l("choose_lang_desc", appLanguage)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Active language preview card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .bounceClickable { showLanguageDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSystem) {
                                Icon(
                                    imageVector = Icons.Outlined.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(
                                    text = activeLangObj?.flagEmoji ?: "🌐",
                                    fontSize = 22.sp
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (isSystem) {
                                        "${l("lang_system", appLanguage)} ($detectedSysName)"
                                    } else {
                                        activeLangObj?.nativeName ?: appLanguage.uppercase()
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = if (isSystem) "AUTO" else appLanguage.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isSystem) {
                                    tr("Синхронизировано с языком вашего устройства", "Synchronized with your device language", appLanguage)
                                } else {
                                    "${activeLangObj?.englishName ?: ""} • ${activeLangObj?.code?.uppercase() ?: ""}"
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Compact trailing chevron button to avoid crushing the text
                        FilledTonalIconButton(
                            onClick = { showLanguageDialog = true },
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Select language",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Full dialog trigger button
                OutlinedButton(
                    onClick = { showLanguageDialog = true },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Translate,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr("Выбрать из 37 языков мира...", "Choose from 37 world languages...", appLanguage),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Quick selector horizontal chips row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val quickLangs = listOf("system", "ru", "en", "es", "fr", "de", "zh", "ja", "ar", "uk", "tr")
                    quickLangs.forEach { code ->
                        val isCurr = appLanguage == code
                        val langItem = allLangs.firstOrNull { it.code == code }
                        val label = if (code == "system") {
                            "🌐 " + l("lang_system", appLanguage)
                        } else {
                            "${langItem?.flagEmoji ?: ""} ${langItem?.nativeName ?: code.uppercase()}"
                        }

                        ElevatedFilterChip(
                            selected = isCurr,
                            onClick = {
                                viewModel.setAppLanguage(code)
                                val toastMsg = if (code == "system") {
                                    tr("Язык синхронизирован с системой: $detectedSysName", "Language synced with system: $detectedSysName", "system")
                                } else {
                                    "${langItem?.nativeName ?: code} (${langItem?.englishName ?: code})"
                                }
                                Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isCurr) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.elevatedFilterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Chip to open full list
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .bounceClickable { showLanguageDialog = true },
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                            Text(tr("+ Все (37)", "+ All (37)", appLanguage), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        }

        // Full 37 Languages Selection Dialog
        if (showLanguageDialog) {
            var selectedCategory by remember { mutableStateOf(0) } // 0: All, 1: Popular, 2: Europe, 3: Asia

            AlertDialog(
                onDismissRequest = {
                    showLanguageDialog = false
                    languageSearchQuery = ""
                    selectedCategory = 0
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 6.dp,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Translate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = l("choose_lang_title", appLanguage),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tr("37 языков мира • Мгновенная синхронизация", "37 world languages • Instant sync", appLanguage),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 520.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = languageSearchQuery,
                            onValueChange = { languageSearchQuery = it },
                            placeholder = { Text(l("search_language", appLanguage), fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (languageSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { languageSearchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        )

                        // Category filter chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val categories = listOf(
                                tr("Все (37)", "All (37)", appLanguage),
                                tr("Популярные", "Popular", appLanguage),
                                tr("Европа", "Europe", appLanguage),
                                tr("Азия", "Asia", appLanguage)
                            )

                            categories.forEachIndexed { idx, label ->
                                val isCatSel = selectedCategory == idx
                                FilterChip(
                                    selected = isCatSel,
                                    onClick = { selectedCategory = idx },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isCatSel) FontWeight.Bold else FontWeight.Medium,
                                            softWrap = false,
                                            maxLines = 1
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isCatSel,
                                        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                        selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        val popularCodes = setOf("system", "ru", "en", "es", "fr", "de", "zh", "ja", "ko", "ar", "uk", "tr", "pt", "it")
                        val europeCodes = setOf("ru", "en", "es", "fr", "de", "it", "pt", "pl", "uk", "nl", "sv", "cs", "el", "ro", "hu", "da", "fi", "no", "be")
                        val asiaCodes = setOf("zh", "ja", "ko", "ar", "hi", "vi", "id", "he", "fa", "bn", "th", "kk", "uz", "az", "ka", "hy")

                        val filteredLangs = remember(languageSearchQuery, selectedCategory) {
                            var list = allLangs
                            if (selectedCategory == 1) {
                                list = list.filter { popularCodes.contains(it.code) }
                            } else if (selectedCategory == 2) {
                                list = list.filter { europeCodes.contains(it.code) || it.code == "system" }
                            } else if (selectedCategory == 3) {
                                list = list.filter { asiaCodes.contains(it.code) || it.code == "system" }
                            }

                            if (languageSearchQuery.isBlank()) {
                                list
                            } else {
                                val q = languageSearchQuery.trim().lowercase()
                                list.filter {
                                    it.code.lowercase().contains(q) ||
                                    it.nativeName.lowercase().contains(q) ||
                                    it.englishName.lowercase().contains(q)
                                }
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredLangs, key = { it.code }) { lang ->
                                val isSelected = appLanguage == lang.code
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .bounceClickable {
                                            viewModel.setAppLanguage(lang.code)
                                            showLanguageDialog = false
                                            languageSearchQuery = ""
                                            val msg = if (lang.code == "system") {
                                                tr("Язык синхронизирован с системой: $detectedSysName", "Language synchronized with system: $detectedSysName", "system")
                                            } else {
                                                "${lang.nativeName} (${lang.englishName})"
                                            }
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainer,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                    else MaterialTheme.colorScheme.surfaceContainerHighest
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (lang.code == "system") {
                                                Icon(
                                                    imageVector = Icons.Outlined.Language,
                                                    contentDescription = null,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            } else {
                                                Text(text = lang.flagEmoji, fontSize = 20.sp)
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = if (lang.code == "system") {
                                                    "${l("lang_system", appLanguage)} ($detectedSysName)"
                                                } else {
                                                    lang.nativeName
                                                },
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (lang.code == "system") {
                                                    tr("Автоопределение языка устройства", "Auto-detects device system language", appLanguage)
                                                } else {
                                                    lang.englishName
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
                                            ) {
                                                Text(
                                                    text = if (lang.code == "system") "AUTO" else lang.code.uppercase(),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    softWrap = false,
                                                    maxLines = 1
                                                )
                                            }

                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    FilledTonalButton(
                        onClick = {
                            showLanguageDialog = false
                            languageSearchQuery = ""
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(l("close", appLanguage), fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(28.dp)
            )
        }

        // 2. Theme settings
        SettingsSectionCard(
            icon = Icons.Outlined.BrightnessMedium,
            title = l("theme", appLanguage),
            subtitle = l("choose_theme_desc", appLanguage)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "LIGHT" to (l("theme_light", appLanguage) to Icons.Outlined.LightMode),
                    "DARK" to (l("theme_dark", appLanguage) to Icons.Outlined.DarkMode),
                    "BLACK" to (l("theme_black", appLanguage) to Icons.Outlined.WaterDrop)
                ).forEach { (themeId, pair) ->
                    val (label, icon) = pair
                    val isSelected = currTheme == themeId
                    
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .bounceClickable { onThemeChange(themeId) },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                                        else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 3. Color Palette Selector (Material Design 3 / Material You Redesign)
        val colorPalette by viewModel.colorPalette.collectAsStateWithLifecycle()
        val customPrimaryHex by viewModel.customPrimaryColor.collectAsStateWithLifecycle()
        val customBgHex by viewModel.customBgColor.collectAsStateWithLifecycle()
        val customParsedColor = remember(customPrimaryHex) { parseColorOrNull(customPrimaryHex) ?: Color(0xFFE5A93C) }
        val customParsedBg = remember(customBgHex, currTheme) {
            if (customBgHex.isNotEmpty()) parseColorOrNull(customBgHex) ?: Color(0xFF0E1015)
            else if (currTheme == "LIGHT") Color(0xFFF8F9FC)
            else if (currTheme == "BLACK") Color(0xFF000000)
            else Color(0xFF11121C)
        }
        var showCustomPaletteDialog by remember { mutableStateOf(false) }

        if (showCustomPaletteDialog) {
            CustomPaletteDialog(
                viewModel = viewModel,
                onDismiss = { showCustomPaletteDialog = false }
            )
        }

        SettingsSectionCard(
            icon = Icons.Outlined.Palette,
            title = l("color_palette", appLanguage),
            subtitle = l("choose_palette_desc", appLanguage)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                val baseSurfaceForSwatch = when (currTheme) {
                    "LIGHT" -> Color(0xFFF8F9FC)
                    "BLACK" -> Color(0xFF000000)
                    else -> Color(0xFF11121C)
                }

                data class M3PresetSwatch(
                    val id: String,
                    val nameRu: String,
                    val nameEn: String,
                    val primary: Color
                )

                val m3Palettes = listOf(
                    M3PresetSwatch("AMBER", "Фиолетовый", "Violet", if (currTheme == "LIGHT") Color(0xFF5D3FD3) else Color(0xFFD0BCFF)),
                    M3PresetSwatch("BLUE", "Океан", "Ocean", if (currTheme == "LIGHT") Color(0xFF165EC0) else Color(0xFF8AB4F8)),
                    M3PresetSwatch("GREEN", "Изумруд", "Emerald", if (currTheme == "LIGHT") Color(0xFF0F6D2E) else Color(0xFF81C995)),
                    M3PresetSwatch("ORANGE", "Янтарь", "Amber", if (currTheme == "LIGHT") Color(0xFFD35400) else Color(0xFFFFB066)),
                    M3PresetSwatch("RED", "Рубин", "Ruby", if (currTheme == "LIGHT") Color(0xFF962D22) else Color(0xFFFF8A80)),
                    M3PresetSwatch("CORAL", "Коралл", "Coral", if (currTheme == "LIGHT") Color(0xFFD85D4E) else Color(0xFFFE8B77)),
                    M3PresetSwatch("YELLOW", "Золото", "Gold", if (currTheme == "LIGHT") Color(0xFFB57C00) else Color(0xFFFAD02C)),
                    M3PresetSwatch("PINK", "Сакура", "Sakura", if (currTheme == "LIGHT") Color(0xFFD81B60) else Color(0xFFFF80AC)),
                    M3PresetSwatch("GREY", "Сланец", "Slate", if (currTheme == "LIGHT") Color(0xFF475569) else Color(0xFF94A3B8))
                )

                // Horizontal Carousel of Material You 3-Tone Swatch Cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Custom Studio Swatch Card
                    val isCustomSelected = colorPalette == "CUSTOM"
                    val customContainerTone = lerp(customParsedBg, customParsedColor, if (isColorBright(customParsedBg)) 0.18f else 0.28f)
                    Surface(
                        modifier = Modifier
                            .width(80.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .bounceClickable {
                                if (isCustomSelected) {
                                    showCustomPaletteDialog = true
                                } else {
                                    viewModel.setColorPalette("CUSTOM")
                                }
                            },
                        shape = RoundedCornerShape(18.dp),
                        color = if (isCustomSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            width = if (isCustomSelected) 1.5.dp else 1.dp,
                            color = if (isCustomSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MaterialYouSwatchIcon(
                                primaryColor = customParsedColor,
                                containerColor = customContainerTone,
                                surfaceColor = customParsedBg,
                                isSelected = isCustomSelected,
                                size = 46.dp,
                                showTuneIconWhenUnselected = true
                            )
                            Text(
                                text = tr("Своя", "Custom", appLanguage),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCustomSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // 2. Curated M3 Preset Swatch Cards
                    m3Palettes.forEach { preset ->
                        val isSelected = colorPalette == preset.id
                        val containerTone = lerp(
                            baseSurfaceForSwatch,
                            preset.primary,
                            if (currTheme == "LIGHT") 0.18f else 0.28f
                        )
                        Surface(
                            modifier = Modifier
                                .width(80.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .bounceClickable { viewModel.setColorPalette(preset.id) },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MaterialYouSwatchIcon(
                                    primaryColor = preset.primary,
                                    containerColor = containerTone,
                                    surfaceColor = baseSurfaceForSwatch,
                                    isSelected = isSelected,
                                    size = 46.dp
                                )
                                Text(
                                    text = tr(preset.nameRu, preset.nameEn, appLanguage),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Dedicated Material 3 Palette Studio Card with Live Tonal Strip
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .bounceClickable { showCustomPaletteDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            MaterialYouSwatchIcon(
                                primaryColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                surfaceColor = MaterialTheme.colorScheme.background,
                                isSelected = false,
                                size = 46.dp,
                                showTuneIconWhenUnselected = true
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = l("palette_studio", appLanguage),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (colorPalette == "CUSTOM") {
                                        "${l("custom_palette_active", appLanguage)} ($customPrimaryHex)"
                                    } else {
                                        l("customize_palette_desc", appLanguage)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            FilledTonalButton(
                                onClick = { showCustomPaletteDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr("Открыть", "Open", appLanguage),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                        }

                        // Live 4-Token Preview Strip inside Settings
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val activeTokens = listOf(
                                Triple("Primary", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary),
                                Triple("Container", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer),
                                Triple("Surface", MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.onSurface),
                                Triple("Canvas", MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.onBackground)
                            )
                            activeTokens.forEach { (tName, tCol, tOnCol) ->
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = tCol,
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tName,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = tOnCol,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Interface Style Selector
        SettingsSectionCard(
            icon = Icons.Outlined.GridView,
            title = l("interface_style", appLanguage),
            subtitle = l("choose_style_desc", appLanguage)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "PIXEL" to (l("style_pixel", appLanguage) to Icons.Default.Apps),
                    "CLASSIC" to (l("style_classic", appLanguage) to Icons.AutoMirrored.Outlined.ViewQuilt)
                ).forEach { (styleId, pair) ->
                    val (label, icon) = pair
                    val isSelected = interfaceStyle == styleId
                    
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .bounceClickable { viewModel.setInterfaceStyle(styleId) },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                                        else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 4b. Bottom Navigation Bar Style Selector
        SettingsSectionCard(
            icon = Icons.Outlined.Navigation,
            title = l("bottom_bar_style", appLanguage),
            subtitle = l("choose_bottom_bar_desc", appLanguage)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "STANDARD" to (l("bar_standard", appLanguage) to Icons.Outlined.HorizontalRule),
                    "CAPSULE" to (l("bar_capsule", appLanguage) to Icons.Outlined.CropFree),
                    "SEGMENTED" to (l("bar_segmented", appLanguage) to Icons.Outlined.MoreHoriz)
                ).forEach { (styleId, pair) ->
                    val (label, icon) = pair
                    val isSelected = bottomBarStyle == styleId

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .bounceClickable { viewModel.setBottomBarStyle(styleId) },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. MASTER LOCK CONFIGS
        SettingsSectionCard(
            icon = Icons.Outlined.Lock,
            title = l("security", appLanguage),
            subtitle = l("security_sub_desc", appLanguage)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = inputPinValue,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) inputPinValue = it },
                        placeholder = { Text(l("four_digits", appLanguage)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = "PIN Lock",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pin_setup_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    )
                    
                    Button(
                        onClick = {
                            if (inputPinValue.length == 4) {
                                viewModel.setupAppPin(inputPinValue)
                                inputPinValue = ""
                                Toast.makeText(context, l("pin_saved", appLanguage), Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("pin_setup_save_btn")
                    ) {
                        Text(l("set", appLanguage), fontWeight = FontWeight.Bold)
                    }
                }
                
                if (isPinConfigured) {
                    Button(
                        onClick = {
                            viewModel.setupAppPin(null)
                            Toast.makeText(context, l("pin_removed", appLanguage), Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(l("disable_pin", appLanguage), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 6. LOCAL BACKUPS
        SettingsSectionCard(
            icon = Icons.Outlined.Backup,
            title = l("backups", appLanguage),
            subtitle = l("backups_desc", appLanguage)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.runBackup(context) { txt ->
                            if (txt != null) {
                                backupPayloadLocal = txt
                                try {
                                    val backupFile = File(context.cacheDir, "writers_studio_backup.json")
                                    FileOutputStream(backupFile).use { fos ->
                                        fos.write(txt.toByteArray())
                                    }
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", backupFile)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, l("export_backup", appLanguage)))
                                } catch (e: Exception) {
                                    Toast.makeText(context, l("export_error", appLanguage), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("backup_export_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.Upload, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(l("export_backup", appLanguage), fontSize = 12.sp, maxLines = 1)
                }

                Button(
                    onClick = { backupRestorerLauncher.launch(arrayOf("application/json", "*/*")) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("backup_restore_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(l("import_backup", appLanguage), fontSize = 12.sp, maxLines = 1)
                }
            }
        }

        // 7. SYNC TO TELEPHONE STORAGE
        SettingsSectionCard(
            icon = Icons.Outlined.Folder,
            title = tr("Синхронизация папок с памятью", "Storage Directory Synchronization", appLanguage),
            subtitle = tr("Синхронизировать все папки и главы проектов в виде файлов во внутренней памяти телефона (в Documents/WriterStudioProjects)", "Export and sync folders/documents as editable files in the phone internal memory", appLanguage)
        ) {
            Button(
                onClick = {
                    viewModel.syncProjectsToInternalMemory(context) { success, path ->
                        if (success) {
                            val msg = tr("Успешно! Файлы экспортированы в:\n$path", "Success! Files exported to:\n$path", appLanguage)
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        } else {
                            val msg = tr("Исключение при экспорте: $path", "Export error: $path", appLanguage)
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("sync_projects_to_storage_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Filled.Sync, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr("Синхронизировать с памятью", "Synchronize with local storage", appLanguage),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 8. GOOGLE KEEP NOTES IMPORT
        SettingsSectionCard(
            icon = Icons.AutoMirrored.Outlined.NoteAdd,
            title = l("keep_import_title", appLanguage),
            subtitle = l("keep_import_desc", appLanguage)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { keepFilePickerLauncher.launch(arrayOf("*/*")) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("keep_import_files_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(l("keep_select_files", appLanguage), fontSize = 12.sp, maxLines = 1)
                    }

                    Button(
                        onClick = { showKeepRawTextDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("keep_import_paste_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.ContentPaste, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(l("keep_paste_text", appLanguage), fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }
    }
    }

    // Google Keep Target Selection & Import Dialog
    if (showKeepImportDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isKeepImporting) {
                    showKeepImportDialog = false
                    pendingKeepUris = emptyList()
                    keepRawInputText = ""
                }
            },
            icon = { Icon(Icons.AutoMirrored.Outlined.NoteAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text(
                    text = l("keep_import_dialog_title", appLanguage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = l("keep_target_project_desc", appLanguage),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Option 1: Create new project
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .bounceClickable { keepTargetProjectId = null },
                        color = if (keepTargetProjectId == null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (keepTargetProjectId == null) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = keepTargetProjectId == null,
                                onClick = { keepTargetProjectId = null }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = l("keep_create_new_proj", appLanguage),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                if (keepTargetProjectId == null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = keepNewProjectName,
                                        onValueChange = { keepNewProjectName = it },
                                        placeholder = { Text(l("keep_project_name_placeholder", appLanguage)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Option 2: Choose existing active project
                    if (activeProjects.isNotEmpty()) {
                        Text(
                            text = l("keep_or_existing_proj", appLanguage),
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        activeProjects.forEach { proj ->
                            val isSelected = keepTargetProjectId == proj.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .bounceClickable { keepTargetProjectId = proj.id },
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { keepTargetProjectId = proj.id }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = proj.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Option 3: Select document mode (Text Mode vs Block Mode)
                    Text(
                        text = tr("Формат импортируемых заметок:", "Format for imported notes:", appLanguage),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .bounceClickable { keepImportIsPlainText = true },
                            color = if (keepImportIsPlainText) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, if (keepImportIsPlainText) MaterialTheme.colorScheme.primary else Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = keepImportIsPlainText, onClick = { keepImportIsPlainText = true })
                                Text(
                                    text = tr("Текстовый (TXT)", "Text Mode (TXT)", appLanguage),
                                    fontSize = 12.sp,
                                    fontWeight = if (keepImportIsPlainText) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .bounceClickable { keepImportIsPlainText = false },
                            color = if (!keepImportIsPlainText) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, if (!keepImportIsPlainText) MaterialTheme.colorScheme.primary else Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = !keepImportIsPlainText, onClick = { keepImportIsPlainText = false })
                                Text(
                                    text = tr("Блочный (Pro)", "Block Mode (Pro)", appLanguage),
                                    fontSize = 12.sp,
                                    fontWeight = if (!keepImportIsPlainText) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    if (isKeepImporting) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(l("keep_importing", appLanguage), fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isKeepImporting,
                    onClick = {
                        isKeepImporting = true
                        val onDone: (Int, String?) -> Unit = { count, errorMsg ->
                            isKeepImporting = false
                            showKeepImportDialog = false
                            if (errorMsg != null) {
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            } else {
                                val msg = tr("Успешно импортировано заметок: $count", "Successfully imported $count notes!", appLanguage)
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                            pendingKeepUris = emptyList()
                            keepRawInputText = ""
                        }

                        if (pendingKeepUris.isNotEmpty()) {
                            viewModel.importGoogleKeepNotesFromUris(
                                context = context,
                                uris = pendingKeepUris,
                                targetProjectId = keepTargetProjectId,
                                newProjectTitle = keepNewProjectName,
                                isPlainText = keepImportIsPlainText,
                                onComplete = onDone
                            )
                        } else if (keepRawInputText.isNotBlank()) {
                            viewModel.importGoogleKeepNotesFromRawText(
                                rawText = keepRawInputText,
                                targetProjectId = keepTargetProjectId,
                                newProjectTitle = keepNewProjectName,
                                isPlainText = keepImportIsPlainText,
                                onComplete = onDone
                            )
                        } else {
                            isKeepImporting = false
                            showKeepImportDialog = false
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(l("import", appLanguage), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isKeepImporting,
                    onClick = {
                        showKeepImportDialog = false
                        pendingKeepUris = emptyList()
                        keepRawInputText = ""
                    }
                ) {
                    Text(l("cancel", appLanguage))
                }
            }
        )
    }

    // Google Keep Raw Text Paste Dialog
    if (showKeepRawTextDialog) {
        AlertDialog(
            onDismissRequest = { showKeepRawTextDialog = false },
            icon = { Icon(Icons.Outlined.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(l("keep_paste_dialog_title", appLanguage), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = l("keep_paste_dialog_desc", appLanguage),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = keepRawInputText,
                        onValueChange = { keepRawInputText = it },
                        placeholder = { Text(l("keep_paste_placeholder", appLanguage)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = keepRawInputText.isNotBlank(),
                    onClick = {
                        showKeepRawTextDialog = false
                        showKeepImportDialog = true
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(l("next", appLanguage), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showKeepRawTextDialog = false }) {
                    Text(l("cancel", appLanguage))
                }
            }
        )
    }
}

// --- LOCALIZATION & TRANSLITERATION DICTIONARY UTILITIES ---
fun l(key: String, lang: String): String {
    val fromManager = com.example.util.LocalizationManager.getString(key, lang)
    if (fromManager != key) return fromManager
    return key.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

fun processPlainTextSmartEditor(oldText: String, newText: String): String {
    if (newText.length < oldText.length) {
        var diffIndex = -1
        for (i in 0 until minOf(oldText.length, newText.length)) {
            if (oldText[i] != newText[i]) {
                diffIndex = i
                break
            }
        }
        if (diffIndex == -1) {
            diffIndex = newText.length
        }
        val lineStart = if (diffIndex <= 0) 0 else {
            val lastNl = newText.substring(0, diffIndex).lastIndexOf('\n')
            if (lastNl == -1) 0 else lastNl + 1
        }
        val lineSnippet = newText.substring(lineStart, minOf(newText.length, lineStart + 10))
        val oldLineSnippet = oldText.substring(lineStart, minOf(oldText.length, lineStart + 11))
        
        if (lineSnippet.startsWith("-") && !lineSnippet.startsWith("- ") && oldLineSnippet.startsWith("- ")) {
            return newText.substring(0, lineStart) + newText.substring(lineStart + 1)
        }
    }

    if (newText.length > oldText.length) {
        var diffIndex = -1
        for (i in 0 until minOf(oldText.length, newText.length)) {
            if (oldText[i] != newText[i]) {
                diffIndex = i
                break
            }
        }
        if (diffIndex == -1) {
            diffIndex = oldText.length
        }
        
        val inserted = newText.substring(diffIndex, diffIndex + (newText.length - oldText.length))
        if (inserted.contains("\n")) {
            val beforePart = newText.substring(0, diffIndex)
            val lastNewline = beforePart.lastIndexOf('\n')
            val lineStart = if (lastNewline == -1) 0 else lastNewline + 1
            val lineBefore = beforePart.substring(lineStart)
            
            if (lineBefore.trimStart().startsWith("- ")) {
                val prefix = if (lineBefore.startsWith("  - ")) "  - " else "- "
                val newlinePosInInserted = inserted.indexOf('\n')
                val absoluteNewlinePos = diffIndex + newlinePosInInserted
                
                val left = newText.substring(0, absoluteNewlinePos + 1)
                val right = newText.substring(absoluteNewlinePos + 1)
                return left + prefix + right
            }
        }
    }
    return newText
}

fun processTextFieldValueSmartEditor(oldVal: TextFieldValue, newVal: TextFieldValue): TextFieldValue {
    val oldText = oldVal.text
    val newText = newVal.text
    
    // 1. Text deleted
    if (newText.length < oldText.length) {
        var diffIndex = -1
        for (i in 0 until minOf(oldText.length, newText.length)) {
            if (oldText[i] != newText[i]) {
                diffIndex = i
                break
            }
        }
        if (diffIndex == -1) {
            diffIndex = newText.length
        }
        val lineStart = if (diffIndex <= 0) 0 else {
            val lastNl = newText.substring(0, diffIndex).lastIndexOf('\n')
            if (lastNl == -1) 0 else lastNl + 1
        }
        val lineSnippet = newText.substring(lineStart, minOf(newText.length, lineStart + 10))
        val oldLineSnippet = oldText.substring(lineStart, minOf(oldText.length, lineStart + 11))
        
        if (lineSnippet.startsWith("-") && !lineSnippet.startsWith("- ") && oldLineSnippet.startsWith("- ")) {
            val processedText = newText.substring(0, lineStart) + newText.substring(lineStart + 1)
            return TextFieldValue(
                text = processedText,
                selection = TextRange(lineStart)
            )
        }
        return newVal
    }

    // 2. Text added
    if (newText.length > oldText.length) {
        var diffIndex = -1
        for (i in 0 until minOf(oldText.length, newText.length)) {
            if (oldText[i] != newText[i]) {
                diffIndex = i
                break
            }
        }
        if (diffIndex == -1) {
            diffIndex = oldText.length
        }
        
        val inserted = newText.substring(diffIndex, diffIndex + (newText.length - oldText.length))
        if (inserted.contains("\n")) {
            val beforePart = newText.substring(0, diffIndex)
            val lastNewline = beforePart.lastIndexOf('\n')
            val lineStart = if (lastNewline == -1) 0 else lastNewline + 1
            val lineBefore = beforePart.substring(lineStart)
            
            if (lineBefore.trimStart().startsWith("- ")) {
                val prefix = if (lineBefore.startsWith("  - ")) "  - " else "- "
                val newlinePosInInserted = inserted.indexOf('\n')
                val absoluteNewlinePos = diffIndex + newlinePosInInserted
                
                val left = newText.substring(0, absoluteNewlinePos + 1)
                val right = newText.substring(absoluteNewlinePos + 1)
                val processedText = left + prefix + right
                
                val newCursorPos = absoluteNewlinePos + 1 + prefix.length
                return TextFieldValue(
                    text = processedText,
                    selection = TextRange(newCursorPos)
                )
            }
        }
    }
    
    return newVal
}

private val TRANSLIT_DIGRAPHS = mapOf(
    "yo" to "ё", "Yo" to "Ё", "YO" to "Ё",
    "zh" to "ж", "Zh" to "Ж", "ZH" to "Ж",
    "kh" to "х", "Kh" to "Х", "KH" to "Х",
    "ts" to "ц", "Ts" to "Ц", "TS" to "Ц",
    "ch" to "ч", "Ch" to "Ч", "CH" to "CH",
    "sh" to "ш", "Sh" to "Ш", "SH" to "SH",
    "shch" to "щ", "Shch" to "Щ", "SHCH" to "Щ",
    "yu" to "ю", "Yu" to "Ю", "YU" to "Ю",
    "ya" to "я", "Ya" to "Я", "YA" to "Я",
    "eh" to "э", "Eh" to "Э", "EH" to "Э"
)

private val TRANSLIT_MONOGRAFTS = mapOf(
    'a' to 'а', 'A' to 'А',
    'b' to 'б', 'B' to 'Б',
    'v' to 'в', 'V' to 'В',
    'g' to 'г', 'G' to 'Г',
    'd' to 'д', 'D' to 'Д',
    'e' to 'е', 'E' to 'Е',
    'z' to 'з', 'Z' to 'З',
    'i' to 'и', 'I' to 'И',
    'y' to 'ы', 'Y' to 'Ы',
    'k' to 'к', 'K' to 'К',
    'l' to 'л', 'L' to 'Л',
    'm' to 'м', 'M' to 'М',
    'n' to 'н', 'N' to 'Н',
    'o' to 'о', 'O' to 'О',
    'p' to 'п', 'P' to 'П',
    'r' to 'р', 'R' to 'Р',
    's' to 'с', 'S' to 'С',
    't' to 'т', 'T' to 'Т',
    'u' to 'у', 'U' to 'У',
    'f' to 'ф', 'F' to 'Ф',
    'j' to 'й', 'J' to 'Й',
    'h' to 'х', 'H' to 'Х',
    'c' to 'ц', 'C' to 'Ц',
    'w' to 'в', 'W' to 'В',
    'q' to 'я', 'Q' to 'Я',
    'x' to 'х', 'X' to 'Х',
    '\'' to 'ь', '\"' to 'ъ',
    '`' to 'ё'
)

fun transliterateEnToRu(input: String): String {
    var result = input
    for ((key, value) in TRANSLIT_DIGRAPHS) {
        result = result.replace(key, value)
    }
    val sb = java.lang.StringBuilder(result.length)
    for (char in result) {
        sb.append(TRANSLIT_MONOGRAFTS[char] ?: char)
    }
    return sb.toString()
}

private val CYRILLIC_KEYBOARD_ROWS = listOf(
    listOf("й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х", "ъ"),
    listOf("ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э"),
    listOf("я", "ч", "с", "м", "и", "т", "ь", "б", "ю", " ", "ё")
)

@Composable
fun CyrillicKeyboardPanel(onKeyTapped: (String) -> Unit, onBackspace: () -> Unit, onClose: () -> Unit, lang: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = l("keyboard_cyrillic", lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    IconButton(onClick = onBackspace, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Backspace, "Backspace")
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
            }
            
            CYRILLIC_KEYBOARD_ROWS.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEach { char ->
                        Button(
                            onClick = { onKeyTapped(char) },
                            modifier = Modifier
                                .weight(if (char == " ") 2f else 1f)
                                .height(36.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (char == " ") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (char == " ") "Space" else char,
                                fontSize = 11.sp,
                                color = if (char == " ") Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardTabItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColorAnimated by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.12f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabContainerColor"
    )

    val contentColorAnimated by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabContentColor"
    )

    val scaleAnimatedState = animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tabScale"
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = scaleAnimatedState.value
                scaleY = scaleAnimatedState.value
            }
            .height(64.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColorAnimated,
            contentColor = contentColorAnimated
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(21.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptyStateIllustration() {
    val infiniteTransition = rememberInfiniteTransition(label = "emptyStateFloat")
    val floatOffsetY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emptyFloatY"
    )
    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starTwinkle"
    )

    Box(
        modifier = Modifier
            .size(170.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        val pathColor = Color(0x33CAC4D0)
        val starColor = Color(0xFFA6ABB6).copy(alpha = starAlpha)
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawContext.transform.rotate(15f, center)
            drawCircle(
                color = pathColor,
                radius = size.width * 0.42f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )
            drawContext.transform.rotate(-15f, center)
            
            drawFourPointStar(this, center.x - size.width * 0.38f, center.y - size.height * 0.15f, 5.dp.toPx(), starColor)
            drawFourPointStar(this, center.x + size.width * 0.15f, center.y - size.height * 0.32f, 7.dp.toPx(), starColor)
            drawFourPointStar(this, center.x + size.width * 0.32f, center.y + size.height * 0.12f, 5.dp.toPx(), starColor)
        }
        
        Box(
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer {
                    translationY = floatOffsetY
                    rotationZ = -5f
                    shadowElevation = 8.dp.toPx()
                }
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF6B6E8F),
                            Color(0xFF383A59)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .offset(x = 12.dp, y = (-7).dp)
                    .width(42.dp)
                    .height(14.dp)
                    .background(
                        color = Color(0xFF626584),
                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                    )
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF5A5C7E),
                                Color(0xFF2C2E4C)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
            )
        }
    }
}

private fun drawFourPointStar(
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    centerX: Float,
    centerY: Float,
    sizePx: Float,
    color: Color
) {
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(centerX, centerY - sizePx)
        quadraticTo(centerX, centerY, centerX + sizePx, centerY)
        quadraticTo(centerX, centerY, centerX, centerY + sizePx)
        quadraticTo(centerX, centerY, centerX - sizePx, centerY)
        quadraticTo(centerX, centerY, centerX, centerY - sizePx)
        close()
    }
    drawScope.drawPath(path = path, color = color)
}
