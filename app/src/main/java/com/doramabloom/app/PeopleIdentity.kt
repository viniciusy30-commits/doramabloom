package com.doramabloom.app

import java.io.File

/**
 * Descobre quando duas grafias de nome são a MESMA pessoa, para o mesmo ator ou atriz nunca aparecer
 * duas vezes nos pódios.
 *
 * Duas regras juntam nomes:
 *  1. Mesma pessoa no Wikidata: o reconhecimento automático guarda o id de cada grafia (Store.wdOf);
 *     grafias com o mesmo id são a mesma pessoa (ex.: "Kim Soo-hyun" e "Kim Su-hyeon").
 *  2. Mesmas letras, só mudando espaço, hífen ou a ordem do sobrenome (ex.: "Kim Soo-hyun",
 *     "Kim Soohyun" e "Soo Hyun Kim"). Esta regra não vale entre duas pessoas que o Wikidata diz serem
 *     diferentes.
 *
 * Cada grupo tem uma chave "oficial" (canon). Tudo no app que procura uma pessoa usa essa chave.
 */
object PeopleIdentity {
    private var ver = -1
    private val canonOf = HashMap<String, String>()
    private val groupOf = HashMap<String, List<String>>()

    /** As letras do nome coladas, em todas as ordens possíveis ("kim soo hyun" -> kimsoohyun, soohyunkim, hyunkimsoo). */
    private fun rotations(key: String): Set<String> {
        val t = key.split(' ').filter { it.isNotEmpty() }
        if (t.isEmpty()) return emptySet()
        val out = LinkedHashSet<String>()
        for (i in t.indices) out.add((t.drop(i) + t.take(i)).joinToString(""))
        return out
    }

    /** Os dois nomes são a mesma pessoa só pela escrita (sem internet)? */
    fun sameName(a: String, b: String): Boolean {
        val ra = rotations(Store.personKey(a))
        if (ra.isEmpty()) return false
        val rb = rotations(Store.personKey(b))
        return ra.any { it in rb }
    }

    private fun info(k: String): Int {
        val i = Store.rawPersonInfo(k) ?: return 0
        var s = 1
        if (i.gender.isNotEmpty()) s += 4
        if (i.photo.isNotEmpty()) s += 2
        if (i.favorite) s += 2
        if (i.note.isNotEmpty() || i.birth.isNotEmpty()) s += 1
        return s
    }

    private fun rebuild() {
        val v = Store.version
        if (v == ver) return
        ver = v
        canonOf.clear()
        groupOf.clear()

        // quantos doramas cada grafia aparece (uma vez por dorama)
        val count = HashMap<String, Int>()
        for (d in Store.all()) {
            val seen = HashSet<String>()
            for (p in d.castPeople) {
                val k = Store.personKey(p.name)
                if (k.isNotEmpty() && seen.add(k)) count[k] = (count[k] ?: 0) + 1
            }
        }
        val keys = count.keys.sorted()
        if (keys.isEmpty()) return

        val parent = HashMap<String, String>()
        for (k in keys) parent[k] = k
        fun find(x: String): String {
            var r = x
            while (parent[r] != r) r = parent[r]!!
            var c = x
            while (parent[c] != r) {
                val n = parent[c]!!
                parent[c] = r
                c = n
            }
            return r
        }

        // 1) mesma pessoa no Wikidata
        val rootWd = HashMap<String, String>()
        val firstOfQ = HashMap<String, String>()
        for (k in keys) {
            val q = Store.wdOf(k)
            if (q.isEmpty()) continue
            val first = firstOfQ[q]
            if (first == null) {
                firstOfQ[q] = k
            } else {
                val a = find(first)
                val b = find(k)
                if (a != b) parent[b] = a
            }
        }
        for (k in keys) {
            val q = Store.wdOf(k)
            if (q.isNotEmpty()) rootWd[find(k)] = q
        }

        // 2) mesmas letras (espaço, hífen e ordem do sobrenome); nunca junta duas pessoas que o Wikidata separa
        val firstOfRot = HashMap<String, String>()
        for (k in keys) {
            for (r in rotations(k)) {
                val o = firstOfRot[r]
                if (o == null) {
                    firstOfRot[r] = k
                    continue
                }
                val a = find(o)
                val b = find(k)
                if (a == b) continue
                val wa = rootWd[a]
                val wb = rootWd[b]
                if (wa != null && wb != null && wa != wb) continue
                parent[b] = a
                if (wa == null && wb != null) rootWd[a] = wb
            }
        }

        val groups = HashMap<String, ArrayList<String>>()
        for (k in keys) groups.getOrPut(find(k)) { ArrayList() }.add(k)
        for ((_, ms) in groups) {
            val ordered = ms.sortedWith(
                compareByDescending<String> { info(it) }
                    .thenByDescending { count[it] ?: 0 }
                    .thenBy { it }
            )
            val best = ordered.first()
            for (m in ms) canonOf[m] = best
            groupOf[best] = ordered
        }
    }

    /** A chave "oficial" da pessoa; se não for conhecida, a própria chave. */
    fun canon(rawKey: String): String {
        rebuild()
        return canonOf[rawKey] ?: rawKey
    }

    fun canonOfName(name: String): String {
        val k = Store.personKey(name)
        return if (k.isEmpty()) "" else canon(k)
    }

    /** Todas as grafias da mesma pessoa (a oficial primeiro). */
    fun membersOf(key: String): List<String> {
        rebuild()
        val c = canonOf[key] ?: key
        return groupOf[c] ?: listOf(key)
    }

    /** Quantas pessoas diferentes o app reconhece (grupos). */
    fun groupCount(): Int {
        rebuild()
        return groupOf.size
    }

    /**
     * Junta de verdade os perfis das variações no perfil da pessoa "oficial" (foto, gênero, nota, favorita),
     * para que nada fique espalhado. Devolve true se mudou alguma coisa.
     */
    fun consolidate(): Boolean {
        rebuild()
        var changed = false
        for ((best, ms) in groupOf.entries.toList()) {
            if (ms.size < 2) continue
            val others = ms.filter { it != best && Store.rawPersonInfo(it) != null }
            if (others.isEmpty()) continue
            val target = Store.rawPersonInfo(best) ?: PersonInfo(best, Store.rawPersonInfo(others[0])!!.name)
            target.key = best
            for (m in others) {
                val i = Store.rawPersonInfo(m) ?: continue
                if (target.gender.isEmpty()) target.gender = i.gender
                if (target.birth.isEmpty()) target.birth = i.birth
                if (target.note.isEmpty()) target.note = i.note
                if (i.favorite) target.favorite = true
                if (target.rating <= 0.0) target.rating = i.rating
                val ownOk = target.photo.isNotEmpty() && File(target.photo).exists()
                if (!ownOk && i.photo.isNotEmpty() && File(i.photo).exists()) {
                    target.photo = i.photo
                } else if (i.photo.isNotEmpty() && i.photo != target.photo) {
                    Store.deletePersonFile(i.photo)
                }
                Store.removeRawPerson(m)
            }
            Store.putRawPerson(target)
            changed = true
        }
        if (changed) Store.persistPeople()
        return changed
    }
}
