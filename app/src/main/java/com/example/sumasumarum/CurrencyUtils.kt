package com.example.sumasumarum

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val formatter = NumberFormat.getNumberInstance(Locale("cs", "CZ")).apply {
        maximumFractionDigits = 0
    }

    fun format(amount: Double): String {
        return "${formatter.format(amount)} Kč"
    }
}