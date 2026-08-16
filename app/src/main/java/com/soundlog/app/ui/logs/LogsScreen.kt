package com.soundlog.app.ui.logs

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.soundlog.app.automation.YouTubeNodeFinder
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import com.soundlog.app.ui.theme.AccentOrange
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

    // 식별 이력 중 실패 로그 (NO_MATCH, FAILURE) - SongResultEntity에 없는 실패 케이스용 (10,000건)
    val recognitionLogs by app.database.executionLogDao().getSongRecognitionLogsFlow(limit = 10000)
        .collectAsState(initial = emptyList())

    // 앱 작동 로그 (시스템 로그만 - 10,000건)
    val systemLogs by app.database.executionLogDao().getSystemLogsFlow(limit = 10000)
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

/** 식별 이력 통합 래퍼 모델 (성공 곡 vs 미인식/실패 로그) */
sealed class RecognitionHistoryItem {
    abstract val timestamp: Long

    data class SuccessSong(val song: SongResultEntity) : RecognitionHistoryItem() {
        override val timestamp: Long get() = song.detectedAt
    }

    data class RecognitionLog(val log: ExecutionLogEntity) : RecognitionHistoryItem() {
        override val timestamp: Long get() = log.timestamp
    }
}

/** 날짜별 그룹핑 데이터 구조체 (3단계 신호등: 성공, 미인식, 실패) */
data class DateGroup(
    val dateKey: String,
    val displayDate: String,
    val successCount: Int,
    val noMatchCount: Int,
    val failureCount: Int,
    val items: List<RecognitionHistoryItem>
)

/**
 * 식별 이력 탭: SongResultEntity (성공 음악) + NO_MATCH/FAILURE 로그 통합 날짜별 그룹핑 리스트
 */
