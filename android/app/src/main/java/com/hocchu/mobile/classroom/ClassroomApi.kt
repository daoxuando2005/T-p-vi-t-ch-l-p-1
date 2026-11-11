package com.hocchu.mobile.classroom

import com.hocchu.mobile.auth.*
import org.json.JSONObject
import java.net.URLEncoder

class ClassroomApi(private val transport: AuthTransport = UrlAuthTransport(8 * 1024 * 1024)) {
    private fun request(session: AuthSession, path: String, method: String = "GET", body: String? = null): String {
        if (session.expiresAt <= System.currentTimeMillis()) throw AuthFailure.Expired()
        val reply = transport.request(session.server + "api/student/" + path, method, session.token, body)
        when(reply.status) {
            in 200..299 -> return reply.body
            401 -> throw AuthFailure.Expired()
            403 -> throw ClassroomFailure("Bé chưa có quyền xem hoặc nộp bài của lớp này. Hãy tải lại danh sách.")
            400 -> throw ClassroomFailure(message(reply.body) ?: "Thông tin chưa hợp lệ. Bé kiểm tra lại nhé.")
            else -> throw ClassroomFailure("Chưa kết nối được lớp học. Hãy thử lại sau.")
        }
    }
    fun classes(session: AuthSession): List<SchoolClass> = parse { ClassroomJson.classes(request(session, "class/joined")) }
    fun review(session:AuthSession,assignmentId:Long):SubmissionReview=parse {
        parseSubmissionReview(request(session,"assignments/$assignmentId/submission")).also {require(it.studentId==session.account.id)}
    }
    fun leave(session:AuthSession,classId:Long)=parse {
        val o=JSONObject(request(session,"class/leave/$classId","DELETE"));require(o.getBoolean("success"))
    }
    fun assignments(session: AuthSession): List<SchoolAssignment> = parse { ClassroomJson.assignments(request(session, "assignments")) }
    fun join(session: AuthSession, code: String) {
        val clean = code.trim()
        if(clean.isBlank() || clean.length > 20) throw ClassroomFailure("Bé nhập mã lớp từ cô giáo nhé.")
        accepted(request(session, "class/join?inviteCode=" + URLEncoder.encode(clean, "UTF-8"), "POST"))
    }
    fun submit(session: AuthSession, id: Long, data: String) {
        require(id > 0)
        accepted(request(session, "assignments/$id/submit", "POST",
            JSONObject().put("submissionData", data).toString()))
    }
    private fun accepted(json: String) = parse {
        val value = JSONObject(json)
        if (!value.getBoolean("success")) throw ClassroomFailure(message(json) ?: "Chưa thực hiện được. Hãy thử lại.")
    }
    private fun message(json: String): String? = try {
        JSONObject(json).optString("message").takeIf { it.isNotBlank() && it.length <= 240 && it.none(Char::isISOControl) }
    } catch (_: Exception) { null }
    private inline fun <T> parse(action: () -> T): T = try { action() }
        catch(e: AuthFailure) { throw e }
        catch(e: ClassroomFailure) { throw e }
        catch (_: Exception) { throw ClassroomFailure("Dữ liệu lớp học chưa hợp lệ. Hãy tải lại hoặc báo cô giáo.") }
}
