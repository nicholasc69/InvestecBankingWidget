package com.example.data.ai

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences("model_manager_prefs", Context.MODE_PRIVATE)

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    private val _selectedModelId = MutableStateFlow(
        prefs.getString(KEY_SELECTED_MODEL, ModelCatalog.GEMMA_4_E2B.id) ?: ModelCatalog.GEMMA_4_E2B.id
    )
    val selectedModelId: StateFlow<String> = _selectedModelId.asStateFlow()

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val activeCalls = ConcurrentHashMap<String, okhttp3.Call>()

    init {
        refreshAllModelStates()
    }

    val modelsDir: File
        get() {
            val dir = File(context.getExternalFilesDir(null), "models")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    fun refreshAllModelStates() {
        val map = mutableMapOf<String, DownloadState>()
        for (model in ModelCatalog.ALL_MODELS) {
            val currentState = _downloadStates.value[model.id]
            if (currentState is DownloadState.Downloading) {
                map[model.id] = currentState
            } else {
                val file = getDownloadedFile(model)
                if (file != null) {
                    val isAdb = file.absolutePath.startsWith("/data/local/tmp")
                    map[model.id] = DownloadState.Downloaded(file, isAdbFallback = isAdb)
                } else {
                    map[model.id] = DownloadState.NotDownloaded
                }
            }
        }
        _downloadStates.value = map
    }

    fun getDownloadedFile(model: ModelInfo): File? {
        // 1. Check app external models directory
        val localFile = File(modelsDir, model.fileName)
        if (localFile.exists() && localFile.length() > 0) {
            return localFile
        }

        // 2. Check fallback ADB path /data/local/tmp/ ONLY in DEBUG builds
        if (com.example.BuildConfig.DEBUG) {
            val adbFile = File("/data/local/tmp/${model.fileName}")
            if (adbFile.exists() && adbFile.length() > 0) {
                return adbFile
            }

            val adbFileLower = File("/data/local/tmp/${model.fileName.lowercase()}")
            if (adbFileLower.exists() && adbFileLower.length() > 0) {
                return adbFileLower
            }
        }

        return null
    }

    private fun isValidDownloadUrl(url: String): Boolean {
        if (!url.startsWith("https://", ignoreCase = true)) return false
        val uri = android.net.Uri.parse(url)
        val host = uri.host?.lowercase() ?: return false
        return host == "huggingface.co" || host.endsWith(".huggingface.co") || host == "storage.googleapis.com"
    }

    fun getSelectedModel(): ModelInfo {
        return ModelCatalog.getById(_selectedModelId.value)
    }

    fun getSelectedModelFile(): File? {
        val selected = getSelectedModel()
        return getDownloadedFile(selected)
    }

    fun setSelectedModel(modelId: String) {
        prefs.edit().putString(KEY_SELECTED_MODEL, modelId).apply()
        _selectedModelId.value = modelId
        refreshAllModelStates()
    }

    fun downloadModel(model: ModelInfo, customUrl: String? = null) {
        if (activeJobs.containsKey(model.id)) return

        val targetUrl = customUrl?.takeIf { it.isNotBlank() } ?: model.downloadUrl
        if (!isValidDownloadUrl(targetUrl)) {
            val errorMsg = "Download rejected: Invalid or insecure URL '$targetUrl'. Download URLs must use HTTPS from trusted model hosts."
            Log.e(TAG, errorMsg)
            updateModelState(model.id, DownloadState.Error(errorMsg))
            return
        }

        Log.d(TAG, "Starting download for ${model.name} from $targetUrl")

        val job = scope.launch {
            val targetFile = File(modelsDir, model.fileName)
            val tmpFile = File(modelsDir, "${model.fileName}.tmp")

            try {
                updateModelState(model.id, DownloadState.Downloading(0f, 0L, 0L, 0L))

                val request = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                    .build()

                val call = okHttpClient.newCall(request)
                activeCalls[model.id] = call

                val response = withContext(Dispatchers.IO) { call.execute() }
                if (!response.isSuccessful) {
                    val msg = "Server returned code ${response.code}: ${response.message}"
                    Log.e(TAG, "Download failed: $msg")
                    updateModelState(model.id, DownloadState.Error(msg))
                    return@launch
                }

                val body = response.body
                val totalBytes = body.contentLength()
                var downloadedBytes = 0L
                val buffer = ByteArray(8192)
                var bytesRead: Int

                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(tmpFile)

                var startTime = System.currentTimeMillis()
                var bytesSinceLastMeasure = 0L
                var currentSpeed = 0L

                inputStream.use { input ->
                    outputStream.use { output ->
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            bytesSinceLastMeasure += bytesRead

                            val currentTime = System.currentTimeMillis()
                            val timeDelta = currentTime - startTime
                            if (timeDelta >= 500) { // Update speed twice per second
                                currentSpeed = (bytesSinceLastMeasure * 1000) / timeDelta
                                startTime = currentTime
                                bytesSinceLastMeasure = 0L
                            }

                            val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                            updateModelState(
                                model.id,
                                DownloadState.Downloading(
                                    progress = progress,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes,
                                    speedBytesPerSec = currentSpeed
                                )
                            )
                        }
                    }
                }

                if (tmpFile.exists() && tmpFile.length() > 0) {
                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                    tmpFile.renameTo(targetFile)
                    Log.d(TAG, "Successfully downloaded ${model.name} to ${targetFile.absolutePath}")
                    updateModelState(model.id, DownloadState.Downloaded(targetFile, isAdbFallback = false))
                    setSelectedModel(model.id)
                } else {
                    updateModelState(model.id, DownloadState.Error("Downloaded file is empty"))
                }
            } catch (e: Exception) {
                if (activeCalls[model.id]?.isCanceled() == true) {
                    Log.d(TAG, "Download cancelled for ${model.id}")
                    updateModelState(model.id, DownloadState.NotDownloaded)
                } else {
                    Log.e(TAG, "Error downloading ${model.id}", e)
                    updateModelState(model.id, DownloadState.Error(e.localizedMessage ?: "Download error"))
                }
                if (tmpFile.exists()) {
                    tmpFile.delete()
                }
            } finally {
                activeJobs.remove(model.id)
                activeCalls.remove(model.id)
            }
        }

        activeJobs[model.id] = job
    }

    fun cancelDownload(modelId: String) {
        activeCalls[modelId]?.cancel()
        activeJobs[modelId]?.cancel()
        activeCalls.remove(modelId)
        activeJobs.remove(modelId)
        
        val tmpFile = File(modelsDir, "${ModelCatalog.getById(modelId).fileName}.tmp")
        if (tmpFile.exists()) {
            tmpFile.delete()
        }
        updateModelState(modelId, DownloadState.NotDownloaded)
    }

    fun deleteModel(modelId: String) {
        val model = ModelCatalog.getById(modelId)
        val file = File(modelsDir, model.fileName)
        if (file.exists()) {
            file.delete()
        }
        refreshAllModelStates()
    }

    private fun updateModelState(modelId: String, state: DownloadState) {
        val map = _downloadStates.value.toMutableMap()
        map[modelId] = state
        _downloadStates.value = map
    }

    companion object {
        private const val TAG = "ModelDownloadManager"
        private const val KEY_SELECTED_MODEL = "key_selected_model_id"
    }
}
