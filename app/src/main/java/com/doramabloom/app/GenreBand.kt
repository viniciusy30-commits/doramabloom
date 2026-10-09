package com.doramabloom.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

/**
 * Faixa decorada que emoldura o destaque do Início, em cima e embaixo (a de cima é a de baixo espelhada).
 * Cada gênero tem a sua: renda e pérolas (Romance), bandeirinhas (Comédia), listras de energia (Ação),
 * espinhos (Terror), trepadeira mágica (Fantasia), telhas e dancheong (Histórico), corrente (Mistério),
 * régua (Escolar), chuva (Drama), rolo de filme (Suspense), batimento (Médico), xadrez de colcha (Família),
 * pauta musical (Musical), bandeira quadriculada (Esporte), coroa com joias (Realeza), painel (Ficção
 * científica), pesponto (Vida real), pingos (Vingança) e fita de isolamento (Crime).
 * Gêneros criados por você usam a faixa padrão (pérolas, brilhos e o símbolo do gênero).
 * Tudo é calculado pelo relógio, sem estado.
 *
 * Coordenadas locais: y = 0 é a borda de dentro (junto do conteúdo) e y = bh é a borda de fora (a do cartão).
 */
class GenreBand(private val u: Float) {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()
    private val ic = IconDrawable("heart", Color.WHITE)
    private val star = Path()
    private val sweep = LinearGradient(
        0f, 0f, 1f, 0f,
        intArrayOf(Color.argb(0, 255, 255, 255), Color.argb(150, 255, 255, 255), Color.argb(0, 255, 255, 255)),
        null, Shader.TileMode.CLAMP
    )

    private var flip = false
    private var w = 0f
    private var bh = 0f
    private var t = 0f
    private var col = Color.WHITE
    private var deep = Color.BLACK
    private var g: Genre? = null

    private var cacheKey = 0L
    private var bodyShader: LinearGradient? = null
    private var glowShader: LinearGradient? = null

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

    // ------------------------------------------------------------------ utilidades

    private fun al(color: Int, a: Float): Int =
        Color.argb(a.coerceIn(0f, 255f).toInt(), Color.red(color), Color.green(color), Color.blue(color))

    private fun lite(c: Int, f: Float): Int = mixColor(c, Color.WHITE, f)
    private fun dark(c: Int, f: Float): Int = mixColor(c, Color.BLACK, f)
    private fun frac(x: Float): Float = x - floor(x)
    private fun hash(i: Int): Float = frac(sin(i * 12.9898f + 3.1f) * 43758.547f)

    /** Símbolo do app; não fica de cabeça para baixo na faixa de cima. */
    private fun icon(c: Canvas, name: String, color: Int, x: Float, y: Float, size: Float, rot: Float) {
        ic.name = name
        ic.color = color
        val h = (size / 2f).toInt().coerceAtLeast(1)
        ic.setBounds(-h, -h, h, h)
        c.save()
        c.translate(x, y)
        if (flip) c.scale(1f, -1f)
        c.rotate(rot)
        ic.draw(c)
        c.restore()
    }

    private fun sparkle(c: Canvas, x: Float, y: Float, size: Float, a: Float) {
        p.style = Paint.Style.FILL
        p.shader = null
        p.color = al(Color.WHITE, 40f * a)
        c.drawCircle(x, y, size * 1.7f, p)
        p.color = al(Color.WHITE, 255f * a)
        c.save()
        c.translate(x, y)
        c.scale(size, size)
        c.drawPath(star, p)
        c.restore()
    }

    /** Fio de pérolas; o brilho corre pela fileira em onda. */
    private fun pearls(c: Canvas, y: Float, step: Float, r: Float, period: Float = 0f, gap: Float = 0f) {
        p.style = Paint.Style.FILL
        p.shader = null
        var x = step / 2f
        while (x < w) {
            val skip = period > 0f && abs((x % period) - period / 2f) < gap
            if (!skip) {
                val tw = 0.5f + 0.5f * sin(t * 2.4f - x / (34f * u))
                p.color = al(Color.WHITE, 120f + 135f * tw)
                c.drawCircle(x, y, r * (1f + 0.35f * tw), p)
            }
            x += step
        }
    }

    /** Antes desenhava símbolos do gênero ao longo da faixa; agora a faixa fica só com o desenho dela. */
    @Suppress("UNUSED_PARAMETER", "unused")
    private fun iconsEvery(
        c: Canvas, names: List<String>, period: Float, y: Float, size: Float,
        color: Int = Color.WHITE, sway: Float = 8f, plate: Int = 0
    ) {
    }

