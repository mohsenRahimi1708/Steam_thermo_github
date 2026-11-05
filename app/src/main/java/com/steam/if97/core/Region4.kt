package com.steam.if97.core

import kotlin.math.pow
import kotlin.math.sqrt

/// IF97 region 4 - the saturation line, plus the B23 region-2/region-3 boundary.
internal object Region4 {

    /// Coefficients of the saturation-pressure equation (IF97 Table 34).
    private val N = doubleArrayOf(
        0.11670521452767e4, -0.72421316703206e6, -0.17073846940092e2,
        0.12020824702470e5, -0.32325550322333e7, 0.14915108613530e2,
        -0.48232657361591e4, 0.40511340542057e6, -0.23855557567849,
        0.65017534844798e3
    )

    /// Coefficients of the B23 boundary (IF97 Eq. 5).
    private const val B23_1 = 0.34805185628969e3
    private const val B23_2 = -0.11671859879975e1
    private const val B23_3 = 0.10192970039326e-2

    /// Saturation pressure in MPa for a temperature in K (273.15 ... 647.096 K).
    fun psatMPa(tK: Double): Double {
        val theta = tK + N[8] / (tK - N[9])
        val a = theta * theta + N[0] * theta + N[1]
        val b = N[2] * theta * theta + N[3] * theta + N[4]
        val c = N[5] * theta * theta + N[6] * theta + N[7]
        return (2.0 * c / (-b + sqrt(b * b - 4.0 * a * c))).pow(4)
    }

    /// Saturation temperature in K for a pressure in MPa (611.657 Pa ... 22.064 MPa).
    fun tsatK(pMPa: Double): Double {
        val beta = pMPa.pow(0.25)
        val e = beta * beta + N[2] * beta + N[5]
        val f = N[0] * beta * beta + N[3] * beta + N[6]
        val g = N[1] * beta * beta + N[4] * beta + N[7]
        val d = 2.0 * g / (-f - sqrt(f * f - 4.0 * e * g))
        return (N[9] + d - sqrt((N[9] + d) * (N[9] + d) - 4.0 * (N[8] + N[9] * d))) / 2.0
    }

    /// Pressure in MPa of the region-2/region-3 boundary at a temperature in K.
    fun pB23MPa(tK: Double): Double = B23_1 + B23_2 * tK + B23_3 * tK * tK
}
