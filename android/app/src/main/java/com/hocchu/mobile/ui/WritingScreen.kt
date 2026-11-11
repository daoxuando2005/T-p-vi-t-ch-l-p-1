package com.hocchu.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hocchu.mobile.data.AlphabetContent
import kotlinx.coroutines.CancellationException

// Store normalized positions, so resizing does not stretch the logical writing space.
internal data class InkStroke(val points: List<Offset>, val pen: Int)
private val StrokeSaver = listSaver<List<InkStroke>, ArrayList<Float>>(
    save = { strokes -> strokes.map { stroke -> ArrayList(listOf(stroke.pen.toFloat()) + stroke.points.flatMap { listOf(it.x, it.y) }) } },
    restore = { strokes -> strokes.map { values -> InkStroke(values.drop(1).chunked(2).map { Offset(it[0], it[1]) }, values[0].toInt()) } }
)

@Composable
fun WritingScreen(initial: Int, speak: (String) -> Unit, onPraise: () -> Unit, guided: GuidedWriting? = null, onPracticed: (Int)->Unit = {}) {
    var index by rememberSaveable(initial) { mutableIntStateOf(initial) }
    var strokes by rememberSaveable(index, stateSaver = StrokeSaver) { mutableStateOf(emptyList<InkStroke>()) }
    var active by remember(index) { mutableStateOf(emptyList<Offset>()) }
    var pen by rememberSaveable { mutableIntStateOf(1) }
    var complete by rememberSaveable { mutableStateOf(false) }
    var revision by rememberSaveable(index) {mutableLongStateOf(0L)}
    var recordedRevision by rememberSaveable(index) {mutableLongStateOf(-1L)}
    var confirmClear by remember { mutableStateOf(false) }
    var pendingLetter by rememberSaveable { mutableStateOf<Int?>(null) }
    var showReference by rememberSaveable { mutableStateOf(false) }
    val lesson = AlphabetContent.lessons[index]
    val letter = lesson.letter.lowercase()
    val form = CopybookForms.forLetter(letter)
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (guided == null) LetterPicker(index) { next ->
            if (next != index) {
                if (strokes.isEmpty()) index = next else pendingLetter = next
            }
        }
        if (guided != null) Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (guided.completed < 2) "Lượt viết ${guided.completed + 1} / 2 · Bé viết theo mẫu nhé"
                else "Luyện thêm · Bé có thể viết thêm", Modifier.weight(1f).padding(horizontal = 4.dp),
                fontSize = 13.sp, color = SkyDark)
            if (guided.completed >= 2) TextButton(onClick = guided.onContinue) { Text("Đố vui với từ") }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text("Cỡ vừa · ${form.caption}", fontSize = 12.sp, color = Muted)
            }
            IconButton(onClick = { showReference = true }, modifier = Modifier.semantics {
                contentDescription = "Xem cách viết chữ $letter"
            }) { Icon(Icons.Rounded.Info, null, tint = SkyDark) }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val board: @Composable (Modifier) -> Unit = { modifier ->
                BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
                    Box(Modifier.size(minOf(maxWidth, maxHeight))) {
                        WritingBoard(
                            letter, strokes, active, pen,
                            onStart = { active = listOf(it) },
                            onMove = { active = active + it },
                            onEnd = {
                                if (active.isNotEmpty()) {strokes = strokes + InkStroke(active, pen);revision++}
                                active = emptyList()
                            },
                            onCancel = { active = emptyList() }
                        )
                    }
                }
            }
            val tools: @Composable (Boolean) -> Unit = { sidePanel ->
                WritingTools(
                    pen = pen,
                    hasStrokes = strokes.isNotEmpty(),
                    sidePanel = sidePanel,
                    onPenChange = { pen = it },
                    onListen = { speak(lesson.sound) },
                    onUndo = { strokes = strokes.dropLast(1);revision++ },
                    onClear = { confirmClear = true },
                    onComplete = {
                        if (!complete) { complete = true; onPraise()
                            if(recordedRevision!=revision) {recordedRevision=revision;onPracticed(index)} }
                    }
                )
            }
            if (maxWidth >= 600.dp && maxWidth > maxHeight) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    board(Modifier.weight(1f).fillMaxHeight())
                    Column(Modifier.width(232.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        tools(true)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    board(Modifier.weight(1f).fillMaxWidth())
                    tools(false)
                }
            }
        }
    }
    if(confirmClear) AlertDialog(onDismissRequest = { confirmClear = false },
        title = { Text("Viết lại từ đầu nhé?") }, text = { Text("Các nét trên bảng sẽ được xóa.") },
        confirmButton = { TextButton(onClick = { strokes = emptyList(); confirmClear = false }) { Text("Viết lại") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Giữ bài viết") } })
    if (complete) WritingCompletion(
        letter = letter,
        lastLetter = guided == null && index == AlphabetContent.lessons.lastIndex,
        continueLabel = guided?.let { if (it.completed == 0) "Viết lượt 2" else "Đố vui với từ" },
        repeatLabel = if (guided != null && guided.completed >= 1) "Luyện thêm một lượt" else "Viết lại",
        onDismiss = { complete = false },
        onRepeat = {
            strokes = emptyList()
            complete = false
            if (guided != null && guided.completed >= 1) guided.onExtra()
        },
        onContinue = {
            strokes = emptyList()
            complete = false
            if (guided == null) index = (index + 1) % AlphabetContent.lessons.size else guided.onContinue()
        }
    )
    if (pendingLetter != null) AlertDialog(
        onDismissRequest = { pendingLetter = null },
        title = { Text("Đổi chữ luyện viết?") },
        text = { Text("Bảng viết hiện tại sẽ được xóa khi chuyển sang chữ mới.") },
        confirmButton = {
            TextButton(onClick = {
                index = pendingLetter ?: index
                pendingLetter = null
            }) { Text("Đổi chữ") }
        },
        dismissButton = { TextButton(onClick = { pendingLetter = null }) { Text("Viết tiếp") } }
    )
    if (showReference) WritingReference(form, onDismiss = { showReference = false })
}

data class GuidedWriting(val completed: Int, val onContinue: () -> Unit, val onExtra: () -> Unit)

@Composable
private fun WritingReference(form: CopybookForm, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cùng viết chữ ${form.letter}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(form.caption, fontWeight = FontWeight.Bold)
                CopybookPaper(form, Modifier.fillMaxWidth().aspectRatio(1f))
                Text("Bắt đầu từ chấm xanh, đi theo hướng mũi tên. Đường 1 là dòng chân chữ.")
                Text("Các phần theo thứ tự:", fontWeight = FontWeight.Bold)
                form.parts.forEachIndexed { order, part -> Text("${order + 1}. ${part.label}") }
                Text("Nối liền các nét khi cần. Viết thân chữ trước, thêm dấu sau.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Con hiểu rồi") } }
    )
}

