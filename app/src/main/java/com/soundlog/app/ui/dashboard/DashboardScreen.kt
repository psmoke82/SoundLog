package com.soundlog.app.ui.dashboard

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundlog.app.SoundLogApp
import com.soundlog.app.automation.SongKeyNormalizer
import com.soundlog.app.data.local.entity.SongResultEntity
import com.soundlog.app.service.ForegroundSchedulerService
import com.soundlog.app.service.ShazamAccessibilityService
import com.soundlog.app.service.WatchdogManager
import com.soundlog.app.ui.theme.AccentGreen
import com.soundlog.app.ui.theme.AccentOrange
import com.soundlog.app.ui.theme.AccentRed
import com.soundlog.app.ui.theme.AccentYellow
import com.soundlog.app.ui.theme.CardBorder
import com.soundlog.app.ui.theme.DarkBackground
import com.soundlog.app.ui.theme.PrimaryNeon
import com.soundlog.app.ui.theme.SurfaceDark
import com.soundlog.app.ui.theme.SurfaceVariantDark
import com.soundlog.app.ui.theme.TextMuted
import com.soundlog.app.ui.theme.TextPrimary
import com.soundlog.app.ui.theme.TextSecondary
import com.soundlog.app.ui.theme.WordmarkFontFamily
import com.soundlog.app.util.AppChecklistHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(onNavigateToLogs: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = SoundLogApp.instance
    val settings = app.settingsRepository

    val statusState by app.database.serviceStatusDao().getStatusFlow()
        .collectAsState(initial = null)

    // 오늘 날짜 집계 쿼리는 SQLite가 로컬 날짜를 직접 판정하지만, Room의 Flow는 테이블이
    // 변경될 때만 재방출된다. 자정을 넘긴 직후 새 로그가 들어오기 전까지 어제 값이 남는 것을
    // 막기 위해, 로컬 날짜가 바뀌면 dayKey를 갱신해 Flow를 다시 구독한다.
    var dayKey by remember { mutableStateOf(java.time.LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000L)
            val now = java.time.LocalDate.now()
            if (now != dayKey) dayKey = now
        }
    }

    val todaySuccessCount by remember(dayKey) {
        app.database.executionLogDao().getTodaySuccessCountFlow()
    }.collectAsState(initial = 0)
    val todayNoMatchCount by remember(dayKey) {
        app.database.executionLogDao().getTodayNoMatchCountFlow()
    }.collectAsState(initial = 0)
    val todayFailureCount by remember(dayKey) {
        app.database.executionLogDao().getTodayFailureCountFlow()
    }.collectAsState(initial = 0)

    var isTestingRecognition by remember { mutableStateOf(false) }
    var isTestingTelegram by remember { mutableStateOf(false) }
    var serviceEnabled by remember { mutableStateOf(settings.isServiceEnabled) }

    // Live checklist items
    var checklistItems by remember { mutableStateOf(AppChecklistHelper.getChecklist(context)) }
    val initialAllPassed = remember(checklistItems) { checklistItems.all { it.isPassed } }
    var isChecklistExpanded by remember { mutableStateOf(!initialAllPassed) }

    fun refreshChecklist() {
        val updated = AppChecklistHelper.getChecklist(context)
        checklistItems = updated
        if (updated.all { it.isPassed }) {
            isChecklistExpanded = false
        }
    }

    // Auto re-check permissions when returning to app (ON_RESUME)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshChecklist()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val passedCount = checklistItems.count { it.isPassed }
    val totalCount = checklistItems.size
    val allPassed = passedCount == totalCount
    val isAccessibilityActive = ShazamAccessibilityService.isServiceRunning()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                PulseBarLogo()
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = TextPrimary)) { append("Sound") }
                        withStyle(SpanStyle(color = AccentYellow)) { append("Log") }
                    },
                    fontFamily = WordmarkFontFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Monitoring Power Toggle Card (중앙 대형 토글)
        MonitoringToggleCard(
            enabled = serviceEnabled,
            onToggle = { enabled ->
                serviceEnabled = enabled
                settings.isServiceEnabled = enabled
                if (enabled) {
                    ForegroundSchedulerService.startService(context)
                    Toast.makeText(context, "모니터링 작동 (ON)", Toast.LENGTH_SHORT).show()
                } else {
                    ForegroundSchedulerService.stopService(context)
                    Toast.makeText(context, "모니터링 중지 (OFF)", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // Checklist Card (체크리스트) - 모든 항목 통과 시 숨김 처리
        if (!allPassed) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AccentYellow, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isChecklistExpanded = !isChecklistExpanded }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FactCheck,
                                contentDescription = null,
                                tint = AccentYellow,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "앱 수행 필수 체크리스트",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "조치가 필요한 항목이 있습니다 ($passedCount/$totalCount 완료)",
                                    fontSize = 12.sp,
                                    color = AccentYellow
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    refreshChecklist()
                                    Toast.makeText(context, "체크리스트 상태가 갱신되었습니다.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "체크리스트 새로고침",
                                    tint = PrimaryNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { isChecklistExpanded = !isChecklistExpanded },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isChecklistExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isChecklistExpanded) {
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            Divider(color = SurfaceVariantDark, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            checklistItems.forEach { item ->
                                ChecklistItemRow(item = item, onRefresh = {
                                    val updated = AppChecklistHelper.getChecklist(context)
                                    checklistItems = updated
                                    if (updated.all { it.isPassed }) {
                                        isChecklistExpanded = false
                                    }
                                })
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
        }

        // Last Recognized Song Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = PrimaryNeon,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "최근 식별된 곡 정보",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val artist = statusState?.lastArtist
                val title = statusState?.lastTitle
                val lastSuccess = statusState?.lastSuccessAt ?: 0L

                if (!artist.isNullOrBlank() && !title.isNullOrBlank()) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNeon
                    )
                    Text(
                        text = artist,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "식별 시각: ${formatTime(lastSuccess)}",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                } else {
                    Text(
                        text = "아직 식별된 곡 정보가 없습니다.",
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                }
            }
        }

        // Today Statistics Row (3-Tier Traffic Light: Success, No Match, Failure)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "오늘 성공",
                value = "${todaySuccessCount}건",
                color = AccentGreen,
                icon = Icons.Default.CheckCircle,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToLogs() }
            )
            StatCard(
                title = "음악 미인식",
                value = "${todayNoMatchCount}건",
                color = AccentOrange,
                icon = Icons.Default.Info,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToLogs() }
            )
            StatCard(
                title = "동작 실패",
                value = "${todayFailureCount}건",
                color = AccentRed,
                icon = Icons.Default.Error,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToLogs() }
            )
        }

        // Utility Buttons
        Text(
            text = "관리자 유틸리티",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 8.dp)
        )

        Button(
            onClick = {
                val service = ShazamAccessibilityService.instance
                if (service == null) {
                    Toast.makeText(context, "접근성 서비스(SoundLog)를 먼저 활성화해주세요.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isTestingRecognition = true
                Toast.makeText(context, "Shazam 실시간 인식 테스트를 시작합니다...", Toast.LENGTH_SHORT).show()

                scope.launch(Dispatchers.IO) {
                    val watchdog = WatchdogManager(context)

                    service.startRecognitionFlow(settings.maxTimeoutSeconds) { result ->
                        scope.launch(Dispatchers.IO) {
                            try {
                                if (result.success && !result.artist.isNullOrBlank() && !result.title.isNullOrBlank()) {
                                    val artistName = result.artist
                                    val songTitle = result.title
                                    val songKey = SongKeyNormalizer.generateSongKey(artistName, songTitle)

                                    var albumArtUrl: String? = null
                                    var albumArtPath: String? = result.albumArtPath

                                    when (settings.albumArtOption) {
                                        com.soundlog.app.data.local.pref.EncryptedSettingsRepository.ALBUM_ART_ITUNES -> {
                                            albumArtUrl = com.soundlog.app.util.iTunesCoverArtFetcher.fetchCoverArtUrl(artistName, songTitle)
                                            albumArtPath = null
                                        }
                                        com.soundlog.app.data.local.pref.EncryptedSettingsRepository.ALBUM_ART_NONE -> {
                                            albumArtUrl = null
                                            albumArtPath = null
                                        }
                                        com.soundlog.app.data.local.pref.EncryptedSettingsRepository.ALBUM_ART_SHAZAM -> {
                                            albumArtUrl = null
                                        }
                                    }

                                    val youtubeUrl = if (settings.musicLinkOption == com.soundlog.app.data.local.pref.EncryptedSettingsRepository.MUSIC_LINK_YOUTUBE) {
                                        service.fetchYouTubeLinkFlow(artistName, songTitle)
                                    } else null

                                    val newSong = SongResultEntity(
                                        artist = artistName,
                                        title = songTitle,
                                        songKey = songKey,
                                        albumArtPath = albumArtPath,
                                        albumArtUrl = albumArtUrl,
                                        youtubeUrl = youtubeUrl
                                    )
                                    val sendSuccess = app.telegramQueueManager.enqueueAndSend(newSong)

                                    if (sendSuccess) {
                                        watchdog.recordSuccess(artistName, songTitle)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "인식 & 텔레그램 전송 성공: $artistName - $songTitle", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        watchdog.recordLog("TELEGRAM_QUEUE", "텔레그램 전송 실패 -> 대기 큐 저장")
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "곡 인식 성공 ($artistName - $songTitle) / 텔레그램 전송 실패", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    val failReason = result.errorMessage ?: "인식 실패"
                                    watchdog.recordFailure(failReason, isNoMatch = result.isNoMatch)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "인식 실패: $failReason", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } finally {
                                withContext(Dispatchers.Main) {
                                    isTestingRecognition = false
                                }
                            }
                        }
                    }
                }
            },
            enabled = !isTestingRecognition,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isTestingRecognition) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(" [ 지금 인식 테스트 ] ", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = {
                val token = settings.telegramBotToken
                val chatId = settings.telegramChatId
                if (token.isBlank() || chatId.isBlank()) {
                    Toast.makeText(context, "텔레그램 토큰 및 Chat ID를 먼저 설정해주세요.", Toast.LENGTH_SHORT).show()
                    return@OutlinedButton
                }

                isTestingTelegram = true
                scope.launch {
                    val result = app.telegramQueueManager.testConnection(token, chatId)
                    isTestingTelegram = false
                    result.fold(
                        onSuccess = { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        },
                        onFailure = { err ->
                            Toast.makeText(context, "오류: ${err.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            },
            enabled = !isTestingTelegram,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryNeon),
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryNeon),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isTestingTelegram) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PrimaryNeon)
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = PrimaryNeon)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("텔레그램 연동 테스트 메시지 발송", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MonitoringToggleCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (enabled) AccentGreen else CardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (enabled) "모니터링 작동 중" else "모니터링 중지됨",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) AccentGreen else TextSecondary
            )
            Text(
                text = if (enabled) "24시간 상시 음악 식별이 실행되고 있습니다." else "토글을 켜면 모니터링이 시작됩니다.",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            BigToggleSwitch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
fun BigToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val trackWidth = 84.dp
    val trackHeight = 44.dp
    val thumbSize = 36.dp
    val trackPadding = 4.dp
    val maxOffset = trackWidth - thumbSize - trackPadding * 2
    val thumbOffset by animateDpAsState(targetValue = if (checked) maxOffset else 0.dp, label = "toggleThumb")

    Box(
        modifier = Modifier
            .width(trackWidth)
            .height(trackHeight)
            .clip(RoundedCornerShape(trackHeight / 2))
            .background(if (checked) AccentGreen else SurfaceVariantDark)
            .border(1.dp, if (checked) AccentGreen else CardBorder, RoundedCornerShape(trackHeight / 2))
            .clickable { onCheckedChange(!checked) }
            .padding(trackPadding)
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (checked) Icons.Default.CheckCircle else Icons.Default.PowerSettingsNew,
                contentDescription = null,
                tint = if (checked) AccentGreen else AccentRed,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ChecklistItemRow(
    item: AppChecklistHelper.ChecklistItem,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (item.isPassed) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (item.isPassed) AccentGreen else AccentRed,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    text = item.description,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        if (!item.isPassed) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AccentRed.copy(alpha = 0.2f))
                    .border(1.dp, AccentRed, RoundedCornerShape(6.dp))
                    .clickable {
                        try {
                            item.onAction(context)
                        } catch (e: Exception) {
                            Toast
                                .makeText(context, "설정 화면을 열 수 없습니다.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("설정하기", fontSize = 11.sp, color = AccentRed, fontWeight = FontWeight.Bold)
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = AccentRed,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = modifier.border(1.dp, CardBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun PulseBarLogo(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pulseBarLogo")
    // 6개 바를 서로 다른 시작 지점에서 출발시켜 비대칭 VU 미터처럼 보이게 한다.
    val startOffsetsMs = listOf(0, 300, 700, 150, 550, 850)
    val centerIndex = (startOffsetsMs.size - 1) / 2f
    val barFractions = startOffsetsMs.mapIndexed { index, offsetMs ->
        // 중앙 바일수록 진폭이 크고 가장자리로 갈수록 진폭이 줄어들어
        // 움직임 전체가 마름모(다이아몬드) 윤곽을 그리도록 한다.
        val distanceFromCenter = kotlin.math.abs(index - centerIndex)
        val envelope = 1f - (distanceFromCenter / centerIndex) * 0.55f
        transition.animateFloat(
            initialValue = 0.22f * envelope,
            targetValue = 1f * envelope,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 550, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(offsetMs)
            ),
            label = "pulseBar$index"
        )
    }

    Row(
        modifier = modifier.height(28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        barFractions.forEachIndexed { index, animatedFraction ->
            val barColor = if (index % 2 == 1) PrimaryNeon else AccentYellow
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(28.dp)
                    // 콘셉트 이미지처럼 상하 중앙을 기준으로 늘었다 줄었다 하도록
                    // 바닥 정렬 대신 세로 스케일(중심 기준)을 사용한다.
                    .graphicsLayer { scaleY = animatedFraction.value }
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return "-"
    val sdf = SimpleDateFormat("MM-dd HH:mm:ss", Locale.KOREA)
    return sdf.format(Date(timestamp))
}
