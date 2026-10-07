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

/** Cenas refeitas: Fantasia e Histórico (formas desenhadas por código, sem piscar). A de Romance agora fica em AuraKawaii.kt. */

// ======================================================================================
// FANTASIA: reino encantado à noite. Aurora, lua crescente, círculo mágico, castelo,
// fadas com rastro, borboletas, cristais flutuando e cogumelos brilhantes.
// ======================================================================================
class FantasiaScene : AuraScene {
    private var kit: SceneKit? = null
    private var stars: List<SceneKit.Mote> = emptyList()
    private var dust: List<SceneKit.Mote> = emptyList()
    private var crystals: List<SceneKit.Mote> = emptyList()

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        stars = k.motes(34, 301L, 0.3f, 0.9f, 2f, 5f, 0f)
        dust = k.motes(22, 302L, 0.03f, 0.06f, 2f, 4.5f, 22f)
        crystals = k.motes(5, 303L, 0.2f, 0.5f, 7f, 12f, 8f)
    }

    private fun ribbon(c: Canvas, k: SceneKit, w: Float, base: Float, amp: Float, f1: Float, sp: Float, ph: Float, t: Float, col: Int, wide: Float, a: Float) {
        val p = k.p
        k.path.reset()
        val steps = 18
        for (i in 0..steps) {
            val x = w * i / steps
            val y = base + amp * sin(x * f1 + t * sp + ph) + amp * 0.45f * sin(x * f1 * 2.3f - t * sp * 0.7f + ph * 2f)
            if (i == 0) k.path.moveTo(x, y) else k.path.lineTo(x, y)
        }
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.strokeWidth = wide * 1.9f
        p.color = k.al(col, a * 0.35f)
        c.drawPath(k.path, p)
        p.strokeWidth = wide
        p.color = k.al(col, a)
        c.drawPath(k.path, p)
    }

    private fun fairy(c: Canvas, k: SceneKit, x: Float, y: Float, s: Float, t: Float, ph: Float, col: Int) {
        val u = k.u
        val flap = 0.5f + 0.5f * sin(t * 14f + ph * 6f)
        k.soft(c, x, y, s * 3.4f, col, 110f)
        for (side in 0..1) {
            val sg = if (side == 0) 1f else -1f
            c.save()
            c.translate(x, y)
            c.rotate(sg * (18f + 22f * flap))
            k.oval(c, 0f, -s * 1.7f, sg * s * 0.95f, -s * 0.1f, Color.WHITE, 170f)
            c.restore()
        }
        k.circ(c, x, y, s * 0.62f, Color.WHITE, 255f)
        k.circ(c, x, y, s * 0.34f, col, 255f)
        if (u > 0f) k.sparkle(c, x + s * 1.3f, y - s * 1.1f, s * 0.7f, t * 40f, Color.WHITE, 120f + 100f * flap)
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        k.tidy()
        val u = k.u
        val p = k.p
        val mint = Color.parseColor("#7FFFE0")
        val sky = Color.parseColor("#8FC8FF")
        val magenta = Color.parseColor("#E39BFF")
        val gold = Color.parseColor("#FFE9A0")
        val cx = if (hasPoster) poster.centerX() else w / 2f
        val cy = if (hasPoster) poster.centerY() else h / 2f

        k.vgrad(c, w, h, Color.parseColor("#0C1348"), Color.parseColor("#34308F"), Color.parseColor("#7C63D6"))
        k.soft(c, w * 0.5f, h * 1.02f, max(w, h) * 0.8f, magenta, 90f)

        // estrelas
        for (i in stars.indices) {
            val m = stars[i]
            val y = h * m.y * 0.75f
            val a = (0.3f + 0.7f * k.tw(t, m.ph, m.sp * 2f)) * k.edge(y, h)
            if (i % 5 == 0) k.sparkle(c, w * m.x, y, m.sz * u * (0.6f + 0.5f * a), t * 8f + m.ph * 90f, Color.WHITE, 230f * a)
            else k.circ(c, w * m.x, y, m.sz * u * 0.32f, Color.WHITE, 220f * a)
        }

        // lua crescente
        val mx = w * 0.84f
        val my = h * 0.17f
        k.soft(c, mx, my, 54f * u, Color.parseColor("#FFF5C9"), 110f)
        k.crescent(c, mx, my, 21f * u, -20f, Color.parseColor("#FFF6D6"), 255f)

        // fitas de aurora
        ribbon(c, k, w, h * 0.22f, 13f * u, 0.016f, 0.45f, 0f, t, mint, 13f * u, 52f)
        ribbon(c, k, w, h * 0.3f, 15f * u, 0.013f, -0.35f, 2f, t, magenta, 12f * u, 46f)
        ribbon(c, k, w, h * 0.38f, 11f * u, 0.019f, 0.3f, 4f, t, sky, 10f * u, 40f)

        // círculo mágico atrás da capa
        val pr = if (hasPoster) k.dist(poster.left, poster.top, poster.right, poster.bottom) / 2f else min(w, h) * 0.4f
        val rr = pr * 1.02f + 10f * u
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = 1.5f * u
        p.color = k.al(mint, 120f)
        c.drawCircle(cx, cy, rr, p)
        p.strokeWidth = 0.9f * u
        p.color = k.al(mint, 80f)
        c.drawCircle(cx, cy, rr * 0.9f, p)
        for (i in 0 until 36) {
            val an = (i * 10f + t * 6f) * 0.0174533f
            val l0 = if (i % 3 == 0) 0.9f else 0.94f
            k.line(c, cx + cos(an) * rr * l0, cy + sin(an) * rr * l0, cx + cos(an) * rr, cy + sin(an) * rr, 1f * u, mint, 90f)
        }
        for (tri in 0..1) {
            val dir = if (tri == 0) 1f else -1f
            k.path.reset()
            for (i in 0..3) {
                val an = (i * 120f + tri * 60f) * 0.0174533f + t * 0.12f * dir
                val x = cx + cos(an) * rr * 0.9f
                val y = cy + sin(an) * rr * 0.9f
                if (i == 0) k.path.moveTo(x, y) else k.path.lineTo(x, y)
            }
            p.strokeWidth = 1.1f * u
            p.color = k.al(if (tri == 0) mint else magenta, 105f)
            c.drawPath(k.path, p)
        }
        for (i in 0 until 6) {
            val an = (i * 60f) * 0.0174533f + t * 0.12f
            val x = cx + cos(an) * rr * 0.9f
            val y = cy + sin(an) * rr * 0.9f
            val g = 0.5f + 0.5f * sin(t * 1.4f + i)
            k.soft(c, x, y, 9f * u, mint, 70f + 80f * g)
            k.circ(c, x, y, 1.8f * u, Color.WHITE, 230f)
        }

        // colinas
        val far = Color.parseColor("#2A2A86")
        val near = Color.parseColor("#171A5C")
        k.path.reset()
        k.path.moveTo(0f, h)
        for (i in 0..14) {
            val x = w * i / 14f
            k.path.lineTo(x, h * 0.84f - 9f * u * sin(i * 0.55f + 1f) - 4f * u * sin(i * 1.3f))
        }
        k.path.lineTo(w, h)
        k.path.close()
        p.style = Paint.Style.FILL
        p.color = k.al(far, 255f)
        c.drawPath(k.path, p)

        // castelo na colina da esquerda
        val bx = w * 0.14f
        val by = h * 0.86f
        val cs = Color.parseColor("#1B1D68")
        val win = Color.parseColor("#FFE9A0")
        k.rrect(c, bx - 20f * u, by - 26f * u, bx + 20f * u, by + 4f * u, 1f * u, cs, 255f)
        val tws = floatArrayOf(-22f, 0f, 22f)
        val ths = floatArrayOf(40f, 56f, 36f)
        for (i in 0 until 3) {
            val tx = bx + tws[i] * u
            val th = ths[i] * u
            k.rrect(c, tx - 6f * u, by - th, tx + 6f * u, by + 4f * u, 1f * u, cs, 255f)
            k.path.reset()
            k.path.moveTo(tx - 8f * u, by - th)
            k.path.lineTo(tx, by - th - 16f * u)
            k.path.lineTo(tx + 8f * u, by - th)
            k.path.close()
            p.style = Paint.Style.FILL
            p.color = k.al(Color.parseColor("#4A3CB0"), 255f)
            c.drawPath(k.path, p)
            val g = 0.65f + 0.35f * sin(t * 1.2f + i * 2f)
            k.soft(c, tx, by - th + 9f * u, 8f * u, win, 130f * g)
            k.rrect(c, tx - 1.6f * u, by - th + 5f * u, tx + 1.6f * u, by - th + 11f * u, 1.6f * u, win, 255f)
        }
        val fx = bx
        val fy = by - ths[1] * u - 16f * u
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f * u
        p.color = k.al(Color.WHITE, 220f)
        c.drawLine(fx, fy, fx, fy - 9f * u, p)
        k.path.reset()
        k.path.moveTo(fx, fy - 9f * u)
        k.path.lineTo(fx + 8f * u + 2f * u * sin(t * 2.4f), fy - 7f * u)
        k.path.lineTo(fx, fy - 4.5f * u)
        p.style = Paint.Style.FILL
        p.color = k.al(magenta, 255f)
        c.drawPath(k.path, p)
        k.rrect(c, bx - 3f * u, by - 9f * u, bx + 3f * u, by + 4f * u, 3f * u, Color.parseColor("#0F1148"), 255f)

        // colina da frente
        k.path.reset()
        k.path.moveTo(0f, h)
        for (i in 0..14) {
            val x = w * i / 14f
            k.path.lineTo(x, h * 0.93f - 6f * u * sin(i * 0.8f + 2f) - 3f * u * sin(i * 1.9f))
        }
        k.path.lineTo(w, h)
        k.path.close()
        p.color = k.al(near, 255f)
        c.drawPath(k.path, p)

        // cogumelos brilhantes
        for (i in 0 until 4) {
            val mxp = w * (0.66f + 0.075f * i)
            val myp = h * 0.935f - (i % 2) * 2f * u
            val g = 0.5f + 0.5f * sin(t * 1.3f + i * 1.4f)
            k.soft(c, mxp, myp - 4f * u, 15f * u, mint, 60f + 70f * g)
            k.rrect(c, mxp - 1.4f * u, myp - 5f * u, mxp + 1.4f * u, myp + 2f * u, 1f * u, Color.parseColor("#E6F8F2"), 255f)
            k.oval(c, mxp - 6f * u, myp - 10f * u, mxp + 6f * u, myp - 1f * u, if (i % 2 == 0) magenta else mint, 255f)
            k.circ(c, mxp - 2f * u, myp - 6f * u, 1.1f * u, Color.WHITE, 230f)
            k.circ(c, mxp + 2.4f * u, myp - 5f * u, 0.9f * u, Color.WHITE, 230f)
        }

        // cristais flutuando
        for (i in crystals.indices) {
            val m = crystals[i]
            val x = w * (0.08f + 0.84f * m.x)
            val y = h * (0.45f + 0.35f * m.y) + sin(t * m.sp * 3f + m.ph * 6f) * 6f * u
            val a = k.edge(y, h)
            k.gem(c, x, y, m.sz * u, sin(t * 0.7f + m.ph * 5f) * 14f, if (i % 2 == 0) mint else magenta, 235f * a)
        }

        // fadas com rastro
        for (f in 0 until 3) {
            val sp = 0.22f + f * 0.07f
            val col = if (f == 1) magenta else mint
            for (q in 6 downTo 1) {
                val tt = t - q * 0.12f
                val x = w * (0.5f + 0.42f * sin(tt * sp + f * 2.1f))
                val y = h * (0.5f + 0.34f * sin(tt * sp * 1.4f + f * 1.3f + 1f))
                k.soft(c, x, y, (2.4f + (6 - q) * 0.9f) * u, col, 110f - q * 14f)
            }
            val x = w * (0.5f + 0.42f * sin(t * sp + f * 2.1f))
            val y = h * (0.5f + 0.34f * sin(t * sp * 1.4f + f * 1.3f + 1f))
            fairy(c, k, x, y, 3.2f * u, t, f * 0.31f, col)
        }

        // borboletas
        for (b in 0 until 3) {
            val sp = 0.13f + b * 0.04f
            val x = w * (0.5f + 0.44f * sin(t * sp + b * 2.7f + 0.5f))
            val y = h * (0.5f + 0.36f * sin(t * sp * 1.3f + b * 1.9f))
            val flap = 0.5f + 0.5f * sin(t * 8f + b * 2f)
            k.butterfly(c, x, y, 9f * u, sin(t * sp * 2f + b) * 20f, flap, if (b == 1) mint else magenta, if (b == 1) sky else gold, 245f * k.edge(y, h))
        }

        // glitter caindo
        for (m in dust) {
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (-0.05f + 1.1f * life)
            val x = w * m.x + sin(t * 0.6f + m.ph * 6f) * m.sw * u
            val a = k.fade(life) * k.edge(y, h)
            k.sparkle(c, x, y, m.sz * u, t * 30f + m.ph * 90f, gold, 230f * a)
        }
        k.tidy()
    }
}

