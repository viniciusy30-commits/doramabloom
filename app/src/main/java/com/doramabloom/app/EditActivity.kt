package com.doramabloom.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class EditActivity : AppCompatActivity() {

    private var id = -1L
    private var existing: Drama? = null
    private var coverPath = ""
    private var originalCover = ""
    private var genreKey = "romance"
    private var statusKey = "quero"
    private var country = countries[0]
    private var rating = 0
    private var favorite = false
    private var saved = false

    private lateinit var coverView: CoverView
    private lateinit var heartsRow: LinearLayout
    private lateinit var titleIn: EditText
    private lateinit var platformIn: EditText
    private lateinit var yearIn: EditText
    private lateinit var epWIn: EditText
    private lateinit var epTIn: EditText
    private lateinit var castIn: EditText
    private lateinit var notesIn: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        id = intent.getLongExtra("id", -1L)
        existing = Store.get(id)
        val ex = existing
        if (ex != null) {
            coverPath = ex.cover
            originalCover = ex.cover
            genreKey = ex.genre
            statusKey = ex.status
            country = ex.country
            rating = ex.rating
            favorite = ex.favorite
        }

        val root = FrameLayout(this)
        root.background = gradient(Palette.bgTop, Palette.bgBottom)
        val page = LinearLayout(this)
        page.orientation = LinearLayout.VERTICAL
        root.addView(page, FrameLayout.LayoutParams(MATCH, MATCH))

        // cabeçalho
        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        head.setPadding(dp(16), dp(10), dp(16), dp(6))
        val back = label("←", 30f, Palette.pink, true)
        back.setPadding(dp(4), dp(4), dp(16), dp(4))
        back.setOnClickListener { finish() }
        head.addView(back)
        head.addView(
            label(if (ex == null) "Novo dorama 🌸" else "Editar dorama ✏️", 22f, Palette.pink, true, true)
        )
        page.addView(head, lin(MATCH, WRAP))

        val sv = ScrollView(this)
        sv.isVerticalScrollBarEnabled = false
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(18), dp(6), dp(18), dp(24))
        sv.addView(col)
        page.addView(sv, lin(MATCH, 0, 1f))

        // capa
        val coverRow = LinearLayout(this)
        coverRow.orientation = LinearLayout.HORIZONTAL
        coverRow.gravity = Gravity.CENTER_VERTICAL
        coverView = CoverView(this, 20)
        coverView.elevation = dp(4).toFloat()
        coverRow.addView(coverView, lin(dp(110), dp(158), r = 16))
        val coverBtns = LinearLayout(this)
        coverBtns.orientation = LinearLayout.VERTICAL
        val pick = pill("📷 Escolher capa", Palette.pink, Color.WHITE, 13f)
        pick.setOnClickListener { pickCover() }
        val clear = pill("Remover capa", Color.WHITE, Palette.pink, 12f)
        clear.setOnClickListener {
            if (coverPath.isNotEmpty() && coverPath != originalCover) File(coverPath).delete()
            coverPath = ""
            refreshCover()
        }
        coverBtns.addView(pick, lin(WRAP, WRAP))
        coverBtns.addView(clear, lin(WRAP, WRAP, t = 8))
        coverBtns.addView(label("Escolha uma imagem da sua galeria", 11f, Palette.muted), lin(WRAP, WRAP, t = 8))
        coverRow.addView(coverBtns, lin(0, WRAP, 1f))
        col.addView(coverRow, lin(MATCH, WRAP))

        // título
        col.addView(sectionLabel("Título *"))
        titleIn = input("Ex.: Pousando no Amor", ex?.title ?: "", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        col.addView(titleIn, lin(MATCH, WRAP))

        // país
        col.addView(sectionLabel("País 🌏"))
        val cOpts = ArrayList<Triple<String, String, Int>>()
        for (c in countries) cOpts.add(Triple(c, c, Palette.pink))
        col.addView(chipScroller(cOpts, country) { country = it }, lin(MATCH, WRAP))

        // gênero
        col.addView(sectionLabel("Gênero 🎀"))
        val gOpts = ArrayList<Triple<String, String, Int>>()
        for (g in Genres.all) gOpts.add(Triple(g.key, g.emoji + " " + g.label, g.primary))
        col.addView(chipScroller(gOpts, genreKey) {
            genreKey = it
            refreshCover()
        }, lin(MATCH, WRAP))

        // status
        col.addView(sectionLabel("Status 📺"))
        val sOpts = ArrayList<Triple<String, String, Int>>()
        for (s in Statuses.all) sOpts.add(Triple(s.key, s.emoji + " " + s.label, s.color))
        col.addView(chipScroller(sOpts, statusKey) { statusKey = it }, lin(MATCH, WRAP))

        // episódios
        col.addView(sectionLabel("Episódios"))
        val epRow = LinearLayout(this)
        epRow.orientation = LinearLayout.HORIZONTAL
        epWIn = input("Assistidos", if (ex != null && ex.epWatched > 0) ex.epWatched.toString() else "", InputType.TYPE_CLASS_NUMBER)
        epTIn = input("Total", if (ex != null && ex.epTotal > 0) ex.epTotal.toString() else "", InputType.TYPE_CLASS_NUMBER)
        epRow.addView(epWIn, lin(0, WRAP, 1f, r = 8))
        epRow.addView(epTIn, lin(0, WRAP, 1f))
        col.addView(epRow, lin(MATCH, WRAP))

        // nota
        col.addView(sectionLabel("Minha nota 💗"))
        heartsRow = LinearLayout(this)
        heartsRow.orientation = LinearLayout.HORIZONTAL
        col.addView(heartsRow, lin(WRAP, WRAP))
        drawHearts()

        // plataforma e ano
        col.addView(sectionLabel("Onde assistir e ano"))
        val pyRow = LinearLayout(this)
        pyRow.orientation = LinearLayout.HORIZONTAL
        platformIn = input("Netflix, Viki...", ex?.platform ?: "", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        yearIn = input("Ano", ex?.year ?: "", InputType.TYPE_CLASS_NUMBER)
        pyRow.addView(platformIn, lin(0, WRAP, 2f, r = 8))
        pyRow.addView(yearIn, lin(0, WRAP, 1f))
        col.addView(pyRow, lin(MATCH, WRAP))

        // elenco
        col.addView(sectionLabel("Elenco 🎭"))
        castIn = input("Atores e atrizes favoritos", ex?.cast ?: "", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        col.addView(castIn, lin(MATCH, WRAP))

        // resenha
        col.addView(sectionLabel("Minha resenha 💌"))
        notesIn = input("O que você achou? Frases e cenas favoritas...", ex?.notes ?: "", InputType.TYPE_CLASS_TEXT, true)
        col.addView(notesIn, lin(MATCH, WRAP))

        // favorito
        val favRow = LinearLayout(this)
        favRow.orientation = LinearLayout.HORIZONTAL
        favRow.gravity = Gravity.CENTER_VERTICAL
        val favTv = label(if (favorite) "💖 Nos favoritos" else "🤍 Marcar como favorito", 14f, Palette.text, true)
        favTv.setOnClickListener {
            favorite = !favorite
            favTv.text = if (favorite) "💖 Nos favoritos" else "🤍 Marcar como favorito"
        }
        favTv.setPadding(0, dp(12), 0, dp(12))
        favRow.addView(favTv)
        col.addView(favRow, lin(MATCH, WRAP, t = 8))

        // botão salvar fixo
        val saveBtn = label("Salvar 🌸", 18f, Color.WHITE, true, true)
        saveBtn.gravity = Gravity.CENTER
        saveBtn.setPadding(dp(16), dp(14), dp(16), dp(14))
        saveBtn.background = gradient(
            Color.parseColor("#FF8FB7"), Color.parseColor("#FF5C93"),
            dp(28).toFloat(), android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT
        )
        saveBtn.elevation = dp(6).toFloat()
        saveBtn.setOnClickListener { save() }
        page.addView(saveBtn, lin(MATCH, WRAP, l = 16, t = 6, r = 16, b = 14))

        setContentView(root)
        refreshCover()
    }

    private fun sectionLabel(t: String): TextView {
        val v = label(t, 14f, Palette.text, true, true)
        v.setPadding(dp(4), dp(16), 0, dp(6))
        return v
    }

    private fun refreshCover() {
        coverView.bind(coverPath, genreKey, 400, 40f)
    }

    private fun drawHearts() {
        heartsRow.removeAllViews()
        for (i in 1..5) {
            val h = label(if (i <= rating) "♥" else "♡", 36f, Palette.pink, true)
            h.setPadding(0, 0, dp(10), 0)
            h.setOnClickListener {
                rating = if (rating == i) 0 else i
                drawHearts()
            }
            heartsRow.addView(h)
        }
    }

    private fun pickCover() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "image/*"
        i.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(i, "Escolher capa"), 101)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == android.app.Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                val p = Store.saveCover(uri)
                if (p != null) {
                    if (coverPath.isNotEmpty() && coverPath != originalCover) File(coverPath).delete()
                    coverPath = p
                    refreshCover()
                } else {
                    Toast.makeText(this, "Não consegui abrir essa imagem 🥺", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun save() {
        val title = titleIn.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, "Dê um nome ao dorama 🌸", Toast.LENGTH_SHORT).show()
            titleIn.requestFocus()
            return
        }
        var ew = epWIn.text.toString().toIntOrNull() ?: 0
        val et = epTIn.text.toString().toIntOrNull() ?: 0
        if (et > 0 && ew > et) ew = et
        var status = statusKey
        if (et > 0 && ew >= et && status == "assistindo") status = "concluido"
        if (status == "concluido" && et > 0) ew = et
        if (ew > 0 && status == "quero") status = "assistindo"

        val old = existing
        val d = Drama(
            id = if (old != null) old.id else System.currentTimeMillis(),
            title = title,
            country = country,
            genre = genreKey,
            status = status,
            rating = rating,
            epWatched = ew,
            epTotal = et,
            year = yearIn.text.toString().trim(),
            platform = platformIn.text.toString().trim(),
            cast = castIn.text.toString().trim(),
            notes = notesIn.text.toString().trim(),
            favorite = favorite,
            cover = coverPath,
            addedAt = if (old != null) old.addedAt else System.currentTimeMillis()
        )
        if (originalCover.isNotEmpty() && originalCover != coverPath) {
            try {
                File(originalCover).delete()
            } catch (e: Exception) {
            }
            Covers.clear()
        }
        saved = true
        Store.save(d)
        Toast.makeText(this, "Salvo com carinho 🌸", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!saved && coverPath.isNotEmpty() && coverPath != originalCover) {
            try {
                File(coverPath).delete()
            } catch (e: Exception) {
            }
        }
    }
}
