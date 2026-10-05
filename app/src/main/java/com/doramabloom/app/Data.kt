package com.doramabloom.app

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class Genre(
    val key: String,
    val label: String,
    val icon: String,
    val primary: Int,
    val soft: Int,
    val dark: Int,
    val petals: List<String>,
    val tagline: String,
    val custom: Boolean = false
)

/** Mistura duas cores (f = 0 fica em a, f = 1 fica em b). */
fun mixColor(a: Int, b: Int, f: Float): Int {
    val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * f).toInt()
    val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * f).toInt()
    val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * f).toInt()
    return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), bl.coerceIn(0, 255))
}

object Genres {
    private fun c(s: String): Int = Color.parseColor(s)

    private val builtin: List<Genre> = listOf(
        Genre("romance", "Romance", "heart", c("#FF6B9D"), c("#FFE4EE"), c("#A3305B"),
            listOf("heart", "petal", "ring"), "Para suspirar abraçada na almofada"),
        Genre("comedia", "Comédia", "smile", c("#FFB84D"), c("#FFF3D6"), c("#8A5A00"),
            listOf("smile", "balloon", "star"), "Risadinhas garantidas"),
        Genre("acao", "Ação", "bolt", c("#FF7A6B"), c("#FFE5E0"), c("#9C2F22"),
            listOf("bolt", "flame", "shield"), "Coração acelerado e muita adrenalina"),
        Genre("terror", "Terror", "ghost", c("#9B7EDE"), c("#EDE6FA"), c("#4B2E8F"),
            listOf("ghost", "bat", "tomb"), "Luz acesa e coberta até o nariz"),
        Genre("fantasia", "Fantasia", "sparkle", c("#6FA8FF"), c("#E3EEFF"), c("#1F4F9C"),
            listOf("sparkle", "butterfly", "wand"), "Magia, brilho e mundos encantados"),
        Genre("historico", "Histórico", "pagoda", c("#D6536D"), c("#FBE3E8"), c("#8E1F3A"),
            listOf("pagoda", "lantern", "fan"), "Hanboks, palácios e intrigas da corte"),
        Genre("misterio", "Mistério", "search", c("#5FB3A8"), c("#DFF4F1"), c("#1F6B62"),
            listOf("search", "key", "lock"), "Quem será o culpado?"),
        Genre("escolar", "Escolar", "school", c("#7FD1AE"), c("#E1F7EC"), c("#226B4B"),
            listOf("school", "book", "pencil"), "Uniformes, amizades e primeiros amores"),
        Genre("drama", "Drama", "drop", c("#C38BD8"), c("#F6E6FB"), c("#6E2F86"),
            listOf("drop", "cloud", "heartbreak"), "Lencinho por perto, vai ter choro"),
        Genre("suspense", "Suspense", "eye", c("#4F6D9A"), c("#E3EAF5"), c("#24395A"),
            listOf("eye", "moon", "clock"), "Segura a respiração, a trama não dá trégua"),
        Genre("medico", "Médico", "cross", c("#3FB6C9"), c("#DDF4F8"), c("#17657A"),
            listOf("cross", "pill", "syringe"), "Plantões, jalecos e corações em tratamento"),
        Genre("familia", "Família", "home", c("#F29B5C"), c("#FFEBDC"), c("#96501A"),
            listOf("home", "cake", "gift"), "Mesa farta, abraço apertado e muito afeto"),
        Genre("musical", "Musical", "music", c("#E36BC4"), c("#FDE4F6"), c("#8A2A72"),
            listOf("music", "mic", "headphones"), "Melodias que grudam no coração"),
        Genre("esporte", "Esporte", "trophy", c("#5DB56E"), c("#E1F5E5"), c("#226B35"),
            listOf("trophy", "medal", "ball"), "Suor, garra e superação em campo"),
        Genre("realeza", "Realeza", "crown", c("#E0A93B"), c("#FFF1CC"), c("#7A5A00"),
            listOf("crown", "gem", "castle"), "Coroas, tronos e segredos do palácio"),
        Genre("scifi", "Ficção científica", "rocket", c("#6C63FF"), c("#E7E5FF"), c("#2E2A99"),
            listOf("rocket", "planet", "ufo"), "Futuro, viagens no tempo e mistérios do espaço"),
        Genre("vida", "Vida real", "coffee", c("#C79A7B"), c("#F6E9DF"), c("#6B4630"),
            listOf("coffee", "leaf", "sun"), "Cotidiano gostoso, café quentinho e paz"),
        Genre("vinganca", "Vingança", "skull", c("#B03A5B"), c("#F8DDE5"), c("#5E1128"),
            listOf("skull", "dagger", "hourglass"), "Frieza, planos e a hora do acerto de contas")
    )

