package com.hocchu.mobile

import com.hocchu.mobile.data.*
import org.junit.Assert.*
import org.junit.Test

class ProgressRestoreTest {
    private val done=LessonProgress(true,2,3,3)
    private fun empty()=List(29){LessonProgress()}
    @Test fun anotherDeviceRestoresUnlockedLettersWithoutLosingLocalSteps() {
        val local=empty().toMutableList().apply {this[0]=done;this[1]=LessonProgress(true,2,2)}
        val remote=empty().toMutableList().apply {this[0]=done;this[1]=done;this[2]=LessonProgress(true,1)}
        val merged=LessonProgress.merge(local,remote)
        assertEquals(done,merged[1]);assertEquals(LessonProgress(true,1),merged[2])
        assertTrue(LessonProgress.unlocked(2,merged));assertFalse(LessonProgress.unlocked(3,merged))
    }
    @Test fun staleCloudDataNeverRegressesOfflineLearningAndMergeIsIdempotent() {
        val local=empty().toMutableList().apply {this[0]=done;this[1]=LessonProgress(true,2,3,1)}
        val merged=LessonProgress.merge(local,empty())
        assertEquals(local,merged);assertEquals(merged,LessonProgress.merge(merged,empty()))
    }
    @Test fun corruptOrSkippedRemoteStepsAreRejected() {
        val skipped=empty().toMutableList().apply {this[2]=done}
        try{LessonProgress.merge(empty(),skipped);fail()}catch(_:IllegalArgumentException){}
        val invalid=empty().toMutableList().apply {this[0]=LessonProgress(true,9)}
        try{LessonProgress.merge(empty(),invalid);fail()}catch(_:IllegalArgumentException){}
    }
}
