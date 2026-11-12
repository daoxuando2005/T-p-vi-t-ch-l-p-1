package com.hocchu.mobile.ui

import android.graphics.*
import android.content.res.Resources
import com.hocchu.mobile.R
import com.hocchu.mobile.assessment.*

/** The same font, baseline and normalized ink coordinates as WritingBoard. */
internal fun assessInk(resources:Resources,letter:String,strokes:List<InkStroke>):TraceRating {
    val side=TraceScoring.SIDE;val cell=side/10f
    val target=copybookMask(resources,letter)
    val actual=inkMask {canvas->val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.BLACK;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND}
        strokes.forEach {stroke->val points=stroke.points;paint.strokeWidth=(.08f+stroke.pen*.04f)*cell
            if(points.size==1){paint.style=Paint.Style.FILL;canvas.drawCircle(points[0].x*side,points[0].y*side,paint.strokeWidth/2,paint)}
            else if(points.size>1){paint.style=Paint.Style.STROKE;val path=Path().apply{moveTo(points[0].x*side,points[0].y*side);points.drop(1).forEach{lineTo(it.x*side,it.y*side)}};canvas.drawPath(path,paint)}
        }}
    return TraceScoring.assess(target,actual)
}

internal fun copybookMask(resources:Resources,letter:String):BooleanArray {
    val cell=TraceScoring.SIDE/10f;val glyph=copybookGlyph(resources.getFont(R.font.handwriting),CopybookForms.forLetter(letter))
    return inkMask {canvas->canvas.translate(CopybookPage.originX(glyph)*cell,CopybookPage.baseline*cell);canvas.scale(cell,cell)
        canvas.drawPath(glyph.path,Paint().apply{color=Color.BLACK;style=Paint.Style.FILL})}
}
private fun inkMask(draw:(Canvas)->Unit):BooleanArray {
    val side=TraceScoring.SIDE;val image=Bitmap.createBitmap(side,side,Bitmap.Config.ARGB_8888)
    try {val canvas=Canvas(image);canvas.drawColor(Color.WHITE);draw(canvas)
        val pixels=IntArray(side*side);image.getPixels(pixels,0,side,0,0,side,side)
        return BooleanArray(pixels.size){(Color.red(pixels[it])*299+Color.green(pixels[it])*587+Color.blue(pixels[it])*114)/1000<170}
    }finally{image.recycle()}
}
