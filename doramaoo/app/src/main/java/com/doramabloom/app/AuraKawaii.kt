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

/**
 * Cenas fofinhas e detalhadas do Início: Romance e Comédia.
 * Tudo é desenhado por código (nada de emotes/ícones prontos) e calculado só pelo relógio,
 * com curvas suaves, para não piscar.
 */

private fun kc(s: String): Int = Color.parseColor(s)

// --------------------------------------------------------------------------------------
// Peças compartilhadas
// --------------------------------------------------------------------------------------

/** Nuvenzinha fofa feita de bolinhas, com sombra lilás por baixo. (x,y) é o centro; s é a meia-largura. */
private fun SceneKit.kwCloud(c: Canvas, x: Float, y: Float, s: Float, body: Int, shade: Int) {
    oval(c, x - s, y - s * 0.02f, x + s, y + s * 0.6f, shade, 150f)
    circ(c, x - s * 0.52f, y + s * 0.1f, s * 0.36f, body, 255f)
    circ(c, x + s * 0.54f, y + s * 0.12f, s * 0.33f, body, 255f)
    circ(c, x - s * 0.14f, y - s * 0.16f, s * 0.48f, body, 255f)
    circ(c, x + s * 0.24f, y - s * 0.02f, s * 0.4f, body, 255f)
    rrect(c, x - s * 0.55f, y + s * 0.08f, x + s * 0.57f, y + s * 0.46f, s * 0.2f, body, 255f)
}

/** Carinha dorminhoca de kawaii: olhinhos fechados, boquinha e bochechas. */
private fun SceneKit.kwSleepy(c: Canvas, x: Float, y: Float, r: Float, ink: Int, blush: Int) {
    p.style = Paint.Style.STROKE
    p.strokeCap = Paint.Cap.ROUND
    p.strokeWidth = max(1f, r * 0.11f)
    p.color = al(ink, 255f)
    rect.set(x - r * 0.62f, y - r * 0.22f, x - r * 0.18f, y + r * 0.2f)
    c.drawArc(rect, 200f, 140f, false, p)
    rect.set(x + r * 0.18f, y - r * 0.22f, x + r * 0.62f, y + r * 0.2f)
    c.drawArc(rect, 200f, 140f, false, p)
    rect.set(x - r * 0.16f, y + r * 0.02f, x + r * 0.16f, y + r * 0.34f)
    c.drawArc(rect, 25f, 130f, false, p)
    circ(c, x - r * 0.82f, y + r * 0.22f, r * 0.2f, blush, 175f)
    circ(c, x + r * 0.82f, y + r * 0.22f, r * 0.2f, blush, 175f)
}

/** Bolha de sabão: aro leve, brilho curvo e pontinho de luz. */
private fun SceneKit.kwBubble(c: Canvas, x: Float, y: Float, r: Float, tint: Int, a: Float) {
    circ(c, x, y, r, tint, a * 0.2f)
    ring(c, x, y, r, max(0.8f, r * 0.07f), Color.WHITE, a * 0.75f)
    p.style = Paint.Style.STROKE
    p.strokeCap = Paint.Cap.ROUND
    p.strokeWidth = max(0.8f, r * 0.14f)
    p.color = al(Color.WHITE, a * 0.9f)
    rect.set(x - r * 0.68f, y - r * 0.68f, x + r * 0.68f, y + r * 0.68f)
    c.drawArc(rect, 200f, 55f, false, p)
    circ(c, x + r * 0.42f, y + r * 0.42f, r * 0.1f, Color.WHITE, a * 0.75f)
}

private fun kwHillY(w: Float, top: Float, amp: Float, ph: Float, x: Float): Float {
    val f = x / max(1f, w)
    return top + amp * sin(f * 6.2832f * 0.8f + ph)
}

private fun SceneKit.kwHill(c: Canvas, w: Float, h: Float, top: Float, amp: Float, ph: Float, color: Int) {
    path.reset()
    path.moveTo(0f, h)
    val n = 28
    for (i in 0..n) {
        val x = w * i / n
        path.lineTo(x, kwHillY(w, top, amp, ph, x))
    }
    path.lineTo(w, h)
    path.close()
    p.style = Paint.Style.FILL
    p.color = al(color, 255f)
    c.drawPath(path, p)
}

/** Balão com brilho e nozinho. (x,y) é o centro do balão. */
private fun SceneKit.kwBalloon(c: Canvas, x: Float, y: Float, r: Float, rot: Float, color: Int) {
    c.save()
    c.translate(x, y)
    c.rotate(rot)
    oval(c, -r * 0.82f, -r, r * 0.82f, r, color, 255f)
    oval(c, -r * 0.5f, -r * 0.45f, r * 0.72f, r * 0.92f, mix(color, Color.BLACK, 0.12f), 110f)
    path.reset()
    path.moveTo(0f, r * 0.96f)
    path.lineTo(-r * 0.17f, r * 1.2f)
    path.lineTo(r * 0.17f, r * 1.2f)
    path.close()
    p.style = Paint.Style.FILL
    p.color = al(color, 255f)
    c.drawPath(path, p)
    oval(c, -r * 0.52f, -r * 0.8f, -r * 0.2f, -r * 0.36f, Color.WHITE, 170f)
    circ(c, -r * 0.34f, -r * 0.18f, r * 0.07f, Color.WHITE, 200f)
    c.restore()
}

// --------------------------------------------------------------------------------------
// Peças do Romance
// --------------------------------------------------------------------------------------

