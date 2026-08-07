package com.soundlog.app.ui.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundlog.app.SoundLogApp
import com.soundlog.app.data.local.entity.ExecutionLogEntity
import com.soundlog.app.data.local.entity.SongResultEntity
import com.soundlog.app.ui.theme.AccentGreen
import com.soundlog.app.ui.theme.AccentRed
import com.soundlog.app.ui.theme.AccentYellow
import com.soundlog.app.ui.theme.CardBorder
import com.soundlog.app.ui.theme.DarkBackground
import com.soundlog.app.ui.theme.PrimaryNeon
import com.soundlog.app.ui.theme.SurfaceDark
import com.soundlog.app.ui.theme.TextMuted
import com.soundlog.app.ui.theme.TextPrimary
import com.soundlog.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen() {
    val app = SoundLogApp.instance
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("곡 식별 이력", "스텝별 세부 작동 로그")

    val songs by app.database.songResultDao().getAllSongsFlow()
        .collectAsState(initial = emptyList())

    val logs by app.database.executionLogDao().getRecentLogsFlow()
        .collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "음악 식별 및 작동 로그",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = SurfaceDark,
            contentColor = PrimaryNeon,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = PrimaryNeon
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == index) PrimaryNeon else TextSecondary
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTabIndex) {
            0 -> SongHistoryList(songs = songs)
            1 -> ExecutionLogList(logs = logs)
        }
    }
}

@Composable
fun SongHistoryList(songs: List<SongResultEntity>) {
    if (songs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("아직 저장된 식별 곡 이력이 없습니다.", color = TextMuted)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(songs) { song ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = PrimaryNeon,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = song.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                            }

                            val (statusColor, statusText) = when (song.telegramStatus) {
                                SongResultEntity.STATUS_SENT -> Pair(AccentGreen, "전송 완료 ✅")
                                SongResultEntity.STATUS_PENDING -> Pair(AccentYellow, "대기 중 ⏳")
                                else -> Pair(AccentRed, "실패 ❌")
                            }

                            Text(
                                text = statusText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = song.artist,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 28.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "식별 시각: ${formatLogTime(song.detectedAt)}",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            if (song.retryCount > 0) {
                                Text(
                                    text = "재시도: ${song.retryCount}회",
                                    fontSize = 11.sp,
                                    color = AccentYellow
                                )
                            }
                        }

                        if (!song.errorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "사유: ${song.errorMessage}",
                                fontSize = 11.sp,
                                color = AccentRed
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExecutionLogList(logs: List<ExecutionLogEntity>) {
    if (logs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("아직 기록된 실행 스텝 로그가 없습니다.", color = TextMuted)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(logs) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when {
                            log.isSuccess -> Icons.Default.CheckCircle
                            log.step == "NO_MATCH" || log.step == "DUPLICATE_SKIP" -> Icons.Default.Info
                            else -> Icons.Default.Error
                        }
                        val tint = when {
                            log.isSuccess -> AccentGreen
                            log.step == "NO_MATCH" || log.step == "DUPLICATE_SKIP" -> AccentYellow
                            else -> AccentRed
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = log.step,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = tint
                                )
                                Text(
                                    text = formatLogTime(log.timestamp),
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = log.message,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatLogTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm:ss", Locale.KOREA)
    return sdf.format(Date(timestamp))
}
