package com.doramabloom.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Segunda leva de cenas por gênero: Comédia, Mistério, Drama, Musical, Esporte e Realeza. */

private fun text(c: Canvas, k: SceneKit, s: String, x: Float, y: Float, size: Float, color: Int, rot: Float) {
    val p = k.p
    p.style = Paint.Style.FILL
    p.typeface = Typeface.DEFAULT_BOLD
    p.textAlign = Paint.Align.CENTER
    p.textSize = size
    p.color = color
    c.save()
    c.translate(x, y)
    c.rotate(rot)
    c.drawText(s, 0f, size * 0.35f, p)
    c.restore()
    p.textAlign = Paint.Align.LEFT
    p.typeface = null
}

// ======================================================================================
// MISTÉRIO: noite de detetive. Lupa, quadro de pistas com barbante, cidade, interrogações.
// ======================================================================================
class MisterioScene : AuraScene {
    private var kit: SceneKit? = null
    private var marks: List<SceneKit.Mote> = emptyList()
    private var fog: List<SceneKit.Mote> = emptyList()
    private var pins: List<SceneKit.Mote> = emptyList()

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        marks = k.motes(9, 801L, 0.025f, 0.05f, 16f, 30f, 14f)
        fog = k.motes(6, 802L, 0.008f, 0.02f, 100f, 160f, 0f)
        pins = k.motes(6, 803L, 0f, 0f, 3f, 4f, 0f)
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        val u = k.u
        val p = k.p
        val teal = Color.parseColor("#5FD6C6")
        val red = Color.parseColor("#FF4F5E")
        val amber = Color.parseColor("#FFD27A")

        k.vgrad(c, w, h, Color.parseColor("#04161A"), Color.parseColor("#0C3F40"), Color.parseColor("#1F6B62"))

        // impressão digital gigante girando bem devagar
        val fx = w * 0.18f
        val fy = h * 0.3f
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.3f * u
        for (i in 1 until 9) {
            p.color = k.al(teal, 55f)
            k.rect.set(fx - i * 6f * u, fy - i * 7.5f * u, fx + i * 6f * u, fy + i * 7.5f * u)
            c.save()
            c.rotate(sin(t * 0.1f) * 6f, fx, fy)
            c.drawArc(k.rect, 200f + i * 9f, 250f - i * 6f, false, p)
            c.restore()
        }

        // quadro de pistas: pinos ligados por barbante vermelho que pulsa
        val px = FloatArray(pins.size)
        val py = FloatArray(pins.size)
        for (i in pins.indices) {
            px[i] = w * (0.08f + 0.84f * pins[i].x)
            py[i] = h * (0.12f + 0.6f * pins[i].y)
        }
        for (i in pins.indices) {
            val j = (i + 1) % pins.size
            p.style = Paint.Style.STROKE
            p.strokeWidth = (1.2f + 0.5f * sin(t * 2f + i)) * u
            p.color = k.al(red, 190f)
            c.drawLine(px[i], py[i], px[j], py[j], p)
        }
        for (i in pins.indices) {
            val a = k.edge(py[i], h)
            // foto polaroid pendurada
            c.save()
            c.translate(px[i], py[i])
            c.rotate(sin(t * 0.6f + i) * 6f + (i - 3) * 4f)
            p.style = Paint.Style.FILL
            p.color = k.al(Color.parseColor("#EFE8D2"), 220f * a)
            k.rect.set(-9f * u, 2f * u, 9f * u, 22f * u)
            c.drawRect(k.rect, p)
            p.color = k.al(Color.parseColor("#17323A"), 230f * a)
            k.rect.set(-7f * u, 4f * u, 7f * u, 15f * u)
            c.drawRect(k.rect, p)
            c.restore()
            p.style = Paint.Style.FILL
            p.color = k.al(red, 255f * a)
            c.drawCircle(px[i], py[i], 3.2f * u, p)
        }