/** Passarinho gordinho de bochecha rosada. dir = 1 olha para a direita, -1 para a esquerda. */
private fun SceneKit.kwBird(
    c: Canvas, x: Float, y: Float, s: Float, dir: Float,
    body: Int, wing: Int, belly: Int, lean: Float, t: Float
) {
    val ink = kc("#5A2340")
    val orange = kc("#FFA94D")
    c.save()
    c.translate(x, y)
    c.rotate(dir * lean * 16f)
    // cauda
    oval(c, -dir * s * 1.55f, -s * 0.3f, -dir * s * 0.55f, s * 0.22f, wing, 255f)
    // pezinhos
    line(c, -s * 0.25f, s * 0.92f, -s * 0.25f, s * 1.22f, s * 0.13f, orange, 255f)
    line(c, s * 0.25f, s * 0.92f, s * 0.25f, s * 1.22f, s * 0.13f, orange, 255f)
    // corpo e barriguinha
    circ(c, 0f, 0f, s, body, 255f)
    oval(c, -s * 0.45f, s * 0.1f, s * 0.5f, s * 0.86f, belly, 255f)
    // asinha batendo de leve
    c.save()
    c.rotate(-dir * (4f + 5f * sin(t * 3.2f)))
    oval(c, -dir * s * 0.78f, -s * 0.15f, -dir * s * 0.08f, s * 0.58f, wing, 255f)
    c.restore()
    // topetinho
    c.save()
    c.rotate(dir * 18f)
    oval(c, -s * 0.1f, -s * 1.3f, s * 0.12f, -s * 0.82f, body, 255f)
    c.restore()
    // olho, brilho, bochecha e bico
    circ(c, dir * s * 0.42f, -s * 0.22f, s * 0.15f, ink, 255f)
    circ(c, dir * s * 0.46f, -s * 0.27f, s * 0.055f, Color.WHITE, 255f)
    circ(c, dir * s * 0.64f, s * 0.14f, s * 0.17f, kc("#FF8FB0"), 190f)
    path.reset()
    path.moveTo(dir * s * 0.92f, -s * 0.16f)
    path.lineTo(dir * s * 1.4f, -s * 0.02f)
    path.lineTo(dir * s * 0.92f, s * 0.1f)
    path.close()
    p.style = Paint.Style.FILL
    p.color = al(orange, 255f)
    c.drawPath(path, p)
    c.restore()
}

/** Lanterna de papel com borlas douradas. (x,y) é o centro. */
private fun SceneKit.kwLantern(c: Canvas, x: Float, y: Float, s: Float, body: Int, glowColor: Int, gold: Int) {
    soft(c, x, y, s * 2.6f, glowColor, 120f)
    oval(c, x - s * 0.85f, y - s, x + s * 0.85f, y + s, body, 255f)
    p.style = Paint.Style.STROKE
    p.strokeWidth = max(0.8f, s * 0.07f)
    p.color = al(mix(body, Color.BLACK, 0.25f), 150f)
    rect.set(x - s * 0.45f, y - s, x + s * 0.45f, y + s)
    c.drawOval(rect, p)
    rect.set(x - s * 0.15f, y - s, x + s * 0.15f, y + s)
    c.drawOval(rect, p)
    oval(c, x - s * 0.5f, y - s * 0.75f, x - s * 0.18f, y - s * 0.2f, Color.WHITE, 120f)
    rrect(c, x - s * 0.42f, y - s * 1.14f, x + s * 0.42f, y - s * 0.86f, s * 0.1f, gold, 255f)
    rrect(c, x - s * 0.42f, y + s * 0.86f, x + s * 0.42f, y + s * 1.14f, s * 0.1f, gold, 255f)
    line(c, x, y + s * 1.12f, x, y + s * 1.75f, max(0.8f, s * 0.07f), gold, 255f)
    circ(c, x, y + s * 1.8f, s * 0.14f, gold, 255f)
    heart(c, x, y + s * 0.05f, s * 0.32f, 0f, gold, 255f)
}

/** Flor com cabeça de coração e uma folhinha. */
private fun SceneKit.kwHeartFlower(c: Canvas, x: Float, base: Float, hgt: Float, t: Float, ph: Float, color: Int) {
    val green = kc("#6FBF73")
    val sway = sin(t * 1.1f + ph) * hgt * 0.12f
    path.reset()
    path.moveTo(x, base)
    path.quadTo(x + sway * 0.2f, base - hgt * 0.5f, x + sway, base - hgt)
    p.style = Paint.Style.STROKE
    p.strokeCap = Paint.Cap.ROUND
    p.strokeWidth = 1.6f * u
    p.color = al(green, 255f)
    c.drawPath(path, p)
    leaf(c, x + 3.5f * u + sway * 0.3f, base - hgt * 0.42f, 4.4f * u, 55f, 1f, green, kc("#4C9A56"), 255f)
    heart(c, x + sway, base - hgt - 3.4f * u, 5.2f * u, sway * 2.2f, color, 255f)
}

/** Coelhinho sentado, com orelhas mexendo, piscadinha e bochechas. (x,y) é o chão sob ele. */
private fun SceneKit.kwBunny(c: Canvas, x: Float, y: Float, s: Float, t: Float, fur: Int, inner: Int) {
    val ink = kc("#5A2340")
    val pinkN = kc("#FF8FB0")
    val ear = sin(t * 2.2f) * 4f
    // rabinho
    circ(c, x - s * 0.98f, y - s * 0.58f, s * 0.3f, Color.WHITE, 255f)
    // corpo
    oval(c, x - s * 0.98f, y - s * 1.22f, x + s * 0.72f, y, fur, 255f)
    oval(c, x - s * 0.2f, y - s * 0.62f, x + s * 0.55f, y - s * 0.04f, Color.WHITE, 110f)
    // orelhas
    for (e in 0..1) {
        c.save()
        c.translate(x + s * (0.12f + 0.5f * e), y - s * 1.9f)
        c.rotate(-8f + e * 18f + ear * (if (e == 0) 1f else -1f))
        oval(c, -s * 0.18f, -s * 0.9f, s * 0.18f, s * 0.12f, fur, 255f)
        oval(c, -s * 0.09f, -s * 0.72f, s * 0.09f, s * 0.02f, inner, 255f)
        c.restore()
    }
    // cabeça
    circ(c, x + s * 0.36f, y - s * 1.36f, s * 0.74f, fur, 255f)
    // olhinhos (piscam de vez em quando)
    val blink = if (frac(t / 3.4f) > 0.93f) 0.15f else 1f
    val ey = y - s * 1.42f
    oval(c, x + s * 0.12f, ey - s * 0.1f * blink, x + s * 0.3f, ey + s * 0.1f * blink, ink, 255f)
    oval(c, x + s * 0.54f, ey - s * 0.1f * blink, x + s * 0.72f, ey + s * 0.1f * blink, ink, 255f)
    if (blink > 0.5f) {
        circ(c, x + s * 0.19f, ey - s * 0.04f, s * 0.035f, Color.WHITE, 255f)
        circ(c, x + s * 0.61f, ey - s * 0.04f, s * 0.035f, Color.WHITE, 255f)
    }
    circ(c, x + s * 0.0f, y - s * 1.22f, s * 0.15f, pinkN, 190f)
    circ(c, x + s * 0.78f, y - s * 1.22f, s * 0.15f, pinkN, 190f)
    circ(c, x + s * 0.42f, y - s * 1.3f, s * 0.07f, pinkN, 255f)
    p.style = Paint.Style.STROKE
    p.strokeCap = Paint.Cap.ROUND
    p.strokeWidth = max(0.8f, s * 0.045f)
    p.color = al(ink, 230f)
    rect.set(x + s * 0.3f, y - s * 1.3f, x + s * 0.42f, y - s * 1.18f)
    c.drawArc(rect, 20f, 140f, false, p)
    rect.set(x + s * 0.42f, y - s * 1.3f, x + s * 0.54f, y - s * 1.18f)
    c.drawArc(rect, 20f, 140f, false, p)
    // patinha da frente
    oval(c, x + s * 0.2f, y - s * 0.34f, x + s * 0.62f, y + s * 0.02f, Color.WHITE, 255f)
}

