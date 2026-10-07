package com.doramabloom.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Cenas refeitas: Vida real, Vingança e Crime (formas desenhadas por código, sem piscar). */

// ======================================================================================
// VIDA REAL: manhã de cidade. Sol nascendo atrás dos prédios, janelas acendendo aos poucos,
// nuvens, passarinhos, xícara de café com vapor, plantinha e folhas ao vento.
// ======================================================================================
class VidaScene : AuraScene {
    private var kit: SceneKit? = null
    private var leaves: List<SceneKit.Mote> = emptyList()
    private var motes: List<SceneKit.Mote> = emptyList()

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        leaves = k.motes(9, 801L, 0.03f, 0.06f, 5f, 8f, 26f)
        motes = k.motes(14, 802L, 0.02f, 0.05f, 1.2f, 2.4f, 12f)
    }

    private fun skyline(c: Canvas, k: SceneKit, w: Float, h: Float, base: Float, seed: Int, minW: Float, maxW: Float, minH: Float, maxH: Float, col: Int, winOn: Boolean, wake: Float) {
        val u = k.u
        var x = -4f * u
        var i = 0
        while (x < w + 4f * u) {
            val bw = (minW + (maxW - minW) * k.hash(seed + i * 3)) * u
            val bh = (minH + (maxH - minH) * k.hash(seed + i * 3 + 1)) * u
            k.rrect(c, x, base - bh, x + bw, h, 1.2f * u, col, 255f)
            if (winOn) {
                val cols = max(1, (bw / (6.2f * u)).toInt())
                val rows = max(1, ((bh - 5f * u) / (7f * u)).toInt())
                for (r in 0 until min(rows, 5)) {
                    for (q in 0 until min(cols, 3)) {
                        val key = seed * 7 + i * 31 + r * 5 + q
                        val th = k.hash(key)
                        val on = k.ss((wake - th) * 6f)
                        val wx = x + (q + 0.5f) * bw / cols
                        val wy = base - bh + 6f * u + r * 7f * u
                        k.rrect(c, wx - 1.6f * u, wy, wx + 1.6f * u, wy + 3.6f * u, 0.5f * u, Color.parseColor("#FFE8A8"), 40f + 215f * on)
                    }
                }
            }
            x += bw + 1.2f * u
            i++
        }
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        k.tidy()
        val u = k.u
        val p = k.p
        val cream = Color.parseColor("#FFF6EE")
        val brown = Color.parseColor("#6B4630")

        k.vgrad(c, w, h, Color.parseColor("#BFE3F5"), Color.parseColor("#FFE3C9"), Color.parseColor("#FFC9A8"))

        // sol nascendo
        val sx = w * 0.74f
        val sy = h * 0.62f
        k.soft(c, sx, sy, 120f * u, Color.parseColor("#FFE2B0"), 190f)
        k.circ(c, sx, sy, 22f * u, Color.parseColor("#FFD59A"), 255f)
        k.circ(c, sx, sy, 17f * u, Color.parseColor("#FFF1CF"), 255f)
        for (i in 0 until 10) {
            val an = (i * 36f + t * 3f) * 0.0174533f
            val a = 0.5f + 0.5f * sin(t * 0.8f + i)
            k.line(c, sx + cos(an) * 26f * u, sy + sin(an) * 26f * u, sx + cos(an) * 38f * u, sy + sin(an) * 38f * u, 1.6f * u, Color.parseColor("#FFE2B0"), 70f + 70f * a)
        }

        // nuvens
        for (i in 0 until 3) {
            val sp = 0.006f + i * 0.003f
            val x = ((0.2f + i * 0.37f + t * sp) % 1f) * (w + 120f * u) - 60f * u
            val y = h * (0.14f + 0.1f * i)
            k.cloud(c, x, y, (34f + 10f * i) * u, cream, 255f)
        }

        k.posterRipple(c, poster, hasPoster, t, 3.8f, Color.WHITE, 120f)

        // passarinhos
        for (i in 0 until 4) {
            val life = k.frac(t * (0.03f + 0.008f * i) + i * 0.27f)
            val x = w * (1.1f - 1.2f * life)
            val y = h * (0.2f + 0.07f * i) + sin(t * 1.1f + i * 2f) * 6f * u
            val flap = sin(t * 7f + i * 1.7f)
            p.style = Paint.Style.STROKE
            p.strokeCap = Paint.Cap.ROUND
            p.strokeJoin = Paint.Join.ROUND
            p.strokeWidth = 1.6f * u
            p.color = k.al(Color.parseColor("#5A4A52"), 235f * k.edge(y, h))
            k.path.reset()
            k.path.moveTo(x - 8f * u, y - 2f * u - flap * 4f * u)
            k.path.quadTo(x - 3f * u, y + 1f * u, x, y)
            k.path.quadTo(x + 3f * u, y + 1f * u, x + 8f * u, y - 2f * u - flap * 4f * u)
            c.drawPath(k.path, p)
        }

        // cidade acordando: as janelas acendem e apagam devagar
        val wake = 0.5f + 0.4f * sin(t * 0.18f)
        skyline(c, k, w, h, h * 0.78f, 11, 14f, 26f, 18f, 44f, Color.parseColor("#E9B7A0"), false, wake)
        skyline(c, k, w, h, h * 0.86f, 53, 12f, 24f, 14f, 36f, Color.parseColor("#C98F86"), true, wake)
        skyline(c, k, w, h, h * 0.93f, 97, 14f, 28f, 10f, 26f, Color.parseColor("#8E5F69"), true, wake)

        // xícara de café com latte art e vapor
        val cx = w * 0.15f
        val cy = h * 0.84f
        k.oval(c, cx - 19f * u, cy + 8f * u, cx + 19f * u, cy + 14f * u, Color.parseColor("#F4E6DA"), 255f)
        k.p.style = Paint.Style.STROKE
        k.p.strokeWidth = 3f * u
        k.p.color = k.al(cream, 255f)
        k.rect.set(cx + 8f * u, cy - 6f * u, cx + 20f * u, cy + 6f * u)
        c.drawArc(k.rect, -90f, 180f, false, k.p)
        k.path.reset()
        k.path.moveTo(cx - 14f * u, cy - 8f * u)
        k.path.lineTo(cx + 14f * u, cy - 8f * u)
        k.path.quadTo(cx + 13f * u, cy + 11f * u, cx, cy + 11f * u)
        k.path.quadTo(cx - 13f * u, cy + 11f * u, cx - 14f * u, cy - 8f * u)
        k.path.close()
        p.style = Paint.Style.FILL
        p.color = k.al(cream, 255f)
        c.drawPath(k.path, p)
        k.oval(c, cx - 14f * u, cy - 11f * u, cx + 14f * u, cy - 5f * u, brown, 255f)
        k.heart(c, cx, cy - 8f * u, 3.4f * u, 0f, Color.parseColor("#F3DCC4"), 255f)
        for (i in 0 until 3) {
            k.path.reset()
            val bx = cx - 6f * u + i * 6f * u
            for (j in 0..10) {
                val q = j / 10f
                val yy = cy - 14f * u - q * 30f * u
                val xx = bx + sin(q * 6f + t * 1.6f + i * 1.9f) * (2f + 3f * q) * u
                if (j == 0) k.path.moveTo(xx, yy) else k.path.lineTo(xx, yy)
            }
            val life = 0.55f + 0.45f * sin(t * 0.9f + i * 2f)
            p.style = Paint.Style.STROKE
            p.strokeCap = Paint.Cap.ROUND
            p.strokeWidth = 2.2f * u
            p.color = k.al(Color.WHITE, 130f * life)
            c.drawPath(k.path, p)
        }

        // vasinho de planta à direita
        val vx = w * 0.92f
        val vb = h * 0.93f
        for (i in 0 until 5) {
            val ang = -70f + i * 35f + sin(t * 0.8f + i) * 4f
            val ln = (14f + (i % 2) * 4f) * u
            c.save()
            c.translate(vx, vb - 10f * u)
            c.rotate(ang)
            k.leaf(c, 0f, -ln, ln * 0.55f, 0f, 1f, if (i % 2 == 0) Color.parseColor("#5FA05A") else Color.parseColor("#7DBE6E"), Color.parseColor("#3F7A40"), 255f)
            c.restore()
        }
        k.path.reset()
        k.path.moveTo(vx - 9f * u, vb - 10f * u)
        k.path.lineTo(vx + 9f * u, vb - 10f * u)
        k.path.lineTo(vx + 6f * u, vb + 2f * u)
        k.path.lineTo(vx - 6f * u, vb + 2f * u)
        k.path.close()
        p.style = Paint.Style.FILL
        p.color = k.al(Color.parseColor("#C8693F"), 255f)
        c.drawPath(k.path, p)
        k.rrect(c, vx - 10.5f * u, vb - 12f * u, vx + 10.5f * u, vb - 8f * u, 1.2f * u, Color.parseColor("#D97B4E"), 255f)

        // folhas ao vento
        for (i in leaves.indices) {
            val m = leaves[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (-0.05f + 1.1f * life)
            val x = w * (m.x + 0.12f * life) + sin(t * 0.8f + m.ph * 6.28f) * m.sw * u
            val a = k.fade(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            k.leaf(c, x, y, m.sz * u, t * 50f * (if (i % 2 == 0) 1f else -1f) + m.ph * 360f, abs(sin(t * 1.3f + m.ph * 7f)),
                if (i % 2 == 0) Color.parseColor("#7DBE6E") else Color.parseColor("#E9A24A"), Color.parseColor("#4B7A3E"), 235f * a)
        }
        for (m in motes) {
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1.05f - 1.1f * life)
            val x = w * m.x + sin(t * 0.7f + m.ph * 6.28f) * m.sw * u
            val a = k.fade(life) * k.edge(y, h)
            k.soft(c, x, y, m.sz * u * 3f, Color.parseColor("#FFF0C8"), 150f * a)
        }
        k.tidy()
    }
}

