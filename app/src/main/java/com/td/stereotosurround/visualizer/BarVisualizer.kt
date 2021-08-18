package com.td.stereotosurround.visualizer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import com.td.stereotosurround.util.Constants
import java.util.*
import kotlin.math.ceil


/**
 *   ref : https://github.com/gauravk95/audio-visualizer-android/blob/master/audiovisualizer/src/main/java/com/gauravk/audiovisualizer/visualizer/BarVisualizer.java
 *
 * Created by TAPOS DATTA on 04,August,2021
 */

class BarVisualizer : BaseVisualizer {

    private var mMaxBatchCount = 0

    private var nPoints = 0

    private lateinit var mSrcY: FloatArray
    private lateinit var mDestY: FloatArray
    private lateinit var mClipBounds: Rect
    private lateinit var mRandom: Random

    private var mBarWidth = 0f
    private var nBatchCount = 0

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun init() {
        nPoints = (BAR_MAX_POINTS * mDensity).toInt()
        if (nPoints < BAR_MIN_POINTS) nPoints = BAR_MIN_POINTS
        mBarWidth = -1f
        nBatchCount = 0
        setAnimationSpeed(mAnimSpeed)
        mRandom = Random()
        mClipBounds = Rect()
        mSrcY = FloatArray(nPoints)
        mDestY = FloatArray(nPoints)
    }

    override fun setAnimationSpeed(animSpeed: AnimSpeed) {
        super.setAnimationSpeed(animSpeed)
        mMaxBatchCount = Constants.MAX_ANIM_BATCH_COUNT - mAnimSpeed.ordinal
    }

    override fun update(data: FloatArray) {
        if (data.isEmpty()) return
        audioBuffer = Array(data.size) { 0.0f }.toFloatArray()
        System.arraycopy(data, 0, audioBuffer, 0, data.lastIndex)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (mBarWidth == -1f) {
            canvas.getClipBounds(mClipBounds)
            mBarWidth = (width / nPoints).toFloat()
            //initialize points
            mSrcY.let {
                for (i in it.indices) {
                    val posY: Float =
                        if (mPositionGravity === PositionGravity.TOP) {
                            mClipBounds.top.toFloat()
                        } else {
                            mClipBounds.bottom.toFloat()
                        }
                    it[i] = posY
                    mDestY[i] = posY
                }
            }
        }

        //create the path and draw
        if (audioBuffer.isNotEmpty()) {
            if (isVisualizationEnabled) {
                //find the destination points for a batch
                if (nBatchCount == 0) {
                    for (i in mSrcY.indices) {
                        val x = audioBuffer.let {
                            val ind = ceil(((i + 1) * (it.size / nPoints)).toDouble())
                                .toInt()
                            if (ind >= it.size) it.size - 1 else ind
                        }
                        val t = (audioBuffer[x] * height).toInt()
                        val posY: Float =
                            if (mPositionGravity === PositionGravity.TOP)
                                mClipBounds.bottom.toFloat() - t
                            else mClipBounds.top.toFloat() + t

                        //change the source and destination y
                        mSrcY[i] = mDestY[i]
                        mDestY[i] = posY
                    }
                }

                //increment batch count
                nBatchCount++

                //calculate bar position and draw
                for (i in mSrcY.indices) {
                    val barY: Float =
                        mSrcY[i] + nBatchCount.toFloat() / mMaxBatchCount * (mDestY[i] - mSrcY[i])
                    val barX = i * mBarWidth + mBarWidth / 2
                    canvas.drawLine(barX, height.toFloat(), barX, barY, mPaint!!)
                }
                //reset the batch count
                if (nBatchCount == mMaxBatchCount) nBatchCount = 0
            }
        }

        super.onDraw(canvas)
    }

    private companion object {
        const val BAR_MAX_POINTS = 120
        const val BAR_MIN_POINTS = 3
    }
}