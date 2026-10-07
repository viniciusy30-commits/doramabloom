package com.doramabloom.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Cenas animadas exclusivas de cada gênero para o fundo do cartão do Início.
 * Cada cena pinta o fundo inteiro (a capa fica de fora porque o AuraView já recortou a capa)
 * e é calculada só pelo relógio, sem estado. Gêneros sem cena usam a animação padrão do AuraView.
 */
class SceneKit(val u: Float) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val path = Path()
    val rect = RectF()
    val twoPi = (2.0 * Math.PI).toFloat()
    private val ic = IconDrawable("heart", Color.WHITE)
    private val grads = HashMap<Int, LinearGradient>()
    val mono: Typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

    class Mote(val x: Float, val y: Float, val ph: Float, val sp: Float, val sz: Float, val sw: Float)

    fun motes(n: Int, seed: Long, spLo: Float, spHi: Float, szLo: Float, szHi: Float, swHi: Float): List<Mote> {
        val r = java.util.Random(seed)
        return List(n) {
            Mote(
                r.nextFloat(), r.nextFloat(), r.nextFloat(),
                spLo + r.nextFloat() * (spHi - spLo),
                szLo + r.nextFloat() * (szHi - szLo),
                r.nextFloat() * swHi
            )
        }
    }

    fun al(color: Int, a: Float): Int =
        Color.argb(a.coerceIn(0f, 255f).toInt(), Color.red(color), Color.green(color), Color.blue(color))

    fun frac(x: Float): Float = x - floor(x)

    /** 0 nas pontas da vida e 1 no meio. */
    fun env(life: Float): Float = sin(life * twoPi / 2f)

    /** Some suavemente perto das bordas de cima e de baixo. */
    fun edge(y: Float, h: Float): Float {
        val a = (y / (22f * u)).coerceIn(0f, 1f)
        val b = ((h - 12f * u - y) / (30f * u)).coerceIn(0f, 1f)
        return a * a * (3f - 2f * a) * b * b * (3f - 2f * b)
    }

    /** Pico suave (0..1) centrado em c com largura w. */
    fun bump(x: Float, c: Float, w: Float): Float {
        val d = (x - c) / w
        return exp(-d * d)
    }

    fun icon(c: Canvas, name: String, color: Int, cx: Float, cy: Float, size: Float, rot: Float) {
        ic.name = name
        ic.color = color
        val h = (size / 2f).toInt()
        if (h <= 0) return
        ic.setBounds(-h, -h, h, h)
        c.save()
        c.translate(cx, cy)
        c.rotate(rot)
        ic.draw(c)
        c.restore()
    }

    /** Ícone com escala separada em x/y (bater de asas, piscar etc.). */
    fun iconXY(c: Canvas, name: String, color: Int, cx: Float, cy: Float, size: Float, rot: Float, sx: Float, sy: Float) {
        ic.name = name
        ic.color = color
        val h = (size / 2f).toInt()
        if (h <= 0) return
        ic.setBounds(-h, -h, h, h)
        c.save()
        c.translate(cx, cy)
        c.rotate(rot)
        c.scale(sx, sy)
        ic.draw(c)
        c.restore()
    }

    /** Fundo em degradê vertical (guarda o shader para não criar um novo a cada quadro). */
    fun vgrad(c: Canvas, w: Float, h: Float, c0: Int, c1: Int, c2: Int) {
        var key = w.toInt() * 31 + h.toInt()
        key = key * 31 + c0
        key = key * 31 + c1
        key = key * 31 + c2
        var sh = grads[key]
        if (sh == null) {
            sh = LinearGradient(0f, 0f, 0f, h, intArrayOf(c0, c1, c2), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
            if (grads.size > 12) grads.clear()
            grads[key] = sh
        }
        p.style = Paint.Style.FILL
        p.shader = sh
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
    }

    /** Brilho macio feito de círculos concêntricos (sem criar objetos a cada quadro). */
    fun glow(c: Canvas, x: Float, y: Float, r: Float, color: Int, a: Float) {
        p.style = Paint.Style.FILL
        for (i in 0 until 6) {
            val f = 1f - i * 0.15f
            p.color = al(color, a * 0.22f * (0.35f + i * 0.2f))
            c.drawCircle(x, y, r * f, p)
        }
    }

    fun star4(c: Canvas, x: Float, y: Float, size: Float, rot: Float, color: Int, a: Float) {
        path.reset()
        path.moveTo(0f, -1f)
        path.lineTo(0.16f, -0.16f)
        path.lineTo(1f, 0f)
        path.lineTo(0.16f, 0.16f)
        path.lineTo(0f, 1f)
        path.lineTo(-0.16f, 0.16f)
        path.lineTo(-1f, 0f)
        path.lineTo(-0.16f, -0.16f)
        path.close()
        p.style = Paint.Style.FILL
        p.color = al(color, 40f * a)
        c.drawCircle(x, y, size * 1.7f, p)
        p.color = al(color, 255f * a)
        c.save()
        c.translate(x, y)
        c.rotate(rot)
        c.scale(size, size)
        c.drawPath(path, p)
        c.restore()
    }
}

interface AuraScene {
    fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit)
}

object AuraScenes {
    private val map: Map<String, AuraScene> = mapOf(
        "romance" to RomanceScene(),
        "terror" to TerrorScene(),
        "fantasia" to FantasiaScene(),
        "acao" to AcaoScene(),
        "historico" to HistoricoScene(),
        "scifi" to ScifiScene(),
        "comedia" to ComediaScene(),
        "misterio" to MisterioScene(),
        "drama" to DramaScene(),
        "musical" to MusicalScene(),
        "esporte" to EsporteScene(),
        "realeza" to RealezaScene(),
        "escolar" to EscolarScene(),
        "suspense" to SuspenseScene(),
        "medico" to MedicoScene(),
        "familia" to FamiliaScene(),
        "vida" to VidaScene(),
        "vinganca" to VingancaScene()
    )

    fun forKey(key: String): AuraScene? = map[key]
}

// ======================================================================================
// ROMANCE: noite rosada de primeiro beijo. Luzinhas, coração que bate, bokeh, pétalas.
// ======================================================================================
private class RomanceScene : AuraScene {
    private var kit: SceneKit? = null
    private var bokeh: List<SceneKit.Mote> = emptyList()
    private var petals: List<SceneKit.Mote> = emptyList()
    private var hearts: List<SceneKit.Mote> = emptyList()
    private var dust: List<SceneKit.Mote> = emptyList()

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        bokeh = k.motes(14, 101L, 0.01f, 0.03f, 14f, 34f, 20f)
        petals = k.motes(14, 102L, 0.035f, 0.07f, 12f, 20f, 30f)
        hearts = k.motes(10, 103L, 0.04f, 0.085f, 10f, 24f, 16f)
        dust = k.motes(26, 104L, 0.5f, 1.3f, 3f, 6f, 0f)
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        val u = k.u
        val p = k.p
        val rose = Color.parseColor("#FF6B9D")
        val deep = Color.parseColor("#C2457A")
        val blush = Color.parseColor("#FFD3E4")
        val gold = Color.parseColor("#FFE2A8")

