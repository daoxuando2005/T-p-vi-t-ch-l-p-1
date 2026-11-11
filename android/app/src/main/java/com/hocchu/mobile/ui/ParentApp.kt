package com.hocchu.mobile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hocchu.mobile.data.DemoContent
import com.hocchu.mobile.family.*
import java.time.LocalDate
import java.time.Instant
import java.time.format.DateTimeFormatter

@Composable
fun ParentApp(name: String,model: ParentViewModel?,logout: ()->Unit) {
    val owner=LocalLifecycleOwner.current
    DisposableEffect(owner,model) {
        val observer=LifecycleEventObserver { _,event->if(event==Lifecycle.Event.ON_RESUME) model?.refresh() }
        owner.lifecycle.addObserver(observer)
        onDispose {owner.lifecycle.removeObserver(observer)}
    }
    ParentScreen(name,model?.state ?: ParentState(loading=true),{model?.refresh()}, {model?.chooseChild(it)},
        {model?.date(it)}, {model?.link(it)},logout)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ParentScreen(name: String,state: ParentState,refresh: ()->Unit,chooseChild: (Long)->Unit,
    selectDate: (LocalDate)->Unit,link: (String)->Unit,logout: ()->Unit,initialTab: Int=0) {
    var tab by rememberSaveable {mutableIntStateOf(initialTab)}
    var profile by remember {mutableStateOf(false)}
    var calendar by remember {mutableStateOf(false)}
    var childMenu by remember {mutableStateOf(false)}
    var code by rememberSaveable {mutableStateOf("")}
    var detail by remember {mutableStateOf<LetterStatus?>(null)}
    val child=state.children.find {it.id==state.selectedId}
    val titles=listOf("Hôm nay","Tiến độ","Gia đình")
    val symbols=listOf(Icons.Rounded.WbSunny,Icons.Rounded.Insights,Icons.Rounded.FamilyRestroom)
    BackHandler(tab!=0) {tab=0}
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val wide=maxWidth>=840.dp
    Scaffold(containerColor=Background,
        topBar={ Row(Modifier.fillMaxWidth().statusBarsPadding().heightIn(min=56.dp).padding(horizontal=16.dp),
            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Mascot(Modifier.size(44.dp))
            Column(Modifier.weight(1f)) {
                Text("Cùng con mỗi ngày",style=MaterialTheme.typography.titleLarge,color=SkyDark)
                Text("Góc phụ huynh",style=MaterialTheme.typography.bodySmall,color=Muted)
            }
            IconButton(onClick={profile=true},modifier=Modifier.testTag("parent-profile")) {Icon(Icons.Rounded.Person,"Tài khoản phụ huynh",tint=SkyDark)}
        } },
        bottomBar={if(!wide) NavigationBar(containerColor=Color.White) {titles.forEachIndexed { i,title->
            NavigationBarItem(selected=tab==i,onClick={tab=i},modifier=Modifier.testTag("parent-tab-$i"),
                icon={Icon(symbols[i],null)},label={Text(title)},colors=NavigationBarItemDefaults.colors(
                    selectedIconColor=SkyDark,selectedTextColor=SkyDark,indicatorColor=PaleBlue))
        }} }
    ) { padding->
        Row(Modifier.fillMaxSize().padding(padding)) {
        if(wide) NavigationRail(containerColor=Color.White) {titles.forEachIndexed { i,title->
            NavigationRailItem(selected=tab==i,onClick={tab=i},modifier=Modifier.testTag("parent-tab-$i"),
                icon={Icon(symbols[i],null)},label={Text(title)})
        }}
        Box(Modifier.weight(1f).fillMaxHeight(),contentAlignment=Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max=880.dp).fillMaxSize().imePadding().testTag("parent-feed"),
                contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                item { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    if(state.children.isNotEmpty()) OutlinedButton(onClick={childMenu=true},modifier=Modifier.weight(1f).heightIn(min=52.dp).testTag("parent-child-select")) {
                        Icon(Icons.Rounded.Face,null,Modifier.size(22.dp));Spacer(Modifier.width(8.dp))
                        Text(child?.name ?: "Chọn con",maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
                        Icon(Icons.Rounded.ExpandMore,null)
                    } else Text("Đồng hành cùng con",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.weight(1f))
                    FilledTonalIconButton(onClick=refresh,enabled=!state.loading,modifier=Modifier.size(52.dp).testTag("parent-refresh")) {
                        Icon(Icons.Rounded.Refresh,"Cập nhật hoạt động")
                    }
                } }
                if(state.loading) item {LinearProgressIndicator(Modifier.fillMaxWidth());Text("Đang cập nhật…",color=Muted,style=MaterialTheme.typography.bodySmall)}
                state.error?.let { message->item {Paper(color=Color(0xFFFFF1EE)) {
                    Text(message,color=MaterialTheme.colorScheme.error)
                    TextButton(onClick=refresh) {Text("Thử lại")}
                }} }
                if(tab!=2&&!state.loading&&state.error==null&&state.children.isEmpty()) item {Paper(color=PaleBlue) {
                    Icon(Icons.Rounded.FamilyRestroom,null,Modifier.size(48.dp),tint=SkyDark)
                    Text("Kết nối với con",style=MaterialTheme.typography.headlineSmall)
                    Text("Mở hồ sơ trong app của con, chọn “Liên kết phụ huynh” rồi nhập mã tại đây.",color=Muted)
                    PlayButton("Liên kết với con",icon=Icons.Rounded.PersonAdd,modifier=Modifier.testTag("parent-start-link")) {tab=2}
                }}
                if(tab==2) {
                    item {SectionTitle("Gia đình của mình")}
                    items(state.children,key={"child-${it.id}"}) { c->Paper {
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            Surface(shape=CircleShape,color=PaleBlue) {Icon(Icons.Rounded.Face,null,Modifier.padding(14.dp).size(28.dp),tint=SkyDark)}
                            Column(Modifier.weight(1f)) {Text(c.name,style=MaterialTheme.typography.titleLarge);Text(c.username,color=Muted)}
                            Icon(Icons.Rounded.Verified,"Đã liên kết",tint=SuccessText)
                        }
                        TextButton(onClick={chooseChild(c.id);tab=0}) {Text("Xem hoạt động của con")}
                    } }
                    item {Paper {
                        Text("Liên kết thêm con",style=MaterialTheme.typography.titleLarge)
                        Text("Mã có hiệu lực 30 phút và chỉ lấy từ hồ sơ của con. Không nhập tên đăng nhập thay mã.",color=Muted)
                        OutlinedTextField(code,{code=it.take(32)},label={Text("Mã liên kết từ app của con")},singleLine=true,
                            enabled=!state.linking,modifier=Modifier.fillMaxWidth().testTag("parent-link-code"))
                        PlayButton(if(state.linking) "Đang liên kết…" else "Liên kết",enabled=code.trim().length==22&&!state.linking,
                            modifier=Modifier.testTag("parent-link-submit")) {link(code)}
                        state.error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
                        state.notice?.let {Text(it,color=SuccessText,modifier=Modifier.testTag("parent-link-notice"))}
                    }}
                    item {Text("Chỉ tài khoản phụ huynh đã liên kết mới xem được hoạt động của con.",color=Muted,style=MaterialTheme.typography.bodySmall)}
                } else if(state.children.isNotEmpty()) {
                    if(tab==0) item { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        IconButton(onClick={selectDate(state.date.minusDays(1))},enabled=state.date>LocalDate.now(SCHOOL_ZONE).minusDays(90)) {
                            Icon(Icons.Rounded.ChevronLeft,"Ngày trước")}
                        TextButton(onClick={calendar=true},modifier=Modifier.weight(1f).testTag("parent-date")) {
                            Icon(Icons.Rounded.CalendarToday,null,Modifier.size(20.dp));Spacer(Modifier.width(8.dp))
                            Text(if(state.date==LocalDate.now(SCHOOL_ZONE)) "Hôm nay · ${dayLabel(state.date)}" else dayLabel(state.date))
                        }
                        IconButton(onClick={selectDate(state.date.plusDays(1))},enabled=state.date<LocalDate.now(SCHOOL_ZONE)) {Icon(Icons.Rounded.ChevronRight,"Ngày tiếp")}
                    } }
                    val dashboard=state.dashboard
                    if(dashboard!=null&&dashboard.studentId==state.selectedId) {
                        if(tab==0) {
                            item {Paper(color=PaleBlue) {
                                Text("Một ngày học của ${dashboard.studentName}",style=MaterialTheme.typography.headlineSmall)
                                Text(if(dashboard.timeline.isEmpty()) "Chưa nhận hoạt động đã đồng bộ cho ngày này." else "Những bước nhỏ, mỗi ngày một tiến bộ.",color=Muted)
                            }}
                            item {Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                ParentStat("Lượt đọc",dashboard.readings,Icons.AutoMirrored.Rounded.MenuBook,Modifier.weight(1f))
                                ParentStat("Lượt viết",dashboard.writings,Icons.Rounded.Edit,Modifier.weight(1f))
                            }}
                            item {Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                ParentStat("Câu luyện tập",dashboard.exercises,Icons.Rounded.Extension,Modifier.weight(1f))
                                ParentStat("Bài đã nộp",dashboard.submissions,Icons.Rounded.AssignmentTurnedIn,Modifier.weight(1f))
                            }}
                            item {SectionTitle("Con đã làm gì?")}
                            if(dashboard.timeline.isEmpty()) item {Paper {
                                Icon(Icons.Rounded.Spa,null,Modifier.size(40.dp),tint=SuccessText)
                                Text("Cùng chờ hoạt động mới từ con",style=MaterialTheme.typography.titleLarge)
                                Text("Bạn có thể đổi ngày hoặc bấm cập nhật để xem lại.",color=Muted)
                            }}
                            items(dashboard.timeline,key={it.id}) { activity->ParentActivityCard(activity) }
                            if(dashboard.truncated) item {Text("Đang hiển thị 200 hoạt động gần nhất của ngày.",color=Muted)}
                        } else {
                            item {Paper(color=PaleBlue) {
                                Text("Hành trình 29 chữ cái",style=MaterialTheme.typography.headlineSmall)
                                if(dashboard.progressAvailable) {
                                    Text("${dashboard.completedLetters} / 29 chữ đã hoàn thành",style=MaterialTheme.typography.titleLarge,color=SkyDark)
                                    LinearProgressIndicator(progress={dashboard.completedLetters/29f},modifier=Modifier.fillMaxWidth().height(10.dp))
                                } else Text("Chưa nhận tiến độ hành trình. Mở app của con và kết nối mạng để cập nhật.",color=Muted)
                                Text("Tiến độ phản ánh hoạt động đã ghi nhận, không phải điểm đánh giá khả năng đọc hay nét chữ.",style=MaterialTheme.typography.bodySmall,color=Muted)
                            }}
                            item {Paper {
                                Text("Các chữ con đang khám phá",style=MaterialTheme.typography.titleLarge)
                                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                    DemoContent.lessons.forEach { letter->
                                        val status=dashboard.progress.find {it.letter==letter.letter}
                                        val learned=status?.complete==true
                                        FilledTonalButton(onClick={if(status!=null) detail=status},contentPadding=PaddingValues(4.dp),
                                            enabled=status!=null,modifier=Modifier.size(48.dp).semantics {contentDescription="Chữ ${letter.letter}, "+if(learned) "đã hoàn thành" else "chưa hoàn thành"},
                                            colors=ButtonDefaults.filledTonalButtonColors(containerColor=if(learned) Color(0xFFE6F9CF) else PaleBlue)) {
                                            Text(letter.letter,fontWeight=FontWeight.Bold,fontSize=19.sp)
                                        }
                                    }
                                }
                            }}
                            item {Paper {
                                Text("7 ngày đến ${dayLabel(state.date)}",style=MaterialTheme.typography.titleLarge)
                                val max=(dashboard.week.maxOfOrNull {it.activities} ?: 0).coerceAtLeast(1)
                                dashboard.week.forEach { day->Column(Modifier.fillMaxWidth().semantics {contentDescription="${dayLabel(day.date)}: ${day.activities} hoạt động"}) {
                                    Row(Modifier.fillMaxWidth()) {Text(day.date.format(DateTimeFormatter.ofPattern("dd/MM")),Modifier.weight(1f),color=Muted);Text("${day.activities} hoạt động")}
                                    LinearProgressIndicator(progress={day.activities.toFloat()/max},modifier=Modifier.fillMaxWidth().padding(vertical=5.dp).height(8.dp),color=SkyDark,trackColor=Outline)
                                } }
                                Text("Lượt đọc, luyện tập và bài nộp đã được ghi nhận.",style=MaterialTheme.typography.bodySmall,color=Muted)
                            }}
                        }
                    }
                    state.refreshedAt?.let { at->item {Text("Đã cập nhật ${timeLabel(at)} · Giờ Việt Nam",color=Muted,style=MaterialTheme.typography.bodySmall)} }
                }
                item {Spacer(Modifier.height(8.dp))}
            }
        }
        }
    }
    }
    if(childMenu) AlertDialog(onDismissRequest={childMenu=false},title={Text("Chọn con để theo dõi")},
        text={Column {state.children.forEach { c->TextButton(onClick={chooseChild(c.id);childMenu=false},modifier=Modifier.fillMaxWidth().heightIn(min=52.dp).testTag("parent-child-${c.id}")) {Text(c.name)} }}},
        confirmButton={TextButton(onClick={childMenu=false}) {Text("Đóng")}})
    if(profile) AlertDialog(onDismissRequest={profile=false},title={Text(name)},text={Text("Tài khoản phụ huynh · Cùng con học từng ngày")},
        confirmButton={TextButton(onClick={profile=false}) {Text("Đóng")}},dismissButton={TextButton(onClick={profile=false;logout()},modifier=Modifier.testTag("parent-logout")) {Text("Đăng xuất")}})
    detail?.let { p->AlertDialog(onDismissRequest={detail=null},title={Text("Chữ ${p.letter}")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(if(p.read) "Đã hoàn thành lượt đọc" else "Chưa hoàn thành lượt đọc")
        Text("Tập viết: ${p.writes}/2 lượt");Text("Điền từ: ${p.words}/3 câu");Text("Ghép câu: ${p.sentences}/3 câu")
    }},confirmButton={TextButton(onClick={detail=null}) {Text("Đã hiểu")}}) }
    if(calendar) {
        val today=LocalDate.now(SCHOOL_ZONE)
        val picker=rememberDatePickerState(initialSelectedDateMillis=state.date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),selectableDates=object:SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {val d=Instant.ofEpochMilli(utcTimeMillis).atZone(java.time.ZoneOffset.UTC).toLocalDate();return d<=today&&d>=today.minusDays(90)}
        })
        DatePickerDialog(onDismissRequest={calendar=false},confirmButton={TextButton(onClick={picker.selectedDateMillis?.let {selectDate(Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate())};calendar=false}) {Text("Xem ngày này")}},
            dismissButton={TextButton(onClick={calendar=false}) {Text("Hủy")}}) {DatePicker(state=picker,title={Text("Chọn ngày học",Modifier.padding(24.dp))})}
    }
}

