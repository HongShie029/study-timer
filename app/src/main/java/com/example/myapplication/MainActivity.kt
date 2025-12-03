package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StudyTimerApp()
        }
    }
}

@Composable
fun StudyTimerApp() {
    var currentScreen by remember { mutableStateOf("timer") }
    val studyRecords = remember { mutableStateListOf<String>() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { currentScreen = "timer" }) { Text("타이머") }
            Button(onClick = { currentScreen = "record" }) { Text("기록") }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (currentScreen) {
                "timer" -> TimerScreen(onRecordAdd = { record ->
                    studyRecords.add(record)    // ★ clear() 제거 → 누적 저장
                })
                "record" -> RecordScreen(records = studyRecords)
            }
        }
    }
}

@Composable
fun TimerScreen(onRecordAdd: (String) -> Unit) {

    var isFocusMode by remember { mutableStateOf(true) }

    var focusHours by remember { mutableStateOf("0") }
    var focusMinutes by remember { mutableStateOf("0") }
    var focusSeconds by remember { mutableStateOf("10") }

    var restHours by remember { mutableStateOf("0") }
    var restMinutes by remember { mutableStateOf("0") }
    var restSeconds by remember { mutableStateOf("10") }

    var remainingTime by remember { mutableStateOf(0) }
    var totalTime by remember { mutableStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }

    var usedRestTime by remember { mutableStateOf(0) }

    fun makeRecord(modeText: String, seconds: Int): String {
        val timeStr = formatTime(seconds)
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        return "$modeText $timeStr 완료 - $timestamp"
    }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000)
            remainingTime--

            if (!isFocusMode) usedRestTime++

            if (remainingTime <= 0) {
                isRunning = false
                val modeText = if (isFocusMode) "집중" else "휴식"
                onRecordAdd(makeRecord(modeText, totalTime))
            }
        }
    }

    fun getFocusSeconds() =
        (focusHours.toIntOrNull() ?: 0) * 3600 +
                (focusMinutes.toIntOrNull() ?: 0) * 60 +
                (focusSeconds.toIntOrNull() ?: 0)

    fun getRestSeconds() =
        (restHours.toIntOrNull() ?: 0) * 3600 +
                (restMinutes.toIntOrNull() ?: 0) * 60 +
                (restSeconds.toIntOrNull() ?: 0)

    val progress = if (totalTime > 0) remainingTime.toFloat() / totalTime else 0f

    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        val circleSize = 420.dp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.size(circleSize)
        ) {
            CircularTimer(
                progress = progress,
                color = if (isFocusMode) Color(0xFF2196F3) else Color(0xFF4CAF50),
                sizeDp = circleSize
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.align(Alignment.Center)
        ) {
            if (isRunning || remainingTime > 0) {
                Text(text = formatTime(remainingTime), fontSize = 36.sp)
            }

            if (!isRunning) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isFocusMode) "집중 시간 입력" else "휴식 시간 입력")

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val h = if (isFocusMode) focusHours else restHours
                        val m = if (isFocusMode) focusMinutes else restMinutes
                        val s = if (isFocusMode) focusSeconds else restSeconds

                        fun updateHours(v: String) { if (isFocusMode) focusHours = v else restHours = v }
                        fun updateMinutes(v: String) { if (isFocusMode) focusMinutes = v else restMinutes = v }
                        fun updateSeconds(v: String) { if (isFocusMode) focusSeconds = v else restSeconds = v }

                        TextField(
                            value = h,
                            onValueChange = { updateHours(it.filter { c -> c.isDigit() }) },
                            label = { Text("시") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(74.dp)
                        )
                        TextField(
                            value = m,
                            onValueChange = { updateMinutes(it.filter { c -> c.isDigit() }) },
                            label = { Text("분") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(74.dp)
                        )
                        TextField(
                            value = s,
                            onValueChange = { updateSeconds(it.filter { c -> c.isDigit() }) },
                            label = { Text("초") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(74.dp)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    isFocusMode = true
                    if (remainingTime == 0) {
                        totalTime = getFocusSeconds()
                        remainingTime = totalTime
                    }
                    usedRestTime = 0
                    isRunning = true
                }) { Text("집중 시작") }

                Button(onClick = {
                    isFocusMode = false
                    if (remainingTime == 0) {
                        totalTime = getRestSeconds()
                        remainingTime = totalTime
                    }
                    usedRestTime = 0
                    isRunning = true
                }) { Text("휴식 시작") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    if (remainingTime < totalTime && remainingTime > 0) {
                        val modeText = if (isFocusMode) "집중" else "휴식"
                        val elapsed = totalTime - remainingTime
                        onRecordAdd(makeRecord(modeText, elapsed))
                    }
                    isRunning = false
                }) { Text("중단") }

                Button(onClick = {
                    isRunning = false
                    remainingTime = 0
                    totalTime = 0
                    usedRestTime = 0
                }) { Text("리셋") }
            }
        }
    }
}

@Composable
fun CircularTimer(progress: Float, color: Color, sizeDp: Dp = 400.dp) {
    Canvas(modifier = Modifier.size(sizeDp)) {
        val strokeWidth = (size.minDimension / 30f).coerceAtLeast(2f)
        val radius = size.minDimension / 2f - strokeWidth
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            color = Color(0xFFE6E6E6),
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun RecordScreen(records: SnapshotStateList<String>) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("공부 기록", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(records) { index, record ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = record)
                    Button(onClick = { records.removeAt(index) }) { Text("삭제") }
                }
            }
        }
    }
}

fun formatTime(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}