@Composable
private fun WritingCompletion(
    letter: String,
    lastLetter: Boolean,
    onDismiss: () -> Unit,
    onRepeat: () -> Unit,
    onContinue: () -> Unit,
    continueLabel: String? = null,
    repeatLabel: String = "Viết lại"
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        val scale = remember { androidx.compose.animation.core.Animatable(.8f) }
        LaunchedEffect(Unit) { scale.animateTo(1f, androidx.compose.animation.core.spring()) }
        Paper(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
            color = Color(0xFFE6F9CF)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Mascot() }
            Text("Bé đã rất chăm chỉ!", style = MaterialTheme.typography.headlineMedium, color = SuccessText)
            Text(
                if (lastLetter) "Bé đã luyện đến cuối bảng chữ cái. Cùng ôn lại nhé!"
                else "Bé vừa luyện chữ $letter. Mỗi lần tập viết là một lần tiến bộ!"
            )
            PlayButton(
                continueLabel ?: if (lastLetter) "Luyện từ chữ A" else "Tiếp tục",
                icon = Icons.AutoMirrored.Rounded.ArrowForward,
                onClick = onContinue
            )
            PlayButton(repeatLabel, color = Color.White, icon = Icons.Rounded.Refresh, onClick = onRepeat)
        }
    }
}

