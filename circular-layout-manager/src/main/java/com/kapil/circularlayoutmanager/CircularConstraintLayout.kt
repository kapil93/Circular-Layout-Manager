package com.kapil.circularlayoutmanager

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * It is a Constraint Layout clipped into a circle or an ellipse depending upon it's width and
 * height.
 *
 * It also provides functionality to set width and height equal in case of match_parent
 * initialization of one the parameters depending on the value of primaryDimension.
 *
 * The same logic could be applied to any layout of choice.
 */
class CircularConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    enum class PrimaryDimension {
        WIDTH,
        HEIGHT,
        NONE
    }

    /**
     * Sets primary dimension of the view so that it's value can be set to the other dimension of
     * the layout.
     *
     * primaryDimension Can be one of WIDTH, HEIGHT and NONE.
     *
     * When WIDTH is set as the primary dimension, the value of the width of the layout is taken and
     * set as height of the layout to form a square.
     *
     * When HEIGHT is set as the primary dimension, the value of the height of the layout is taken
     * and set as width of the layout to form a square.
     *
     * When NONE is set as the primary dimension, the value of neither width nor height are altered
     * and the dimensions of the layout could be set as needed.
     *
     * If primaryDimension is set dynamically after the view is inflated, it must be invalidated for
     * the change to take effect.
     */
    var primaryDimension: PrimaryDimension = PrimaryDimension.WIDTH

    private val ovalPath: Path = Path()
    private val ovalRect: RectF = RectF()

    init {
        setWillNotDraw(false)

        context.theme.obtainStyledAttributes(attrs, R.styleable.CircularConstraintLayout, 0, 0)
            .apply {
                primaryDimension = PrimaryDimension.values()[getInt(
                    R.styleable.CircularConstraintLayout_primaryDimension,
                    PrimaryDimension.WIDTH.ordinal
                )]
                recycle()
            }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        when (primaryDimension) {
            PrimaryDimension.WIDTH -> layoutParams.height = measuredWidth
            PrimaryDimension.HEIGHT -> layoutParams.width = measuredHeight
            PrimaryDimension.NONE -> {
                // do nothing
            }
        }
        ovalRect.set(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat())
        ovalPath.apply {
            reset()
            addOval(ovalRect, Path.Direction.CW)
            close()
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.clipPath(ovalPath)
        super.onDraw(canvas)
    }
}