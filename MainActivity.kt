package com.lumaglass.launcher

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private lateinit var launcherView: LauncherView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        launcherView = LauncherView(this)
        setContentView(launcherView)
    }

    override fun onResume() {
        super.onResume()
        if (::launcherView.isInitialized) launcherView.refreshApps()
    }
}

data class LaunchApp(
    val label: String,
    val packageName: String,
    val icon: Drawable
)

class LauncherView(private val ctx: Context) : View(ctx) {
    private val pm = ctx.packageManager
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val apps = mutableListOf<LaunchApp>()
    private var filtered = mutableListOf<LaunchApp>()
    private var search = ""
    private var drawer = false
    private var settings = false
    private var settingsScroll = 0f
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var clock = ""
    private val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

    private val bg = LinearGradient(
        0f, 0f, 0f, 1800f,
        intArrayOf(Color.rgb(10, 5, 35), Color.rgb(37, 10, 76), Color.rgb(73, 22, 150)),
        null, Shader.TileMode.CLAMP
    )

    init {
        textPaint.typeface = Typeface.create("sans", Typeface.NORMAL)
        isFocusable = true
        refreshApps()
        post(object : Runnable {
            override fun run() {
                clock = sdf.format(Date())
                invalidate()
                postDelayed(this, 1000)
            }
        })
    }

    fun refreshApps() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val infos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        val list = infos.mapNotNull {
            val ai = it.activityInfo.applicationInfo
            if (ai.packageName == ctx.packageName) null
            else LaunchApp(
                ai.loadLabel(pm).toString(),
                ai.packageName,
                ai.loadIcon(pm)
            )
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
        apps.clear()
        apps.addAll(list)
        applyFilter()
    }

    private fun applyFilter() {
        filtered = apps.filter {
            search.isBlank() || it.label.contains(search, true)
        }.toMutableList()
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint.apply {
            shader = bg
        })
        paint.shader = null

        // soft decorative glow
        paint.color = Color.argb(55, 153, 73, 255)
        c.drawCircle(width * .10f, height * .82f, width * .55f, paint)
        paint.color = Color.argb(35, 80, 180, 255)
        c.drawCircle(width * .92f, height * .28f, width * .35f, paint)

        drawTopBar(c)
        drawSearch(c)
        drawApps(c)
        drawDock(c)

