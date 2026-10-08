package com.doramabloom.app

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import java.io.File
import java.util.zip.CRC32

/**
 * Uma pessoa do elenco, juntando todos os doramas em que o nome aparece
 * (o mesmo ator com fotos diferentes em elencos diferentes vira UMA pessoa só).
 */
class Actor(
    val key: String,
    val name: String,
    /** "m" = ator, "f" = atriz, vazio = ainda sem classificar. */
    val gender: String,
    /** Doramas em que aparece, os mais recentes primeiro. */
    val dramas: List<Drama>,
    /** Foto a mostrar (a escolhida no perfil ou, sem escolha, uma do elenco); vazio = sem foto. */
    val photo: String,
    val info: PersonInfo?
) {
    val count: Int get() = dramas.size

    /** Média das notas (0 a 10) dos doramas avaliados; 0 se nenhum tem nota. */
    val avg: Double
        get() {
            val r = dramas.filter { it.score > 0 }
            return if (r.isEmpty()) 0.0 else r.map { it.score }.sum().toDouble() / r.size
        }

    val favorite: Boolean get() = info?.favorite == true
}

object Actors {
    private val FEM = Color.parseColor("#FF6B9D")
    private val MASC = Color.parseColor("#5B9BFF")
    private val NEUTRAL = Color.parseColor("#A78BFA")
    val GOLD: Int = Color.parseColor("#F5B83D")
    val SILVER: Int = Color.parseColor("#A9B4C4")
    val BRONZE: Int = Color.parseColor("#D48B5C")

    fun accent(g: String): Int = when (g) {
        "f" -> FEM
        "m" -> MASC
        else -> NEUTRAL
    }

    /** Fundo suave da cor da pessoa (claro no modo claro, escuro com um toque da cor no escuro). */
    fun soft(g: String): Int = mixColor(Palette.card, accent(g), if (Palette.dark) 0.24f else 0.16f)

    /** Cor para texto sobre o fundo suave. */
    fun deep(g: String): Int =
        if (Palette.dark) mixColor(accent(g), Color.WHITE, 0.45f) else mixColor(accent(g), Color.BLACK, 0.38f)

    fun genderLabel(g: String): String = when (g) {
        "f" -> "Atriz"
        "m" -> "Ator"
        else -> "Sem classificar"
    }

    fun genderIcon(g: String): String = when (g) {
        "f" -> "heart"
        "m" -> "star"
        else -> "person"
    }

    fun medal(rank: Int): Int = when (rank) {
        1 -> GOLD
        2 -> SILVER
        3 -> BRONZE
        else -> NEUTRAL
    }

    /** Mais doramas primeiro; empate: nota média maior, depois ordem alfabética. */
    val order: Comparator<Actor> = Comparator { a, b ->
        if (a.count != b.count) {
            b.count - a.count
        } else {
            val c = b.avg.compareTo(a.avg)
            if (c != 0) c else a.name.compareTo(b.name, true)
        }
    }

    /** Todas as pessoas que aparecem em algum elenco. */
    fun all(): List<Actor> {
        val dramas = Store.all().sortedByDescending { it.addedAt }
        val byKey = LinkedHashMap<String, ArrayList<Drama>>()
        val spell = HashMap<String, LinkedHashMap<String, Int>>()
        val photoOf = HashMap<String, String>()
        for (d in dramas) {
            val seenHere = HashSet<String>()
            for (p in d.castPeople) {
                val nm = p.name.trim()
                if (nm.isEmpty()) continue
                val k = Store.personKey(nm)
                if (k.isEmpty()) continue
                val m = spell.getOrPut(k) { LinkedHashMap() }
                m[nm] = (m[nm] ?: 0) + 1
                if (!photoOf.containsKey(k) && p.photo.isNotEmpty() && File(p.photo).exists()) photoOf[k] = p.photo
                if (seenHere.add(k)) byKey.getOrPut(k) { ArrayList() }.add(d)
            }
        }
        val out = ArrayList<Actor>()
        for ((k, ds) in byKey) {
            val info = Store.personInfo(k)
            val shown = if (info != null && info.name.isNotBlank()) info.name else (spell[k]?.maxByOrNull { it.value }?.key ?: k)
            val own = info?.photo ?: ""
            val photo = if (own.isNotEmpty() && File(own).exists()) own else (photoOf[k] ?: "")
            out.add(Actor(k, shown, info?.gender ?: "", ds, photo, info))
        }
        return out
    }

