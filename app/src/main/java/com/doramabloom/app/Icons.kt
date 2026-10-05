package com.doramabloom.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.PathParser
import kotlin.math.sin

/** Biblioteca de ícones desenhados (grade 24x24). Nada de emojis: tudo é vetor. */
object Icons {
    private val svg: Map<String, String> = mapOf(
        "heart" to "M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z",
        "star" to "M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-0.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z",
        "home" to "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z",
        "search" to "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z",
        "add" to "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z",
        "minus" to "M19 13H5v-2h14v2z",
        "back" to "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z",
        "check" to "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z",
        "close" to "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z",
        "edit" to "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z",
        "delete" to "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z",
        "play" to "M8 5v14l11-7z",
        "pause" to "M6 19h4V5H6v14zm8-14v14h4V5h-4z",
        "bookmark" to "M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2z",
        "chart" to "M5 9.2h3V19H5zM10.6 5h2.8v14h-2.8zm5.6 8H19v6h-2.8z",
        "list" to "M3 14h4v-4H3v4zm0 5h4v-4H3v4zM3 9h4V5H3v4zm5 5h13v-4H8v4zm0 5h13v-4H8v4zM8 5v4h13V5H8z",
        "grid" to "M3 3v8h8V3H3zm6 6H5V5h4v4zm-6 4v8h8v-8H3zm6 6H5v-4h4v4zm4-16v8h8V3h-8zm6 6h-4V5h4v4zm-6 4v8h8v-8h-8zm6 6h-4v-4h4v4z",
        "sparkle" to "M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z",
        "bolt" to "M7 2v11h3v9l7-12h-4l4-8z",
        "moon" to "M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9 9-4.03 9-9c0-.46-.04-.92-.1-1.36-.98 1.37-2.58 2.26-4.4 2.26-2.98 0-5.4-2.42-5.4-5.4 0-1.81.89-3.42 2.26-4.4-.44-.06-.9-.1-1.36-.1z",
        "smile" to "M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z",
        "pagoda" to "M12 2L18 6H6zM9 6H15V9H9zM12 9L20 14H4zM8 14H16V17H8zM12 17L22 22H2z",
        "school" to "M12 3L1 9l4 2.18v6L12 21l7-3.82v-6l2-1.09V17h2V9L12 3zm6.82 6L12 12.72 5.18 9 12 5.28 18.82 9zM17 15.99l-5 2.73-5-2.73v-3.72L12 15l5-2.73v3.72z",
        "drop" to "M12 2c-5.33 4.55-8 8.48-8 11.8 0 4.98 3.8 8.2 8 8.2s8-3.22 8-8.2c0-3.32-2.67-7.25-8-11.8z",
        "clock" to "M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z",
        "tv" to "M21 3H3c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h5v2h8v-2h5c1.1 0 1.99-.9 1.99-2L23 5c0-1.1-.9-2-2-2zm0 14H3V5h18v12z",
        "person" to "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z",
        "calendar" to "M19 4h-1V2h-2v2H8V2H6v2H5c-1.11 0-1.99.9-1.99 2L3 20c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V10h14v10zm0-12H5V6h14v2z",
        "flag" to "M14.4 6L14 4H5v17h2v-7h5.6l.4 2h7V6z",
        "sort" to "M3 18h6v-2H3v2zM3 6v2h18V6H3zm0 7h12v-2H3v2z",
        "filter" to "M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z",
        "upload" to "M9 16h6v-6h4l-7-7-7 7h4zm-4 2h14v2H5z",
        "download" to "M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z",
        "image" to "M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z",
        "leaf" to "M17 8C8 10 5.9 16.17 3.82 21.34L5.71 22l1-2.3A4.49 4.49 0 0 0 8 20c11 0 14-9 14-18-2 2-4 4-5 6z",
        "replay" to "M12 5V1L7 6l5 5V7c3.31 0 6 2.69 6 6s-2.69 6-6 6-6-2.69-6-6H4c0 4.42 3.58 8 8 8s8-3.58 8-8-3.58-8-8-8z",
        "tag" to "M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58.55 0 1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41 0-.55-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z",
        "book" to "M18 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM6 4h5v8l-2.5-1.5L6 12V4z",
        "tune" to "M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z",
        "more" to "M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z",
        "link" to "M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1zM8 13h8v-2H8v2zm9-6h-4v1.9h4c1.71 0 3.1 1.39 3.1 3.1s-1.39 3.1-3.1 3.1h-4V17h4c2.76 0 5-2.24 5-5s-2.24-5-5-5z",
        "forward" to "M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z",
        "globe" to "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z",
        "eye" to "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z",
        "cross" to "M9.5 3h5v6.5H21v5h-6.5V21h-5v-6.5H3v-5h6.5z",
        "music" to "M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z",
        "trophy" to "M19 5h-2V3H7v2H5c-1.1 0-2 .9-2 2v1c0 2.55 1.92 4.63 4.39 4.94.63 1.5 1.98 2.63 3.61 2.96V19H7v2h10v-2h-4v-3.1c1.63-.33 2.98-1.46 3.61-2.96C19.08 12.63 21 10.55 21 8V7c0-1.1-.9-2-2-2zM5 8V7h2v3.82C5.84 10.4 5 9.3 5 8zm14 0c0 1.3-.84 2.4-2 2.82V7h2v1z",
        "crown" to "M4 19h16v2H4zM3.2 7l4.6 4.2L12 4.5l4.2 6.7L20.8 7 19 17H5z",
        "rocket" to "M12 2c3.2 2.6 4.8 6.2 4.8 10.4V16l2.2 2.6V21l-3.4-1.4H8.4L5 21v-2.4L7.2 16v-3.6C7.2 8.2 8.8 4.6 12 2zM12 9.2a1.9 1.9 0 1 0 0 3.8 1.9 1.9 0 0 0 0-3.8z",
        "coffee" to "M4 8h12.5v6a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5zM16.5 9.5h1.3a2.6 2.6 0 0 1 0 5.2h-1.3V13h1.3a.9.9 0 0 0 0-1.8h-1.3zM3 21h15v-1.6H3z"
    )

