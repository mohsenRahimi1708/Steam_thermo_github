package com.steam.if97

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.steam.if97.databinding.ActivityAboutBinding

/**
 * Bilingual explanation of the app. Both languages are always visible so the
 * Persian block is laid out right-to-left even when the device runs in English;
 * the two buttons switch the whole app locale (persisted by AppCompat).
 */
class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.versionText.text = getString(R.string.about_version, versionName())

        binding.langEn.setOnClickListener { applyLocale("en") }
        binding.langFa.setOnClickListener { applyLocale("fa") }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun applyLocale(languageTag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }

    private fun versionName(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: DEFAULT_VERSION
    } catch (e: Exception) {
        DEFAULT_VERSION
    }

    private companion object {
        const val DEFAULT_VERSION = "1.0"
    }
}
