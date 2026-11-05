package com.steam.if97.core

/// Reference constants of the IAPWS Industrial Formulation 1997 (R7-97, rev. 2007).
internal object If97Constants {

    /// Specific gas constant of ordinary water, kJ/(kg K).
    const val R = 0.461526

    /// Critical temperature, K.
    const val TC = 647.096

    /// Critical pressure, MPa.
    const val PC = 22.064

    /// Critical density, kg/m3.
    const val RHOC = 322.0

    /// Triple point temperature, K.
    const val T_TRIPLE = 273.16

    /// Triple point pressure, MPa.
    const val P_TRIPLE = 611.657e-6

    /// Lower temperature limit of regions 1 and 2, K.
    const val T_MIN = 273.15

    /// Upper temperature limit of region 2, K (800 degC).
    const val T_MAX = 1073.15

    /// Upper pressure limit of IF97, MPa.
    const val P_MAX = 100.0

    /// Molar mass of ordinary water, kg/mol.
    const val MOLAR_MASS = 0.018015268

    /// Temperature of the B23 boundary at p = 16.529 MPa, K.
    const val T_B23 = 623.15

    /// Pressure of the B23 boundary at T = 623.15 K, MPa.
    const val P_B23 = 16.529

    /// Upper temperature of the B23 boundary, K.
    const val T_B23_MAX = 863.15
}
