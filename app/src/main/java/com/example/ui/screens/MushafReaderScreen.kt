package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.quran.MushafPage
import com.example.data.quran.MushafPageLine
import com.example.data.quran.MushafRepository
import com.example.data.quran.QuranRepository
import com.example.data.quran.TafseerRepository
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.QuranFontFamily
import kotlinx.coroutines.launch

/**
 * Reading Color Themes tailored for Holy Quran study.
 */
enum class MushafTheme(val titleAr: String) {
    CHAMOIS("ورق شاموا عاجي"),
    NIGHT("الوضع الليلي"),
    SEPIA("سيبيا دافئ")
}

data class MushafColorScheme(
    val background: Color,
    val text: Color,
    val frameBorder: Color,
    val innerFrame: Color,
    val goldAccent: Color,
    val headerFooter: Color,
    val ribbon: Color
)

fun getMushafColors(theme: MushafTheme): MushafColorScheme {
    return when (theme) {
        MushafTheme.CHAMOIS -> MushafColorScheme(
            background = Color(0xFFFAF7EE),
            text = Color(0xFF1B1D1A),
            frameBorder = Color(0xFFC7A868),
            innerFrame = Color(0xFFE4D5B7),
            goldAccent = Color(0xFFB38938),
            headerFooter = Color(0xFF5A665D),
            ribbon = Color(0xFFB71C1C)
        )
        MushafTheme.NIGHT -> MushafColorScheme(
            background = Color(0xFF121513),
            text = Color(0xFFECEFEA),
            frameBorder = Color(0xFF2C3E2D),
            innerFrame = Color(0xFF1E2820),
            goldAccent = Color(0xFFE5C158),
            headerFooter = Color(0xFF9EABA1),
            ribbon = Color(0xFFE5C158)
        )
        MushafTheme.SEPIA -> MushafColorScheme(
            background = Color(0xFFF3ECE0),
            text = Color(0xFF2D2319),
            frameBorder = Color(0xFF9E7D4E),
            innerFrame = Color(0xFFD8C7A8),
            goldAccent = Color(0xFF8B6428),
            headerFooter = Color(0xFF6B5843),
            ribbon = Color(0xFF8B0000)
        )
    }
}

