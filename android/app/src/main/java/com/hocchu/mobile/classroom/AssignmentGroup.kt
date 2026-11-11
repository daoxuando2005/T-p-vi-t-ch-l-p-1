package com.hocchu.mobile.classroom

enum class AssignmentGroup(val title: String) {
    Writing("Tập viết chữ"), Quiz("Đố vui ghép chữ"), Sentence("Ghép câu")
}

/** Classify by the server type, never guess a task's format from its title. */
fun SchoolAssignment.group(): AssignmentGroup? = when (type.uppercase(java.util.Locale.ROOT)) {
    "WRITING" -> AssignmentGroup.Writing
    "QUIZ" -> AssignmentGroup.Quiz
    "SENTENCE", "SENTENCE_BUILDING" -> AssignmentGroup.Sentence
    else -> null
}

fun List<SchoolAssignment>.pendingCount(classId: Long? = null, group: AssignmentGroup? = null): Int =
    count { !it.submitted && (classId == null || it.classId == classId) && (group == null || it.group() == group) }
