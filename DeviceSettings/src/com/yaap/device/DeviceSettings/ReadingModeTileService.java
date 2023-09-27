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

import android.content.Context;
import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.yaap.device.DeviceSettings.ModeSwitch.ReadingModeSwitch;

public class ReadingModeTileService extends TileService
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences mPrefs;
    private boolean mInternalStart = false;

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)  {
        if (!key.equals(ReadingModeSwitch.KEY_READING_SWITCH)) return;
        if (mInternalStart) {
            mInternalStart = false;
            return;
        }
        refreshState();
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
        super.onTileRemoved();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        refreshState();
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
        int state = ReadingModeSwitch.getState(this);
        if (++state > 2) state = 0;
        ReadingModeSwitch.setState(state, this);
        refreshState(state);
    }

    private void refreshState() {
        refreshState(ReadingModeSwitch.getState(this));
    }

    private void refreshState(int state) {
        final Tile tile = getQsTile();
        setLabelByState(this, tile, state);
        setTileStateByState(this, tile, state);
        tile.updateTile();
    }

    private static void setLabelByState(Context context, Tile tile, int state) {
        if (!ReadingModeSwitch.isSupported()) return;
        String label = context.getString(R.string.off);
        if (state == ReadingModeSwitch.STATE_ENABLED)
            label = context.getString(R.string.enabled);
        else if (state == ReadingModeSwitch.STATE_ENABLED_HIGH)
            label = context.getString(R.string.enabled_high);
        tile.setSubtitle(label);
    }

    private static void setTileStateByState(Context context, Tile tile, int state) {
        int tileState = Tile.STATE_INACTIVE;
        if (!ReadingModeSwitch.isSupported())
            tileState = Tile.STATE_UNAVAILABLE;
        else if (state != ReadingModeSwitch.STATE_DISABLED)
            tileState = Tile.STATE_ACTIVE;
        tile.setState(tileState);
    }
}
