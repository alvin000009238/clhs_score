package com.clhs.score.ui.schedule

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import com.clhs.score.data.ScheduleItem
import com.clhs.score.data.getSubjectColor
import com.clhs.score.ui.OutlinedRoundedSymbol
import com.clhs.score.viewmodel.ScheduleUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    uiState: ScheduleUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onYearSelected: (String) -> Unit,
    onClassSelected: (String) -> Unit,
    onConfirmSelection: () -> Unit,
    onClearSelection: () -> Unit,
    onOpenWidgetSettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showMoreMenu by remember { mutableStateOf(false) }
    var captureView: View? by remember { mutableStateOf(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的課表") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (uiState.report != null) {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多選項")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("重新選擇") },
                                onClick = {
                                    onClearSelection()
                                    showMoreMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("另存為圖片") },
                                onClick = {
                                    captureView?.let { view ->
                                        coroutineScope.launch {
                                            saveBitmapToGallery(context, view)
                                                .onSuccess {
                                                    Toast.makeText(context, "課表已儲存至相簿", Toast.LENGTH_SHORT).show()
                                                }
                                                .onFailure { error ->
                                                    Toast.makeText(
                                                        context,
                                                        "儲存失敗: ${error.message ?: "未知錯誤"}",
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                                }
                                        }
                                    } ?: run {
                                        Toast.makeText(context, "無法擷取課表", Toast.LENGTH_SHORT).show()
                                    }
                                    showMoreMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("新增 Widget") },
                                onClick = {
                                        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
                                        val componentName = android.content.ComponentName(context, com.clhs.score.widget.ScheduleWidgetReceiver::class.java)
                                        if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                            appWidgetManager.requestPinAppWidget(componentName, null, null)
                                        } else {
                                            Toast.makeText(context, "您的裝置不支援新增桌面小工具", Toast.LENGTH_SHORT).show()
                                        }
                                    showMoreMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Widget 設定") },
                                onClick = {
                                    onOpenWidgetSettings()
                                    showMoreMenu = false
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isInitialLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.report == null) {
                // Selection Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    var yearExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = yearExpanded,
                        onExpandedChange = { yearExpanded = !yearExpanded },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.availableYears.find { it.value == uiState.selectedYearValue }?.text ?: "選擇學期",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = yearExpanded,
                            onDismissRequest = { yearExpanded = false }
                        ) {
                            uiState.availableYears.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.text) },
                                    onClick = {
                                        onYearSelected(option.value)
                                        yearExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    var classExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = classExpanded,
                        onExpandedChange = { classExpanded = !classExpanded },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.availableClasses.find { it.value == uiState.selectedClassValue }?.text ?: "選擇班級",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = classExpanded,
                            onDismissRequest = { classExpanded = false }
                        ) {
                            if (uiState.availableClasses.isEmpty()) {
                                DropdownMenuItem(text = { Text("無可用班級") }, onClick = { classExpanded = false })
                            }
                            uiState.availableClasses.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.text) },
                                    onClick = {
                                        onClassSelected(option.value)
                                        classExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Button(
                        onClick = onConfirmSelection,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading && uiState.selectedYearValue != null
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("查詢課表")
                        }
                    }
                    
                    if (uiState.isError) {
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Grid Screen
                uiState.report.let { report ->
                    AndroidView(
                        factory = { ctx ->
                            ComposeView(ctx).apply {
                                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                                doOnLayout {
                                    captureView = this
                                }
                            }
                        },
                        update = { composeView ->
                            composeView.setContent {
                                Surface {
                                    ScheduleGrid(
                                        items = report.items,
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

private suspend fun saveBitmapToGallery(context: Context, view: View): Result<String> =
    try {
        val bitmap = withContext(Dispatchers.Main) {
            if (view.width <= 0 || view.height <= 0) {
                throw IOException("課表尚未完成排版")
            }
            androidx.core.graphics.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                .also { bitmap ->
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    view.draw(canvas)
                }
        }
        try {
            val filename = "Schedule_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.png"
            withContext(Dispatchers.IO) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Schedules")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IOException("無法建立圖片檔")
                try {
                    val output: OutputStream = resolver.openOutputStream(imageUri)
                        ?: throw IOException("無法開啟圖片檔")
                    output.use {
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) {
                            throw IOException("圖片壓縮失敗")
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                } catch (error: Exception) {
                    resolver.delete(imageUri, null, null)
                    throw error
                }
            }
            Result.success(filename)
        } finally {
            bitmap.recycle()
        }
    } catch (error: Exception) {
        Result.failure(error)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleGrid(
    items: List<ScheduleItem>,
    modifier: Modifier = Modifier
) {
    val days = listOf("時間", "週一", "週二", "週三", "週四", "週五")
    val periods = (1..8).toList()
    val itemsBySlot = remember(items) {
        items.associateBy { it.dayOfWeek to it.period }
    }
    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    var selectedItem by remember { mutableStateOf<ScheduleItem?>(null) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            days.forEachIndexed { index, day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
        
        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(periods) { period ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = period.toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = com.clhs.score.data.PERIOD_TIMES.getOrNull(period - 1)?.multiLine ?: "",
                            fontSize = 8.sp,
                            lineHeight = 10.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            softWrap = true
                        )
                    }

                    for (day in 1..5) {
                        val item = itemsBySlot[day to period]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 2.dp)
                        ) {
                            if (item != null) {
                                val tileColors = scheduleTileColors(item.subjectName, isDarkSurface)
                                val shortName = item.subjectName.split("-")[0]
                                Card(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { selectedItem = item },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = tileColors.container)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = shortName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            color = tileColors.content,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedItem?.let { item ->
        ScheduleDetailSheet(
            item = item,
            onDismiss = { selectedItem = null },
        )
    }
}

private data class ScheduleTileColors(
    val container: Color,
    val content: Color,
)

private fun scheduleTileColors(subjectName: String, isDarkSurface: Boolean): ScheduleTileColors {
    val rawBgColor = Color(getSubjectColor(subjectName))
    if (isDarkSurface) {
        return ScheduleTileColors(
            container = rawBgColor.copy(alpha = 0.15f),
            content = rawBgColor,
        )
    }

    val darkenedTextColor = Color(
        red = (rawBgColor.red * 0.3f).coerceIn(0f, 1f),
        green = (rawBgColor.green * 0.3f).coerceIn(0f, 1f),
        blue = (rawBgColor.blue * 0.3f).coerceIn(0f, 1f),
    )
    return ScheduleTileColors(
        container = rawBgColor,
        content = darkenedTextColor,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ScheduleDetailSheet(
    item: ScheduleItem,
    onDismiss: () -> Unit,
) {
    val hasTeacher = hasScheduleDetail(item.teacherName)
    val hasClassroom = hasScheduleDetail(item.classroom)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = item.subjectName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (hasTeacher || hasClassroom) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (hasTeacher) {
                        ScheduleDetailRow(
                            icon = "person",
                            label = "教師",
                            value = item.teacherName,
                        )
                    }
                    if (hasClassroom) {
                        ScheduleDetailRow(
                            icon = "meeting_room",
                            label = "教室",
                            value = item.classroom,
                        )
                    }
                }
            } else {
                Text(
                    text = "無詳細資訊",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("確定", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private fun hasScheduleDetail(value: String): Boolean = value.isNotBlank() && value != "null"

@Composable
private fun ScheduleDetailRow(
    icon: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedRoundedSymbol(
            icon = icon,
            tint = MaterialTheme.colorScheme.primary,
            size = 24.dp
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