    private var extra: List<Genre> = emptyList()

    /** Os gêneros de fábrica e depois os que você criou. */
    val all: List<Genre> get() = builtin + extra

    fun custom(): List<Genre> = extra

    fun setCustom(l: List<Genre>) {
        extra = l
    }

    fun exists(k: String): Boolean = all.any { it.key == k }

    fun byKey(k: String): Genre = all.firstOrNull { it.key == k } ?: builtin[0]

    /** Monta um gênero novo a partir de nome, ícone e cor; o resto (tons claro e escuro) sai da cor. */
    fun makeCustom(key: String, label: String, icon: String, primary: Int, tagline: String): Genre =
        Genre(
            key, label, icon, primary,
            mixColor(primary, Color.WHITE, 0.84f),
            mixColor(primary, Color.BLACK, 0.42f),
            listOf(icon, "sparkle", "petal").distinct(),
            if (tagline.isBlank()) "Seu gênero $label" else tagline,
            true
        )
}

/** Três tons do gênero para as pétalas: uma mistura rica em vez de uma cor só. */
fun genreTints(g: Genre): List<Int> = listOf(
    g.primary,
    mixColor(g.primary, g.dark, 0.32f),
    mixColor(g.primary, Color.WHITE, 0.28f)
)

data class Status(val key: String, val label: String, val icon: String, val color: Int)

object Statuses {
    val all: List<Status> = listOf(
        Status("assistindo", "Assistindo", "play", Color.parseColor("#6FA8FF")),
        Status("quero", "Quero ver", "bookmark", Color.parseColor("#C38BD8")),
        Status("concluido", "Concluído", "check", Color.parseColor("#5CC6A0")),
        Status("pausado", "Pausado", "pause", Color.parseColor("#FFB84D")),
        Status("dropado", "Dropado", "close", Color.parseColor("#B7A3AE"))
    )

    fun byKey(k: String): Status = all.firstOrNull { it.key == k } ?: all[1]
}

val countries: List<String> = listOf(
    "Coreia do Sul", "Japão", "China", "Tailândia", "Taiwan", "Outro"
)

data class Drama(
    var id: Long,
    var title: String,
    var original: String,
    var synopsis: String,
    var country: String,
    var genre: String,
    var tags: List<String>,
    var status: String,
    var score: Int,
    var seasonEps: List<Int>,
    var watched: List<Int>,
    var epMinutes: Int,
    var year: String,
    var platform: String,
    var cast: String,
    var couple: String,
    var startDate: Long,
    var endDate: Long,
    var rewatch: Int,
    var notes: String,
    var favorite: Boolean,
    var cover: String,
    var addedAt: Long,
    var link: String = "",
    var lastUrl: String = "",
    var watchSeason: Int = -1
)

fun totalEps(d: Drama): Int = d.seasonEps.sum()
fun watchedEps(d: Drama): Int = d.watched.sum()
fun seasonCount(d: Drama): Int = d.seasonEps.size

fun allTotalsKnown(d: Drama): Boolean = d.seasonEps.all { it > 0 }

fun seasonDone(d: Drama, i: Int): Boolean =
    i in d.seasonEps.indices && d.seasonEps[i] > 0 && d.watched[i] >= d.seasonEps[i]

fun currentSeason(d: Drama): Int {
    for (i in d.seasonEps.indices) {
        val t = d.seasonEps[i]
        if (t == 0 || d.watched[i] < t) return i
    }
    return maxOf(0, d.seasonEps.size - 1)
}

