package com.example.prova4
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class GnssPlotView_SVID_CN : View {
    private val plots = mutableMapOf<Int, Float>()
    private var constelations = emptyList<Int>()

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun setPlots(data: Map<Int, Float>, constelationTypes : List<Int>) {
        plots.clear()
        plots.putAll(data)
        constelations=constelationTypes
        invalidate() // Redraw the view
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas?.let {
            val width = it.width.toFloat()
            val height = it.height.toFloat()
            val plotCount = plots.size
            val plotSpacing = width / (plotCount + 1)

            val barWidth = plotSpacing / 2

            val paint = Paint()
            paint.color = Color.BLACK
            paint.strokeWidth = 2f

            for ((index, entry) in plots.entries.withIndex()) {
                val svId = entry.key
                val cn0Value = entry.value
                val constellation = constelations[index]

                paint.color = getColorFromConstelationType(constellation)
                val left = (index + 1) * plotSpacing - barWidth / 2
                val top = height - (cn0Value / 60f) * height // Scale CN0 values to fit height
                val right = left + barWidth
                val bottom = height
                it.drawRect(left, top, right, bottom, paint)
            }
        }
    }

    private fun getColorFromConstelationType(constelationType: Int): Int {
        // You can define your own color logic here based on the SvId
        // For simplicity, we use random colors for demonstration
        return when (constelationType) {
            1 -> Color.rgb(255,153,153)
            2 -> Color.MAGENTA
            3 -> Color.rgb(153,204,255)
            4 -> Color.CYAN
            5 -> Color.rgb(0,193,102)
            6 -> Color.YELLOW
            else -> Color.WHITE

        }
    }
}