package com.doramabloom.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Ferramentas de desenho das cenas refeitas (Romance, Fantasia, Histórico, Comédia, Médico,
 * Família, Vida real, Vingança e Crime). Tudo aqui é pensado para NÃO piscar:
 *  - brilhos são um degradê radial de verdade (nada de anéis que aparecem e somem);
 *  - as formas são desenhadas por caminhos (Path) com escala contínua, sem "pulos" de pixel;
 *  - os efeitos de acender e apagar usam curvas suaves, nunca liga/desliga seco.
 * Convenção: alfa vai de 0 a 255, e as formas unitárias vão de -1 a 1 (o tamanho "s" é o raio).
 */

private const val PIF = 3.14159265f

private val glowCache = HashMap<Int, RadialGradient>()

private fun mk(build: Path.() -> Unit): Path {
    val pa = Path()
    pa.build()
    return pa
}

private val HEART = mk {
    moveTo(0f, 0.95f)
    cubicTo(-0.2f, 0.75f, -1.05f, 0.2f, -1f, -0.35f)
    cubicTo(-0.95f, -0.85f, -0.3f, -1.05f, 0f, -0.5f)
    cubicTo(0.3f, -1.05f, 0.95f, -0.85f, 1f, -0.35f)
    cubicTo(1.05f, 0.2f, 0.2f, 0.75f, 0f, 0.95f)
    close()
}

/** Pétala de cerejeira: ponta embaixo (0,1) e um entalhe no topo. */
private val PETAL = mk {
    moveTo(0f, 1f)
    cubicTo(-0.25f, 0.6f, -0.85f, 0.1f, -0.7f, -0.5f)
    cubicTo(-0.6f, -0.9f, -0.2f, -1f, 0f, -0.78f)
    cubicTo(0.2f, -1f, 0.6f, -0.9f, 0.7f, -0.5f)
    cubicTo(0.85f, 0.1f, 0.25f, 0.6f, 0f, 1f)
    close()
}

private val DROP = mk {
    moveTo(0f, -1f)
    cubicTo(0.2f, -0.6f, 0.7f, -0.1f, 0.7f, 0.35f)
    cubicTo(0.7f, 0.8f, 0.38f, 1f, 0f, 1f)
    cubicTo(-0.38f, 1f, -0.7f, 0.8f, -0.7f, 0.35f)
    cubicTo(-0.7f, -0.1f, -0.2f, -0.6f, 0f, -1f)
    close()
}

private val LEAF = mk {
    moveTo(0f, -1f)
    cubicTo(0.95f, -0.5f, 0.95f, 0.5f, 0f, 1f)
    cubicTo(-0.95f, 0.5f, -0.95f, -0.5f, 0f, -1f)
    close()
}

/** Estrela de quatro pontas com lados côncavos (brilho). */
private val SPARK = mk {
    moveTo(0f, -1f)
    quadTo(0.09f, -0.09f, 1f, 0f)
    quadTo(0.09f, 0.09f, 0f, 1f)
    quadTo(-0.09f, 0.09f, -1f, 0f)
    quadTo(-0.09f, -0.09f, 0f, -1f)
    close()
}

private val WING_U = mk {
    moveTo(0.05f, -0.1f)
    cubicTo(0.3f, -1f, 1.1f, -1f, 1f, -0.35f)
    cubicTo(0.95f, 0.05f, 0.5f, 0.1f, 0.05f, 0.05f)
    close()
}

private val WING_L = mk {
    moveTo(0.05f, 0.05f)
    cubicTo(0.6f, 0.1f, 0.95f, 0.4f, 0.75f, 0.75f)
    cubicTo(0.55f, 1f, 0.2f, 0.7f, 0.05f, 0.2f)
    close()
}

private val GEM = mk {
    moveTo(0f, -1f)
    lineTo(0.72f, -0.25f)
    lineTo(0f, 1f)
    lineTo(-0.72f, -0.25f)
    close()
}

private val GEM_HI = mk {
    moveTo(0f, -1f)
    lineTo(0.72f, -0.25f)
    lineTo(0f, -0.25f)
    close()
}

/** Sinal de mais com pontas arredondadas. */
private val PLUS = mk {
    val a = 0.34f
    val r = 0.26f
    moveTo(-a, -1f + r)
    quadTo(-a, -1f, -a + r, -1f)
    lineTo(a - r, -1f)
    quadTo(a, -1f, a, -1f + r)
    lineTo(a, -a)
    lineTo(1f - r, -a)
    quadTo(1f, -a, 1f, -a + r)
    lineTo(1f, a - r)
    quadTo(1f, a, 1f - r, a)
    lineTo(a, a)
    lineTo(a, 1f - r)
    quadTo(a, 1f, a - r, 1f)
    lineTo(-a + r, 1f)
    quadTo(-a, 1f, -a, 1f - r)
    lineTo(-a, a)
    lineTo(-1f + r, a)
    quadTo(-1f, a, -1f, a - r)
    lineTo(-1f, -a + r)
    quadTo(-1f, -a, -1f + r, -a)
    lineTo(-a, -a)
    close()
}

