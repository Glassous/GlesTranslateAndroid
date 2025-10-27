package com.glassous.glestranslate.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.glassous.glestranslate.data.AiConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    aiConfigEnabled: Boolean = false,
    aiConfig: AiConfig = AiConfig("", "", "", ""),
    multiModalEnabled: Boolean = false,
    customLanguages: List<com.glassous.glestranslate.data.CustomLanguage> = emptyList(),
    onAiConfigEnabledChange: (Boolean) -> Unit = {},
    onAiConfigChange: (AiConfig) -> Unit = {},
    onMultiModalEnabledChange: (Boolean) -> Unit = {},
    onAddCustomLanguage: (String) -> Unit = {},
    onDeleteCustomLanguage: (String) -> Unit = {},
    onEditCustomLanguage: (String, String) -> Unit = { _, _ -> }
) {
    var showApiConfigDialog by remember { mutableStateOf(false) }
    var showLanguageManagementDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 语言管理卡片
            Card {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "语言管理",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "管理翻译语言列表",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        IconButton(onClick = { showLanguageManagementDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "语言管理")
                        }
                    }
                }
            }
            
            // API配置卡片
            Card {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "API配置",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (aiConfigEnabled) "使用自定义AI API" else "使用项目API",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        IconButton(onClick = { showApiConfigDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "配置API")
                        }
                    }
                }
            }
        }
    }
    
    // API配置对话框
    if (showApiConfigDialog) {
        ApiConfigDialog(
            aiConfigEnabled = aiConfigEnabled,
            aiConfig = aiConfig,
            multiModalEnabled = multiModalEnabled,
            onDismiss = { showApiConfigDialog = false },
            onAiConfigEnabledChange = onAiConfigEnabledChange,
            onAiConfigChange = onAiConfigChange,
            onMultiModalEnabledChange = onMultiModalEnabledChange
        )
    }
    
    // 语言管理对话框
    if (showLanguageManagementDialog) {
        LanguageManagementDialog(
            onDismiss = { showLanguageManagementDialog = false },
            customLanguages = customLanguages,
            onAddCustomLanguage = onAddCustomLanguage,
            onDeleteCustomLanguage = onDeleteCustomLanguage,
            onEditCustomLanguage = onEditCustomLanguage
        )
    }
}