@Composable
fun SongHistoryList(songs: List<SongResultEntity>, recognitionLogs: List<ExecutionLogEntity>) {
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val dateKeySdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.KOREA) }
    val displayDateSdf = remember { SimpleDateFormat("yyyy년 MM월 dd일 (E)", Locale.KOREA) }

    val dateGroups = remember(songs, recognitionLogs, searchQuery) {
        val songItems = songs.map { RecognitionHistoryItem.SuccessSong(it) }
        val logItems = recognitionLogs
            .filter { it.step == "NO_MATCH" || it.step == "FAILURE" }
            .map { RecognitionHistoryItem.RecognitionLog(it) }

        val allItems = (songItems + logItems).sortedByDescending { it.timestamp }

        val filteredItems = if (searchQuery.isBlank()) {
            allItems
        } else {
            allItems.filter { item ->
                when (item) {
                    is RecognitionHistoryItem.SuccessSong ->
                        item.song.title.contains(searchQuery, ignoreCase = true) ||
                        item.song.artist.contains(searchQuery, ignoreCase = true)
                    is RecognitionHistoryItem.RecognitionLog ->
                        item.log.message.contains(searchQuery, ignoreCase = true)
                }
            }
        }

        filteredItems
            .groupBy { dateKeySdf.format(Date(it.timestamp)) }
            .map { (dateKey, groupItems) ->
                val firstTimestamp = groupItems.firstOrNull()?.timestamp ?: System.currentTimeMillis()
                DateGroup(
                    dateKey = dateKey,
                    displayDate = displayDateSdf.format(Date(firstTimestamp)),
                    successCount = groupItems.count { it is RecognitionHistoryItem.SuccessSong },
                    noMatchCount = groupItems.count { it is RecognitionHistoryItem.RecognitionLog && it.log.step == "NO_MATCH" },
                    failureCount = groupItems.count { it is RecognitionHistoryItem.RecognitionLog && it.log.step == "FAILURE" },
                    items = groupItems
                )
            }
    }

    // 신규 곡 인식 또는 화면 로드 시 항상 최상단(최신순)으로 자동 스크롤
    val latestTimestamp = remember(dateGroups) {
        dateGroups.firstOrNull()?.items?.firstOrNull()?.timestamp ?: 0L
    }
    LaunchedEffect(latestTimestamp) {
        if (latestTimestamp > 0L) {
            listState.scrollToItem(0)
        }
    }

    // 스마트 접기 관리:
    // 기본값: 가장 최근 1개 일자(index 0)만 펼치고, 과거 일자(index >= 1)는 기본 접힘
    // 검색 중에는 매칭 결과 표시를 위해 전체 펼침
    var userCollapsedDates by remember { mutableStateOf<Set<String>?>(null) }
    val effectiveCollapsedDates = remember(dateGroups, userCollapsedDates, searchQuery) {
        if (searchQuery.isNotBlank()) {
            emptySet()
        } else if (userCollapsedDates != null) {
            userCollapsedDates!!
        } else {
            // 최초 로드 시 최신 날짜 제외 과거 날짜는 기본 접힘
            dateGroups.drop(1).map { it.dateKey }.toSet()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 검색창
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("곡 제목, 아티스트 또는 오류 내용 검색...", color = TextMuted, fontSize = 13.sp) },
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

        if (dateGroups.isEmpty()) {
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
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                dateGroups.forEach { group ->
                    val isCollapsed = effectiveCollapsedDates.contains(group.dateKey)

                    item(key = "header_${group.dateKey}") {
                        DateGroupHeader(
                            group = group,
                            isCollapsed = isCollapsed,
                            onToggle = {
                                userCollapsedDates = if (isCollapsed) {
                                    effectiveCollapsedDates - group.dateKey
                                } else {
                                    effectiveCollapsedDates + group.dateKey
                                }
                            }
                        )
                    }

                    if (!isCollapsed) {
                        items(
                            items = group.items,
                            key = { item ->
                                when (item) {
                                    is RecognitionHistoryItem.SuccessSong -> "song_${item.song.id}"
                                    is RecognitionHistoryItem.RecognitionLog -> "log_${item.log.id}"
                                }
                            }
                        ) { item ->
                            when (item) {
                                is RecognitionHistoryItem.SuccessSong -> {
                                    SongHistoryItemCard(song = item.song)
                                }
                                is RecognitionHistoryItem.RecognitionLog -> {
                                    NoMatchLogCard(log = item.log)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 날짜별 그룹 구분선 및 접기/펼치기 헤더 (슬림 섹션 구분선 디자인) */
@Composable
fun DateGroupHeader(
    group: DateGroup,
    isCollapsed: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp)
            .clickable { onToggle() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 좌측: 네온 테두리 날짜 배지 (토글 아이콘 포함)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(PrimaryNeon.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                .border(1.dp, PrimaryNeon.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                contentDescription = if (isCollapsed) "펼치기" else "접기",
                tint = PrimaryNeon,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = group.displayDate,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryNeon
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 중앙: 은은한 수평 구분선 (Divider)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(CardBorder.copy(alpha = 0.8f))
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 우측: 3단계 캡슐형 실패/미인식/성공 뱃지 (신호등 색상)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (group.failureCount > 0) {
                Box(
                    modifier = Modifier
                        .background(AccentRed.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, AccentRed.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 6.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = "● ${group.failureCount}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentRed
                    )
                }
            }
            if (group.noMatchCount > 0) {
                Box(
                    modifier = Modifier
                        .background(AccentOrange.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, AccentOrange.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 6.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = "● ${group.noMatchCount}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange
                    )
                }
            }
            if (group.successCount > 0) {
                Box(
                    modifier = Modifier
                        .background(AccentGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, AccentGreen.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 6.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = "● ${group.successCount}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen
                    )
                }
            }
        }
    }
}

@Composable
fun SongHistoryItemCard(song: SongResultEntity) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .clickable {
                val youtubeUrl = if (!song.youtubeUrl.isNullOrBlank()) {
                    song.youtubeUrl
                } else {
                    YouTubeNodeFinder.buildYouTubeSearchUrl(song.artist, song.title)
                }
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.e("LogsScreen", "Failed to open YouTube URL: $youtubeUrl", e)
                }
            }
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            // 상단: 곡 제목 (좌측) + 인식 일시 (우측)
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = formatLogTimeFull(song.detectedAt),
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 하단: 아티스트명 (좌측) + 전송 성공/실패 뱃지 (우측)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = song.artist,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 24.dp)
                )

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

            if (!song.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "오류 사유: ${song.errorMessage}",
                    fontSize = 11.sp,
                    color = AccentRed,
                    modifier = Modifier.padding(start = 24.dp)
                )
            }
        }
    }
}

/** 음악 미인식(NO_MATCH) / 동작 실패(FAILURE) 로그 카드 */
@Composable
fun NoMatchLogCard(log: ExecutionLogEntity) {
    val isFailure = log.step == "FAILURE"
    val itemColor = if (isFailure) AccentRed else AccentOrange
    val titleText = if (isFailure) "인식 실패" else "음악 미인식"
    val icon = if (isFailure) Icons.Default.Error else Icons.Default.Info

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
                imageVector = icon,
                contentDescription = null,
                tint = itemColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = titleText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = itemColor
                    )
                    Text(
                        text = formatLogTimeFull(log.timestamp),
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
    val sdf = SimpleDateFormat("yy/MM/dd HH:mm:ss", Locale.KOREA)
    return sdf.format(Date(timestamp))
}