    private val cache = HashMap<String, Path>()

    fun path(name: String): Path {
        val hit = cache[name]
        if (hit != null) return hit
        val p = build(name)
        cache[name] = p
        return p
    }

    private fun build(name: String): Path {
        if (name == "blossom") return blossom()
        if (name == "petal") return petalCentered()
        if (name == "ghost") return ghost()
        if (name == "skull") return skull()
        val d = svg[name] ?: return Path()
        return try {
            PathParser.createPathFromPathData(d)
        } catch (e: Exception) {
            Path()
        }
    }

    private fun petalShape(): Path {
        val p = Path()
        p.moveTo(12f, 12f)
        p.cubicTo(6f, 9f, 7f, 2f, 10.8f, 1f)
        p.lineTo(12f, 2.6f)
        p.lineTo(13.2f, 1f)
        p.cubicTo(17f, 2f, 18f, 9f, 12f, 12f)
        p.close()
        return p
    }

    private fun petalCentered(): Path {
        val p = petalShape()
        val m = Matrix()
        m.setTranslate(0f, 5.5f)
        p.transform(m)
        return p
    }

    private fun blossom(): Path {
        val all = Path()
        val base = petalShape()
        val m = Matrix()
        for (i in 0 until 5) {
            val q = Path(base)
            m.setRotate(i * 72f, 12f, 12f)
            q.transform(m)
            all.addPath(q)
        }
        return all
    }

    private fun skull(): Path {
        val p = Path()
        p.addCircle(12f, 10.5f, 8.5f, Path.Direction.CW)
        p.addRoundRect(7.8f, 15f, 16.2f, 21.5f, 1.5f, 1.5f, Path.Direction.CW)
        p.addCircle(8.8f, 11f, 2.2f, Path.Direction.CCW)
        p.addCircle(15.2f, 11f, 2.2f, Path.Direction.CCW)
        p.addRect(10.2f, 17f, 10.9f, 21.5f, Path.Direction.CCW)
        p.addRect(13.1f, 17f, 13.8f, 21.5f, Path.Direction.CCW)
        return p
    }

    private fun ghost(): Path {
        val p = Path()
        p.moveTo(4f, 21f)
        p.lineTo(4f, 10f)
        p.cubicTo(4f, 5.6f, 7.6f, 2f, 12f, 2f)
        p.cubicTo(16.4f, 2f, 20f, 5.6f, 20f, 10f)
        p.lineTo(20f, 21f)
        p.lineTo(17.33f, 19f)
        p.lineTo(14.67f, 21f)
        p.lineTo(12f, 19f)
        p.lineTo(9.33f, 21f)
        p.lineTo(6.67f, 19f)
        p.close()
        p.addCircle(9f, 10f, 1.7f, Path.Direction.CCW)
        p.addCircle(15f, 10f, 1.7f, Path.Direction.CCW)
        p.fillType = Path.FillType.EVEN_ODD
        return p
    }
}

