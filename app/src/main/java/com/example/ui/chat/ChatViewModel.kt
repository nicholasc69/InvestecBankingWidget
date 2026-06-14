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
import com.example.data.repository.BankRepository
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.tool
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    application: Application,
    private val repository: BankRepository,
    private val engineManager: LiteRtEngineManager
) : AndroidViewModel(application) {

    private val systemPrompt = """
        You are Alex, a helpful financial assistant for Investec Private Banking.
        You have access to tools that allow you to view account balances, recent transactions, all transactions, and synchronize data.
        
        Guidelines:
        1. When asked about accounts or balances, use 'getAllAccounts' to get the latest info.
        2. To see recent transactions for a specific account, first get the account list to find the correct 'accountId', then use 'getRecentTransactions' with that accountId.
        3. If the user asks for 'latest' or 'updated' info, use 'syncBankingData' first.
        4. Always present financial data clearly and conversationally. Use clean bullet points or lists. Avoid using markdown tables (such as |:---| structures) or database-style notation, as they are not user friendly.
        5. If you need an 'accountId' that you don't have, use 'getAllAccounts' to find it by name.
        6. If the user asks to see recent transactions across all accounts, or does not specify which account to check, use 'getRecentTransactions' with accountId set to 'ALL'.
        7. If the user asks to see all transactions (not just recent ones) for an account or across all accounts, use 'getAllTransactions' (passing the specific accountId, or 'ALL' if not specified / across all accounts).
    """.trimIndent()

    val messages = mutableStateListOf<Message>()
    var inputText by mutableStateOf("")
    var isInitializing by mutableStateOf(true)
    var initializationError by mutableStateOf<String?>(null)

    private var engine: Engine? = null
    private var conversation: Conversation? = null

    init {
        initializeEngine()
    }

    private fun initializeEngine() {
        Log.d("ChatViewModel", "Requesting LiteRT-LM Engine from LiteRtEngineManager")
        viewModelScope.launch {
            try {
                // Retrieve the pre-initialized or currently initializing engine
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
                                text = "Hello! I'm Alex, your Investec Private Banking assistant. How can I help you manage your accounts, view transactions, or execute payments today?",
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
                            text = "Failed to initialize AI: ${e.message}",
                            isUser = false
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
                withContext(Dispatchers.IO) {
                    currentConversation.sendMessageAsync(query).collect { token ->
                        fullResponse += token
                        Log.d("ChatViewModel", "Token: '$token'")
                        withContext(Dispatchers.Main) {
                            if (botMessageIndex < messages.size) {
                                messages[botMessageIndex] =
                                    Message(text = fullResponse, isUser = false)
                            }
                        }
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
            // Do NOT close engine as its lifecycle is managed by LiteRtEngineManager singleton
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error closing resources", e)
        }
    }
}
