package com.steam.if97.core

import kotlin.math.abs

/// Phase of a single-phase IF97 state.
enum class Phase(val label: String) {
    LIQUID("Compressed liquid"),
    SATURATED("On the saturation line"),
    VAPOUR("Superheated steam"),
    SUPERCRITICAL("Supercritical fluid")
}

/// All thermodynamic properties of one single-phase state.
///
/// Units: v in m3/kg, rho in kg/m3, h/u/g/f in kJ/kg, s/cp/cv in kJ/(kg K),
/// w in m/s, alphaV in 1/K, kappaT in 1/MPa, muJT in K/MPa.
data class SteamState(
    val pressureBar: Double,
    val temperatureC: Double,
    val region: Int,
    val phase: Phase,
    val v: Double,
    val rho: Double,
    val z: Double,
    val h: Double,
    val u: Double,
    val s: Double,
    val cp: Double,
    val cv: Double,
    val kappa: Double,
    val w: Double,
    val g: Double,
    val f: Double,
    val alphaV: Double,
    val kappaT: Double,
    val muJT: Double
) {
    val pressureMPa: Double get() = pressureBar / 10.0
    val temperatureK: Double get() = temperatureC + 273.15
}

/// Saturated liquid and saturated vapour at one pressure.
data class SaturatedPoint(
    val pressureBar: Double,
    val tsatC: Double,
    val liquid: SteamState,
    val vapour: SteamState
) {
    val latentHeat: Double get() = vapour.h - liquid.h
    val latentEntropy: Double get() = vapour.s - liquid.s
    val volumeChange: Double get() = vapour.v - liquid.v
}

/// IAPWS-IF97 property interface (regions 1-4).
object If97 {

    const val MIN_PRESSURE_BAR = 10.0
    const val MAX_PRESSURE_BAR = 180.0
    const val MIN_TEMPERATURE_C = 100.0
    const val MAX_TEMPERATURE_C = 600.0

    /// IF97 region for a pressure in MPa and a temperature in K.
    fun regionAt(pMPa: Double, tK: Double): Int {
        require(pMPa > 0.0) { "Pressure must be positive" }
        require(tK >= If97Constants.T_MIN) { "Temperature below the IF97 range" }
        require(tK <= If97Constants.T_MAX) { "Temperature above the IF97 range" }
        require(pMPa <= If97Constants.P_MAX) { "Pressure above the IF97 range" }
        return when {
            tK <= If97Constants.T_B23 ->
                if (pMPa >= Region4.psatMPa(tK)) 1 else 2
            tK <= If97Constants.T_B23_MAX ->
                if (pMPa >= Region4.pB23MPa(tK)) 3 else 2
            else -> 2
        }
    }

    /// Saturation temperature in degC for a pressure in bar.
    fun saturationTemperatureC(pressureBar: Double): Double {
        require(pressureBar in (0.0)..(If97Constants.PC * 10.0)) {
            "Saturation line is defined below the critical pressure"
        }
        return Region4.tsatK(pressureBar / 10.0) - 273.15
    }

    /// Saturation pressure in bar for a temperature in degC.
    fun saturationPressureBar(temperatureC: Double): Double =
        Region4.psatMPa(temperatureC + 273.15) * 10.0

    /// Single-phase state from pressure in bar and temperature in degC.
    fun state(pressureBar: Double, temperatureC: Double): SteamState {
        val pMPa = pressureBar / 10.0
        val tK = temperatureC + 273.15
        val region = regionAt(pMPa, tK)
        val tsat = if (pMPa <= If97Constants.PC) Region4.tsatK(pMPa) else Double.NaN

        return when (region) {
            1 -> build(pressureBar, temperatureC, 1, phaseOf(tK, tsat), Region1.properties(pMPa, tK))
            2 -> build(pressureBar, temperatureC, 2, phaseOf(tK, tsat), Region2.properties(pMPa, tK))
            else -> {
                // Above the critical pressure the Helmholtz equation has a single
                // monotonic branch; below it the stable branch follows Tsat.
                val liquid = if (pMPa > If97Constants.PC || tsat.isNaN()) true else tK < tsat
                val phase = when {
                    pMPa > If97Constants.PC -> Phase.SUPERCRITICAL
                    abs(tK - tsat) <= SAT_TOLERANCE -> Phase.SATURATED
                    liquid -> Phase.LIQUID
                    else -> Phase.VAPOUR
                }
                build(pressureBar, temperatureC, 3, phase, Region3.properties(pMPa, tK, liquid))
            }
        }
    }

    /// Saturated liquid/vapour pair at a pressure in bar.
    fun saturatedAt(pressureBar: Double): SaturatedPoint {
        val pMPa = pressureBar / 10.0
        require(pMPa > 0.0 && pMPa <= If97Constants.PC) {
            "Saturation line is defined below the critical pressure"
        }
        val tsat = Region4.tsatK(pMPa)
        val tsatC = tsat - 273.15
        val liquid: SteamState
        val vapour: SteamState
        if (pMPa <= If97Constants.P_B23) {
            liquid = build(pressureBar, tsatC, 1, Phase.LIQUID, Region1.properties(pMPa, tsat))
            vapour = build(pressureBar, tsatC, 2, Phase.VAPOUR, Region2.properties(pMPa, tsat))
        } else {
            liquid = build(pressureBar, tsatC, 3, Phase.LIQUID, Region3.properties(pMPa, tsat, true))
            vapour = build(pressureBar, tsatC, 3, Phase.VAPOUR, Region3.properties(pMPa, tsat, false))
        }
        return SaturatedPoint(pressureBar, tsatC, liquid, vapour)
    }

    private const val SAT_TOLERANCE = 0.02 // K

    private fun phaseOf(tK: Double, tsat: Double): Phase = when {
        tsat.isNaN() -> Phase.SUPERCRITICAL
        abs(tK - tsat) <= SAT_TOLERANCE -> Phase.SATURATED
        tK < tsat -> Phase.LIQUID
        else -> Phase.VAPOUR
    }

    private fun build(
        pressureBar: Double,
        temperatureC: Double,
        region: Int,
        phase: Phase,
        p: Props
    ): SteamState {
        val tK = temperatureC + 273.15
        val pMPa = pressureBar / 10.0
        val r = If97Constants.R
        val z = 1000.0 * pMPa * p.v / (r * tK)
        val kappa = p.cp / p.cv
        val muJT = 1000.0 * p.v * (tK * p.alphaV - 1.0) / p.cp
        return SteamState(
            pressureBar = pressureBar,
            temperatureC = temperatureC,
            region = region,
            phase = phase,
            v = p.v,
            rho = 1.0 / p.v,
            z = z,
            h = p.h,
            u = p.u,
            s = p.s,
            cp = p.cp,
            cv = p.cv,
            kappa = kappa,
            w = p.w,
            g = p.h - tK * p.s,
            f = p.u - tK * p.s,
            alphaV = p.alphaV,
            kappaT = p.kappaT,
            muJT = muJT
        )
    }
}
