package com.hocchu.mobile.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sin
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hocchu.mobile.data.DemoContent

@Composable
fun HomeScreen(current: Int, onRead: () -> Unit, onWrite: () -> Unit, onClasses: () -> Unit, onLetter: (Int) -> Unit) {
    val list = rememberLazyListState()
    var lastFocusedLetter by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(current) {
        if (current != lastFocusedLetter) {
            list.animateScrollToItem(current + 2)
            lastFocusedLetter = current
        }
    }
    val scope = rememberCoroutineScope()
    val pulse by rememberInfiniteTransition(label = "Chữ đang học").animateFloat(
        initialValue = .94f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse), label = "Nhịp chuyển động")
    LazyColumn(
        state = list,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    ) {
        item {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle("Hôm nay bé học gì?", "Chọn một hoạt động yêu thích nhé")
        BoxWithConstraints {
            val wide = maxWidth >= 650.dp
            if (wide) Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ActivityCard("Đọc chữ", "Nghe và làm quen", Icons.AutoMirrored.Rounded.MenuBook, Sky, Modifier.weight(1f), onRead)
                ActivityCard("Tập viết", "Nét chữ xinh xắn", Icons.Rounded.Edit, Green, Modifier.weight(1f), onWrite)
                ActivityCard("Lớp học", "Bài tập của bé", Icons.Rounded.School, Orange, Modifier.weight(1f), onClasses)
            } else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ActivityCard("Đọc chữ", "Nghe và làm quen", Icons.AutoMirrored.Rounded.MenuBook, Sky, onClick = onRead)
                ActivityCard("Tập viết", "Nét chữ xinh xắn", Icons.Rounded.Edit, Green, onClick = onWrite)
                ActivityCard("Lớp học", "Bài tập của bé", Icons.Rounded.School, Orange, onClick = onClasses)
            }
        }
        Spacer(Modifier.height(24.dp))
        }
        }
        item {
            SectionTitle("Hành trình chữ cái", "Khám phá 29 chữ cái tiếng Việt")
            TextButton(onClick = { scope.launch { list.animateScrollToItem(current + 2) } }) {
                Text("Đến chữ ${DemoContent.lessons[current].letter} đang học")
            }
            Spacer(Modifier.height(18.dp))
        }
        items(DemoContent.lessons.size, key = { DemoContent.lessons[it].letter }) { index ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                JourneyLetter(index, current, { pulse }) { onLetter(index) }
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            PlayButton("Tiếp tục chữ ${DemoContent.lessons[current].letter}",
                icon = Icons.AutoMirrored.Rounded.ArrowForward, onClick = { onLetter(current) })
        }
    }
}

@Composable
private fun JourneyLetter(index: Int, current: Int, pulse: () -> Float, onClick: () -> Unit) {
    val selected = index == current
    BoxWithConstraints(Modifier.widthIn(max = 440.dp).fillMaxWidth().height(144.dp)) {
        val position = .5f + .28f * sin(index * Math.PI.toFloat() / 3f)
        val next = .5f + .28f * sin((index + 1) * Math.PI.toFloat() / 3f)
        if(index < DemoContent.lessons.lastIndex) Canvas(Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(size.width * position, 50.dp.toPx())
                cubicTo(size.width * position, 105.dp.toPx(),
                    size.width * next, 130.dp.toPx(),
                    size.width * next, size.height + 50.dp.toPx())
            }
            drawPath(path, Outline, style = Stroke(width = 9.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        if (index % 3 == 1) {
            JourneyDecoration(
                scene = index / 3,
                modifier = Modifier.align(if (position > .5f) Alignment.CenterStart else Alignment.CenterEnd)
                    .size(88.dp).graphicsLayer { translationY = (pulse() - 1f) * 80f }
            )
        }
        Column(
            Modifier.offset(x = maxWidth * position - 54.dp).width(108.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.height(24.dp), contentAlignment = Alignment.Center) {
                if(selected) Text("ĐANG HỌC", color = SuccessText, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
            Surface(onClick = onClick, modifier = Modifier.semantics { this.selected = index == current; stateDescription = if (index == current) "Đang học" else "Chưa chọn" }.size(76.dp).graphicsLayer {
                scaleX = if(selected) pulse() else 1f
                scaleY = scaleX
            }, shape = CircleShape, color = if(selected) Green else PaleBlue,
                shadowElevation = if(selected) 6.dp else 2.dp,
                border = androidx.compose.foundation.BorderStroke(3.dp, if(selected) GreenDark else Outline)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(DemoContent.lessons[index].letter, fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold, color = if(selected) Color(0xFF1F5100) else SkyDark)
                }
            }
            Text("Chữ ${DemoContent.lessons[index].letter}", Modifier.padding(top = 6.dp),
                fontWeight = FontWeight.Bold, color = Ink)
        }
    }
}

@Composable
private fun ActivityCard(title: String, subtitle: String, icon: ImageVector, color: Color,
                         modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = color.copy(alpha = .09f), border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = .2f))) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(color), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(29.dp), tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, color = Muted, fontSize = 13.sp)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = color)
        }
    }
}

@Composable
fun LetterPicker(selected: Int, onSelect: (Int) -> Unit) {
    val state = rememberLazyListState()
    LaunchedEffect(selected) { state.animateScrollToItem(selected) }
    androidx.compose.foundation.lazy.LazyRow(
        state = state, modifier = Modifier.fillMaxWidth().testTag("letter-picker"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        items(DemoContent.lessons.size, key = { DemoContent.lessons[it].letter }) { index ->
            val lesson = DemoContent.lessons[index]
            Surface(
                onClick = { onSelect(index) },
                modifier = Modifier.size(54.dp).semantics { this.selected = index == selected },
                shape = RoundedCornerShape(16.dp),
                color = if (index == selected) Sky else Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, if(index == selected) SkyDark else Outline)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(lesson.letter, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold,
                        color = if(index == selected) Color(0xFF00374D) else Ink)
                }
            }
        }
    }
}

