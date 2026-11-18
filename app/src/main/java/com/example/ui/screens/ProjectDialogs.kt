package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.entity.*
import com.example.ui.viewmodel.WriterViewModel
import com.example.util.ImageStorageUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.launch

// Helper to calculate SHA-1 for Google Sign-In debugging
fun getAppSha1(ctx: Context): String {
    return try {
        val packageManager = ctx.packageManager
        val packageName = ctx.packageName
        @Suppress("DEPRECATION")
        val packageInfo = packageManager.getPackageInfo(
            packageName,
            android.content.pm.PackageManager.GET_SIGNATURES
        )
        @Suppress("DEPRECATION")
        val signatures = packageInfo.signatures
        if (signatures != null && signatures.isNotEmpty()) {
            val sig = signatures[0]
            val md = java.security.MessageDigest.getInstance("SHA-1")
            val digest = md.digest(sig.toByteArray())
            val sb = StringBuilder()
            for (b in digest) {
                sb.append(String.format("%02X:", b))
            }
            if (sb.isNotEmpty()) sb.setLength(sb.length - 1)
            sb.toString()
        } else {
            "No signatures found"
        }
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}

// --- CREATE PROJECT DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectDialog(
    viewModel: WriterViewModel,
    onDismiss: () -> Unit
) {
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    var pTitle by remember { mutableStateOf("") }
    var pType by remember { mutableStateOf("BOOK") } // SCREENPLAY, BOOK, STORY, TEXT
    val colorsPalette = listOf("#6200EE", "#D32F2F", "#388E3C", "#1976D2", "#FBC02D", "#7B1FA2", "#808080")
    var selectedCol by remember { mutableStateOf("#6200EE") }

    val coroutineScope = rememberCoroutineScope()
    val dragOffsetY = remember { Animatable(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .offset(y = dragOffsetY.value.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragOffsetY.value > 120f) {
                                    onDismiss()
                                } else {
                                    coroutineScope.launch {
                                        dragOffsetY.animateTo(0f, spring())
                                    }
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    dragOffsetY.animateTo(0f, spring())
                                }
                            },
                            onVerticalDrag = { _, dragAmount ->
                                if (dragAmount > 0 || dragOffsetY.value > 0) {
                                    coroutineScope.launch {
                                        dragOffsetY.snapTo(maxOf(0f, dragOffsetY.value + dragAmount * 0.5f))
                                    }
                                }
                            }
                        )
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                    .imePadding()
                    .navigationBarsPadding()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = l("create_proj_title", appLanguage),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = pTitle,
                        onValueChange = { pTitle = it },
                        label = { Text(l("proj_name_hint", appLanguage)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_project_title_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = l("select_format", appLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("BOOK", l("cat_book", appLanguage), Icons.Outlined.Book),
                            Triple("SCREENPLAY", l("cat_screenplay", appLanguage), Icons.Outlined.Movie),
                            Triple("STORY", l("cat_story", appLanguage), Icons.Outlined.AutoStories),
                            Triple("TEXT", l("cat_text", appLanguage), Icons.Outlined.Article)
                        ).forEach { (tKey, tLabel, tIcon) ->
                            val isSel = pType == tKey
                            FilterChip(
                                selected = isSel,
                                onClick = { pType = tKey },
                                leadingIcon = {
                                    Icon(
                                        imageVector = tIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = { Text(tLabel) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = l("cover_accent_color", appLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        colorsPalette.forEach { cHex ->
                            val parsedCol = try {
                                Color(android.graphics.Color.parseColor(cHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                            val isSelected = selectedCol == cHex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(parsedCol)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedCol = cHex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(l("cancel", appLanguage))
                        }

                        Button(
                            onClick = {
                                if (pTitle.isNotBlank()) {
                                    viewModel.createProject(
                                        title = pTitle.trim(),
                                        type = pType,
                                        colorHex = selectedCol,
                                        password = null
                                    )
                                    onDismiss()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("dialog_create_project_confirm"),
                            shape = RoundedCornerShape(14.dp),
                            enabled = pTitle.isNotBlank()
                        ) {
                            Text(l("create", appLanguage), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- ACCOUNT PROFILE DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountProfileDialog(
    viewModel: WriterViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val activeProjs by viewModel.activeProjects.collectAsStateWithLifecycle()
    val archivedProjs by viewModel.archivedProjects.collectAsStateWithLifecycle()
    val trashProjs by viewModel.trashProjects.collectAsStateWithLifecycle()
    val statsList by viewModel.statistics.collectAsStateWithLifecycle()
    val authorName by viewModel.authorName.collectAsStateWithLifecycle()
    val authorBio by viewModel.authorBio.collectAsStateWithLifecycle()
    val authorAvatar by viewModel.authorAvatar.collectAsStateWithLifecycle()
    val authorEmail by viewModel.authorEmail.collectAsStateWithLifecycle()
    val googleUserId by viewModel.googleUserId.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    val localAuthorName by viewModel.localAuthorName.collectAsStateWithLifecycle()
    val localAuthorBio by viewModel.localAuthorBio.collectAsStateWithLifecycle()
    val localAuthorAvatar by viewModel.localAuthorAvatar.collectAsStateWithLifecycle()

    var editName by remember { mutableStateOf(localAuthorName) }
    var editBio by remember { mutableStateOf(localAuthorBio) }
    var editAvatarPath by remember { mutableStateOf(localAuthorAvatar) }
    var selectedTabIndex by remember { mutableStateOf(0) }

    LaunchedEffect(localAuthorName, localAuthorBio, localAuthorAvatar) {
        editName = localAuthorName
        editBio = localAuthorBio
        editAvatarPath = localAuthorAvatar
    }

    var authErrorDialogMessage by remember { mutableStateOf<String?>(null) }

    // Google Sign In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_CANCELED) {
            // User cancelled the sign-in intent - do nothing quietly
            Log.d("GoogleSignIn", "User cancelled Google Sign-In flow.")
            return@rememberLauncherForActivityResult
        }
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val gId = account.id ?: account.email ?: "google_user"
            val gName = account.displayName ?: "Google User"
            val gEmail = account.email ?: ""
            val gPhoto = account.photoUrl?.toString() ?: ""
            viewModel.linkGoogleAccount(context, gId, gName, gEmail, gPhoto)
            Toast.makeText(
                context,
                "${l("google_connected", appLanguage)}: $gEmail",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: ApiException) {
            // Code 12501 = GoogleSignInStatusCodes.SIGN_IN_CANCELLED
            if (e.statusCode == com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CANCELLED || e.statusCode == 12501) {
                Log.d("GoogleSignIn", "Google sign-in cancelled by user (12501).")
            } else {
                Log.e("GoogleSignIn", "Google sign in failed code: ${e.statusCode}, msg: ${e.message}", e)
                authErrorDialogMessage = "Google Sign-In Error: ${e.statusCode}"
            }
        } catch (e: Exception) {
            Log.e("GoogleSignIn", "Google sign in exception: ${e.message}", e)
        }
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val copiedPath = ImageStorageUtil.copyImageToProject(context, uri)
            if (copiedPath != null) {
                editAvatarPath = copiedPath
            }
        }
    }

    val totalWords = remember(statsList) { statsList.sumOf { it.wordsCount } }
    val totalProjectsCount = remember(activeProjs, archivedProjs, trashProjs) {
        activeProjs.size + archivedProjs.size + trashProjs.size
    }

    val isGoogleLinked = googleUserId.isNotBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                    .imePadding()
                    .navigationBarsPadding()
                    .wrapContentHeight()
                    .heightIn(max = 680.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Bar
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = l("close", appLanguage),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val displayAvatar = if (isGoogleLinked && authorAvatar.isNotEmpty()) authorAvatar else editAvatarPath
                            val defaultAuthorLabel = com.example.util.LocalizationManager.synchronizeAuthorName("", appLanguage)
                            val displayName = if (isGoogleLinked && authorName.isNotEmpty()) {
                                authorName
                            } else {
                                if (editName.isNotBlank()) editName else defaultAuthorLabel
                            }

                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .clickable { avatarPickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (displayAvatar.isNotEmpty()) {
                                    AsyncImage(
                                        model = displayAvatar,
                                        contentDescription = "Avatar",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(52.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (isGoogleLinked && authorEmail.isNotEmpty()) {
                                Text(
                                    text = authorEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = {
                                Text(
                                    tr("Аккаунт и Cloud", "Account & Cloud", appLanguage),
                                    fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = {
                                Text(
                                    tr("Профиль", "Profile", appLanguage),
                                    fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = selectedTabIndex == 2,
                            onClick = { selectedTabIndex = 2 },
                            text = {
                                Text(
                                    tr("Статистика", "Stats", appLanguage),
                                    fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (selectedTabIndex) {
                            0 -> {
                                // Google Account & Cloud Sync Tab
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (isGoogleLinked) {
                                        // Connected Card
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CloudDone,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = tr("Google Drive Синхронизация", "Google Drive Sync", appLanguage),
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = if (isSyncing) {
                                                                tr("Синхронизация...", "Syncing...", appLanguage)
                                                            } else {
                                                                tr("Подключено к Google", "Connected to Google", appLanguage)
                                                            },
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            viewModel.syncAllWithDrive(context)
                                                            Toast.makeText(
                                                                context,
                                                                tr("Синхронизация...", "Syncing...", appLanguage),
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(10.dp),
                                                        enabled = !isSyncing
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Sync,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(l("sync_now", appLanguage), fontSize = 12.sp)
                                                    }

                                                    Button(
                                                        onClick = {
                                                            viewModel.disconnectGoogleAccount(context)
                                                            Toast.makeText(
                                                                context,
                                                                l("google_disconnected", appLanguage),
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                                        ),
                                                        shape = RoundedCornerShape(10.dp)
                                                    ) {
                                                        Text(tr("Выйти", "Disconnect", appLanguage), fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        // Connect Google Account Banner
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CloudSync,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = tr("Облачное резервное копирование", "Cloud Backup & Sync", appLanguage),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = l("google_sync_desc", appLanguage),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )

                                                Spacer(modifier = Modifier.height(14.dp))

                                                // Google Sign-In Button
                                                Button(
                                                    onClick = {
                                                        try {
                                                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                                                .requestEmail()
                                                                .requestScopes(
                                                                    Scope("https://www.googleapis.com/auth/drive.file"),
                                                                    Scope("https://www.googleapis.com/auth/drive.appdata")
                                                                )
                                                                .build()
                                                            val client = GoogleSignIn.getClient(context, gso)
                                                            googleSignInLauncher.launch(client.signInIntent)
                                                        } catch (e: Exception) {
                                                            Log.e("GoogleSignIn", "Error launching Google Sign-In: ${e.message}", e)
                                                            authErrorDialogMessage = "Google Sign In setup exception: ${e.message}"
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(48.dp)
                                                        .testTag("google_sign_in_button"),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary,
                                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.AccountCircle,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = tr("Войти через Google", "Sign in with Google", appLanguage),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            1 -> {
                                // Author Profile Tab
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = editName,
                                        onValueChange = { editName = it },
                                        label = { Text(tr("Имя автора", "Author Name", appLanguage)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    OutlinedTextField(
                                        value = editBio,
                                        onValueChange = { editBio = it },
                                        label = { Text(tr("О себе", "Bio", appLanguage)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 3,
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Button(
                                        onClick = {
                                            viewModel.setLocalAuthorProfile(editName.trim(), editBio.trim())
                                            if (editAvatarPath.isNotEmpty()) {
                                                viewModel.setLocalAuthorAvatar(editAvatarPath)
                                            }
                                            Toast.makeText(
                                                context,
                                                l("save", appLanguage),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onDismiss()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(l("save", appLanguage), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            2 -> {
                                // Stats Tab
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = l("total_projects", appLanguage),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "$totalProjectsCount",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 18.sp
                                            )
                                        }
                                    }

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = l("total_words_written", appLanguage),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "$totalWords",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontSize = 18.sp
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

        // Error / Sandbox Auth Dialog
        if (authErrorDialogMessage != null) {
            AlertDialog(
                onDismissRequest = { authErrorDialogMessage = null },
                title = {
                    Text(
                        text = l("google_sync", appLanguage),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = authErrorDialogMessage ?: "")
                        Text(
                            text = "SHA-1: ${getAppSha1(context)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                confirmButton = {
                    Button(
                        modifier = Modifier.testTag("sandbox_auth_login"),
                        onClick = {
                            val simulatedEmail = "writer.account@gmail.com"
                            val simulatedName = "Google Author"
                            val simulatedAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde"
                            val simulatedId = "simulated_google_id_writer"
                            viewModel.linkGoogleAccount(context, simulatedId, simulatedName, simulatedEmail, simulatedAvatar)
                            authErrorDialogMessage = null
                        }
                    ) {
                        Text(
                            text = l("connect_google", appLanguage),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { authErrorDialogMessage = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(l("close", appLanguage))
                    }
                }
            )
        }
    }
}
