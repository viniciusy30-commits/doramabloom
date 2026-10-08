package com.doramabloom.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.View
import kotlin.math.cos
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
 * Coroa de metal com joias, brilho que passa e balanço leve. O desenho tem folga nas bordas,
 * então nada é cortado. Para "usar" a coroa, sobreponha a base dela ao topo da foto.
 */
class CrownView(ctx: Context, rank: Int, private val gem: Int) : View(ctx) {
    private val tone = Metal.tone(rank)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val body = Path()
    private val para = Path()
    private val star = starPath()
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

    private fun spark(c: Canvas, x: Float, y: Float, size: Float, a: Float) {
        p.style = Paint.Style.FILL
        p.shader = null
        p.color = Color.argb((255 * a).toInt().coerceIn(0, 255), 255, 255, 255)
        c.save()
        c.translate(x, y)
        c.scale(size, size)
        c.drawPath(star, p)
        c.restore()
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

        // brilhinhos em volta
        val a1 = 0.5f + 0.5f * sin(t * 3.1f)
        val a2 = 0.5f + 0.5f * sin(t * 2.3f + 2f)
        spark(c, width * 0.06f, height * 0.16f, 2.6f * d * (0.6f + 0.4f * a1), a1)
        spark(c, width * 0.95f, height * 0.3f, 2.2f * d * (0.6f + 0.4f * a2), a2)

        if (isAttachedToWindow) postInvalidateDelayed(45)
    }
}

/**
 * Aura do 1º lugar (e das medalhas do perfil): brilho, raios girando, fio de pérolas que cintilam,
 * coroa de louros nos dois lados e estrelinhas orbitando. Fica ATRÁS da foto; use um quadrado
 * de cerca de 1,6 vezes o tamanho da foto, com o centro igual ao da foto.
 */
