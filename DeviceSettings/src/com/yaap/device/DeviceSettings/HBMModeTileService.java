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

import android.content.Intent;
import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.yaap.device.DeviceSettings.ModeSwitch.HBMModeSwitch;

public class HBMModeTileService extends TileService
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences mPrefs;
    private Intent mHbmIntent;
    private boolean mInternalStart = false;

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)  {
        if (!key.equals(HBMModeSwitch.PREF_KEY_HBM_STATE)) return;
        if (mInternalStart) {
            mInternalStart = false;
            return;
        }
        updateState();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
    }

    @Override
    public void onTileRemoved() {
        tryStopService();
        super.onTileRemoved();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateState();
        mPrefs = Constants.getDESharedPrefs(getApplicationContext());
        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        mPrefs = null;
    }

    @Override
    public void onClick() {
        super.onClick();
        mInternalStart = true;
        boolean enabled = HBMModeSwitch.isCurrentlyEnabled();
        HBMModeSwitch.setEnabled(!enabled, this);
        updateState();
    }

    private void updateState() {
        boolean enabled = HBMModeSwitch.isCurrentlyEnabled();
        if (!enabled) tryStopService();
        getQsTile().setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        getQsTile().updateTile();
    }

    private void tryStopService() {
        if (mHbmIntent == null) return;
        this.stopService(mHbmIntent);
        mHbmIntent = null;
    }
}
