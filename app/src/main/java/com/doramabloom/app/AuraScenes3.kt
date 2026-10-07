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
        val blink = if (k.frac(t) < 0.6f) 1f else 0.3f
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

// ======================================================================================
// MÉDICO: hospital. Monitor cardíaco, DNA, cruzes, remédios, hexágonos e luz limpa.
// ======================================================================================
class MedicoScene : AuraScene {
    private var kit: SceneKit? = null
    private var crosses: List<SceneKit.Mote> = emptyList()
    private var meds: List<SceneKit.Mote> = emptyList()
    private var bubbles: List<SceneKit.Mote> = emptyList()

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        crosses = k.motes(12, 1501L, 0.03f, 0.07f, 12f, 24f, 18f)
        meds = k.motes(6, 1502L, 0.025f, 0.05f, 20f, 30f, 14f)
        bubbles = k.motes(14, 1503L, 0.04f, 0.09f, 3f, 8f, 12f)
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        val u = k.u
        val p = k.p
        val teal = Color.parseColor("#1FA5BC")
        val mint = Color.parseColor("#9CFFE0")
        val red = Color.parseColor("#FF5C7A")

        k.vgrad(c, w, h, Color.parseColor("#F2FEFF"), Color.parseColor("#8FE3EE"), Color.parseColor("#2FA9C0"))

        // grade de hexágonos de molécula, bem suave
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f * u
        val hs = 22f * u
        for (r in 0 until 8) {
            for (q in 0 until 9) {
                val hx = q * hs * 1.75f + (if (r % 2 == 0) 0f else hs * 0.875f)
                val hy = r * hs * 1.52f
                val a = 40f + 28f * sin(t * 0.6f + r * 0.8f + q * 0.5f)
                p.color = k.al(Color.WHITE, a)
                k.path.reset()
                for (s in 0..5) {
                    val ang = s * k.twoPi / 6f + 0.5236f
                    val px = hx + cos(ang) * hs * 0.9f
                    val py = hy + sin(ang) * hs * 0.9f
                    if (s == 0) k.path.moveTo(px, py) else k.path.lineTo(px, py)
                }
                k.path.close()
                c.drawPath(k.path, p)
            }
        }

        // DNA em hélice girando na lateral
        for (side in 0 until 2) {
            val bx = if (side == 0) w * 0.07f else w * 0.93f
            val n = 20
            for (s in 0 until n) {
                val fy = s / (n - 1f)
                val y = h * (0.05f + 0.9f * fy)
                val ph = fy * 9f + t * 1.6f + side * 2f
                val x1 = bx + sin(ph) * 14f * u
                val x2 = bx - sin(ph) * 14f * u
                val d1 = cos(ph)
                val a = k.edge(y, h)
                p.style = Paint.Style.STROKE
                p.strokeWidth = 1.4f * u
                p.color = k.al(Color.WHITE, 150f * a)
                c.drawLine(x1, y, x2, y, p)
                p.style = Paint.Style.FILL
                p.color = k.al(mint, 255f * a * (0.6f + 0.4f * d1))
                c.drawCircle(x1, y, (3f + 1.3f * d1) * u, p)
                p.color = k.al(red, 255f * a * (0.6f - 0.4f * d1))
                c.drawCircle(x2, y, (3f - 1.3f * d1) * u, p)
            }
        }

