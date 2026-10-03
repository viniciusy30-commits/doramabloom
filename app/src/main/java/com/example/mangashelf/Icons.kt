package com.example.mangashelf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ReplacementSpan
import android.view.View
import android.widget.TextView
import androidx.core.graphics.PathParser

/**
 * Ícones próprios do app, desenhados por código (traço arredondado, grade 24x24).
 * Nenhum emoji: tudo vem daqui e acompanha as cores do tema.
 */
enum class Ic(val d: String, val solid: Boolean = false, val w: Float = 2f) {
    Home("M3,10.5 L12,3 l9,7.5 V20 a1,1 0 0 1 -1,1 h-5 v-6 H9 v6 H4 a1,1 0 0 1 -1,-1 z"),
    Library("M4,4 v16 M8,8 v12 M12,6 v14 M16,6 l4,14"),
    Plus("M12,5 v14 M5,12 h14"),
    Minus("M5,12 h14"),
    Chart("M5,20 V11 M12,20 V4 M19,20 v-6"),
    Sliders("M4,7 h9 M17,7 h3 M4,17 h3 M11,17 h9 M13,7 a2,2 0 1 0 4,0 a2,2 0 1 0 -4,0 M7,17 a2,2 0 1 0 4,0 a2,2 0 1 0 -4,0"),
    Search("M11,4 a7,7 0 1 0 0,14 a7,7 0 1 0 0,-14 M21,21 l-4.35,-4.35"),
    Globe("M12,3 a9,9 0 1 0 0,18 a9,9 0 1 0 0,-18 M3,12 h18 M12,3 a14,14 0 0 1 0,18 a14,14 0 0 1 0,-18"),
    Help("M12,3 a9,9 0 1 0 0,18 a9,9 0 1 0 0,-18 M9.1,9 a3,3 0 0 1 5.8,1 c0,2 -3,3 -3,3 M12,17 h0.01"),
    BookOpen("M2,4 h6 a4,4 0 0 1 4,4 v13 a3,3 0 0 0 -3,-3 H2 z M22,4 h-6 a4,4 0 0 0 -4,4 v13 a3,3 0 0 1 3,-3 h7 z"),
    Bookmark("M6,3 h12 a1,1 0 0 1 1,1 v17 l-7,-4 l-7,4 V4 a1,1 0 0 1 1,-1 z"),
    CheckCircle("M12,3 a9,9 0 1 0 0,18 a9,9 0 1 0 0,-18 M8.5,12.5 l2.5,2.5 l4.5,-5"),
    Check("M5,13 l4,4 L19,7"),
    Pause("M9,5 v14 M15,5 v14"),
    Heart("M19,14 c1.49,-1.46 3,-3.21 3,-5.5 A5.5,5.5 0 0 0 16.5,3 c-1.76,0 -3,0.5 -4.5,2 c-1.5,-1.5 -2.74,-2 -4.5,-2 A5.5,5.5 0 0 0 2,8.5 c0,2.3 1.5,4.05 3,5.5 l7,7 z"),
    HeartSolid("M19,14 c1.49,-1.46 3,-3.21 3,-5.5 A5.5,5.5 0 0 0 16.5,3 c-1.76,0 -3,0.5 -4.5,2 c-1.5,-1.5 -2.74,-2 -4.5,-2 A5.5,5.5 0 0 0 2,8.5 c0,2.3 1.5,4.05 3,5.5 l7,7 z", true, 1.2f),
    PlaySolid("M7,4.5 L19,12 L7,19.5 z", true, 2.2f),
    Star("M12,3 L14.41,9.48 L21.32,9.77 L15.9,14.07 L17.76,20.73 L12,16.9 L6.24,20.73 L8.1,14.07 L2.68,9.77 L9.59,9.48 Z"),
    StarSolid("M12,3 L14.41,9.48 L21.32,9.77 L15.9,14.07 L17.76,20.73 L12,16.9 L6.24,20.73 L8.1,14.07 L2.68,9.77 L9.59,9.48 Z", true, 1.6f),
    ArrowLeft("M19,12 H5 M12,19 l-7,-7 l7,-7"),
    ArrowRight("M5,12 h14 M13,6 l6,6 l-6,6"),
    ChevronLeft("M15,18 l-6,-6 l6,-6"),
    ChevronRight("M9,18 l6,-6 l-6,-6"),
    ChevronDown("M6,9 l6,6 l6,-6"),
    Close("M18,6 L6,18 M6,6 l12,12"),
    More("M12,5 h0.01 M12,12 h0.01 M12,19 h0.01", false, 3f),
    Refresh("M21,12 a9,9 0 1 1 -9,-9 c2.52,0 4.93,1 6.74,2.74 L21,8 M21,3 v5 h-5"),
    External("M15,3 h6 v6 M10,14 L21,3 M18,13 v6 a2,2 0 0 1 -2,2 H5 a2,2 0 0 1 -2,-2 V8 a2,2 0 0 1 2,-2 h6"),
    ZoomIn("M11,3 a8,8 0 1 0 0,16 a8,8 0 1 0 0,-16 M21,21 l-4.35,-4.35 M11,8 v6 M8,11 h6"),
    ZoomOut("M11,3 a8,8 0 1 0 0,16 a8,8 0 1 0 0,-16 M21,21 l-4.35,-4.35 M8,11 h6"),
    Maximize("M8,3 H5 a2,2 0 0 0 -2,2 v3 M21,8 V5 a2,2 0 0 0 -2,-2 h-3 M3,16 v3 a2,2 0 0 0 2,2 h3 M16,21 h3 a2,2 0 0 0 2,-2 v-3"),
    Minimize("M8,3 v3 a2,2 0 0 1 -2,2 H3 M21,8 h-3 a2,2 0 0 1 -2,-2 V3 M3,16 h3 a2,2 0 0 1 2,2 v3 M16,21 v-3 a2,2 0 0 1 2,-2 h3"),
    Clock("M12,3 a9,9 0 1 0 0,18 a9,9 0 1 0 0,-18 M12,7 v5 l3,2"),
    Copy("M10,8 h10 a2,2 0 0 1 2,2 v10 a2,2 0 0 1 -2,2 H10 a2,2 0 0 1 -2,-2 V10 a2,2 0 0 1 2,-2 z M4,16 a2,2 0 0 1 -2,-2 V4 a2,2 0 0 1 2,-2 h10 a2,2 0 0 1 2,2"),
    Trash("M3,6 h18 M19,6 l-1,14 a2,2 0 0 1 -2,2 H8 a2,2 0 0 1 -2,-2 L5,6 M8,6 V4 a2,2 0 0 1 2,-2 h4 a2,2 0 0 1 2,2 v2 M10,11 v6 M14,11 v6"),
    Pen("M12,20 h9 M16.5,3.5 a2.12,2.12 0 0 1 3,3 L7,19 l-4,1 l1,-4 z"),
    Chain("M10,13 a5,5 0 0 0 7.54,0.54 l3,-3 a5,5 0 0 0 -7.07,-7.07 l-1.72,1.71 M14,11 a5,5 0 0 0 -7.54,-0.54 l-3,3 a5,5 0 0 0 7.07,7.07 l1.71,-1.71"),
    Rows("M8,6 h13 M8,12 h13 M8,18 h13 M3,6 h0.01 M3,12 h0.01 M3,18 h0.01", false, 2.4f),
    Picture("M5,3 h14 a2,2 0 0 1 2,2 v14 a2,2 0 0 1 -2,2 H5 a2,2 0 0 1 -2,-2 V5 a2,2 0 0 1 2,-2 z M7,9 a2,2 0 1 0 4,0 a2,2 0 1 0 -4,0 M21,15 l-5,-5 L5,21"),
    Download("M21,15 v4 a2,2 0 0 1 -2,2 H5 a2,2 0 0 1 -2,-2 v-4 M7,10 l5,5 l5,-5 M12,15 V3"),
    Upload("M21,15 v4 a2,2 0 0 1 -2,2 H5 a2,2 0 0 1 -2,-2 v-4 M17,8 l-5,-5 l-5,5 M12,3 v12"),
    Sort("M7,4 v16 M7,20 l-3,-3 M7,20 l3,-3 M17,20 V4 M17,4 l-3,3 M17,4 l3,3"),
    Person("M16,8 a4,4 0 1 0 -8,0 a4,4 0 1 0 8,0 M4,21 v-1 a6,6 0 0 1 6,-6 h4 a6,6 0 0 1 6,6 v1"),
    Bulb("M9,18 h6 M10,22 h4 M12,2 a7,7 0 0 0 -4,12.7 c0.6,0.5 1,1.3 1,2.3 h6 c0,-1 0.4,-1.8 1,-2.3 A7,7 0 0 0 12,2 z"),
    Layers("M12,2 L2,7 l10,5 l10,-5 z M2,17 l10,5 l10,-5 M2,12 l10,5 l10,-5"),
    Hash("M4,9 h16 M4,15 h16 M10,3 L8,21 M16,3 l-2,18"),
    Moon("M21,12.8 A9,9 0 1 1 11.2,3 a7,7 0 0 0 9.8,9.8 z")
}

