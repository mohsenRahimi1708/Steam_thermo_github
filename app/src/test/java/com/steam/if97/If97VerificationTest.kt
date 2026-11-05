package com.steam.if97

import com.steam.if97.core.If97
import com.steam.if97.core.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Verification against the reference values published in IAPWS R7-97
 * (tables 5, 15, 33, 35 and 36 of the release).
 */
class If97VerificationTest {

    private fun assertProp(label: String, expected: Double, actual: Double, tol: Double = 1e-7) {
        val scale = if (Math.abs(expected) < 1e-12) 1.0 else Math.abs(expected)
        val rel = Math.abs(actual - expected) / scale
        assertTrue(
            String.format(
                Locale.US, "%s: expected %.12g but was %.12g (rel %.2e)", label, expected, actual, rel
            ),
            rel < tol
        )
    }

    private fun check(
        pBar: Double,
        tC: Double,
        v: Double,
        h: Double,
        u: Double,
        s: Double,
        cp: Double,
        w: Double,
        region: Int
    ) {
        val st = If97.state(pBar, tC)
        val tag = String.format(Locale.US, "p=%.6f bar T=%.4f C", pBar, tC)
        assertEquals("region of $tag", region, st.region)
        assertProp("v $tag", v, st.v)
        assertProp("h $tag", h, st.h)
        assertProp("u $tag", u, st.u)
        assertProp("s $tag", s, st.s)
        assertProp("cp $tag", cp, st.cp)
        assertProp("w $tag", w, st.w, 1e-6)
    }

    @Test
    fun region1MatchesReferenceTable() {
        // T = 300 K, 300 K, 500 K at p = 3, 80, 3 MPa
        check(30.0, 26.85, 1.00215168e-3, 115.331273, 112.324818, 0.392294792, 4.17301218, 1507.73921, 1)
        check(800.0, 26.85, 0.971180894e-3, 184.142828, 106.448356, 0.368563852, 4.01008987, 1634.69054, 1)
        check(30.0, 226.85, 1.20241800e-3, 975.542239, 971.934985, 2.58041912, 4.65580682, 1240.71337, 1)
    }

    @Test
    fun region2MatchesReferenceTable() {
        check(0.035, 26.85, 39.4913866, 2549.91145, 2411.69160, 8.52238967, 1.91300162, 427.920172, 2)
        check(0.035, 426.85, 92.3015898, 3335.68375, 3012.62819, 10.1749996, 2.08141274, 644.289068, 2)
        check(300.0, 426.85, 5.42946619e-3, 2631.49474, 2468.61076, 5.17540298, 10.3505092, 480.386523, 2)
    }

    @Test
    fun region3MatchesReferenceTable() {
        check(255.837018, 376.85, 2.0e-3, 1863.43019, 1812.26279, 4.05427273, 13.8935717, 502.005554, 3)
        check(222.930643, 376.85, 5.0e-3, 2375.12401, 2263.65869, 4.85438792, 44.6579342, 383.444594, 3)
        check(783.095639, 476.85, 2.0e-3, 2258.68845, 2102.06931, 4.46971906, 6.34165359, 760.696041, 3)
    }

    @Test
    fun region3ThermoElasticProperties() {
        // alpha and kappa_T are not published in R7-97; these references come from
        // central differences of v at constant p (independent iapws implementation).
        val dense = If97.state(255.837018, 376.85)   // 650 K, rho = 500 kg/m3
        assertProp("alpha_v (650 K, 500 kg/m3)", 0.016865311, dense.alphaV, 1e-6)
        assertProp("kappa_T (650 K, 500 kg/m3)", 0.034550696, dense.kappaT, 1e-6)

        val light = If97.state(170.0, 353.0)         // region 3, vapour side
        assertProp("alpha_v (626.15 K, vapour)", 0.023039126, light.alphaV, 1e-6)
        assertProp("kappa_T (626.15 K, vapour)", 0.214920588, light.kappaT, 1e-6)
    }

    @Test
    fun saturationLineMatchesReferenceTable() {
        // reference values are published to nine significant digits
        assertProp("psat(300 K)", 0.353658941e-2 * 10.0, If97.saturationPressureBar(26.85), 1e-8)
        assertProp("psat(500 K)", 0.263889776e1 * 10.0, If97.saturationPressureBar(226.85), 1e-8)
        assertProp("psat(600 K)", 0.123443146e2 * 10.0, If97.saturationPressureBar(326.85), 1e-8)
        assertProp("Tsat(1 bar)", 372.755919 - 273.15, If97.saturationTemperatureC(1.0), 1e-8)
        assertProp("Tsat(10 bar)", 453.035632 - 273.15, If97.saturationTemperatureC(10.0), 1e-8)
        assertProp("Tsat(100 bar)", 584.149488 - 273.15, If97.saturationTemperatureC(100.0), 1e-8)
    }

    @Test
    fun phaseDetectionInTheAppRange() {
        // 10 bar, 200 degC is superheated steam (Tsat = 179.9 degC)
        assertEquals(Phase.VAPOUR, If97.state(10.0, 200.0).phase)
        // 180 bar, 100 degC is strongly subcooled liquid
        assertEquals(Phase.LIQUID, If97.state(180.0, 100.0).phase)
        // exactly on the saturation line
        val tsat = If97.saturationTemperatureC(10.0)
        assertEquals(Phase.SATURATED, If97.state(10.0, tsat).phase)
        // narrow high-pressure strip: region 3 compressed liquid at 180 bar / 355 degC
        val strip = If97.state(180.0, 355.0)
        assertEquals(3, strip.region)
        assertEquals(Phase.LIQUID, strip.phase)
    }

    @Test
    fun thermodynamicsAreSelfConsistent() {
        val st = If97.state(50.0, 400.0)
        // u = h - p v, g = h - T s, f = u - T s
        assertProp("u from h-pv", st.u, st.h - 1000.0 * st.pressureMPa * st.v, 1e-10)
        assertProp("g from h-Ts", st.g, st.h - st.temperatureK * st.s, 1e-12)
        assertProp("f from u-Ts", st.f, st.u - st.temperatureK * st.s, 1e-12)
        // Z = p v / (R T)
        assertProp("Z", st.z, 1000.0 * st.pressureMPa * st.v / (0.461526 * st.temperatureK), 1e-10)
        // kappa = cp / cv
        assertProp("kappa", st.kappa, st.cp / st.cv, 1e-12)
        // cp > cv > 0 and positive speed of sound for a stable single phase
        assertTrue("cp > cv", st.cp > st.cv)
        assertTrue("w > 0", st.w > 0.0)
    }

    @Test
    fun saturationTableCoversHighPressureStrip() {
        // region 3 saturation branch (p > 16.529 MPa)
        val sat = If97.saturatedAt(180.0)
        assertTrue("Tsat(180 bar) above 350 C", sat.tsatC > 350.0 && sat.tsatC < 360.0)
        assertTrue("latent heat positive", sat.latentHeat > 0.0)
        assertTrue("v_g > v_f", sat.vapour.v > sat.liquid.v)
        assertTrue("h_g > h_f", sat.vapour.h > sat.liquid.h)
    }
}
