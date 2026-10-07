package com.doramabloom.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Cenas refeitas: Médico e Família (formas desenhadas por código, sem piscar). A de Comédia agora fica em AuraKawaii.kt. */

// ======================================================================================
// MÉDICO: hospital limpinho. Monitor de ECG, DNA girando, estetoscópio, cápsulas,
// cruzes, molécula e raios de luz da janela.
// ======================================================================================
class MedicoScene : AuraScene {
    private var kit: SceneKit? = null
    private var pills: List<SceneKit.Mote> = emptyList()
    private var plusM: List<SceneKit.Mote> = emptyList()
    private var bubbles: List<SceneKit.Mote> = emptyList()
    private var cols = IntArray(0)

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        pills = k.motes(6, 601L, 0.014f, 0.03f, 16f, 24f, 20f)
        plusM = k.motes(10, 602L, 0.03f, 0.06f, 3.5f, 7f, 14f)
        bubbles = k.motes(10, 603L, 0.03f, 0.06f, 2f, 5f, 12f)
        cols = intArrayOf(
            Color.parseColor("#FF7B9C"), Color.parseColor("#1E8FA3"), Color.parseColor("#6FA8FF"),
            Color.WHITE, Color.parseColor("#FFE08A"), Color.WHITE
        )
    }

    private fun trace(c: Canvas, k: SceneKit, w: Float, y0: Float, amp: Float, t: Float, n: Int, color: Int, sw: Float, glow: Boolean) {
        val period = 120f * k.u
        val speed = 46f * k.u
        if (glow) {
            k.path.reset()
            for (i in 0..n) {
                val x = w * i / n
                val y = y0 - k.ecg((x + speed * t) / period) * amp
                if (i == 0) k.path.moveTo(x, y) else k.path.lineTo(x, y)
            }
            k.p.style = Paint.Style.STROKE
            k.p.strokeCap = Paint.Cap.ROUND
            k.p.strokeJoin = Paint.Join.ROUND
            k.p.strokeWidth = sw * 3.2f
            k.p.color = k.al(color, 40f)
            c.drawPath(k.path, k.p)
        }
        for (i in 0 until n) {
            val x1 = w * i / n
            val x2 = w * (i + 1) / n
            val y1 = y0 - k.ecg((x1 + speed * t) / period) * amp
            val y2 = y0 - k.ecg((x2 + speed * t) / period) * amp
            val f = (i + 1f) / n
            k.line(c, x1, y1, x2, y2, sw, color, 30f + 225f * f * f)
        }
        val hx = w * 0.985f
        val hy = y0 - k.ecg((hx + speed * t) / period) * amp
        k.soft(c, hx, hy, 9f * k.u, color, 200f)
        k.circ(c, hx, hy, 2f * k.u, Color.WHITE, 255f)
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        k.tidy()
        val u = k.u
        val p = k.p
        val steel = Color.parseColor("#C9D6DC")
        val tube = Color.parseColor("#2C4D5B")
        val pinkR = Color.parseColor("#FF5A7A")
        val mint = Color.parseColor("#9CFFE6")

        k.vgrad(c, w, h, Color.parseColor("#1FA3BA"), Color.parseColor("#4CC7D6"), Color.parseColor("#A5ECF1"))
        val beat = k.pulse(t, 1.6f)

        // luz entrando pela janela
        for (i in 0 until 3) {
            val x0 = w * (0.1f + 0.3f * i) + sin(t * 0.25f + i * 2f) * 14f * u
            k.path.reset()
            k.path.moveTo(x0, -6f * u)
            k.path.lineTo(x0 + 26f * u, -6f * u)
            k.path.lineTo(x0 + 26f * u + h * 0.55f, h)
            k.path.lineTo(x0 + h * 0.55f, h)
            k.path.close()
            p.style = Paint.Style.FILL
            p.color = k.al(Color.WHITE, 22f + 6f * sin(t * 0.4f + i))
            c.drawPath(k.path, p)
        }

        // cruz grande de fundo, respirando com o batimento
        val cx = if (hasPoster) poster.centerX() else w / 2f
        val cy = if (hasPoster) poster.centerY() else h / 2f
        k.soft(c, cx, cy, max(w, h) * 0.6f, Color.WHITE, 80f + 30f * beat)
        k.plus(c, cx, cy, min(w, h) * (0.72f + 0.03f * beat), 0f, Color.WHITE, 34f)
        k.posterRipple(c, poster, hasPoster, t, 3.2f, Color.WHITE, 150f)

        // hélice de DNA na esquerda
        val step = 5f * u
        var yy = 0f
        var idx = 0
        while (yy < h) {
            val ph = yy * 0.05f + t * 1.25f
            val xa = 17f * u + 9.5f * u * sin(ph)
            val xb = 17f * u - 9.5f * u * sin(ph)
            val d = cos(ph)
            val a = k.edge(yy, h)
            if (idx % 2 == 0) k.line(c, xa, yy, xb, yy, 1f * u, Color.WHITE, 90f * a)
            k.circ(c, xa, yy, (2.5f + 0.9f * d) * u, if (d > 0f) Color.WHITE else mint, (150f + 90f * d) * a)
            k.circ(c, xb, yy, (2.5f - 0.9f * d) * u, if (d < 0f) Color.WHITE else pinkR, (150f - 90f * d) * a)
            yy += step
            idx++
        }

        // estetoscópio na direita
        val sx = w - 18f * u
        val swv = sin(t * 0.9f) * 2.5f * u
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.strokeWidth = 1.8f * u
        p.color = k.al(steel, 255f)
        c.drawLine(sx - 12f * u, 2f * u, sx - 2f * u, 20f * u, p)
        c.drawLine(sx + 8f * u, 2f * u, sx - 2f * u, 20f * u, p)
        k.circ(c, sx - 12f * u, 2f * u, 2.6f * u, steel, 255f)
        k.circ(c, sx + 8f * u, 2f * u, 2.6f * u, steel, 255f)
        k.path.reset()
        k.path.moveTo(sx - 2f * u, 20f * u)
        k.path.cubicTo(sx + 14f * u, h * 0.3f, sx - 22f * u, h * 0.5f, sx + swv * 0.4f, h * 0.76f)
        p.strokeWidth = 3.4f * u
        p.color = k.al(tube, 255f)
        c.drawPath(k.path, p)
        p.strokeWidth = 0.9f * u
        p.color = k.al(Color.parseColor("#6E98A8"), 200f)
        c.drawPath(k.path, p)
        val cpx = sx + swv * 0.4f
        val cpy = h * 0.76f + 7f * u
        k.circ(c, cpx, cpy, 8.5f * u, Color.parseColor("#7F96A0"), 255f)
        k.circ(c, cpx, cpy, 7f * u, steel, 255f)
        k.circ(c, cpx, cpy, 4.6f * u, Color.parseColor("#9FB4BC"), 255f)
        k.circ(c, cpx - 1.6f * u, cpy - 1.6f * u, 1.2f * u, Color.WHITE, 230f)

        // molécula girando
        val mx = w * 0.22f
        val my = h * 0.34f
        val mr = 17f * u
        for (i in 0 until 6) {
            val an = i * 1.0472f + t * 0.3f
            val nx = mx + cos(an) * mr
            val ny = my + sin(an) * mr
            val nx2 = mx + cos(an + 1.0472f) * mr
            val ny2 = my + sin(an + 1.0472f) * mr
            k.line(c, mx, my, nx, ny, 1.1f * u, Color.WHITE, 110f)
            k.line(c, nx, ny, nx2, ny2, 1.1f * u, Color.WHITE, 110f)
            k.soft(c, nx, ny, 7f * u, Color.WHITE, 110f)
            k.circ(c, nx, ny, 2.6f * u, if (i % 2 == 0) Color.WHITE else mint, 245f)
        }
        k.soft(c, mx, my, 10f * u, pinkR, 120f)
        k.circ(c, mx, my, 3.6f * u, pinkR, 255f)

        // cápsulas flutuando
        for (i in pills.indices) {
            val m = pills[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1.08f - 1.16f * life)
            val x = w * (0.12f + 0.76f * m.x) + sin(t * 0.5f + m.ph * 6.28f) * m.sw * u
            val a = k.fade(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            val len = m.sz * u
            val wd = len * 0.42f
            val ca = cols[i % cols.size]
            val cb = cols[(i + 3) % cols.size]
            c.save()
            c.translate(x, y)
            c.rotate(t * 14f * (if (i % 2 == 0) 1f else -1f) + m.ph * 360f)
            if (i % 3 == 2) {
                k.circ(c, 0f, 0f, wd * 0.85f, Color.WHITE, 245f * a)
                k.line(c, -wd * 0.7f, 0f, wd * 0.7f, 0f, 1f * u, Color.parseColor("#9FB4BC"), 220f * a)
            } else {
                k.rrect(c, -len / 2f, -wd / 2f, 0f, wd / 2f, wd / 2f, ca, 250f * a)
                k.rrect(c, 0f, -wd / 2f, len / 2f, wd / 2f, wd / 2f, cb, 250f * a)
                k.rrect(c, -len * 0.4f, -wd * 0.34f, len * 0.4f, -wd * 0.12f, wd * 0.1f, Color.WHITE, 110f * a)
            }
            c.restore()
        }

        // cruzinhas e bolhas subindo
        for (m in plusM) {
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1.06f - 1.12f * life)
            val x = w * m.x + sin(t * 0.7f + m.ph * 6.28f) * m.sw * u
            val a = k.fade(life) * k.edge(y, h)
            k.plus(c, x, y, m.sz * u, sin(t * 0.8f + m.ph * 5f) * 10f, Color.WHITE, 170f * a)
        }
        for (m in bubbles) {
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1.06f - 1.12f * life)
            val x = w * m.x + sin(t * 0.9f + m.ph * 6.28f) * m.sw * u
            val a = k.fade(life) * k.edge(y, h)
            k.ring(c, x, y, m.sz * u, 1f * u, Color.WHITE, 160f * a)
            k.circ(c, x - m.sz * u * 0.3f, y - m.sz * u * 0.3f, m.sz * u * 0.18f, Color.WHITE, 200f * a)
        }

        // monitor cardíaco em cima e embaixo
        trace(c, k, w, 20f * u, 11f * u, t, 44, Color.WHITE, 1.7f * u, true)
        trace(c, k, w, h - 18f * u, 8f * u, t + 0.6f, 36, Color.parseColor("#C9FFF0"), 1.5f * u, false)
        k.heart(c, 15f * u, h - 36f * u, 5f * u * (1f + 0.16f * beat), 0f, pinkR, 255f)
        k.label(c, "72", 33f * u, h - 36f * u, 11f * u, Color.WHITE, 245f, 0f, 1f)
        k.tidy()
    }
}

