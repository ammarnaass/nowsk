package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
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
import com.example.data.quran.Ayah
import com.example.data.quran.Surah
import com.example.ui.theme.GoldAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(
    surah: Surah,
    ayahs: List<Ayah>,
    fontSizeSp: Float,
    isFavorite: Boolean,
    onBackClick: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onToggleFavoriteSurah: () -> Unit,
    onBookmarkAyah: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showFontDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "سورة ${surah.nameAr}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${surah.typeAr} • ${surah.versesCount} آية • الجزء ${surah.juz}",
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
                    IconButton(onClick = { showFontDialog = true }) {
                        Icon(imageVector = Icons.Default.FormatSize, contentDescription = "تغيير حجم الخط")
                    }
                    IconButton(onClick = onToggleFavoriteSurah) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "إضافة للمفضلة",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        snackbarHost = {
            snackbarMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { snackbarMessage = null }) {
                            Text("إغلاق")
                        }
                    }
                ) {
                    Text(msg)
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Surah Header Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "سُورَةُ ${surah.nameAr}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${surah.nameEn} • ترتيبها: ${surah.number}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )

                        if (surah.number != 9 && surah.number != 1) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Verses List
            items(ayahs, key = { "${it.surahNumber}_${it.ayahNumber}" }) { ayah ->
                AyahCard(
                    ayah = ayah,
                    fontSizeSp = fontSizeSp,
                    onBookmarkClick = {
                        onBookmarkAyah(ayah.ayahNumber)
                        snackbarMessage = "تم حفظ العلامة عند الآية ${ayah.ayahNumber} من سورة ${surah.nameAr}"
                    },
                    onCopyClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Ayah", "${ayah.text} [${surah.nameAr}: ${ayah.ayahNumber}]")
                        clipboard.setPrimaryClip(clip)
                        snackbarMessage = "تم نسخ الآية إلى الحافظة"
                    },
                    onShareClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "﴿${ayah.text}﴾\n[سورة ${surah.nameAr} - الآية ${ayah.ayahNumber}]\nتطبيق نسكّر")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة الآية"))
                    }
                )
            }
        }
    }

    // Font Size Dialog
    if (showFontDialog) {
        var currentSize by remember { mutableFloatStateOf(fontSizeSp) }
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text("حجم خط القرآن") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                        fontSize = currentSize.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                    Slider(
                        value = currentSize,
                        onValueChange = { currentSize = it },
                        valueRange = 16f..36f,
                        steps = 10
                    )
                    Text(
                        text = "${currentSize.toInt()} sp",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onFontSizeChange(currentSize)
                    showFontDialog = false
                }) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFontDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun AyahCard(
    ayah: Ayah,
    fontSizeSp: Float,
    onBookmarkClick: () -> Unit,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Ayah Text with ornamental bracket
            Text(
                text = "${ayah.text} ﴿${ayah.ayahNumber}﴾",
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * 1.8f).sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Right,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBookmarkClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "حفظ موضع القراءة",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onCopyClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "نسخ",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onShareClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "مشاركة",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
