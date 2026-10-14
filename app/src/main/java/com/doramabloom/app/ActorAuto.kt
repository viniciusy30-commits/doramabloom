package com.doramabloom.app

import android.app.Activity

/**
 * Reconhecimento automático do elenco: para cada pessoa que aparece nos doramas, descobre sozinho
 * (Wikidata) QUEM ela é e se é ator ou atriz. Grafias diferentes da mesma pessoa viram uma só nos pódios,
 * e quem já é reconhecida nunca é pesquisada de novo.
 */
object ActorAuto {
    private const val RETRY_MS = 7L * 24L * 3600L * 1000L

    @Volatile
    var running = false
        private set

    @Volatile
    private var cancel = false

    /** Uma pessoa: chave oficial, grafias do nome e os doramas em que foi adicionada (os mais recentes primeiro). */
    class Job(val key: String, val names: List<String>, val dramas: List<Drama>) {
        val name: String get() = names.first()
    }

    class Summary(
        val searched: Int,
        /** pessoas reconhecidas nesta rodada */
        val found: Int,
        /** ator/atriz definido sozinho */
        val classified: Int,
        /** duplicatas que viraram uma pessoa só */
        val merged: Int,
        /** true se nada respondeu (provavelmente sem internet) */
        val offline: Boolean
    )

    /** Uma entrada por pessoa ainda não reconhecida (ou, com [force], por todas) e que não falhou há pouco tempo. */
    fun jobs(force: Boolean): List<Job> {
        PeopleIdentity.consolidate()
        val spell = LinkedHashMap<String, HashMap<String, Int>>()
        val dramasOf = HashMap<String, ArrayList<Drama>>()
        for (d in Store.all().sortedByDescending { it.addedAt }) {
            val seenHere = HashSet<String>()
            for (p in d.castPeople) {
                val nm = p.name.trim()
                if (nm.isEmpty()) continue
                val k = Store.personKey(nm)
                if (k.isEmpty()) continue
                val m = spell.getOrPut(k) { HashMap() }
                m[nm] = (m[nm] ?: 0) + 1
                if (seenHere.add(PeopleIdentity.canon(k))) dramasOf.getOrPut(PeopleIdentity.canon(k)) { ArrayList() }.add(d)
            }
        }
        val now = System.currentTimeMillis()
        val out = ArrayList<Job>()
        val seen = HashSet<String>()
        for (k in spell.keys) {
            val c = PeopleIdentity.canon(k)
            if (!seen.add(c)) continue
            val members = PeopleIdentity.membersOf(c)
            if (!force) {
                if (members.any { Store.wdOf(it).isNotEmpty() }) continue
                val at = members.maxOf { Store.autoMissAt(it) }
                if (at > 0L && now - at < RETRY_MS) continue
            }
            val names = HashMap<String, Int>()
            for (m in members) spell[m]?.let { for ((n, v) in it) names[n] = (names[n] ?: 0) + v }
            val ordered = names.entries.sortedByDescending { it.value }.map { it.key }
            if (ordered.isEmpty()) continue
            out.add(Job(c, ordered, dramasOf[c] ?: ArrayList()))
        }
        return out
    }

    /** Quantas pessoas ainda faltam reconhecer. */
    fun pendingCount(): Int = jobs(false).size

    /**
     * Guarda o resultado de uma pesquisa: o id do Wikidata (se o nome bateu) e, se o gênero for seguro,
     * ator ou atriz. Chame na thread principal. Devolve true se reconheceu a pessoa.
     */
    fun remember(name: String, r: GenderLookup.Result): Boolean {
        val raw = Store.personKey(name)
        if (raw.isEmpty() || r.qid.isEmpty() || !r.exact) return false
        return rememberGroup(PeopleIdentity.canon(raw), r)
    }

    /** O id vale para todas as grafias da pessoa (o grupo inteiro). */
    fun rememberGroup(canon: String, r: GenderLookup.Result): Boolean {
        if (r.qid.isEmpty() || !r.exact) return false
        for (k in PeopleIdentity.membersOf(canon)) Store.setWd(k, r.qid)
        return true
    }

    private fun classify(canon: String, name: String, r: GenderLookup.Result): Boolean {
        if (r.gender.isEmpty() || !r.exact) return false
        val info = Store.personInfo(canon) ?: PersonInfo(canon, name)
        if (info.gender.isNotEmpty()) return false
        info.gender = r.gender
        Store.savePersonInfo(info)
        return true
    }

    fun stop() {
        cancel = true
    }

    /** Roda em segundo plano; os retornos acontecem na thread principal. */
    fun start(
        act: Activity,
        force: Boolean,
        onProgress: (done: Int, total: Int, name: String) -> Unit,
        onDone: (Summary) -> Unit
    ) {
        if (running) return
        if (force) Store.clearAutoMiss()
        val list = jobs(force)
        if (list.isEmpty()) {
            onDone(Summary(0, 0, 0, 0, false))
            return
        }
        running = true
        cancel = false
        val before = PeopleIdentity.groupCount()
        Thread {
            var found = 0
            var classified = 0
            var answered = 0
            val misses = ArrayList<String>()
            for ((i, j) in list.withIndex()) {
                if (cancel || act.isFinishing) break
                val r = GenderLookup.lookupInDramas(j.names, j.dramas) { }
                val ok = r.qid.isNotEmpty()
                if (ok) answered++
                act.runOnUiThread {
                    if (rememberGroup(j.key, r)) found++ else misses.add(j.key)
                    if (classify(j.key, j.name, r)) classified++
                    onProgress(i + 1, list.size, j.name)
                }
                try {
                    Thread.sleep(150)
                } catch (e: Exception) {
                }
            }
            act.runOnUiThread {
                // só marca "não achei" se a internet respondeu alguma vez (senão é só falta de conexão)
                if (answered > 0) for (k in misses) Store.setAutoMiss(k)
                PeopleIdentity.consolidate()
                val after = PeopleIdentity.groupCount()
                running = false
                onDone(Summary(list.size, found, classified, maxOf(0, before - after), answered == 0 && !cancel))
            }
        }.start()
    }
}
