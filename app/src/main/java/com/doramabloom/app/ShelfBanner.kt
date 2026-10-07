package com.doramabloom.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import kotlin.math.sin

/**
 * Fundo animado da faixa de cada tema na Estante.
 * Reaproveita a cena exclusiva do gênero (cemitério, castelo, cidade...) e põe por cima:
 * sombra para o texto ler bem, símbolo do tema grande balançando, estrelinhas, reflexo de luz e borda.
 * Ao trocar de página a cena antiga some devagar enquanto a nova aparece.
 */
class ShelfBanner(ctx: Context) : View(ctx) {

    private val u = ctx.resources.displayMetrics.density
    private val kit = SceneKit(u)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val empty = RectF()
    private val rect = RectF()
    private val clip = Path()
    private val start = System.nanoTime()
    private val motes = kit.motes(14, 91L, 0.03f, 0.07f, 9f, 17f, 14f)

    private var key = "all"
    private var tint = Palette.pink
    private var prevKey: String? = null
    private var prevTint = Palette.pink
    private var fadeStart = 0L

    /** View que balança de leve junto com a animação (o selo do tema). */
    var bob: View? = null

    private var leftScrim: LinearGradient? = null
    private var bottomScrim: LinearGradient? = null
    private val sheen = LinearGradient(
        0f, 0f, 1f, 0f,
        intArrayOf(Color.argb(0, 255, 255, 255), Color.argb(110, 255, 255, 255), Color.argb(0, 255, 255, 255)),
        null, Shader.TileMode.CLAMP
    )

