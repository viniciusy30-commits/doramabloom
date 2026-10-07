package com.doramabloom.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Desenha um símbolo do app (viewBox 24x24) no ponto dado. */
private fun drawGlyph(c: Canvas, p: Paint, name: String, cx: Float, cy: Float, size: Float, rot: Float, color: Int, alpha: Int) {
    p.style = Paint.Style.FILL
    p.color = color
    p.alpha = alpha.coerceIn(0, 255)
    c.save()
    c.translate(cx, cy)
    c.rotate(rot)
    val s = size / 24f
    c.scale(s, s)
    c.translate(-12f, -12f)
    c.drawPath(Icons.path(name), p)
    c.restore()
}

private fun fmtTime(ms: Int): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return (s / 60).toString() + ":" + (s % 60).toString().padStart(2, '0')
}

/**
 * Player da trilha sonora: um disco de vinil que gira com o braço descendo, o botão de play com
 * ondinhas e os símbolos do gênero girando em volta, ondas que pulam no ritmo (toque para pular
 * para outro pedaço) e notinhas misturadas com os símbolos do gênero subindo. Tudo na cor do gênero.
 */
class SoundtrackView(ctx: Context, private val g: Genre, seed: Long) : View(ctx) {
    private val u = resources.displayMetrics.density
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val clip = Path()
    private val rnd = java.util.Random(seed)
    private val amps = ArrayList<Float>()
    private val icons: List<String> = (g.petals + listOf("heart", "sparkle", "music")).distinct().take(4)

    var playing = false
        set(v) {
            if (field != v) {
                field = v
                animateGlow(if (v) 1f else 0f)
                invalidate()
            }
        }

    private fun animateGlow(to: Float) {
        glowAnim?.cancel()
        val from = glow
        if (from == to) return
        val a = ValueAnimator.ofFloat(from, to)
        a.duration = (FADE_MS * abs(to - from)).toLong().coerceAtLeast(120L)
        a.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
        a.addUpdateListener {
            glow = it.animatedValue as Float
            invalidate()
        }
        glowAnim = a
        a.start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val wf = w.toFloat()
        val hf = h.toFloat()
        val primary = g.primary
        val deep = g.deep
        val l0 = mixColor(primary, Color.WHITE, 0.34f)
        val l1 = mixColor(primary, deep, 0.30f)
        lightShader = LinearGradient(0f, 0f, wf, hf, l0, l1, Shader.TileMode.CLAMP)
        darkShader = LinearGradient(
            0f, 0f, wf, hf,
            mixColor(l0, mixColor(deep, Color.BLACK, 0.55f), 0.88f),
            mixColor(l1, mixColor(deep, Color.BLACK, 0.80f), 0.92f),
            Shader.TileMode.CLAMP
        )
        val R = 48f * u
        spotShader = RadialGradient(
            vx(), vy(), R * 2.3f,
            intArrayOf(mixColor(primary, Color.WHITE, 0.25f), mixColor(primary, deep, 0.2f), Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP
        )
    }

    override fun onDetachedFromWindow() {
        glowAnim?.cancel()
        super.onDetachedFromWindow()
    }

    companion object {
        /** Duração do escurecer ao dar play (o áudio só começa depois dela). */
        const val FADE_MS = 900L
    }
    var progress = 0f
        set(v) {
            field = v
            invalidate()
        }
    var posMs = 0
    var durMs = 0
    var onToggle: (() -> Unit)? = null
    var onSeek: ((Float) -> Unit)? = null

    private var last = 0L
    private var t = 0f
    private var ang = 0f
    private var orbit = 0f
    private var armT = 0f
    private var pressT = 0f
    // escurecer ao tocar: um único número (0 = claro, 1 = escuro) animado por um ValueAnimator
    private var glow = 0f
    private var glowAnim: ValueAnimator? = null
    private var lightShader: Shader? = null
    private var darkShader: Shader? = null
    private var spotShader: Shader? = null
    private var pressing = false
    private var seeking = false
    private var seekFrac = 0f
    private var zone = 0
    private var spawnAcc = 0f

    private class Pt(
        var x: Float, var y: Float, var vx: Float, var vy: Float, var size: Float,
        var rot: Float, var vr: Float, var life: Float, var icon: String, var color: Int
    )

    private val pts = ArrayList<Pt>()

    private fun vx() = 16f * u + 48f * u + 6f * u
    private fun vy() = 62f * u
    private fun bx() = width - 16f * u - 44f * u
    private fun fracOf(x: Float): Float = ((x - 16f * u) / (width - 32f * u).coerceAtLeast(1f)).coerceIn(0f, 1f)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(getDefaultSize(0, widthMeasureSpec), (184f * u).toInt())
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        last = 0L
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val br = 27f * u
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val bdx = e.x - bx()
                val bdy = e.y - vy()
                if (bdx * bdx + bdy * bdy <= (br + 10f * u) * (br + 10f * u)) {
                    // só o botão de play/pausa liga e desliga a música
                    zone = 1
                    pressing = true
                } else if (e.y > 122f * u && durMs > 0) {
                    // as ondas só pulam para outro pedaço quando a música já está carregada
                    zone = 2
                    seeking = true
                    seekFrac = fracOf(e.x)
                } else {
                    zone = 0
                    return false
                }
                parent?.requestDisallowInterceptTouchEvent(true)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (zone == 2) {
                    seekFrac = fracOf(e.x)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (zone == 1) onToggle?.invoke()
                if (zone == 2) {
                    progress = seekFrac
                    onSeek?.invoke(seekFrac)
                }
                zone = 0
                pressing = false
                seeking = false
                invalidate()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                zone = 0
                pressing = false
                seeking = false
                invalidate()
                return true
            }
        }
        return true
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val now = System.nanoTime()
        // se a tela ficou parada (ou o app demorou a desenhar), não "pula" a animação: recomeça de um passo normal
        val frameGap = if (last == 0L) 0f else (now - last) / 1_000_000_000f
        val dt = if (last == 0L || frameGap > 0.1f) 0.016f else frameGap.coerceAtMost(0.05f)
        last = now
        t += dt
        if (playing) {
            ang = (ang + dt * 110f) % 360f
            orbit = (orbit + dt * 38f) % 360f
        }
        val armTarget = if (playing) 1f else 0f
        armT += (armTarget - armT) * minOf(1f, dt * 5f)
        val pressTarget = if (pressing) 1f else 0f
        pressT += (pressTarget - pressT) * minOf(1f, dt * 14f)

