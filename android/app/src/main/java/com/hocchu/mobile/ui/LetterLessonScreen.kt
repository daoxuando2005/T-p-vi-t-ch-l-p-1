package com.hocchu.mobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hocchu.mobile.data.*

@Composable
fun LetterLessonScreen(index: Int, saved: LessonProgress, onSave: (LessonProgress) -> Unit,
                      speak: (String) -> Unit, onPraise: () -> Unit, onExit: () -> Unit, onNext: () -> Unit,
                      onActivity: (LessonProgress,LessonProgress,Boolean)->Unit = {_,_,_->},
                      onWriting: (Boolean)->Unit = {}) {
    // A review has its own session and never changes or erases earned progress.
    val review = rememberSaveable(index) { saved.complete }
    var reviewValue by rememberSaveable(index) { mutableStateOf(LessonProgress().encode()) }
    val progress = if (review) LessonProgress.decode(reviewValue) else saved
    var step by rememberSaveable(index) { mutableIntStateOf(progress.stage.ordinal) }
    val stage = LessonStage.entries[step]
    fun save(next: LessonProgress, advance: Boolean = true) {
        if (review) reviewValue = next.encode() else onSave(next)
        if(next!=progress) onActivity(progress,next,review)
        if (advance) step = next.stage.ordinal
    }
    val letter = LocalStudyContent.current.lessons[index]
    Column(Modifier.fillMaxSize()) {
        if (stage != LessonStage.Done) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val labels = listOf("Đọc", "Viết ${progress.writes}/2", "Từ ${progress.words}/3", "Câu ${progress.sentences}/3")
                labels.forEachIndexed { i, label ->
                    Surface(Modifier.weight(1f), color = if (i == step) Green else if(i < step) PaleBlue else Color.White,
                        shape = MaterialTheme.shapes.medium) {
                        Text(label, Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1)
                    }
                }
            }
            Text(if (review) "Ôn lại chữ ${letter.letter}" else "Chặng ${step + 1} / 4 · Chữ ${letter.letter}",
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp).testTag("lesson-stage"), color = Muted, fontSize = 12.sp)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (stage) {
                LessonStage.Read -> {
                    val read=rememberMediaSpeech(speak)
                    var heard by rememberSaveable(index) { mutableStateOf(false) }
                    ScreenColumn {
                        SectionTitle("Nghe rồi đọc theo nhé", "Làm quen chữ ${letter.letter} và ${letter.letter.lowercase()}")
                        LetterCard(letter.letter) { heard = true; read(letter.sound,letter.audioUrl) }
                        ExampleCard(letter.picture, letter.word,imageUrl=letter.imageUrl) { read(letter.word,letter.exampleAudioUrl) }
                        PlayButton("Con đã đọc theo", enabled = heard, icon = Icons.Rounded.Check,
                            modifier = Modifier.testTag("lesson-read-done")) { save(progress.finishReading()) }
                    }
                }
                LessonStage.Write -> WritingScreen(index, speak, onPraise,
                    guided = GuidedWriting(progress.writes,
                        onContinue = { save(progress.finishWriting()) },
                        onExtra = { save(progress.finishWriting(), advance = false) }),
                    onPracticed = {onWriting(review)})
                LessonStage.Words -> key(index, progress.words) {
                    LessonWordQuestion(index, progress.words, speak) { correct ->
                        save(progress.answerWord(progress.words, correct))
                    }
                }
                LessonStage.Sentences -> key(index, progress.sentences) {
                    LessonSentenceQuestion(index, progress.sentences, speak) { correct ->
                        save(progress.answerSentence(progress.sentences, correct))
                    }
                }
                LessonStage.Done -> ScreenColumn {
                    Paper(color = Color(0xFFE6F9CF)) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Mascot() }
                        SectionTitle(if(review) "Bé ôn tập thật chăm!" else "Hoàn thành chữ ${letter.letter}!",
                            "Đã đọc theo, luyện viết và làm xong 3 từ, 3 câu.")
                        Text("Phần viết ghi nhận lượt luyện của bé.", color = Muted)
                        if (index < AlphabetContent.lessons.lastIndex) PlayButton(
                            "Học chữ ${AlphabetContent.lessons[index + 1].letter}",
                            icon = Icons.AutoMirrored.Rounded.ArrowForward, onClick = onNext)
                        else Text("Bé đã hoàn thành hành trình 29 chữ cái. Cùng ôn lại nhé!", color = SuccessText)
                        PlayButton("Về hành trình", color = Color.White, icon = Icons.Rounded.Home, onClick = onExit)
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonWordQuestion(index: Int, question: Int, speak: (String) -> Unit, onAnswer: (Boolean) -> Unit) {
    val live = LocalStudyContent.current.practices[index][question]
    val item = remember(index,question) {live}
    val answer = AlphabetContent.lessons[index].letter.lowercase()
    var selected by rememberSaveable { mutableStateOf("") }
    var checked by rememberSaveable { mutableStateOf(false) }
    ScreenColumn {
        SectionTitle("Điền chữ còn thiếu", "Từ ${question + 1} / 3 · Nghe từ và nhìn tranh để chọn")
        Paper {
            Text(item.picture, fontSize = 70.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text(if (checked && selected == answer) item.text else item.withBlank(answer),
                style = MaterialTheme.typography.headlineMedium, color = SkyDark)
            PlayButton("Nghe từ", color = Orange, icon = Icons.AutoMirrored.Rounded.VolumeUp) { speak(item.text) }
        }
        LetterCurriculum.options(index, question).chunked(2).forEach { options ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEach { option ->
                    OutlinedButton(onClick = { selected = option }, enabled = !checked,
                        modifier = Modifier.weight(1f).heightIn(min = 64.dp).testTag("word-option-$option"),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected == option) PaleBlue else Color.White)) {
                        Text(option, fontSize = 30.sp, color = Ink)
                    }
                }
            }
        }
        PlayButton("Kiểm tra", enabled = selected.isNotEmpty() && !checked, icon = Icons.Rounded.Check) { checked = true }
    }
    if (checked) ResultPopup(if(selected == answer) "Đúng rồi, từ này là ${item.text}!" else "Bé nghe lại và thử chọn chữ khác nhé!", selected == answer) {
        val correct = selected == answer
        checked = false
        selected = ""
        if (correct) onAnswer(true)
    }
}

