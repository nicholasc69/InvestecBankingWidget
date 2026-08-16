package com.example.data.ai

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteRtEngineManager @Inject constructor(
    @ApplicationContext private val context: Context,
    val modelDownloadManager: ModelDownloadManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var engineDeferred: Deferred<Engine>? = null
    private var currentEngine: Engine? = null

    init {
        startInitialization()
    }

    @Synchronized
    fun startInitialization(): Deferred<Engine> {
        val existing = engineDeferred
        if (existing != null) return existing

        val deferred = scope.async {
            Engine.setNativeMinLogSeverity(LogSeverity.VERBOSE)
            
            val modelFile = modelDownloadManager.getSelectedModelFile()
            val selectedModel = modelDownloadManager.getSelectedModel()
            
            if (modelFile == null || !modelFile.exists()) {
                val errorMsg = "No model file found for '${selectedModel.name}'. Please download the model using the Model Manager."
                Log.e("LiteRtEngineManager", errorMsg)
                throw FileNotFoundException(errorMsg)
            }

            val availableCores = Runtime.getRuntime().availableProcessors()
            val optimalThreads = (availableCores - 2).coerceIn(4, 8)
            Log.d("LiteRtEngineManager", "Initializing LiteRT-LM Engine with model: ${modelFile.absolutePath} using $optimalThreads CPU threads (Detected $availableCores cores)")
            
            val config = EngineConfig(
                modelPath = modelFile.absolutePath,
                backend = Backend.CPU(numOfThreads = optimalThreads),
                cacheDir = context.cacheDir.absolutePath
            )
            val newEngine = Engine(config)
            newEngine.initialize()
            currentEngine = newEngine
            Log.d("LiteRtEngineManager", "LiteRT-LM Engine initialized successfully with ${selectedModel.name}!")
            newEngine
        }
        engineDeferred = deferred
        return deferred
    }

    suspend fun getEngine(): Engine {
        return startInitialization().await()
    }

    suspend fun reloadEngine(): Engine {
        Log.d("LiteRtEngineManager", "Reloading engine with updated model selection...")
        synchronized(this) {
            try {
                currentEngine?.close()
            } catch (e: Exception) {
                Log.w("LiteRtEngineManager", "Error closing old engine: ${e.message}")
            }
            currentEngine = null
            engineDeferred = null
        }
        return getEngine()
    }
}
