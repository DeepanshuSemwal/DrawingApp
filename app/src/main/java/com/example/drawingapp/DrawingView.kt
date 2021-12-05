package com.example.drawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View



class DrawingView(context:Context,attr:AttributeSet):View(context,attr) {

    private var mDrawpath:CustomPath?=null
    private var mCanvabitmap:Bitmap?=null
    private var mDrawpaint:Paint?=null
    private var mCanvaspaint:Paint?=null
    private var mbrushsize:Float=0.toFloat()
    private var color=Color.BLACK
    private var canvas:Canvas?=null
    private var mpaths=ArrayList<CustomPath>()
    private val munodpath=ArrayList<CustomPath>()

    init {
        setupdrwaing()
    }
         fun onclickUndo()
    {
        if(mpaths.size>0)
        {
            munodpath.add(mpaths.removeAt(mpaths.size-1))
            invalidate()
        }

    }
    private fun setupdrwaing()
    {
        mDrawpaint=Paint()
        mDrawpath=CustomPath(color,mbrushsize)
        mDrawpaint!!.color=color
        mDrawpaint!!.style=Paint.Style.STROKE
        mDrawpaint!!.strokeJoin=Paint.Join.ROUND
        mDrawpaint!!.strokeCap=Paint.Cap.ROUND
        mCanvaspaint=Paint(Paint.DITHER_FLAG)
        //mbrushsize=20.toFloat()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvabitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        canvas=Canvas(mCanvabitmap!!)

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mCanvabitmap!!,0f,0f,mCanvaspaint)
        for(path in mpaths)
        {
            mDrawpaint!!.strokeWidth=path!!.brushthickness
            mDrawpaint!!.color=path!!.color
            canvas.drawPath(path,mDrawpaint!!)
        }
        if(!mDrawpath!!.isEmpty)
        {
            mDrawpaint!!.strokeWidth=mDrawpath!!.brushthickness
            mDrawpaint!!.color=mDrawpath!!.color
            canvas.drawPath(mDrawpath!!,mDrawpaint!!)
        }

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        val touchx=event?.x
        val touchy=event?.y
        when(event?.action)
        {
            MotionEvent.ACTION_DOWN->{
                mDrawpath!!.color=color
                mDrawpath!!.brushthickness=mbrushsize

                mDrawpath!!.reset()
                if(touchx!=null)
                {
                    if(touchy!=null)
                    {
                        mDrawpath!!.moveTo(touchx,touchy)
                    }
                }
            }
            MotionEvent.ACTION_MOVE->{
                if(touchx!=null)
                {
                    if(touchy!=null)
                    {
                        mDrawpath!!.lineTo(touchx,touchy)
                    }
                }

            }
            MotionEvent.ACTION_UP->{
                mpaths.add(mDrawpath!!)
                mDrawpath=CustomPath(color,mbrushsize)
            }
            else->return false
        }
        invalidate()
        return true
    }
    fun setbrushsize(newsize:Float)
    {
        mbrushsize=TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,newsize,resources.displayMetrics)

        mDrawpaint!!.strokeWidth=mbrushsize
    }

    fun setcolor(newcolor: String) {
        color=Color.parseColor(newcolor)
        mDrawpaint!!.color=color
    }

    internal inner class CustomPath(var color:Int,var brushthickness:Float): Path() {

    }
}
