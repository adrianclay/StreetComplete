package de.westnordost.streetcomplete.ui.user.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.westnordost.streetcomplete.ui.theme.DisabledGray
import de.westnordost.streetcomplete.ui.theme.GrassGreen
import de.westnordost.streetcomplete.ui.util.pxToDp
import de.westnordost.streetcomplete.util.ktx.systemTimeNow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import java.text.DateFormatSymbols
import kotlin.math.ceil
import kotlin.math.floor

/** Draws a github-style like days-active graphic */
@Composable
fun DatesActive(
    datesActive: Set<LocalDate>,
    datesActiveRange: Int,
    modifier: Modifier = Modifier,
    boxColor: Color = GrassGreen,
    emptyBoxColor: Color = DisabledGray,
    cellPadding: Dp = 2.dp,
    cellCornerRadius: Dp = 8.dp,
) {
    BoxWithConstraints(modifier) {
        val dayOffset = 7 - systemTimeNow().toLocalDateTime(TimeZone.UTC).dayOfWeek.value

        val verticalCells = 7 // days in a week
        val horizontalCells = ceil((dayOffset + datesActiveRange).toDouble() / verticalCells).toInt()

        val textMeasurer = rememberTextMeasurer()

        val textStyle = MaterialTheme.typography.caption
        val symbols = DateFormatSymbols.getInstance()
        val weekdays = Array(7) { symbols.shortWeekdays[1 + (it + 1) % 7] }
        val months = symbols.shortMonths

        val weekdayColumnWidth = weekdays.maxOf { textMeasurer.measure(it, textStyle).size.width }.pxToDp()
        val textHeight = textMeasurer.measure(months[0]).size.height.pxToDp()

        // stretch 100% width and determine available box size and then the height from that
        val boxSize = (maxWidth - weekdayColumnWidth - cellPadding * 2) / horizontalCells - cellPadding
        val height = textHeight + cellPadding * 2 + (boxSize + cellPadding) * verticalCells

        fun getLeft(x: Int) = weekdayColumnWidth + cellPadding * 2 + (boxSize + cellPadding) * x
        fun getTop(y: Int) = textHeight + cellPadding * 2 + (boxSize + cellPadding) * y

        Canvas(Modifier.size(maxWidth, height)) {
            // weekdays
            for (i in 0 until 7) {
                val top = getTop(i)
                val bottom = (getTop(i + 1) - cellPadding)
                // center text vertically
                val centerTop = top + (bottom - top - textHeight) / 2
                val left = 0f
                drawText(
                    textMeasurer,
                    text = weekdays[i],
                    topLeft = Offset(left, centerTop.toPx()),
                    style = textStyle
                )
            }
            // grid + months
            for (i in 0..datesActiveRange) {
                val time = systemTimeNow().minus(i, DateTimeUnit.DAY, TimeZone.UTC)
                val date = time.toLocalDateTime(TimeZone.UTC).date

                val y = (verticalCells - 1) - (i + dayOffset) % verticalCells
                val x = (horizontalCells - 1) - floor(((i + dayOffset) / verticalCells).toDouble()).toInt()

                val left = getLeft(x).toPx()
                val top = getTop(y).toPx()

                drawRoundRect(
                    color = if (date in datesActive) boxColor else emptyBoxColor,
                    topLeft = Offset(left, top),
                    size = Size(boxSize.toPx(), boxSize.toPx()),
                    cornerRadius = CornerRadius(cellCornerRadius.toPx(), cellCornerRadius.toPx())
                )

                if (date.dayOfMonth == 1) {
                    drawText(
                        textMeasurer,
                        text = months[date.month.value - 1],
                        topLeft = Offset(left, 0f),
                        style = textStyle
                    )
                }
            }
        }
    }
}

@Preview @Composable
fun DatesActivePreview() {
    DatesActive(
        datesActive = IntArray(30) { (0..90).random() }.map {
            systemTimeNow().minus(it, DateTimeUnit.DAY, TimeZone.UTC).toLocalDateTime(TimeZone.UTC).date
        }.toSet(),
        datesActiveRange = 90
    )
}
