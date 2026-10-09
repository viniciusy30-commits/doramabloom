package com.doramabloom.app

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Descobre se uma pessoa do elenco é ator ou atriz pesquisando no Wikidata (gratuito, sem chave).
 * Tudo aqui é bloqueante: chame sempre em segundo plano.
 */
object GenderLookup {

    /** [gender]: "m", "f" ou "" (não achou). [confidence]: 0 nenhuma, 1 média, 2 alta. */
    class Result(
        val gender: String,
        val confidence: Int,
        val foundName: String,
        val description: String
    )

    private val cache = HashMap<String, Result>()

    private val ACTOR = Regex("\\b(actor|actress)\\b")
    private val PERSON = Regex("\\b(singer|model|idol|presenter|entertainer|comedian|rapper|dancer|host|personality|musician|performer|youtuber|director)\\b")
    private val ASIA = Regex("korean|chinese|japanese|thai|taiwanese|hong kong|vietnamese|filipino|indonesian|malaysian|singaporean")

    fun lookup(name: String, onStep: (String) -> Unit): Result {
        val key = name.trim().lowercase()
        synchronized(cache) { cache[key]?.let { return it } }
        var res = tryName(name.trim(), onStep)
        if (res.gender.isEmpty()) {
            // "Kim Soo-hyun" também pode estar cadastrado como "Soo-hyun Kim"
            val parts = name.trim().split(Regex("\\s+"))
            if (parts.size == 2) {
                val swapped = parts[1] + " " + parts[0]
                onStep("Tentando \"$swapped\"…")
                val r2 = tryName(swapped, onStep)
                if (r2.gender.isNotEmpty()) res = r2
            }
        }
        // só guarda no cache o que deu certo (erro de rede pode ser passageiro)
        if (res.gender.isNotEmpty()) synchronized(cache) { cache[key] = res }
        return res
    }

    private fun tryName(name: String, onStep: (String) -> Unit): Result {
        val none = Result("", 0, "", "")
        onStep("Buscando \"$name\" no Wikidata…")
        val url = "https://www.wikidata.org/w/api.php?action=wbsearchentities&type=item&language=en&uselang=en&limit=10&format=json&search=" +
            URLEncoder.encode(name, "UTF-8")
        val body = get(url) ?: return none
        val arr = try { JSONObject(body).optJSONArray("search") } catch (e: Exception) { null } ?: return none
        if (arr.length() == 0) return none

        var bestId = ""
        var bestLabel = ""
        var bestDesc = ""
        var bestScore = 0
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val desc = o.optString("description", "").lowercase()
            var sc = 0
            if (ACTOR.containsMatchIn(desc)) sc += 4 else if (PERSON.containsMatchIn(desc)) sc += 1
            if (sc == 0) continue
            if (ASIA.containsMatchIn(desc)) sc += 2
            val label = o.optString("label", "")
            if (label.equals(name, ignoreCase = true)) sc += 1
            if (sc > bestScore) {
                bestScore = sc
                bestId = o.optString("id", "")
                bestLabel = label
                bestDesc = o.optString("description", "")
            }
        }
        if (bestId.isEmpty()) return none

        onStep("Achei \"$bestLabel\" (${bestDesc.ifBlank { "pessoa" }}). Conferindo o gênero…")
        val d = bestDesc.lowercase()
        var g = ""
        val ent = get("https://www.wikidata.org/w/api.php?action=wbgetentities&props=claims&format=json&ids=$bestId")
        if (ent != null) {
            try {
                val claims = JSONObject(ent).getJSONObject("entities").getJSONObject(bestId).optJSONObject("claims")
                val p21 = claims?.optJSONArray("P21")
                if (p21 != null && p21.length() > 0) {
                    val qid = p21.getJSONObject(0).getJSONObject("mainsnak").getJSONObject("datavalue").getJSONObject("value").optString("id")
                    g = when (qid) {
                        "Q6581072", "Q1052281" -> "f"
                        "Q6581097", "Q2449503" -> "m"
                        else -> ""
                    }
                }
            } catch (e: Exception) {
            }
        }
        val fromDesc = when {
            Regex("\\bactress\\b").containsMatchIn(d) -> "f"
            Regex("\\bactor\\b").containsMatchIn(d) -> "m"
            else -> ""
        }
        if (g.isEmpty()) g = fromDesc
        if (g.isEmpty()) return none
        // alta: o cadastro do gênero e a descrição (ator/atriz) concordam ou o nome bate certinho
        val conf = if (fromDesc.isNotEmpty() && fromDesc == g) 2 else 1
        return Result(g, conf, bestLabel, bestDesc)
    }

    private fun get(url: String): String? {
        var c: HttpURLConnection? = null
        return try {
            c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 8000
            c.readTimeout = 8000
            c.setRequestProperty("User-Agent", "MyDoramas/1.0 (app pessoal de doramas)")
            if (c.responseCode != 200) null else c.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        } finally {
            c?.disconnect()
        }
    }
}
