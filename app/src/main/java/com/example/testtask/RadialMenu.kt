package com.example.testtask

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.logging.Handler
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sqrt

suspend fun PointerInputScope.radialMenuOnInput(
    mainRing: RadialMenuRing,
    selected: SnapshotStateList<Int?>,
    selectedLabel: MutableState<String>,
    animateProgress: suspend (target: Float) -> Unit,
    animateOpacity: suspend (target: Float) -> Unit,
    progress: Animatable<Float, AnimationVector1D>
) = coroutineScope {
    awaitEachGesture {
        val offset = awaitFirstDown().position
        val xPos = size.center.x - offset.x
        val yPos = size.center.y - offset.y
        val length = sqrt(xPos * xPos + yPos * yPos)
        var touchAngle = (atan2(
            xPos,
            yPos
        ) * 180 / Math.PI) * -1 % 360f

        if (touchAngle < 0) {
            touchAngle += 360f
        }

        var currentRing = mainRing
        var targetSectorI = -1
        var currentLevel = 0

        do {
            val innerRadius = currentRing.outerRadius - currentRing.width

            if (length in innerRadius..currentRing.outerRadius) {
                targetSectorI = currentRing.sectors.indexOfFirst {
                    touchAngle > it.startAngle && touchAngle < it.startAngle + it.sweepAngle
                }
            } else {
                val selectedI = selected[currentLevel]
                if (selectedI != null) {
                    val nextRing =
                        currentRing.sectors[selectedI].children ?: break

                    currentRing = nextRing
                    currentLevel++
                } else {
                    break
                }
            }
        } while (targetSectorI == -1 && currentLevel <= selected.size)

        if (targetSectorI != -1) {
            if (currentRing.sectors[targetSectorI].children != null) {
                selected[currentLevel] = targetSectorI
                selected[currentLevel + 1] = null
                selectedLabel.value =
                    currentRing.sectors[targetSectorI].description
            } else {
                launch {
                    animateProgress(360f)
                    animateOpacity(0f)
                    selected[currentLevel] = targetSectorI
                    selectedLabel.value =
                        currentRing.sectors[targetSectorI].description
                }
            }
        }

        do {
            val event: PointerEvent = awaitPointerEvent()

            event.changes.forEach { pointerInputChange: PointerInputChange ->
                pointerInputChange.consume()
            }
        } while (event.changes.any { it.pressed })


        launch {
            progress.snapTo(0f)
        }
    }
}

@Composable
fun RadialMenu(
    mainColor: Color,
    fraction: Float,
    ringMargin: Float = 40f,
    initEntries: RadialMenuBuilder.() -> Unit
) {
    val opacity = remember {
        Animatable(1f)
    }

    suspend fun animateOpacity(target: Float) {
        opacity.animateTo(
            targetValue = target,
            animationSpec = tween(
                durationMillis = 300,
                easing = LinearEasing
            )
        )
    }

    var progress = remember { Animatable(0F) }

    suspend fun animateProgress(target: Float) {
        progress.animateTo(
            targetValue = target,
            animationSpec = tween(
                durationMillis = 1000,
                easing = LinearEasing
            )
        )
    }

    val selectedLabel = remember {
        mutableStateOf("")
    }

    val builder = RadialMenuBuilder()
    builder.initEntries()

    var selected = remember {
        mutableStateListOf(*Array<Int?>(builder.maxDepth) { null })
    }

    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        FilledTonalButton(
            onClick = {
                for (i in selected.indices) {
                    selected[i] = null
                }
                coroutineScope.launch {
                    animateOpacity(1f)
                }
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .absoluteOffset(200.dp, 0.dp)
        ) {
            Text(text = "Открыть меню")
        }

        if (selectedLabel.value != "") {
            Text(
                text = "Выбранная опция: ${selectedLabel.value}",
                color = Color.Black,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .absoluteOffset(200.dp, 50.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.White)
                    .padding(10.dp)
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize(fraction)
            .alpha(opacity.value)
            .aspectRatio(1f, true)
    ) {
        val density = LocalDensity.current

        val maxHeight = with(density) {
            maxHeight.roundToPx()
        }
        val mainRing = builder.compile(ringMargin, maxHeight, mainColor)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    radialMenuOnInput(
                        mainRing,
                        selected,
                        selectedLabel,
                        ::animateProgress,
                        ::animateOpacity,
                        progress
                    )
                },
        ) {
            val borderColor = Color(0xFFD9D9D9)
            val outerRadius = 70f
            val innerRadius = 50f
            val center = Offset(this.size.width + outerRadius * 2, this.size.height - outerRadius)

            drawCircle(
                color = borderColor,
                radius = outerRadius,
                center = center,
                style = Stroke(1f)
            )
            drawCircle(
                color = Color(0x26000000),
                radius = innerRadius + (outerRadius - innerRadius) / 2,
                center = center,
                style = Stroke(outerRadius - innerRadius)
            )
            drawCircle(
                color = Color(0xFFD9D9D9),
                radius = innerRadius,
                center = center,
                style = Stroke(1f)
            )
            drawArc(
                color = Color(0xFFBC4400),
                startAngle = 90f,
                sweepAngle = progress.value,
                false,
                Offset(
                    this.size.width + outerRadius * 2 - 60f,
                    this.size.height - 60f - outerRadius
                ),
                size = Size(70f * 2 - 20f, 70f * 2 - 20f),
                style = Stroke(70f - 50f)
            )

            with(mainRing) {
                drawCircle(mainColor, width)
                drawSectors(
                    selected.toTypedArray(),
                    listOf(Color(0xFFC04500), Color.Black),
                    1f
                )
            }
        }

        val vector = ImageVector.vectorResource(id = R.drawable.ic_close)
        val painter = rememberVectorPainter(image = vector)
        with(mainRing) {
            val widthDp = with(density) {
                width.toDp()
            }
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        opacity.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = LinearEasing
                            )
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Transparent)
                    .padding(top = widthDp)
                    .size(widthDp)
            ) {
                Image(painter = painter, contentDescription = null)
            }

            var currentRing = mainRing
            var currentSector: RadialMenuSector? = null

            for (sel in selected) {
                if (sel == null) break

                currentSector = currentRing.sectors[sel]
                currentRing.sectors[sel].children?.let { currentRing = it } ?: break
            }

            if (currentSector != null) {
                with(density) {
                    Text(
                        text = currentSector.description,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = widthDp / 2)
                            .width((width * 1.8f).toDp()),
                        color = Color.White,
                        fontSize = (width / 5).toSp(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
