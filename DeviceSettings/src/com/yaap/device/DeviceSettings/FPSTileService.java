/*
 * Copyright (C) 2020 YAAP
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

import android.content.Intent;
import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

// TODO: Add FPS drawables
public class FPSTileService extends TileService
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences mPrefs;
    private boolean mIsShowing = false;
    private boolean mInternalStart = false;

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)  {
        if (!key.equals(FPSInfoService.PREF_KEY_FPS_STATE)) return;
        if (mInternalStart) {
            mInternalStart = false;
            return;
        }
        mIsShowing = sharedPreferences.getBoolean(key, false);
        updateTile();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        mPrefs = Constants.getDESharedPrefs(getApplicationContext());
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        mIsShowing = isRunning();
        updateTile();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        mPrefs = null;
    }

    @Override
    public void onClick() {
        mInternalStart = true;
        Intent fpsinfo = new Intent(this, FPSInfoService.class);
        mIsShowing = isRunning();
        if (!mIsShowing) this.startService(fpsinfo);
        else this.stopService(fpsinfo);
        mIsShowing = !mIsShowing;
        updateTile();
    }

    private void updateTile() {
        final Tile tile = getQsTile();
        tile.setState(mIsShowing ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    private boolean isRunning() {
        if (mPrefs == null) return false;
        return mPrefs.getBoolean(FPSInfoService.PREF_KEY_FPS_STATE, false);
    }
}