        k.vgrad(c, w, h, Color.parseColor("#FFC1D8"), Color.parseColor("#FF86B1"), Color.parseColor("#B93C74"))

        // batida do coração: dois pulsos seguidos (tum-tum) e uma pausa
        val ph = k.frac(t / 1.7f)
        val beat = k.bump(ph, 0.06f, 0.05f) + 0.7f * k.bump(ph, 0.24f, 0.06f)

        // luz quente atrás da capa, pulsando junto
        val cx = w / 2f
        val cy = h / 2f
        k.glow(c, cx, cy, max(w, h) * (0.55f + 0.06f * beat), blush, 150f + 90f * beat)

        // bokeh grande e macio à deriva
        p.style = Paint.Style.FILL
        for (i in bokeh.indices) {
            val m = bokeh[i]
            val x = w * m.x + sin(t * 0.12f + m.ph * k.twoPi) * m.sw * u
            val y = h * (0.1f + 0.85f * k.frac(m.y + t * m.sp))
            val a = k.env(k.frac(m.y + t * m.sp)) * k.edge(y, h)
            if (a <= 0.02f) continue
            val r = m.sz * u
            p.style = Paint.Style.FILL
            p.color = k.al(if (i % 3 == 0) gold else if (i % 3 == 1) blush else rose, 60f * a)
            c.drawCircle(x, y, r, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 1.2f * u
            p.color = k.al(Color.WHITE, 90f * a)
            c.drawCircle(x, y, r, p)
        }

        // fio de luzinhas no alto, balançando e piscando
        val sag = 16f * u
        val n = 9
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.3f * u
        p.color = k.al(Color.WHITE, 150f)
        k.path.reset()
        for (s in 0..24) {
            val fx = s / 24f
            val x = w * fx
            val y = 6f * u + sag * sin(fx * Math.PI.toFloat()) + 1.5f * u * sin(t * 1.2f + fx * 6f)
            if (s == 0) k.path.moveTo(x, y) else k.path.lineTo(x, y)
        }
        c.drawPath(k.path, p)
        for (i in 0 until n) {
            val fx = (i + 0.5f) / n
            val x = w * fx
            val y = 6f * u + sag * sin(fx * Math.PI.toFloat()) + 1.5f * u * sin(t * 1.2f + fx * 6f) + 6f * u
            val tw = 0.55f + 0.45f * sin(t * 2.6f + i * 1.9f)
            val col = if (i % 3 == 0) Color.WHITE else if (i % 3 == 1) gold else blush
            k.glow(c, x, y, 11f * u, col, 170f * tw)
            p.style = Paint.Style.FILL
            p.color = k.al(Color.WHITE, 255f * (0.6f + 0.4f * tw))
            c.drawCircle(x, y, 2.6f * u, p)
        }

        // ondas em formato de coração saindo da capa a cada batida
        if (hasPoster) {
            p.style = Paint.Style.STROKE
            for (j in 0 until 2) {
                val life = k.frac(t / 1.7f - j * 0.18f - 0.06f)
                val e = life * 34f * u
                p.strokeWidth = (3f - 2.2f * life) * u
                p.color = k.al(Color.WHITE, 190f * (1f - life) * (1f - life))
                k.rect.set(poster.left - e, poster.top - e, poster.right + e, poster.bottom + e)
                c.drawRoundRect(k.rect, 20f * u + e, 20f * u + e, p)
            }
        }

        // pétalas de cerejeira caindo de lado, girando
        for (i in petals.indices) {
            val m = petals[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (-0.08f + 1.16f * life)
            val x = w * (m.x + 0.1f * life) + sin(t * 0.7f + m.ph * k.twoPi) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            val s = m.sz * u * (0.85f + 0.15f * sin(t * 2f + m.ph * 8f))
            val sx = 0.45f + 0.55f * abs(sin(t * 1.3f + m.ph * 7f))
            val col = if (i % 2 == 0) Color.WHITE else blush
            k.iconXY(c, if (i % 4 == 0) "blossom" else "petal", k.al(col, 240f * a), x, y, s, t * 40f * (if (i % 2 == 0) 1f else -1f) + m.ph * 360f, sx, 1f)
        }

        // corações subindo, respirando com a batida
        for (i in hearts.indices) {
            val m = hearts[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1.05f - 1.12f * life)
            val x = w * m.x + sin(t * 0.9f + m.ph * k.twoPi) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            val s = m.sz * u * (1f + 0.18f * beat)
            k.icon(c, "heart", k.al(if (i % 2 == 0) Color.WHITE else deep, 235f * a), x, y, s, sin(t + m.ph * 6f) * 14f)
        }

        // poeira dourada cintilando
        for (m in dust) {
            val s = sin(t * m.sp * 2f + m.ph * k.twoPi)
            val kk = if (s > 0f) s * s * s else 0f
            if (kk <= 0.03f) continue
            val y = h * m.y
            val a = kk * k.edge(y, h)
            if (a <= 0.02f) continue
            k.star4(c, w * m.x, y, m.sz * u * (0.5f + 0.8f * kk), t * 24f + m.ph * 90f, gold, a)
        }

        // um coração grande "estoura" em faíscas de tempos em tempos
        val per = 6.5f
        val idx = floor(t / per).toInt()
        val q = (t - idx * per) / 1.6f
        if (q in 0f..1f) {
            val rr = java.util.Random(idx * 311L + 7L)
            val bx = w * (0.15f + 0.7f * rr.nextFloat())
            val by = h * (0.2f + 0.6f * rr.nextFloat())
            val a = k.env(q)
            k.icon(c, "heart", k.al(Color.WHITE, 210f * a), bx, by - q * 26f * u, (16f + 40f * q) * u, -10f + 20f * q)
            for (s in 0 until 8) {
                val ang = s * k.twoPi / 8f
                val d = q * 32f * u
                p.style = Paint.Style.FILL
                p.color = k.al(gold, 230f * (1f - q))
                c.drawCircle(bx + cos(ang) * d, by - q * 26f * u + sin(ang) * d, 2.4f * u * (1f - q), p)
            }
        }
    }
}

// ======================================================================================
// TERROR: cemitério sob a lua cheia. Neblina, morcegos, fantasmas, olhos, raio, aranha.
// ======================================================================================
private class TerrorScene : AuraScene {
    private var kit: SceneKit? = null
    private var bats: List<SceneKit.Mote> = emptyList()
    private var ghosts: List<SceneKit.Mote> = emptyList()
    private var fog: List<SceneKit.Mote> = emptyList()
    private var motes: List<SceneKit.Mote> = emptyList()

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        bats = k.motes(6, 201L, 0.03f, 0.07f, 16f, 26f, 0f)
        ghosts = k.motes(4, 202L, 0.015f, 0.03f, 26f, 40f, 22f)
        fog = k.motes(7, 203L, 0.01f, 0.03f, 90f, 160f, 0f)
        motes = k.motes(20, 204L, 0.02f, 0.05f, 1.4f, 2.6f, 12f)
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        val u = k.u
        val p = k.p
        val moon = Color.parseColor("#E9E4FF")
        val violet = Color.parseColor("#9B7EDE")
        val green = Color.parseColor("#7CFF9A")

