package xyz.xfqlittlefan.notdeveloper

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.io.File
import java.io.RandomAccessFile

class SpotifyLibraryPatcher : IXposedHookLoadPackage {
    
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName != "com.spotify.music") return
        
        XposedBridge.log("Spotify Library Patcher: inizializzato")
        
        try {
            // Trova il file della libreria
            val appInfo = lpparam.appInfo
            val nativeLibDir = appInfo.nativeLibraryDir
            val libraryPath = "$nativeLibDir/liborbit-jni-spotify.so"
            val libraryFile = File(libraryPath)
            
            if (!libraryFile.exists()) {
                XposedBridge.log("File libreria non trovato: $libraryPath")
                return
            }
            
            XposedBridge.log("Libreria trovata: $libraryPath")
            
            // Controlla i permessi
            if (!libraryFile.canWrite()) {
                XposedBridge.log("Impossibile scrivere il file della libreria. Tentativo di renderlo scrivibile.")
                if (!libraryFile.setWritable(true)) {
                    XposedBridge.log("Impossibile rendere il file scrivibile.")
                    return
                }
            }
            
            // Definisci i replacement da applicare
            val replacements = listOf(
                Replacement(
                    "01 0a 2a 89 00 00 34",
                    "01 0a 2a 1f 20 03 d5"
                ),
                Replacement(
                    "94 1f 31 00 71 81 00 00 54",
                    "94 1f 31 00 71 04 00 00 14"
                ),
                Replacement(
                    "e1 01 00 54 20",
                    "0f 00 00 14 20"
                )
            )
            
            // Crea una copia di lavoro da patchare
            val workDir = File("${appInfo.dataDir}/patched_libs")
            if (!workDir.exists()) {
                workDir.mkdirs()
            }
            
            val workFile = File(workDir, "patched_liborbit-jni-spotify.so")
            libraryFile.copyTo(workFile, overwrite = true)
            workFile.setWritable(true)
            
            // Applica le patch
            val patchCount = applyPatches(workFile, replacements)
            
            if (patchCount > 0) {
                XposedBridge.log("Applicate con successo $patchCount patch")
                
                // Sostituisci il file originale con la versione patchata se possibile
                try {
                    workFile.copyTo(libraryFile, overwrite = true)
                    XposedBridge.log("File originale sostituito con versione patchata")
                } catch (e: Exception) {
                    XposedBridge.log("Impossibile sostituire la libreria originale: ${e.message}")
                }
            } else {
                XposedBridge.log("Nessuna patch applicata. Pattern non trovati.")
            }
        } catch (e: Exception) {
            XposedBridge.log("Errore durante il patching: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun applyPatches(file: File, replacements: List<Replacement>): Int {
        var patchCount = 0
        val raf = RandomAccessFile(file, "rw")
        
        try {
            // Leggi il contenuto del file
            val buffer = ByteArray(raf.length().toInt())
            raf.readFully(buffer)
            
            for (replacement in replacements) {
                // Converti le stringhe hex in array di byte
                val originalBytes = hexStringToByteArray(replacement.original)
                val replacementBytes = hexStringToByteArray(replacement.replacement)
                
                // Verifica che gli array abbiano la stessa lunghezza
                if (originalBytes.size != replacementBytes.size) {
                    XposedBridge.log("Errore: I pattern originali e sostitutivi devono avere la stessa lunghezza")
                    continue
                }
                
                // Trova tutte le occorrenze e applica le sostituzioni
                val positions = findAllPatterns(buffer, originalBytes)
                for (position in positions) {
                    raf.seek(position.toLong())
                    raf.write(replacementBytes)
                    patchCount++
                    
                    XposedBridge.log("Patch applicata alla posizione $position")
                }
            }
        } finally {
            raf.close()
        }
        
        return patchCount
    }
    
    private fun findAllPatterns(data: ByteArray, pattern: ByteArray): List<Int> {
        val positions = mutableListOf<Int>()
        
        for (i in 0 until data.size - pattern.size + 1) {
            var matches = true
            for (j in pattern.indices) {
                if (data[i + j] != pattern[j]) {
                    matches = false
                    break
                }
            }
            if (matches) {
                positions.add(i)
            }
        }
        
        return positions
    }
    
    private fun hexStringToByteArray(hex: String): ByteArray {
        val hexWithoutSpaces = hex.replace(" ", "")
        val result = ByteArray(hexWithoutSpaces.length / 2)
        
        for (i in result.indices) {
            val index = i * 2
            val j = hexWithoutSpaces.substring(index, index + 2).toInt(16)
            result[i] = j.toByte()
        }
        
        return result
    }
    
    data class Replacement(val original: String, val replacement: String)
}