/**
 * Screen that renders the Holy Quran page-by-page (604 pages)
 * matching the authentic Madani Mushaf layout with full Uthmani script and diacritics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MushafReaderScreen(
    initialPage: Int = 1,
    isDarkTheme: Boolean = false,
    onBackClick: () -> Unit,
    onSaveBookmark: (Int, String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val allPages = remember { MushafRepository.getAllPages() }
    val initialIndex = (initialPage - 1).coerceIn(0, 603)
    val pagerState = rememberPagerState(initialPage = initialIndex) { 604 }

    var selectedTheme by remember {
        mutableStateOf(if (isDarkTheme) MushafTheme.NIGHT else MushafTheme.CHAMOIS)
    }
    val colors = remember(selectedTheme) { getMushafColors(selectedTheme) }

    var isBarsVisible by remember { mutableStateOf(true) }
    var showIndexSheet by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var bookmarkedPage by remember { mutableStateOf<Int?>(null) }
    var fontScaleFactor by remember { mutableFloatStateOf(1.0f) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var selectedLineForAction by remember { mutableStateOf<MushafPageLine?>(null) }
    var showTafseerForLine by remember { mutableStateOf<MushafPageLine?>(null) }

    val currentPageNum = pagerState.currentPage + 1
    val currentPage = remember(currentPageNum, allPages) {
        if (currentPageNum in 1..allPages.size) allPages[currentPageNum - 1] else null
    }

    BackHandler {
        if (showTafseerForLine != null) {
            showTafseerForLine = null
        } else if (showIndexSheet) {
            showIndexSheet = false
        } else if (showJumpDialog) {
            showJumpDialog = false
        } else {
            onBackClick()
        }
    }

    LaunchedEffect(initialPage) {
        val targetIndex = (initialPage - 1).coerceIn(0, 603)
        if (pagerState.currentPage != targetIndex) {
            pagerState.scrollToPage(targetIndex)
        }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = isBarsVisible,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it }
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "المصحف الشريف",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            currentPage?.let { p ->
                                Text(
                                    text = "سورة ${p.surahName} • الجزء ${toArabicDigits(p.juzNumber)} • صفحة ${toArabicDigits(p.pageNumber)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    actions = {
                        // Bookmark button
                        IconButton(onClick = {
                            bookmarkedPage = currentPageNum
                            currentPage?.let { p ->
                                onSaveBookmark(p.pageNumber, p.surahName, p.surahNumber)
                                snackbarMessage = "تم حفظ الفاصلة عند صفحة ${toArabicDigits(p.pageNumber)} (سورة ${p.surahName})"
                            }
                        }) {
                            Icon(
                                imageVector = if (bookmarkedPage == currentPageNum) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "حفظ الفاصلة",
                                tint = if (bookmarkedPage == currentPageNum) GoldAccent else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Direct Page Jump
                        IconButton(onClick = { showJumpDialog = true }) {
                            Icon(imageVector = Icons.Default.FilterList, contentDescription = "انتقال لصفحة")
                        }

                        // Index Menu
                        IconButton(onClick = { showIndexSheet = true }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.MenuBook, contentDescription = "فهرس القرآن")
                        }

                        // Theme switch menu
                        Box {
                            IconButton(onClick = { showThemeMenu = true }) {
                                Icon(imageVector = Icons.Default.Palette, contentDescription = "نمط القراءة")
                            }
                            DropdownMenu(
                                expanded = showThemeMenu,
                                onDismissRequest = { showThemeMenu = false }
                            ) {
                                MushafTheme.entries.forEach { theme ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (selectedTheme == theme) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                }
                                                Text(theme.titleAr)
                                            }
                                        },
                                        onClick = {
                                            selectedTheme = theme
                                            showThemeMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Font scale
                        IconButton(onClick = {
                            fontScaleFactor = if (fontScaleFactor >= 1.15f) 0.9f else fontScaleFactor + 0.1f
                        }) {
                            Icon(imageVector = Icons.Default.FormatSize, contentDescription = "تغيير مقاس الخط")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isBarsVisible,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "صفحة ${toArabicDigits(currentPageNum)} من ٦٠٤",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            currentPage?.let { p ->
                                Text(
                                    text = "الجزء ${toArabicDigits(p.juzNumber)} • الحزب ${toArabicDigits(p.hizbNumber)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Slider(
                            value = currentPageNum.toFloat(),
                            onValueChange = { targetPage ->
                                coroutineScope.launch {
                                    pagerState.scrollToPage(targetPage.toInt() - 1)
                                }
                            },
                            valueRange = 1f..604f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        snackbarHost = {
            snackbarMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { snackbarMessage = null }) {
                            Text("حسناً")
                        }
                    }
                ) {
                    Text(msg)
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colors.background)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isBarsVisible = !isBarsVisible
                }
        ) {
            // Right-To-Left Pager for authentic Quran flipping
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val pageNum = pageIndex + 1
                    val pageData = if (pageNum in 1..allPages.size) allPages[pageNum - 1] else null
                    val isPageBookmarked = bookmarkedPage == pageNum

                    if (pageData != null) {
                        MushafPageView(
                            page = pageData,
                            fontScale = fontScaleFactor,
                            colors = colors,
                            isBookmarked = isPageBookmarked,
                            onLineClick = { line ->
                                if (line.type == "text" && line.text.isNotBlank()) {
                                    selectedLineForAction = line
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = colors.goldAccent)
                        }
                    }
                }
            }
        }
    }

    // Direct Jump Dialog
    if (showJumpDialog) {
        var pageInputText by remember { mutableStateOf(currentPageNum.toString()) }
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("الانتقال إلى صفحة") },
            text = {
                Column {
                    Text("أدخل رقم الصفحة بين ١ و ٦٠٤:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pageInputText,
                        onValueChange = { pageInputText = it.filter { ch -> ch.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val p = pageInputText.toIntOrNull()
                    if (p != null && p in 1..604) {
                        showJumpDialog = false
                        coroutineScope.launch {
                            pagerState.scrollToPage(p - 1)
                        }
                    }
                }) {
                    Text("انتقال")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Ayah Quick Actions Dialog
    selectedLineForAction?.let { line ->
        AlertDialog(
            onDismissRequest = { selectedLineForAction = null },
            title = {
                Text(
                    text = "سورة ${currentPage?.surahName ?: ""} • صفحة ${toArabicDigits(currentPageNum)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = line.text,
                        fontFamily = QuranFontFamily,
                        fontSize = 17.sp,
                        color = colors.text,
                        lineHeight = 26.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "خيارات الآية الكريمة:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(onClick = {
                        val targetLine = line
                        selectedLineForAction = null
                        showTafseerForLine = targetLine
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("التفسير الميسر")
                    }
                    TextButton(onClick = {
                        clipboardManager.setText(AnnotatedString(line.text))
                        snackbarMessage = "تم نسخ نص الآيات إلى الحافظة بنجاح"
                        selectedLineForAction = null
                    }) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ")
                    }
                    TextButton(onClick = {
                        val shareText = "${line.text}\n[سورة ${currentPage?.surahName ?: ""} - صفحة ${currentPageNum}]\nتطبيق نسك إسلامي"
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "مشاركة الآية الكريمة")
                        context.startActivity(shareIntent)
                        selectedLineForAction = null
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedLineForAction = null }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Comprehensive Tafseer Al-Muyassar Bottom Sheet
    showTafseerForLine?.let { line ->
        val ayahMatch = Regex("﴿([٠-٩0-9]+)﴾").find(line.text)
        val aNum = ayahMatch?.groupValues?.get(1)?.let { parseArabicDigits(it) } ?: 1
        val sNum = currentPage?.surahNumber ?: 1
        val sName = currentPage?.surahName ?: ""
        val intro = TafseerRepository.getSurahIntro(sNum)
        val tafseerText = TafseerRepository.getAyahTafseer(sNum, aNum, line.text, sName)

        ModalBottomSheet(
            onDismissRequest = { showTafseerForLine = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "سُورَةُ $sName",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${intro.typeAr} • عدد آياتها ${toArabicDigits(intro.versesCount)} • صفحة ${toArabicDigits(currentPageNum)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = colors.goldAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.goldAccent.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "التفسير الميسر",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldAccent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Card 1: Ayah Text in Uthmani Script
                Surface(
                    color = colors.background,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colors.frameBorder.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = line.text,
                            fontFamily = QuranFontFamily,
                            fontSize = 18.5.sp,
                            color = colors.text,
                            lineHeight = 29.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Card 2: Tafseer Explanation
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "بيان المعنى والهداية:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tafseerText,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Card 3: Surah Thematic Intro & Virtues
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "مقاصد وفضائل سورة $sName:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldAccent
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = intro.theme,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "فضائلها: ${intro.virtues}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val copyContent = "${line.text}\n[سورة $sName - صفحة $currentPageNum]\n\nالتفسير:\n$tafseerText\n\nتطبيق نسك إسلامي"
                            clipboardManager.setText(AnnotatedString(copyContent))
                            snackbarMessage = "تم نسخ الآية والتفسير بنجاح"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ التفسير")
                    }

                    Button(
                        onClick = {
                            val shareContent = "${line.text}\n[سورة $sName - صفحة $currentPageNum]\n\nالتفسير الميسر:\n$tafseerText\n\nتطبيق نسك إسلامي"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareContent)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة تفسير الآية"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showIndexSheet) {
        MushafIndexBottomSheet(
            onDismiss = { showIndexSheet = false },
            onSelectPage = { targetPage ->
                showIndexSheet = false
                coroutineScope.launch {
                    pagerState.scrollToPage(targetPage - 1)
                }
            }
        )
    }
}

/**
 * Single Mushaf Page View displaying the 15 lines within an authentic decorative border.
 */
