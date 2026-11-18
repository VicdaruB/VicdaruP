package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.audio.HapticsHelper
import com.example.data.attemptAutoSignIn
import com.example.data.onGoogleSignInClicked
import com.example.ui.components.SoftCyberParticleCanvas
import com.example.ui.theme.VicdaruSkyBlue
import com.example.ui.theme.VicdaruSoftPink

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = remember(context) { HapticsHelper(context) }
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        SoftCyberParticleCanvas()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(28.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.hero_cyber_zen_1794974224612),
                    contentDescription = "Horizonte celeste y rosa de Vicdaru Project",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.72f))
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(VicdaruSkyBlue, VicdaruSoftPink)))
                        .padding(3.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.vicdaru_app_logo_1794974223268),
                        contentDescription = "Logotipo de Vicdaru Project",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Vicdaru Project",
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Impulso diario anime, hábitos serenos, música adaptativa y retos con amigos.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AuthHighlightRow(Icons.Outlined.AutoAwesome, "Frases antidepresivas e imágenes anime en vivo")
                AuthHighlightRow(Icons.Outlined.GraphicEq, "Visualizador musical que se adapta a tu enfoque")
                AuthHighlightRow(Icons.Outlined.Terrain, "Escalada con avatares desbloqueables y medallas")
                AuthHighlightRow(Icons.Outlined.PeopleOutline, "Sincronización en la nube y multijugador con amigos")
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (errorMsg != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Button(
                onClick = {
                    haptics.subtleTap()
                    errorMsg = null
                    isLoading = true
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            haptics.quoteDelivered()
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            haptics.errorNotice()
                            errorMsg = msg
                        },
                        scope = scope,
                        onAuthCancelled = {
                            isLoading = false
                            haptics.habitUndo()
                        }
                    )
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VicdaruSkyBlue,
                    contentColor = Color(0xFF09131F)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("google_sign_in_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = Color(0xFF09131F)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Conectando...", fontWeight = FontWeight.Bold)
                } else {
                    Text("Iniciar sesión con Google", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AuthHighlightRow(icon: ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
