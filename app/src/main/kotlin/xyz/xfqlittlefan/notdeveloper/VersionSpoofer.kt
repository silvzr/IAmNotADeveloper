package xyz.xfqlittlefan.notdeveloper

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import android.content.pm.PackageInfo
import android.os.Build
import androidx.annotation.Keep
import java.util.regex.Pattern

@Keep
class VersionSpoofer : IXposedHookLoadPackage {
    
    // Set per tenere traccia delle app già loggate
    private val loggedPackages = mutableSetOf<String>()
    
    // Pattern per validare il versionName: X.Y.ZZ.AAA
    private val versionNamePattern = Pattern.compile("^\\d+\\.\\d+\\.\\d{2}\\.\\d{3}$")
    
    // Pattern per validare il versionCode: deve avere esattamente 9 cifre
    private val versionCodePattern = Pattern.compile("^\\d{9}$")
    
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // Ignora il nostro package
        if (lpparam.packageName == "xyz.xfqlittlefan.notdeveloper") return
        
        // Ottieni i valori di version e versionCode dalle preferences
        val prefs = PreferencesHelper.getXSharedPreferences()
        prefs.reload() // Forza il ricaricamento per ottenere i valori aggiornati
        
        val spoofedVersion = prefs.getString("version_name", "") ?: ""
        val spoofedVersionCodeStr = prefs.getString("version_code", "") ?: ""
        
        // Verifica la validità dei valori
        val isVersionNameValid = spoofedVersion.isNotEmpty() && versionNamePattern.matcher(spoofedVersion).matches()
        val isVersionCodeValid = spoofedVersionCodeStr.isNotEmpty() && versionCodePattern.matcher(spoofedVersionCodeStr).matches()
        
        // Se entrambi i valori non sono validi, non fare nulla
        if (!isVersionNameValid && !isVersionCodeValid) return
        
        try {
            // Hook del metodo getPackageInfo
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "getPackageInfo",
                String::class.java,
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val packageInfo = param.result as PackageInfo? ?: return
                        
                        // Modifica version name solo se valido
                        if (isVersionNameValid) {
                            packageInfo.versionName = spoofedVersion
                            
                            // Log solo se non abbiamo ancora loggato per questo package
                            if (!loggedPackages.contains(lpparam.packageName)) {
                                XposedBridge.log("Spoofed versionName for ${lpparam.packageName} to $spoofedVersion")
                            }
                        }
                        
                        // Modifica version code solo se valido
                        if (isVersionCodeValid) {
                            try {
                                val spoofedVersionCode = spoofedVersionCodeStr.toLong()
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    packageInfo.longVersionCode = spoofedVersionCode
                                } else {
                                    @Suppress("DEPRECATION")
                                    packageInfo.versionCode = spoofedVersionCode.toInt()
                                }
                                
                                // Log solo se non abbiamo ancora loggato per questo package
                                if (!loggedPackages.contains(lpparam.packageName)) {
                                    XposedBridge.log("Spoofed versionCode for ${lpparam.packageName} to $spoofedVersionCodeStr")
                                }
                            } catch (e: NumberFormatException) {
                                // Non dovrebbe succedere grazie alla validazione, ma per sicurezza
                                if (!loggedPackages.contains(lpparam.packageName)) {
                                    XposedBridge.log("Invalid version code format: $spoofedVersionCodeStr")
                                }
                            }
                        }
                        
                        // Segna questo package come già loggato
                        if (!loggedPackages.contains(lpparam.packageName)) {
                            loggedPackages.add(lpparam.packageName)
                            XposedBridge.log("Successfully hooked ${lpparam.packageName}")
                        }
                        
                        param.result = packageInfo
                    }
                }
            )
        } catch (e: Throwable) {
            if (!loggedPackages.contains(lpparam.packageName)) {
                XposedBridge.log("Failed to hook ${lpparam.packageName}: ${e.message}")
                // Aggiungiamo comunque al set per evitare spam di errori
                loggedPackages.add(lpparam.packageName)
            }
        }
    }
}