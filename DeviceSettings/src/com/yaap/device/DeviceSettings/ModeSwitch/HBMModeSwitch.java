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

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

import com.yaap.device.DeviceSettings.Utils;

public class HBMModeSwitch {

    public static final String ACTION_HBM_SERVICE_CHANGED =
            "com.yaap.device.DeviceSettings.ModeSwitch.HBM_SERVICE_CHANGED";
    public static final String EXTRA_HBM_STATE = "running";
    
    private static final String FILE = "/sys/devices/platform/soc/ae00000.qcom,mdss_mdp/drm/card0/card0-DSI-1/hbm";

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
        Utils.writeValue(getFile(), enabled ? "5" : "0");
        Intent hbmIntent = new Intent(context,
                    com.yaap.device.DeviceSettings.HBMModeService.class);
        if (enabled) context.startService(hbmIntent);
        else context.stopService(hbmIntent);
        Intent intent = new Intent(ACTION_HBM_SERVICE_CHANGED);
        intent.putExtra(EXTRA_HBM_STATE, enabled);
        intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcastAsUser(intent, UserHandle.CURRENT);;
    }
}