@Composable
private fun LessonSentenceQuestion(index: Int, question: Int, speak: (String) -> Unit, onAnswer: (Boolean) -> Unit) {
    val live = LocalStudyContent.current.practices[index][question]
    val item = remember(index,question) {live}
    var chosen by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var checked by rememberSaveable { mutableStateOf(false) }
    val correct = chosen == item.sentence.indices.toList()
    ScreenColumn {
        SectionTitle("Ghép theo câu mẫu", "Câu ${question + 1} / 3 · Xếp đúng câu vừa nghe, chạm lại để bỏ")
        Paper(color = PaleBlue) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(item.picture, fontSize = 45.sp)
                Text("Từ vừa học: ${item.text}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            }
            PlayButton("Nghe câu mẫu", color = Sky, icon = Icons.AutoMirrored.Rounded.VolumeUp) { speak(item.sentence.joinToString(" ")) }
            Text("Bé có thể nghe từng thẻ từ trước khi ghép.", color = Muted, fontSize = 13.sp)
        }
        Paper {
            Text("Câu của bé", fontWeight = FontWeight.Bold)
            FlowRow(Modifier.fillMaxWidth().heightIn(min = 70.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (chosen.isEmpty()) Text("Chạm vào các thẻ từ bên dưới…", color = Muted)
                chosen.forEach { tile ->
                    WordChip(item.sentence[tile], selected = true, enabled = !checked) { chosen = chosen.filter { it != tile } }
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            List(item.sentence.size) {(it+1+question%(item.sentence.size-1))%item.sentence.size}.forEach { tile ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WordChip(item.sentence[tile], enabled = tile !in chosen && !checked,
                        modifier = Modifier.testTag("sentence-tile-$tile")) { chosen = chosen + tile }
                    TextButton(onClick = { speak(item.sentence[tile]) }) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, Modifier.size(18.dp))
                        Text("Nghe", fontSize = 12.sp)
                    }
                }
            }
        }
        PlayButton("Kiểm tra", enabled = chosen.size == item.sentence.size && !checked, icon = Icons.Rounded.Check) { checked = true }
        TextButton(onClick = { chosen = emptyList() }, enabled = chosen.isNotEmpty() && !checked) { Text("Xếp lại từ đầu") }
    }
    if (checked) ResultPopup(if(correct) "Bé ghép đúng rồi!" else "Bé nghe câu mẫu rồi xếp lại nhé!", correct) {
        checked = false
        if (correct) onAnswer(true) else chosen = emptyList()
    }
}
