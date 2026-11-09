package com.hocchu.mobile.ui

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hocchu.mobile.data.DemoContent

@Composable
fun QuizScreen(speak: (String) -> Unit, onExit: () -> Unit) {
    var question by rememberSaveable { mutableIntStateOf(0) }
    var selection by rememberSaveable { mutableStateOf("") }
    var checked by rememberSaveable { mutableStateOf(false) }
    var finished by rememberSaveable { mutableStateOf(false) }
    val item = DemoContent.questions[question]
    ScreenColumn {
        if(finished) {
            Paper {
                Text("🌟", Modifier.fillMaxWidth(), fontSize = 70.sp, textAlign = TextAlign.Center)
                SectionTitle("Bé đã làm xong!", "Cảm ơn bé đã chăm chỉ luyện tập.")
                PlayButton("Làm lại", icon = Icons.Rounded.Refresh) {
                    question = 0; selection = ""; checked = false; finished = false
                }
                PlayButton("Về lớp học", color = Color.White, onClick = onExit)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CandyProgress(question.toFloat() / DemoContent.questions.size, Modifier.weight(1f))
                Text("${question + 1} / ${DemoContent.questions.size}", fontWeight = FontWeight.Bold)
            }
            SectionTitle(item.prompt, "Chạm vào chữ bé chọn nhé")
            Paper {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(item.picture, fontSize = 95.sp)
                    Text(item.word.replace("_", if(checked) item.answer else "□"), color = SkyDark,
                        fontSize = 60.sp, fontWeight = FontWeight.Bold)
                    RoundAction("Nghe từ", Icons.AutoMirrored.Rounded.VolumeUp) { speak(item.word.replace("_", item.answer)) }
                }
            }
            item.options.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEach { option ->
                        Surface(onClick = { selection = option }, enabled = !checked, modifier = Modifier.weight(1f).heightIn(min = 90.dp).semantics { selected = selection == option },
                            color = if(selection == option) PaleBlue else Color.White,
                            shape = RoundedCornerShape(22.dp),
                            border = androidx.compose.foundation.BorderStroke(if(selection == option) 3.dp else 1.dp,
                                if(selection == option) Sky else Outline)) {
                            Box(contentAlignment = Alignment.Center) { Text(option, fontSize = 42.sp, color = Ink) }
                        }
                    }
                }
            }
            if(checked) ResultPopup(if(selection == item.answer) "Bé chọn đúng rồi!" else "Bé thử chọn chữ khác nhé!", selection == item.answer) {
                if(selection == item.answer) {
                    if(question == DemoContent.questions.lastIndex) finished = true else question++
                }
                selection = ""; checked = false
            }
            PlayButton("Kiểm tra", enabled = selection.isNotEmpty() && !checked, icon = Icons.Rounded.Check) {
                checked = true
            }
        }
    }
}

@Composable
fun SentenceScreen(speak: (String) -> Unit, onExit: () -> Unit) {
    val words = listOf("Bé", "có", "ba", "quả bóng", "đi", "xe")
    val target = listOf(0, 1, 2, 3)
    var chosen by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var checked by rememberSaveable { mutableStateOf(false) }
    val correct = chosen == target
    ScreenColumn {
        SectionTitle("Ghép câu hoàn chỉnh", "Nghe câu mẫu, rồi chạm các thẻ từ theo thứ tự")
        Paper(color = PaleBlue) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Mascot(Modifier.size(72.dp))
                Column(Modifier.weight(1f)) {
                    Text("Bé có ba quả bóng", style = MaterialTheme.typography.titleLarge)
                    Text("Chạm vào loa để nghe lại", fontSize = 13.sp, color = Muted)
                }
                RoundAction("Nghe câu mẫu", Icons.AutoMirrored.Rounded.VolumeUp) { speak("Bé có ba quả bóng") }
            }
        }
        Paper {
            Text("Câu của bé", fontWeight = FontWeight.Bold, color = Muted)
            FlowRow(Modifier.fillMaxWidth().heightIn(min = 110.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if(chosen.isEmpty()) Text("Chọn các thẻ từ bên dưới…", color = Muted, modifier = Modifier.padding(top = 24.dp))
                chosen.forEach { index ->
                    WordChip(words[index], selected = true) { if(!checked) chosen = chosen - index }
                }
            }
            HorizontalDivider(color = Outline)
            Text("Chạm lại một thẻ để bỏ khỏi câu", color = Muted, fontSize = 13.sp)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(2, 4, 0, 3, 1, 5).forEach { index ->
                WordChip(words[index], enabled = index !in chosen && !checked) { chosen = chosen + index }
            }
        }
        if(checked) ResultPopup(if(correct) "Bé ghép đúng rồi!" else "Bé thử xếp lại nhé!", correct) {
            checked = false; chosen = emptyList()
            if(correct) onExit()
        }
        PlayButton("Kiểm tra", enabled = chosen.isNotEmpty() && !checked, icon = Icons.Rounded.Check) {
            checked = true
        }
        PlayButton("Xếp lại từ đầu", color = Color.White, icon = Icons.Rounded.Refresh,
            enabled = chosen.isNotEmpty()) { chosen = emptyList(); checked = false }
    }
}

@Composable
private fun WordChip(word: String, selected: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(20.dp), shadowElevation = if(enabled) 3.dp else 0.dp,
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