// ======================================================================================
// FAMÍLIA: lar aconchegante ao entardecer. Bandeirinhas, casa com fumaça e janelas
// acesas, família de mãos dadas, balões de coração, presentes e folhas caindo.
// ======================================================================================
class FamiliaScene : AuraScene {
    private var kit: SceneKit? = null
    private var balloons: List<SceneKit.Mote> = emptyList()
    private var leaves: List<SceneKit.Mote> = emptyList()
    private var flies: List<SceneKit.Mote> = emptyList()
    private var flagCols = IntArray(0)
    private val fx = floatArrayOf(-27f, -9f, 8f, 26f)
    private val fh = floatArrayOf(31f, 19f, 16f, 28f)

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        balloons = k.motes(5, 701L, 0.022f, 0.04f, 10f, 15f, 12f)
        leaves = k.motes(10, 702L, 0.03f, 0.06f, 5f, 9f, 24f)
        flies = k.motes(10, 703L, 0.2f, 0.5f, 1.4f, 2.4f, 0f)
        flagCols = intArrayOf(
            Color.parseColor("#FF7A6B"), Color.parseColor("#FFD166"), Color.parseColor("#7FD1AE"),
            Color.parseColor("#74B9FF"), Color.parseColor("#FF9CC0")
        )
    }

    private fun person(c: Canvas, k: SceneKit, x: Float, yb: Float, hh: Float, shirt: Int, hair: Int, dress: Boolean) {
        val skin = Color.parseColor("#FFD3B0")
        val pants = Color.parseColor("#5A4A6B")
        val hip = yb - 0.3f * hh
        val sh = yb - 0.7f * hh
        val hy = yb - 0.86f * hh
        k.oval(c, x - 0.3f * hh, yb - 0.03f * hh, x + 0.3f * hh, yb + 0.05f * hh, Color.parseColor("#3A5A28"), 90f)
        if (!dress) {
            k.line(c, x - 0.07f * hh, hip, x - 0.07f * hh, yb, 0.09f * hh, pants, 255f)
            k.line(c, x + 0.07f * hh, hip, x + 0.07f * hh, yb, 0.09f * hh, pants, 255f)
            k.rrect(c, x - 0.16f * hh, sh, x + 0.16f * hh, hip + 0.04f * hh, 0.06f * hh, shirt, 255f)
        } else {
            k.line(c, x - 0.05f * hh, yb - 0.15f * hh, x - 0.05f * hh, yb, 0.07f * hh, skin, 255f)
            k.line(c, x + 0.05f * hh, yb - 0.15f * hh, x + 0.05f * hh, yb, 0.07f * hh, skin, 255f)
            k.path.reset()
            k.path.moveTo(x - 0.13f * hh, sh)
            k.path.lineTo(x + 0.13f * hh, sh)
            k.path.lineTo(x + 0.27f * hh, yb - 0.14f * hh)
            k.path.lineTo(x - 0.27f * hh, yb - 0.14f * hh)
            k.path.close()
            k.p.style = Paint.Style.FILL
            k.p.color = k.al(shirt, 255f)
            c.drawPath(k.path, k.p)
        }
        k.circ(c, x, hy - 0.02f * hh, 0.155f * hh, hair, 255f)
        if (dress) k.oval(c, x - 0.17f * hh, hy - 0.04f * hh, x + 0.17f * hh, hy + 0.2f * hh, hair, 255f)
        k.circ(c, x, hy + 0.015f * hh, 0.135f * hh, skin, 255f)
        k.circ(c, x - 0.05f * hh, hy + 0.01f * hh, 0.017f * hh, Color.parseColor("#4A2A1A"), 255f)
        k.circ(c, x + 0.05f * hh, hy + 0.01f * hh, 0.017f * hh, Color.parseColor("#4A2A1A"), 255f)
        k.circ(c, x, hy + 0.06f * hh, 0.02f * hh, Color.parseColor("#E8808A"), 255f)
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        k.tidy()
        val u = k.u
        val p = k.p
        val cream = Color.parseColor("#FFF1DC")
        val terracotta = Color.parseColor("#D2593F")
        val warm = Color.parseColor("#FFE08A")
        val pinkH = Color.parseColor("#FF7A9C")

        k.vgrad(c, w, h, Color.parseColor("#FFE6BF"), Color.parseColor("#FFC58E"), Color.parseColor("#F59A6C"))
        val sx = w * 0.2f
        val sy = h * 0.3f
        k.soft(c, sx, sy, 100f * u, Color.parseColor("#FFF0C2"), 170f)
        k.circ(c, sx, sy, 14f * u, Color.parseColor("#FFF6D8"), 255f)

        k.posterRipple(c, poster, hasPoster, t, 3.6f, Color.WHITE, 130f)

        // colinas
        for (layer in 0..1) {
            k.path.reset()
            k.path.moveTo(0f, h)
            val base = if (layer == 0) h * 0.8f else h * 0.88f
            for (i in 0..14) {
                val x = w * i / 14f
                k.path.lineTo(x, base - (if (layer == 0) 9f else 6f) * u * sin(i * 0.6f + layer * 2f) - 3f * u * sin(i * 1.7f))
            }
            k.path.lineTo(w, h)
            k.path.close()
            p.style = Paint.Style.FILL
            p.color = k.al(if (layer == 0) Color.parseColor("#A9C97A") else Color.parseColor("#78B05A"), 255f)
            c.drawPath(k.path, p)
        }

        // árvore à direita com copa que balança
        val tx = w * 0.95f
        val tb = h * 0.9f
        k.rrect(c, tx - 3f * u, tb - 30f * u, tx + 3f * u, tb, 1.5f * u, Color.parseColor("#8A5A3C"), 255f)
        val tsw = sin(t * 0.6f) * 1.5f * u
        k.circ(c, tx + tsw, tb - 40f * u, 17f * u, Color.parseColor("#5E9C4A"), 255f)
        k.circ(c, tx - 11f * u + tsw, tb - 33f * u, 12f * u, Color.parseColor("#6BAE55"), 255f)
        k.circ(c, tx + 11f * u + tsw, tb - 34f * u, 11f * u, Color.parseColor("#4E8A3E"), 255f)
        for (i in 0 until 4) {
            k.circ(c, tx + (i - 1.5f) * 8f * u + tsw, tb - 38f * u + (i % 2) * 6f * u, 2.4f * u, Color.parseColor("#FF7A6B"), 255f)
        }

        // casa com fumaça
        val hx = w * 0.14f
        val hb = h * 0.9f
        k.rrect(c, hx + 11f * u, hb - 44f * u, hx + 19f * u, hb - 22f * u, 1f * u, Color.parseColor("#B4543C"), 255f)
        for (i in 0 until 5) {
            val life = k.frac(t * 0.13f + i / 5f)
            val sy2 = hb - 46f * u - life * 42f * u
            val sx2 = hx + 15f * u + sin(t * 0.9f + i * 1.3f) * 4f * u * life + life * 8f * u
            k.soft(c, sx2, sy2, (4f + 10f * life) * u, Color.WHITE, 170f * k.fade(life))
        }
        k.rrect(c, hx - 24f * u, hb - 30f * u, hx + 24f * u, hb, 1f * u, cream, 255f)
        k.path.reset()
        k.path.moveTo(hx - 30f * u, hb - 28f * u)
        k.path.lineTo(hx, hb - 54f * u)
        k.path.lineTo(hx + 30f * u, hb - 28f * u)
        k.path.close()
        p.style = Paint.Style.FILL
        p.color = k.al(terracotta, 255f)
        c.drawPath(k.path, p)
        k.heart(c, hx, hb - 38f * u, 4.6f * u, 0f, Color.parseColor("#FFE0B8"), 255f)
        k.rrect(c, hx - 5f * u, hb - 17f * u, hx + 5f * u, hb, 1.6f * u, Color.parseColor("#8C4A2F"), 255f)
        k.circ(c, hx + 3f * u, hb - 8f * u, 0.8f * u, warm, 255f)
        for (s in 0..1) {
            val wx = hx + (if (s == 0) -16f else 16f) * u
            val g = 0.75f + 0.25f * sin(t * 1.1f + s * 2f)
            k.soft(c, wx, hb - 18f * u, 15f * u, warm, 150f * g)
            k.rrect(c, wx - 5f * u, hb - 24f * u, wx + 5f * u, hb - 12f * u, 1f * u, warm, 255f)
            k.line(c, wx, hb - 24f * u, wx, hb - 12f * u, 0.9f * u, Color.parseColor("#8C4A2F"), 255f)
            k.line(c, wx - 5f * u, hb - 18f * u, wx + 5f * u, hb - 18f * u, 0.9f * u, Color.parseColor("#8C4A2F"), 255f)
        }
        // presentes ao lado da casa
        val gx = hx + 34f * u
        k.rrect(c, gx - 6f * u, hb - 11f * u, gx + 6f * u, hb, 1f * u, Color.parseColor("#74B9FF"), 255f)
        k.rrect(c, gx - 1f * u, hb - 11f * u, gx + 1f * u, hb, 0f, Color.WHITE, 255f)
        k.oval(c, gx - 4.5f * u, hb - 15f * u, gx, hb - 11f * u, Color.WHITE, 255f)
        k.oval(c, gx, hb - 15f * u, gx + 4.5f * u, hb - 11f * u, Color.WHITE, 255f)
        k.rrect(c, gx + 8f * u, hb - 8f * u, gx + 17f * u, hb, 1f * u, pinkH, 255f)
        k.rrect(c, gx + 11.5f * u, hb - 8f * u, gx + 13.5f * u, hb, 0f, Color.WHITE, 255f)

        // família de mãos dadas
        val fx0 = w * 0.8f
        val fb = h * 0.93f
        val sway = sin(t * 0.8f) * 1.2f
        c.save()
        c.rotate(sway, fx0, fb)
        val shirts = intArrayOf(Color.parseColor("#4A90D9"), Color.parseColor("#FFD166"), Color.parseColor("#7FD1AE"), Color.parseColor("#E8505B"))
        val hairs = intArrayOf(Color.parseColor("#3A2A22"), Color.parseColor("#6B3E26"), Color.parseColor("#2E2230"), Color.parseColor("#4A2A1A"))
        for (i in 0 until 4) {
            val hh = fh[i] * u
            val hx2 = fx0 + fx[i] * u
            val handY = fb - 0.42f * hh
            if (i < 3) {
                val nx = fx0 + fx[i + 1] * u
                val nh = fh[i + 1] * u
                k.line(c, hx2 + 0.17f * hh, handY, nx - 0.17f * nh, fb - 0.42f * nh, 0.06f * hh, Color.parseColor("#FFD3B0"), 255f)
            }
        }
        for (i in 0 until 4) {
            person(c, k, fx0 + fx[i] * u, fb, fh[i] * u, shirts[i], hairs[i], i == 3)
        }
        c.restore()
        // coraçõezinhos saindo da família
        for (i in 0 until 3) {
            val life = k.frac(t * 0.18f + i / 3f)
            val x = fx0 + (i - 1) * 18f * u + sin(t + i) * 3f * u
            val y = fb - 40f * u - life * 38f * u
            k.heart(c, x, y, 4f * u, sin(t * 1.4f + i) * 10f, pinkH, 240f * k.fade(life))
        }

        // balões de coração
        for (i in balloons.indices) {
            val m = balloons[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1.2f - 1.4f * life)
            val x = w * (0.3f + 0.4f * m.x) + sin(t * 0.7f + m.ph * 6.28f) * m.sw * u
            val r = m.sz * u * 0.55f
            k.path.reset()
            k.path.moveTo(x, y + r)
            for (j in 1..6) k.path.lineTo(x + sin(t * 1.8f + j * 0.9f + i) * 2f * u * j / 6f, y + r + j * 5f * u)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 0.9f * u
            p.strokeCap = Paint.Cap.ROUND
            p.color = k.al(Color.WHITE, 200f)
            c.drawPath(k.path, p)
            k.heart(c, x, y, r, sin(t * 0.8f + i) * 8f, flagCols[i % flagCols.size], 255f)
        }

        // folhas de outono
        for (i in leaves.indices) {
            val m = leaves[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (-0.06f + 1.1f * life)
            val x = w * m.x + sin(t * 0.7f + m.ph * 6.28f) * m.sw * u
            val a = k.fade(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            val col = if (i % 3 == 0) Color.parseColor("#F28A3C") else if (i % 3 == 1) Color.parseColor("#E0563C") else Color.parseColor("#F2C14E")
            k.leaf(c, x, y, m.sz * u, t * 40f * (if (i % 2 == 0) 1f else -1f) + m.ph * 360f, abs(sin(t * 1.2f + m.ph * 7f)), col, Color.parseColor("#8A3A22"), 240f * a)
        }

        // vagalumes quentinhos
        for (m in flies) {
            val x = w * (0.5f + 0.46f * sin(t * m.sp * 0.8f + m.ph * 6.28f))
            val y = h * (0.55f + 0.3f * sin(t * m.sp * 0.6f + m.ph * 9f))
            val g = 0.5f + 0.5f * sin(t * 1.6f + m.ph * 12f)
            k.soft(c, x, y, m.sz * u * 4f, warm, 110f * g * k.edge(y, h))
            k.circ(c, x, y, m.sz * u * 0.6f, Color.WHITE, 230f * g * k.edge(y, h))
        }

        // bandeirinhas de festa no alto
        val sag = 10f * u
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f * u
        p.color = k.al(Color.parseColor("#8A5A3C"), 230f)
        k.path.reset()
        for (s in 0..20) {
            val f = s / 20f
            val yy = 3f * u + sag * sin(f * 3.1416f)
            if (s == 0) k.path.moveTo(w * f, yy) else k.path.lineTo(w * f, yy)
        }
        c.drawPath(k.path, p)
        val nf = 14
        for (i in 0 until nf) {
            val f = (i + 0.5f) / nf
            val x = w * f
            val y = 3f * u + sag * sin(f * 3.1416f)
            c.save()
            c.translate(x, y)
            c.rotate(sin(t * 1.2f + i * 0.8f) * 5f)
            k.path.reset()
            k.path.moveTo(-6f * u, 0f)
            k.path.lineTo(6f * u, 0f)
            k.path.lineTo(0f, 13f * u)
            k.path.close()
            p.style = Paint.Style.FILL
            p.color = k.al(flagCols[i % flagCols.size], 255f)
            c.drawPath(k.path, p)
            k.circ(c, 0f, 4f * u, 1.1f * u, Color.WHITE, 200f)
            c.restore()
        }
        k.tidy()
    }
}
