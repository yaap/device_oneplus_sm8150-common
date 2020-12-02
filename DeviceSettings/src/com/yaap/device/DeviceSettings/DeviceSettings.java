/*
* Copyright (C) 2016 The OmniROM Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package com.yaap.device.DeviceSettings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.util.Log;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.internal.util.yaap.FileUtils;
import com.yaap.device.DeviceSettings.Constants;

public class DeviceSettings extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_CATEGORY_CAMERA = "camera";
    private static final String KEY_CATEGORY_GRAPHICS = "graphics";
    public static final String KEY_SRGB_SWITCH = "srgb";
    public static final String KEY_HBM_SWITCH = "hbm";
    public static final String KEY_DC_SWITCH = "dc";
    public static final String KEY_DCI_SWITCH = "dci";
    public static final String KEY_WIDECOLOR_SWITCH = "widecolor";

    private static final String KEY_CATEGORY_REFRESH = "refresh";
    public static final String KEY_REFRESH_RATE = "refresh_rate";
    public static final String KEY_AUTO_REFRESH_RATE = "auto_refresh_rate";
    public static final String KEY_FPS_INFO = "fps_info";
    public static final String KEY_ALWAYS_CAMERA_DIALOG = "always_on_camera_dialog";

    public static final String KEY_VIBSTRENGTH = "vib_strength";

    public static final String KEY_SETTINGS_PREFIX = "device_setting_";

    private static final boolean sHasPopupCamera =
            Build.DEVICE.equals("OnePlus7Pro") ||
            Build.DEVICE.equals("OnePlus7TPro") ||
            Build.DEVICE.equals("OnePlus7TProNR");

    private static TwoStatePreference mHBMModeSwitch;
    private static TwoStatePreference mDCModeSwitch;
    private static TwoStatePreference mRefreshRate;
    private static SwitchPreference mAutoRefreshRate;
    private static SwitchPreference mFpsInfo;
    private ListPreference mTopKeyPref;
    private ListPreference mMiddleKeyPref;
    private ListPreference mBottomKeyPref;
    private SwitchPreference mAlwaysCameraSwitch;
    private PreferenceCategory mCameraCategory;
    private VibratorStrengthPreference mVibratorStrength;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        addPreferencesFromResource(R.xml.main);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);

        mVibratorStrength = (VibratorStrengthPreference) findPreference(KEY_VIBSTRENGTH);
        if (mVibratorStrength == null || !VibratorStrengthPreference.isSupported()) {
            getPreferenceScreen().removePreference((Preference) findPreference("vibrator"));
        }

        mTopKeyPref = (ListPreference) findPreference(Constants.NOTIF_SLIDER_TOP_KEY);
        mTopKeyPref.setValueIndex(Constants.getPreferenceInt(getContext(), Constants.NOTIF_SLIDER_TOP_KEY));
        mTopKeyPref.setOnPreferenceChangeListener(this);
        mMiddleKeyPref = (ListPreference) findPreference(Constants.NOTIF_SLIDER_MIDDLE_KEY);
        mMiddleKeyPref.setValueIndex(Constants.getPreferenceInt(getContext(), Constants.NOTIF_SLIDER_MIDDLE_KEY));
        mMiddleKeyPref.setOnPreferenceChangeListener(this);
        mBottomKeyPref = (ListPreference) findPreference(Constants.NOTIF_SLIDER_BOTTOM_KEY);
        mBottomKeyPref.setValueIndex(Constants.getPreferenceInt(getContext(), Constants.NOTIF_SLIDER_BOTTOM_KEY));
        mBottomKeyPref.setOnPreferenceChangeListener(this);

        mDCModeSwitch = (TwoStatePreference) findPreference(KEY_DC_SWITCH);
        mDCModeSwitch.setEnabled(DCModeSwitch.isSupported());
        mDCModeSwitch.setChecked(DCModeSwitch.isCurrentlyEnabled(this.getContext()));
        mDCModeSwitch.setOnPreferenceChangeListener(new DCModeSwitch());

        mHBMModeSwitch = (TwoStatePreference) findPreference(KEY_HBM_SWITCH);
        mHBMModeSwitch.setEnabled(HBMModeSwitch.isSupported());
        mHBMModeSwitch.setChecked(HBMModeSwitch.isCurrentlyEnabled(this.getContext()));
        mHBMModeSwitch.setOnPreferenceChangeListener(this);

        if (getResources().getBoolean(R.bool.config_deviceHasHighRefreshRate)) {
            boolean autoRefresh = AutoRefreshRateSwitch.isCurrentlyEnabled(this.getContext());
            mAutoRefreshRate = (SwitchPreference) findPreference(KEY_AUTO_REFRESH_RATE);
            mAutoRefreshRate.setChecked(autoRefresh);
            mAutoRefreshRate.setOnPreferenceChangeListener(this);

            mRefreshRate = (TwoStatePreference) findPreference(KEY_REFRESH_RATE);
            mRefreshRate.setChecked(RefreshRateSwitch.isCurrentlyEnabled(this.getContext()));
            mRefreshRate.setOnPreferenceChangeListener(this);
            updateRefreshRateState(autoRefresh);

            mFpsInfo = (SwitchPreference) findPreference(KEY_FPS_INFO);
            mFpsInfo.setChecked(prefs.getBoolean(KEY_FPS_INFO, false));
            mFpsInfo.setOnPreferenceChangeListener(this);
        } else {
            getPreferenceScreen().removePreference((Preference) findPreference(KEY_CATEGORY_REFRESH));
        }

        mCameraCategory = (PreferenceCategory) findPreference(KEY_CATEGORY_CAMERA);
        if (sHasPopupCamera) {
            mAlwaysCameraSwitch = (SwitchPreference) findPreference(KEY_ALWAYS_CAMERA_DIALOG);
            boolean enabled = Settings.System.getInt(getContext().getContentResolver(),
                        KEY_SETTINGS_PREFIX + KEY_ALWAYS_CAMERA_DIALOG, 0) == 1;
            mAlwaysCameraSwitch.setChecked(enabled);
            mAlwaysCameraSwitch.setOnPreferenceChangeListener(this);
        } else {
            mCameraCategory.setVisible(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mHBMModeSwitch.setChecked(HBMModeSwitch.isCurrentlyEnabled(this.getContext()));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mFpsInfo) {
            boolean enabled = (Boolean) newValue;
            Intent fpsinfo = new Intent(this.getContext(),
                    com.yaap.device.DeviceSettings.FPSInfoService.class);
            if (enabled) {
                this.getContext().startService(fpsinfo);
            } else {
                this.getContext().stopService(fpsinfo);
            }
        } else if (preference == mAlwaysCameraSwitch) {
            boolean enabled = (Boolean) newValue;
            Settings.System.putInt(getContext().getContentResolver(),
                        KEY_SETTINGS_PREFIX + KEY_ALWAYS_CAMERA_DIALOG,
                        enabled ? 1 : 0);
        } else if (preference == mAutoRefreshRate) {
            Boolean enabled = (Boolean) newValue;
            Settings.System.putFloat(getContext().getContentResolver(),
                    Settings.System.PEAK_REFRESH_RATE, 90f);
            Settings.System.putFloat(getContext().getContentResolver(),
                    Settings.System.MIN_REFRESH_RATE, 60f);
            Settings.System.putInt(getContext().getContentResolver(),
                    AutoRefreshRateSwitch.SETTINGS_KEY, enabled ? 1 : 0);
            updateRefreshRateState(enabled);
        } else if (preference == mRefreshRate) {
            Boolean enabled = (Boolean) newValue;
            RefreshRateSwitch.setPeakRefresh(getContext(), enabled);
        } else if (preference == mHBMModeSwitch) {
            Boolean enabled = (Boolean) newValue;
            Utils.writeValue(HBMModeSwitch.getFile(), enabled ? "5" : "0");
            Intent hbmIntent = new Intent(this.getContext(),
                    com.yaap.device.DeviceSettings.HBMModeService.class);
            if (enabled) {
                this.getContext().startService(hbmIntent);
            } else {
                this.getContext().stopService(hbmIntent);
            }
        } else {
            Constants.setPreferenceInt(getContext(), preference.getKey(),
                    Integer.parseInt((String) newValue));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        // Respond to the action bar's Up/Home button
        case android.R.id.home:
            getActivity().finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateRefreshRateState(boolean auto) {
        mRefreshRate.setEnabled(!auto);
        if (auto) mRefreshRate.setChecked(false);
    }
}
