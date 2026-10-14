package com.doramabloom.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.View
import kotlin.math.sin

/** Os tons de um metal: claro, médio, escuro e o contorno. */
class MetalTone(val hi: Int, val mid: Int, val lo: Int, val edge: Int)

/** Ouro, prata e bronze do pódio. */
object Metal {
    private val GOLD = MetalTone(Color.parseColor("#FFEDB3"), Color.parseColor("#F5B83D"), Color.parseColor("#B9791A"), Color.parseColor("#8A5A0E"))
    private val SILVER = MetalTone(Color.parseColor("#F6F9FD"), Color.parseColor("#BFC9D8"), Color.parseColor("#7F8CA1"), Color.parseColor("#5C687B"))
    private val BRONZE = MetalTone(Color.parseColor("#F8CBA4"), Color.parseColor("#D48B5C"), Color.parseColor("#94552B"), Color.parseColor("#6E3C1B"))

    fun tone(rank: Int): MetalTone = when (rank) {
        2 -> SILVER
        3 -> BRONZE
        else -> GOLD
    }

    /** Medalhinha redonda com brilho de metal (o contorno branco é opcional). */
    fun medal(rank: Int, strokePx: Int): GradientDrawable {
        val t = tone(rank)
        val d = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(t.hi, t.mid, t.lo))
        d.shape = GradientDrawable.OVAL
        if (strokePx > 0) d.setStroke(strokePx, Color.WHITE)
        return d
    }
}

/** Relógio das animações (em segundos). */
internal fun clockSec(): Float = (SystemClock.uptimeMillis() % 3600000L) / 1000f

/** Estrelinha de 4 pontas em coordenadas -1..1. */
internal fun starPath(): Path {
    val s = Path()
    s.moveTo(0f, -1f)
    s.lineTo(0.18f, -0.18f)
    s.lineTo(1f, 0f)
    s.lineTo(0.18f, 0.18f)
    s.lineTo(0f, 1f)
    s.lineTo(-0.18f, 0.18f)
    s.lineTo(-1f, 0f)
    s.lineTo(-0.18f, -0.18f)
    s.close()
    return s
}

/**
 * Coroa de metal com joias, brilho que passa e balanço leve (só o 1º lugar usa). O desenho tem folga nas bordas,
 * então nada é cortado. Para "usar" a coroa, sobreponha a base dela ao topo da foto.
 */
