package com.steam.if97

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.steam.if97.core.If97
import com.steam.if97.core.Phase
import com.steam.if97.core.SteamState
import com.steam.if97.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var updating = false

    /** One row of the property list. */
    private class Spec(
        val section: Int?,
        val label: String,
        val unit: String,
        val format: (Double) -> String,
        val value: (SteamState) -> Double
    )

    /** One row of the saturation table. */
    private class SatSpec(
        val label: String,
        val format: (Double) -> String,
        val value: (SteamState) -> Double
    )

    private val rows = mutableListOf<Pair<Spec, TextView>>()
    private val satRows = mutableListOf<Pair<SatSpec, Pair<TextView, TextView>>>()
    private lateinit var specs: List<Spec>
    private lateinit var satSpecs: List<SatSpec>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        specs = buildSpecs()
        satSpecs = buildSatSpecs()

        buildPropertyRows()
        buildSaturationRows()
        wireInputs()

        binding.pressureEdit.setText(String.format(Locale.US, "%.0f", DEFAULT_PRESSURE_BAR))
        binding.tempEdit.setText(String.format(Locale.US, "%.0f", DEFAULT_TEMPERATURE_C))
        binding.pressureSeek.progress = (DEFAULT_PRESSURE_BAR - If97.MIN_PRESSURE_BAR).toInt()
        binding.tempSeek.progress = (DEFAULT_TEMPERATURE_C - If97.MIN_TEMPERATURE_C).toInt()
        recalc()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_about) {
            startActivity(Intent(this, AboutActivity::class.java))
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    // ---------------------------------------------------------------- inputs

    private fun wireInputs() {
        binding.pressureEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (updating) return
                syncSliderFromText()
                recalc()
            }
        })
        binding.tempEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (updating) return
                syncSliderFromText()
                recalc()
            }
        })

        binding.pressureSeek.setOnSeekBarChangeListener(seekListener { progress ->
            setText(binding.pressureEdit, (If97.MIN_PRESSURE_BAR + progress).toInt())
        })
        binding.tempSeek.setOnSeekBarChangeListener(seekListener { progress ->
            setText(binding.tempEdit, (If97.MIN_TEMPERATURE_C + progress).toInt())
        })
    }

    private fun seekListener(onChange: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
            if (!fromUser) return
            onChange(progress)
            recalc()
        }

        override fun onStartTrackingTouch(seek: SeekBar) = Unit
        override fun onStopTrackingTouch(seek: SeekBar) = Unit
    }

    private fun setText(view: TextView, value: Int) {
        updating = true
        view.setText(value.toString())
        updating = false
    }

    private fun syncSliderFromText() {
        val p = binding.pressureEdit.text?.toString()?.toDoubleOrNull()
        if (p != null && p >= If97.MIN_PRESSURE_BAR && p <= If97.MAX_PRESSURE_BAR) {
            updating = true
            binding.pressureSeek.progress = (p - If97.MIN_PRESSURE_BAR).toInt()
            updating = false
        }
        val t = binding.tempEdit.text?.toString()?.toDoubleOrNull()
        if (t != null && t >= If97.MIN_TEMPERATURE_C && t <= If97.MAX_TEMPERATURE_C) {
            updating = true
            binding.tempSeek.progress = (t - If97.MIN_TEMPERATURE_C).toInt()
            updating = false
        }
    }

    // ------------------------------------------------------------ calculation

    private fun recalc() {
        val pText = binding.pressureEdit.text?.toString().orEmpty().trim()
        val tText = binding.tempEdit.text?.toString().orEmpty().trim()
        val pBar = pText.toDoubleOrNull()
        val tC = tText.toDoubleOrNull()

        val pOk = pBar != null && pBar >= If97.MIN_PRESSURE_BAR && pBar <= If97.MAX_PRESSURE_BAR
        val tOk = tC != null && tC >= If97.MIN_TEMPERATURE_C && tC <= If97.MAX_TEMPERATURE_C

        binding.pressureInput.error =
            if (pText.isNotEmpty() && !pOk) getString(R.string.error_pressure) else null
        binding.tempInput.error =
            if (tText.isNotEmpty() && !tOk) getString(R.string.error_temperature) else null

        if (!pOk || !tOk || pBar == null || tC == null) {
            clearResults()
            return
        }

        val state = try {
            If97.state(pBar, tC)
        } catch (e: RuntimeException) {
            null
        }
        if (state == null) {
            clearResults()
            return
        }

        showSummary(state, pBar, tC)
        for ((spec, view) in rows) view.text = spec.format(spec.value(state))
        showSaturation(pBar)
    }

    private fun showSummary(state: SteamState, pBar: Double, tC: Double) {
        binding.phaseText.text = state.phase.label
        binding.phaseText.setTextColor(
            ContextCompat.getColor(
                this,
                when (state.phase) {
                    Phase.LIQUID -> R.color.phase_liquid
                    Phase.VAPOUR -> R.color.phase_vapour
                    Phase.SATURATED -> R.color.brand_accent_dark
                    Phase.SUPERCRITICAL -> R.color.phase_critical
                }
            )
        )

        binding.regionText.text = getString(
            R.string.region_line,
            state.region,
            num(pBar, 4),
            num(tC, 2)
        )

        if (pBar > CRITICAL_PRESSURE_BAR) {
            binding.tsatText.setText(R.string.no_saturation)
        } else {
            val tsat = If97.saturationTemperatureC(pBar)
            val delta = state.temperatureC - tsat
            binding.tsatText.text = when (state.phase) {
                Phase.LIQUID -> getString(
                    R.string.tsat_subcooled, num(pBar, 2), num(tsat, 2), num(-delta, 2)
                )
                Phase.VAPOUR -> getString(
                    R.string.tsat_superheated, num(pBar, 2), num(tsat, 2), num(delta, 2)
                )
                else -> getString(R.string.tsat_saturated, num(pBar, 2), num(tsat, 2))
            }
        }

        binding.keyHValue.text = fixed(1)(state.h)
        binding.keySValue.text = fixed(3)(state.s)
        binding.keyVValue.text = sig(4)(state.v)
    }

    private fun showSaturation(pBar: Double) {
        if (pBar > CRITICAL_PRESSURE_BAR) {
            binding.saturationCaption.setText(R.string.no_saturation)
            for ((_, views) in satRows) {
                views.first.text = EM_DASH
                views.second.text = EM_DASH
            }
            return
        }
        val sat = try {
            If97.saturatedAt(pBar)
        } catch (e: RuntimeException) {
            return
        }
        binding.saturationCaption.text = getString(
            R.string.sat_caption_format,
            num(sat.pressureBar, 2),
            num(sat.tsatC, 2),
            num(sat.latentHeat, 1)
        )
        for ((spec, views) in satRows) {
            views.first.text = spec.format(spec.value(sat.liquid))
            views.second.text = spec.format(spec.value(sat.vapour))
        }
    }

    private fun clearResults() {
        binding.phaseText.text = getString(R.string.no_state)
        binding.regionText.text = ""
        binding.tsatText.text = ""
        binding.keyHValue.text = EM_DASH
        binding.keySValue.text = EM_DASH
        binding.keyVValue.text = EM_DASH
        for ((_, view) in rows) view.text = EM_DASH
        for ((_, views) in satRows) {
            views.first.text = EM_DASH
            views.second.text = EM_DASH
        }
        binding.saturationCaption.setText(R.string.sat_caption)
    }

    // ------------------------------------------------------------- row building

    private fun buildPropertyRows() {
        val inflater = LayoutInflater.from(this)
        var section = -1
        for (spec in specs) {
            if (spec.section != null && spec.section != section) {
                section = spec.section
                val header = inflater.inflate(R.layout.item_property, binding.propertiesContainer, false)
                val label = header.findViewById<TextView>(R.id.rowLabel)
                label.setText(spec.section)
                label.setTextColor(ContextCompat.getColor(this, R.color.brand_primary))
                label.setTypeface(null, Typeface.BOLD)
                label.setPadding(0, resources.getDimensionPixelSize(R.dimen.section_top_padding), 0, 0)
                header.findViewById<TextView>(R.id.rowValue).visibility = View.GONE
                header.findViewById<TextView>(R.id.rowUnit).visibility = View.GONE
                binding.propertiesContainer.addView(header)
            }
            val row = inflater.inflate(R.layout.item_property, binding.propertiesContainer, false)
            row.findViewById<TextView>(R.id.rowLabel).text = spec.label
            row.findViewById<TextView>(R.id.rowUnit).text = spec.unit
            val value = row.findViewById<TextView>(R.id.rowValue)
            binding.propertiesContainer.addView(row)
            rows += spec to value
        }
    }

    private fun buildSaturationRows() {
        val inflater = LayoutInflater.from(this)
        val header = inflater.inflate(R.layout.item_sat_row, binding.saturationContainer, false)
        header.findViewById<TextView>(R.id.satLabel).setText(R.string.sat_header_property)
        header.findViewById<TextView>(R.id.satLiquid).setText(R.string.sat_header_liquid)
        header.findViewById<TextView>(R.id.satVapour).setText(R.string.sat_header_vapour)
        for (id in listOf(R.id.satLabel, R.id.satLiquid, R.id.satVapour)) {
            header.findViewById<TextView>(id).apply {
                setTypeface(null, Typeface.BOLD)
                gravity = if (id == R.id.satLabel) Gravity.START else Gravity.END
            }
        }
        binding.saturationContainer.addView(header)

        for (spec in satSpecs) {
            val row = inflater.inflate(R.layout.item_sat_row, binding.saturationContainer, false)
            row.findViewById<TextView>(R.id.satLabel).text = spec.label
            val liquid = row.findViewById<TextView>(R.id.satLiquid)
            val vapour = row.findViewById<TextView>(R.id.satVapour)
            binding.saturationContainer.addView(row)
            satRows += spec to (liquid to vapour)
        }
    }

    private fun buildSpecs(): List<Spec> = listOf(
        Spec(R.string.section_volumetric, "Specific volume v", "m³/kg", sig(6)) { it.v },
        Spec(R.string.section_volumetric, "Density ρ = 1/v", "kg/m³", sig(6)) { it.rho },
        Spec(R.string.section_volumetric, "Compressibility factor Z", "–", sig(4)) { it.z },

        Spec(R.string.section_caloric, "Enthalpy h", "kJ/kg", fixed(2)) { it.h },
        Spec(R.string.section_caloric, "Internal energy u = h − pv", "kJ/kg", fixed(2)) { it.u },
        Spec(R.string.section_caloric, "Entropy s", "kJ/(kg·K)", fixed(5)) { it.s },
        Spec(R.string.section_caloric, "Heat capacity cp", "kJ/(kg·K)", fixed(5)) { it.cp },
        Spec(R.string.section_caloric, "Heat capacity cv", "kJ/(kg·K)", fixed(5)) { it.cv },
        Spec(R.string.section_caloric, "Isentropic exponent k = cp/cv", "–", fixed(4)) { it.kappa },
        Spec(R.string.section_caloric, "Speed of sound w", "m/s", fixed(1)) { it.w },

        Spec(R.string.section_derived, "Gibbs energy g = h − Ts", "kJ/kg", fixed(2)) { it.g },
        Spec(R.string.section_derived, "Helmholtz energy f = u − Ts", "kJ/kg", fixed(2)) { it.f },
        Spec(R.string.section_derived, "Cubic expansion α", "1/K", sig(4)) { it.alphaV },
        Spec(R.string.section_derived, "Isothermal compressibility κ", "1/bar", sig(4)) { it.kappaT / 10.0 },
        Spec(R.string.section_derived, "Joule–Thomson coefficient μ", "°C/bar", sig(4)) { it.muJT / 10.0 }
    )

    private fun buildSatSpecs(): List<SatSpec> = listOf(
        SatSpec("v (m³/kg)", sig(6)) { it.v },
        SatSpec("h (kJ/kg)", fixed(2)) { it.h },
        SatSpec("u (kJ/kg)", fixed(2)) { it.u },
        SatSpec("s (kJ/(kg·K))", fixed(5)) { it.s },
        SatSpec("cp (kJ/(kg·K))", fixed(5)) { it.cp },
        SatSpec("w (m/s)", fixed(1)) { it.w }
    )

    // -------------------------------------------------------------- formatting

    private fun num(value: Double, decimals: Int): String =
        String.format(Locale.US, "%.${decimals}f", value)

    private fun fixed(decimals: Int): (Double) -> String = { value ->
        if (value.isNaN()) EM_DASH else String.format(Locale.US, "%.${decimals}f", clean(value))
    }

    private fun sig(digits: Int): (Double) -> String = { value ->
        if (value.isNaN()) EM_DASH else String.format(Locale.US, "%.${digits}g", clean(value))
    }

    private fun clean(value: Double): Double = if (Math.abs(value) < 1e-13) 0.0 else value

    private companion object {
        const val DEFAULT_PRESSURE_BAR = 10.0
        const val DEFAULT_TEMPERATURE_C = 200.0
        const val EM_DASH = "—"
        const val CRITICAL_PRESSURE_BAR = 220.64
    }
}
