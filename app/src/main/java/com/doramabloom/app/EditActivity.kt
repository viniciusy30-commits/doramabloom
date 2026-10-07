package com.doramabloom.app

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
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
    private var soundPath = ""
    private var soundName = ""
    private var originalSound = ""
    private lateinit var soundLabel: TextView
    private var genreKey = "romance"
    private var tags = HashSet<String>()
    private val shelfPick = ArrayList<String>() // até 2 gêneros extras que aparecem na Estante
    private var shelfBox: LinearLayout? = null
    private var statusKey = "quero"
    private var kindKey = "serie"
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
    private var petalsView: PetalsView? = null
    private var favPill: TextView? = null
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
    private lateinit var linkIn: EditText
    private lateinit var yearIn: EditText
    // elenco com foto: cada pessoa tem nome e (opcional) foto; o casal tem nomes e uma foto
    private class PS(var name: String, var photo: String)
    private val peopleState = ArrayList<PS>()
    private val peopleEdits = ArrayList<EditText>()
    private lateinit var castBox: LinearLayout
    private lateinit var coupleBox: LinearLayout
    private var couplePhoto = ""
    private val originalPhotos = HashSet<String>()
    private var pickTarget = -2 // -1 = casal, 0 em diante = posição na lista do elenco
    private lateinit var coupleIn: EditText
    private lateinit var notesIn: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        ThemeMode.refresh(this)
        id = intent.getLongExtra("id", -1L)
        existing = Store.get(id)
        val ex = existing
        if (ex != null) {
            coverPath = ex.cover
            originalCover = ex.cover
            soundPath = ex.soundtrack
            soundName = ex.soundtrackName
            originalSound = ex.soundtrack
            for (p in ex.castPeople) {
                peopleState.add(PS(p.name, p.photo))
                if (p.photo.isNotEmpty()) originalPhotos.add(p.photo)
            }
            couplePhoto = ex.couplePhoto
            if (ex.couplePhoto.isNotEmpty()) originalPhotos.add(ex.couplePhoto)
            genreKey = ex.genre
            tags = HashSet(ex.tags)
            shelfPick.addAll(ex.shelfTags)
            statusKey = ex.status
            kindKey = ex.kind
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
        val clear = pill("Remover capa", Palette.card, Palette.pink, 12f, "close")
        clear.background = roundRect(Palette.card, dp(20).toFloat(), Palette.pink, dp(1))
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
        c2.addView(chipFlow(cOpts, country) {
            country = it
            updateEffects()
        }, lin(MATCH, WRAP))
        genreBox = LinearLayout(this)
        genreBox.orientation = LinearLayout.VERTICAL
        c2.addView(genreBox, lin(MATCH, WRAP))
        buildGenreArea()
        c2.addView(fieldLabel("Tipo"))
        val kOpts = ArrayList<Opt>()
        kOpts.add(Opt("serie", "Série", Color.parseColor("#6FA8FF"), "tv"))
        kOpts.add(Opt("filme", "Filme", Color.parseColor("#E0A93B"), "film"))
        c2.addView(chipFlow(kOpts, kindKey) { kindKey = it }, lin(MATCH, WRAP))
        c2.addView(fieldLabel("Status"))
        val sOpts = ArrayList<Opt>()
        for (s in Statuses.all) sOpts.add(Opt(s.key, s.label, s.color, s.icon))
        c2.addView(chipFlow(sOpts, statusKey) {
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
        val favTv = pill("Marcar como favorito", Palette.card, Palette.pink, 15f, "heart")
        favTv.setPadding(dp(18), dp(10), dp(20), dp(10))
        favPill = favTv
        favTv.setOnClickListener {
            favorite = !favorite
            styleFavPill()
            favTv.pop(1.3f)
        }
        styleFavPill()
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
        c5.addView(fieldLabel("Link de início (opcional)"))
        linkIn = input("https://...", ex?.link ?: "", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
        linkIn.setSingleLine(true)
        c5.addView(linkIn, lin(MATCH, WRAP))
        val linkHint = label("A página onde você assiste. Sem link, a aba abre no Google. Depois, ela sempre volta de onde você parou.", 11.5f, Palette.muted)
        linkHint.maxLines = 3
        c5.addView(linkHint, lin(MATCH, WRAP, t = 4, l = 4))
        c5.addView(fieldLabel("Datas (segure para limpar)"))
        val dRow = LinearLayout(this)
        dRow.orientation = LinearLayout.HORIZONTAL
        startBtn = pill("", Palette.card, Palette.pink, 12f, "play")
        endBtn = pill("", Palette.card, Palette.pink, 12f, "check")
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
        c6.addView(
            label("Coloque o nome de cada ator ou atriz. Toque no círculo para escolher a foto: ela aparece com o nome embaixo na tela do dorama.", 11.5f, Palette.muted),
            lin(MATCH, WRAP, b = 6)
        )
        castBox = LinearLayout(this)
        castBox.orientation = LinearLayout.VERTICAL
        c6.addView(castBox, lin(MATCH, WRAP))
        val addPerson = pill("Adicionar ator ou atriz", Palette.pink, Color.WHITE, 13f, "add")
        addPerson.setOnClickListener {
            syncNames()
            if (peopleState.size < 30) {
                peopleState.add(PS("", ""))
                rebuildCast()
                peopleEdits.lastOrNull()?.requestFocus()
            }
        }
        c6.addView(addPerson, lin(WRAP, WRAP, t = 8))
        rebuildCast()

        c6.addView(fieldLabel("Casal favorito"))
        coupleIn = input("Quem formou o casal que você shippa?", ex?.couple ?: "", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        c6.addView(coupleIn, lin(MATCH, WRAP))
        coupleBox = LinearLayout(this)
        coupleBox.orientation = LinearLayout.HORIZONTAL
        coupleBox.gravity = Gravity.CENTER_VERTICAL
        coupleBox.setPadding(0, dp(10), 0, 0)
        c6.addView(coupleBox, lin(MATCH, WRAP))
        refreshCouple()
        col.addView(c6, lin(MATCH, WRAP, t = 12))

        // ---------------- trilha sonora
        val cm = card(14, 24)
        cm.addView(sectionTitle("Trilha sonora", "music"))
        soundLabel = label("", 12.5f, Palette.text, true)
        soundLabel.maxLines = 2
        soundLabel.ellipsize = TextUtils.TruncateAt.END
        cm.addView(soundLabel, lin(MATCH, WRAP, b = 10))
        val soundBtns = LinearLayout(this)
        soundBtns.orientation = LinearLayout.HORIZONTAL
        soundBtns.gravity = Gravity.CENTER_VERTICAL
        val pickS = pill("Escolher trilha", Palette.pink, Color.WHITE, 13f, "music")
        pickS.setOnClickListener { pickSound() }
        val clearS = pill("Remover", Palette.card, Palette.pink, 12f, "close")
        clearS.background = roundRect(Palette.card, dp(20).toFloat(), Palette.pink, dp(1))
        clearS.setOnClickListener {
            if (soundPath.isNotEmpty() && soundPath != originalSound) File(soundPath).delete()
            soundPath = ""
            soundName = ""
            refreshSound()
        }
        soundBtns.addView(pickS, lin(WRAP, WRAP))
        soundBtns.addView(clearS, lin(WRAP, WRAP, l = 8))
        cm.addView(soundBtns, lin(MATCH, WRAP))
        cm.addView(label("Escolha o arquivo de áudio que você baixou (mp3, m4a...). Ele toca na tela do dorama e para quando você sai de lá.", 11.5f, Palette.muted), lin(MATCH, WRAP, t = 8))
        col.addView(cm, lin(MATCH, WRAP, t = 12))
        refreshSound()

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

        val pv = PetalsView(this, listOf("petal", "blossom", "sparkle"), Palette.pink, 14)
        petalsView = pv
        root.addView(pv, 0, FrameLayout.LayoutParams(MATCH, MATCH))

        setContentView(root)
        refreshCover()
        updateEffects()

        head.riseIn(0L, 12, 320L)
        for (i in 0 until col.childCount) col.getChildAt(i).riseIn(90L + minOf(i, 7) * 60L, 22, 420L)
        saveBtn.riseIn(400L, 30, 420L)
    }

    /** Gênero principal (em destaque, porque define o tema) e "outros gêneros"; refeito quando você cria um gênero novo. */
    private fun buildGenreArea() {
        genreBox.removeAllViews()

        val panel = LinearLayout(this)
        panel.orientation = LinearLayout.VERTICAL
        panel.setPadding(dp(14), dp(14), dp(14), dp(14))

        val titleTv = label("Gênero principal", 19f, Palette.text, true, true)
        val star = IconView(this, "sparkle", Palette.pink, 20)
        fun stylePanel() {
            val g = Genres.byKey(genreKey)
            panel.background = roundRect(g.soft, dp(22).toFloat(), g.primary, dp(2))
            titleTv.setTextColor(g.dark)
            star.tint = g.primary
        }

        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        head.addView(star, lin(WRAP, WRAP, r = 8))
        head.addView(titleTv, lin(WRAP, WRAP))
        panel.addView(head, lin(MATCH, WRAP))
        panel.addView(
            label("Ele define o tema do dorama: cores, símbolos e pétalas.", 12f, Palette.muted),
            lin(MATCH, WRAP, t = 4, b = 12)
        )

        val gOpts = ArrayList<Opt>()
        for (g in Genres.all) gOpts.add(Opt(g.key, g.label, g.primary, g.icon))
        val flow = chipFlow(gOpts, genreKey) {
            genreKey = it
            refreshShelfPicks()
            stylePanel()
            refreshCover()
            coverView.pop(1.25f)
            updateEffects()
        }
        // "Novo gênero" faz parte da própria lista de chips (nada fica cortado)
        val newG = pill("Novo gênero", Palette.card, Palette.pinkDark, 13f, "add")
        newG.background = roundRect(Palette.card, dp(20).toFloat(), Palette.pinkDark, dp(1))
        newG.setOnClickListener {
            showGenreCreator { g ->
                genreKey = g.key
                buildGenreArea()
                refreshCover()
                updateEffects()
            }
        }
        flow.addView(newG)
        panel.addView(flow, lin(MATCH, WRAP))
        stylePanel()
        genreBox.addView(panel, lin(MATCH, WRAP, t = 12))

        // "Outros gêneros": qualquer gênero extra (os com tema e as etiquetas simples) numa seção só.
        // Nenhum deles muda as cores do dorama; só o gênero principal faz isso.
        genreBox.addView(fieldLabel("Outros gêneros"))
        genreBox.addView(
            label("Só para classificar e filtrar. Quem define as cores e os símbolos é o gênero principal.", 12f, Palette.muted),
            lin(MATCH, WRAP, b = 8)
        )
        val allOpts = ArrayList<Opt>(gOpts)
        for (og in OtherGenres.all) allOpts.add(Opt(og.key, og.label, og.color, og.icon, og.colors))
        genreBox.addView(multiFlow(allOpts, tags) {
            tags = HashSet(it)
            refreshShelfPicks()
        }, lin(MATCH, WRAP))

        // quais dos extras aparecem no cartão da Estante (o principal sempre aparece)
        val sb = LinearLayout(this)
        sb.orientation = LinearLayout.VERTICAL
        shelfBox = sb
        genreBox.addView(sb, lin(MATCH, WRAP))
        refreshShelfPicks()
    }

    /** Lista dos gêneros extras marcados, na ordem em que o dorama já os tinha. */
    private fun orderedExtras(): List<String> {
        val base = existing?.tags ?: emptyList()
        return tags.filter { it != genreKey && (OtherGenres.exists(it) || Genres.exists(it)) }
            .sortedBy { val i = base.indexOf(it); if (i < 0) 1000 else i }
    }

    /** Escolha de até 2 gêneros extras para aparecerem na Estante. */
    private fun refreshShelfPicks() {
        val box = shelfBox ?: return
        box.removeAllViews()
        val extras = orderedExtras()
        shelfPick.retainAll(extras.toSet())
        if (shelfPick.isEmpty()) shelfPick.addAll(extras.take(2))
        while (shelfPick.size > 2) shelfPick.removeAt(0)
        if (extras.isEmpty()) return
        box.addView(fieldLabel("Aparecem na Estante"))
        box.addView(
            label("O gênero principal sempre aparece. Escolha até 2 dos extras para aparecerem junto no cartão.", 12f, Palette.muted),
            lin(MATCH, WRAP, b = 8)
        )
        val fl = FlowLayout(this)
        fl.hGap = dp(8)
        fl.vGap = dp(8)
        val views = ArrayList<Pair<String, TextView>>()
        fun colorOf(k: String): Int = if (OtherGenres.exists(k)) OtherGenres.byKey(k).color else Genres.byKey(k).primary
        fun style() {
            for ((k, tv) in views) {
                val c = colorOf(k)
                val on = shelfPick.contains(k)
                val cols = if (OtherGenres.exists(k)) OtherGenres.byKey(k).colors else emptyList()
                val multi = cols.size > 1
                val fg = if (on) Color.WHITE else if (multi) Palette.text else c
                tv.background = if (multi) multiColorBg(cols, on, dp(20).toFloat(), dp(2)) else roundRect(if (on) c else Palette.card, dp(20).toFloat(), c, dp(1))
                tv.setTextColor(fg)
                (tv.compoundDrawables[0] as? IconDrawable)?.color = fg
            }
        }
        for (k in extras) {
            val tv = if (OtherGenres.exists(k)) {
                val og = OtherGenres.byKey(k)
                pill(og.label, Palette.card, og.color, 13f, og.icon)
            } else {
                val g = Genres.byKey(k)
                pill(g.label, Palette.card, g.primary, 13f, g.icon)
            }
            tv.setOnClickListener {
                if (shelfPick.contains(k)) {
                    shelfPick.remove(k)
                } else {
                    if (shelfPick.size >= 2) shelfPick.removeAt(0)
                    shelfPick.add(k)
                }
                style()
                tv.pop(1.15f)
            }
            views.add(Pair(k, tv))
            fl.addView(tv)
        }
        style()
        box.addView(fl, lin(MATCH, WRAP))
    }

    /** Botão de favorito na cor do gênero escolhido. */
    private fun styleFavPill() {
        val tv = favPill ?: return
        val c = Genres.byKey(genreKey).primary
        val fg = if (favorite) Color.WHITE else c
        tv.text = if (favorite) "Nos favoritos" else "Marcar como favorito"
        tv.background = roundRect(if (favorite) c else Palette.card, dp(24).toFloat(), c, dp(2))
        tv.setTextColor(fg)
        tv.setCompoundDrawables(iconDrawable("heart", fg, dp(24)), null, null, null)
        tv.compoundDrawablePadding = dp(8)
    }

    /** O tema (pétalas e cor) acompanha o gênero, o status e o país escolhidos. */
    private fun updateEffects() {
        styleFavPill()
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

    private fun syncNames() {
        for (i in peopleState.indices) {
            val et = peopleEdits.getOrNull(i) ?: continue
            peopleState[i].name = et.text.toString()
        }
    }

    private fun dropPhoto(path: String) {
        if (path.isNotEmpty() && !originalPhotos.contains(path)) {
            try {
                File(path).delete()
            } catch (e: Exception) {
            }
        }
    }

    private fun rebuildCast() {
        syncNames()
        if (peopleState.isEmpty()) peopleState.add(PS("", ""))
        castBox.removeAllViews()
        peopleEdits.clear()
        for (i in peopleState.indices) {
            val ps = peopleState[i]
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(0, dp(6), 0, dp(6))
            val av = avatarView(ps.photo, 58, Palette.pink, Palette.pinkSoft, Palette.pink)
            av.setOnClickListener {
                syncNames()
                pickTarget = i
                pickPhoto()
            }
            av.pressable(0.92f)
            row.addView(av, lin(dp(58), dp(58), r = 12))
            val et = input("Nome do ator ou atriz", ps.name, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
            et.setSingleLine(true)
            peopleEdits.add(et)
            row.addView(et, lin(0, WRAP, 1f))
            row.addView(roundBtn("close", Palette.pink, false, 14) {
                syncNames()
                dropPhoto(peopleState[i].photo)
                peopleState.removeAt(i)
                rebuildCast()
            }, lin(dp(32), dp(32), l = 8))
            castBox.addView(row, lin(MATCH, WRAP))
        }
    }

    private fun refreshCouple() {
        coupleBox.removeAllViews()
        val av = avatarView(couplePhoto, 76, Palette.pink, Palette.pinkSoft, Palette.pink, "heart", true)
        av.setOnClickListener {
            pickTarget = -1
            pickPhoto()
        }
        av.pressable(0.94f)
        coupleBox.addView(av, lin(dp(76), dp(76), r = 14))
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.addView(label("Foto do casal", 13f, Palette.text, true))
        col.addView(label("Toque na foto para escolher uma da galeria", 11.5f, Palette.muted), lin(MATCH, WRAP, t = 2))
        if (couplePhoto.isNotEmpty()) {
            val rm = pill("Remover foto", Palette.card, Palette.pink, 12f, "close")
            rm.background = roundRect(Palette.card, dp(20).toFloat(), Palette.pink, dp(1))
            rm.setOnClickListener {
                dropPhoto(couplePhoto)
                couplePhoto = ""
                refreshCouple()
            }
            col.addView(rm, lin(WRAP, WRAP, t = 8))
        }
        coupleBox.addView(col, lin(0, WRAP, 1f))
    }

    private fun pickPhoto() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "image/*"
        i.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(i, "Escolher foto"), 103)
    }

    private fun refreshSound() {
        soundLabel.text = if (soundPath.isEmpty()) "Nenhuma trilha escolhida" else soundName.ifEmpty { "Trilha sonora" }
    }

    private fun pickSound() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "audio/*"
        i.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(i, "Escolher trilha sonora"), 102)
    }

    private fun pickCover() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "image/*"
        i.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(i, "Escolher capa"), 101)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 103 && resultCode == android.app.Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                val p = Store.saveCover(uri, "people", "p", 700)
                if (p == null) {
                    Toast.makeText(this, "Não consegui abrir essa imagem.", Toast.LENGTH_SHORT).show()
                } else if (pickTarget == -1) {
                    dropPhoto(couplePhoto)
                    couplePhoto = p
                    refreshCouple()
                } else if (pickTarget >= 0 && pickTarget < peopleState.size) {
                    dropPhoto(peopleState[pickTarget].photo)
                    peopleState[pickTarget].photo = p
                    rebuildCast()
                } else {
                    File(p).delete()
                }
            }
        }
        if (requestCode == 102 && resultCode == android.app.Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                Toast.makeText(this, "Copiando a trilha...", Toast.LENGTH_SHORT).show()
                Thread {
                    val r = Store.saveSound(uri)
                    runOnUiThread {
                        if (isFinishing || isDestroyed) {
                            if (r != null) File(r.first).delete()
                            return@runOnUiThread
                        }
                        if (r != null) {
                            if (soundPath.isNotEmpty() && soundPath != originalSound) File(soundPath).delete()
                            soundPath = r.first
                            soundName = r.second
                            refreshSound()
                        } else {
                            Toast.makeText(this, "Não consegui abrir esse arquivo.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            }
        }
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
        syncNames()
        val people = ArrayList<Person>()
        for (ps in peopleState) {
            val n = ps.name.trim()
            if (n.isNotEmpty()) people.add(Person(n, ps.photo)) else dropPhoto(ps.photo)
        }
        val old = existing
        var newLink = linkIn.text.toString().trim()
        if (newLink.isNotEmpty() && !newLink.startsWith("http://") && !newLink.startsWith("https://")) newLink = "https://$newLink"
        val d = Drama(
            id = if (old != null) old.id else System.currentTimeMillis(),
            title = title,
            original = originalIn.text.toString().trim(),
            synopsis = synopsisIn.text.toString().trim(),
            country = country,
            genre = genreKey,
            tags = tags.filter { it != genreKey },
            status = statusKey,
            kind = kindKey,
            soundtrack = soundPath,
            soundtrackName = soundName,
            score = score,
            seasonEps = ArrayList(seasonTotals),
            watched = ArrayList(seasonWatched),
            epMinutes = minutesIn.text.toString().toIntOrNull() ?: 0,
            year = yearIn.text.toString().trim(),
            platform = platformIn.text.toString().trim(),
            cast = people.joinToString(", ") { it.name },
            couple = coupleIn.text.toString().trim(),
            castPeople = people,
            couplePhoto = couplePhoto,
            startDate = startDate,
            endDate = endDate,
            rewatch = rewatch,
            notes = notesIn.text.toString().trim(),
            favorite = favorite,
            cover = coverPath,
            addedAt = if (old != null) old.addedAt else System.currentTimeMillis(),
            link = newLink,
            lastUrl = if (old != null && old.link == newLink) old.lastUrl else "",
            watchSeason = old?.watchSeason ?: -1,
            order = old?.order ?: 0L,
            shelfTags = shelfPick.filter { tags.contains(it) && it != genreKey }.take(2)
        )
        normalize(d)
        val tot = totalEps(d)
        val wat = watchedEps(d)
        var st = statusKey
        if (wat > 0 && st == "quero") st = "assistindo"
        applyStatus(d, st)

        if (originalCover.isNotEmpty() && originalCover != coverPath) {
            try {
                File(originalCover).delete()
            } catch (e: Exception) {
            }
            Covers.clear()
        }
        if (originalSound.isNotEmpty() && originalSound != soundPath) {
            try {
                File(originalSound).delete()
            } catch (e: Exception) {
            }
        }
        val finalPhotos = HashSet<String>()
        for (p in people) if (p.photo.isNotEmpty()) finalPhotos.add(p.photo)
        if (couplePhoto.isNotEmpty()) finalPhotos.add(couplePhoto)
        for (p in originalPhotos) {
            if (!finalPhotos.contains(p)) {
                try {
                    File(p).delete()
                } catch (e: Exception) {
                }
            }
        }
        saved = true
        Store.save(d)
        Toast.makeText(this, "Salvo com carinho!", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!saved) {
            for (ps in peopleState) dropPhoto(ps.photo)
            dropPhoto(couplePhoto)
        }
        if (!saved && soundPath.isNotEmpty() && soundPath != originalSound) {
            try {
                File(soundPath).delete()
            } catch (e: Exception) {
            }
        }
        if (!saved && coverPath.isNotEmpty() && coverPath != originalCover) {
            try {
                File(coverPath).delete()
            } catch (e: Exception) {
            }
        }
    }
}