        // cruzes médicas subindo, pulsando
        for (i in crosses.indices) {
            val m = crosses[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1.05f - 1.1f * life)
            val x = w * (0.15f + 0.7f * m.x) + sin(t * 0.9f + m.ph * 8f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            k.icon(c, "cross", k.al(if (i % 3 == 0) red else Color.WHITE, 235f * a), x, y, m.sz * u * (1f + 0.1f * sin(t * 3f + m.ph * 9f)), 0f)
        }

        // remédios e seringas flutuando
        for (i in meds.indices) {
            val m = meds[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (-0.06f + 1.12f * life)
            val x = w * (0.15f + 0.7f * m.x) + sin(t * 0.7f + m.ph * 8f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            k.icon(c, if (i % 2 == 0) "pill" else "syringe", k.al(if (i % 2 == 0) Color.parseColor("#FFB347") else Color.WHITE, 245f * a), x, y, m.sz * u, t * 25f + m.ph * 300f)
        }

        // ondas do monitor saindo da capa no ritmo do coração
        val ph = k.frac(t / 1.2f)
        val beat = k.bump(ph, 0.1f, 0.05f)
        if (hasPoster) {
            for (j in 0 until 2) {
                val life = k.frac(t / 1.2f - j * 0.15f)
                val e = life * 34f * u
                p.style = Paint.Style.STROKE
                p.strokeWidth = (3f - 2.4f * life) * u
                p.color = k.al(Color.WHITE, 210f * (1f - life))
                k.rect.set(poster.left - e, poster.top - e, poster.right + e, poster.bottom + e)
                c.drawRoundRect(k.rect, 20f * u + e, 20f * u + e, p)
            }
        }

        // linha de monitor cardíaco com ponto brilhante na ponta
        val my = h * 0.86f
        val head = k.frac(t * 0.28f)
        k.path.reset()
        val n = 90
        var started = false
        var hx = 0f
        var hy = my
        for (s in 0..n) {
            val fx = s / n.toFloat()
            if (fx > head) break
            val q = k.frac(fx * 3f)
            val spike = k.bump(q, 0.5f, 0.015f) * 24f - k.bump(q, 0.55f, 0.015f) * 9f + k.bump(q, 0.7f, 0.04f) * 5f
            val y = my - spike * u
            if (!started) { k.path.moveTo(w * fx, y); started = true } else k.path.lineTo(w * fx, y)
            hx = w * fx
            hy = y
        }
        p.style = Paint.Style.STROKE
        p.strokeWidth = 4.5f * u
        p.color = k.al(teal, 50f)
        c.drawPath(k.path, p)
        p.strokeWidth = 1.8f * u
        p.color = k.al(Color.parseColor("#0C6A7A"), 255f)
        c.drawPath(k.path, p)
        k.glow(c, hx, hy, 9f * u, red, 230f)

        // bolhas de soro e brilhos de limpeza
        for (m in bubbles) {
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1.02f - 1.0f * life)
            val x = w * m.x + sin(t * 1.2f + m.ph * 7f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            p.style = Paint.Style.STROKE
            p.strokeWidth = 1.2f * u
            p.color = k.al(Color.WHITE, 190f * a)
            c.drawCircle(x, y, m.sz * u, p)
            p.style = Paint.Style.FILL
            p.color = k.al(Color.WHITE, 220f * a)
            c.drawCircle(x - m.sz * u * 0.35f, y - m.sz * u * 0.35f, m.sz * u * 0.2f, p)
        }
        if (beat > 0.05f) {
            p.style = Paint.Style.FILL
            p.color = k.al(Color.WHITE, 36f * beat)
            c.drawRect(0f, 0f, w, h, p)
        }
    }
}

// ======================================================================================
// FAMÍLIA: lar aconchegante. Casa com fumaça, mãos dadas, bolo, presentes e luzinhas.
// ======================================================================================
class FamiliaScene : AuraScene {
    private var kit: SceneKit? = null
    private var hearts: List<SceneKit.Mote> = emptyList()
    private var gifts: List<SceneKit.Mote> = emptyList()
    private var flies: List<SceneKit.Mote> = emptyList()
    private var leaves: List<SceneKit.Mote> = emptyList()

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        hearts = k.motes(9, 1601L, 0.04f, 0.08f, 10f, 20f, 16f)
        gifts = k.motes(5, 1602L, 0.025f, 0.05f, 22f, 30f, 14f)
        flies = k.motes(12, 1603L, 0.12f, 0.3f, 1.8f, 3f, 0f)
        leaves = k.motes(8, 1604L, 0.03f, 0.06f, 12f, 18f, 26f)
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        val u = k.u
        val p = k.p
        val warm = Color.parseColor("#FFD08A")
        val coral = Color.parseColor("#FF7A59")

        k.vgrad(c, w, h, Color.parseColor("#FFF6E3"), Color.parseColor("#FFC794"), Color.parseColor("#F29B5C"))

        // sol quentinho com raios girando devagar
        val sx = w * 0.15f
        val sy = h * 0.14f
        k.glow(c, sx, sy, 70f * u, warm, 180f)
        for (i in 0 until 12) {
            val a = t * 0.15f + i * k.twoPi / 12f
            p.style = Paint.Style.STROKE
            p.strokeWidth = 2.4f * u
            p.strokeCap = Paint.Cap.ROUND
            p.color = k.al(Color.WHITE, 190f)
            c.drawLine(sx + cos(a) * 24f * u, sy + sin(a) * 24f * u, sx + cos(a) * (34f + 4f * sin(t * 2f + i)) * u, sy + sin(a) * (34f + 4f * sin(t * 2f + i)) * u, p)
        }
        p.strokeCap = Paint.Cap.BUTT
        k.icon(c, "sun", k.al(Color.parseColor("#FFE27A"), 255f), sx, sy, 38f * u, t * 8f)

        // fio de luzinhas quentes balançando
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.2f * u
        p.color = k.al(Color.parseColor("#8A4A1A"), 150f)
        k.path.reset()
        for (s in 0..20) {
            val fx = s / 20f
            val y = 4f * u + 12f * u * sin(fx * Math.PI.toFloat() * 2f)
            if (s == 0) k.path.moveTo(w * fx, y) else k.path.lineTo(w * fx, y)
        }
        c.drawPath(k.path, p)
        for (i in 0 until 10) {
            val fx = (i + 0.5f) / 10f
            val y = 4f * u + 12f * u * sin(fx * Math.PI.toFloat() * 2f) + 6f * u
            val tw = 0.6f + 0.4f * sin(t * 2.2f + i * 1.7f)
            k.glow(c, w * fx, y, 10f * u, warm, 170f * tw)
            p.style = Paint.Style.FILL
            p.color = k.al(Color.WHITE, 255f)
            c.drawCircle(w * fx, y, 2.4f * u, p)
        }

        // casa com janela acesa e fumaça saindo da chaminé
        val hx = w * 0.82f
        val hy = h * 0.72f
        k.glow(c, hx, hy, 60f * u, warm, 120f)
        k.icon(c, "home", k.al(Color.parseColor("#7A3A12"), 245f), hx, hy, 80f * u, 0f)
        p.style = Paint.Style.FILL
        p.color = k.al(Color.parseColor("#FFE27A"), 200f + 40f * sin(t * 3f))
        c.drawRect(hx - 8f * u, hy + 4f * u, hx + 8f * u, hy + 18f * u, p)
        for (i in 0 until 5) {
            val life = k.frac(t * 0.25f + i / 5f)
            val x = hx + 18f * u + sin(life * 6f + i) * 7f * u
            val y = hy - 34f * u - life * 60f * u
            p.color = k.al(Color.WHITE, 150f * (1f - life))
            c.drawCircle(x, y, (4f + 8f * life) * u, p)
        }

        // família de mãos dadas passeando, com os pezinhos balançando
        val base = h - 14f * u
        val fx0 = w * (0.1f + 0.4f * k.frac(t * 0.02f))
        val heights = floatArrayOf(52f, 46f, 30f, 38f)
        val xs = FloatArray(4)
        for (i in 0 until 4) xs[i] = fx0 + i * 24f * u
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.2f * u
        p.color = k.al(Color.parseColor("#7A3A12"), 235f)
        for (i in 0 until 3) c.drawLine(xs[i], base - heights[i] * u * 0.55f, xs[i + 1], base - heights[i + 1] * u * 0.55f, p)
        for (i in 0 until 4) {
            val hh = heights[i] * u
            val bob = abs(sin(t * 4f + i)) * 2f * u
            val colB = if (i == 0) coral else if (i == 1) Color.parseColor("#4D9BFF") else if (i == 2) Color.parseColor("#FFD54A") else Color.parseColor("#9B5DE5")
            p.style = Paint.Style.FILL
            p.color = k.al(colB, 255f)
            k.rect.set(xs[i] - hh * 0.18f, base - hh * 0.72f - bob, xs[i] + hh * 0.18f, base - hh * 0.2f - bob)
            c.drawRoundRect(k.rect, 5f * u, 5f * u, p)
            p.color = k.al(Color.parseColor("#FFDCB8"), 255f)
            c.drawCircle(xs[i], base - hh * 0.86f - bob, hh * 0.13f, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 3f * u
            p.color = k.al(Color.parseColor("#7A3A12"), 255f)
            val sw = sin(t * 5f + i * 1.4f) * 3f * u
            c.drawLine(xs[i] - 2f * u, base - hh * 0.2f - bob, xs[i] - 2f * u + sw, base, p)
            c.drawLine(xs[i] + 2f * u, base - hh * 0.2f - bob, xs[i] + 2f * u - sw, base, p)
        }
        val ph = k.frac(t * 0.4f)
        if (ph < 0.8f) k.icon(c, "heart", k.al(coral, 255f * k.env(ph / 0.8f)), xs[1] + 12f * u, base - 70f * u - ph * 24f * u, 14f * u, 0f)

        // bolo com velinhas tremendo
        val cx = w * 0.14f
        val cy = h * 0.5f + sin(t * 1.1f) * 4f * u
        k.icon(c, "cake", k.al(Color.WHITE, 250f), cx, cy, 44f * u, -6f)
        for (i in 0 until 3) {
            val fl = 0.7f + 0.3f * sin(t * 9f + i * 2f)
            k.glow(c, cx - 8f * u + i * 8f * u, cy - 26f * u, 9f * u * fl, Color.parseColor("#FFB347"), 190f)
            k.icon(c, "flame", k.al(Color.parseColor("#FFD54A"), 250f), cx - 8f * u + i * 8f * u, cy - 25f * u, 9f * u * fl, sin(t * 7f + i) * 6f)
        }

        // presentes e corações flutuando
        for (i in gifts.indices) {
            val m = gifts[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (-0.06f + 1.12f * life)
            val x = w * m.x + sin(t * 0.8f + m.ph * 8f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            k.icon(c, "gift", k.al(if (i % 2 == 0) coral else Color.WHITE, 245f * a), x, y, m.sz * u, sin(t * 1.3f + m.ph * 6f) * 12f)
        }
        for (i in hearts.indices) {
            val m = hearts[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1.05f - 1.1f * life)
            val x = w * m.x + sin(t + m.ph * 8f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            k.icon(c, "heart", k.al(if (i % 2 == 0) Color.WHITE else coral, 235f * a), x, y, m.sz * u, sin(t * 1.4f + m.ph * 5f) * 12f)
        }

        // folhas de outono e vagalumes quentes
        for (i in leaves.indices) {
            val m = leaves[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (-0.08f + 1.16f * life)
            val x = w * m.x + sin(t * 0.7f + m.ph * 7f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            k.icon(c, "leaf", k.al(if (i % 2 == 0) Color.parseColor("#E07A2F") else Color.parseColor("#C9501C"), 235f * a), x, y, m.sz * u, t * 50f + m.ph * 360f)
        }
        for (m in flies) {
            val x = w * (0.5f + 0.47f * sin(t * m.sp * 2.2f + m.ph * k.twoPi))
            val y = h * (0.5f + 0.4f * sin(t * m.sp * 1.7f + m.ph * 9f))
            val a = k.edge(y, h) * (0.6f + 0.4f * sin(t * 2.6f + m.ph * 12f))
            k.glow(c, x, y, 7f * u, warm, 150f * a)
            p.style = Paint.Style.FILL
            p.color = k.al(Color.WHITE, 245f * a)
            c.drawCircle(x, y, m.sz * u * 0.7f, p)
        }
    }
}

// ======================================================================================
// VIDA REAL: manhã de café. Vapor, janela, sol, folhas, pássaros e a cidade acordando.
// ======================================================================================
class VidaScene : AuraScene {
    private var kit: SceneKit? = null
    private var leaves: List<SceneKit.Mote> = emptyList()
    private var dust: List<SceneKit.Mote> = emptyList()
    private var birds: List<SceneKit.Mote> = emptyList()
    private var cups: List<SceneKit.Mote> = emptyList()

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        leaves = k.motes(10, 1701L, 0.03f, 0.06f, 12f, 20f, 28f)
        dust = k.motes(26, 1702L, 0.02f, 0.05f, 1.2f, 2.6f, 12f)
        birds = k.motes(3, 1703L, 0.02f, 0.035f, 8f, 12f, 0f)
        cups = k.motes(5, 1704L, 0.02f, 0.04f, 20f, 28f, 14f)
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        val u = k.u
        val p = k.p
        val cream = Color.parseColor("#FFF1DC")
        val brown = Color.parseColor("#6B4630")

        k.vgrad(c, w, h, Color.parseColor("#FBE9D0"), Color.parseColor("#E3BC96"), Color.parseColor("#A9754F"))

        // sol baixo da manhã e raios atravessando a janela
        val sx = w * 0.8f
        val sy = h * 0.2f
        k.glow(c, sx, sy, 80f * u, Color.parseColor("#FFD79A"), 180f)
        k.icon(c, "sun", k.al(Color.parseColor("#FFE9B0"), 255f), sx, sy, 40f * u, t * 6f)
        for (i in 0 until 4) {
            val bx = w * (0.45f + 0.15f * i)
            k.path.reset()
            k.path.moveTo(bx, 0f)
            k.path.lineTo(bx + 20f * u, 0f)
            k.path.lineTo(bx + 20f * u - h * 0.45f, h)
            k.path.lineTo(bx - h * 0.45f, h)
            k.path.close()
            p.style = Paint.Style.FILL
            p.color = k.al(cream, 36f + 14f * sin(t * 0.6f + i * 1.4f))
            c.drawPath(k.path, p)
        }

        // nuvens leves passando
        for (i in 0 until 3) {
            val x = w * (1.2f - 1.5f * k.frac(t * 0.01f + i * 0.33f))
            k.icon(c, "cloud", k.al(Color.WHITE, 150f), x, h * (0.1f + 0.08f * i), (60f + 14f * i) * u, 0f)
        }

        // passarinhos cruzando o céu
        for (i in birds.indices) {
            val m = birds[i]
            val life = k.frac(t * m.sp + m.ph)
            val bx = w * (-0.1f + 1.2f * life)
            val by = h * (0.22f + 0.12f * m.y) + sin(life * 10f + i) * 5f * u
            val flap = sin(t * 7f + i) * 3.4f * u
            p.style = Paint.Style.STROKE
            p.strokeWidth = 1.8f * u
            p.color = k.al(brown, 220f)
            k.path.reset()
            k.path.moveTo(bx - m.sz * u, by - flap)
            k.path.quadTo(bx - m.sz * u * 0.4f, by - 1f * u, bx, by)
            k.path.quadTo(bx + m.sz * u * 0.4f, by - 1f * u, bx + m.sz * u, by - flap)
            c.drawPath(k.path, p)
        }

        // silhueta da cidade acordando, janelas acendendo uma a uma
        val baseY = h - 10f * u
        for (i in 0 until 9) {
            val bw = w * 0.11f
            val bh = (30f + 24f * ((i * 5) % 4)) * u
            val bx = w * i * 0.11f
            p.style = Paint.Style.FILL
            p.color = k.al(Color.parseColor("#6B4630"), 235f)
            k.rect.set(bx, baseY - bh, bx + bw - 3f * u, baseY + 6f * u)
            c.drawRect(k.rect, p)
            for (r in 0 until 3) {
                for (cc in 0 until 2) {
                    val on = k.frac(t * 0.02f + i * 0.13f + r * 0.07f + cc * 0.31f) > 0.45f
                    p.color = k.al(if (on) Color.parseColor("#FFD27A") else Color.parseColor("#8A6244"), 230f)
                    val wx = bx + 5f * u + cc * 11f * u
                    val wy = baseY - bh + 6f * u + r * 10f * u
                    c.drawRect(wx, wy, wx + 5f * u, wy + 6f * u, p)
                }
            }
        }

        // xícara de café com vapor ondulando
        val cx = w * 0.14f
        val cy = h * 0.6f
        k.glow(c, cx, cy - 20f * u, 46f * u, Color.parseColor("#FFD79A"), 90f)
        k.icon(c, "coffee", k.al(Color.WHITE, 255f), cx, cy, 52f * u, 0f)
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        for (i in 0 until 3) {
            for (s in 0 until 14) {
                val life = k.frac(t * 0.35f + i / 3f - s * 0.02f)
                val y = cy - 24f * u - life * 56f * u
                val x = cx + (i - 1) * 9f * u + sin(life * 9f + i * 2f + t) * 6f * u
                p.strokeWidth = (3.4f - 2.6f * life) * u
                p.color = k.al(Color.WHITE, 130f * (1f - life) * (1f - s / 14f))
                c.drawPoint(x, y, p)
            }
        }
        p.strokeCap = Paint.Cap.BUTT

        // xícaras e folhas flutuando
        for (i in cups.indices) {
            val m = cups[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1.05f - 1.1f * life)
            val x = w * (0.2f + 0.7f * m.x) + sin(t * 0.7f + m.ph * 8f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            k.icon(c, "coffee", k.al(cream, 200f * a), x, y, m.sz * u, sin(t + m.ph * 6f) * 10f)
        }
        for (i in leaves.indices) {
            val m = leaves[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (-0.08f + 1.16f * life)
            val x = w * m.x + sin(t * 0.6f + m.ph * 7f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            k.icon(c, "leaf", k.al(if (i % 2 == 0) Color.parseColor("#7DA55A") else Color.parseColor("#C08A3E"), 235f * a), x, y, m.sz * u, t * 40f + m.ph * 360f)
        }

        // poeira dourada na luz
        for (m in dust) {
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1f - life)
            val x = w * m.x + sin(t * 0.8f + m.ph * 9f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            p.style = Paint.Style.FILL
            p.color = k.al(Color.WHITE, 190f * a)
            c.drawCircle(x, y, m.sz * u, p)
        }
    }
}

// ======================================================================================
// VINGANÇA: sangue e aço. Lua vermelha, ampulheta, adagas, gotas, vidro trincando e caveiras.
// ======================================================================================
class VingancaScene : AuraScene {
    private var kit: SceneKit? = null
    private var drips: List<SceneKit.Mote> = emptyList()
    private var daggers: List<SceneKit.Mote> = emptyList()
    private var embers: List<SceneKit.Mote> = emptyList()
    private var sand: List<SceneKit.Mote> = emptyList()

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        drips = k.motes(9, 1801L, 0.04f, 0.09f, 3f, 6f, 0f)
        daggers = k.motes(5, 1802L, 0.05f, 0.09f, 26f, 38f, 20f)
        embers = k.motes(18, 1803L, 0.05f, 0.12f, 1.4f, 3f, 14f)
        sand = k.motes(22, 1804L, 0.8f, 1.4f, 1.2f, 2f, 0f)
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        val u = k.u
        val p = k.p
        val blood = Color.parseColor("#C41E3A")
        val ember = Color.parseColor("#FF6A3D")
        val steel = Color.parseColor("#D8DEE9")

        k.vgrad(c, w, h, Color.parseColor("#060103"), Color.parseColor("#3A0A1B"), Color.parseColor("#7C1A32"))

        // lua vermelha eclipsada com anel de fogo
        val mx = w * 0.2f
        val my = h * 0.2f
        val mr = 28f * u
        k.glow(c, mx, my, mr * 3.2f, blood, 150f + 30f * sin(t * 0.7f))
        p.style = Paint.Style.FILL
        p.color = k.al(Color.parseColor("#E8374F"), 255f)
        c.drawCircle(mx, my, mr, p)
        p.color = k.al(Color.parseColor("#14030A"), 255f)
        c.drawCircle(mx + mr * 0.28f, my - mr * 0.05f, mr * 0.9f, p)

        // ampulheta no centro-direita, com areia caindo grão a grão
        val hgx = w * 0.88f
        val hgy = h * 0.4f
        k.glow(c, hgx, hgy, 38f * u, ember, 80f)
        k.icon(c, "hourglass", k.al(Color.parseColor("#F1D6A8"), 245f), hgx, hgy, 56f * u, 0f)
        for (m in sand) {
            val life = k.frac(t * m.sp + m.ph)
            p.style = Paint.Style.FILL
            p.color = k.al(Color.parseColor("#FFD27A"), 230f * (1f - life * 0.4f))
            c.drawCircle(hgx + sin(m.ph * 30f) * 1.2f * u, hgy - 4f * u + life * 24f * u, m.sz * u, p)
        }

        // gotas de sangue escorrendo do alto
        for (i in drips.indices) {
            val m = drips[i]
            val x = w * (0.05f + 0.9f * m.x)
            val life = k.frac(t * m.sp + m.ph)
            val len = h * (0.1f + 0.3f * m.y) * min(1f, life * 3f)
            val dropY = len + max(0f, life - 0.33f) * h * 0.9f
            p.style = Paint.Style.STROKE
            p.strokeWidth = (m.sz + 1f) * u
            p.strokeCap = Paint.Cap.ROUND
            p.color = k.al(blood, 235f)
            c.drawLine(x, 0f, x, min(len, dropY), p)
            p.strokeCap = Paint.Cap.BUTT
            if (life > 0.33f && dropY < h) {
                val a = k.edge(dropY, h)
                p.style = Paint.Style.FILL
                p.color = k.al(blood, 245f * a)
                k.rect.set(x - m.sz * u, dropY - m.sz * 1.6f * u, x + m.sz * u, dropY + m.sz * 1.2f * u)
                c.drawOval(k.rect, p)
            } else {
                p.style = Paint.Style.FILL
                p.color = k.al(blood, 245f)
                c.drawCircle(x, min(len, dropY), (m.sz + 1.6f) * u, p)
            }
        }

        // adagas caindo e cravando, girando de leve
        for (i in daggers.indices) {
            val m = daggers[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (-0.1f + 1.2f * life)
            val x = w * m.x + sin(t * 0.6f + m.ph * 8f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            val gl = k.bump(k.frac(t * 0.6f + m.ph), 0.5f, 0.06f)
            k.glow(c, x, y, m.sz * u * 0.8f, Color.WHITE, 60f * gl * a)
            k.icon(c, "dagger", k.al(steel, 250f * a), x, y, m.sz * u, 180f + sin(t * 0.9f + m.ph * 6f) * 18f)
        }

        // caveiras piscando nas sombras
        for (i in 0 until 3) {
            val x = w * (0.28f + 0.22f * i)
            val y = h * (0.7f + 0.1f * sin(i * 2.3f))
            val vis = k.bump(k.frac(t * 0.09f + i * 0.31f), 0.5f, 0.2f)
            if (vis < 0.05f) continue
            val a = vis * k.edge(y, h)
            k.glow(c, x, y, 24f * u, blood, 70f * a)
            k.icon(c, "skull", k.al(Color.parseColor("#E9DDE0"), 190f * a), x, y, 28f * u, sin(t + i) * 6f)
        }

        // vidro trincando a partir de um ponto de impacto
        val per = 7f
        val idx = floor(t / per).toInt()
        val q = (t - idx * per) / 1.2f
        if (q in 0f..1.6f) {
            val rr = java.util.Random(idx * 947L + 4L)
            val ix = w * (0.2f + 0.6f * rr.nextFloat())
            val iy = h * (0.25f + 0.5f * rr.nextFloat())
            val prog = min(1f, q * 2.4f)
            val fade = if (q > 1f) (1.6f - q) / 0.6f else 1f
            p.style = Paint.Style.STROKE
            p.strokeCap = Paint.Cap.ROUND
            for (s in 0 until 9) {
                val ang = s * k.twoPi / 9f + rr.nextFloat() * 0.5f
                var px = ix
                var py = iy
                k.path.reset()
                k.path.moveTo(px, py)
                val segs = 6
                for (g in 1..segs) {
                    val len = (10f + rr.nextFloat() * 18f) * u
                    val da = ang + (rr.nextFloat() - 0.5f) * 0.7f
                    px += cos(da) * len
                    py += sin(da) * len
                    if (g.toFloat() / segs <= prog) k.path.lineTo(px, py)
                }
                p.strokeWidth = 1.4f * u
                p.color = k.al(Color.WHITE, 235f * fade)
                c.drawPath(k.path, p)
            }
            p.strokeCap = Paint.Cap.BUTT
            k.glow(c, ix, iy, 16f * u, Color.WHITE, 140f * fade * (1f - prog * 0.5f))
        }

        // brasas subindo e fumaça escura rasteira
        for (m in embers) {
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1.02f - life)
            val x = w * m.x + sin(t * 1.3f + m.ph * 9f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            p.style = Paint.Style.FILL
            p.color = k.al(ember, 235f * a)
            c.drawCircle(x, y, m.sz * u, p)
        }
        for (i in 0 until 4) {
            val x = w * (k.frac(0.25f * i + t * 0.012f) * 1.5f - 0.25f)
            val y = h * (0.82f + 0.03f * i)
            p.style = Paint.Style.FILL
            for (s in 0 until 3) {
                p.color = k.al(Color.parseColor("#2A0610"), 40f)
                k.rect.set(x - (110f - s * 22f) * u, y - (16f - s * 3f) * u, x + (110f - s * 22f) * u, y + (16f - s * 3f) * u)
                c.drawOval(k.rect, p)
            }
        }

        // pulso vermelho de ódio na borda
        val pulse = k.bump(k.frac(t / 2.2f), 0.1f, 0.08f)
        if (pulse > 0.04f) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = 14f * u
            p.color = k.al(blood, 120f * pulse)
            c.drawRect(0f, 0f, w, h, p)
        }
    }
}