// ======================================================================================
// VINGANÇA: sangue e aço. Lua vermelha, lírios-aranha, adaga pendurada num fio,
// ampulheta, fio vermelho do destino, vidro trincado, brasas e pétalas.
// ======================================================================================
class VingancaScene : AuraScene {
    private var kit: SceneKit? = null
    private var embers: List<SceneKit.Mote> = emptyList()
    private var petals: List<SceneKit.Mote> = emptyList()

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        embers = k.motes(22, 901L, 0.03f, 0.07f, 1.4f, 3f, 16f)
        petals = k.motes(8, 902L, 0.025f, 0.05f, 6f, 10f, 24f)
    }

    private fun lily(c: Canvas, k: SceneKit, x: Float, base: Float, top: Float, s: Float, t: Float, ph: Float) {
        val u = k.u
        val p = k.p
        val sw = sin(t * 0.7f + ph * 6f) * 3f * u
        val red = Color.parseColor("#E01F44")
        k.path.reset()
        k.path.moveTo(x, base)
        k.path.quadTo(x + sw * 0.3f, (base + top) / 2f, x + sw, top)
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = 1.6f * u
        p.color = k.al(Color.parseColor("#3A0F18"), 255f)
        c.drawPath(k.path, p)
        val hx = x + sw
        for (i in 0 until 7) {
            val an = (-160f + i * 22f) * 0.0174533f
            val ln = s * (0.8f + 0.25f * (i % 2))
            val ex = hx + cos(an) * ln
            val ey = top + sin(an) * ln * 0.9f
            k.path.reset()
            k.path.moveTo(hx, top)
            k.path.quadTo(hx + cos(an) * ln * 0.5f, top + sin(an) * ln * 0.95f - 3f * u, ex, ey)
            p.strokeWidth = 1.2f * u
            p.color = k.al(red, 245f)
            c.drawPath(k.path, p)
            k.circ(c, ex, ey, 1.3f * u, Color.parseColor("#FFB0BE"), 240f)
        }
        k.circ(c, hx, top, 1.6f * u, Color.parseColor("#9C1230"), 255f)
    }

    private fun dagger(c: Canvas, k: SceneKit, s: Float) {
        val steel = Color.parseColor("#C9CED6")
        val shade = Color.parseColor("#8D95A3")
        val gold = Color.parseColor("#B8893B")
        k.path.reset()
        k.path.moveTo(0f, -1.3f * s)
        k.path.lineTo(0.17f * s, -0.12f * s)
        k.path.lineTo(-0.17f * s, -0.12f * s)
        k.path.close()
        k.p.style = Paint.Style.FILL
        k.p.color = k.al(steel, 255f)
        c.drawPath(k.path, k.p)
        k.path.reset()
        k.path.moveTo(0f, -1.3f * s)
        k.path.lineTo(0.17f * s, -0.12f * s)
        k.path.lineTo(0f, -0.12f * s)
        k.path.close()
        k.p.color = k.al(shade, 255f)
        c.drawPath(k.path, k.p)
        k.rrect(c, -0.36f * s, -0.12f * s, 0.36f * s, 0.02f * s, 0.05f * s, gold, 255f)
        k.rrect(c, -0.07f * s, 0.02f * s, 0.07f * s, 0.58f * s, 0.03f * s, Color.parseColor("#3A0F18"), 255f)
        for (i in 0 until 4) k.line(c, -0.07f * s, 0.1f * s + i * 0.12f * s, 0.07f * s, 0.16f * s + i * 0.12f * s, 0.03f * s, gold, 230f)
        k.circ(c, 0f, 0.66f * s, 0.1f * s, gold, 255f)
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        k.tidy()
        val u = k.u
        val p = k.p
        val crimson = Color.parseColor("#E01F44")
        val blood = Color.parseColor("#C4122F")

        k.vgrad(c, w, h, Color.parseColor("#0A0206"), Color.parseColor("#2B0714"), Color.parseColor("#5A0F25"))

        // lua de sangue com faixas de nuvem
        val mx = w * 0.8f
        val my = h * 0.22f
        k.soft(c, mx, my, 96f * u, blood, 150f)
        k.circ(c, mx, my, 27f * u, Color.parseColor("#B5102B"), 255f)
        k.circ(c, mx - 4f * u, my - 3f * u, 22f * u, Color.parseColor("#D3203D"), 255f)
        k.circ(c, mx + 8f * u, my + 7f * u, 6f * u, Color.parseColor("#9A0C24"), 200f)
        k.circ(c, mx - 9f * u, my + 5f * u, 4f * u, Color.parseColor("#9A0C24"), 190f)
        k.circ(c, mx + 3f * u, my - 11f * u, 3.4f * u, Color.parseColor("#9A0C24"), 180f)
        for (i in 0 until 2) {
            val x = mx + sin(t * 0.12f + i * 2f) * 34f * u
            k.softOval(c, x, my + (i * 2 - 1) * 9f * u, 44f * u, 5f * u, Color.parseColor("#12030A"), 230f)
        }

        // fio vermelho do destino atravessando a cena
        val fy = h * 0.5f
        k.path.reset()
        for (i in 0..30) {
            val x = w * i / 30f
            val y = fy + sin(x * 0.02f + t * 0.8f) * 14f * u + sin(x * 0.007f - t * 0.4f) * 10f * u
            if (i == 0) k.path.moveTo(x, y) else k.path.lineTo(x, y)
        }
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.strokeWidth = 6f * u
        p.color = k.al(crimson, 40f)
        c.drawPath(k.path, p)
        p.strokeWidth = 1.5f * u
        p.color = k.al(Color.parseColor("#FF4A68"), 235f)
        c.drawPath(k.path, p)

        k.posterRipple(c, poster, hasPoster, t, 3.6f, crimson, 150f)

        // vidro trincado na esquerda
        val ix = w * 0.04f
        val iy = h * 0.48f
        val glint = 0.5f + 0.5f * sin(t * 0.7f)
        for (i in 0 until 9) {
            val an = (i * 40f + k.hash(i + 5) * 20f) * 0.0174533f
            val len = (34f + 60f * k.hash(i + 20)) * u
            var px = ix
            var py = iy
            k.path.reset()
            k.path.moveTo(px, py)
            for (j in 1..4) {
                val a2 = an + (k.hash(i * 7 + j) - 0.5f) * 0.5f
                px += cos(a2) * len / 4f
                py += sin(a2) * len / 4f
                k.path.lineTo(px, py)
            }
            p.strokeWidth = 1f * u
            p.color = k.al(Color.WHITE, 60f + 50f * glint)
            c.drawPath(k.path, p)
        }
        for (r in 1..3) {
            k.path.reset()
            for (i in 0..8) {
                val an = (i * 40f + 10f) * 0.0174533f
                val rr = r * 11f * u * (0.85f + 0.3f * k.hash(r * 11 + i))
                val x = ix + cos(an) * rr
                val y = iy + sin(an) * rr
                if (i == 0) k.path.moveTo(x, y) else k.path.lineTo(x, y)
            }
            p.strokeWidth = 0.8f * u
            p.color = k.al(Color.WHITE, 40f)
            c.drawPath(k.path, p)
        }

        // adaga pendurada num fio vermelho, balançando, com gota de sangue
        val ax = w * 0.3f
        val swing = sin(t * 0.9f) * 6f
        c.save()
        c.translate(ax, 0f)
        c.rotate(swing)
        k.line(c, 0f, 0f, 0f, 22f * u, 1.1f * u, Color.parseColor("#FF4A68"), 240f)
        c.translate(0f, 22f * u + 18f * u)
        c.scale(1f, -1f)
        dagger(c, k, 15f * u)
        c.restore()
        val tipY = 22f * u + 18f * u + 19.5f * u
        val dl = k.frac(t * 0.28f)
        val dx = ax - sin(swing * 0.0174533f) * tipY
        val dyp = tipY + dl * dl * (h * 0.5f)
        k.drop(c, dx, dyp, (2.4f + 1.4f * k.ss(dl * 4f)) * u, 0f, Color.parseColor("#D3203D"), 255f * k.ss((1f - dl) * 4f) * k.ss(dl * 10f))

        // ampulheta que vira
        val hxp = w * 0.12f
        val hyp = h * 0.2f
        val cyc = k.frac(t / 9f)
        val flip = k.ss((cyc - 0.9f) / 0.1f)
        val fill = if (cyc < 0.9f) cyc / 0.9f else 1f
        c.save()
        c.translate(hxp, hyp)
        c.rotate(flip * 180f)
        c.scale(15f * u, 15f * u)
        k.path.reset()
        k.path.moveTo(-0.7f, -1f)
        k.path.lineTo(0.7f, -1f)
        k.path.cubicTo(0.7f, -0.35f, 0.1f, -0.2f, 0.06f, 0f)
        k.path.cubicTo(0.1f, 0.2f, 0.7f, 0.35f, 0.7f, 1f)
        k.path.lineTo(-0.7f, 1f)
        k.path.cubicTo(-0.7f, 0.35f, -0.1f, 0.2f, -0.06f, 0f)
        k.path.cubicTo(-0.1f, -0.2f, -0.7f, -0.35f, -0.7f, -1f)
        k.path.close()
        p.style = Paint.Style.FILL
        p.color = k.al(Color.WHITE, 40f)
        c.drawPath(k.path, p)
        c.save()
        c.clipPath(k.path)
        p.color = k.al(Color.parseColor("#E8B15D"), 245f)
        val topLevel = -1f + 1.0f * fill
        c.drawRect(-0.8f, topLevel, 0.8f, -0.02f, p)
        val botLevel = 1f - 0.85f * fill
        c.drawRect(-0.8f, botLevel + 0.12f, 0.8f, 1.05f, p)
        k.path.reset()
        k.path.moveTo(-0.5f, botLevel + 0.12f)
        k.path.lineTo(0f, botLevel - 0.08f)
        k.path.lineTo(0.5f, botLevel + 0.12f)
        k.path.close()
        c.drawPath(k.path, p)
        if (cyc < 0.9f) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = 0.05f
            c.drawLine(0f, 0f, 0f, botLevel, p)
        }
        c.restore()
        p.style = Paint.Style.STROKE
        p.strokeWidth = 0.07f
        p.strokeJoin = Paint.Join.ROUND
        p.color = k.al(Color.parseColor("#F2D9A0"), 235f)
        k.path.reset()
        k.path.moveTo(-0.7f, -1f)
        k.path.lineTo(0.7f, -1f)
        k.path.cubicTo(0.7f, -0.35f, 0.1f, -0.2f, 0.06f, 0f)
        k.path.cubicTo(0.1f, 0.2f, 0.7f, 0.35f, 0.7f, 1f)
        k.path.lineTo(-0.7f, 1f)
        k.path.cubicTo(-0.7f, 0.35f, -0.1f, 0.2f, -0.06f, 0f)
        k.path.cubicTo(-0.1f, -0.2f, -0.7f, -0.35f, -0.7f, -1f)
        k.path.close()
        c.drawPath(k.path, p)
        c.restore()
        k.rrect(c, hxp - 13f * u, hyp - 17.5f * u, hxp + 13f * u, hyp - 14.5f * u, 1f * u, Color.parseColor("#6E1426"), 255f)
        k.rrect(c, hxp - 13f * u, hyp + 14.5f * u, hxp + 13f * u, hyp + 17.5f * u, 1f * u, Color.parseColor("#6E1426"), 255f)

        // lírios-aranha no chão
        k.path.reset()
        k.path.moveTo(0f, h)
        for (i in 0..12) k.path.lineTo(w * i / 12f, h * 0.93f - 4f * u * sin(i * 0.9f))
        k.path.lineTo(w, h)
        k.path.close()
        p.style = Paint.Style.FILL
        p.color = k.al(Color.parseColor("#12030A"), 255f)
        c.drawPath(k.path, p)
        for (i in 0 until 6) {
            val x = w * (0.08f + 0.17f * i) + (k.hash(i + 3) - 0.5f) * 8f * u
            lily(c, k, x, h * 0.96f, h * (0.74f + 0.09f * k.hash(i + 9)), (14f + 5f * k.hash(i + 14)) * u, t, k.hash(i + 31))
        }

        // pétalas e brasas
        for (i in petals.indices) {
            val m = petals[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (-0.06f + 1.1f * life)
            val x = w * m.x + sin(t * 0.7f + m.ph * 6.28f) * m.sw * u
            val a = k.fade(life) * k.edge(y, h)
            k.petal(c, x, y, m.sz * u * 0.8f, t * 30f + m.ph * 360f, abs(sin(t * 1.1f + m.ph * 7f)), crimson, 230f * a)
        }
        for (m in embers) {
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1.05f - 1.1f * life)
            val x = w * m.x + sin(t * 0.8f + m.ph * 6.28f) * m.sw * u
            val a = k.fade(life) * k.edge(y, h)
            k.soft(c, x, y, m.sz * u * 3.4f, Color.parseColor("#FF6A3D"), 150f * a)
            k.circ(c, x, y, m.sz * u * 0.55f, Color.parseColor("#FFD08A"), 240f * a)
        }
        k.tidy()
    }
}