@Composable
fun LanguageManagementDialog(
    onDismiss: () -> Unit,
    customLanguages: List<com.glassous.glestranslate.data.CustomLanguage> = emptyList(),
    onAddCustomLanguage: (String) -> Unit = {},
    onDeleteCustomLanguage: (String) -> Unit = {},
    onEditCustomLanguage: (String, String) -> Unit = { _, _ -> }
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingLanguage by remember { mutableStateOf<com.glassous.glestranslate.data.CustomLanguage?>(null) }
    var newLanguageName by remember { mutableStateOf("") }
    var editLanguageName by remember { mutableStateOf("") }
    
    val predefinedLanguages = com.glassous.glestranslate.data.PredefinedLanguages.languages
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("语言管理") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                Text(
                    text = "预设语言",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(predefinedLanguages.size) { index ->
                        val language = predefinedLanguages[index]
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = language.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = language.code,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "预设",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "自定义语言",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加")
                    }
                }
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.6f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(customLanguages.size) { index ->
                        val language = customLanguages[index]
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = language.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = language.code,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row {
                                    TextButton(
                                        onClick = {
                                            editingLanguage = language
                                            editLanguageName = language.name
                                            showEditDialog = true
                                        }
                                    ) {
                                        Text("编辑")
                                    }
                                    TextButton(
                                        onClick = { onDeleteCustomLanguage(language.code) }
                                    ) {
                                        Text("删除")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
    
    // 添加自定义语言对话框
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                newLanguageName = ""
            },
            title = { Text("添加自定义语言") },
            text = {
                OutlinedTextField(
                    value = newLanguageName,
                    onValueChange = { newLanguageName = it },
                    label = { Text("语言名称") },
                    placeholder = { Text("例如：乌克兰语") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newLanguageName.isNotBlank()) {
                            onAddCustomLanguage(newLanguageName)
                            showAddDialog = false
                            newLanguageName = ""
                        }
                    },
                    enabled = newLanguageName.isNotBlank()
                ) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddDialog = false
                    newLanguageName = ""
                }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 编辑自定义语言对话框
    if (showEditDialog && editingLanguage != null) {
        AlertDialog(
            onDismissRequest = { 
                showEditDialog = false
                editingLanguage = null
                editLanguageName = ""
            },
            title = { Text("编辑自定义语言") },
            text = {
                OutlinedTextField(
                    value = editLanguageName,
                    onValueChange = { editLanguageName = it },
                    label = { Text("语言名称") },
                    placeholder = { Text("例如：乌克兰语") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editLanguageName.isNotBlank() && editingLanguage != null) {
                            onEditCustomLanguage(editingLanguage!!.code, editLanguageName)
                            showEditDialog = false
                            editingLanguage = null
                            editLanguageName = ""
                        }
                    },
                    enabled = editLanguageName.isNotBlank()
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showEditDialog = false
                    editingLanguage = null
                    editLanguageName = ""
                }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun ApiConfigDialog(
    aiConfigEnabled: Boolean,
    aiConfig: AiConfig,
    multiModalEnabled: Boolean,
    onDismiss: () -> Unit,
    onAiConfigEnabledChange: (Boolean) -> Unit,
    onAiConfigChange: (AiConfig) -> Unit,
    onMultiModalEnabledChange: (Boolean) -> Unit
) {
    var tempAiConfigEnabled by remember { mutableStateOf(aiConfigEnabled) }
    var tempMultiModalEnabled by remember { mutableStateOf(multiModalEnabled) }
    var tempBaseUrl by remember { mutableStateOf(aiConfig.baseUrl) }
    var tempApiKey by remember { mutableStateOf(aiConfig.apiKey) }
    var tempModel by remember { mutableStateOf(aiConfig.model) }
    var tempMultiModalModel by remember { mutableStateOf(aiConfig.multiModalModel) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("API配置") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 启用自定义API开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("启用自定义AI API")
                    Switch(
                        checked = tempAiConfigEnabled,
                        onCheckedChange = { tempAiConfigEnabled = it }
                    )
                }
                
                if (tempAiConfigEnabled) {
                    // Base URL
                    OutlinedTextField(
                        value = tempBaseUrl,
                        onValueChange = { tempBaseUrl = it },
                        label = { Text("Base URL") },
                        placeholder = { Text("https://api.example.com/v1") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // API Key
                    OutlinedTextField(
                        value = tempApiKey,
                        onValueChange = { tempApiKey = it },
                        label = { Text("API Key") },
                        placeholder = { Text("sk-...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // 翻译模型
                    OutlinedTextField(
                        value = tempModel,
                        onValueChange = { tempModel = it },
                        label = { Text("翻译模型") },
                        placeholder = { Text("gpt-3.5-turbo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // 多模态模型
                    OutlinedTextField(
                        value = tempMultiModalModel,
                        onValueChange = { tempMultiModalModel = it },
                        label = { Text("多模态模型") },
                        placeholder = { Text("gpt-4-vision-preview") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // 多模态功能开关 - 作为二级开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("多模态功能")
                        Switch(
                            checked = tempMultiModalEnabled,
                            onCheckedChange = { tempMultiModalEnabled = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAiConfigEnabledChange(tempAiConfigEnabled)
                    // 只有启用自定义AI时才更新多模态设置，否则设为false
                    onMultiModalEnabledChange(if (tempAiConfigEnabled) tempMultiModalEnabled else false)
                    if (tempAiConfigEnabled) {
                        onAiConfigChange(
                            AiConfig(
                                baseUrl = tempBaseUrl,
                                apiKey = tempApiKey,
                                model = tempModel,
                                multiModalModel = tempMultiModalModel
                            )
                        )
                    }
                    onDismiss()
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}