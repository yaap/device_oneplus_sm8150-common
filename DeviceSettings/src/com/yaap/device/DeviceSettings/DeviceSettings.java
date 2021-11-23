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

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.preference.TwoStatePreference;

import com.yaap.device.DeviceSettings.Constants;
import com.yaap.device.DeviceSettings.FPSInfoService;

public class DeviceSettings extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_CATEGORY_CAMERA = "camera";
    public static final String KEY_SRGB_SWITCH = "srgb";
    public static final String KEY_HBM_SWITCH = "hbm";
    public static final String KEY_DC_SWITCH = "dc";
    public static final String KEY_DCI_SWITCH = "dci";
    public static final String KEY_WIDECOLOR_SWITCH = "widecolor";
    public static final String KEY_NATURAL_SWITCH = "natural";
    public static final String KEY_VIVID_SWITCH = "vivid";

    private static final String KEY_CATEGORY_REFRESH = "refresh";
    private static final String KEY_REFRESH_RATE = "refresh_rate";
    private static final String KEY_ALWAYS_CAMERA_DIALOG = "always_on_camera_dialog";
    public static final String KEY_FPS_INFO = "fps_info";

    public static final String KEY_SETTINGS_PREFIX = "device_setting_";

    private static final boolean sHasPopupCamera =
            Build.DEVICE.equals("OnePlus7Pro") ||
            Build.DEVICE.equals("OnePlus7TPro") ||
            Build.DEVICE.equals("OnePlus7TProNR");

    private static TwoStatePreference mHBMModeSwitch;
    private static TwoStatePreference mRefreshRate;
    private static SwitchPreference mFpsInfo;
    private SwitchPreference mAlwaysCameraSwitch;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.main);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);

        ListPreference mTopKeyPref = (ListPreference) findPreference(Constants.NOTIF_SLIDER_TOP_KEY);
        mTopKeyPref.setValueIndex(Constants.getPreferenceInt(getContext(), Constants.NOTIF_SLIDER_TOP_KEY));
        mTopKeyPref.setOnPreferenceChangeListener(this);
        ListPreference mMiddleKeyPref = (ListPreference) findPreference(Constants.NOTIF_SLIDER_MIDDLE_KEY);
        mMiddleKeyPref.setValueIndex(Constants.getPreferenceInt(getContext(), Constants.NOTIF_SLIDER_MIDDLE_KEY));
        mMiddleKeyPref.setOnPreferenceChangeListener(this);
        ListPreference mBottomKeyPref = (ListPreference) findPreference(Constants.NOTIF_SLIDER_BOTTOM_KEY);
        mBottomKeyPref.setValueIndex(Constants.getPreferenceInt(getContext(), Constants.NOTIF_SLIDER_BOTTOM_KEY));
        mBottomKeyPref.setOnPreferenceChangeListener(this);

        TwoStatePreference mDCModeSwitch = (TwoStatePreference) findPreference(KEY_DC_SWITCH);
        mDCModeSwitch.setEnabled(DCModeSwitch.isSupported());
        mDCModeSwitch.setChecked(DCModeSwitch.isCurrentlyEnabled(getContext()));
        mDCModeSwitch.setOnPreferenceChangeListener(new DCModeSwitch());

        mHBMModeSwitch = (TwoStatePreference) findPreference(KEY_HBM_SWITCH);
        mHBMModeSwitch.setEnabled(HBMModeSwitch.isSupported());
        mHBMModeSwitch.setChecked(HBMModeSwitch.isCurrentlyEnabled(getContext()));
        mHBMModeSwitch.setOnPreferenceChangeListener(this);

        if (getResources().getBoolean(R.bool.config_deviceHasHighRefreshRate)) {
            mRefreshRate = (TwoStatePreference) findPreference(KEY_REFRESH_RATE);
            mRefreshRate.setOnPreferenceChangeListener(this);
        } else {
            getPreferenceScreen().removePreference((Preference) findPreference(KEY_CATEGORY_REFRESH));
        }

        mFpsInfo = (SwitchPreference) findPreference(KEY_FPS_INFO);
        mFpsInfo.setChecked(isFPSOverlayRunning());
        mFpsInfo.setOnPreferenceChangeListener(this);

        PreferenceCategory mCameraCategory = (PreferenceCategory) findPreference(KEY_CATEGORY_CAMERA);
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
        mHBMModeSwitch.setChecked(HBMModeSwitch.isCurrentlyEnabled(getContext()));
        mFpsInfo.setChecked(isFPSOverlayRunning());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mFpsInfo) {
            boolean enabled = (Boolean) newValue;
            Intent fpsinfo = new Intent(getContext(), FPSInfoService.class);
            if (enabled) {
                getContext().startService(fpsinfo);
            } else {
                getContext().stopService(fpsinfo);
            }
        } else if (preference == mAlwaysCameraSwitch) {
            boolean enabled = (Boolean) newValue;
            Settings.System.putInt(getContext().getContentResolver(),
                        KEY_SETTINGS_PREFIX + KEY_ALWAYS_CAMERA_DIALOG,
                        enabled ? 1 : 0);
        } else if (preference == mRefreshRate) {
            Boolean enabled = (Boolean) newValue;
            RefreshRateSwitch.setPeakRefresh(getContext(), enabled);
        } else if (preference == mHBMModeSwitch) {
            Boolean enabled = (Boolean) newValue;
            Utils.writeValue(HBMModeSwitch.getFile(), enabled ? "5" : "0");
            Intent hbmIntent = new Intent(getContext(),
                    com.yaap.device.DeviceSettings.HBMModeService.class);
            if (enabled) {
                getContext().startService(hbmIntent);
            } else {
                getContext().stopService(hbmIntent);
            }
        } else if (newValue instanceof String) {
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

    private boolean isFPSOverlayRunning() {
        ActivityManager am = (ActivityManager) getContext().getSystemService(
                Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service :
                am.getRunningServices(Integer.MAX_VALUE))
            if (FPSInfoService.class.getName().equals(service.service.getClassName()))
                return true;
        return false;
   }
}
