package com.hocchu.mobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.activity.compose.BackHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hocchu.mobile.classroom.*
import com.hocchu.mobile.data.AlphabetContent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalContext
import com.hocchu.mobile.assessment.TraceRating
import kotlinx.coroutines.*

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
    var expanded by rememberSaveable(state.classId) { mutableStateOf(false) }
    var classesExpanded by rememberSaveable(state.classId) { mutableStateOf(false) }
    var joiningRequested by rememberSaveable { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf("") }
    var leaving by remember {mutableStateOf(false)}
    val focus = LocalFocusManager.current
    val selected = state.classes.find { it.id == state.classId }
    LaunchedEffect(state.joining, state.notice, state.error) {
        if(joiningRequested && !state.joining) {
            if(state.notice != null) {expanded=false;code="";focus.clearFocus();joiningRequested=false}
            else if(state.error != null) joiningRequested=false
        }
    }
    BackHandler(classesExpanded || expanded || group != null) {
        when { classesExpanded -> classesExpanded=false; expanded -> {expanded=false;focus.clearFocus()}; else -> groupName=null }
    }
    if(leaving)AlertDialog(onDismissRequest={leaving=false},title={Text("Rời ${selected?.name?:"lớp học"}?")},
        text={Text("Bé sẽ không nhận bài mới của lớp. Bài đã nộp vẫn được giữ; bé có thể dùng mã lớp để tham gia lại.")},
        confirmButton={TextButton(onClick={leaving=false;model?.leave()}){Text("Rời lớp")}},dismissButton={TextButton(onClick={leaving=false}){Text("Ở lại")}})
    ScreenColumn {
        Surface(shape=RoundedCornerShape(26.dp),color=androidx.compose.ui.graphics.Color.White,
            border=BorderStroke(1.dp,Outline),shadowElevation=2.dp) {
          Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            val canSelect=model!=null && !state.joining && !state.submitting && !state.loading
            val fontScale=LocalDensity.current.fontScale
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stacked=maxWidth<280.dp || fontScale>=1.35f
                val classPicker: @Composable (Modifier)->Unit = { modifier ->
                    Surface(onClick={classesExpanded=!classesExpanded;expanded=false;focus.clearFocus()},
                        enabled=canSelect&&state.classes.isNotEmpty(),shape=RoundedCornerShape(16.dp),color=PaleBlue,
                        border=BorderStroke(1.dp,Sky.copy(alpha=.35f)),modifier=modifier.heightIn(min=64.dp)
                            .testTag("class-selector-toggle").semantics {
                                role=Role.Button;stateDescription=if(classesExpanded) "Danh sách lớp đang mở" else "Danh sách lớp đã thu gọn"
                            }) {
                        Row(Modifier.padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                                Text(selected?.name?:"Lớp của bé",color=Ink,fontSize=19.sp,lineHeight=25.sp,
                                    fontWeight=FontWeight.ExtraBold,maxLines=2,overflow=TextOverflow.Ellipsis)
                                selected?.let {Text(it.teacher,color=Muted,fontSize=15.sp,lineHeight=21.sp,
                                    fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis)}
                            }
                            AssignmentCount(if(selected==null) 0 else state.assignments.pendingCount(state.classId),Modifier.testTag("selected-class-pending"))
                            Icon(if(classesExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,null,tint=SkyDark,
                                modifier=Modifier.size(22.dp))
                        }
                    }
                }
                val joinButton: @Composable (Modifier)->Unit = { modifier ->
                    OutlinedButton(onClick={expanded=!expanded;classesExpanded=false;focus.clearFocus()},enabled=canSelect,
                        shape=RoundedCornerShape(16.dp),contentPadding=PaddingValues(horizontal=12.dp,vertical=10.dp),
                        colors=ButtonDefaults.outlinedButtonColors(contentColor=SkyDark,
                            containerColor=if(expanded) PaleBlue else androidx.compose.ui.graphics.Color.White),
                        border=BorderStroke(1.dp,Sky.copy(alpha=.4f)),modifier=modifier.heightIn(min=48.dp).testTag("join-expand")
                            .semantics {stateDescription=if(expanded) "Ô nhập mã lớp đang mở" else "Ô nhập mã lớp đã thu gọn"}) {
                        Text("Tham gia lớp",fontSize=16.sp,fontWeight=FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Icon(if(expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,null,Modifier.size(18.dp))
                    }
                }
                if(stacked) Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    classPicker(Modifier.fillMaxWidth());joinButton(Modifier.fillMaxWidth())
                } else Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    classPicker(Modifier.weight(1f));joinButton(Modifier)
                }
            }
            androidx.compose.animation.AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(code, { code = it.take(20) }, label = { Text("Mã lớp từ cô giáo") },
                        enabled = !state.joining, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("class-code"))
                    PlayButton(if(state.joining) "Đang tham gia…" else "Vào lớp", enabled = code.isNotBlank() && !state.joining,
                        modifier = Modifier.testTag("class-join")) { focus.clearFocus();joiningRequested=true;model?.join(code) }
                }
            }
            androidx.compose.animation.AnimatedVisibility(classesExpanded && state.classes.isNotEmpty()) {
              Column(verticalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.testTag("class-selector-list")) {
                Column(Modifier.heightIn(max=280.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                  state.classes.forEach { school ->
                    val chosen=state.classId==school.id
                    Surface(onClick={model?.chooseClass(school.id);classesExpanded=false},enabled=canSelect,
                        color=if(chosen) PaleBlue else androidx.compose.ui.graphics.Color.White,contentColor=Ink,
                        shape=RoundedCornerShape(16.dp),border=BorderStroke(if(chosen) 2.dp else 1.dp,if(chosen) Sky else Outline),
                        modifier=Modifier.fillMaxWidth().heightIn(min=64.dp).testTag("class-${school.id}")
                            .semantics {stateDescription=if(chosen) "Lớp đang chọn" else "Chọn lớp này"}) {
                        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                                Text(school.name,color=Ink,fontWeight=FontWeight.Bold,fontSize=18.sp,lineHeight=24.sp)
                                Text(school.teacher,color=Muted,fontWeight=FontWeight.SemiBold,fontSize=15.sp,lineHeight=21.sp)
                            }
                            AssignmentCount(state.assignments.pendingCount(school.id),Modifier.testTag("class-pending-${school.id}"))
                            Icon(if(chosen) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,null,tint=SkyDark,modifier=Modifier.size(22.dp))
                        }
                    }
                  }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    TextButton(onClick={model?.refresh()},enabled=canSelect,modifier=Modifier.heightIn(min=48.dp)) {
                        Icon(Icons.Rounded.Refresh,null,tint=SkyDark);Spacer(Modifier.width(6.dp));Text("Tải lại",color=SkyDark,fontWeight=FontWeight.Bold)
                    }
                    if(selected!=null) TextButton(onClick={classesExpanded=false;leaving=true},enabled=canSelect,modifier=Modifier.heightIn(min=48.dp)) {
                        Text("Rời lớp",color=MaterialTheme.colorScheme.error,fontWeight=FontWeight.Bold)
                    }
                }
              }
            }
            if(state.classes.isEmpty())TextButton(onClick={model?.refresh()},enabled=canSelect,modifier=Modifier.heightIn(min=48.dp)) {
                Icon(Icons.Rounded.Refresh,null);Spacer(Modifier.width(6.dp));Text("Tải lại",fontWeight=FontWeight.Bold)
            }
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
                is AssignmentContent.Sentence -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    content.questions.forEachIndexed { i,q->Paper {
                        Text("Câu ${i+1}: ${q.prompt}",style=MaterialTheme.typography.titleMedium)
                        TextButton(onClick={speak(q.words.joinToString(" "))}) {Icon(Icons.AutoMirrored.Rounded.VolumeUp,null);Text("Nghe câu mẫu")}
                        val chosen=state.draft.sentences.getOrElse(i) {emptyList()}
                        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            chosen.forEachIndexed {position,tile->WordChip(q.words[tile],selected=true,enabled=!state.submitting) {
                                model.sentence(i,chosen.filterIndexed {n,_->n!=position})
                            }}
                        }
                        if(chosen.isEmpty())Text("Chạm các thẻ từ bên dưới để xếp câu",color=Muted)
                        val order=remember(assignment.id,i,q.words) {q.words.indices.shuffled(kotlin.random.Random(assignment.id.toInt()+i))}
                        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            order.forEach {tile->WordChip(q.words[tile],Modifier.testTag("sentence-$i-$tile"),enabled=!state.submitting&&tile !in chosen) {
                                model.sentence(i,chosen+tile)
                            }}
                        }
                        TextButton(onClick={model.sentence(i,emptyList())},enabled=!state.submitting&&chosen.isNotEmpty()){Text("Xếp lại")}
                    }}
                }
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
    var rating by remember(letter,saved){mutableStateOf<TraceRating?>(null)}
    var checking by remember {mutableStateOf(false)}
    var gradeError by remember {mutableStateOf<String?>(null)}
    val resources=LocalContext.current.resources;val scope=rememberCoroutineScope()
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rating?.let {Text("Bám mẫu: ${it.score}/100 · ${it.feedback}",color=SkyDark)}
        gradeError?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { changeLetter(index - 1) }, enabled = enabled && index > 0) { Text("Trước") }
            Text("Chữ $letter · ${index + 1}/${content.letters.size}")
            TextButton(enabled=enabled&&ink.isNotEmpty()&&!checking,onClick={
                checking=true;gradeError=null;scope.launch {try {rating=withContext(Dispatchers.Default){assessInk(resources,letter.lowercase(),ink)}}
                    catch(e:CancellationException){throw e}catch(_:Exception){gradeError="Chưa kiểm tra được nét, bé thử lại nhé."}finally{checking=false}}
            }){Text(if(checking)"Đang chấm…" else "Kiểm tra nét")}
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
