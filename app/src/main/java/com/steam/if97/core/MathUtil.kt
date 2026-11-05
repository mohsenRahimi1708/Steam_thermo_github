package com.steam.if97.core

/// Numerical helpers shared by the IF97 region implementations.
internal object MathUtil {

    /// Integer power with support for negative exponents and negative bases.
    fun powi(base: Double, exp: Int): Double {
        if (exp == 0) return 1.0
        var n = if (exp < 0) -exp.toLong() else exp.toLong()
        var b = base
        var acc = 1.0
        while (n > 0) {
            if (n and 1L == 1L) acc *= b
            b *= b
            n = n shr 1
        }
        return if (exp < 0) 1.0 / acc else acc
    }
}
