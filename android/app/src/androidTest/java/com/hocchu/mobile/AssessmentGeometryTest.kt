package com.hocchu.mobile

import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import com.hocchu.mobile.assessment.TraceScoring
import com.hocchu.mobile.data.AlphabetContent
import com.hocchu.mobile.ui.copybookMask
import org.junit.Assert.*
import org.junit.Test

class AssessmentGeometryTest {
    @Test fun all29AndroidModelsAgreeWithServerFontGeometry() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        for(lesson in AlphabetContent.lessons) {
            val image=requireNotNull(instrumentation.context.assets.open("copybook/${lesson.letter}.png").use(BitmapFactory::decodeStream))
            try {
                val mask=BooleanArray(256*256){i->Color.red(image.getPixel(i%256,i/256))<170}
                val android=copybookMask(instrumentation.targetContext.resources,lesson.letter.lowercase())
                val rating=TraceScoring.assess(android,mask)
                assertTrue("${lesson.letter}: score=${rating.score}",rating.score>=90)
            }finally{image.recycle()}
        }
    }
}