    fun find(key: String): Actor? = all().firstOrNull { it.key == key }

    fun ranked(list: List<Actor>, gender: String): List<Actor> = list.filter { it.gender == gender }.sortedWith(order)

    /** Posição (1, 2, 3...) entre quem tem o mesmo tipo; 0 se ainda não foi classificada. */
    fun rankOf(a: Actor, everyone: List<Actor>): Int {
        if (a.gender.isEmpty()) return 0
        val i = ranked(everyone, a.gender).indexOfFirst { it.key == a.key }
        return if (i < 0) 0 else i + 1
    }

    /** Assinatura barata de uma imagem (tamanho + soma dos primeiros bytes): fotos iguais têm a mesma. */
    fun sig(path: String): String {
        return try {
            val f = File(path)
            val crc = CRC32()
            f.inputStream().use { s ->
                val buf = ByteArray(16384)
                var left = 65536
                while (left > 0) {
                    val n = s.read(buf, 0, minOf(buf.size, left))
                    if (n <= 0) break
                    crc.update(buf, 0, n)
                    left -= n
                }
            }
            f.length().toString() + ":" + crc.value
        } catch (e: Exception) {
            path
        }
    }

    /** Uma foto que a pessoa já tem em algum elenco (ou a que está escolhida). */
    class PhotoOpt(val path: String, val from: String, val sig: String, val own: Boolean)

    /** Todas as fotos diferentes dessa pessoa; a escolhida (se houver) vem primeiro. */
    fun photoOptions(a: Actor): List<PhotoOpt> {
        val out = ArrayList<PhotoOpt>()
        val seen = HashSet<String>()
        val own = a.info?.photo ?: ""
        if (own.isNotEmpty() && File(own).exists()) {
            val s = sig(own)
            seen.add(s)
            out.add(PhotoOpt(own, "Escolhida", s, true))
        }
        for (d in a.dramas) {
            for (p in d.castPeople) {
                if (Store.personKey(p.name) != a.key) continue
                if (p.photo.isEmpty() || !File(p.photo).exists()) continue
                val s = sig(p.photo)
                if (seen.add(s)) out.add(PhotoOpt(p.photo, d.title, s, false))
            }
        }
        return out
    }

    /** Quem mais aparece junto: pessoa e em quantos doramas dividiram o elenco. */
    fun costars(a: Actor, everyone: List<Actor>, max: Int): List<Pair<Actor, Int>> {
        val byKey = HashMap<String, Actor>()
        for (x in everyone) byKey[x.key] = x
        val together = HashMap<String, Int>()
        for (d in a.dramas) {
            val seenHere = HashSet<String>()
            for (p in d.castPeople) {
                val k = Store.personKey(p.name)
                if (k.isEmpty() || k == a.key) continue
                if (seenHere.add(k)) together[k] = (together[k] ?: 0) + 1
            }
        }
        val r = ArrayList<Pair<Actor, Int>>()
        for ((k, n) in together) {
            val x = byKey[k] ?: continue
            if (n >= 2) r.add(Pair(x, n))
        }
        r.sortWith(Comparator<Pair<Actor, Int>> { p, q ->
            if (p.second != q.second) q.second - p.second else p.first.name.compareTo(q.first.name, true)
        })
        return r.take(max)
    }
}

private fun dramasText(n: Int): String = if (n == 1) "1 dorama" else "$n doramas"

// ===================================================================== NÚMEROS: TOP 10

