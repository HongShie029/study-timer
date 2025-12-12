package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.material3.IconButton


// -------------------------------
// MainActivity: 앱 시작점 (변경 없음)
// -------------------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StudyTimerApp()
        }
    }
}

data class StudyRecord(
    var title: String = "집중시간",
    val elapsedSeconds: Int,
    val timestamp: String
)



// ------------------------------------------------------------
// StudyTimerApp: 타이머 관련 모든 상태를 호이스팅(상위로 올림)
//  - 화면 전환과 무관하게 타이머가 계속 동작하도록 함
//  - TimerScreen, RecordScreen에게 필요한 상태 / setter 들을 전달
// ------------------------------------------------------------
@Composable
fun StudyTimerApp() {
    // 화면 전환 상태 (timer / record)
    var currentScreen by remember { mutableStateOf("timer") }

    // ========== 타이머 관련 상태 (모두 여기로 옮김) ==========
    var isFocusMode by remember { mutableStateOf(true) }

    // 사용자 입력 (문자열 상태 유지)
    var focusHours by remember { mutableStateOf("0") }
    var focusMinutes by remember { mutableStateOf("0") }
    var focusSeconds by remember { mutableStateOf("0") }

    var restHours by remember { mutableStateOf("0") }
    var restMinutes by remember { mutableStateOf("0") }
    var restSeconds by remember { mutableStateOf("0") }

    // 실행 관련 상태
    var remainingTime by remember { mutableStateOf(0) } // 남은 초
    var totalTime by remember { mutableStateOf(0) }     // 현재 사이클의 전체 초
    var isRunning by remember { mutableStateOf(false) }

    // 반복 관련 상태
    var repeatCount by remember { mutableStateOf("1") }
    var repeatRemaining by remember { mutableStateOf(0) } // 내부 카운트(집중/휴식 토글 단위)
    var isRepeatMode by remember { mutableStateOf(false) }

    // 기존: val studyRecords = remember { mutableStateListOf<String>() }
    val studyRecords = remember { mutableStateListOf<StudyRecord>() }

    // 보조: 입력값을 초로 변환하는 함수들
    fun getFocusSeconds(): Int =
        (focusHours.toIntOrNull() ?: 0) * 3600 +
                (focusMinutes.toIntOrNull() ?: 0) * 60 +
                (focusSeconds.toIntOrNull() ?: 0)

    fun getRestSeconds(): Int =
        (restHours.toIntOrNull() ?: 0) * 3600 +
                (restMinutes.toIntOrNull() ?: 0) * 60 +
                (restSeconds.toIntOrNull() ?: 0)

    // 기록 문자열 생성 (formatTime 보조 함수는 파일 하단에 있음)
    // 기존 fun makeRecord(...) 대신 이 함수를 넣으세요
    fun makeRecord(modeText: String, seconds: Int): StudyRecord {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        return StudyRecord(
            title = modeText,         // 기본 제목은 modeText (예: "집중")
            elapsedSeconds = seconds, // 초 단위 경과 시간
            timestamp = timestamp
        )
    }


    // ========== 타이머 동작: 화면과 무관하게 동작하도록 여기에서 처리 ==========
    LaunchedEffect(isRunning) {
        // 이 루프는 isRunning이 true일 때만 동작
        while (isRunning) {
            delay(1000L)
            // 안전하게 0 밑으로 내려가는 것을 방지
            if (remainingTime > 0) {
                remainingTime--
            } else {
                // remainingTime이 0 이하가 된 시점: 사이클 종료 처리
                // 집중 모드였으면 기록 추가
                if (isFocusMode) {
                    // totalTime이 0이면 elapsed 계산이 이상하므로 안전 체크
                    val elapsed = if (totalTime > 0) totalTime else getFocusSeconds()
                    studyRecords.add(makeRecord("집중", elapsed))
                }

                if (isRepeatMode) {
                    // 반복 모드에서는 repeatRemaining을 감소시키고 토글
                    repeatRemaining--

                    if (repeatRemaining > 0) {
                        // 토글 (집중 <-> 휴식)
                        isFocusMode = !isFocusMode
                        totalTime = if (isFocusMode) getFocusSeconds() else getRestSeconds()
                        remainingTime = totalTime
                        // 계속 isRunning == true 이므로 루프 지속
                    } else {
                        // 반복 끝
                        isRunning = false
                        isRepeatMode = false
                    }
                } else {
                    // 반복 모드가 아니면 단일 사이클 종료 -> 멈춤
                    isRunning = false
                }
            }
        }
    }

    // ========== UI (버튼으로 화면 전환) ==========
    Column(modifier = Modifier.fillMaxSize()) {

        // 화면 컨텐츠: 하단 버튼을 위해 남는 공간만 차지하도록 weight 사용
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when (currentScreen) {
                "profile" -> {
                    ProfileScreen()
                }

                "timer" -> {
                    TimerScreen(
                        isFocusMode = isFocusMode,
                        onFocusModeChange = { isFocusMode = it },

                        focusHours = focusHours,
                        onFocusHoursChange = { focusHours = it },
                        focusMinutes = focusMinutes,
                        onFocusMinutesChange = { focusMinutes = it },
                        focusSeconds = focusSeconds,
                        onFocusSecondsChange = { focusSeconds = it },

                        restHours = restHours,
                        onRestHoursChange = { restHours = it },
                        restMinutes = restMinutes,
                        onRestMinutesChange = { restMinutes = it },
                        restSeconds = restSeconds,
                        onRestSecondsChange = { restSeconds = it },

                        remainingTime = remainingTime,
                        totalTime = totalTime,
                        isRunning = isRunning,
                        setRemainingTime = { remainingTime = it },
                        setTotalTime = { totalTime = it },
                        setRunning = { isRunning = it },

                        repeatCount = repeatCount,
                        onRepeatCountChange = { repeatCount = it },
                        repeatRemaining = repeatRemaining,
                        setRepeatRemaining = { repeatRemaining = it },
                        isRepeatMode = isRepeatMode,
                        setRepeatMode = { isRepeatMode = it },
                        onRequestStart = {
                            if (remainingTime <= 0) {
                                totalTime = if (isFocusMode) getFocusSeconds() else getRestSeconds()
                                remainingTime = totalTime
                                repeatRemaining = 0
                                isRepeatMode = false
                            }
                            isRunning = true
                        },
                        onRequestStop = {
                            if (remainingTime < totalTime && remainingTime > 0 && isFocusMode) {
                                val elapsed = totalTime - remainingTime
                                studyRecords.add(makeRecord("집중", elapsed))
                            }
                            isRunning = false
                        },
                        onRequestRepeat = {
                            val r = repeatCount.toIntOrNull() ?: 0
                            if (r > 0) {
                                repeatRemaining = r * 2
                                isRepeatMode = true
                                totalTime = if (isFocusMode) getFocusSeconds() else getRestSeconds()
                                remainingTime = totalTime
                                isRunning = true
                            }
                        },
                        onRequestReset = {
                            isRunning = false
                            remainingTime = 0
                            totalTime = 0
                            repeatRemaining = 0
                            isRepeatMode = false
                        },
                        onRecordAdd = { record -> studyRecords.add(record) }
                    )
                }

                "record" -> {
                    RecordScreen(
                        records = studyRecords,
                        onBack = { currentScreen = "timer" },
                        onRecordUpdate = { idx, newTitle ->
                            studyRecords[idx] = studyRecords[idx].copy(title = newTitle)
                        }
                    )
                }
            }
        }

        // 하단 네비게이션 바 (텍스트 버튼 → 이후 아이콘으로 바꾸기 쉬움)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 프로필 버튼
            Image(
                painter = painterResource(id = R.drawable.download),
                contentDescription = "프로필",
                modifier = Modifier
                    .size(48.dp)
                    .clickable { currentScreen = "profile" }
            )

            // 타이머 버튼
            Image(
                painter = painterResource(id = R.drawable.download),
                contentDescription = "타이머",
                modifier = Modifier
                    .size(48.dp)
                    .clickable { currentScreen = "timer" }
            )

            // 기록 버튼
            Image(
                painter = painterResource(id = R.drawable.download),
                contentDescription = "기록",
                modifier = Modifier
                    .size(48.dp)
                    .clickable { currentScreen = "record" }
            )
        }


    }
}