    private fun line(c: Canvas, x0: Float, y0: Float, x1: Float, y1: Float, sw: Float, color: Int) {
        p.style = Paint.Style.STROKE
        p.shader = null
        p.strokeWidth = sw
        p.color = color
        c.drawLine(x0, y0, x1, y1, p)
        p.style = Paint.Style.FILL
    }

    private fun clipBand(c: Canvas) {
        c.clipRect(0f, 0f, w, bh)
    }

    // ------------------------------------------------------------------ desenho principal

    /**
     * @param top true = faixa de cima (espelhada); false = faixa de baixo.
     * @param height altura da view (a faixa de baixo encosta no fundo dela).
     */
    fun draw(c: Canvas, width: Float, height: Float, top: Boolean, bandH: Float, time: Float, genre: Genre) {
        if (width <= 0f || bandH <= 0f) return
        w = width
        bh = bandH
        t = time
        g = genre
        col = genre.primary
        deep = genre.deep
        flip = top

        val key = (bh.toBits().toLong() shl 32) xor (col.toLong() and 0xFFFFFFFFL) xor (genre.key.hashCode().toLong() * 31L)
        if (key != cacheKey || bodyShader == null) {
            cacheKey = key
            val tone = tones(genre.key)
            bodyShader = LinearGradient(
                0f, 0f, 0f, bh, intArrayOf(tone[0], tone[1], tone[2]),
                floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP
            )
            glowShader = LinearGradient(
                0f, -16f * u, 0f, 0f, intArrayOf(al(col, 0f), al(col, 105f)), null, Shader.TileMode.CLAMP
            )
        }

        c.save()
        if (top) {
            c.translate(0f, bh)
            c.scale(1f, -1f)
        } else {
            c.translate(0f, height - bh)
        }

        // luz suave saindo da faixa para dentro da cena
        p.style = Paint.Style.FILL
        p.shader = glowShader
        c.drawRect(0f, -16f * u, w, 0f, p)

        // corpo: degradê de tubo
        p.shader = bodyShader
        c.drawRect(0f, 0f, w, bh, p)
        p.shader = null

        // desenho exclusivo do gênero
        try {
            when (genre.key) {
                "romance" -> romance(c)
                "comedia" -> comedia(c)
                "acao" -> acao(c)
                "terror" -> terror(c)
                "fantasia" -> fantasia(c)
                "historico" -> historico(c)
                "misterio" -> misterio(c)
                "escolar" -> escolar(c)
                "drama" -> drama(c)
                "suspense" -> suspense(c)
                "medico" -> medico(c)
                "familia" -> familia(c)
                "musical" -> musical(c)
                "esporte" -> esporte(c)
                "realeza" -> realeza(c)
                "scifi" -> scifi(c)
                "vida" -> vida(c)
                "vinganca" -> vinganca(c)
                "crime" -> crime(c)
                "lgbt" -> lgbt(c)
                else -> padrao(c)
            }
        } catch (e: Exception) {
            // se algum enfeite falhar, a faixa continua aparecendo sem ele
        }

        // fio de brilho na borda de dentro e fio claro na de fora
        p.style = Paint.Style.FILL
        p.shader = null
        p.color = al(Color.WHITE, 190f)
        c.drawRect(0f, 0f, w, 1.1f * u, p)
        p.color = lite(col, 0.6f)
        c.drawRect(0f, bh - 1.3f * u, w, bh, p)

        // reflexo de luz passando
        val ph = (t % 5f) / 1.4f
        if (ph < 1f) {
            val e = ph * ph * (3f - 2f * ph)
            val bw = 120f * u
            c.save()
            clipBand(c)
            c.translate(-bw + e * (w + bw * 2f), 0f)
            c.scale(bw, bh)
            p.shader = sweep
            c.drawRect(0f, 0f, 1f, 1f, p)
            c.restore()
            p.shader = null
        }
        p.style = Paint.Style.FILL
        p.shader = null
        p.alpha = 255
        c.restore()
    }

    /** Cores do corpo da faixa (claro, meio, escuro), com ajuste fino por gênero. */
    private fun tones(key: String): IntArray = when (key) {
        "terror", "suspense", "vinganca", "crime", "misterio" ->
            intArrayOf(lite(col, 0.28f), dark(col, 0.18f), dark(deep, 0.38f))
        "scifi" -> intArrayOf(lite(col, 0.2f), dark(col, 0.25f), dark(deep, 0.5f))
        "musical", "drama" -> intArrayOf(lite(col, 0.5f), col, dark(col, 0.3f))
        else -> intArrayOf(lite(col, 0.6f), col, dark(col, 0.25f))
    }

