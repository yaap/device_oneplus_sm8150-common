/*
 * Copyright (C) 2019 The LineageOS Project
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

import android.annotation.NonNull;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

public class CameraMotorService extends Service implements Handler.Callback {
    private static final boolean DEBUG = true;
    private static final String TAG = "CameraMotorService";

    // Should follow KEY_SETTINGS_PREFIX + KEY_ALWAYS_CAMERA_DIALOG From DeviceSettings
    private static final String ALWAYS_ON_DIALOG_KEY = "device_setting_always_on_camera_dialog";

    public static final int CAMERA_EVENT_DELAY_TIME = 100; // ms

    public static final String FRONT_CAMERA_ID = "1";

    public static final int MSG_CAMERA_CLOSED = 1000;
    public static final int MSG_CAMERA_OPEN = 1001;

    private final Handler mHandler = new Handler(this);

    private AlertDialog mAlertDialog;
    private DisplayManager mDisplayManager;

    private long mClosedEvent;
    private long mOpenEvent;

    private final CameraManager.AvailabilityCallback mAvailabilityCallback =
            new CameraManager.AvailabilityCallback() {
                @Override
                public void onCameraClosed(@NonNull String cameraId) {
                    super.onCameraClosed(cameraId);

                    if (cameraId.equals(FRONT_CAMERA_ID)) {
                        mClosedEvent = SystemClock.elapsedRealtime();
                        if (SystemClock.elapsedRealtime() - mOpenEvent < CAMERA_EVENT_DELAY_TIME
                                && mHandler.hasMessages(MSG_CAMERA_OPEN)) {
                            mHandler.removeMessages(MSG_CAMERA_OPEN);
                        }
                        mHandler.sendEmptyMessageDelayed(MSG_CAMERA_CLOSED,
                                CAMERA_EVENT_DELAY_TIME);
                    }
                }

                @Override
                public void onCameraOpened(@NonNull String cameraId, @NonNull String packageId) {
                    super.onCameraClosed(cameraId);

                    if (cameraId.equals(FRONT_CAMERA_ID)) {
                        mOpenEvent = SystemClock.elapsedRealtime();
                        if (SystemClock.elapsedRealtime() - mClosedEvent < CAMERA_EVENT_DELAY_TIME
                                && mHandler.hasMessages(MSG_CAMERA_CLOSED)) {
                            mHandler.removeMessages(MSG_CAMERA_CLOSED);
                        }
                        mHandler.sendEmptyMessageDelayed(MSG_CAMERA_OPEN,
                                CAMERA_EVENT_DELAY_TIME);
                    }
                }
            };

    @Override
    public void onCreate() {
        CameraMotorController.calibrate();

        CameraManager cameraManager = getSystemService(CameraManager.class);
        cameraManager.registerAvailabilityCallback(mAvailabilityCallback, null);

        mDisplayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "Starting service");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_CAMERA_CLOSED:
                lowerCamera();
                break;
            case MSG_CAMERA_OPEN:
                maybeRaiseCamera();
                break;
        }
        return true;
    }

    private void maybeRaiseCamera() {
        boolean screenOn = false;
        for (Display display : mDisplayManager.getDisplays()) {
            if (display.getState() != Display.STATE_OFF) {
                screenOn = true;
                break;
            }
        }
        boolean alwaysOnDialog = Settings.System.getInt(getContentResolver(),
                ALWAYS_ON_DIALOG_KEY, 0) == 1;
        if (screenOn && !alwaysOnDialog) {
            raiseCamera();
        } else {
            if (mAlertDialog == null) {
                mAlertDialog = new AlertDialog.Builder(this)
                        .setMessage(R.string.popup_camera_dialog_message)
                        .setNegativeButton(R.string.popup_camera_dialog_no,
                                (dialog, which) -> {
                            // Go back to home screen
                            Intent intent = new Intent(Intent.ACTION_MAIN);
                            intent.addCategory(Intent.CATEGORY_HOME);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            this.startActivity(intent);
                        })
                        .setPositiveButton(R.string.popup_camera_dialog_raise,
                                (dialog, which) -> raiseCamera())
                        .create();
                mAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
                mAlertDialog.setCanceledOnTouchOutside(false);
            }
            if (!mAlertDialog.isShowing()) mAlertDialog.show();
        }
    }

    private void raiseCamera() {
        if (DEBUG) Log.d(TAG, "Raising camera");
        CameraMotorController.setMotorDirection(CameraMotorController.DIRECTION_UP);
        CameraMotorController.setMotorEnabled();
    }

    private void lowerCamera() {
        if (DEBUG) Log.d(TAG, "Lowering camera");
        if (mAlertDialog != null && mAlertDialog.isShowing()) mAlertDialog.dismiss();
        CameraMotorController.setMotorDirection(CameraMotorController.DIRECTION_DOWN);
        CameraMotorController.setMotorEnabled();
    }
}
