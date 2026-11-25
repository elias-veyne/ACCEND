package com.accend.app.ui.charts

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accend.app.ui.theme.AccendPalette
import com.accend.app.ui.theme.AccendThemeTokens
import com.accend.app.ui.theme.MonoFontFamily
import com.accend.app.util.AccendDate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun MindScoreRing(
    score: Int,
    modifier: Modifier = Modifier,
    ringSize: Dp = 210.dp
) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }

    val animatedFraction by animateFloatAsState(
        targetValue = if (started) (score.coerceIn(0, 100) / 100f) else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "mind_score_sweep"
    )

    Box(
        modifier = modifier.size(ringSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 15.dp.toPx()
            val inset = strokePx / 2f + 8.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val center = Offset(size.width / 2f, size.height / 2f)

            // Subtle ambient radial halo behind ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                    center = center,
                    radius = size.minDimension * 0.54f
                )
            )

            // Outer hairline frame ring
            drawCircle(
                color = AccendPalette.BorderSubtle.copy(alpha = 0.7f),
                radius = size.minDimension / 2f - 2.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )

            // Background track ring
            drawArc(
                color = AccendPalette.SurfaceHighlight,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Subtle white glow behind active arc
            if (animatedFraction > 0.01f) {
                drawArc(
                    color = Color.White.copy(alpha = 0.18f),
                    startAngle = -90f,
                    sweepAngle = animatedFraction * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx + 8.dp.toPx(), cap = StrokeCap.Round)
                )
                // Crisp glowing active arc
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            AccendPalette.AccentSilver,
                            AccendPalette.AccentWhite,
                            AccendPalette.AccentWhite
                        ),
                        center = center
                    ),
                    startAngle = -90f,
                    sweepAngle = animatedFraction * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 56.sp
                ),
                color = AccendPalette.AccentWhite
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "MIND SCORE",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.0.sp),
                color = AccendPalette.TextMuted
            )
        }
    }
}

data class GridCellData(
    val date: AccendDate,
    val completionPercent: Int,
    val isBeforeJoin: Boolean,
    val isFuture: Boolean,
    val isToday: Boolean
)

/**
 * 6 columns x 5 rows chronological column-major contribution grid (PRD A11 & screen_54d495bc.png).
 */