        val pad = 16f * u
        val R = 48f * u
        val cx = vx()
        val cy = vy()
        val bx = bx()
        val by = cy
        val br = 27f * u
        val primary = g.primary
        val deep = g.deep
        val white = Color.WHITE

        // ---- cartão: degradê do gênero com brilho nas bordas
        rect.set(0f, 0f, w, h)
        val rad = 28f * u
        p.style = Paint.Style.FILL
        p.shader = lightShader
        p.alpha = 255
        c.drawRoundRect(rect, rad, rad, p)
        if (glow > 0.003f) {
            // o degradê escuro entra por cima do claro, só mudando a transparência
            p.shader = darkShader
            p.alpha = (255 * glow).toInt()
            c.drawRoundRect(rect, rad, rad, p)
        }
        p.shader = null
        p.alpha = 255
        clip.reset()
        clip.addRoundRect(rect, rad, rad, Path.Direction.CW)
        c.save()
        c.clipPath(clip)

        // foco de luz no disco enquanto toca
        if (glow > 0.003f) {
            p.style = Paint.Style.FILL
            p.shader = spotShader
            p.alpha = (110 * glow).toInt()
            c.drawCircle(cx, cy, R * 2.3f, p)
            p.shader = null
            p.alpha = 255
        }

        // enfeites de fundo: bolhas, símbolos do gênero e brilhinhos
        p.style = Paint.Style.FILL
        p.color = white
        p.alpha = 26
        c.drawCircle(w * 0.98f, h * 0.04f, 80f * u, p)
        c.drawCircle(w * 0.02f, h * 1.0f, 70f * u, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f * u
        p.alpha = 44
        c.drawCircle(bx, by, 62f * u, p)
        c.drawCircle(bx, by, 84f * u, p)
        drawGlyph(c, p, g.icon, w * 0.55f, h * 0.30f, 92f * u, 14f, white, 22)
        drawGlyph(c, p, icons.getOrElse(1) { "sparkle" }, w * 0.40f, h * 0.80f, 54f * u, -12f, white, 20)
        val spark = floatArrayOf(0.30f, 0.14f, 0.62f, 0.10f, 0.74f, 0.74f, 0.50f, 0.62f, 0.90f, 0.40f)
        for (i in 0 until spark.size / 2) {
            val tw = if (playing) 0.5f + 0.5f * sin(t * 2.4f + i * 1.7f) else 0.55f
            drawGlyph(c, p, "sparkle", w * spark[i * 2], h * spark[i * 2 + 1], (8f + 3f * (i % 3)) * u, i * 17f, white, (70 + 120 * tw).toInt())
        }

        // ---- disco de vinil
        p.style = Paint.Style.FILL
        p.color = Color.BLACK
        p.alpha = 50
        c.drawCircle(cx, cy + 3f * u, R + 1f * u, p)
        c.save()
        c.rotate(ang, cx, cy)
        p.color = mixColor(deep, Color.BLACK, 0.72f)
        p.alpha = 255
        c.drawCircle(cx, cy, R, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f * u
        var gr = R * 0.50f
        while (gr < R * 0.97f) {
            p.color = white
            p.alpha = if (((gr / R) * 100).toInt() % 2 == 0) 34 else 20
            c.drawCircle(cx, cy, gr, p)
            gr += R * 0.065f
        }
        p.strokeWidth = 2f * u
        p.color = mixColor(primary, white, 0.25f)
        p.alpha = 150
        c.drawCircle(cx, cy, R - 1f * u, p)
        // reflexos que giram junto com o disco
        p.style = Paint.Style.FILL
        p.color = white
        p.alpha = 30
        rect.set(cx - R * 0.94f, cy - R * 0.94f, cx + R * 0.94f, cy + R * 0.94f)
        c.drawArc(rect, 18f, 26f, true, p)
        c.drawArc(rect, 198f, 26f, true, p)
        // rótulo com o símbolo do gênero
        val lr = R * 0.40f
        p.shader = LinearGradient(cx, cy - lr, cx, cy + lr, mixColor(primary, white, 0.30f), primary, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, lr, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.6f * u
        p.color = white
        p.alpha = 210
        c.drawCircle(cx, cy, lr - 3f * u, p)
        drawGlyph(c, p, g.icon, cx, cy, lr * 1.05f, 0f, white, 245)
        p.style = Paint.Style.FILL
        p.color = mixColor(deep, Color.BLACK, 0.6f)
        p.alpha = 255
        c.drawCircle(cx, cy, 2.4f * u, p)
        c.restore()

        // ---- braço do toca-discos: desce no disco quando toca
        val px = cx + R * 1.02f
        val py = cy - R * 0.96f
        val th = Math.toRadians((-8.0 + 30.0 * armT))
        val len = R * 1.25f
        val tx = px - sin(th).toFloat() * len
        val ty = py + cos(th).toFloat() * len
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = 4.6f * u
        p.color = Color.BLACK
        p.alpha = 40
        c.drawLine(px, py + 2f * u, tx, ty + 2f * u, p)
        p.color = white
        p.alpha = 245
        c.drawLine(px, py, tx, ty, p)
        p.strokeWidth = 8f * u
        p.color = mixColor(primary, white, 0.15f)
        p.alpha = 255
        val hx = tx + sin(th).toFloat() * 7f * u
        val hy = ty - cos(th).toFloat() * 7f * u
        c.drawLine(hx, hy, tx, ty, p)
        p.strokeCap = Paint.Cap.BUTT
        p.style = Paint.Style.FILL
        p.color = white
        p.alpha = 255
        c.drawCircle(px, py, 8f * u, p)
        p.color = primary
        c.drawCircle(px, py, 3.6f * u, p)

        // ---- notinhas e símbolos do gênero subindo do disco
        if (playing) {
            spawnAcc += dt
            if (spawnAcc > 0.30f) {
                spawnAcc = 0f
                val pool = listOf("music", "music") + icons
                pts.add(
                    Pt(
                        cx + (rnd.nextFloat() - 0.5f) * R, cy - R * 0.1f,
                        (rnd.nextFloat() - 0.1f) * 20f * u, -(24f + rnd.nextFloat() * 26f) * u,
                        (10f + rnd.nextFloat() * 8f) * u, (rnd.nextFloat() - 0.5f) * 40f, (rnd.nextFloat() - 0.5f) * 70f, 1f,
                        pool[rnd.nextInt(pool.size)], if (rnd.nextBoolean()) white else mixColor(primary, white, 0.65f)
                    )
                )
            }
        }
        val it = pts.iterator()
        while (it.hasNext()) {
            val q = it.next()
            q.life -= dt * 0.40f
            if (q.life <= 0f) {
                it.remove()
                continue
            }
            q.vx += sin(t * 3f + q.y * 0.03f) * 14f * u * dt
            q.x += q.vx * dt
            q.y += q.vy * dt
            q.rot += q.vr * dt
            drawGlyph(c, p, q.icon, q.x, q.y, q.size, q.rot, q.color, (235 * q.life.coerceIn(0f, 1f)).toInt())
        }

        // ---- botão de play: ondas pulsando, símbolos do gênero em volta
        if (playing) {
            for (k in 0..1) {
                val ph = ((t * 0.85f) + k * 0.5f) % 1f
                p.style = Paint.Style.STROKE
                p.strokeWidth = 2.4f * u
                p.color = white
                p.alpha = (140 * (1f - ph)).toInt()
                c.drawCircle(bx, by, br + 4f * u + 20f * u * ph, p)
            }
        }
        val oi = icons.take(4)
        for (i in oi.indices) {
            val a = Math.toRadians((orbit + i * (360.0 / oi.size)))
            val ox = bx + cos(a).toFloat() * 41f * u
            val oy = by + sin(a).toFloat() * 41f * u
            drawGlyph(c, p, oi[i], ox, oy, 13f * u, (orbit * 0.5f) + i * 30f, white, if (playing) 235 else 150)
        }
        val sc = 1f - 0.08f * pressT
        c.save()
        c.scale(sc, sc, bx, by)
        p.style = Paint.Style.FILL
        p.color = Color.BLACK
        p.alpha = 45
        c.drawCircle(bx, by + 3f * u, br, p)
        p.color = white
        p.alpha = 255
        c.drawCircle(bx, by, br, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.4f * u
        p.color = mixColor(primary, white, 0.45f)
        p.alpha = 255
        c.drawCircle(bx, by, br - 4f * u, p)
        // o triângulo do play já é desenhado um pouco à direita no seu quadro; centrado no botão fica bem no meio
        val gx = bx
        drawGlyph(c, p, if (playing) "pause" else "play", gx, by, 26f * u, 0f, primary, 255)
        c.restore()

        // ---- legenda e tempo
        tp.typeface = Typeface.DEFAULT_BOLD
        tp.textSize = 9.5f * u
        tp.color = white
        tp.alpha = 225
        tp.letterSpacing = 0.14f
        tp.textAlign = Paint.Align.LEFT
        c.drawText("TRILHA SONORA", pad, 137f * u, tp)
        tp.letterSpacing = 0.02f
        tp.textAlign = Paint.Align.RIGHT
        val shownPos = if (seeking && durMs > 0) (seekFrac * durMs).toInt() else posMs
        val txt = if (durMs > 0) fmtTime(shownPos) + " / " + fmtTime(durMs) else if (playing) "tocando…" else "toque no play"
        c.drawText(txt, w - pad, 137f * u, tp)

        // ---- ondas: quem já tocou fica branco; toque para pular
        val barW = 3.4f * u
        val gap = 2.8f * u
        val step = barW + gap
        val n = ((w - 2f * pad + gap) / step).toInt().coerceAtLeast(8)
        if (amps.size != n) {
            amps.clear()
            val ph1 = rnd.nextFloat() * 6f
            val ph2 = rnd.nextFloat() * 6f
            for (i in 0 until n) {
                val v = 0.5f * abs(sin(i * 0.33f + ph1)) + 0.35f * abs(sin(i * 0.12f + ph2)) + 0.15f * rnd.nextFloat()
                amps.add((0.22f + 0.78f * v).coerceAtMost(1f))
            }
        }
        val shown = if (seeking) seekFrac else progress
        val head = shown * (n - 1)
        val wy = 161f * u
        val maxHalf = 17f * u
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = barW
        for (i in 0 until n) {
            val x = pad + barW / 2f + i * step
            var a = amps[i]
            if (playing) a *= 0.70f + 0.30f * sin(t * 6f + i * 0.6f)
            val half = maxOf(1.6f * u, maxHalf * a)
            p.color = white
            p.alpha = if (shown > 0f && i <= head) 255 else 92
            c.drawLine(x, wy - half, x, wy + half, p)
        }
        p.strokeCap = Paint.Cap.BUTT
        if (shown > 0f || seeking) {
            val kx = pad + barW / 2f + head * step
            p.style = Paint.Style.FILL
            p.color = white
            p.alpha = 255
            c.drawCircle(kx, wy, 6f * u, p)
            p.color = primary
            c.drawCircle(kx, wy, 2.6f * u, p)
        }
        c.restore()

        val armBusy = abs(armT - armTarget) > 0.01f
        val pressBusy = abs(pressT - pressTarget) > 0.01f
        if (playing || pts.isNotEmpty() || armBusy || pressBusy || seeking) postInvalidateOnAnimation()
    }
}

/** Anda de leve para cima e para baixo (e balança um pouquinho); para sozinho quando a tela sai. */
class Floaty(ctx: Context, private val ampDp: Float, private val periodMs: Long, private val baseRot: Float, private val rotAmp: Float) : FrameLayout(ctx) {
    private var anim: ValueAnimator? = null

    init {
        clipChildren = false
        clipToPadding = false
        rotation = baseRot
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        anim?.cancel()
        val a = ValueAnimator.ofFloat(0f, (2.0 * Math.PI).toFloat())
        a.duration = periodMs
        a.repeatCount = ValueAnimator.INFINITE
        a.interpolator = LinearInterpolator()
        val d = resources.displayMetrics.density
        a.addUpdateListener {
            val v = it.animatedValue as Float
            translationY = sin(v) * ampDp * d
            rotation = baseRot + cos(v) * rotAmp
        }
        anim = a
        a.start()
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        anim = null
        super.onDetachedFromWindow()
    }
}

/** Coraçãozinho que bate (cresce e volta) sem parar enquanto estiver na tela. */
class Beat(ctx: Context) : FrameLayout(ctx) {
    private var anim: ValueAnimator? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        anim?.cancel()
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = 900
        a.repeatCount = ValueAnimator.INFINITE
        a.interpolator = LinearInterpolator()
        a.addUpdateListener {
            val v = it.animatedValue as Float
            // duas batidinhas seguidas e uma pausa
            val b1 = sin(Math.PI * (v / 0.28f).coerceIn(0f, 1f).toDouble()).toFloat()
            val b2 = sin(Math.PI * ((v - 0.30f) / 0.28f).coerceIn(0f, 1f).toDouble()).toFloat()
            val s = 1f + 0.13f * maxOf(b1, b2)
            scaleX = s
            scaleY = s
        }
        anim = a
        a.start()
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        anim = null
        scaleX = 1f
        scaleY = 1f
        super.onDetachedFromWindow()
    }
}

/** Fundo do cartão do casal: bolhas, marcas d'água do gênero e corações/brilhos subindo devagarinho. */
class CoupleDecor(ctx: Context, private val g: Genre, themed: Boolean = false) : View(ctx) {
    private class P(var x: Float, var y: Float, var vy: Float, var size: Float, var phase: Float, var rot: Float, var a: Int, var ic: Int)

    private val u = resources.displayMetrics.density
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rnd = java.util.Random()
    private val ps = ArrayList<P>()
    // themed = só os símbolos do gênero (usado no Elenco); senão, corações e brilhos junto
    private val icons: List<String> =
        if (themed) (listOf(g.icon) + g.petals + listOf("sparkle")).distinct()
        else (listOf("heart", "sparkle", "heart") + g.petals).distinct()
    private var last = 0L

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = getDefaultSize(0, widthMeasureSpec)
        val h = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) MeasureSpec.getSize(heightMeasureSpec) else 0
        setMeasuredDimension(w, h)
    }

