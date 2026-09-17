package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.adhkar.AdhkarCategory
import com.example.data.adhkar.DhikrItem
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DhikrDetailScreen(
    category: AdhkarCategory,
    items: List<DhikrItem>,
    isItemFavorite: (String) -> Boolean,
    onToggleFavorite: (DhikrItem) -> Unit,
    onVibrate: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(0) }
    val countsMap = remember { mutableStateMapOf<String, Int>() }

    val currentItem = items.getOrElse(currentIndex) { items.firstOrNull() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = category.nameAr,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${currentIndex + 1} من ${items.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    currentItem?.let { item ->
                        val isFav = isItemFavorite(item.title)
                        IconButton(onClick = { onToggleFavorite(item) }) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "المفضلة",
                                tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (currentItem == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد أذكار في هذا القسم حالياً.")
            }
        } else {
            val count = countsMap[currentItem.id] ?: 0
            val target = currentItem.count
            val isCompleted = count >= target

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Progress Bar
                LinearProgressIndicator(
                    progress = { if (target > 0) (count.toFloat() / target).coerceIn(0f, 1f) else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Dhikr Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        item {
                            Text(
                                text = currentItem.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = currentItem.text,
                                style = MaterialTheme.typography.bodyLarge,
                                fontSize = 19.sp,
                                lineHeight = 34.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (currentItem.source.isNotBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = currentItem.source,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (currentItem.virtue.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "الفضل: ${currentItem.virtue}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Completion badge
                AnimatedVisibility(
                    visible = isCompleted,
                    enter = fadeIn() + scaleIn()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "أحسنت، تم إكمال الذكر",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Interactive Large Count Button
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            else MaterialTheme.colorScheme.primary
                        )
                        .clickable {
                            if (!isCompleted) {
                                countsMap[currentItem.id] = count + 1
                                onVibrate()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$count / $target",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isCompleted) "تم" else "تسبيح",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Navigation Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous
                    TextButton(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        enabled = currentIndex > 0
                    ) {
                        Text("السابق")
                    }

                    // Reset count
                    IconButton(onClick = { countsMap[currentItem.id] = 0 }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "إعادة التصفير")
                    }

                    // Share
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${currentItem.title}\n\n${currentItem.text}\n\nتطبيق نسكّر")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة الذكر"))
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "مشاركة")
                    }

                    // Next
                    Button(
                        onClick = {
                            if (currentIndex < items.size - 1) {
                                currentIndex++
                            }
                        },
                        enabled = currentIndex < items.size - 1,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("التالي")
                    }
                }

                // AdMob Banner Placement (إعلان البانر في شاشة الأذكار)
                com.example.ads.AdMobBannerView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
            }
        }
    }
}
