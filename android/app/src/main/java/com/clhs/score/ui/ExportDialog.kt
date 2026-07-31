package com.clhs.score.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clhs.score.data.ExamSelection
import com.clhs.score.data.YearTermOption

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExportDialog(
    structure: List<YearTermOption>,
    onConfirm: (List<ExamSelection>) -> Unit,
    onDismiss: () -> Unit,
) {
    val checkedState = remember(structure) {
        mutableStateMapOf<String, Boolean>().also { map ->
            structure.forEach { yearTerm ->
                yearTerm.exams.forEach { exam ->
                    map["${yearTerm.value}|${exam.value}"] = true
                }
            }
        }
    }

    val allKeys = remember(structure) {
        structure.flatMap { yearTerm ->
            yearTerm.exams.map { exam -> "${yearTerm.value}|${exam.value}" }
        }
    }
    val allChecked = allKeys.isNotEmpty() && allKeys.all { checkedState[it] == true }
    val noneChecked = allKeys.all { checkedState[it] != true }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("選擇要匯出的考試") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SegmentedListItem(
                    checked = allChecked,
                    onCheckedChange = { target ->
                        allKeys.forEach { checkedState[it] = target }
                    },
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                    leadingContent = {
                        Checkbox(
                            checked = allChecked,
                            onCheckedChange = null,
                        )
                    },
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "全選",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                structure.forEach { yearTerm ->
                    Text(
                        text = yearTerm.text,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 2.dp),
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                    ) {
                        yearTerm.exams.forEachIndexed { index, exam ->
                            val key = "${yearTerm.value}|${exam.value}"
                            SegmentedListItem(
                                checked = checkedState[key] == true,
                                onCheckedChange = { checkedState[key] = it },
                                shapes = ListItemDefaults.segmentedShapes(
                                    index = index,
                                    count = yearTerm.exams.size,
                                ),
                                leadingContent = {
                                    Checkbox(
                                        checked = checkedState[key] == true,
                                        onCheckedChange = null,
                                    )
                                },
                                colors = ListItemDefaults.segmentedColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(text = exam.text)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val selections = structure.flatMap { yearTerm ->
                        yearTerm.exams
                            .filter { exam -> checkedState["${yearTerm.value}|${exam.value}"] == true }
                            .map { exam ->
                                ExamSelection(
                                    yearValue = yearTerm.value,
                                    examValue = exam.value,
                                    displayName = exam.text,
                                )
                            }
                    }
                    onConfirm(selections)
                },
                enabled = !noneChecked,
                shapes = ButtonDefaults.shapes(),
            ) { Text("匯出") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes(),
            ) { Text("取消") }
        },
    )
}