    // ------------------------------------------------------------------ LGBTQ+: arco-íris ondulando com corações e brilhos

    private fun lgbt(c: Canvas) {
        val cols = intArrayOf(
        Color.parseColor("#E8505B"), Color.parseColor("#FF7A3D"), Color.parseColor("#F5D547"),
        Color.parseColor("#5DB56E"), Color.parseColor("#4F6D9A"), Color.parseColor("#A068E0")
        )
        c.save()
        clipBand(c)
        val sh = bh / cols.size
        p.style = Paint.Style.FILL
        p.shader = null
        for (i in cols.indices) {
            // cada listra ondula um pouquinho, uma depois da outra
            val dy = sin(t * 1.6f + i * 0.7f) * 0.35f * u
            p.color = al(cols[i], 245f)
            c.drawRect(0f, i * sh + dy, w, (i + 1) * sh + dy + 0.6f * u, p)
        }
        c.restore()
        // corações brancos e brilhos passeando pela faixa
        var k = 0
        var x = 22f * u
        while (x < w) {
            val bob = sin(t * 2f + k * 1.3f) * 1.4f * u
            icon(c, if (k % 3 == 2) "sparkle" else "heart", al(Color.WHITE, 235f), x, bh * 0.5f + bob, 8f * u, sin(t + k) * 10f)
            x += 58f * u
            k++
        }
        for (i in 0 until 4) {
            val a = 0.55f + 0.45f * sin(t * 2f + i * 1.9f)
            sparkle(c, w * (0.1f + 0.27f * i), bh * 0.5f, 3.4f * u * (0.85f + 0.15f * a), a)
        }
    }

    // ------------------------------------------------------------------ faixa padrão (gêneros criados por você)

    private fun padrao(c: Canvas) {
        val gg = g ?: return
        pearls(c, bh * 0.55f, 11f * u, 1.0f * u, 84f * u, 9f * u)
        iconsEvery(c, listOf(gg.icon) + gg.petals.take(2), 84f * u, bh * 0.52f, 9f * u)
        for (i in 0 until 4) {
            val a = 0.55f + 0.45f * sin(t * 2f + i * 1.9f)
            sparkle(c, w * (0.12f + 0.25f * i), bh * 0.5f, 3.6f * u * (0.85f + 0.15f * a), a)
        }
    }

    // ------------------------------------------------------------------ ROMANCE: renda, pérolas e coraçõezinhos

