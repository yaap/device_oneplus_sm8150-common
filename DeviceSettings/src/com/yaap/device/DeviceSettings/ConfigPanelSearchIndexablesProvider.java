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


import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.SearchIndexablesProvider;

import static android.provider.SearchIndexablesContract.INDEXABLES_RAW_COLUMNS;
import static android.provider.SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS;
import static android.provider.SearchIndexablesContract.NON_INDEXABLES_KEYS_COLUMNS;

public class ConfigPanelSearchIndexablesProvider extends SearchIndexablesProvider {

    public static final int SEARCH_IDX_GESTURE_PANEL = 1;
    public static final int SEARCH_IDX_OCLICK_PANEL = 2;

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor queryXmlResources(String[] projection) {
        return new MatrixCursor(INDEXABLES_XML_RES_COLUMNS);
    }

    @Override
    public Cursor queryRawData(String[] projection) {
        return new MatrixCursor(INDEXABLES_RAW_COLUMNS);
    }

    @Override
    public Cursor queryNonIndexableKeys(String[] projection) {
        return new MatrixCursor(NON_INDEXABLES_KEYS_COLUMNS);
    }
}