@Composable
private fun MushafPageView(
    page: MushafPage,
    fontScale: Float,
    colors: MushafColorScheme,
    isBookmarked: Boolean,
    onLineClick: (MushafPageLine) -> Unit,
    modifier: Modifier = Modifier
) {
    val isOpeningPage = page.pageNumber == 1 || page.pageNumber == 2

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        // Decorative Dual Border Frame
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, colors.frameBorder, RoundedCornerShape(12.dp))
                .padding(2.5.dp)
                .border(1.dp, colors.innerFrame, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // Corner Ornaments Canvas
            IslamicCornerCanvas(
                borderColor = colors.frameBorder,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Page Header (Surah name & Juz/Hizb)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سُورَةُ ${page.surahName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.headerFooter,
                        fontFamily = QuranFontFamily
                    )
                    Text(
                        text = "الجُزْءُ ${toArabicDigits(page.juzNumber)} • الحِزْبُ ${toArabicDigits(page.hizbNumber)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.headerFooter,
                        fontFamily = QuranFontFamily
                    )
                }

                HorizontalDivider(
                    color = colors.innerFrame,
                    thickness = 0.5.dp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
                )

                // 2. The 15 Lines of the Madani Mushaf
                if (isOpeningPage) {
                    // Special Royal Illuminated Opening Page (Al-Fatiha or Al-Baqarah 1-5)
                    RoyalOpeningPageContent(
                        page = page,
                        fontScale = fontScale,
                        colors = colors,
                        onLineClick = onLineClick,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                } else {
                    // Standard 15-Line Grid
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        page.lines.forEach { line ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                when (line.type) {
                                    "surah_header" -> {
                                        SurahHeaderBanner(
                                            surahName = line.surahName,
                                            versesCount = line.versesCount,
                                            typeAr = line.typeAr,
                                            borderColor = colors.frameBorder,
                                            goldColor = colors.goldAccent,
                                            textColor = colors.text
                                        )
                                    }
                                    "bismillah" -> {
                                        Text(
                                            text = line.text,
                                            fontFamily = QuranFontFamily,
                                            fontSize = (17.5f * fontScale).sp,
                                            color = colors.text,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    "ornament" -> {
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                    "empty" -> {
                                        Spacer(modifier = Modifier.height(1.dp))
                                    }
                                    else -> {
                                        // Standard Quran Text Line
                                        val lineLen = line.text.length
                                        val baseSize = when {
                                            lineLen > 105 -> 13.5f
                                            lineLen > 90 -> 14.5f
                                            lineLen > 75 -> 15.5f
                                            else -> 16.5f
                                        }
                                        val lineFontSize = (baseSize * fontScale).sp

                                        val annotatedLine = remember(line.text, colors.text, colors.goldAccent) {
                                            buildQuranLineWithColoredAyahMarkers(
                                                text = line.text,
                                                textColor = colors.text,
                                                ayahColor = colors.goldAccent
                                            )
                                        }

                                        Text(
                                            text = annotatedLine,
                                            fontFamily = QuranFontFamily,
                                            fontSize = lineFontSize,
                                            lineHeight = (lineFontSize.value * 1.55f).sp,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Clip,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onLineClick(line) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = colors.innerFrame,
                    thickness = 0.5.dp,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                )

                // 3. Page Footer (Page Number in floral medallion)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PageNumberMedallion(
                        pageNumber = page.pageNumber,
                        borderColor = colors.frameBorder,
                        textColor = colors.headerFooter
                    )
                }
            }
        }

        // Hanging Satin Ribbon Bookmark
        if (isBookmarked) {
            RibbonBookmark(
                ribbonColor = colors.ribbon,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 24.dp)
            )
        }
    }
}

/**
 * Royal opening layout for Page 1 (Al-Fatiha) and Page 2 (Al-Baqarah 1-5).
 */
@Composable
private fun RoyalOpeningPageContent(
    page: MushafPage,
    fontScale: Float,
    colors: MushafColorScheme,
    onLineClick: (MushafPageLine) -> Unit,
    modifier: Modifier = Modifier
) {
    val surahHeader = page.lines.firstOrNull { it.type == "surah_header" }
    val bismillah = page.lines.firstOrNull { it.type == "bismillah" }
    val textLines = page.lines.filter { it.type == "text" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        surahHeader?.let { h ->
            SurahHeaderBanner(
                surahName = h.surahName,
                versesCount = h.versesCount,
                typeAr = h.typeAr,
                borderColor = colors.frameBorder,
                goldColor = colors.goldAccent,
                textColor = colors.text
            )
        }

        bismillah?.let { b ->
            Text(
                text = b.text,
                fontFamily = QuranFontFamily,
                fontSize = (18f * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = colors.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Surface(
            color = colors.goldAccent.copy(alpha = 0.05f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.innerFrame),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                textLines.forEach { line ->
                    val annotatedLine = remember(line.text, colors.text, colors.goldAccent) {
                        buildQuranLineWithColoredAyahMarkers(
                            text = line.text,
                            textColor = colors.text,
                            ayahColor = colors.goldAccent
                        )
                    }
                    Text(
                        text = annotatedLine,
                        fontFamily = QuranFontFamily,
                        fontSize = (17.5f * fontScale).sp,
                        lineHeight = (27f * fontScale).sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLineClick(line) }
                    )
                }
            }
        }
    }
}

/**
 * Builds an AnnotatedString coloring the Ayah markers ﴿١﴾ with gold accent.
 */
private fun buildQuranLineWithColoredAyahMarkers(
    text: String,
    textColor: Color,
    ayahColor: Color
): AnnotatedString {
    val regex = Regex("﴿[٠-٩0-9]+﴾")
    val builder = AnnotatedString.Builder()
    var currentIndex = 0

    val matches = regex.findAll(text)
    for (match in matches) {
        if (match.range.first > currentIndex) {
            builder.append(text.substring(currentIndex, match.range.first))
        }
        builder.pushStyle(SpanStyle(color = ayahColor, fontWeight = FontWeight.Bold))
        builder.append(match.value)
        builder.pop()
        currentIndex = match.range.last + 1
    }

    if (currentIndex < text.length) {
        builder.append(text.substring(currentIndex))
    }

    builder.addStyle(SpanStyle(color = textColor), 0, builder.length)
    return builder.toAnnotatedString()
}

/**
 * Authentic decorative header banner displayed at the beginning of each Surah in the Mushaf.
 */
@Composable
private fun SurahHeaderBanner(
    surahName: String,
    versesCount: Int,
    typeAr: String,
    borderColor: Color,
    goldColor: Color,
    textColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        color = goldColor.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${toArabicDigits(versesCount)} آية",
                style = MaterialTheme.typography.labelSmall,
                color = goldColor,
                fontWeight = FontWeight.Bold,
                fontFamily = QuranFontFamily
            )
            Text(
                text = "سُورَةُ $surahName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontFamily = QuranFontFamily,
                textAlign = TextAlign.Center
            )
            Text(
                text = typeAr,
                style = MaterialTheme.typography.labelSmall,
                color = goldColor,
                fontWeight = FontWeight.Bold,
                fontFamily = QuranFontFamily
            )
        }
    }
}

