package com.steam.if97.core

import kotlin.math.ln
import kotlin.math.sqrt

/// IF97 region 2 - superheated and saturated vapour.
/// Basic equation: gamma(pi, tau) = gamma0(pi, tau) + gammar(pi, tau),
/// pi = p / 1 MPa, tau = 540 K / T.
internal object Region2 {

    private const val TAU_STAR = 540.0 // K

    fun properties(pMPa: Double, tK: Double): Props {
        val pi = pMPa
        val tau = TAU_STAR / tK
        val r = If97Constants.R

        // ---- ideal-gas part ------------------------------------------------
        var g0 = ln(pi)
        var g0p = 1.0 / pi
        var g0t = 0.0
        var g0tt = 0.0

        val n0 = If97Tables.R2_N0
        val j0 = If97Tables.R2_J0
        for (k in n0.indices) {
            val nk = n0[k]
            val j = j0[k]
            g0 += nk * MathUtil.powi(tau, j)
            g0t += nk * j * MathUtil.powi(tau, j - 1)
            g0tt += nk * j * (j - 1) * MathUtil.powi(tau, j - 2)
        }

        // ---- residual part -------------------------------------------------
        var gr = 0.0
        var grp = 0.0
        var grpp = 0.0
        var grt = 0.0
        var grtt = 0.0
        var grpt = 0.0

        val n = If97Tables.R2_N
        val ii = If97Tables.R2_I
        val jj = If97Tables.R2_J
        val b = tau - 0.5

        for (k in n.indices) {
            val nk = n[k]
            val i = ii[k]
            val j = jj[k]

            val ai = MathUtil.powi(pi, i)
            val ai1 = MathUtil.powi(pi, i - 1)
            val ai2 = MathUtil.powi(pi, i - 2)
            val bj = MathUtil.powi(b, j)
            val bj1 = MathUtil.powi(b, j - 1)
            val bj2 = MathUtil.powi(b, j - 2)

            gr += nk * ai * bj
            grp += nk * i * ai1 * bj
            grpp += nk * i * (i - 1) * ai2 * bj
            grt += nk * ai * j * bj1
            grtt += nk * ai * j * (j - 1) * bj2
            grpt += nk * i * ai1 * j * bj1
        }

        val v = pi * (g0p + grp) * r * tK / pMPa / 1000.0
        val h = tau * (g0t + grt) * r * tK
        val u = h - 1000.0 * pMPa * v
        val s = r * (tau * (g0t + grt) - (g0 + gr))

        val cp = -r * tau * tau * (g0tt + grtt)
        val a1 = 1.0 + pi * grp
        val a2 = a1 - tau * pi * grpt
        val cv = r * (-tau * tau * (g0tt + grtt) - a2 * a2 / (1.0 - pi * pi * grpp))

        val num = r * tK * 1000.0 * (1.0 + 2.0 * pi * grp + pi * pi * grp * grp)
        val den = 1.0 - pi * pi * grpp + a2 * a2 / (tau * tau * (g0tt + grtt))
        val w = if (num / den > 0.0) sqrt(num / den) else Double.NaN

        val alphaV = a2 / (a1 * tK)
        val kappaT = (1.0 - pi * pi * grpp) / (a1 * pMPa)

        return Props(v, h, u, s, cp, cv, w, alphaV, kappaT)
    }
}