        // interrogações, chaves e cadeados à deriva
        for (i in marks.indices) {
            val m = marks[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1.05f - 1.1f * life)
            val x = w * m.x + sin(t * 0.7f + m.ph * 8f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            val rot = sin(t + m.ph * 6f) * 14f
            if (i % 3 == 0) text(c, k, "?", x, y, m.sz * u * 1.4f, k.al(amber, 235f * a), rot)
            else if (i % 3 == 1) k.icon(c, "key", k.al(teal, 220f * a), x, y, m.sz * u, rot)
            else k.icon(c, "lock", k.al(Color.WHITE, 190f * a), x, y, m.sz * u * 0.9f, rot)
        }

        // lupa varrendo a tela com feixe de luz circular
        val lx = w * (0.5f + 0.38f * sin(t * 0.45f))
        val ly = h * (0.5f + 0.3f * sin(t * 0.31f + 1.3f))
        k.glow(c, lx, ly, 52f * u, amber, 90f)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f * u
        p.color = k.al(amber, 160f)
        c.drawCircle(lx, ly, 30f * u, p)
        k.icon(c, "search", k.al(Color.WHITE, 245f), lx, ly, 62f * u, 0f)

        // cidade em silhueta com janelas piscando
        val baseY = h - 10f * u
        for (i in 0 until 10) {
            val bw = w * 0.1f
            val bh = (24f + 20f * ((i * 7) % 4)) * u
            val bx = w * i * 0.1f
            p.style = Paint.Style.FILL
            p.color = k.al(Color.parseColor("#030D10"), 255f)
            k.rect.set(bx, baseY - bh, bx + bw - 2f * u, baseY + 6f * u)
            c.drawRect(k.rect, p)
            for (r in 0 until 3) {
                for (cc in 0 until 2) {
                    val on = sin(t * 0.3f + i * 3.7f + r * 1.9f + cc * 5.1f) > 0.3f
                    if (!on) continue
                    p.color = k.al(amber, 200f)
                    val wx = bx + 5f * u + cc * 10f * u
                    val wy = baseY - bh + 6f * u + r * 9f * u
                    c.drawRect(wx, wy, wx + 4f * u, wy + 5f * u, p)
                }
            }
        }

        // neblina rasteira
        for (m in fog) {
            val x = w * (k.frac(m.x + t * m.sp) * 1.5f - 0.25f)
            val y = h * (0.7f + 0.25f * m.y)
            val r = m.sz * u
            p.style = Paint.Style.FILL
            for (s in 0 until 4) {
                p.color = k.al(Color.parseColor("#BFEFE8"), 11f * (1f + s * 0.3f))
                k.rect.set(x - r * (1f - s * 0.18f), y - r * 0.16f, x + r * (1f - s * 0.18f), y + r * 0.16f)
                c.drawOval(k.rect, p)
            }
        }
    }
}

// ======================================================================================
// DRAMA: noite de chuva. Gotas, vidraça, nuvens pesadas, poças e um coração partido.
// ======================================================================================
class DramaScene : AuraScene {
    private var kit: SceneKit? = null
    private var rain: List<SceneKit.Mote> = emptyList()
    private var drops: List<SceneKit.Mote> = emptyList()
    private var tears: List<SceneKit.Mote> = emptyList()
    private var bokeh: List<SceneKit.Mote> = emptyList()

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        rain = k.motes(60, 901L, 1.1f, 1.8f, 0.6f, 1.4f, 0f)
        drops = k.motes(10, 902L, 0.04f, 0.09f, 3f, 6f, 6f)
        tears = k.motes(6, 903L, 0.05f, 0.09f, 12f, 18f, 12f)
        bokeh = k.motes(8, 904L, 0.01f, 0.02f, 16f, 34f, 0f)
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        val u = k.u
        val p = k.p
        val lilac = Color.parseColor("#C9A8F0")
        val blue = Color.parseColor("#9DB4FF")

        k.vgrad(c, w, h, Color.parseColor("#120D24"), Color.parseColor("#33244F"), Color.parseColor("#5C3F7E"))