/** Telhado de hanok (casa coreana): beiral curvado para cima nas pontas. */
private val ROOF = mk {
    moveTo(-0.55f, -0.95f)
    lineTo(0.55f, -0.95f)
    quadTo(0.75f, -0.05f, 1.4f, 0.02f)
    quadTo(0.7f, 0.4f, 0f, 0.4f)
    quadTo(-0.7f, 0.4f, -1.4f, 0.02f)
    quadTo(-0.75f, -0.05f, -0.55f, -0.95f)
    close()
}

private val CRESCENT = Path()

private var crescentReady = false

/** Zera o estado do pincel compartilhado (traço, sombra, texto) para a próxima cena. */
fun SceneKit.tidy() {
    p.shader = null
    p.pathEffect = null
    p.strokeCap = Paint.Cap.BUTT
    p.strokeJoin = Paint.Join.MITER
    p.typeface = null
    p.textAlign = Paint.Align.LEFT
    p.style = Paint.Style.FILL
    p.alpha = 255
}

/** Suavização clássica 0..1. */
fun SceneKit.ss(x: Float): Float {
    val v = x.coerceIn(0f, 1f)
    return v * v * (3f - 2f * v)
}

/** Entra e sai bem devagar, ficando inteiro no meio da vida (0..1). */
fun SceneKit.fade(life: Float): Float = ss(life / 0.16f) * ss((1f - life) / 0.16f)

/** Cintilar suave de 0 a 1 (sem liga/desliga). */
fun SceneKit.tw(t: Float, ph: Float, sp: Float): Float {
    val s = 0.5f + 0.5f * sin(t * sp + ph * twoPi)
    return s * s
}

/** Número "aleatório" fixo entre 0 e 1 para cada inteiro (sem estado). */
fun SceneKit.hash(i: Int): Float {
    val x = sin(i * 12.9898f + 78.233f) * 43758.547f
    return x - floor(x)
}

/** Ponto de uma curva quadrática (para colocar coisas ao longo de galhos e cordas). */
fun SceneKit.qb(a: Float, b: Float, c: Float, tt: Float): Float {
    val v = 1f - tt
    return v * v * a + 2f * v * tt * b + tt * tt * c
}

/** Brilho macio de verdade: degradê radial guardado em cache, tingido pela cor. */
fun SceneKit.soft(c: Canvas, x: Float, y: Float, r: Float, color: Int, a: Float) {
    if (a <= 1f || r <= 0.5f) return
    val key = color or (0xFF shl 24)
    var sh = glowCache[key]
    if (sh == null) {
        sh = RadialGradient(
            0f, 0f, 100f,
            intArrayOf(al(key, 255f), al(key, 118f), al(key, 36f), al(key, 0f)),
            floatArrayOf(0f, 0.3f, 0.64f, 1f), Shader.TileMode.CLAMP
        )
        if (glowCache.size > 60) glowCache.clear()
        glowCache[key] = sh
    }
    p.style = Paint.Style.FILL
    p.shader = sh
    p.alpha = a.coerceIn(0f, 255f).toInt()
    c.save()
    c.translate(x, y)
    c.scale(r / 100f, r / 100f)
    c.drawCircle(0f, 0f, 100f, p)
    c.restore()
    p.shader = null
    p.alpha = 255
}

/** Brilho achatado (neblina, faixas de luz). */
fun SceneKit.softOval(c: Canvas, x: Float, y: Float, rx: Float, ry: Float, color: Int, a: Float) {
    if (a <= 1f || rx <= 0.5f || ry <= 0.5f) return
    val key = color or (0xFF shl 24)
    var sh = glowCache[key]
    if (sh == null) {
        sh = RadialGradient(
            0f, 0f, 100f,
            intArrayOf(al(key, 255f), al(key, 118f), al(key, 36f), al(key, 0f)),
            floatArrayOf(0f, 0.3f, 0.64f, 1f), Shader.TileMode.CLAMP
        )
        if (glowCache.size > 60) glowCache.clear()
        glowCache[key] = sh
    }
    p.style = Paint.Style.FILL
    p.shader = sh
    p.alpha = a.coerceIn(0f, 255f).toInt()
    c.save()
    c.translate(x, y)
    c.scale(rx / 100f, ry / 100f)
    c.drawCircle(0f, 0f, 100f, p)
    c.restore()
    p.shader = null
    p.alpha = 255
}

