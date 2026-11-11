package com.hocchu.mobile.family

import com.hocchu.mobile.auth.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.net.URLEncoder

class FamilyApi(private val transport: AuthTransport = UrlAuthTransport(2 * 1024 * 1024)) {
    private fun request(session: AuthSession, path: String, method: String = "GET", body: String? = null): String {
        if(session.expiresAt <= System.currentTimeMillis()) throw AuthFailure.Expired()
        val reply = transport.request(session.server+"api/"+path, method, session.token, body)
        when(reply.status) {
            in 200..299 -> return reply.body
            401 -> throw AuthFailure.Expired()
            403 -> throw FamilyFailure("Bạn chưa có quyền xem thông tin của học sinh này.")
            400 -> throw FamilyFailure(try { JSONObject(reply.body).text("message").takeIf { it.isNotBlank() && it.length<=240 } ?: "Thông tin chưa hợp lệ." } catch (_: Exception) { "Thông tin chưa hợp lệ." })
            else -> throw FamilyFailure("Chưa tải được dữ liệu. Hãy kiểm tra kết nối và thử lại.")
        }
    }
    fun children(session: AuthSession): List<LinkedChild> = parse {
        val array = JSONArray(request(session,"parent/students"))
        (0 until array.length()).mapNotNull { val o=array.getJSONObject(it)
            if(o.text("status")!="APPROVED") null else LinkedChild(o.getLong("studentId"),o.text("studentName"),o.text("studentUsername")) }
            .distinctBy { it.id }.also { require(it.all { c->c.id>0 }) }
    }
    fun dashboard(session: AuthSession, id: Long, date: LocalDate): ParentDashboard = parse {
        require(id>0)
        val o=JSONObject(request(session,"parent/students/$id/dashboard?date=$date"))
        require(o.getLong("studentId")==id && o.getString("date")==date.toString())
        val s=o.getJSONObject("summary")
        val timeline=o.getJSONArray("timeline").let { a->(0 until a.length()).map { val v=a.getJSONObject(it)
            ChildActivity(v.getString("id"),v.getString("kind"),v.text("letter"),v.getString("title"),Instant.parse(v.getString("occurredAt")),
                if(v.isNull("score")) null else v.getDouble("score"),v.text("feedback")) } }
        val progress=o.getJSONArray("progress").let { a->(0 until a.length()).map { val v=a.getJSONObject(it)
            LetterStatus(v.getString("letter"),v.getBoolean("read"),v.getInt("writes"),v.getInt("words"),v.getInt("sentences"),v.getBoolean("complete")) } }
        val week=o.getJSONArray("week").let { a->(0 until a.length()).map { val v=a.getJSONObject(it);DayCount(LocalDate.parse(v.getString("date")),v.getInt("activities")) } }
        ParentDashboard(id,o.getString("studentName"),date,s.getInt("readings"),s.getInt("writings"),s.getInt("exercises"),s.getInt("submissions"),timeline,
            progress,week,o.getInt("completedLetters"),o.getBoolean("progressAvailable"),o.optBoolean("timelineTruncated"))
    }
    fun link(session: AuthSession, code: String) = parse {
        val o=JSONObject(request(session,"parent/link?linkCode="+URLEncoder.encode(code.trim(),"UTF-8"),"POST"))
        if(!o.getBoolean("success")) throw FamilyFailure(o.text("message").ifBlank { "Chưa liên kết được với con." })
    }
    fun linkCode(session: AuthSession): LinkCode = parse {
        val o=JSONObject(request(session,"student/link-code"))
        LinkCode(o.text("linked")=="true",o.text("linkCode"),o.text("expiresAt"),o.text("parentName"))
    }
    fun sync(session: AuthSession, body: JSONObject): Set<String> = parse {
        val o=JSONObject(request(session,"student/learning/sync","POST",body.toString()))
        require(o.getBoolean("success"))
        o.getJSONArray("acceptedIds").let { a->(0 until a.length()).map { a.getString(it) }.toSet() }
    }
    private inline fun <T> parse(block: () -> T): T = try { block() }
        catch(e: AuthFailure) { throw e }
        catch(e: FamilyFailure) { throw e }
        catch (_: Exception) { throw FamilyFailure("Dữ liệu theo dõi chưa hợp lệ. Hãy thử tải lại.") }
}
