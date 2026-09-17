package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TasbeehEntity
import com.example.ui.theme.GoldAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbeehScreen(
    tasbeehList: List<TasbeehEntity>,
    onIncrement: (TasbeehEntity) -> Unit,
    onReset: (Long) -> Unit,
    onAddCustom: (String, Int) -> Unit,
    onDeleteCustom: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedId by remember { mutableLongStateOf(0L) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val currentTasbeeh = tasbeehList.find { it.id == selectedId } ?: tasbeehList.firstOrNull()

    LaunchedEffect(tasbeehList) {
        if (selectedId == 0L && tasbeehList.isNotEmpty()) {
            selectedId = tasbeehList.first().id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "المسبحة الإلكترونية",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة ذكر مخصص")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Preset Dhikr Selector Horizontal Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasbeehList, key = { it.id }) { item ->
                    val isSelected = item.id == currentTasbeeh?.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedId = item.id },
                        label = {
                            Text(
                                text = item.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dhikr Title & Target info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currentTasbeeh?.title ?: "سبحان الله",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "الهدف الموصى به: ${currentTasbeeh?.target ?: 33} مرة",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Giant Bead / Circular Counter Button
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    )
                    .clickable {
                        currentTasbeeh?.let { onIncrement(it) }
                    }
                    .testTag("btn_tasbeeh_count"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${currentTasbeeh?.count ?: 0}",
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "اضغط للتسبيح",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Action Controls: Reset & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("btn_tasbeeh_reset")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تصفير العداد")
                }

                if (currentTasbeeh?.isCustom == true) {
                    IconButton(
                        onClick = { onDeleteCustom(currentTasbeeh.id) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف الذكر المخصص",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    // Reset Confirm Dialog
    if (showResetDialog && currentTasbeeh != null) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("تصفير العداد") },
            text = { Text("هل تريد إعادة عداد \"${currentTasbeeh.title}\" إلى الصفر؟") },
            confirmButton = {
                Button(onClick = {
                    onReset(currentTasbeeh.id)
                    showResetDialog = false
                }) {
                    Text("نعم، صفّر")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Add Custom Dhikr Dialog
    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var targetText by remember { mutableStateOf("100") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة ذكر مخصص") },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("نص الذكر") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it.filter { char -> char.isDigit() } },
                        label = { Text("العدد المستهدف (مثال: 33، 100)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val target = targetText.toIntOrNull() ?: 100
                            onAddCustom(title.trim(), target)
                            showAddDialog = false
                        }
                    },
                    enabled = title.isNotBlank()
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
