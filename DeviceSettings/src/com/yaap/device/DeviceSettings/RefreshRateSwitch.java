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

import android.content.Context;
import android.provider.Settings;

public class RefreshRateSwitch {

    public RefreshRateSwitch() { }

    /**
     * Gets the current state of the switch
     * @param context context for Resources and ContentResolver
     * @return true if peak rate is currently forced to 60Hz
     **/
    public static boolean isCurrentlyEnabled(Context context) {
        float def = context.getResources().getInteger(
                com.android.internal.R.integer.config_defaultPeakRefreshRate);
        return Settings.System.getFloat(context.getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE, def) == 60f;
    }

    /**
     * Sets peak refresh rate
     * @param context context for Resources and ContentResolver
     * @param enabled true to force 60Hz, false for default peak rate
     */
    public static void setPeakRefresh(Context context, boolean enabled) {
        float def = context.getResources().getInteger(
                com.android.internal.R.integer.config_defaultPeakRefreshRate);
        Settings.System.putFloat(context.getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE, enabled ? 60f : def);
    }
}