/**
 * Temporada em que você está: a mais avançada que já tem episódio assistido;
 * se ela já terminou e existe a próxima, é a próxima. (Não depende mais da aba escolhida no
 * navegador, que antes ficava presa na T1.)
 */
fun activeSeason(d: Drama): Int {
    var last = -1
    for (i in d.seasonEps.indices) if (d.watched[i] > 0) last = i
    if (last < 0) return 0
    if (seasonDone(d, last) && last + 1 < d.seasonEps.size) return last + 1
    return last
}

/** Quantos episódios ainda faltam (só conta temporadas com total conhecido). */
fun remainingEps(d: Drama): Int {
    var r = 0
    for (i in d.seasonEps.indices) if (d.seasonEps[i] > 0) r += maxOf(0, d.seasonEps[i] - d.watched[i])
    return r
}

/** "Temp. 2" ou "" quando só há uma temporada. */
fun seasonTag(d: Drama): String = if (seasonCount(d) > 1) "Temp. " + (activeSeason(d) + 1) else ""

/** "Ep. 3/12" da temporada atual. */
fun epTag(d: Drama): String {
    val s = activeSeason(d)
    val t = d.seasonEps[s]
    return if (t > 0) "Ep. " + d.watched[s] + "/" + t else "Ep. " + d.watched[s]
}

/** Texto do contador do navegador, ex.: "T2 · Ep. 5/12". */
fun counterText(d: Drama, s: Int): String {
    val t = d.seasonEps[s]
    val w = d.watched[s]
    val ep = if (t > 0) "Ep. $w/$t" else "Ep. $w"
    return if (seasonCount(d) > 1) "T${s + 1} · $ep" else ep
}

fun progressText(d: Drama): String {
    val t = totalEps(d)
    val w = watchedEps(d)
    if (seasonCount(d) <= 1) {
        return if (t > 0) "Ep. $w de $t" else "Ep. $w"
    }
    val cs = activeSeason(d)
    val st = d.seasonEps[cs]
    val part = if (st > 0) "${d.watched[cs]}/$st" else "${d.watched[cs]}"
    return "Temp. ${cs + 1} · Ep. $part"
}

fun progressOf(d: Drama): Float {
    var t = 0
    var w = 0
    for (i in d.seasonEps.indices) {
        if (d.seasonEps[i] > 0) {
            t += d.seasonEps[i]
            w += minOf(d.watched[i], d.seasonEps[i])
        }
    }
    return if (t > 0) w.toFloat() / t.toFloat() else 0f
}

fun subtitle(d: Drama): String {
    val parts = ArrayList<String>()
    parts.add(d.country)
    if (d.year.isNotBlank()) parts.add(d.year)
    if (seasonCount(d) > 1) parts.add(seasonCount(d).toString() + " temp.")
    val t = totalEps(d)
    if (t > 0) parts.add(t.toString() + " eps")
    return parts.joinToString(" · ")
}

fun hoursWatched(d: Drama): Double =
    watchedEps(d) * (if (d.epMinutes > 0) d.epMinutes else 60) / 60.0

fun normalize(d: Drama) {
    val e = d.seasonEps.toMutableList()
    val w = d.watched.toMutableList()
    if (e.isEmpty()) e.add(0)
    while (w.size < e.size) w.add(0)
    while (w.size > e.size) w.removeAt(w.size - 1)
    for (i in e.indices) {
        if (e[i] < 0) e[i] = 0
        if (w[i] < 0) w[i] = 0
        if (e[i] > 0 && w[i] > e[i]) w[i] = e[i]
    }
    d.seasonEps = e
    d.watched = w
    if (d.watchSeason < -1 || d.watchSeason >= e.size) d.watchSeason = -1
}

fun applyStatus(d: Drama, key: String) {
    d.status = key
    val now = System.currentTimeMillis()
    if (key == "assistindo" && d.startDate == 0L) d.startDate = now
    if (key == "concluido") {
        val w = ArrayList<Int>()
        for (i in d.seasonEps.indices) {
            w.add(if (d.seasonEps[i] > 0) d.seasonEps[i] else d.watched[i])
        }
        d.watched = w
        if (d.startDate == 0L) d.startDate = now
        if (d.endDate == 0L) d.endDate = now
    }
}

