package com.jarvis.assistant.audio

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.vosk.Model
import org.vosk.SpeakerModel
import java.io.File
import java.io.FileOutputStream

object VoskModelManager {
    private const val TAG = "VoskModelManager"

    private val _isModelReady = MutableStateFlow(false)
    val isModelReady: StateFlow<Boolean> = _isModelReady.asStateFlow()

    private val _isSpeakerModelReady = MutableStateFlow(false)
    val isSpeakerModelReady: StateFlow<Boolean> = _isSpeakerModelReady.asStateFlow()

    @Volatile
    var isModelAvailable: Boolean = false
        private set

    @Volatile
    var isSpeakerModelAvailable: Boolean = false
        private set

    @Volatile
    var model: Model? = null
        private set

    @Volatile
    var speakerModel: SpeakerModel? = null
        private set

    private val isInitializing = java.util.concurrent.atomic.AtomicBoolean(false)

    fun init(context: Context, scope: CoroutineScope) {
        if (model != null && speakerModel != null) return
        if (!isInitializing.compareAndSet(false, true)) return

        scope.launch(Dispatchers.IO) {
            try {
                // Check if acoustic model files exist in assets before attempting unpack/load
                val hasAcousticAsset = assetExists(context.assets, "model-en-us/am/final.mdl") ||
                        assetExists(context.assets, "model-en-us/final.mdl")

                if (!hasAcousticAsset) {
                    Log.i(TAG, "Offline Vosk speech model binary (final.mdl) is not packaged in assets. System fallback recognizer will be used.")
                    isModelAvailable = false
                    _isModelReady.value = false
                } else {
                    isModelAvailable = true
                    val targetDir = File(context.filesDir, "vosk-model")
                    val markerFile = File(targetDir, "am/final.mdl")
                    val rootMarkerFile = File(targetDir, "final.mdl")

                    if ((!markerFile.exists() || markerFile.length() == 0L) &&
                        (!rootMarkerFile.exists() || rootMarkerFile.length() == 0L)) {
                        Log.i(TAG, "Unpacking Vosk model to internal storage: ${targetDir.absolutePath}")
                        targetDir.deleteRecursively()
                        targetDir.mkdirs()
                        copyAssetFolder(context.assets, "model-en-us", targetDir)
                        Log.i(TAG, "Vosk model unpacked successfully.")
                    } else {
                        Log.i(TAG, "Vosk model already unpacked at: ${targetDir.absolutePath}")
                    }

                    if (markerFile.exists() || rootMarkerFile.exists()) {
                        try {
                            val loadedModel = Model(targetDir.absolutePath)
                            model = loadedModel
                            _isModelReady.value = true
                            Log.i(TAG, "Vosk Model ready in memory!")
                        } catch (e: Throwable) {
                            Log.w(TAG, "Failed to instantiate Vosk Model: ${e.message}")
                            _isModelReady.value = false
                        }
                    }
                }

                // Check if speaker recognition model (x-vector extractor) is in assets
                val hasSpeakerAsset = assetExists(context.assets, "model-spk/final.ext.raw")
                if (!hasSpeakerAsset) {
                    Log.i(TAG, "Offline Vosk speaker model binary (final.ext.raw) is not packaged in assets. Offline speaker verification will be skipped.")
                    isSpeakerModelAvailable = false
                    _isSpeakerModelReady.value = false
                } else {
                    isSpeakerModelAvailable = true
                    try {
                        val spkTargetDir = File(context.filesDir, "vosk-spk-model")
                        val spkMarkerFile = File(spkTargetDir, "final.ext.raw")

                        if (!spkMarkerFile.exists() || spkMarkerFile.length() == 0L) {
                            Log.i(TAG, "Unpacking Vosk Speaker model to: ${spkTargetDir.absolutePath}")
                            spkTargetDir.deleteRecursively()
                            spkTargetDir.mkdirs()
                            copyAssetFolder(context.assets, "model-spk", spkTargetDir)
                            Log.i(TAG, "Vosk Speaker model unpacked successfully.")
                        } else {
                            Log.i(TAG, "Vosk Speaker model already unpacked at: ${spkTargetDir.absolutePath}")
                        }

                        if (spkMarkerFile.exists()) {
                            val loadedSpk = SpeakerModel(spkTargetDir.absolutePath)
                            speakerModel = loadedSpk
                            _isSpeakerModelReady.value = true
                            Log.i(TAG, "Vosk SpeakerModel ready in memory!")
                        }
                    } catch (e: Throwable) {
                        Log.w(TAG, "Failed to load Vosk SpeakerModel: ${e.message}")
                        _isSpeakerModelReady.value = false
                    }
                }

                isInitializing.set(false)
            } catch (e: Throwable) {
                isInitializing.set(false)
                Log.w(TAG, "Error during Vosk initialization: ${e.message}")
            }
        }
    }

    private fun assetExists(assetManager: AssetManager, path: String): Boolean {
        return try {
            assetManager.open(path).use { true }
        } catch (_: Exception) {
            false
        }
    }

    private fun copyAssetFolder(assetManager: AssetManager, fromAssetPath: String, toDir: File) {
        val list = assetManager.list(fromAssetPath)
        if (list.isNullOrEmpty()) {
            try {
                assetManager.open(fromAssetPath).use { input ->
                    toDir.parentFile?.mkdirs()
                    FileOutputStream(toDir).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not copy asset $fromAssetPath: ${e.message}")
            }
        } else {
            toDir.mkdirs()
            for (item in list) {
                val childAsset = if (fromAssetPath.isEmpty()) item else "$fromAssetPath/$item"
                val childTarget = File(toDir, item)
                copyAssetFolder(assetManager, childAsset, childTarget)
            }
        }
    }
}
