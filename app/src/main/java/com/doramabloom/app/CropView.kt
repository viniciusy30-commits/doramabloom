package com.doramabloom.app

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import java.io.File

/**
 * Tela de enquadramento: a foto fica atrás de uma moldura fixa. Arraste para mover e use dois dedos
 * para aproximar ou afastar; o que está dentro da moldura é o que será salvo.
 */
class CropView(ctx: Context, private val aspect: Float, private val circle: Boolean) : View(ctx) {
    private var bmp: Bitmap? = null
    private val m = Matrix()
    private val frame = RectF()
    private var minScale = 1f
    private val tmp = FloatArray(9)
    private val rect = RectF()

    private val dim = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG)
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG)
    private val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val hole = Path()
    private val dimPath = Path()

    private var lastX = 0f
    private var lastY = 0f
    private var pointerId = -1
    private val scaler = ScaleGestureDetector(ctx, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(d: ScaleGestureDetector): Boolean {
            zoomBy(d.scaleFactor, d.focusX, d.focusY)
            return true
        }
    })

    init {
        dim.color = Color.argb(150, 0, 0, 0)
        line.style = Paint.Style.STROKE
        line.color = Color.WHITE
        line.strokeWidth = context.dp(2).toFloat()
        grid.style = Paint.Style.STROKE
        grid.color = Color.argb(90, 255, 255, 255)
        grid.strokeWidth = context.dp(1).toFloat()
    }

    fun setBitmap(b: Bitmap) {
        bmp = b
        resetFit()
    }

    fun hasBitmap(): Boolean = bmp != null

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        val margin = context.dp(22).toFloat()
        var fw = w - 2 * margin
        var fh = fw / aspect
        val maxH = h - 2 * margin
        if (fh > maxH) {
            fh = maxH
            fw = fh * aspect
        }
        val cx = w / 2f
        val cy = h / 2f
        frame.set(cx - fw / 2f, cy - fh / 2f, cx + fw / 2f, cy + fh / 2f)
        hole.reset()
        if (circle) hole.addOval(frame, Path.Direction.CW) else hole.addRoundRect(frame, context.dp(10).toFloat(), context.dp(10).toFloat(), Path.Direction.CW)
        dimPath.reset()
        dimPath.fillType = Path.FillType.EVEN_ODD
        dimPath.addRect(0f, 0f, w.toFloat(), h.toFloat(), Path.Direction.CW)
        dimPath.addPath(hole)
        resetFit()
    }

    /** Volta para a foto cobrindo a moldura inteira, centralizada. */
    fun resetFit() {
        val b = bmp ?: return
        if (frame.width() <= 0f) return
        minScale = maxOf(frame.width() / b.width, frame.height() / b.height)
        m.reset()
        m.postScale(minScale, minScale)
        m.postTranslate(frame.centerX() - b.width * minScale / 2f, frame.centerY() - b.height * minScale / 2f)
        invalidate()
    }

    /** Gira a foto 90° para a direita e recomeça o enquadramento. */
    fun rotate() {
        val b = bmp ?: return
        val r = Matrix()
        r.postRotate(90f)
        try {
            val nb = Bitmap.createBitmap(b, 0, 0, b.width, b.height, r, true)
            bmp = nb
            resetFit()
        } catch (e: Throwable) {
        }
    }

    private fun curScale(): Float {
        m.getValues(tmp)
        return tmp[Matrix.MSCALE_X]
    }

    private fun zoomBy(f: Float, fx: Float, fy: Float) {
        val s = curScale()
        val target = (s * f).coerceIn(minScale, minScale * 8f)
        val k = target / s
        m.postScale(k, k, fx, fy)
        clamp()
        invalidate()
    }

    /** Não deixa a foto sair da moldura: sempre cobre tudo. */
    private fun clamp() {
        val b = bmp ?: return
        rect.set(0f, 0f, b.width.toFloat(), b.height.toFloat())
        m.mapRect(rect)
        var dx = 0f
        var dy = 0f
        if (rect.left > frame.left) dx = frame.left - rect.left
        if (rect.right < frame.right) dx = frame.right - rect.right
        if (rect.top > frame.top) dy = frame.top - rect.top
        if (rect.bottom < frame.bottom) dy = frame.bottom - rect.bottom
        if (dx != 0f || dy != 0f) m.postTranslate(dx, dy)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        scaler.onTouchEvent(e)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                pointerId = e.getPointerId(0)
                lastX = e.x
                lastY = e.y
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // ao entrar o 2º dedo, só a pinça age; evita "pulo" na volta
                pointerId = -1
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val up = e.actionIndex
                val keep = if (up == 0) 1 else 0
                pointerId = e.getPointerId(keep)
                lastX = e.getX(keep)
                lastY = e.getY(keep)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaler.isInProgress && pointerId != -1) {
                    val i = e.findPointerIndex(pointerId)
                    if (i >= 0) {
                        m.postTranslate(e.getX(i) - lastX, e.getY(i) - lastY)
                        lastX = e.getX(i)
                        lastY = e.getY(i)
                        clamp()
                        invalidate()
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> pointerId = -1
        }
        return true
    }

    override fun onDraw(c: Canvas) {
        val b = bmp ?: return
        c.drawBitmap(b, m, imgPaint)
        // escurece tudo, menos a moldura
        c.drawPath(dimPath, dim)
        if (circle) {
            c.drawOval(frame, line)
        } else {
            val r = context.dp(10).toFloat()
            c.drawRoundRect(frame, r, r, line)
            val w3 = frame.width() / 3f
            val h3 = frame.height() / 3f
            for (i in 1..2) {
                c.drawLine(frame.left + w3 * i, frame.top + 4f, frame.left + w3 * i, frame.bottom - 4f, grid)
                c.drawLine(frame.left + 4f, frame.top + h3 * i, frame.right - 4f, frame.top + h3 * i, grid)
            }
        }
    }

    /** O pedaço da foto que está dentro da moldura (com no máximo [maxSide] px no lado maior). */
    fun cropped(maxSide: Int = 2000): Bitmap? {
        val b = bmp ?: return null
        return try {
            val inv = Matrix()
            if (!m.invert(inv)) return null
            val r = RectF(frame)
            inv.mapRect(r)
            val l = r.left.toInt().coerceIn(0, b.width - 1)
            val t = r.top.toInt().coerceIn(0, b.height - 1)
            val w = r.width().toInt().coerceIn(1, b.width - l)
            val h = r.height().toInt().coerceIn(1, b.height - t)
            var out = Bitmap.createBitmap(b, l, t, w, h)
            val big = maxOf(out.width, out.height)
            if (big > maxSide) {
                val k = maxSide.toFloat() / big
                out = Bitmap.createScaledBitmap(out, maxOf(1, (out.width * k).toInt()), maxOf(1, (out.height * k).toInt()), true)
            }
            // recorte pequeno/borrado: amplia com suavização e realça os detalhes
            ImageEnhance.autoIfSmall(out)
        } catch (e: Throwable) {
            null
        }
    }
}

