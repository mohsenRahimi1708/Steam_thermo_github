package com.steam.if97.core

import kotlin.math.sqrt

/// IF97 region 1 - compressed liquid.
/// 273.15 K <= T <= 623.15 K and p >= psat(T).
/// Basic equation: dimensionless Gibbs free energy gamma(pi, tau).
internal object Region1 {

    private const val PI_STAR = 16.53   // MPa
    private const val TAU_STAR = 1386.0 // K

    fun properties(pMPa: Double, tK: Double): Props {
        val pi = pMPa / PI_STAR
        val tau = TAU_STAR / tK

        val a = 7.1 - pi
        val b = tau - 1.222

        var g = 0.0
        var gp = 0.0
        var gpp = 0.0
        var gt = 0.0
        var gtt = 0.0
        var gpt = 0.0

        val n = If97Tables.R1_N
        val ii = If97Tables.R1_I
        val jj = If97Tables.R1_J

        for (k in n.indices) {
            val nk = n[k]
            val i = ii[k]
            val j = jj[k]

            val ai = MathUtil.powi(a, i)
            val ai1 = MathUtil.powi(a, i - 1)
            val ai2 = MathUtil.powi(a, i - 2)
            val bj = MathUtil.powi(b, j)
            val bj1 = MathUtil.powi(b, j - 1)
            val bj2 = MathUtil.powi(b, j - 2)

            g += nk * ai * bj
            gp -= nk * i * ai1 * bj
            gpp += nk * i * (i - 1) * ai2 * bj
            gt += nk * ai * j * bj1
            gtt += nk * ai * j * (j - 1) * bj2
            gpt -= nk * i * ai1 * j * bj1
        }

        val r = If97Constants.R
        val v = pi * gp * r * tK / pMPa / 1000.0
        val h = tau * gt * r * tK
        val u = h - 1000.0 * pMPa * v
        val s = r * (tau * gt - g)
        val cp = -r * tau * tau * gtt
        val cv = r * (-tau * tau * gtt + (gp - tau * gpt) * (gp - tau * gpt) / gpp)
        val dpdt = gp - tau * gpt
        val w2 = r * tK * 1000.0 * gp * gp / (dpdt * dpdt / (tau * tau * gtt) - gpp)
        val w = if (w2 > 0.0) sqrt(w2) else Double.NaN
        val alphaV = (1.0 - tau * gpt / gp) / tK
        val kappaT = -pi * gpp / gp / pMPa

        return Props(v, h, u, s, cp, cv, w, alphaV, kappaT)
    }
}