object Icons {
    private val cache = HashMap<Ic, Path>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun path(ic: Ic): Path = cache.getOrPut(ic) {
        try {
            PathParser.createPathFromPathData(ic.d)
        } catch (e: Exception) {
            Path()
        }
    }

    /** Desenha o ícone num quadrado de [size] px com o canto em ([left], [top]). */
    fun draw(c: Canvas, ic: Ic, left: Float, top: Float, size: Float, color: Int) {
        val s = size / 24f
        paint.color = color
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.style = if (ic.solid) Paint.Style.FILL_AND_STROKE else Paint.Style.STROKE
        paint.strokeWidth = ic.w
        c.save()
        c.translate(left, top)
        c.scale(s, s)
        c.drawPath(path(ic), paint)
        c.restore()
    }
}

/** View quadrada que desenha um ícone centralizado. */
class IconView(ctx: Context, ic: Ic, color: Int, val sizeDp: Int = 22) : View(ctx) {
    var ic: Ic = ic
        set(v) {
            field = v
            invalidate()
        }
    var color: Int = color
        set(v) {
            field = v
            invalidate()
        }

    override fun onMeasure(w: Int, h: Int) {
        val s = (sizeDp * resources.displayMetrics.density + 0.5f).toInt()
        setMeasuredDimension(resolveSize(s, w), resolveSize(s, h))
    }

