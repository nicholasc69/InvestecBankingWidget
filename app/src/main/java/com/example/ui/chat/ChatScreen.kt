package com.example.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp

data class Message(val text: String, val isUser: Boolean, val isSystem: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages = viewModel.messages
    val inputText = viewModel.inputText
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val backgroundLight = remember { Color(0xFFF3F4F9) }
    val textPrimary = remember { Color(0xFF1A1C1E) }
    val accentContainer = remember { Color(0xFFD6E3FF) }
    val accentOnContainer = remember { Color(0xFF001B3E) }

    Scaffold(
        modifier = modifier.background(backgroundLight),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(accentContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "A",
                                fontWeight = FontWeight.Bold,
                                color = accentOnContainer,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Alex",
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "AI Financial Assistant",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundLight,
                    titleContentColor = textPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundLight)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .verticalScrollbar(listState),
                state = listState
            ) {
                items(messages.filter { !it.isSystem && it.text.isNotBlank() }) { message ->
                    ChatBubble(message)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.onInputTextChanged(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...", color = textPrimary) },
                    enabled = !viewModel.isInitializing,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            viewModel.sendMessage()
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    ),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentOnContainer,
                        unfocusedBorderColor = Color(0xFFC4C6D0),
                        focusedContainerColor = Color(0xFFFDFBFF),
                        unfocusedContainerColor = Color(0xFFFDFBFF),
                        focusedTextColor = Color.Black
                    )
                )
                Button(
                    onClick = {
                        viewModel.sendMessage()
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.padding(start = 8.dp),
                    enabled = !viewModel.isInitializing && inputText.isNotBlank(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = accentContainer,
                        contentColor = accentOnContainer
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = accentOnContainer
                    )
                ) {
                    Text("Send", fontWeight = FontWeight.Bold, color = textPrimary)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message) {
    val alignment = when {
        message.isSystem -> Alignment.Center
        message.isUser -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }
    val color = when {
        message.isSystem -> MaterialTheme.colorScheme.surfaceVariant
        message.isUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val textColor = when {
        message.isSystem -> MaterialTheme.colorScheme.onSurfaceVariant
        message.isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp), contentAlignment = alignment) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = color,
            tonalElevation = if (message.isSystem) 0.dp else 2.dp
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                style = if (message.isSystem) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}

fun Modifier.verticalScrollbar(
    state: LazyListState,
    width: Dp = 5.dp
): Modifier = drawWithContent {
    drawContent()

    val layoutInfo = state.layoutInfo
    val visibleItemsInfo = layoutInfo.visibleItemsInfo
    val totalItemsCount = layoutInfo.totalItemsCount

    if (visibleItemsInfo.isNotEmpty() && totalItemsCount > visibleItemsInfo.size) {
        val firstVisibleElementIndex = visibleItemsInfo.first().index
        val visibleItemsCount = visibleItemsInfo.size

        val elementHeight = this.size.height / totalItemsCount
        val scrollbarOffsetY = firstVisibleElementIndex * elementHeight
        val scrollbarHeight = (visibleItemsCount * elementHeight).coerceAtLeast(24.dp.toPx())

        drawRoundRect(
            color = Color.Gray.copy(alpha = 0.5f),
            topLeft = Offset(this.size.width - width.toPx() - 2.dp.toPx(), scrollbarOffsetY),
            size = Size(width.toPx(), scrollbarHeight),
            cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2)
        )
    }
}
