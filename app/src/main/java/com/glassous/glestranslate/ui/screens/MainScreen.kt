package com.glassous.glestranslate.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.glassous.glestranslate.data.SelectedLanguage
import com.glassous.glestranslate.data.PredefinedLanguages
import com.glassous.glestranslate.data.TranslationHistoryItem
import com.glassous.glestranslate.ui.theme.aladinFontFamily
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    navController: NavController,
    translationHistory: List<TranslationHistoryItem> = emptyList(),
    customLanguages: List<SelectedLanguage> = emptyList(),
    selectedLanguage: SelectedLanguage = SelectedLanguage("en", "英语"),
    translationResult: String = "",
    isTranslating: Boolean = false,
    aiConfigEnabled: Boolean = false,
    multiModalEnabled: Boolean = false,
    recognitionResult: String = "",
    isRecognizing: Boolean = false,
    onTranslate: (String, String) -> Unit = { _, _ -> },
    onLanguageSelected: (SelectedLanguage) -> Unit = {},
    onDeleteHistoryItem: (Long) -> Unit = { _ -> },
    onRecognizeImage: (android.net.Uri) -> Unit = {},
    onRecognizeAudio: (android.net.Uri) -> Unit = {},
    onClearRecognition: () -> Unit = {}
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    var sourceText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }
    var showRecognitionDialog by remember { mutableStateOf(false) }
    
    val allLanguages = PredefinedLanguages.languages + customLanguages

    // 将 ViewModel 的翻译结果同步到界面本地状态（同时保留从历史选择的能力）
    LaunchedEffect(translationResult) {
        if (translationResult.isNotBlank()) {
            translatedText = translationResult
        }
    }

    // 处理返回键：当侧边栏打开时，返回键关闭侧边栏而不是退出应用
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch {
            drawerState.close()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "翻译历史",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(translationHistory.size) { index ->
                            val item = translationHistory[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 120.dp),
                                onClick = {
                                    sourceText = item.sourceText
                                    translatedText = item.translatedText
                                    scope.launch { drawerState.close() }
                                }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = item.sourceText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 2,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { confirmDeleteId = item.id }) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "删除历史",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "→ ${item.translatedText}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${item.targetLanguage} • ${item.timestamp}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { navController.navigate("settings") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("设置")
                    }
                }
            }
        }
    ) {
        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            topBar = {
                TopAppBar(
                    modifier = if (showLanguageDialog) Modifier.blur(16.dp) else Modifier,
                    title = { 
                        Text(
                            "GlesTranslate",
                            fontFamily = aladinFontFamily
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "菜单")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                        .then(if (showLanguageDialog) Modifier.blur(16.dp) else Modifier),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 输入区域
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "自动检测",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 输入文本框，使用Box包装以支持右下角按钮
                        Box {
                            OutlinedTextField(
                                value = sourceText,
                                onValueChange = { sourceText = it },
                                placeholder = { Text("请输入要翻译的文本...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp),
                                maxLines = 6
                            )
                            
                            // 清空按钮 - 仅在有输入文本时显示
                            if (sourceText.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        sourceText = ""
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                         Icons.Filled.Clear,
                                         contentDescription = "清空",
                                         tint = MaterialTheme.colorScheme.primary
                                     )
                                }
                            }
                        }
                    }
                }
                
                // 翻译按钮
                Button(
                    onClick = {
                        if (sourceText.isNotBlank()) {
                            onTranslate(sourceText, selectedLanguage.code)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = sourceText.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("翻译")
                }
                
                // 输出区域
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 目标语言文字按钮 + 弹窗呼出
                            TextButton(onClick = { showLanguageDialog = true }) {
                                Text("目标语言：${selectedLanguage.name}")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 输出文本框，使用Box包装以支持右下角按钮
                        Box {
                            // 绑定到 ViewModel 的 translationResult，以确保流式增量实时显示
                            val displayText = if (translationResult.isNotEmpty()) translationResult else translatedText
                            OutlinedTextField(
                                value = displayText,
                                onValueChange = { },
                                placeholder = { Text("翻译结果将显示在这里...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp),
                                maxLines = 6,
                                readOnly = true
                            )
                            // 加载动画：
                            // - 内置API：翻译期间始终显示
                            // - 自定义流式：未收到首个片段前显示，收到后取消
                            val showLoader = if (aiConfigEnabled) {
                                isTranslating && displayText.isBlank()
                            } else {
                                isTranslating
                            }
                            if (showLoader) {
                                // 颜色方案：指示器使用 @android:color/system_accent1_600
                                // 容器使用 @android:color/system_accent1_100
                                val context = LocalContext.current
                                val fallbackIndicator = MaterialTheme.colorScheme.primary
                                val fallbackContainer = MaterialTheme.colorScheme.surfaceVariant
                                val indicatorColor = remember(context, fallbackIndicator) {
                                    runCatching {
                                        Color(ContextCompat.getColor(context, android.R.color.system_accent1_600))
                                    }.getOrElse { fallbackIndicator }
                                }
                                val containerColor = remember(context, fallbackContainer) {
                                    runCatching {
                                        Color(ContextCompat.getColor(context, android.R.color.system_accent1_100))
                                    }.getOrElse { fallbackContainer }
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                ) {
                                    ContainedLoadingIndicator(
                                        modifier = Modifier.size(48.dp),
                                        containerColor = containerColor,
                                        indicatorColor = indicatorColor
                                    )
                                }
                            }
                            
                            // 复制按钮 - 仅在有翻译文本时显示
                            if (displayText.isNotBlank()) {
                                val clipboardManager = LocalClipboardManager.current
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(displayText))
                                        scope.launch { snackbarHostState.showSnackbar("已复制到剪贴板") }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.ContentCopy,
                                        contentDescription = "复制",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        // 识别动作按钮：语音、拍照、图片上传
                        val context = LocalContext.current
                        var latestPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
                        var pendingPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }

                        fun createTempImageUri(): android.net.Uri {
                            val imagesDir = File(context.cacheDir, "images")
                            if (!imagesDir.exists()) imagesDir.mkdirs()
                            val file = File.createTempFile("capture_", ".jpg", imagesDir)
                            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        }

                        val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                            if (success) {
                                latestPhotoUri?.let {
                                    onClearRecognition()
                                    showRecognitionDialog = true
                                    onRecognizeImage(it)
                                }
                            }
                        }

                        val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                            if (granted) {
                                pendingPhotoUri?.let { uri ->
                                    latestPhotoUri = uri
                                    takePictureLauncher.launch(uri)
                                }
                            } else {
                                // 提示权限未授予
                                scope.launch { snackbarHostState.showSnackbar("未授予相机权限") }
                            }
                        }

                        val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                            if (uri != null) {
                                onClearRecognition()
                                showRecognitionDialog = true
                                onRecognizeImage(uri)
                            }
                        }

                        val pickAudioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                            if (uri != null) {
                                onClearRecognition()
                                showRecognitionDialog = true
                                onRecognizeAudio(uri)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { pickAudioLauncher.launch("audio/*") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Mic, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("语音")
                            }
                            OutlinedButton(
                                onClick = {
                                    val permissionGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED

                                    val newUri = createTempImageUri()
                                    if (permissionGranted) {
                                        latestPhotoUri = newUri
                                        takePictureLauncher.launch(newUri)
                                    } else {
                                        pendingPhotoUri = newUri
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("拍照")
                            }
                            OutlinedButton(
                                onClick = { pickImageLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Image, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("图片")
                            }
                        }
                    }
                }
                
                // 语言选择弹窗（MD3）：标题与关闭按钮固定，仅列表可滚动
                if (showLanguageDialog) {
                    Dialog(onDismissRequest = { showLanguageDialog = false }) {
                        Card {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .heightIn(min = 240.dp, max = 480.dp)
                            ) {
                                Text(
                                    text = "选择目标语言",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(8.dp))

                                val allLanguages = PredefinedLanguages.languages + customLanguages
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(allLanguages.size) { index ->
                                        val language = allLanguages[index]
                                        ListItem(
                                            headlineContent = { Text(language.name) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onLanguageSelected(language)
                                                    showLanguageDialog = false
                                                }
                                        )
                                        Divider()
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Divider()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { showLanguageDialog = false }) {
                                        Text("关闭")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 删除确认弹窗
        if (confirmDeleteId != null) {
            AlertDialog(
                onDismissRequest = { confirmDeleteId = null },
                title = { Text("确认删除") },
                text = { Text("确定要删除该历史记录吗？") },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteHistoryItem(confirmDeleteId!!)
                        confirmDeleteId = null
                    }) { Text("删除") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDeleteId = null }) { Text("取消") }
                }
            )
        }

        // 识别结果弹窗
        if (showRecognitionDialog) {
            Dialog(onDismissRequest = { showRecognitionDialog = false }) {
                Card {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .heightIn(min = 240.dp, max = 480.dp)
                    ) {
                        Text(
                            text = "识别结果",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Box {
                            OutlinedTextField(
                                value = recognitionResult,
                                onValueChange = {},
                                placeholder = { Text("识别结果将显示在这里...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp),
                                maxLines = 6,
                                readOnly = true
                            )

                            // 加载动画：仅在未开启多模态或未使用多模态时显示
                            val showRecLoader = (!aiConfigEnabled || !multiModalEnabled) && isRecognizing
                            if (showRecLoader) {
                                val contextRec = LocalContext.current
                                val fallbackIndicatorRec = MaterialTheme.colorScheme.primary
                                val fallbackContainerRec = MaterialTheme.colorScheme.surfaceVariant
                                val indicatorColorRec = remember(contextRec, fallbackIndicatorRec) {
                                    runCatching {
                                        Color(ContextCompat.getColor(contextRec, android.R.color.system_accent1_600))
                                    }.getOrElse { fallbackIndicatorRec }
                                }
                                val containerColorRec = remember(contextRec, fallbackContainerRec) {
                                    runCatching {
                                        Color(ContextCompat.getColor(contextRec, android.R.color.system_accent1_100))
                                    }.getOrElse { fallbackContainerRec }
                                }

                                Box(
                                    modifier = Modifier.align(Alignment.Center)
                                ) {
                                    ContainedLoadingIndicator(
                                        modifier = Modifier.size(48.dp),
                                        containerColor = containerColorRec,
                                        indicatorColor = indicatorColorRec
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = {
                                showRecognitionDialog = false
                            }) { Text("关闭") }
                            Button(
                                enabled = recognitionResult.isNotBlank(),
                                onClick = {
                                    if (recognitionResult.isNotBlank()) {
                                        // 插入到输入框
                                        sourceText = recognitionResult
                                        showRecognitionDialog = false
                                    }
                                }
                            ) { Text("插入输入框") }
                        }
                    }
                }
            }
        }
    }
}
}