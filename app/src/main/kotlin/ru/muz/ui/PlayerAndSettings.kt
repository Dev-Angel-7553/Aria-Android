package ru.muz.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.muz.lyrics.Lyrics
import ru.muz.lyrics.LyricsScreen
import ru.muz.lyrics.LyricsSettings
import ru.muz.player.PlayerState
import ru.muz.player.RepeatMode
import ru.muz.player.formatDuration
import ru.muz.settings.AppSettings
import ru.muz.yandex.YandexClient

@Composable
internal fun LoginScreen(
    busy: Boolean,
    message: String,
    onConnect: (String) -> Unit,
) {
    var token by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Box(
            Modifier.size(72.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp * LocalCornerRadiusScale.current))
                .background(Accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Headphones,
                null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(38.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        Text("Aria", color = Accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(
            "Вся музыка рядом",
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 38.sp,
        )
        Text(
            "Войдите с токеном Яндекс Музыки. Он хранится зашифрованно только на этом устройстве.",
            color = SoftText,
            modifier = Modifier.padding(top = 10.dp, bottom = 28.dp),
        )
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("OAuth-токен") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )
        androidx.compose.material3.Button(
            onClick = { onConnect(token) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(52.dp),
        ) {
            if (busy) {
                MuzLoadingIndicator(
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Продолжить", fontWeight = FontWeight.Bold)
            }
        }
        if (message.isNotBlank()) {
            Text(message, color = SoftText, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