    private fun spawn(anywhere: Boolean): P = P(
        rnd.nextFloat() * width,
        if (anywhere) rnd.nextFloat() * height else height + 20f * u,
        (10f + rnd.nextFloat() * 18f) * u,
        (10f + rnd.nextFloat() * 12f) * u,
        rnd.nextFloat() * 6.28f,
        (rnd.nextFloat() - 0.5f) * 40f,
        60 + rnd.nextInt(80),
        rnd.nextInt(icons.size)
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        ps.clear()
        if (w > 0 && h > 0) for (i in 0 until 11) ps.add(spawn(true))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        last = 0L
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val now = System.nanoTime()
        val dt = if (last == 0L) 0.016f else ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
        last = now
        val pr = g.primary

        p.style = Paint.Style.FILL
        p.color = pr
        p.alpha = 30
        c.drawCircle(w * 0.0f, h * 0.0f, 110f * u, p)
        c.drawCircle(w * 1.0f, h * 0.9f, 130f * u, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f * u
        p.alpha = 52
        c.drawCircle(w * 0.98f, h * 0.05f, 62f * u, p)
        c.drawCircle(w * 0.02f, h * 0.62f, 80f * u, p)
        drawGlyph(c, p, g.icon, w * 0.90f, h * 0.76f, 130f * u, 12f, pr, 30)
        drawGlyph(c, p, g.icon, w * 0.08f, h * 0.24f, 84f * u, -14f, pr, 28)

        for (i in ps.indices) {
            var q = ps[i]
            q.y -= q.vy * dt
            q.phase += dt * 1.2f
            if (q.y < -24f * u) {
                q = spawn(false)
                ps[i] = q
            }
            val sx = q.x + sin(q.phase) * 12f * u
            val col = if (i % 3 == 0) mixColor(pr, Color.WHITE, 0.35f) else pr
            drawGlyph(c, p, icons[q.ic % icons.size], sx, q.y, q.size, q.rot + sin(q.phase) * 8f, col, q.a)
        }
        postInvalidateOnAnimation()
    }
}

/**
 * Aura do casal favorito: tudo acontece por trás/em volta da polaroid. Um coração de luz desenha o
 * contorno com cometas correndo e luzinhas piscando, raios suaves giram, o brilho bate no ritmo de um
 * coração (duas batidinhas e pausa), estrelas e corações giram em órbita por trás da foto, bolhas
 * e pétalas sobem balançando, estrelinhas de 4 pontas cintilam e, de tempos em tempos, uma chuva de
 * corações explode de trás da foto. Tudo nas cores do gênero com toques de rosa e dourado.
 */
class LoveAura(ctx: Context, private val g: Genre, private val photoWDp: Float, private val photoHDp: Float) : View(ctx) {
    private class Mote(var x: Float, var y: Float, var vy: Float, var size: Float, var phase: Float, var rot: Float, var a: Int, var ic: Int, var kind: Int)
    private class Bit(var ang: Float, var speed: Float, var size: Float, var age: Float, var ic: Int, var spin: Float)

