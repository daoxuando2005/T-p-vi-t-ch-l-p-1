package com.hocchu.mobile

import com.hocchu.mobile.classroom.*
import org.junit.Assert.*
import org.junit.Test

class AssignmentGroupTest {
    private fun task(id: Long, classId: Long, type: String, submitted: Boolean = false) = SchoolAssignment(
        id, classId, "Lớp $classId", "Ghép câu là tên bài, không phải định dạng", "", type, "", "",
        submitted, null, "", AssignmentContent.Unsupported)

    @Test fun badgesBelongToTheirClassAndGroupAndExcludeSubmittedWork() {
        val tasks = listOf(task(1,10,"WRITING"),task(2,10,"QUIZ"),task(3,10,"QUIZ",true),
            task(4,20,"QUIZ"),task(5,20,"UNKNOWN"))
        assertEquals(4,tasks.pendingCount())
        assertEquals(2,tasks.pendingCount(10))
        assertEquals(1,tasks.pendingCount(10,AssignmentGroup.Quiz))
        assertEquals(0,tasks.pendingCount(10,AssignmentGroup.Sentence))
        assertEquals(2,tasks.pendingCount(20))
        assertEquals(0,tasks.map {it.copy(submitted=true)}.pendingCount())
    }
    @Test fun categoryUsesServerTypeInsteadOfTitleOrUnsupportedParserResult() {
        assertEquals(AssignmentGroup.Writing,task(1,10,"writing").group())
        assertEquals(AssignmentGroup.Quiz,task(2,10,"QUIZ").group())
        assertEquals(AssignmentGroup.Sentence,task(3,10,"SENTENCE").group())
        assertNull(task(4,10,"UNKNOWN").group())
    }
}
