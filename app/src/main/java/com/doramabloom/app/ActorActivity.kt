package com.doramabloom.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File

/**
 * Perfil de um ator ou atriz: foto grande, números, gêneros, escolha da foto (entre as de todos os elencos),
 * informações editáveis, os doramas que fez e quem costuma aparecer junto.
 * Recebe "key" (a chave do nome, veja Store.personKey).
 */
class ActorActivity : AppCompatActivity() {

    private var key = ""
    private var editing = false
    private var seen = -1
    private lateinit var sv: ScrollView
    private lateinit var col: LinearLayout
    private var actor: Actor? = null
    private var everyone: List<Actor> = emptyList()
    private var photoAnchor: View? = null
    private var infoAnchor: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        ThemeMode.refresh(this)
        key = intent.getStringExtra("key") ?: ""
        if (Actors.find(key) == null) {
            finish()
            return
        }
        val root = FrameLayout(this)
        root.background = gradient(Palette.bgTop, Palette.bgBottom)
        root.addView(PetalsView(this, listOf("petal", "blossom", "sparkle"), Palette.pink, 14), FrameLayout.LayoutParams(MATCH, MATCH))
        sv = ScrollView(this)
        sv.isVerticalScrollBarEnabled = false
        col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(12), dp(16), dp(28))
        sv.addView(col)
        root.addView(sv, FrameLayout.LayoutParams(MATCH, MATCH))
        setContentView(root)
        render(true)
    }

    override fun onResume() {
        super.onResume()
        // voltou de outra tela (um dorama, outra pessoa): se os dados mudaram, refaz
        if (seen != -1 && seen != Store.version) render(false)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.screen_back_in, R.anim.screen_back_out)
    }

    // ------------------------------------------------------------------ montagem

    private fun render(animate: Boolean) {
        everyone = Actors.all()
        val a = everyone.firstOrNull { it.key == key }
        if (a == null) {
            finish()
            return
        }
        actor = a
        seen = Store.version
        val y = sv.scrollY
        col.removeAllViews()
        col.addView(buildHero(a), lin(MATCH, WRAP))
        col.addView(buildTiles(a), lin(MATCH, WRAP, t = 12))
        buildGenres(a)?.let { col.addView(it, lin(MATCH, WRAP, t = 12)) }
        val photos = buildPhotos(a)
        photoAnchor = photos
        col.addView(photos, lin(MATCH, WRAP, t = 12))
        val info = buildInfo(a)
        infoAnchor = info
        col.addView(info, lin(MATCH, WRAP, t = 12))
        col.addView(buildDramas(a), lin(MATCH, WRAP, t = 12))
        buildCostars(a)?.let { col.addView(it, lin(MATCH, WRAP, t = 12)) }
        if (animate) {
            col.staggerIn(70L)
        } else {
            sv.post { sv.scrollTo(0, y) }
        }
    }

    private fun scrollToView(v: View?) {
        if (v == null) return
        sv.post { sv.smoothScrollTo(0, maxOf(0, v.top - dp(10))) }
    }

    private fun clip(v: View, radiusDp: Int) {
        v.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, o: Outline) {
                o.setRoundRect(0, 0, view.width, view.height, view.dp(radiusDp).toFloat())
            }
        }
        v.clipToOutline = true
    }

    private fun favWord(g: String): String = when (g) {
        "f" -> "Favorita"
        "m" -> "Favorito"
        else -> "Favorito(a)"
    }

    // ------------------------------------------------------------------ cabeçalho

    private fun buildHero(a: Actor): View {
        val g = a.gender
        val acc = Actors.accent(g)
        val deep = Actors.deep(g)
        val rank = Actors.rankOf(a, everyone)

        val root = FrameLayout(this)
        val bg = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Actors.soft(g), mixColor(Palette.card, acc, if (Palette.dark) 0.12f else 0.06f))
        )
        bg.cornerRadius = dp(32).toFloat()
        bg.setStroke(dp(1), mixColor(acc, Palette.card, 0.5f))
        root.background = bg
        val clipper = FrameLayout(this)
        clip(clipper, 32)
        val petals = PetalsView(this, listOf("petal", "sparkle", "blossom"), acc, 12)
        petals.alpha = 0.5f
        clipper.addView(petals, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(clipper, FrameLayout.LayoutParams(MATCH, MATCH))

        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.gravity = Gravity.CENTER_HORIZONTAL
        c.setPadding(dp(14), dp(14), dp(14), dp(20))
        root.addView(c, FrameLayout.LayoutParams(MATCH, WRAP))

        // botões de cima: voltar, favoritar e editar
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.addView(roundBtn("back", deep, false, 18) { finish() }, lin(dp(40), dp(40)))
        bar.addView(View(this), lin(0, 1, 1f))
        bar.addView(roundBtn("heart", acc, a.favorite, 18) { toggleFavorite() }, lin(dp(40), dp(40), r = 8))
        bar.addView(roundBtn("edit", acc, editing, 16) { toggleEdit() }, lin(dp(40), dp(40)))
        c.addView(bar, lin(MATCH, WRAP))

        // foto grande (toque para escolher outra)
        val big = 136
        val medalRank = rank in 1..3
        val ring = if (medalRank) Actors.medal(rank) else acc
        val ph = FrameLayout(this)
        ph.clipChildren = false
        ph.clipToPadding = false
        val avTop = if (medalRank) 36 else 0
        if (medalRank) {
            // aura (brilho, raios, louros) atrás da foto, com o mesmo centro dela
            val aSize = (big * 1.62f).toInt()
            ph.addView(
                HeroAuraView(this, big, rank),
                FrameLayout.LayoutParams(dp(aSize), dp(aSize), Gravity.TOP or Gravity.CENTER_HORIZONTAL).also {
                    it.topMargin = dp(avTop + big / 2 - aSize / 2)
                }
            )
        }
        ph.addView(
            avatarView(a.photo, big, ring, Actors.soft(g), acc),
            FrameLayout.LayoutParams(dp(big), dp(big), Gravity.TOP or Gravity.CENTER_HORIZONTAL).also { it.topMargin = dp(avTop) }
        )
        if (medalRank) {
            // a base da coroa entra na foto: coroa "usada", não solta no ar
            val crown = CrownView(this, rank, acc)
            crown.elevation = dp(8).toFloat()
            ph.addView(
                crown,
                FrameLayout.LayoutParams(dp(64), dp(50), Gravity.TOP or Gravity.CENTER_HORIZONTAL).also { it.topMargin = dp(avTop + 14 - 47) }
            )
        }
        val cam = FrameLayout(this)
        val cb = GradientDrawable()
        cb.shape = GradientDrawable.OVAL
        cb.setColor(acc)
        cb.setStroke(dp(2), Color.WHITE)
        cam.background = cb
        cam.elevation = dp(6).toFloat()
        cam.addView(IconView(this, "image", Color.WHITE, 16), FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
        val camLp = FrameLayout.LayoutParams(dp(36), dp(36), Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        camLp.topMargin = dp(avTop + big - 38)
        camLp.leftMargin = dp(big / 2 - 16)
        ph.addView(cam, camLp)
        ph.setOnClickListener { scrollToView(photoAnchor) }
        ph.pressable(0.96f)
        c.addView(ph, lin(dp(big + 28), dp(big + avTop), t = 4))

        val nm = label(a.name, 26f, deep, true, true)
        nm.setShadowLayer(dp(5).toFloat(), 0f, 0f, Actors.soft(g))
        nm.gravity = Gravity.CENTER
        nm.maxLines = 2
        nm.ellipsize = TextUtils.TruncateAt.END
        c.addView(nm, lin(MATCH, WRAP, t = 12))

        val fl = FlowLayout(this)
        fl.hGap = dp(8)
        fl.vGap = dp(8)
        fl.center = true
        fl.addView(vividPill(Actors.genderLabel(g), Actors.genderIcon(g), listOf(acc), 12f))
        if (rank > 0) {
            val plural = if (g == "f") "atrizes" else "atores"
            fl.addView(vividPill("Nº $rank das $plural", if (rank <= 3) "trophy" else "star", listOf(if (rank <= 3) Actors.medal(rank) else acc), 12f))
        }
        if (a.favorite) fl.addView(vividPill(favWord(g), "heart", listOf(acc), 12f))
        c.addView(fl, lin(MATCH, WRAP, t = 10))

        val birth = a.info?.birth ?: ""
        if (birth.isNotBlank()) {
            val br = LinearLayout(this)
            br.orientation = LinearLayout.HORIZONTAL
            br.gravity = Gravity.CENTER
            br.addView(IconView(this, "cake", acc, 16))
            br.addView(label(birth, 12.5f, Palette.muted, true), lin(WRAP, WRAP, l = 6))
            c.addView(br, lin(MATCH, WRAP, t = 10))
        }
        val note = a.info?.note ?: ""
        if (note.isNotBlank()) {
            val nt = label(note, 12.5f, Palette.text)
            nt.gravity = Gravity.CENTER
            nt.maxLines = 4
            nt.ellipsize = TextUtils.TruncateAt.END
            c.addView(nt, lin(MATCH, WRAP, t = 10, l = 8, r = 8))
        } else {
            val tag = label("Aparece em " + (if (a.count == 1) "1 dorama" else a.count.toString() + " doramas") + " da sua estante", 12.5f, Palette.muted)
            tag.gravity = Gravity.CENTER
            c.addView(tag, lin(MATCH, WRAP, t = 10))
        }
        return root
    }

    private fun toggleFavorite() {
        val a = actor ?: return
        val info = Store.personInfo(key) ?: PersonInfo(key, a.name)
        info.favorite = !info.favorite
        Store.savePersonInfo(info)
        softToast(if (info.favorite) "Entrou nos favoritos" else "Saiu dos favoritos", Actors.accent(a.gender), "heart")
        render(false)
    }

    private fun toggleEdit() {
        editing = !editing
        render(false)
        if (editing) scrollToView(infoAnchor)
    }

    // ------------------------------------------------------------------ números

    private fun tile(icon: String, value: String, name: String, count: Int?, acc: Int, deep: Int): LinearLayout {
        val b = card(10, 20)
        b.gravity = Gravity.CENTER_HORIZONTAL
        b.addView(IconView(this, icon, acc, 20))
        val v = label(value, 18f, deep, true, true)
        b.addView(v, lin(WRAP, WRAP, t = 4))
        if (count != null) v.countTo(count, 0, 200L, 700L)
        b.addView(label(name, 10.5f, Palette.muted), lin(WRAP, WRAP, t = 1))
        b.pressable(0.95f)
        return b
    }

    private fun buildTiles(a: Actor): View {
        val acc = Actors.accent(a.gender)
        val deep = Actors.deep(a.gender)
        val hours = a.dramas.map { hoursWatched(it) }.sum()
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.addView(tile("film", a.count.toString(), if (a.count == 1) "dorama" else "doramas", a.count, acc, deep), lin(0, WRAP, 1f, r = 6))
        row.addView(tile("star", if (a.avg > 0) "%.1f".format(a.avg) else "-", "nota média", null, acc, deep), lin(0, WRAP, 1f, r = 6))
        row.addView(tile("clock", "%.0f h".format(hours), "assistidas", null, acc, deep), lin(0, WRAP, 1f))
        return row
    }

    private fun buildGenres(a: Actor): View? {
        val acc = Actors.accent(a.gender)
        val byGenre = a.dramas.groupBy { it.genre }.entries.sortedByDescending { it.value.size }
        if (byGenre.isEmpty()) return null
        val c = card(14, 24)
        c.addView(sectionTitle("Gêneros e países", "tag", acc))
        val fg = FlowLayout(this)
        fg.hGap = dp(8)
        fg.vGap = dp(8)
        for (e in byGenre.take(8)) {
            val g = Genres.byKey(e.key)
            fg.addView(vividPill(g.label + "  " + e.value.size, g.icon, listOf(g.primary), 12f))
        }
        c.addView(fg, lin(MATCH, WRAP, t = 12))
        val byCountry = a.dramas.groupBy { it.country }.entries.sortedByDescending { it.value.size }
        val fc = FlowLayout(this)
        fc.hGap = dp(8)
        fc.vGap = dp(8)
        for (e in byCountry.take(6)) {
            if (e.key.isBlank()) continue
            fc.addView(vividPill(e.key + "  " + e.value.size, "flag", listOf(Atmosphere.country(e.key).second), 12f))
        }
        if (fc.childCount > 0) c.addView(fc, lin(MATCH, WRAP, t = 8))
        return c
    }

    // ------------------------------------------------------------------ escolher a foto

    private fun buildPhotos(a: Actor): View {
        val g = a.gender
        val acc = Actors.accent(g)
        val deep = Actors.deep(g)
        val c = card(14, 24)
        c.addView(sectionTitle("Escolha a foto", "image", acc))
        val hint = label(
            "O mesmo nome pode ter fotos diferentes em cada elenco. Toque na que você mais gosta: ela aparece só nos rankings e no perfil. Cada dorama continua com a própria foto no elenco.",
            12f, Palette.muted
        )
        c.addView(hint, lin(MATCH, WRAP, t = 8))

        val ownPath = a.info?.photo ?: ""
        val hasOwn = ownPath.isNotEmpty() && File(ownPath).exists()
        val opts = Actors.photoOptions(a)
        val hs = HorizontalScrollView(this)
        hs.isHorizontalScrollBarEnabled = false
        hs.overScrollMode = View.OVER_SCROLL_NEVER
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        for (o in opts) {
            val sel = o.own || (!hasOwn && o.path == a.photo)
            row.addView(photoTile(a, o, sel, acc, deep), lin(dp(88), WRAP, r = 8))
        }
        row.addView(newPhotoTile(acc, deep), lin(dp(88), WRAP))
        hs.addView(row)
        c.addView(hs, lin(MATCH, WRAP, t = 12))

        if (opts.isEmpty()) {
            c.addView(label("Nenhum elenco tem foto dessa pessoa ainda. Use \"Nova foto\" para escolher uma.", 12f, Palette.muted), lin(MATCH, WRAP, t = 6))
        }
        if (hasOwn) {
            val back = pill("Voltar para a foto automática", Palette.pinkSoft, Palette.pinkDark, 12f, "replay")
            back.setOnClickListener { resetPhoto() }
            c.addView(back, lin(WRAP, WRAP, t = 12))
        }
        return c
    }

    private fun photoTile(a: Actor, o: Actors.PhotoOpt, sel: Boolean, acc: Int, deep: Int): View {
        val t = LinearLayout(this)
        t.orientation = LinearLayout.VERTICAL
        t.gravity = Gravity.CENTER_HORIZONTAL
        val fr = FrameLayout(this)
        val ring = if (sel) acc else mixColor(acc, Palette.card, 0.65f)
        fr.addView(
            avatarView(o.path, 74, ring, Actors.soft(a.gender), acc),
            FrameLayout.LayoutParams(dp(74), dp(74), Gravity.CENTER)
        )
        if (sel) {
            val b = FrameLayout(this)
            val bb = GradientDrawable()
            bb.shape = GradientDrawable.OVAL
            bb.setColor(acc)
            bb.setStroke(dp(2), Color.WHITE)
            b.background = bb
            b.elevation = dp(6).toFloat()
            b.addView(IconView(this, "check", Color.WHITE, 12), FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
            fr.addView(b, FrameLayout.LayoutParams(dp(24), dp(24), Gravity.BOTTOM or Gravity.END))
        }
        t.addView(fr, lin(dp(80), dp(80)))
        val lb = label(o.from, 10.5f, if (sel) deep else Palette.muted, sel)
        lb.gravity = Gravity.CENTER
        lb.maxLines = 1
        lb.ellipsize = TextUtils.TruncateAt.END
        t.addView(lb, lin(MATCH, WRAP, t = 4))
        t.setOnClickListener {
            if (o.own) t.pop(1.2f) else choosePhoto(o)
        }
        t.pressable(0.94f)
        return t
    }

    private fun newPhotoTile(acc: Int, deep: Int): View {
        val t = LinearLayout(this)
        t.orientation = LinearLayout.VERTICAL
        t.gravity = Gravity.CENTER_HORIZONTAL
        val circle = FrameLayout(this)
        val d = GradientDrawable()
        d.shape = GradientDrawable.OVAL
        d.setColor(mixColor(Palette.card, acc, 0.14f))
        d.setStroke(dp(2), acc)
        circle.background = d
        circle.addView(IconView(this, "add", acc, 26), FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
        t.addView(circle, lin(dp(74), dp(74), t = 3))
        val lb = label("Nova foto", 10.5f, deep, true)
        lb.gravity = Gravity.CENTER
        t.addView(lb, lin(MATCH, WRAP, t = 7))
        t.setOnClickListener { pickNewPhoto() }
        t.pressable(0.94f)
        return t
    }

    private fun choosePhoto(o: Actors.PhotoOpt) {
        val a = actor ?: return
        val copy = Store.copyPersonPhoto(o.path)
        if (copy == null) {
            Toast.makeText(this, "Não consegui usar essa foto.", Toast.LENGTH_SHORT).show()
            return
        }
        val info = Store.personInfo(key) ?: PersonInfo(key, a.name)
        val old = info.photo
        info.photo = copy
        Store.savePersonInfo(info)
        if (old.isNotEmpty() && old != copy) Store.deletePersonFile(old)
        softToast("Foto escolhida!", Actors.accent(a.gender), "check")
        render(false)
    }

    private fun resetPhoto() {
        val a = actor ?: return
        val info = Store.personInfo(key) ?: return
        val old = info.photo
        info.photo = ""
        Store.savePersonInfo(info)
        Store.deletePersonFile(old)
        softToast("Voltou para a foto automática", Actors.accent(a.gender), "replay")
        render(false)
    }

    private fun pickNewPhoto() {
        val a = actor ?: return
        val items = arrayOf("Escolher da galeria do celular", "Buscar na internet")
        AlertDialog.Builder(this)
            .setTitle("Nova foto de " + a.name)
            .setItems(items) { _, which ->
                if (which == 0) {
                    val i = Intent(Intent.ACTION_GET_CONTENT)
                    i.type = "image/*"
                    i.addCategory(Intent.CATEGORY_OPENABLE)
                    startActivityForResult(Intent.createChooser(i, "Escolher foto"), 111)
                } else {
                    val q = when (a.gender) {
                        "f" -> a.name + " actress"
                        "m" -> a.name + " actor"
                        else -> a.name
                    }
                    val i = Intent(this, WebPickActivity::class.java)
                    i.putExtra("query", q)
                    i.putExtra("title", "Foto de " + a.name)
                    // resultado da internet volta com o código + 100
                    startActivityForResult(i, 211)
                }
            }
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        var uri: Uri? = data?.data
        var temp: File? = null
        if (requestCode == 211) {
            val path = data?.getStringExtra("path")
            if (path.isNullOrEmpty()) return
            temp = File(path)
            uri = Uri.fromFile(temp)
        } else if (requestCode != 111) {
            return
        }
        if (uri == null) return
        try {
            applyNewPhoto(uri)
        } finally {
            try {
                temp?.delete()
            } catch (e: Exception) {
            }
        }
    }

    private fun applyNewPhoto(uri: Uri) {
        val a = actor ?: return
        val p = Store.savePersonPhoto(uri)
        if (p == null) {
            Toast.makeText(this, "Não consegui abrir essa imagem.", Toast.LENGTH_SHORT).show()
            return
        }
        val info = Store.personInfo(key) ?: PersonInfo(key, a.name)
        val old = info.photo
        info.photo = p
        Store.savePersonInfo(info)
        if (old.isNotEmpty() && old != p) Store.deletePersonFile(old)
        softToast("Foto escolhida!", Actors.accent(a.gender), "check")
        render(false)
    }

    // ------------------------------------------------------------------ informações

    private fun fieldLabel(t: String): android.widget.TextView {
        val v = label(t, 12.5f, Palette.muted, true)
        v.setPadding(dp(2), dp(14), 0, dp(6))
        return v
    }

    private fun infoLine(icon: String, name: String, value: String, acc: Int, dim: Boolean = false): View {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.setPadding(0, dp(10), 0, 0)
        r.addView(IconView(this, icon, acc, 18), lin(WRAP, WRAP, t = 2))
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.addView(label(name, 11f, Palette.muted, true))
        c.addView(label(value, 14f, if (dim) Palette.muted else Palette.text), lin(MATCH, WRAP, t = 1))
        r.addView(c, lin(0, WRAP, 1f, l = 10))
        return r
    }

    private fun buildInfo(a: Actor): View {
        val acc = Actors.accent(a.gender)
        val c = card(14, 24)
        c.addView(sectionTitle("Informações", "person", acc))
        val info = a.info
        if (!editing) {
            c.addView(infoLine("person", "Nome", a.name, acc))
            c.addView(infoLine(Actors.genderIcon(a.gender), "Tipo", Actors.genderLabel(a.gender), acc, a.gender.isEmpty()))
            val birth = info?.birth ?: ""
            c.addView(infoLine("cake", "Nascimento", if (birth.isBlank()) "Não informado" else birth, acc, birth.isBlank()))
            val note = info?.note ?: ""
            c.addView(infoLine("book", "Sobre", if (note.isBlank()) "Nada anotado ainda" else note, acc, note.isBlank()))
            val b = pill("Editar informações", acc, Color.WHITE, 13f, "edit")
            b.setOnClickListener { toggleEdit() }
            c.addView(b, lin(WRAP, WRAP, t = 14))
            return c
        }

        c.addView(fieldLabel("Nome"))
        val nameIn = input("Nome da pessoa", a.name, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        c.addView(nameIn, lin(MATCH, WRAP))
        c.addView(label("Se mudar o nome, ele muda em todos os doramas onde ela(e) aparece.", 11f, Palette.muted), lin(MATCH, WRAP, t = 4))
        c.addView(fieldLabel("Tipo"))
        var gSel = a.gender
        val gOpts = listOf(
            Opt("f", "Atriz", Actors.accent("f"), "heart"),
            Opt("m", "Ator", Actors.accent("m"), "star"),
            Opt("", "Sem classificar", Actors.accent(""), "person")
        )
        c.addView(chipFlow(gOpts, a.gender) { gSel = it }, lin(MATCH, WRAP))
        c.addView(fieldLabel("Nascimento"))
        val birthIn = input("Ex.: 16 de junho de 1987", info?.birth ?: "", InputType.TYPE_CLASS_TEXT)
        c.addView(birthIn, lin(MATCH, WRAP))
        c.addView(fieldLabel("Sobre"))
        val noteIn = input("Anotações, curiosidades, por que você gosta...", info?.note ?: "", InputType.TYPE_CLASS_TEXT, true)
        c.addView(noteIn, lin(MATCH, WRAP))

        val btns = LinearLayout(this)
        btns.orientation = LinearLayout.HORIZONTAL
        val save = bigPill("Salvar", acc, Color.WHITE, 14f, "check")
        save.setOnClickListener { saveInfo(a, nameIn, gSel, birthIn, noteIn) }
        val cancel = bigPill("Cancelar", Palette.pinkSoft, Palette.pinkDark, 14f, "close")
        cancel.setOnClickListener { toggleEdit() }
        btns.addView(save, lin(0, WRAP, 1f, r = 6))
        btns.addView(cancel, lin(0, WRAP, 1f, l = 6))
        c.addView(btns, lin(MATCH, WRAP, t = 18))
        return c
    }

    private fun saveInfo(a: Actor, nameIn: EditText, gender: String, birthIn: EditText, noteIn: EditText) {
        val acc = Actors.accent(gender)
        val nn = nameIn.text.toString().trim()
        if (nn.isEmpty()) {
            softToast("Escreva o nome", acc, "close")
            return
        }
        val info = Store.personInfo(key) ?: PersonInfo(key, a.name)
        info.gender = gender
        info.birth = birthIn.text.toString().trim()
        info.note = noteIn.text.toString().trim()
        Store.savePersonInfo(info)
        val nk = Store.personKey(nn)
        if (nk.isNotEmpty() && nn != a.name) {
            Store.renamePerson(key, nn)
            key = nk
        }
        editing = false
        softToast("Salvo com carinho!", acc, "check")
        render(false)
    }

    // ------------------------------------------------------------------ doramas e parceiros

    private fun buildDramas(a: Actor): View {
        val acc = Actors.accent(a.gender)
        val c = card(14, 24)
        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        head.addView(sectionTitle("Doramas que fez", "film", acc), lin(0, WRAP, 1f))
        head.addView(vividPill(a.count.toString(), "heart", listOf(acc), 12f), lin(WRAP, WRAP))
        c.addView(head, lin(MATCH, WRAP))

        val sorted = a.dramas.sortedWith(Comparator<Drama> { x, y ->
            if (x.score != y.score) y.score - x.score else x.title.compareTo(y.title, true)
        })
        val ids = LongArray(sorted.size) { sorted[it].id }
        for ((i, d) in sorted.withIndex()) {
            val r = dramaRow(d, ids, a.name)
            c.addView(r, lin(MATCH, WRAP, t = 10))
            r.fadeScaleIn(minOf(i, 8) * 50L, 300L)
        }
        return c
    }

    private fun dramaRow(d: Drama, ids: LongArray, listName: String): View {
        val g = Genres.byKey(d.genre)
        val st = Statuses.byKey(d.status)
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(10), dp(10), dp(12), dp(10))
        row.background = roundRect(mixColor(Palette.card, g.primary, 0.07f), dp(20).toFloat(), mixColor(Palette.line, g.primary, 0.4f), dp(1))

        val cv = CoverView(this, 14)
        cv.elevation = 0f
        cv.bind(d, 200)
        row.addView(cv, lin(dp(56), dp(80), r = 12))

        val mid = LinearLayout(this)
        mid.orientation = LinearLayout.VERTICAL
        val tt = label(d.title, 14.5f, Palette.text, true)
        tt.maxLines = 2
        tt.ellipsize = TextUtils.TruncateAt.END
        mid.addView(tt, lin(MATCH, WRAP))
        val bits = ArrayList<String>()
        if (d.kind == "filme") bits.add("Filme")
        if (d.year.isNotBlank()) bits.add(d.year)
        if (d.country.isNotBlank()) bits.add(d.country)
        if (bits.isNotEmpty()) mid.addView(label(bits.joinToString("  ·  "), 11f, Palette.muted), lin(MATCH, WRAP, t = 2))
        val fl = FlowLayout(this)
        fl.hGap = dp(6)
        fl.vGap = dp(6)
        fl.addView(vividPill(st.label, st.icon, listOf(st.color), 10.5f))
        fl.addView(vividPill(g.label, g.icon, listOf(g.primary), 10.5f))
        mid.addView(fl, lin(MATCH, WRAP, t = 6))
        if (d.score > 0) {
            val rv = RatingView(this, 13, false)
            rv.score = d.score
            rv.color = g.primary
            mid.addView(rv, lin(WRAP, WRAP, t = 7))
        } else {
            mid.addView(label("sem nota ainda", 11f, Palette.muted), lin(WRAP, WRAP, t = 7))
        }
        row.addView(mid, lin(0, WRAP, 1f))

        row.setOnClickListener {
            val i = Intent(this, DetailActivity::class.java)
            i.putExtra("id", d.id)
            i.putExtra("listName", listName)
            i.putExtra("ids", ids)
            startActivity(i)
            overridePendingTransition(R.anim.screen_in, R.anim.screen_out_back)
        }
        row.pressable(0.97f)
        return row
    }

    private fun buildCostars(a: Actor): View? {
        val list = Actors.costars(a, everyone, 10)
        if (list.isEmpty()) return null
        val acc = Actors.accent(a.gender)
        val c = card(14, 24)
        c.addView(sectionTitle("Costuma aparecer com", "person", acc))
        val hs = HorizontalScrollView(this)
        hs.isHorizontalScrollBarEnabled = false
        hs.overScrollMode = View.OVER_SCROLL_NEVER
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        for ((x, n) in list) {
            val xa = Actors.accent(x.gender)
            val item = LinearLayout(this)
            item.orientation = LinearLayout.VERTICAL
            item.gravity = Gravity.CENTER_HORIZONTAL
            item.setPadding(dp(4), dp(8), dp(4), dp(4))
            item.addView(avatarView(x.photo, 64, xa, Actors.soft(x.gender), xa), lin(dp(64), dp(64)))
            val nm = label(x.name, 12f, Palette.text, true)
            nm.gravity = Gravity.CENTER
            nm.maxLines = 2
            nm.ellipsize = TextUtils.TruncateAt.END
            item.addView(nm, lin(MATCH, WRAP, t = 6))
            item.addView(vividPill(n.toString() + " juntos", null, listOf(xa), 10f), lin(WRAP, WRAP, t = 4))
            item.setOnClickListener {
                val i = Intent(this, ActorActivity::class.java)
                i.putExtra("key", x.key)
                startActivity(i)
                overridePendingTransition(R.anim.screen_in, R.anim.screen_out_back)
            }
            item.pressable(0.95f)
            row.addView(item, lin(dp(96), WRAP, r = 6))
        }
        hs.addView(row)
        c.addView(hs, lin(MATCH, WRAP, t = 8))
        return c
    }
}
