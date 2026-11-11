package com.hocchu.mobile.data

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hocchu.mobile.auth.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

data class StudyContent(val lessons: List<LetterLesson>,val practices: List<List<WordPractice>>) {
    companion object {fun bundled()=StudyContent(AlphabetContent.lessons,AlphabetContent.lessons.indices.map(LetterCurriculum::words))}
}
val LocalStudyContent=staticCompositionLocalOf {StudyContent.bundled()}
val LocalMediaServer=staticCompositionLocalOf {""}

internal fun parseStudyContent(raw: String): StudyContent {
    val array=JSONArray(raw);require(array.length()==29)
    val letters=mutableListOf<LetterLesson>();val words=mutableListOf<List<WordPractice>>()
    for(i in 0 until 29) {
        val o=array.getJSONObject(i);require(o.getString("letter")==AlphabetContent.lessons[i].letter)
        fun text(key: String)=if(o.isNull(key)) "" else o.optString(key)
        letters+=LetterLesson(o.getString("letter"),o.getString("sound"),o.getString("word"),o.getString("picture"),
            text("imageUrl"),text("audioUrl"),text("exampleAudioUrl"),o.optBoolean("hidden"))
        require(letters.last().sound.isNotBlank()&&letters.last().word.isNotBlank())
        val practice=o.getJSONArray("practices");require(practice.length()==3)
        words+=(0 until 3).map {n->val p=practice.getJSONObject(n);val sentence=p.getJSONArray("sentence")
            require(sentence.length() in 2..12)
            WordPractice(p.getString("text"),p.getString("picture"),(0 until sentence.length()).map(sentence::getString))
                .also {require(it.sentence.all(String::isNotBlank));it.withBlank(letters.last().letter)}
        }
    }
    return StudyContent(letters,words)
}

class StudyContentModel(app: Application,private val transport: AuthTransport):AndroidViewModel(app) {
    constructor(app:Application):this(app,UrlAuthTransport(8*1024*1024))
    var content by mutableStateOf(StudyContent.bundled());private set
    var loading by mutableStateOf(false);private set
    var notice by mutableStateOf<String?>(null);private set
    private var session:AuthSession?=null
    private var request:Job?=null
    private var expired:()->Unit={}
    fun connect(value:AuthSession?,onExpired:()->Unit) {
        expired=onExpired;if(value==session)return
        request?.cancel();session=value;content=StudyContent.bundled();notice=null;loading=false
        if(value!=null) refresh()
    }
    fun refresh() {
        val user=session?:return;request?.cancel()
        request=viewModelScope.launch {loading=true;notice=null
            val prefs=getApplication<Application>().getSharedPreferences(accountPreferences(user.server,user.account.id)+".content",0)
            try {
                val cached=withContext(Dispatchers.IO) {prefs.getString("content",null)?.let {runCatching {parseStudyContent(it)}.getOrNull()}}
                if(cached!=null) content=cached
                val fresh=withContext(Dispatchers.IO) {
                    val response=transport.request(user.server+"api/student/content","GET",user.token,null)
                    if(response.status==401)throw AuthFailure.Expired()
                    require(response.status in 200..299)
                    val parsed=parseStudyContent(response.body)
                    check(prefs.edit().putString("content",response.body).commit());parsed
                }
                content=fresh
            } catch(e:CancellationException){throw e}
            catch(e:AuthFailure.Expired){expired()}
            catch(_:Exception){notice="Chưa cập nhật được học liệu. Bé đang dùng nội dung đã lưu trên máy."}
            finally{if(session==user)loading=false}
        }
    }
}
