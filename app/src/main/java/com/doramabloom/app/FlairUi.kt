package com.doramabloom.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
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
                invalidate()
            }
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
    private var dimT = 0f
    private var dimRaw = 0f
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
        val R = 48f * u
        val br = 27f * u
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val bdx = e.x - bx()
                val bdy = e.y - vy()
                val vdx = e.x - vx()
                val vdy = e.y - vy()
                if (bdx * bdx + bdy * bdy <= (br + 10f * u) * (br + 10f * u)) {
                    zone = 1
                    pressing = true
                } else if (e.y > 122f * u) {
                    zone = 2
                    seeking = true
                    seekFrac = fracOf(e.x)
                } else if (vdx * vdx + vdy * vdy <= R * R) {
                    zone = 3
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
                if (zone == 1 || zone == 3) onToggle?.invoke()
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
        // escurece devagar enquanto toca (para destacar o disco) e clareia devagar ao pausar:
        // progresso linear no tempo (~1,3 s) passado por uma curva suave de entrada e saída
        val dimTarget = if (playing) 1f else 0f
        val dimStep = dt / 1.3f
        dimRaw = if (dimRaw < dimTarget) minOf(dimTarget, dimRaw + dimStep) else maxOf(dimTarget, dimRaw - dimStep)
        dimT = dimRaw * dimRaw * (3f - 2f * dimRaw)

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
        p.shader = LinearGradient(
            0f, 0f, w, h,
            mixColor(mixColor(primary, white, 0.34f), mixColor(deep, Color.BLACK, 0.55f), dimT * 0.88f),
            mixColor(mixColor(primary, deep, 0.30f), mixColor(deep, Color.BLACK, 0.80f), dimT * 0.92f),
            Shader.TileMode.CLAMP
        )
        c.drawRoundRect(rect, rad, rad, p)
        p.shader = null
        clip.reset()
        clip.addRoundRect(rect, rad, rad, Path.Direction.CW)
        c.save()
        c.clipPath(clip)

        // foco de luz no disco enquanto toca
        if (dimT > 0.01f) {
            p.style = Paint.Style.FILL
            p.shader = RadialGradient(
                cx, cy, R * 2.3f,
                intArrayOf(mixColor(primary, white, 0.25f), mixColor(primary, deep, 0.2f), Color.TRANSPARENT),
                floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP
            )
            p.alpha = (110 * dimT).toInt()
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
        val dimBusy = dimRaw != dimTarget
        if (playing || pts.isNotEmpty() || armBusy || pressBusy || seeking || dimBusy) postInvalidateOnAnimation()
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
 * Casal favorito: uma polaroid grandona e centralizada, levemente torta e flutuando, com fita adesiva,
 * adesivo do gênero, coração batendo, os nomes escritos à mão e corações subindo no fundo.
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

    // adesivo do gênero no canto
    val seal = SealView(this)
    seal.set(g)
    seal.rotation = -12f
    val slp = FrameLayout.LayoutParams(dp(54), dp(54), Gravity.BOTTOM or Gravity.START)
    slp.setMargins(-dp(14), 0, 0, dp(46))
    paper.addView(seal, slp)

    // coração batendo
    val beat = Beat(this)
    val bbg = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(mixColor(g.primary, Color.WHITE, 0.25f), g.deep))
    bbg.shape = GradientDrawable.OVAL
    bbg.setStroke(dp(3), Color.WHITE)
    beat.background = bbg
    beat.elevation = dp(5).toFloat()
    beat.addView(IconView(this, "heart", Color.WHITE, 26), FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
    val blp = FrameLayout.LayoutParams(dp(58), dp(58), Gravity.BOTTOM or Gravity.END)
    blp.setMargins(0, 0, -dp(14), dp(40))
    paper.addView(beat, blp)

    col.addView(frame, lin(dp(pw), dp(ph), t = 26, b = 4))

    // etiqueta final: de qual gênero é esse shipp
    val tag = pill("Shipp de " + g.label, g.primary, Color.WHITE, 12.5f, g.icon)
    col.addView(tag, lin(WRAP, WRAP, t = 22))
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
