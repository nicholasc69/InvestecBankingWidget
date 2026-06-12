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
import com.example.data.repository.BankRepository
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.tool
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    application: Application,
    private val repository: BankRepository
) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val systemPrompt = """
        You are Alex, a helpful financial assistant for Investec Private Banking.
        You have access to tools that allow you to view account balances, recent transactions, and synchronize data.
        
        Guidelines:
        1. When asked about accounts or balances, use 'getAllAccounts' to get the latest info.
        2. To see transactions for a specific account, first get the account list to find the correct 'accountId', then use 'getRecentTransactions'.
        3. If the user asks for 'latest' or 'updated' info, use 'syncBankingData' first.
        4. Always present financial data clearly. Use bullet points or tables where appropriate.
        5. If you need an 'accountId' that you don't have, use 'getAllAccounts' to find it by name.
    """.trimIndent()
    
    val messages = mutableStateListOf<Message>()
    var inputText by mutableStateOf("")
    var isInitializing by mutableStateOf(true)
    var initializationError by mutableStateOf<String?>(null)

    private val engine: Engine by lazy {
        val engineConfig = EngineConfig(
            modelPath = "/data/local/tmp/gemma-4-E2B-it.litertlm",
            backend = Backend.CPU(),
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

                    processQuery("""Hello! Please introduce yourself to the client

 Your Name is Alex.
 
 Keep you introduction short to one paragraph describing the services you offer.
                    """.trimMargin(), isHidden = true)
                }
                isInitializing = false
            } catch (e: Exception) {
                isInitializing = false
                initializationError = e.message
                withContext(Dispatchers.Main) {
                    messages.add(Message(text = "Failed to initialize AI: ${e.message}", isUser = false))
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

        messages.add(Message(text = query, isUser = true))
        inputText = ""

        viewModelScope.launch {
            val botMessageIndex = messages.size
            messages.add(Message(text = "Thinking...", isUser = false))
            
            try {
                val history = messages.map {
                    when {
                        it.isSystem -> com.google.ai.edge.litertlm.Message.system(it.text)
                        it.isUser -> com.google.ai.edge.litertlm.Message.user(it.text)
                        else -> com.google.ai.edge.litertlm.Message.model(it.text)
                    }
                }

                val conversationConfig = ConversationConfig(
                    systemInstruction = Contents.of(systemPrompt),
                    initialMessages = history,
                    samplerConfig = SamplerConfig(topK = 10, topP = 0.95, temperature = 0.8),
                    tools = listOf(tool(BankingToolSet(repository = repository))),
                    automaticToolCalling = true
                )
                
                engine.createConversation(conversationConfig).use { conversation ->
                    var fullResponse = ""
                    var currentBotMessageIndex = botMessageIndex
                    conversation.sendMessageAsync(query).collect { responseMessage ->
                        // The library returns a Flow<Message>. We extract the text from the model's response.
                        val contentText = responseMessage.contents.contents
                            .filterIsInstance<com.google.ai.edge.litertlm.Content.Text>()
                            .joinToString("") { it.text }
                        
                        if (contentText.isNotEmpty()) {
                            fullResponse += contentText
                            withContext(Dispatchers.Main) {
                                if (currentBotMessageIndex < messages.size) {
                                    messages[currentBotMessageIndex] = Message(text = fullResponse, isUser = false)
                                } else {
                                    // Fallback in case list changed
                                    messages.add(Message(text = fullResponse, isUser = false))
                                    currentBotMessageIndex = messages.size - 1
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending message", e)
                withContext(Dispatchers.Main) {
                    messages[botMessageIndex] = Message(text = "Error: ${e.localizedMessage ?: "Unknown error"}", isUser = false)
                }
            }
        }
    }

    private fun processQuery(query: String, isHidden: Boolean = false) {
        viewModelScope.launch {
            if (!isHidden) {
                messages.add(Message(text = query, isUser = true))
            }

            val botMessageIndex = messages.size
            if (!isHidden) {
                messages.add(Message(text = "...", isUser = false))
            }

            try {
                val history = if (isHidden) {
                    messages.map {
                        when {
                            it.isSystem -> com.google.ai.edge.litertlm.Message.system(it.text)
                            it.isUser -> com.google.ai.edge.litertlm.Message.user(it.text)
                            else -> com.google.ai.edge.litertlm.Message.model(it.text)
                        }
                    }
                } else {
                    messages.dropLast(1).map { // Drop the "..." placeholder
                        when {
                            it.isSystem -> com.google.ai.edge.litertlm.Message.system(it.text)
                            it.isUser -> com.google.ai.edge.litertlm.Message.user(it.text)
                            else -> com.google.ai.edge.litertlm.Message.model(it.text)
                        }
                    }
                }

                val conversationConfig = ConversationConfig(
                    systemInstruction = Contents.of(systemPrompt),
                    initialMessages = history,
                    samplerConfig = SamplerConfig(topK = 10, topP = 0.95, temperature = 0.8),
                    tools = listOf(tool(BankingToolSet(repository = repository))),
                    automaticToolCalling = true
                )
                
                engine.createConversation(conversationConfig).use { conversation ->
                    var fullResponse = ""
                    var currentBotMessageIndex = if (!isHidden) botMessageIndex else -1

                    conversation.sendMessageAsync(query).collect { responseMessage ->
                        val contentText = responseMessage.contents.contents
                            .filterIsInstance<com.google.ai.edge.litertlm.Content.Text>()
                            .joinToString("") { it.text }
                        
                        if (contentText.isNotEmpty()) {
                            fullResponse += contentText
                            
                            withContext(Dispatchers.Main) {
                                if (!isHidden) {
                                    // Update existing placeholder
                                    if (currentBotMessageIndex != -1 && currentBotMessageIndex < messages.size) {
                                        messages[currentBotMessageIndex] = Message(text = fullResponse, isUser = false)
                                    }
                                } else if (responseMessage.role == com.google.ai.edge.litertlm.Role.MODEL) {
                                    // For hidden queries, add the model response as a single bubble once it starts coming
                                    if (currentBotMessageIndex == -1) {
                                        messages.add(Message(text = fullResponse, isUser = false))
                                        currentBotMessageIndex = messages.size - 1
                                    } else {
                                        messages[currentBotMessageIndex] = Message(text = fullResponse, isUser = false)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error in processQuery", e)
                if (!isHidden) {
                    withContext(Dispatchers.Main) {
                        messages[botMessageIndex] = Message(text = "Error: ${e.localizedMessage}", isUser = false)
                    }
                }
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
