package com.soundlog.app.ui.logs

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── 날짜 구분 헬퍼 ─────────────────────────────────────────────────────────────

private fun getDateStr(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date(timestamp))

private fun getDateLabel(timestamp: Long): String {
    val today = getDateStr(System.currentTimeMillis())
    val yesterday = getDateStr(System.currentTimeMillis() - 86_400_000L)
    return when (val d = getDateStr(timestamp)) {
        today -> "오늘"
        yesterday -> "어제"
        else -> SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(Date(timestamp))
    }
}

// ── Sealed 목록 아이템 ─────────────────────────────────────────────────────────

private sealed class SongListItem {
    data class DateHeader(val dateStr: String, val label: String) : SongListItem()
    data class SongEntry(val song: SongResultEntity) : SongListItem()
}

private sealed class LogListItem {
    data class DateHeader(val dateStr: String, val label: String) : LogListItem()
    data class LogEntry(val log: ExecutionLogEntity) : LogListItem()
}

private fun buildSongItems(songs: List<SongResultEntity>): List<SongListItem> {
    val result = mutableListOf<SongListItem>()
    var lastDate = ""
    for (song in songs) {
        val ds = getDateStr(song.detectedAt)
        if (ds != lastDate) {
            result.add(SongListItem.DateHeader(ds, getDateLabel(song.detectedAt)))
            lastDate = ds
        }
        result.add(SongListItem.SongEntry(song))
    }
    return result
}

private fun buildLogItems(logs: List<ExecutionLogEntity>): List<LogListItem> {
    val result = mutableListOf<LogListItem>()
    var lastDate = ""
    for (log in logs) {
        val ds = getDateStr(log.timestamp)
        if (ds != lastDate) {
            result.add(LogListItem.DateHeader(ds, getDateLabel(log.timestamp)))
            lastDate = ds
        }
        result.add(LogListItem.LogEntry(log))
    }
    return result
}

// ── 날짜 구분선 컴포넌트 ────────────────────────────────────────────────────────

@Composable
private fun DateSeparator(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(CardBorder))
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp),
            fontSize = 11.sp,
            color = TextMuted,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Box(modifier = Modifier.weight(1f).height(1.dp).background(CardBorder))
    }
}

// ── 메인 LogsScreen ──────────────────────────────────────────────────────────

@Composable
fun LogsScreen() {
    val app = SoundLogApp.instance
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("식별 이력 (음악 식별 로그)", "작동 로그 (앱 시스템 작동)")

    val songs by app.database.songResultDao().getAllSongsFlow()
        .collectAsState(initial = emptyList())

    val logs by app.database.executionLogDao().getRecentLogsFlow(limit = 1000)
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
            0 -> SongHistoryList(songs = songs)
            1 -> ExecutionLogList(logs = logs)
        }
    }
}

// ── 식별 이력 탭 ─────────────────────────────────────────────────────────────

@Composable
fun SongHistoryList(songs: List<SongResultEntity>) {
    val app = SoundLogApp.instance
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }

    val filteredSongs = remember(songs, searchQuery) {
        if (searchQuery.isBlank()) songs
        else songs.filter { song ->
            song.title.contains(searchQuery, ignoreCase = true) ||
                    song.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    val songItems = remember(filteredSongs) { buildSongItems(filteredSongs) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 컴팩트 & 밝은 검색 필드
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    "곡명 또는 아티스트 검색...",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = PrimaryNeon,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = TextMuted,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { searchQuery = "" }
                    )
                }
            },
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryNeon,
                unfocusedBorderColor = CardBorder.copy(alpha = 0.6f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                // 로그 카드보다 밝은 배경색으로 차별화
                focusedContainerColor = Color(0xFF1E2D4A),
                unfocusedContainerColor = Color(0xFF1A2840)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),   // 기존 대비 ~60% 높이
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredSongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (searchQuery.isBlank()) "아직 저장된 식별 곡 이력이 없습니다." else "검색 결과가 없습니다.",
                    color = TextMuted
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = songItems,
                    key = { item ->
                        when (item) {
                            is SongListItem.DateHeader -> "song_header_${item.dateStr}"
                            is SongListItem.SongEntry  -> "song_${item.song.id}"
                        }
                    }
                ) { item ->
                    when (item) {
                        is SongListItem.DateHeader -> DateSeparator(label = item.label)
                        is SongListItem.SongEntry  -> SwipeableSongCard(
                            song = item.song,
                            onDelete = {
                                coroutineScope.launch {
                                    app.database.songResultDao().deleteById(item.song.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ── 우측 스와이프 삭제 - 식별 이력 ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableSongCard(song: SongResultEntity, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val isRevealed = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            val bgColor by animateColorAsState(
                targetValue = if (isRevealed) AccentRed.copy(alpha = 0.85f) else Color.Transparent,
                label = "song_swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor, RoundedCornerShape(10.dp))
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (isRevealed) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "삭제",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    ) {
        SongHistoryItemCard(song = song)
    }
}

// ── 작동 로그 탭 ─────────────────────────────────────────────────────────────

@Composable
fun ExecutionLogList(logs: List<ExecutionLogEntity>) {
    val app = SoundLogApp.instance
    val coroutineScope = rememberCoroutineScope()
    val logItems = remember(logs) { buildLogItems(logs) }

    if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("아직 기록된 실행 스텝 로그가 없습니다.", color = TextMuted)
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = logItems,
                key = { item ->
                    when (item) {
                        is LogListItem.DateHeader -> "log_header_${item.dateStr}"
                        is LogListItem.LogEntry   -> "log_${item.log.id}"
                    }
                }
            ) { item ->
                when (item) {
                    is LogListItem.DateHeader -> DateSeparator(label = item.label)
                    is LogListItem.LogEntry   -> SwipeableLogCard(
                        log = item.log,
                        onDelete = {
                            coroutineScope.launch {
                                app.database.executionLogDao().deleteById(item.log.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

// ── 우측 스와이프 삭제 - 작동 로그 ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableLogCard(log: ExecutionLogEntity, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val isRevealed = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            val bgColor by animateColorAsState(
                targetValue = if (isRevealed) AccentRed.copy(alpha = 0.85f) else Color.Transparent,
                label = "log_swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor, RoundedCornerShape(10.dp))
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (isRevealed) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "삭제",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    ) {
        ExecutionLogCard(log = log)
    }
}

// ── 카드 UI ────────────────────────────────────────────────────────────────────

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
                    SongResultEntity.STATUS_SENT    -> Pair(AccentGreen,  "전송 완료 ✅")
                    SongResultEntity.STATUS_PENDING -> Pair(AccentYellow, "대기 중 ⏳")
                    else                            -> Pair(AccentRed,    "실패 ❌")
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

@Composable
fun ExecutionLogCard(log: ExecutionLogEntity) {
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
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = log.step, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = tint)
                    Text(text = formatLogTime(log.timestamp), fontSize = 11.sp, color = TextMuted)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = log.message, fontSize = 12.sp, color = TextPrimary)
            }
        }
    }
}

// ── 시간 포맷 헬퍼 ─────────────────────────────────────────────────────────────

private fun formatLogTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(Date(timestamp))

private fun formatLogTimeFull(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).format(Date(timestamp))
