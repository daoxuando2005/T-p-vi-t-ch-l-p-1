package com.hocchu.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hocchu.mobile.R

val Sky = Color(0xFF1CB0F6)
val SkyDark = Color(0xFF006590)
val Green = Color(0xFF58CC02)
val GreenDark = Color(0xFF46A302)
val SuccessText = Color(0xFF2B6C00)
val Orange = Color(0xFFFF9600)
val Gold = Color(0xFFFFC800)
val Ink = Color(0xFF263E50)
val Muted = Color(0xFF526879)
val Background = Color(0xFFF5FAFF)
val PaleBlue = Color(0xFFE5F3FF)
val Outline = Color(0xFFDBEAF4)
val Coral = Color(0xFFFF6464)
val Handwriting = FontFamily(Font(R.font.handwriting))

@Composable
fun VuiHocTheme(content: @Composable () -> Unit) {
    val rounded = FontFamily(
        Font(R.font.nunito_regular, FontWeight.Normal),
        Font(R.font.nunito_medium, FontWeight.Medium),
        Font(R.font.nunito_semibold, FontWeight.SemiBold),
        Font(R.font.nunito_bold, FontWeight.Bold),
        Font(R.font.nunito_extrabold, FontWeight.ExtraBold)
    )
    val defaults = Typography()
    val base = Typography(
        displayLarge = defaults.displayLarge.copy(fontFamily = rounded),
        displayMedium = defaults.displayMedium.copy(fontFamily = rounded),
        displaySmall = defaults.displaySmall.copy(fontFamily = rounded),
        headlineLarge = defaults.headlineLarge.copy(fontFamily = rounded),
        headlineMedium = defaults.headlineMedium.copy(fontFamily = rounded),
        headlineSmall = defaults.headlineSmall.copy(fontFamily = rounded),
        titleLarge = defaults.titleLarge.copy(fontFamily = rounded),
        titleMedium = defaults.titleMedium.copy(fontFamily = rounded),
        titleSmall = defaults.titleSmall.copy(fontFamily = rounded),
        bodyLarge = defaults.bodyLarge.copy(fontFamily = rounded),
        bodyMedium = defaults.bodyMedium.copy(fontFamily = rounded),
        bodySmall = defaults.bodySmall.copy(fontFamily = rounded),
        labelLarge = defaults.labelLarge.copy(fontFamily = rounded),
        labelMedium = defaults.labelMedium.copy(fontFamily = rounded),
        labelSmall = defaults.labelSmall.copy(fontFamily = rounded)
    )
    MaterialTheme(
        colorScheme = lightColorScheme(primary = SkyDark, secondary = GreenDark,
            background = Background, surface = Color.White, onSurface = Ink,
            onBackground = Ink, outline = Outline),
        typography = base.copy(
            headlineLarge = TextStyle(fontFamily = rounded, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold),
            headlineMedium = TextStyle(fontFamily = rounded, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold),
            titleLarge = TextStyle(fontFamily = rounded, fontSize = 21.sp, fontWeight = FontWeight.Bold),
            bodyLarge = TextStyle(fontFamily = rounded, fontSize = 18.sp, lineHeight = 27.sp),
            bodyMedium = TextStyle(fontFamily = rounded, fontSize = 16.sp, lineHeight = 23.sp),
            labelLarge = TextStyle(fontFamily = rounded, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        ), content = content
    )
}

@Composable
fun PlayButton(text: String, modifier: Modifier = Modifier, color: Color = Green,
               icon: ImageVector? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val shade = when (color) { Green -> GreenDark; Sky -> SkyDark; Orange -> Color(0xFFE58400); else -> Outline }
    Box(modifier.heightIn(min = 60.dp).clip(RoundedCornerShape(22.dp))
        .background(if (enabled) shade else Outline).padding(bottom = 5.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 55.dp)
            .clip(RoundedCornerShape(22.dp)).background(if (enabled) color else Color(0xFFEDF2F5))
            .clickable(enabled = enabled, role = androidx.compose.ui.semantics.Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            val foreground = if (!enabled) Muted else when(color) {
                Green -> Color(0xFF1F5100)
                Orange -> Color(0xFF603500)
                Sky -> Color(0xFF00374D)
                Color.White -> Ink
                else -> Color.White
            }
            if (icon != null) {
                Icon(icon, null, Modifier.size(25.dp), tint = foreground)
                Spacer(Modifier.width(9.dp))
            }
            Text(text, modifier = Modifier.weight(1f, fill = false), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelLarge, color = foreground)
        }
    }
}

@Composable
fun Paper(modifier: Modifier = Modifier, color: Color = Color.White,
          content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(26.dp), color = color,
        border = androidx.compose.foundation.BorderStroke(1.dp, Outline), shadowElevation = 2.dp) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable
fun RoundAction(label: String, icon: ImageVector, onClick: () -> Unit) {
    FilledIconButton(onClick, Modifier.size(52.dp),
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = PaleBlue, contentColor = SkyDark)) {
        Icon(icon, label, Modifier.size(26.dp))
    }
}

@Composable
fun CandyProgress(fraction: Float, modifier: Modifier = Modifier, color: Color = Green) {
    Box(modifier.height(14.dp).clip(CircleShape).background(Outline).padding(3.dp)) {
        if (fraction > 0f) Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f))
            .clip(CircleShape).background(color)) {
            Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = .4f)))
        }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (subtitle != null) Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun Mascot(modifier: Modifier = Modifier) {
    // Original code-drawn bird; no dependency on remote prototype images.
    Canvas(modifier.size(110.dp)) {
        val w = size.width; val h = size.height
        drawOval(SkyDark, Offset(w * .1f, h * .14f), androidx.compose.ui.geometry.Size(w * .8f, h * .81f))
        drawOval(Sky, Offset(w * .08f, h * .08f), androidx.compose.ui.geometry.Size(w * .84f, h * .8f))
        for (x in listOf(.35f, .65f)) {
            drawCircle(Color.White, w * .16f, Offset(w * x, h * .38f))
            drawCircle(Ink, w * .075f, Offset(w * x, h * .39f))
            drawCircle(Color.White, w * .024f, Offset(w * (x + .025f), h * .365f))
        }
        val beak = Path().apply {
            moveTo(w * .4f, h * .58f); lineTo(w * .6f, h * .58f)
            quadraticTo(w * .5f, h * .8f, w * .4f, h * .58f); close()
        }
        drawPath(beak, Gold)
        drawOval(PaleBlue, Offset(w * .25f, h * .76f), androidx.compose.ui.geometry.Size(w * .5f, h * .13f))
    }
}
