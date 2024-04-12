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

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.content.Intent.ACTION_BOOT_COMPLETED;
import static android.content.Intent.ACTION_LOCKED_BOOT_COMPLETED;

import static com.yaap.device.DeviceSettings.FPSInfoService.PREF_KEY_FPS_STATE;
import static com.yaap.device.DeviceSettings.ModeSwitch.DCModeSwitch.KEY_DC_SWITCH;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.provider.Settings;
import androidx.preference.PreferenceManager;

import com.yaap.device.DeviceSettings.ModeSwitch.*;

import java.util.List;
import java.util.Map;

public class Startup extends BroadcastReceiver {

    private static final String KEY_MIGRATION_DONE = "migration_done_2";
    private static final String PKG_NAME = "com.yaap.device.DeviceSettings";
    private static final String READING_TILE_CLASS_NAME = PKG_NAME + ".ReadingModeTileService";

    private static final Map<String, String> sKeyFileMap = Map.of(
        // DC Dimming
        KEY_DC_SWITCH, DCModeSwitch.getFile()
    );

    private void restore(String file, boolean enabled) {
        if (file == null) return;
        if (enabled) Utils.writeValue(file, "1");
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final SharedPreferences dePrefs = Constants.getDESharedPrefs(context);

        if (intent.getAction().equals(ACTION_BOOT_COMPLETED)) {
            if (!dePrefs.getBoolean(KEY_MIGRATION_DONE, false)) {
                // migration of old user encrypted preferences
                final SharedPreferences oldPrefs = PreferenceManager.getDefaultSharedPreferences(context);
                final SharedPreferences.Editor oldPrefsEditor = oldPrefs.edit();
                final SharedPreferences.Editor dePrefsEditor = dePrefs.edit();

                for (String prefKey : sKeyFileMap.keySet()) {
                    if (!oldPrefs.contains(prefKey)) continue;
                    dePrefsEditor.putBoolean(prefKey, oldPrefs.getBoolean(prefKey, false));
                    oldPrefsEditor.remove(prefKey);
                }

                dePrefsEditor.putBoolean(KEY_MIGRATION_DONE, true);
                // must use commit (and not apply) because of what follows!
                dePrefsEditor.commit();
                oldPrefsEditor.commit();

                TouchscreenGestureSettings.MainSettingsFragment.migrateTouchscreenGestureStates(context);
            }

            // disable unavailable tiles
            if (!ReadingModeSwitch.isSupported()) {
                PackageManager pm = context.getPackageManager();
                ComponentName cn = new ComponentName(PKG_NAME, READING_TILE_CLASS_NAME);
                final int enabledSetting = pm.getComponentEnabledSetting(cn);
                if (enabledSetting != COMPONENT_ENABLED_STATE_DISABLED)
                    pm.setComponentEnabledSetting(cn, COMPONENT_ENABLED_STATE_DISABLED, 0);
            }
        }

        TouchscreenGestureSettings.MainSettingsFragment.restoreTouchscreenGestureStates(context);

        // restoring state from DE shared preferences
        for (Map.Entry<String, String> set : sKeyFileMap.entrySet()) {
            final String prefKey = set.getKey();
            final String file = set.getValue();
            restore(file, dePrefs.getBoolean(prefKey, false));
        }

        // reset prefs that reflect a state that does not retain a reboot
        List<String> touchKeys = TouchscreenGestureSettings.MainSettingsFragment.getPrefKeys(context);
        Map<String,?> keys = dePrefs.getAll();
        for (Map.Entry<String,?> entry : keys.entrySet()) {
            final String key = entry.getKey();
            if (sKeyFileMap.containsKey(key)) continue;
            if (touchKeys.contains(key)) continue;
            if (KEY_MIGRATION_DONE.equals(key)) continue;
            dePrefs.edit().remove(key).commit();
        }
    }
}
