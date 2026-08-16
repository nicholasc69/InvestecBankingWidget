package com.example.data.ai

import java.io.File

data class ModelInfo(
    val id: String,
    val name: String,
    val fileName: String,
    val downloadUrl: String,
    val sizeFormatted: String,
    val parameterCount: String,
    val description: String
)

object ModelCatalog {
    val GEMMA_4_E2B = ModelInfo(
        id = "gemma-4-e2b-it",
        name = "Gemma 4 (2B IT)",
        fileName = "gemma-4-E2B-it.litertlm",
        downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
        sizeFormatted = "~1.5 GB",
        parameterCount = "2B",
        description = "Lightweight 2 billion parameter instruction-tuned model optimized for mobile CPUs & fast response times."
    )

    val GEMMA_4_E4B = ModelInfo(
        id = "gemma-4-e4b-it",
        name = "Gemma 4 (4B IT)",
        fileName = "gemma-4-E4B-it.litertlm",
        downloadUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
        sizeFormatted = "~3.2 GB",
        parameterCount = "4B",
        description = "High-capacity 4 billion parameter instruction-tuned model for superior reasoning and complex tasks."
    )

    val ALL_MODELS = listOf(GEMMA_4_E2B, GEMMA_4_E4B)

    fun getById(id: String): ModelInfo {
        return ALL_MODELS.find { it.id.equals(id, ignoreCase = true) } ?: GEMMA_4_E2B
    }
}

sealed class DownloadState {
    data object NotDownloaded : DownloadState()
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long
    ) : DownloadState()
    data class Downloaded(val file: File, val isAdbFallback: Boolean = false) : DownloadState()
    data class Error(val message: String) : DownloadState()
}
