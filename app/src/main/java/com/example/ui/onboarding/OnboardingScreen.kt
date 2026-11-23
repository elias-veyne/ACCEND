package com.example.ui.onboarding

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.AppContainer
import com.example.R
import com.example.ui.components.AccendCard
import com.example.ui.components.PrimaryWhiteButton
import com.example.ui.theme.AccendPalette
import com.example.ui.theme.AccendThemeTokens
import com.example.ui.theme.MonoFontFamily
import com.example.util.ImageUtils
import com.example.work.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun OnboardingScreen(
    container: AppContainer,
    onCompleted: () -> Unit
) {
    val c = AccendThemeTokens.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var touched by remember { mutableStateOf(false) }
    var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var saving by remember { mutableStateOf(false) }

    val trimmed = name.trim()
    val isNameValid = trimmed.length in 1..24

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onCompleted()
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pickedPhotoUri = uri
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bgBase)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.accend_brand_mark),
                    contentDescription = "Accend emblem",
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "WELCOME TO ACCEND",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.6.sp
                    ),
                    color = c.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Build the person you'll be at 18.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))

                // 96dp Circular Avatar Picker
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .testTag("onboarding_avatar_picker")
                        .clickable {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(c.surfaceInner)
                            .border(1.5.dp, c.borderStrong, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (pickedPhotoUri != null) {
                            AsyncImage(
                                model = pickedPhotoUri,
                                contentDescription = "Selected avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            val initial = trimmed.firstOrNull()?.uppercaseChar()?.toString() ?: "A"
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = AccendPalette.AccentWhite
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AccendPalette.AccentWhite)
                            .border(2.dp, c.bgBase, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = "Choose photo",
                            tint = AccendPalette.OnAccentWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "TAP TO SET OPERATOR AVATAR (OPTIONAL)",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textMuted
                )

                Spacer(Modifier.height(28.dp))

                AccendCard {
                    Text(
                        text = "OPERATOR IDENTIFIER",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.textMuted
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            touched = true
                            if (it.length <= 24) name = it
                        },
                        placeholder = {
                            Text("Enter your name", color = c.textMuted)
                        },
                        singleLine = true,
                        isError = touched && !isNameValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccendPalette.AccentWhite,
                            unfocusedBorderColor = c.borderStrong,
                            focusedTextColor = c.textPrimary,
                            unfocusedTextColor = c.textPrimary,
                            cursorColor = AccendPalette.AccentWhite
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    if (touched && !isNameValid) {
                        Text(
                            text = "Name must be between 1 and 24 characters.",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.danger
                        )
                    } else {
                        Text(
                            text = "100% local-first · No cloud · No accounts · 365-day protocol.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textMuted
                        )
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(24.dp))
                PrimaryWhiteButton(
                    text = if (saving) "INITIALIZING..." else "BEGIN PROTOCOL",
                    icon = Icons.Outlined.Bolt,
                    enabled = isNameValid && !saving,
                    testTag = "onboarding_begin_button",
                    onClick = {
                        if (!isNameValid || saving) return@PrimaryWhiteButton
                        saving = true
                        scope.launch {
                            val savedPfp = withContext(Dispatchers.IO) {
                                pickedPhotoUri?.let { ImageUtils.saveProfilePhoto(context, it) }
                            }
                            withContext(Dispatchers.IO) {
                                container.userRepo.completeOnboarding(trimmed, savedPfp)
                                container.dayEvaluator.catchUp()
                                val prefs = container.prefsRepo.getSnapshot()
                                NotificationScheduler.sync(context, prefs)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onCompleted()
                            }
                        }
                    }
                )
            }
        }
    }
}
