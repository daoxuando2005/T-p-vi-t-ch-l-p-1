package com.hocchu.mobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.activity.compose.BackHandler
import androidx.compose.ui.unit.dp
import com.hocchu.mobile.classroom.*
import com.hocchu.mobile.data.AlphabetContent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun ClassroomScreen(model: ClassroomViewModel?, speak: (String) -> Unit) {
    val state = model?.state ?: ClassroomState()
    var groupName by rememberSaveable(state.classId) { mutableStateOf<String?>(null) }
    val group = AssignmentGroup.entries.find { it.name == groupName }
    model?.let {SubmissionReviewDialog(it.review,it::closeReview,it::retryReview)}
    if(state.selected != null) {
        AssignmentScreen(state, model!!, speak)
        return
    }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf("") }
    var leaving by remember {mutableStateOf(false)}
    val focus = LocalFocusManager.current
    val selected = state.classes.find { it.id == state.classId }
    BackHandler(group != null) { groupName = null }
    if(leaving)AlertDialog(onDismissRequest={leaving=false},title={Text("Rời ${selected?.name?:"lớp học"}?")},
        text={Text("Bé sẽ không nhận bài mới của lớp. Bài đã nộp vẫn được giữ; bé có thể dùng mã lớp để tham gia lại.")},
        confirmButton={TextButton(onClick={leaving=false;model?.leave()}){Text("Rời lớp")}},dismissButton={TextButton(onClick={leaving=false}){Text("Ở lại")}})
    ScreenColumn {
        Paper {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(selected?.name ?: "Lớp của bé", style = MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f,false),
                            maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        AssignmentCount(state.assignments.pendingCount(state.classId),Modifier.testTag("selected-class-pending"))
                    }
                    selected?.let { Text(it.teacher, color = Muted) }
                }
                OutlinedButton(onClick = { expanded = !expanded }, enabled = model != null,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("join-expand")) { Text("Tham gia lớp") }
            }
            androidx.compose.animation.AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(code, { code = it.take(20) }, label = { Text("Mã lớp từ cô giáo") },
                        enabled = !state.joining, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("class-code"))
                    PlayButton(if(state.joining) "Đang tham gia…" else "Vào lớp", enabled = code.isNotBlank() && !state.joining,
                        modifier = Modifier.testTag("class-join")) { focus.clearFocus(); model?.join(code) }
                }
            }
            if(state.classes.size > 1) FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                state.classes.forEach { school ->
                    FilterChip(selected = state.classId == school.id, onClick = { model?.chooseClass(school.id) },
                        label = { Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text(school.name); AssignmentCount(state.assignments.pendingCount(school.id),Modifier.testTag("class-pending-${school.id}"))
                        } }, modifier = Modifier.testTag("class-${school.id}"))
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            TextButton(onClick = { model?.refresh() }, enabled = model != null && !state.loading) {
                Icon(Icons.Rounded.Refresh, null); Text("Tải lại")
            }
            if(selected!=null) TextButton(onClick={leaving=true},enabled=!state.joining&&!state.submitting){Text("Rời lớp",color=MaterialTheme.colorScheme.error)}
            }
        }
        if(state.loading) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Đang tải lớp học…", color = Muted) }
        state.error?.let { ClassroomError(it) { model?.refresh() } }
        state.notice?.let { notice -> Paper {
            Text(notice, color = SuccessText)
            TextButton(onClick = { model?.dismissNotice() }) { Text("Đã hiểu") }
        } }
        if(!state.loading && state.classes.isEmpty() && state.error == null) {
            Text("Bé chưa tham gia lớp nào. Nhập mã cô giáo cung cấp để nhận bài tập nhé.", color = Muted)
        }
        val assignments = state.assignments.filter { it.classId == state.classId }
        if(group==null) {
            SectionTitle("Bài tập của bé")
            AssignmentGroups(assignments, null) {groupName=it.name}
        } else {
            TextButton(onClick={groupName=null},modifier=Modifier.testTag("assignment-groups-back")){Text("‹ Các hoạt động")}
            SectionTitle(group.title)
            if(!state.loading && assignments.none {it.group()==group}) Text("Lớp chưa có bài tập ở mục này.", color=Muted)
        }
        // Keep unknown legacy formats visible rather than silently hiding assigned work.
        assignments.filter {if(group==null)it.group()==null else it.group()==group}.forEach { assignment ->
            Paper {
                Text(assignment.title, style = MaterialTheme.typography.titleLarge)
                if(assignment.description.isNotBlank()) Text(assignment.description, color = Muted)
                if(assignment.dueDate.isNotBlank()) Text("Hạn nộp: ${dateLabel(assignment.dueDate)}", color = Muted)
                Text(if(assignment.submitted) assignment.score?.let { "Điểm: ${String.format(java.util.Locale.ROOT, "%.1f", it)} / 10" }
                    ?: "Đã nộp · Chờ cô chấm" else "Chưa nộp", color = if(assignment.submitted) SuccessText else SkyDark)
                if(assignment.feedback.isNotBlank()) Text(assignment.feedback)
                if(!assignment.submitted) {
                    if(assignment.content == AssignmentContent.Unsupported) Text("Bài này chưa mở được trên app. Bé báo cô kiểm tra nội dung nhé.", color = Muted)
                    else PlayButton("Làm bài", modifier = Modifier.testTag("assignment-${assignment.id}")) { model?.open(assignment.id) }
                } else TextButton(onClick={model?.review(assignment.id)},modifier=Modifier.testTag("review-assignment-${assignment.id}")){Text("Xem bài đã nộp")}
            }
        }
    }
}