class RomanceScene : AuraScene {
    private var kit: SceneKit? = null
    private var bubbles: List<SceneKit.Mote> = emptyList()
    private var petals: List<SceneKit.Mote> = emptyList()
    private var hearts: List<SceneKit.Mote> = emptyList()
    private var sparks: List<SceneKit.Mote> = emptyList()
    private val bt = floatArrayOf(0.18f, 0.32f, 0.46f, 0.58f, 0.7f, 0.84f, 0.97f)

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        bubbles = k.motes(9, 111L, 0.02f, 0.04f, 7f, 17f, 14f)
        petals = k.motes(15, 112L, 0.03f, 0.055f, 7f, 13f, 28f)
        hearts = k.motes(6, 113L, 0.026f, 0.045f, 5f, 9f, 14f)
        sparks = k.motes(22, 114L, 0.25f, 0.6f, 3f, 6f, 0f)
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        k.tidy()
        val u = k.u
        val p = k.p
        val rose = kc("#FF6B9D")
        val pink = kc("#FF9CC0")
        val deep = kc("#E0457F")
        val blush = kc("#FFD3E4")
        val gold = kc("#FFE2A8")
        val bark = kc("#8A3A5C")
        val barkHi = kc("#B8648A")
        val lilac = kc("#D9A6E8")
        val ink = kc("#6B2D4E")
        val cloudC = kc("#FFF4F8")
        val cloudShade = kc("#E9B6DA")

        // céu de algodão-doce
        k.vgrad(c, w, h, kc("#FFE8F0"), kc("#FFB9D3"), kc("#D98BD0"))
        val beat = k.pulse(t, 2.6f)
        k.soft(c, w / 2f, h * 0.42f, max(w, h) * 0.7f, Color.WHITE, 90f + 24f * beat)

        // lua dorminhoca com halo
        val mx = w * 0.8f
        val my = h * 0.15f + sin(t * 0.4f) * 2f * u
        k.soft(c, mx, my, 58f * u, Color.WHITE, 120f + 30f * beat)
        k.circ(c, mx, my, 22f * u, kc("#FFF9FC"), 255f)
        k.circ(c, mx + 9f * u, my - 9f * u, 3.2f * u, kc("#FFE3EE"), 255f)
        k.circ(c, mx - 12f * u, my + 8f * u, 2.4f * u, kc("#FFE3EE"), 255f)
        k.kwSleepy(c, mx, my + 1f * u, 15f * u, ink, kc("#FF9CC0"))

        // nuvenzinhas à deriva (a primeira está cochilando)
        for (i in 0..2) {
            val sp = (6f + 3f * i) * u
            val cwid = (34f + 8f * i) * u
            val span = w + cwid * 4f
            val x = ((w * (0.2f + 0.37f * i) + t * sp) % span) - cwid * 2f
            val y = h * (0.2f + 0.27f * i) + sin(t * 0.5f + i) * 3f * u
            k.kwCloud(c, x, y, cwid, cloudC, cloudShade)
            if (i == 0) k.kwSleepy(c, x, y + cwid * 0.14f, cwid * 0.32f, ink, kc("#FF9CC0"))
        }

        // ondinhas saindo da capa
        k.posterRipple(c, poster, hasPoster, t, 3.4f, Color.WHITE, 150f)

        // fio de luzinhas com lâmpadas e corações
        val sag = 22f * u
        k.path.reset()
        for (i in 0..24) {
            val f = i / 24f
            val x = w * f
            val y = 5f * u + sag * 4f * f * (1f - f)
            if (i == 0) k.path.moveTo(x, y) else k.path.lineTo(x, y)
        }
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.strokeWidth = 1.1f * u
        p.color = k.al(kc("#C06A92"), 230f)
        c.drawPath(k.path, p)
        val nb = 9
        for (i in 0 until nb) {
            val f = (i + 0.5f) / nb
            val x = w * f
            val y = 5f * u + sag * 4f * f * (1f - f)
            val glow = 0.5f + 0.5f * sin(t * 1.6f - i * 0.8f)
            val bc = if (i % 3 == 0) gold else if (i % 3 == 1) Color.WHITE else pink
            k.line(c, x, y, x, y + 4f * u, 1f * u, kc("#C06A92"), 255f)
            k.soft(c, x, y + 9f * u, 13f * u, bc, 70f + 110f * glow)
            if (i % 2 == 0) k.heart(c, x, y + 9f * u, 3.8f * u, 0f, bc, 255f)
            else k.circ(c, x, y + 8.5f * u, 3f * u, bc, 255f)
        }

