package com.example.testtask

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.RenderVectorGroup
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

class RadialMenuBuilder {
    private var entries: MutableList<RadialMenuEntry> = mutableListOf()
    var maxDepth = 0

    fun addEntry(
        @DrawableRes
        icon: Int,
        text: String? = null,
        description: String,
        initChildren: (RadialMenuBuilder.() -> Unit)? = null
    ) {
        var childrenEntries: List<RadialMenuEntry>? = null

        if (maxDepth == 0) {
            maxDepth = 1
        }

        if (initChildren != null) {
            val builder = RadialMenuBuilder()
            builder.initChildren()
            childrenEntries = builder.entries

            if (maxDepth < 1 + builder.maxDepth) {
                maxDepth = 1 + builder.maxDepth
            }
        }

        entries.add(RadialMenuEntry(icon, text, description, childrenEntries))
    }

    @Composable
    fun compile(
        ringMargin: Float,
        maxHeight: Int,
        color: Color,
    ): RadialMenuRing {
        val sectorWidth = floor((maxHeight / 2 - ringMargin * maxDepth) / (maxDepth + 1))

        val density = LocalDensity.current

        val fontSize = with(density) {
            (sectorWidth / 6).toSp()
        }

        return this.compileRing(
            1,
            0f,
            360f,
            sectorWidth,
            ringMargin,
            color,
            fontSize,
            entries
        )
    }

    @Composable
    private fun compileRing(
        level: Int,
        startAngle: Float,
        maxAngle: Float,
        sectorWidth: Float,
        ringMargin: Float,
        color: Color,
        fontSize: TextUnit,
        entries: List<RadialMenuEntry>
    ): RadialMenuRing {
        val sweepAngle = min(360f / entries.size, maxAngle)

        val ring = RadialMenuRing(
            sectorWidth,
            level * (sectorWidth + ringMargin) + sectorWidth,
            level,
            color,
        )

        var currentAngle = startAngle

        for (entry in entries) {
            ring.sectors.add(
                this.compileSector(
                    entry,
                    sectorWidth,
                    currentAngle,
                    sweepAngle,
                    fontSize,
                    entry.children?.let {
                        this.compileRing(
                            level + 1,
                            currentAngle,
                            sweepAngle,
                            sectorWidth,
                            ringMargin,
                            color,
                            fontSize,
                            it
                        )
                    }
                )
            )

            currentAngle += sweepAngle
        }

        return ring
    }

    @Composable
    private fun compileSector(
        entry: RadialMenuEntry,
        sectorWidth: Float,
        currentAngle: Float,
        sweepAngle: Float,
        fontSize: TextUnit,
        children: RadialMenuRing?
    ): RadialMenuSector {
        val textMeasurer = rememberTextMeasurer()
        val vector = ImageVector.vectorResource(id = entry.icon)
        val painter = rememberVectorPainter(image = vector)

        var textMeasurerResult: TextLayoutResult? = null;

        entry.text?.let {
            textMeasurerResult = remember {
                textMeasurer.measure(
                    text = it,
                    style = TextStyle(
                        fontSize = fontSize,
                        textAlign = TextAlign.Center
                    ),
                    constraints = Constraints.fixedWidth(sectorWidth.toInt())
                )
            }
        }

        return RadialMenuSector(
            currentAngle,
            sweepAngle,
            painter,
            textMeasurerResult,
            entry.description,
            children
        )
    }
}

class RadialMenuEntry(
    @DrawableRes
    val icon: Int,
    val text: String?,
    val description: String,
    val children: List<RadialMenuEntry>?
)

fun polarToCartesian(radius: Float, angle: Float): Pair<Float, Float> {
    val properAngle = (angle % 360 - 90f).toDouble()

    return Pair(
        radius * cos(Math.toRadians(properAngle)).toFloat(),
        radius * sin(Math.toRadians(properAngle)).toFloat()
    )
}

class RadialMenuRing(
    val width: Float,
    val outerRadius: Float,
    val level: Int,
    val color: Color,
    var sectors: MutableList<RadialMenuSector> = mutableListOf()
) {
    fun DrawScope.drawSectors(
        selected: Array<Int?>,
        selectedColors: List<Color>,
        selectedRadiusFraction: Float
    ) {
        for (i in sectors.indices) {
            val sector = sectors[i]
            val radiusToCenter = outerRadius - width / 2f

            val startAngle = sector.startAngle - 90f
            val topLeft = Offset(
                this.size.width / 2f - radiusToCenter,
                this.size.height / 2f - radiusToCenter
            )
            val size = Size(radiusToCenter * 2f, radiusToCenter * 2f)
            val style = Stroke(width = width, cap = StrokeCap.Butt)
            val (x, y) = polarToCartesian(
                radiusToCenter,
                sector.startAngle + sector.sweepAngle / 2
            )
            val offset = Offset(x + this.size.width / 2, y + this.size.height / 2)

            if (i == selected[0]) {
                drawArc(
                    brush = Brush.radialGradient(
                        selectedColors,
                        offset,
                        (sector.sweepAngle / 40) * width * selectedRadiusFraction,
                    ),
                    startAngle = startAngle,
                    sweepAngle = sector.sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = size,
                    style = style
                )

                sector.children?.let {
                    with(it) {
                        drawSectors(
                            selected.sliceArray(1 until selected.size),
                            selectedColors,
                            selectedRadiusFraction
                        )
                    }
                }
            } else {
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sector.sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = size,
                    style = style
                )
            }

            var iconToText = 8f / level

            if ((sector.startAngle + sector.sweepAngle - 1) % 360 > 180) {
                iconToText *= -1
            }

            if(sector.text != null) {
                var additionalSpace = sector.text.lineCount.toFloat()

                val distanceToHalf = (sector.startAngle + sector.sweepAngle / 2) % 180
                if(distanceToHalf <= 45) {
                    additionalSpace += distanceToHalf / 10
                } else if(distanceToHalf >= 135) {
                    additionalSpace += (180 - distanceToHalf) / 10
                }

                if(iconToText < 0) {
                    iconToText -= additionalSpace
                } else {
                    iconToText += additionalSpace
                }
            } else {
                iconToText = 0f
            }

            val (vx, vy) = polarToCartesian(
                radiusToCenter,
                sector.startAngle + sector.sweepAngle / 2 - iconToText
            )
            val vectorOffset = Offset(vx + this.size.width / 2, vy + this.size.height / 2)

            with(sector.vector) {
                val vectorSize = width * .5f
                translate(
                    vectorOffset.x - vectorSize / 2,
                    vectorOffset.y - vectorSize / 2
                ) {
                    draw(Size(vectorSize, vectorSize))
                }
            }

            if (sector.text != null) {
                val (tx, ty) = polarToCartesian(
                    radiusToCenter,
                    sector.startAngle + sector.sweepAngle / 2 + iconToText
                )
                val textOffset = Offset(tx + this.size.width / 2, ty + this.size.height / 2)

                drawText(
                    sector.text,
                    Color.White,
                    Offset(
                        textOffset.x - sector.text.size.width / 2,
                        textOffset.y - sector.text.size.height / 2
                    ),
                )
            }
        }
    }
}

class RadialMenuSector(
    val startAngle: Float,
    val sweepAngle: Float,
    val vector: VectorPainter,
    val text: TextLayoutResult?,
    val description: String,
    val children: RadialMenuRing?
)