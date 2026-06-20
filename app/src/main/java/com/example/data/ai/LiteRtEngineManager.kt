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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteRtEngineManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var engineDeferred: Deferred<Engine>? = null

    init {
        // Eagerly start initialization when the app starts
        startInitialization()
    }

    @Synchronized
    fun startInitialization(): Deferred<Engine> {
        val existing = engineDeferred
        if (existing != null) return existing

        val deferred = scope.async {
            Engine.setNativeMinLogSeverity(LogSeverity.VERBOSE)
            Log.d("LiteRtEngineManager", "Eagerly initializing LiteRT-LM Engine...")
            
            // Optimize CPU backend by setting the thread count.
            // Let's use 4 threads as a balanced default for mobile CPUs.
            val config = EngineConfig(
                modelPath = "/data/local/tmp/gemma-4-E2B-it.litertlm",
                backend = Backend.CPU(numOfThreads = 4),
//                backend = Backend.GPU(),
                cacheDir = context.cacheDir.absolutePath
            )
            val newEngine = Engine(config)
            newEngine.initialize()
            Log.d("LiteRtEngineManager", "LiteRT-LM Engine initialized successfully!")
            newEngine
        }
        engineDeferred = deferred
        return deferred
    }

    suspend fun getEngine(): Engine {
        return startInitialization().await()
    }
}