private fun intsToJson(l: List<Int>): JSONArray {
    val a = JSONArray()
    for (v in l) a.put(v)
    return a
}

private fun jsonInts(a: JSONArray?): List<Int> {
    val r = ArrayList<Int>()
    if (a != null) {
        for (i in 0 until a.length()) r.add(a.optInt(i, 0))
    }
    return r
}

private fun jsonStrings(a: JSONArray?): List<String> {
    val r = ArrayList<String>()
    if (a != null) {
        for (i in 0 until a.length()) r.add(a.optString(i, ""))
    }
    return r
}

private fun Drama.toJson(): JSONObject {
    val o = JSONObject()
    o.put("id", id)
    o.put("title", title)
    o.put("original", original)
    o.put("synopsis", synopsis)
    o.put("country", country)
    o.put("genre", genre)
    val ta = JSONArray()
    for (t in tags) ta.put(t)
    o.put("tags", ta)
    o.put("status", status)
    o.put("score", score)
    o.put("seasonEps", intsToJson(seasonEps))
    o.put("watched", intsToJson(watched))
    o.put("epMinutes", epMinutes)
    o.put("year", year)
    o.put("platform", platform)
    o.put("cast", cast)
    o.put("couple", couple)
    o.put("startDate", startDate)
    o.put("endDate", endDate)
    o.put("rewatch", rewatch)
    o.put("notes", notes)
    o.put("favorite", favorite)
    o.put("cover", cover)
    o.put("addedAt", addedAt)
    o.put("link", link)
    o.put("lastUrl", lastUrl)
    o.put("watchSeason", watchSeason)
    return o
}

private fun dramaFromJson(o: JSONObject): Drama {
    var eps = jsonInts(o.optJSONArray("seasonEps"))
    var wat = jsonInts(o.optJSONArray("watched"))
    if (eps.isEmpty()) {
        // versão antiga: um único bloco de episódios
        eps = listOf(o.optInt("epTotal", 0))
        wat = listOf(o.optInt("epWatched", 0))
    }
    val score = if (o.has("score")) o.optInt("score", 0) else o.optInt("rating", 0) * 2
    val d = Drama(
        id = o.optLong("id", System.currentTimeMillis()),
        title = o.optString("title", ""),
        original = o.optString("original", ""),
        synopsis = o.optString("synopsis", ""),
        country = o.optString("country", countries[0]),
        genre = o.optString("genre", "romance"),
        tags = jsonStrings(o.optJSONArray("tags")),
        status = o.optString("status", "quero"),
        score = score,
        seasonEps = eps,
        watched = wat,
        epMinutes = o.optInt("epMinutes", 0),
        year = o.optString("year", ""),
        platform = o.optString("platform", ""),
        cast = o.optString("cast", ""),
        couple = o.optString("couple", ""),
        startDate = o.optLong("startDate", 0L),
        endDate = o.optLong("endDate", 0L),
        rewatch = o.optInt("rewatch", 0),
        notes = o.optString("notes", ""),
        favorite = o.optBoolean("favorite", false),
        cover = o.optString("cover", ""),
        addedAt = o.optLong("addedAt", System.currentTimeMillis()),
        link = o.optString("link", ""),
        lastUrl = o.optString("lastUrl", ""),
        watchSeason = o.optInt("watchSeason", -1)
    )
    normalize(d)
    return d
}

object Store {
    private const val PREF = "doramabloom"
    private const val KEY = "dramas"

    private lateinit var prefs: SharedPreferences
    lateinit var appContext: Context
    private val list = ArrayList<Drama>()
    private var loaded = false

    /** Sobe a cada gravação; as telas usam para saber se precisam se atualizar. */
    var version = 0
        private set

    fun init(c: Context) {
        if (loaded) return
        appContext = c.applicationContext
        prefs = appContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        loadGenres()
        load()
        loaded = true
    }

    const val DEFAULT_HOME = "https://www.google.com"

    var textZoom: Int
        get() = prefs.getInt("textZoom", 100)
        set(v) {
            prefs.edit().putInt("textZoom", v).apply()
        }