@Composable
private fun ParentStat(title: String,count: Int,icon: androidx.compose.ui.graphics.vector.ImageVector,modifier: Modifier) {
    Paper(modifier) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Icon(icon,null,tint=SkyDark,modifier=Modifier.size(24.dp))
            Column {Text(count.toString(),style=MaterialTheme.typography.headlineSmall,color=Ink);Text(title,color=Muted,style=MaterialTheme.typography.bodySmall)}
        }
    }
}
@Composable
private fun ParentActivityCard(activity: ChildActivity) {Paper {
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.Top) {
        val icon=when(activity.kind) {"READING"->Icons.AutoMirrored.Rounded.MenuBook;"WRITING"->Icons.Rounded.Edit;"SUBMISSION"->Icons.Rounded.AssignmentTurnedIn;"LESSON_COMPLETE"->Icons.Rounded.CheckCircle;else->Icons.Rounded.Extension}
        Surface(shape=CircleShape,color=if(activity.kind=="LESSON_COMPLETE") Color(0xFFE6F9CF) else PaleBlue) {Icon(icon,null,tint=SkyDark,modifier=Modifier.padding(12.dp).size(24.dp))}
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Text(activity.title,style=MaterialTheme.typography.titleMedium)
            Text(timeLabel(activity.occurredAt),color=Muted,style=MaterialTheme.typography.bodySmall)
            activity.score?.let {Text("Điểm: ${String.format(java.util.Locale.ROOT,"%.1f",it)} / 10",color=SuccessText)}
            if(activity.feedback.isNotBlank()) Text(activity.feedback,color=Muted)
            if(activity.kind=="WRITING") Text("Ghi nhận lượt luyện, chưa đánh giá nét viết",style=MaterialTheme.typography.bodySmall,color=Muted)
        }
    }
} }
private fun dayLabel(date: LocalDate)=date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
private fun timeLabel(instant: Instant)=instant.atZone(SCHOOL_ZONE).format(DateTimeFormatter.ofPattern("HH:mm"))