@Composable
private fun WritingTools(
    pen: Int,
    hasStrokes: Boolean,
    sidePanel: Boolean,
    onPenChange: (Int) -> Unit,
    onListen: () -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onComplete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val penSelector: @Composable (Modifier) -> Unit = { modifier ->
                Box(modifier) {
                    OutlinedButton(
                        onClick = { menu = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                            contentDescription = "Độ đậm nét " + listOf("nhỏ", "vừa", "to")[pen]
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text(listOf("Nhỏ", "Vừa", "To")[pen], maxLines = 1)
                        Icon(Icons.Rounded.ArrowDropDown, null, Modifier.size(18.dp))
                    }
                    DropdownMenu(menu, { menu = false }) {
                        listOf("Nhỏ", "Vừa", "To").forEachIndexed { value, name ->
                            DropdownMenuItem(text = { Text(name) }, onClick = {
                                onPenChange(value)
                                menu = false
                            })
                        }
                    }
                }
            }
            if (sidePanel) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    penSelector(Modifier.weight(1f))
                    WritingTool("Nghe chữ", Icons.AutoMirrored.Rounded.VolumeUp, true, Modifier.weight(1f), onListen)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WritingTool("Hoàn tác", Icons.AutoMirrored.Rounded.Undo, hasStrokes, Modifier.weight(1f), onUndo)
                    WritingTool("Xóa bảng", Icons.Rounded.Delete, hasStrokes, Modifier.weight(1f), onClear)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    penSelector(Modifier.weight(1f))
                    WritingTool("Nghe", Icons.AutoMirrored.Rounded.VolumeUp, true, Modifier.weight(1f), onListen)
                    WritingTool("Hoàn tác", Icons.AutoMirrored.Rounded.Undo, hasStrokes, Modifier.weight(1f), onUndo)
                    WritingTool("Xóa", Icons.Rounded.Delete, hasStrokes, Modifier.weight(1f), onClear)
                }
            }
            PlayButton("Con viết xong", enabled = hasStrokes, icon = Icons.Rounded.Check, onClick = onComplete)
        }
    }
}

@Composable
internal fun WritingBoard(letter: String, strokes: List<InkStroke>, active: List<Offset>, pen: Int,
                         onStart: (Offset) -> Unit, onMove: (Offset) -> Unit, onEnd: () -> Unit, onCancel: () -> Unit) {
    // A fixed square canvas preserves the same aspect ratio on phones and tablets.
    val currentStart by rememberUpdatedState(onStart)
    val currentMove by rememberUpdatedState(onMove)
    val currentEnd by rememberUpdatedState(onEnd)
    val currentCancel by rememberUpdatedState(onCancel)
    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(18.dp))) {
        CopybookPaper(CopybookForms.forLetter(letter), Modifier.fillMaxSize(),
            showStart = strokes.isEmpty() && active.isEmpty())
        Canvas(Modifier.fillMaxSize().semantics { contentDescription = "Bảng ô ly tập viết chữ $letter" }
            .pointerInput(letter) {
                fun normalized(point: Offset) = Offset((point.x / size.width).coerceIn(0f, 1f), (point.y / size.height).coerceIn(0f, 1f))
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    currentStart(normalized(down.position))
                    try {
                        do {
                            val event = awaitPointerEvent()
                            val point = event.changes.firstOrNull { it.id == down.id }
                            event.changes.forEach { it.consume() }
                            if(point != null) currentMove(normalized(point.position))
                        } while(point != null && point.pressed)
                        currentEnd()
                    } catch(cancelled: CancellationException) {
                        currentCancel()
                        throw cancelled
                    }
                }
            }) {
            for(stroke in strokes + InkStroke(active, pen)) {
                val points = stroke.points
                val inkWidth = (.08f + stroke.pen * .04f) * size.width / CopybookPage.squares
                if(points.size == 1) drawCircle(Ink, inkWidth / 2f, Offset(points[0].x * size.width, points[0].y * size.height))
                if(points.size > 1) {
                    val path = Path().apply {
                        moveTo(points[0].x * size.width, points[0].y * size.height)
                        points.drop(1).forEach { lineTo(it.x * size.width, it.y * size.height) }
                    }
                    drawPath(path, Ink, style = Stroke(width = inkWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }
}

@Composable
private fun WritingTool(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    modifier: Modifier,
    action: () -> Unit
) {
    Surface(onClick = action, enabled = enabled, modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(12.dp), color = PaleBlue) {
        Column(Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = if (enabled) SkyDark else Muted, modifier = Modifier.size(22.dp))
            Text(label, fontSize = 12.sp, color = Muted, textAlign = TextAlign.Center)
        }
    }
}
