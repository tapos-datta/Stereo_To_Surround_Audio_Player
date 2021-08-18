package com.td.stereotosurround.visualizer

import android.content.Context
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.td.stereotosurround.R
import com.td.stereotosurround.util.Constants
import java.util.*


/**
 * Created by TAPOS DATTA on 04,August,2021
 */

abstract class BaseVisualizer(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    protected var audioBuffer = emptyArray<Float>().toFloatArray()
    protected var mPaint: Paint? = null
    protected var mColor: Int = Constants.DEFAULT_COLOR

    protected var mPaintStyle: PaintStyle = PaintStyle.FILL
    protected var mPositionGravity = PositionGravity.BOTTOM

    protected var mStrokeWidth: Float = Constants.DEFAULT_STROKE_WIDTH
    protected var mDensity: Float = Constants.DEFAULT_DENSITY

    protected var mAnimSpeed = AnimSpeed.MEDIUM
    protected var isVisualizationEnabled = false

    init {
        init(context, attrs!!)
        init()
    }

    private fun init(context: Context, attrs: AttributeSet) {

        //get the attributes specified in attrs.xml using the name we included
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.BaseVisualizer, 0, 0
        )
        if (typedArray.length() > 0) {
            try {
                //get the text and colors specified using the names in attrs.xml
                this.mDensity = typedArray.getFloat(
                    R.styleable.BaseVisualizer_avDensity,
                    Constants.DEFAULT_DENSITY
                )
                this.mColor = typedArray.getColor(
                    R.styleable.BaseVisualizer_avColor,
                    Constants.DEFAULT_COLOR
                )
                this.mStrokeWidth = typedArray.getDimension(
                    R.styleable.BaseVisualizer_avWidth,
                    Constants.DEFAULT_STROKE_WIDTH
                )
                val paintType = typedArray.getString(R.styleable.BaseVisualizer_avType)
                if (paintType != null && paintType != "") this.mPaintStyle =
                    if (paintType.lowercase(Locale.getDefault()) == "outline") PaintStyle.OUTLINE else PaintStyle.FILL
                val gravityType = typedArray.getString(R.styleable.BaseVisualizer_avGravity)
                if (gravityType != null && gravityType != "") this.mPositionGravity =
                    if (gravityType.lowercase(Locale.getDefault()) == "top") PositionGravity.TOP else PositionGravity.BOTTOM
                val speedType = typedArray.getString(R.styleable.BaseVisualizer_avSpeed)
                if (speedType != null && speedType != "") {
                    this.mAnimSpeed = AnimSpeed.MEDIUM
                    if (speedType.lowercase(Locale.getDefault()) == "slow") this.mAnimSpeed =
                        AnimSpeed.SLOW else if (speedType.lowercase(Locale.getDefault()) == "fast") this.mAnimSpeed =
                        AnimSpeed.FAST
                }
            } finally {
                typedArray.recycle()
            }
        }
        mPaint = Paint().apply {
            color = mColor
            strokeWidth = mStrokeWidth
            style =
                if (mPaintStyle === PaintStyle.FILL)
                    Paint.Style.FILL
                else Paint.Style.STROKE
        }
    }

    open fun setColor(color: Int) {
        this.mColor = color
        this.mPaint!!.setColor(this.mColor)
    }

    open fun setPaintStyle(paintStyle: PaintStyle) {
        this.mPaintStyle = paintStyle
        this.mPaint!!.style =
            if (paintStyle === PaintStyle.FILL) Paint.Style.FILL else Paint.Style.STROKE
    }

    open fun setPositionGravity(positionGravity: PositionGravity) {
        this.mPositionGravity = positionGravity
    }

    open fun setAnimationSpeed(animSpeed: AnimSpeed) {
        this.mAnimSpeed = animSpeed
    }

    open fun setStrokeWidth(width: Float) {
        this.mStrokeWidth = width
        this.mPaint!!.setStrokeWidth(width)
    }

    /**
     * Enable Visualization
     */
    open fun show() {
        isVisualizationEnabled = true
    }

    /**
     * Disable Visualization
     */
    open fun hide() {
        isVisualizationEnabled = false
    }


    protected abstract fun init()
    abstract fun update(data: FloatArray)


    /**
     * model
     */
    enum class AnimSpeed {
        SLOW, MEDIUM, FAST
    }

    enum class PaintStyle {
        OUTLINE, FILL
    }

    enum class PositionGravity {
        TOP, BOTTOM
    }

}