    private val u = resources.displayMetrics.density
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rnd = java.util.Random(11L)
    private val motes = ArrayList<Mote>()
    private val bits = ArrayList<Bit>()
    private val heart = Path()
    private val seg = Path()
    private val star = Path()
    private val ray = Path()
    private var pm: PathMeasure? = null
    private var plen = 0f
    private val pos = FloatArray(2)
    private val tan = FloatArray(2)
    private var dotX = FloatArray(0)
    private var dotY = FloatArray(0)
    private var glow: RadialGradient? = null
    private var blobA: RadialGradient? = null
    private var blobB: RadialGradient? = null
    private var rayShader: LinearGradient? = null
    private var t = 0f
    private var last = 0L
    private var burstIn = 1.4f
    private val twinkles = ArrayList<FloatArray>()

    private val pink = Color.parseColor("#FF8FB8")
    private val gold = Color.parseColor("#FFE6A3")
    private val icons: List<String> = (listOf("heart", "heart", "sparkle", "heart") + g.petals).distinct()

    private val lite: Int get() = mixColor(g.primary, Color.WHITE, 0.6f)
    private val warm: Int get() = mixColor(g.primary, pink, 0.55f)

    init {
        // estrelinha de 4 pontas (raio 1)
        star.moveTo(0f, -1f)
        star.quadTo(0.10f, -0.10f, 1f, 0f)
        star.quadTo(0.10f, 0.10f, 0f, 1f)
        star.quadTo(-0.10f, 0.10f, -1f, 0f)
        star.quadTo(-0.10f, -0.10f, 0f, -1f)
        star.close()
        for (i in 0 until 9) twinkles.add(floatArrayOf(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat() * 6.28f, 0.7f + rnd.nextFloat() * 0.9f, 7f + rnd.nextFloat() * 8f))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = getDefaultSize(0, widthMeasureSpec)
        val h = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) MeasureSpec.getSize(heightMeasureSpec) else 0
        setMeasuredDimension(w, h)
    }

