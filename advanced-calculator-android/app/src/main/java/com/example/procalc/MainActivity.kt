package com.example.procalc

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.ArrayDeque
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var tvExpression: TextView
    private lateinit var tvResult: TextView

    private var memory: Double = 0.0
    private var expression: StringBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvExpression = findViewById(R.id.tvExpression)
        tvResult = findViewById(R.id.tvResult)
    }

    fun onDigit(v: View) {
        val t = (v as Button).text.toString()
        appendToken(t)
    }

    fun onDot(@Suppress("UNUSED_PARAMETER") v: View) {
        appendToken(".")
    }

    fun onOperator(v: View) {
        val op = (v as Button).text.toString()
        val token = when (op) {
            "×" -> "*"
            "÷" -> "/"
            "−" -> "-"
            else -> op
        }
        appendToken(token, isOperator = true)
    }

    fun onParen(v: View) {
        val s = (v as Button).text.toString()
        appendToken(s)
    }

    fun onFunc(v: View) {
        when ((v as Button).text.toString()) {
            "√" -> applyUnary { sqrt(it) }
            "x²" -> applyUnary { it.pow(2) }
            "1/x" -> applyUnary { if (it == 0.0) Double.NaN else 1.0 / it }
        }
    }

    fun onEquals(@Suppress("UNUSED_PARAMETER") v: View) {
        val exp = tvExpression.text.toString()
        val res = evaluate(exp)
        tvResult.text = format(res)
    }

    fun onClear(@Suppress("UNUSED_PARAMETER") v: View) {
        expression.clear()
        tvExpression.text = ""
        tvResult.text = "0"
    }

    fun onDelete(@Suppress("UNUSED_PARAMETER") v: View) {
        if (expression.isNotEmpty()) {
            expression.deleteCharAt(expression.length - 1)
            tvExpression.text = expression.toString()
        }
    }

    fun onMemory(v: View) {
        when ((v as Button).text.toString()) {
            "MC" -> memory = 0.0
            "MR" -> {
                val m = format(memory)
                appendToken(m)
            }
            "M+" -> {
                val valNow = evaluate(tvExpression.text.toString())
                if (!valNow.isNaN()) memory += valNow
            }
            "M-" -> {
                val valNow = evaluate(tvExpression.text.toString())
                if (!valNow.isNaN()) memory -= valNow
            }
        }
    }

    private fun appendToken(t: String, isOperator: Boolean = false) {
        if (isOperator && expression.isEmpty()) return
        expression.append(t)
        tvExpression.text = expression.toString()
    }

    private fun applyUnary(op: (Double) -> Double) {
        val now = evaluate(tvExpression.text.toString())
        val r = op(now)
        tvResult.text = format(r)
        expression.clear()
        expression.append(format(r))
        tvExpression.text = expression.toString()
    }

    // --- Simple Shunting Yard evaluator supporting + - * / ^ and parentheses ---
    private fun precedence(op: String): Int = when (op) {
        "+", "-" -> 1
        "*", "/" -> 2
        "^" -> 3
        else -> 0
    }

    private fun applyOp(a: Double, b: Double, op: String): Double = when (op) {
        "+" -> a + b
        "-" -> a - b
        "*" -> a * b
        "/" -> if (b == 0.0) Double.NaN else a / b
        "^" -> a.pow(b)
        else -> Double.NaN
    }

    private fun evaluate(input: String): Double {
        if (input.isBlank()) return 0.0
        val tokens = tokenize(input)
        val output = ArrayDeque<String>()
        val ops = ArrayDeque<String>()

        for (t in tokens) {
            when {
                t.isNumber() -> output.addLast(t)
                t == "(" -> ops.addLast(t)
                t == ")" -> {
                    while (ops.isNotEmpty() && ops.last() != "(") {
                        output.addLast(ops.removeLast())
                    }
                    if (ops.isNotEmpty() && ops.last() == "(") ops.removeLast()
                }
                else -> { // operator
                    while (ops.isNotEmpty() && precedence(ops.last()) >= precedence(t)) {
                        output.addLast(ops.removeLast())
                    }
                    ops.addLast(t)
                }
            }
        }
        while (ops.isNotEmpty()) output.addLast(ops.removeLast())

        val stack = ArrayDeque<Double>()
        for (t in output) {
            if (t.isNumber()) {
                stack.addLast(t.toDouble())
            } else {
                val b = stack.removeLastOrNull() ?: return Double.NaN
                val a = stack.removeLastOrNull() ?: return Double.NaN
                stack.addLast(applyOp(a, b, t))
            }
        }
        return stack.lastOrNull() ?: Double.NaN
    }

    private fun tokenize(s: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    val start = i
                    i++
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                    tokens.add(s.substring(start, i))
                }
                c in charArrayOf('+', '-', '*', '/', '^', '(', ')') -> {
                    tokens.add(c.toString())
                    i++
                }
                else -> i++ // ignore unknowns
            }
        }
        return tokens
    }

    private fun String.isNumber(): Boolean =
        this.toDoubleOrNull() != null

    private fun format(x: Double): String =
        if (x.isNaN()) "Error" else if (x % 1.0 == 0.0) x.toLong().toString() else x.toString()
}
