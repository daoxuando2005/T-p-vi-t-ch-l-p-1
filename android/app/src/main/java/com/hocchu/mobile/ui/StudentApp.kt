package com.hocchu.mobile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

private val destinations = listOf("Trang chủ", "Đọc chữ", "Tập viết", "Lớp học")
private val symbols = listOf(Icons.Rounded.Home, Icons.AutoMirrored.Rounded.MenuBook, Icons.Rounded.Edit, Icons.Rounded.Groups)

@Composable
fun StudentApp() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var exercise by rememberSaveable { mutableStateOf("") }
    val contextForProgress = LocalContext.current
    val prefs = remember { contextForProgress.getSharedPreferences("learning", 0) }
    var reading by rememberSaveable { mutableIntStateOf(prefs.getInt("letter", 0).coerceIn(0, 28)) }
    val selectLetter: (Int) -> Unit = { reading = it; prefs.edit().putInt("letter", it).apply() }
    var initialLetter by rememberSaveable { mutableIntStateOf(0) }
    var writingEntry by rememberSaveable { mutableIntStateOf(0) }
    var profile by remember { mutableStateOf(false) }
    var sound by rememberSaveable { mutableStateOf(true) }
    var speechNotice by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current
    val speech = remember(context, inspection) { if (inspection) null else Speech(context) }
    DisposableEffect(speech) { onDispose { speech?.close() } }
    val speak: (String) -> Unit = { if (sound && speech?.speak(it) == false) speechNotice = true }
    val stateHolder = rememberSaveableStateHolder()
    val go: (Int) -> Unit = { tab = it; exercise = "" }
    val startWriting: (Int) -> Unit = { letter ->
        stateHolder.removeState("writing:$writingEntry")
        initialLetter = letter
        writingEntry++
        go(2)
    }
    BackHandler(exercise.isNotEmpty() || tab != 0) {
        if (exercise.isNotEmpty()) exercise = "" else tab = 0
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Background)) {
        val expanded = maxWidth >= 840.dp
        Scaffold(containerColor = Background,
            topBar = {
                Row(Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 0.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (exercise.isNotEmpty()) RoundAction("Quay lại", Icons.AutoMirrored.Rounded.ArrowBack) { exercise = "" }
                    else Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Mascot(Modifier.size(44.dp)) }
                    Column(Modifier.weight(1f)) {

                        Text(when(exercise) { "quiz" -> "Đố vui ghép chữ"; "sentence" -> "Ghép câu"; else -> destinations[tab] },
                            style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    RoundAction("Hồ sơ", Icons.Rounded.Person) { profile = true }
                }
            },
            bottomBar = {
                if (!expanded && exercise.isEmpty()) NavigationBar(containerColor = androidx.compose.ui.graphics.Color.White) {
                    destinations.forEachIndexed { index, title ->
                        NavigationBarItem(selected = tab == index, onClick = { go(index) },
                            modifier = Modifier.testTag("destination-$index"),
                            icon = { Icon(symbols[index], null) }, label = { Text(title, fontSize = 12.sp) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = SkyDark, selectedTextColor = SkyDark,
                                indicatorColor = PaleBlue))
                    }
                }
            }
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                if (expanded) Column(
                    Modifier.width(88.dp).fillMaxHeight()
                        .background(androidx.compose.ui.graphics.Color.White).padding(vertical = 4.dp)
                ) {
                    destinations.forEachIndexed { index, title ->
                        NavigationRailItem(selected = tab == index && exercise.isEmpty(), onClick = { go(index) },
                            modifier = Modifier.testTag("destination-$index").weight(1f),
                            icon = { Icon(symbols[index], null) },
                            label = { Text(title, fontSize = 12.sp) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = SkyDark,
                                selectedTextColor = SkyDark,
                                indicatorColor = PaleBlue
                            ))
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.widthIn(max = 1240.dp).fillMaxSize()) {
                        val key = exercise.ifEmpty { if (tab == 2) "writing:$writingEntry" else "tab$tab" }
                        stateHolder.SaveableStateProvider(key) {
                            when(exercise) {
                                "quiz" -> QuizScreen(speak) { exercise = "" }
                                "sentence" -> SentenceScreen(speak) { exercise = "" }
                                else -> when(tab) {
                                    0 -> HomeScreen(reading, onRead = { go(1) }, onWrite = { startWriting(reading) },
                                        onClasses = { go(3) }, onLetter = { selectLetter(it); go(1) })
                                    1 -> ReadingScreen(reading, selectLetter, speak, onWrite = { startWriting(reading) })
                                    2 -> WritingScreen(initialLetter, speak, onPraise = {
                                        if (sound) speech?.speak("Bé đã rất chăm chỉ! Mình cùng luyện tiếp nhé!")
                                    })
                                    3 -> ClassesScreen(onWriting = { startWriting(3) },
                                        onQuiz = { exercise = "quiz" }, onSentence = { exercise = "sentence" })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (profile) AlertDialog(onDismissRequest = { profile = false },
        title = { Text("Cùng khám phá Vui Học!") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Đây là bản xem trước giao diện học sinh. Lớp học, bài tập và tiến trình là dữ liệu mẫu; chưa kết nối tài khoản hoặc gửi bài lên hệ thống.")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Âm thanh", Modifier.weight(1f)); Switch(sound, onCheckedChange = { sound = it })
            }
        } }, confirmButton = { TextButton(onClick = { profile = false }) { Text("Bắt đầu khám phá") } })
    if (speechNotice) AlertDialog(onDismissRequest = { speechNotice = false },
        title = { Text("Âm thanh tiếng Việt") },
        text = { Text("Máy đang khởi tạo giọng đọc hoặc chưa có giọng tiếng Việt. Nếu chưa nghe được, hãy kiểm tra phần Chuyển văn bản thành giọng nói trong cài đặt thiết bị.") },
        confirmButton = { TextButton(onClick = { speechNotice = false }) { Text("Đã hiểu") } })
}

@Composable
fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp), content = content)
}

@Preview(name = "Điện thoại", widthDp = 412, heightDp = 915, showBackground = true)
@Preview(name = "Máy tính bảng", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun StudentAppPreview() { VuiHocTheme { StudentApp() } }
