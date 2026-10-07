package com.doramabloom.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Terceira leva de cenas por gênero: Escolar, Suspense, Médico, Família, Vida real e Vingança. */

private fun txt(c: Canvas, k: SceneKit, s: String, x: Float, y: Float, size: Float, color: Int, rot: Float) {
    val p = k.p
    p.style = Paint.Style.FILL
    p.typeface = Typeface.DEFAULT_BOLD
    p.textAlign = Paint.Align.CENTER
    p.textSize = size
    p.color = color
    c.save()
    c.translate(x, y)
    c.rotate(rot)
    c.drawText(s, 0f, size * 0.35f, p)
    c.restore()
    p.textAlign = Paint.Align.LEFT
    p.typeface = null
}

// ======================================================================================
// ESCOLAR: manhã na escola. Raios de sol na janela, aviõezinhos, pétalas, lousa e caderno.
// ======================================================================================
class EscolarScene : AuraScene {
    private var kit: SceneKit? = null
    private var petals: List<SceneKit.Mote> = emptyList()
    private var stuff: List<SceneKit.Mote> = emptyList()
    private var planes: List<SceneKit.Mote> = emptyList()
    private var dust: List<SceneKit.Mote> = emptyList()
    private val doodles = arrayOf("A+", "1+1", "ABC", "x²", "10!", "E=mc²")

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        petals = k.motes(14, 1301L, 0.03f, 0.065f, 10f, 17f, 28f)
        stuff = k.motes(8, 1302L, 0.025f, 0.05f, 18f, 28f, 16f)
        planes = k.motes(3, 1303L, 0.05f, 0.09f, 14f, 20f, 0f)
        dust = k.motes(22, 1304L, 0.02f, 0.05f, 1.2f, 2.4f, 10f)
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        val u = k.u
        val p = k.p
        val chalk = Color.parseColor("#F4FFF8")
        val sun = Color.parseColor("#FFF3B0")

        k.vgrad(c, w, h, Color.parseColor("#D6F8E8"), Color.parseColor("#86D9B6"), Color.parseColor("#3E9C78"))

        // raios de sol entrando pela janela, em diagonal
        for (i in 0 until 5) {
            val bx = w * (0.55f + 0.18f * i) - h * 0.2f
            k.path.reset()
            k.path.moveTo(bx, 0f)
            k.path.lineTo(bx + 26f * u, 0f)
            k.path.lineTo(bx + 26f * u - h * 0.5f, h)
            k.path.lineTo(bx - h * 0.5f, h)
            k.path.close()
            p.style = Paint.Style.FILL
            p.color = k.al(sun, 40f + 18f * sin(t * 0.7f + i * 1.3f))
            c.drawPath(k.path, p)
        }
        k.glow(c, w * 0.95f, h * 0.02f, 70f * u, sun, 170f)

        // rabiscos de giz flutuando como numa lousa
        for (i in 0 until 6) {
            val x = w * (0.1f + 0.16f * i)
            val y = h * (0.14f + 0.16f * ((i * 5) % 4)) + sin(t * 0.8f + i) * 4f * u
            val a = (0.55f + 0.45f * sin(t * 0.5f + i * 1.7f)) * k.edge(y, h)
            txt(c, k, doodles[i], x, y, 15f * u, k.al(chalk, 200f * a), sin(t * 0.4f + i) * 10f - 6f)
        }