/** Coloca na aba Números o aviso para classificar (se precisar) e os dois Top 10: atores e atrizes. */
fun Context.addActorSections(col: LinearLayout, onOpen: (Actor) -> Unit, onClassify: () -> Unit) {
    val everyone = Actors.all()
    if (everyone.isEmpty()) {
        val e = card(16, 22)
        e.addView(label("Top 10 de atores e atrizes", 16f, Palette.text, true, true))
        e.addView(
            label("Quando você colocar o elenco nos doramas, quem mais aparece ganha um lugar aqui.", 13f, Palette.muted),
            lin(WRAP, WRAP, t = 4)
        )
        col.addView(e, lin(MATCH, WRAP, t = 16))
        return
    }
    val unknown = everyone.filter { it.gender.isEmpty() }
    if (unknown.isNotEmpty()) col.addView(classifyBanner(unknown.size, onClassify), lin(MATCH, WRAP, t = 14))
    val st = sectionTitle("Top 10 do elenco", "crown", Actors.GOLD)
    st.setPadding(dp(4), dp(20), 0, dp(10))
    col.addView(st, lin(MATCH, WRAP))
    val both = LinearLayout(this)
    both.orientation = LinearLayout.HORIZONTAL
    both.isBaselineAligned = false
    both.addView(actorColumn("m", Actors.ranked(everyone, "m"), onOpen), lin(0, MATCH, 1f, r = 5))
    both.addView(actorColumn("f", Actors.ranked(everyone, "f"), onOpen), lin(0, MATCH, 1f, l = 5))
    col.addView(both, lin(MATCH, WRAP))
}

private fun Context.classifyBanner(n: Int, onClick: () -> Unit): View {
    val acc = Actors.accent("")
    val c = LinearLayout(this)
    c.orientation = LinearLayout.HORIZONTAL
    c.gravity = Gravity.CENTER_VERTICAL
    c.setPadding(dp(14), dp(12), dp(14), dp(12))
    val bg = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Actors.soft(""), Actors.soft("f")))
    bg.cornerRadius = dp(22).toFloat()
    bg.setStroke(dp(1), mixColor(acc, Palette.card, 0.5f))
    c.background = bg
    val dot = FrameLayout(this)
    dot.background = ovalGradient(acc, mixColor(acc, Actors.accent("f"), 0.5f))
    dot.addView(IconView(this, "sparkle", Color.WHITE, 20), FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
    c.addView(dot, lin(dp(42), dp(42), r = 12))
    val t = LinearLayout(this)
    t.orientation = LinearLayout.VERTICAL
    t.addView(label(if (n == 1) "1 pessoa sem classificar" else "$n pessoas sem classificar", 14.5f, Actors.deep(""), true, true))
    t.addView(label("Diga quem é ator e quem é atriz para entrar nos rankings.", 11.5f, Palette.muted), lin(WRAP, WRAP, t = 2))
    c.addView(t, lin(0, WRAP, 1f))
    c.addView(pill("Classificar", acc, Color.WHITE, 12.5f, "check"), lin(WRAP, WRAP, l = 8))
    c.setOnClickListener { onClick() }
    c.pressable(0.97f)
    return c
}

/**
 * Fundo de brilhinhos e coraçõezinhos do Top 10. Tudo fica dentro de uma margem de segurança,
 * então nenhum símbolo é cortado nas bordas do cartão.
 */
private class SparkleBackdrop(ctx: Context, private val tint: Int, seed: Int) : View(ctx) {
    private class Spec(val fx: Float, val fy: Float, val size: Float, val heart: Boolean, val phase: Float, val speed: Float)

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val star = Path()
    private val heart = Path()
    private val specs = ArrayList<Spec>()

    init {
        star.moveTo(0f, -1f)
        star.lineTo(0.18f, -0.18f)
        star.lineTo(1f, 0f)
        star.lineTo(0.18f, 0.18f)
        star.lineTo(0f, 1f)
        star.lineTo(-0.18f, 0.18f)
        star.lineTo(-1f, 0f)
        star.lineTo(-0.18f, -0.18f)
        star.close()
        heart.moveTo(0f, 0.85f)
        heart.cubicTo(-1.4f, -0.1f, -0.85f, -1.0f, 0f, -0.35f)
        heart.cubicTo(0.85f, -1.0f, 1.4f, -0.1f, 0f, 0.85f)
        heart.close()
        val r = java.util.Random(seed.toLong())
        for (i in 0 until 18) {
            specs.add(
                Spec(
                    r.nextFloat(), r.nextFloat(),
                    dp(3) + r.nextInt(dp(4)).toFloat(),
                    i % 3 == 0, r.nextFloat() * 6.28f, 0.9f + r.nextFloat() * 1.4f
                )
            )
        }
    }