fun SceneKit.circ(c: Canvas, x: Float, y: Float, r: Float, color: Int, a: Float) {
    if (a <= 0.5f || r <= 0.2f) return
    p.style = Paint.Style.FILL
    p.color = al(color, a)
    c.drawCircle(x, y, r, p)
}

fun SceneKit.ring(c: Canvas, x: Float, y: Float, r: Float, sw: Float, color: Int, a: Float) {
    if (a <= 0.5f || r <= 0.2f) return
    p.style = Paint.Style.STROKE
    p.strokeWidth = sw
    p.color = al(color, a)
    c.drawCircle(x, y, r, p)
}

fun SceneKit.line(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, sw: Float, color: Int, a: Float) {
    if (a <= 0.5f) return
    p.style = Paint.Style.STROKE
    p.strokeWidth = sw
    p.strokeCap = Paint.Cap.ROUND
    p.color = al(color, a)
    c.drawLine(x1, y1, x2, y2, p)
}

fun SceneKit.rrect(c: Canvas, l: Float, tp: Float, r: Float, b: Float, rad: Float, color: Int, a: Float) {
    if (a <= 0.5f) return
    p.style = Paint.Style.FILL
    p.color = al(color, a)
    rect.set(min(l, r), min(tp, b), max(l, r), max(tp, b))
    c.drawRoundRect(rect, rad, rad, p)
}

fun SceneKit.oval(c: Canvas, l: Float, tp: Float, r: Float, b: Float, color: Int, a: Float) {
    if (a <= 0.5f) return
    p.style = Paint.Style.FILL
    p.color = al(color, a)
    rect.set(min(l, r), min(tp, b), max(l, r), max(tp, b))
    c.drawOval(rect, p)
}

/** Texto com tamanho fixo e escala pelo canvas (não "treme" ao crescer). */
fun SceneKit.label(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, a: Float, rot: Float, scale: Float) {
    if (a <= 0.5f || scale <= 0.02f) return
    p.style = Paint.Style.FILL
    p.typeface = Typeface.DEFAULT_BOLD
    p.textAlign = Paint.Align.CENTER
    p.textSize = size
    p.color = al(color, a)
    c.save()
    c.translate(x, y)
    c.rotate(rot)
    c.scale(scale, scale)
    c.drawText(s, 0f, size * 0.35f, p)
    c.restore()
    p.textAlign = Paint.Align.LEFT
    p.typeface = null
}

/** Desenha um caminho unitário com tamanho, giro e achatamento. */
fun SceneKit.shape(c: Canvas, path: Path, x: Float, y: Float, s: Float, rot: Float, color: Int, a: Float, sx: Float = 1f, sy: Float = 1f) {
    if (a <= 0.5f || s <= 0.2f) return
    p.style = Paint.Style.FILL
    p.color = al(color, a)
    c.save()
    c.translate(x, y)
    c.rotate(rot)
    c.scale(s * sx, s * sy)
    c.drawPath(path, p)
    c.restore()
}

/** Coração com brilho. */
fun SceneKit.heart(c: Canvas, x: Float, y: Float, s: Float, rot: Float, color: Int, a: Float) {
    if (a <= 0.5f || s <= 0.3f) return
    shape(c, HEART, x, y, s, rot, color, a)
    p.style = Paint.Style.FILL
    p.color = al(Color.WHITE, a * 0.5f)
    c.save()
    c.translate(x, y)
    c.rotate(rot)
    c.scale(s, s)
    rect.set(-0.76f, -0.66f, -0.36f, -0.4f)
    c.drawOval(rect, p)
    c.restore()
}

/** Pétala de cerejeira; flip de 0 a 1 simula ela virando no ar. */
fun SceneKit.petal(c: Canvas, x: Float, y: Float, s: Float, rot: Float, flip: Float, color: Int, a: Float) {
    shape(c, PETAL, x, y, s, rot, color, a, 0.25f + 0.75f * flip, 1f)
}