        // relógio da escola com ponteiro de segundos
        val cx = w * 0.1f
        val cy = h * 0.25f
        val cr = 17f * u
        p.style = Paint.Style.FILL
        p.color = k.al(Color.WHITE, 235f)
        c.drawCircle(cx, cy, cr, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f * u
        p.color = k.al(Color.parseColor("#226B4B"), 255f)
        c.drawCircle(cx, cy, cr, p)
        for (i in 0 until 12) {
            val a = i * k.twoPi / 12f
            c.drawLine(cx + cos(a) * cr * 0.8f, cy + sin(a) * cr * 0.8f, cx + cos(a) * cr * 0.92f, cy + sin(a) * cr * 0.92f, p)
        }
        p.strokeWidth = 1.6f * u
        val sa = t * k.twoPi / 6f - k.twoPi / 4f
        c.drawLine(cx, cy, cx + cos(sa) * cr * 0.75f, cy + sin(sa) * cr * 0.75f, p)
        val ma = t * k.twoPi / 60f - k.twoPi / 4f
        c.drawLine(cx, cy, cx + cos(ma) * cr * 0.5f, cy + sin(ma) * cr * 0.5f, p)

        // aviõezinhos de papel cruzando a sala
        for (i in planes.indices) {
            val m = planes[i]
            val life = k.frac(t * m.sp + m.ph)
            val x = w * (-0.1f + 1.2f * life)
            val y = h * (0.2f + 0.5f * m.y) + sin(life * 14f + i) * 8f * u - life * 20f * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            val s = m.sz * u
            val tilt = Math.toDegrees(sin(life * 14f + i).toDouble() * 0.25).toFloat()
            c.save()
            c.translate(x, y)
            c.rotate(tilt)
            p.style = Paint.Style.FILL
            p.color = k.al(Color.WHITE, 245f * a)
            k.path.reset()
            k.path.moveTo(s, 0f)
            k.path.lineTo(-s, -s * 0.55f)
            k.path.lineTo(-s * 0.45f, 0f)
            k.path.close()
            c.drawPath(k.path, p)
            p.color = k.al(Color.parseColor("#B9E8D2"), 245f * a)
            k.path.reset()
            k.path.moveTo(s, 0f)
            k.path.lineTo(-s * 0.45f, 0f)
            k.path.lineTo(-s, s * 0.35f)
            k.path.close()
            c.drawPath(k.path, p)
            c.restore()
            p.style = Paint.Style.STROKE
            p.strokeWidth = 1f * u
            p.color = k.al(Color.WHITE, 90f * a)
            for (s2 in 1..5) {
                c.drawLine(x - s - s2 * 7f * u, y + sin(s2.toFloat() + t * 3f) * 1.5f * u, x - s - s2 * 7f * u - 4f * u, y + sin(s2.toFloat() + t * 3f) * 1.5f * u, p)
            }
        }

        // livros, lápis e mochilas de sonho caindo girando
        for (i in stuff.indices) {
            val m = stuff[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (-0.08f + 1.14f * life)
            val x = w * m.x + sin(t * 0.8f + m.ph * 8f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            val name = if (i % 3 == 0) "book" else if (i % 3 == 1) "pencil" else "school"
            val col = if (i % 2 == 0) Color.WHITE else Color.parseColor("#FFE27A")
            k.icon(c, name, k.al(col, 240f * a), x, y, m.sz * u, t * 30f * (if (i % 2 == 0) 1f else -1f) + m.ph * 200f)
        }

        // pétalas de cerejeira da entrada da escola
        for (i in petals.indices) {
            val m = petals[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (-0.08f + 1.16f * life)
            val x = w * (m.x + 0.1f * life) + sin(t * 0.6f + m.ph * 7f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            k.iconXY(c, "petal", k.al(if (i % 2 == 0) Color.parseColor("#FFD3E0") else Color.WHITE, 235f * a), x, y, m.sz * u, t * 40f + m.ph * 360f, 0.5f + 0.5f * abs(sin(t * 1.2f + m.ph * 7f)), 1f)
        }

        // linhas de caderno na base com lápis escrevendo
        val ly = h - 26f * u
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.2f * u
        for (i in 0 until 3) {
            p.color = k.al(Color.parseColor("#226B4B"), 90f)
            c.drawLine(0f, ly + i * 7f * u - 6f * u, w, ly + i * 7f * u - 6f * u, p)
        }
        val wx = w * k.frac(t * 0.12f)
        k.path.reset()
        k.path.moveTo(0f, ly)
        var xx = 0f
        while (xx < wx) {
            k.path.lineTo(xx, ly + sin(xx * 0.4f / u) * 3f * u)
            xx += 3f * u
        }
        p.strokeWidth = 1.8f * u
        p.color = k.al(Color.parseColor("#1F4F9C"), 200f)
        c.drawPath(k.path, p)
        k.icon(c, "pencil", k.al(Color.parseColor("#FFD54A"), 255f), wx + 4f * u, ly - 6f * u, 20f * u, -30f)

        // poeirinha de giz no ar
        for (m in dust) {
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1f - life)
            val x = w * m.x + sin(t + m.ph * 9f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            p.style = Paint.Style.FILL
            p.color = k.al(Color.WHITE, 200f * a)
            c.drawCircle(x, y, m.sz * u, p)
        }
    }
}

// ======================================================================================
// SUSPENSE: sala escura noir. Persianas, olho observando, relógio, batimento e lanterna.
// ======================================================================================
class SuspenseScene : AuraScene {
    private var kit: SceneKit? = null
    private var dust: List<SceneKit.Mote> = emptyList()

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        dust = k.motes(30, 1401L, 0.02f, 0.05f, 1f, 2.4f, 10f)
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        val u = k.u
        val p = k.p
        val steel = Color.parseColor("#8FB3E8")
        val red = Color.parseColor("#FF3B4E")

        k.vgrad(c, w, h, Color.parseColor("#03060C"), Color.parseColor("#10213A"), Color.parseColor("#233B5C"))

        // luz da rua entrando pelas persianas, as faixas deslizam devagar
        val off = k.frac(t * 0.03f) * 26f * u
        p.style = Paint.Style.FILL
        for (i in -2 until 14) {
            val x = i * 26f * u + off
            k.path.reset()
            k.path.moveTo(x, 0f)
            k.path.lineTo(x + 11f * u, 0f)
            k.path.lineTo(x + 11f * u - h * 0.42f, h)
            k.path.lineTo(x - h * 0.42f, h)
            k.path.close()
            p.color = k.al(steel, 26f + 10f * sin(t * 0.5f + i))
            c.drawPath(k.path, p)
        }

        // olho gigante que observa, pisca e segue o "movimento"
        val ex = w * 0.5f
        val ey = h * 0.26f
        val blinkPh = k.frac(t / 6f)
        val lid = if (blinkPh > 0.94f) abs(blinkPh - 0.97f) / 0.03f else 1f
        val ew = 60f * u
        val eh = 24f * u * lid.coerceIn(0.06f, 1f)
        k.glow(c, ex, ey, 70f * u, red, 40f)
        p.style = Paint.Style.FILL
        p.color = k.al(Color.parseColor("#E8EEF8"), 235f)
        k.rect.set(ex - ew, ey - eh, ex + ew, ey + eh)
        c.drawOval(k.rect, p)
        val ix = ex + sin(t * 0.7f) * 22f * u
        val iy = ey + sin(t * 0.43f) * 5f * u
        p.color = k.al(Color.parseColor("#3C6FB8"), 255f)
        c.save()
        c.clipRect(ex - ew, ey - eh, ex + ew, ey + eh)
        c.drawCircle(ix, iy, 17f * u, p)
        p.color = k.al(Color.BLACK, 255f)
        c.drawCircle(ix, iy, 8f * u * (0.8f + 0.2f * sin(t * 2f)), p)
        p.color = k.al(Color.WHITE, 240f)
        c.drawCircle(ix - 5f * u, iy - 5f * u, 2.6f * u, p)
        c.restore()
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.4f * u
        p.color = k.al(Color.parseColor("#0A1220"), 255f)
        c.drawOval(k.rect, p)

        // relógio de parede com ponteiro de segundos saltando e tique-taque
        val cx = w * 0.88f
        val cy = h * 0.2f
        val cr = 20f * u
        p.style = Paint.Style.FILL
        p.color = k.al(Color.parseColor("#0C1626"), 245f)
        c.drawCircle(cx, cy, cr, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f * u
        p.color = k.al(steel, 230f)
        c.drawCircle(cx, cy, cr, p)
        for (i in 0 until 12) {
            val a = i * k.twoPi / 12f
            c.drawLine(cx + cos(a) * cr * 0.82f, cy + sin(a) * cr * 0.82f, cx + cos(a) * cr * 0.94f, cy + sin(a) * cr * 0.94f, p)
        }
        val tick = floor(t * 1f)
        val sa = tick * k.twoPi / 60f * 5f - k.twoPi / 4f + 0.05f * sin(k.frac(t) * 40f) * (1f - k.frac(t))
        p.color = k.al(red, 255f)
        p.strokeWidth = 1.6f * u
        c.drawLine(cx, cy, cx + cos(sa) * cr * 0.8f, cy + sin(sa) * cr * 0.8f, p)

        // lanterna varrendo o escuro
        val lx = w * (0.5f + 0.45f * sin(t * 0.4f))
        val ly = h * 0.95f
        val lang = -1.5708f + sin(t * 0.4f + 1f) * 0.6f
        k.path.reset()
        k.path.moveTo(lx, ly)
        k.path.lineTo(lx + cos(lang - 0.17f) * h, ly + sin(lang - 0.17f) * h)
        k.path.lineTo(lx + cos(lang + 0.17f) * h, ly + sin(lang + 0.17f) * h)
        k.path.close()
        p.style = Paint.Style.FILL
        p.color = k.al(Color.parseColor("#FFF6D0"), 38f)
        c.drawPath(k.path, p)

        // sombra de uma figura atravessando devagar
        val fx = w * (1.1f - 1.2f * k.frac(t * 0.025f))
        val fy = h - 14f * u
        p.color = k.al(Color.BLACK, 235f)
        c.drawCircle(fx, fy - 56f * u, 8f * u, p)
        k.rect.set(fx - 10f * u, fy - 48f * u, fx + 10f * u, fy - 18f * u)
        c.drawRoundRect(k.rect, 7f * u, 7f * u, p)
        val step = sin(t * 3.2f) * 5f * u
        p.style = Paint.Style.STROKE
        p.strokeWidth = 5f * u
        p.strokeCap = Paint.Cap.ROUND
        p.color = k.al(Color.BLACK, 235f)
        c.drawLine(fx - 3f * u, fy - 20f * u, fx - 3f * u + step, fy, p)
        c.drawLine(fx + 3f * u, fy - 20f * u, fx + 3f * u - step, fy, p)
        p.strokeCap = Paint.Cap.BUTT

        // eletrocardiograma correndo, acelera de tempos em tempos
        val ey2 = h * 0.82f
        k.path.reset()
        val n = 80
        for (s in 0..n) {
            val fxx = s / n.toFloat()
            val ph = k.frac(fxx * 2.2f - t * 0.45f)
            val spike = k.bump(ph, 0.5f, 0.012f) * 22f - k.bump(ph, 0.54f, 0.012f) * 10f + k.bump(ph, 0.62f, 0.03f) * 4f
            val y = ey2 - spike * u
            if (s == 0) k.path.moveTo(w * fxx, y) else k.path.lineTo(w * fxx, y)
        }
        p.style = Paint.Style.STROKE
        p.strokeWidth = 4f * u
        p.color = k.al(red, 60f)
        c.drawPath(k.path, p)
        p.strokeWidth = 1.6f * u
        p.color = k.al(red, 245f)
        c.drawPath(k.path, p)

        // contagem regressiva piscando
        val secs = 9 - (floor(t) % 10f).toInt()
        val blink = if (k.calm) 0.85f else if (k.frac(t) < 0.6f) 1f else 0.3f
        txt(c, k, "00:0" + secs, w * 0.14f, h * 0.55f, 15f * u, k.al(red, 230f * blink * k.edge(h * 0.55f, h)), 0f)

        // poeira no feixe de luz
        for (m in dust) {
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1f - life)
            val x = w * m.x + sin(t * 0.8f + m.ph * 9f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            p.style = Paint.Style.FILL
            p.color = k.al(Color.WHITE, 170f * a)
            c.drawCircle(x, y, m.sz * u, p)
        }
    }
}
