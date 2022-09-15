/*
 *
 *  Copyright (c) 2013, The Linux Foundation. All rights reserved.
 *  Not a Contribution, Apache license notifications and license are retained
 *  for attribution purposes only.
 *
 * Copyright (C) 2012 The Android Open Source Project
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

#ifndef _BDROID_BUILDCFG_H
#define _BDROID_BUILDCFG_H

#include <stdint.h>
#include <string.h>

#ifdef __cplusplus
extern "C" {
#endif
int property_get(const char *key, char *value, const char *default_value);
#ifdef __cplusplus
}
#endif

#include "osi/include/osi.h"

typedef struct {
    const char *project_name;
    const char *product_model;
} device_t;

static const device_t devices[] = {
    {"18857", "OnePlus 7"},
    {"18821", "OnePlus 7 Pro"},
    {"18831", "OnePlus 7 Pro TMO"},
    {"18865", "OnePlus 7T"},
    {"19863", "OnePlus 7T TMO"},
    {"19801", "OnePlus 7T Pro"},
    {"19861", "OnePlus 7T Pro NR"},
};

static inline const char *BtmGetDefaultName()
{
    char project_name[92];
    property_get("ro.boot.project_name", project_name, "");

    for (unsigned int i = 0; i < ARRAY_SIZE(devices); i++) {
        device_t device = devices[i];

        if (strcmp(device.project_name, project_name) == 0) {
            return device.product_model;
        }
    }

    // Fallback to ro.product.model
    return "";
}

#define BTM_DEF_LOCAL_NAME BtmGetDefaultName()
// Disables read remote device feature
#define MAX_ACL_CONNECTIONS   16
#define MAX_L2CAP_CHANNELS    32
#define BLE_VND_INCLUDED   TRUE
#define GATT_MAX_PHY_CHANNEL  10

#define AVDT_NUM_SEPS 35

#endif
