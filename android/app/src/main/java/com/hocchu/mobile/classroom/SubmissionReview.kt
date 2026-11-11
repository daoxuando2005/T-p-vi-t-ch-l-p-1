package com.hocchu.mobile.classroom

import org.json.JSONArray
import org.json.JSONObject

data class ReviewedAnswer(val question:String,val selected:String,val expected:String)
data class ReviewedImage(val letter:String,val data:String)
data class SubmissionReview(val id:Long,val studentId:Long,val title:String,val type:String,val instructions:String,
    val score:Double?,val feedback:String,val submittedAt:String,val answers:List<ReviewedAnswer>,val images:List<ReviewedImage>)
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
    } else if(type=="WRITING") {
        val array=runCatching {if(data.trim().startsWith("["))JSONArray(data) else JSONArray().put(JSONObject().put("image",data))}.getOrDefault(JSONArray())
        for(i in 0 until array.length()) {val image=array.getJSONObject(i);val source=image.optString("image")
            if(source.startsWith("data:image/")&&source.length<=4_000_000) images+=ReviewedImage(image.optString("letter"),source)}
    }
    return SubmissionReview(o.getLong("id"),o.getLong("studentId"),o.getString("title"),type,text("instructions"),
        if(o.isNull("score"))null else o.getDouble("score"),text("feedback"),text("submittedAt"),answers,images)
}