    override fun onDraw(c: Canvas) {
        val m = dp(16).toFloat()
        val w = width - 2 * m
        val h = height - 2 * m
        if (w <= 0f || h <= 0f) return
        val time = SystemClock.uptimeMillis() / 1000f
        p.style = Paint.Style.FILL
        val starCol = mixColor(tint, Color.WHITE, if (Palette.dark) 0.55f else 0.2f)
        for (sp in specs) {
            val tw = 0.5f + 0.5f * Math.sin((time * sp.speed + sp.phase).toDouble()).toFloat()
            val a = 45 + (150 * tw).toInt()
            val sz = sp.size * (0.7f + 0.3f * tw)
            c.save()
            c.translate(m + w * sp.fx, m + h * sp.fy)
            c.scale(sz, sz)
            p.color = if (sp.heart) tint else starCol
            p.alpha = if (sp.heart) (a * 0.75f).toInt() else a
            c.drawPath(if (sp.heart) heart else star, p)
            c.restore()
        }
        if (isAttachedToWindow) postInvalidateDelayed(60)
    }
}

/** Uma coluna do Top 10 (atores OU atrizes): título em faixa, destaque do 1º lugar e as linhas dos outros. */
private fun Context.actorColumn(gender: String, list: List<Actor>, onOpen: (Actor) -> Unit): View {
    val acc = Actors.accent(gender)
    val plural = if (gender == "f") "atrizes" else "atores"
    val root = FrameLayout(this)
    val bg = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(Actors.soft(gender), mixColor(Palette.card, acc, if (Palette.dark) 0.08f else 0.04f))
    )
    bg.cornerRadius = dp(26).toFloat()
    bg.setStroke(dp(1), mixColor(acc, Palette.card, 0.55f))
    root.background = bg
    root.addView(SparkleBackdrop(this, acc, if (gender == "f") 7 else 3), FrameLayout.LayoutParams(MATCH, MATCH))

    val c = LinearLayout(this)
    c.orientation = LinearLayout.VERTICAL
    c.setPadding(dp(8), dp(8), dp(8), dp(12))
    root.addView(c, FrameLayout.LayoutParams(MATCH, WRAP))

    // faixa do título
    val head = LinearLayout(this)
    head.orientation = LinearLayout.HORIZONTAL
    head.gravity = Gravity.CENTER_VERTICAL
    head.setPadding(dp(7), dp(7), dp(12), dp(7))
    val hb = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(acc, mixColor(acc, Color.WHITE, 0.38f)))
    hb.cornerRadius = dp(20).toFloat()
    head.background = hb
    val bubble = FrameLayout(this)
    val bb0 = GradientDrawable()
    bb0.shape = GradientDrawable.OVAL
    bb0.setColor(Color.argb(80, 255, 255, 255))
    bubble.background = bb0
    bubble.addView(IconView(this, "crown", Color.WHITE, 15), FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
    head.addView(bubble, lin(dp(28), dp(28), r = 8))
    val tt = LinearLayout(this)
    tt.orientation = LinearLayout.VERTICAL
    val title = label(if (gender == "f") "Atrizes" else "Atores", 15.5f, Color.WHITE, true, true)
    title.setShadowLayer(dp(2).toFloat(), 0f, dp(1).toFloat(), Color.argb(110, 0, 0, 0))
    tt.addView(title)
    val sub = label("top 10", 10f, Color.argb(220, 255, 255, 255), true)
    tt.addView(sub, lin(WRAP, WRAP, t = -1))
    head.addView(tt, lin(0, WRAP, 1f))
    head.addView(IconView(this, Actors.genderIcon(gender), Color.WHITE, 15), lin(WRAP, WRAP))
    c.addView(head, lin(MATCH, WRAP))
    head.riseIn(0L, 10, 320L)

    if (list.isEmpty()) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.gravity = Gravity.CENTER_HORIZONTAL
        box.setPadding(dp(6), dp(22), dp(6), dp(14))
        box.addView(IconView(this, "crown", acc, 30))
        box.addView(label("Ainda sem $plural", 13.5f, Actors.deep(gender), true, true), lin(WRAP, WRAP, t = 8))
        val m = label("Marque quem é quem em Classificar, ou abra uma pessoa no elenco de um dorama.", 11f, Palette.muted)
        m.gravity = Gravity.CENTER
        box.addView(m, lin(MATCH, WRAP, t = 4))
        c.addView(box, lin(MATCH, WRAP))
        return root
    }

    val top = list.take(10)
    val maxCount = maxOf(1, top[0].count)

    val hero = heroCell(top[0], onOpen)
    c.addView(hero, lin(MATCH, WRAP, t = 6))
    hero.riseIn(80L)

    // degraus de prata e bronze (2º e 3º), lado a lado, cada um mais baixo que o outro
    if (top.size > 1) {
        val pod = LinearLayout(this)
        pod.orientation = LinearLayout.HORIZONTAL
        pod.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        pod.clipChildren = false
        val c2 = podiumCell(top[1], 2, onOpen)
        pod.addView(c2, lin(0, WRAP, 1f, r = 3))
        c2.riseIn(180L)
        if (top.size > 2) {
            val c3 = podiumCell(top[2], 3, onOpen)
            pod.addView(c3, lin(0, WRAP, 1f, l = 3))
            c3.riseIn(260L)
        } else {
            pod.addView(View(this), lin(0, 1, 1f, l = 3))
        }
        c.addView(pod, lin(MATCH, WRAP, t = 10))
    }

    if (top.size > 3) {
        val rows = LinearLayout(this)
        rows.orientation = LinearLayout.VERTICAL
        for (i in 3 until top.size) {
            val r = actorRow(top[i], i + 1, maxCount, onOpen)
            rows.addView(r, lin(MATCH, WRAP, t = if (i == 3) 0 else 6))
            r.riseIn(320L + (i - 3) * 45L)
        }
        c.addView(rows, lin(MATCH, WRAP, t = 12))
    }

    if (list.size > 10) {
        val extra = LinearLayout(this)
        extra.orientation = LinearLayout.VERTICAL
        c.addView(extra, lin(MATCH, WRAP, t = 6))
        val more = pill("Ver todos (" + list.size + ")", acc, Color.WHITE, 11.5f, "list")
        more.setOnClickListener {
            for (i in 10 until minOf(list.size, 60)) extra.addView(actorRow(list[i], i + 1, maxCount, onOpen), lin(MATCH, WRAP, t = 6))
            more.visibility = View.GONE
        }
        val mr = LinearLayout(this)
        mr.gravity = Gravity.CENTER_HORIZONTAL
        mr.addView(more, lin(WRAP, WRAP))
        c.addView(mr, lin(MATCH, WRAP, t = 10))
    }
    return root
}