// ======================================================================================
// HISTÓRICO: palácio ao entardecer. Sol enorme, montanhas em camadas, pagode e hanok,
// lanternas penduradas, grous voando, névoa e pétalas de ameixeira.
// ======================================================================================
class HistoricoScene : AuraScene {
    private var kit: SceneKit? = null
    private var petals: List<SceneKit.Mote> = emptyList()
    private var gold: List<SceneKit.Mote> = emptyList()
    private var mist: List<SceneKit.Mote> = emptyList()

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        petals = k.motes(14, 401L, 0.028f, 0.055f, 7f, 12f, 28f)
        gold = k.motes(16, 402L, 0.03f, 0.07f, 1.6f, 3.2f, 14f)
        mist = k.motes(4, 403L, 0.012f, 0.025f, 70f, 110f, 0f)
    }

    private fun ridge(c: Canvas, k: SceneKit, w: Float, h: Float, base: Float, amp: Float, f1: Float, f2: Float, ph: Float, col: Int) {
        k.path.reset()
        k.path.moveTo(0f, h)
        val steps = 24
        for (i in 0..steps) {
            val x = w * i / steps
            val y = base - amp * (0.55f + 0.45f * sin(x * f1 + ph)) - amp * 0.4f * sin(x * f2 + ph * 2.3f)
            k.path.lineTo(x, y)
        }
        k.path.lineTo(w, h)
        k.path.close()
        k.p.style = Paint.Style.FILL
        k.p.color = k.al(col, 255f)
        c.drawPath(k.path, k.p)
    }

    private fun roof(c: Canvas, k: SceneKit, x: Float, y: Float, s: Float, col: Int) {
        // telhado de hanok: forma unitária desenhada à mão
        val u = k.u
        k.path.reset()
        k.path.moveTo(x - 0.55f * s, y - 0.95f * s)
        k.path.lineTo(x + 0.55f * s, y - 0.95f * s)
        k.path.quadTo(x + 0.75f * s, y - 0.05f * s, x + 1.4f * s, y + 0.02f * s)
        k.path.quadTo(x + 0.7f * s, y + 0.4f * s, x, y + 0.4f * s)
        k.path.quadTo(x - 0.7f * s, y + 0.4f * s, x - 1.4f * s, y + 0.02f * s)
        k.path.quadTo(x - 0.75f * s, y - 0.05f * s, x - 0.55f * s, y - 0.95f * s)
        k.path.close()
        k.p.style = Paint.Style.FILL
        k.p.color = k.al(col, 255f)
        c.drawPath(k.path, k.p)
        k.line(c, x - 0.55f * s, y - 0.95f * s, x + 0.55f * s, y - 0.95f * s, 1.4f * u, Color.parseColor("#B9667F"), 200f)
    }

    private fun lantern(c: Canvas, k: SceneKit, x: Float, y: Float, s: Float, glowA: Float) {
        val red = Color.parseColor("#E5384F")
        val dark = Color.parseColor("#B01E3A")
        val goldC = Color.parseColor("#F2C063")
        k.soft(c, x, y + s * 1.2f, s * 3.4f, Color.parseColor("#FFC27A"), glowA)
        k.rrect(c, x - s * 0.5f, y, x + s * 0.5f, y + s * 0.3f, s * 0.1f, goldC, 255f)
        k.oval(c, x - s * 0.82f, y + s * 0.15f, x + s * 0.82f, y + s * 2.35f, red, 255f)
        for (i in -1..1) {
            val ww = s * 0.4f * i
            k.p.style = Paint.Style.STROKE
            k.p.strokeWidth = 0.9f * k.u
            k.p.color = k.al(dark, 230f)
            k.rect.set(x - abs(ww) - s * 0.05f, y + s * 0.15f, x + abs(ww) + s * 0.05f, y + s * 2.35f)
            if (i != 0) c.drawOval(k.rect, k.p)
        }
        k.line(c, x, y + s * 0.15f, x, y + s * 2.35f, 0.9f * k.u, dark, 230f)
        k.rrect(c, x - s * 0.5f, y + s * 2.2f, x + s * 0.5f, y + s * 2.5f, s * 0.1f, goldC, 255f)
        k.line(c, x, y + s * 2.5f, x, y + s * 3.4f, 1.1f * k.u, goldC, 255f)
        k.circ(c, x, y + s * 3.45f, s * 0.2f, goldC, 255f)
    }

    private fun crane(c: Canvas, k: SceneKit, x: Float, y: Float, s: Float, flap: Float, a: Float) {
        val white = Color.parseColor("#FFF7F2")
        val u = k.u
        val wingA = -10f - 38f * flap
        // asa de trás
        c.save()
        c.translate(x, y)
        c.rotate(wingA)
        k.path.reset()
        k.path.moveTo(0f, 0f)
        k.path.quadTo(-s * 0.4f, -s * 1.5f, -s * 1.5f, -s * 1.2f)
        k.path.quadTo(-s * 0.9f, -s * 0.7f, -s * 0.6f, 0f)
        k.path.close()
        k.p.style = Paint.Style.FILL
        k.p.color = k.al(Color.parseColor("#E4D4D0"), a)
        c.drawPath(k.path, k.p)
        c.restore()
        // corpo, pescoço, cabeça e pernas
        k.oval(c, x - s * 0.9f, y - s * 0.22f, x + s * 0.7f, y + s * 0.3f, white, a)
        k.p.style = Paint.Style.STROKE
        k.p.strokeCap = Paint.Cap.ROUND
        k.p.strokeWidth = s * 0.22f
        k.p.color = k.al(white, a)
        k.path.reset()
        k.path.moveTo(x + s * 0.55f, y)
        k.path.quadTo(x + s * 1.05f, y - s * 0.1f, x + s * 1.1f, y - s * 0.55f)
        c.drawPath(k.path, k.p)
        k.circ(c, x + s * 1.12f, y - s * 0.62f, s * 0.17f, white, a)
        k.circ(c, x + s * 1.12f, y - s * 0.76f, s * 0.12f, Color.parseColor("#E5384F"), a)
        k.line(c, x + s * 1.25f, y - s * 0.62f, x + s * 1.65f, y - s * 0.55f, 1.1f * u, Color.parseColor("#3A2A30"), a)
        k.line(c, x - s * 0.8f, y + s * 0.15f, x - s * 1.9f, y + s * 0.35f, 0.9f * u, Color.parseColor("#3A2A30"), a)
        // asa da frente
        c.save()
        c.translate(x, y)
        c.rotate(wingA * 0.8f + 8f)
        k.path.reset()
        k.path.moveTo(0f, 0f)
        k.path.quadTo(s * 0.5f, -s * 1.7f, -s * 1.1f, -s * 1.65f)
        k.path.quadTo(-s * 0.5f, -s * 1.0f, -s * 0.4f, 0f)
        k.path.close()
        k.p.style = Paint.Style.FILL
        k.p.color = k.al(white, a)
        c.drawPath(k.path, k.p)
        k.p.color = k.al(Color.parseColor("#2A2230"), a)
        k.path.reset()
        k.path.moveTo(-s * 1.1f, -s * 1.65f)
        k.path.quadTo(-s * 0.8f, -s * 1.3f, -s * 0.62f, -s * 1.05f)
        k.path.lineTo(-s * 1.0f, -s * 1.2f)
        k.path.close()
        c.drawPath(k.path, k.p)
        c.restore()
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        k.tidy()
        val u = k.u
        val p = k.p
        val sun = Color.parseColor("#FFE7B0")
        val amber = Color.parseColor("#FFB070")
        val plum = Color.parseColor("#3A1A4A")

        k.vgrad(c, w, h, Color.parseColor("#4F2466"), Color.parseColor("#DE5470"), Color.parseColor("#FFB27A"))

        // sol enorme com halo
        val sx = w * 0.7f
        val sy = h * 0.34f
        k.soft(c, sx, sy, 110f * u, amber, 170f)
        k.circ(c, sx, sy, 34f * u, Color.parseColor("#FFD08A"), 255f)
        k.circ(c, sx, sy, 28f * u, sun, 255f)
        k.circ(c, sx, sy, 20f * u, Color.parseColor("#FFF6DC"), 255f)

        // montanhas em camadas com névoa entre elas
        ridge(c, k, w, h, h * 0.68f, 30f * u, 0.011f, 0.027f, 0.4f, Color.parseColor("#C8648B"))
        for (m in mist) {
            val x = ((m.x + t * m.sp) % 1f) * (w + 2f * m.sz * u) - m.sz * u
            k.softOval(c, x, h * (0.6f + 0.1f * m.y), m.sz * u, 14f * u, Color.parseColor("#FFD2B8"), 120f)
        }
        ridge(c, k, w, h, h * 0.78f, 26f * u, 0.014f, 0.033f, 1.9f, Color.parseColor("#9A4378"))
        for (m in mist) {
            val x = ((m.x * 0.7f + 0.3f + t * m.sp * 1.4f) % 1f) * (w + 2f * m.sz * u) - m.sz * u
            k.softOval(c, x, h * (0.76f + 0.08f * m.y), m.sz * u, 12f * u, Color.parseColor("#F5B3B0"), 100f)
        }
        ridge(c, k, w, h, h * 0.9f, 20f * u, 0.017f, 0.04f, 3.3f, Color.parseColor("#6A2C63"))

        // pagode à esquerda
        val px = w * 0.12f
        val pb = h * 0.9f
        val tiers = 4
        for (i in 0 until tiers) {
            val s = (15f - i * 2.6f) * u
            val y = pb - i * 17f * u
            k.rrect(c, px - s * 0.62f, y - 14f * u, px + s * 0.62f, y, 0.5f * u, plum, 255f)
            val g = 0.7f + 0.3f * sin(t * 1.1f + i * 1.7f)
            k.soft(c, px, y - 7f * u, 9f * u, Color.parseColor("#FFC27A"), 100f * g)
            k.rrect(c, px - 1.3f * u, y - 11f * u, px + 1.3f * u, y - 4f * u, 0.8f * u, Color.parseColor("#FFD79A"), 255f)
            roof(c, k, px, y - 14f * u, s * 0.72f, Color.parseColor("#2C1239"))
        }
        k.line(c, px, pb - tiers * 17f * u - 8f * u, px, pb - tiers * 17f * u - 24f * u, 1.5f * u, Color.parseColor("#2C1239"), 255f)
        k.circ(c, px, pb - tiers * 17f * u - 18f * u, 1.8f * u, Color.parseColor("#F2C063"), 255f)

        // salão de hanok à direita
        val hx = w * 0.84f
        val hb = h * 0.93f
        k.rrect(c, hx - 24f * u, hb - 22f * u, hx + 24f * u, hb, 0.5f * u, plum, 255f)
        for (i in 0 until 4) {
            val wx = hx - 18f * u + i * 12f * u
            val g = 0.7f + 0.3f * sin(t * 0.9f + i * 1.3f)
            k.soft(c, wx + 2.5f * u, hb - 11f * u, 11f * u, Color.parseColor("#FFC27A"), 110f * g)
            k.rrect(c, wx, hb - 18f * u, wx + 5f * u, hb - 2f * u, 0.8f * u, Color.parseColor("#FFD79A"), 255f)
            k.line(c, wx + 2.5f * u, hb - 18f * u, wx + 2.5f * u, hb - 2f * u, 0.6f * u, plum, 255f)
        }
        roof(c, k, hx, hb - 22f * u, 26f * u, Color.parseColor("#2C1239"))

        // lanternas penduradas numa corda
        val sag = 12f * u
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.3f * u
        p.color = k.al(Color.parseColor("#3A1A2A"), 230f)
        k.path.reset()
        for (s in 0..20) {
            val fx = s / 20f
            val x = w * fx
            val y = 2f * u + sag * sin(fx * 3.1416f)
            if (s == 0) k.path.moveTo(x, y) else k.path.lineTo(x, y)
        }
        c.drawPath(k.path, p)
        val nL = 7
        for (i in 0 until nL) {
            val fx = (i + 0.5f) / nL
            val x = w * fx
            val y = 2f * u + sag * sin(fx * 3.1416f)
            val sw = sin(t * 0.9f + i * 1.1f) * 3.5f
            c.save()
            c.translate(x, y)
            c.rotate(sw)
            lantern(c, k, 0f, 0f, 5.2f * u, 95f + 40f * sin(t * 1.3f + i))
            c.restore()
        }

        // grous voando
        for (i in 0 until 2) {
            val life = k.frac(t * (0.018f + i * 0.006f) + i * 0.45f)
            val x = w * (1.15f - 1.3f * life)
            val y = h * (0.2f + 0.1f * i) + sin(t * 0.8f + i * 2f) * 6f * u
            val flap = 0.5f + 0.5f * sin(t * 3.2f + i * 1.7f)
            c.save()
            c.translate(x, y)
            c.scale(-1f, 1f)
            crane(c, k, 0f, 0f, 7.5f * u, flap, 250f * k.edge(y, h))
            c.restore()
        }

        // pétalas de ameixeira
        for (i in petals.indices) {
            val m = petals[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (-0.08f + 1.16f * life)
            val x = w * (m.x + 0.12f * life) + sin(t * 0.6f + m.ph * 6.28f) * m.sw * u
            val a = k.fade(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            val flip = abs(sin(t * 1.0f + m.ph * 7f))
            k.petal(c, x, y, m.sz * u, t * 25f + m.ph * 360f, flip, if (i % 2 == 0) Color.parseColor("#FFE3EA") else Color.parseColor("#FFC1D2"), 240f * a)
        }

        // brasas douradas subindo
        for (m in gold) {
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1.05f - 1.1f * life)
            val x = w * m.x + sin(t * 0.7f + m.ph * 6.28f) * m.sw * u
            val a = k.fade(life) * k.edge(y, h)
            k.soft(c, x, y, m.sz * u * 3f, Color.parseColor("#FFD08A"), 160f * a)
            k.circ(c, x, y, m.sz * u * 0.55f, Color.WHITE, 230f * a)
        }
        k.tidy()
    }
}