/**
 * Abre a tela de enquadramento da imagem em [path]. Ao tocar em "Pronto", salva o recorte num arquivo
 * novo e devolve o caminho dele; se cancelar (ou der erro), devolve null e nada muda.
 * [aspect] = largura / altura da moldura (capa = 2/3, foto de pessoa = 1).
 */
fun Activity.showCropDialog(path: String, aspect: Float, circle: Boolean, title: String, onDone: (String?) -> Unit) {
    val src: Bitmap? = try {
        if (File(path).exists()) BitmapFactory.decodeFile(path) else null
    } catch (e: Throwable) {
        null
    }
    if (src == null) {
        Toast.makeText(this, "Não consegui abrir essa imagem.", Toast.LENGTH_SHORT).show()
        onDone(null)
        return
    }
    val dlg = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dlg.setCancelable(false)

    val root = FrameLayout(this)
    root.setBackgroundColor(Color.parseColor("#101014"))
    val cv = CropView(this, aspect, circle)
    root.addView(cv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    cv.setBitmap(src)

    val top = LinearLayout(this)
    top.orientation = LinearLayout.VERTICAL
    top.setPadding(dp(20), dp(34), dp(20), dp(10))
    top.addView(label(title, 17f, Color.WHITE, true, true))
    top.addView(label("Arraste para mover · use dois dedos para aproximar", 11.5f, Color.parseColor("#CCFFFFFF")), lin(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, t = 2))
    root.addView(top, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP))

    val bar = LinearLayout(this)
    bar.orientation = LinearLayout.HORIZONTAL
    bar.gravity = Gravity.CENTER_VERTICAL
    bar.setPadding(dp(14), dp(10), dp(14), dp(22))
    fun btn(text: String, icon: String?, fill: Boolean): android.widget.TextView {
        val p = pill(text, if (fill) Palette.pink else Color.parseColor("#33FFFFFF"), Color.WHITE, 13f, icon)
        p.setPadding(dp(14), dp(10), dp(14), dp(10))
        return p
    }
    val cancel = btn("Cancelar", "close", false)
    val rot = btn("Girar", "replay", false)
    val reset = btn("Centralizar", null, false)
    val ok = btn("Pronto", "check", true)
    bar.addView(cancel, lin(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, r = 6))
    bar.addView(rot, lin(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, r = 6))
    bar.addView(reset, lin(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, r = 6))
    bar.addView(View(this), lin(0, 1, 1f))
    bar.addView(ok, lin(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    root.addView(bar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))

    cancel.setOnClickListener {
        dlg.dismiss()
        onDone(null)
    }
    rot.setOnClickListener { cv.rotate() }
    reset.setOnClickListener { cv.resetFit() }
    ok.setOnClickListener {
        val out = cv.cropped()
        val saved = if (out != null) Store.saveBitmap(out) else null
        dlg.dismiss()
        if (saved == null) Toast.makeText(this, "Não consegui salvar o enquadramento.", Toast.LENGTH_SHORT).show()
        onDone(saved)
    }
    dlg.setContentView(root)
    dlg.show()
}
