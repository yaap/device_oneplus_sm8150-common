/*
 * Copyright (C) 2015 The CyanogenMod Project
 * Copyright (C) 2017 The LineageOS Project
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

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.UserHandle;
import android.provider.Settings;
import androidx.preference.SwitchPreference;

import android.media.AudioManager;

public class Constants {

    // Broadcast action for settings update
    static final String UPDATE_PREFS_ACTION = "com.yaap.device.DeviceSettings.UPDATE_SETTINGS";
    static final String SLIDER_UPDATE_ACTION = "com.yaap.device.DeviceSettings.UPDATE_SLIDER";

    // Shared preferences
    private static final String DE_PREF_FILE_NAME = "device_settings";

    // Preference keys
    public static final String NOTIF_SLIDER_TOP_KEY = "keycode_top_position";
    public static final String NOTIF_SLIDER_MIDDLE_KEY = "keycode_middle_position";
    public static final String NOTIF_SLIDER_BOTTOM_KEY = "keycode_bottom_position";
    public static final String NOTIF_SLIDER_MUTE_MEDIA_KEY = "slider_mute_media";
    private static final String NOTIF_SLIDER_MUTE_MEDIA_LEVEL_KEY = "slider_mute_media_level";

    // Slider positions
    public static final int POSITION_TOP = 603;
    public static final int POSITION_MIDDLE = 602;
    public static final int POSITION_BOTTOM = 601;

    // Button prefs
    public static final String NOTIF_SLIDER_TOP_PREF = "pref_keycode_top_position";
    public static final String NOTIF_SLIDER_MIDDLE_PREF = "pref_keycode_middle_position";
    public static final String NOTIF_SLIDER_BOTTOM_PREF = "pref_keycode_bottom_position";

    // Default values
    public static final int KEY_VALUE_TOTAL_SILENCE = 0;
    public static final int KEY_VALUE_SILENT = 1;
    public static final int KEY_VALUE_PRIORTY_ONLY = 2;
    public static final int KEY_VALUE_VIBRATE = 3;
    public static final int KEY_VALUE_NORMAL = 4;

    // Screen off Gesture actions
    static final int ACTION_FLASHLIGHT = 1;
    static final int ACTION_CAMERA = 2;
    static final int ACTION_BROWSER = 3;
    static final int ACTION_DIALER = 4;
    static final int ACTION_EMAIL = 5;
    static final int ACTION_MESSAGES = 6;
    static final int ACTION_PLAY_PAUSE_MUSIC = 7;
    static final int ACTION_PREVIOUS_TRACK = 8;
    static final int ACTION_NEXT_TRACK = 9;
    static final int ACTION_VOLUME_DOWN = 10;
    static final int ACTION_VOLUME_UP = 11;
    static final int ACTION_AMBIENT_DISPLAY = 12;
    static final int ACTION_WAKE_DEVICE = 13;

    public static final Map<String, String> sStringKeyPreferenceMap = new HashMap<>();
    public static final Map<Integer, String> sKeyMap = new HashMap<>();
    public static final Map<String, Integer> sKeyDefaultMap = new HashMap<>();

    // Broadcast extra: keycode mapping (int[]: key = gesture ID, value = keycode)
    static final String UPDATE_EXTRA_KEYCODE_MAPPING = "keycode_mappings";
    // Broadcast extra: assigned actions (int[]: key = gesture ID, value = action)
    static final String UPDATE_EXTRA_ACTION_MAPPING = "action_mappings";

    static {
        sStringKeyPreferenceMap.put(NOTIF_SLIDER_TOP_KEY, NOTIF_SLIDER_TOP_PREF);
        sStringKeyPreferenceMap.put(NOTIF_SLIDER_MIDDLE_KEY, NOTIF_SLIDER_MIDDLE_PREF);
        sStringKeyPreferenceMap.put(NOTIF_SLIDER_BOTTOM_KEY, NOTIF_SLIDER_BOTTOM_PREF);

        sKeyMap.put(POSITION_TOP, NOTIF_SLIDER_TOP_KEY);
        sKeyMap.put(POSITION_MIDDLE, NOTIF_SLIDER_MIDDLE_KEY);
        sKeyMap.put(POSITION_BOTTOM, NOTIF_SLIDER_BOTTOM_KEY);

        sKeyDefaultMap.put(NOTIF_SLIDER_TOP_KEY, KEY_VALUE_TOTAL_SILENCE);
        sKeyDefaultMap.put(NOTIF_SLIDER_MIDDLE_KEY, KEY_VALUE_VIBRATE);
        sKeyDefaultMap.put(NOTIF_SLIDER_BOTTOM_KEY, KEY_VALUE_NORMAL);
    }

    public static int getPreferenceInt(Context context, String key) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                sStringKeyPreferenceMap.get(key), sKeyDefaultMap.get(key), UserHandle.USER_CURRENT);
    }

    public static void setPreferenceInt(Context context, String key, int value) {
        Settings.System.putIntForUser(context.getContentResolver(),
                sStringKeyPreferenceMap.get(key), value, UserHandle.USER_CURRENT);
    }

    public static void setLastMediaLevel(Context context, int level) {
        Settings.System.putIntForUser(context.getContentResolver(),
                NOTIF_SLIDER_MUTE_MEDIA_LEVEL_KEY, level, UserHandle.USER_CURRENT);
    }

    public static int getLastMediaLevel(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                NOTIF_SLIDER_MUTE_MEDIA_LEVEL_KEY, 80, UserHandle.USER_CURRENT);
    }

    public static boolean getIsMuteMediaEnabled(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                NOTIF_SLIDER_MUTE_MEDIA_KEY, 0, UserHandle.USER_CURRENT) == 1;
    }

    public static SharedPreferences getDESharedPrefs(Context context) {
        return context.createDeviceProtectedStorageContext()
                .getSharedPreferences(DE_PREF_FILE_NAME, Context.MODE_PRIVATE);
    }
}
