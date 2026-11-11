package com.hocchu.mobile

import com.hocchu.mobile.auth.*
import com.hocchu.mobile.family.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class FamilyProtocolTest {
    private val user=AuthSession("https://school.example/","not-a-real-token",UserAccount(71,"parent-test","Phụ huynh",setOf("ROLE_PARENT")),Long.MAX_VALUE)
    @Test fun childListOnlyIncludesApprovedChildrenAndSendsBearer() {
        val api=FamilyApi(AuthTransport {url,method,token,body->
            assertEquals(user.server+"api/parent/students",url);assertEquals("GET",method);assertEquals(user.token,token);assertNull(body)
            HttpReply(200,"""[{"studentId":1,"studentName":"An","studentUsername":"an","status":"APPROVED"},{"studentId":2,"status":"PENDING"}]""")
        })
        assertEquals(listOf(LinkedChild(1,"An","an")),api.children(user))
    }
    @Test fun anotherChildOrDateInResponseCannotAppearInSelectedDashboard() {
        val day=LocalDate.now(SCHOOL_ZONE)
        val api=FamilyApi(AuthTransport {_,_,_,_->HttpReply(200,JSONObject().put("studentId",2).put("date",day).toString())})
        try {api.dashboard(user,1,day);fail()} catch(_: FamilyFailure) {}
    }
    @Test fun authorizationFailuresAreNotShownAsEmptySuccess() {
        try {FamilyApi(AuthTransport {_,_,_,_->HttpReply(401,"")}).children(user);fail()} catch(_: AuthFailure.Expired) {}
        try {FamilyApi(AuthTransport {_,_,_,_->HttpReply(403,"private server details")}).children(user);fail()}
        catch(e: FamilyFailure) {assertFalse(e.publicMessage.contains("private server"))}
    }
    @Test fun invitationIsCaseSensitiveAndSyncUsesAcknowledgedEventIds() {
        val code="AbCdEfGhIjKlMnOpQr-_12"
        val api=FamilyApi(AuthTransport {url,method,token,body->
            assertEquals("POST",method);assertEquals(user.token,token)
            if(url.contains("parent/link")) {
                assertTrue(url.endsWith("linkCode=$code"));HttpReply(200,"""{"success":true}""")
            } else {
                assertEquals("id-1",JSONObject(body!!).getJSONArray("events").getJSONObject(0).getString("id"))
                HttpReply(200,"""{"success":true,"acceptedIds":["id-1"]}""")
            }
        })
        api.link(user," $code ")
        assertEquals(setOf("id-1"),api.sync(user,JSONObject().put("events",JSONArray().put(JSONObject().put("id","id-1")))))
    }
}
