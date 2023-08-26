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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

// TODO: Add FPS drawables
public class FPSTileService extends TileService {

    private boolean mIsShowing = false;
    private boolean mInternalStart = false;

    private final BroadcastReceiver mServiceStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mInternalStart) {
                mInternalStart = false;
                return;
            }
            mIsShowing = intent.getBooleanExtra(FPSInfoService.EXTRA_FPS_STATE, false);
            updateTile();
        }
    };

    public FPSTileService() { }

    @Override
    public void onStartListening() {
        super.onStartListening();
        mIsShowing = isRunning();
        updateTile();
        IntentFilter filter = new IntentFilter(FPSInfoService.ACTION_FPS_SERVICE_CHANGED);
        registerReceiver(mServiceStateReceiver, filter);
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        unregisterReceiver(mServiceStateReceiver);
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
        final SharedPreferences prefs = Constants.getDESharedPrefs(this);
        return prefs.getBoolean(FPSInfoService.PREF_KEY_FPS_STATE, false);
    }
}