        k.vgrad(c, w, h, Color.parseColor("#07040F"), Color.parseColor("#1E0F3A"), Color.parseColor("#3A1A55"))

        // relâmpago de tempos em tempos: clarão duplo e um raio desenhado
        val per = 9f
        val idx = floor(t / per).toInt()
        val lt = t - idx * per
        val flash = k.bump(lt, 0.10f, 0.035f) + 0.7f * k.bump(lt, 0.30f, 0.05f)

        // lua cheia com halo e crateras
        val mx = w * 0.82f
        val my = h * 0.17f
        val mr = 26f * u
        k.glow(c, mx, my, mr * 3.4f, violet, 170f)
        p.style = Paint.Style.FILL
        p.color = k.al(moon, 255f)
        c.drawCircle(mx, my, mr, p)
        p.color = k.al(Color.parseColor("#B9AEE0"), 150f)
        c.drawCircle(mx - mr * 0.35f, my - mr * 0.2f, mr * 0.22f, p)
        c.drawCircle(mx + mr * 0.3f, my + mr * 0.3f, mr * 0.28f, p)
        c.drawCircle(mx + mr * 0.12f, my - mr * 0.5f, mr * 0.12f, p)

        // nuvens escuras passando na frente da lua
        val cl = k.frac(t * 0.012f)
        p.color = k.al(Color.parseColor("#12081F"), 190f)
        for (s in 0 until 3) {
            val x = w * (1.3f - 1.6f * k.frac(cl + s * 0.33f))
            val y = my + (s - 1) * 10f * u
            k.rect.set(x - 50f * u, y - 6f * u, x + 50f * u, y + 6f * u)
            c.drawRoundRect(k.rect, 6f * u, 6f * u, p)
        }

        // raio
        if (flash > 0.04f) {
            val rr = java.util.Random(idx * 7919L + 3L)
            var x = w * (0.15f + 0.5f * rr.nextFloat())
            var y = 0f
            k.path.reset()
            k.path.moveTo(x, y)
            while (y < h * 0.8f) {
                x += (rr.nextFloat() - 0.5f) * 34f * u
                y += (16f + rr.nextFloat() * 18f) * u
                k.path.lineTo(x, y)
            }
            p.style = Paint.Style.STROKE
            p.strokeJoin = Paint.Join.ROUND
            p.strokeWidth = 5f * u
            p.color = k.al(violet, 120f * min(1f, flash))
            c.drawPath(k.path, p)
            p.strokeWidth = 1.8f * u
            p.color = k.al(Color.WHITE, 255f * min(1f, flash))
            c.drawPath(k.path, p)
        }

        // fantasmas flutuando, ondulando
        for (i in ghosts.indices) {
            val m = ghosts[i]
            val life = k.frac(t * m.sp + m.ph)
            val x = w * (0.1f + 0.8f * k.frac(m.x + life * 0.5f)) + sin(t * 0.9f + m.ph * 6f) * m.sw * u
            val y = h * (0.28f + 0.5f * m.y) + sin(t * 1.1f + m.ph * 9f) * 10f * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            k.glow(c, x, y, m.sz * u * 1.1f, green, 40f * a)
            k.iconXY(c, "ghost", k.al(Color.parseColor("#E8F5FF"), 120f * a), x, y, m.sz * u, sin(t + m.ph * 5f) * 8f, if (cos(t * 0.5f + m.ph) > 0f) 1f else -1f, 1f)
        }

        // morcegos cruzando o céu com asas batendo
        for (i in bats.indices) {
            val m = bats[i]
            val life = k.frac(t * m.sp + m.ph)
            val dir = if (i % 2 == 0) 1f else -1f
            val x = if (dir > 0f) w * (-0.1f + 1.2f * life) else w * (1.1f - 1.2f * life)
            val y = h * (0.1f + 0.5f * m.y) + sin(life * 18f + m.ph * 6f) * 12f * u
            val flap = 0.35f + 0.65f * abs(sin(t * 9f + m.ph * 11f))
            k.iconXY(c, "bat", k.al(Color.parseColor("#0A0512"), 245f), x, y, m.sz * u, sin(life * 18f + m.ph) * 10f, dir * 1f, flap)
        }

        // olhos brilhando no escuro, piscando
        for (i in 0 until 4) {
            val ex = w * (0.1f + 0.27f * i + 0.04f * sin(i * 3f))
            val ey = h * (0.62f + 0.16f * sin(i * 2.1f + 1f))
            val cyc = k.frac(t * 0.11f + i * 0.27f)
            val eyeOpen = if (cyc > 0.9f) 0.1f else 1f
            val vis = k.bump(k.frac(t * 0.07f + i * 0.31f), 0.5f, 0.28f)
            if (vis < 0.05f) continue
            val col = if (i % 2 == 0) green else Color.parseColor("#FF4F5E")
            val a = vis * k.edge(ey, h)
            for (s in 0 until 2) {
                val sx = ex + (s - 0.5f) * 11f * u
                k.glow(c, sx, ey, 8f * u, col, 140f * a)
                p.style = Paint.Style.FILL
                p.color = k.al(col, 255f * a)
                k.rect.set(sx - 3.4f * u, ey - 1.5f * u * eyeOpen, sx + 3.4f * u, ey + 1.5f * u * eyeOpen)
                c.drawOval(k.rect, p)
            }
        }

