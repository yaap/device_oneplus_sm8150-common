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

import android.database.ContentObserver;
import android.graphics.drawable.Icon;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class RefreshRateTileService extends TileService {
    private boolean mIsInternalChange = false;
    private final ContentObserver mRefreshRateObserver = new ContentObserver(
            new Handler(Looper.getMainLooper())) {
        void observe() {
            getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(
                    Settings.System.PEAK_REFRESH_RATE),
                    false, this, UserHandle.USER_ALL);
        }

        void stop() {
            getContentResolver().unregisterContentObserver(this);
        } 

        @Override
        public void onChange(boolean selfChange) {
            if (mIsInternalChange) {
                mIsInternalChange = false;
                return;
            }
            refreshState();
        }
    };

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
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();
        mIsInternalChange = true;
        boolean enabled = RefreshRateSwitch.isCurrentlyEnabled(this);
        RefreshRateSwitch.setPeakRefresh(this, !enabled);
        getQsTile().setIcon(Icon.createWithResource(this,
                enabled ? R.drawable.ic_refresh_tile_90 : R.drawable.ic_refresh_tile_60));
        getQsTile().setState(enabled ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
        getQsTile().updateTile();
    }

    private void refreshState() {
        boolean enabled = RefreshRateSwitch.isCurrentlyEnabled(this);
        getQsTile().setIcon(Icon.createWithResource(this,
                enabled ? R.drawable.ic_refresh_tile_60 : R.drawable.ic_refresh_tile_90));
        getQsTile().setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        getQsTile().updateTile();
    }
}
