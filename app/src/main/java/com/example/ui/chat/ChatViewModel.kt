package com.example.ui.chat

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.BankingToolSet
import com.example.data.ai.LiteRtEngineManager
import com.example.data.ai.ModelDownloadManager
import com.example.data.ai.ModelInfo
import com.example.data.repository.BankRepository
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.tool
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    application: Application,
    private val repository: BankRepository,
    private val engineManager: LiteRtEngineManager,
    val downloadManager: ModelDownloadManager
) : AndroidViewModel(application) {

    private val systemPrompt: String by lazy {
        try {
            getApplication<Application>().assets.open("system_prompt.txt").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error loading system_prompt.txt from assets", e)
            ""
        }
    }

    val messages = mutableStateListOf<Message>()
    var inputText by mutableStateOf("")
    var isInitializing by mutableStateOf(true)
    var initializationError by mutableStateOf<String?>(null)
    var showModelManager by mutableStateOf(false)

    val selectedModelId: StateFlow<String> = downloadManager.selectedModelId
    val downloadStates = downloadManager.downloadStates

    private var engine: Engine? = null
    private var conversation: Conversation? = null

    init {
        initializeEngine()
    }

    fun openModelManager() {
        downloadManager.refreshAllModelStates()
        showModelManager = true
    }

    fun closeModelManager() {
        showModelManager = false
    }

    fun downloadModel(model: ModelInfo, customUrl: String? = null) {
        downloadManager.downloadModel(model, customUrl)
    }

    fun cancelDownload(modelId: String) {
        downloadManager.cancelDownload(modelId)
    }

    fun deleteModel(modelId: String) {
        downloadManager.deleteModel(modelId)
    }

    fun selectAndSwitchModel(modelId: String) {
        downloadManager.setSelectedModel(modelId)
        reinitializeEngine()
    }

    fun reinitializeEngine() {
        isInitializing = true
        initializationError = null
        try {
            conversation?.close()
        } catch (e: Exception) {
            Log.w("ChatViewModel", "Error closing old conversation: ${e.message}")
        }
        conversation = null
        engine = null

        viewModelScope.launch {
            try {
                val newEngine = engineManager.reloadEngine()
                engine = newEngine

                withContext(Dispatchers.IO) {
                    val convConfig = ConversationConfig(
                        systemInstruction = Contents.of(systemPrompt),
                        tools = listOf(tool(BankingToolSet(repository))),
                        automaticToolCalling = true
                    )
                    conversation = newEngine.createConversation(convConfig)
                }
                isInitializing = false
                withContext(Dispatchers.Main) {
                    val currentModelName = downloadManager.getSelectedModel().name
                    messages.add(
                        Message(
                            text = "Model changed to **$currentModelName**. AI Engine successfully initialized and ready to assist!",
                            isUser = false,
                            isSystem = true
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to re-initialize engine", e)
                isInitializing = false
                initializationError = e.message
                withContext(Dispatchers.Main) {
                    messages.add(
                        Message(
                            text = "Failed to load model: ${e.localizedMessage ?: e.message}. Click model icon to download model.",
                            isUser = false,
                            isSystem = true
                        )
                    )
                }
            }
        }
    }

    private fun initializeEngine() {
        Log.d("ChatViewModel", "Requesting LiteRT-LM Engine from LiteRtEngineManager")
        viewModelScope.launch {
            try {
                val newEngine = engineManager.getEngine()
                engine = newEngine

                withContext(Dispatchers.IO) {
                    val convConfig = ConversationConfig(
                        systemInstruction = Contents.of(systemPrompt),
                        tools = listOf(tool(BankingToolSet(repository))),
                        automaticToolCalling = true
                    )
                    conversation = newEngine.createConversation(convConfig)
                }
                isInitializing = false
                withContext(Dispatchers.Main) {
                    if (messages.isEmpty()) {
                        messages.add(
                            Message(
                                text = "Hello! I'm Alex, your professional Investec Private Banking financial advisor. I can help you analyze your accounts, view recent transaction histories, synchronize your banking data, or plan your payments. How can I assist you with your financial goals today?",
                                isUser = false
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to initialize engine", e)
                isInitializing = false
                initializationError = e.message
                withContext(Dispatchers.Main) {
                    messages.add(
                        Message(
                            text = "Model initialisation warning: ${e.localizedMessage ?: e.message}\nTap the Model button at the top to download gemma-4-e2b-it or gemma-4-e4b-it.",
                            isUser = false,
                            isSystem = true
                        )
                    )
                }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        inputText = text
    }

    fun sendMessage() {
        val query = inputText
        if (query.isBlank() || isInitializing) return

        val currentConversation = conversation ?: return

        messages.add(Message(text = query, isUser = true))
        inputText = ""

        viewModelScope.launch {
            val botMessageIndex = messages.size
            messages.add(Message(text = "", isUser = false))

            try {
                var fullResponse = ""
                var lastUiUpdateTime = 0L
                withContext(Dispatchers.IO) {
                    currentConversation.sendMessageAsync(query).collect { token ->
                        val cleanToken = token.toString()
                        fullResponse += cleanToken
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastUiUpdateTime >= 45L) {
                            lastUiUpdateTime = currentTime
                            val snapshot = fullResponse
                            withContext(Dispatchers.Main) {
                                if (botMessageIndex < messages.size) {
                                    messages[botMessageIndex] = Message(text = snapshot, isUser = false)
                                }
                            }
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    if (botMessageIndex < messages.size) {
                        messages[botMessageIndex] = Message(text = fullResponse, isUser = false)
                    }
                }
                Log.d("ChatViewModel", "Full response: '$fullResponse'")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending message", e)
                withContext(Dispatchers.Main) {
                    val errorMessage = "Error: ${e.localizedMessage ?: "Unknown error"}"
                    if (botMessageIndex < messages.size) {
                        messages[botMessageIndex] = Message(text = errorMessage, isUser = false)
                    } else {
                        messages.add(Message(text = errorMessage, isUser = false))
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            conversation?.close()
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error closing resources", e)
        }
    }
}
