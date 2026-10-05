package com.happykeys.keyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView

class SetupActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(28))
            setBackgroundColor(Color.parseColor("#FFF6E0"))
        }
        fun title(t: String) = TextView(this).apply {
            text = t; textSize = 28f; setTextColor(Color.parseColor("#E63E8C")); setPadding(0, 0, 0, dp(8))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        fun body(t: String) = TextView(this).apply {
            text = t; textSize = 16f; setTextColor(Color.parseColor("#3B1450")); setPadding(0, dp(6), 0, dp(10))
        }
        fun btn(t: String, onClick: () -> Unit) = Button(this).apply {
            text = t; isAllCaps = false; textSize = 17f
            setOnClickListener { onClick() }
        }

        col.addView(title("Happy Keys"))
        col.addView(body("A musical keyboard where every key plays a tune. English and Arabic.\n\nSet it up in two steps:"))
        col.addView(btn("1. Turn on Happy Keys") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        col.addView(body("Find \"Happy Keys\" in the list and switch it on. Tap OK if your phone shows a warning. That is normal for any keyboard."))
        col.addView(btn("2. Choose Happy Keys") {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        })
        col.addView(body("Pick \"Happy Keys\" from the list. You can switch back any time with the keyboard icon at the bottom right of the screen."))

        col.addView(body("Song:"))
        val spinner = Spinner(this)
        val names = Songs.names
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        spinner.setSelection(names.indexOf(prefs.getString("song", names.first())).coerceAtLeast(0))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                prefs.edit().putString("song", names[pos]).apply()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        col.addView(spinner)

        val sw = Switch(this).apply {
            text = "  Play tunes"; textSize = 16f; isChecked = prefs.getBoolean("sound", true)
            setPadding(0, dp(14), 0, dp(14))
            setOnCheckedChangeListener { _, on -> prefs.edit().putBoolean("sound", on).apply() }
        }
        col.addView(sw)

        col.addView(body("Try it here:"))
        col.addView(EditText(this).apply {
            hint = "Tap here to type"; minLines = 3; gravity = Gravity.TOP or Gravity.START
            setBackgroundColor(Color.WHITE); setPadding(dp(12), dp(12), dp(12), dp(12)); textSize = 20f
        })

        setContentView(ScrollView(this).apply { addView(col) })
    }
}
