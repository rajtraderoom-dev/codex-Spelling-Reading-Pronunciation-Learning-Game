package com.wordgarden.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/** A simple front-view mouth cue, paired with spoken placement guidance. */
class MouthView @JvmOverloads constructor(context: Context, private val shape: String="open"): View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val oval=RectF()
    override fun onDraw(canvas: Canvas) {
        canvas.save();canvas.translate(width/2f,height/2f);val scale=resources.displayMetrics.density*0.8f;canvas.scale(scale,scale)
        val cx=0f; val cy=0f
        paint.color=Color.rgb(242,216,188); canvas.drawCircle(cx,cy,95f,paint)
        paint.color=Color.rgb(59,63,51); canvas.drawCircle(cx-30,cy-38,5f,paint); canvas.drawCircle(cx+30,cy-38,5f,paint)
        val wide=if(shape=="round") 27f else 55f
        val tall=when(shape){"closed"->4f;"open"->38f;"round"->32f;"smile"->15f;else->23f}
        paint.color=Color.rgb(114,62,57); oval.set(cx-wide,cy-5,cx+wide,cy+tall);canvas.drawOval(oval,paint)
        if(shape=="teeth" || shape=="tongue") { paint.color=Color.WHITE; canvas.drawRect(cx-42,cy-4,cx+42,cy+5,paint) }
        if(shape=="tongue") { paint.color=Color.rgb(218,132,135);oval.set(cx-20,cy+3,cx+20,cy+27);canvas.drawOval(oval,paint) }
        canvas.restore()
    }
}