        // aranha descendo no fio
        val sy = (h * 0.12f) + (h * 0.22f) * (0.5f + 0.5f * sin(t * 0.5f))
        val sx = w * 0.08f
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f * u
        p.color = k.al(Color.WHITE, 150f)
        c.drawLine(sx, 0f, sx, sy, p)
        p.style = Paint.Style.FILL
        p.color = k.al(Color.parseColor("#05020A"), 255f)
        c.drawCircle(sx, sy + 4f * u, 4.6f * u, p)
        c.drawCircle(sx, sy - 1f * u, 2.8f * u, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.2f * u
        p.color = k.al(Color.parseColor("#05020A"), 255f)
        for (s in 0 until 4) {
            val ky = sy + (s - 1.5f) * 2.4f * u + 3f * u
            val wig = sin(t * 5f + s) * 1.2f * u
            c.drawLine(sx, ky, sx - 8f * u, ky + 3f * u + wig, p)
            c.drawLine(sx, ky, sx + 8f * u, ky + 3f * u - wig, p)
        }
        p.style = Paint.Style.FILL
        p.color = k.al(Color.parseColor("#FF4F5E"), 255f)
        c.drawCircle(sx - 1.4f * u, sy + 4f * u, 0.9f * u, p)
        c.drawCircle(sx + 1.4f * u, sy + 4f * u, 0.9f * u, p)

        // lápides na base e galhos mortos
        val baseY = h - 12f * u
        for (i in 0 until 7) {
            val gx = w * (0.04f + 0.15f * i + 0.02f * sin(i * 4f))
            k.icon(c, "tomb", k.al(Color.parseColor("#0C0716"), 255f), gx, baseY - 8f * u, (22f + 8f * ((i * 5) % 3)) * u, (i % 3 - 1) * 5f)
        }

        // neblina rastejando no chão
        for (m in fog) {
            val x = w * (k.frac(m.x + t * m.sp) * 1.5f - 0.25f)
            val y = h * (0.62f + 0.36f * m.y)
            val r = m.sz * u
            p.style = Paint.Style.FILL
            for (s in 0 until 4) {
                p.color = k.al(Color.parseColor("#C9B8F0"), 11f * (1f + s * 0.3f))
                k.rect.set(x - r * (1f - s * 0.18f), y - r * 0.17f * (1f - s * 0.12f), x + r * (1f - s * 0.18f), y + r * 0.17f * (1f - s * 0.12f))
                c.drawOval(k.rect, p)
            }
        }

        // poeirinha de espírito subindo
        for (m in motes) {
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1f - life)
            val x = w * m.x + sin(t + m.ph * 8f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            p.style = Paint.Style.FILL
            p.color = k.al(green, 210f * a)
            c.drawCircle(x, y, m.sz * u, p)
        }

        // clarão do relâmpago por cima de tudo
        if (flash > 0.02f) {
            p.style = Paint.Style.FILL
            p.color = k.al(Color.parseColor("#E9E4FF"), 120f * min(1f, flash))
            c.drawRect(0f, 0f, w, h, p)
        }
    }
}

// ======================================================================================
// FANTASIA: reino encantado. Aurora, círculo mágico, borboletas, castelo e poeira de fada.
// ======================================================================================
private class FantasiaScene : AuraScene {
    private var kit: SceneKit? = null
    private var stars: List<SceneKit.Mote> = emptyList()
    private var flies: List<SceneKit.Mote> = emptyList()
    private var dust: List<SceneKit.Mote> = emptyList()

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        stars = k.motes(30, 301L, 0.4f, 1.3f, 3f, 7f, 0f)
        flies = k.motes(5, 302L, 0.06f, 0.13f, 18f, 28f, 0f)
        dust = k.motes(22, 303L, 0.03f, 0.07f, 2f, 4.5f, 22f)
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        val u = k.u
        val p = k.p
        val cyan = Color.parseColor("#7FFFE0")
        val sky = Color.parseColor("#6FA8FF")
        val magenta = Color.parseColor("#E39BFF")
        val gold = Color.parseColor("#FFE9A0")

        k.vgrad(c, w, h, Color.parseColor("#14205E"), Color.parseColor("#3C3A9C"), Color.parseColor("#5C8FE8"))

        // estrelas ao fundo
        for (m in stars) {
            val y = h * m.y * 0.8f
            val s = sin(t * m.sp * 2f + m.ph * k.twoPi)
            val kk = 0.35f + 0.65f * (0.5f + 0.5f * s)
            k.star4(c, w * m.x, y, m.sz * u * (0.4f + 0.5f * kk), t * 10f + m.ph * 90f, Color.WHITE, kk * k.edge(y, h))
        }

        // fitas de aurora ondulando
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        for (b in 0 until 3) {
            val col = if (b == 0) cyan else if (b == 1) magenta else sky
            for (layer in 0 until 3) {
                k.path.reset()
                for (s in 0..16) {
                    val fx = s / 16f
                    val x = w * fx
                    val y = h * (0.22f + 0.2f * b) + sin(fx * 5f + t * (0.35f + 0.1f * b) + b * 2f) * 16f * u +
                        sin(fx * 11f - t * 0.6f) * 5f * u
                    if (s == 0) k.path.moveTo(x, y) else k.path.lineTo(x, y)
                }
                p.strokeWidth = (30f - layer * 9f) * u
                p.color = k.al(col, 22f + layer * 14f)
                c.drawPath(k.path, p)
            }
        }
        p.strokeCap = Paint.Cap.BUTT

        // círculo mágico girando atrás da capa
        val cx = w / 2f
        val cy = h / 2f
        val rr = min(w, h) * 0.5f
        val pulse = 0.85f + 0.15f * sin(t * 1.4f)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f * u
        p.color = k.al(gold, 150f * pulse)
        c.drawCircle(cx, cy, rr, p)
        p.color = k.al(cyan, 120f * pulse)
        c.drawCircle(cx, cy, rr * 0.86f, p)
        for (layer in 0 until 2) {
            val dir = if (layer == 0) 1f else -1f
            val a0 = t * 0.18f * dir
            k.path.reset()
            for (i in 0..5) {
                val a = a0 + i * k.twoPi / 6f * 2f % k.twoPi
                val x = cx + cos(a) * rr * 0.86f
                val y = cy + sin(a) * rr * 0.86f
                if (i == 0) k.path.moveTo(x, y) else k.path.lineTo(x, y)
            }
            k.path.close()
            p.color = k.al(if (layer == 0) gold else magenta, 110f * pulse)
            c.drawPath(k.path, p)
        }
        for (i in 0 until 24) {
            val a = -t * 0.12f + i * k.twoPi / 24f
            val r0 = rr * (if (i % 2 == 0) 0.92f else 0.95f)
            p.color = k.al(gold, 140f)
            c.drawLine(cx + cos(a) * r0, cy + sin(a) * r0, cx + cos(a) * rr * 1.03f, cy + sin(a) * rr * 1.03f, p)
        }
        for (i in 0 until 6) {
            val a = t * 0.25f + i * k.twoPi / 6f
            k.star4(c, cx + cos(a) * rr, cy + sin(a) * rr, 4.5f * u, a * 57f, gold, 0.9f * pulse)
        }