class CrownView(ctx: Context, rank: Int, private val gem: Int) : View(ctx) {
    private val tone = Metal.tone(rank)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val body = Path()
    private val para = Path()
    private val band = RectF()
    private var x0 = 0f
    private var aw = 0f
    private var yTop = 0f
    private var ah = 0f
    private var bodyShader: LinearGradient? = null
    private var bandShader: LinearGradient? = null

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        x0 = w * 0.07f
        aw = w * 0.86f
        yTop = h * 0.16f
        ah = h * 0.78f
        body.reset()
        body.moveTo(x0 + 0.05f * aw, yTop + ah)
        body.lineTo(x0, yTop + 0.22f * ah)
        body.lineTo(x0 + 0.27f * aw, yTop + 0.55f * ah)
        body.lineTo(x0 + 0.5f * aw, yTop)
        body.lineTo(x0 + 0.73f * aw, yTop + 0.55f * ah)
        body.lineTo(x0 + aw, yTop + 0.22f * ah)
        body.lineTo(x0 + 0.95f * aw, yTop + ah)
        body.close()
        band.set(x0 + 0.03f * aw, yTop + 0.8f * ah, x0 + 0.97f * aw, yTop + ah)
        bodyShader = LinearGradient(
            0f, yTop, 0f, yTop + ah, intArrayOf(tone.hi, tone.mid, tone.lo),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
        )
        bandShader = LinearGradient(0f, band.top, 0f, band.bottom, tone.mid, tone.lo, Shader.TileMode.CLAMP)
    }

    override fun onDraw(c: Canvas) {
        if (width == 0 || height == 0) return
        val d = resources.displayMetrics.density
        val t = clockSec()
        c.save()
        c.rotate(sin(t * 1.7f) * 2.5f, width / 2f, height.toFloat())

        p.style = Paint.Style.FILL
        p.shader = bodyShader
        c.drawPath(body, p)
        p.shader = null

        // brilho que passa
        c.save()
        c.clipPath(body)
        val ph = (t * 0.4f) % 1.8f
        val sx = x0 + aw * (ph - 0.4f)
        para.reset()
        para.moveTo(sx, yTop)
        para.lineTo(sx + 0.16f * aw, yTop)
        para.lineTo(sx + 0.02f * aw, yTop + ah)
        para.lineTo(sx - 0.14f * aw, yTop + ah)
        para.close()
        p.color = Color.argb(130, 255, 255, 255)
        c.drawPath(para, p)
        c.restore()

        // frisos do metal
        p.style = Paint.Style.STROKE
        p.strokeWidth = 0.8f * d
        p.color = Color.argb(80, Color.red(tone.edge), Color.green(tone.edge), Color.blue(tone.edge))
        c.drawLine(x0 + 0.27f * aw, yTop + 0.55f * ah, x0 + 0.31f * aw, yTop + 0.82f * ah, p)
        c.drawLine(x0 + 0.73f * aw, yTop + 0.55f * ah, x0 + 0.69f * aw, yTop + 0.82f * ah, p)
        c.drawLine(x0 + 0.5f * aw, yTop + 0.22f * ah, x0 + 0.5f * aw, yTop + 0.8f * ah, p)

        // contorno
        p.strokeWidth = 1.2f * d
        p.strokeJoin = Paint.Join.ROUND
        p.color = tone.edge
        c.drawPath(body, p)

        // faixa de baixo
        p.style = Paint.Style.FILL
        p.shader = bandShader
        c.drawRoundRect(band, ah * 0.08f, ah * 0.08f, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f * d
        p.color = tone.edge
        c.drawRoundRect(band, ah * 0.08f, ah * 0.08f, p)

        // joias nas pontas
        val gr = ah * 0.085f
        val tipsX = floatArrayOf(x0, x0 + 0.5f * aw, x0 + aw)
        val tipsY = floatArrayOf(yTop + 0.22f * ah, yTop, yTop + 0.22f * ah)
        for (i in 0 until 3) {
            p.style = Paint.Style.FILL
            p.color = if (i == 1) gem else Color.WHITE
            c.drawCircle(tipsX[i], tipsY[i], gr, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 0.9f * d
            p.color = tone.edge
            c.drawCircle(tipsX[i], tipsY[i], gr, p)
        }
        // joias na faixa
        val by = (band.top + band.bottom) / 2f
        val bx = floatArrayOf(x0 + 0.25f * aw, x0 + 0.5f * aw, x0 + 0.75f * aw)
        for (i in 0 until 3) {
            val r = if (i == 1) ah * 0.075f else ah * 0.05f
            p.style = Paint.Style.FILL
            p.color = if (i == 1) gem else Color.argb(235, 255, 255, 255)
            c.drawCircle(bx[i], by, r, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 0.7f * d
            p.color = tone.edge
            c.drawCircle(bx[i], by, r, p)
        }
        // brilho na joia do meio
        p.style = Paint.Style.FILL
        p.color = Color.argb(200, 255, 255, 255)
        c.drawCircle(tipsX[1] - gr * 0.3f, tipsY[1] - gr * 0.3f, gr * 0.28f, p)
        c.restore()

        if (isAttachedToWindow) postInvalidateDelayed(45)
    }
}

/**
 * Plataforma de pódio em metal (ouro, prata ou bronze): topo mais claro, número gravado e um
 * brilho que passa de tempos em tempos. Várias plataformas lado a lado se encostam.
 */
class PedestalView(ctx: Context, private val rank: Int) : View(ctx) {
    private val tone = Metal.tone(rank)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shape = Path()
    private val para = Path()
    private val rect = RectF()

    override fun onDraw(c: Canvas) {
        if (width == 0 || height == 0) return
        val d = resources.displayMetrics.density
        val t = clockSec()
        val w = width.toFloat()
        val h = height.toFloat()
        val r = 6f * d
        val lip = 5f * d

        // corpo
        rect.set(0f, 0f, w, h)
        shape.reset()
        shape.addRoundRect(rect, floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f), Path.Direction.CW)
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, 0f, 0f, h, intArrayOf(tone.hi, tone.mid, tone.lo), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        c.drawPath(shape, p)
        p.shader = null

        c.save()
        c.clipPath(shape)
        // tampa de cima (mais clara)
        p.color = Color.argb(150, 255, 255, 255)
        c.drawRect(0f, 0f, w, lip, p)
        p.color = Color.argb(70, Color.red(tone.edge), Color.green(tone.edge), Color.blue(tone.edge))
        c.drawRect(0f, lip, w, lip + 1.2f * d, p)
        // quina de luz na esquerda e sombra na direita
        p.color = Color.argb(90, 255, 255, 255)
        c.drawRect(0f, lip, 2f * d, h, p)
        p.color = Color.argb(60, 0, 0, 0)
        c.drawRect(w - 2.5f * d, lip, w, h, p)
        // frisos
        p.color = Color.argb(45, 0, 0, 0)
        c.drawRect(5f * d, h - 7f * d, w - 5f * d, h - 6f * d, p)
        c.drawRect(5f * d, h - 4.5f * d, w - 5f * d, h - 3.5f * d, p)

        // número gravado
        p.typeface = Typeface.create("casual", Typeface.BOLD)
        p.textAlign = Paint.Align.CENTER
        p.textSize = (h - lip) * 0.62f
        val base = lip + (h - lip) * 0.5f + p.textSize * 0.33f - 2f * d
        p.color = Color.argb(150, 255, 255, 255)
        c.drawText(rank.toString(), w / 2f, base + 1.3f * d, p)
        p.color = tone.edge
        c.drawText(rank.toString(), w / 2f, base, p)

        // brilho passando
        val ph = (t * 0.33f + rank * 0.37f) % 2.2f
        val sx = -0.3f * w + w * 1.6f * (ph / 2.2f) * 1.5f
        para.reset()
        para.moveTo(sx, 0f)
        para.lineTo(sx + 0.22f * w, 0f)
        para.lineTo(sx + 0.02f * w, h)
        para.lineTo(sx - 0.2f * w, h)
        para.close()
        p.color = Color.argb(95, 255, 255, 255)
        c.drawPath(para, p)
        c.restore()

        // contorno
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f * d
        p.color = Color.argb(150, Color.red(tone.edge), Color.green(tone.edge), Color.blue(tone.edge))
        c.drawPath(shape, p)

        if (isAttachedToWindow) postInvalidateDelayed(50)
    }
}