        if (drawer) drawDrawer(c)
        if (settings) drawSettings(c)
    }

    private fun rounded(c: Canvas, l: Float, t: Float, r: Float, b: Float, radius: Float, alpha: Int = 70) {
        paint.shader = null
        paint.color = Color.argb(alpha, 255, 255, 255)
        c.drawRoundRect(l, t, r, b, radius, radius, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f
        paint.color = Color.argb(80, 255, 255, 255)
        c.drawRoundRect(l, t, r, b, radius, radius, paint)
        paint.style = Paint.Style.FILL
    }

    private fun txt(c: Canvas, s: String, x: Float, y: Float, size: Float, alpha: Int = 255, bold: Boolean = false) {
        textPaint.textSize = size
        textPaint.color = Color.argb(alpha, 255, 255, 255)
        textPaint.typeface = Typeface.create("sans", if (bold) Typeface.BOLD else Typeface.NORMAL)
        c.drawText(s, x, y, textPaint)
    }

    private fun drawTopBar(c: Canvas) {
        txt(c, clock, 28f, 52f, 18f, 245, true)
        txt(c, "LUMA", width / 2f - 28f, 52f, 16f, 230, true)
        txt(c, "⋯", width - 55f, 55f, 32f, 240)
        // tiny status icons
        txt(c, "⌁  ◔  ▰", width - 145f, 30f, 12f, 200)
    }

    private fun drawSearch(c: Canvas) {
        val l = 28f
        val t = 82f
        val r = width - 28f
        rounded(c, l, t, r, t + 54f, 28f, 45)
        txt(c, "⌕", l + 20f, t + 36f, 28f, 220)
        txt(c, if (search.isBlank()) "Search apps" else search, l + 55f, t + 35f, 16f, 210)
    }

    private fun drawApps(c: Canvas) {
        val top = 166f
        val cols = 4
        val cellW = width / cols.toFloat()
        val size = minOf(58f, cellW * .46f)
        val maxRows = 4
        filtered.take(cols * maxRows).forEachIndexed { i, app ->
            val row = i / cols
            val col = i % cols
            val cx = cellW * col + cellW / 2f
            val cy = top + row * 112f
            rounded(c, cx - size / 2, cy, cx + size / 2, cy + size, 18f, 48)
            app.icon.setBounds(
                (cx - size * .36f).toInt(), (cy + size * .14f).toInt(),
                (cx + size * .36f).toInt(), (cy + size * .86f).toInt()
            )
            app.icon.alpha = 255
            app.icon.draw(c)
            var label = app.label
            if (label.length > 11) label = label.take(10) + "…"
            textPaint.textAlign = Paint.Align.CENTER
            txt(c, label, cx, cy + size + 23f, 12f, 225)
            textPaint.textAlign = Paint.Align.LEFT
        }
    }

    private fun drawDock(c: Canvas) {
        val h = 78f
        val l = 22f
        val t = height - h - 28f
        rounded(c, l, t, width - l, t + h, 30f, 52)
        val labels = listOf("⌂", "⌕", "◉", "⚙", "☰")
        labels.forEachIndexed { i, s ->
            val x = l + 42f + i * ((width - 2*l - 84f) / 4f)
            txt(c, s, x - 12f, t + 48f, 27f, 235)
        }
    }

    private fun drawDrawer(c: Canvas) {
        val w = 82f
        val l = width - w - 16f
        val t = 98f
        val b = height - 118f
        rounded(c, l, t, width - 16f, b, 30f, 55)
        val icons = listOf("☰", "⌕", "⌁", "▣", "◐", "♧", "▣", "⌁")
        icons.forEachIndexed { i, s ->
            val y = t + 42f + i * 66f
            txt(c, s, l + 25f, y, 25f, 230)
            if (i == 4) {
                paint.color = Color.argb(25, 255,255,255)
                c.drawCircle(l + 39f, y - 8f, 27f, paint)
            }
        }
    }

    private fun drawSettings(c: Canvas) {
        paint.color = Color.argb(125, 3, 1, 18)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        val l = 30f
        val t = 92f
        val r = width - 30f
        val b = height - 62f
        rounded(c, l, t, r, b, 30f, 85)

        txt(c, "Settings", l + 25f, t + 54f - settingsScroll, 30f, 255, true)
        txt(c, "×", r - 43f, t + 52f - settingsScroll, 30f, 230)

        rounded(c, l + 22f, t + 76f - settingsScroll, r - 22f, t + 130f - settingsScroll, 28f, 45)
        txt(c, "⌕", l + 42f, t + 111f - settingsScroll, 24f, 220)
        txt(c, "Search settings", l + 72f, t + 110f - settingsScroll, 15f, 210)

        val rows = listOf(
            "⌁" to "Network & Internet",
            "▣" to "Connected devices",
            "◐" to "Display",
            "♧" to "Notifications",
            "▱" to "Battery",
            "▤" to "Storage",
            "⌖" to "Location",
            "♿" to "Accessibility",
            "▦" to "Apps",
            "◖" to "Sound & vibration"
        )

        var y = t + 180f - settingsScroll
        rows.forEachIndexed { index, pair ->
            if (index == 2 || index == 6 || index == 8) {
                paint.color = Color.argb(55, 255,255,255)
                c.drawRect(l + 22f, y - 18f, r - 22f, y - 17f, paint)
                y += 15f
            }
            txt(c, pair.first, l + 28f, y, 23f, 225)
            txt(c, pair.second, l + 70f, y, 15f, 235)
            y += 67f
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = e.x
                touchDownY = e.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                val dx = e.x - touchDownX
                val dy = e.y - touchDownY

                if (settings) {
                    if (abs(dy) > 40f) {
                        settingsScroll = (settingsScroll - dy).coerceIn(0f, 260f)
                        invalidate()
                        return true
                    }
                    if (e.x > width - 100 && e.y < 180) {
                        settings = false
                        invalidate()
                        return true
                    }
                    handleSettingsTap(e.x, e.y)
                    return true
                }

                if (drawer) {
                    if (e.x < width - 110) {
                        drawer = false
                        invalidate()
                        return true
                    }
                    if (e.y > 400 && e.y < 530) {
                        settings = true
                        drawer = false
                        invalidate()
                    }
                    return true
                }

                // Search
                if (e.y in 80f..150f) {
                    showSearch()
                    return true
                }

                // right-side menu
                if (e.x > width - 85 && e.y < 90) {
                    drawer = true
                    invalidate()
                    return true
                }

                // dock settings / menu
                if (e.y > height - 130) {
                    if (e.x > width * .55f && e.x < width * .85f) {
                        settings = true
                        invalidate()
                        return true
                    }
                    if (e.x > width * .82f) {
                        drawer = true
                        invalidate()
                        return true
                    }
                }

                // app grid
                if (e.y > 155 && e.y < height - 170) {
                    val cols = 4
                    val cellW = width / cols.toFloat()
                    val col = (e.x / cellW).toInt().coerceIn(0, cols - 1)
                    val row = ((e.y - 166f) / 112f).toInt()
                    val index = row * cols + col
                    if (index in filtered.indices) launch(filtered[index])
                }
                return true
            }
        }
        return true
    }

    private fun showSearch() {
        val input = android.widget.EditText(ctx)
        input.setSingleLine(true)
        input.hint = "Search apps"
        input.setText(search)
        input.setTextColor(Color.WHITE)
        input.setHintTextColor(Color.LTGRAY)
        val dialog = android.app.Dialog(ctx)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setContentView(input)
        dialog.setOnShowListener {
            input.requestFocus()
            (ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }
        input.setOnEditorActionListener { _, _, _ ->
            search = input.text.toString()
            applyFilter()
            dialog.dismiss()
            true
        }
        dialog.show()
        dialog.window?.setLayout((width * .85f).toInt(), 70)
    }

    private fun launch(app: LaunchApp) {
        try {
            val i = pm.getLaunchIntentForPackage(app.packageName)
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(i)
            }
        } catch (_: Exception) {}
    }

    private fun handleSettingsTap(x: Float, y: Float) {
        val localY = y - 92f + settingsScroll
        val index = ((localY - 165f) / 67f).toInt()
        val actions = listOf(
            Settings.ACTION_WIFI_SETTINGS,
            Settings.ACTION_BLUETOOTH_SETTINGS,
            Settings.ACTION_DISPLAY_SETTINGS,
            Settings.ACTION_APP_NOTIFICATION_SETTINGS,
            Settings.ACTION_BATTERY_SAVER_SETTINGS,
            Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
            Settings.ACTION_LOCATION_SOURCE_SETTINGS,
            Settings.ACTION_ACCESSIBILITY_SETTINGS,
            Settings.ACTION_APPLICATION_SETTINGS,
            Settings.ACTION_SOUND_SETTINGS
        )
        if (index in actions.indices) {
            try { ctx.startActivity(Intent(actions[index])) } catch (_: Exception) {}
        }
    }

    private fun abs(v: Float) = kotlin.math.abs(v)
}
