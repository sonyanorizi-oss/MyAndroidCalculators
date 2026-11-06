package com.calc2600.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.text.DecimalFormat
import kotlin.math.abs

class MainActivity : Activity() {
    private lateinit var expressionView: TextView
    private lateinit var resultView: TextView
    private val expression = StringBuilder()
    private var justEvaluated = false
    private val backgroundColor = Color.rgb(18, 20, 24)
    private val numberColor = Color.rgb(42, 46, 53)
    private val functionColor = Color.rgb(55, 61, 70)
    private val accentColor = Color.rgb(167, 210, 255)
    private val textColor = Color.rgb(245, 247, 250)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        window.decorView.systemUiVisibility = 0
        buildCalculator()
    }

    private fun buildCalculator() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            setPadding(dp(20), dp(18), dp(20), dp(18))
        }

        val brand = TextView(this).apply {
            text = "Calc 2600"
            textSize = 16f
            setTextColor(Color.rgb(170, 178, 190))
            letterSpacing = 0.04f
        }
        root.addView(brand, LinearLayout.LayoutParams(-1, dp(42)))

        val displayArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM or Gravity.END
            setPadding(dp(4), dp(8), dp(4), dp(26))
        }
        expressionView = TextView(this).apply {
            textSize = 23f
            gravity = Gravity.END
            setTextColor(Color.rgb(155, 164, 177))
            maxLines = 2
        }
        resultView = TextView(this).apply {
            text = "0"
            textSize = 48f
            gravity = Gravity.END
            setTextColor(textColor)
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            maxLines = 2
            includeFontPadding = true
        }
        displayArea.addView(expressionView, LinearLayout.LayoutParams(-1, -2))
        displayArea.addView(resultView, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(displayArea, LinearLayout.LayoutParams(-1, 0, 1f))

        val keys = listOf(
            listOf("AC", "⌫", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("±", "0", ".", "=")
        )

        keys.forEachIndexed { rowIndex, row ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            row.forEach { key ->
                val button = Button(this).apply {
                    text = key
                    textSize = if (key == "AC" || key == "⌫") 18f else 23f
                    isAllCaps = false
                    setTextColor(when (key) {
                        "AC", "⌫", "%", "÷", "×", "−", "+" -> accentColor
                        "=" -> Color.rgb(15, 28, 42)
                        else -> textColor
                    })
                    background = roundedBackground(
                        when (key) {
                            "=" -> accentColor
                            "AC", "⌫", "%", "÷", "×", "−", "+" -> functionColor
                            else -> numberColor
                        },
                        100
                    )
                    stateListAnimator = null
                    setPadding(0, 0, 0, 0)
                    setOnClickListener { onKey(key) }
                }
                val params = LinearLayout.LayoutParams(0, dp(70), 1f)
                params.setMargins(dp(4), dp(5), dp(4), dp(5))
                rowLayout.addView(button, params)
            }
            root.addView(rowLayout, LinearLayout.LayoutParams(-1, dp(80)))
        }

        setContentView(root)
    }

    private fun onKey(key: String) {
        when (key) {
            "AC" -> {
                expression.clear()
                expressionView.text = ""
                resultView.text = "0"
                justEvaluated = false
            }
            "⌫" -> {
                if (justEvaluated) {
                    expression.clear()
                    justEvaluated = false
                } else if (expression.isNotEmpty()) {
                    expression.deleteCharAt(expression.length - 1)
                }
                resultView.text = if (expression.isEmpty()) "0" else expression.toString()
            }
            "=" -> calculate()
            "±" -> toggleSign()
            else -> {
                if (justEvaluated && key.firstOrNull()?.isDigit() == true) {
                    expression.clear()
                } else if (justEvaluated && key in listOf("+", "−", "×", "÷", "%")) {
                    expression.clear()
                    expression.append("2600")
                }
                justEvaluated = false
                if (key == "." && currentNumberHasDecimal()) return
                expression.append(key)
                resultView.text = expression.toString()
            }
        }
    }

    private fun toggleSign() {
        if (justEvaluated) {
            expression.clear()
            justEvaluated = false
        }
        val text = expression.toString()
        val lastNumber = Regex("([0-9.]+)$").find(text)
        if (lastNumber != null) {
            val start = lastNumber.range.first
            val before = text.substring(0, start)
            val number = lastNumber.value
            val replacement = if (before.endsWith("−")) {
                before.dropLast(1) + number
            } else {
                before + "−" + number
            }
            expression.clear()
            expression.append(replacement)
        } else if (expression.isEmpty()) {
            expression.append("−")
        }
        resultView.text = if (expression.isEmpty()) "0" else expression.toString()
    }

    private fun currentNumberHasDecimal(): Boolean {
        val current = expression.toString().substringAfterLast(Regex("[+−×÷%]"))
        return current.contains(".")
    }

    private fun calculate() {
        if (expression.isEmpty()) return
        try {
            val source = expression.toString()
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
            val value = ExpressionParser(source).parse()
            if (!value.isFinite()) throw IllegalArgumentException("Invalid result")
            expressionView.text = expression.toString() + " ="
            if (abs(value - 2600.0) < 0.0000001) {
                resultView.text = "حمید از ریشه خرابه"
                resultView.textSize = 28f
            } else {
                resultView.textSize = 48f
                resultView.text = formatNumber(value)
            }
            expression.clear()
            expression.append(formatNumber(value))
            justEvaluated = true
        } catch (_: Exception) {
            resultView.text = "خطا"
            resultView.textSize = 36f
            justEvaluated = true
        }
    }

    private fun formatNumber(value: Double): String {
        if (abs(value) < 0.0000000001) return "0"
        val rounded = if (abs(value - value.toLong()) < 0.0000001) value.toLong().toDouble() else value
        return DecimalFormat("#,##0.##########").format(rounded)
    }

    private fun roundedBackground(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

private class ExpressionParser(private val input: String) {
    private var position = 0

    fun parse(): Double {
        val result = expression()
        skipSpaces()
        if (position != input.length) error("Unexpected character")
        return result
    }

    private fun expression(): Double {
        var value = term()
        while (true) {
            skipSpaces()
            value = when {
                eat('+') -> value + term()
                eat('-') -> value - term()
                else -> return value
            }
        }
    }

    private fun term(): Double {
        var value = factor()
        while (true) {
            skipSpaces()
            value = when {
                eat('*') -> value * factor()
                eat('/') -> value / factor()
                eat('%') -> value / 100.0
                else -> return value
            }
        }
    }

    private fun factor(): Double {
        skipSpaces()
        if (eat('+')) return factor()
        if (eat('-')) return -factor()
        if (eat('(')) {
            val value = expression()
            skipSpaces()
            if (!eat(')')) error("Missing closing bracket")
            return value
        }
        val start = position
        while (position < input.length &&
            (input[position].isDigit() || input[position] == '.')) position++
        if (start == position) error("Number expected")
        return input.substring(start, position).toDouble()
    }

    private fun eat(char: Char): Boolean {
        if (position < input.length && input[position] == char) {
            position++
            return true
        }
        return false
    }

    private fun skipSpaces() {
        while (position < input.length && input[position].isWhitespace()) position++
    }
}