/** O 1º lugar: foto grande com aura, louros e raios, coroa assentada na cabeça e faixa dourada com o nome. */
private fun Context.heroCell(a: Actor, onOpen: (Actor) -> Unit): View {
    val acc = Actors.accent(a.gender)
    val tone = Metal.tone(1)
    val cell = LinearLayout(this)
    cell.orientation = LinearLayout.VERTICAL
    cell.gravity = Gravity.CENTER_HORIZONTAL
    cell.setPadding(dp(2), 0, dp(2), dp(6))

    val stage = FrameLayout(this)
    stage.clipChildren = false
    stage.clipToPadding = false
    stage.addView(
        HeroAuraView(this, 84, 1),
        FrameLayout.LayoutParams(dp(136), dp(136), Gravity.TOP or Gravity.CENTER_HORIZONTAL).also { it.topMargin = dp(14) }
    )
    val wrap = FrameLayout(this)
    wrap.clipChildren = false
    wrap.addView(
        avatarView(a.photo, 84, tone.mid, Actors.soft(a.gender), acc),
        FrameLayout.LayoutParams(dp(84), dp(84), Gravity.TOP or Gravity.CENTER_HORIZONTAL)
    )
    val badge = FrameLayout(this)
    badge.background = Metal.medal(1, dp(2))
    badge.elevation = dp(6).toFloat()
    val num = label("1", 13f, Color.parseColor("#4A2E00"), true, true)
    num.gravity = Gravity.CENTER
    badge.addView(num, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
    wrap.addView(badge, FrameLayout.LayoutParams(dp(28), dp(28), Gravity.BOTTOM or Gravity.END))
    stage.addView(wrap, FrameLayout.LayoutParams(dp(90), dp(90), Gravity.TOP or Gravity.CENTER_HORIZONTAL).also { it.topMargin = dp(40) })
    // a base da coroa entra uns dp na foto: ela fica "usada", não solta no ar
    val crown = CrownView(this, 1, acc)
    crown.elevation = dp(8).toFloat()
    stage.addView(crown, FrameLayout.LayoutParams(dp(46), dp(36), Gravity.TOP or Gravity.CENTER_HORIZONTAL).also { it.topMargin = dp(14) })
    cell.addView(stage, lin(MATCH, dp(150)))

    // faixa dourada com o nome
    val banner = LinearLayout(this)
    banner.gravity = Gravity.CENTER
    banner.setPadding(dp(10), dp(5), dp(10), dp(5))
    val bg = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(tone.hi, tone.mid, mixColor(tone.lo, tone.mid, 0.3f)))
    bg.cornerRadius = dp(12).toFloat()
    bg.setStroke(dp(1), Color.argb(210, 255, 255, 255))
    banner.background = bg
    banner.elevation = dp(3).toFloat()
    val nm = label(a.name, 14.5f, Color.parseColor("#4A2E00"), true, true)
    nm.gravity = Gravity.CENTER
    nm.maxLines = 2
    nm.ellipsize = TextUtils.TruncateAt.END
    banner.addView(nm, lin(WRAP, WRAP))
    cell.addView(banner, lin(MATCH, WRAP, l = 4, r = 4, t = -12))

    cell.addView(vividPill(dramasText(a.count), null, listOf(acc), 10.5f), lin(WRAP, WRAP, t = 8))
    if (a.avg > 0) {
        val rv = RatingView(this, 12, false)
        rv.score = Math.round(a.avg).toInt()
        rv.color = acc
        cell.addView(rv, lin(WRAP, WRAP, t = 7))
        cell.addView(label("nota média " + "%.1f".format(a.avg), 10.5f, Palette.muted), lin(WRAP, WRAP, t = 2))
    }
    cell.setOnClickListener { onOpen(a) }
    cell.pressable(0.96f)
    return cell
}

