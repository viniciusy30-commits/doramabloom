package com.doramabloom.app

import android.content.Intent
import android.net.Uri
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
import androidx.appcompat.app.AlertDialog
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
    private var shelf = ArrayList<String>()
    private var shelfBox: LinearLayout? = null
    private var statusKey = "quero"
    private var country = countries[0]
    private var score = 0
    private var favorite = false
    private var supremacy = false
    private var supPill: TextView? = null
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
    private lateinit var startIn: EditText
    private lateinit var endIn: EditText
    private lateinit var titleIn: EditText
    private lateinit var originalIn: EditText
    private lateinit var synopsisIn: EditText
    private lateinit var minutesIn: EditText
    private var streams: MutableList<Streaming> = ArrayList()
    private lateinit var streamFlow: FlowLayout
    private lateinit var yearIn: EditText
    private var kind = "serie"
    private var soundtrackPath = ""
    private var snapQuery = ""
    private var snapSince = 0L
    private var snapWaiting = false
    private var couplePhotoPath = ""
    private val castList = ArrayList<CastPerson>()
    private var pendingCast = -1
    private val createdFiles = ArrayList<String>()
    private val originalFiles = HashSet<String>()
    private lateinit var castBox: LinearLayout
    private lateinit var coupleBox: FrameLayout
    private lateinit var soundtrackTv: TextView
    private lateinit var soundtrackNameIn: EditText
    private lateinit var serieBtn: TextView
    private lateinit var filmeBtn: TextView
    private lateinit var coupleIn: EditText
    private lateinit var notesIn: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        ThemeMode.refresh(this)
        id = intent.getLongExtra("id", -1L)
        existing = Store.get(id)
        val real = existing
        // rascunho: se o app foi fechado no meio do formulário, volta tudo como estava
        val prefill = intent.getStringExtra("prefill") ?: ""
        val draft = if (prefill.isEmpty()) Store.loadDraft(id) else null
        if (draft == null && prefill.isNotEmpty()) Store.clearDraft(id)
        if (draft != null) createdFiles.addAll(draft.second)
        val ex: Drama? = draft?.first ?: real
        if (real != null) {
            if (real.soundtrack.isNotEmpty()) originalFiles.add(real.soundtrack)
            if (real.couplePhoto.isNotEmpty()) originalFiles.add(real.couplePhoto)
            for (p in real.castPeople) if (p.photo.isNotEmpty()) originalFiles.add(p.photo)
            originalCover = real.cover
        }
        if (ex != null) {
            coverPath = ex.cover
            kind = ex.kind
            soundtrackPath = ex.soundtrack
            couplePhotoPath = ex.couplePhoto
            for (p in ex.castPeople) castList.add(CastPerson(p.name, p.photo, p.role))
            genreKey = ex.genre
            tags = HashSet(ex.tags)
            shelf = ArrayList(ex.shelfTags)
            statusKey = ex.status
            country = ex.country
            score = ex.score
            favorite = ex.favorite
            supremacy = ex.supremacy
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
        head.addView(label(if (real == null) "Novo dorama" else "Editar dorama", 22f, Palette.pinkDark, true, true))
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
        titleIn = input("Ex.: Pousando no Amor", ex?.title ?: prefill, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
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

        val supTv = pill("Marcar como Supremacy", Palette.card, Color.parseColor("#E0A100"), 15f, "crown")
        supTv.setPadding(dp(18), dp(10), dp(20), dp(10))
        supPill = supTv
        supTv.setOnClickListener {
            supremacy = !supremacy
            if (supremacy) {
                favorite = true // quem é Supremacy também é favorito
                styleFavPill()
            }
            styleSupPill()
            supTv.pop(1.3f)
        }
        styleSupPill()
        c4.addView(supTv, lin(WRAP, WRAP, t = 8))
        c4.addView(label("Supremacy é para os pouquíssimos doramas acima de todos os favoritos.", 11f, Palette.muted), lin(WRAP, WRAP, t = 4))

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
        c5.addView(fieldLabel("Onde assistir (toque para marcar)"))
        streams = ArrayList(Streamings.parse(ex?.platform ?: ""))
        streamFlow = FlowLayout(this)
        streamFlow.hGap = dp(8)
        streamFlow.vGap = dp(8)
        c5.addView(streamFlow, lin(MATCH, WRAP))
        refreshStreams()
        c5.addView(fieldLabel("Ano"))
        yearIn = input("Ano (várias temporadas? ex.: 19-22)", ex?.year ?: "", InputType.TYPE_CLASS_TEXT)
        c5.addView(yearIn, lin(MATCH, WRAP))
        c5.addView(fieldLabel("Datas (só os números; o ano pode ter 2 dígitos: 22 vira 2022)"))
        val dRow = LinearLayout(this)
        dRow.orientation = LinearLayout.HORIZONTAL
        startIn = dateInput("Início: dd/mm/aa", startDate) { startDate = it }
        endIn = dateInput("Fim: dd/mm/aa", endDate) { endDate = it }
        dRow.addView(startIn, lin(0, WRAP, 1f, r = 8))
        dRow.addView(endIn, lin(0, WRAP, 1f))
        c5.addView(dRow, lin(MATCH, WRAP))
        col.addView(c5, lin(MATCH, WRAP, t = 12))

        // ---------------- tipo e trilha sonora
        val c8 = card(14, 24)
        c8.addView(sectionTitle("Tipo e trilha sonora", "music"))
        c8.addView(fieldLabel("Tipo"))
        val kindRow = LinearLayout(this)
        kindRow.orientation = LinearLayout.HORIZONTAL
        serieBtn = pill("Série", Palette.pinkSoft, Palette.pinkDark, 13f, "tv")
        filmeBtn = pill("Filme", Palette.pinkSoft, Palette.pinkDark, 13f, "film")
        serieBtn.setOnClickListener {
            kind = "serie"
            styleKind()
        }
        filmeBtn.setOnClickListener {
            kind = "filme"
            styleKind()
        }
        kindRow.addView(serieBtn, lin(WRAP, WRAP, r = 8))
        kindRow.addView(filmeBtn, lin(WRAP, WRAP))
        c8.addView(kindRow, lin(MATCH, WRAP))
        styleKind()
        c8.addView(fieldLabel("Trilha sonora"))
        soundtrackTv = label("", 13f, Palette.muted)
        c8.addView(soundtrackTv, lin(MATCH, WRAP, b = 8))
        c8.addView(fieldLabel("Nome da música"))
        soundtrackNameIn = input("Se ficar vazio, aparece só \"Trilha sonora\"", ex?.soundtrackName ?: "", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
        soundtrackNameIn.maxLines = 1
        soundtrackNameIn.setSingleLine(true)
        c8.addView(soundtrackNameIn, lin(MATCH, WRAP, b = 8))
        val sRow = LinearLayout(this)
        sRow.orientation = LinearLayout.HORIZONTAL
        val sPick = pill("Escolher áudio", Palette.pinkSoft, Palette.pinkDark, 13f, "music")
        sPick.setOnClickListener { pickSoundtrack() }
        val sClear = pill("Tirar", Palette.pinkSoft, Palette.pinkDark, 13f)
        sClear.setOnClickListener {
            soundtrackPath = ""
            soundtrackNameIn.setText("")
            refreshSoundtrack()
        }
        sRow.addView(sPick, lin(WRAP, WRAP, r = 8))
        sRow.addView(sClear, lin(WRAP, WRAP))
        c8.addView(sRow, lin(MATCH, WRAP))
        val snapRow = LinearLayout(this)
        snapRow.orientation = LinearLayout.HORIZONTAL
        val snapGo = pill("Buscar no SnapTube", Palette.pinkSoft, Palette.pinkDark, 13f, "search")
        snapGo.setOnClickListener { startSnap() }
        val snapLast = pill("Pegar o último baixado", Palette.pinkSoft, Palette.pinkDark, 13f, "download")
        snapLast.setOnClickListener { checkSnap(System.currentTimeMillis() - 60L * 60L * 1000L) }
        snapRow.addView(snapGo, lin(WRAP, WRAP, r = 8))
        snapRow.addView(snapLast, lin(WRAP, WRAP))
        c8.addView(snapRow, lin(MATCH, WRAP, t = 8))
        c8.addView(
            label("Toque em Buscar no SnapTube, baixe como MP3 e volte: o app acha o arquivo sozinho.", 11.5f, Palette.muted),
            lin(MATCH, WRAP, t = 6)
        )
        refreshSoundtrack()
        col.addView(c8, lin(MATCH, WRAP, t = 12))

        // ---------------- elenco
        val c6 = card(14, 24)
        c6.addView(sectionTitle("Elenco e casal", "person"))
        c6.addView(fieldLabel("Elenco"))
        castBox = LinearLayout(this)
        castBox.orientation = LinearLayout.VERTICAL
        c6.addView(castBox, lin(MATCH, WRAP))
        val addCast = pill("Adicionar ator ou atriz", Palette.pinkSoft, Palette.pinkDark, 13f, "person")
        addCast.setOnClickListener {
            castList.add(CastPerson("", ""))
            rebuildCast()
        }
        c6.addView(addCast, lin(WRAP, WRAP, t = 8))
        rebuildCast()
        c6.addView(fieldLabel("Casal favorito"))
        coupleIn = input("Quem formou o casal que você shippa?", ex?.couple ?: "", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        c6.addView(coupleIn, lin(MATCH, WRAP))
        c6.addView(label("Foto do casal (toque para escolher)", 12f, Palette.muted), lin(MATCH, WRAP, t = 8, b = 6))
        coupleBox = FrameLayout(this)
        c6.addView(coupleBox, lin(dp(84), dp(84)))
        refreshCouplePhoto()
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

        // ---------------- excluir (só aparece ao editar um dorama que já existe)
        if (real != null) {
            val del = pill("Excluir dorama", Color.parseColor("#FFE0E6"), Color.parseColor("#C2185B"), 14f, "delete")
            del.setPadding(dp(18), dp(12), dp(18), dp(12))
            del.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Excluir dorama?")
                    .setMessage("\"" + real.title + "\" será removido da sua estante.")
                    .setPositiveButton("Excluir") { _, _ ->
                        sv.animate().alpha(0f).translationY(dp(30).toFloat()).setDuration(260).withEndAction {
                            Store.delete(real.id)
                            Toast.makeText(this, "Dorama excluído", Toast.LENGTH_SHORT).show()
                            finish()
                        }.start()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            col.addView(del, lin(MATCH, WRAP, t = 18))
        }

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
        for (g in Genres.all) gOpts.add(Opt(g.key, g.label, g.primary, g.icon, g.colors))
        val flow = chipFlow(gOpts, genreKey) {
            genreKey = it
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
            buildShelfArea()
        }, lin(MATCH, WRAP))

        // "Aparecem na Estante": quais dos outros gêneros vão no cartão (além do principal)
        val sb = LinearLayout(this)
        sb.orientation = LinearLayout.VERTICAL
        shelfBox = sb
        genreBox.addView(sb, lin(MATCH, WRAP))
        buildShelfArea()
    }

    private fun optFor(k: String): Opt? {
        if (OtherGenres.exists(k)) {
            val og = OtherGenres.byKey(k)
            return Opt(og.key, og.label, og.color, og.icon, og.colors)
        }
        if (Genres.exists(k)) {
            val g = Genres.byKey(k)
            return Opt(g.key, g.label, g.primary, g.icon, g.colors)
        }
        return null
    }

    /** Escolha (até 3) dos gêneros que aparecem no cartão da Estante; o principal sempre aparece. */
    private fun buildShelfArea() {
        val box = shelfBox ?: return
        box.removeAllViews()
        val order = Genres.all.map { it.key } + OtherGenres.all.map { it.key }
        val avail = tags.filter { it != genreKey && optFor(it) != null }.sortedBy { order.indexOf(it) }
        shelf = ArrayList(shelf.filter { avail.contains(it) }.distinct().take(3))
        if (avail.isEmpty()) {
            box.visibility = View.GONE
            return
        }
        box.visibility = View.VISIBLE
        box.addView(fieldLabel("Aparecem na Estante"))
        box.addView(
            label(
                "O gênero principal sempre aparece. Escolha até 3 dos outros para aparecerem no cartão (sem escolha, aparecem os 3 primeiros).",
                12f, Palette.muted
            ),
            lin(MATCH, WRAP, b = 8)
        )
        val opts = avail.mapNotNull { optFor(it) }
        box.addView(multiFlow(opts, shelf.toSet(), 3) { chosen ->
            shelf = ArrayList(avail.filter { chosen.contains(it) })
        }, lin(MATCH, WRAP))
    }

    /** Botão Supremacy: dourado. */
    private fun styleSupPill() {
        val tv = supPill ?: return
        val gold = Color.parseColor("#E0A100")
        val fg = if (supremacy) Color.WHITE else gold
        tv.text = if (supremacy) "Supremacy" else "Marcar como Supremacy"
        tv.background = roundRect(if (supremacy) gold else Palette.card, dp(24).toFloat(), gold, dp(2))
        tv.setTextColor(fg)
        tv.setCompoundDrawables(iconDrawable("crown", fg, dp(24)), null, null, null)
        tv.compoundDrawablePadding = dp(8)
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

    /** Desenha todos os streamings: os marcados com a cor cheia, os outros apagadinhos. */
    private fun refreshStreams() {
        streamFlow.removeAllViews()
        val shown = Streamings.all + streams.filter { s -> Streamings.all.none { it.key == s.key } }
        for (s in shown) {
            val on = streams.any { it.key == s.key }
            val chip = streamChip(s, 12f)
            chip.alpha = if (on) 1f else 0.35f
            chip.setOnClickListener {
                if (on) streams.removeAll { it.key == s.key } else streams.add(s)
                refreshStreams()
            }
            streamFlow.addView(chip)
        }
        val plus = pill("Outro", Palette.pinkSoft, Palette.pink, 12f, "add")
        plus.setOnClickListener {
            showStreamingCreator { s ->
                streams.removeAll { it.key == s.key }
                streams.add(s)
                refreshStreams()
            }
        }
        streamFlow.addView(plus)
    }

    private fun fieldLabel(t: String): TextView {
        val v = label(t, 12.5f, Palette.muted, true)
        v.setPadding(dp(2), dp(14), 0, dp(6))
        return v
    }

    private fun scoreText(): String = if (score <= 0) "Sem nota ainda" else "$score / 10"

    private fun fmt(ms: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(ms))

    /** Campo de data digitada: só números, as barras entram sozinhas (ex.: 07102026 vira 07/10/2026). */
    private fun dateInput(hint: String, initial: Long, onValue: (Long) -> Unit): EditText {
        val e = input(hint, if (initial > 0L) fmt(initial) else "", InputType.TYPE_CLASS_NUMBER)
        e.textSize = 14f
        e.maxLines = 1
        var busy = false
        e.doAfterTextChanged { ed ->
            if (busy || ed == null) return@doAfterTextChanged
            val digits = ed.toString().filter { it.isDigit() }.take(8)
            val sb = StringBuilder()
            for (i in digits.indices) {
                if (i == 2 || i == 4) sb.append('/')
                sb.append(digits[i])
            }
            val txt = sb.toString()
            if (txt != ed.toString()) {
                busy = true
                e.setText(txt)
                e.setSelection(txt.length)
                busy = false
            }
            val ms = parseDate(txt)
            onValue(if (ms != null) ms else 0L)
            val bad = txt.isNotEmpty() && ms == null && (txt.length == 10 || txt.length == 8)
            e.background = roundRect(Palette.card, dp(18).toFloat(), if (bad) Color.parseColor("#E5484D") else Palette.line, dp(if (bad) 2 else 1))
        }
        return e
    }

    /** "dd/MM/aaaa" para milissegundos (meio-dia); null se não for uma data de verdade. */
    private fun parseDate(t: String): Long? {
        // aceita dd/mm/aa (22 vira 2022) ou dd/mm/aaaa
        if (t.length != 8 && t.length != 10) return null
        val dd = t.substring(0, 2).toIntOrNull() ?: return null
        val mm = t.substring(3, 5).toIntOrNull() ?: return null
        val yRaw = t.substring(6)
        var yy = yRaw.toIntOrNull() ?: return null
        if (yRaw.length == 2) yy = fullYear(yy)
        if (yy < 1900 || yy > 2100) return null
        val c = Calendar.getInstance()
        c.isLenient = false
        c.clear()
        c.set(yy, mm - 1, dd, 12, 0, 0)
        return try {
            c.timeInMillis
        } catch (e: Exception) {
            null
        }
    }

    /** Ano de 2 dígitos para 4: 00 a 69 viram 2000 a 2069; 70 a 99 viram 1970 a 1999. */
    private fun fullYear(yy: Int): Int = if (yy <= 69) 2000 + yy else 1900 + yy

    /** Campo preenchido pela metade ou com data que não existe: não deixa salvar sem avisar. */
    private fun dateProblem(e: EditText): Boolean {
        val t = e.text.toString()
        return t.isNotEmpty() && parseDate(t) == null
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

    private fun styleKind() {
        for ((b, k) in listOf(Pair(serieBtn, "serie"), Pair(filmeBtn, "filme"))) {
            val sel = kind == k
            b.background = roundRect(if (sel) Palette.pink else Palette.pinkSoft, dp(20).toFloat())
            b.setTextColor(if (sel) Color.WHITE else Palette.pinkDark)
            b.compoundDrawables[0]?.setTint(if (sel) Color.WHITE else Palette.pinkDark)
        }
    }

    private fun refreshSoundtrack() {
        soundtrackTv.text = if (soundtrackPath.isEmpty()) "Nenhuma trilha escolhida" else "Trilha escolhida ✓"
    }

    private fun rebuildCast() {
        castBox.removeAllViews()
        for ((i, p) in castList.withIndex()) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            val av = avatarView(p.photo, 52, Palette.pink, Palette.pinkSoft, Palette.pink)
            av.setOnClickListener {
                pendingCast = i
                pickImage(104, "Escolher foto", p.name.trim())
            }
            row.addView(av, lin(dp(52), dp(52), r = 10))
            val fields = LinearLayout(this)
            fields.orientation = LinearLayout.VERTICAL
            val nm = input("Nome do ator ou atriz", p.name, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
            nm.doAfterTextChanged { p.name = it?.toString() ?: "" }
            fields.addView(nm, lin(MATCH, WRAP))
            val rl = input("Personagem (ex.: Kim Sun-woo)", p.role, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
            rl.textSize = 13.5f
            rl.doAfterTextChanged { p.role = it?.toString() ?: "" }
            fields.addView(rl, lin(MATCH, WRAP, t = 6))
            row.addView(fields, lin(0, WRAP, 1f))
            val rm = label("✕", 16f, Palette.pinkDark, true)
            rm.setPadding(dp(12), dp(8), dp(4), dp(8))
            rm.setOnClickListener {
                castList.removeAt(i)
                rebuildCast()
            }
            row.addView(rm, lin(WRAP, WRAP))
            castBox.addView(row, lin(MATCH, WRAP, t = 8))
        }
    }

    private fun refreshCouplePhoto() {
        coupleBox.removeAllViews()
        val av = avatarView(couplePhotoPath, 84, Palette.pink, Palette.pinkSoft, Palette.pink, "heart", true)
        av.setOnClickListener {
            val q = coupleIn.text.toString().trim()
            pickImage(103, "Escolher foto do casal", if (q.isEmpty()) "" else "$q casal")
        }
        coupleBox.addView(av, FrameLayout.LayoutParams(MATCH, MATCH))
    }

    /** Pergunta de onde vem a imagem: galeria do celular ou busca direto na internet (sem baixar antes). */
    private fun pickImage(code: Int, title: String, query: String = "") {
        val items = arrayOf("Escolher da galeria do celular", "Buscar na internet")
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(items) { _, which ->
                if (which == 0) {
                    val i = Intent(Intent.ACTION_GET_CONTENT)
                    i.type = "image/*"
                    i.addCategory(Intent.CATEGORY_OPENABLE)
                    startActivityForResult(Intent.createChooser(i, title), code)
                } else {
                    val i = Intent(this, WebPickActivity::class.java)
                    i.putExtra("query", query)
                    i.putExtra("title", title)
                    // resultado da internet volta com o código + 100
                    startActivityForResult(i, code + 100)
                }
            }
            .show()
    }

    // ---------------- SnapTube

    private fun startSnap() {
        val t = titleIn.text.toString().trim()
        val nm = soundtrackNameIn.text.toString().trim()
        snapQuery = if (nm.isNotEmpty()) nm else if (t.isNotEmpty()) "$t OST" else "dorama OST"
        if (!SnapTube.hasPermission(this)) {
            requestPermissions(arrayOf(SnapTube.permission()), 301)
            return
        }
        launchSnap()
    }

    private fun launchSnap() {
        snapSince = System.currentTimeMillis() - 15000L
        if (SnapTube.open(this, snapQuery)) {
            snapWaiting = true
            Toast.makeText(this, "Baixe o MP3 no SnapTube e volte para cá.", Toast.LENGTH_LONG).show()
        } else {
            AlertDialog.Builder(this)
                .setTitle("SnapTube não encontrado")
                .setMessage("Não consegui abrir o SnapTube. Baixe a música por lá e depois toque em \"Pegar o último baixado\", ou escolha o arquivo.")
                .setPositiveButton("Escolher áudio") { _, _ -> pickSoundtrack() }
                .setNegativeButton("Fechar", null)
                .show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 302) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                checkSnap(System.currentTimeMillis() - 60L * 60L * 1000L)
            }
            return
        }
        if (requestCode != 301) return
        if (grantResults.isEmpty() || grantResults[0] != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Sem a permissão não consigo achar o MP3 sozinho: use \"Escolher áudio\" depois de baixar.", Toast.LENGTH_LONG).show()
        }
        launchSnap()
    }

    override fun onResume() {
        super.onResume()
        if (snapWaiting) {
            snapWaiting = false
            val since = snapSince
            window.decorView.postDelayed({ if (!isFinishing) checkSnap(since) }, 700L)
        }
    }

    private fun checkSnap(since: Long) {
        if (!SnapTube.hasPermission(this)) {
            requestPermissions(arrayOf(SnapTube.permission()), 302)
            return
        }
        val list = SnapTube.recent(this, since)
        if (list.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Ainda não achei o MP3")
                .setMessage("Se o download ainda está terminando, espere acabar e toque em Tentar de novo.")
                .setPositiveButton("Tentar de novo") { _, _ -> checkSnap(since) }
                .setNeutralButton("Escolher arquivo") { _, _ -> pickSoundtrack() }
                .setNegativeButton("Fechar", null)
                .show()
            return
        }
        val names = Array(list.size) { list[it].title.ifEmpty { "Música baixada" } }
        AlertDialog.Builder(this)
            .setTitle("Usar qual música como trilha?")
            .setItems(names) { _, i -> useSnapFile(list[i]) }
            .setNeutralButton("Tentar de novo") { _, _ -> checkSnap(since) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun useSnapFile(f: SnapFile) {
        val p = Store.saveAudio(f.uri)
        if (p == null) {
            Toast.makeText(this, "Não consegui abrir esse arquivo.", Toast.LENGTH_SHORT).show()
            return
        }
        createdFiles.add(p)
        soundtrackPath = p
        if (soundtrackNameIn.text.toString().isBlank()) soundtrackNameIn.setText(f.title)
        refreshSoundtrack()
        Toast.makeText(this, "Trilha pronta ✓", Toast.LENGTH_SHORT).show()
    }

    private fun pickSoundtrack() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "audio/*"
        i.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(i, "Escolher a trilha sonora"), 102)
    }

    private fun refreshCover() {
        coverView.bind(coverPath, genreKey, 400)
        coverSeal.set(Genres.byKey(genreKey))
    }

    private fun pickCover() {
        val t = titleIn.text.toString().trim()
        pickImage(101, "Escolher capa", if (t.isEmpty()) "" else "$t dorama poster")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != android.app.Activity.RESULT_OK) return
        var code = requestCode
        var uri: Uri? = data?.data
        var temp: File? = null
        if (requestCode >= 200) {
            // veio do navegador de imagens: o arquivo temporário é tratado igual a uma imagem da galeria
            code = requestCode - 100
            val path = data?.getStringExtra("path")
            if (path.isNullOrEmpty()) return
            temp = File(path)
            uri = Uri.fromFile(temp)
        }
        if (uri == null) return
        try {
            applyPicked(code, uri)
        } finally {
            try {
                temp?.delete()
            } catch (e: Exception) {
            }
        }
    }

    /** 101 = capa, 102 = trilha sonora, 103 = foto do casal, 104 = foto de quem está no elenco. */
    private fun applyPicked(code: Int, uri: Uri) {
        if (code == 102 || code == 103 || code == 104) {
            val p = if (code == 102) Store.saveAudio(uri) else Store.saveCover(uri)
            if (p == null) {
                Toast.makeText(this, "Não consegui abrir esse arquivo.", Toast.LENGTH_SHORT).show()
            } else {
                createdFiles.add(p)
                when (code) {
                    102 -> {
                        soundtrackPath = p
                        refreshSoundtrack()
                    }
                    103 -> {
                        couplePhotoPath = p
                        refreshCouplePhoto()
                    }
                    else -> {
                        if (pendingCast in castList.indices) castList[pendingCast].photo = p
                        rebuildCast()
                    }
                }
            }
            return
        }
        if (code == 101) {
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

    /** Junta tudo o que está no formulário num Dorama (sem validar nem salvar). */
    private fun collect(): Drama {
        val old = existing
        val title = titleIn.text.toString().trim()
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
                        year = normalizeYears(yearIn.text.toString()),
            platform = Streamings.encode(streams),
            cast = castList.map { it.name.trim() }.filter { it.isNotEmpty() }.joinToString(", "),
            couple = coupleIn.text.toString().trim(),
            startDate = startDate,
            endDate = endDate,
            rewatch = rewatch,
            notes = notesIn.text.toString().trim(),
            favorite = favorite,
            supremacy = supremacy,
            cover = coverPath,
            addedAt = if (old != null) old.addedAt else System.currentTimeMillis(),
            link = old?.link ?: "",
            lastUrl = old?.lastUrl ?: "",
            watchSeason = old?.watchSeason ?: -1,
            order = old?.order ?: 0L,
            kind = kind,
            soundtrack = soundtrackPath,
            soundtrackName = if (soundtrackPath.isEmpty()) "" else soundtrackNameIn.text.toString().trim(),
            castPeople = castList.filter { it.name.isNotBlank() }.map { CastPerson(it.name.trim(), it.photo, it.role.trim()) },
            couplePhoto = couplePhotoPath,
            shelfTags = shelf.filter { tags.contains(it) && it != genreKey }
        )
        return d
    }

    private fun writeDraft() {
        try {
            val d = collect()
            val has = d.title.isNotBlank() || d.original.isNotBlank() || d.synopsis.isNotBlank() || d.notes.isNotBlank() ||
                d.couple.isNotBlank() || d.cover.isNotEmpty() || d.castPeople.isNotEmpty() || d.soundtrack.isNotEmpty() ||
                d.year.isNotBlank() || d.platform.isNotBlank() || d.startDate > 0L || d.endDate > 0L || d.epMinutes > 0
            if (existing == null && !has) Store.clearDraft(id) else Store.saveDraft(id, d, createdFiles)
        } catch (e: Exception) {
        }
    }

    override fun onPause() {
        super.onPause()
        if (!saved && !isFinishing) writeDraft()
    }

    private fun save() {
        val title = titleIn.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, "Dê um nome ao dorama.", Toast.LENGTH_SHORT).show()
            titleIn.requestFocus()
            return
        }
        if (dateProblem(startIn) || dateProblem(endIn)) {
            Toast.makeText(this, "Confira a data: use dia/mês/ano, como 07/10/26 ou 07/10/2026.", Toast.LENGTH_SHORT).show()
            (if (dateProblem(startIn)) startIn else endIn).requestFocus()
            return
        }
        val d = collect()
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
        val used = HashSet<String>()
        if (d.soundtrack.isNotEmpty()) used.add(d.soundtrack)
        if (d.couplePhoto.isNotEmpty()) used.add(d.couplePhoto)
        for (p in d.castPeople) if (p.photo.isNotEmpty()) used.add(p.photo)
        for (f in originalFiles + createdFiles) {
            if (!used.contains(f)) {
                try {
                    File(f).delete()
                } catch (e: Exception) {
                }
            }
        }
        Covers.clear()
        saved = true
        Store.clearDraft(id)
        Store.save(d)
        Toast.makeText(this, "Salvo com carinho!", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // só apaga os arquivos novos se você realmente saiu da tela; se o sistema só recriou, o rascunho continua valendo
        if (!saved && !isFinishing) return
        if (!saved) Store.clearDraft(id)
        if (!saved) {
            for (f in createdFiles) {
                try {
                    File(f).delete()
                } catch (e: Exception) {
                }
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
