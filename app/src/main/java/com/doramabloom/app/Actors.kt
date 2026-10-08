package com.doramabloom.app

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
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

private fun actorClip(v: View, radiusDp: Int) {
    v.outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, o: Outline) {
            o.setRoundRect(0, 0, view.width, view.height, view.dp(radiusDp).toFloat())
        }
    }
    v.clipToOutline = true
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
    for (g in listOf("m", "f")) {
        val st = sectionTitle(if (g == "f") "Top 10 atrizes" else "Top 10 atores", "crown", Actors.accent(g))
        st.setPadding(dp(4), dp(20), 0, dp(10))
        col.addView(st, lin(MATCH, WRAP))
        col.addView(actorTopCard(g, Actors.ranked(everyone, g), onOpen), lin(MATCH, WRAP))
    }
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

/** Cartão fofo do Top 10: pódio com os 3 primeiros e uma linha para cada um dos outros. */
private fun Context.actorTopCard(gender: String, list: List<Actor>, onOpen: (Actor) -> Unit): View {
    val acc = Actors.accent(gender)
    val root = FrameLayout(this)
    val bg = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(Actors.soft(gender), mixColor(Palette.card, acc, if (Palette.dark) 0.10f else 0.05f))
    )
    bg.cornerRadius = dp(28).toFloat()
    bg.setStroke(dp(1), mixColor(acc, Palette.card, 0.55f))
    root.background = bg
    val clipper = FrameLayout(this)
    actorClip(clipper, 28)
    clipper.addView(PetalsView(this, listOf("petal", "sparkle"), acc, 9), FrameLayout.LayoutParams(MATCH, MATCH))
    root.addView(clipper, FrameLayout.LayoutParams(MATCH, MATCH))

    val c = LinearLayout(this)
    c.orientation = LinearLayout.VERTICAL
    c.setPadding(dp(14), dp(16), dp(14), dp(16))
    root.addView(c, FrameLayout.LayoutParams(MATCH, WRAP))

    val plural = if (gender == "f") "atrizes" else "atores"
    if (list.isEmpty()) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.gravity = Gravity.CENTER_HORIZONTAL
        box.setPadding(dp(8), dp(10), dp(8), dp(10))
        box.addView(IconView(this, "crown", acc, 34))
        box.addView(label("Ainda sem $plural no ranking", 15f, Actors.deep(gender), true, true), lin(WRAP, WRAP, t = 8))
        val m = label("Toque em \"Classificar\" lá em cima para marcar quem é quem. Ou abra uma pessoa no elenco de um dorama.", 12f, Palette.muted)
        m.gravity = Gravity.CENTER
        box.addView(m, lin(MATCH, WRAP, t = 4))
        c.addView(box, lin(MATCH, WRAP))
        return root
    }

    val top = list.take(10)
    val maxCount = maxOf(1, top[0].count)

    // pódio: 2º, 1º e 3º (o primeiro fica maior, no meio, com coroinha)
    val podium = top.take(3)
    val pr = LinearLayout(this)
    pr.orientation = LinearLayout.HORIZONTAL
    pr.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
    val orderIdx = when (podium.size) {
        3 -> listOf(1, 0, 2)
        2 -> listOf(0, 1)
        else -> listOf(0)
    }
    for ((pos, i) in orderIdx.withIndex()) {
        val a = podium[i]
        val rank = i + 1
        val cell = podiumCell(a, rank, onOpen)
        pr.addView(cell, lin(0, WRAP, 1f, l = 2, r = 2))
        cell.riseIn(pos * 90L)
    }
    c.addView(pr, lin(MATCH, WRAP, t = 2))

    // 4º ao 10º
    if (top.size > 3) {
        val rows = LinearLayout(this)
        rows.orientation = LinearLayout.VERTICAL
        for (i in 3 until top.size) rows.addView(actorRow(top[i], i + 1, maxCount, onOpen), lin(MATCH, WRAP, t = 8))
        c.addView(rows, lin(MATCH, WRAP, t = 10))
    }

    // quem passou do 10º
    if (list.size > 10) {
        val extra = LinearLayout(this)
        extra.orientation = LinearLayout.VERTICAL
        val more = pill("Ver todas (" + list.size + ")", acc, Color.WHITE, 12.5f, "list")
        more.setOnClickListener {
            for (i in 10 until minOf(list.size, 60)) extra.addView(actorRow(list[i], i + 1, maxCount, onOpen), lin(MATCH, WRAP, t = 8))
            more.visibility = View.GONE
        }
        c.addView(extra, lin(MATCH, WRAP, t = 2))
        val mr = LinearLayout(this)
        mr.gravity = Gravity.CENTER_HORIZONTAL
        mr.addView(more, lin(WRAP, WRAP))
        c.addView(mr, lin(MATCH, WRAP, t = 12))
    }
    return root
}

