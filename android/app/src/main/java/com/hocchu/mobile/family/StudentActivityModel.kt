package com.hocchu.mobile.family

import android.app.Application
import android.content.SharedPreferences
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hocchu.mobile.auth.*
import com.hocchu.mobile.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

/** Persistent per-account outbox. A snapshot never fabricates historical activity timestamps. */
class StudentActivityModel(app: Application, private val api: FamilyApi) : AndroidViewModel(app) {
    constructor(app: Application): this(app,FamilyApi())
    private var session: AuthSession? = null
    private var work = CoroutineScope(viewModelScope.coroutineContext+SupervisorJob(viewModelScope.coroutineContext[Job]))
    private val disk = Mutex()
    private val network = Mutex()
    private var syncJob: Job? = null
    private var lastWrite: Job? = null
    private var expired: () -> Unit = {}
    private var progressVersion=0L
    var pending by mutableIntStateOf(0); private set
    var error by mutableStateOf<String?>(null); private set
    var finishing by mutableStateOf(false); private set
    var code by mutableStateOf<LinkCode?>(null); private set
    var codeLoading by mutableStateOf(false); private set
    fun connect(value: AuthSession?, onExpired: () -> Unit = {}) {
        expired=onExpired
        if(session==value) return
        work.cancel();work=CoroutineScope(viewModelScope.coroutineContext+SupervisorJob(viewModelScope.coroutineContext[Job]))
        syncJob=null;session=value;pending=0;error=null;finishing=false;code=null;codeLoading=false
        if(value!=null) retry()
    }
    private fun prefs(user: AuthSession)=getApplication<Application>().getSharedPreferences(accountPreferences(user.server,user.account.id)+".activity",0)
    private fun queue(prefs: SharedPreferences): JSONArray = JSONArray(prefs.getString("outbox","[]"))
    private fun store(prefs: SharedPreferences,array: JSONArray) {
        if(!prefs.edit().putString("outbox",array.toString()).commit()) throw FamilyFailure("Chưa lưu được hoạt động trên máy.")
    }
    fun record(kind: String,index: Int,source: String="FREE",review: Boolean=false) {
        val user=session ?: return
        if(index !in DemoContent.lessons.indices) return
        val event=JSONObject().put("id",UUID.randomUUID().toString()).put("kind",kind).put("letter",DemoContent.lessons[index].letter)
            .put("source",source).put("review",review).put("occurredAt",Instant.now().toString())
        val local=prefs(user)
        // Persist even if logout begins while the disk write is waiting; never reuse another account's preferences.
        lastWrite=viewModelScope.launch(start=CoroutineStart.UNDISPATCHED) {
            try {
                val size=withContext(NonCancellable+Dispatchers.IO) { disk.withLock {val q=queue(local);q.put(event);store(local,q);q.length()} }
                if(session==user) {pending=size;retry()}
            } catch(e: CancellationException) {throw e}
            catch (_: Exception) {if(session==user) error="Chưa lưu được hoạt động. Hãy kiểm tra bộ nhớ thiết bị."}
        }
    }
    fun lesson(index: Int,old: LessonProgress,next: LessonProgress,review: Boolean) {
        progressVersion++
        if(!old.read&&next.read) record("READING",index,"JOURNEY",review)
        if(next.words>old.words) record("WORDS",index,"JOURNEY",review)
        if(next.sentences>old.sentences) record("SENTENCES",index,"JOURNEY",review)
        if(!old.complete&&next.complete) record("LESSON_COMPLETE",index,"JOURNEY",review)
        retry()
    }
    fun retry() {
        val user=session ?: return
        if(syncJob?.isActive==true) return
        var succeeded=false
        syncJob=work.launch {
            var delayMs=1500L
            do {
                try { flush(user);error=null;succeeded=true;return@launch }
                catch(e: CancellationException) {throw e}
                catch(e: AuthFailure.Expired) {if(session==user) expired();return@launch}
                catch (e: Exception) {error=(e as? FamilyFailure)?.publicMessage ?: "Có hoạt động chưa gửi được. App sẽ thử lại khi có kết nối."}
                delay(delayMs);delayMs=(delayMs*2).coerceAtMost(60000)
            } while(session==user)
        }
        syncJob?.invokeOnCompletion {
            if(succeeded) viewModelScope.launch {if(session==user&&pending>0) retry()}
        }
    }
    private suspend fun flush(user: AuthSession) = network.withLock {
        do {
            val local=prefs(user)
            val batch=withContext(Dispatchers.IO) {disk.withLock {val q=queue(local);JSONArray().apply {for(i in 0 until minOf(q.length(),100)) put(q.getJSONObject(i))}}}
            val progress=getApplication<Application>().getSharedPreferences(accountPreferences(user.server,user.account.id),0)
            val sentVersion=progressVersion
            val snapshot=LessonProgressStore(progress).readAll()
            val body=JSONObject().put("events",batch).put("progress",JSONArray().apply { snapshot.forEachIndexed { i,p ->
                put(JSONObject().put("letter",DemoContent.lessons[i].letter).put("read",p.read).put("writes",p.writes).put("words",p.words).put("sentences",p.sentences)) } })
            val accepted=withContext(Dispatchers.IO) {api.sync(user,body)}
            require(accepted==(0 until batch.length()).map {batch.getJSONObject(it).getString("id")}.toSet())
            val left=withContext(Dispatchers.IO) {disk.withLock {val current=queue(local);val remaining=JSONArray()
                for(i in 0 until current.length()) if(current.getJSONObject(i).getString("id") !in accepted) remaining.put(current.getJSONObject(i))
                store(local,remaining);remaining.length()}}
            if(session==user) pending=left
        } while(left>0 || sentVersion!=progressVersion)
    }
    fun finishSession(done: () -> Unit) {
        if(finishing) return
        val user=session
        if(user==null) {done();return}
        finishing=true
        viewModelScope.launch {
            try {withTimeout(9000) {lastWrite?.join();flush(user)}} catch (_: Exception) { /* Outbox remains for the next sign-in. */ }
            finally {finishing=false;done()}
        }
    }
    fun showLinkCode() {
        val user=session ?: return
        if(codeLoading) return
        work.launch {codeLoading=true;error=null
            try {code=withContext(Dispatchers.IO) {api.linkCode(user)}}
            catch(e: CancellationException) {throw e}
            catch(e: AuthFailure.Expired) {expired()}
            catch(e: Exception) {error=(e as? FamilyFailure)?.publicMessage ?: "Chưa lấy được mã liên kết. Hãy thử lại."}
            finally {codeLoading=false}
        }
    }
    fun hideCode() {code=null}
}