/**
 * Authentic floral medallion for page number at the bottom of the page.
 */
@Composable
private fun PageNumberMedallion(
    pageNumber: Int,
    borderColor: Color,
    textColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(vertical = 1.dp)
    ) {
        Text(
            text = "۞",
            fontSize = 10.sp,
            color = borderColor.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        Surface(
            color = borderColor.copy(alpha = 0.12f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor.copy(alpha = 0.5f))
        ) {
            Text(
                text = toArabicDigits(pageNumber),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontFamily = QuranFontFamily,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
            )
        }
        Text(
            text = "۞",
            fontSize = 10.sp,
            color = borderColor.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}

/**
 * Hanging Satin Ribbon Bookmark.
 */
@Composable
private fun RibbonBookmark(
    ribbonColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(16.dp)
            .height(44.dp)
            .shadow(4.dp, shape = RoundedCornerShape(2.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val notchH = 10.dp.toPx()
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, h)
                lineTo(w / 2f, h - notchH)
                lineTo(0f, h)
                close()
            }
            drawPath(path, color = ribbonColor)
        }
    }
}

/**
 * Canvas drawing delicate corner accents on the page frame.
 */
@Composable
private fun IslamicCornerCanvas(
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cornerSize = 10.dp.toPx()
        val w = size.width
        val h = size.height
        val alphaColor = borderColor.copy(alpha = 0.35f)

        // Top Left
        drawLine(alphaColor, Offset(0f, cornerSize), Offset(cornerSize, 0f), strokeWidth = 1.dp.toPx())
        // Top Right
        drawLine(alphaColor, Offset(w - cornerSize, 0f), Offset(w, cornerSize), strokeWidth = 1.dp.toPx())
        // Bottom Left
        drawLine(alphaColor, Offset(0f, h - cornerSize), Offset(cornerSize, h), strokeWidth = 1.dp.toPx())
        // Bottom Right
        drawLine(alphaColor, Offset(w - cornerSize, h), Offset(w, h - cornerSize), strokeWidth = 1.dp.toPx())
    }
}