// -------------------------------
// 보조 함수: 초를 HH:MM:SS로 포맷
// (Timer/기록 양쪽에서 공용으로 사용)
// -------------------------------
fun formatTime(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}


// ------------------------------------------------------------
// TimerScreen: UI만 담당하도록 리팩토링된 컴포저블
// ------------------------------------------------------------
// (매개변수는 StudyTimerApp에서 전달하는 상태 / setter / 콜백과 정확히 일치해야 합니다.)
@Composable
fun TimerScreen(
    // 모드(집중/휴식)
    isFocusMode: Boolean,
    onFocusModeChange: (Boolean) -> Unit,

    // 집중 시간 입력 (문자열)
    focusHours: String,
    onFocusHoursChange: (String) -> Unit,
    focusMinutes: String,
    onFocusMinutesChange: (String) -> Unit,
    focusSeconds: String,
    onFocusSecondsChange: (String) -> Unit,

    // 휴식 시간 입력 (문자열)
    restHours: String,
    onRestHoursChange: (String) -> Unit,
    restMinutes: String,
    onRestMinutesChange: (String) -> Unit,
    restSeconds: String,
    onRestSecondsChange: (String) -> Unit,

    // 실행/시간 상태 (상위에서 관리)
    remainingTime: Int,
    totalTime: Int,
    isRunning: Boolean,
    setRemainingTime: (Int) -> Unit,
    setTotalTime: (Int) -> Unit,
    setRunning: (Boolean) -> Unit,

    // 반복 관련
    repeatCount: String,
    onRepeatCountChange: (String) -> Unit,
    repeatRemaining: Int,
    setRepeatRemaining: (Int) -> Unit,
    isRepeatMode: Boolean,
    setRepeatMode: (Boolean) -> Unit,

    // 액션 콜백 (상위에서 실제 로직 처리)
    onRequestStart: () -> Unit,
    onRequestStop: () -> Unit,
    onRequestRepeat: () -> Unit,
    onRequestReset: () -> Unit,

    // 기록 추가 (상위 리스트에 직접 추가할 수 있게)
    onRecordAdd: (StudyRecord) -> Unit
)
{
    // 진행률 계산 (0..1)
    val progress = if (totalTime > 0) remainingTime.toFloat() / totalTime else 0f
    val circleSize = 420.dp
// ⬇⬇⬇ 여기 추가
    val lifecycleOwner = LocalLifecycleOwner.current

// 최신 상태 안전하게 참조
    val currentIsRunning by rememberUpdatedState(isRunning)
    val currentRemaining by rememberUpdatedState(remainingTime)
    var wasRunningBeforePause by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {

                Lifecycle.Event.ON_PAUSE -> {
                    // 화면 벗어날 때 자동 중단(기록은 남기지 않음)
                    wasRunningBeforePause = currentIsRunning
                    setRunning(false)
                }

                Lifecycle.Event.ON_RESUME -> {
                    // 화면 복귀 시 자동 재생 금지 → 의도적으로 아무것도 하지 않음
                    // (setRunning(true) 절대 금지)
                }

                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }





    Column(
        modifier = Modifier.fillMaxSize().background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 모드 선택 버튼 (집중 / 휴식)
        // 현재 모드 표시 아이콘 (눌리지 않는 표시용)

        if (remainingTime > 0 || isRunning) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(
                        id = if (isFocusMode) R.drawable.download else R.drawable.imga
                    ),
                    contentDescription = if (isFocusMode) "집중 모드" else "휴식 모드",
                    modifier = Modifier.size(60.dp)
                )
            }
        }

        // 원형 타이머 + 내부 UI
        Box(
            modifier = Modifier.size(circleSize),
            contentAlignment = Alignment.Center
        ) {
            // CircularTimer는 3번째 조각에서 구현됩니다.
            CircularTimer(
                progress = progress,
                color = if (isFocusMode) Color(0xFF2196F3) else Color(0xFF4CAF50),
                sizeDp = circleSize
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
// 타이머가 한 번이라도 시작된 상태일 때만 모드 아이콘 표시


                if (isRunning || remainingTime > 0) {
                    // 타이머 실행 중 화면
                    Text(formatTime(remainingTime), fontSize = 36.sp)
                } else {
                    // ----------------------------
                    //  집중 + 휴식 입력을 모두 보여주는 화면
                    // ----------------------------

                    // ★ 집중시간 입력
                    Text("집중시간", fontSize = 22.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextField(
                            value = focusHours,
                            onValueChange = { onFocusHoursChange(it.filter(Char::isDigit)) },
                            label = { Text("시") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(70.dp)
                        )

                        TextField(
                            value = focusMinutes,
                            onValueChange = { onFocusMinutesChange(it.filter(Char::isDigit)) },
                            label = { Text("분") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(70.dp)
                        )

                        TextField(
                            value = focusSeconds,
                            onValueChange = { onFocusSecondsChange(it.filter(Char::isDigit)) },
                            label = { Text("초") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(70.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ★ 휴식시간 입력
                    Text("휴식시간", fontSize = 22.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextField(
                            value = restHours,
                            onValueChange = { onRestHoursChange(it.filter(Char::isDigit)) },
                            label = { Text("시") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(70.dp)
                        )

                        TextField(
                            value = restMinutes,
                            onValueChange = { onRestMinutesChange(it.filter(Char::isDigit)) },
                            label = { Text("분") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(70.dp)
                        )

                        TextField(
                            value = restSeconds,
                            onValueChange = { onRestSecondsChange(it.filter(Char::isDigit)) },
                            label = { Text("초") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(70.dp)
                        )
                    }
                }
            }


        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 버튼
            IconButton(onClick = { onRequestReset() }) {
                Image(
                    painter = painterResource(id = R.drawable.download),
                    contentDescription = "리셋"
                )
            }

            // 반복 버튼
            IconButton(onClick = { onRequestRepeat() }) {
                Image(
                    painter = painterResource(id = R.drawable.download),
                    contentDescription = "반복"
                )
            }

            // 중단 버튼
            IconButton(onClick = { onRequestStop() }) {
                Image(
                    painter = painterResource(id = R.drawable.download),
                    contentDescription = "중단"
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 반복 수 입력
            Text("반복 수:")
            TextField(
                value = repeatCount,
                onValueChange = { onRepeatCountChange(it.filter(Char::isDigit)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp)
            )
        }


        Spacer(modifier = Modifier.height(24.dp))

        // 제어 버튼들 (시작 / 중단 / 반복 / 리셋)

    }
}

// ------------------------------------------------------------
//  원형 타이머 UI — TimerScreen 내부에서 사용
// ------------------------------------------------------------
@Composable
fun CircularTimer(
    progress: Float,      // 0f ~ 1f
    color: Color,
    sizeDp: Dp = 300.dp,
    strokeWidth: Dp = 20.dp
) {
    Canvas(modifier = Modifier.size(sizeDp)) {

        val sweep = 360 * progress
        val halfStroke = strokeWidth.toPx() / 2

        // 배경 원
        drawArc(
            color = Color.LightGray.copy(alpha = 0.4f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round
            ),
            topLeft = Offset(halfStroke, halfStroke),
            size = androidx.compose.ui.geometry.Size(
                width = size.width - strokeWidth.toPx(),
                height = size.height - strokeWidth.toPx()
            )
        )

        // 진행률 원
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round
            ),
            topLeft = Offset(halfStroke, halfStroke),
            size = androidx.compose.ui.geometry.Size(
                width = size.width - strokeWidth.toPx(),
                height = size.height - strokeWidth.toPx()
            )
        )
    }
}

// ------------------------------------------------------------
//  기록 화면
// ------------------------------------------------------------
// RecordScreen 컴포저블 정의 (수정된 시그니처와 버튼 동작)
@Composable
fun RecordScreen(
    records: List<StudyRecord>,
    onBack: () -> Unit,
    onRecordUpdate: (index: Int, newTitle: String) -> Unit
) {
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editingTitle by remember { mutableStateOf("") }

    // 총 시간 계산 (초 → 포맷)
    val totalSeconds = records.sumOf { it.elapsedSeconds }
    val totalTimeStr = formatTime(totalSeconds)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("기록", fontSize = 28.sp)
        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(records) { index, record ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            editingIndex = index
                            editingTitle = record.title
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${index + 1}. ${record.title}", fontSize = 18.sp)
                        Text("${formatTime(record.elapsedSeconds)} · ${record.timestamp}", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("총 시간: $totalTimeStr", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (editingIndex != null) {
        AlertDialog(
            onDismissRequest = { editingIndex = null },
            title = { Text("기록 제목 수정") },
            text = {
                TextField(
                    value = editingTitle,
                    onValueChange = { editingTitle = it },
                    label = { Text("제목") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    editingIndex?.let { idx ->
                        onRecordUpdate(idx, editingTitle)
                    }
                    editingIndex = null
                }) { Text("저장") }
            },
            dismissButton = {
                Button(onClick = { editingIndex = null }) { Text("취소") }
            }
        )
    }
}

@Composable
fun ProfileScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("프로필 화면", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text("여기에 프로필 정보를 표시하세요.")
    }
}