    private fun spawn(anywhere: Boolean): Mote {
        val kind = if (rnd.nextInt(4) == 0) 1 else 0 // 1 = bolha de luz
        return Mote(
            rnd.nextFloat() * width,
            if (anywhere) rnd.nextFloat() * height else height + 24f * u,
            (kind.let { if (it == 1) 9f else 14f } + rnd.nextFloat() * 16f) * u,
            (if (kind == 1) 6f + rnd.nextFloat() * 11f else 9f + rnd.nextFloat() * 12f) * u,
            rnd.nextFloat() * 6.28f,
            (rnd.nextFloat() - 0.5f) * 50f,
            110 + rnd.nextInt(110),
            rnd.nextInt(icons.size),
            kind
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        motes.clear()
        if (w <= 0 || h <= 0) return
        for (i in 0 until 20) motes.add(spawn(true))
        val cx = w / 2f
        val cy = h / 2f
        val pr = g.primary
        glow = RadialGradient(cx, cy, w * 0.62f, intArrayOf(mixColor(pr, Color.WHITE, 0.25f), pr, Color.TRANSPARENT), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        blobA = RadialGradient(0f, 0f, 120f * u, intArrayOf(pink, Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
        blobB = RadialGradient(0f, 0f, 130f * u, intArrayOf(mixColor(pr, Color.WHITE, 0.5f), Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
        val rr = w * 0.80f
        rayShader = LinearGradient(0f, 0f, 0f, -rr, mixColor(pr, Color.WHITE, 0.75f), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        ray.rewind()
        val half = rr * 0.085f
        ray.moveTo(0f, 0f)
        ray.lineTo(-half, -rr)
        ray.lineTo(half, -rr)
        ray.close()

        // contorno de coração em volta da foto
        val hw = minOf(w * 0.97f, photoWDp * u + 96f * u)
        val sx = hw / 32f
        val sy = sx * 1.10f
        heart.rewind()
        val n = 220
        for (i in 0..n) {
            val a = (i.toDouble() / n) * 2.0 * Math.PI
            val x = 16.0 * Math.pow(Math.sin(a), 3.0)
            val y = -(13.0 * Math.cos(a) - 5.0 * Math.cos(2 * a) - 2.0 * Math.cos(3 * a) - Math.cos(4 * a))
            val px = cx + (x * sx).toFloat()
            val py = cy + ((y - 2.5) * sy).toFloat() + 8f * u
            if (i == 0) heart.moveTo(px, py) else heart.lineTo(px, py)
        }
        heart.close()
        val m = PathMeasure(heart, true)
        pm = m
        plen = m.length
        val dots = 40
        dotX = FloatArray(dots)
        dotY = FloatArray(dots)
        for (i in 0 until dots) {
            m.getPosTan(plen * i / dots, pos, tan)
            dotX[i] = pos[0]
            dotY[i] = pos[1]
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        last = 0L
    }

    /** Desenha o trecho [a, b] do contorno (dando a volta se precisar). */
    private fun strokeSeg(c: Canvas, a: Float, b: Float) {
        val m = pm ?: return
        if (plen <= 0f) return
        val a0 = ((a % plen) + plen) % plen
        val b0 = ((b % plen) + plen) % plen
        seg.rewind()
        if (b0 >= a0) {
            m.getSegment(a0, b0, seg, true)
        } else {
            m.getSegment(a0, plen, seg, true)
            m.getSegment(0f, b0, seg, true)
        }
        c.drawPath(seg, p)
    }

    private fun drawStar(c: Canvas, x: Float, y: Float, r: Float, rot: Float, color: Int, alpha: Int) {
        p.style = Paint.Style.FILL
        p.shader = null
        p.color = color
        p.alpha = alpha.coerceIn(0, 255)
        c.save()
        c.translate(x, y)
        c.rotate(rot)
        c.scale(r, r)
        c.drawPath(star, p)
        c.restore()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val now = System.nanoTime()
        val dt = if (last == 0L) 0.016f else ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
        last = now
        t += dt
        val cx = w / 2f
        val cy = h / 2f
        val pr = g.primary
        val lt = lite
        val wm = warm

        // batida de coração: duas batidinhas e uma pausa
        val v = (t % 1.8f) / 1.8f
        val b1 = sin(Math.PI * (v / 0.17f).coerceIn(0f, 1f).toDouble()).toFloat()
        val b2 = sin(Math.PI * ((v - 0.19f) / 0.17f).coerceIn(0f, 1f).toDouble()).toFloat()
        val beat = maxOf(b1, b2)

        // 1) brilho grande que respira e bate
        val gl = glow
        if (gl != null) {
            c.save()
            val k = 1f + 0.03f * sin(t * 1.1f) + 0.07f * beat
            c.scale(k, k, cx, cy)
            p.style = Paint.Style.FILL
            p.shader = gl
            p.alpha = (105 + 28 * sin(t * 0.9f) + 40 * beat).toInt().coerceIn(0, 255)
            c.drawCircle(cx, cy, w * 0.62f, p)
            p.shader = null
            c.restore()
        }

        // 2) duas manchas de luz rosa e clara passeando devagar
        val ba = blobA
        val bb = blobB
        if (ba != null && bb != null) {
            p.style = Paint.Style.FILL
            c.save()
            c.translate(cx + cos(t * 0.45f) * w * 0.30f, cy + sin(t * 0.37f) * h * 0.30f)
            p.shader = ba
            p.alpha = 80
            c.drawCircle(0f, 0f, 120f * u, p)
            c.restore()
            c.save()
            c.translate(cx + cos(t * 0.33f + 3f) * w * 0.32f, cy + sin(t * 0.41f + 2f) * h * 0.32f)
            p.shader = bb
            p.alpha = 90
            c.drawCircle(0f, 0f, 130f * u, p)
            c.restore()
            p.shader = null
        }

        // 3) raios de luz girando bem devagar
        val rs = rayShader
        if (rs != null) {
            p.style = Paint.Style.FILL
            p.shader = rs
            p.color = lt
            val spin = t * 7f
            for (i in 0 until 12) {
                c.save()
                c.translate(cx, cy)
                c.rotate(spin + i * 30f)
                p.alpha = (26 + 22 * sin(t * 1.2f + i * 1.7f)).toInt().coerceIn(0, 255)
                c.drawPath(ray, p)
                c.restore()
            }
            p.shader = null
        }

        // 4) bolhas de luz e pétalas subindo, balançando
        for (i in motes.indices) {
            var q = motes[i]
            q.y -= q.vy * dt
            q.phase += dt * 1.3f
            if (q.y < -26f * u) {
                q = spawn(false)
                motes[i] = q
            }
            val sx = q.x + sin(q.phase) * 14f * u
            val edge = minOf(1f, minOf(q.y, h - q.y).coerceAtLeast(0f) / (46f * u))
            val al = (q.a * edge).toInt()
            if (q.kind == 1) {
                p.shader = null
                p.style = Paint.Style.FILL
                p.color = if (i % 2 == 0) lt else wm
                p.alpha = al / 5
                c.drawCircle(sx, q.y, q.size, p)
                p.style = Paint.Style.STROKE
                p.strokeWidth = 1.4f * u
                p.alpha = al / 2
                c.drawCircle(sx, q.y, q.size, p)
                p.style = Paint.Style.FILL
                p.color = Color.WHITE
                p.alpha = al / 2
                c.drawCircle(sx - q.size * 0.35f, q.y - q.size * 0.35f, q.size * 0.18f, p)
            } else {
                val col = when (i % 3) { 0 -> wm; 1 -> lt; else -> pr }
                drawGlyph(c, p, icons[q.ic % icons.size], sx, q.y, q.size, q.rot + sin(q.phase) * 14f, col, al)
            }
        }

        // 5) contorno de coração: halo, linha e luzinhas
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.shader = null
        p.color = wm
        p.strokeWidth = (15f + 5f * beat) * u
        p.alpha = (26 + 22 * beat).toInt()
        c.drawPath(heart, p)
        p.color = lt
        p.strokeWidth = 6f * u
        p.alpha = 54
        c.drawPath(heart, p)
        p.color = Color.WHITE
        p.strokeWidth = 1.6f * u
        p.alpha = (120 + 70 * beat).toInt()
        c.drawPath(heart, p)

        // dois cometas correndo pelo contorno, com rastro que apaga
        if (plen > 0f) {
            val speed = plen / 6.5f
            for (k in 0 until 2) {
                val head = (t * speed + k * plen / 2f)
                val tail = plen * 0.17f
                val parts = 12
                for (s in 0 until parts) {
                    val f = s.toFloat() / parts
                    p.style = Paint.Style.STROKE
                    p.color = if (k == 0) Color.WHITE else gold
                    p.strokeWidth = (4.4f - 3.2f * f) * u
                    p.alpha = (235 * (1f - f) * (1f - f)).toInt()
                    strokeSeg(c, head - tail * (f + 1f / parts), head - tail * f)
                }
                val m = pm
                if (m != null) {
                    m.getPosTan(((head % plen) + plen) % plen, pos, tan)
                    p.style = Paint.Style.FILL
                    p.color = if (k == 0) lt else gold
                    p.alpha = 70
                    c.drawCircle(pos[0], pos[1], 9f * u, p)
                    drawStar(c, pos[0], pos[1], 9f * u, t * 90f, Color.WHITE, 245)
                }
            }
        }

        // luzinhas piscando em volta do contorno (algumas são coraçõezinhos)
        for (i in dotX.indices) {
            val tw = 0.5f + 0.5f * sin(t * 2.3f + i * 0.93f)
            if (i % 5 == 0) {
                drawGlyph(c, p, "heart", dotX[i], dotY[i], (8f + 4f * tw) * u, 0f, if (i % 10 == 0) wm else Color.WHITE, (90 + 140 * tw).toInt())
            } else {
                p.style = Paint.Style.FILL
                p.color = lt
                p.alpha = (40 * tw).toInt()
                c.drawCircle(dotX[i], dotY[i], 5f * u, p)
                p.color = Color.WHITE
                p.alpha = (110 + 140 * tw).toInt()
                c.drawCircle(dotX[i], dotY[i], (1.2f + 1.3f * tw) * u, p)
            }
        }

        // 6) corações e estrelinhas em órbita, passando por trás da foto
        c.save()
        c.rotate(-7f, cx, cy)
        val rx = (photoWDp / 2f + 26f) * u
        val ry = (photoHDp / 2f + 20f) * u
        val orb = 9
        for (i in 0 until orb) {
            val a = t * 0.55f + i * 6.2832f / orb
            val ox = cx + cos(a) * rx
            val oy = cy + sin(a) * ry
            val depth = 0.55f + 0.45f * sin(a)
            val sz = (11f + 6f * depth) * u
            if (i % 3 == 2) {
                drawStar(c, ox, oy, sz * 0.8f, a * 60f, gold, (120 + 120 * depth).toInt())
            } else {
                drawGlyph(c, p, "heart", ox, oy, sz, sin(a) * 18f, if (i % 2 == 0) wm else lt, (110 + 130 * depth).toInt())
            }
        }
        c.restore()

        // 7) estrelinhas de 4 pontas cintilando
        for (tw in twinkles) {
            val ph = (t * tw[3] + tw[2]) % 6.2832f
            val s = maxOf(0f, sin(ph))
            if (s > 0.02f) drawStar(c, tw[0] * w, tw[1] * h, tw[4] * u * s, 20f * s, if (tw[2] > 3f) gold else Color.WHITE, (60 + 195 * s).toInt())
        }

        // 8) de vez em quando, uma chuva de corações sai de trás da foto
        burstIn -= dt
        if (burstIn <= 0f) {
            burstIn = 5.5f + rnd.nextFloat() * 2f
            for (i in 0 until 16) {
                bits.add(Bit((i / 16f) * 6.2832f + rnd.nextFloat() * 0.3f, (70f + rnd.nextFloat() * 70f) * u, (9f + rnd.nextFloat() * 11f) * u, 0f, rnd.nextInt(icons.size), (rnd.nextFloat() - 0.5f) * 120f))
            }
        }
        var bi = 0
        while (bi < bits.size) {
            val b = bits[bi]
            b.age += dt
            if (b.age >= 1.7f) {
                bits.removeAt(bi)
                continue
            }
            val e = 1f - Math.exp((-b.age * 2.2f).toDouble()).toFloat()
            val d = b.speed * 1.5f * e
            val bx = cx + cos(b.ang) * d * 1.05f
            val by = cy + sin(b.ang) * d * 1.15f - b.age * 14f * u
            val life = 1f - b.age / 1.7f
            drawGlyph(c, p, icons[b.ic % icons.size], bx, by, b.size * (0.6f + 0.5f * e), b.spin * b.age, if (bi % 2 == 0) wm else Color.WHITE, (235 * life).toInt())
            bi++
        }

        postInvalidateOnAnimation()
    }
}

/**
 * Casal favorito: uma polaroid grandona e centralizada, levemente torta e flutuando, com os nomes
 * escritos à mão e, por trás/em volta, a aura animada de coração (LoveAura).
 */
fun Context.coupleCard(d: Drama, g: Genre): View? {
    val hasPhoto = d.couplePhoto.isNotEmpty() && File(d.couplePhoto).exists()
    if (d.couple.isBlank() && !hasPhoto) return null

    val root = FrameLayout(this)
    root.clipChildren = false
    root.clipToPadding = false
    val bg = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(g.soft, mixColor(g.soft, g.primary, 0.20f)))
    bg.cornerRadius = dp(28).toFloat()
    bg.setStroke(dp(1), mixColor(g.primary, Color.WHITE, 0.45f))
    root.background = bg
    val clipper = FrameLayout(this)
    // o fundo enfeitado fica cortado nos cantos arredondados do cartão
    clipper.outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(v: View, o: Outline) {
            o.setRoundRect(0, 0, v.width, v.height, dp(28).toFloat())
        }
    }
    clipper.clipToOutline = true
    clipper.addView(CoupleDecor(this, g), FrameLayout.LayoutParams(MATCH, MATCH))
    root.addView(clipper, FrameLayout.LayoutParams(MATCH, MATCH))

    val col = LinearLayout(this)
    col.orientation = LinearLayout.VERTICAL
    col.gravity = Gravity.CENTER_HORIZONTAL
    col.clipChildren = false
    col.clipToPadding = false
    col.setPadding(dp(16), dp(20), dp(16), dp(22))
    root.addView(col, FrameLayout.LayoutParams(MATCH, WRAP))

    // título centralizado, com coraçõezinhos dos lados
    val head = LinearLayout(this)
    head.orientation = LinearLayout.HORIZONTAL
    head.gravity = Gravity.CENTER
    head.addView(IconView(this, "heart", g.primary, 14))
    head.addView(label("Casal favorito", 21f, g.dark, true, true), lin(WRAP, WRAP, l = 8, r = 8))
    head.addView(IconView(this, "heart", g.primary, 14))
    col.addView(head, lin(WRAP, WRAP))

    // ---- a polaroid
    val pw = 236
    val ph = 292
    val frame = Floaty(this, 3.5f, 3400L, -2.5f, 0.8f)
    val paper = FrameLayout(this)
    paper.clipChildren = false
    paper.clipToPadding = false
    val pbg = GradientDrawable()
    pbg.setColor(Color.parseColor("#FFFDFB"))
    pbg.cornerRadius = dp(20).toFloat()
    pbg.setStroke(dp(1), Color.parseColor("#EADDE3"))
    paper.background = pbg
    paper.elevation = dp(9).toFloat()
    frame.addView(paper, FrameLayout.LayoutParams(dp(pw), dp(ph)))

    // a foto (ou um fundinho fofo com coração, quando não tem foto)
    val photo = FrameLayout(this)
    photo.setBackgroundColor(g.soft)
    photo.outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(v: View, o: Outline) {
            o.setRoundRect(0, 0, v.width, v.height, dp(13).toFloat())
        }
    }
    photo.clipToOutline = true
    val bmp = if (hasPhoto) Covers.load(d.couplePhoto, dp(pw)) else null
    if (bmp != null) {
        val iv = ImageView(this)
        iv.scaleType = ImageView.ScaleType.CENTER_CROP
        iv.setImageBitmap(bmp)
        photo.addView(iv, FrameLayout.LayoutParams(MATCH, MATCH))
        // brilho suave por cima, como foto revelada
        val shine = View(this)
        shine.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.argb(46, 255, 255, 255), Color.TRANSPARENT, Color.argb(34, 0, 0, 0))
        )
        photo.addView(shine, FrameLayout.LayoutParams(MATCH, MATCH))
    } else {
        photo.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(mixColor(g.primary, Color.WHITE, 0.45f), g.primary)
        )
        photo.addView(IconView(this, "heart", Color.WHITE, 78), FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
        photo.addView(IconView(this, "sparkle", Color.WHITE, 24), FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP or Gravity.START).also { it.setMargins(dp(26), dp(26), 0, 0) })
        photo.addView(IconView(this, "heart", Color.parseColor("#B3FFFFFF"), 26), FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM or Gravity.END).also { it.setMargins(0, 0, dp(28), dp(28)) })
    }
    val plp = FrameLayout.LayoutParams(MATCH, dp(ph - 10 - 62))
    plp.setMargins(dp(10), dp(10), dp(10), 0)
    paper.addView(photo, plp)

    // legenda escrita à mão: os nomes do casal com um coração no meio
    val names = coupleNames(d.couple)
    val cap = fitLabel(names, 21f, Color.parseColor("#4A3340"), true, true, 12f, true)
    cap.gravity = Gravity.CENTER
    cap.maxLines = 2
    cap.ellipsize = TextUtils.TruncateAt.END
    val clp = FrameLayout.LayoutParams(MATCH, dp(62), Gravity.BOTTOM)
    clp.setMargins(dp(14), 0, dp(14), 0)
    cap.gravity = Gravity.CENTER
    paper.addView(cap, clp)

    // palco: a aura animada fica por trás/em volta da polaroid e vai de ponta a ponta do cartão
    val stage = FrameLayout(this)
    stage.clipChildren = false
    stage.clipToPadding = false
    stage.addView(LoveAura(this, g, pw.toFloat(), ph.toFloat()), FrameLayout.LayoutParams(MATCH, MATCH))
    stage.addView(frame, FrameLayout.LayoutParams(dp(pw), dp(ph), Gravity.CENTER))
    col.addView(stage, lin(MATCH, dp(ph + 96), l = -16, r = -16, t = 0))

    // etiqueta final: de qual gênero é esse shipp
    val tag = pill("Shipp de " + g.label, g.primary, Color.WHITE, 12.5f, g.icon)
    col.addView(tag, lin(WRAP, WRAP, t = 2))
    return root
}

/** "Eu e Mo", "Ana & Léo", "A + B" viram "Eu ♥ Mo"; um nome só fica como está. */
private fun coupleNames(raw: String): String {
    val s = raw.trim()
    if (s.isEmpty()) return "Meu casal"
    val parts = s.split(Regex("""\s+(?:[eE]|&|\+|[xX])\s+"""))
    if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
        return parts[0].trim() + " ♥ " + parts[1].trim()
    }
    return s
}