/** Flor de cerejeira de cinco pétalas, com miolo e estames. */
fun SceneKit.blossom(c: Canvas, x: Float, y: Float, s: Float, rot: Float, color: Int, core: Int, a: Float) {
    if (a <= 0.5f || s <= 0.6f) return
    val sp = s * 0.5f
    p.style = Paint.Style.FILL
    p.color = al(color, a)
    for (i in 0 until 5) {
        c.save()
        c.translate(x, y)
        c.rotate(rot + i * 72f)
        c.translate(0f, -sp)
        c.scale(sp, sp)
        c.drawPath(PETAL, p)
        c.restore()
    }
    p.color = al(core, a)
    c.drawCircle(x, y, s * 0.15f, p)
    p.style = Paint.Style.STROKE
    p.strokeWidth = max(0.6f, s * 0.05f)
    p.strokeCap = Paint.Cap.ROUND
    p.color = al(core, a * 0.9f)
    for (i in 0 until 5) {
        val an = (rot + i * 72f + 36f) * PIF / 180f
        c.drawLine(x, y, x + sin(an) * s * 0.36f, y - cos(an) * s * 0.36f, p)
    }
    p.style = Paint.Style.FILL
}

/** Brilho de quatro pontas com halo macio. */
fun SceneKit.sparkle(c: Canvas, x: Float, y: Float, s: Float, rot: Float, color: Int, a: Float) {
    if (a <= 1f || s <= 0.4f) return
    soft(c, x, y, s * 2.2f, color, a * 0.4f)
    shape(c, SPARK, x, y, s, rot, Color.WHITE, a)
}

fun SceneKit.drop(c: Canvas, x: Float, y: Float, s: Float, rot: Float, color: Int, a: Float) {
    shape(c, DROP, x, y, s, rot, color, a)
    if (a > 1f && s > 1f) {
        p.style = Paint.Style.FILL
        p.color = al(Color.WHITE, a * 0.45f)
        c.save()
        c.translate(x, y)
        c.rotate(rot)
        c.scale(s, s)
        rect.set(-0.42f, 0.05f, -0.2f, 0.55f)
        c.drawOval(rect, p)
        c.restore()
    }
}

/** Folha com nervura central. */
fun SceneKit.leaf(c: Canvas, x: Float, y: Float, s: Float, rot: Float, flip: Float, color: Int, vein: Int, a: Float) {
    if (a <= 0.5f || s <= 0.3f) return
    shape(c, LEAF, x, y, s, rot, color, a, 0.55f * (0.3f + 0.7f * flip), 1f)
    p.style = Paint.Style.STROKE
    p.strokeWidth = max(0.5f, s * 0.07f)
    p.strokeCap = Paint.Cap.ROUND
    p.color = al(vein, a * 0.8f)
    c.save()
    c.translate(x, y)
    c.rotate(rot)
    c.drawLine(0f, -s * 0.9f, 0f, s * 0.9f, p)
    c.restore()
}

/** Borboleta com asas de duas cores; flap de 0 a 1 é o bater de asas. */
fun SceneKit.butterfly(c: Canvas, x: Float, y: Float, s: Float, rot: Float, flap: Float, c1: Int, c2: Int, a: Float) {
    if (a <= 0.5f || s <= 0.5f) return
    val fx = 0.22f + 0.78f * flap
    p.style = Paint.Style.FILL
    c.save()
    c.translate(x, y)
    c.rotate(rot)
    c.scale(s, s)
    for (side in 0..1) {
        val sg = if (side == 0) 1f else -1f
        c.save()
        c.scale(sg * fx, 1f)
        p.color = al(c1, a)
        c.drawPath(WING_U, p)
        p.color = al(c2, a)
        c.drawPath(WING_L, p)
        p.color = al(Color.WHITE, a * 0.75f)
        c.drawCircle(0.62f, -0.45f, 0.13f, p)
        c.drawCircle(0.5f, 0.5f, 0.09f, p)
        c.restore()
    }
    p.color = al(Color.parseColor("#3B2A55"), a)
    rect.set(-0.07f, -0.55f, 0.07f, 0.68f)
    c.drawRoundRect(rect, 0.07f, 0.07f, p)
    p.style = Paint.Style.STROKE
    p.strokeWidth = 0.05f
    p.strokeCap = Paint.Cap.ROUND
    c.drawLine(0f, -0.55f, 0.24f, -0.95f, p)
    c.drawLine(0f, -0.55f, -0.24f, -0.95f, p)
    c.restore()
}

/** Pedra preciosa lapidada. */
fun SceneKit.gem(c: Canvas, x: Float, y: Float, s: Float, rot: Float, color: Int, a: Float) {
    if (a <= 0.5f || s <= 0.4f) return
    soft(c, x, y, s * 2.3f, color, a * 0.45f)
    shape(c, GEM, x, y, s, rot, color, a, 0.85f, 1f)
    shape(c, GEM_HI, x, y, s, rot, Color.WHITE, a * 0.55f, 0.85f, 1f)
}

