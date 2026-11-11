package com.hocchu.mobile.family

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hocchu.mobile.auth.*
import com.hocchu.mobile.classroom.ReviewState
import kotlinx.coroutines.*
import java.time.Instant
import java.time.LocalDate

class ParentViewModel(app: Application,private val api: FamilyApi) : AndroidViewModel(app) {
    constructor(app: Application):this(app,FamilyApi())
    var state by mutableStateOf(ParentState());private set
    private var session: AuthSession?=null
    private var scope=CoroutineScope(viewModelScope.coroutineContext+SupervisorJob(viewModelScope.coroutineContext[Job]))
    private var load: Job?=null
    private var action: Job?=null
    private var expired: ()->Unit={}
    var review by mutableStateOf(ReviewState());private set
    private var reviewJob:Job?=null
    private var reviewId:Long?=null
    fun connect(value: AuthSession?,onExpired: ()->Unit) {
        expired=onExpired
        if(session==value) return
        scope.cancel();scope=CoroutineScope(viewModelScope.coroutineContext+SupervisorJob(viewModelScope.coroutineContext[Job]))
        session=value;state=ParentState();load=null;action=null;review=ReviewState();reviewJob=null;reviewId=null
        if(value!=null) refresh()
    }
    fun refresh() {
        val user=session ?: return
        load?.cancel()
        load=scope.launch {state=state.copy(loading=true,error=null)
            try {
                val children=withContext(Dispatchers.IO) {api.children(user)}
                val selected=state.selectedId?.takeIf {id->children.any {it.id==id}} ?: children.firstOrNull()?.id
                if (selected != state.selectedId) closeReview()
                state=state.copy(children=children,selectedId=selected,
                    dashboard=state.dashboard?.takeIf {it.studentId==selected && it.date==state.date})
                val date=state.date
                val dashboard=selected?.let {withContext(Dispatchers.IO) {api.dashboard(user,it,date)}}
                state=state.copy(loading=false,dashboard=dashboard,refreshedAt=Instant.now())
            } catch(e: CancellationException) {throw e}
            catch(e: Exception) {closeReview();state=state.copy(loading=false,dashboard=null);failure(e)}
        }
    }
    fun chooseChild(id: Long) {
        if(state.children.none {it.id==id}) return
        closeReview();state=state.copy(selectedId=id,dashboard=null);refresh()
    }
    fun date(value: LocalDate) {
        if(value>LocalDate.now(SCHOOL_ZONE) || value<LocalDate.now(SCHOOL_ZONE).minusDays(90)) return
        closeReview();state=state.copy(date=value,dashboard=null);refresh()
    }
    fun link(code: String) {
        val user=session ?: return
        if(state.linking) return
        action=scope.launch {state=state.copy(linking=true,error=null,notice=null)
            try {withContext(Dispatchers.IO) {api.link(user,code)};state=state.copy(linking=false,notice="Đã liên kết với con.");refresh()}
            catch(e: CancellationException) {throw e}
            catch(e: Exception) {state=state.copy(linking=false);failure(e)}
        }
    }
    fun dismissNotice() {state=state.copy(notice=null)}
    fun unlink(id:Long) {
        val user=session?:return;if(state.linking)return
        action=scope.launch {state=state.copy(linking=true,error=null,notice=null)
            try{withContext(Dispatchers.IO){api.unlink(user,id)};closeReview();state=state.copy(linking=false,dashboard=null,notice="Đã ngắt liên kết. Tiến độ của con vẫn được giữ.");refresh()}
            catch(e:CancellationException){throw e}catch(e:Exception){state=state.copy(linking=false);failure(e)}
        }
    }
    fun openReview(id:Long) {
        val user=session?:return;val child=state.selectedId?:return
        reviewJob?.cancel();reviewId=id;review=ReviewState(visible=true,loading=true)
        reviewJob=scope.launch {try {
            val detail=withContext(Dispatchers.IO) {api.review(user,child,id)}
            review=ReviewState(visible=true,review=detail)
        } catch(e:CancellationException){throw e}
        catch(e:Exception){if(e is AuthFailure.Expired)expired() else review=ReviewState(visible=true,error=(e as? FamilyFailure)?.publicMessage?:"Chưa tải được bài làm. Hãy thử lại.")}}
    }
    fun retryReview(){reviewId?.let(::openReview)}
    fun closeReview(){reviewJob?.cancel();review=ReviewState();reviewId=null}
    private fun failure(e: Exception) {
        if(e is AuthFailure.Expired) {expired();return}
        state=state.copy(error=when(e) {is FamilyFailure->e.publicMessage;is AuthFailure->e.publicMessage;else->"Chưa tải được hoạt động của con. Hãy thử lại."})
    }
}
