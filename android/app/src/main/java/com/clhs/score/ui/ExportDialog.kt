package com.clhs.score.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val target = !allChecked
                            allKeys.forEach { checkedState[it] = target }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = allChecked,
                        onCheckedChange = { target ->
                            allKeys.forEach { checkedState[it] = target }
                        },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "全選",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )

                structure.forEachIndexed { yearIndex, yearTerm ->
                    if (yearIndex > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = yearTerm.text,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 2.dp),
                    )
                    yearTerm.exams.forEach { exam ->
                        val key = "${yearTerm.value}|${exam.value}"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { checkedState[key] = !(checkedState[key] ?: false) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checkedState[key] == true,
                                onCheckedChange = { checkedState[key] = it },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = exam.text,
                                style = MaterialTheme.typography.bodyMedium,
                            )
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
            ) { Text("匯出") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
