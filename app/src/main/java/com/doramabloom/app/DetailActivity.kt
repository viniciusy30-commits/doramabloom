package com.doramabloom.app

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetailActivity : AppCompatActivity() {

    private var id = -1L
    private var scroll: ScrollView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        id = intent.getLongExtra("id", -1L)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun fmt(ms: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(ms))

    private fun render() {
        val d = Store.get(id)
        if (d == null) {
            finish()
            return
        }
        val keep = scroll?.scrollY ?: 0
        val g = Genres.byKey(d.genre)

        val root = FrameLayout(this)
        root.background = gradient(g.soft, Color.WHITE)

        val sv = ScrollView(this)
        sv.isVerticalScrollBarEnabled = false
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(10), dp(16), dp(40))
        sv.addView(col)
        root.addView(sv, FrameLayout.LayoutParams(MATCH, MATCH))

        // ---- barra superior
        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        top.addView(roundBtn("back", g.dark, false, 18) { finish() }, lin(dp(40), dp(40)))
        top.addView(View(this), lin(0, dp(1), 1f))
        top.addView(roundBtn("edit", g.dark, false, 18) {
            val i = Intent(this, EditActivity::class.java)
            i.putExtra("id", d.id)
            startActivity(i)
        }, lin(dp(40), dp(40), r = 8))
        top.addView(roundBtn("heart", g.primary, d.favorite, 18) {
            d.favorite = !d.favorite
            Store.save(d)
            render()
        }, lin(dp(40), dp(40)))
        col.addView(top, lin(MATCH, WRAP))

        // ---- hero
        val hero = FrameLayout(this)
        hero.background = gradient(g.primary, g.dark, dp(30).toFloat(), GradientDrawable.Orientation.TL_BR)
        hero.elevation = dp(6).toFloat()
        val deco = IconView(this, "blossom", Color.parseColor("#22FFFFFF"), 130)
        val dlp = FrameLayout.LayoutParams(WRAP, WRAP)
        dlp.gravity = Gravity.END or Gravity.BOTTOM
        dlp.setMargins(0, 0, dp(-26), dp(-26))
        hero.addView(deco, dlp)

        val hrow = LinearLayout(this)
        hrow.orientation = LinearLayout.HORIZONTAL
        hrow.setPadding(dp(16), dp(16), dp(16), dp(16))
        val cover = CoverView(this, 22)
        cover.bind(d, 600)
        cover.elevation = dp(6).toFloat()
        hrow.addView(cover, lin(dp(124), dp(182), r = 14))

        val info = LinearLayout(this)
        info.orientation = LinearLayout.VERTICAL
        val title = label(d.title, 21f, Color.WHITE, true, true)
        title.maxLines = 4
        title.ellipsize = TextUtils.TruncateAt.END
        info.addView(title)
        if (d.original.isNotBlank()) {
            info.addView(label(d.original, 12f, Color.parseColor("#E6FFFFFF")), lin(WRAP, WRAP, t = 2))
        }
        info.addView(label(subtitle(d), 12f, Color.parseColor("#E6FFFFFF")), lin(WRAP, WRAP, t = 6))

        val gs = HorizontalScrollView(this)
        gs.isHorizontalScrollBarEnabled = false
        val grow = LinearLayout(this)
        grow.orientation = LinearLayout.HORIZONTAL
        gs.addView(grow)
        val glass = Color.parseColor("#44FFFFFF")
        grow.addView(pill(g.label, glass, Color.WHITE, 11f, g.icon), lin(WRAP, WRAP, r = 6))
        for (tk in d.tags) {
            if (tk == d.genre) continue
            val tg = Genres.byKey(tk)
            grow.addView(pill(tg.label, glass, Color.WHITE, 11f, tg.icon), lin(WRAP, WRAP, r = 6))
        }
        info.addView(gs, lin(MATCH, WRAP, t = 8))

        val srow = LinearLayout(this)
        srow.orientation = LinearLayout.HORIZONTAL
        srow.gravity = Gravity.CENTER_VERTICAL
        val badge = scoreBadge(50, 20f)
        badge.text = if (d.score <= 0) "-" else d.score.toString()
        badge.setTextColor(g.primary)
        val bbg = GradientDrawable()
        bbg.shape = GradientDrawable.OVAL
        bbg.setColor(Color.WHITE)
        badge.background = bbg
        srow.addView(badge, lin(dp(50), dp(50), r = 10))
        val rcol = LinearLayout(this)
        rcol.orientation = LinearLayout.VERTICAL
        val rv = RatingView(this, 15, false)
        rv.score = d.score
        rv.color = Color.WHITE
        rcol.addView(rv)
        rcol.addView(label("nota de 10", 11f, Color.parseColor("#E6FFFFFF")), lin(WRAP, WRAP, t = 2))
        srow.addView(rcol)
        info.addView(srow, lin(WRAP, WRAP, t = 12))

        hrow.addView(info, lin(0, WRAP, 1f))
        hero.addView(hrow, FrameLayout.LayoutParams(MATCH, WRAP))
        col.addView(hero, lin(MATCH, WRAP, t = 10))

        // ---- tiles
        val tiles = LinearLayout(this)
        tiles.orientation = LinearLayout.HORIZONTAL
        val tw = totalEps(d)
        tiles.addView(tile("tv", seasonCount(d).toString(), "temporadas", g.primary), lin(0, WRAP, 1f, r = 6))
        tiles.addView(tile("play", if (tw > 0) watchedEps(d).toString() + "/" + tw else watchedEps(d).toString(), "episódios", g.primary), lin(0, WRAP, 1f, r = 6))
        tiles.addView(tile("clock", if (d.epMinutes > 0) d.epMinutes.toString() + " min" else "-", "por episódio", g.primary), lin(0, WRAP, 1f, r = 6))
        tiles.addView(tile("replay", d.rewatch.toString() + "x", "reassistido", g.primary), lin(0, WRAP, 1f))
        col.addView(tiles, lin(MATCH, WRAP, t = 14))

        // ---- status
        val stCard = card(14, 22)
        stCard.addView(sectionTitle("Status", "bookmark", g.primary))
        val stOpts = ArrayList<Opt>()
        for (s in Statuses.all) stOpts.add(Opt(s.key, s.label, s.color, s.icon))
        stCard.addView(chipScroller(stOpts, d.status) { key ->
            Store.setStatus(d, key)
            root.post { render() }
        }, lin(MATCH, WRAP, t = 8))
        col.addView(stCard, lin(MATCH, WRAP, t = 14))

        // ---- progresso por temporada
        val pc = card(14, 22)
        pc.addView(sectionTitle("Meu progresso", "play", g.primary))
        val total = totalEps(d)
        val overall = label(
            if (total > 0) watchedEps(d).toString() + " de " + total + " episódios" else watchedEps(d).toString() + " episódios assistidos",
            14f, Palette.text, true
        )
        pc.addView(overall, lin(WRAP, WRAP, t = 8))
        val obar = SoftBar(this)
        obar.progress = progressOf(d)
        obar.barColor = g.primary
        pc.addView(obar, lin(MATCH, dp(10), t = 6))

        val plus = pill("+1 episódio", g.primary, Color.WHITE, 14f, "play")
        plus.setPadding(dp(18), dp(12), dp(18), dp(12))
        plus.setOnClickListener {
            val finished = Store.bump(d)
            if (finished) Toast.makeText(this, "Parabéns, você terminou!", Toast.LENGTH_LONG).show()
            render()
        }
        pc.addView(plus, lin(MATCH, WRAP, t = 12))

        for (i in d.seasonEps.indices) {
            val t = d.seasonEps[i]
            val w = d.watched[i]
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.addView(label("Temp. " + (i + 1), 13f, g.dark, true), lin(dp(62), WRAP))
            val b = SoftBar(this)
            b.progress = if (t > 0) w.toFloat() / t.toFloat() else 0f
            b.barColor = g.primary
            row.addView(b, lin(0, dp(9), 1f, l = 2, r = 8))
            row.addView(label(if (t > 0) "$w/$t" else "$w", 12f, Palette.muted, true), lin(dp(46), WRAP))
            row.addView(roundBtn("minus", g.primary, false, 14) {
                Store.adjust(d, i, -1)
                render()
            }, lin(dp(30), dp(30), l = 4))
            row.addView(roundBtn("add", g.primary, true, 14) {
                val fin = Store.adjust(d, i, 1)
                if (fin) Toast.makeText(this, "Parabéns, você terminou!", Toast.LENGTH_LONG).show()
                render()
            }, lin(dp(30), dp(30), l = 6))
            pc.addView(row, lin(MATCH, WRAP, t = 10))
        }

        val rw = LinearLayout(this)
        rw.orientation = LinearLayout.HORIZONTAL
        rw.gravity = Gravity.CENTER_VERTICAL
        rw.addView(IconView(this, "replay", g.primary, 16))
        rw.addView(label("Vezes que reassisti", 13f, Palette.text, true), lin(0, WRAP, 1f, l = 8))
        rw.addView(roundBtn("minus", g.primary, false, 14) {
            if (d.rewatch > 0) {
                d.rewatch -= 1
                Store.save(d)
                render()
            }
        }, lin(dp(30), dp(30)))
        rw.addView(label(d.rewatch.toString(), 15f, g.dark, true), lin(dp(34), WRAP))
        rw.addView(roundBtn("add", g.primary, true, 14) {
            d.rewatch += 1
            Store.save(d)
            render()
        }, lin(dp(30), dp(30)))
        pc.addView(rw, lin(MATCH, WRAP, t = 16))
        col.addView(pc, lin(MATCH, WRAP, t = 12))

        // ---- nota
        val rc = card(14, 22)
        rc.addView(sectionTitle("Minha nota", "heart", g.primary))
        val scoreTxt = label(
            if (d.score <= 0) "Sem nota ainda" else d.score.toString() + " / 10",
            15f, g.dark, true, true
        )
        val big = RatingView(this, 34, true)
        big.score = d.score
        big.color = g.primary
        big.onChange = { ns ->
            d.score = ns
            Store.save(d)
            scoreTxt.text = if (ns <= 0) "Sem nota ainda" else ns.toString() + " / 10"
            root.post { render() }
        }
        rc.addView(big, lin(WRAP, WRAP, t = 10))
        rc.addView(scoreTxt, lin(WRAP, WRAP, t = 6))
        rc.addView(label("Toque na metade esquerda do coração para meio ponto.", 11f, Palette.muted), lin(WRAP, WRAP, t = 2))
        col.addView(rc, lin(MATCH, WRAP, t = 12))

        // ---- sinopse
        if (d.synopsis.isNotBlank()) {
            val sc = card(14, 22)
            sc.addView(sectionTitle("Sinopse", "book", g.primary))
            val syn = label(d.synopsis, 14f, Palette.text)
            syn.maxLines = 4
            syn.ellipsize = TextUtils.TruncateAt.END
            sc.addView(syn, lin(MATCH, WRAP, t = 8))
            if (d.synopsis.length > 140) {
                var expanded = false
                val more = label("Ler mais", 12f, g.primary, true)
                more.setOnClickListener {
                    expanded = !expanded
                    syn.maxLines = if (expanded) 100 else 4
                    more.text = if (expanded) "Ler menos" else "Ler mais"
                }
                sc.addView(more, lin(WRAP, WRAP, t = 6))
            }
            col.addView(sc, lin(MATCH, WRAP, t = 12))
        }

        // ---- informações
        val ic = card(14, 22)
        ic.addView(sectionTitle("Informações", "tag", g.primary))
        if (d.platform.isNotBlank()) ic.addView(infoRow("tv", "Onde assistir", d.platform, g.primary))
        if (d.year.isNotBlank()) ic.addView(infoRow("calendar", "Ano de lançamento", d.year, g.primary))
        if (d.cast.isNotBlank()) ic.addView(infoRow("person", "Elenco", d.cast, g.primary))
        if (d.couple.isNotBlank()) ic.addView(infoRow("heart", "Casal favorito", d.couple, g.primary))
        if (d.startDate > 0L) ic.addView(infoRow("play", "Comecei em", fmt(d.startDate), g.primary))
        if (d.endDate > 0L) ic.addView(infoRow("check", "Terminei em", fmt(d.endDate), g.primary))
        ic.addView(infoRow("calendar", "Adicionado em", fmt(d.addedAt), g.primary))
        col.addView(ic, lin(MATCH, WRAP, t = 12))

        // ---- resenha
        if (d.notes.isNotBlank()) {
            val nc = card(14, 22)
            nc.addView(sectionTitle("Minha resenha", "edit", g.primary))
            nc.addView(label(d.notes, 14f, Palette.text), lin(MATCH, WRAP, t = 8))
            col.addView(nc, lin(MATCH, WRAP, t = 12))
        }

        // ---- excluir
        val del = pill("Excluir dorama", Color.parseColor("#FFE0E6"), Color.parseColor("#C2185B"), 14f, "delete")
        del.setPadding(dp(18), dp(12), dp(18), dp(12))
        del.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Excluir dorama?")
                .setMessage("\"" + d.title + "\" será removido da sua estante.")
                .setPositiveButton("Excluir") { _, _ ->
                    Store.delete(d.id)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        col.addView(del, lin(MATCH, WRAP, t = 18))

        root.addView(PetalsView(this, g.petals, g.primary, 14), FrameLayout.LayoutParams(MATCH, MATCH))

        setContentView(root)
        scroll = sv
        if (keep > 0) sv.post { sv.scrollTo(0, keep) }
    }

    private fun tile(icon: String, value: String, name: String, color: Int): LinearLayout {
        val b = card(10, 18)
        b.gravity = Gravity.CENTER_HORIZONTAL
        b.addView(IconView(this, icon, color, 18))
        val v = label(value, 14f, Palette.text, true, true)
        v.maxLines = 1
        b.addView(v, lin(WRAP, WRAP, t = 4))
        val n = label(name, 10f, Palette.muted)
        n.maxLines = 1
        b.addView(n, lin(WRAP, WRAP, t = 1))
        return b
    }

    private fun infoRow(icon: String, name: String, value: String, color: Int): View {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.setPadding(0, dp(10), 0, 0)
        r.addView(IconView(this, icon, color, 18), lin(WRAP, WRAP, t = 2))
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.addView(label(name, 11f, Palette.muted, true))
        c.addView(label(value, 14f, Palette.text), lin(MATCH, WRAP, t = 1))
        r.addView(c, lin(0, WRAP, 1f, l = 10))
        return r
    }
}
