package com.happykeys.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

interface KeyListener {
    fun onText(text: String)
    fun onBackspace()
    fun onEnter()
    fun onTune()
}

private class Placed(val def: KeyDef, val rect: RectF, val color: Int, val pattern: Int)

class KeyboardView(context: Context, private val listener: KeyListener) : View(context) {
    private val d = resources.displayMetrics.density
    private fun dp(v: Float) = v * d

    private val palette = intArrayOf(
        0xFFFF5D73.toInt(), 0xFFFF9F45.toInt(), 0xFFFFD84A.toInt(),
        0xFF59D98E.toInt(), 0xFF4DB8FF.toInt(), 0xFFC27CFF.toInt(),
    )
    private val ink = 0xFF3B1450.toInt()
    private val gold = 0xFFFFC93C.toInt()
    private val cream = 0xFFFFE9A8.toInt()

    private var arabic = false
    private var symbols = false
    private var shift = false

    private var placed: List<Placed> = emptyList()
    private val pressed = HashMap<Int, Placed>()

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val deco = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = ink
    }
    private val icon = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(2.4f)
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()

    private val handler = Handler(Looper.getMainLooper())
    private var repeating: Runnable? = null
    private val longPress = HashMap<Int, Runnable>()
    private val deferred = HashSet<Int>()
    private val fired = HashSet<Int>()
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; color = 0xB33B1450.toInt()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val rowH get() = dp(60f)
    private val pad get() = dp(8f)
    private val gap get() = dp(6f)

    fun setSymbols(on: Boolean) { symbols = on; shift = false; rebuild(); invalidate() }

    private fun rows(): List<List<KeyDef>> = when {
        symbols -> Layouts.symbols(if (arabic) "،" else ",")
        arabic -> Layouts.arLetters
        else -> Layouts.enLetters
    }

    override fun onMeasure(w: Int, h: Int) {
        val width = MeasureSpec.getSize(w)
        setMeasuredDimension(width, (rows().size * rowH + pad * 2 + dp(4f)).toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh); rebuild()
    }

    private fun rebuild() {
        val list = ArrayList<Placed>()
        var n = 0
        val rs = rows()
        val top = pad + dp(4f)
        rs.forEachIndexed { r, row ->
            val total = row.sumOf { it.weight.toDouble() }.toFloat()
            val avail = width - pad * 2 - gap * (row.size - 1)
            val unit = avail / total
            var x = pad
            val y = top + r * rowH
            row.forEach { k ->
                val kw = unit * k.weight
                val rect = RectF(x, y, x + kw, y + rowH - gap)
                if (k.kind == Kind.CHAR) {
                    list.add(Placed(k, rect, palette[n % 6], (n * 7 + n / 6) % 5)); n++
                } else list.add(Placed(k, rect, 0, -1))
                x += kw + gap
            }
        }
        placed = list
    }

    // ---------- drawing ----------
    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        fill.shader = LinearGradient(0f, 0f, 0f, h, 0xFF6D2F9C.toInt(), 0xFF4A1F73.toInt(), Shader.TileMode.CLAMP)
        fill.style = Paint.Style.FILL
        c.drawRect(0f, 0f, w, h, fill)
        fill.shader = null
        deco.style = Paint.Style.FILL; deco.color = 0x14FFFFFF
        var yy = dp(10f)
        while (yy < h) { var xx = dp(9f) * (if (((yy / dp(18f)).toInt()) % 2 == 0) 1 else 2)
            while (xx < w) { c.drawCircle(xx, yy, dp(1.6f), deco); xx += dp(18f) }
            yy += dp(18f) }
        fill.color = gold; c.drawRect(0f, 0f, w, dp(3f), fill)

        val down = pressed.values.toSet()
        for (k in placed) drawKey(c, k, k in down)
    }

    private fun keyPath(k: Placed, r: RectF): Path {
        path.reset()
        val w = r.width(); val h = r.height()
        when {
            k.def.kind == Kind.CHAR -> path.addRoundRect(r, floatArrayOf(
                w * .5f, h * .58f, w * .5f, h * .58f, w * .46f, h * .42f, w * .46f, h * .42f), Path.Direction.CW)
            k.def.kind == Kind.SPACE -> path.addRoundRect(r, h / 2, h / 2, Path.Direction.CW)
            else -> path.addRoundRect(r, dp(14f), dp(14f), Path.Direction.CW)
        }
        return path
    }

    private fun drawKey(c: Canvas, k: Placed, isDown: Boolean) {
        val r = RectF(k.rect)
        if (isDown) { r.inset(r.width() * .04f, r.height() * .04f) }
        val base = when (k.def.kind) {
            Kind.CHAR -> k.color
            Kind.SPACE -> gold
            Kind.SHIFT -> if (shift) 0xFFE63E8C.toInt() else cream
            else -> cream
        }
        val col = if (isDown) lighten(base) else base

        // shadow
        fill.style = Paint.Style.FILL; fill.color = 0x55000000
        c.save(); c.translate(0f, dp(3f)); c.drawPath(keyPath(k, r), fill); c.restore()
        // body
        fill.color = col
        val p = Path(keyPath(k, r))
        c.drawPath(p, fill)

        // decal
        c.save(); c.clipPath(p)
        deco.style = Paint.Style.FILL; deco.color = 0x88FFFFFF.toInt()
        when (k.def.kind) {
            Kind.CHAR -> drawDecal(c, r, k.pattern)
            Kind.SPACE -> { deco.color = 0x55FFFFFF; var x = r.left; while (x < r.right) { c.drawRect(x, r.top, x + dp(6f), r.bottom, deco); x += dp(14f) } }
            else -> { deco.color = 0x99FFFFFF.toInt(); var y = r.top + dp(3f); while (y < r.bottom) { var x = r.left + dp(3f); while (x < r.right) { c.drawCircle(x, y, dp(1.2f), deco); x += dp(9f) }; y += dp(9f) } }
        }
        // shade (bottom-right) + highlight (top-left)
        fill.color = 0x22000000
        c.drawRect(r.left, r.bottom - r.height() * .22f, r.right, r.bottom, fill)
        fill.color = 0x66FFFFFF
        c.drawOval(RectF(r.left + r.width() * .16f, r.top + r.height() * .1f, r.left + r.width() * .36f, r.top + r.height() * .3f), fill)
        c.restore()

        // label / icon
        val cx = r.centerX(); val cy = r.centerY()
        when (k.def.kind) {
            Kind.SPACE -> {}
            Kind.SHIFT -> drawShift(c, cx, cy, dp(9f), if (shift) Color.WHITE else ink)
            Kind.BACKSPACE -> drawBack(c, cx, cy, dp(10f))
            Kind.ENTER -> drawEnter(c, cx, cy, dp(9f))
            else -> {
                val s = label(k.def)
                text.textSize = if (k.def.kind == Kind.CHAR) rowH * .42f else rowH * .30f
                if (s.length > 2 && k.def.kind == Kind.CHAR) text.textSize = rowH * .34f
                text.color = ink
                c.drawText(s, cx, cy - (text.descent() + text.ascent()) / 2, text)
                hintOf(k.def)?.let { h ->
                    hintPaint.textSize = rowH * .22f
                    c.drawText(hintLabel(h), r.centerX() + r.width() * .24f, r.top + rowH * .26f, hintPaint)
                }
            }
        }
    }

    /** Small secondary character: digits on the top row, extra marks/variants elsewhere (Arabic). Long-press types it. */
    private fun hintOf(k: KeyDef): String? {
        if (k.kind != Kind.CHAR || shift || symbols) return null
        return k.hint ?: if (arabic) Layouts.arShift[k.id] else null
    }

    private fun hintLabel(h: String): String =
        if (h.length == 1 && Character.getType(h[0]) == Character.NON_SPACING_MARK.toInt()) "ـ$h" else h

    private fun lighten(col: Int): Int {
        val f = { v: Int -> min(255, v + 45) }
        return Color.rgb(f(Color.red(col)), f(Color.green(col)), f(Color.blue(col)))
    }

    private fun drawDecal(c: Canvas, r: RectF, pattern: Int) {
        when (pattern) {
            0 -> { // polka dots
                val s = dp(11f); var y = r.top
                var row = 0
                while (y < r.bottom + s) { var x = r.left - (if (row % 2 == 0) 0f else s / 2)
                    while (x < r.right + s) { c.drawCircle(x, y, dp(2.2f), deco); x += s }; y += s; row++ }
            }
            1 -> { // diagonal stripes
                deco.style = Paint.Style.STROKE; deco.strokeWidth = dp(4f); deco.color = 0x66FFFFFF
                var x = r.left - r.height()
                while (x < r.right) { c.drawLine(x, r.bottom, x + r.height(), r.top, deco); x += dp(10f) }
                deco.style = Paint.Style.FILL
            }
            2 -> { // stars
                val s = dp(22f); var y = r.top + dp(2f); var row = 0
                while (y < r.bottom + s) { var x = r.left + (if (row % 2 == 0) 0f else s / 2)
                    while (x < r.right + s) { star(c, x, y, dp(5.5f)); x += s }; y += s * .8f; row++ }
            }
            3 -> { // zigzag
                deco.style = Paint.Style.STROKE; deco.strokeWidth = dp(2.4f); deco.color = 0x80FFFFFF.toInt()
                var y = r.top + dp(4f)
                while (y < r.bottom) {
                    path.reset(); path.moveTo(r.left, y)
                    var x = r.left; var up = false
                    while (x < r.right) { x += dp(7f); path.lineTo(x, if (up) y else y + dp(5f)); up = !up }
                    c.drawPath(path, deco); y += dp(11f)
                }
                deco.style = Paint.Style.FILL
            }
            else -> { // squiggles + confetti
                deco.style = Paint.Style.STROKE; deco.strokeWidth = dp(2f); deco.color = 0x99FFFFFF.toInt()
                deco.strokeCap = Paint.Cap.ROUND
                var y = r.top + dp(5f); var row = 0
                while (y < r.bottom) {
                    path.reset(); path.moveTo(r.left, y)
                    var x = r.left
                    while (x < r.right) { path.quadTo(x + dp(4f), y - dp(5f), x + dp(8f), y); path.quadTo(x + dp(12f), y + dp(5f), x + dp(16f), y); x += dp(16f) }
                    c.drawPath(path, deco); y += dp(13f); row++
                }
                deco.style = Paint.Style.FILL
            }
        }
    }

    private fun star(c: Canvas, cx: Float, cy: Float, r: Float) {
        path.reset()
        for (i in 0 until 10) {
            val rad = if (i % 2 == 0) r else r * .45f
            val a = -Math.PI / 2 + i * Math.PI / 5
            val x = cx + (rad * cos(a)).toFloat(); val y = cy + (rad * sin(a)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close(); c.drawPath(path, deco)
    }

    private fun drawShift(c: Canvas, cx: Float, cy: Float, s: Float, col: Int) {
        path.reset()
        path.moveTo(cx, cy - s); path.lineTo(cx + s, cy); path.lineTo(cx + s * .45f, cy)
        path.lineTo(cx + s * .45f, cy + s); path.lineTo(cx - s * .45f, cy + s)
        path.lineTo(cx - s * .45f, cy); path.lineTo(cx - s, cy); path.close()
        fill.style = Paint.Style.FILL; fill.color = col; c.drawPath(path, fill)
    }

    private fun drawBack(c: Canvas, cx: Float, cy: Float, s: Float) {
        icon.color = ink
        path.reset()
        path.moveTo(cx - s, cy); path.lineTo(cx - s * .35f, cy - s * .8f); path.lineTo(cx + s, cy - s * .8f)
        path.lineTo(cx + s, cy + s * .8f); path.lineTo(cx - s * .35f, cy + s * .8f); path.close()
        c.drawPath(path, icon)
        c.drawLine(cx, cy - s * .3f, cx + s * .55f, cy + s * .3f, icon)
        c.drawLine(cx + s * .55f, cy - s * .3f, cx, cy + s * .3f, icon)
    }

    private fun drawEnter(c: Canvas, cx: Float, cy: Float, s: Float) {
        icon.color = ink
        path.reset()
        path.moveTo(cx + s, cy - s * .7f); path.lineTo(cx + s, cy + s * .2f); path.lineTo(cx - s, cy + s * .2f)
        c.drawPath(path, icon)
        path.reset()
        path.moveTo(cx - s * .35f, cy - s * .45f); path.lineTo(cx - s, cy + s * .2f); path.lineTo(cx - s * .35f, cy + s * .85f)
        c.drawPath(path, icon)
    }

    private fun label(k: KeyDef): String = when (k.kind) {
        Kind.CHAR -> if (shift) {
            if (arabic) Layouts.arShift[k.id] ?: k.id else Layouts.enShift[k.id] ?: k.id.uppercase()
        } else k.id
        Kind.LANG -> "EN ع"
        Kind.SYMBOLS -> "123"
        Kind.LETTERS -> if (arabic) "أبج" else "abc"
        else -> ""
    }

    // ---------- touch ----------
    private fun hit(x: Float, y: Float): Placed? {
        var best: Placed? = null; var bd = Float.MAX_VALUE
        for (k in placed) {
            val r = k.rect
            val dx = if (x < r.left - gap / 2) r.left - x else if (x > r.right + gap / 2) x - r.right else 0f
            val dy = if (y < r.top) r.top - y else if (y > r.bottom + gap) y - r.bottom - gap else 0f
            val dist = dx * dx + dy * dy
            if (dist < bd) { bd = dist; best = k }
        }
        return if (bd <= dp(12f) * dp(12f)) best else null
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                val k = hit(e.getX(i), e.getY(i)) ?: return true
                pressed[e.getPointerId(i)] = k
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                val id = e.getPointerId(i)
                val h = hintOf(k.def)
                if (h != null) {
                    // typed on release, or the hint character if held
                    deferred.add(id)
                    listener.onTune()
                    val r = Runnable {
                        fired.add(id); listener.onText(h)
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                    longPress[id] = r
                    handler.postDelayed(r, 380)
                } else press(k)
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val i = e.actionIndex
                val id = e.getPointerId(i)
                val k = pressed.remove(id)
                longPress.remove(id)?.let { handler.removeCallbacks(it) }
                if (deferred.remove(id)) {
                    val wasFired = fired.remove(id)
                    if (!wasFired && k != null && e.actionMasked != MotionEvent.ACTION_CANCEL) {
                        listener.onText(label(k.def))
                        if (shift) { shift = false; invalidate() }
                    }
                }
                if (k?.def?.kind == Kind.BACKSPACE) stopRepeat()
                if (e.actionMasked == MotionEvent.ACTION_CANCEL) {
                    pressed.clear(); stopRepeat()
                    longPress.values.forEach { handler.removeCallbacks(it) }; longPress.clear(); deferred.clear(); fired.clear()
                }
                invalidate()
            }
        }
        return true
    }

    private fun press(k: Placed) {
        when (k.def.kind) {
            Kind.CHAR -> {
                listener.onText(label(k.def)); listener.onTune()
                if (shift) { shift = false; invalidate() }
            }
            Kind.SPACE -> { listener.onText(" "); listener.onTune() }
            Kind.ENTER -> { listener.onEnter(); listener.onTune() }
            Kind.BACKSPACE -> { listener.onBackspace(); listener.onTune(); startRepeat() }
            Kind.SHIFT -> { shift = !shift; invalidate() }
            Kind.LANG -> { arabic = !arabic; symbols = false; shift = false; pressed.clear(); rebuild(); requestLayout(); invalidate() }
            Kind.SYMBOLS -> { symbols = true; shift = false; pressed.clear(); rebuild(); requestLayout(); invalidate() }
            Kind.LETTERS -> { symbols = false; shift = false; pressed.clear(); rebuild(); requestLayout(); invalidate() }
        }
    }

    private fun startRepeat() {
        stopRepeat()
        val r = object : Runnable {
            override fun run() { listener.onBackspace(); handler.postDelayed(this, 55) }
        }
        repeating = r
        handler.postDelayed(r, 420)
    }

    private fun stopRepeat() { repeating?.let { handler.removeCallbacks(it) }; repeating = null }

    override fun onDetachedFromWindow() { stopRepeat(); super.onDetachedFromWindow() }
}
