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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.hocchu.mobile.data.DemoContent
import com.hocchu.mobile.data.LessonProgress
import com.hocchu.mobile.data.LessonProgressStore
import com.hocchu.mobile.classroom.ClassroomViewModel
import com.hocchu.mobile.family.StudentActivityModel

private val destinations = listOf("Trang chủ", "Đọc chữ", "Tập viết", "Lớp học")
private val symbols = listOf(Icons.Rounded.Home, Icons.AutoMirrored.Rounded.MenuBook, Icons.Rounded.Edit, Icons.Rounded.Groups)

@Composable
fun StudentApp(progressStore: LessonProgressStore? = null, preferencesName: String = "learning",
               studentName: String? = null, onLogout: (() -> Unit)? = null, classroom: ClassroomViewModel? = null,
               activities: StudentActivityModel? = null) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var exercise by rememberSaveable { mutableStateOf("") }
    val contextForProgress = LocalContext.current
    val prefs = remember(preferencesName) { contextForProgress.getSharedPreferences(preferencesName, 0) }
    val lessonStore = progressStore ?: remember(prefs) { LessonProgressStore(prefs) }
    var lessonProgress by remember { mutableStateOf(lessonStore.readAll()) }
    val journeyCurrent = LessonProgress.current(lessonProgress)
    var journeyIndex by rememberSaveable { mutableIntStateOf(-1) }
    var journeyEntry by rememberSaveable { mutableIntStateOf(0) }
    var reading by rememberSaveable { mutableIntStateOf(prefs.getInt("letter", 0).coerceIn(0, 28)) }
    val selectLetter: (Int) -> Unit = { reading = it; prefs.edit().putInt("letter", it).apply() }
    var initialLetter by rememberSaveable { mutableIntStateOf(0) }
    var writingEntry by rememberSaveable { mutableIntStateOf(0) }
    var profile by remember { mutableStateOf(false) }
    var sound by rememberSaveable { mutableStateOf(prefs.getBoolean("sound", true)) }
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
    val openLesson: (Int) -> Unit = { letter ->
        if (LessonProgress.unlocked(letter, lessonProgress)) {
            if (journeyIndex != letter || lessonProgress[letter].complete) {
                stateHolder.removeState("journey:$journeyIndex:$journeyEntry")
                journeyEntry++
            }
            journeyIndex = letter
            tab = 0
            exercise = "lesson"
        }
    }
    BackHandler(exercise.isNotEmpty() || tab != 0) {
        if(tab == 3 && classroom?.state?.selectedId != null) classroom.close()
        else if (exercise.isNotEmpty()) exercise = "" else tab = 0
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

                        Text(when(exercise) { "lesson" -> "Cùng học chữ ${DemoContent.lessons[journeyIndex].letter}"
                            else -> if (tab == 3) "Lớp học của bé" else destinations[tab] },
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
                        val key = if (exercise == "lesson") "journey:$journeyIndex:$journeyEntry"
                            else exercise.ifEmpty { if (tab == 2) "writing:$writingEntry" else "tab$tab" }
                        stateHolder.SaveableStateProvider(key) {
                            when(exercise) {
                                "lesson" -> LetterLessonScreen(journeyIndex, lessonProgress[journeyIndex],
                                    onSave = { lessonProgress = lessonStore.save(journeyIndex, it) },
                                    speak = speak,
                                    onPraise = { if(sound) speech?.speak("Bé đã rất chăm chỉ! Cùng luyện tiếp nhé!") },
                                    onExit = { exercise = "" }, onNext = { openLesson(journeyIndex + 1) },
                                    onActivity={old,next,review->activities?.lesson(journeyIndex,old,next,review)},
                                    onWriting={review->activities?.record("WRITING",journeyIndex,"JOURNEY",review)})
                                else -> when(tab) {
                                    0 -> HomeScreen(journeyCurrent, lessonProgress, onRead = { go(1) }, onWrite = { startWriting(reading) },
                                        onClasses = { go(3) }, onLetter = openLesson)
                                    1 -> ReadingScreen(reading, selectLetter, speak, onWrite = { startWriting(reading) },onRead={activities?.record("READING",reading)})
                                    2 -> WritingScreen(initialLetter, speak, onPraise = {
                                        if (sound) speech?.speak("Bé đã rất chăm chỉ! Mình cùng luyện tiếp nhé!")
                                    },onPracticed={activities?.record("WRITING",it)})
                                    3 -> ClassroomScreen(classroom, speak)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (profile) AlertDialog(onDismissRequest = { profile = false },
        title = { Text(studentName ?: "Cùng khám phá Vui Học!") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Bài cô giao được gửi đến cô khi bé nộp. Hoạt động tự học được gửi để phụ huynh theo dõi khi có kết nối.")
            if(activities!=null) {
                TextButton(onClick=activities::showLinkCode,enabled=!activities.codeLoading) {Text(if(activities.codeLoading) "Đang lấy mã…" else "Liên kết phụ huynh")}
                if(activities.pending>0) Text("Còn ${activities.pending} hoạt động chờ gửi",color=Muted)
                activities.error?.let {Text(it,color=MaterialTheme.colorScheme.error);TextButton(onClick=activities::retry) {Text("Thử gửi lại")}}
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Âm thanh", Modifier.weight(1f)); Switch(sound, onCheckedChange = { sound = it; prefs.edit().putBoolean("sound", it).apply() })
            }
        } }, confirmButton = { TextButton(onClick = { profile = false }) { Text("Đóng") } },
        dismissButton = { if(onLogout != null) TextButton(onClick = { profile = false; onLogout() }) { Text("Đăng xuất") } })
    if (speechNotice) AlertDialog(onDismissRequest = { speechNotice = false },
        title = { Text("Âm thanh tiếng Việt") },
        text = { Text("Máy đang khởi tạo giọng đọc hoặc chưa có giọng tiếng Việt. Nếu chưa nghe được, hãy kiểm tra phần Chuyển văn bản thành giọng nói trong cài đặt thiết bị.") },
        confirmButton = { TextButton(onClick = { speechNotice = false }) { Text("Đã hiểu") } })
    activities?.code?.let {link ->
        val clipboard=androidx.compose.ui.platform.LocalClipboardManager.current
        AlertDialog(onDismissRequest=activities::hideCode,title={Text(if(link.linked) "Phụ huynh đã liên kết" else "Mã liên kết với phụ huynh")},
            text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                if(link.linked) Text(link.parentName) else {
                    Text("Đưa mã này cho phụ huynh. Mã có hiệu lực 30 phút, không chia sẻ công khai.")
                    Text(link.code,style=MaterialTheme.typography.titleLarge)
                    TextButton(onClick={clipboard.setText(androidx.compose.ui.text.AnnotatedString(link.code))}) {Text("Sao chép mã")}
                }
            }},confirmButton={TextButton(onClick=activities::hideCode) {Text("Đóng")}})
    }
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
