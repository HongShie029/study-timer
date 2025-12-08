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
                    studyRecords.add(record)
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
    var focusSeconds by remember { mutableStateOf("0") }

    var restHours by remember { mutableStateOf("0") }
    var restMinutes by remember { mutableStateOf("0") }
    var restSeconds by remember { mutableStateOf("0") }

    var remainingTime by remember { mutableStateOf(0) }
    var totalTime by remember { mutableStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }

    // 반복 기능
    var repeatCount by remember { mutableStateOf("0") }
    var repeatRemaining by remember { mutableStateOf(0) }
    var isRepeatMode by remember { mutableStateOf(false) }

    fun makeRecord(modeText: String, seconds: Int): String {
        val timeStr = formatTime(seconds)
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        return "$modeText $timeStr 완료 - $timestamp"
    }

    fun getFocusSeconds(): Int =
        (focusHours.toIntOrNull() ?: 0) * 3600 +
                (focusMinutes.toIntOrNull() ?: 0) * 60 +
                (focusSeconds.toIntOrNull() ?: 0)

    fun getRestSeconds(): Int =
        (restHours.toIntOrNull() ?: 0) * 3600 +
                (restMinutes.toIntOrNull() ?: 0) * 60 +
                (restSeconds.toIntOrNull() ?: 0)

    // 🔥 반복 = "라운드 1개" (집중 or 휴식 하나 끝날 때마다 반복 1 감소)
    //     반복 2라면 → 집중2 + 휴식2 = 총 4라운드
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000)
            remainingTime--

            if (remainingTime <= 0) {

                // 기록 저장
                val modeText = if (isFocusMode) "집중" else "휴식"
                onRecordAdd(makeRecord(modeText, totalTime))

                if (isRepeatMode) {

                    // 🔥 라운드 1개 종료 → repeatRemaining 1 감소
                    repeatRemaining--

                    if (repeatRemaining > 0) {
                        // 다음 라운드로 모드 전환
                        isFocusMode = !isFocusMode
                        totalTime = if (isFocusMode) getFocusSeconds() else getRestSeconds()
                        remainingTime = totalTime
                    } else {
                        // 반복 끝
                        isRunning = false
                        isRepeatMode = false
                    }

                } else {
                    // 일반 모드 종료
                    isRunning = false
                }
            }
        }
    }

    val progress = if (totalTime > 0) remainingTime.toFloat() / totalTime else 0f
    val circleSize = 420.dp

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Button(
                onClick = { isFocusMode = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFocusMode) Color(0xFF1976D2) else Color.LightGray
                )
            ) { Text("집중") }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = { isFocusMode = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isFocusMode) Color(0xFF388E3C) else Color.LightGray
                )
            ) { Text("휴식") }
        }

        Box(
            modifier = Modifier.size(circleSize),
            contentAlignment = Alignment.Center
        ) {
            CircularTimer(
                progress = progress,
                color = if (isFocusMode) Color(0xFF2196F3) else Color(0xFF4CAF50),
                sizeDp = circleSize
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                if (isRunning || remainingTime > 0) {
                    Text(formatTime(remainingTime), fontSize = 36.sp)
                } else {
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
                            onValueChange = { updateHours(it.filter(Char::isDigit)) },
                            label = { Text("시") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(70.dp)
                        )

                        TextField(
                            value = m,
                            onValueChange = { updateMinutes(it.filter(Char::isDigit)) },
                            label = { Text("분") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(70.dp)
                        )

                        TextField(
                            value = s,
                            onValueChange = { updateSeconds(it.filter(Char::isDigit)) },
                            label = { Text("초") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(70.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("반복 수:")
            Spacer(modifier = Modifier.width(8.dp))
            TextField(
                value = repeatCount,
                onValueChange = { repeatCount = it.filter(Char::isDigit) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

            Button(onClick = {
                if (!isRunning) {
                    totalTime = if (isFocusMode) getFocusSeconds() else getRestSeconds()
                    remainingTime = totalTime
                }
                isRepeatMode = false
                isRunning = true
            }) { Text("시작") }

            Button(onClick = {
                if (remainingTime < totalTime && remainingTime > 0) {
                    val modeText = if (isFocusMode) "집중" else "휴식"
                    val elapsed = totalTime - remainingTime
                    onRecordAdd(makeRecord(modeText, elapsed))
                }
                isRunning = false
                isRepeatMode = false
            }) { Text("중단") }

            // 🔥 반복 시작: 라운드 = 반복 수 * 2 (집중 + 휴식)
            Button(onClick = {
                val r = (repeatCount.toIntOrNull() ?: 0)
                if (r > 0) {
                    repeatRemaining = r * 2
                    isRepeatMode = true

                    totalTime = if (isFocusMode) getFocusSeconds() else getRestSeconds()
                    remainingTime = totalTime
                    isRunning = true
                }
            }) { Text("반복") }

            Button(onClick = {
                isRunning = false
                remainingTime = 0
                totalTime = 0
                repeatRemaining = 0
                isRepeatMode = false
            }) { Text("리셋") }
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
//ab