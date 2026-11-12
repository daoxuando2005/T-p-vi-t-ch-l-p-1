package com.hocchu.mobile
import com.hocchu.mobile.assessment.*
import org.junit.Assert.*
import org.junit.Test

class TraceScoringTest {
    private fun target()=BooleanArray(256*256).apply {for(y in 90..160)for(x in 100..104)this[y*256+x]=true}
    @Test fun matchingInkScoresWellButBlankAndScribbledBoardsDoNot() {
        val expected=target();assertEquals(100,TraceScoring.assess(expected,expected.copyOf()).score)
        assertEquals(0,TraceScoring.assess(expected,BooleanArray(expected.size)).score)
        assertTrue(TraceScoring.assess(expected,BooleanArray(expected.size){true}).score<35)
    }
    @Test fun missingSeparateMarkCannotBeHiddenByGoodBodyCoverage() {
        val body=target();val expected=body.copyOf();for(y in 55..60)for(x in 100..104)expected[y*256+x]=true
        val score=TraceScoring.assess(expected,body);assertTrue(score.missingParts>0);assertTrue(score.score<80)
    }
}
