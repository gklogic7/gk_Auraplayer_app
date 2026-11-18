package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.TrackEntity
import com.example.ui.theme.AlabasterWhite
import com.example.ui.theme.GlassBorderSpecular

@Composable
fun TrackArtworkView(
    track: TrackEntity?,
    cornerRadius: Dp = 24.dp,
    showOverlayTypography: Boolean = true,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(cornerRadius)
    val primaryColor = Color(track?.primaryColorHex ?: 0xFF5E4534L)
    val secondaryColor = Color(track?.secondaryColorHex ?: 0xFF211812L)
    val accentColor = Color(track?.accentColorHex ?: 0xFFF5B041L)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(primaryColor, secondaryColor)
                )
            )
            .border(width = 0.8.dp, color = GlassBorderSpecular, shape = shape)
    ) {
        when {
            track?.artworkResName == "cover_piche_tere" -> {
                Image(
                    painter = painterResource(id = R.drawable.cover_piche_tere),
                    contentDescription = stringResource(R.string.cd_album_artwork),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            !track?.artworkUriString.isNullOrBlank() -> {
                AsyncImage(
                    model = track?.artworkUriString,
                    contentDescription = stringResource(R.string.cd_album_artwork),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                // Editorial acoustic vinyl & sun flare artwork for local/synthesized tracks
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width * 0.52f, size.height * 0.54f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.50f),
                                primaryColor.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.45f, size.height * 0.38f),
                            radius = size.minDimension * 0.65f
                        )
                    )
                    // Subtle vinyl grooves
                    val maxRadius = size.minDimension * 0.36f
                    for (ring in 1..4) {
                        drawCircle(
                            color = AlabasterWhite.copy(alpha = 0.08f),
                            radius = maxRadius * (ring / 4f),
                            center = center,
                            style = Stroke(width = 1.5f)
                        )
                    }
                    drawCircle(
                        color = accentColor.copy(alpha = 0.75f),
                        radius = size.minDimension * 0.07f,
                        center = center
                    )
                }

                if (showOverlayTypography && track != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 21.sp
                            ),
                            color = AlabasterWhite.copy(alpha = 0.92f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.album.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = AlabasterWhite.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
