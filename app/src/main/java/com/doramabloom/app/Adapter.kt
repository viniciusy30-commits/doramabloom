package com.doramabloom.app

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.drawable.GradientDrawable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

/** Coraçãozinho branco redondo usado nos pôsteres de favoritos. */
fun Context.favBadge(): FrameLayout {
    val f = FrameLayout(this)
    f.background = ovalGradient(Color.WHITE, Color.WHITE)
    f.elevation = 0f
    val ic = IconView(this, "heart", Palette.pink, 13)
    f.addView(ic, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
    return f
}

private fun setPillIcon(t: TextView, name: String, color: Int) {
    val dr = t.compoundDrawables[0]
    if (dr is IconDrawable) {
        dr.name = name
        dr.color = color
    }
}

/**
 * mode 0 = linha da lista, 1 = card do carrossel, 2 = mini pôster horizontal, 3 = pôster da grade,
 * 4 = destaque grande (ocupa toda a área, usado no Início).
 */
class DramaAdapter(
    private val mode: Int,
    private val onClick: (Drama) -> Unit,
    private val onPlus: ((Drama, View) -> Unit)? = null
) : RecyclerView.Adapter<DramaAdapter.VH>() {

    var items: List<Drama> = emptyList()
    private var sigs: List<String> = emptyList()
    private var lastAnimated = -1

    init {
        setHasStableIds(true)
    }

    private fun sig(d: Drama): String =
        d.title + "|" + d.status + "|" + d.score + "|" + d.favorite + "|" + d.watched + "|" + d.seasonEps +
            "|" + d.genre + "|" + d.cover + "|" + d.country + "|" + d.year

    override fun getItemId(position: Int): Long = items[position].id

    /** Atualiza a lista com animação (entram, saem e se mexem suavemente). */
    fun submit(l: List<Drama>) {
        val oldItems = items
        val oldSigs = sigs
        val newSigs = l.map { sig(it) }
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldItems.size
            override fun getNewListSize(): Int = l.size
            override fun areItemsTheSame(o: Int, n: Int): Boolean = oldItems[o].id == l[n].id
            override fun areContentsTheSame(o: Int, n: Int): Boolean = oldSigs[o] == newSigs[n]
        })
        items = l
        sigs = newSigs
        diff.dispatchUpdatesTo(this)
    }

    /** Atualiza só o item (usado no +1 episódio, sem piscar). */
    fun refresh(d: Drama) {
        val i = items.indexOfFirst { it.id == d.id }
        if (i >= 0) {
            val m = sigs.toMutableList()
            m[i] = sig(d)
            sigs = m
            notifyItemChanged(i, "progress")
        }
    }

    override fun onViewRecycled(holder: VH) {
        holder.itemView.animate().cancel()
        holder.itemView.alpha = 1f
        holder.itemView.translationY = 0f
        super.onViewRecycled(holder)
    }

    class VH(
        v: View,
        val cover: CoverView,
        val title: TextView,
        val sub: TextView? = null,
        val prog: TextView? = null,
        val bar: SoftBar? = null,
        val rating: RatingView? = null,
        val chip: TextView? = null,
        val genreChip: TextView? = null,
        val badge: TextView? = null,
        val plus: TextView? = null,
        val fav: View? = null,
        val scrim: View? = null
    ) : RecyclerView.ViewHolder(v)

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val c = parent.context
        return when (mode) {
            0 -> buildRow(c)
            1 -> buildCard(c)
            2 -> buildPoster(c, c.dp(112), c.dp(160), false)
            4 -> buildFeatured(c)
            else -> {
                val cell = (c.resources.displayMetrics.widthPixels - c.dp(28)) / 3 - c.dp(8)
                buildPoster(c, MATCH, cell * 3 / 2, true)
            }
        }
    }

    override fun onBindViewHolder(h: VH, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val d = items[position]
            val g = Genres.byKey(d.genre)
            val st = Statuses.byKey(d.status)
            h.prog?.text = progressText(d)
            h.prog?.pop(1.05f)
            h.bar?.animateTo(progressOf(d))
            val chip = h.chip
            if (chip != null) {
                chip.text = st.label
                chip.background = roundRect(st.color, chip.dp(14).toFloat())
                setPillIcon(chip, st.icon, Color.WHITE)
            }
            val badge = h.badge
            if (badge != null) styleBadge(badge, d.score, g.primary)
            h.fav?.visibility = if (d.favorite) View.VISIBLE else View.GONE
            return
        }
        super.onBindViewHolder(h, position, payloads)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val d = items[position]
        val g = Genres.byKey(d.genre)
        val st = Statuses.byKey(d.status)
        val reqW = when (mode) {
            1 -> 500
            0 -> 260
            2 -> 320
            4 -> 800
            else -> 360
        }
        h.cover.bind(d, reqW)
        h.title.text = d.title
        h.sub?.text = subtitle(d)
        h.prog?.text = progressText(d)
        val bar = h.bar
        if (bar != null) {
            bar.barColor = g.primary
            bar.progress = progressOf(d)
        }
        val rating = h.rating
        if (rating != null) {
            rating.score = d.score
            rating.color = g.primary
        }
        val chip = h.chip
        if (chip != null) {
            chip.text = st.label
            chip.background = roundRect(st.color, chip.dp(14).toFloat())
            setPillIcon(chip, st.icon, Color.WHITE)
        }
        val gc = h.genreChip
        if (gc != null) {
            gc.text = g.label
            gc.background = roundRect(g.soft, gc.dp(14).toFloat())
            gc.setTextColor(g.dark)
            setPillIcon(gc, g.icon, g.dark)
        }
        val badge = h.badge
        if (badge != null) styleBadge(badge, d.score, g.primary)
        h.fav?.visibility = if (d.favorite) View.VISIBLE else View.GONE
        val scrim = h.scrim
        if (scrim != null) {
            val dk = g.dark
            val deep = Color.argb(215, Color.red(dk), Color.green(dk), Color.blue(dk))
            scrim.background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.TRANSPARENT, deep))
        }
        val plus = h.plus
        if (plus != null) {
            plus.background = roundRect(g.primary, plus.dp(if (mode == 4) 26 else 22).toFloat())
            plus.setOnClickListener { onPlus?.invoke(d, plus) }
        }
        h.itemView.setOnClickListener { onClick(d) }

        // entrada suave em cascata (só na primeira vez que o item aparece)
        if ((mode == 0 || mode == 3) && position > lastAnimated) {
            lastAnimated = position
            h.itemView.riseIn(minOf(position, 8) * 45L, 18, 380L)
        } else {
            h.itemView.alpha = 1f
            h.itemView.translationY = 0f
        }
    }

    private fun smallPill(c: Context, icon: String): TextView {
        val t = c.pill("", Palette.pink, Color.WHITE, 10.5f, icon)
        t.setPadding(c.dp(9), c.dp(4), c.dp(9), c.dp(4))
        return t
    }

    private fun buildRow(c: Context): VH {
        val root = LinearLayout(c)
        root.orientation = LinearLayout.HORIZONTAL
        root.setPadding(c.dp(10), c.dp(10), c.dp(10), c.dp(10))
        root.background = roundRect(Color.WHITE, c.dp(22).toFloat(), Palette.line, c.dp(1))
        root.elevation = 0f
        val rlp = RecyclerView.LayoutParams(MATCH, WRAP)
        rlp.setMargins(c.dp(4), c.dp(6), c.dp(4), c.dp(6))
        root.layoutParams = rlp
        root.pressable(0.97f)

        val cover = CoverView(c, 16)
        root.addView(cover, c.lin(c.dp(76), c.dp(112), r = 12))

        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL

        val trow = LinearLayout(c)
        trow.orientation = LinearLayout.HORIZONTAL
        trow.gravity = Gravity.CENTER_VERTICAL
        val title = c.label("", 16f, Palette.text, true, true)
        title.maxLines = 2
        title.ellipsize = TextUtils.TruncateAt.END
        val badge = c.scoreBadge(32, 14f)
        trow.addView(title, c.lin(0, WRAP, 1f))
        trow.addView(badge, c.lin(WRAP, WRAP, l = 8))
        col.addView(trow, c.lin(MATCH, WRAP))

        val sub = c.label("", 11.5f, Palette.muted)
        sub.maxLines = 1
        sub.ellipsize = TextUtils.TruncateAt.END
        col.addView(sub, c.lin(MATCH, WRAP, t = 2))

        val chips = LinearLayout(c)
        chips.orientation = LinearLayout.HORIZONTAL
        chips.gravity = Gravity.CENTER_VERTICAL
        val gc = smallPill(c, "heart")
        val chip = smallPill(c, "play")
        val fav = IconView(c, "heart", Palette.pink, 15)
        chips.addView(gc, c.lin(WRAP, WRAP, r = 6))
        chips.addView(chip, c.lin(WRAP, WRAP, r = 6))
        chips.addView(fav, c.lin(WRAP, WRAP))
        col.addView(chips, c.lin(MATCH, WRAP, t = 6))

        val prog = c.label("", 11.5f, Palette.muted)
        col.addView(prog, c.lin(WRAP, WRAP, t = 6))
        val bar = SoftBar(c)
        col.addView(bar, c.lin(MATCH, c.dp(7), t = 3))

        root.addView(col, c.lin(0, WRAP, 1f))
        return VH(
            root, cover, title, sub = sub, prog = prog, bar = bar,
            chip = chip, genreChip = gc, badge = badge, fav = fav
        )
    }

    private fun buildCard(c: Context): VH {
        val root = LinearLayout(c)
        root.orientation = LinearLayout.HORIZONTAL
        root.setPadding(c.dp(12), c.dp(12), c.dp(12), c.dp(12))
        root.background = roundRect(Color.WHITE, c.dp(28).toFloat(), Palette.line, c.dp(1))
        root.elevation = 0f
        val rlp = RecyclerView.LayoutParams(MATCH, WRAP)
        rlp.setMargins(c.dp(6), c.dp(6), c.dp(6), c.dp(10))
        root.layoutParams = rlp
        root.pressable(0.97f)

        val cover = CoverView(c, 20)
        root.addView(cover, c.lin(c.dp(116), c.dp(172), r = 14))

        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL

        val trow = LinearLayout(c)
        trow.orientation = LinearLayout.HORIZONTAL
        trow.gravity = Gravity.CENTER_VERTICAL
        val title = c.label("", 19f, Palette.text, true, true)
        title.maxLines = 2
        title.ellipsize = TextUtils.TruncateAt.END
        val fav = IconView(c, "heart", Palette.pink, 18)
        trow.addView(title, c.lin(0, WRAP, 1f))
        trow.addView(fav, c.lin(WRAP, WRAP, l = 6))
        col.addView(trow, c.lin(MATCH, WRAP))

        val gc = smallPill(c, "heart")
        col.addView(gc, c.lin(WRAP, WRAP, t = 6))

        val sub = c.label("", 11.5f, Palette.muted)
        sub.maxLines = 1
        sub.ellipsize = TextUtils.TruncateAt.END
        col.addView(sub, c.lin(MATCH, WRAP, t = 6))

        val rating = RatingView(c, 15, false)
        col.addView(rating, c.lin(WRAP, WRAP, t = 6))

        val prog = c.label("", 12.5f, Palette.text, true)
        col.addView(prog, c.lin(WRAP, WRAP, t = 8))
        val bar = SoftBar(c)
        col.addView(bar, c.lin(MATCH, c.dp(9), t = 4))

        val plus = c.pill("+1 episódio", Palette.pink, Color.WHITE, 13f, "play")
        plus.setPadding(c.dp(14), c.dp(9), c.dp(14), c.dp(9))
        col.addView(plus, c.lin(MATCH, WRAP, t = 10))

        root.addView(col, c.lin(0, WRAP, 1f))
        return VH(
            root, cover, title, sub = sub, prog = prog, bar = bar,
            rating = rating, genreChip = gc, plus = plus, fav = fav
        )
    }

    private fun buildFeatured(c: Context): VH {
        val root = LinearLayout(c)
        root.orientation = LinearLayout.VERTICAL
        root.background = roundRect(Color.WHITE, c.dp(34).toFloat(), Palette.line, c.dp(1))
        root.elevation = 0f
        root.clipToOutline = true
        val rlp = RecyclerView.LayoutParams(MATCH, MATCH)
        rlp.setMargins(c.dp(2), c.dp(4), c.dp(2), c.dp(8))
        root.layoutParams = rlp
        root.pressable(0.985f)

        // ---- capa grande
        val hero = FrameLayout(c)
        val cover = CoverView(c, 0)
        hero.addView(cover, FrameLayout.LayoutParams(MATCH, MATCH))
        val scrim = View(c)
        hero.addView(scrim, FrameLayout.LayoutParams(MATCH, c.dp(170), Gravity.BOTTOM))

        val badge = c.scoreBadge(40, 17f)
        val blp = FrameLayout.LayoutParams(c.dp(40), c.dp(40))
        blp.gravity = Gravity.TOP or Gravity.START
        blp.setMargins(c.dp(14), c.dp(14), 0, 0)
        hero.addView(badge, blp)

        val fav = c.favBadge()
        val flp = FrameLayout.LayoutParams(c.dp(36), c.dp(36))
        flp.gravity = Gravity.TOP or Gravity.END
        flp.setMargins(0, c.dp(14), c.dp(14), 0)
        hero.addView(fav, flp)

        val tcol = LinearLayout(c)
        tcol.orientation = LinearLayout.VERTICAL
        tcol.setPadding(c.dp(18), 0, c.dp(18), c.dp(14))
        val title = c.label("", 26f, Color.WHITE, true, true)
        title.maxLines = 2
        title.ellipsize = TextUtils.TruncateAt.END
        title.setShadowLayer(6f, 0f, 2f, Color.parseColor("#66000000"))
        tcol.addView(title, c.lin(MATCH, WRAP))
        val sub = c.label("", 12.5f, Color.parseColor("#F2FFFFFF"))
        sub.maxLines = 1
        sub.ellipsize = TextUtils.TruncateAt.END
        tcol.addView(sub, c.lin(MATCH, WRAP, t = 2))
        hero.addView(tcol, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM))
        root.addView(hero, c.lin(MATCH, 0, 1f))

        // ---- informações e botão
        val info = LinearLayout(c)
        info.orientation = LinearLayout.VERTICAL
        info.setPadding(c.dp(18), c.dp(14), c.dp(18), c.dp(16))

        val chips = LinearLayout(c)
        chips.orientation = LinearLayout.HORIZONTAL
        chips.gravity = Gravity.CENTER_VERTICAL
        val gc = smallPill(c, "heart")
        val chip = smallPill(c, "play")
        chips.addView(gc, c.lin(WRAP, WRAP, r = 6))
        chips.addView(chip, c.lin(WRAP, WRAP))
        info.addView(chips, c.lin(MATCH, WRAP))

        val prow = LinearLayout(c)
        prow.orientation = LinearLayout.HORIZONTAL
        prow.gravity = Gravity.CENTER_VERTICAL
        val prog = c.label("", 14f, Palette.text, true)
        prog.maxLines = 1
        prog.ellipsize = TextUtils.TruncateAt.END
        prow.addView(prog, c.lin(0, WRAP, 1f, r = 8))
        val rating = RatingView(c, 17, false)
        prow.addView(rating, c.lin(WRAP, WRAP))
        info.addView(prow, c.lin(MATCH, WRAP, t = 12))
        val bar = SoftBar(c)
        info.addView(bar, c.lin(MATCH, c.dp(11), t = 6))

        val plus = c.pill("+1 episódio", Palette.pink, Color.WHITE, 15f, "play")
        plus.setPadding(c.dp(18), c.dp(13), c.dp(18), c.dp(13))
        info.addView(plus, c.lin(MATCH, WRAP, t = 12))
        root.addView(info, c.lin(MATCH, WRAP))

        return VH(
            root, cover, title, sub = sub, prog = prog, bar = bar, rating = rating,
            chip = chip, genreChip = gc, badge = badge, plus = plus, fav = fav, scrim = scrim
        )
    }

    private fun buildPoster(c: Context, wParam: Int, hPx: Int, withBar: Boolean): VH {
        val root = LinearLayout(c)
        root.orientation = LinearLayout.VERTICAL
        val rlp = RecyclerView.LayoutParams(wParam, WRAP)
        if (withBar) rlp.setMargins(c.dp(4), c.dp(4), c.dp(4), c.dp(10)) else rlp.setMargins(0, c.dp(4), c.dp(10), c.dp(8))
        root.layoutParams = rlp
        root.pressable(0.95f)

        val frame = FrameLayout(c)
        val cover = CoverView(c, 18)
        cover.elevation = 0f
        frame.addView(cover, FrameLayout.LayoutParams(MATCH, MATCH))

        val badge = c.scoreBadge(28, 12f)
        val blp = FrameLayout.LayoutParams(WRAP, WRAP)
        blp.gravity = Gravity.BOTTOM or Gravity.START
        blp.setMargins(c.dp(6), 0, 0, c.dp(6))
        frame.addView(badge, blp)

        val fav = c.favBadge()
        val flp = FrameLayout.LayoutParams(c.dp(24), c.dp(24))
        flp.gravity = Gravity.TOP or Gravity.END
        flp.setMargins(0, c.dp(6), c.dp(6), 0)
        frame.addView(fav, flp)

        root.addView(frame, c.lin(MATCH, hPx))

        val title = c.label("", 12f, Palette.text, true)
        title.maxLines = 2
        title.ellipsize = TextUtils.TruncateAt.END
        root.addView(title, c.lin(MATCH, WRAP, t = 6))

        var bar: SoftBar? = null
        if (withBar) {
            val b = SoftBar(c)
            root.addView(b, c.lin(MATCH, c.dp(5), t = 4))
            bar = b
        }
        return VH(root, cover, title, bar = bar, badge = badge, fav = fav)
    }
}