// ======================================================================================
// CRIME: noite de chuva na cidade. Sirenes vermelha e azul, fita policial, contorno de giz,
// marcadores de evidência, impressão digital sendo escaneada, algemas e distintivo.
// ======================================================================================
class CrimeScene : AuraScene {
    private var kit: SceneKit? = null
    private var rain: List<SceneKit.Mote> = emptyList()
    private var chalk: DashPathEffect? = null

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        rain = k.motes(30, 1001L, 0.5f, 0.9f, 8f, 14f, 0f)
        chalk = DashPathEffect(floatArrayOf(6f * k.u, 4f * k.u), 0f)
    }

    private fun tape(c: Canvas, k: SceneKit, w: Float, y: Float, ang: Float, t: Float, ph: Float) {
        val u = k.u
        val yellow = Color.parseColor("#FFD400")
        c.save()
        c.translate(w / 2f, y)
        c.rotate(ang + sin(t * 0.9f + ph) * 0.8f)
        k.rrect(c, -w, -6f * u, w, 6f * u, 0f, Color.parseColor("#000000"), 70f)
        k.rrect(c, -w, -6.5f * u, w, 5.5f * u, 0f, yellow, 255f)
        val gap = 175f * u
        var x = -w
        while (x < w) {
            k.label(c, "LINHA POLICIAL  •  NÃO ULTRAPASSE  •", x, -0.5f * u, 6.6f * u, Color.parseColor("#1A1A1A"), 255f, 0f, 1f)
            x += gap
        }
        c.restore()
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        k.tidy()
        val u = k.u
        val p = k.p
        val red = Color.parseColor("#FF2D55")
        val blue = Color.parseColor("#2D7BFF")
        val steel = Color.parseColor("#C5CCD8")
        val neon = Color.parseColor("#3DF5D0")

        k.vgrad(c, w, h, Color.parseColor("#080B18"), Color.parseColor("#131A33"), Color.parseColor("#262244"))

        // sirenes: vermelho e azul trocando de lugar, sempre suave
        val s1 = 0.5f + 0.5f * sin(t * 2.2f)
        k.soft(c, 0f, 0f, max(w, h) * 0.85f, red, 40f + 120f * s1)
        k.soft(c, w, 0f, max(w, h) * 0.85f, blue, 40f + 120f * (1f - s1))
        k.soft(c, w * 0.5f, h * 1.05f, max(w, h) * 0.7f, mixB(red, blue, s1, k), 50f)

        // holofote do helicóptero varrendo
        val hang = (100f + 24f * sin(t * 0.4f)) * 0.0174533f
        k.path.reset()
        k.path.moveTo(w * 0.92f, -6f * u)
        k.path.lineTo(w * 0.92f + cos(hang - 0.1f) * h * 1.6f, -6f * u + sin(hang - 0.1f) * h * 1.6f)
        k.path.lineTo(w * 0.92f + cos(hang + 0.1f) * h * 1.6f, -6f * u + sin(hang + 0.1f) * h * 1.6f)
        k.path.close()
        p.style = Paint.Style.FILL
        p.color = k.al(Color.parseColor("#FFFBE0"), 22f)
        c.drawPath(k.path, p)

        // impressão digital sendo escaneada
        val fpx = w * 0.14f
        val fpy = h * 0.5f
        val scan = fpy + sin(t * 1.1f) * 30f * u
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = 1.1f * u
        for (i in 0 until 9) {
            val rr = (4f + i * 3.6f) * u
            val st = 200f + k.hash(i + 2) * 120f
            val sw = 220f + k.hash(i + 12) * 100f
            val glow = k.tw(t, i * 0.09f, 1.4f)
            p.color = k.al(neon, 60f + 95f * glow)
            k.rect.set(fpx - rr, fpy - rr * 1.15f, fpx + rr, fpy + rr * 1.15f)
            c.drawArc(k.rect, st, sw, false, p)
            val st2 = st + sw + 28f
            p.color = k.al(neon, 40f + 70f * glow)
            c.drawArc(k.rect, st2, 360f - sw - 70f, false, p)
        }
        k.line(c, fpx - 34f * u, scan, fpx + 34f * u, scan, 1.6f * u, Color.WHITE, 220f)
        k.soft(c, fpx, scan, 38f * u, neon, 70f)

        // prédios ao fundo com janelas
        for (layer in 0..1) {
            var x = -4f * u
            var i = 0
            val base = if (layer == 0) h * 0.8f else h * 0.9f
            while (x < w) {
                val bw = (14f + 16f * k.hash(layer * 50 + i)) * u
                val bh = (18f + 38f * k.hash(layer * 50 + i + 100)) * u
                k.rrect(c, x, base - bh, x + bw, h, 1f * u, if (layer == 0) Color.parseColor("#0E1226") else Color.parseColor("#070A15"), 255f)
                if (layer == 0) {
                    for (r in 0 until 3) for (q in 0 until 2) {
                        val th = k.hash(i * 13 + r * 3 + q + 400)
                        if (th > 0.45f) {
                            val on = 0.6f + 0.4f * sin(t * 0.3f + th * 20f)
                            k.rrect(c, x + 3f * u + q * 7f * u, base - bh + 5f * u + r * 8f * u, x + 6.5f * u + q * 7f * u, base - bh + 8.5f * u + r * 8f * u, 0.5f * u, Color.parseColor("#FFD27A"), 150f * on)
                        }
                    }
                }
                x += bw + 1.5f * u
                i++
            }
        }

        // contorno de giz com marcadores de evidência
        val bx = w * 0.82f
        val by = h * 0.8f
        c.save()
        c.translate(bx, by)
        c.rotate(-72f)
        k.path.reset()
        k.path.addCircle(0f, -22f * u, 5.5f * u, android.graphics.Path.Direction.CW)
        k.path.moveTo(0f, -16f * u)
        k.path.lineTo(0f, 8f * u)
        k.path.moveTo(0f, -12f * u)
        k.path.lineTo(-13f * u, -2f * u)
        k.path.lineTo(-17f * u, 6f * u)
        k.path.moveTo(0f, -12f * u)
        k.path.lineTo(12f * u, -6f * u)
        k.path.lineTo(18f * u, 2f * u)
        k.path.moveTo(0f, 8f * u)
        k.path.lineTo(-6f * u, 24f * u)
        k.path.moveTo(0f, 8f * u)
        k.path.lineTo(8f * u, 22f * u)
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.strokeWidth = 1.6f * u
        p.pathEffect = chalk
        p.color = k.al(Color.WHITE, 190f)
        c.drawPath(k.path, p)
        p.pathEffect = null
        c.restore()
        for (i in 0 until 3) {
            val mx = bx - 24f * u + i * 18f * u
            val my = by + (if (i == 1) 14f else 8f) * u
            k.oval(c, mx - 6f * u, my + 3f * u, mx + 6f * u, my + 6f * u, Color.BLACK, 90f)
            k.path.reset()
            k.path.moveTo(mx - 5f * u, my + 4f * u)
            k.path.lineTo(mx + 5f * u, my + 4f * u)
            k.path.lineTo(mx + 3f * u, my - 8f * u)
            k.path.lineTo(mx - 3f * u, my - 8f * u)
            k.path.close()
            p.style = Paint.Style.FILL
            p.color = k.al(Color.parseColor("#FFD400"), 255f)
            c.drawPath(k.path, p)
            k.label(c, (i + 1).toString(), mx, my - 2f * u, 6.5f * u, Color.parseColor("#1A1A1A"), 255f, 0f, 1f)
        }

        // algemas penduradas
        val hx = w * 0.64f
        val sw2 = sin(t * 1.1f) * 5f
        c.save()
        c.translate(hx, 0f)
        c.rotate(sw2)
        for (i in 0 until 4) {
            k.p.style = Paint.Style.STROKE
            k.p.strokeWidth = 1.5f * u
            k.p.color = k.al(steel, 255f)
            k.rect.set(-2.6f * u, 2f * u + i * 6f * u, 2.6f * u, 7.5f * u + i * 6f * u)
            c.drawOval(k.rect, k.p)
        }
        for (s in 0..1) {
            val cx = (if (s == 0) -10f else 10f) * u
            val cyy = 36f * u
            k.ring(c, cx, cyy, 7.5f * u, 2.4f * u, steel, 255f)
            k.ring(c, cx - 1.2f * u, cyy - 1.2f * u, 7.5f * u, 0.8f * u, Color.WHITE, 140f)
            k.rrect(c, cx - 3.5f * u, cyy - 11f * u, cx + 3.5f * u, cyy - 5.5f * u, 1.2f * u, Color.parseColor("#9AA4B4"), 255f)
            k.circ(c, cx, cyy - 8f * u, 1f * u, Color.parseColor("#2A3040"), 255f)
        }
        k.line(c, -4f * u, 26f * u, 4f * u, 26f * u, 1.6f * u, steel, 255f)
        c.restore()

        // distintivo brilhando
        val bdx = w * 0.12f
        val bdy = h * 0.2f
        val gl = 0.6f + 0.4f * sin(t * 1.2f)
        k.soft(c, bdx, bdy, 30f * u, Color.parseColor("#FFD27A"), 90f * gl)
        k.path.reset()
        for (i in 0 until 14) {
            val an = (-90f + i * 360f / 14f) * 0.0174533f
            val rr = if (i % 2 == 0) 13f * u else 8.5f * u
            val x = bdx + cos(an) * rr
            val y = bdy + sin(an) * rr
            if (i == 0) k.path.moveTo(x, y) else k.path.lineTo(x, y)
        }
        k.path.close()
        p.style = Paint.Style.FILL
        p.color = k.al(Color.parseColor("#E8B84A"), 255f)
        c.drawPath(k.path, p)
        k.circ(c, bdx, bdy, 6.6f * u, Color.parseColor("#B8892B"), 255f)
        k.circ(c, bdx, bdy, 5f * u, Color.parseColor("#F5D27A"), 255f)
        k.path.reset()
        for (i in 0 until 10) {
            val an = (-90f + i * 36f) * 0.0174533f
            val rr = if (i % 2 == 0) 4.4f * u else 1.8f * u
            val x = bdx + cos(an) * rr
            val y = bdy + sin(an) * rr
            if (i == 0) k.path.moveTo(x, y) else k.path.lineTo(x, y)
        }
        k.path.close()
        p.color = k.al(Color.parseColor("#7A5A12"), 255f)
        c.drawPath(k.path, p)

        // fitas policiais
        tape(c, k, w, h * 0.13f, -4f, t, 0f)
        tape(c, k, w, h * 0.92f, 3f, t, 2f)

        k.posterRipple(c, poster, hasPoster, t, 3.6f, Color.WHITE, 110f)

        // chuva fina
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = 1f * u
        for (m in rain) {
            val life = k.frac(t * m.sp + m.ph)
            val y = life * (h + 40f * u) - 20f * u
            val x = w * m.x - life * 14f * u
            p.color = k.al(Color.parseColor("#BFD4FF"), 80f)
            c.drawLine(x, y, x - 2.4f * u, y + m.sz * u, p)
        }
        k.tidy()
    }

    private fun mixB(a: Int, b: Int, f: Float, k: SceneKit): Int = k.mix(a, b, f)
}