/**
 * Fileira que rola para o lado e apaga as pontas com um degradê de verdade (some para transparente,
 * deixando aparecer o fundo do cartão). O esmaecer nativo do Android desenha uma faixa por cima; este não.
 */
class FadeScrollView(ctx: Context, private val fadeW: Float) : android.widget.HorizontalScrollView(ctx) {
    private val erase = Paint().apply { xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_OUT) }
    private val leftShader = LinearGradient(0f, 0f, fadeW, 0f, Color.BLACK, Color.TRANSPARENT, Shader.TileMode.CLAMP)
    private val rightShader = LinearGradient(0f, 0f, fadeW, 0f, Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP)

    override fun dispatchDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f || childCount == 0) {
            super.dispatchDraw(c)
            return
        }
        // o canvas já vem deslocado pela rolagem: desenhamos a camada na janela visível
        val sx = scrollX.toFloat()
        val layer = c.saveLayer(sx, 0f, sx + w, h, null)
        super.dispatchDraw(c)
        val maxScroll = (computeHorizontalScrollRange() - width).coerceAtLeast(0)
        val left = (sx / fadeW).coerceIn(0f, 1f)
        val right = ((maxScroll - sx) / fadeW).coerceIn(0f, 1f)
        if (left > 0f) {
            c.save()
            c.translate(sx, 0f)
            erase.shader = leftShader
            erase.alpha = (255 * left).toInt()
            c.drawRect(0f, 0f, fadeW, h, erase)
            c.restore()
        }
        if (right > 0f) {
            c.save()
            c.translate(sx + w - fadeW, 0f)
            erase.shader = rightShader
            erase.alpha = (255 * right).toInt()
            c.drawRect(0f, 0f, fadeW, h, erase)
            c.restore()
        }
        erase.shader = null
        c.restoreToCount(layer)
    }
}