/** 2º e 3º lugar: foto com coroinha de prata ou bronze, nome, nota e um degrau de metal embaixo. */
private fun Context.podiumCell(a: Actor, rank: Int, onOpen: (Actor) -> Unit): View {
    val acc = Actors.accent(a.gender)
    val tone = Metal.tone(rank)
    val cell = LinearLayout(this)
    cell.orientation = LinearLayout.VERTICAL
    cell.gravity = Gravity.CENTER_HORIZONTAL
    cell.clipChildren = false

    val block = FrameLayout(this)
    block.clipChildren = false
    val avSize = 54
    block.addView(
        avatarView(a.photo, avSize, tone.mid, Actors.soft(a.gender), acc),
        FrameLayout.LayoutParams(dp(avSize), dp(avSize), Gravity.TOP or Gravity.CENTER_HORIZONTAL).also { it.topMargin = dp(15) }
    )
    val badge = FrameLayout(this)
    badge.background = Metal.medal(rank, dp(1))
    badge.elevation = dp(6).toFloat()
    val num = label(rank.toString(), 10f, tone.edge, true, true)
    num.gravity = Gravity.CENTER
    badge.addView(num, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
    block.addView(badge, FrameLayout.LayoutParams(dp(20), dp(20), Gravity.TOP or Gravity.END).also {
        it.topMargin = dp(15 + avSize - 18)
        it.rightMargin = dp(2)
    })
    val crown = CrownView(this, rank, acc)
    crown.elevation = dp(8).toFloat()
    // avatar começa em y = 15; a base da coroa entra uns dp na foto
    block.addView(crown, FrameLayout.LayoutParams(dp(30), dp(24), Gravity.TOP or Gravity.CENTER_HORIZONTAL).also { it.topMargin = dp(1) })
    cell.addView(block, lin(MATCH, dp(15 + avSize + 2)))

    val nm = label(a.name, 12f, Palette.text, true, true)
    nm.gravity = Gravity.CENTER
    nm.maxLines = 2
    nm.ellipsize = TextUtils.TruncateAt.END
    cell.addView(nm, lin(MATCH, WRAP, t = 4, l = 2, r = 2))
    cell.addView(label(dramasText(a.count), 10f, Palette.muted), lin(WRAP, WRAP, t = 1))
    if (a.avg > 0) {
        val rv = RatingView(this, 9, false)
        rv.score = Math.round(a.avg).toInt()
        rv.color = acc
        cell.addView(rv, lin(WRAP, WRAP, t = 4))
    }
    cell.addView(PedestalView(this, rank), lin(MATCH, dp(if (rank == 2) 46 else 34), t = 8))
    cell.setOnClickListener { onOpen(a) }
    cell.pressable(0.95f)
    return cell
}

/** Da 2ª posição em diante: linha compacta com foto, medalhinha, nome, doramas e barrinha. */
private fun Context.actorRow(a: Actor, rank: Int, maxCount: Int, onOpen: (Actor) -> Unit): View {
    val acc = Actors.accent(a.gender)
    val podium = rank <= 3
    val ring = if (podium) Actors.medal(rank) else acc
    val row = LinearLayout(this)
    row.orientation = LinearLayout.HORIZONTAL
    row.gravity = Gravity.CENTER_VERTICAL
    row.setPadding(dp(6), dp(6), dp(8), dp(6))
    val fillC = if (podium) mixColor(Palette.card, ring, if (Palette.dark) 0.20f else 0.14f) else Palette.card
    val strokeC = if (podium) mixColor(Palette.line, ring, 0.75f) else mixColor(Palette.line, acc, 0.3f)
    row.background = roundRect(fillC, dp(18).toFloat(), strokeC, dp(1))

    val wrap = FrameLayout(this)
    wrap.clipChildren = false
    wrap.addView(
        avatarView(a.photo, 40, ring, Actors.soft(a.gender), acc),
        FrameLayout.LayoutParams(dp(40), dp(40), Gravity.TOP or Gravity.START)
    )
    val badge = FrameLayout(this)
    val bb = GradientDrawable()
    bb.shape = GradientDrawable.OVAL
    bb.setColor(if (podium) ring else mixColor(acc, Palette.card, 0.15f))
    bb.setStroke(dp(1), Color.WHITE)
    badge.background = bb
    badge.elevation = dp(5).toFloat()
    val rk = label(rank.toString(), if (rank >= 10) 8f else 9.5f, Color.WHITE, true)
    rk.gravity = Gravity.CENTER
    badge.addView(rk, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
    wrap.addView(badge, FrameLayout.LayoutParams(dp(18), dp(18), Gravity.BOTTOM or Gravity.END))
    row.addView(wrap, lin(dp(46), dp(46), r = 6))

    val mid = LinearLayout(this)
    mid.orientation = LinearLayout.VERTICAL
    val nl = LinearLayout(this)
    nl.orientation = LinearLayout.HORIZONTAL
    nl.gravity = Gravity.CENTER_VERTICAL
    val nm = label(a.name, 12.5f, Palette.text, true)
    nm.maxLines = 1
    nm.ellipsize = TextUtils.TruncateAt.END
    nl.addView(nm, lin(0, WRAP, 1f))
    if (a.favorite) nl.addView(IconView(this, "heart", acc, 11), lin(WRAP, WRAP, l = 3))
    mid.addView(nl, lin(MATCH, WRAP))
    val subRow = LinearLayout(this)
    subRow.orientation = LinearLayout.HORIZONTAL
    subRow.gravity = Gravity.CENTER_VERTICAL
    val subL = label(dramasText(a.count), 10.5f, Palette.muted)
    subL.maxLines = 1
    subRow.addView(subL, lin(WRAP, WRAP))
    if (a.avg > 0) {
        subRow.addView(IconView(this, "heart", acc, 9), lin(WRAP, WRAP, l = 6, r = 2))
        subRow.addView(label("%.1f".format(a.avg), 10.5f, Actors.deep(a.gender), true), lin(WRAP, WRAP))
    }
    mid.addView(subRow, lin(MATCH, WRAP, t = 1))
    val bar = SoftBar(this)
    bar.barColor = ring
    bar.animateTo(a.count.toFloat() / maxCount, 0f, 300L + rank * 50L, 700L)
    mid.addView(bar, lin(MATCH, dp(4), t = 5))
    row.addView(mid, lin(0, WRAP, 1f))

    row.setOnClickListener { onOpen(a) }
    row.pressable(0.97f)
    return row
}

// ===================================================================== CLASSIFICAR (ator ou atriz)

/** Passa pessoa por pessoa perguntando se é ator ou atriz. Cada toque já fica salvo. */
fun Activity.showClassifyDialog(onDone: () -> Unit) {
    val todo = Actors.all().filter { it.gender.isEmpty() }.sortedWith(Actors.order)
    if (todo.isEmpty()) {
        onDone()
        return
    }
    var idx = 0
    val box = LinearLayout(this)
    box.orientation = LinearLayout.VERTICAL
    box.gravity = Gravity.CENTER_HORIZONTAL
    box.setPadding(dp(20), dp(20), dp(20), dp(16))
    box.background = roundRect(Palette.card, dp(28).toFloat())
    val dialog = AlertDialog.Builder(this).setView(box).create()

    fun choose(g: String) {
        val a = todo[idx]
        val info = Store.personInfo(a.key) ?: PersonInfo(a.key, a.name)
        info.gender = g
        Store.savePersonInfo(info)
        idx++
    }

    fun render() {
        box.removeAllViews()
        if (idx >= todo.size) {
            dialog.dismiss()
            return
        }
        val a = todo[idx]
        val neutral = Actors.accent("")
        box.addView(label((idx + 1).toString() + " de " + todo.size, 12f, Palette.muted, true))
        val bar = SoftBar(this)
        bar.barColor = neutral
        bar.progress = idx.toFloat() / todo.size
        box.addView(bar, lin(MATCH, dp(6), t = 6))
        box.addView(avatarView(a.photo, 112, neutral, Actors.soft(""), neutral), lin(dp(112), dp(112), t = 16))
        val nm = label(a.name, 22f, Palette.text, true, true)
        nm.gravity = Gravity.CENTER
        nm.maxLines = 2
        nm.ellipsize = TextUtils.TruncateAt.END
        box.addView(nm, lin(MATCH, WRAP, t = 10))
        val titles = a.dramas.take(2).joinToString("  ·  ") { it.title }
        val sub = label(dramasText(a.count) + (if (titles.isNotEmpty()) "\n" + titles else ""), 12f, Palette.muted)
        sub.gravity = Gravity.CENTER
        box.addView(sub, lin(MATCH, WRAP, t = 4))

        val btns = LinearLayout(this)
        btns.orientation = LinearLayout.HORIZONTAL
        val bf = bigPill("Atriz", Actors.accent("f"), Color.WHITE, 15f, "heart")
        bf.setOnClickListener {
            choose("f")
            render()
        }
        val bm = bigPill("Ator", Actors.accent("m"), Color.WHITE, 15f, "star")
        bm.setOnClickListener {
            choose("m")
            render()
        }
        btns.addView(bf, lin(0, WRAP, 1f, r = 6))
        btns.addView(bm, lin(0, WRAP, 1f, l = 6))
        box.addView(btns, lin(MATCH, WRAP, t = 18))

        val foot = LinearLayout(this)
        foot.orientation = LinearLayout.HORIZONTAL
        foot.gravity = Gravity.CENTER_HORIZONTAL
        val skip = pill("Pular", Palette.pinkSoft, Palette.pinkDark, 12.5f, "forward")
        skip.setOnClickListener {
            idx++
            render()
        }
        val close = pill("Fechar", Palette.pinkSoft, Palette.pinkDark, 12.5f, "close")
        close.setOnClickListener { dialog.dismiss() }
        foot.addView(skip, lin(WRAP, WRAP, r = 8))
        foot.addView(close, lin(WRAP, WRAP))
        box.addView(foot, lin(MATCH, WRAP, t = 12))
        box.riseIn(0L, 10, 260L)
    }

    dialog.setOnDismissListener { onDone() }
    render()
    dialog.show()
}