/**
 * Bottom Sheet containing Surah index and Juz index for instant jumping to any page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MushafIndexBottomSheet(
    onDismiss: () -> Unit,
    onSelectPage: (Int) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Surahs, 1: Juz
    var searchQuery by remember { mutableStateOf("") }

    val filteredSurahs = remember(searchQuery) {
        if (searchQuery.isBlank()) QuranRepository.SURAHS
        else {
            val q = searchQuery.trim().lowercase()
            QuranRepository.SURAHS.filter {
                it.nameAr.contains(q) || it.number.toString() == q
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "فهرس المصحف الشريف",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("السور (١١٤)") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("الأجزاء (٣٠)") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن سورة أو رقمها...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredSurahs.size) { idx ->
                        val surah = filteredSurahs[idx]
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = "سورة ${surah.nameAr}",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = "${surah.typeAr} • ${toArabicDigits(surah.versesCount)} آية • الجزء ${toArabicDigits(surah.juz)}"
                                )
                            },
                            leadingContent = {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = toArabicDigits(surah.number),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                Surface(
                                    color = GoldAccent.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "ص ${toArabicDigits(surah.page)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            },
                            modifier = Modifier.clickable {
                                onSelectPage(surah.page)
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            } else {
                val juzStarts = listOf(
                    1 to "الجزء الأول (الم)",
                    22 to "الجزء الثاني (سيقول السفهاء)",
                    42 to "الجزء الثالث (تلك الرسل)",
                    62 to "الجزء الرابع (لن تنالوا البر)",
                    82 to "الجزء الخامس (والمحصنات)",
                    102 to "الجزء السادس (لا يحب الله)",
                    122 to "الجزء السابع (وإذا سمعوا)",
                    142 to "الجزء الثامن (ولو أننا)",
                    162 to "الجزء التاسع (قال الملأ)",
                    182 to "الجزء العاشر (واعلموا)",
                    202 to "الجزء الحادي عشر (يعتذرون)",
                    222 to "الجزء الثاني عشر (وما من دابة)",
                    242 to "الجزء الثالث عشر (وما أبرئ نفسي)",
                    262 to "الجزء الرابع عشر (ربما)",
                    282 to "الجزء الخامس عشر (سبحان الذي)",
                    302 to "الجزء السادس عشر (قال ألم)",
                    322 to "الجزء السابع عشر (اقترب للناس)",
                    342 to "الجزء الثامن عشر (قد أفلح)",
                    362 to "الجزء التاسع عشر (وقال الذين)",
                    382 to "الجزء العشرون (أمن خلق)",
                    402 to "الجزء الحادي والعشرون (اتل ما أوحي)",
                    422 to "الجزء الثاني والعشرون (ومن يقنت)",
                    442 to "الجزء الثالث والعشرون (وما أنزلنا)",
                    462 to "الجزء الرابع والعشرون (فمن أظلم)",
                    482 to "الجزء الخامس والعشرون (إليه يرد)",
                    502 to "الجزء السادس والعشرون (حم)",
                    522 to "الجزء السابع والعشرون (قال فما خطبكم)",
                    542 to "الجزء الثامن والعشرون (قد سمع الله)",
                    562 to "الجزء التاسع والعشرون (تبارك الذي)",
                    582 to "الجزء الثلاثون (عمّ يتساءلون)"
                )

                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(juzStarts.size) { idx ->
                        val (page, title) = juzStarts[idx]
                        val juzNum = idx + 1
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            supportingContent = {
                                Text("يبدأ من صفحة ${toArabicDigits(page)}")
                            },
                            leadingContent = {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = toArabicDigits(juzNum),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                Surface(
                                    color = GoldAccent.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "ص ${toArabicDigits(page)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            },
                            modifier = Modifier.clickable {
                                onSelectPage(page)
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

/**
 * Helper to convert western digits to authentic Arabic-Indic digits (١, ٢, ٣...).
 */
private fun toArabicDigits(number: Int): String {
    val arabicChars = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val str = number.toString()
    val sb = StringBuilder()
    for (ch in str) {
        if (ch in '0'..'9') {
            sb.append(arabicChars[ch - '0'])
        } else {
            sb.append(ch)
        }
    }
    return sb.toString()
}

/**
 * Helper to parse authentic Arabic-Indic digits (١, ٢, ٣...) to standard Int.
 */
private fun parseArabicDigits(str: String): Int {
    val western = str.map { ch ->
        when (ch) {
            '٠' -> '0'
            '١' -> '1'
            '٢' -> '2'
            '٣' -> '3'
            '٤' -> '4'
            '٥' -> '5'
            '٦' -> '6'
            '٧' -> '7'
            '٨' -> '8'
            '٩' -> '9'
            else -> ch
        }
    }.joinToString("")
    return western.toIntOrNull() ?: 1
}