@Composable
private fun AssignmentScreen(state: ClassroomState, model: ClassroomViewModel, speak: (String) -> Unit) {
    val assignment = state.selected ?: return
    val read=rememberMediaSpeech(speak)
    var confirm by rememberSaveable(assignment.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = model::close, enabled = !state.submitting) { Text("Về lớp") }
            Text(assignment.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("submission-error")) }
        if(state.opening) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else Box(Modifier.weight(1f).fillMaxWidth()) {
            when(val content = assignment.content) {
                is AssignmentContent.Quiz -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    content.questions.forEachIndexed { index, question -> Paper {
                        Text("Câu ${index + 1}: ${question.text}", style = MaterialTheme.typography.titleMedium)
                        if(question.imageUrl.isNotBlank()) LearningPicture(question.imageUrl,"",question.spoken)
                        TextButton(onClick = { read(question.spoken,question.audioUrl) }) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, null); Text("Nghe câu hỏi") }
                        question.options.forEachIndexed { optionIndex, value ->
                            FilterChip(selected = state.draft.answers.getOrNull(index) == value,
                                enabled = !state.submitting, onClick = { model.answer(index, value) },
                                label = { Text(value) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                                    .testTag("answer-$index-$optionIndex"))
                        }
                    } }
                }
                is AssignmentContent.Writing -> AssignmentWriting(content, state.draft, !state.submitting,
                    model::letter, model::drawing, speak)
                AssignmentContent.Unsupported -> Text("Nội dung bài chưa được hỗ trợ.")
            }
        }
        PlayButton(if(state.submitting) "Đang gửi bài…" else "Nộp bài cho cô", enabled = !state.submitting &&
            !state.opening && state.draft.ready(assignment.content), modifier = Modifier.testTag("submit-assignment")) { confirm = true }
    }
    if(confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Gửi bài cho cô nhé?") },
        text = { Text("Bé kiểm tra lại bài trước khi nộp. Sau khi gửi, cô có thể xem và nhận xét bài của bé.") },
        confirmButton = { TextButton(onClick = { confirm = false; model.submit() }, modifier = Modifier.testTag("confirm-submit")) { Text("Nộp bài") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Xem lại") } })
}

@Composable
private fun AssignmentWriting(content: AssignmentContent.Writing, draft: AssignmentDraft, enabled: Boolean,
    changeLetter: (Int) -> Unit, changeDrawing: (String, List<DraftStroke>) -> Unit, speak: (String) -> Unit) {
    val index = draft.letterIndex.coerceIn(content.letters.indices)
    val letter = content.letters[index]
    val saved = draft.drawings[letter].orEmpty()
    val ink = remember(saved) { saved.map { InkStroke(it.points.map { p -> Offset(p.x, p.y) }, it.pen) } }
    var active by remember(letter) { mutableStateOf(emptyList<Offset>()) }
    var pen by rememberSaveable { mutableIntStateOf(1) }
    var clear by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { changeLetter(index - 1) }, enabled = enabled && index > 0) { Text("Trước") }
            Text("Chữ $letter · ${index + 1}/${content.letters.size}")
            TextButton(onClick = { changeLetter(index + 1) }, enabled = enabled && index < content.letters.lastIndex) { Text("Tiếp") }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(minOf(maxWidth, maxHeight)).testTag("class-writing-board")) {
                WritingBoard(letter.lowercase(), ink, active, pen,
                    onStart = { if(enabled) active = listOf(it) }, onMove = { if(enabled && active.size < 12000) active = active + it },
                    onEnd = { if(enabled && active.isNotEmpty()) changeDrawing(letter, saved + DraftStroke(active.map { DraftPoint(it.x, it.y) }, pen)); active = emptyList() },
                    onCancel = { active = emptyList() })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = false, enabled = enabled, onClick = { pen = (pen + 1) % 3 }, label = { Text("Nét ${pen + 1}") })
            TextButton(onClick = { changeDrawing(letter, saved.dropLast(1)) }, enabled = enabled && saved.isNotEmpty()) { Text("Hoàn tác") }
            TextButton(onClick = { clear = true }, enabled = enabled && saved.isNotEmpty()) { Text("Xóa") }
            IconButton(onClick = { speak(AlphabetContent.lessons.first { it.letter == letter }.sound) }) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Nghe chữ") }
        }
        Text("Đã viết ${content.letters.count { draft.drawings[it].orEmpty().isNotEmpty() }}/${content.letters.size} chữ · Cô sẽ chấm bài", color = Muted,
            style = MaterialTheme.typography.bodySmall)
    }
    if(clear) AlertDialog(onDismissRequest = { clear = false }, title = { Text("Xóa chữ đang viết?") },
        confirmButton = { TextButton(onClick = { changeDrawing(letter, emptyList()); clear = false }) { Text("Xóa") } },
        dismissButton = { TextButton(onClick = { clear = false }) { Text("Giữ lại") } })
}

@Composable
private fun ClassroomError(message: String, retry: () -> Unit) { Paper {
    Text(message, color = MaterialTheme.colorScheme.error)
    TextButton(onClick = retry) { Text("Thử lại") }
} }
private fun dateLabel(value: String): String = try {
    LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
} catch (_: Exception) { value }
