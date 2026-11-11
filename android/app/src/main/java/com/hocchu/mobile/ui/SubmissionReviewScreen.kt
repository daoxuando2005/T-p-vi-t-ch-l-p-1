package com.hocchu.mobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hocchu.mobile.classroom.ReviewState

@Composable
internal fun SubmissionReviewDialog(state:ReviewState,close:()->Unit,retry:()->Unit) {
    if(!state.visible)return
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize(),color=Background) {
            Column(Modifier.safeDrawingPadding().padding(16.dp).testTag("submission-review")) {
                Row(verticalAlignment=Alignment.CenterVertically) {Text("Bài đã nộp",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f));TextButton(onClick=close){Text("Đóng")}}
                if(state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.error?.let {Text(it,color=MaterialTheme.colorScheme.error);TextButton(onClick=retry){Text("Thử lại")}}
                state.review?.let {review->Column(Modifier.weight(1f).widthIn(max=880.dp).fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    Text(review.title,style=MaterialTheme.typography.headlineSmall)
                    if(review.instructions.isNotBlank())Text(review.instructions)
                    Text(if(review.score==null)"Đang chờ cô chấm bài" else "Điểm: ${review.score}/10",color=SkyDark)
                    if(review.feedback.isNotBlank())Paper {Text("Nhận xét của cô",style=MaterialTheme.typography.titleMedium);Text(review.feedback)}
                    Text("Nộp lúc: ${review.submittedAt.replace('T',' ')}",style=MaterialTheme.typography.bodySmall,color=Muted)
                    review.answers.forEachIndexed {i,answer->Paper {
                        Text("Câu ${i+1}: ${answer.question}",style=MaterialTheme.typography.titleMedium)
                        Text("Đã chọn: ${answer.selected}")
                        if(review.score!=null&&answer.expected.isNotBlank())Text("Đáp án: ${answer.expected}",color=SuccessText)
                    }}
                    review.images.forEach {image->Paper {
                        if(image.letter.isNotBlank())Text("Chữ ${image.letter}",style=MaterialTheme.typography.titleMedium)
                        LearningPicture(image.data,"","Bài viết của con chữ ${image.letter}")
                    }}
                    if(review.images.isEmpty()&&review.answers.isEmpty())Text("Bài đã được lưu theo định dạng cũ. Hãy xem nhận xét của cô ở trên.",color=Muted)
                    Spacer(Modifier.height(24.dp))
                }}
            }
        }
    }
}