/** Drawable de um ícone da biblioteca. Pode ser usado em TextView, ImageView ou fundos. */
class IconDrawable(iconName: String, iconColor: Int, private val filled: Boolean = true) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    var name: String = iconName
        set(v) {
            field = v
            invalidateSelf()
        }
    var color: Int = iconColor
        set(v) {
            field = v
            invalidateSelf()
        }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.width() <= 0) return
        paint.color = color
        if (filled) {
            paint.style = Paint.Style.FILL
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.9f
            paint.strokeJoin = Paint.Join.ROUND
        }
        canvas.save()
        canvas.translate(b.left.toFloat(), b.top.toFloat())
        val s = b.width() / 24f
        canvas.scale(s, s)
        canvas.drawPath(Icons.path(name), paint)
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Suppress("DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

fun Context.iconDrawable(name: String, color: Int, px: Int, filled: Boolean = true): IconDrawable {
    val d = IconDrawable(name, color, filled)
    d.setBounds(0, 0, px, px)
    return d
}

/** View que mostra um ícone. */
class IconView(ctx: Context, name: String, tint: Int, private val sizeDp: Int, filled: Boolean = true) : View(ctx) {
    private val d = IconDrawable(name, tint, filled)

    var tint: Int
        get() = d.color
        set(v) {
            d.color = v
            invalidate()
        }

    fun setIcon(name: String) {
        d.name = name
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val s = dp(sizeDp)
        setMeasuredDimension(s, s)
    }

    override fun onDraw(canvas: Canvas) {
        d.setBounds(0, 0, width, height)
        d.draw(canvas)
    }
}

/** Nota em 5 corações com meio coração (0 a 10). */
class RatingView(ctx: Context, private val heartDp: Int, private val editable: Boolean) : View(ctx) {
    var score: Int = 0
        set(v) {
            field = v.coerceIn(0, 10)
            invalidate()
        }
    var color: Int = Palette.pink
        set(v) {
            field = v
            invalidate()
        }
    var onChange: ((Int) -> Unit)? = null

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun hs(): Float = dp(heartDp).toFloat()
    private fun gap(): Float = hs() * 0.22f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = hs()
        setMeasuredDimension((5 * h + 4 * gap()).toInt(), h.toInt())
    }

    override fun onDraw(c: Canvas) {
        val h = hs()
        val g = gap()
        val path = Icons.path("heart")
        line.style = Paint.Style.STROKE
        line.strokeWidth = 1.8f
        line.color = color
        fill.style = Paint.Style.FILL
        for (i in 0 until 5) {
            c.save()
            c.translate(i * (h + g), 0f)
            c.scale(h / 24f, h / 24f)
            val full = score >= (i + 1) * 2
            val half = !full && score == i * 2 + 1
            if (full) {
                fill.color = color
                c.drawPath(path, fill)
            } else {
                fill.color = (color and 0x00FFFFFF) or 0x30000000
                c.drawPath(path, fill)
                if (half) {
                    c.save()
                    c.clipRect(0f, 0f, 12f, 24f)
                    fill.color = color
                    c.drawPath(path, fill)
                    c.restore()
                }
                c.drawPath(path, line)
            }
            c.restore()
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (!editable) return false
        if (e.action == MotionEvent.ACTION_UP) {
            val h = hs()
            val g = gap()
            val idx = (e.x / (h + g)).toInt().coerceIn(0, 4)
            val within = e.x - idx * (h + g)
            val v = idx * 2 + (if (within < h / 2f) 1 else 2)
            val ns = if (v == score) 0 else v
            score = ns
            onChange?.invoke(ns)
        }
        return e.action == MotionEvent.ACTION_DOWN || e.action == MotionEvent.ACTION_UP
    }
}

/** Pétalas e símbolos caindo suavemente, desenhados com os ícones próprios. Muda de tema com fade e solta explosões de confete. */
class PetalsView(ctx: Context, icons: List<String>, tint: Int, private val count: Int = 14) : View(ctx) {
    private class P(
        var x: Float, var y: Float, var vy: Float, var size: Float,
        var phase: Float, var rot: Float, var vr: Float, var e: Int, var a: Int
    )

    private class B(
        var x: Float, var y: Float, var vx: Float, var vy: Float, var size: Float,
        var rot: Float, var vr: Float, var life: Float, var icon: String, var color: Int
    )

    private var icons: List<String> = icons
    private var tints: List<Int> = listOf(tint)
    private val ps = ArrayList<P>()
    private val bursts = ArrayList<B>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rnd = java.util.Random()
    private var last = 0L
    private var master = 1f
    private var fadeAnim: android.animation.ValueAnimator? = null
    private var themeKey = ""

    private fun spawn(anywhere: Boolean): P {
        val size = (dp(12) + rnd.nextInt(dp(14))).toFloat()
        return P(
            rnd.nextFloat() * width,
            if (anywhere) rnd.nextFloat() * height else -size,
            dp(24) + rnd.nextFloat() * dp(34),
            size,
            rnd.nextFloat() * 6.28f,
            rnd.nextFloat() * 360f,
            (rnd.nextFloat() - 0.5f) * 80f,
            rnd.nextInt(icons.size),
            70 + rnd.nextInt(80)
        )
    }

    /** Troca o tema das pétalas com um fade suave (some, troca, volta). */
    fun setTheme(newIcons: List<String>, newTints: List<Int>) {
        val key = newIcons.joinToString(",") + "|" + newTints.joinToString(",")
        if (key == themeKey) return
        val first = themeKey.isEmpty()
        themeKey = key
        if (first || width == 0) {
            icons = newIcons
            tints = newTints
            for (p in ps) p.e = rnd.nextInt(icons.size)
            return
        }
        fadeAnim?.cancel()
        val out = android.animation.ValueAnimator.ofFloat(master, 0f)
        out.duration = 180
        out.addUpdateListener { master = it.animatedValue as Float }
        out.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                icons = newIcons
                tints = newTints
                for (p in ps) p.e = rnd.nextInt(icons.size)
                val inn = android.animation.ValueAnimator.ofFloat(0f, 1f)
                inn.duration = 420
                inn.addUpdateListener { master = it.animatedValue as Float }
                fadeAnim = inn
                inn.start()
            }
        })
        fadeAnim = out
        out.start()
        invalidate()
    }

    /** Explosão de símbolos a partir de um ponto (coordenadas desta view). */
    fun burst(cx: Float, cy: Float, bIcons: List<String>, bColors: List<Int>, n: Int = 18) {
        for (i in 0 until n) {
            val ang = rnd.nextFloat() * 6.2831f
            val sp = dp(90) + rnd.nextFloat() * dp(190)
            bursts.add(
                B(
                    cx, cy, (kotlin.math.cos(ang.toDouble()) * sp).toFloat(), (kotlin.math.sin(ang.toDouble()) * sp).toFloat() - dp(90),
                    (dp(10) + rnd.nextInt(dp(12))).toFloat(), rnd.nextFloat() * 360f, (rnd.nextFloat() - 0.5f) * 520f, 1f,
                    bIcons[rnd.nextInt(bIcons.size)], bColors[rnd.nextInt(bColors.size)]
                )
            )
        }
        invalidate()
    }

    /** Explosão a partir do centro de outra view (qualquer view na mesma tela). */
    fun burstFrom(v: View, bIcons: List<String>, bColors: List<Int>, n: Int = 18) {
        val loc = IntArray(2)
        val mine = IntArray(2)
        v.getLocationInWindow(loc)
        getLocationInWindow(mine)
        burst(loc[0] - mine[0] + v.width / 2f, loc[1] - mine[1] + v.height / 2f, bIcons, bColors, n)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        ps.clear()
        if (w > 0 && h > 0) {
            for (i in 0 until count) ps.add(spawn(true))
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        last = 0L
    }

    override fun onDraw(c: Canvas) {
        val now = System.nanoTime()
        val dt = if (last == 0L) 0.016f else ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
        last = now
        paint.style = Paint.Style.FILL
        for (i in ps.indices) {
            var p = ps[i]
            p.y += p.vy * dt
            p.phase += dt * 1.3f
            p.rot += p.vr * dt
            if (p.y > height + p.size) {
                p = spawn(false)
                ps[i] = p
            }
            val sx = p.x + (sin(p.phase.toDouble()) * dp(16)).toFloat()
            paint.color = tints[p.e % tints.size]
            paint.alpha = (p.a * master).toInt().coerceIn(0, 255)
            c.save()
            c.translate(sx, p.y)
            c.rotate(p.rot)
            val s = p.size / 24f
            c.scale(s, s)
            c.translate(-12f, -12f)
            c.drawPath(Icons.path(icons[p.e % icons.size]), paint)
            c.restore()
        }
        val it = bursts.iterator()
        while (it.hasNext()) {
            val b = it.next()
            b.life -= dt * 0.95f
            if (b.life <= 0f) {
                it.remove()
                continue
            }
            b.vy += dp(260) * dt
            b.x += b.vx * dt
            b.y += b.vy * dt
            b.rot += b.vr * dt
            paint.color = b.color
            paint.alpha = (255 * b.life.coerceIn(0f, 1f)).toInt()
            c.save()
            c.translate(b.x, b.y)
            c.rotate(b.rot)
            val s = b.size / 24f * (0.6f + 0.4f * b.life)
            c.scale(s, s)
            c.translate(-12f, -12f)
            c.drawPath(Icons.path(b.icon), paint)
            c.restore()
        }
        postInvalidateOnAnimation()
    }
}

