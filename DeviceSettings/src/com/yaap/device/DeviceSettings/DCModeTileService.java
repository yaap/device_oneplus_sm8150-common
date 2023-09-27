/*
* Copyright (C) 2018 The OmniROM Project
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

import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.yaap.device.DeviceSettings.ModeSwitch.DCModeSwitch;

public class DCModeTileService extends TileService
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences mPrefs;
    private boolean mEnabled = false;
    private boolean mInternalStart = false;

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)  {
        if (!key.equals(DCModeSwitch.KEY_DC_SWITCH)) return;
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
        mEnabled = DCModeSwitch.isCurrentlyEnabled();
        DCModeSwitch.setEnabled(!mEnabled, this);
        //getQsTile().setLabel(mEnabled ? "DC off" : "DC On");
        getQsTile().setIcon(Icon.createWithResource(this,
                    mEnabled ? R.drawable.ic_dimming_off : R.drawable.ic_dimming_on));
        getQsTile().setState(mEnabled ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
        getQsTile().updateTile();
    }

    private void refreshState() {
        mEnabled = DCModeSwitch.isCurrentlyEnabled();
        getQsTile().setIcon(Icon.createWithResource(this,
                    mEnabled ? R.drawable.ic_dimming_on : R.drawable.ic_dimming_off));
        getQsTile().setState(mEnabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        getQsTile().updateTile();
    }
}
