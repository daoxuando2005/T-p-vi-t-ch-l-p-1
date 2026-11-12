package com.hocchu.mobile.classroom

import org.json.JSONArray
import org.json.JSONObject

data class ReviewedAnswer(val question:String,val selected:String,val expected:String)
data class ReviewedImage(val letter:String,val data:String)
data class WritingRating(val letter:String,val score:Int,val coverage:Int,val precision:Int,val feedback:String)
data class SubmissionReview(val id:Long,val studentId:Long,val title:String,val type:String,val instructions:String,
    val score:Double?,val feedback:String,val submittedAt:String,val answers:List<ReviewedAnswer>,val images:List<ReviewedImage>,
    val autoScore:Double?=null,val gradeSource:String="",val writingRatings:List<WritingRating> = emptyList())
data class ReviewState(val visible:Boolean=false,val loading:Boolean=false,val review:SubmissionReview?=null,val error:String?=null)
internal fun parseSubmissionReview(raw:String):SubmissionReview {
    val o=JSONObject(raw)
    fun text(key:String)=if(o.isNull(key)) "" else o.optString(key)
    require(o.getLong("id")>0&&o.getLong("studentId")>0)
    val type=o.getString("type");val data=text("submissionData")
    val answers=mutableListOf<ReviewedAnswer>();val images=mutableListOf<ReviewedImage>()
    if(type=="QUIZ") {
        val questions=runCatching {JSONArray(text("assignmentContent"))}.getOrDefault(JSONArray())
        val chosen=runCatching {if(data.trim().startsWith("["))JSONArray(data) else JSONObject(data).getJSONArray("answers")}.getOrDefault(JSONArray())
        for(i in 0 until questions.length()) {val q=questions.getJSONObject(i)
            answers+=ReviewedAnswer(q.optString("question"),chosen.optString(i,"Chưa ghi nhận"),q.optString("answer"))}
    } else if(type=="SENTENCE") {
        val questions=JSONObject(text("assignmentContent")).getJSONArray("sentences")
        val chosen=JSONObject(data).getJSONArray("sentences")
        for(i in 0 until questions.length()) {
            val q=questions.getJSONObject(i);val words=q.getJSONArray("words");val tiles=chosen.getJSONArray(i)
            val expected=(0 until words.length()).joinToString(" ") {words.getString(it)}
            val actual=(0 until tiles.length()).joinToString(" ") {words.getString(tiles.getInt(it))}
            answers+=ReviewedAnswer(q.optString("prompt","Ghép câu ${i+1}"),actual,expected)
        }
    } else if(type=="WRITING") {
        val array=runCatching {if(data.trim().startsWith("["))JSONArray(data) else JSONArray().put(JSONObject().put("image",data))}.getOrDefault(JSONArray())
        for(i in 0 until array.length()) {val image=array.getJSONObject(i);val source=image.optString("image")
            if(source.startsWith("data:image/")&&source.length<=4_000_000) images+=ReviewedImage(image.optString("letter"),source)}
    }
    val ratings=runCatching {val a=JSONArray(text("assessmentData"));(0 until a.length()).map {val r=a.getJSONObject(it)
        WritingRating(r.getString("letter"),r.getInt("score"),r.getInt("coverage"),r.getInt("precision"),r.getString("feedback"))
            .also {require(it.score in 0..100&&it.coverage in 0..100&&it.precision in 0..100)}
    }}.getOrDefault(emptyList())
    return SubmissionReview(o.getLong("id"),o.getLong("studentId"),o.getString("title"),type,text("instructions"),
        if(o.isNull("score"))null else o.getDouble("score"),text("feedback"),text("submittedAt"),answers,images,
        if(o.isNull("autoScore"))null else o.getDouble("autoScore"),text("gradeSource"),ratings)
}
