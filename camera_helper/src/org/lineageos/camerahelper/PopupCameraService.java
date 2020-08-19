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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

public class PopupCameraService extends Service {
    private static final String TAG = "PopupCameraService";
    private static final boolean DEBUG = true;

    private static final String closeCameraState = "0";
    private static final String openCameraState = "1";

    private FallSensor mFallSensor;

    private boolean mMotorDown;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                if (DEBUG) Log.d(TAG, "Screen off, disabling fall sensor");
                mFallSensor.disable();
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

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
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
            // Open the camera
            CameraMotorController.setMotorDirection(CameraMotorController.DIRECTION_UP);
            CameraMotorController.setMotorEnabled();

            mFallSensor.enable();
        } else if (cameraState.equals(closeCameraState) && !mMotorDown) {
            // Close the camera
            CameraMotorController.setMotorDirection(CameraMotorController.DIRECTION_DOWN);
            CameraMotorController.setMotorEnabled();

            mFallSensor.disable();
        }
    }
}
