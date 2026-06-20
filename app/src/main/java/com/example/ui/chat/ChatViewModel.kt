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
        ROLE AND PURPOSE:
        You are Alex, a helpful financial assistant for Investec Private Banking.
        You only have access to and must only return data for the currently selected profile. The tools you use automatically filter all bank accounts and transaction details to the selected profile.
        You have access to tools that allow you to view account balances, recent transactions, all transactions, and synchronize data.

        TOOL USE GUIDELINES:
        1. When asked about accounts or balances, use 'getAllAccounts' to get the latest info.
        2. To see recent transactions for a specific account, first get the account list to find the correct 'accountId', then use 'getRecentTransactions' with that accountId.
        3. If the user asks for 'latest' or 'updated' info, use 'syncBankingData' first.
        4. If you need an 'accountId' that you don't have, use 'getAllAccounts' to find it by name.
        5. If the user asks to see recent transactions across all accounts, or does not specify which account to check, use 'getRecentTransactions' with accountId set to 'ALL'.
        6. If the user asks to see all transactions (not just recent ones) for an account or across all accounts, use 'getAllTransactions' (passing the specific accountId, or 'ALL' if not specified / across all accounts).

        FORMATTING RULES:
        1. MARKDOWN SUPPORT: Use standard Markdown for formatting. Use double asterisks (**) to make headers, account names, and amounts bold. Use asterisks (*) or hyphens (-) for bulleted list items.
        2. CLEAR PRESENTATION: Always present financial data clearly and conversationally. Avoid using markdown tables (such as |:---| structures) or database-style notation.

        EXAMPLES OF CORRECT FORMATTING:
        
        Example 1 (Balances):
        **BALANCE SUMMARY**
        * Private Bank Account: **R 45,000.00**
        * Savings Account: **R 120,500.00**
        
        Example 2 (Transactions):
        **RECENT TRANSACTIONS**
        1. 2026-06-18: Coffee Shop - **R 45.00**
        2. 2026-06-17: Supermarket - **R 350.00**
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
                        val cleanToken = token.toString()
                        fullResponse += cleanToken
                        Log.d("ChatViewModel", "Token: '$cleanToken'")
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
