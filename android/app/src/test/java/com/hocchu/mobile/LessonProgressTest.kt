package com.hocchu.mobile

import com.hocchu.mobile.data.*
import org.junit.Assert.*
import org.junit.Test

class LessonProgressTest {
    @Test fun cannotSkipStagesOrCountStaleAnswersTwice() {
        var p = LessonProgress()
        assertEquals(p, p.finishWriting())
        assertEquals(p, p.answerWord(0, true))
        p = p.finishReading().finishWriting()
        assertEquals(LessonStage.Write, p.stage)
        assertEquals(p, p.answerWord(0, true))
        p = p.finishWriting()
        assertEquals(LessonStage.Words, p.stage)
        assertEquals(p, p.answerWord(0, false))
        assertEquals(p, p.answerWord(2, true))
        p = p.answerWord(0, true)
        assertEquals(1, p.words)
        assertEquals(p, p.answerWord(0, true))
        assertEquals(p, p.answerSentence(0, true))
        assertEquals(p, p.finishWriting())
    }

    @Test fun allRequiredActivitiesUnlockNextLetterAndLastLetterIsWellDefined() {
        val all = MutableList(29) { LessonProgress() }
        for (index in all.indices) {
            assertEquals(index, LessonProgress.current(all))
            assertTrue(LessonProgress.unlocked(index, all))
            if(index < 28) assertFalse(LessonProgress.unlocked(index + 1, all))
            var p = all[index].finishReading().finishWriting().finishWriting()
            for (i in 0..2) p = p.answerWord(i, true)
            for (i in 0..1) p = p.answerSentence(i, true)
            assertFalse(p.complete)
            if(index < 28) assertFalse(LessonProgress.unlocked(index + 1, all))
            p = p.answerSentence(2, true)
            assertTrue(p.complete)
            all[index] = p
        }
        assertEquals(28, LessonProgress.current(all))
        assertFalse(LessonProgress.unlocked(29, all))
    }

    @Test fun progressRoundTripsAtEveryCheckpoint() {
        val states = listOf(LessonProgress(), LessonProgress(true, 1), LessonProgress(true, 2),
            LessonProgress(true, 2, 2), LessonProgress(true, 2, 3, 2), LessonProgress(true, 2, 3, 3))
        states.forEach { assertEquals(it, LessonProgress.decode(it.encode())) }
        assertEquals(LessonProgress(), LessonProgress.decode(null))
        assertEquals(LessonProgress(), LessonProgress.decode("bad data"))
        assertEquals(LessonProgress(), LessonProgress.decode("99;1;2;3;3"))
        assertEquals(LessonProgress(), LessonProgress.decode("1;0;99;99;99"))
        assertEquals(LessonProgress(true, 1), LessonProgress.decode("1;1;1;99;99"))
    }

    @Test fun curriculumHasThreeValidWordsAndReusableSentencesForEveryLetter() {
        AlphabetContent.lessons.indices.forEach { index ->
            val letter = AlphabetContent.lessons[index].letter.lowercase()
            val rows = LetterCurriculum.words(index)
            assertEquals("three different words for $letter", 3, rows.map { it.text }.toSet().size)
            rows.forEachIndexed { question, row ->
                assertTrue("${row.text} contains $letter", row.text.contains(letter))
                assertEquals(1, row.withBlank(letter).count { it == '□' })
                assertEquals(row.text, row.withBlank(letter).replace("□", letter))
                val options = LetterCurriculum.options(index, question)
                assertEquals(4, options.toSet().size)
                assertEquals(1, options.count { it == letter })
                assertTrue(row.sentence.size in 2..4)
                assertTrue("reuse ${row.text}", row.sentence.joinToString(" ").lowercase().contains(row.text))
                val order = LetterCurriculum.tileOrder(index, question)
                assertEquals(row.sentence.indices.toSet(), order.toSet())
                assertFalse(LetterCurriculum.sentenceCorrect(index, question, order))
                assertTrue(LetterCurriculum.sentenceCorrect(index, question, row.sentence.indices.toList()))
                assertFalse(LetterCurriculum.sentenceCorrect(index, question, listOf(0, 0, 0)))
            }
        }
    }
}