    var homeUrl: String
        get() {
            val u = prefs.getString("homeUrl", DEFAULT_HOME) ?: DEFAULT_HOME
            return if (u.isBlank()) DEFAULT_HOME else u
        }
        set(v) {
            prefs.edit().putString("homeUrl", v).apply()
        }

    var userName: String
        get() = prefs.getString("userName", "") ?: ""
        set(v) {
            prefs.edit().putString("userName", v).apply()
        }

    var askedName: Boolean
        get() = prefs.getBoolean("askedName", false)
        set(v) {
            prefs.edit().putBoolean("askedName", v).apply()
        }

    private const val GKEY = "customGenres"

    private fun genreToJson(g: Genre): JSONObject {
        val o = JSONObject()
        o.put("key", g.key)
        o.put("label", g.label)
        o.put("icon", g.icon)
        o.put("color", g.primary)
        o.put("tagline", g.tagline)
        return o
    }

    private fun loadGenres() {
        val r = ArrayList<Genre>()
        try {
            val arr = JSONArray(prefs.getString(GKEY, "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val k = o.optString("key", "")
                val l = o.optString("label", "")
                if (k.isBlank() || l.isBlank()) continue
                r.add(
                    Genres.makeCustom(
                        k, l, o.optString("icon", "heart"),
                        o.optInt("color", Palette.pink), o.optString("tagline", "")
                    )
                )
            }
        } catch (e: Exception) {
            // dados corrompidos: sem gêneros próprios
        }
        Genres.setCustom(r)
    }

    private fun persistGenres() {
        version++
        val arr = JSONArray()
        for (g in Genres.custom()) arr.put(genreToJson(g))
        prefs.edit().putString(GKEY, arr.toString()).apply()
    }

    fun addGenre(g: Genre) {
        val l = ArrayList(Genres.custom())
        l.removeAll { it.key == g.key }
        l.add(g)
        Genres.setCustom(l)
        persistGenres()
    }

    /** Apaga um gênero criado por você; os doramas dele voltam para Romance. */
    fun removeGenre(key: String) {
        Genres.setCustom(Genres.custom().filter { it.key != key })
        var changed = false
        for (d in list) {
            if (d.genre == key) {
                d.genre = "romance"
                changed = true
            }
            if (d.tags.contains(key)) {
                d.tags = d.tags.filter { it != key }
                changed = true
            }
        }
        persistGenres()
        if (changed) persist()
    }

    fun setWatchSeason(d: Drama, s: Int) {
        d.watchSeason = s
        persist()
    }

    private fun load() {
        list.clear()
        try {
            val arr = JSONArray(prefs.getString(KEY, "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                list.add(dramaFromJson(arr.getJSONObject(i)))
            }
        } catch (e: Exception) {
            // dados corrompidos: começa vazio
        }
    }

    private fun persist() {
        version++
        val arr = JSONArray()
        for (d in list) arr.put(d.toJson())
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun all(): List<Drama> = list.toList()

    fun get(id: Long): Drama? = list.firstOrNull { it.id == id }

    fun save(d: Drama) {
        normalize(d)
        val i = list.indexOfFirst { it.id == d.id }
        if (i >= 0) list[i] = d else list.add(0, d)
        persist()
    }

    fun delete(id: Long) {
        val d = get(id) ?: return
        if (d.cover.isNotEmpty()) {
            try {
                File(d.cover).delete()
            } catch (e: Exception) {
            }
            Covers.clear()
        }
        list.removeAll { it.id == id }
        persist()
    }

    /** Soma um episódio na temporada que você está assistindo (ou na primeira em andamento). Devolve true se acabou de concluir o dorama. */
    fun bump(d: Drama): Boolean {
        val w = d.watched.toMutableList()
        var target = -1
        val a = activeSeason(d)
        val ta = d.seasonEps[a]
        if (ta == 0 || w[a] < ta) {
            target = a
        } else {
            for (i in d.seasonEps.indices) {
                val t = d.seasonEps[i]
                if (t == 0 || w[i] < t) {
                    target = i
                    break
                }
            }
        }
        if (target < 0) return false
        w[target] = w[target] + 1
        d.watched = w
        return afterProgress(d)
    }

    /** Ajusta os episódios de uma temporada específica (delta positivo ou negativo). */
    fun adjust(d: Drama, season: Int, delta: Int): Boolean {
        if (season < 0 || season >= d.seasonEps.size) return false
        val w = d.watched.toMutableList()
        val t = d.seasonEps[season]
        var nv = w[season] + delta
        if (nv < 0) nv = 0
        if (t > 0 && nv > t) nv = t
        w[season] = nv
        d.watched = w
        if (delta < 0 && d.status == "concluido") d.status = "assistindo"
        return afterProgress(d)
    }

    private fun afterProgress(d: Drama): Boolean {
        var finished = false
        val total = totalEps(d)
        if (watchedEps(d) > 0 && total > 0 && d.seasonEps.all { it > 0 } && watchedEps(d) >= total) {
            if (d.status != "concluido") finished = true
            applyStatus(d, "concluido")
        } else if (watchedEps(d) > 0 && d.status != "assistindo" && d.status != "concluido") {
            applyStatus(d, "assistindo")
        }
        persist()
        return finished
    }

    fun setStatus(d: Drama, key: String) {
        applyStatus(d, key)
        persist()
    }

    fun persistNow() {
        persist()
    }

    fun exportJson(): String {
        val arr = JSONArray()
        for (d in list) {
            val o = d.toJson()
            o.put("cover", "")
            arr.put(o)
        }
        val ga = JSONArray()
        for (g in Genres.custom()) ga.put(genreToJson(g))
        val root = JSONObject()
        root.put("genres", ga)
        root.put("dramas", arr)
        return root.toString()
    }

    /** Importa um backup (novo ou antigo). Devolve quantos foram adicionados, ou -1 se o texto for inválido. */
    fun importJson(s: String): Int {
        return try {
            val t = s.trim()
            val arr: JSONArray
            if (t.startsWith("{")) {
                val root = JSONObject(t)
                val ga = root.optJSONArray("genres")
                if (ga != null) {
                    val cur = ArrayList(Genres.custom())
                    for (i in 0 until ga.length()) {
                        val o = ga.getJSONObject(i)
                        val k = o.optString("key", "")
                        val l = o.optString("label", "")
                        if (k.isBlank() || l.isBlank() || Genres.exists(k)) continue
                        cur.add(
                            Genres.makeCustom(
                                k, l, o.optString("icon", "heart"),
                                o.optInt("color", Palette.pink), o.optString("tagline", "")
                            )
                        )
                    }
                    Genres.setCustom(cur)
                    persistGenres()
                }
                arr = root.optJSONArray("dramas") ?: JSONArray()
            } else {
                arr = JSONArray(t)
            }
            var n = 0
            for (i in 0 until arr.length()) {
                val d = dramaFromJson(arr.getJSONObject(i))
                if (d.title.isBlank()) continue
                if (list.any { it.title.equals(d.title, true) && it.year == d.year }) continue
                d.cover = ""
                d.id = System.currentTimeMillis() + i
                list.add(d)
                n++
            }
            persist()
            n
        } catch (e: Exception) {
            -1
        }
    }

    /** Copia a imagem escolhida para a pasta do app (reduzida) e devolve o caminho. */
    fun saveCover(uri: Uri): String? {
        return try {
            val cr = appContext.contentResolver
            val bounds = BitmapFactory.Options()
            bounds.inJustDecodeBounds = true
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > 1400 || bounds.outHeight / sample > 1400) sample *= 2
            val opts = BitmapFactory.Options()
            opts.inSampleSize = sample
            var bmp: Bitmap = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
            var rot = 0
            try {
                cr.openInputStream(uri)?.use { s ->
                    val ex = ExifInterface(s)
                    val o = ex.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    rot = when (o) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                }
            } catch (e: Exception) {
            }
            if (rot != 0) {
                val m = Matrix()
                m.postRotate(rot.toFloat())
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            }
            val dir = File(appContext.filesDir, "covers")
            dir.mkdirs()
            val f = File(dir, "c" + System.currentTimeMillis() + ".jpg")
            FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            f.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
