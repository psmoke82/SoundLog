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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
    var messageTemplate by remember { mutableStateOf(settings.telegramMessageTemplate) }
    var intervalStr by remember { mutableStateOf(settings.recognitionIntervalMinutes.toString()) }
    var timeoutStr by remember { mutableStateOf(settings.maxTimeoutSeconds.toString()) }
    var dedupStr by remember { mutableStateOf(settings.deduplicationWindowMinutes.toString()) }
    var retryStr by remember { mutableStateOf(settings.maxRetryCount.toString()) }
    var maxSongCountStr by remember { mutableStateOf(settings.maxSongLogCount.toString()) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Fixed Top Header
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBackground)
                .padding(16.dp)
        ) {
            Text(
                text = "시스템 및 텔레그램 설정",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Scrollable Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

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

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = CardBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Telegram Message Format Editor & Simulation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📱 텔레그램 메시지 포맷 설정",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    TextButton(
                        onClick = {
                            messageTemplate = EncryptedSettingsRepository.DEFAULT_TELEGRAM_MESSAGE_TEMPLATE
                        }
                    ) {
                        Text("기본 포맷 복원", fontSize = 12.sp, color = PrimaryNeon)
                    }
                }

                Text(
                    text = "아래 치환 태그 칩을 누르면 에디터에 자동 입력됩니다.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tag Chips Group
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tags = listOf(
                        "곡명" to "{title}",
                        "아티스트" to "{artist}",
                        "날짜" to "{date}",
                        "시간" to "{time}",
                        "일시" to "{datetime}"
                    )
                    tags.forEach { (label, tag) ->
                        Button(
                            onClick = {
                                messageTemplate += " $tag"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "+ $label",
                                fontSize = 11.sp,
                                color = PrimaryNeon,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = messageTemplate,
                    onValueChange = { messageTemplate = it },
                    label = { Text("메시지 포맷 템플릿") },
                    placeholder = { Text("보낼 메시지 양식을 입력하세요...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                    colors = customTextFieldColors()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Live Simulation Preview Box
                Text(
                    text = "👁️ 실시간 전송 시뮬레이션 미리보기",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2638)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF2B3752), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = Color(0xFF29B6F6),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Telegram Bot (Preview)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF29B6F6)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = com.soundlog.app.util.TelegramMessageFormatter.formatSimulation(messageTemplate),
                            fontSize = 13.sp,
                            color = Color.White,
                            lineHeight = 18.sp
                        )
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
                settings.telegramMessageTemplate = messageTemplate.ifBlank { EncryptedSettingsRepository.DEFAULT_TELEGRAM_MESSAGE_TEMPLATE }
                settings.recognitionIntervalMinutes = interval.coerceAtLeast(1)
                settings.maxTimeoutSeconds = timeout.coerceAtLeast(5)
                settings.deduplicationWindowMinutes = dedup.coerceAtLeast(1)
                settings.maxRetryCount = retry.coerceAtLeast(1)
                settings.maxSongLogCount = maxSong.coerceAtLeast(10)

                scope.launch(Dispatchers.IO) {
                    app.database.songResultDao().pruneOldSongs(settings.maxSongLogCount)
                    app.database.executionLogDao().pruneOldLogs(1000)
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

        Spacer(modifier = Modifier.height(16.dp))
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
