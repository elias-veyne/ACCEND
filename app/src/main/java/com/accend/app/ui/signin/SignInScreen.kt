package com.accend.app.ui.signin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.accend.app.R
import com.accend.app.ui.theme.AccendPalette
import com.accend.app.ui.theme.AccendThemeTokens
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(
    onSignIn: (String) -> Unit,
    onMessage: (String) -> Unit
) {
    val c = AccendThemeTokens.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var busy by remember { mutableStateOf(false) }

    suspend fun idTokenFrom(option: CredentialOption): String {
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        error("Unsupported Google credential.")
    }

    fun startGoogleSignIn() {
        if (busy) return
        busy = true
        scope.launch {
            // default_web_client_id is generated from the type-3 (Web) OAuth client in
            // google-services.json — never use the Android client ID here.
            val serverClientId = context.getString(R.string.default_web_client_id)

            runCatching {
                // Primary attempt: show every Google account on the device. Auto-select must stay
                // off — on a fresh install it throws NoCredentialException instead of a picker.
                val option = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(false)
                    .build()
                idTokenFrom(option)
            }.recoverCatching { throwable ->
                if (throwable !is NoCredentialException) throw throwable
                // Fallback: force the explicit Google account picker so the user can always sign in.
                val pickerOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
                idTokenFrom(pickerOption)
            }.onSuccess { idToken ->
                onSignIn(idToken)
            }.onFailure { throwable ->
                busy = false
                onMessage(
                    if (throwable is NoCredentialException) {
                        "No Google account available on this device. Add one in Settings and retry."
                    } else {
                        throwable.message ?: "Google sign-in failed."
                    }
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bgBase)
            .padding(horizontal = 28.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black)
                .border(1.dp, c.borderStrong, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.accend_brand_mark),
                contentDescription = "ACCEND emblem",
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "ACCEND",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 8.sp
            ),
            color = c.textPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "DISCIPLINE · SYNCHRONIZED",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
            color = c.textMuted
        )
        Spacer(Modifier.height(48.dp))

        Button(
            onClick = { startGoogleSignIn() },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(14.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccendPalette.AccentWhite,
                contentColor = AccendPalette.OnAccentWhite
            )
        ) {
            if (busy) {
                CircularProgressIndicator(
                    color = AccendPalette.OnAccentWhite,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = "CONTINUE WITH GOOGLE",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Your progress is encrypted in transit and synced to your Firebase account. " +
                "Sign in to unlock cloud backup, EXP and the global leaderboard.",
            style = MaterialTheme.typography.bodySmall,
            color = c.textMuted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.app_name).uppercase() + " · V2.1 CLOUD",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
            color = c.textMuted
        )
    }
}