        // castelo distante flutuando entre as nuvens
        val fl = sin(t * 0.5f) * 3f * u
        k.glow(c, w * 0.16f, h * 0.84f + fl, 52f * u, magenta, 90f)
        k.icon(c, "castle", k.al(Color.parseColor("#2A2468"), 240f), w * 0.16f, h * 0.84f + fl, 54f * u, 0f)
        k.icon(c, "castle", k.al(Color.parseColor("#2A2468"), 200f), w * 0.9f, h * 0.9f - fl, 34f * u, 0f)
        p.style = Paint.Style.FILL
        for (i in 0 until 4) {
            val x = w * (k.frac(0.2f * i + t * 0.01f) * 1.3f - 0.15f)
            p.color = k.al(Color.WHITE, 34f)
            k.rect.set(x - 46f * u, h * (0.9f + 0.03f * i) - 9f * u, x + 46f * u, h * (0.9f + 0.03f * i) + 9f * u)
            c.drawOval(k.rect, p)
        }

        // borboletas mágicas com rastro de brilho
        for (i in flies.indices) {
            val m = flies[i]
            val col = if (i % 3 == 0) gold else if (i % 3 == 1) cyan else magenta
            for (tr in 3 downTo 0) {
                val tt = t - tr * 0.18f
                val x = w * (0.5f + 0.44f * sin(tt * m.sp * 6f + m.ph * k.twoPi))
                val y = h * (0.5f + 0.4f * sin(tt * m.sp * 4.3f + m.ph * 9f + 1f))
                if (tr > 0) {
                    p.style = Paint.Style.FILL
                    p.color = k.al(col, 130f * (1f - tr * 0.25f) * k.edge(y, h))
                    c.drawCircle(x, y, 2.4f * u * (1f - tr * 0.2f), p)
                } else {
                    val flap = 0.25f + 0.75f * abs(sin(t * 8f + m.ph * 10f))
                    k.glow(c, x, y, m.sz * u * 0.9f, col, 90f * k.edge(y, h))
                    k.iconXY(c, "butterfly", k.al(col, 245f * k.edge(y, h)), x, y, m.sz * u, sin(t * 1.2f + m.ph) * 14f, flap, 1f)
                }
            }
        }

        // poeira de fada subindo em espiral
        for (m in dust) {
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1.05f - 1.1f * life)
            val x = w * m.x + sin(t * 1.1f + m.ph * 9f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            k.star4(c, x, y, m.sz * u, t * 50f + m.ph * 90f, if (m.ph > 0.5f) gold else Color.WHITE, a)
        }

        // varinha que desenha uma espiral de brilhos de vez em quando
        val per = 8f
        val idx = floor(t / per).toInt()
        val q = (t - idx * per) / 2.2f
        if (q in 0f..1f) {
            val rr2 = java.util.Random(idx * 613L + 11L)
            val wx = w * (0.15f + 0.7f * rr2.nextFloat())
            val wy = h * (0.25f + 0.5f * rr2.nextFloat())
            for (s in 0 until 22) {
                val f = q - s * 0.018f
                if (f < 0f) continue
                val ang = f * 3.4f * k.twoPi
                val rad = f * 34f * u
                val a = (1f - s / 22f) * k.env(q)
                k.star4(c, wx + cos(ang) * rad, wy + sin(ang) * rad, (4.5f - s * 0.15f) * u, ang * 57f, gold, a)
            }
            k.icon(c, "wand", k.al(Color.WHITE, 235f * k.env(q)), wx + cos(q * 3.4f * k.twoPi) * q * 34f * u + 8f * u, wy + sin(q * 3.4f * k.twoPi) * q * 34f * u + 8f * u, 24f * u, 40f)
        }
    }
}

// ======================================================================================
// AÇÃO: explosão e velocidade. Linhas de impacto, faíscas, cortes, chamas e ondas de choque.
// ======================================================================================
private class AcaoScene : AuraScene {
    private var kit: SceneKit? = null
    private var sparks: List<SceneKit.Mote> = emptyList()
    private var embers: List<SceneKit.Mote> = emptyList()
    private var lines: List<SceneKit.Mote> = emptyList()
    private var bolts: List<SceneKit.Mote> = emptyList()

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        sparks = k.motes(26, 401L, 0.35f, 0.8f, 1.6f, 3.2f, 0f)
        embers = k.motes(16, 402L, 0.05f, 0.12f, 1.6f, 3.4f, 14f)
        lines = k.motes(34, 403L, 0.9f, 2.0f, 0.15f, 0.55f, 0f)
        bolts = k.motes(3, 404L, 0.12f, 0.2f, 20f, 30f, 0f)
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        val u = k.u
        val p = k.p
        val fire = Color.parseColor("#FF7A3D")
        val hot = Color.parseColor("#FFC24D")
        val red = Color.parseColor("#FF3B30")

        k.vgrad(c, w, h, Color.parseColor("#1B0504"), Color.parseColor("#7A1D14"), Color.parseColor("#FF6B3A"))

        val cx = w / 2f
        val cy = h / 2f
        val rMax = sqrt(w * w + h * h) * 0.6f

        // clarão central pulsando
        val ph = k.frac(t / 1.2f)
        val hit = k.bump(ph, 0.05f, 0.06f)
        k.glow(c, cx, cy, max(w, h) * (0.5f + 0.1f * hit), hot, 110f + 120f * hit)

        // linhas de velocidade (estilo mangá) saindo do centro
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        for (i in lines.indices) {
            val m = lines[i]
            val ang = m.x * k.twoPi
            val life = k.frac(t * m.sp + m.ph)
            val r0 = rMax * (0.18f + 0.82f * life)
            val len = rMax * m.sz * (0.3f + life)
            p.strokeWidth = (0.8f + 2.4f * m.y) * u
            p.color = k.al(if (i % 4 == 0) hot else Color.WHITE, 200f * k.env(life))
            c.drawLine(cx + cos(ang) * r0, cy + sin(ang) * r0, cx + cos(ang) * (r0 + len), cy + sin(ang) * (r0 + len), p)
        }
        p.strokeCap = Paint.Cap.BUTT

