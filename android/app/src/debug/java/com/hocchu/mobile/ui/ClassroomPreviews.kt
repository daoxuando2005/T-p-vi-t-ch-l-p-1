package com.hocchu.mobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.hocchu.mobile.classroom.*

private fun classroomPreviewTasks() = listOf(
    SchoolAssignment(1,10,"Lớp 1A","Tập viết A","","WRITING","","",false,null,"",AssignmentContent.Unsupported),
    SchoolAssignment(2,10,"Lớp 1A","Chọn chữ","","QUIZ","","",false,null,"",AssignmentContent.Unsupported),
    SchoolAssignment(3,10,"Lớp 1A","Ôn chữ B","","QUIZ","","",false,null,"",AssignmentContent.Unsupported))

@Preview(name="Classroom activity cards",widthDp=393,heightDp=780,showBackground=true)
@Preview(name="Classroom tablet cards",widthDp=1200,heightDp=800,showBackground=true)
@Preview(name="Classroom large text",widthDp=393,heightDp=900,fontScale=1.5f,showBackground=true)
@Composable
fun ClassroomGroupsPreview() { VuiHocTheme { ScreenColumn {
    SectionTitle("Bài tập của bé")
    AssignmentGroups(classroomPreviewTasks(),null) {}
} } }
