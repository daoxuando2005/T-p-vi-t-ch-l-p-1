package com.hocchu.mobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.hocchu.mobile.data.DemoContent
import com.hocchu.mobile.family.*
import java.time.LocalDate

// Only compiled in debug. Preview fixtures are never a fallback for production API data.
private fun previewFamily(): ParentState {
    val day=LocalDate.now(SCHOOL_ZONE)
    val activity=day.atTime(9,15).atZone(SCHOOL_ZONE).toInstant()
    return ParentState(children=listOf(LinkedChild(1,"Nguyễn Minh An","preview-child")),selectedId=1,date=day,
        dashboard=ParentDashboard(1,"Minh An",day,1,2,3,1,listOf(
            ChildActivity("preview:1","SUBMISSION","","Nộp bài: Ôn tập chữ A",activity, null,"Đang chờ cô nhận xét"),
            ChildActivity("preview:2","WRITING","A","Tập viết chữ A",activity.minusSeconds(120),null,""),
            ChildActivity("preview:3","READING","A","Nghe và đọc chữ A",activity.minusSeconds(300),null,"")),
            DemoContent.lessons.mapIndexed {i,l->LetterStatus(l.letter,i<5,if(i<4) 2 else 0,if(i<4) 3 else 0,if(i<4) 3 else 0,i<4)},
            (6 downTo 0).map {DayCount(day.minusDays(it.toLong()),if(it%2==0) 7 else 3)},4,true,false),
        refreshedAt=activity)
}
@Preview(name="Parent today",widthDp=393,heightDp=852,showBackground=true)
@Composable
fun ParentTodayPreview() {VuiHocTheme {ParentScreen("Phụ huynh",previewFamily(),{},{},{},{},{})}}
@Preview(name="Parent progress",widthDp=393,heightDp=852,showBackground=true)
@Composable
fun ParentProgressPreview() {VuiHocTheme {ParentScreen("Phụ huynh",previewFamily(),{},{},{},{},{},initialTab=1)}}
@Preview(name="Parent family tablet",widthDp=960,heightDp=700,showBackground=true)
@Composable
fun ParentTabletPreview() {VuiHocTheme {ParentScreen("Phụ huynh",previewFamily(),{},{},{},{},{},initialTab=2)}}
