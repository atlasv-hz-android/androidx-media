package androidx.media3.ui.compose.ext.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Created by weiping on 2024/10/7
 */
val Context.playerPreferenceStore: DataStore<Preferences> by preferencesDataStore(
    name = "player_preference_store",
    corruptionHandler = ReplaceFileCorruptionHandler {
        emptyPreferences() // Provide default preferences in case of corruption
    }
)

object PlayerPreferences {
    private const val KEY_HAS_SHOW_SWIPE_GUIDE = "has_show_swipe_guide"

    fun shouldShowClickSwipeGuide(appContext: Context): Flow<Boolean> {
        return appContext.playerPreferenceStore.data.map {
            it[booleanPreferencesKey(KEY_HAS_SHOW_SWIPE_GUIDE)] != true
        }
    }

    suspend fun saveHasClickSwipeGuide(appContext: Context) {
        appContext.playerPreferenceStore.edit {
            it[booleanPreferencesKey(KEY_HAS_SHOW_SWIPE_GUIDE)] = true
        }
    }
}