@Composable
fun ContributionGrid(
    cells: List<GridCellData>,
    showDayNumber: Boolean = false,
    onCellClick: ((GridCellData) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cols = 6
    val rows = 5
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (r in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (cIdx in 0 until cols) {
                    val idx = cIdx * rows + r // column-major
                    val cell = cells.getOrNull(idx)
                    if (cell == null || cell.isBeforeJoin) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Transparent)
                        )
                    } else {
                        val fillColor = when {
                            cell.isFuture -> Color.Transparent
                            cell.completionPercent <= 0 -> AccendPalette.SurfaceInner
                            cell.completionPercent < 35 -> Color(0xFF474752)
                            cell.completionPercent < 70 -> Color(0xFF8A8A98)
                            cell.completionPercent < 100 -> Color(0xFFD0D0DC)
                            else -> AccendPalette.AccentWhite
                        }
                        val borderColor = when {
                            cell.isToday -> AccendPalette.AccentWhite
                            cell.isFuture -> AccendPalette.BorderSubtle
                            cell.completionPercent <= 0 -> AccendPalette.BorderSubtle
                            else -> Color.Transparent
                        }
                        val hasGlow = !cell.isFuture && cell.completionPercent >= 95
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .drawBehind {
                                    if (hasGlow) {
                                        drawRoundRect(
                                            color = Color.White.copy(alpha = 0.22f),
                                            topLeft = Offset(-2.dp.toPx(), -2.dp.toPx()),
                                            size = Size(size.width + 4.dp.toPx(), size.height + 4.dp.toPx()),
                                            cornerRadius = CornerRadius(8.dp.toPx())
                                        )
                                    }
                                }
                                .clip(RoundedCornerShape(6.dp))
                                .background(fillColor)
                                .then(
                                    if (borderColor != Color.Transparent) {
                                        Modifier.border(
                                            width = if (cell.isToday) 1.5.dp else 1.dp,
                                            color = borderColor,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    } else Modifier
                                )
                                .then(
                                    if (onCellClick != null && !cell.isFuture) {
                                        Modifier.clickable { onCellClick(cell) }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (showDayNumber) {
                                Text(
                                    text = cell.date.day.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = if (cell.completionPercent >= 70 && !cell.isFuture) {
                                        AccendPalette.OnAccentWhite
                                    } else {
                                        AccendPalette.TextSecondary
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class RadarAxisPoint(
    val label: String,
    val normalized: Float, // 0f..1f
    val rawVolume: Double
)

@Composable
fun MuscleRadarChart(
    axes: List<RadarAxisPoint>,
    modifier: Modifier = Modifier
) {
    if (axes.size < 3) return
    var animStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animStarted = true }
    val animProgress by animateFloatAsState(
        targetValue = if (animStarted) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "radar_progress"
    )

    val count = axes.size
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(32.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f
            val rings = 4

            // Concentric guide polygons
            for (r in 1..rings) {
                val frac = r / rings.toFloat()
                val poly = Path()
                for (i in 0 until count) {
                    val angle = (-PI / 2.0) + (2.0 * PI * i / count)
                    val pt = Offset(
                        center.x + (radius * frac * cos(angle)).toFloat(),
                        center.y + (radius * frac * sin(angle)).toFloat()
                    )
                    if (i == 0) poly.moveTo(pt.x, pt.y) else poly.lineTo(pt.x, pt.y)
                }
                poly.close()
                drawPath(
                    path = poly,
                    color = AccendPalette.BorderSubtle.copy(alpha = if (r == rings) 0.9f else 0.55f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Spokes
            for (i in 0 until count) {
                val angle = (-PI / 2.0) + (2.0 * PI * i / count)
                val end = Offset(
                    center.x + (radius * cos(angle)).toFloat(),
                    center.y + (radius * sin(angle)).toFloat()
                )
                drawLine(
                    color = AccendPalette.BorderSubtle.copy(alpha = 0.6f),
                    start = center,
                    end = end,
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Data polygon
            val dataPath = Path()
            val vertexPoints = mutableListOf<Offset>()
            for (i in 0 until count) {
                val angle = (-PI / 2.0) + (2.0 * PI * i / count)
                val mag = (axes[i].normalized.coerceIn(0.08f, 1f)) * animProgress
                val pt = Offset(
                    center.x + (radius * mag * cos(angle)).toFloat(),
                    center.y + (radius * mag * sin(angle)).toFloat()
                )
                vertexPoints += pt
                if (i == 0) dataPath.moveTo(pt.x, pt.y) else dataPath.lineTo(pt.x, pt.y)
            }
            dataPath.close()

            drawPath(
                path = dataPath,
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.08f)),
                    center = center,
                    radius = radius
                )
            )
            // Soft outer glow stroke
            drawPath(
                path = dataPath,
                color = Color.White.copy(alpha = 0.22f),
                style = Stroke(width = 6.dp.toPx())
            )
            drawPath(
                path = dataPath,
                color = AccendPalette.AccentWhite,
                style = Stroke(width = 2.dp.toPx())
            )

            // Vertex nodes
            vertexPoints.forEach { pt ->
                drawCircle(color = AccendPalette.AccentWhite, radius = 4.dp.toPx(), center = pt)
                drawCircle(color = AccendPalette.BgBase, radius = 1.8.dp.toPx(), center = pt)
            }
        }

        // Top / Bottom / Left / Right cardinal labels when showing 4 axes or compact ring legend
        if (axes.size == 4) {
            // Top
            RadarCardinalLabel(axes[0], Modifier.align(Alignment.TopCenter))
            // Right
            RadarCardinalLabel(axes[1], Modifier.align(Alignment.CenterEnd))
            // Bottom
            RadarCardinalLabel(axes[2], Modifier.align(Alignment.BottomCenter))
            // Left
            RadarCardinalLabel(axes[3], Modifier.align(Alignment.CenterStart))
        }
    }
}

@Composable
private fun RadarCardinalLabel(axis: RadarAxisPoint, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = axis.label,
            style = MaterialTheme.typography.labelSmall,
            color = AccendPalette.TextMuted
        )
        Text(
            text = "${axis.rawVolume.toInt()} PTS",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = AccendPalette.AccentWhite
        )
    }
}

data class LineChartPoint(
    val xLabel: String,
    val value: Float,
    val tooltipText: String
)

@Composable
fun TelemetryLineChart(
    points: List<LineChartPoint>,
    modifier: Modifier = Modifier,
    minY: Float? = null,
    maxY: Float? = null,
    targetLineValue: Float? = null,
    targetLineLabel: String? = null,
    targetIsCeiling: Boolean = false,
    height: Dp = 170.dp
) {
    val c = AccendThemeTokens.colors
    if (points.isEmpty()) return

    var selectedIndex by remember(points) { mutableIntStateOf(-1) }
    val rawMin = minY ?: (points.minOf { it.value }.coerceAtMost(targetLineValue ?: Float.MAX_VALUE))
    val rawMax = maxY ?: (points.maxOf { it.value }.coerceAtLeast(targetLineValue ?: Float.MIN_VALUE))
    val span = (rawMax - rawMin).let { if (it < 0.001f) 1f else it }

    Column(modifier = modifier.fillMaxWidth()) {
        if (selectedIndex in points.indices) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(c.surfaceHighlight)
                    .border(1.dp, c.borderStrong, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = points[selectedIndex].tooltipText,
                    style = MaterialTheme.typography.labelMedium,
                    color = AccendPalette.AccentWhite
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        if (points.size == 1) {
                            selectedIndex = 0
                        } else {
                            val idx = ((offset.x / size.width) * (points.lastIndex))
                                .roundToInt()
                                .coerceIn(0, points.lastIndex)
                            selectedIndex = idx
                        }
                    }
                }
                .pointerInput(points) {
                    detectHorizontalDragGestures { change, _ ->
                        if (points.size > 1) {
                            val idx = ((change.position.x / size.width) * (points.lastIndex))
                                .roundToInt()
                                .coerceIn(0, points.lastIndex)
                            selectedIndex = idx
                        }
                    }
                }
        ) {
            val padTop = 14.dp.toPx()
            val padBottom = 14.dp.toPx()
            val padH = 12.dp.toPx()
            val drawW = (size.width - padH * 2).coerceAtLeast(1f)
            val drawH = (size.height - padTop - padBottom).coerceAtLeast(1f)

            fun yFor(v: Float): Float {
                val frac = ((v - rawMin) / span).coerceIn(0f, 1f)
                return padTop + drawH * (1f - frac)
            }

            // Target / Ceiling dashed reference line
            if (targetLineValue != null) {
                val ty = yFor(targetLineValue)
                drawLine(
                    color = if (targetIsCeiling) AccendPalette.Danger.copy(alpha = 0.8f) else AccendPalette.TextMuted,
                    start = Offset(padH, ty),
                    end = Offset(size.width - padH, ty),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
            }

            val coords = points.mapIndexed { i, pt ->
                val x = if (points.size == 1) size.width / 2f else padH + (drawW * i / points.lastIndex)
                Offset(x, yFor(pt.value))
            }

            if (coords.size >= 2) {
                val linePath = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    for (i in 0 until coords.lastIndex) {
                        val p0 = coords[i]
                        val p1 = coords[i + 1]
                        val midX = (p0.x + p1.x) / 2f
                        cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                    }
                }
                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(coords.last().x, size.height - padBottom)
                    lineTo(coords.first().x, size.height - padBottom)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                        startY = padTop,
                        endY = size.height - padBottom
                    )
                )
                // Glow stroke
                drawPath(
                    path = linePath,
                    color = Color.White.copy(alpha = 0.22f),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
                // Main crisp white stroke
                drawPath(
                    path = linePath,
                    color = AccendPalette.AccentWhite,
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Highlight dots (violations in crimson, latest in emerald/white, selected with guide line)
            coords.forEachIndexed { idx, pt ->
                val v = points[idx].value
                val exceedsCeiling = targetIsCeiling && targetLineValue != null && v > targetLineValue
                val isLast = idx == coords.lastIndex
                val isSelected = idx == selectedIndex
                if (isSelected) {
                    drawLine(
                        color = AccendPalette.TextMuted.copy(alpha = 0.5f),
                        start = Offset(pt.x, padTop),
                        end = Offset(pt.x, size.height - padBottom),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                }
                if (exceedsCeiling || isLast || isSelected || coords.size <= 10) {
                    val dotColor = when {
                        exceedsCeiling -> AccendPalette.Danger
                        isLast -> AccendPalette.Success
                        else -> AccendPalette.AccentWhite
                    }
                    drawCircle(
                        color = dotColor,
                        radius = if (isSelected || exceedsCeiling || isLast) 4.5.dp.toPx() else 3.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = AccendPalette.BgBase,
                        radius = 1.8.dp.toPx(),
                        center = pt
                    )
                }
            }
        }

        // Sparse X-axis labels & optional target caption
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = points.first().xLabel,
                style = MaterialTheme.typography.labelSmall,
                color = c.textMuted
            )
            if (targetLineLabel != null) {
                Text(
                    text = targetLineLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (targetIsCeiling) c.danger else c.textMuted
                )
            }
            if (points.size > 1) {
                Text(
                    text = points.last().xLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.success
                )
            }
        }
    }
}

@Composable
fun WeeklyBarChart(
    points: List<LineChartPoint>,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp
) {
    val c = AccendThemeTokens.colors
    if (points.isEmpty()) return
    var selectedIdx by remember(points) { mutableIntStateOf(-1) }
    val maxVal = (points.maxOfOrNull { it.value } ?: 1f).coerceAtLeast(1f)

    Column(modifier = modifier.fillMaxWidth()) {
        if (selectedIdx in points.indices) {
            Text(
                text = points[selectedIdx].tooltipText,
                style = MaterialTheme.typography.labelMedium,
                color = AccendPalette.AccentWhite,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 6.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            points.forEachIndexed { idx, pt ->
                val frac = (pt.value / maxVal).coerceIn(0.04f, 1f)
                val isSelected = idx == selectedIdx
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedIdx = idx },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height * frac * 0.85f)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = if (isSelected || idx == points.lastIndex) {
                                        listOf(AccendPalette.AccentWhite, AccendPalette.AccentSilver.copy(alpha = 0.45f))
                                    } else {
                                        listOf(AccendPalette.AccentSilver.copy(alpha = 0.6f), AccendPalette.SurfaceHighlight)
                                    }
                                )
                            )
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = points.first().xLabel, style = MaterialTheme.typography.labelSmall, color = c.textMuted)
            if (points.size > 1) {
                Text(text = points.last().xLabel, style = MaterialTheme.typography.labelSmall, color = c.textPrimary)
            }
        }
    }
}
