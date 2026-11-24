package com.accend.app.ui.splash

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accend.app.R
import com.accend.app.ui.theme.AccendPalette
import com.accend.app.ui.theme.MonoFontFamily
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val animationsDisabled = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }

    // 0 = hidden, 1 = visible/holding, 2 = fading out
    var phase by remember { mutableIntStateOf(if (animationsDisabled) 1 else 0) }

    LaunchedEffect(Unit) {
        if (animationsDisabled) {
            delay(350)
            onFinished()
        } else {
            phase = 1
            delay(400 + 1200)
            phase = 2
            delay(300)
            onFinished()
        }
    }

    val contentAlpha by animateFloatAsState(
        targetValue = when (phase) {
            1 -> 1f
            else -> 0f
        },
        animationSpec = tween(durationMillis = if (phase == 1) 400 else 300, easing = FastOutSlowInEasing),
        label = "splash_alpha"
    )
    val contentScale by animateFloatAsState(
        targetValue = if (phase == 0) 0.9f else 1f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "splash_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "splash_glow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AccendPalette.BgBase)
            .drawBehind {
                val radius = 220.dp.toPx() * glowPulse
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.14f * contentAlpha),
                            Color(0xFF9CA3AF).copy(alpha = 0.05f * contentAlpha),
                            Color.Transparent
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = radius
                    ),
                    radius = radius,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .alpha(contentAlpha)
                .scale(contentScale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.accend_brand_mark),
                contentDescription = "Accend mark",
                modifier = Modifier.size(92.dp)
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "ACCEND",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 42.sp,
                    letterSpacing = 6.sp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, Color(0xFFA8A8B8))
                    )
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "EXECUTION OVER EXCUSES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = MonoFontFamily,
                    letterSpacing = 2.4.sp
                ),
                color = AccendPalette.TextMuted
            )
        }
    }
}
