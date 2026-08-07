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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundlog.app.SoundLogApp
import com.soundlog.app.service.ForegroundSchedulerService
import com.soundlog.app.service.ShazamAccessibilityService
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen() {
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

    // Re-check permissions on resume/re-render
    LaunchedEffect(Unit) {
        val updated = AppChecklistHelper.getChecklist(context)
        checklistItems = updated
        if (updated.all { it.isPassed }) {
            isChecklistExpanded = false
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SoundLog Dashboard",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "24시간 상시 음악 식별 & 텔레그램 공유",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isChecklistExpanded = !isChecklistExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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

                    Icon(
                        imageVector = if (isChecklistExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondary
                    )
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

        // Service Main Toggle Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (serviceEnabled && isAccessibilityActive) AccentGreen else AccentRed)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (serviceEnabled && isAccessibilityActive) "모니터링 작동" else "서비스 멈춤/대기",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                    }

                    Switch(
                        checked = serviceEnabled,
                        onCheckedChange = { enabled ->
                            serviceEnabled = enabled
                            settings.isServiceEnabled = enabled
                            if (enabled) {
                                ForegroundSchedulerService.startService(context)
                                Toast.makeText(context, "서비스가 시작되었습니다.", Toast.LENGTH_SHORT).show()
                            } else {
                                ForegroundSchedulerService.stopService(context)
                                Toast.makeText(context, "서비스가 중지되었습니다.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryNeon,
                            checkedTrackColor = SurfaceVariantDark
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "스케줄 주기: ${settings.recognitionIntervalMinutes}분 | 타임아웃: ${settings.maxTimeoutSeconds}초 | 중복방지: ${settings.deduplicationWindowMinutes}분",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
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
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "오늘 오류/실패",
                value = "${statusState?.todayFailureCount ?: 0}건",
                color = AccentRed,
                icon = Icons.Default.Error,
                modifier = Modifier.weight(1f)
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
                if (!isAccessibilityActive) {
                    Toast.makeText(context, "접근성 서비스를 먼저 켜주세요.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isTestingRecognition = true
                scope.launch {
                    ForegroundSchedulerService.startService(context)
                    Toast.makeText(context, "인식 테스트를 시작합니다...", Toast.LENGTH_SHORT).show()
                    isTestingRecognition = false
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
                isTestingTelegram = true
                scope.launch {
                    val token = settings.telegramBotToken
                    val chatId = settings.telegramChatId
                    if (token.isBlank() || chatId.isBlank()) {
                        Toast.makeText(context, "설정 화면에서 텔레그램 토큰과 Chat ID를 등록해주세요.", Toast.LENGTH_LONG).show()
                    } else {
                        val result = app.telegramQueueManager.testConnection(token, chatId)
                        result.fold(
                            onSuccess = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() },
                            onFailure = { err -> Toast.makeText(context, err.localizedMessage, Toast.LENGTH_LONG).show() }
                        )
                    }
                    isTestingTelegram = false
                }
            },
            enabled = !isTestingTelegram,
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
            Text(" [ Telegram 연동 테스트 ] ", color = PrimaryNeon, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = {
                ForegroundSchedulerService.stopService(context)
                ForegroundSchedulerService.startService(context)
                Toast.makeText(context, "서비스가 재시작되었습니다.", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = TextPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(" [ 서비스 재시작 ] ", color = TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ChecklistItemRow(item: AppChecklistHelper.ChecklistItem, onRefresh: () -> Unit) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariantDark.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (item.isPassed) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (item.isPassed) AccentGreen else AccentYellow,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.description,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        if (!item.isPassed) {
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    item.onAction(context)
                    onRefresh()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentYellow),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(text = item.actionText, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
        modifier = modifier.border(1.dp, CardBorder, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, fontSize = 12.sp, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return "-"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
    return sdf.format(Date(timestamp))
}
