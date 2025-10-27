package com.glassous.glestranslate.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore

val Context.appDataStore: DataStore<TranslationAppData> by dataStore(
    fileName = "translation_app_data.json",
    serializer = TranslationAppDataSerializer
)