/** Sinal de mais arredondado (cruz médica). */
fun SceneKit.plus(c: Canvas, x: Float, y: Float, s: Float, rot: Float, color: Int, a: Float) {
    shape(c, PLUS, x, y, s, rot, color, a)
}

/** Nuvem fofa (cor opaca para as bolas não aparecerem sobrepostas). */
fun SceneKit.cloud(c: Canvas, x: Float, y: Float, s: Float, color: Int, a: Float) {
    if (a <= 0.5f || s <= 1f) return
    p.style = Paint.Style.FILL
    p.color = al(color, a)
    c.drawCircle(x - s * 0.42f, y, s * 0.3f, p)
    c.drawCircle(x - s * 0.06f, y - s * 0.16f, s * 0.4f, p)
    c.drawCircle(x + s * 0.34f, y - s * 0.02f, s * 0.32f, p)
    rect.set(x - s * 0.72f, y - s * 0.02f, x + s * 0.66f, y + s * 0.3f)
    c.drawRoundRect(rect, s * 0.15f, s * 0.15f, p)
}

/** Lua crescente com a abertura virada para cima e para a direita. */
fun SceneKit.crescent(c: Canvas, x: Float, y: Float, s: Float, rot: Float, color: Int, a: Float) {
    if (!crescentReady) {
        crescentReady = true
        val big = Path()
        big.addCircle(0f, 0f, 1f, Path.Direction.CW)
        val cut = Path()
        cut.addCircle(0.46f, -0.14f, 0.84f, Path.Direction.CW)
        CRESCENT.set(big)
        CRESCENT.op(cut, Path.Op.DIFFERENCE)
    }
    shape(c, CRESCENT, x, y, s, rot, color, a)
}

/** Pulso (0..1) de um batimento: dois toques suaves e uma pausa. */
fun SceneKit.pulse(t: Float, period: Float): Float {
    if (calm) return 0.2f // Estante: sem batimento de luz, o brilho fica estável
    val ph = frac(t / period)
    return min(1f, bump(ph, 0.12f, 0.07f) + 0.6f * bump(ph, 0.3f, 0.08f))
}

/** Onda de ECG de um ciclo (x de 0 a 1) devolvendo altura de -1 a 1. */
fun SceneKit.ecg(x: Float): Float {
    val f = x - floor(x)
    var v = 0.14f * exp(-((f - 0.18f) / 0.05f) * ((f - 0.18f) / 0.05f))
    v -= 0.16f * exp(-((f - 0.36f) / 0.012f) * ((f - 0.36f) / 0.012f))
    v += 1f * exp(-((f - 0.4f) / 0.014f) * ((f - 0.4f) / 0.014f))
    v -= 0.3f * exp(-((f - 0.44f) / 0.014f) * ((f - 0.44f) / 0.014f))
    v += 0.24f * exp(-((f - 0.64f) / 0.07f) * ((f - 0.64f) / 0.07f))
    return v
}

/** Distância entre dois pontos. */
fun SceneKit.dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

/** Mistura duas cores (0 = a, 1 = b). */
fun SceneKit.mix(a: Int, b: Int, f: Float): Int {
    val q = f.coerceIn(0f, 1f)
    return Color.rgb(
        (Color.red(a) + (Color.red(b) - Color.red(a)) * q).toInt(),
        (Color.green(a) + (Color.green(b) - Color.green(a)) * q).toInt(),
        (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * q).toInt()
    )
}

/** Ondinha de contorno em volta da capa, bem suave. */
fun SceneKit.posterRipple(c: Canvas, poster: android.graphics.RectF, hasPoster: Boolean, t: Float, period: Float, color: Int, strength: Float) {
    if (!hasPoster) return
    p.style = Paint.Style.STROKE
    for (j in 0 until 2) {
        val life = frac(t / period - j * 0.5f)
        val e = life * 30f * u
        p.strokeWidth = (2.4f - 1.8f * life) * u
        val fd = (1f - life)
        p.color = al(color, strength * fd * fd * ss(life / 0.12f))
        rect.set(poster.left - e, poster.top - e, poster.right + e, poster.bottom + e)
        c.drawRoundRect(rect, 20f * u + e, 20f * u + e, p)
    }
    p.style = Paint.Style.FILL
}

/** Faixa de abs para evitar import duplicado nas cenas. */
fun SceneKit.ab(x: Float): Float = abs(x)

/** Cosseno em graus (comodidade). */
fun SceneKit.cosd(deg: Float): Float = cos(deg * PIF / 180f)

/** Seno em graus (comodidade). */
fun SceneKit.sind(deg: Float): Float = sin(deg * PIF / 180f)
