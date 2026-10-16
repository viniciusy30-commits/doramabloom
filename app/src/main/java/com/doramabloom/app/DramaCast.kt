package com.doramabloom.app

import org.json.JSONObject
import java.net.URLEncoder

/**
 * Elenco de um dorama no Wikidata: acha o dorama pelo título (e ano) e lê a lista oficial de atores.
 * Procurar a pessoa DENTRO do elenco do dorama em que ela foi adicionada é bem mais certeiro do que
 * procurar só pelo nome. Tudo aqui é bloqueante: chame sempre em segundo plano.
 */
object DramaCast {
    /** Um ator do elenco: id no Wikidata e todos os nomes dele (nome principal e apelidos, em inglês e coreano). */
    class Member(val qid: String, val names: List<String>)

    class Cast(val dramaQid: String, val label: String, val members: List<Member>)

    /**
     * Quem, no elenco oficial, é a pessoa com estas grafias. Primeiro procura o mesmo nome (espaço, hífen, ordem).
     * Se não achar, aceita um nome MUITO parecido (erro de digitação ou outra romanização), mas só quando:
     * divide uma palavra inteira com ele, a diferença é de poucas letras e é o único candidato parecido do elenco.
     * Na dúvida, devolve null (melhor não juntar do que juntar pessoas diferentes).
     */
    fun match(names: List<String>, cast: Cast): Member? {
        for (m in cast.members) {
            if (names.any { n -> m.names.any { PeopleIdentity.sameName(n, it) } }) return m
        }
        var best: Member? = null
        var bestD = 99
        var tie = false
        for (m in cast.members) {
            var d = 99
            for (n in names) for (x in m.names) {
                val v = PeopleIdentity.closeness(n, x)
                if (v < d) d = v
            }
            if (d > 2) continue
            if (d < bestD) {
                best = m
                bestD = d
                tie = false
            } else if (d == bestD) {
                tie = true
            }
        }
        val b = best ?: return null
        if (tie) return null
        return if (bestD <= PeopleIdentity.allowedTypos(names)) b else null
    }

    private val cache = HashMap<Long, Cast>()
    private val KIND = Regex("television series|tv series|drama|web series|miniseries|film|movie|series")
    private val SHOW = Regex("television|tv|drama|series")

    private fun search(title: String): org.json.JSONArray? {
        val hangul = Regex("[\\uAC00-\\uD7A3\\u1100-\\u11FF\\u3130-\\u318F]").containsMatchIn(title)
        val lang = if (hangul) "ko" else "en"
        val url = "https://www.wikidata.org/w/api.php?action=wbsearchentities&type=item&language=$lang&uselang=en&limit=10&format=json&search=" +
            URLEncoder.encode(title, "UTF-8")
        val body = GenderLookup.get(url) ?: return null
        return try {
            JSONObject(body).optJSONArray("search")
        } catch (e: Exception) {
            null
        }
    }

    /** O elenco deste dorama; null se não achou o dorama ou a internet falhou (não guarda, para tentar de novo). */
    fun load(d: Drama, onStep: (String) -> Unit): Cast? {
        synchronized(cache) { cache[d.id]?.let { return it } }
        val titles = listOf(d.title.trim(), d.original.trim()).filter { it.isNotEmpty() }.distinct()
        val year = Regex("\\d{4}").find(d.year)?.value ?: ""

        var bestId = ""
        var bestLabel = ""
        var bestScore = 0
        for (t in titles) {
            onStep("Procurando \"$t\" no Wikidata…")
            val arr = search(t) ?: continue
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val desc = o.optString("description", "").lowercase()
                if (!KIND.containsMatchIn(desc)) continue
                var sc = 1
                if (desc.contains("korean")) sc += 2
                else if (Regex("chinese|japanese|thai|taiwanese|hong kong").containsMatchIn(desc)) sc += 1
                if (SHOW.containsMatchIn(desc)) sc += 1
                if (year.isNotEmpty() && desc.contains(year)) sc += 3
                val label = o.optString("label", "")
                if (label.equals(t, true)) sc += 2
                if (sc > bestScore) {
                    bestScore = sc
                    bestId = o.optString("id", "")
                    bestLabel = label
                }
            }
            if (bestScore >= 6) break
        }
        if (bestId.isEmpty()) return null

        onStep("Lendo o elenco de \"$bestLabel\"…")
        val cl = GenderLookup.get("https://www.wikidata.org/w/api.php?action=wbgetclaims&format=json&property=P161&entity=$bestId") ?: return null
        val ids = ArrayList<String>()
        try {
            val arr = JSONObject(cl).optJSONObject("claims")?.optJSONArray("P161")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val id = arr.optJSONObject(i)?.optJSONObject("mainsnak")?.optJSONObject("datavalue")
                        ?.optJSONObject("value")?.optString("id", "") ?: ""
                    if (id.isNotEmpty() && id !in ids) ids.add(id)
                }
            }
        } catch (e: Exception) {
            return null
        }

        val members = ArrayList<Member>()
        var i = 0
        while (i < ids.size && i < 120) {
            val part = ids.subList(i, minOf(ids.size, i + 50)).joinToString("|")
            val body = GenderLookup.get(
                "https://www.wikidata.org/w/api.php?action=wbgetentities&props=labels|aliases&languages=en|ko&format=json&ids=" +
                    URLEncoder.encode(part, "UTF-8")
            ) ?: return null
            try {
                val ents = JSONObject(body).optJSONObject("entities")
                if (ents != null) {
                    for (q in ents.keys()) {
                        val e = ents.optJSONObject(q) ?: continue
                        val names = ArrayList<String>()
                        val labels = e.optJSONObject("labels")
                        if (labels != null) for (lg in labels.keys()) labels.optJSONObject(lg)?.optString("value", "")?.let { if (it.isNotEmpty()) names.add(it) }
                        val al = e.optJSONObject("aliases")
                        if (al != null) {
                            for (lg in al.keys()) {
                                val a = al.optJSONArray(lg) ?: continue
                                for (k in 0 until a.length()) a.optJSONObject(k)?.optString("value", "")?.let { if (it.isNotEmpty()) names.add(it) }
                            }
                        }
                        if (names.isNotEmpty()) members.add(Member(q, names))
                    }
                }
            } catch (e: Exception) {
                return null
            }
            i += 50
        }
        val cast = Cast(bestId, bestLabel, members)
        synchronized(cache) { cache[d.id] = cast }
        return cast
    }
}