    private fun romance(c: Canvas) {
        c.save()
        clipBand(c)
        // renda: arquinhos na borda de dentro
        p.style = Paint.Style.FILL
        val r = 4.2f * u
        var x = r
        while (x < w + r) {
            p.color = al(Color.WHITE, 85f)
            c.drawCircle(x, 0f, r, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 0.8f * u
            p.color = al(Color.WHITE, 190f)
            c.drawCircle(x, 0f, r - 0.6f * u, p)
            p.style = Paint.Style.FILL
            x += r * 2f
        }
        // bolinhas de renda na borda de fora
        x = r
        while (x < w + r) {
            p.color = al(Color.WHITE, 120f)
            c.drawCircle(x, bh - 2.2f * u, 0.9f * u, p)
            x += r * 2f
        }
        c.restore()
        pearls(c, bh * 0.58f, 9f * u, 1.1f * u, 54f * u, 7f * u)
        val hearts = listOf("heart", "blossom", "heart", "petal")
        iconsEvery(c, hearts, 54f * u, bh * 0.58f, 8.5f * u, Color.WHITE, 10f)
        for (i in 0 until 3) {
            val a = 0.55f + 0.45f * sin(t * 2f + i * 2.3f)
            sparkle(c, w * (0.18f + 0.32f * i), bh * 0.58f, 3.4f * u * (0.85f + 0.15f * a), a)
        }
    }

    // ------------------------------------------------------------------ COMÉDIA: varal de bandeirinhas e confete

    private fun comedia(c: Canvas) {
        val cols = intArrayOf(Color.WHITE, lite(col, 0.55f), Color.parseColor("#FF6B9D"), Color.parseColor("#5CC8FF"), deep)
        // barbante
        line(c, 0f, 1.6f * u, w, 1.6f * u, 0.9f * u, al(Color.WHITE, 210f))
        val fw = 9f * u
        var i = 0
        var x = 2f * u
        p.style = Paint.Style.FILL
        while (x < w) {
            val sw = 0.9f * u * sin(t * 2.2f + i * 0.9f)
            path.rewind()
            path.moveTo(x, 1.6f * u)
            path.lineTo(x + fw, 1.6f * u)
            path.lineTo(x + fw / 2f + sw, bh - 1.8f * u)
            path.close()
            p.color = cols[i % cols.size]
            c.drawPath(path, p)
            // bolinha de enfeite na bandeirinha
            p.color = al(if (i % cols.size == 0) deep else Color.WHITE, 190f)
            c.drawCircle(x + fw / 2f + sw * 0.4f, 4.4f * u, 0.9f * u, p)
            x += fw + 2f * u
            i++
        }
        // confete que sobe
        for (k in 0 until 9) {
            val life = frac(t * 0.25f + hash(k) * 3f)
            val cx = w * hash(k + 40)
            val cy = bh - life * (bh + 10f * u)
            p.color = al(cols[k % 4], 230f * sin(life * 3.1416f))
            c.save()
            c.translate(cx, cy)
            c.rotate(t * 90f + k * 40f)
            c.drawRect(-1.4f * u, -0.8f * u, 1.4f * u, 0.8f * u, p)
            c.restore()
        }
    }

    // ------------------------------------------------------------------ AÇÃO: listras de energia e raios

    private fun acao(c: Canvas) {
        c.save()
        clipBand(c)
        val period = bh * 1.6f
        val off = (t * 16f * u) % period
        p.style = Paint.Style.FILL
        p.color = al(deep, 120f)
        var x = -period + off
        while (x < w + period) {
            path.rewind()
            path.moveTo(x, 0f)
            path.lineTo(x + period * 0.5f, 0f)
            path.lineTo(x + period * 0.5f - bh * 0.9f, bh)
            path.lineTo(x - bh * 0.9f, bh)
            path.close()
            c.drawPath(path, p)
            x += period
        }
        c.restore()
        // faíscas amarelas nas bordas das listras
        line(c, 0f, bh * 0.5f, w, bh * 0.5f, 0.8f * u, al(Color.WHITE, 120f))
        val gg = g ?: return
        iconsEvery(c, gg.petals, 76f * u, bh * 0.5f, 10.5f * u, Color.WHITE, 6f, al(deep, 200f))
    }

    // ------------------------------------------------------------------ TERROR: espinhos, morcegos e fantasminhas

    private fun terror(c: Canvas) {
        // espinhos que saem da borda de dentro
        p.style = Paint.Style.FILL
        p.color = dark(deep, 0.45f)
        var i = 0
        var x = 0f
        while (x < w) {
            val sw = 5f + 4f * hash(i)
            val len = (4f + 6f * hash(i + 9)) * u
            path.rewind()
            path.moveTo(x, 0.5f * u)
            path.lineTo(x + sw * u * 0.5f, -len)
            path.lineTo(x + sw * u, 0.5f * u)
            path.close()
            c.drawPath(path, p)
            x += sw * u
            i++
        }
        // gotas roxas escorrendo na faixa
        for (k in 0 until 6) {
            val life = frac(t * 0.18f + hash(k + 70))
            val cx = w * hash(k + 5)
            p.color = al(lite(col, 0.5f), 200f * sin(life * 3.1416f))
            c.drawCircle(cx, 2f * u + life * (bh - 4f * u), 1.1f * u, p)
        }
        val gg = g ?: return
        // brilhos fantasmagóricos que piscam
        for (k in 0 until 5) {
            val a = max(0f, sin(t * 1.3f + k * 2.4f))
            p.color = al(Color.WHITE, 200f * a * a)
            c.drawCircle(w * (0.1f + 0.2f * k), bh * 0.7f, 1.2f * u, p)
        }
        iconsEvery(c, gg.petals, 80f * u, bh * 0.5f, 10.5f * u, Color.WHITE, 9f)
    }

    // ------------------------------------------------------------------ FANTASIA: trepadeira mágica

    private fun fantasia(c: Canvas) {
        val mid = bh * 0.52f
        path.rewind()
        var x = 0f
        path.moveTo(0f, mid)
        while (x <= w) {
            path.lineTo(x, mid + sin(x / (13f * u) + t * 0.9f) * 2.7f * u)
            x += 3f * u
        }
        p.style = Paint.Style.STROKE
        p.shader = null
        p.strokeWidth = 1.2f * u
        p.color = al(Color.WHITE, 225f)
        c.drawPath(path, p)
        // folhinhas na trepadeira
        p.style = Paint.Style.FILL
        var i = 0
        x = 7f * u
        while (x < w) {
            val y = mid + sin(x / (13f * u) + t * 0.9f) * 2.7f * u
            val side = if (i % 2 == 0) -1f else 1f
            p.color = al(lite(col, 0.75f), 235f)
            c.drawCircle(x, y + side * 2.1f * u, 1.25f * u, p)
            x += 14f * u
            i++
        }
        // estrelinhas e borboletas
        for (k in 0 until 6) {
            val a = 0.5f + 0.5f * sin(t * 2f + k * 1.9f)
            sparkle(c, w * (0.08f + 0.17f * k) + 6f * u, bh * (k % 2 * 0.3f + 0.3f), 3.3f * u * (0.85f + 0.15f * a), a)
        }
        val gg = g ?: return
        iconsEvery(c, listOf(gg.petals.getOrElse(1) { "butterfly" }, gg.petals.getOrElse(2) { "wand" }), 118f * u, bh * 0.5f, 10.5f * u, Color.WHITE, 14f)
    }

    // ------------------------------------------------------------------ HISTÓRICO: telhas de palácio e dancheong

    private fun historico(c: Canvas) {
        c.save()
        clipBand(c)
        // telhas (giwa): arcos sobrepostos
        val r = 5f * u
        var x = r
        p.style = Paint.Style.FILL
        while (x < w + r) {
            p.color = al(deep, 110f)
            c.drawCircle(x, 0f, r, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 0.9f * u
            p.color = al(Color.WHITE, 190f)
            c.drawCircle(x, 0f, r - 0.5f * u, p)
            p.style = Paint.Style.FILL
            x += r * 2f
        }
        c.restore()
        // dancheong: quadradinhos coloridos tradicionais
        val tradicional = intArrayOf(
            Color.parseColor("#E4572E"), Color.parseColor("#2FA58B"),
            Color.parseColor("#F5C542"), Color.parseColor("#3E73C9"), Color.WHITE
        )
        var i = 0
        x = 3f * u
        val y = bh * 0.74f
        while (x < w) {
            p.color = tradicional[i % tradicional.size]
            rect.set(x, y - 1.5f * u, x + 3.4f * u, y + 1.5f * u)
            c.drawRoundRect(rect, 0.7f * u, 0.7f * u, p)
            x += 6f * u
            i++
        }
        line(c, 0f, bh * 0.74f, w, bh * 0.74f, 0.5f * u, al(Color.WHITE, 110f))
        val gg = g ?: return
        iconsEvery(c, gg.petals, 92f * u, bh * 0.46f, 11f * u, Color.WHITE, 10f)
    }

    // ------------------------------------------------------------------ MISTÉRIO: corrente e pistas

    private fun misterio(c: Canvas) {
        val y = bh * 0.52f
        p.style = Paint.Style.STROKE
        p.shader = null
        var i = 0
        var x = 3f * u
        while (x < w) {
            val horizontal = i % 2 == 0
            p.strokeWidth = 1.2f * u
            p.color = al(Color.WHITE, 215f)
            if (horizontal) rect.set(x - 4.2f * u, y - 2.1f * u, x + 4.2f * u, y + 2.1f * u)
            else rect.set(x - 1.8f * u, y - 2.9f * u, x + 1.8f * u, y + 2.9f * u)
            c.drawRoundRect(rect, 2f * u, 2f * u, p)
            x += 6.2f * u
            i++
        }
        p.style = Paint.Style.FILL
        // lanterna varrendo a faixa
        val sx = ((t * 0.12f) % 1f) * (w + 80f * u) - 40f * u
        p.color = al(Color.WHITE, 60f)
        c.drawCircle(sx, y, 12f * u, p)
        val gg = g ?: return
        iconsEvery(c, gg.petals, 90f * u, y, 11f * u, Color.WHITE, 8f, al(deep, 215f))
    }

    // ------------------------------------------------------------------ ESCOLAR: régua e pesponto de caderno

    private fun escolar(c: Canvas) {
        // régua na borda de dentro
        var i = 0
        var x = 0f
        while (x < w) {
            val len = when {
                i % 10 == 0 -> 5.2f
                i % 5 == 0 -> 3.8f
                else -> 2.2f
            }
            line(c, x, 0f, x, len * u, 0.8f * u, al(Color.WHITE, 215f))
            x += 3f * u
            i++
        }
        // pesponto de caderno
        x = 0f
        val y = bh * 0.68f
        while (x < w) {
            line(c, x, y, x + 4f * u, y, 0.9f * u, al(Color.WHITE, 190f))
            x += 7f * u
        }
        val gg = g ?: return
        iconsEvery(c, gg.petals, 82f * u, bh * 0.5f, 10.5f * u, Color.WHITE, 12f, al(deep, 190f))
    }

    // ------------------------------------------------------------------ DRAMA: chuva e ondas de lágrima

    private fun drama(c: Canvas) {
        c.save()
        clipBand(c)
        for (k in 0 until 2) {
            path.rewind()
            var x = 0f
            val base = bh * (0.38f + 0.28f * k)
            path.moveTo(0f, base)
            while (x <= w) {
                path.lineTo(x, base + sin(x / (11f * u) + t * (1.0f + 0.4f * k) + k * 2f) * 1.8f * u)
                x += 3f * u
            }
            p.style = Paint.Style.STROKE
            p.shader = null
            p.strokeWidth = 1f * u
            p.color = al(Color.WHITE, if (k == 0) 200f else 110f)
            c.drawPath(path, p)
        }
        // gotinhas caindo
        p.style = Paint.Style.FILL
        for (k in 0 until 14) {
            val life = frac(t * (0.35f + 0.25f * hash(k)) + hash(k + 20))
            val cx = w * hash(k + 3)
            p.color = al(Color.WHITE, 230f * sin(life * 3.1416f))
            c.drawOval(cx - 0.9f * u, -3f * u + life * (bh + 6f * u), cx + 0.9f * u, -3f * u + life * (bh + 6f * u) + 3.2f * u, p)
        }
        c.restore()
        val gg = g ?: return
        iconsEvery(c, gg.petals, 108f * u, bh * 0.5f, 11f * u, Color.WHITE, 6f, al(deep, 150f))
    }

    // ------------------------------------------------------------------ SUSPENSE: rolo de filme e olhar

    private fun suspense(c: Canvas) {
        // furos do filme em cima e embaixo
        p.style = Paint.Style.FILL
        var x = 3f * u
        while (x < w) {
            p.color = al(Color.BLACK, 150f)
            rect.set(x, 1.8f * u, x + 4f * u, 4.6f * u)
            c.drawRoundRect(rect, 0.9f * u, 0.9f * u, p)
            rect.set(x, bh - 4.9f * u, x + 4f * u, bh - 2.1f * u)
            c.drawRoundRect(rect, 0.9f * u, 0.9f * u, p)
            x += 9f * u
        }
        // luz de projetor correndo
        val sx = ((t * 0.18f) % 1f) * (w + 60f * u) - 30f * u
        p.color = al(Color.WHITE, 55f)
        rect.set(sx - 22f * u, 0f, sx + 22f * u, bh)
        c.drawRect(rect, p)
        val gg = g ?: return
        iconsEvery(c, gg.petals, 100f * u, bh * 0.5f, 9.5f * u, Color.WHITE, 4f, al(Color.BLACK, 120f))
    }

    // ------------------------------------------------------------------ MÉDICO: batimento cardíaco

    private fun medico(c: Canvas) {
        val y = bh * 0.56f
        val pw = 78f * u
        path.rewind()
        var x0 = -pw * 0.2f
        path.moveTo(0f, y)
        while (x0 < w + pw) {
            path.lineTo(x0 + pw * 0.20f, y)
            path.lineTo(x0 + pw * 0.26f, y - 1.6f * u)
            path.lineTo(x0 + pw * 0.32f, y)
            path.lineTo(x0 + pw * 0.40f, y)
            path.lineTo(x0 + pw * 0.44f, y + 1.6f * u)
            path.lineTo(x0 + pw * 0.50f, y - 5.4f * u)
            path.lineTo(x0 + pw * 0.56f, y + 3.4f * u)
            path.lineTo(x0 + pw * 0.60f, y)
            path.lineTo(x0 + pw * 0.72f, y)
            path.lineTo(x0 + pw * 0.80f, y - 2f * u)
            path.lineTo(x0 + pw * 0.88f, y)
            path.lineTo(x0 + pw, y)
            x0 += pw
        }
        p.style = Paint.Style.STROKE
        p.shader = null
        p.strokeJoin = Paint.Join.ROUND
        p.strokeWidth = 1.1f * u
        p.color = al(Color.WHITE, 130f)
        c.drawPath(path, p)
        // o pulso que corre brilha mais forte
        val head = ((t * 0.22f) % 1f) * (w + 140f * u)
        c.save()
        c.clipRect(head - 90f * u, 0f, head, bh)
        p.strokeWidth = 1.7f * u
        p.color = al(Color.WHITE, 255f)
        c.drawPath(path, p)
        c.restore()
        p.strokeJoin = Paint.Join.MITER
        p.style = Paint.Style.FILL
        val gg = g ?: return
        iconsEvery(c, gg.petals, 150f * u, bh * 0.42f, 10f * u, Color.WHITE, 0f, al(deep, 205f))
    }

    // ------------------------------------------------------------------ FAMÍLIA: colcha xadrez com bordado

    private fun familia(c: Canvas) {
        c.save()
        clipBand(c)
        val s = bh / 3f
        var j = 0
        while (j < 3) {
            var i = 0
            var x = -s + (t * 6f * u) % (s * 2f)
            while (x < w + s) {
                if ((i + j) % 2 == 0) {
                    p.style = Paint.Style.FILL
                    p.color = al(Color.WHITE, 85f)
                    c.drawRect(x, j * s, x + s, (j + 1) * s, p)
                }
                x += s
                i++
            }
            j++
        }
        c.restore()
        // pontinhos de bordado
        var x = 0f
        while (x < w) {
            line(c, x, 2f * u, x + 3f * u, 2f * u, 0.8f * u, al(Color.WHITE, 200f))
            line(c, x, bh - 3f * u, x + 3f * u, bh - 3f * u, 0.8f * u, al(Color.WHITE, 200f))
            x += 6f * u
        }
        val gg = g ?: return
        iconsEvery(c, gg.petals + "heart", 66f * u, bh * 0.5f, 9.5f * u, Color.WHITE, 8f, al(deep, 195f))
    }

    // ------------------------------------------------------------------ MUSICAL: pauta com notinhas

    private fun musical(c: Canvas) {
        for (k in 0 until 5) {
            val y = bh * (0.18f + 0.16f * k)
            line(c, 0f, y, w, y, 0.7f * u, al(Color.WHITE, 175f))
        }
        var i = 0
        var x = 24f * u
        while (x < w) {
            val lvl = (hash(i) * 5f).toInt()
            val y = bh * (0.18f + 0.16f * lvl) + 1.2f * u * sin(t * 2f + i)
            val a = 0.8f + 0.2f * sin(t * 3f + i * 1.3f)
            p.style = Paint.Style.FILL
            p.color = al(deep, 215f)
            c.drawCircle(x, y, 2.6f * u, p)
            p.color = al(Color.WHITE, 255f * a)
            c.drawCircle(x, y, 1.9f * u, p)
            x += 34f * u
            i++
        }
        val gg = g ?: return
        iconsEvery(c, gg.petals, 120f * u, bh * 0.5f, 11f * u, Color.WHITE, 12f, al(deep, 195f))
    }

    // ------------------------------------------------------------------ ESPORTE: bandeira quadriculada

    private fun esporte(c: Canvas) {
        c.save()
        clipBand(c)
        val s = bh / 3f
        var j = 0
        while (j < 3) {
            var i = 0
            var x = -s * 2f + (t * 12f * u) % (s * 2f)
            while (x < w + s) {
                p.style = Paint.Style.FILL
                p.color = if ((i + j) % 2 == 0) al(Color.WHITE, 235f) else al(dark(deep, 0.2f), 215f)
                c.drawRect(x, j * s, x + s, (j + 1) * s, p)
                x += s
                i++
            }
            j++
        }
        c.restore()
        val gg = g ?: return
        iconsEvery(c, gg.petals, 104f * u, bh * 0.5f, 10.5f * u, Color.WHITE, 8f, al(col, 255f))
    }

    // ------------------------------------------------------------------ REALEZA: coroa, joias e pérolas

    private fun realeza(c: Canvas) {
        // pontas da coroa saindo da borda de dentro, cada uma com uma pérola
        val step = 18f * u
        var x = 0f
        p.style = Paint.Style.FILL
        while (x < w) {
            path.rewind()
            path.moveTo(x, 0.5f * u)
            path.lineTo(x + step * 0.5f, -6f * u)
            path.lineTo(x + step, 0.5f * u)
            path.close()
            p.shader = null
            p.color = lite(col, 0.35f)
            c.drawPath(path, p)
            p.color = al(Color.WHITE, 235f)
            c.drawCircle(x + step * 0.5f, -6.4f * u, 1.5f * u, p)
            x += step
        }
        // joias coloridas
        val gems = intArrayOf(Color.parseColor("#E5395B"), Color.parseColor("#2FBF71"), Color.parseColor("#4A7BFF"))
        var i = 0
        x = 6f * u
        while (x < w) {
            val a = 0.8f + 0.2f * sin(t * 2.4f + i)
            c.save()
            c.translate(x, bh * 0.52f)
            c.rotate(45f)
            p.color = gems[i % 3]
            c.drawRect(-2.1f * u * a, -2.1f * u * a, 2.1f * u * a, 2.1f * u * a, p)
            p.color = al(Color.WHITE, 200f)
            c.drawRect(-1.2f * u, -1.2f * u, -0.2f * u, -0.2f * u, p)
            c.restore()
            x += 12f * u
            i++
        }
        val gg = g ?: return
        iconsEvery(c, gg.petals, 96f * u, bh * 0.52f, 11f * u, Color.WHITE, 6f, al(dark(col, 0.2f), 235f))
    }

    // ------------------------------------------------------------------ FICÇÃO CIENTÍFICA: painel com luzes

    private fun scifi(c: Canvas) {
        val neon = Color.parseColor("#7AF3FF")
        var x = 0f
        while (x < w) {
            line(c, x, bh * 0.32f, x + 7f * u, bh * 0.32f, 0.9f * u, al(neon, 220f))
            x += 9f * u
        }
        x = 3f * u
        while (x < w) {
            line(c, x, bh * 0.76f, x + 3f * u, bh * 0.76f, 0.7f * u, al(Color.WHITE, 120f))
            x += 6f * u
        }
        // luzes que piscam em sequência
        p.style = Paint.Style.FILL
        var i = 0
        x = 6f * u
        while (x < w) {
            val on = max(0f, sin(t * 3f - i * 0.8f))
            p.color = al(if (i % 3 == 0) Color.parseColor("#FF7AD9") else neon, 70f + 185f * on)
            c.drawCircle(x, bh * 0.54f, 1.3f * u, p)
            x += 24f * u
            i++
        }
        // feixe de varredura
        val sx = ((t * 0.2f) % 1f) * (w + 40f * u) - 20f * u
        p.color = al(neon, 70f)
        c.drawRect(sx - 10f * u, 0f, sx + 10f * u, bh, p)
        val gg = g ?: return
        iconsEvery(c, gg.petals, 118f * u, bh * 0.5f, 10.5f * u, Color.WHITE, 10f, al(deep, 205f))
    }

    // ------------------------------------------------------------------ VIDA REAL: pesponto, sol e café

    private fun vida(c: Canvas) {
        var x = 0f
        val y = bh * 0.5f
        while (x < w) {
            line(c, x, y, x + 4.4f * u, y, 1f * u, al(Color.WHITE, 225f))
            x += 7.6f * u
        }
        x = 0f
        while (x < w) {
            line(c, x, 2.3f * u, x + 2f * u, 2.3f * u, 0.7f * u, al(Color.WHITE, 130f))
            line(c, x, bh - 3f * u, x + 2f * u, bh - 3f * u, 0.7f * u, al(Color.WHITE, 130f))
            x += 4f * u
        }
    }

    // ------------------------------------------------------------------ VINGANÇA: pingos de sangue

    private fun vinganca(c: Canvas) {
        val blood = dark(col, 0.42f)
        var i = 0
        var x = 4f * u
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.shader = null
        while (x < w) {
            val len = (3.5f + 6.5f * hash(i)) * u * (0.88f + 0.12f * sin(t * 0.9f + i * 1.4f))
            p.strokeWidth = (2.2f + 1.2f * hash(i + 11)) * u
            p.color = blood
            c.drawLine(x, 0f, x, -len, p)
            p.style = Paint.Style.FILL
            c.drawCircle(x, -len, p.strokeWidth * 0.62f, p)
            p.style = Paint.Style.STROKE
            x += (9f + 12f * hash(i + 4)) * u
            i++
        }
        p.strokeCap = Paint.Cap.BUTT
        p.style = Paint.Style.FILL
        // brilho úmido nos pingos
        val gg = g ?: return
        for (k in 0 until 5) {
            val a = max(0f, sin(t * 1.1f + k * 2.2f))
            p.color = al(Color.WHITE, 150f * a)
            c.drawCircle(w * (0.12f + 0.2f * k), bh * 0.3f, 0.9f * u, p)
        }
        iconsEvery(c, gg.petals, 90f * u, bh * 0.52f, 10.5f * u, Color.WHITE, 8f)
    }

    // ------------------------------------------------------------------ CRIME: fita de isolamento

    private fun crime(c: Canvas) {
        c.save()
        clipBand(c)
        val yellow = Color.parseColor("#F5C518")
        val ink = Color.parseColor("#262626")
        val period = bh * 1.5f
        val off = (t * 10f * u) % (period * 2f)
        var x = -period * 2f + off
        p.style = Paint.Style.FILL
        p.shader = null
        p.color = yellow
        c.drawRect(0f, 0f, w, bh, p)
        p.color = ink
        while (x < w + period * 2f) {
            path.rewind()
            path.moveTo(x, 0f)
            path.lineTo(x + period, 0f)
            path.lineTo(x + period - bh, bh)
            path.lineTo(x - bh, bh)
            path.close()
            c.drawPath(path, p)
            x += period * 2f
        }
        c.restore()
        val gg = g ?: return
        iconsEvery(c, gg.petals, 112f * u, bh * 0.5f, 10f * u, yellow, 6f, al(ink, 245f))
    }
}
