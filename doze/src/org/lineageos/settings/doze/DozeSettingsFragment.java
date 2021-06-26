/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2019 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.doze;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragment;

public class DozeSettingsFragment extends PreferenceFragment
        implements OnPreferenceChangeListener {

    private ListPreference mPickUpPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.doze_settings);

        SharedPreferences prefs = getActivity().getSharedPreferences("doze_settings",
                Activity.MODE_PRIVATE);
        if (savedInstanceState == null && !prefs.getBoolean("first_help_shown", false)) {
            showHelp();
        }

        mPickUpPreference = (ListPreference) findPreference(Utils.GESTURE_PICK_UP_KEY);
        mPickUpPreference.setOnPreferenceChangeListener(this);
        updateEnablement();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateEnablement();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mPickUpPreference) {
            int index = Integer.parseInt((String) newValue);
            mPickUpPreference.setValueIndex(index);
            updatePickUpSummary();
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getActivity().onBackPressed();
            return true;
        }
        return false;
    }

    private static class HelpDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.doze_settings_help_title)
                    .setMessage(R.string.doze_settings_help_text)
                    .setNegativeButton(R.string.dialog_ok, (dialog, which) -> dialog.cancel())
                    .create();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            getActivity().getSharedPreferences("doze_settings", Activity.MODE_PRIVATE)
                    .edit()
                    .putBoolean("first_help_shown", true)
                    .commit();
        }
    }

    private void showHelp() {
        HelpDialogFragment fragment = new HelpDialogFragment();
        fragment.show(getFragmentManager(), "help_dialog");
    }

    private void updateEnablement() {
        boolean dozeEnabled = Utils.isDozeEnabled(getActivity());
        boolean aodEnabled = Utils.isAlwaysOnEnabled(getActivity());
        boolean enabled = dozeEnabled && !aodEnabled;
        mPickUpPreference.setEnabled(enabled);
        if (!dozeEnabled) mPickUpPreference.setSummary(R.string.disabled_for_doze);
        else if (aodEnabled) mPickUpPreference.setSummary(R.string.disabled_for_aod);
        else if (enabled) updatePickUpSummary();
    }

    private void updatePickUpSummary() {
        int index = Integer.parseInt(mPickUpPreference.getValue());
        mPickUpPreference.setSummary(mPickUpPreference.getEntries()[index]);
    }
}