    override fun onDraw(canvas: Canvas) {
        val want = sizeDp * resources.displayMetrics.density
        val size = Math.min(want, Math.min(width, height).toFloat())
        Icons.draw(canvas, ic, (width - size) / 2f, (height - size) / 2f, size, color)
    }
}

/** Ícone dentro de um texto (acompanha a cor e a posição do texto). */
class IconSpan(
    private val ic: Ic,
    private val sizePx: Int,
    private val gapPx: Int,
    private val trailing: Boolean
) : ReplacementSpan() {
    override fun getSize(paint: Paint, text: CharSequence?, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int =
        sizePx + gapPx

    override fun draw(
        canvas: Canvas, text: CharSequence?, start: Int, end: Int,
        x: Float, top: Int, y: Int, bottom: Int, paint: Paint
    ) {
        val fm = paint.fontMetricsInt
        val cy = y + (fm.ascent + fm.descent) / 2f
        val left = if (trailing) x + gapPx else x
        Icons.draw(canvas, ic, left, cy - sizePx / 2f, sizePx.toFloat(), paint.color)
    }
}

/** Coloca texto com ícone antes (ou depois, se [trailing]). Sem ícone, vira texto comum. */
fun TextView.setIconText(ic: Ic?, label: CharSequence, trailing: Boolean = false, iconDp: Int = 18) {
    if (ic == null) {
        text = label
        return
    }
    val d = resources.displayMetrics.density
    val sizePx = (iconDp * d + 0.5f).toInt()
    val gapPx = (7 * d + 0.5f).toInt()
    val sb = SpannableStringBuilder()
    if (!trailing) {
        sb.append(" ")
        sb.setSpan(IconSpan(ic, sizePx, gapPx, false), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.append(label)
    } else {
        sb.append(label)
        val s = sb.length
        sb.append(" ")
        sb.setSpan(IconSpan(ic, sizePx, gapPx, true), s, s + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    text = sb
}
