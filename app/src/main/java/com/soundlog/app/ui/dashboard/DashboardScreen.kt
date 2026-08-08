package com.soundlog.app.ui.dashboard

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundlog.app.SoundLogApp
import com.soundlog.app.automation.SongKeyNormalizer
import com.soundlog.app.data.local.entity.SongResultEntity
import com.soundlog.app.service.ForegroundSchedulerService
import com.soundlog.app.service.ShazamAccessibilityService
import com.soundlog.app.service.WatchdogManager
import com.soundlog.app.ui.theme.AccentGreen
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
        // Header with Right Top Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SoundLog Dashboard",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "24시간 상시 음악 식별 & 텔레그램 공유",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // Top Right Monitoring Power Toggle Button
            Switch(
                checked = serviceEnabled,
                onCheckedChange = { enabled ->
                    serviceEnabled = enabled
                    settings.isServiceEnabled = enabled
                    if (enabled) {
                        ForegroundSchedulerService.startService(context)
                        Toast.makeText(context, "모니터링 작동 (ON)", Toast.LENGTH_SHORT).show()
                    } else {
                        ForegroundSchedulerService.stopService(context)
                        Toast.makeText(context, "모니터링 중지 (OFF)", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PrimaryNeon,
                    checkedTrackColor = SurfaceVariantDark
                )
            )
        }

        // Checklist Card (체크리스트)
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (allPassed) AccentGreen else AccentYellow,
                    RoundedCornerShape(16.dp)
                )
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
                            tint = if (allPassed) AccentGreen else AccentYellow,
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
                                text = if (allPassed) "모든 환경 및 권한 설정 완료 ($passedCount/$totalCount)"
                                else "조치가 필요한 항목이 있습니다 ($passedCount/$totalCount 완료)",
                                fontSize = 12.sp,
                                color = if (allPassed) AccentGreen else AccentYellow
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

        // Today Statistics Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "오늘 식별 성공",
                value = "${statusState?.todaySuccessCount ?: 0}건",
                color = AccentGreen,
                icon = Icons.Default.CheckCircle,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToLogs() }
            )
            StatCard(
                title = "오늘 식별 실패",
                value = "${statusState?.todayFailureCount ?: 0}건",
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

                                    val newSong = SongResultEntity(
                                        artist = artistName,
                                        title = songTitle,
                                        songKey = songKey
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
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 12.sp, color = TextSecondary)
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return "-"
    val sdf = SimpleDateFormat("MM-dd HH:mm:ss", Locale.KOREA)
    return sdf.format(Date(timestamp))
}