        // luzes da rua desfocadas atrás do vidro
        for (i in bokeh.indices) {
            val m = bokeh[i]
            val x = w * m.x
            val y = h * (0.45f + 0.5f * m.y)
            val a = (0.6f + 0.4f * sin(t * 0.6f + m.ph * 9f)) * k.edge(y, h)
            val col = if (i % 3 == 0) Color.parseColor("#FFC27A") else if (i % 3 == 1) blue else Color.parseColor("#FF8FB8")
            p.style = Paint.Style.FILL
            p.color = k.al(col, 55f * a)
            c.drawCircle(x, y, m.sz * u, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 1f * u
            p.color = k.al(col, 90f * a)
            c.drawCircle(x, y, m.sz * u, p)
        }

        // nuvens pesadas passando
        for (i in 0 until 4) {
            val x = w * (1.2f - 1.5f * k.frac(t * 0.012f + i * 0.25f))
            val y = h * (0.04f + 0.1f * ((i * 3) % 4)) + 2f * u * sin(t * 0.5f + i)
            k.icon(c, "cloud", k.al(Color.parseColor("#1B1330"), 235f), x, y, (80f + 20f * (i % 2)) * u, 0f)
            k.icon(c, "cloud", k.al(lilac, 40f), x - 4f * u, y - 3f * u, (70f + 20f * (i % 2)) * u, 0f)
        }

        // chuva fina inclinada
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        for (i in rain.indices) {
            val m = rain[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (-0.1f + 1.15f * life)
            val x = w * m.x - y * 0.18f
            val a = k.edge(y, h)
            if (a <= 0.02f) continue
            p.strokeWidth = m.sz * u
            p.color = k.al(if (i % 3 == 0) Color.WHITE else blue, 150f * a)
            c.drawLine(x, y, x - 3f * u, y + 13f * u, p)
        }
        p.strokeCap = Paint.Cap.BUTT

        // gotas escorrendo no vidro, com rastro
        for (m in drops) {
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (-0.05f + 1.1f * life)
            val x = w * m.x + sin(y * 0.05f + m.ph * 9f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            p.style = Paint.Style.STROKE
            p.strokeWidth = 1.4f * u
            p.color = k.al(Color.WHITE, 60f * a)
            c.drawLine(x, y - 40f * u * life, x, y, p)
            p.style = Paint.Style.FILL
            p.color = k.al(Color.WHITE, 150f * a)
            k.rect.set(x - m.sz * u * 0.8f, y - m.sz * u, x + m.sz * u * 0.8f, y + m.sz * u * 1.3f)
            c.drawOval(k.rect, p)
        }

        // lágrimas grandes caindo devagar
        for (i in tears.indices) {
            val m = tears[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (0.05f + 0.95f * life * life)
            val x = w * m.x + sin(t * 0.8f + m.ph * 7f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            k.glow(c, x, y, m.sz * u, blue, 60f * a)
            k.icon(c, "drop", k.al(Color.parseColor("#DCE6FF"), 235f * a), x, y, m.sz * u, 0f)
        }

        // coração partido pulsando fraco, num canto
        val hb = 0.9f + 0.1f * sin(t * 1.1f)
        k.glow(c, w * 0.86f, h * 0.2f, 40f * u, Color.parseColor("#FF7AA8"), 70f)
        k.icon(c, "heartbreak", k.al(Color.parseColor("#FF8FB8"), 235f), w * 0.86f, h * 0.2f, 44f * u * hb, -8f)

        // poças com ondas na base
        for (i in 0 until 5) {
            val cx = w * (0.1f + 0.2f * i)
            for (j in 0 until 2) {
                val life = k.frac(t * 0.6f + i * 0.31f + j * 0.5f)
                p.style = Paint.Style.STROKE
                p.strokeWidth = 1.2f * u
                p.color = k.al(Color.WHITE, 140f * (1f - life))
                k.rect.set(cx - life * 22f * u, h - 18f * u - life * 5f * u, cx + life * 22f * u, h - 18f * u + life * 5f * u)
                c.drawOval(k.rect, p)
            }
        }

        // relâmpago distante, bem discreto
        val per = 11f
        val lt = t - floor(t / per) * per
        val fl = k.bump(lt, 0.2f, 0.05f)
        if (fl > 0.03f) {
            p.style = Paint.Style.FILL
            p.color = k.al(lilac, 60f * fl)
            c.drawRect(0f, 0f, w, h, p)
        }
    }
}

// ======================================================================================
// MUSICAL: palco. Holofotes, equalizador, pauta com notas, ondas sonoras e fones.
// ======================================================================================
class MusicalScene : AuraScene {
    private var kit: SceneKit? = null
    private var notes: List<SceneKit.Mote> = emptyList()
    private var disco: List<SceneKit.Mote> = emptyList()

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        notes = k.motes(11, 1001L, 0.04f, 0.085f, 16f, 30f, 22f)
        disco = k.motes(26, 1002L, 0.5f, 1.4f, 3f, 6f, 0f)
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        val u = k.u
        val p = k.p
        val pink = Color.parseColor("#FF6BD6")
        val cyan = Color.parseColor("#5BE7FF")
        val gold = Color.parseColor("#FFD86B")
        val beatPh = k.frac(t / 0.5f)
        val beat = k.bump(beatPh, 0.05f, 0.15f)

        k.vgrad(c, w, h, Color.parseColor("#14041F"), Color.parseColor("#5A1766"), Color.parseColor("#B4399A"))

        // feixes de holofote coloridos cruzando
        val beams = intArrayOf(pink, cyan, gold, Color.WHITE)
        for (i in 0 until 4) {
            val bx = w * (0.1f + 0.27f * i)
            val ang = sin(t * 0.8f + i * 1.7f) * 0.6f
            k.path.reset()
            k.path.moveTo(bx - 3f * u, 0f)
            k.path.lineTo(bx + 3f * u, 0f)
            k.path.lineTo(bx + sin(ang) * h * 1.3f + 34f * u, h)
            k.path.lineTo(bx + sin(ang) * h * 1.3f - 34f * u, h)
            k.path.close()
            p.style = Paint.Style.FILL
            p.color = k.al(beams[i], 40f + 25f * beat)
            c.drawPath(k.path, p)
            k.glow(c, bx, 4f * u, 12f * u, beams[i], 160f)
        }

        // pauta ondulando com notas deslizando
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.2f * u
        for (l in 0 until 5) {
            k.path.reset()
            for (s in 0..18) {
                val fx = s / 18f
                val y = h * 0.3f + l * 7f * u + sin(fx * 6f + t * 1.5f) * 8f * u
                if (s == 0) k.path.moveTo(w * fx, y) else k.path.lineTo(w * fx, y)
            }
            p.color = k.al(Color.WHITE, 110f)
            c.drawPath(k.path, p)
        }
        for (i in 0 until 6) {
            val fx = k.frac(t * 0.07f + i / 6f)
            val x = w * fx
            val y = h * 0.3f + ((i * 3) % 5) * 7f * u + sin(fx * 6f + t * 1.5f) * 8f * u
            p.style = Paint.Style.FILL
            p.color = k.al(Color.WHITE, 245f)
            k.rect.set(x - 5f * u, y - 3.6f * u, x + 5f * u, y + 3.6f * u)
            c.drawOval(k.rect, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 1.6f * u
            c.drawLine(x + 4.6f * u, y, x + 4.6f * u, y - 20f * u, p)
        }

        // ondas sonoras saindo da capa no ritmo
        if (hasPoster) {
            for (j in 0 until 3) {
                val life = k.frac(t * 0.5f + j / 3f)
                val e = life * 40f * u
                p.style = Paint.Style.STROKE
                p.strokeWidth = (4f - 3.4f * life) * u
                p.color = k.al(beams[j], 200f * (1f - life))
                k.rect.set(poster.left - e, poster.top - e, poster.right + e, poster.bottom + e)
                c.drawRoundRect(k.rect, 20f * u + e, 20f * u + e, p)
            }
        }

        // notas musicais subindo e balançando
        for (i in notes.indices) {
            val m = notes[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (1.05f - 1.1f * life)
            val x = w * m.x + sin(t * 1.3f + m.ph * 8f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            val s = m.sz * u * (1f + 0.15f * beat)
            k.glow(c, x, y, s * 0.9f, beams[i % 3], 70f * a)
            k.icon(c, "music", k.al(if (i % 2 == 0) Color.WHITE else beams[i % 3], 245f * a), x, y, s, sin(t * 1.5f + m.ph * 5f) * 18f)
        }

        // fones, microfone e brilho de bola de espelhos
        k.icon(c, "headphones", k.al(Color.WHITE, 235f), w * 0.1f, h * 0.2f, (34f + 4f * beat * 3f) * u, -10f)
        k.icon(c, "mic", k.al(gold, 235f), w * 0.9f, h * 0.2f, (30f + 3f * beat * 3f) * u, 14f)
        for (m in disco) {
            val s = sin(t * m.sp * 2f + m.ph * k.twoPi)
            val kk = if (s > 0f) s * s * s else 0f
            if (kk <= 0.03f) continue
            val y = h * m.y * 0.85f
            val a = kk * k.edge(y, h)
            if (a <= 0.02f) continue
            k.star4(c, w * m.x, y, m.sz * u * (0.5f + 0.8f * kk), t * 30f, beams[(m.ph * 4f).toInt() % 4], a)
        }

        // equalizador animado na base
        val n = 22
        val bw = w / n
        for (i in 0 until n) {
            val v = 0.25f + 0.75f * abs(sin(t * (2.2f + (i % 5) * 0.55f) + i * 1.3f)) * (0.7f + 0.3f * beat)
            val bh = v * 46f * u
            val x = i * bw
            p.style = Paint.Style.FILL
            p.color = k.al(if (i % 3 == 0) cyan else if (i % 3 == 1) pink else gold, 235f)
            k.rect.set(x + 1.5f * u, h - 10f * u - bh, x + bw - 1.5f * u, h - 10f * u)
            c.drawRoundRect(k.rect, 2f * u, 2f * u, p)
            p.color = k.al(Color.WHITE, 220f)
            c.drawRect(x + 1.5f * u, h - 10f * u - bh - 3f * u, x + bw - 1.5f * u, h - 10f * u - bh, p)
        }
    }
}

// ======================================================================================
// ESPORTE: estádio à noite. Refletores, gramado, bola quicando, troféu, medalhas e torcida.
// ======================================================================================
class EsporteScene : AuraScene {
    private var kit: SceneKit? = null
    private var medals: List<SceneKit.Mote> = emptyList()
    private var sparks: List<SceneKit.Mote> = emptyList()
    private var bugs: List<SceneKit.Mote> = emptyList()

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        medals = k.motes(6, 1101L, 0.04f, 0.08f, 18f, 28f, 18f)
        sparks = k.motes(30, 1102L, 0.5f, 1.2f, 2f, 4f, 0f)
        bugs = k.motes(14, 1103L, 0.1f, 0.3f, 1.5f, 2.5f, 0f)
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        val u = k.u
        val p = k.p
        val gold = Color.parseColor("#FFD54A")
        val lime = Color.parseColor("#B8FF8A")

        k.vgrad(c, w, h, Color.parseColor("#06182B"), Color.parseColor("#0E5A3A"), Color.parseColor("#3FA85A"))

        // refletores do estádio com feixe e insetos na luz
        for (i in 0 until 2) {
            val fx = if (i == 0) w * 0.1f else w * 0.9f
            val dir = if (i == 0) 1f else -1f
            k.path.reset()
            k.path.moveTo(fx - 4f * u, 6f * u)
            k.path.lineTo(fx + 4f * u, 6f * u)
            k.path.lineTo(fx + dir * w * 0.7f + 40f * u, h)
            k.path.lineTo(fx + dir * w * 0.7f - 40f * u, h)
            k.path.close()
            p.style = Paint.Style.FILL
            p.color = k.al(Color.WHITE, 28f + 8f * sin(t * 2f + i))
            c.drawPath(k.path, p)
            k.glow(c, fx, 8f * u, 22f * u, Color.WHITE, 230f)
            for (r in 0 until 3) {
                for (cc in 0 until 3) {
                    p.color = k.al(Color.WHITE, 255f)
                    c.drawCircle(fx + (cc - 1) * 5.4f * u, 8f * u + (r - 1) * 5.4f * u, 1.9f * u, p)
                }
            }
        }
        for (m in bugs) {
            val x = w * (0.5f + 0.5f * sin(t * m.sp * 2f + m.ph * 9f))
            val y = h * (0.1f + 0.5f * (0.5f + 0.5f * sin(t * m.sp * 3f + m.ph * 5f)))
            p.style = Paint.Style.FILL
            p.color = k.al(Color.WHITE, 140f * k.edge(y, h))
            c.drawCircle(x, y, m.sz * u, p)
        }

        // gramado em faixas com linhas do campo
        val horizon = h * 0.62f
        for (i in 0 until 8) {
            val y0 = horizon + (h - horizon) * (i / 8f) * (i / 8f)
            val y1 = horizon + (h - horizon) * ((i + 1) / 8f) * ((i + 1) / 8f)
            p.style = Paint.Style.FILL
            p.color = k.al(if (i % 2 == 0) Color.parseColor("#2E8F4A") else Color.parseColor("#379C54"), 255f)
            c.drawRect(0f, y0, w, y1, p)
        }
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f * u
        p.color = k.al(Color.WHITE, 220f)
        k.rect.set(w * 0.3f, horizon + (h - horizon) * 0.22f, w * 0.7f, h - 12f * u)
        c.drawOval(k.rect, p)
        c.drawLine(0f, horizon + (h - horizon) * 0.55f, w, horizon + (h - horizon) * 0.55f, p)

        // torcida fazendo a ola no fundo
        for (i in 0 until 24) {
            val x = w * (i + 0.5f) / 24f
            val wave = max(0f, sin(t * 3f - i * 0.5f))
            val y = horizon - 4f * u - wave * 10f * u
            p.style = Paint.Style.FILL
            val col = when (i % 4) {
                0 -> Color.parseColor("#FF5C5C")
                1 -> Color.parseColor("#FFD54A")
                2 -> Color.parseColor("#4D9BFF")
                else -> Color.WHITE
            }
            p.color = k.al(col, 240f)
            c.drawCircle(x, y, 3.8f * u, p)
            k.rect.set(x - 3.4f * u, y + 3f * u, x + 3.4f * u, horizon + 4f * u)
            c.drawRect(k.rect, p)
        }

        // medalhas caindo reluzentes
        for (i in medals.indices) {
            val m = medals[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (-0.08f + 1.14f * life)
            val x = w * m.x + sin(t * 0.8f + m.ph * 8f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            k.iconXY(c, "medal", k.al(gold, 245f * a), x, y, m.sz * u, sin(t * 1.2f + m.ph * 6f) * 20f, 0.5f + 0.5f * abs(sin(t * 1.5f + m.ph * 5f)), 1f)
        }

        // bola quicando com sombra no gramado
        val bph = k.frac(t * 0.55f)
        val bounce = abs(sin(bph * Math.PI.toFloat()))
        val bx = w * (0.1f + 0.8f * k.frac(t * 0.08f))
        val groundY = h - 28f * u
        val by = groundY - bounce * 90f * u
        p.style = Paint.Style.FILL
        p.color = k.al(Color.BLACK, 60f)
        k.rect.set(bx - 14f * u * (1.1f - bounce * 0.5f), groundY + 12f * u, bx + 14f * u * (1.1f - bounce * 0.5f), groundY + 17f * u)
        c.drawOval(k.rect, p)
        k.icon(c, "ball", k.al(Color.WHITE, 255f), bx, by, 30f * u, t * 200f)

        // troféu brilhando com raios girando
        val tx = w * 0.88f
        val ty = h * 0.34f
        for (i in 0 until 10) {
            val a = t * 0.5f + i * k.twoPi / 10f
            p.style = Paint.Style.STROKE
            p.strokeWidth = 2f * u
            p.color = k.al(gold, 120f)
            c.drawLine(tx + cos(a) * 24f * u, ty + sin(a) * 24f * u, tx + cos(a) * (38f + 6f * sin(t * 3f + i)) * u, ty + sin(a) * (38f + 6f * sin(t * 3f + i)) * u, p)
        }
        k.glow(c, tx, ty, 34f * u, gold, 130f)
        k.icon(c, "trophy", k.al(gold, 255f), tx, ty, 38f * u, sin(t) * 4f)

        // fogos dourados de comemoração
        val per = 4.5f
        val idx = floor(t / per).toInt()
        val q = (t - idx * per) / 1.4f
        if (q in 0f..1f) {
            val rr = java.util.Random(idx * 677L + 2L)
            val fx = w * (0.2f + 0.6f * rr.nextFloat())
            val fy = h * (0.18f + 0.25f * rr.nextFloat())
            for (s in 0 until 16) {
                val ang = s * k.twoPi / 16f
                val d = q * 38f * u
                p.style = Paint.Style.FILL
                p.color = k.al(if (s % 2 == 0) gold else lime, 255f * (1f - q))
                c.drawCircle(fx + cos(ang) * d, fy + sin(ang) * d + 14f * u * q * q, 2.6f * u * (1f - q * 0.5f), p)
            }
        }
        for (m in sparks) {
            val s = sin(t * m.sp * 2f + m.ph * k.twoPi)
            val kk = if (s > 0.6f) (s - 0.6f) / 0.4f else 0f
            if (kk <= 0.03f) continue
            val y = h * m.y * 0.55f
            k.star4(c, w * m.x, y, m.sz * u * (0.4f + kk), t * 20f, Color.WHITE, kk * k.edge(y, h))
        }
    }
}

// ======================================================================================
// REALEZA: salão do trono. Cortinas de veludo, estandartes, coroa radiante, joias e glitter.
// ======================================================================================
class RealezaScene : AuraScene {
    private var kit: SceneKit? = null
    private var glitter: List<SceneKit.Mote> = emptyList()
    private var gems: List<SceneKit.Mote> = emptyList()
    private var twinkle: List<SceneKit.Mote> = emptyList()

    private fun prep(k: SceneKit) {
        if (kit === k) return
        kit = k
        glitter = k.motes(36, 1201L, 0.04f, 0.1f, 2f, 4f, 20f)
        gems = k.motes(7, 1202L, 0.03f, 0.06f, 16f, 26f, 16f)
        twinkle = k.motes(24, 1203L, 0.5f, 1.3f, 3f, 7f, 0f)
    }

    override fun draw(c: Canvas, w: Float, h: Float, t: Float, poster: RectF, hasPoster: Boolean, k: SceneKit) {
        prep(k)
        val u = k.u
        val p = k.p
        val gold = Color.parseColor("#FFD36A")
        val darkGold = Color.parseColor("#B8841F")
        val velvet = Color.parseColor("#6E1233")

        k.vgrad(c, w, h, Color.parseColor("#1D0638"), Color.parseColor("#4B1A78"), Color.parseColor("#8A4A1C"))

        // sol real: raios dourados girando atrás da capa
        val cx = w / 2f
        val cy = h * 0.46f
        val rmax = max(w, h) * 0.8f
        p.style = Paint.Style.FILL
        for (i in 0 until 16) {
            val a = t * 0.08f + i * k.twoPi / 16f
            k.path.reset()
            k.path.moveTo(cx, cy)
            k.path.lineTo(cx + cos(a - 0.05f) * rmax, cy + sin(a - 0.05f) * rmax)
            k.path.lineTo(cx + cos(a + 0.05f) * rmax, cy + sin(a + 0.05f) * rmax)
            k.path.close()
            p.color = k.al(gold, if (i % 2 == 0) 46f else 24f)
            c.drawPath(k.path, p)
        }
        k.glow(c, cx, cy, min(w, h) * 0.55f, gold, 110f + 20f * sin(t * 1.2f))

        // castelo ao fundo, em silhueta dourada
        k.icon(c, "castle", k.al(Color.parseColor("#2A0F3F"), 245f), w * 0.5f, h * 0.9f, 80f * u, 0f)

        // estandartes balançando no alto
        for (i in 0 until 5) {
            val bx = w * (0.12f + 0.19f * i)
            val sway = sin(t * 1.4f + i * 1.1f) * 4f * u
            k.path.reset()
            k.path.moveTo(bx - 9f * u, 0f)
            k.path.lineTo(bx + 9f * u, 0f)
            k.path.lineTo(bx + 9f * u + sway, 30f * u)
            k.path.lineTo(bx + sway, 38f * u)
            k.path.lineTo(bx - 9f * u + sway, 30f * u)
            k.path.close()
            p.style = Paint.Style.FILL
            p.color = k.al(if (i % 2 == 0) velvet else Color.parseColor("#3C1670"), 250f)
            c.drawPath(k.path, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 1.4f * u
            p.color = k.al(gold, 230f)
            c.drawPath(k.path, p)
            k.icon(c, "crown", k.al(gold, 240f), bx + sway * 0.5f, 15f * u, 12f * u, 0f)
        }

        // cortinas de veludo nas laterais, com dobras ondulando
        for (side in 0 until 2) {
            val dir = if (side == 0) 1f else -1f
            val x0 = if (side == 0) 0f else w
            val cw = w * 0.09f
            p.style = Paint.Style.FILL
            p.color = k.al(velvet, 255f)
            k.rect.set(min(x0, x0 + dir * cw), 0f, max(x0, x0 + dir * cw), h)
            c.drawRect(k.rect, p)
            for (f in 0 until 5) {
                val fx = x0 + dir * (f + 0.5f) * cw / 5f
                val sh = 0.5f + 0.5f * sin(t * 0.8f + f * 1.7f + side * 2f)
                p.strokeWidth = 2.4f * u
                p.style = Paint.Style.STROKE
                p.color = k.al(if (f % 2 == 0) Color.parseColor("#A02050") else Color.parseColor("#4A0A22"), 150f + 60f * sh)
                c.drawLine(fx, 0f, fx + dir * 2f * u * sh, h, p)
            }
            p.style = Paint.Style.FILL
            p.color = k.al(gold, 240f)
            c.drawRect(min(x0, x0 + dir * cw) , h * 0.58f, max(x0, x0 + dir * cw), h * 0.58f + 3f * u, p)
            c.drawCircle(x0 + dir * cw * 0.5f, h * 0.58f + 12f * u, 4f * u, p)
        }

        // coroa grande flutuando, com brilho
        val bob = sin(t * 1.1f) * 3f * u
        k.glow(c, w * 0.5f, 26f * u + bob, 34f * u, gold, 150f)
        k.icon(c, "crown", k.al(gold, 255f), w * 0.5f, 26f * u + bob, 40f * u, sin(t * 0.8f) * 5f)

        // joias girando e cintilando
        for (i in gems.indices) {
            val m = gems[i]
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (-0.06f + 1.12f * life)
            val x = w * (0.1f + 0.8f * m.x) + sin(t * 0.8f + m.ph * 8f) * m.sw * u
            val a = k.env(life) * k.edge(y, h)
            if (a <= 0.02f) continue
            val colg = when (i % 3) {
                0 -> Color.parseColor("#7CE7FF")
                1 -> Color.parseColor("#FF7AC8")
                else -> Color.parseColor("#9CFFB0")
            }
            k.glow(c, x, y, m.sz * u, colg, 70f * a)
            k.iconXY(c, "gem", k.al(colg, 245f * a), x, y, m.sz * u, 0f, 0.55f + 0.45f * abs(sin(t * 1.4f + m.ph * 9f)), 1f)
        }

        // coroas pequenas flutuando
        for (i in 0 until 4) {
            val x = w * (0.15f + 0.23f * i)
            val y = h * (0.62f + 0.12f * sin(i * 2f)) + sin(t * 1.2f + i * 1.5f) * 6f * u
            k.icon(c, "crown", k.al(darkGold, 150f * k.edge(y, h)), x, y, 20f * u, sin(t + i) * 8f)
        }

        // chuva de glitter dourado
        for (m in glitter) {
            val life = k.frac(t * m.sp + m.ph)
            val y = h * (-0.05f + 1.1f * life)
            val x = w * m.x + sin(t * 1.1f + m.ph * 9f) * m.sw * u
            val a = k.edge(y, h)
            if (a <= 0.02f) continue
            val fl = abs(sin(t * 4f + m.ph * 20f))
            p.style = Paint.Style.FILL
            p.color = k.al(if (m.ph > 0.5f) gold else Color.WHITE, 250f * a * (0.3f + 0.7f * fl))
            c.drawCircle(x, y, m.sz * u * (0.5f + 0.5f * fl), p)
        }
        for (m in twinkle) {
            val s = sin(t * m.sp * 2f + m.ph * k.twoPi)
            val kk = if (s > 0f) s * s * s else 0f
            if (kk <= 0.03f) continue
            val y = h * m.y
            val a = kk * k.edge(y, h)
            if (a <= 0.02f) continue
            k.star4(c, w * m.x, y, m.sz * u * (0.5f + 0.8f * kk), t * 18f, gold, a)
        }

        // ondas de luz real saindo da capa
        if (hasPoster) {
            for (j in 0 until 3) {
                val life = k.frac(t * 0.25f + j / 3f)
                val e = life * 34f * u
                p.style = Paint.Style.STROKE
                p.strokeWidth = (3f - 2.4f * life) * u
                p.color = k.al(gold, 190f * (1f - life))
                k.rect.set(poster.left - e, poster.top - e, poster.right + e, poster.bottom + e)
                c.drawRoundRect(k.rect, 20f * u + e, 20f * u + e, p)
            }
        }
    }
}
