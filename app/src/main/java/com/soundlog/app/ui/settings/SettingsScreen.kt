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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Telegram
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settings = SoundLogApp.instance.settingsRepository

    var botToken by remember { mutableStateOf(settings.telegramBotToken) }
    var chatId by remember { mutableStateOf(settings.telegramChatId) }
    var intervalStr by remember { mutableStateOf(settings.recognitionIntervalMinutes.toString()) }
    var timeoutStr by remember { mutableStateOf(settings.maxTimeoutSeconds.toString()) }
    var dedupStr by remember { mutableStateOf(settings.deduplicationWindowMinutes.toString()) }
    var retryStr by remember { mutableStateOf(settings.maxRetryCount.toString()) }

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
                    Icon(imageVector = Icons.Default.Telegram, contentDescription = null, tint = PrimaryNeon)
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
                    leadingIcon = { Icon(Icons.Default.Telegram, contentDescription = null, tint = PrimaryNeon) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = customTextFieldColors()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🔒 입출력되는 Bot Token 및 Chat ID는 Android Keystore로 암호화되어 안전하게 보관됩니다.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
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
                        text = "동적 서비스 동작 설정",
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
            }
        }

        // Save Button
        Button(
            onClick = {
                val interval = intervalStr.toIntOrNull() ?: 5
                val timeout = timeoutStr.toIntOrNull() ?: 12
                val dedup = dedupStr.toIntOrNull() ?: 10
                val retry = retryStr.toIntOrNull() ?: 3

                settings.telegramBotToken = botToken.trim()
                settings.telegramChatId = chatId.trim()
                settings.recognitionIntervalMinutes = interval.coerceAtLeast(1)
                settings.maxTimeoutSeconds = timeout.coerceAtLeast(5)
                settings.deduplicationWindowMinutes = dedup.coerceAtLeast(1)
                settings.maxRetryCount = retry.coerceAtLeast(1)

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
