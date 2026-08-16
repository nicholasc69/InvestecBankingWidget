package com.example.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ai.DownloadState
import com.example.data.ai.ModelCatalog
import com.example.data.ai.ModelInfo
import java.util.Locale

@Composable
fun ModelManagerDialog(
    downloadStates: Map<String, DownloadState>,
    selectedModelId: String,
    onDismiss: () -> Unit,
    onDownloadModel: (ModelInfo, customUrl: String?) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteModel: (String) -> Unit,
    onSelectModel: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF3F4F9),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD6E3FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = Color(0xFF001B3E),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Gemma Model Manager",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1A1C1E)
                            )
                            Text(
                                text = "Google AI Edge LiteRT Models",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF44474E)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF44474E)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info banner about local files / adb
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E2EC))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF001B3E),
                            modifier = Modifier.size(18.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Download model directly via app or push `.litertlm` file via ADB to `/data/local/tmp/`.",
                            fontSize = 12.sp,
                            color = Color(0xFF1A1C1E)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // List of Gemma models
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(ModelCatalog.ALL_MODELS) { model ->
                        val state = downloadStates[model.id] ?: DownloadState.NotDownloaded
                        val isSelected = model.id == selectedModelId

                        ModelItemCard(
                            model = model,
                            downloadState = state,
                            isSelected = isSelected,
                            onDownload = { customUrl -> onDownloadModel(model, customUrl) },
                            onCancel = { onCancelDownload(model.id) },
                            onDelete = { onDeleteModel(model.id) },
                            onSelect = { onSelectModel(model.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Done Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF001B3E),
                        contentColor = Color.White
                    )
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ModelItemCard(
    model: ModelInfo,
    downloadState: DownloadState,
    isSelected: Boolean,
    onDownload: (customUrl: String?) -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit
) {
    var showCustomUrl by remember { mutableStateOf(false) }
    var customUrlText by remember { mutableStateOf("") }

    val cardBorder = if (isSelected) {
        BorderStroke(2.dp, Color(0xFF001B3E))
    } else {
        BorderStroke(1.dp, Color(0xFFC4C6D0))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFD6E3FF) else Color(0xFFFDFBFF)
        ),
        border = cardBorder
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Model Title and Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1A1C1E)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF001B3E))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = model.parameterCount,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF001B3E))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = model.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF44474E)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Size: ${model.sizeFormatted} • File: ${model.fileName}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF74777F)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // State specific UI
            when (downloadState) {
                is DownloadState.Downloaded -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF1B6C31),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (downloadState.isAdbFallback) "Ready (ADB)" else "Downloaded",
                                color = Color(0xFF1B6C31),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!downloadState.isAdbFallback) {
                                IconButton(
                                    onClick = onDelete,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFBA1A1A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            if (!isSelected) {
                                Button(
                                    onClick = onSelect,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF001B3E),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Use Model", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                is DownloadState.Downloading -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { downloadState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF001B3E),
                            trackColor = Color(0xFFC4C6D0)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val percentText = (downloadState.progress * 100).toInt()
                            val mbDownloaded = downloadState.downloadedBytes / (1024 * 1024)
                            val mbTotal = downloadState.totalBytes / (1024 * 1024)
                            val speedMb = downloadState.speedBytesPerSec / (1024f * 1024f)

                            val detailText = if (downloadState.totalBytes > 0) {
                                "$percentText% ($mbDownloaded MB / $mbTotal MB) • ${String.format(Locale.US, "%.1f", speedMb)} MB/s"
                            } else {
                                "$mbDownloaded MB downloaded • ${String.format(Locale.US, "%.1f", speedMb)} MB/s"
                            }

                            Text(
                                text = detailText,
                                fontSize = 11.sp,
                                color = Color(0xFF001B3E),
                                fontWeight = FontWeight.SemiBold
                            )

                            OutlinedButton(
                                onClick = onCancel,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFFBA1A1A)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFBA1A1A))
                            ) {
                                Text("Cancel", fontSize = 11.sp)
                            }
                        }
                    }
                }

                is DownloadState.NotDownloaded -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Not Downloaded",
                                fontSize = 12.sp,
                                color = Color(0xFF74777F)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { showCustomUrl = !showCustomUrl },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = "Custom URL",
                                        tint = Color(0xFF44474E),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                Button(
                                    onClick = { onDownload(customUrlText.takeIf { showCustomUrl }) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF001B3E),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download", fontSize = 12.sp)
                                }
                            }
                        }

                        AnimatedVisibility(visible = showCustomUrl) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                OutlinedTextField(
                                    value = customUrlText,
                                    onValueChange = { customUrlText = it },
                                    placeholder = { Text("Custom Download URL (Hugging Face / Mirror)", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF001B3E),
                                        unfocusedBorderColor = Color(0xFFC4C6D0)
                                    )
                                )
                            }
                        }
                    }
                }

                is DownloadState.Error -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Error: ${downloadState.message}",
                            fontSize = 11.sp,
                            color = Color(0xFFBA1A1A),
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { onDownload(customUrlText.takeIf { showCustomUrl }) },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF001B3E),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
