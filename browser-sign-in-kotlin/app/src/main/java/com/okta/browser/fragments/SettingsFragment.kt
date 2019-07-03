package com.okta.browser.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.okta.browser.R

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        fun newInstance() = SettingsFragment()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)
    }
}