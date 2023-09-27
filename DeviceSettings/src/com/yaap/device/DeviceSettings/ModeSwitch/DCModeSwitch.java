/*
* Copyright (C) Yet Another AOSP Project
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
package com.yaap.device.DeviceSettings.ModeSwitch;

import android.content.SharedPreferences;
import android.content.Context;

import com.yaap.device.DeviceSettings.Constants;
import com.yaap.device.DeviceSettings.Utils;

public class DCModeSwitch {

    private static final String FILE = "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/dc_dim";

    public static final String KEY_DC_SWITCH = "dc";

    public static String getFile() {
        if (Utils.fileWritable(FILE)) {
            return FILE;
        }
        return null;
    }

    public static boolean isSupported() {
        return Utils.fileWritable(getFile());
    }

    public static boolean isCurrentlyEnabled() {
        return Utils.getFileValueAsBoolean(getFile(), false);
    }

    public static void setEnabled(boolean enabled, Context context) {
        Utils.writeValue(getFile(), enabled ? "1" : "0");
        SharedPreferences prefs = Constants.getDESharedPrefs(context);
        prefs.edit().putBoolean(KEY_DC_SWITCH, enabled).commit();
    }
}