/** Gráfico em anel, com fatias que crescem. */
class DonutView(ctx: Context) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = android.graphics.RectF()
    private var values: List<Float> = emptyList()
    private var colors: List<Int> = emptyList()
    private var grow = 1f
    var centerText: String = ""
    var centerSub: String = ""
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)

    fun set(v: List<Float>, c: List<Int>, big: String, sub: String) {
        values = v
        colors = c
        centerText = big
        centerSub = sub
        grow = 0f
        val a = android.animation.ValueAnimator.ofFloat(0f, 1f)
        a.duration = 900
        a.startDelay = 150
        a.interpolator = android.view.animation.DecelerateInterpolator(1.6f)
        a.addUpdateListener {
            grow = it.animatedValue as Float
            invalidate()
        }
        a.start()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val s = dp(132)
        setMeasuredDimension(s, s)
    }

    override fun onDraw(c: Canvas) {
        val stroke = dp(16).toFloat()
        val pad = stroke / 2f + dp(2)
        rect.set(pad, pad, width - pad, height - pad)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.parseColor("#F6DCE6")
        c.drawArc(rect, 0f, 360f, false, paint)
        val total = values.sum()
        if (total > 0f) {
            var start = -90f
            for (i in values.indices) {
                if (values[i] <= 0f) continue
                val sweep = values[i] / total * 360f * grow
                val gap = if (values.count { it > 0f } > 1) 6f else 0f
                paint.color = colors[i]
                if (sweep > gap) c.drawArc(rect, start + gap / 2f, sweep - gap, false, paint)
                start += values[i] / total * 360f * grow
            }
        }
        tp.textAlign = Paint.Align.CENTER
        tp.color = Palette.pinkDark
        tp.typeface = android.graphics.Typeface.create("casual", android.graphics.Typeface.BOLD)
        tp.textSize = dp(26).toFloat()
        c.drawText(centerText, width / 2f, height / 2f + dp(4), tp)
        tp.color = Palette.muted
        tp.typeface = android.graphics.Typeface.DEFAULT
        tp.textSize = dp(11).toFloat()
        c.drawText(centerSub, width / 2f, height / 2f + dp(20), tp)
    }
}

