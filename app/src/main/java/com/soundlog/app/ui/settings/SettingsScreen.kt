package com.soundlog.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundlog.app.SoundLogApp
import com.soundlog.app.data.local.pref.EncryptedSettingsRepository
import com.soundlog.app.ui.theme.CardBorder
import com.soundlog.app.ui.theme.DarkBackground
import com.soundlog.app.ui.theme.PrimaryNeon
import com.soundlog.app.ui.theme.SurfaceDark
import com.soundlog.app.ui.theme.SurfaceVariantDark
import com.soundlog.app.ui.theme.TextPrimary
import com.soundlog.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = SoundLogApp.instance
    val settings = app.settingsRepository

    var botToken by remember { mutableStateOf(settings.telegramBotToken) }
    var chatId by remember { mutableStateOf(settings.telegramChatId) }
    var messageFormat by remember { mutableStateOf(settings.telegramMessageFormat) }
    var intervalStr by remember { mutableStateOf(settings.recognitionIntervalMinutes.toString()) }
    var timeoutStr by remember { mutableStateOf(settings.maxTimeoutSeconds.toString()) }
    var dedupStr by remember { mutableStateOf(settings.deduplicationWindowMinutes.toString()) }
    var retryStr by remember { mutableStateOf(settings.maxRetryCount.toString()) }
    var maxSongCountStr by remember { mutableStateOf(settings.maxSongLogCount.toString()) }
    var albumArtOption by remember { mutableStateOf(settings.albumArtOption) }
    var musicLinkOption by remember { mutableStateOf(settings.musicLinkOption) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "시스템 및 텔레그램 설정",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // Telegram Credentials Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = PrimaryNeon)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "텔레그램 Bot API 설정 (암호화 저장)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = botToken,
                    onValueChange = { botToken = it },
                    label = { Text("HTTP API Bot Token") },
                    placeholder = { Text("123456789:ABCdefGhIJKlmNoPQ...") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryNeon) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = customTextFieldColors()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = chatId,
                    onValueChange = { chatId = it },
                    label = { Text("Telegram Channel/Chat ID") },
                    placeholder = { Text("@my_channel_name 또는 -100123456789") },
                    leadingIcon = { Icon(Icons.Default.Send, contentDescription = null, tint = PrimaryNeon) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = customTextFieldColors()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🔒 Bot Token은 보안을 위해 항상 비공개(***)로 보호되며 붙여넣은 후에도 조회할 수 없습니다.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        // Telegram Message Format Card
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
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = PrimaryNeon)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "텔레그램 메시지 포맷 설정",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                    }

                    Button(
                        onClick = {
                            messageFormat = EncryptedSettingsRepository.DEFAULT_TELEGRAM_FORMAT
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.height(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("기본값 복원", fontSize = 11.sp, color = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "빠른 치환 태그 삽입",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tagList = listOf(
                        "+ 곡명" to "{title}",
                        "+ 아티스트" to "{artist}",
                        "+ 날짜" to "{date}",
                        "+ 시간" to "{time}",
                        "+ 일시" to "{datetime}"
                    )
                    for ((label, tag) in tagList) {
                        Button(
                            onClick = {
                                messageFormat = if (messageFormat.endsWith(" ") || messageFormat.endsWith("\n") || messageFormat.isEmpty()) {
                                    messageFormat + tag
                                } else {
                                    "$messageFormat $tag"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(label, fontSize = 11.sp, color = PrimaryNeon, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = messageFormat,
                    onValueChange = { messageFormat = it },
                    label = { Text("메시지 포맷 템플릿") },
                    placeholder = { Text("예: 🎵 {artist} - {title}") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                    colors = customTextFieldColors()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "📱 텔레그램 전송 시뮬레이션 샘플",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryNeon
                )
                Spacer(modifier = Modifier.height(6.dp))

                val samplePreview = remember(messageFormat) {
                    val formatted = com.soundlog.app.data.remote.telegram.TelegramQueueManager.formatTemplate(
                        template = messageFormat,
                        title = "Cruel Summer",
                        artist = "Taylor Swift"
                    )
                    // HTML 태그 단순 제거 후 시뮬레이션 렌더링
                    formatted.replace(Regex("<[^>]*>"), "")
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = samplePreview.ifBlank { "(메시지 포맷이 비어있습니다)" },
                            fontSize = 13.sp,
                            color = TextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                androidx.compose.material3.HorizontalDivider(color = CardBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = PrimaryNeon)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🖼️ 앨범 자켓이미지 전송 옵션",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val artOptions = listOf(
                    Triple(
                        EncryptedSettingsRepository.ALBUM_ART_NONE,
                        "1. 미전송",
                        "기존처럼 곡제목/아티스트 정보 텍스트만 텔레그램으로 전송합니다."
                    ),
                    Triple(
                        EncryptedSettingsRepository.ALBUM_ART_SHAZAM,
                        "2. Shazam",
                        "샤잠 앱 아티스트 상세 페이지 클릭 ➔ 스와이프를 통해 앨범 자켓을 캡처하여 전송합니다."
                    ),
                    Triple(
                        EncryptedSettingsRepository.ALBUM_ART_ITUNES,
                        "3. iTunes API",
                        "식별된 곡 정보를 기반으로 iTunes API에서 1000x1000 고화질 원본 자켓을 0.1초 만에 획득하여 전송합니다."
                    )
                )

                artOptions.forEach { (key, label, description) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (albumArtOption == key),
                                onClick = {
                                    albumArtOption = key
                                    settings.albumArtOption = key
                                }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        RadioButton(
                            selected = (albumArtOption == key),
                            onClick = {
                                albumArtOption = key
                                settings.albumArtOption = key
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = PrimaryNeon,
                                unselectedColor = TextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (albumArtOption == key) PrimaryNeon else TextPrimary
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = description,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                androidx.compose.material3.HorizontalDivider(color = CardBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = PrimaryNeon)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🔗 음악 링크 전송 옵션",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val linkOptions = listOf(
                    Triple(
                        EncryptedSettingsRepository.MUSIC_LINK_NONE,
                        "1. 미전송",
                        "음악 링크를 수집하지 않으며, 기존 텍스트 형태 메시지로 전송합니다."
                    ),
                    Triple(
                        EncryptedSettingsRepository.MUSIC_LINK_YOUTUBE,
                        "2. YouTube",
                        "아티스트와 곡명 기반의 유튜브 검색 링크를 생성하여 곡 제목에 하이퍼링크로 삽입합니다."
                    )
                )

                linkOptions.forEach { (key, label, description) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (musicLinkOption == key),
                                onClick = {
                                    musicLinkOption = key
                                    settings.musicLinkOption = key
                                }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        RadioButton(
                            selected = (musicLinkOption == key),
                            onClick = {
                                musicLinkOption = key
                                settings.musicLinkOption = key
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = PrimaryNeon,
                                unselectedColor = TextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (musicLinkOption == key) PrimaryNeon else TextPrimary
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = description,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Dynamic Configurations Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = PrimaryNeon)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "동적 서비스 및 로그 저장 설정",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = intervalStr,
                        onValueChange = { intervalStr = it },
                        label = { Text("인식 주기 (분)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = customTextFieldColors()
                    )

                    OutlinedTextField(
                        value = timeoutStr,
                        onValueChange = { timeoutStr = it },
                        label = { Text("최대 타임아웃 (초)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = customTextFieldColors()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = dedupStr,
                        onValueChange = { dedupStr = it },
                        label = { Text("중복 방지 (분)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = customTextFieldColors()
                    )

                    OutlinedTextField(
                        value = retryStr,
                        onValueChange = { retryStr = it },
                        label = { Text("Telegram 재시도 (회)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = customTextFieldColors()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = maxSongCountStr,
                    onValueChange = { maxSongCountStr = it },
                    label = { Text("음악 식별 로그 최대 저장 건수") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = customTextFieldColors()
                )
            }
        }

        // Save Button
        Button(
            onClick = {
                val interval = intervalStr.toIntOrNull() ?: 5
                val timeout = timeoutStr.toIntOrNull() ?: 12
                val dedup = dedupStr.toIntOrNull() ?: 10
                val retry = retryStr.toIntOrNull() ?: 3
                val maxSong = maxSongCountStr.toIntOrNull() ?: 1000

                settings.telegramBotToken = botToken.trim()
                settings.telegramChatId = chatId.trim()
                settings.telegramMessageFormat = messageFormat.ifBlank { com.soundlog.app.data.local.pref.EncryptedSettingsRepository.DEFAULT_TELEGRAM_FORMAT }
                settings.recognitionIntervalMinutes = interval.coerceAtLeast(1)
                settings.maxTimeoutSeconds = timeout.coerceAtLeast(5)
                settings.deduplicationWindowMinutes = dedup.coerceAtLeast(1)
                settings.maxRetryCount = retry.coerceAtLeast(1)
                settings.maxSongLogCount = maxSong.coerceAtLeast(10)
                settings.albumArtOption = albumArtOption
                settings.musicLinkOption = musicLinkOption

                scope.launch(Dispatchers.IO) {
                    app.database.songResultDao().pruneOldSongs(settings.maxSongLogCount)
                    app.database.executionLogDao().pruneOldLogs(10000)
                }

                Toast.makeText(context, "설정이 성공적으로 저장되었습니다.", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("설정값 저장하기", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        val appVersionName = remember {
            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val verName = if (pInfo.versionName.startsWith("v")) pInfo.versionName else "v${pInfo.versionName}"
                val buildCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    pInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION") pInfo.versionCode.toLong()
                }
                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.KOREA).format(java.util.Date(pInfo.lastUpdateTime))
                "$verName (Build $buildCode) • $dateStr"
            } catch (e: Exception) {
                "v1.2.1 (Build 46) • 2026-08-14"
            }
        }

        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = appVersionName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryNeon,
    unfocusedBorderColor = CardBorder,
    focusedLabelColor = PrimaryNeon,
    unfocusedLabelColor = TextSecondary,
    focusedContainerColor = SurfaceVariantDark,
    unfocusedContainerColor = SurfaceVariantDark,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
