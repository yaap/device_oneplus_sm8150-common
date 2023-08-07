/**
 * Copyright (C) 2022 FlamingoOS Project
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

package com.derp.device.DeviceSettings;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.FileObserver;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.util.rising.systemUtils;
import org.derpfest.device.DeviceSettings.FileUtils;

import java.io.File;

import vendor.oneplus.hardware.camera.V1_0.IOnePlusCameraProvider;

public class ClientPackageObserverService extends Service {
    private boolean isOpCameraInstalledAndActive = false;
    private boolean clientObserverRegistered = false;
    private boolean receiverRegistered = false;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_OFF:
                    unregisterClientObserver();
                    break;
                case Intent.ACTION_SCREEN_ON:
                    registerClientObserver();
                    break;
            }
        }
    };

    private FileObserver fileObserver = new FileObserver(new File(CLIENT_PACKAGE_PATH)) {
        @Override
        public void onEvent(int event, String file) {
            setPackageName(file);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        logD("onCreate");
        isOpCameraInstalledAndActive = systemUtils.isPackageInstalled(this,
                CLIENT_PACKAGE_NAME, false /** ignore state */);
        logD("isOpCameraInstalledAndActive = " + isOpCameraInstalledAndActive);
        if (isOpCameraInstalledAndActive) {
            setPackageName(CLIENT_PACKAGE_PATH);
        } else {
            stopSelf();
            return;
        }
        registerReceiver(broadcastReceiver, new IntentFilter() {{
            addAction(Intent.ACTION_SCREEN_OFF);
            addAction(Intent.ACTION_SCREEN_ON);
        }});
        registerClientObserver();
        receiverRegistered = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (receiverRegistered) {
            unregisterReceiver(broadcastReceiver);
        }
        unregisterClientObserver();
    }

    private void registerClientObserver() {
        isOpCameraInstalledAndActive = systemUtils.isPackageInstalled(this,
                CLIENT_PACKAGE_NAME, false /** ignore state */);
        if (isOpCameraInstalledAndActive && !clientObserverRegistered) {
            logD("registering client observer");
            fileObserver.startWatching();
            clientObserverRegistered = true;
        }
    }

    private void unregisterClientObserver() {
        if (clientObserverRegistered) {
            logD("unregistering client observer");
            fileObserver.stopWatching();
            clientObserverRegistered = false;
        }
    }

    private void setPackageName(String file) {
        String pkgName = FileUtils.readOneLine(file);
        if (pkgName == null) {
            pkgName = CLIENT_PACKAGE_NAME;
        }
        try {
            logD("client_package " + file + " and pkg = " + pkgName);
            IOnePlusCameraProvider.getService().setPackageName(pkgName);
        } catch (RemoteException e) {
            Log.e(TAG, "Error communicating with IOnePlusCameraProvider");
        }
    }

    private void logD(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }

    private static final String CLIENT_PACKAGE_NAME = "com.oneplus.camera";
    private static final String CLIENT_PACKAGE_PATH = "/data/misc/aosp/client_package_name";

    private static final String TAG = "ClientPackageObserverService";
    private static final boolean DEBUG = false;
}