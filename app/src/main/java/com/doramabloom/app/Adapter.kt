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
import androidx.recyclerview.widget.RecyclerView

/** Coraçãozinho branco redondo usado nos pôsteres de favoritos. */
fun Context.favBadge(): FrameLayout {
    val f = FrameLayout(this)
    f.background = ovalGradient(Color.WHITE, Color.WHITE)
    f.elevation = dp(2).toFloat()
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
 * mode 0 = linha da lista, 1 = card do carrossel, 2 = mini pôster horizontal, 3 = pôster da grade.
 */
class DramaAdapter(
    private val mode: Int,
    private val onClick: (Drama) -> Unit,
    private val onPlus: ((Drama) -> Unit)? = null
) : RecyclerView.Adapter<DramaAdapter.VH>() {

    var items: List<Drama> = emptyList()

    fun submit(l: List<Drama>) {
        items = l
        notifyDataSetChanged()
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
        val fav: View? = null
    ) : RecyclerView.ViewHolder(v)

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val c = parent.context
        return when (mode) {
            0 -> buildRow(c)
            1 -> buildCard(c)
            2 -> buildPoster(c, c.dp(112), c.dp(160), false)
            else -> {
                val cell = (c.resources.displayMetrics.widthPixels - c.dp(28)) / 3 - c.dp(8)
                buildPoster(c, MATCH, cell * 3 / 2, true)
            }
        }
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val d = items[position]
        val g = Genres.byKey(d.genre)
        val st = Statuses.byKey(d.status)
        val reqW = when (mode) {
            1 -> 500
            0 -> 260
            2 -> 320
            else -> 360
        }
        h.cover.bind(d, reqW)
        h.title.text = d.title
        h.sub?.text = subtitle(d)
        h.prog?.text = progressText(d)
        val bar = h.bar
        if (bar != null) {
            bar.progress = progressOf(d)
            bar.barColor = g.primary
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
        val plus = h.plus
        if (plus != null) {
            plus.background = roundRect(g.primary, plus.dp(22).toFloat())
            plus.setOnClickListener { onPlus?.invoke(d) }
        }
        h.itemView.setOnClickListener { onClick(d) }
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
        root.elevation = c.dp(2).toFloat()
        val rlp = RecyclerView.LayoutParams(MATCH, WRAP)
        rlp.setMargins(c.dp(4), c.dp(6), c.dp(4), c.dp(6))
        root.layoutParams = rlp

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
        root.elevation = c.dp(4).toFloat()
        val rlp = RecyclerView.LayoutParams(MATCH, WRAP)
        rlp.setMargins(c.dp(6), c.dp(6), c.dp(6), c.dp(10))
        root.layoutParams = rlp

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

    private fun buildPoster(c: Context, wParam: Int, hPx: Int, withBar: Boolean): VH {
        val root = LinearLayout(c)
        root.orientation = LinearLayout.VERTICAL
        val rlp = RecyclerView.LayoutParams(wParam, WRAP)
        if (withBar) rlp.setMargins(c.dp(4), c.dp(4), c.dp(4), c.dp(10)) else rlp.setMargins(0, c.dp(4), c.dp(10), c.dp(8))
        root.layoutParams = rlp

        val frame = FrameLayout(c)
        val cover = CoverView(c, 18)
        cover.elevation = c.dp(3).toFloat()
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
