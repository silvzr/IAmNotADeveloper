package xyz.xfqlittlefan.notdeveloper.xposed

import android.util.Log
import androidx.annotation.Keep
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
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
    private lateinit var currentLpparam: LoadPackageParam

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        this.currentLpparam = lpparam

        XposedBridge.log("$TAG: processing " + lpparam.packageName)

        // Hook per l'app principale
        if (lpparam.packageName == BuildConfig.APPLICATION_ID) {
            hookSafely0("xyz.xfqlittlefan.notdeveloper.xposed.ModuleStatusKt", "isModuleActive",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = true
                    }
                }
            )
            return
        }

        // Caricamento preferenze
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

        val roundedTimeMillis = (customTimeMillis / 1000) * 1000
        customTime.timeInMillis = roundedTimeMillis
        XposedBridge.log("$TAG: Using custom time: ${customTime.time} for ${lpparam.packageName}")

        // System.currentTimeMillis()
        hookSafely0("java.lang.System", "currentTimeMillis",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    param.result = roundedTimeMillis
                }
            }
        )

        // Date constructor
        try {
            val dateClass = XposedHelpers.findClass("java.util.Date", lpparam.classLoader)
            XposedBridge.hookAllConstructors(dateClass, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    XposedHelpers.callMethod(param.thisObject, "setTime", roundedTimeMillis)
                }
            })
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: Failed to hook Date constructors: ${t.message}")
        }

        // Date.getTime()
        hookSafely0("java.util.Date", "getTime",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    param.result = roundedTimeMillis
                }
            }
        )

        // Date.setTime()
        hookSafely1("java.util.Date", "setTime", Long::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.args[0] = roundedTimeMillis
                }
            }
        )

        // Hook per i metodi setter deprecati
        val deprecatedSetters = arrayOf("setHours", "setMinutes", "setSeconds", "setDate", "setMonth", "setYear")
        for (setter in deprecatedSetters) {
            hookSafely1("java.util.Date", setter, Int::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        XposedHelpers.callMethod(param.thisObject, "setTime", roundedTimeMillis)
                        param.result = null
                    }
                }
            )
        }

        // Calendar.getInstance()
        hookSafely0("java.util.Calendar", "getInstance",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val calendar = param.result as Calendar
                    calendar.timeInMillis = roundedTimeMillis
                    param.result = calendar
                }
            }
        )

        // Calendar.getTime()
        hookSafely0("java.util.Calendar", "getTime",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val date = param.result as Date
                    date.time = roundedTimeMillis
                    param.result = date
                }
            }
        )

        // Calendar.getTimeInMillis()
        hookSafely0("java.util.Calendar", "getTimeInMillis",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    param.result = roundedTimeMillis
                }
            }
        )

        // Calendar.setTimeInMillis()
        hookSafely1("java.util.Calendar", "setTimeInMillis", Long::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.args[0] = roundedTimeMillis
                }
            }
        )

        // set(int, int)
        hookSafely2("java.util.Calendar", "set", Int::class.java, Int::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    XposedHelpers.callMethod(param.thisObject, "setTimeInMillis", roundedTimeMillis)
                    param.result = null
                }
            }
        )

        // set(int, int, int)
        hookSafely3("java.util.Calendar", "set", Int::class.java, Int::class.java, Int::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    XposedHelpers.callMethod(param.thisObject, "setTimeInMillis", roundedTimeMillis)
                    param.result = null
                }
            }
        )	

	// Hook per GnssClock.getTimeNanos()
	hookSafely0("android.location.GnssClock", "getTimeNanos",
    	    object : XC_MethodHook() {
        	override fun afterHookedMethod(param: MethodHookParam) {
            	    param.result = roundedTimeMillis * 1_000_000 // Converti millis in nanos
                }
    	    }
	)

	// Hook per NetworkTimeUpdateService
	hookSafely0("com.android.server.NetworkTimeUpdateService", "getCurrentTimeMillis",
    	    object : XC_MethodHook() {
        	override fun afterHookedMethod(param: MethodHookParam) {
            	    param.result = roundedTimeMillis
        	}
    	    }
	)

	// Hook per SystemClock.currentGnssTimeClock() (API level 33+)
	try {
    	    hookSafely0("android.os.SystemClock", "currentGnssTimeClock",
        	object : XC_MethodHook() {
            	    override fun afterHookedMethod(param: MethodHookParam) {
                	val instant = XposedHelpers.callStaticMethod(
                    	    XposedHelpers.findClass("java.time.Instant", currentLpparam.classLoader),
                    	    "ofEpochMilli", roundedTimeMillis
                	)
                    	param.result = XposedHelpers.callStaticMethod(
                    	    XposedHelpers.findClass("java.time.Clock", currentLpparam.classLoader),
                    	    "ofInstant", instant,
                    	    XposedHelpers.callStaticMethod(
                                XposedHelpers.findClass("java.time.ZoneId", currentLpparam.classLoader),
                                "systemDefault"
                    	    )
                        )
            	    }
        	}
    	    )
	} catch (t: Throwable) {
    	    XposedBridge.log("$TAG: Failed to hook currentGnssTimeClock: ${t.message}")
	}

	// Hook per SystemClock.currentNetworkTimeClock() (API level 33+)
	try {
    	    hookSafely0("android.os.SystemClock", "currentNetworkTimeClock",
        	object : XC_MethodHook() {
            	    override fun afterHookedMethod(param: MethodHookParam) {
                	val instant = XposedHelpers.callStaticMethod(
                    	    XposedHelpers.findClass("java.time.Instant", currentLpparam.classLoader),
                    	    "ofEpochMilli", roundedTimeMillis
                	)
                	param.result = XposedHelpers.callStaticMethod(
                    	    XposedHelpers.findClass("java.time.Clock", currentLpparam.classLoader),
                    	    "ofInstant", instant,
                    	    XposedHelpers.callStaticMethod(
                        	XposedHelpers.findClass("java.time.ZoneId", currentLpparam.classLoader),
                        	"systemDefault"
                    	    )
                	)
            	    }
        	}
    	    )
	} catch (t: Throwable) {
    	    XposedBridge.log("$TAG: Failed to hook currentNetworkTimeClock: ${t.message}")
	}


        // Hook Java 8 Time API
        try {
            lpparam.classLoader.loadClass("java.time.LocalDateTime")
            
            // LocalDateTime.now()
            hookSafely0("java.time.LocalDateTime", "now",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val instant = XposedHelpers.callStaticMethod(
                            XposedHelpers.findClass("java.time.Instant", lpparam.classLoader),
                            "ofEpochMilli", roundedTimeMillis
                        )
                        param.result = XposedHelpers.callStaticMethod(
                            param.thisObject.javaClass,
                            "ofInstant", instant,
                            XposedHelpers.callStaticMethod(
                                XposedHelpers.findClass("java.time.ZoneId", lpparam.classLoader),
                                "systemDefault"
                            )
                        )
                    }
                }
            )
            
            // Altri hook per Java 8...
        } catch (e: Throwable) {
            XposedBridge.log("$TAG: Java 8 Time API not available: ${e.message}")
        }
    }
    
    // Funzioni hookSafely con numeri fissi di parametri
    private fun hookSafely0(className: String, methodName: String, callback: XC_MethodHook) {
        try {
            XposedHelpers.findAndHookMethod(className, currentLpparam.classLoader, methodName, callback)
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: Failed to hook $className.$methodName: ${t.message}")
        }
    }
    
    private fun hookSafely1(className: String, methodName: String, param1: Class<*>, callback: XC_MethodHook) {
        try {
            XposedHelpers.findAndHookMethod(className, currentLpparam.classLoader, methodName, param1, callback)
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: Failed to hook $className.$methodName: ${t.message}")
        }
    }
    
    private fun hookSafely2(className: String, methodName: String, param1: Class<*>, param2: Class<*>, callback: XC_MethodHook) {
        try {
            XposedHelpers.findAndHookMethod(className, currentLpparam.classLoader, methodName, param1, param2, callback)
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: Failed to hook $className.$methodName: ${t.message}")
        }
    }
    
    private fun hookSafely3(className: String, methodName: String, param1: Class<*>, param2: Class<*>, param3: Class<*>, callback: XC_MethodHook) {
        try {
            XposedHelpers.findAndHookMethod(className, currentLpparam.classLoader, methodName, param1, param2, param3, callback)
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: Failed to hook $className.$methodName: ${t.message}")
        }
    }
    
    // Aggiungere più funzioni se necessario per metodi con più parametri...
}