@Composable
fun ReadingScreen(index: Int, select: (Int) -> Unit, speak: (String) -> Unit, onWrite: () -> Unit) {
    val lesson = DemoContent.lessons[index]
    ScreenColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("CÙNG LÀM QUEN", color = Muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Chữ ${lesson.letter}", style = MaterialTheme.typography.headlineMedium)
            }
            Text("${index + 1} / ${DemoContent.lessons.size} chữ", color = SkyDark, fontWeight = FontWeight.Bold)
        }
        LetterPicker(index, select)
        BoxWithConstraints {
            val wide = maxWidth >= 650.dp
            if (wide) Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                LetterCard(lesson.letter, Modifier.weight(1f)) { speak(lesson.sound) }
                ExampleCard(lesson.picture, lesson.word, Modifier.weight(1f)) { speak(lesson.word) }
            } else Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                LetterCard(lesson.letter) { speak(lesson.sound) }
                ExampleCard(lesson.picture, lesson.word) { speak(lesson.word) }
            }
        }
        Paper(color = PaleBlue) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Rounded.RecordVoiceOver, null, tint = SkyDark, modifier = Modifier.size(32.dp))
                Text("Bé nghe rồi đọc theo nhé!", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
        }
        PlayButton("Tập viết chữ ${lesson.letter}", color = Sky, icon = Icons.Rounded.Edit, onClick = onWrite)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PlayButton("Trước", Modifier.weight(1f), Color.White, Icons.AutoMirrored.Rounded.ArrowBack, enabled = index > 0) { select(index - 1) }
            PlayButton("Tiếp theo", Modifier.weight(1f), icon = Icons.AutoMirrored.Rounded.ArrowForward,
                enabled = index < DemoContent.lessons.lastIndex) { select(index + 1) }
        }
        if (index == DemoContent.lessons.lastIndex) Text("Bé đã xem hết bảng chữ cái. Chọn chữ bất kỳ để nghe lại nhé!", color = SuccessText)
    }
}

@Composable
private fun LetterCard(letter: String, modifier: Modifier = Modifier, onListen: () -> Unit) {
    Paper(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CHỮ HOA VÀ CHỮ THƯỜNG", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted)
            Text("$letter ${letter.lowercase()}", fontSize = 100.sp, fontWeight = FontWeight.Bold, color = SkyDark,
                modifier = Modifier.padding(vertical = 20.dp), textAlign = TextAlign.Center)
            PlayButton("Nghe chữ cái", color = Sky, icon = Icons.AutoMirrored.Rounded.VolumeUp, onClick = onListen)
        }
    }
}

@Composable
private fun ExampleCard(picture: String, word: String, modifier: Modifier = Modifier, onListen: () -> Unit) {
    Paper(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("TỪ VÍ DỤ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted)
            Text(picture, fontSize = 70.sp)
            Text(word, style = MaterialTheme.typography.headlineMedium)
            PlayButton("Nghe từ ví dụ", color = Orange, icon = Icons.AutoMirrored.Rounded.VolumeUp, onClick = onListen)
        }
    }
}

@Composable
fun ClassesScreen(onWriting: () -> Unit, onQuiz: () -> Unit, onSentence: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var invite by rememberSaveable { mutableStateOf("") }
    var notice by remember { mutableStateOf(false) }
    ScreenColumn {
        SectionTitle("Lớp học của bé", "Cùng cô và các bạn học điều mới")
        Paper {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Rounded.School, null, Modifier.size(52.dp), tint = SkyDark)
                Column {
                    Text("Lớp 1A", style = MaterialTheme.typography.headlineMedium)
                    Text("Cô Mai • Lớp minh họa", color = Muted)
                    Text("Mã lớp: A108", color = SkyDark, fontWeight = FontWeight.Bold)
                }
            }
        }
        TextButton(onClick = { expanded = !expanded }) { Text(if(expanded) "Thu gọn mã lớp ▴" else "+ Tham gia bằng mã lớp") }
        androidx.compose.animation.AnimatedVisibility(visible = expanded) { Paper {
            OutlinedTextField(invite, onValueChange = { invite = it.take(20).uppercase() },
                label = { Text("Nhập mã lớp") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp))
            PlayButton("Vào lớp", enabled = invite.isNotBlank(), icon = Icons.AutoMirrored.Rounded.Login) { notice = true }
        } }
        SectionTitle("Bài tập của bé", "3 hoạt động để bé trải nghiệm")
        ActivityCard("Tập viết chữ", "Chữ b • Viết trên bảng ô ly", Icons.Rounded.Edit, Sky, onClick = onWriting)
        ActivityCard("Đố vui ghép chữ", "3 câu hỏi • Chọn đáp án đúng", Icons.Rounded.Extension, Green, onClick = onQuiz)
        ActivityCard("Ghép câu", "Chạm các từ để tạo thành câu", Icons.Rounded.AutoAwesome, Orange, onClick = onSentence)
    }
    if (notice) AlertDialog(onDismissRequest = { notice = false }, title = { Text("Mã lớp đã nhập") },
        text = { Text("Bản giao diện đã nhận mã $invite. Tham gia lớp thật sẽ được kết nối với hệ thống ở bước tiếp theo.") },
        confirmButton = { TextButton(onClick = { notice = false }) { Text("Đã hiểu") } })
}