        // ondas de choque grossas saindo da capa
        if (hasPoster) {
            for (j in 0 until 3) {
                val life = k.frac(t * 0.45f + j / 3f)
                val e = life * 42f * u
                p.style = Paint.Style.STROKE
                p.strokeWidth = (5f - 4.2f * life) * u
                p.color = k.al(if (j == 1) hot else Color.WHITE, 210f * (1f - life) * (1f - life))
                k.rect.set(poster.left - e, poster.top - e, poster.right + e, poster.bottom + e)
                c.drawRoundRect(k.rect, 20f * u + e, 20f * u + e, p)
            }
        }

        // cortes de espada atravessando a tela
        val per = 4.2f
        val idx = floor(t / per).toInt()
        val q = (t - idx * per) / 0.5f
        if (q in 0f..1f) {
            val rr = java.util.Random(idx * 877L + 1L)
            val fromLeft = rr.nextBoolean()
            val y0 = h * (0.1f + 0.3f * rr.nextFloat())
            val y1 = h * (0.6f + 0.3f * rr.nextFloat())
            val x0 = if (fromLeft) -10f * u else w + 10f * u
            val x1 = if (fromLeft) w + 10f * u else -10f * u
            val head = q
            val tail = max(0f, q - 0.45f)
            val ax = x0 + (x1 - x0) * tail
            val ay = y0 + (y1 - y0) * tail
            val bx = x0 + (x1 - x0) * head
            val by = y0 + (y1 - y0) * head
            p.style = Paint.Style.STROKE
            p.strokeCap = Paint.Cap.ROUND
            p.strokeWidth = 9f * u
            p.color = k.al(red, 130f * (1f - q * 0.6f))
            c.drawLine(ax, ay, bx, by, p)
            p.strokeWidth = 3f * u
            p.color = k.al(Color.WHITE, 255f * (1f - q * 0.5f))
            c.drawLine(ax, ay, bx, by, p)
            p.strokeCap = Paint.Cap.BUTT
        }

        // raios correndo na diagonal
        for (i in bolts.indices) {
            val m = bolts[i]
            val life = k.frac(t * m.sp + m.ph)
            val x = w * (-0.1f + 1.2f * life)
            val y = h * (0.15f + 0.7f * m.y) - life * 30f * u
            k.icon(c, "bolt", k.al(hot, 235f * k.env(life) * k.edge(y, h)), x, y, m.sz * u, 18f)
        }

        // faíscas voando em arco, com gravidade
        for (i in sparks.indices) {
            val m = sparks[i]
            val life = k.frac(t * m.sp + m.ph)
            val ang = (-0.15f - 0.7f * m.x) * Math.PI.toFloat() + (if (i % 2 == 0) 0f else Math.PI.toFloat() * 0.55f)
            val v = (60f + 150f * m.y) * u
            val x = cx + cos(ang) * v * life * 2f
            val y = h * 0.95f + sin(ang) * v * life * 2f + 160f * u * life * life
            val a = (1f - life) * k.edge(y, h)
            if (a <= 0.02f) continue
            p.style = Paint.Style.STROKE
            p.strokeCap = Paint.Cap.ROUND
            p.strokeWidth = m.sz * u
            p.color = k.al(if (i % 3 == 0) Color.WHITE else hot, 255f * a)
            c.drawLine(x, y, x - cos(ang) * 8f * u, y - sin(ang) * 8f * u - 3f * u * life, p)
            p.strokeCap = Paint.Cap.BUTT
        }

        // chamas dançando na base
        for (i in 0 until 9) {
            val fx = w * (0.02f + 0.115f * i)
            val sc = 0.75f + 0.45f * sin(t * 6f + i * 1.7f) * sin(t * 3.3f + i)
            val fh = (30f + 14f * ((i * 7) % 4)) * u * sc
            k.glow(c, fx, h - 14f * u, 24f * u, fire, 110f)
            k.icon(c, "flame", k.al(red, 240f), fx, h - 12f * u - fh * 0.4f, fh * 1.15f, sin(t * 4f + i) * 4f)
            k.icon(c, "flame", k.al(hot, 245f), fx, h - 10f * u - fh * 0.25f, fh * 0.72f, sin(t * 5f + i * 2f) * 5f)
        }

        // brasas subindo
        for (m in embers) {
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1.02f - 1.0f * life)
            val x = w * m.x + sin(t * 1.4f + m.ph * 9f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            p.style = Paint.Style.FILL
            p.color = k.al(hot, 230f * a)
            c.drawCircle(x, y, m.sz * u, p)
        }

        // flash branco rápido no "impacto"
        if (hit > 0.05f) {
            p.style = Paint.Style.FILL
            p.color = k.al(Color.WHITE, 70f * hit)
            c.drawRect(0f, 0f, w, h, p)
        }
    }
}

