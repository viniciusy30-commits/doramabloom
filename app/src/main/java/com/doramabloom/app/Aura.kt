package com.doramabloom.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Region
import android.graphics.Shader
import android.os.Build
import android.view.View
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

/**
 * Animação do fundo em volta da capa no Início (a capa em si fica de fora).
 * Camadas, de trás para frente: auroras coloridas à deriva, raios de luz girando, ondas de luz
 * saindo da capa, bolhas brilhantes subindo, símbolos do gênero caindo como pétalas,
 * estrelinhas cintilando, vagalumes com rastro e, de tempos em tempos, uma estrela cadente.
 * Tudo é calculado pelo relógio (sem estado), então é leve e nunca "trava" em loop.
 */
class AuraView(ctx: Context, private val cover: CoverView) : View(ctx) {

    private class Mote(val x: Float, val y: Float, val ph: Float, val sp: Float, val sz: Float, val sw: Float)

    private fun makeMotes(n: Int, seed: Long, spLo: Float, spHi: Float, szLo: Float, szHi: Float, swHi: Float): List<Mote> {
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

    private val u = ctx.resources.displayMetrics.density
    private val start = System.nanoTime()
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ic = IconDrawable("heart", Color.WHITE)
    private val poster = RectF()
    private val grown = RectF()
    private val clip = Path()
    private val star = Path()
    private val ray = Path()
    private val twoPi = (2.0 * Math.PI).toFloat()
    private val kit = SceneKit(u)

    private val bubbles = makeMotes(15, 11L, 0.035f, 0.075f, 5f, 15f, 18f)
    private val petalsM = makeMotes(13, 23L, 0.03f, 0.06f, 13f, 22f, 26f)
    private val stars = makeMotes(24, 37L, 0.5f, 1.4f, 4f, 9f, 0f)
    private val flies = makeMotes(9, 51L, 0.15f, 0.4f, 2f, 3.2f, 0f)

    private var g: Genre? = null
    private var tints: List<Int> = listOf(Color.WHITE, Color.WHITE, Color.WHITE)
    private var icons: List<String> = listOf("heart", "sparkle")
    private var aur: Array<RadialGradient> = emptyArray()
    private var rayShader: RadialGradient? = null

    // destaque da capa: halo, moldura dupla, cometas de luz, brilho de foto e reflexo passando
    private val ring = Path()
    private val ringSeg = Path()
    private val pmeas = PathMeasure()
    private val ringPos = FloatArray(2)
    private val ringTan = FloatArray(2)
    private val clipIn = Path()
    private val inner = RectF()
    private var halo: RadialGradient? = null
    private var haloR = 0f
    private val gloss = LinearGradient(
        0f, 0f, 1f, 1f,
        intArrayOf(Color.argb(85, 255, 255, 255), Color.argb(0, 255, 255, 255), Color.argb(0, 255, 255, 255)),
        floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
    )
    private val sweep = LinearGradient(
        0f, 0f, 1f, 0f,
        intArrayOf(Color.argb(0, 255, 255, 255), Color.argb(150, 255, 255, 255), Color.argb(0, 255, 255, 255)),
        null, Shader.TileMode.CLAMP
    )

    init {
        star.moveTo(0f, -1f)
        star.lineTo(0.16f, -0.16f)
        star.lineTo(1f, 0f)
        star.lineTo(0.16f, 0.16f)
        star.lineTo(0f, 1f)
        star.lineTo(-0.16f, 0.16f)
        star.lineTo(-1f, 0f)
        star.lineTo(-0.16f, -0.16f)
        star.close()
    }

    private fun al(color: Int, a: Float): Int =
        Color.argb(a.coerceIn(0f, 255f).toInt(), Color.red(color), Color.green(color), Color.blue(color))

    private fun frac(x: Float): Float = x - floor(x)

    /** 0 nas pontas da vida e 1 no meio: entra e sai suave. */
    private fun env(life: Float): Float = sin(life * twoPi / 2f)

    /** Some suavemente perto das bordas de cima e de baixo. */
    private fun edge(y: Float, h: Float): Float {
        val a = (y / (26f * u)).coerceIn(0f, 1f)
        val b = ((h - 14f * u - y) / (34f * u)).coerceIn(0f, 1f)
        return a * a * (3f - 2f * a) * b * b * (3f - 2f * b)
    }

    fun setGenre(genre: Genre) {
        g = genre
        tints = listOf(
            mixColor(genre.primary, Color.WHITE, 0.55f),
            mixColor(genre.primary, Color.WHITE, 0.25f),
            Color.WHITE
        )
        icons = (genre.petals + listOf("sparkle", "heart")).distinct()
        aur = Array(3) { i ->
            RadialGradient(0f, 0f, 100f, intArrayOf(al(tints[i], 120f), al(tints[i], 0f)), null, Shader.TileMode.CLAMP)
        }
        buildRays()
        halo = null
        invalidate()
    }

    private fun buildRays() {
        if (width <= 0 || height <= 0) return
        val r = max(width, height) * 0.95f
        rayShader = RadialGradient(
            width / 2f, height / 2f, r,
            intArrayOf(al(Color.WHITE, 70f), al(Color.WHITE, 0f)), null, Shader.TileMode.CLAMP
        )
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        buildRays()
    }

    private fun drawIcon(c: Canvas, name: String, color: Int, cx: Float, cy: Float, size: Float, rot: Float) {
        ic.name = name
        ic.color = color
        val h = (size / 2f).toInt()
        ic.setBounds(-h, -h, h, h)
        c.save()
        c.translate(cx, cy)
        c.rotate(rot)
        ic.draw(c)
        c.restore()
    }

    private fun drawStar(c: Canvas, x: Float, y: Float, size: Float, rot: Float, a: Float) {
        p.style = Paint.Style.FILL
        p.color = al(Color.WHITE, 38f * a)
        c.drawCircle(x, y, size * 1.7f, p)
        p.color = al(Color.WHITE, 255f * a)
        c.save()
        c.translate(x, y)
        c.rotate(rot)
        c.scale(size, size)
        c.drawPath(star, p)
        c.restore()
    }

    override fun onDraw(c: Canvas) {
        val gg = g ?: return
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val t = (System.nanoTime() - start) / 1_000_000_000f

        c.save()
        c.clipRect(0f, 0f, w, h - 12f * u)
        // a capa fica de fora: nada passa por cima dela
        val hasPoster = cover.posterRect(poster)
        if (hasPoster) {
            clip.reset()
            clip.addRoundRect(poster, 20f * u, 20f * u, Path.Direction.CW)
            if (Build.VERSION.SDK_INT >= 26) c.clipOutPath(clip)
            else @Suppress("DEPRECATION") c.clipPath(clip, Region.Op.DIFFERENCE)
        }

        // gêneros com cena própria (Romance, Terror, Fantasia, Ação, Histórico, Ficção científica)
        val scene = AuraScenes.forKey(gg.key)
        if (scene != null) {
            scene.draw(c, w, h, t, poster, hasPoster, kit)
            if (hasPoster) drawSpotlight(c, t, gg)
            c.restore()
            drawSeam(c, t, gg)
            if (hasPoster) drawPosterGloss(c, t)
            if (isShown) postInvalidateOnAnimation()
            return
        }

        // 1) auroras: manchas de luz colorida à deriva
        p.style = Paint.Style.FILL
        for (i in 0 until aur.size) {
            val cx = w * (0.5f + 0.5f * sin(t * 0.13f + i * 2.1f))
            val cy = h * (0.5f + 0.45f * cos(t * 0.11f + i * 1.7f))
            val r = max(w, h) * (0.55f + 0.08f * sin(t * 0.2f + i))
            p.shader = aur[i]
            p.alpha = (170f + 70f * sin(t * 0.4f + i)).toInt().coerceIn(0, 255)
            c.save()
            c.translate(cx, cy)
            c.scale(r / 100f, r / 100f)
            c.drawCircle(0f, 0f, 100f, p)
            c.restore()
        }

        // 2) raios de luz girando devagar, em dois sentidos
        val rs = rayShader
        if (rs != null) {
            p.shader = rs
            p.alpha = 255
            val rr = max(w, h) * 0.95f
            val half = 0.07f
            for (set in 0 until 2) {
                val dir = if (set == 0) 1f else -1f
                val base = t * 0.07f * dir + set * 0.5f
                for (i in 0 until 6) {
                    val a = base + i * (twoPi / 6f)
                    ray.reset()
                    ray.moveTo(w / 2f, h / 2f)
                    ray.lineTo(w / 2f + rr * cos(a - half), h / 2f + rr * sin(a - half))
                    ray.lineTo(w / 2f + rr * cos(a + half), h / 2f + rr * sin(a + half))
                    ray.close()
                    c.drawPath(ray, p)
                }
            }
            p.shader = null
            p.alpha = 255
        }
        p.shader = null

        // 3) ondas de luz que saem da capa
        if (hasPoster) {
            p.style = Paint.Style.STROKE
            for (k in 0 until 3) {
                val life = frac(t * 0.22f + k / 3f)
                val e = life * 30f * u
                p.strokeWidth = (2.2f - 1.6f * life) * u
                p.color = al(if (k == 1) tints[0] else Color.WHITE, 130f * (1f - life) * (1f - life))
                grown.set(poster.left - e, poster.top - e, poster.right + e, poster.bottom + e)
                c.drawRoundRect(grown, 20f * u + e, 20f * u + e, p)
            }
        }

        // 4) bolhas brilhantes subindo
        for (m in bubbles) {
            val life = frac(t * m.sp + m.ph)
            val y = h * (1.05f - 1.15f * life)
            val x = w * m.x + sin(t * 0.8f + m.ph * twoPi) * m.sw * u
            val a = env(life) * edge(y, h)
            if (a <= 0.02f) continue
            val r = m.sz * u
            p.style = Paint.Style.FILL
            p.color = al(Color.WHITE, 26f * a)
            c.drawCircle(x, y, r, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 1.2f * u
            p.color = al(Color.WHITE, 150f * a)
            c.drawCircle(x, y, r, p)
            p.style = Paint.Style.FILL
            p.color = al(Color.WHITE, 230f * a)
            c.drawCircle(x - r * 0.35f, y - r * 0.35f, r * 0.18f, p)
        }

        // 5) símbolos do gênero caindo como pétalas, girando
        for (i in 0 until petalsM.size) {
            val m = petalsM[i]
            val life = frac(t * m.sp + m.ph)
            val y = h * (-0.08f + 1.16f * life)
            val x = w * m.x + sin(t * 0.6f + m.ph * twoPi) * m.sw * u
            val a = env(life) * edge(y, h)
            if (a <= 0.02f) continue
            val s = m.sz * u * (0.88f + 0.12f * sin(t * 2f + m.ph * 9f))
            val col = if (i % 3 == 0) Color.WHITE else if (i % 3 == 1) tints[0] else tints[1]
            val rot = t * (18f + m.sp * 300f) * (if (i % 2 == 0) 1f else -1f) + m.ph * 360f
            drawIcon(c, icons[i % icons.size], al(col, 235f * a), x, y, s, rot)
        }

        // 6) estrelinhas cintilando
        for (m in stars) {
            val s = sin(t * m.sp * 2f + m.ph * twoPi)
            val k = if (s > 0f) s * s * s else 0f
            if (k <= 0.03f) continue
            val x = w * m.x
            val y = h * m.y
            val a = k * edge(y, h)
            if (a <= 0.02f) continue
            drawStar(c, x, y, m.sz * u * (0.5f + 0.8f * k), t * 20f + m.ph * 90f, a)
        }

        // 7) vagalumes com rastro
        val warm = mixColor(gg.primary, Color.WHITE, 0.75f)
        for (m in flies) {
            for (k in 3 downTo 0) {
                val tt = t - k * 0.14f
                val x = w * (0.5f + 0.47f * sin(tt * m.sp * 2.1f + m.ph * twoPi))
                val y = h * (0.5f + 0.44f * sin(tt * m.sp * 1.7f + m.ph * 9.1f + 1f))
                val a = edge(y, h) * (1f - k * 0.25f) * (0.65f + 0.35f * sin(t * 2.4f + m.ph * 12f))
                if (a <= 0.02f) continue
                val r = m.sz * u * (1f - k * 0.18f)
                p.style = Paint.Style.FILL
                p.color = al(warm, 45f * a)
                c.drawCircle(x, y, r * 3.2f, p)
                p.color = al(Color.WHITE, 245f * a * (if (k == 0) 1f else 0.5f))
                c.drawCircle(x, y, r, p)
            }
        }

        // 8) estrela cadente de vez em quando
        val per = 7f
        val idx = floor(t / per).toInt()
        val ph = (t - idx * per) / 1.1f
        if (ph in 0f..1f) {
            val rr = java.util.Random(idx * 977L + 5L)
            val sx = w * (0.25f + 0.75f * rr.nextFloat())
            val sy = h * (0.02f + 0.30f * rr.nextFloat())
            val ang = 2.3f + 0.3f * rr.nextFloat()
            val dx = cos(ang)
            val dy = sin(ang)
            val dist = ph * 0.6f * max(w, h)
            val hx = sx + dx * dist
            val hy = sy + dy * dist
            val tail = 90f * u
            val a = sin(ph * twoPi / 2f)
            p.style = Paint.Style.STROKE
            p.strokeCap = Paint.Cap.ROUND
            for (k in 0 until 8) {
                val f0 = k / 8f
                val f1 = (k + 1) / 8f
                p.strokeWidth = 2.6f * (1f - f0) * u
                p.color = al(Color.WHITE, 230f * a * (1f - f0))
                c.drawLine(hx - dx * tail * f0, hy - dy * tail * f0, hx - dx * tail * f1, hy - dy * tail * f1, p)
            }
            p.strokeCap = Paint.Cap.BUTT
            p.style = Paint.Style.FILL
            p.color = al(Color.WHITE, 255f * a)
            c.drawCircle(hx, hy, 2.6f * u, p)
        }

        if (hasPoster) drawSpotlight(c, t, gg)
        c.restore()
        drawSeam(c, t, gg)
        if (hasPoster) drawPosterGloss(c, t)
        if (isShown) postInvalidateOnAnimation()
    }

    // faixa decorada de cada gênero (embaixo, entre a cena e as informações)
    private val band = GenreBand(u)
    private val edgePath = Path()
    private val edgeRect = RectF()
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * Faixa brilhante na cor do gênero, com desenhos do tema (GenreBand), e o fio fino do contorno do card,
     * que continua por cima da capa (o mesmo que já aparece em volta das informações, lá embaixo).
     */
    private fun drawSeam(c: Canvas, t: Float, gg: Genre) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        band.draw(c, w, h, false, 13f * u, t, gg)

        // contorno fininho em volta do destaque: sobe pelas laterais e acompanha os cantos redondos do card
        val sw = 1f * u
        val r = 34f * u
        val l = sw / 2f
        val rt = w - sw / 2f
        edgePath.rewind()
        edgePath.moveTo(l, h)
        edgePath.lineTo(l, l + r)
        edgeRect.set(l, l, l + 2f * r, l + 2f * r)
        edgePath.arcTo(edgeRect, 180f, 90f)
        edgePath.lineTo(rt - r, l)
        edgeRect.set(rt - 2f * r, l, rt, l + 2f * r)
        edgePath.arcTo(edgeRect, 270f, 90f)
        edgePath.lineTo(rt, h)
        edgePaint.style = Paint.Style.STROKE
        edgePaint.strokeWidth = sw
        edgePaint.color = Palette.line
        c.drawPath(edgePath, edgePaint)
    }

    /** Trecho [a, b] do contorno medido (dá a volta se precisar). */
    private fun ringStroke(c: Canvas, len: Float, a: Float, b: Float) {
        val a0 = ((a % len) + len) % len
        val b0 = ((b % len) + len) % len
        ringSeg.rewind()
        if (b0 >= a0) {
            pmeas.getSegment(a0, b0, ringSeg, true)
        } else {
            pmeas.getSegment(a0, len, ringSeg, true)
            pmeas.getSegment(0f, b0, ringSeg, true)
        }
        c.drawPath(ringSeg, p)
    }

    /** Em volta da capa (nunca por cima dela): halo de luz, moldura dupla, enfeites e cometas correndo. */
    private fun drawSpotlight(c: Canvas, t: Float, gg: Genre) {
        drawThemeMarks(c, t, gg)
        val pw = poster.width()
        val ph = poster.height()
        val rad = max(pw, ph) * 0.82f
        if (halo == null || Math.abs(haloR - rad) > 1f) {
            haloR = rad
            halo = RadialGradient(
                0f, 0f, rad,
                intArrayOf(al(Color.WHITE, 62f), al(tints[0], 70f), al(tints[0], 0f)),
                floatArrayOf(0.35f, 0.62f, 1f), Shader.TileMode.CLAMP
            )
        }
        val breathe = 0.5f + 0.5f * sin(t * 1.5f)
        p.style = Paint.Style.FILL
        p.shader = halo
        p.alpha = (200f + 55f * breathe).toInt().coerceIn(0, 255)
        c.save()
        c.translate(poster.centerX(), poster.centerY())
        c.drawCircle(0f, 0f, rad, p)
        c.restore()
        p.shader = null
        p.alpha = 255

        // moldura dupla: uma borda de pérola e outra na cor do gênero
        p.style = Paint.Style.STROKE
        grown.set(poster.left - 4f * u, poster.top - 4f * u, poster.right + 4f * u, poster.bottom + 4f * u)
        p.strokeWidth = 2f * u
        p.color = al(Color.WHITE, 150f + 70f * breathe)
        c.drawRoundRect(grown, 24f * u, 24f * u, p)
        grown.set(poster.left - 8f * u, poster.top - 8f * u, poster.right + 8f * u, poster.bottom + 8f * u)
        val r2 = 28f * u
        p.strokeWidth = 1.3f * u
        p.color = al(tints[0], 170f)
        c.drawRoundRect(grown, r2, r2, p)

        // enfeites nos quatro cantos da moldura
        val k = r2 * 0.29f
        val cxs = floatArrayOf(grown.left + k, grown.right - k, grown.left + k, grown.right - k)
        val cys = floatArrayOf(grown.top + k, grown.top + k, grown.bottom - k, grown.bottom - k)
        for (i in 0 until 4) {
            val s = (14f + 2.5f * sin(t * 2.2f + i * 1.6f)) * u
            drawIcon(c, if (i % 2 == 0) gg.icon else "sparkle", al(Color.WHITE, 235f), cxs[i], cys[i], s, (if (i % 2 == 0) -12f else 12f) + 8f * sin(t * 1.3f + i))
        }

        // dois cometas de luz correndo pela moldura
        ring.rewind()
        ring.addRoundRect(grown, r2, r2, Path.Direction.CW)
        pmeas.setPath(ring, true)
        val len = pmeas.length
        if (len > 0f) {
            p.strokeCap = Paint.Cap.ROUND
            p.style = Paint.Style.STROKE
            for (kk in 0 until 2) {
                val head = ((t * 0.10f + kk * 0.5f) % 1f) * len
                val tail = len * 0.13f
                val parts = 10
                for (s in 0 until parts) {
                    val f = s.toFloat() / parts
                    p.strokeWidth = (3.4f - 2.4f * f) * u
                    p.color = al(if (kk == 0) Color.WHITE else tints[0], 235f * (1f - f) * (1f - f))
                    ringStroke(c, len, head - tail * (f + 1f / parts), head - tail * f)
                }
                pmeas.getPosTan(head, ringPos, ringTan)
                drawStar(c, ringPos[0], ringPos[1], 6f * u, t * 70f + kk * 40f, 1f)
            }
            p.strokeCap = Paint.Cap.BUTT
        }
        p.style = Paint.Style.FILL
    }

    /** Símbolo único do tema em tamanho grande, flutuando atrás da capa (a capa fica de fora). */
    private fun drawThemeMarks(c: Canvas, t: Float, gg: Genre) {
        val w = width.toFloat()
        val h = height.toFloat()
        val bob = 4f * sin(t * 0.7f)
        drawIcon(c, gg.icon, al(Color.WHITE, 46f), w * 0.15f, h * 0.20f + bob * u, 170f * u, -14f + bob)
        drawIcon(c, gg.icon, al(tints[0], 70f), w * 0.88f, h * 0.80f - bob * u, 150f * u, 12f - bob)
        drawIcon(c, gg.icon, al(Color.WHITE, 70f), w * 0.90f, h * 0.17f, 54f * u, 10f + bob)
        drawIcon(c, gg.icon, al(Color.WHITE, 60f), w * 0.09f, h * 0.84f, 46f * u, -10f - bob)
    }

    /** Por cima da capa: brilho de foto revelada e, de tempos em tempos, um reflexo de luz passando. */
    private fun drawPosterGloss(c: Canvas, t: Float) {
        val b = 3f * u
        inner.set(poster.left + b, poster.top + b, poster.right - b, poster.bottom - b)
        if (inner.width() <= 0f || inner.height() <= 0f) return
        clipIn.rewind()
        clipIn.addRoundRect(inner, 17f * u, 17f * u, Path.Direction.CW)
        c.save()
        c.clipPath(clipIn)
        p.style = Paint.Style.FILL
        c.save()
        c.translate(inner.left, inner.top)
        c.scale(inner.width(), inner.height())
        p.shader = gloss
        p.alpha = 255
        c.drawRect(0f, 0f, 1f, 1f, p)
        c.restore()
        val ph = (t % 6.5f) / 1.5f
        if (ph < 1f) {
            val e = ph * ph * (3f - 2f * ph)
            val bw = 95f * u
            val bx = inner.left - bw + e * (inner.width() + bw * 2f)
            c.save()
            c.rotate(18f, inner.centerX(), inner.centerY())
            c.translate(bx, inner.top - inner.height() * 0.3f)
            c.scale(bw, inner.height() * 1.6f)
            p.shader = sweep
            c.drawRect(0f, 0f, 1f, 1f, p)
            c.restore()
        }
        p.shader = null
        p.alpha = 255
        c.restore()
    }
}