private fun Context.podiumCell(a: Actor, rank: Int, onOpen: (Actor) -> Unit): View {
    val acc = Actors.accent(a.gender)
    val medal = Actors.medal(rank)
    val size = if (rank == 1) 92 else 72
    val cell = LinearLayout(this)
    cell.orientation = LinearLayout.VERTICAL
    cell.gravity = Gravity.CENTER_HORIZONTAL
    cell.setPadding(0, dp(4), 0, dp(4))

    val extraTop = if (rank == 1) 18 else 0
    val ph = FrameLayout(this)
    ph.clipChildren = false
    ph.addView(
        avatarView(a.photo, size, medal, Actors.soft(a.gender), acc),
        FrameLayout.LayoutParams(dp(size), dp(size), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
    )
    if (rank == 1) {
        val crown = IconView(this, "crown", Actors.GOLD, 26)
        crown.rotation = -8f
        ph.addView(crown, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP or Gravity.CENTER_HORIZONTAL))
    }
    val badge = FrameLayout(this)
    val bb = GradientDrawable()
    bb.shape = GradientDrawable.OVAL
    bb.setColor(medal)
    bb.setStroke(dp(2), Color.WHITE)
    badge.background = bb
    val num = label(rank.toString(), 12f, Color.WHITE, true, true)
    num.gravity = Gravity.CENTER
    badge.addView(num, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
    ph.addView(badge, FrameLayout.LayoutParams(dp(26), dp(26), Gravity.BOTTOM or Gravity.END))
    cell.addView(ph, lin(dp(size + 6), dp(size + extraTop)))

    val nm = label(a.name, if (rank == 1) 13.5f else 12f, Palette.text, true)
    nm.gravity = Gravity.CENTER
    nm.maxLines = 2
    nm.ellipsize = TextUtils.TruncateAt.END
    cell.addView(nm, lin(MATCH, WRAP, t = 8))
    cell.addView(vividPill(dramasText(a.count), null, listOf(acc), 10.5f), lin(WRAP, WRAP, t = 5))
    if (a.avg > 0) {
        val rv = RatingView(this, 11, false)
        rv.score = Math.round(a.avg).toInt()
        rv.color = acc
        cell.addView(rv, lin(WRAP, WRAP, t = 5))
    }
    cell.setOnClickListener { onOpen(a) }
    cell.pressable(0.95f)
    return cell
}

private fun Context.actorRow(a: Actor, rank: Int, maxCount: Int, onOpen: (Actor) -> Unit): View {
    val acc = Actors.accent(a.gender)
    val row = LinearLayout(this)
    row.orientation = LinearLayout.HORIZONTAL
    row.gravity = Gravity.CENTER_VERTICAL
    row.setPadding(dp(10), dp(8), dp(12), dp(8))
    row.background = roundRect(Palette.card, dp(20).toFloat(), mixColor(Palette.line, acc, 0.35f), dp(1))

    val rk = label(rank.toString(), 12f, Actors.deep(a.gender), true, true)
    rk.gravity = Gravity.CENTER
    rk.background = ovalGradient(Actors.soft(a.gender), Actors.soft(a.gender))
    row.addView(rk, lin(dp(26), dp(26), r = 8))
    row.addView(avatarView(a.photo, 46, acc, Actors.soft(a.gender), acc), lin(dp(46), dp(46), r = 10))

    val mid = LinearLayout(this)
    mid.orientation = LinearLayout.VERTICAL
    val nl = LinearLayout(this)
    nl.orientation = LinearLayout.HORIZONTAL
    nl.gravity = Gravity.CENTER_VERTICAL
    val nm = label(a.name, 14f, Palette.text, true)
    nm.maxLines = 1
    nm.ellipsize = TextUtils.TruncateAt.END
    nl.addView(nm, lin(0, WRAP, 1f))
    if (a.favorite) nl.addView(IconView(this, "heart", acc, 13), lin(WRAP, WRAP, l = 6))
    mid.addView(nl, lin(MATCH, WRAP))
    var sub = dramasText(a.count)
    if (a.avg > 0) sub += "  ·  nota média " + "%.1f".format(a.avg)
    mid.addView(label(sub, 11f, Palette.muted), lin(MATCH, WRAP, t = 1))
    val bar = SoftBar(this)
    bar.barColor = acc
    bar.animateTo(a.count.toFloat() / maxCount, 0f, 300L + rank * 50L, 700L)
    mid.addView(bar, lin(MATCH, dp(6), t = 6))
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
