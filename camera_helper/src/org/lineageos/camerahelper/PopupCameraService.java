/*
 * Copyright (c) 2019 The LineageOS Project
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

package org.lineageos.camerahelper;

import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;

public class PopupCameraService extends Service {
    private static final String TAG = "PopupCameraService";
    private static final boolean DEBUG = true;

    private static final String closeCameraState = "0";
    private static final String openCameraState = "1";
    // Should follow KEY_SETTINGS_PREFIX + KEY_ALWAYS_CAMERA_DIALOG
    // From org.omnirom.device.DeviceSettings:
    private static final String alwaysOnDialogKey = "device_setting_always_on_camera_dialog";

    private AlertDialog mAlertDialog;
    private FallSensor mFallSensor;

    private boolean mMotorDown;
    private boolean mScreenOn = true;
    private int mDialogThemeResID;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                if (DEBUG) Log.d(TAG, "Screen off, disabling fall sensor");
                mFallSensor.disable();
                mScreenOn = false;
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                mScreenOn = true;
            } else if (action.equals(Intent.ACTION_CAMERA_STATUS_CHANGED)) {
                mMotorDown = CameraMotorController.getMotorPosition().equals(CameraMotorController.POSITION_DOWN);
                String cameraState = intent.getExtras().getString(Intent.EXTRA_CAMERA_STATE);
                if (DEBUG) Log.d(TAG, "Intent, ACTION_CAMERA_STATUS_CHANGED=" + cameraState + " motorDown=" + mMotorDown);
                updateMotor(cameraState);
            }
        }
    };

    @Override
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service");
        mFallSensor = new FallSensor(this);
        mDialogThemeResID = android.R.style.Theme_DeviceDefault_Light_Dialog_Alert;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_CAMERA_STATUS_CHANGED);
        registerReceiver(mIntentReceiver, intentFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "Starting service");
        mFallSensor.enable();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service");
        mFallSensor.disable();
        unregisterReceiver(mIntentReceiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void updateMotor(String cameraState) {
        if (cameraState.equals(openCameraState) && mMotorDown) {
            boolean alwaysOnDialog = Settings.System.getInt(getContentResolver(),
                        alwaysOnDialogKey, 0) == 1;
            if (alwaysOnDialog || !mScreenOn) {
                updateDialogTheme();
                if (mAlertDialog == null) {
                    mAlertDialog = new AlertDialog.Builder(this, mDialogThemeResID)
                            .setMessage(R.string.popup_camera_dialog_message)
                            .setNegativeButton(R.string.popup_camera_dialog_no, (dialog, which) -> {
                                // Go back to home screen
                                Intent intent = new Intent(Intent.ACTION_MAIN);
                                intent.addCategory(Intent.CATEGORY_HOME);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                this.startActivity(intent);
                            })
                            .setPositiveButton(R.string.popup_camera_dialog_raise, (dialog, which) -> {
                                // Open the camera
                                CameraMotorController.setMotorDirection(CameraMotorController.DIRECTION_UP);
                                CameraMotorController.setMotorEnabled();

                                mFallSensor.enable();
                            })
                            .create();
                    mAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
                    mAlertDialog.setCanceledOnTouchOutside(false);
                }
                mAlertDialog.show();
            } else {
                // Open the camera
                CameraMotorController.setMotorDirection(CameraMotorController.DIRECTION_UP);
                CameraMotorController.setMotorEnabled();

                mFallSensor.enable();
            }
        } else if (cameraState.equals(closeCameraState) && !mMotorDown) {
            if (mAlertDialog != null && mAlertDialog.isShowing()) {
                mAlertDialog.dismiss();
            }

            // Close the camera
            CameraMotorController.setMotorDirection(CameraMotorController.DIRECTION_DOWN);
            CameraMotorController.setMotorEnabled();

            mFallSensor.disable();
        }
    }

    private void updateDialogTheme() {
        int nightModeFlags = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        int themeResId;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES)
            themeResId = android.R.style.Theme_DeviceDefault_Dialog_Alert;
        else
            themeResId = android.R.style.Theme_DeviceDefault_Light_Dialog_Alert;
        if (mDialogThemeResID != themeResId) {
            mDialogThemeResID = themeResId;
            // if the theme changed force re-creating the dialog
            mAlertDialog = null;
        }
    }
}
