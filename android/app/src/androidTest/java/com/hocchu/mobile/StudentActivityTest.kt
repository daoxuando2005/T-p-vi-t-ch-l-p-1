package com.hocchu.mobile

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.hocchu.mobile.auth.*
import com.hocchu.mobile.family.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class StudentActivityTest {
    @Test fun offlineOutboxSurvivesModelRestartAndDoesNotMoveToAnotherAccount() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val app=instrumentation.targetContext.applicationContext as Application
        val origin="https://activity-${UUID.randomUUID()}.invalid/"
        fun user(id: Long)=AuthSession(origin,"test-token-$id",UserAccount(id,"test-$id","Test",setOf("ROLE_STUDENT")),Long.MAX_VALUE)
        val a=user(1);val b=user(2)
        val names=listOf(a,b).flatMap { val n=accountPreferences(it.server,it.account.id);listOf(n,n+".activity") }
        val aPrefs=app.getSharedPreferences(names[1],0)
        val owners=mutableListOf<ViewModelStore>()
        val received=CopyOnWriteArrayList<Pair<String?,String>>()
        fun model(api: FamilyApi): StudentActivityModel {
            lateinit var value: StudentActivityModel
            instrumentation.runOnMainSync {value=StudentActivityModel(app,api);owners.add(ViewModelStore().also {it.put("activity",value)})}
            return value
        }
        fun waitUntil(check: ()->Boolean) {val end=System.currentTimeMillis()+5000;while(!check()&&System.currentTimeMillis()<end) Thread.sleep(30);assertTrue(check())}
        try {
            val offline=model(FamilyApi(AuthTransport {_,_,_,_->HttpReply(503,"")}))
            instrumentation.runOnMainSync {offline.connect(a);offline.record("WRITING",0)}
            waitUntil {JSONArray(aPrefs.getString("outbox","[]")).length()==1}
            val saved=JSONArray(aPrefs.getString("outbox","[]")).getJSONObject(0).getString("id")
            instrumentation.runOnMainSync {offline.connect(null)}
            val online=model(FamilyApi(AuthTransport {_,_,token,body->
                val payload=JSONObject(body!!);received.add(token to body)
                HttpReply(200,JSONObject().put("success",true).put("acceptedIds",JSONArray().apply {
                    val events=payload.getJSONArray("events");for(i in 0 until events.length()) put(events.getJSONObject(i).getString("id"))
                }).toString())
            }))
            instrumentation.runOnMainSync {online.connect(b)}
            waitUntil {received.any {it.first==b.token}}
            assertEquals(1,JSONArray(aPrefs.getString("outbox","[]")).length())
            assertTrue(received.filter {it.first==b.token}.all {JSONObject(it.second).getJSONArray("events").length()==0})
            instrumentation.runOnMainSync {online.connect(a)}
            waitUntil {JSONArray(aPrefs.getString("outbox","[]")).length()==0}
            assertTrue(received.filter {it.first==a.token}.any {JSONObject(it.second).getJSONArray("events").optJSONObject(0)?.optString("id")==saved})
            assertTrue(received.all {JSONObject(it.second).getJSONArray("progress").length()==29})
        } finally {
            instrumentation.runOnMainSync {owners.forEach {it.clear()}}
            names.forEach {app.deleteSharedPreferences(it)}
        }
    }
}
