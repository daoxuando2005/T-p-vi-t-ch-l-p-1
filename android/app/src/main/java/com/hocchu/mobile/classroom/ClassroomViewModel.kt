package com.hocchu.mobile.classroom

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hocchu.mobile.auth.*
import kotlinx.coroutines.*

class ClassroomViewModel(app: Application, private val api: ClassroomApi) : AndroidViewModel(app) {
    constructor(app: Application) : this(app, ClassroomApi())
    var state by mutableStateOf(ClassroomState())
        private set
    private var session: AuthSession? = null
    private var store: AssignmentDraftStore? = null
    private var onExpired: () -> Unit = {}
    private var loadJob: Job? = null
    private var actionJob: Job? = null
    private var draftJob: Job? = null
    private var openJob: Job? = null
    private var accountScope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job]))

    fun connect(value: AuthSession?, expired: () -> Unit) {
        onExpired = expired
        if(session == value) return
        accountScope.cancel()
        accountScope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job]))
        loadJob?.cancel(); actionJob?.cancel(); draftJob?.cancel(); openJob?.cancel()
        session = value
        state = ClassroomState()
        store = value?.let { AssignmentDraftStore(getApplication<Application>()
            .getSharedPreferences(accountPreferences(it.server, it.account.id) + ".classroom", 0)) }
        if(value != null) refresh()
    }
    fun refresh() {
        val user = session ?: return
        loadJob?.cancel()
        loadJob = accountScope.launch {
            state = state.copy(loading = true, error = null)
            try {
                val (classes, assignments) = withContext(Dispatchers.IO) { api.classes(user) to api.assignments(user) }
                val id = state.classId?.takeIf { n -> classes.any { it.id == n } } ?: classes.firstOrNull()?.id
                state = state.copy(loading = false, classes = classes, assignments = assignments, classId = id)
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { state = state.copy(loading = false); failed(e) }
        }
    }
    fun chooseClass(id: Long) { if(!state.submitting && !state.joining) state = state.copy(classId = id, selectedId = null) }
    fun join(code: String) {
        val user = session ?: return
        if(state.joining || state.submitting) return
        actionJob = accountScope.launch {
            state = state.copy(joining = true, error = null, notice = null)
            try {
                withContext(Dispatchers.IO) { api.join(user, code) }
                state = state.copy(joining = false, notice = "Đã tham gia lớp. Bé chọn lớp để xem bài nhé!")
                refresh()
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { state = state.copy(joining = false); failed(e) }
        }
    }
    fun open(id: Long) {
        if(state.submitting || state.joining) return
        val assignment = state.assignments.find { it.id == id } ?: return
        val local = store ?: return
        openJob?.cancel()
        state = state.copy(selectedId = id, draft = AssignmentDraft(), opening = true, error = null, notice = null)
        openJob = accountScope.launch {
            try {
                draftJob?.join()
                val draft = withContext(Dispatchers.IO) { local.load(assignment) }
                if(state.selectedId == id) state = state.copy(draft = draft, opening = false)
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { state = state.copy(opening = false); failed(e) }
        }
    }
    fun close() { if(!state.submitting) { openJob?.cancel(); state = state.copy(selectedId = null, opening = false, error = null) } }
    fun answer(index: Int, value: String) {
        val questions = (state.selected?.content as? AssignmentContent.Quiz)?.questions ?: return
        if(index !in questions.indices || value !in questions[index].options) return
        val answers = List(questions.size) { state.draft.answers.getOrElse(it) { "" } }.toMutableList()
        answers[index] = value
        updateDraft(state.draft.copy(answers = answers))
    }
    fun drawing(letter: String, strokes: List<DraftStroke>) {
        if(strokes.sumOf { it.points.size } > 30000) {
            state = state.copy(error = "Bảng viết đã có nhiều nét. Bé hoàn tác hoặc xóa để viết lại nhé."); return
        }
        updateDraft(state.draft.copy(drawings = state.draft.drawings + (letter to strokes)))
    }
    fun letter(index: Int) {
        val content = state.selected?.content as? AssignmentContent.Writing ?: return
        if(index in content.letters.indices) updateDraft(state.draft.copy(letterIndex = index))
    }
    private fun updateDraft(draft: AssignmentDraft) {
        if(state.submitting || state.opening || state.selected?.submitted != false) return
        val assignment = state.selected ?: return
        val local = store ?: return
        state = state.copy(draft = draft, error = null)
        // Join the preceding write rather than cancel an in-progress disk commit.
        val previous = draftJob
        draftJob = accountScope.launch {
            try { previous?.join(); withContext(Dispatchers.IO) { local.save(assignment, draft) } }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { failed(e) }
        }
    }
    fun submit() {
        val user = session ?: return
        val assignment = state.selected ?: return
        val draft = state.draft
        val local = store ?: return
        if(state.submitting || state.joining || state.opening || assignment.submitted || !draft.ready(assignment.content)) return
        actionJob = accountScope.launch {
            state = state.copy(submitting = true, error = null, notice = null)
            try {
                draftJob?.join()
                withContext(Dispatchers.IO) { api.submit(user, assignment.id, submissionData(assignment, draft)) }
                // Acknowledged by the server: never report a failed submission for a later refresh/storage error.
                state = state.copy(submitting = false, selectedId = null,
                    assignments = state.assignments.map { if(it.id == assignment.id) it.copy(submitted = true) else it },
                    notice = if(assignment.content is AssignmentContent.Writing) "Đã nộp bài viết. Cô sẽ xem và nhận xét nhé!" else "Đã nộp bài. Bé tải lại để xem kết quả nhé!")
                try { withContext(Dispatchers.IO) { local.clear(assignment.id) } }
                catch(e: CancellationException) { throw e }
                catch (_: Exception) { /* Server acknowledgement remains authoritative. */ }
                refresh()
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { state = state.copy(submitting = false); failed(e) }
        }
    }
    fun dismissNotice() { state = state.copy(notice = null) }
    private fun failed(e: Exception) {
        if(e is AuthFailure.Expired) { onExpired(); return }
        state = state.copy(error = when(e) {
            is ClassroomFailure -> e.publicMessage
            is AuthFailure -> e.publicMessage
            else -> "Chưa thực hiện được. Bài đang làm vẫn được giữ để bé thử lại."
        })
    }
}
