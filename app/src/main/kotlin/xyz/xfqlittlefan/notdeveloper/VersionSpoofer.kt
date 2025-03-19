package xyz.xfqlittlefan.notdeveloper

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import android.content.pm.PackageInfo
import android.os.Build
import androidx.annotation.Keep

@Keep
class VersionSpoofer : IXposedHookLoadPackage {
    
    // Set per tenere traccia delle app già loggate
    private val loggedPackages = mutableSetOf<String>()
    
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // Ignora il nostro package
        if (lpparam.packageName == "xyz.xfqlittlefan.notdeveloper") return
        
        // Verifica se il modulo è attivo e ottieni i valori configurati
        val prefs = PreferencesHelper.getXSharedPreferences()
        prefs.reload() // Forza il ricaricamento per ottenere i valori aggiornati
        
        val isModuleEnabled = prefs.getBoolean("module_enabled", true)
        if (!isModuleEnabled) return
        
        val spoofedVersion = prefs.getString("version_name", "") ?: ""
        val spoofedVersionCodeStr = prefs.getString("version_code", "") ?: ""
        
        if (spoofedVersion.isEmpty() && spoofedVersionCodeStr.isEmpty()) return
        
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
                        
                        // Modifica version name se specificato
                        if (spoofedVersion.isNotEmpty()) {
                            packageInfo.versionName = spoofedVersion
                            
                            // Log solo se non abbiamo ancora loggato per questo package
                            if (!loggedPackages.contains(lpparam.packageName)) {
                                XposedBridge.log("Spoofed versionName for ${lpparam.packageName} to $spoofedVersion")
                            }
                        }
                        
                        // Modifica version code se specificato
                        if (spoofedVersionCodeStr.isNotEmpty()) {
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
