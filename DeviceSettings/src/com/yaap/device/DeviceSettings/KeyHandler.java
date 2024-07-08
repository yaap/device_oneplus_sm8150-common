/*
 * Copyright (C) 2021 Yet Another AOSP Project
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

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.VibrationEffect;
import android.os.VibratorManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.SparseIntArray;
import android.view.KeyEvent;

import com.android.internal.os.DeviceKeyHandler;

public class KeyHandler implements DeviceKeyHandler {

    private static final String TAG = KeyHandler.class.getSimpleName();

    private static final SparseIntArray sSupportedSliderZenModes = new SparseIntArray();
    private static final SparseIntArray sSupportedSliderRingModes = new SparseIntArray();
    private static final SparseIntArray sSupportedSliderHaptics = new SparseIntArray();
    static {
        sSupportedSliderZenModes.put(Constants.KEY_VALUE_TOTAL_SILENCE, Settings.Global.ZEN_MODE_NO_INTERRUPTIONS);
        sSupportedSliderZenModes.put(Constants.KEY_VALUE_SILENT, Settings.Global.ZEN_MODE_OFF);
        sSupportedSliderZenModes.put(Constants.KEY_VALUE_PRIORTY_ONLY, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        sSupportedSliderZenModes.put(Constants.KEY_VALUE_VIBRATE, Settings.Global.ZEN_MODE_OFF);
        sSupportedSliderZenModes.put(Constants.KEY_VALUE_NORMAL, Settings.Global.ZEN_MODE_OFF);

        sSupportedSliderRingModes.put(Constants.KEY_VALUE_TOTAL_SILENCE, AudioManager.RINGER_MODE_NORMAL);
        sSupportedSliderRingModes.put(Constants.KEY_VALUE_SILENT, AudioManager.RINGER_MODE_SILENT);
        sSupportedSliderRingModes.put(Constants.KEY_VALUE_PRIORTY_ONLY, AudioManager.RINGER_MODE_NORMAL);
        sSupportedSliderRingModes.put(Constants.KEY_VALUE_VIBRATE, AudioManager.RINGER_MODE_VIBRATE);
        sSupportedSliderRingModes.put(Constants.KEY_VALUE_NORMAL, AudioManager.RINGER_MODE_NORMAL);

        sSupportedSliderHaptics.put(Constants.KEY_VALUE_TOTAL_SILENCE, VibrationEffect.EFFECT_THUD);
        sSupportedSliderHaptics.put(Constants.KEY_VALUE_SILENT, VibrationEffect.EFFECT_DOUBLE_CLICK);
        sSupportedSliderHaptics.put(Constants.KEY_VALUE_PRIORTY_ONLY, VibrationEffect.EFFECT_POP);
        sSupportedSliderHaptics.put(Constants.KEY_VALUE_VIBRATE, VibrationEffect.EFFECT_HEAVY_CLICK);
        sSupportedSliderHaptics.put(Constants.KEY_VALUE_NORMAL, -1);
    }

    private final Context mContext;
    private final NotificationManager mNotificationManager;
    private final AudioManager mAudioManager;
    private final HandlerThread mHandlerThread = new HandlerThread("KeyHandlerThread");
    private final Handler mHandler;
    private boolean mNeedsRun;
    private Vibrator mVibrator;
    private int mPrevKeyCode = 0;

    public KeyHandler(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        VibratorManager vm = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
        mVibrator = vm != null ? vm.getDefaultVibrator() : null;
        if (mVibrator == null || !mVibrator.hasVibrator()) {
            mVibrator = null;
        }

        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    private boolean hasSetupCompleted() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.USER_SETUP_COMPLETE, 0) != 0;
    }

    @Override
    public KeyEvent handleKeyEvent(KeyEvent event) {
        final int scanCode = event.getScanCode();
        final String keyCode = Constants.sKeyMap.get(scanCode);
        int keyCodeValue;

        try {
            keyCodeValue = Constants.getPreferenceInt(mContext, keyCode);
        } catch (Exception e) {
            return event;
        }

        if (!hasSetupCompleted()) {
            return event;
        }

        // We only want ACTION_UP event
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return event;
        }

        mNeedsRun = false;
        mHandler.removeCallbacksAndMessages(null);

        if (mPrevKeyCode == Constants.KEY_VALUE_TOTAL_SILENCE && keyCodeValue != mPrevKeyCode) {
            // if previous was total silence we need to vibrate after setRingerModeInternal
            // for it to actually fire.
            // we also have to exit it before setRingerModeInternal because it sets it internally
            final int targetMode = sSupportedSliderRingModes.get(keyCodeValue);
            mNotificationManager.setZenMode(sSupportedSliderZenModes.get(keyCodeValue), null, TAG);
            mAudioManager.setRingerModeInternal(targetMode);
            doHapticFeedback(sSupportedSliderHaptics.get(keyCodeValue));
            // make sure ringer mode was set correctly (race condition because setZenMode is async)
            mNeedsRun = true;
            mHandler.postDelayed(() -> {
                if (mAudioManager.getRingerModeInternal() != targetMode && mNeedsRun) {
                    mAudioManager.setRingerModeInternal(targetMode);
                }
            }, 200); // 200ms is long enough even if the system is very busy
        } else {
            // here we have to vibrate before setting anything else.
            // also setRingerModeInternal before setZenMode because it could set the ringer mode
            doHapticFeedback(sSupportedSliderHaptics.get(keyCodeValue));
            mAudioManager.setRingerModeInternal(sSupportedSliderRingModes.get(keyCodeValue));
            mNotificationManager.setZenMode(sSupportedSliderZenModes.get(keyCodeValue), null, TAG);
        }

        if (Constants.getIsMuteMediaEnabled(mContext)) {
            final int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            final int curr = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (keyCodeValue == Constants.KEY_VALUE_SILENT) {
                // going into silent:
                // saving current media volume and setting to 0
                Constants.setLastMediaLevel(mContext, Math.round((float)curr * 100f / (float)max));
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                        0, AudioManager.FLAG_SHOW_UI);
            } else if (mPrevKeyCode == Constants.KEY_VALUE_SILENT && curr == 0) {
                // going out of silent:
                // setting media volume back if and only if current volume is still 0
                final int last = Constants.getLastMediaLevel(mContext);
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                        Math.round((float)max * (float)last / 100f), AudioManager.FLAG_SHOW_UI);
            }
        }
        if (Constants.getIsSliderDialogEnabled(mContext))
            sendNotification(scanCode, keyCodeValue);

        mPrevKeyCode = keyCodeValue;
        return null;
    }

    @Override
    public void onPocketStateChanged(boolean inPocket) {
        // do nothing
    }

    private void doHapticFeedback(int effect) {
        if (mVibrator != null && mVibrator.hasVibrator() && effect != -1) {
            mVibrator.vibrate(VibrationEffect.get(effect));
        }
    }

    private void sendNotification(int position, int mode) {
        final Intent intent = new Intent(Constants.SLIDER_UPDATE_ACTION);
        intent.putExtra("position", position);
        intent.putExtra("mode", mode);
        mContext.sendBroadcast(intent);
    }
}
