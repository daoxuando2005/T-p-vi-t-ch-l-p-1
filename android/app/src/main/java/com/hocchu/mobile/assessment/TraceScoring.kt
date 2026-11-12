package com.hocchu.mobile.assessment

import kotlin.math.roundToInt

data class TraceRating(val score:Int,val coverage:Int,val precision:Int,val missingParts:Int) {
    val feedback:String get()=when {
        missingParts>0 -> "Bé bổ sung dấu hoặc phần nét còn thiếu nhé."
        score>=80 -> "Nét chữ bám mẫu tốt. Bé tiếp tục phát huy nhé!"
        precision<65 -> "Bé đưa nét viết sát mẫu và giữ đúng vị trí trên ô li nhé."
        else -> "Bé viết đủ thân chữ, nét nối và dấu còn thiếu nhé."
    }
    fun encode()="$score;$coverage;$precision;$missingParts"
    companion object {
        fun decode(value:String):TraceRating?=runCatching {val p=value.split(';').map(String::toInt)
            require(p.size==4&&p.take(3).all {it in 0..100}&&p[3] in 0..100);TraceRating(p[0],p[1],p[2],p[3])}.getOrNull()
    }
}

/** Symmetric coverage/precision with a five-pixel tolerance on the 256px copybook. */
object TraceScoring {
    const val SIDE=256
    private fun distance(mask:BooleanArray):IntArray {
        val d=IntArray(mask.size){if(mask[it])0 else 10000}
        for(y in 0 until SIDE)for(x in 0 until SIDE) {val i=y*SIDE+x
            if(x>0)d[i]=minOf(d[i],d[i-1]+1);if(y>0)d[i]=minOf(d[i],d[i-SIDE]+1)
            if(x>0&&y>0)d[i]=minOf(d[i],d[i-SIDE-1]+1);if(x+1<SIDE&&y>0)d[i]=minOf(d[i],d[i-SIDE+1]+1)
        }
        for(y in SIDE-1 downTo 0)for(x in SIDE-1 downTo 0){val i=y*SIDE+x
            if(x+1<SIDE)d[i]=minOf(d[i],d[i+1]+1);if(y+1<SIDE)d[i]=minOf(d[i],d[i+SIDE]+1)
            if(x+1<SIDE&&y+1<SIDE)d[i]=minOf(d[i],d[i+SIDE+1]+1);if(x>0&&y+1<SIDE)d[i]=minOf(d[i],d[i+SIDE-1]+1)
        };return d
    }
    fun assess(target:BooleanArray,ink:BooleanArray):TraceRating {
        require(target.size==SIDE*SIDE&&ink.size==target.size)
        val toTarget=distance(target);val toInk=distance(ink)
        var size=0;var written=0;var covered=0;var inside=0
        for(i in target.indices){if(target[i]){size++;if(toInk[i]<=5)covered++};if(ink[i]){written++;if(toTarget[i]<=5)inside++}}
        val coverage=if(size==0)0.0 else covered.toDouble()/size;val precision=if(written<4)0.0 else inside.toDouble()/written
        var score=if(coverage+precision==0.0)0 else (200*coverage*precision/(coverage+precision)).roundToInt()
        val seen=BooleanArray(target.size);var missing=0
        for(start in target.indices)if(target[start]&&!seen[start]) {
            val queue=ArrayDeque<Int>();queue.add(start);seen[start]=true;var count=0;var hit=0
            while(queue.isNotEmpty()) {val p=queue.removeFirst();count++;if(toInk[p]<=5)hit++
                for(dy in -1..1)for(dx in -1..1){val x=p%SIDE+dx;val y=p/SIDE+dy
                    if(x in 0 until SIDE&&y in 0 until SIDE){val next=y*SIDE+x;if(target[next]&&!seen[next]){seen[next]=true;queue.add(next)}}}
            }
            if(count>=5&&hit.toDouble()/count<.45)missing++
        }
        if(missing>0)score=(score*.5).roundToInt()
        return TraceRating(score,(coverage*100).roundToInt(),(precision*100).roundToInt(),missing)
    }
}
