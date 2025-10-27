package com.glassous.glestranslate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.glassous.glestranslate.viewmodel.TranslationViewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.glassous.glestranslate.ui.screens.MainScreen
import com.glassous.glestranslate.ui.screens.SettingsScreen
import com.glassous.glestranslate.ui.theme.GlesTranslateTheme
import com.glassous.glestranslate.viewmodel.TranslationViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlesTranslateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TranslationApp()
                }
            }
        }
    }
}

@Composable
fun TranslationApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val viewModel: TranslationViewModel = viewModel(factory = TranslationViewModelFactory(context))
    
    val translationHistory by viewModel.translationHistory.collectAsState()
    val customLanguages by viewModel.customLanguages.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val aiConfigEnabled by viewModel.aiConfigEnabled.collectAsState()
    val aiConfig by viewModel.aiConfig.collectAsState()
    val multiModalEnabled by viewModel.multiModalEnabled.collectAsState()
    val translationResult by viewModel.translationResult.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()
    
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
                MainScreen(
                    navController = navController,
                    translationHistory = translationHistory,
                    customLanguages = customLanguages.map { 
                        com.glassous.glestranslate.data.SelectedLanguage(it.code, it.name) 
                    },
                    selectedLanguage = selectedLanguage,
                    translationResult = translationResult,
                    isTranslating = isTranslating,
                    aiConfigEnabled = aiConfigEnabled,
                    onTranslate = { sourceText, targetLanguageCode ->
                        viewModel.translate(sourceText, targetLanguageCode)
                    },
                    onLanguageSelected = { language ->
                        viewModel.selectLanguage(language)
                    },
                onDeleteHistoryItem = { id ->
                    viewModel.deleteHistoryItem(id)
                }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                navController = navController,
                aiConfigEnabled = aiConfigEnabled,
                aiConfig = aiConfig,
                multiModalEnabled = multiModalEnabled,
                customLanguages = customLanguages,
                onAiConfigEnabledChange = { enabled ->
                    viewModel.updateAiConfigEnabled(enabled)
                },
                onAiConfigChange = { config ->
                    viewModel.updateAiConfig(config)
                },
                onMultiModalEnabledChange = { enabled ->
                    viewModel.updateMultiModalEnabled(enabled)
                },
                onAddCustomLanguage = { languageName ->
                    viewModel.addCustomLanguage(languageName)
                },
                onDeleteCustomLanguage = { languageCode ->
                    viewModel.deleteCustomLanguage(languageCode)
                },
                onEditCustomLanguage = { languageCode, newName ->
                    viewModel.editCustomLanguage(languageCode, newName)
                }
            )
        }
    }
}