/*
 * Copyright (C) 2023 Yet Another AOSP Project
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
package com.yaap.device.DeviceSettings;

import static com.android.internal.util.yaap.AutoSettingConsts.MODE_DISABLED;
import static com.android.internal.util.yaap.AutoSettingConsts.MODE_NIGHT;
import static com.android.internal.util.yaap.AutoSettingConsts.MODE_TIME;
import static com.android.internal.util.yaap.AutoSettingConsts.MODE_MIXED_SUNSET;
import static com.android.internal.util.yaap.AutoSettingConsts.MODE_MIXED_SUNRISE;

import android.app.Fragment;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.MenuItem;
import android.widget.TimePicker;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

import com.yaap.device.DeviceSettings.R;

import java.time.format.DateTimeFormatter;
import java.time.LocalTime;

public class DCDimScheduleSettings extends CollapsingToolbarBaseActivity
        implements PreferenceFragment.OnPreferenceStartFragmentCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new MainSettingsFragment())
                    .commit();
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment preferenceFragment,
            Preference preference) {
        Fragment instantiate = Fragment.instantiate(this, preference.getFragment(),
            preference.getExtras());
        getFragmentManager().beginTransaction().replace(
                android.R.id.content, instantiate).addToBackStack(preference.getKey()).commit();

        return true;
    }

    public static class MainSettingsFragment extends PreferenceFragment implements
            Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

        private static final String TAG = "DCDimScheduleSettings";
        private static final String MODE_KEY = "dc_dim_auto_mode";
        private static final String SINCE_PREF_KEY = "dc_dim_auto_since";
        private static final String TILL_PREF_KEY = "dc_dim_auto_till";

        private ListPreference mModePref;
        private Preference mSincePref;
        private Preference mTillPref;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.dc_schedule_settings, rootKey);

            PreferenceScreen screen = getPreferenceScreen();
            ContentResolver resolver = getActivity().getContentResolver();

            mSincePref = findPreference(SINCE_PREF_KEY);
            mSincePref.setOnPreferenceClickListener(this);
            mTillPref = findPreference(TILL_PREF_KEY);
            mTillPref.setOnPreferenceClickListener(this);

            int mode = Settings.Secure.getIntForUser(resolver,
                    MODE_KEY, MODE_DISABLED, UserHandle.USER_CURRENT);
            mModePref = (ListPreference) findPreference(MODE_KEY);
            mModePref.setValue(String.valueOf(mode));
            mModePref.setSummary(mModePref.getEntry());
            mModePref.setOnPreferenceChangeListener(this);

            updateTimeEnablement(mode);
            updateTimeSummary(mode);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object objValue) {
            int value = Integer.parseInt((String) objValue);
            int index = mModePref.findIndexOfValue((String) objValue);
            mModePref.setSummary(mModePref.getEntries()[index]);
            Settings.Secure.putIntForUser(getActivity().getContentResolver(),
                    MODE_KEY, value, UserHandle.USER_CURRENT);
            updateTimeEnablement(value);
            updateTimeSummary(value);
            return true;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            String[] times = getCustomTimeSetting();
            boolean isSince = preference == mSincePref;
            int hour, minute;
            TimePickerDialog.OnTimeSetListener listener = (view, hourOfDay, minute1) -> {
                updateTimeSetting(isSince, hourOfDay, minute1);
            };
            if (isSince) {
                String[] sinceValues = times[0].split(":", 0);
                hour = Integer.parseInt(sinceValues[0]);
                minute = Integer.parseInt(sinceValues[1]);
            } else {
                String[] tillValues = times[1].split(":", 0);
                hour = Integer.parseInt(tillValues[0]);
                minute = Integer.parseInt(tillValues[1]);
            }
            TimePickerDialog dialog = new TimePickerDialog(getContext(), listener,
                    hour, minute, DateFormat.is24HourFormat(getContext()));
            dialog.show();
            return true;
        }

        private String[] getCustomTimeSetting() {
            String value = Settings.Secure.getStringForUser(getActivity().getContentResolver(),
                    Settings.Secure.DC_DIM_AUTO_TIME, UserHandle.USER_CURRENT);
            if (value == null || value.equals("")) value = "20:00,07:00";
            return value.split(",", 0);
        }

        private void updateTimeEnablement(int mode) {
            mSincePref.setEnabled(mode == MODE_TIME || mode == MODE_MIXED_SUNRISE);
            mTillPref.setEnabled(mode == MODE_TIME || mode == MODE_MIXED_SUNSET);
        }

        private void updateTimeSummary(int mode) {
            updateTimeSummary(getCustomTimeSetting(), mode);
        }

        private void updateTimeSummary(String[] times, int mode) {
            if (mode == MODE_DISABLED) {
                mSincePref.setSummary("-");
                mTillPref.setSummary("-");
                return;
            }

            if (mode == MODE_NIGHT) {
                mSincePref.setSummary(R.string.dc_dim_schedule_sunset);
                mTillPref.setSummary(R.string.dc_dim_schedule_sunrise);
                return;
            }

            String outputFormat = DateFormat.is24HourFormat(getContext()) ? "HH:mm" : "hh:mm a";
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(outputFormat);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime sinceDT = LocalTime.parse(times[0], formatter);
            LocalTime tillDT = LocalTime.parse(times[1], formatter);

            if (mode == MODE_MIXED_SUNSET) {
                mSincePref.setSummary(R.string.dc_dim_schedule_sunset);
                mTillPref.setSummary(tillDT.format(outputFormatter));
            } else if (mode == MODE_MIXED_SUNRISE) {
                mTillPref.setSummary(R.string.dc_dim_schedule_sunrise);
                mSincePref.setSummary(sinceDT.format(outputFormatter));
            } else {
                mSincePref.setSummary(sinceDT.format(outputFormatter));
                mTillPref.setSummary(tillDT.format(outputFormatter));
            }
        }

        private void updateTimeSetting(boolean since, int hour, int minute) {
            String[] times = getCustomTimeSetting();
            String nHour = "";
            String nMinute = "";
            if (hour < 10) nHour += "0";
            if (minute < 10) nMinute += "0";
            nHour += String.valueOf(hour);
            nMinute += String.valueOf(minute);
            times[since ? 0 : 1] = nHour + ":" + nMinute;
            Settings.Secure.putStringForUser(getActivity().getContentResolver(),
                    Settings.Secure.DC_DIM_AUTO_TIME,
                    times[0] + "," + times[1], UserHandle.USER_CURRENT);
            updateTimeSummary(times, Integer.parseInt(mModePref.getValue()));
        }


        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
        }

        @Override
        public void onResume() {
            super.onResume();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
