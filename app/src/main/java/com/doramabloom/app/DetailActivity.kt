package com.doramabloom.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
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
        col.setPadding(dp(18), dp(10), dp(18), dp(40))
        sv.addView(col)
        root.addView(sv, FrameLayout.LayoutParams(MATCH, MATCH))

        // barra superior
        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        val back = label("←", 30f, g.dark, true)
        back.setPadding(dp(4), dp(4), dp(16), dp(4))
        back.setOnClickListener { finish() }
        top.addView(back)
        top.addView(View(this), lin(0, dp(1), 1f))
        val fav = label(if (d.favorite) "💖" else "🤍", 28f)
        fav.setPadding(dp(16), dp(4), dp(4), dp(4))
        fav.setOnClickListener {
            d.favorite = !d.favorite
            Store.save(d)
            render()
        }
        top.addView(fav)
        col.addView(top, lin(MATCH, WRAP))

        // capa
        val cv = CoverView(this, 26)
        cv.bind(d, 700, 72f)
        cv.elevation = dp(8).toFloat()
        val clp = lin(dp(190), dp(272), t = 6)
        clp.gravity = Gravity.CENTER_HORIZONTAL
        col.addView(cv, clp)

        // título e chips
        val title = label(d.title, 26f, g.dark, true, true)
        title.gravity = Gravity.CENTER
        col.addView(title, lin(MATCH, WRAP, t = 16))

        val chips = LinearLayout(this)
        chips.orientation = LinearLayout.HORIZONTAL
        chips.gravity = Gravity.CENTER
        chips.addView(pill(g.emoji + " " + g.label, g.primary, Color.WHITE, 12f), lin(WRAP, WRAP, r = 6))
        chips.addView(pill(d.country, Color.WHITE, g.dark, 12f), lin(WRAP, WRAP))
        col.addView(chips, lin(MATCH, WRAP, t = 8))

        val tag = label(g.tagline, 12f, g.dark)
        tag.gravity = Gravity.CENTER
        col.addView(tag, lin(MATCH, WRAP, t = 8))

        // status
        val stCard = card(14, 22)
        stCard.addView(label("Status", 15f, g.dark, true, true))
        val stOpts = ArrayList<Triple<String, String, Int>>()
        for (s in Statuses.all) stOpts.add(Triple(s.key, s.emoji + " " + s.label, s.color))
        stCard.addView(chipScroller(stOpts, d.status) { key ->
            d.status = key
            if (key == "concluido" && d.epTotal > 0) d.epWatched = d.epTotal
            Store.save(d)
            render()
        }, lin(MATCH, WRAP, t = 8))
        col.addView(stCard, lin(MATCH, WRAP, t = 16))

        // progresso
        val pc = card(14, 22)
        pc.addView(label("Meu progresso 📺", 15f, g.dark, true, true))
        pc.addView(label(progressText(d), 14f, Palette.text), lin(WRAP, WRAP, t = 6))
        val bar = SoftBar(this)
        bar.progress = progressOf(d)
        bar.barColor = g.primary
        pc.addView(bar, lin(MATCH, dp(10), t = 6))
        val prow = LinearLayout(this)
        prow.orientation = LinearLayout.HORIZONTAL
        prow.gravity = Gravity.CENTER_VERTICAL
        val minus = pill("−", Color.WHITE, g.primary, 18f)
        minus.background = roundRect(Color.WHITE, dp(22).toFloat(), g.primary, dp(2))
        minus.setPadding(dp(22), dp(8), dp(22), dp(8))
        minus.setOnClickListener {
            if (d.epWatched > 0) {
                d.epWatched -= 1
                if (d.status == "concluido") d.status = "assistindo"
                Store.save(d)
                render()
            }
        }
        val plus = pill("+1 episódio 🌸", g.primary, Color.WHITE, 15f)
        plus.setPadding(dp(18), dp(12), dp(18), dp(12))
        plus.setOnClickListener {
            val finished = Store.bump(d)
            if (finished) {
                Toast.makeText(this, "Parabéns, você terminou! 🎉", Toast.LENGTH_LONG).show()
            }
            render()
        }
        prow.addView(minus, lin(WRAP, WRAP, r = 10))
        prow.addView(plus, lin(0, WRAP, 1f))
        pc.addView(prow, lin(MATCH, WRAP, t = 10))
        col.addView(pc, lin(MATCH, WRAP, t = 12))

        // nota
        val rc = card(14, 22)
        rc.addView(label("Minha nota 💗", 15f, g.dark, true, true))
        val hr = LinearLayout(this)
        hr.orientation = LinearLayout.HORIZONTAL
        for (i in 1..5) {
            val h = label(if (i <= d.rating) "♥" else "♡", 36f, g.primary, true)
            h.setPadding(0, 0, dp(10), 0)
            h.setOnClickListener {
                d.rating = if (d.rating == i) 0 else i
                Store.save(d)
                render()
            }
            hr.addView(h)
        }
        rc.addView(hr, lin(WRAP, WRAP, t = 4))
        col.addView(rc, lin(MATCH, WRAP, t = 12))

        // informações
        val ic = card(14, 22)
        ic.addView(label("Informações 🎀", 15f, g.dark, true, true))
        var rows = 0
        if (d.platform.isNotBlank()) {
            ic.addView(infoRow("🌐", "Onde assistir", d.platform)); rows++
        }
        if (d.year.isNotBlank()) {
            ic.addView(infoRow("📅", "Ano", d.year)); rows++
        }
        if (d.cast.isNotBlank()) {
            ic.addView(infoRow("🎭", "Elenco", d.cast)); rows++
        }
        val date = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(d.addedAt))
        ic.addView(infoRow("🗓️", "Adicionado em", date))
        col.addView(ic, lin(MATCH, WRAP, t = 12))

        // resenha
        if (d.notes.isNotBlank()) {
            val nc = card(14, 22)
            nc.addView(label("Minha resenha 💌", 15f, g.dark, true, true))
            nc.addView(label(d.notes, 14f, Palette.text), lin(MATCH, WRAP, t = 6))
            col.addView(nc, lin(MATCH, WRAP, t = 12))
        }

        // ações
        val actions = LinearLayout(this)
        actions.orientation = LinearLayout.HORIZONTAL
        val edit = pill("✏️ Editar", g.primary, Color.WHITE, 14f)
        edit.setPadding(dp(18), dp(12), dp(18), dp(12))
        edit.setOnClickListener {
            val i = Intent(this, EditActivity::class.java)
            i.putExtra("id", d.id)
            startActivity(i)
        }
        val del = pill("🗑️ Excluir", Color.parseColor("#FFE0E6"), Color.parseColor("#C2185B"), 14f)
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
        actions.addView(edit, lin(0, WRAP, 1f, r = 8))
        actions.addView(del, lin(0, WRAP, 1f))
        col.addView(actions, lin(MATCH, WRAP, t = 18))

        root.addView(PetalsView(this, g.petals, 14), FrameLayout.LayoutParams(MATCH, MATCH))

        setContentView(root)
        scroll = sv
        if (keep > 0) sv.post { sv.scrollTo(0, keep) }
    }

    private fun infoRow(emoji: String, name: String, value: String): View {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.VERTICAL
        r.setPadding(0, dp(8), 0, 0)
        r.addView(label(emoji + " " + name, 11f, Palette.muted, true))
        r.addView(label(value, 14f, Palette.text), lin(MATCH, WRAP, t = 1))
        return r
    }
}
