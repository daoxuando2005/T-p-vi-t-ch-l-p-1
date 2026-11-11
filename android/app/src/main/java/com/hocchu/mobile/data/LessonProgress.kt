package com.hocchu.mobile.data

enum class LessonStage(val title: String) { Read("Đọc"), Write("Viết"), Words("Điền từ"), Sentences("Ghép câu"), Done("Hoàn thành") }

/** Progress represents completed activities, not assessed reading/handwriting accuracy. */
data class LessonProgress(val read: Boolean = false, val writes: Int = 0, val words: Int = 0, val sentences: Int = 0) {
    val stage: LessonStage get() = when {
        !read -> LessonStage.Read
        writes < 2 -> LessonStage.Write
        words < 3 -> LessonStage.Words
        sentences < 3 -> LessonStage.Sentences
        else -> LessonStage.Done
    }
    val complete: Boolean get() = stage == LessonStage.Done
    fun finishReading() = if (stage == LessonStage.Read) copy(read = true) else this
    fun finishWriting() = if (stage == LessonStage.Write) copy(writes = writes + 1) else this
    fun answerWord(question: Int, correct: Boolean) =
        if (stage == LessonStage.Words && question == words && correct) copy(words = words + 1) else this
    fun answerSentence(question: Int, correct: Boolean) =
        if (stage == LessonStage.Sentences && question == sentences && correct) copy(sentences = sentences + 1) else this
    fun encode() = "1;${if (read) 1 else 0};$writes;$words;$sentences"
    companion object {
        fun decode(value: String?): LessonProgress {
            val values = value?.split(';')?.map { it.toIntOrNull() } ?: return LessonProgress()
            if (values.size != 5 || values[0] != 1 || values.any { it == null }) return LessonProgress()
            val read = values[1] == 1
            val writes = if (read) values[2]!!.coerceIn(0, 2) else 0
            val words = if (writes == 2) values[3]!!.coerceIn(0, 3) else 0
            val sentences = if (words == 3) values[4]!!.coerceIn(0, 3) else 0
            return LessonProgress(read, writes, words, sentences)
        }
        fun current(all: List<LessonProgress>) = all.indexOfFirst { !it.complete }.let { if (it < 0) all.lastIndex else it }
        fun unlocked(index: Int, all: List<LessonProgress>) = index in all.indices && all.take(index).all { it.complete }
        fun merge(local:List<LessonProgress>,remote:List<LessonProgress>):List<LessonProgress> {
            require(local.size==AlphabetContent.lessons.size&&remote.size==local.size)
            require(remote.indices.all {remote[it]==decode(remote[it].encode())&&(unlocked(it,remote)||remote[it]==LessonProgress())})
            return local.zip(remote).map {(a,b)->decode(LessonProgress(a.read||b.read,maxOf(a.writes,b.writes),
                maxOf(a.words,b.words),maxOf(a.sentences,b.sentences)).encode())}
        }
    }
}
