package com.doramabloom.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.min
import kotlin.math.sin

// ======================================================================================
// LGBTQ+: céu pastel de fim de tarde com um arco-íris grande que pulsa devagar, nuvens
// fofas nas pontas, corações coloridos subindo e estrelinhas piscando.
// ======================================================================================
class LgbtScene : AuraScene {
    private var kit: SceneKit? = null
    private var hearts: List<SceneKit.Mote> = emptyList()
    private var stars: List<SceneKit.Mote> = emptyList()

    private val rainbow = intArrayOf(
        Color.parseColor("#E8505B"), Color.parseColor("#FF7A3D"), Color.parseColor("#F5D547"),
        Color.parseColor("#5DB56E"), Color.parseColor("#4F6D9A"), Color.parseColor("#A068E0")
    )

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        hearts = k.motes(14, 901L, 0.025f, 0.06f, 6f, 12f, 20f)
        stars = k.motes(24, 902L, 0.02f, 0.05f, 1.4f, 3.2f, 10f)
    }

    private fun cloud(c: Canvas, k: SceneKit, x: Float, y: Float, s: Float) {
        val white = Color.WHITE
        k.circ(c, x - 12f * s, y + 3f * s, 9f * s, white, 235f)
        k.circ(c, x, y - 3f * s, 13f * s, white, 245f)
        k.circ(c, x + 13f * s, y + 2f * s, 10f * s, white, 235f)
        k.rrect(c, x - 20f * s, y + 2f * s, x + 22f * s, y + 12f * s, 5f * s, white, 235f)
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        k.tidy()
        val u = k.u
        val p = k.p

        k.vgrad(c, w, h, Color.parseColor("#E9D8FF"), Color.parseColor("#FFDDEF"), Color.parseColor("#FFF0D4"))

        // brilho suave atrás do arco-íris
        val cx = w * 0.5f
        val cy = h * 0.72f
        val outer = min(w * 0.46f, 170f * u)
        k.soft(c, cx, cy - outer * 0.4f, outer * 1.3f, Color.WHITE, 120f)

        // arco-íris: seis faixas, cada uma pulsando um pouquinho depois da outra
        val band = outer * 0.075f
        p.style = Paint.Style.STROKE
        p.strokeWidth = band
        p.shader = null
        for (i in rainbow.indices) {
            val r = outer - i * band - band / 2f
            val pulse = 0.82f + 0.18f * sin(t * 1.4f - i * 0.6f)
            p.color = k.al(rainbow[i], 215f * pulse)
            k.rect.set(cx - r, cy - r, cx + r, cy + r)
            c.drawArc(k.rect, 180f, 180f, false, p)
        }
        p.style = Paint.Style.FILL

        // nuvens nas pontas, flutuando de leve
        val drift = sin(t * 0.5f) * 3f * u
        cloud(c, k, cx - outer + band * 2.5f + drift, cy + 2f * u, u * 1.5f)
        cloud(c, k, cx + outer - band * 2.5f - drift, cy + 2f * u, u * 1.5f)

        // estrelinhas piscando
        for ((i, m) in stars.withIndex()) {
            val a = 0.5f + 0.5f * sin(t * 2f + m.ph * 6.2832f)
            val sx = w * m.x
            val sy = h * (0.04f + m.y * 0.5f)
            k.star4(c, sx, sy, m.sz * u * (0.6f + 0.6f * a), t * 20f + i * 30f, Color.WHITE, 120f + 135f * a)
        }

        // corações coloridos subindo
        for ((i, m) in hearts.withIndex()) {
            val life = k.frac(t * m.sp + m.ph)
            val hy = h * (1.05f - 1.1f * life)
            val hx = w * m.x + sin(t * 0.9f + m.ph * 6.2832f) * m.sw * u
            val a = k.env(life) * 200f
            k.icon(c, "heart", k.al(rainbow[i % rainbow.size], a), hx, hy, m.sz * u * 1.5f, sin(t + m.ph * 6f) * 12f)
        }
    }
}
