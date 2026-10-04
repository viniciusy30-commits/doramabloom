package com.doramabloom.app

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * mode 0 = linha da lista, mode 1 = card grande do carrossel, mode 2 = mini card horizontal.
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
        val sub: TextView?,
        val prog: TextView?,
        val bar: SoftBar?,
        val hearts: TextView?,
        val chip: TextView?,
        val plus: TextView?,
        val fav: TextView?
    ) : RecyclerView.ViewHolder(v)

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val c = parent.context
        return when (mode) {
            0 -> buildRow(c)
            1 -> buildCard(c)
            else -> buildMini(c)
        }
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val d = items[position]
        val g = Genres.byKey(d.genre)
        val st = Statuses.byKey(d.status)
        val reqW = if (mode == 1) 900 else if (mode == 0) 260 else 320
        val emojiSize = if (mode == 1) 64f else if (mode == 0) 28f else 36f
        h.cover.bind(d, reqW, emojiSize)
        h.title.text = d.title
        h.sub?.text = g.emoji + " " + g.label + " · " + d.country
        h.prog?.text = progressText(d)
        val bar = h.bar
        if (bar != null) {
            bar.progress = progressOf(d)
            bar.barColor = g.primary
        }
        val heartsView = h.hearts
        if (heartsView != null) {
            heartsView.text = hearts(d.rating)
            heartsView.setTextColor(g.primary)
        }
        val chip = h.chip
        if (chip != null) {
            chip.text = st.emoji + " " + st.label
            chip.background = roundRect(st.color, chip.dp(14).toFloat())
        }
        h.fav?.visibility = if (d.favorite) View.VISIBLE else View.GONE
        val plus = h.plus
        if (plus != null) {
            plus.background = roundRect(g.primary, plus.dp(22).toFloat())
            plus.setOnClickListener { onPlus?.invoke(d) }
        }
        h.itemView.setOnClickListener { onClick(d) }
    }

    private fun buildRow(c: Context): VH {
        val root = LinearLayout(c)
        root.orientation = LinearLayout.HORIZONTAL
        root.setPadding(c.dp(10), c.dp(10), c.dp(10), c.dp(10))
        root.background = roundRect(Color.WHITE, c.dp(20).toFloat())
        root.elevation = c.dp(2).toFloat()
        val rlp = RecyclerView.LayoutParams(MATCH, WRAP)
        rlp.setMargins(c.dp(4), c.dp(6), c.dp(4), c.dp(6))
        root.layoutParams = rlp

        val cover = CoverView(c, 14)
        root.addView(cover, c.lin(c.dp(70), c.dp(100), r = 12))

        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL

        val trow = LinearLayout(c)
        trow.orientation = LinearLayout.HORIZONTAL
        trow.gravity = Gravity.CENTER_VERTICAL
        val title = c.label("", 16f, Palette.text, true, true)
        title.maxLines = 2
        title.ellipsize = TextUtils.TruncateAt.END
        val fav = c.label("💖", 14f)
        trow.addView(title, c.lin(0, WRAP, 1f))
        trow.addView(fav, c.lin(WRAP, WRAP, l = 6))
        col.addView(trow, c.lin(MATCH, WRAP))

        val sub = c.label("", 12f, Palette.muted)
        col.addView(sub, c.lin(WRAP, WRAP, t = 2))

        val row2 = LinearLayout(c)
        row2.orientation = LinearLayout.HORIZONTAL
        row2.gravity = Gravity.CENTER_VERTICAL
        val hearts = c.label("", 16f, Palette.pink, true)
        val chip = c.pill("", Palette.pink, Color.WHITE, 11f)
        chip.setPadding(c.dp(10), c.dp(4), c.dp(10), c.dp(4))
        row2.addView(hearts, c.lin(0, WRAP, 1f))
        row2.addView(chip, c.lin(WRAP, WRAP))
        col.addView(row2, c.lin(MATCH, WRAP, t = 4))

        val prog = c.label("", 12f, Palette.muted)
        col.addView(prog, c.lin(WRAP, WRAP, t = 4))
        val bar = SoftBar(c)
        col.addView(bar, c.lin(MATCH, c.dp(7), t = 3))

        root.addView(col, c.lin(0, WRAP, 1f))
        return VH(root, cover, title, sub, prog, bar, hearts, chip, null, fav)
    }

    private fun buildCard(c: Context): VH {
        val root = LinearLayout(c)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(c.dp(12), c.dp(12), c.dp(12), c.dp(14))
        root.background = roundRect(Color.WHITE, c.dp(28).toFloat())
        root.elevation = c.dp(4).toFloat()
        val rlp = RecyclerView.LayoutParams(MATCH, WRAP)
        rlp.setMargins(c.dp(6), c.dp(6), c.dp(6), c.dp(10))
        root.layoutParams = rlp

        val cover = CoverView(c, 22)
        root.addView(cover, c.lin(MATCH, c.dp(250)))

        val trow = LinearLayout(c)
        trow.orientation = LinearLayout.HORIZONTAL
        trow.gravity = Gravity.CENTER_VERTICAL
        val title = c.label("", 22f, Palette.text, true, true)
        title.maxLines = 2
        title.ellipsize = TextUtils.TruncateAt.END
        val fav = c.label("💖", 18f)
        trow.addView(title, c.lin(0, WRAP, 1f))
        trow.addView(fav, c.lin(WRAP, WRAP, l = 6))
        root.addView(trow, c.lin(MATCH, WRAP, t = 10))

        val sub = c.label("", 13f, Palette.muted)
        root.addView(sub, c.lin(WRAP, WRAP, t = 2))

        val hearts = c.label("", 20f, Palette.pink, true)
        root.addView(hearts, c.lin(WRAP, WRAP, t = 4))

        val prog = c.label("", 13f, Palette.text)
        root.addView(prog, c.lin(WRAP, WRAP, t = 6))
        val bar = SoftBar(c)
        root.addView(bar, c.lin(MATCH, c.dp(10), t = 4))

        val plus = c.label("+1 episódio 🌸", 15f, Color.WHITE, true, true)
        plus.gravity = Gravity.CENTER
        plus.setPadding(c.dp(12), c.dp(12), c.dp(12), c.dp(12))
        root.addView(plus, c.lin(MATCH, WRAP, t = 12))

        return VH(root, cover, title, sub, prog, bar, hearts, null, plus, fav)
    }

    private fun buildMini(c: Context): VH {
        val root = LinearLayout(c)
        root.orientation = LinearLayout.VERTICAL
        val rlp = RecyclerView.LayoutParams(c.dp(112), WRAP)
        rlp.setMargins(0, c.dp(4), c.dp(10), c.dp(8))
        root.layoutParams = rlp

        val cover = CoverView(c, 18)
        cover.elevation = c.dp(3).toFloat()
        root.addView(cover, c.lin(c.dp(112), c.dp(158)))

        val title = c.label("", 12f, Palette.text, true)
        title.maxLines = 2
        title.ellipsize = TextUtils.TruncateAt.END
        root.addView(title, c.lin(MATCH, WRAP, t = 6))
        return VH(root, cover, title, null, null, null, null, null, null, null)
    }
}
