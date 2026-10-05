package com.doramabloom.app

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditActivity : AppCompatActivity() {

    private var id = -1L
    private var existing: Drama? = null
    private var coverPath = ""
    private var originalCover = ""
    private var genreKey = "romance"
    private var tags = HashSet<String>()
    private var statusKey = "quero"
    private var country = countries[0]
    private var score = 0
    private var favorite = false
    private var rewatch = 0
    private var startDate = 0L
    private var endDate = 0L
    private var seasonTotals = ArrayList<Int>()
    private var seasonWatched = ArrayList<Int>()
    private var saved = false

    private lateinit var coverView: CoverView
    private lateinit var coverSeal: SealView
    private lateinit var genreBox: LinearLayout
    private lateinit var genrePreview: LinearLayout
    private var petalsView: PetalsView? = null
    private lateinit var seasonBox: LinearLayout
    private lateinit var seasonCountTv: TextView
    private lateinit var rewatchTv: TextView
    private lateinit var scoreTv: TextView
    private lateinit var startBtn: TextView
    private lateinit var endBtn: TextView
    private lateinit var titleIn: EditText
    private lateinit var originalIn: EditText
    private lateinit var synopsisIn: EditText
    private lateinit var minutesIn: EditText
    private lateinit var platformIn: EditText
    private lateinit var yearIn: EditText
    private lateinit var castIn: EditText
    private lateinit var coupleIn: EditText
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
            tags = HashSet(ex.tags)
            statusKey = ex.status
            country = ex.country
            score = ex.score
            favorite = ex.favorite
            rewatch = ex.rewatch
            startDate = ex.startDate
            endDate = ex.endDate
            seasonTotals = ArrayList(ex.seasonEps)
            seasonWatched = ArrayList(ex.watched)
        }
        if (seasonTotals.isEmpty()) {
            seasonTotals.add(0)
            seasonWatched.add(0)
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
        head.addView(roundBtn("back", Palette.pinkDark, false, 18) { finish() }, lin(dp(40), dp(40), r = 12))
        head.addView(label(if (ex == null) "Novo dorama" else "Editar dorama", 22f, Palette.pinkDark, true, true))
        page.addView(head, lin(MATCH, WRAP))

        val sv = ScrollView(this)
        sv.isVerticalScrollBarEnabled = false
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(6), dp(16), dp(24))
        sv.addView(col)
        page.addView(sv, lin(MATCH, 0, 1f))

        // ---------------- capa e título
        val c1 = card(14, 24)
        c1.addView(sectionTitle("Capa e título", "image"))
        val coverRow = LinearLayout(this)
        coverRow.orientation = LinearLayout.HORIZONTAL
        coverRow.gravity = Gravity.CENTER_VERTICAL
        coverView = CoverView(this, 20)
        coverView.elevation = 0f
        val coverFrame = FrameLayout(this)
        coverFrame.addView(coverView, FrameLayout.LayoutParams(MATCH, MATCH))
        coverSeal = SealView(this)
        val csl = FrameLayout.LayoutParams(dp(36), dp(36), Gravity.TOP or Gravity.START)
        csl.setMargins(dp(5), dp(5), 0, 0)
        coverFrame.addView(coverSeal, csl)
        coverRow.addView(coverFrame, lin(dp(104), dp(150), r = 16))
        val coverBtns = LinearLayout(this)
        coverBtns.orientation = LinearLayout.VERTICAL
        val pick = pill("Escolher capa", Palette.pink, Color.WHITE, 13f, "image")
        pick.setOnClickListener { pickCover() }
        val clear = pill("Remover capa", Color.WHITE, Palette.pink, 12f, "close")
        clear.background = roundRect(Color.WHITE, dp(20).toFloat(), Palette.pink, dp(1))
        clear.setOnClickListener {
            if (coverPath.isNotEmpty() && coverPath != originalCover) File(coverPath).delete()
            coverPath = ""
            refreshCover()
        }
        coverBtns.addView(pick, lin(WRAP, WRAP))
        coverBtns.addView(clear, lin(WRAP, WRAP, t = 8))
        coverBtns.addView(label("Use uma imagem da sua galeria", 11f, Palette.muted), lin(WRAP, WRAP, t = 8))
        coverRow.addView(coverBtns, lin(0, WRAP, 1f))
        c1.addView(coverRow, lin(MATCH, WRAP, t = 10))
        c1.addView(fieldLabel("Título *"))
        titleIn = input("Ex.: Pousando no Amor", ex?.title ?: (intent.getStringExtra("prefill") ?: ""), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        c1.addView(titleIn, lin(MATCH, WRAP))
        c1.addView(fieldLabel("Título original"))
        originalIn = input("Ex.: 사랑의 불시착", ex?.original ?: "")
        c1.addView(originalIn, lin(MATCH, WRAP))
        col.addView(c1, lin(MATCH, WRAP, t = 8))

        // ---------------- classificação
        val c2 = card(14, 24)
        c2.addView(sectionTitle("Classificação", "tag"))
        c2.addView(fieldLabel("País"))
        val cOpts = ArrayList<Opt>()
        for (c in countries) cOpts.add(Opt(c, c, Atmosphere.country(c).second, "flag"))
        c2.addView(chipScroller(cOpts, country) {
            country = it
            updateEffects()
        }, lin(MATCH, WRAP))
        genreBox = LinearLayout(this)
        genreBox.orientation = LinearLayout.VERTICAL
        c2.addView(genreBox, lin(MATCH, WRAP))
        buildGenreArea()
        c2.addView(fieldLabel("Status"))
        val sOpts = ArrayList<Opt>()
        for (s in Statuses.all) sOpts.add(Opt(s.key, s.label, s.color, s.icon))
        c2.addView(chipScroller(sOpts, statusKey) {
            statusKey = it
            updateEffects()
        }, lin(MATCH, WRAP))
        col.addView(c2, lin(MATCH, WRAP, t = 12))

        // ---------------- temporadas
        val c3 = card(14, 24)
        c3.addView(sectionTitle("Temporadas e episódios", "tv"))
        val stepRow = LinearLayout(this)
        stepRow.orientation = LinearLayout.HORIZONTAL
        stepRow.gravity = Gravity.CENTER_VERTICAL
        stepRow.addView(label("Quantidade de temporadas", 13f, Palette.text, true), lin(0, WRAP, 1f))
        seasonCountTv = label("1", 16f, Palette.pinkDark, true)
        seasonCountTv.gravity = Gravity.CENTER
        stepRow.addView(roundBtn("minus", Palette.pink, false, 14) {
            if (seasonTotals.size > 1) {
                seasonTotals.removeAt(seasonTotals.size - 1)
                seasonWatched.removeAt(seasonWatched.size - 1)
                rebuildSeasons()
            }
        }, lin(dp(32), dp(32)))
        stepRow.addView(seasonCountTv, lin(dp(36), WRAP))
        stepRow.addView(roundBtn("add", Palette.pink, true, 14) {
            if (seasonTotals.size < 30) {
                seasonTotals.add(0)
                seasonWatched.add(0)
                rebuildSeasons()
            }
        }, lin(dp(32), dp(32)))
        c3.addView(stepRow, lin(MATCH, WRAP, t = 10))
        seasonBox = LinearLayout(this)
        seasonBox.orientation = LinearLayout.VERTICAL
        c3.addView(seasonBox, lin(MATCH, WRAP, t = 4))
        c3.addView(fieldLabel("Duração de cada episódio (minutos)"))
        minutesIn = input("Ex.: 60", if (ex != null && ex.epMinutes > 0) ex.epMinutes.toString() else "", InputType.TYPE_CLASS_NUMBER)
        c3.addView(minutesIn, lin(MATCH, WRAP))
        col.addView(c3, lin(MATCH, WRAP, t = 12))
        rebuildSeasons()

        // ---------------- nota
        val c4 = card(14, 24)
        c4.addView(sectionTitle("Minha nota", "heart"))
        val rating = RatingView(this, 36, true)
        rating.score = score
        scoreTv = label(scoreText(), 15f, Palette.pinkDark, true, true)
        rating.onChange = { ns ->
            score = ns
            scoreTv.text = scoreText()
        }
        c4.addView(rating, lin(WRAP, WRAP, t = 10))
        c4.addView(scoreTv, lin(WRAP, WRAP, t = 6))
        val favTv = pill(
            if (favorite) "Nos favoritos" else "Marcar como favorito",
            if (favorite) Palette.pink else Color.WHITE,
            if (favorite) Color.WHITE else Palette.pink,
            13f, "heart"
        )
        favTv.setOnClickListener {
            favorite = !favorite
            favTv.text = if (favorite) "Nos favoritos" else "Marcar como favorito"
            favTv.background = roundRect(if (favorite) Palette.pink else Color.WHITE, dp(20).toFloat(), Palette.pink, dp(1))
            favTv.setTextColor(if (favorite) Color.WHITE else Palette.pink)
            val dr = favTv.compoundDrawables[0]
            if (dr is IconDrawable) dr.color = if (favorite) Color.WHITE else Palette.pink
        }
        favTv.background = roundRect(if (favorite) Palette.pink else Color.WHITE, dp(20).toFloat(), Palette.pink, dp(1))
        c4.addView(favTv, lin(WRAP, WRAP, t = 12))

        val rwRow = LinearLayout(this)
        rwRow.orientation = LinearLayout.HORIZONTAL
        rwRow.gravity = Gravity.CENTER_VERTICAL
        rwRow.addView(label("Vezes que reassisti", 13f, Palette.text, true), lin(0, WRAP, 1f))
        rewatchTv = label(rewatch.toString(), 16f, Palette.pinkDark, true)
        rewatchTv.gravity = Gravity.CENTER
        rwRow.addView(roundBtn("minus", Palette.pink, false, 14) {
            if (rewatch > 0) {
                rewatch -= 1
                rewatchTv.text = rewatch.toString()
            }
        }, lin(dp(32), dp(32)))
        rwRow.addView(rewatchTv, lin(dp(36), WRAP))
        rwRow.addView(roundBtn("add", Palette.pink, true, 14) {
            rewatch += 1
            rewatchTv.text = rewatch.toString()
        }, lin(dp(32), dp(32)))
        c4.addView(rwRow, lin(MATCH, WRAP, t = 14))
        col.addView(c4, lin(MATCH, WRAP, t = 12))

        // ---------------- detalhes
        val c5 = card(14, 24)
        c5.addView(sectionTitle("Detalhes", "calendar"))
        c5.addView(fieldLabel("Onde assistir e ano"))
        val pyRow = LinearLayout(this)
        pyRow.orientation = LinearLayout.HORIZONTAL
        platformIn = input("Netflix, Viki...", ex?.platform ?: "", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        yearIn = input("Ano", ex?.year ?: "", InputType.TYPE_CLASS_NUMBER)
        pyRow.addView(platformIn, lin(0, WRAP, 2f, r = 8))
        pyRow.addView(yearIn, lin(0, WRAP, 1f))
        c5.addView(pyRow, lin(MATCH, WRAP))
        c5.addView(fieldLabel("Datas (segure para limpar)"))
        val dRow = LinearLayout(this)
        dRow.orientation = LinearLayout.HORIZONTAL
        startBtn = pill("", Color.WHITE, Palette.pink, 12f, "play")
        endBtn = pill("", Color.WHITE, Palette.pink, 12f, "check")
        startBtn.setOnClickListener {
            pickDate(startDate) {
                startDate = it
                refreshDates()
            }
        }
        startBtn.setOnLongClickListener {
            startDate = 0L
            refreshDates()
            true
        }
        endBtn.setOnClickListener {
            pickDate(endDate) {
                endDate = it
                refreshDates()
            }
        }
        endBtn.setOnLongClickListener {
            endDate = 0L
            refreshDates()
            true
        }
        dRow.addView(startBtn, lin(WRAP, WRAP, r = 8))
        dRow.addView(endBtn, lin(WRAP, WRAP))
        c5.addView(dRow, lin(MATCH, WRAP))
        refreshDates()
        col.addView(c5, lin(MATCH, WRAP, t = 12))

        // ---------------- elenco
        val c6 = card(14, 24)
        c6.addView(sectionTitle("Elenco e casal", "person"))
        c6.addView(fieldLabel("Elenco"))
        castIn = input("Atores e atrizes favoritos", ex?.cast ?: "", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        c6.addView(castIn, lin(MATCH, WRAP))
        c6.addView(fieldLabel("Casal favorito"))
        coupleIn = input("Quem formou o casal que você shippa?", ex?.couple ?: "", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        c6.addView(coupleIn, lin(MATCH, WRAP))
        col.addView(c6, lin(MATCH, WRAP, t = 12))

        // ---------------- textos
        val c7 = card(14, 24)
        c7.addView(sectionTitle("Sinopse e resenha", "book"))
        c7.addView(fieldLabel("Sinopse"))
        synopsisIn = input("Do que se trata?", ex?.synopsis ?: "", InputType.TYPE_CLASS_TEXT, true)
        c7.addView(synopsisIn, lin(MATCH, WRAP))
        c7.addView(fieldLabel("Minha resenha"))
        notesIn = input("O que você achou? Cenas e frases favoritas...", ex?.notes ?: "", InputType.TYPE_CLASS_TEXT, true)
        c7.addView(notesIn, lin(MATCH, WRAP))
        col.addView(c7, lin(MATCH, WRAP, t = 12))

        // ---------------- botão salvar fixo
        val saveBtn = label("Salvar", 18f, Color.WHITE, true, true)
        saveBtn.gravity = Gravity.CENTER
        saveBtn.setPadding(dp(16), dp(14), dp(16), dp(14))
        saveBtn.background = gradient(
            Color.parseColor("#FF8FB7"), Color.parseColor("#FF5C93"),
            dp(28).toFloat(), android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT
        )
        saveBtn.elevation = 0f
        saveBtn.setOnClickListener { save() }
        saveBtn.pressable(0.96f)
        page.addView(saveBtn, lin(MATCH, WRAP, l = 16, t = 6, r = 16, b = 14))

        val pv = PetalsView(this, listOf("petal", "petal", "blossom"), Palette.pink, 12)
        petalsView = pv
        root.addView(pv, FrameLayout.LayoutParams(MATCH, MATCH))

        setContentView(root)
        refreshCover()
        updateEffects()

        head.riseIn(0L, 12, 320L)
        for (i in 0 until col.childCount) col.getChildAt(i).riseIn(90L + minOf(i, 7) * 60L, 22, 420L)
        saveBtn.riseIn(400L, 30, 420L)
    }

    /** Gênero principal, cartão do gênero escolhido e "outros gêneros"; refeito quando você cria um gênero novo. */
    private fun buildGenreArea() {
        genreBox.removeAllViews()
        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        head.addView(fieldLabel("Gênero principal (define o tema do dorama)"), lin(0, WRAP, 1f))
        val newG = pill("Novo", Palette.pinkSoft, Palette.pinkDark, 11f, "add")
        newG.setOnClickListener {
            showGenreCreator { g ->
                genreKey = g.key
                buildGenreArea()
                refreshCover()
                updateEffects()
            }
        }
        head.addView(newG, lin(WRAP, WRAP, t = 8))
        genreBox.addView(head, lin(MATCH, WRAP))

        val gOpts = ArrayList<Opt>()
        for (g in Genres.all) gOpts.add(Opt(g.key, g.label, g.primary, g.icon))
        genreBox.addView(chipScroller(gOpts, genreKey) {
            genreKey = it
            refreshCover()
            coverView.pop(1.25f)
            updateEffects()
            refreshGenrePreview(true)
        }, lin(MATCH, WRAP))

        genrePreview = LinearLayout(this)
        genrePreview.orientation = LinearLayout.HORIZONTAL
        genrePreview.gravity = Gravity.CENTER_VERTICAL
        genreBox.addView(genrePreview, lin(MATCH, WRAP, t = 10))
        refreshGenrePreview(false)

        genreBox.addView(fieldLabel("Outros gêneros"))
        genreBox.addView(multiChips(gOpts, tags) { tags = HashSet(it) }, lin(MATCH, WRAP))
    }

    private fun refreshGenrePreview(animate: Boolean) {
        val g = Genres.byKey(genreKey)
        val box = genrePreview
        box.removeAllViews()
        box.setPadding(dp(12), dp(10), dp(14), dp(10))
        box.background = roundRect(g.soft, dp(20).toFloat(), mixColor(g.primary, Color.WHITE, 0.5f), dp(1))
        val seal = SealView(this)
        seal.set(g)
        box.addView(seal, lin(dp(50), dp(50), r = 12))
        val tx = LinearLayout(this)
        tx.orientation = LinearLayout.VERTICAL
        tx.addView(label(g.label, 16f, g.dark, true, true))
        tx.addView(label(g.tagline, 12f, Palette.muted), lin(MATCH, WRAP, t = 1))
        box.addView(tx, lin(0, WRAP, 1f))
        if (animate) seal.pop(1.4f)
    }

    /** O tema (pétalas e cor) acompanha o gênero, o status e o país escolhidos. */
    private fun updateEffects() {
        val pv = petalsView ?: return
        val a = Atmosphere.of(genreKey, statusKey, country)
        pv.setTheme(a.icons, a.tints)
        window.statusBarColor = Palette.bgTop
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.screen_back_in, R.anim.screen_back_out)
    }

    private fun fieldLabel(t: String): TextView {
        val v = label(t, 12.5f, Palette.muted, true)
        v.setPadding(dp(2), dp(14), 0, dp(6))
        return v
    }

    private fun scoreText(): String = if (score <= 0) "Sem nota ainda" else "$score / 10"

    private fun fmt(ms: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(ms))

    private fun refreshDates() {
        startBtn.text = "Início: " + (if (startDate > 0L) fmt(startDate) else "escolher")
        endBtn.text = "Fim: " + (if (endDate > 0L) fmt(endDate) else "escolher")
    }

    private fun pickDate(current: Long, onPick: (Long) -> Unit) {
        val cal = Calendar.getInstance()
        if (current > 0L) cal.timeInMillis = current
        DatePickerDialog(
            this,
            { _, y, m, dd ->
                val c = Calendar.getInstance()
                c.set(y, m, dd, 12, 0, 0)
                onPick(c.timeInMillis)
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun rebuildSeasons() {
        seasonBox.removeAllViews()
        seasonCountTv.text = seasonTotals.size.toString()
        for (i in seasonTotals.indices) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.addView(label("Temp. " + (i + 1), 13f, Palette.text, true), lin(dp(62), WRAP))
            val w = input(
                "Vistos",
                if (seasonWatched[i] > 0) seasonWatched[i].toString() else "",
                InputType.TYPE_CLASS_NUMBER
            )
            val t = input(
                "Total de eps",
                if (seasonTotals[i] > 0) seasonTotals[i].toString() else "",
                InputType.TYPE_CLASS_NUMBER
            )
            w.doAfterTextChanged { seasonWatched[i] = it?.toString()?.toIntOrNull() ?: 0 }
            t.doAfterTextChanged { seasonTotals[i] = it?.toString()?.toIntOrNull() ?: 0 }
            row.addView(w, lin(0, WRAP, 1f, r = 6))
            row.addView(t, lin(0, WRAP, 1f))
            seasonBox.addView(row, lin(MATCH, WRAP, t = 8))
        }
    }

    private fun refreshCover() {
        coverView.bind(coverPath, genreKey, 400)
        coverSeal.set(Genres.byKey(genreKey))
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
                    Toast.makeText(this, "Não consegui abrir essa imagem.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun save() {
        val title = titleIn.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, "Dê um nome ao dorama.", Toast.LENGTH_SHORT).show()
            titleIn.requestFocus()
            return
        }
        val old = existing
        val d = Drama(
            id = if (old != null) old.id else System.currentTimeMillis(),
            title = title,
            original = originalIn.text.toString().trim(),
            synopsis = synopsisIn.text.toString().trim(),
            country = country,
            genre = genreKey,
            tags = tags.filter { it != genreKey },
            status = statusKey,
            score = score,
            seasonEps = ArrayList(seasonTotals),
            watched = ArrayList(seasonWatched),
            epMinutes = minutesIn.text.toString().toIntOrNull() ?: 0,
            year = yearIn.text.toString().trim(),
            platform = platformIn.text.toString().trim(),
            cast = castIn.text.toString().trim(),
            couple = coupleIn.text.toString().trim(),
            startDate = startDate,
            endDate = endDate,
            rewatch = rewatch,
            notes = notesIn.text.toString().trim(),
            favorite = favorite,
            cover = coverPath,
            addedAt = if (old != null) old.addedAt else System.currentTimeMillis(),
            link = old?.link ?: "",
            lastUrl = old?.lastUrl ?: "",
            watchSeason = old?.watchSeason ?: -1
        )
        normalize(d)
        val tot = totalEps(d)
        val wat = watchedEps(d)
        var st = statusKey
        if (tot > 0 && d.seasonEps.all { it > 0 } && wat >= tot && st == "assistindo") st = "concluido"
        if (wat > 0 && st == "quero") st = "assistindo"
        applyStatus(d, st)

        if (originalCover.isNotEmpty() && originalCover != coverPath) {
            try {
                File(originalCover).delete()
            } catch (e: Exception) {
            }
            Covers.clear()
        }
        saved = true
        Store.save(d)
        Toast.makeText(this, "Salvo com carinho!", Toast.LENGTH_SHORT).show()
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
