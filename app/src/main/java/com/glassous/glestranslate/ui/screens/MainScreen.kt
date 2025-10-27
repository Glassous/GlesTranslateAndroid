package com.glassous.glestranslate.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.glassous.glestranslate.data.SelectedLanguage
import com.glassous.glestranslate.data.PredefinedLanguages
import com.glassous.glestranslate.data.TranslationHistoryItem
import com.glassous.glestranslate.ui.components.ExpressiveLoadingIndicator
import com.glassous.glestranslate.ui.theme.aladinFontFamily
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    translationHistory: List<TranslationHistoryItem> = emptyList(),
    customLanguages: List<SelectedLanguage> = emptyList(),
    selectedLanguage: SelectedLanguage = SelectedLanguage("en", "英语"),
    translationResult: String = "",
    isTranslating: Boolean = false,
    aiConfigEnabled: Boolean = false,
    onTranslate: (String, String) -> Unit = { _, _ -> },
    onLanguageSelected: (SelectedLanguage) -> Unit = {},
    onDeleteHistoryItem: (Long) -> Unit = { _ -> }
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    var sourceText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }
    
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
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                ) {
                                    ExpressiveLoadingIndicator(
                                        contained = true,
                                        modifier = Modifier.size(56.dp)
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
    }
}
}