        // galhos de cerejeira nos cantos de cima
        for (side in 0..1) {
            val sg = if (side == 0) 1f else -1f
            val ox = if (side == 0) 0f else w
            val sway = sin(t * 0.5f + side * 1.7f) * 2f * u
            val x0 = ox - sg * 6f * u
            val y0 = 2f * u
            val x1 = ox + sg * w * 0.17f
            val y1 = 34f * u + sway
            val x2 = ox + sg * w * 0.42f
            val y2 = 20f * u + sway * 1.4f
            k.path.reset()
            k.path.moveTo(x0, y0)
            k.path.quadTo(x1, y1, x2, y2)
            p.style = Paint.Style.STROKE
            p.strokeCap = Paint.Cap.ROUND
            p.strokeWidth = 5.2f * u
            p.color = k.al(bark, 255f)
            c.drawPath(k.path, p)
            k.path.reset()
            k.path.moveTo(x0, y0 - 1.3f * u)
            k.path.quadTo(x1, y1 - 1.3f * u, x2, y2 - 1.3f * u)
            p.strokeWidth = 1.5f * u
            p.color = k.al(barkHi, 160f)
            c.drawPath(k.path, p)
            // raminho
            val tx = k.qb(x0, x1, x2, 0.45f)
            val ty = k.qb(y0, y1, y2, 0.45f)
            k.path.reset()
            k.path.moveTo(tx, ty)
            k.path.quadTo(tx + sg * 8f * u, ty + 14f * u, tx + sg * 4f * u, ty + 26f * u + sway)
            p.strokeWidth = 2.6f * u
            p.color = k.al(bark, 255f)
            c.drawPath(k.path, p)
            // flores e botões
            for (i in bt.indices) {
                val bx = k.qb(x0, x1, x2, bt[i])
                val by = k.qb(y0, y1, y2, bt[i]) + (if (i % 2 == 0) -5f else 6f) * u
                val rot = 14f * sin(t * 0.6f + i * 1.3f + side)
                val sz = (10f + 3f * (i % 3)) * u
                k.blossom(c, bx, by, sz, rot + i * 20f, if (i % 2 == 0) blush else Color.WHITE, deep, 255f)
                if (i % 3 == 0) k.circ(c, bx + sg * 7f * u, by + 7f * u, 2.6f * u, rose, 255f)
            }
            k.blossom(c, tx + sg * 4f * u, ty + 26f * u + sway, 9f * u, 20f, blush, deep, 255f)

            if (side == 0) {
                // casalzinho de passarinhos: de vez em quando se aproximam e sai um coração
                val q = k.frac(t / 5.2f)
                val lean = k.ss((q - 0.52f) / 0.1f) * (1f - k.ss((q - 0.74f) / 0.1f))
                val sb = 8f * u
                val ax = k.qb(x0, x1, x2, 0.6f)
                val ay = k.qb(y0, y1, y2, 0.6f) - 1.25f * sb - 2.6f * u
                val bx = k.qb(x0, x1, x2, 0.82f)
                val by = k.qb(y0, y1, y2, 0.82f) - 1.25f * sb - 2.6f * u
                k.kwBird(c, ax, ay, sb, 1f, kc("#FFFFFF"), kc("#FFC6DA"), kc("#FFE9F1"), lean, t)
                k.kwBird(c, bx, by, sb, -1f, kc("#FFE9A8"), kc("#FFC857"), kc("#FFF6D6"), lean, t)
                val life = ((q - 0.58f) / 0.32f).coerceIn(0f, 1f)
                if (life > 0f && life < 1f) {
                    val hx = (ax + bx) / 2f
                    val hy = min(ay, by) - 8f * u - 22f * u * life
                    k.heart(c, hx, hy, (4f + 4f * sin(life * 3.1416f)) * u, 0f, deep, 255f * sin(life * 3.1416f))
                }
            } else {
                // lanterna de papel balançando
                val lx0 = k.qb(x0, x1, x2, 0.6f)
                val ly0 = k.qb(y0, y1, y2, 0.6f)
                val ang = sin(t * 0.8f) * 5f * 0.01745f
                val len = 30f * u
                val lx = lx0 + sin(ang) * len
                val ly = ly0 + cos(ang) * len + 8f * u
                k.line(c, lx0, ly0, lx, ly - 9f * u, 1.1f * u, gold, 255f)
                k.kwLantern(c, lx, ly, 9.5f * u, kc("#FF7DA7"), kc("#FFB3CC"), kc("#FFD36B"))
            }
        }

