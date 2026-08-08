package com.soundlog.app.ui.logs

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val tabs = listOf("식별 이력", "작동 로그")

    // 음악 식별 결과 (SongResultEntity) - 성공/실패 모두 포함
    val songs by app.database.songResultDao().getAllSongsFlow()
        .collectAsState(initial = emptyList())

    // 식별 이력 중 실패 로그 (NO_MATCH, FAILURE) - SongResultEntity에 없는 실패 케이스용
    val recognitionLogs by app.database.executionLogDao().getSongRecognitionLogsFlow(limit = 1000)
        .collectAsState(initial = emptyList())

    // 앱 작동 로그 (시스템 로그만)
    val systemLogs by app.database.executionLogDao().getSystemLogsFlow(limit = 1000)
        .collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "음악 식별 및 작동 로그",
            fontSize = 22.sp,
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
                            fontSize = 13.sp,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == index) PrimaryNeon else TextSecondary
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedTabIndex) {
            0 -> SongHistoryList(songs = songs, recognitionLogs = recognitionLogs)
            1 -> SystemLogList(logs = systemLogs)
        }
    }
}

/**
 * 식별 이력 탭: SongResultEntity (성공/실패 음악) + NO_MATCH/FAILURE 로그
 * - SongResultEntity: 실제로 곡명/아티스트가 인식된 경우 (성공 or 텔레그램 실패)
 * - recognitionLogs: 음악 미인식 (NO_MATCH, FAILURE) 등
 */
@Composable
fun SongHistoryList(songs: List<SongResultEntity>, recognitionLogs: List<ExecutionLogEntity>) {
    var searchQuery by remember { mutableStateOf("") }

    // NO_MATCH/FAILURE 로그 중 SongResultEntity에 해당하지 않는 순수 실패 건만 분리
    val noMatchLogs = remember(recognitionLogs) {
        recognitionLogs.filter { it.step == "NO_MATCH" || it.step == "FAILURE" }
    }

    val filteredSongs = remember(songs, searchQuery) {
        if (searchQuery.isBlank()) songs
        else songs.filter { song ->
            song.title.contains(searchQuery, ignoreCase = true) ||
            song.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 검색창 (SongResultEntity 대상)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("곡 제목 또는 아티스트 검색...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryNeon) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = TextMuted,
                        modifier = Modifier.clickable { searchQuery = "" }
                    )
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryNeon,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            shape = RoundedCornerShape(12.dp)
        )

        if (filteredSongs.isEmpty() && noMatchLogs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "아직 저장된 식별 이력이 없습니다." else "검색 결과가 없습니다.",
                    color = TextMuted
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 인식 성공/텔레그램 상태 카드
                if (filteredSongs.isNotEmpty()) {
                    items(filteredSongs) { song ->
                        SongHistoryItemCard(song = song)
                    }
                }

                // 검색어 없을 때만 NO_MATCH/FAILURE 로그 표시
                if (searchQuery.isBlank() && noMatchLogs.isNotEmpty()) {
                    items(noMatchLogs) { log ->
                        NoMatchLogCard(log = log)
                    }
                }
            }
        }
    }
}

@Composable
fun SongHistoryItemCard(song: SongResultEntity) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
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
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = PrimaryNeon,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = song.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                val (statusColor, statusText) = when (song.telegramStatus) {
                    SongResultEntity.STATUS_SENT -> Pair(AccentGreen, "전송 완료 ✅")
                    SongResultEntity.STATUS_PENDING -> Pair(AccentYellow, "대기 중 ⏳")
                    else -> Pair(AccentRed, "실패 ❌")
                }

                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = song.artist,
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 24.dp)
            )

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "인식 일시: ${formatLogTimeFull(song.detectedAt)}",
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
                        text = "오류 사유: ${song.errorMessage}",
                        fontSize = 11.sp,
                        color = AccentRed
                    )
                }
            }
        }
    }
}

/** 음악 미인식(NO_MATCH) / 실패(FAILURE) 로그 카드 */
@Composable
fun NoMatchLogCard(log: ExecutionLogEntity) {
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
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = if (log.step == "FAILURE") AccentRed else AccentYellow,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (log.step == "NO_MATCH") "음악 미인식" else "인식 실패",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (log.step == "FAILURE") AccentRed else AccentYellow
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
                    color = TextSecondary
                )
            }
        }
    }
}

/** 작동 로그 탭: 앱 시스템 작동 관련 로그만 */
@Composable
fun SystemLogList(logs: List<ExecutionLogEntity>) {
    if (logs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("아직 기록된 앱 작동 로그가 없습니다.", color = TextMuted)
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
                            log.step == "WATCHDOG_RECOVERY" -> Icons.Default.Error
                            else -> Icons.Default.Terminal
                        }
                        val tint = when {
                            log.isSuccess -> AccentGreen
                            log.step == "WATCHDOG_RECOVERY" -> AccentRed
                            else -> AccentYellow
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
                                    text = stepLabel(log.step),
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

/** Step 코드를 사람이 읽기 좋은 한글 레이블로 변환 */
private fun stepLabel(step: String): String = when (step) {
    "SERVICE_START" -> "서비스 시작"
    "SERVICE_STOP"  -> "서비스 종료"
    "CYCLE_START"   -> "인식 주기 시작"
    "CYCLE_END"     -> "인식 주기 완료"
    "TELEGRAM_QUEUE" -> "텔레그램 큐"
    "WATCHDOG_RECOVERY" -> "자동 복구"
    "DUPLICATE_SKIP" -> "중복 스킵"
    else -> step
}

private fun formatLogTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm:ss", Locale.KOREA)
    return sdf.format(Date(timestamp))
}

private fun formatLogTimeFull(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
    return sdf.format(Date(timestamp))
}
