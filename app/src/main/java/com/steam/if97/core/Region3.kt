package com.steam.if97.core

import kotlin.math.ln
import kotlin.math.sqrt

/// IF97 region 3 - high-pressure / near-critical region.
/// 623.15 K <= T <= 863.15 K and p >= pB23(T), plus the narrow strip between
/// the saturation line and 623.15 K at p > 16.529 MPa.
///
/// Basic equation: Helmholtz free energy phi(delta, tau) with
/// delta = rho / 322 kg/m3 and tau = 647.096 K / T. Because the state is
/// entered as (p, T), the density is obtained by inverting p(rho, T).
internal object Region3 {

    private const val N1 = 1.0658070028513
    private const val TAU_STAR = 647.096
    private const val RHO_STAR = 322.0

    private class Phi(
        val phi: Double,
        val d: Double,
        val dd: Double,
        val t: Double,
        val tt: Double,
        val dt: Double
    )

    private fun phi(delta: Double, tau: Double): Phi {
        var f = N1 * ln(delta)
        var fd = N1 / delta
        var fdd = -N1 / (delta * delta)
        var ft = 0.0
        var ftt = 0.0
        var fdt = 0.0

        val n = If97Tables.R3_N
        val ii = If97Tables.R3_I
        val jj = If97Tables.R3_J

        for (k in n.indices) {
            val nk = n[k]
            val i = ii[k]
            val j = jj[k]

            val di = MathUtil.powi(delta, i)
            val di1 = MathUtil.powi(delta, i - 1)
            val di2 = MathUtil.powi(delta, i - 2)
            val tj = MathUtil.powi(tau, j)
            val tj1 = MathUtil.powi(tau, j - 1)
            val tj2 = MathUtil.powi(tau, j - 2)

            f += nk * di * tj
            fd += nk * i * di1 * tj
            fdd += nk * i * (i - 1) * di2 * tj
            ft += nk * di * j * tj1
            ftt += nk * di * j * (j - 1) * tj2
            fdt += nk * i * di1 * j * tj1
        }

        return Phi(f, fd, fdd, ft, ftt, fdt)
    }

    /// Pressure in MPa for a given density (kg/m3) and temperature (K).
    fun pressureAt(rho: Double, tK: Double): Double {
        val p = phi(rho / RHO_STAR, TAU_STAR / tK)
        return rho / RHO_STAR * p.d * If97Constants.R * tK * rho / 1000.0
    }

    /// Solves p(rho, T) = p for the liquid-like (`liquid = true`, rho > rho_c)
    /// or vapour-like branch.
    fun density(pMPa: Double, tK: Double, liquid: Boolean): Double =
        if (liquid) solveLiquid(pMPa, tK) else solveVapour(pMPa, tK)

    private fun solveVapour(pMPa: Double, tK: Double): Double {
        // First crossing at increasing density is the vapour root.
        var lo = 1e-6
        var hi = 1e-3
        var guard = 0
        while (pressureAt(hi, tK) < pMPa && guard++ < 600) {
            lo = hi
            hi *= 1.5
        }
        return bisect(lo, hi, pMPa, tK)
    }

    private fun solveLiquid(pMPa: Double, tK: Double): Double {
        var lo = If97Constants.RHOC
        var hi = 1500.0
        if (pressureAt(lo, tK) > pMPa) {
            // Unusual input: the target pressure lies below the loop at rho_c.
            var guard = 0
            hi = lo
            lo *= 0.9
            while (pressureAt(lo, tK) > pMPa && guard++ < 600) {
                hi = lo
                lo *= 0.9
                if (lo < 1.0) {
                    lo = 1.0
                    break
                }
            }
        }
        return bisect(lo, hi, pMPa, tK)
    }

    private fun bisect(lo0: Double, hi0: Double, pMPa: Double, tK: Double): Double {
        var lo = lo0
        var hi = hi0
        repeat(200) {
            val mid = sqrt(lo * hi)
            if (pressureAt(mid, tK) > pMPa) hi = mid else lo = mid
        }
        return 0.5 * (lo + hi)
    }

    fun properties(pMPa: Double, tK: Double, liquid: Boolean): Props {
        val rho = density(pMPa, tK, liquid)
        val delta = rho / RHO_STAR
        val tau = TAU_STAR / tK
        val f = phi(delta, tau)
        val r = If97Constants.R

        val a = 2.0 * delta * f.d + delta * delta * f.dd
        val b = delta * f.d - delta * tau * f.dt

        val v = 1.0 / rho
        val h = r * tK * (tau * f.t + delta * f.d)
        val u = r * tK * tau * f.t
        val s = r * (tau * f.t - f.phi)
        val cp = r * (-tau * tau * f.tt + b * b / a)
        val cv = -r * tau * tau * f.tt
        val w2 = r * tK * 1000.0 * (a - b * b / (tau * tau * f.tt))
        val w = if (w2 > 0.0) sqrt(w2) else Double.NaN
        // alpha = (1/v)(dv/dT)_p and kappa_T = -(1/v)(dv/dp)_T from the
        // Helmholtz derivatives; note the (2 phi_d + delta phi_dd) denominator,
        // which is a/delta.
        val alphaV = (f.d - tau * f.dt) / (tK * (2.0 * f.d + delta * f.dd))
        val kappaT = 1000.0 / (rho * r * tK * a)

        return Props(v, h, u, s, cp, cv, w, alphaV, kappaT)
    }
}
