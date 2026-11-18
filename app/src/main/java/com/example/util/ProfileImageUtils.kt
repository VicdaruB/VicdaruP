package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VicdaruSkyBlue
import com.example.ui.theme.VicdaruSoftPink
import java.io.ByteArrayOutputStream
import kotlin.math.min

object ProfileImageUtils {
    private const val TARGET_SIZE_PX = 160
    private const val MAX_BASE64_LENGTH = 36000

    /**
     * Loads a gallery photo from a Photo Picker URI, center-crops it to a square,
     * scales it to 160x160, and compresses it to a compact JPEG Base64 string
     * suitable for real-time Firestore synchronization.
     */
    fun encodeProfilePhotoToBase64(context: Context, uri: Uri): String? {
        return runCatching {
            val rawBitmap = decodeScaledBitmap(context, uri) ?: return null
            val side = min(rawBitmap.width, rawBitmap.height).coerceAtLeast(1)
            val x = (rawBitmap.width - side) / 2
            val y = (rawBitmap.height - side) / 2
            val square = Bitmap.createBitmap(rawBitmap, x, y, side, side)
            if (square !== rawBitmap) rawBitmap.recycle()

            val scaled = if (square.width != TARGET_SIZE_PX || square.height != TARGET_SIZE_PX) {
                Bitmap.createScaledBitmap(square, TARGET_SIZE_PX, TARGET_SIZE_PX, true).also {
                    if (it !== square) square.recycle()
                }
            } else {
                square
            }

            var quality = 78
            var encoded = ""
            while (quality >= 35) {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                val bytes = out.toByteArray()
                val candidate = Base64.encodeToString(bytes, Base64.NO_WRAP)
                encoded = candidate
                if (candidate.length <= MAX_BASE64_LENGTH) {
                    break
                }
                quality -= 12
            }
            scaled.recycle()
            encoded.takeIf { it.isNotBlank() && it.length <= 44000 }
        }.getOrNull()
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val w = info.size.width.coerceAtLeast(1)
                val h = info.size.height.coerceAtLeast(1)
                val minSide = min(w, h)
                if (minSide > 360) {
                    val scale = 360f / minSide.toFloat()
                    decoder.setTargetSize(
                        (w * scale).toInt().coerceAtLeast(1),
                        (h * scale).toInt().coerceAtLeast(1)
                    )
                }
            }
        } else {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, boundsOpts)
            }
            val minSide = min(boundsOpts.outWidth, boundsOpts.outHeight).coerceAtLeast(1)
            var sample = 1
            while (minSide / (sample * 2) >= 240) {
                sample *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            }
        }
    }

    fun decodeBase64ToImageBitmap(base64: String?): ImageBitmap? {
        if (base64.isNullOrBlank()) return null
        return runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
}

@Composable
fun ProfileAvatarBadge(
    photoBase64: String?,
    fallbackSymbol: String,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String = "Foto de perfil",
) {
    val decodedBitmap = remember(photoBase64) {
        ProfileImageUtils.decodeBase64ToImageBitmap(photoBase64)
    }
    val ringGradient = remember {
        Brush.linearGradient(listOf(VicdaruSkyBlue, VicdaruSoftPink))
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(ringGradient)
            .border(1.5.dp, ringGradient, CircleShape)
            .padding(2.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (decodedBitmap != null) {
            Image(
                bitmap = decodedBitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Text(
                text = fallbackSymbol.ifBlank { "★" },
                fontSize = (size.value * 0.44f).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
