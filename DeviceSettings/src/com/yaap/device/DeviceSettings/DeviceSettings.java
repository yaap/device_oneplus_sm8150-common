/*
* Copyright (C) 2021 Yet Another AOSP Project
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
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.MenuItem;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreference;
import androidx.preference.TwoStatePreference;

import com.yaap.device.DeviceSettings.ModeSwitch.DCModeSwitch;
import com.yaap.device.DeviceSettings.ModeSwitch.HBMModeSwitch;

public class DeviceSettings extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_CATEGORY_CAMERA = "camera";
    public static final String KEY_SRGB_SWITCH = "srgb";
    public static final String KEY_HBM_SWITCH = "hbm";
    public static final String KEY_DCI_SWITCH = "dci";
    public static final String KEY_WIDECOLOR_SWITCH = "widecolor";
    public static final String KEY_NATURAL_SWITCH = "natural";
    public static final String KEY_VIVID_SWITCH = "vivid";

    private static final String KEY_REFRESH_RATE = "refresh_rate";
    private static final String KEY_ALWAYS_CAMERA_DIALOG = "always_on_camera_dialog";
    public static final String KEY_FPS_INFO = "fps_info";

    public static final String KEY_SETTINGS_PREFIX = "device_setting_";

    private static final String POPUP_HELPER_PKG_NAME = "org.lineageos.camerahelper";

    private TwoStatePreference mDCModeSwitch;
    private TwoStatePreference mHBMModeSwitch;
    private TwoStatePreference mRefreshRate;
    private SwitchPreference mFpsInfo;
    private SwitchPreference mAlwaysCameraSwitch;
    private SwitchPreference mMuteMediaSwitch;

    private boolean mInternalFpsStart = false;
    private boolean mInternalHbmStart = false;
    private boolean mInternalDCStart = false;

    private final BroadcastReceiver mServiceStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case FPSInfoService.ACTION_FPS_SERVICE_CHANGED:
                    if (mInternalFpsStart) {
                        mInternalFpsStart = false;
                        return;
                    }
                    if (mFpsInfo == null) return;
                    final boolean fpsStarted = intent.getBooleanExtra(
                            FPSInfoService.EXTRA_FPS_STATE, false);
                    mFpsInfo.setChecked(fpsStarted);
                    break;
                case HBMModeSwitch.ACTION_HBM_SERVICE_CHANGED:
                    if (mInternalHbmStart) {
                        mInternalHbmStart = false;
                        return;
                    }
                    if (mHBMModeSwitch == null) return;
                    final boolean hbmStarted = intent.getBooleanExtra(
                            HBMModeSwitch.EXTRA_HBM_STATE, false);
                    mHBMModeSwitch.setChecked(hbmStarted);
                    break;
                case DCModeSwitch.ACTION_DCMODE_CHANGED:
                    if (mInternalDCStart) {
                        mInternalDCStart = false;
                        return;
                    }
                    if (mDCModeSwitch == null) return;
                    final boolean dcEnabled = intent.getBooleanExtra(
                            DCModeSwitch.EXTRA_DCMODE_STATE, false);
                    mDCModeSwitch.setChecked(dcEnabled);
                    break;
            }
        }
    };

    private final ContentObserver mRefreshRateObserver = new ContentObserver(
            new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            if (mRefreshRate == null) return;
            mRefreshRate.setChecked(RefreshRateSwitch.isCurrentlyEnabled(getContext()));
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.main);

        ListPreference mTopKeyPref = findPreference(Constants.NOTIF_SLIDER_TOP_KEY);
        mTopKeyPref.setValueIndex(Constants.getPreferenceInt(getContext(), Constants.NOTIF_SLIDER_TOP_KEY));
        mTopKeyPref.setOnPreferenceChangeListener(this);
        ListPreference mMiddleKeyPref = findPreference(Constants.NOTIF_SLIDER_MIDDLE_KEY);
        mMiddleKeyPref.setValueIndex(Constants.getPreferenceInt(getContext(), Constants.NOTIF_SLIDER_MIDDLE_KEY));
        mMiddleKeyPref.setOnPreferenceChangeListener(this);
        ListPreference mBottomKeyPref = findPreference(Constants.NOTIF_SLIDER_BOTTOM_KEY);
        mBottomKeyPref.setValueIndex(Constants.getPreferenceInt(getContext(), Constants.NOTIF_SLIDER_BOTTOM_KEY));
        mBottomKeyPref.setOnPreferenceChangeListener(this);

        mMuteMediaSwitch = findPreference(Constants.NOTIF_SLIDER_MUTE_MEDIA_KEY);
        mMuteMediaSwitch.setChecked(Constants.getIsMuteMediaEnabled(getContext()));
        mMuteMediaSwitch.setOnPreferenceChangeListener(this);

        mDCModeSwitch = findPreference(DCModeSwitch.KEY_DC_SWITCH);
        mDCModeSwitch.setEnabled(DCModeSwitch.isSupported());
        mDCModeSwitch.setChecked(DCModeSwitch.isCurrentlyEnabled());
        mDCModeSwitch.setOnPreferenceChangeListener(this);

        mHBMModeSwitch = findPreference(KEY_HBM_SWITCH);
        mHBMModeSwitch.setEnabled(HBMModeSwitch.isSupported());
        mHBMModeSwitch.setChecked(HBMModeSwitch.isCurrentlyEnabled());
        mHBMModeSwitch.setOnPreferenceChangeListener(this);

        if (getResources().getBoolean(R.bool.config_deviceHasHighRefreshRate)) {
            mRefreshRate = findPreference(KEY_REFRESH_RATE);
            mRefreshRate.setChecked(RefreshRateSwitch.isCurrentlyEnabled(getContext()));
            mRefreshRate.setOnPreferenceChangeListener(this);
        } else {
            getPreferenceScreen().removePreference(findPreference(KEY_REFRESH_RATE));
        }

        mFpsInfo = findPreference(KEY_FPS_INFO);
        mFpsInfo.setChecked(isFPSOverlayRunning());
        mFpsInfo.setOnPreferenceChangeListener(this);

        PreferenceCategory mCameraCategory = findPreference(KEY_CATEGORY_CAMERA);
        boolean hasPopup = Utils.isPackageInstalled(POPUP_HELPER_PKG_NAME, getContext());
        if (hasPopup) {
            mAlwaysCameraSwitch = findPreference(KEY_ALWAYS_CAMERA_DIALOG);
            boolean enabled = Settings.System.getInt(getContext().getContentResolver(),
                    KEY_SETTINGS_PREFIX + KEY_ALWAYS_CAMERA_DIALOG, 0) == 1;
            mAlwaysCameraSwitch.setChecked(enabled);
            mAlwaysCameraSwitch.setOnPreferenceChangeListener(this);
        } else {
            mCameraCategory.setVisible(false);
        }

        // Registering observers
        IntentFilter filter = new IntentFilter();
        filter.addAction(FPSInfoService.ACTION_FPS_SERVICE_CHANGED);
        filter.addAction(HBMModeSwitch.ACTION_HBM_SERVICE_CHANGED);
        filter.addAction(DCModeSwitch.ACTION_DCMODE_CHANGED);
        getContext().registerReceiver(mServiceStateReceiver, filter);

        if (getResources().getBoolean(R.bool.config_deviceHasHighRefreshRate)) {
            getContext().getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(
                    Settings.System.PEAK_REFRESH_RATE),
                    false, mRefreshRateObserver, UserHandle.USER_ALL);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mHBMModeSwitch.setChecked(HBMModeSwitch.isCurrentlyEnabled());
        mFpsInfo.setChecked(isFPSOverlayRunning());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final ContentResolver resolver = getContext().getContentResolver();
        if (preference == mFpsInfo) {
            mInternalFpsStart = true;
            boolean enabled = (Boolean) newValue;
            Intent fpsinfo = new Intent(getContext(), FPSInfoService.class);
            if (enabled) getContext().startService(fpsinfo);
            else getContext().stopService(fpsinfo);
        } else if (preference == mAlwaysCameraSwitch) {
            boolean enabled = (Boolean) newValue;
            Settings.System.putInt(resolver,
                    KEY_SETTINGS_PREFIX + KEY_ALWAYS_CAMERA_DIALOG,
                    enabled ? 1 : 0);
        } else if (preference == mRefreshRate) {
            Boolean enabled = (Boolean) newValue;
            RefreshRateSwitch.setPeakRefresh(getContext(), enabled);
        } else if (preference == mHBMModeSwitch) {
            mInternalHbmStart = true;
            Boolean enabled = (Boolean) newValue;
            HBMModeSwitch.setEnabled(enabled, getContext());            
        } else if (preference == mMuteMediaSwitch) {
            Boolean enabled = (Boolean) newValue;
            Settings.System.putInt(resolver,
                    Constants.NOTIF_SLIDER_MUTE_MEDIA_KEY, enabled ? 1 : 0);
        } else if (preference == mDCModeSwitch) {
            mInternalDCStart = true;
            Boolean enabled = (Boolean) newValue;
            DCModeSwitch.setEnabled(enabled, getContext());
        } else if (newValue instanceof String) {
            Constants.setPreferenceInt(getContext(), preference.getKey(),
                    Integer.parseInt((String) newValue));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Respond to the action bar's Up/Home button
        if (item.getItemId() == android.R.id.home) {
            getActivity().finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unregisterReceiver(mServiceStateReceiver);
        if (getResources().getBoolean(R.bool.config_deviceHasHighRefreshRate)) {
            getContext().getContentResolver().unregisterContentObserver(
                    mRefreshRateObserver);
        }
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
