package xyz.xfqlittlefan.notdeveloper.xposed

import android.util.Log
import androidx.annotation.Keep
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import xyz.xfqlittlefan.notdeveloper.BuildConfig
import java.util.*

@Keep
class Hook : IXposedHookLoadPackage {
    private val TAG = "TimeModifier"
    private val customTime = Calendar.getInstance()

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName.startsWith("android") || lpparam.packageName.startsWith("com.android")) {
            return
        }

        XposedBridge.log("$TAG: processing " + lpparam.packageName)

        if (lpparam.packageName == BuildConfig.APPLICATION_ID) {
            XposedHelpers.findAndHookMethod(
                "xyz.xfqlittlefan.notdeveloper.xposed.ModuleStatusKt",
                lpparam.classLoader,
                "isModuleActive",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = true
                    }
                }
            )
            return
        }

        val prefs = XSharedPreferences(BuildConfig.APPLICATION_ID, "time_modifier_prefs")
        if (!prefs.file.canRead()) {
            XposedBridge.log("$TAG: Cannot read preferences file. Make sure it's created with MODE_WORLD_READABLE")
            return
        }

        val customTimeMillis = prefs.getLong("custom_time_millis", 0)
        if (customTimeMillis <= 0) {
            XposedBridge.log("$TAG: No custom time set, skipping hooks for ${lpparam.packageName}")
            return
        }

	val roundedTimeMillis = (customTimeMillis / 1000) * 1000 // Arrotonda a secondi interi
	customTime.timeInMillis = roundedTimeMillis
	XposedBridge.log("$TAG: Using custom time: ${customTime.time} for ${lpparam.packageName}")
        
        // Hook System.currentTimeMillis()
        XposedHelpers.findAndHookMethod(
            "java.lang.System",
            lpparam.classLoader,
            "currentTimeMillis",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    prefs.reload()
                    val currentCustomTimeMillis = prefs.getLong("custom_time_millis", customTimeMillis)
                    param.result = roundedTimeMillis
                }
            }
        )

        val dateClass = XposedHelpers.findClass("java.util.Date", lpparam.classLoader)
        XposedBridge.hookAllConstructors(dateClass, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                prefs.reload()
                val currentCustomTimeMillis = prefs.getLong("custom_time_millis", customTimeMillis)
                XposedHelpers.callMethod(param.thisObject, "setTime", currentCustomTimeMillis)
            }
        })
        
        XposedHelpers.findAndHookMethod(
            "java.util.Date",
            lpparam.classLoader,
            "getTime",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    prefs.reload()
                    val currentCustomTimeMillis = prefs.getLong("custom_time_millis", customTimeMillis)
                    param.result = roundedTimeMillis
                }
            }
        )
        
        // Hook Calendar.getInstance()
        XposedHelpers.findAndHookMethod(
            "java.util.Calendar",
            lpparam.classLoader,
            "getInstance",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    prefs.reload()
                    val currentCustomTimeMillis = prefs.getLong("custom_time_millis", customTimeMillis)
                    val calendar = param.result as Calendar
                    calendar.timeInMillis = roundedTimeMillis
                    param.result = calendar
                }
            }
        )
    }
}
