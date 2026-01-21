package ma.zer0ne.ocr.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ma.zer0ne.ocr.ui.theme.AppTheme
import ma.zer0ne.ocr.model.FileStatus
import ma.zer0ne.ocr.model.InvoiceFile
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListSection(
    files: List<Any>, // Can be List<InvoiceFile> or List<File>
    onRemove: (String) -> Unit,
    isProcessing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val listState = rememberLazyListState()

    // Animate the file count text
    val animatedFileCount by animateIntAsState(
        targetValue = files.size,
        animationSpec = tween(300),
        label = "fileCount"
    )

    // Track which items should be visible (for sequential hide animation)
    var visibleItems by remember { mutableStateOf(setOf<String>()) }

    // Initialize visible items when files change
    LaunchedEffect(files) {
        val ids = files.map {
            when (it) {
                is InvoiceFile -> it.id
                is File -> it.absolutePath
                else -> it.hashCode().toString()
            }
        }.toSet()
        visibleItems = ids
    }

    // Auto-scroll to the currently processing item
    LaunchedEffect(files) {
        if (isProcessing) {
            val processingIndex = files.indexOfFirst { file ->
                when (file) {
                    is InvoiceFile -> file.status == FileStatus.Processing
                    else -> false
                }
            }
            if (processingIndex >= 0) {
                listState.animateScrollToItem(processingIndex)
            }
        }
    }

    Column(modifier = modifier) {
        AnimatedVisibility(
            visible = files.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = "$animatedFileCount file${if (animatedFileCount != 1) "s" else ""}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
        }

        AnimatedVisibility(
            visible = files.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(500)) + shrinkVertically(animationSpec = tween(500))
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.bgMedium,
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(files, key = {
                        when (it) {
                            is InvoiceFile -> it.id
                            is File -> it.absolutePath
                            else -> it.hashCode()
                        }
                    }) { file ->
                        val itemId = when (file) {
                            is InvoiceFile -> file.id
                            is File -> file.absolutePath
                            else -> file.hashCode().toString()
                        }

                        var isVisible by remember { mutableStateOf(false) }

                        // Animate in on first appearance
                        LaunchedEffect(Unit) {
                            isVisible = true
                        }

                        AnimatedVisibility(
                            visible = isVisible && visibleItems.contains(itemId),
                            enter = fadeIn(animationSpec = tween(200)),
                            exit = fadeOut(animationSpec = tween(200)) +
                                   shrinkVertically(animationSpec = tween(200)),
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(200),
                                fadeOutSpec = tween(200),
                                placementSpec = tween(200)
                            )
                        ) {
                            when (file) {
                                is InvoiceFile -> FileItem(
                                    file = file,
                                    onRemove = { onRemove(file.id) },
                                    isProcessing = isProcessing
                                )
                                is File -> BasicFileItem(
                                    file = file,
                                    onRemove = { onRemove(file.absolutePath) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BasicFileItem(file: File, onRemove: () -> Unit) {
    val colors = AppTheme.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (file.path.endsWith(".pdf"))
                        Icons.Default.PictureAsPdf else Icons.Default.Description,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = file.name,
                    fontSize = 12.sp,
                    color = colors.text,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun FileItem(file: InvoiceFile, onRemove: () -> Unit, isProcessing: Boolean = false) {
    val colors = AppTheme.colors

    // Animation for processing spinner
    val rotation = remember { Animatable(0f) }

    // Animation for status icon scale (for success/error pop effect)
    val iconScale = remember { Animatable(1f) }

    // Track previous status to trigger animations
    var previousStatus by remember { mutableStateOf(file.status) }

    LaunchedEffect(file.status) {
        when (file.status) {
            FileStatus.Processing -> {
                rotation.animateTo(
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )
            }
            FileStatus.Completed, FileStatus.Error -> {
                // Pop animation when status changes to completed or error
                if (previousStatus == FileStatus.Processing) {
                    iconScale.snapTo(0.5f)
                    iconScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 0.4f,
                            stiffness = 400f
                        )
                    )
                }
                rotation.snapTo(0f)
            }
            else -> {
                rotation.snapTo(0f)
            }
        }
        previousStatus = file.status
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when (file.status) {
                    FileStatus.Processing -> colors.accent.copy(alpha = 0.08f)
                    FileStatus.Completed -> colors.success.copy(alpha = 0.08f)
                    FileStatus.Error -> colors.error.copy(alpha = 0.08f)
                    else -> Color.Transparent
                }
            ),
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // File type icon on the left
                Icon(
                    imageVector = if (file.path.endsWith(".pdf"))
                        Icons.Default.PictureAsPdf else Icons.Default.Description,
                    contentDescription = null,
                    tint = when (file.status) {
                        FileStatus.Processing -> colors.accent
                        FileStatus.Completed -> colors.success
                        FileStatus.Error -> colors.error
                        else -> colors.textSecondary
                    },
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = file.name,
                        fontSize = 12.sp,
                        color = colors.text,
                        fontWeight = FontWeight.Medium
                    )
                    if (file.status == FileStatus.Error) {
                        Text(
                            text = "Failed",
                            fontSize = 10.sp,
                            color = colors.error,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Animated status/action icon on the right
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                when (file.status) {
                    FileStatus.Processing -> {
                        // Spinning loader
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = "Processing",
                            tint = colors.accent,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(rotation.value)
                        )
                    }
                    FileStatus.Completed -> {
                        // Animated success checkmark
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = colors.success,
                            modifier = Modifier
                                .size(18.dp)
                                .scale(iconScale.value)
                        )
                    }
                    FileStatus.Error -> {
                        // Animated error icon
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = colors.error,
                            modifier = Modifier
                                .size(18.dp)
                                .scale(iconScale.value)
                        )
                    }
                    else -> {
                        // X button to remove file (only when pending)
                        IconButton(
                            onClick = onRemove,
                            enabled = !isProcessing,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