    fun setKey(k: String, color: Int, animate: Boolean) {
        if (k == key && color == tint) return
        if (animate && k != key) {
            prevKey = key
            prevTint = tint
            fadeStart = System.nanoTime()
        }
        key = k
        tint = color
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // não influencia a altura da faixa: só acompanha o tamanho que o texto der a ela
        val w = getDefaultSize(0, widthMeasureSpec)
        val h = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) MeasureSpec.getSize(heightMeasureSpec) else 0
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        leftScrim = LinearGradient(
            0f, 0f, w * 0.9f, 0f,
            Color.argb(125, 0, 0, 0), Color.argb(0, 0, 0, 0), Shader.TileMode.CLAMP
        )
        bottomScrim = LinearGradient(
            0f, h * 0.5f, 0f, h.toFloat(),
            Color.argb(0, 0, 0, 0), Color.argb(115, 0, 0, 0), Shader.TileMode.CLAMP
        )
    }

    private fun lite(c: Int, f: Float): Int = mixColor(c, Color.WHITE, f)

    /** Cena padrão (gêneros sem cena própria e o "Todos"): degradê na cor do tema com símbolos subindo. */
    private fun generic(c: Canvas, w: Float, h: Float, t: Float, k: String, col: Int) {
        kit.vgrad(c, w, h, mixColor(col, Color.BLACK, 0.5f), mixColor(col, Color.BLACK, 0.18f), col)
        for (i in 0 until 3) {
            val cx = w * (0.18f + 0.32f * i + 0.07f * sin(t * 0.4f + i))
            val cy = h * (0.5f + 0.3f * kotlin.math.cos(t * 0.33f + i * 2f))
            kit.glow(c, cx, cy, 95f * u, lite(col, 0.4f), 150f)
        }
        val icons: List<String> = if (k == "all") {
            listOf("heart", "blossom", "sparkle", "petal")
        } else {
            (Genres.byKey(k).petals + listOf("sparkle", "heart")).distinct()
        }
        for (i in motes.indices) {
            val m = motes[i]
            val life = kit.frac(t * m.sp * 2f + m.ph)
            val y = h * (1.1f - 1.2f * life)
            val x = w * m.x + sin(t * 0.8f + m.ph * kit.twoPi) * m.sw * u
            val a = kit.env(life)
            if (a <= 0.03f) continue
            val colr = if (i % 2 == 0) Color.WHITE else lite(col, 0.5f)
            kit.icon(c, icons[i % icons.size], kit.al(colr, 215f * a), x, y, m.sz * u, t * 20f * (if (i % 2 == 0) 1f else -1f) + m.ph * 360f)
        }
    }

    private fun base(c: Canvas, w: Float, h: Float, t: Float, k: String, col: Int) {
        val scene = AuraScenes.forKey(k)
        if (scene != null) {
            try {
                scene.draw(c, w, h, t, empty, false, kit)
                return
            } catch (e: Exception) {
                // se a cena falhar, cai na cena padrão
            }
        }
        generic(c, w, h, t, k, col)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val t = (System.nanoTime() - start) / 1_000_000_000f
        val r = 24f * u

        clip.rewind()
        rect.set(0f, 0f, w, h)
        clip.addRoundRect(rect, r, r, Path.Direction.CW)
        c.save()
        c.clipPath(clip)

        val pk = prevKey
        val f = if (pk != null) ((System.nanoTime() - fadeStart) / 380_000_000f).coerceIn(0f, 1f) else 1f
        if (pk != null && f < 1f) {
            base(c, w, h, t, pk, prevTint)
            c.saveLayerAlpha(0f, 0f, w, h, (f * 255f).toInt())
            base(c, w, h, t, key, tint)
            c.restore()
        } else {
            prevKey = null
            base(c, w, h, t, key, tint)
        }

        // símbolo do tema, grande e balançando, no canto direito
        val iconName = if (key == "all") "heart" else Genres.byKey(key).icon
        kit.icon(c, iconName, kit.al(Color.WHITE, 46f), w * 0.87f, h * 0.55f, 128f * u, 10f + 7f * sin(t * 0.55f))
        kit.icon(c, iconName, kit.al(Color.WHITE, 90f), w * 0.93f, h * 0.17f, 30f * u, -12f + 10f * sin(t * 0.9f))

        // sombra para o texto ficar legível em qualquer cena
        p.style = Paint.Style.FILL
        p.shader = leftScrim
        c.drawRect(0f, 0f, w, h, p)
        p.shader = bottomScrim
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null

        // estrelinhas cintilando
        for (i in 0 until 9) {
            val x = w * kit.frac(i * 0.37f + 0.11f)
            val y = h * kit.frac(i * 0.61f + 0.07f)
            val s = sin(t * 1.3f + i * 2.1f)
            if (s <= 0.1f) continue
            kit.star4(c, x, y, (2.2f + 3.2f * s) * u, t * 20f + i * 40f, Color.WHITE, s * s)
        }

        // reflexo de luz passando de tempos em tempos
        val ph = (t % 7.5f) / 1.5f
        if (ph < 1f) {
            val e = ph * ph * (3f - 2f * ph)
            val bw = 80f * u
            c.save()
            c.rotate(18f, w / 2f, h / 2f)
            c.translate(-bw + e * (w + bw * 2f), -h * 0.3f)
            c.scale(bw, h * 1.6f)
            p.shader = sheen
            c.drawRect(0f, 0f, 1f, 1f, p)
            p.shader = null
            c.restore()
        }
        c.restore()

        // borda na cor do tema + fio de luz por dentro
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.6f * u
        p.color = lite(tint, 0.45f)
        rect.set(0.8f * u, 0.8f * u, w - 0.8f * u, h - 0.8f * u)
        c.drawRoundRect(rect, r, r, p)
        p.strokeWidth = 1f * u
        p.color = Color.argb(60, 255, 255, 255)
        rect.set(3.2f * u, 3.2f * u, w - 3.2f * u, h - 3.2f * u)
        c.drawRoundRect(rect, r - 2.4f * u, r - 2.4f * u, p)
        p.style = Paint.Style.FILL

        val b = bob
        if (b != null) {
            b.translationY = 3f * u * sin(t * 1.6f)
            b.rotation = -8f + 4f * sin(t * 1.1f)
        }
        if (isShown) postInvalidateOnAnimation()
    }
}