// ======================================================================================
// HISTÓRICO: palácio ao entardecer. Sol enorme, montanhas, pagode, lanternas, grous e pétalas.
// ======================================================================================
private class HistoricoScene : AuraScene {
    private var kit: SceneKit? = null
    private var lanterns: List<SceneKit.Mote> = emptyList()
    private var petals: List<SceneKit.Mote> = emptyList()
    private var birds: List<SceneKit.Mote> = emptyList()
    private var glints: List<SceneKit.Mote> = emptyList()
    private var mist: List<SceneKit.Mote> = emptyList()

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        lanterns = k.motes(7, 501L, 0.02f, 0.045f, 18f, 28f, 14f)
        petals = k.motes(12, 502L, 0.03f, 0.06f, 10f, 17f, 26f)
        birds = k.motes(5, 503L, 0.025f, 0.04f, 9f, 14f, 0f)
        glints = k.motes(20, 504L, 0.4f, 1.1f, 2.5f, 5f, 0f)
        mist = k.motes(5, 505L, 0.008f, 0.02f, 90f, 150f, 0f)
    }

    private fun ridge(c: Canvas, k: SceneKit, w: Float, h: Float, base: Float, amp: Float, freq: Float, off: Float, color: Int) {
        k.path.reset()
        k.path.moveTo(0f, h)
        for (s in 0..20) {
            val fx = s / 20f
            val y = base + sin(fx * freq + off) * amp + sin(fx * freq * 2.3f + off * 1.7f) * amp * 0.45f
            k.path.lineTo(w * fx, y)
        }
        k.path.lineTo(w, h)
        k.path.close()
        k.p.style = Paint.Style.FILL
        k.p.color = color
        c.drawPath(k.path, k.p)
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        val u = k.u
        val p = k.p
        val gold = Color.parseColor("#FFD27A")
        val crimson = Color.parseColor("#D6536D")
        val lantern = Color.parseColor("#FF5A4A")

        k.vgrad(c, w, h, Color.parseColor("#2B1236"), Color.parseColor("#B8344F"), Color.parseColor("#F2A65A"))

        // sol grande se pondo atrás das montanhas, com anéis de luz
        val sx = w * 0.3f
        val sy = h * 0.58f
        val sr = 46f * u
        k.glow(c, sx, sy, sr * 3.2f, gold, 150f + 25f * sin(t * 0.6f))
        p.style = Paint.Style.FILL
        p.color = k.al(Color.parseColor("#FFE7B0"), 255f)
        c.drawCircle(sx, sy, sr, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.2f * u
        for (i in 0 until 3) {
            val life = k.frac(t * 0.1f + i / 3f)
            p.color = k.al(gold, 120f * (1f - life))
            c.drawCircle(sx, sy, sr * (1.2f + life * 1.6f), p)
        }

        // pinceladas de tinta flutuando
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        for (m in mist) {
            val x = w * (k.frac(m.x + t * m.sp) * 1.4f - 0.2f)
            val y = h * (0.15f + 0.4f * m.y)
            p.strokeWidth = 6f * u
            p.color = k.al(Color.parseColor("#2B1236"), 38f)
            c.drawLine(x - m.sz * u * 0.5f, y, x + m.sz * u * 0.5f, y + 3f * u * sin(t * 0.3f + m.ph * 6f), p)
        }
        p.strokeCap = Paint.Cap.BUTT

        // grous voando em formação de V
        for (i in birds.indices) {
            val m = birds[i]
            val life = k.frac(t * m.sp + m.ph)
            val bx = w * (1.15f - 1.3f * life)
            val by = h * (0.14f + 0.18f * m.y) + sin(life * 12f + i) * 4f * u
            for (s in 0 until 3) {
                val ox = s * 17f * u
                val oy = (if (s == 0) 0f else (if (s == 1) -7f else 7f)) * u
                val flap = sin(t * 4f + i + s * 1.3f) * 3.4f * u
                p.style = Paint.Style.STROKE
                p.strokeWidth = 1.8f * u
                p.color = k.al(Color.parseColor("#1B0B24"), 235f)
                k.path.reset()
                k.path.moveTo(bx + ox - m.sz * u, by + oy - flap)
                k.path.quadTo(bx + ox - m.sz * u * 0.4f, by + oy - 1f * u, bx + ox, by + oy)
                k.path.quadTo(bx + ox + m.sz * u * 0.4f, by + oy - 1f * u, bx + ox + m.sz * u, by + oy - flap)
                c.drawPath(k.path, p)
            }
        }

        // montanhas em camadas, com paralaxe lenta
        ridge(c, k, w, h, h * 0.66f, 14f * u, 6f, t * 0.03f, k.al(Color.parseColor("#7A2540"), 235f))
        ridge(c, k, w, h, h * 0.76f, 12f * u, 7.5f, 2f + t * 0.05f, k.al(Color.parseColor("#4A1830"), 245f))

        // pagode e palácio em silhueta sobre a crista
        k.icon(c, "pagoda", k.al(Color.parseColor("#240D22"), 255f), w * 0.86f, h * 0.7f, 66f * u, 0f)
        k.icon(c, "pagoda", k.al(Color.parseColor("#2E1128"), 255f), w * 0.1f, h * 0.8f, 40f * u, 0f)
        ridge(c, k, w, h, h * 0.9f, 8f * u, 9f, 5f + t * 0.07f, k.al(Color.parseColor("#1D0A1C"), 255f))

        // lanternas de papel subindo, balançando e brilhando
        for (i in lanterns.indices) {
            val m = lanterns[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1.0f - 1.05f * life)
            val x = w * m.x + sin(t * 0.7f + m.ph * 8f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            val fl = 0.75f + 0.25f * sin(t * 7f + m.ph * 20f)
            k.glow(c, x, y, m.sz * u * 1.3f, lantern, 130f * a * fl)
            k.icon(c, "lantern", k.al(lantern, 250f * a), x, y, m.sz * u, sin(t + m.ph * 6f) * 6f)
            k.icon(c, "lantern", k.al(Color.parseColor("#FFE7B0"), 120f * a * fl), x, y, m.sz * u * 0.7f, sin(t + m.ph * 6f) * 6f)
        }

        // pétalas de ameixeira caindo devagar
        for (i in petals.indices) {
            val m = petals[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (-0.08f + 1.16f * life)
            val x = w * (m.x - 0.12f * life) + sin(t * 0.6f + m.ph * 6f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            k.iconXY(c, "petal", k.al(if (i % 2 == 0) Color.parseColor("#FFD3E0") else crimson, 235f * a), x, y, m.sz * u, t * 35f + m.ph * 360f, 0.5f + 0.5f * abs(sin(t + m.ph * 7f)), 1f)
        }

        // brilhos dourados (poeira de seda)
        for (m in glints) {
            val s = sin(t * m.sp * 2f + m.ph * k.twoPi)
            val kk = if (s > 0f) s * s * s else 0f
            if (kk <= 0.03f) continue
            val y = h * m.y
            val a = kk * k.edge(y, h)
            if (a <= 0.02f) continue
            k.star4(c, w * m.x, y, m.sz * u * (0.5f + 0.7f * kk), t * 16f + m.ph * 90f, gold, a)
        }
    }
}

// ======================================================================================
// FICÇÃO CIENTÍFICA: espaço profundo + cidade neon. Planeta, OVNI, foguete, grade e chuva binária.
// ======================================================================================
private class ScifiScene : AuraScene {
    private var kit: SceneKit? = null
    private var far: List<SceneKit.Mote> = emptyList()
    private var near: List<SceneKit.Mote> = emptyList()
    private var rain: List<SceneKit.Mote> = emptyList()

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        far = k.motes(40, 601L, 0.4f, 1.2f, 1f, 2f, 0f)
        near = k.motes(16, 602L, 0.5f, 1.3f, 2f, 4f, 0f)
        rain = k.motes(9, 603L, 0.08f, 0.16f, 8f, 10f, 0f)
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        val u = k.u
        val p = k.p
        val cyan = Color.parseColor("#4DF3FF")
        val violet = Color.parseColor("#8B7BFF")
        val pink = Color.parseColor("#FF4FD8")
        val lime = Color.parseColor("#B6FF5C")

        k.vgrad(c, w, h, Color.parseColor("#04051A"), Color.parseColor("#1A1470"), Color.parseColor("#3B2BC9"))

        // nebulosas
        k.glow(c, w * 0.15f, h * 0.25f, 120f * u, pink, 70f + 20f * sin(t * 0.3f))
        k.glow(c, w * 0.9f, h * 0.55f, 140f * u, cyan, 60f + 20f * sin(t * 0.25f + 1f))

        // estrelas em duas profundidades, derivando
        for (m in far) {
            val y = h * m.y * 0.78f
            val a = (0.4f + 0.6f * sin(t * m.sp * 2f + m.ph * k.twoPi) * sin(t * m.sp * 2f + m.ph * k.twoPi)) * k.edge(y, h)
            p.style = Paint.Style.FILL
            p.color = k.al(Color.WHITE, 220f * a)
            c.drawCircle(k.frac(m.x + t * 0.004f) * w, y, m.sz * u * 0.7f, p)
        }
        for (m in near) {
            val y = h * m.y * 0.78f
            val a = k.edge(y, h) * (0.5f + 0.5f * sin(t * m.sp * 3f + m.ph * 6f))
            k.star4(c, k.frac(m.x + t * 0.01f) * w, y, m.sz * u, 0f, Color.WHITE, a)
        }

        // planeta com anel girando devagar
        val px = w * 0.84f
        val py = h * 0.2f
        k.glow(c, px, py, 52f * u, violet, 120f)
        k.icon(c, "planet", k.al(Color.parseColor("#C9C1FF"), 255f), px, py, 56f * u, -18f + sin(t * 0.3f) * 4f)
        val px2 = w * 0.1f
        val py2 = h * 0.4f
        k.icon(c, "planet", k.al(Color.parseColor("#FF9BE8"), 190f), px2, py2, 26f * u, 24f + sin(t * 0.4f) * 5f)

        // OVNI cruzando com feixe de luz
        val per = 11f
        val idx = floor(t / per).toInt()
        val q = (t - idx * per) / 5f
        if (q in 0f..1f) {
            val rr = java.util.Random(idx * 431L + 9L)
            val ux = w * (1.1f - 1.2f * q)
            val uy = h * (0.12f + 0.2f * rr.nextFloat()) + sin(q * 14f) * 5f * u
            val a = k.env(q)
            val beam = k.bump(k.frac(q * 3f), 0.5f, 0.28f)
            if (beam > 0.05f) {
                k.path.reset()
                k.path.moveTo(ux - 6f * u, uy + 10f * u)
                k.path.lineTo(ux + 6f * u, uy + 10f * u)
                k.path.lineTo(ux + 30f * u, h * 0.9f)
                k.path.lineTo(ux - 30f * u, h * 0.9f)
                k.path.close()
                p.style = Paint.Style.FILL
                p.color = k.al(lime, 70f * beam * a)
                c.drawPath(k.path, p)
            }
            k.glow(c, ux, uy, 26f * u, lime, 90f * a)
            k.icon(c, "ufo", k.al(Color.parseColor("#D9FFE0"), 255f * a), ux, uy, 34f * u, sin(q * 20f) * 6f)
        }

        // foguete em órbita com rastro de plasma
        val ra = t * 0.35f
        for (tr in 6 downTo 0) {
            val aa = ra - tr * 0.07f
            val rx = w * (0.5f + 0.46f * cos(aa))
            val ry = h * (0.5f + 0.4f * sin(aa))
            if (tr > 0) {
                p.style = Paint.Style.FILL
                p.color = k.al(if (tr % 2 == 0) cyan else pink, 150f * (1f - tr / 7f) * k.edge(ry, h))
                c.drawCircle(rx, ry, (3.4f - tr * 0.4f) * u, p)
            } else {
                k.icon(c, "rocket", k.al(Color.WHITE, 250f * k.edge(ry, h)), rx, ry, 24f * u, (ra * 57.3f) + 135f)
            }
        }

        // grade neon em perspectiva no chão, avançando
        val horizon = h * 0.7f
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.2f * u
        for (i in 0..14) {
            val fx = (i - 7f) / 7f
            p.color = k.al(pink, 120f)
            c.drawLine(w / 2f + fx * w * 0.05f, horizon, w / 2f + fx * w * 1.3f, h, p)
        }
        for (i in 0 until 8) {
            val f = k.frac(i / 8f + t * 0.12f)
            val y = horizon + (h - horizon) * f * f
            p.color = k.al(cyan, 150f * f)
            c.drawLine(0f, y, w, y, p)
        }
        p.color = k.al(cyan, 200f)
        p.strokeWidth = 1.6f * u
        c.drawLine(0f, horizon, w, horizon, p)

        // chuva de código binário nas laterais
        p.style = Paint.Style.FILL
        p.typeface = k.mono
        p.textSize = 10f * u
        for (i in rain.indices) {
            val m = rain[i]
            val colX = if (i % 2 == 0) w * (0.02f + 0.07f * (i / 2)) else w * (0.98f - 0.07f * (i / 2)) - 8f * u
            val head = k.frac(t * m.sp + m.ph)
            for (s in 0 until 7) {
                val y = (head * 1.3f - 0.1f) * h - s * 12f * u
                val a = (1f - s / 7f) * k.edge(y, h)
                if (a <= 0.02f) continue
                val bit = if ((floor(t * 4f + s * 3f + i * 5f).toInt() and 1) == 0) "0" else "1"
                p.color = k.al(if (s == 0) Color.WHITE else cyan, 200f * a)
                c.drawText(bit, colX, y, p)
            }
        }
        p.typeface = null

        // linha de varredura de radar subindo
        val sc = k.frac(t * 0.18f)
        val syy = h * sc
        p.style = Paint.Style.FILL
        p.color = k.al(cyan, 26f)
        c.drawRect(0f, syy - 16f * u, w, syy, p)
        p.color = k.al(cyan, 150f)
        c.drawRect(0f, syy - 1f * u, w, syy, p)

        // cometa de tempos em tempos
        val q2 = (t - floor(t / 8f) * 8f) / 1.4f
        if (q2 in 0f..1f) {
            val hx = w * (1.05f - 1.1f * q2)
            val hy = h * (0.05f + 0.45f * q2)
            for (s in 0 until 10) {
                val f = s / 10f
                p.color = k.al(Color.WHITE, 230f * (1f - f) * k.env(q2))
                p.strokeWidth = (3f - 2.6f * f) * u
                p.style = Paint.Style.STROKE
                c.drawLine(hx + f * 70f * u, hy - f * 30f * u, hx + (f + 0.1f) * 70f * u, hy - (f + 0.1f) * 30f * u, p)
            }
            p.style = Paint.Style.FILL
            p.color = k.al(Color.WHITE, 255f * k.env(q2))
            c.drawCircle(hx, hy, 2.8f * u, p)
        }
    }
}
