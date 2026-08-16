package com.example

import com.example.data.ai.ModelCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ModelDownloadManagerTest {

    @Test
    fun testModelCatalogContainsRequiredGemmaModels() {
        val models = ModelCatalog.ALL_MODELS
        assertEquals(2, models.size)

        val e2b = ModelCatalog.getById("gemma-4-e2b-it")
        assertNotNull(e2b)
        assertEquals("Gemma 4 (2B IT)", e2b.name)
        assertEquals("gemma-4-E2B-it.litertlm", e2b.fileName)
        assertEquals("2B", e2b.parameterCount)

        val e4b = ModelCatalog.getById("gemma-4-e4b-it")
        assertNotNull(e4b)
        assertEquals("Gemma 4 (4B IT)", e4b.name)
        assertEquals("gemma-4-E4B-it.litertlm", e4b.fileName)
        assertEquals("4B", e4b.parameterCount)
    }

    @Test
    fun testModelCatalogDownloadUrls() {
        val e2b = ModelCatalog.getById("gemma-4-e2b-it")
        assertEquals(
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
            e2b.downloadUrl
        )

        val e4b = ModelCatalog.getById("gemma-4-e4b-it")
        assertEquals(
            "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
            e4b.downloadUrl
        )
    }
}