class HeroAuraView(ctx: Context, private val avatarDp: Int, rank: Int = 1) : View(ctx) {
    private val tone = Metal.tone(rank)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val star = starPath()
    private val wedge = Path()
    private val oval = RectF()
    private val arc = RectF()
    private var glow: RadialGradient? = null
    private var rays: RadialGradient? = null
    private var sweep: SweepGradient? = null

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        val cx = w / 2f
        val cy = h / 2f
        val half = minOf(w, h) / 2f
        val ar = avatarDp * resources.displayMetrics.density / 2f
        val m = tone.mid
        glow = RadialGradient(
            cx, cy, half,
            intArrayOf(Color.argb(120, Color.red(m), Color.green(m), Color.blue(m)), Color.argb(45, Color.red(m), Color.green(m), Color.blue(m)), Color.argb(0, Color.red(m), Color.green(m), Color.blue(m))),
            floatArrayOf(0.3f, 0.7f, 1f), Shader.TileMode.CLAMP
        )
        val hi = tone.hi
        val f = (ar / half).coerceIn(0.05f, 0.9f)
        rays = RadialGradient(
            cx, cy, half,
            intArrayOf(Color.argb(0, Color.red(hi), Color.green(hi), Color.blue(hi)), Color.argb(150, Color.red(hi), Color.green(hi), Color.blue(hi)), Color.argb(0, Color.red(hi), Color.green(hi), Color.blue(hi))),
            floatArrayOf(f * 0.9f, f + 0.08f, 1f), Shader.TileMode.CLAMP
        )
        sweep = SweepGradient(
            cx, cy,
            intArrayOf(Color.argb(0, 255, 255, 255), Color.argb(240, 255, 255, 255), Color.argb(0, 255, 255, 255), Color.argb(0, 255, 255, 255)),
            floatArrayOf(0f, 0.1f, 0.28f, 1f)
        )
    }

    private fun spark(c: Canvas, x: Float, y: Float, size: Float, a: Float) {
        p.style = Paint.Style.FILL
        p.shader = null
        p.color = Color.argb((60 * a).toInt(), 255, 255, 255)
        c.drawCircle(x, y, size * 1.8f, p)
        p.color = Color.argb((255 * a).toInt().coerceIn(0, 255), 255, 244, 205)
        c.save()
        c.translate(x, y)
        c.scale(size, size)
        c.drawPath(star, p)
        c.restore()
    }

    override fun onDraw(c: Canvas) {
        if (width == 0 || height == 0) return
        val d = resources.displayMetrics.density
        val t = clockSec()
        val cx = width / 2f
        val cy = height / 2f
        val half = minOf(width, height) / 2f
        val ar = avatarDp * d / 2f
        val s = ar / (42f * d)

        // 1. brilho
        p.style = Paint.Style.FILL
        p.shader = glow
        c.drawCircle(cx, cy, half, p)

        // 2. raios girando devagar
        p.shader = rays
        c.save()
        c.rotate(t * 7f, cx, cy)
        oval.set(cx - half, cy - half, cx + half, cy + half)
        for (i in 0 until 12) {
            wedge.reset()
            wedge.moveTo(cx, cy)
            wedge.arcTo(oval, i * 30f, 13f)
            wedge.close()
            c.drawPath(wedge, p)
        }
        c.restore()
        p.shader = null

        // 3. fio de pérolas + brilho que corre pelo anel
        val r1 = ar + 4f * d * s
        for (i in 0 until 36) {
            val ang = i * (Math.PI * 2 / 36)
            val tw = 0.5f + 0.5f * sin(t * 2.6f - i * 0.5f)
            p.color = Color.argb((110 + 145 * tw).toInt().coerceIn(0, 255), 255, 244, 214)
            c.drawCircle(cx + (r1 * cos(ang)).toFloat(), cy + (r1 * sin(ang)).toFloat(), 1.15f * d * s * (0.9f + 0.45f * tw), p)
        }
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f * d * s
        p.shader = sweep
        c.save()
        c.rotate(t * 110f, cx, cy)
        c.drawCircle(cx, cy, r1, p)
        c.restore()
        p.shader = null

        // 4. louros dos dois lados
        val r2 = ar + 12f * d * s
        val leafL = 10.5f * d * s
        val leafW = 4.8f * d * s
        arc.set(cx - r2, cy - r2, cx + r2, cy + r2)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.3f * d * s
        p.color = Color.argb(210, Color.red(tone.lo), Color.green(tone.lo), Color.blue(tone.lo))
        c.drawArc(arc, 100f, 118f, false, p)
        c.drawArc(arc, -38f, 118f, false, p)
        p.style = Paint.Style.FILL
        val n = 9
        for (side in 0 until 2) {
            for (i in 0 until n) {
                val deg = 104f + i * (112f / (n - 1))
                val th = if (side == 0) deg else 180f - deg
                val rad = Math.toRadians(th.toDouble())
                val lx = cx + (r2 * cos(rad)).toFloat()
                val ly = cy + (r2 * sin(rad)).toFloat()
                val grow = if (side == 0) th + 90f else th - 90f
                val tilt = if (i % 2 == 0) 38f else -38f
                val k = 0.5f + 0.5f * sin(t * 2.2f - i * 0.6f)
                p.color = mixColor(tone.mid, tone.hi, k)
                val sz = 0.75f + 0.25f * (i / (n - 1f))
                c.save()
                c.translate(lx, ly)
                c.rotate(grow + tilt)
                oval.set(-leafL * sz / 2f, -leafW * sz / 2f, leafL * sz / 2f, leafW * sz / 2f)
                c.drawOval(oval, p)
                c.restore()
            }
            // bolinha de fruto na ponta de cima de cada ramo
            val top = if (side == 0) 104f + 112f else 180f - (104f + 112f)
            val tr = Math.toRadians(top.toDouble())
            p.color = Color.argb(255, 255, 214, 235)
            c.drawCircle(cx + ((r2 + 1f * d) * cos(tr)).toFloat(), cy + ((r2 + 1f * d) * sin(tr)).toFloat(), 1.7f * d * s, p)
        }
        // lacinho (um brilho) onde os dois ramos se encontram
        spark(c, cx, cy + r2 + 1f * d, 3.4f * d * s, 0.7f + 0.3f * sin(t * 2.4f))

        // 5. estrelinhas orbitando
        for (k in 0 until 4) {
            val ang = t * 0.5f + k * 1.5708f
            val rr = half * 0.88f
            val tw = 0.5f + 0.5f * sin(t * 2.8f + k * 1.7f)
            spark(c, cx + rr * cos(ang), cy + rr * sin(ang), 3.2f * d * s * (0.55f + 0.45f * tw), 0.35f + 0.65f * tw)
        }

        if (isAttachedToWindow) postInvalidateDelayed(40)
    }
}

/**
 * Degrau de pódio em metal (prata ou bronze): topo mais claro, número gravado e um brilho
 * que passa de tempos em tempos.
 */
class PedestalView(ctx: Context, private val rank: Int) : View(ctx) {
    private val tone = Metal.tone(rank)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shape = Path()
    private val para = Path()
    private val rect = RectF()
    private val star = starPath()

    private fun spark(c: Canvas, x: Float, y: Float, size: Float, a: Float) {
        p.style = Paint.Style.FILL
        p.shader = null
        p.color = Color.argb((255 * a).toInt().coerceIn(0, 255), 255, 255, 255)
        c.save()
        c.translate(x, y)
        c.scale(size, size)
        c.drawPath(star, p)
        c.restore()
    }

    override fun onDraw(c: Canvas) {
        if (width == 0 || height == 0) return
        val d = resources.displayMetrics.density
        val t = clockSec()
        val w = width.toFloat()
        val h = height.toFloat()
        val r = 10f * d
        val lip = 6f * d

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

        // brilhinhos no degrau
        val a1 = 0.5f + 0.5f * sin(t * 3f + rank)
        val a2 = 0.5f + 0.5f * sin(t * 2.2f + rank * 2f)
        spark(c, w * 0.16f, lip + 7f * d, 2.4f * d * (0.6f + 0.4f * a1), a1)
        spark(c, w * 0.85f, lip + 11f * d, 2f * d * (0.6f + 0.4f * a2), a2)

        if (isAttachedToWindow) postInvalidateDelayed(50)
    }
}
