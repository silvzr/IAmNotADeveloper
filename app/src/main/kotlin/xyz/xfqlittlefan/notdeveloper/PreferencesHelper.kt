package xyz.xfqlittlefan.notdeveloper

import android.content.Context
import android.content.SharedPreferences
import de.robv.android.xposed.XSharedPreferences

object PreferencesHelper {
    
    private const val PREFERENCES_FILE = "xyz.xfqlittlefan.notdeveloper_preferences"
    
    // Per ottenere le preferenze dal contesto Xposed
    fun getXSharedPreferences(): XSharedPreferences {
        val prefs = XSharedPreferences("xyz.xfqlittlefan.notdeveloper", PREFERENCES_FILE)
        prefs.makeWorldReadable()
        return prefs
    }
    
    // Per ottenere le preferenze dall'app
    fun getAppPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_WORLD_READABLE)
    }
    
    // Salva la configurazione del modulo con il nuovo parametro spotifyPatchEnabled
    fun saveModuleConfig(context: Context, versionName: String, versionCode: String, spotifyPatchEnabled: Boolean = false) {
        val prefs = getAppPreferences(context)
        prefs.edit().apply {
            putString("version_name", versionName)
            putString("version_code", versionCode)
            putBoolean("spotify_patch_enabled", spotifyPatchEnabled)
            apply()
        }
    }
    
    // Ottieni la configurazione del modulo con il nuovo campo
    fun getModuleConfig(context: Context): ModuleConfig {
        val prefs = getAppPreferences(context)
        return ModuleConfig(
            versionName = prefs.getString("version_name", "") ?: "",
            versionCode = prefs.getString("version_code", "") ?: "",
            spotifyPatchEnabled = prefs.getBoolean("spotify_patch_enabled", false)
        )
    }
    
    // Classe dati per la configurazione aggiornata
    data class ModuleConfig(
        val versionName: String,
        val versionCode: String,
        val spotifyPatchEnabled: Boolean
    )
}
