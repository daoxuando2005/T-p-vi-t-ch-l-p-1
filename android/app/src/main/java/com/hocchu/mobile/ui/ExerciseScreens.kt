package com.hocchu.mobile.ui

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun WordChip(word: String, modifier: Modifier = Modifier, selected: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, modifier = modifier, shape = RoundedCornerShape(20.dp), shadowElevation = if(enabled) 3.dp else 0.dp,
        color = if(selected) PaleBlue else if(enabled) Color.White else Outline.copy(alpha = .5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Outline)) {
        Text(word, Modifier.padding(horizontal = 22.dp, vertical = 18.dp), fontSize = 24.sp,
            fontWeight = FontWeight.Bold, color = if(enabled) Ink else Muted)
    }
}

@Composable
fun ResultPopup(text: String, success: Boolean?, dismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = dismiss) {
        val scale = remember { androidx.compose.animation.core.Animatable(.72f) }
        LaunchedEffect(Unit) { scale.animateTo(1f, androidx.compose.animation.core.spring()) }
        Paper(Modifier.fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState()).graphicsLayer { scaleX = scale.value; scaleY = scale.value },
            color = if(success == false) Color(0xFFFFE8E5) else Color(0xFFE6F9CF)) {
            Icon(
                imageVector = when (success) {
                    true -> Icons.Rounded.CheckCircle
                    false -> Icons.Rounded.Refresh
                    null -> Icons.Rounded.Edit
                },
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if(success == false) MaterialTheme.colorScheme.error else SuccessText
            )
            Text(text, style = MaterialTheme.typography.titleLarge)
            PlayButton(if(success == false) "Thử lại" else "Tiếp tục", onClick = dismiss)
        }
    }
}