/** Arte decorativa do cabeçalho: flores de cerejeira espalhadas. */
class HeaderArt(ctx: Context) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private data class B(val fx: Float, val fy: Float, val sizeDp: Int, val rot: Float, val color: Int)

    private val items = listOf(
        B(0.88f, 0.34f, 46, 15f, Color.parseColor("#80FFFFFF")),
        B(0.74f, 0.80f, 26, 40f, Color.parseColor("#66FF8FB7")),
        B(0.97f, 0.88f, 30, 0f, Color.parseColor("#80FFFFFF")),
        B(0.62f, 0.22f, 16, 25f, Color.parseColor("#66FF8FB7")),
        B(0.50f, 0.85f, 12, 10f, Color.parseColor("#80FFFFFF")),
        B(0.04f, 0.18f, 20, 30f, Color.parseColor("#66FFFFFF"))
    )

    // Não pede altura própria: assim o cabeçalho tem só a altura do texto
    // e a arte se ajusta a ele (antes ocupava a tela inteira).
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = android.view.View.MeasureSpec.getSize(widthMeasureSpec)
        val hMode = android.view.View.MeasureSpec.getMode(heightMeasureSpec)
        val h = if (hMode == android.view.View.MeasureSpec.EXACTLY) {
            android.view.View.MeasureSpec.getSize(heightMeasureSpec)
        } else {
            0
        }
        setMeasuredDimension(w, h)
    }

    override fun onDraw(c: Canvas) {
        val path = Icons.path("blossom")
        paint.style = Paint.Style.FILL
        for (b in items) {
            val s = dp(b.sizeDp) / 24f
            paint.color = b.color
            c.save()
            c.translate(width * b.fx, height * b.fy)
            c.rotate(b.rot)
            c.scale(s, s)
            c.translate(-12f, -12f)
            c.drawPath(path, paint)
            c.restore()
        }
    }
}
