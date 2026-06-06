package com.example.ui.chat

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val systemPrompt = "You are a helpful financial assistant."
    
    val messages = mutableStateListOf(
        Message(text = systemPrompt, isUser = false, isSystem = true)
    )
    var inputText by mutableStateOf("")
    var isInitializing by mutableStateOf(true)
    var initializationError by mutableStateOf<String?>(null)

    private val engine: Engine by lazy {
        val engineConfig = EngineConfig(
            modelPath = "/data/local/tmp/gemma-4-E2B-it.litertlm",
            backend = Backend.GPU(),
            cacheDir = context.cacheDir.absolutePath
        )
        Engine(engineConfig)
    }

    init {
        initializeEngine()
    }

    private fun initializeEngine() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    engine.initialize()
                }
                isInitializing = false
            } catch (e: Exception) {
                isInitializing = false
                initializationError = e.message
                messages.add(Message(text = "Failed to initialize AI: ${e.message}", isUser = false))
            }
        }
    }

    fun onInputTextChanged(text: String) {
        inputText = text
    }

    fun sendMessage() {
        val query = inputText
        if (query.isBlank() || isInitializing) return

        val history = messages.map {
            when {
                it.isSystem -> com.google.ai.edge.litertlm.Message.system(it.text)
                it.isUser -> com.google.ai.edge.litertlm.Message.user(it.text)
                else -> com.google.ai.edge.litertlm.Message.model(it.text)
            }
        }

        messages.add(Message(text = query, isUser = true))
        inputText = ""

        viewModelScope.launch {
            val botMessageIndex = messages.size
            messages.add(Message(text = "...", isUser = false))
            
            try {
                val conversationConfig = ConversationConfig(
                    systemInstruction = Contents.of(systemPrompt),
                    initialMessages = history,
                    samplerConfig = SamplerConfig(topK = 10, topP = 0.95, temperature = 0.8),
                )
                engine.createConversation(conversationConfig).use { conversation ->
                    var responseText = ""
                    conversation.sendMessageAsync(query).collect { chunk ->
                        responseText += chunk
                        messages[botMessageIndex] = Message(text = responseText, isUser = false)
                    }
                }
            } catch (e: Exception) {
                messages[botMessageIndex] = Message(text = "Error: ${e.message}", isUser = false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            engine.close()
        } catch (e: Exception) {
            // Log error
        }
    }
}
