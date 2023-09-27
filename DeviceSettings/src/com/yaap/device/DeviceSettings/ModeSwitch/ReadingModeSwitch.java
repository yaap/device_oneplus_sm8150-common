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
package com.yaap.device.DeviceSettings.ModeSwitch;

import android.content.SharedPreferences;
import android.content.Context;
import android.os.Build;

import com.yaap.device.DeviceSettings.Constants;
import com.yaap.device.DeviceSettings.Utils;

public class ReadingModeSwitch {

    private static final String FILE = "/sys/devices/platform/soc/ae00000.qcom,mdss_mdp/drm/card0/card0-DSI-1/reading";

    private static final String[] SUPPORTED_DEVICES = {
        "OnePlus7T",
        "OnePlus7"
    };

    public static final String KEY_READING_SWITCH = "reading_mode";
    public static final int STATE_DISABLED = 0;
    public static final int STATE_ENABLED = 1;
    public static final int STATE_ENABLED_HIGH = 2;

    public static String getFile() {
        if (Utils.fileWritable(FILE)) {
            return FILE;
        }
        return null;
    }

    public static boolean isSupported() {
        boolean deviceSupported = false;
        for (String str : SUPPORTED_DEVICES) {
            if (Build.DEVICE.equals(str)) {
                deviceSupported = true;
                break;
            }
        }
        return deviceSupported && Utils.fileWritable(getFile());
    }

    public static void setState(int state, Context context) {
        Utils.writeValue(getFile(), String.valueOf(state));
        SharedPreferences prefs = Constants.getDESharedPrefs(context);
        prefs.edit().putInt(KEY_READING_SWITCH, state).commit();
    }

    public static int getState(Context context) {
        return Integer.parseInt(Utils.getFileValue(getFile(), String.valueOf(STATE_DISABLED)));
    }
}
