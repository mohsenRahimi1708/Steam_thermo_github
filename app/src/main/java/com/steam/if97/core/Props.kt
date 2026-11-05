package com.steam.if97.core

/// Dimensionless-result holder for a single IF97 region evaluation.
/// Volumes in m3/kg, energies in kJ/kg, entropies and heat capacities in
/// kJ/(kg K), speed of sound in m/s, alphaV in 1/K and kappaT in 1/MPa.
internal class Props(
    val v: Double,
    val h: Double,
    val u: Double,
    val s: Double,
    val cp: Double,
    val cv: Double,
    val w: Double,
    val alphaV: Double,
    val kappaT: Double
)
