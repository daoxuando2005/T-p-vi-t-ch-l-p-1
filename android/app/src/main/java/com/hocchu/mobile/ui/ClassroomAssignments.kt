package com.hocchu.mobile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hocchu.mobile.classroom.*

@Composable
internal fun AssignmentCount(count: Int, modifier: Modifier = Modifier) {
    if (count > 0) Badge(modifier.semantics { contentDescription = "$count bài chưa nộp" },
        containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White) {
        Text(if (count > 99) "99+" else count.toString())
    }
}

@Composable
internal fun AssignmentGroups(assignments: List<SchoolAssignment>, selected: AssignmentGroup?,
    select: (AssignmentGroup) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth >= 1080.dp
        @Composable fun Card(group: AssignmentGroup, modifier: Modifier) {
            val color = when(group) {AssignmentGroup.Writing -> Sky; AssignmentGroup.Quiz -> Green; AssignmentGroup.Sentence -> Orange}
            val icon = when(group) {AssignmentGroup.Writing -> Icons.Rounded.Edit; AssignmentGroup.Quiz -> Icons.Rounded.Extension; AssignmentGroup.Sentence -> Icons.Rounded.AutoAwesome}
            val count = assignments.pendingCount(group = group)
            val total = assignments.count { it.group() == group }
            Surface(onClick = {select(group)}, modifier = modifier.testTag("assignment-group-${group.name}"),
                shape = RoundedCornerShape(24.dp), color = color.copy(alpha = .12f),
                border = BorderStroke(if(selected == group) 2.dp else 1.dp, if(selected == group) color else color.copy(alpha = .35f))) {
                Row(Modifier.padding(18.dp).heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(shape = RoundedCornerShape(18.dp), color = color.copy(alpha = .2f)) {
                        Icon(icon, null, Modifier.padding(14.dp).size(28.dp), tint = Ink)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(group.title, style = MaterialTheme.typography.titleMedium, color = Ink)
                        Text(when {count > 0 -> "$count bài chưa nộp"; total > 0 -> "Bé đã nộp đủ bài"; else -> "Chưa có bài cô giao"},
                            style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                    AssignmentCount(count, Modifier.testTag("group-pending-${group.name}"))
                    Icon(if(selected == group) Icons.Rounded.ExpandLess else Icons.Rounded.ChevronRight, null, tint = Ink)
                }
            }
        }
        if(wide) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AssignmentGroup.entries.forEach {Card(it, Modifier.weight(1f))}
        } else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AssignmentGroup.entries.forEach {Card(it, Modifier.fillMaxWidth())}
        }
    }
}