        // pétalas caindo e virando no ar
        for (i in petals.indices) {
            val m = petals[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (-0.08f + 1.16f * life)
            val x = w * (m.x + 0.1f * life) + sin(t * 0.7f + m.ph * k.twoPi) * m.sw * u
            val a = k.fade(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            val flip = abs(sin(t * 1.1f + m.ph * 7f))
            val pc = if (i % 3 == 0) Color.WHITE else if (i % 3 == 1) blush else pink
            k.petal(c, x, y, m.sz * u, t * 28f * (if (i % 2 == 0) 1f else -1f) + m.ph * 360f, flip, pc, 245f * a)
        }

        // bolhas de sabão subindo (algumas guardam um coraçãozinho)
        for (i in bubbles.indices) {
            val m = bubbles[i]
            val life = k.frac(m.y + t * m.sp)
            val y = h * (1.1f - 1.2f * life)
            val x = w * m.x + sin(t * 0.6f + m.ph * k.twoPi) * m.sw * u
            val a = k.fade(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            val r = m.sz * u
            k.kwBubble(c, x, y, r, if (i % 2 == 0) pink else lilac, 255f * a)
            if (i % 3 == 0) k.heart(c, x, y, r * 0.36f, 0f, rose, 210f * a)
        }

        // coraçõezinhos subindo
        for (i in hearts.indices) {
            val m = hearts[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1.08f - 1.16f * life)
            val x = w * m.x + sin(t * 0.8f + m.ph * k.twoPi) * m.sw * u
            val a = k.fade(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            k.heart(c, x, y, m.sz * u * (1f + 0.08f * beat) * 0.6f, sin(t * 0.9f + m.ph * 6f) * 12f, if (i % 2 == 0) Color.WHITE else deep, 235f * a)
        }

        // carta de amor com asinhas
        val ex = w * 0.13f + sin(t * 0.45f) * 7f * u
        val ey = h * 0.6f + sin(t * 0.7f) * 5f * u
        k.soft(c, ex, ey, 32f * u, Color.WHITE, 80f)
        val flap = sin(t * 7f)
        for (sd in intArrayOf(-1, 1)) {
            c.save()
            c.translate(ex + sd * 12f * u, ey - 3f * u)
            c.rotate(sd * (-22f + 24f * flap))
            k.oval(c, 0f, -7f * u, sd * 17f * u, 4f * u, Color.WHITE, 240f)
            k.oval(c, sd * 3f * u, -4f * u, sd * 14f * u, 2f * u, blush, 200f)
            c.restore()
        }
        c.save()
        c.translate(ex, ey)
        c.rotate(sin(t * 0.5f) * 8f - 6f)
        k.rrect(c, -14f * u, -9.5f * u, 14f * u, 9.5f * u, 2.5f * u, kc("#FFF6F9"), 255f)
        k.path.reset()
        k.path.moveTo(-14f * u, -9.5f * u)
        k.path.lineTo(0f, 2f * u)
        k.path.lineTo(14f * u, -9.5f * u)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.3f * u
        p.strokeJoin = Paint.Join.ROUND
        p.color = k.al(kc("#E58AAE"), 255f)
        c.drawPath(k.path, p)
        k.heart(c, 0f, 2.4f * u, 4.4f * u, 0f, kc("#E0305F"), 255f)
        c.restore()

        // colinas de algodão com florzinhas, e um coelhinho
        val backTop = h - 46f * u
        val frontTop = h - 27f * u
        k.kwHill(c, w, h, backTop, 5f * u, 0.8f, kc("#F8B2D0"))
        val fxs = floatArrayOf(0.07f, 0.2f, 0.33f, 0.47f, 0.6f, 0.95f)
        val fcs = intArrayOf(kc("#FF6B9D"), Color.WHITE, kc("#FFD36B"), kc("#FF6B9D"), Color.WHITE, kc("#FFD36B"))
        for (i in fxs.indices) {
            val fx = w * fxs[i]
            k.kwHeartFlower(c, fx, kwHillY(w, backTop, 5f * u, 0.8f, fx) + 3f * u, (15f + 6f * k.hash(i + 3)) * u, t, i * 1.3f, fcs[i])
        }
        k.kwHill(c, w, h, frontTop, 4f * u, 2.2f, kc("#F08BB9"))
        for (i in 0 until 7) {
            val fx = w * (0.04f + 0.15f * i) + 6f * u * k.hash(i + 11)
            val fy = kwHillY(w, frontTop, 4f * u, 2.2f, fx) + 4f * u
            k.circ(c, fx, fy, 1.6f * u, if (i % 2 == 0) Color.WHITE else gold, 255f)
            k.circ(c, fx + 5f * u, fy + 2f * u, 1.2f * u, if (i % 2 == 0) gold else Color.WHITE, 255f)
        }
        val bxx = w * 0.8f
        k.kwBunny(c, bxx, kwHillY(w, frontTop, 4f * u, 2.2f, bxx) + 6f * u, 11f * u, t, kc("#FFF1F6"), kc("#FFB3CC"))

        // brilhos dourados
        for (m in sparks) {
            val y = h * m.y
            val a = k.tw(t, m.ph, m.sp * 2f) * k.edge(y, h)
            if (a <= 0.03f) continue
            k.sparkle(c, w * m.x, y, m.sz * u * (0.55f + 0.5f * a), t * 12f + m.ph * 90f, gold, 235f * a)
        }
        k.tidy()
    }
}

// --------------------------------------------------------------------------------------
// Peças da Comédia
// --------------------------------------------------------------------------------------

/** Pipoca fofinha. */
private fun SceneKit.kwKernel(c: Canvas, x: Float, y: Float, r: Float, a: Float) {
    val cream = kc("#FFF6DC")
    circ(c, x, y, r, cream, a)
    circ(c, x - r * 0.62f, y + r * 0.3f, r * 0.62f, cream, a)
    circ(c, x + r * 0.62f, y + r * 0.22f, r * 0.62f, cream, a)
    circ(c, x + r * 0.05f, y - r * 0.55f, r * 0.6f, cream, a)
    circ(c, x + r * 0.2f, y - r * 0.05f, r * 0.3f, kc("#FFD86B"), a)
}

/** Balde de pipoca listrado com carinha, pipocas estourando. (x,y) é o meio da base; s é o tamanho de 1 "unidade". */
private fun SceneKit.kwBucket(c: Canvas, x: Float, y: Float, s: Float, t: Float, red: Int) {
    val topY = y - 34f * s
    val ink = kc("#6B2E12")
    // monte de pipoca
    for (i in 0..6) {
        val px = x + (i - 3) * 5.6f * s
        val py = topY - 3f * s - (i % 2) * 3.2f * s - hash(i + 90) * 2f * s
        kwKernel(c, px, py, 6.2f * s, 255f)
    }
    // pipocas pulando
    for (i in 0..3) {
        val q = frac(t * 0.55f + i * 0.25f)
        val arc = 4f * q * (1f - q)
        val ky = topY - 8f * s - arc * (30f + 9f * (i % 2)) * s
        val kx = x + (i - 1.5f) * 7f * s + (q - 0.5f) * 12f * s * (if (i % 2 == 0) 1f else -1f)
        val a = ss(q / 0.12f) * ss((1f - q) / 0.2f)
        if (a > 0.02f) {
            c.save()
            c.translate(kx, ky)
            c.rotate(q * 300f * (if (i % 2 == 0) 1f else -1f))
            kwKernel(c, 0f, 0f, 4.6f * s, 255f * a)
            c.restore()
        }
    }
    // corpo do balde
    path.reset()
    path.moveTo(x - 17f * s, topY)
    path.lineTo(x + 17f * s, topY)
    path.lineTo(x + 12f * s, y)
    path.lineTo(x - 12f * s, y)
    path.close()
    p.style = Paint.Style.FILL
    p.color = al(Color.WHITE, 255f)
    c.drawPath(path, p)
    for (j in intArrayOf(0, 2, 4)) {
        path.reset()
        path.moveTo(x - 17f * s + j * 6.8f * s, topY)
        path.lineTo(x - 17f * s + (j + 1) * 6.8f * s, topY)
        path.lineTo(x - 12f * s + (j + 1) * 4.8f * s, y)
        path.lineTo(x - 12f * s + j * 4.8f * s, y)
        path.close()
        p.color = al(red, 255f)
        c.drawPath(path, p)
    }
    // etiqueta com carinha
    rrect(c, x - 9f * s, y - 27f * s, x + 9f * s, y - 9f * s, 5f * s, Color.WHITE, 255f)
    circ(c, x - 3.6f * s, y - 19f * s, 1.15f * s, ink, 255f)
    circ(c, x + 3.6f * s, y - 19f * s, 1.15f * s, ink, 255f)
    circ(c, x - 6.2f * s, y - 16.2f * s, 1.6f * s, kc("#FF8FA3"), 190f)
    circ(c, x + 6.2f * s, y - 16.2f * s, 1.6f * s, kc("#FF8FA3"), 190f)
    p.style = Paint.Style.STROKE
    p.strokeCap = Paint.Cap.ROUND
    p.strokeWidth = max(0.8f, 0.9f * s)
    p.color = al(ink, 255f)
    rect.set(x - 2.4f * s, y - 19f * s, x + 2.4f * s, y - 14.6f * s)
    c.drawArc(rect, 20f, 140f, false, p)
    // aro de cima
    rrect(c, x - 19f * s, topY - 3f * s, x + 19f * s, topY + 3f * s, 3f * s, red, 255f)
    line(c, x - 15f * s, topY - 0.8f * s, x + 15f * s, topY - 0.8f * s, 1f * s, Color.WHITE, 120f)
}

/** Chapéu de festa com bolinhas e pompom. */
private fun SceneKit.kwHat(c: Canvas, x: Float, y: Float, s: Float, rot: Float, color: Int, dot: Int) {
    c.save()
    c.translate(x, y)
    c.rotate(rot)
    path.reset()
    path.moveTo(0f, -s * 1.6f)
    path.lineTo(s * 0.82f, s)
    path.quadTo(0f, s * 1.25f, -s * 0.82f, s)
    path.close()
    p.style = Paint.Style.FILL
    p.color = al(color, 255f)
    c.drawPath(path, p)
    circ(c, 0f, -s * 0.35f, s * 0.12f, dot, 255f)
    circ(c, -s * 0.3f, s * 0.25f, s * 0.13f, dot, 255f)
    circ(c, s * 0.3f, s * 0.35f, s * 0.13f, dot, 255f)
    circ(c, 0f, s * 0.62f, s * 0.12f, dot, 255f)
    oval(c, -s * 0.3f, -s * 0.9f, -s * 0.12f, -s * 0.1f, Color.WHITE, 90f)
    rrect(c, -s * 0.88f, s * 0.86f, s * 0.88f, s * 1.14f, s * 0.12f, kc("#FFD36B"), 255f)
    circ(c, 0f, -s * 1.66f, s * 0.3f, kc("#FFE066"), 255f)
    circ(c, -s * 0.08f, -s * 1.74f, s * 0.1f, Color.WHITE, 200f)
    c.restore()
}

/** Pirulito de espiral girando. */
private fun SceneKit.kwLolli(c: Canvas, x: Float, y: Float, r: Float, rot: Float, t: Float, col1: Int, col2: Int) {
    c.save()
    c.translate(x, y)
    c.rotate(rot)
    line(c, 0f, r * 0.8f, 0f, r * 3.3f, r * 0.16f, Color.WHITE, 255f)
    circ(c, 0f, 0f, r, col1, 255f)
    path.reset()
    val spin = t * 0.7f
    for (i in 0..44) {
        val f = i / 44f
        val an = f * 3.6f * twoPi + spin
        val rr = r * 0.86f * f
        val px = cos(an) * rr
        val py = sin(an) * rr
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    p.style = Paint.Style.STROKE
    p.strokeCap = Paint.Cap.ROUND
    p.strokeJoin = Paint.Join.ROUND
    p.strokeWidth = r * 0.2f
    p.color = al(col2, 255f)
    c.drawPath(path, p)
    oval(c, -r * 0.62f, -r * 0.74f, -r * 0.2f, -r * 0.46f, Color.WHITE, 150f)
    circ(c, -r * 0.34f, r * 1.3f, r * 0.2f, col2, 255f)
    circ(c, r * 0.34f, r * 1.3f, r * 0.2f, col2, 255f)
    circ(c, 0f, r * 1.3f, r * 0.14f, col1, 255f)
    c.restore()
}

/** Cortina de teatro presa de um lado (desenhada no canto esquerdo; o direito é espelhado). */
private fun SceneKit.kwCurtain(c: Canvas, h: Float, t: Float, red: Int, dark: Int, light: Int, gold: Int) {
    path.reset()
    path.moveTo(0f, -4f * u)
    path.lineTo(38f * u, -4f * u)
    path.cubicTo(32f * u, h * 0.22f, 8f * u, h * 0.36f, 13f * u, h * 0.5f)
    path.cubicTo(18f * u, h * 0.64f, 36f * u, h * 0.86f, 31f * u, h)
    path.lineTo(0f, h)
    path.close()
    p.style = Paint.Style.FILL
    p.color = al(red, 255f)
    c.drawPath(path, p)
    c.save()
    c.clipPath(path)
    for (i in 0..5) {
        val fx = (4f + i * 6.5f) * u
        path.reset()
        path.moveTo(fx, 0f)
        path.quadTo(fx + sin(t * 0.8f + i) * 2f * u + (if (i % 2 == 0) -3f else 3f) * u, h * 0.5f, fx + 5f * u, h)
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = 3.2f * u
        p.color = al(if (i % 2 == 0) dark else light, if (i % 2 == 0) 80f else 70f)
        c.drawPath(path, p)
    }
    c.restore()
    // laço dourado com borla
    val ty = h * 0.5f
    line(c, -2f * u, ty, 15f * u, ty, 3.2f * u, gold, 255f)
    line(c, -2f * u, ty - 1f * u, 15f * u, ty - 1f * u, 1f * u, Color.WHITE, 140f)
    circ(c, 15f * u, ty, 3f * u, gold, 255f)
    line(c, 15f * u, ty + 2f * u, 15f * u, ty + 11f * u, 1.2f * u, gold, 255f)
    rrect(c, 12.5f * u, ty + 9f * u, 17.5f * u, ty + 16f * u, 1.6f * u, gold, 255f)
}

/** Balãozinho de fala que "pula" na tela. */
private fun SceneKit.kwSpeech(c: Canvas, x: Float, y: Float, sc: Float, rot: Float, word: String, ink: Int, tailDir: Float) {
    if (sc <= 0.03f) return
    c.save()
    c.translate(x, y)
    c.rotate(rot)
    c.scale(sc, sc)
    rrect(c, -24f * u + 1.5f * u, -12f * u + 2f * u, 24f * u + 1.5f * u, 12f * u + 2f * u, 12f * u, ink, 50f)
    rrect(c, -24f * u, -12f * u, 24f * u, 12f * u, 12f * u, Color.WHITE, 255f)
    path.reset()
    path.moveTo(-6f * u * tailDir, 10f * u)
    path.lineTo(-13f * u * tailDir, 20f * u)
    path.lineTo(3f * u * tailDir, 11f * u)
    path.close()
    p.style = Paint.Style.FILL
    p.color = al(Color.WHITE, 255f)
    c.drawPath(path, p)
    label(c, word, 0f, 0f, 12.5f * u, ink, 255f, 0f, 1f)
    // tracinhos de animação
    line(c, -30f * u, -14f * u, -26f * u, -10f * u, 1.6f * u, ink, 220f)
    line(c, 30f * u, -14f * u, 26f * u, -10f * u, 1.6f * u, ink, 220f)
    c.restore()
}

class ComediaScene : AuraScene {
    private var kit: SceneKit? = null
    private var confetti: List<SceneKit.Mote> = emptyList()
    private var floaters: List<SceneKit.Mote> = emptyList()
    private var sparks: List<SceneKit.Mote> = emptyList()
    private var pal = IntArray(0)

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        confetti = k.motes(26, 521L, 0.05f, 0.1f, 3.5f, 6.5f, 20f)
        floaters = k.motes(5, 522L, 0.03f, 0.05f, 11f, 15f, 14f)
        sparks = k.motes(16, 523L, 0.3f, 0.8f, 3f, 6f, 0f)
        pal = intArrayOf(kc("#FF4D6D"), kc("#FFD84A"), kc("#2EC4B6"), kc("#4D96FF"), kc("#B26CFF"), kc("#FF9F43"))
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        k.tidy()
        val u = k.u
        val p = k.p
        val red = kc("#E8445A")
        val redDark = kc("#B52A42")
        val redLight = kc("#FF7A8E")
        val gold = kc("#F2C063")
        val cream = kc("#FFF6DC")
        val ink = kc("#FF5C8A")

        k.vgrad(c, w, h, kc("#FFE680"), kc("#FFB27A"), kc("#FF86A8"))

        // raios de sol girando devagar
        val cx = w / 2f
        val cy = h * 0.42f
        val len = max(w, h) * 1.2f
        k.path.reset()
        val rays = 14
        val base = t * 0.05f
        for (i in 0 until rays) {
            val a1 = base + i * k.twoPi / rays
            val a2 = a1 + k.twoPi / rays * 0.5f
            k.path.moveTo(cx, cy)
            k.path.lineTo(cx + cos(a1) * len, cy + sin(a1) * len)
            k.path.lineTo(cx + cos(a2) * len, cy + sin(a2) * len)
            k.path.close()
        }
        p.style = Paint.Style.FILL
        p.color = k.al(Color.WHITE, 40f)
        c.drawPath(k.path, p)
        k.soft(c, cx, cy, max(w, h) * 0.6f, Color.WHITE, 90f)

        // bolinhas deslizando
        val gap = 34f * u
        val cols = (w / gap).toInt() + 2
        val rows = (h / gap).toInt() + 2
        for (gy in 0 until rows) {
            for (gx in 0 until cols) {
                val x = ((gx * gap + t * 5f * u + (gy % 2) * gap * 0.5f) % (cols * gap)) - gap * 0.5f
                val y = gy * gap - ((t * 3f * u) % gap)
                k.circ(c, x, y, 5f * u, Color.WHITE, 34f)
            }
        }

        k.posterRipple(c, poster, hasPoster, t, 3.0f, Color.WHITE, 140f)

        // varal de bandeirinhas
        val sag = 30f * u
        k.path.reset()
        for (i in 0..28) {
            val f = i / 28f
            val x = w * f
            val y = 8f * u + sag * 4f * f * (1f - f)
            if (i == 0) k.path.moveTo(x, y) else k.path.lineTo(x, y)
        }
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.strokeWidth = 1.3f * u
        p.color = k.al(cream, 255f)
        c.drawPath(k.path, p)
        val nf = 11
        for (i in 0 until nf) {
            val f = (i + 0.5f) / nf
            val x = w * f
            val y = 8f * u + sag * 4f * f * (1f - f)
            c.save()
            c.translate(x, y)
            c.rotate(sin(t * 1.6f + i) * 6f)
            k.path.reset()
            k.path.moveTo(-8f * u, 0f)
            k.path.lineTo(8f * u, 0f)
            k.path.lineTo(0f, 17f * u)
            k.path.close()
            p.style = Paint.Style.FILL
            p.color = k.al(pal[i % pal.size], 255f)
            c.drawPath(k.path, p)
            k.circ(c, 0f, 5.5f * u, 1.7f * u, Color.WHITE, 210f)
            c.restore()
        }

        // holofotes varrendo
        for (s in 0..1) {
            val sg = if (s == 0) 1f else -1f
            val sx = if (s == 0) 0f else w
            val ang = (90f + sg * (28f + 24f * sin(t * 0.55f + s * 1.7f))) * 0.0174533f
            val ln = h * 1.5f
            val half = 0.16f
            k.path.reset()
            k.path.moveTo(sx, -4f * u)
            k.path.lineTo(sx + cos(ang - half) * ln, -4f * u + sin(ang - half) * ln)
            k.path.lineTo(sx + cos(ang + half) * ln, -4f * u + sin(ang + half) * ln)
            k.path.close()
            p.style = Paint.Style.FILL
            p.color = k.al(kc("#FFFBE0"), 34f)
            c.drawPath(k.path, p)
        }

        // balões subindo
        for (i in floaters.indices) {
            val m = floaters[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1.2f - 1.45f * life)
            val x = w * m.x + sin(t * 0.8f + m.ph * 6.28f) * m.sw * u
            val r = m.sz * u
            val col = pal[i % pal.size]
            k.path.reset()
            k.path.moveTo(x, y + r * 1.2f)
            for (j in 1..6) {
                k.path.lineTo(x + sin(t * 2f + j * 0.9f + i) * 2.2f * u * j / 6f, y + r * 1.2f + j * 5f * u)
            }
            p.style = Paint.Style.STROKE
            p.strokeWidth = 0.9f * u
            p.strokeCap = Paint.Cap.ROUND
            p.color = k.al(Color.WHITE, 190f)
            c.drawPath(k.path, p)
            k.kwBalloon(c, x, y, r, sin(t * 1.2f + i) * 5f, col)
        }

        // cortinas de teatro nos dois lados
        k.kwCurtain(c, h, t, red, redDark, redLight, gold)
        c.save()
        c.translate(w, 0f)
        c.scale(-1f, 1f)
        k.kwCurtain(c, h, t, red, redDark, redLight, gold)
        c.restore()

        // cacho de balões amarrado no canto de baixo
        val ax = w * 0.15f
        val ay = h * 0.9f
        val offX = floatArrayOf(-13f, 3f, 17f)
        val offY = floatArrayOf(66f, 84f, 58f)
        val bc = intArrayOf(kc("#FF4D6D"), kc("#4D96FF"), kc("#FFD84A"))
        for (i in 0..2) {
            val r = (15f - i * 1.4f) * u
            val bx = ax + offX[i] * u + sin(t * 0.9f + i) * 3f * u
            val by = ay - offY[i] * u + sin(t * 1.2f + i * 1.7f) * 3f * u
            k.path.reset()
            k.path.moveTo(ax, ay)
            k.path.quadTo((ax + bx) / 2f + sin(t * 1.4f + i) * 3f * u, (ay + by) / 2f, bx, by + r * 1.2f)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 0.9f * u
            p.strokeCap = Paint.Cap.ROUND
            p.color = k.al(Color.WHITE, 200f)
            c.drawPath(k.path, p)
            k.kwBalloon(c, bx, by, r, sin(t * 1.1f + i) * 6f, bc[i])
            if (i == 0) k.heart(c, bx, by - r * 0.05f, r * 0.34f, 0f, Color.WHITE, 235f)
        }
        k.circ(c, ax, ay, 2.2f * u, kc("#FF4D6D"), 255f)
        k.oval(c, ax - 6f * u, ay - 2f * u, ax - 1f * u, ay + 2f * u, kc("#FF4D6D"), 255f)
        k.oval(c, ax + 1f * u, ay - 2f * u, ax + 6f * u, ay + 2f * u, kc("#FF4D6D"), 255f)

        // chapéu de festa pulando e pirulito girando
        val hop = abs(sin(t * 2.2f)) * 6f * u
        k.kwHat(c, w * 0.15f, h * 0.36f - hop, 11f * u, -14f + sin(t * 2.2f) * 4f, kc("#B26CFF"), Color.WHITE)
        k.kwLolli(c, w * 0.88f, h * 0.3f + sin(t * 0.9f) * 3f * u, 12f * u, 18f, t, kc("#FF7EB6"), Color.WHITE)

        // balde de pipoca estourando
        k.kwBucket(c, w * 0.85f, h * 0.9f, 1f * u, t, red)

        // balõezinhos de fala pulando
        val words = arrayOf("haha!", "kkkk", "rsrs")
        val bxs = floatArrayOf(0.28f, 0.74f, 0.7f)
        val bys = floatArrayOf(0.2f, 0.52f, 0.75f)
        for (i in 0 until 3) {
            val q = k.frac(t / 5.4f + i / 3f)
            val appear = k.ss(q / 0.1f)
            val vanish = 1f - k.ss((q - 0.42f) / 0.1f)
            val sc = appear * vanish * (1f + 0.18f * sin(appear * 3.1416f))
            k.kwSpeech(c, w * bxs[i], h * bys[i], sc, -8f + i * 8f + sin(t * 1.2f + i) * 3f, words[i], ink, if (i == 1) -1f else 1f)
        }

        // confete virando no ar
        for (i in confetti.indices) {
            val m = confetti[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (-0.06f + 1.12f * life)
            val x = w * m.x + sin(t * 0.9f + m.ph * 6.28f) * m.sw * u
            val a = k.fade(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            val s = m.sz * u
            val flip = cos(t * 3f + m.ph * 20f)
            c.save()
            c.translate(x, y)
            c.rotate(t * 60f * (if (i % 2 == 0) 1f else -1f) + m.ph * 360f)
            c.scale(1f, flip)
            p.style = Paint.Style.FILL
            p.color = k.al(pal[i % pal.size], 245f * a)
            if (i % 5 == 0) c.drawCircle(0f, 0f, s * 0.45f, p) else c.drawRect(-s, -s * 0.38f, s, s * 0.38f, p)
            c.restore()
        }

        // brilhinhos
        for (m in sparks) {
            val y = h * m.y
            val a = k.tw(t, m.ph, m.sp * 2f) * k.edge(y, h)
            if (a <= 0.03f) continue
            k.sparkle(c, w * m.x, y, m.sz * u * (0.55f + 0.5f * a), t * 14f + m.ph * 90f, Color.WHITE, 230f * a)
        }

        // cortininha de cima com franja dourada
        val n = max(8, (w / (22f * u)).toInt())
        val sw = w / n
        for (i in 0 until n) {
            val x = i * sw
            val depth = 11f * u + 1.2f * u * sin(t * 1.3f + i * 0.8f)
            val col = if (i % 2 == 0) red else cream
            k.rrect(c, x, -4f * u, x + sw + 0.5f, depth, 0f, col, 255f)
            k.circ(c, x + sw / 2f, depth, sw / 2f, col, 255f)
            k.circ(c, x + sw / 2f, depth + sw / 2f + 1.5f * u, 1.3f * u, gold, 255f)
        }
        k.line(c, 0f, 0f, w, 0f, 3f * u, gold, 255f)
        k.tidy()
    }
}
