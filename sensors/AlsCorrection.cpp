/*
 * Copyright (C) 2021 The LineageOS Project
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

#include "AlsCorrection.h"

#include <cutils/properties.h>
#include <fstream>
#include <cmath>
#include <log/log.h>
#include <time.h>

namespace android {
namespace hardware {
namespace sensors {
namespace V2_1 {
namespace implementation {

static int red_max_lux, green_max_lux, blue_max_lux, white_max_lux, max_brightness, cali_coe;
static int als_bias, max_lux;
static bool als_change, first_run;
static float als_change_light_diff, als_change_light_diff_local, prev_color_adj_light, als_change_frac, als_change_light, prev_raw_light;
static float als_color_change_light_offset, max_color_adj_light, min_color_adj_light, prev_brightness, prev_color_correction, als_color_change_light, als_color_change_light_offset_local;

template <typename T>
static T get(const std::string& path, const T& def) {
    std::ifstream file(path);
    T result;

    file >> result;
    return file.fail() ? def : result;
}

void AlsCorrection::init() {
    red_max_lux = get("/mnt/vendor/persist/engineermode/red_max_lux", 0);
    green_max_lux = get("/mnt/vendor/persist/engineermode/green_max_lux", 0);
    blue_max_lux = get("/mnt/vendor/persist/engineermode/blue_max_lux", 0);
    white_max_lux = get("/mnt/vendor/persist/engineermode/white_max_lux", 0);
    als_bias = get("/mnt/vendor/persist/engineermode/als_bias", 0);
    cali_coe = get("/mnt/vendor/persist/engineermode/cali_coe", 1000);
    max_brightness = get("/sys/class/backlight/panel0-backlight/max_brightness", 255);
    max_lux = red_max_lux + green_max_lux + blue_max_lux + white_max_lux;
    ALOGD("max r = %d, max g = %d, max b = %d, max_white: %d, cali_coe: %d, als_bias: %d, max_brightness: %d, max_lux: %d", red_max_lux, green_max_lux, blue_max_lux, white_max_lux, cali_coe, als_bias, max_brightness, max_lux);
    als_change_light_diff = 0.0f;
    als_change_light_diff_local = 0.0f;
    prev_color_adj_light = 0.0f;
    prev_raw_light = 0.0f;
    prev_brightness = 0.0f;
    als_change_frac = 0.1f;
    als_change_light = 0.0f;
    prev_color_correction = 0.0f;
    als_color_change_light_offset = 0.0f;
    als_color_change_light_offset_local = 0.0f;
    als_color_change_light = 0.0f;
    max_color_adj_light = 0.0f;
    min_color_adj_light = 0.0f;
    als_change = false;
    first_run = true;
}

void AlsCorrection::correct(float& light) {
    pid_t pid = property_get_int32("vendor.sensors.als_correction.pid", 0);
    if (pid != 0) {
        kill(pid, SIGUSR1);
    }
    uint8_t updated = property_get_int32("vendor.sensors.als_correction.updated", 0);
    if (updated == 0) {
        light = (prev_color_adj_light - als_change_light_diff_local);
        return;
    }
    uint8_t r = property_get_int32("vendor.sensors.als_correction.r", 0);
    uint8_t g = property_get_int32("vendor.sensors.als_correction.g", 0);
    uint8_t b = property_get_int32("vendor.sensors.als_correction.b", 0);
    ALOGV("Screen Color Above Sensor: %d, %d, %d", r, g, b);
    ALOGV("Original reading: %f", light);
    int screen_brightness = get("/sys/class/backlight/panel0-backlight/brightness", 0);
    if (screen_brightness == 0){
        ALOGD("Screen is off");
        light = (prev_color_adj_light - als_change_light_diff_local);
        return;
    }
    float color_correction = 0.0f;
    float brightness_factor = 0.0f;
    float brightness = 0.0f;
    uint32_t rgb_min = 0;
    float light_frac = 0.0f;
    //float coe_frac = ((float) cali_coe) / 1000.0;
    float change_threshold = 0.0f;
    float change = 0.0f;
    if (max_lux > 0) {
        brightness = ((float) screen_brightness) / ((float) max_brightness);
        max_lux = 3000;
        light_frac = (light > max_lux) ? 1.0f : light/((float) max_lux);
        rgb_min = std::min({r, g, b});
        color_correction += ((float) rgb_min) / 255.0f * ((float) white_max_lux);
        color_correction += ((float) r) / 255.0f * ((float) red_max_lux);
        color_correction += ((float) g) / 255.0f * ((float) green_max_lux);
        color_correction += ((float) b) / 255.0f * ((float) blue_max_lux);
        color_correction += als_bias;
        color_correction = color_correction/(white_max_lux + red_max_lux + green_max_lux + blue_max_lux);
        light_frac = light_frac * std::pow(color_correction, 0.8f);
        float exp = 1.2f - std::pow(color_correction*brightness, 0.4f);
        brightness_factor = std::pow(brightness, exp);
        brightness_factor = (brightness_factor > 1.0f) ? 1.0f : brightness_factor;
        light_frac = light_frac * brightness_factor;
        light_frac = (light > max_lux) ? light_frac - (light - max_lux)/((float) max_lux) : light_frac;
        light_frac = (light_frac > 0.5f) ? 0.5f : light_frac < 0.0f ? 0.0f : light_frac;
        float color_adj_light = light * (1.0f - light_frac);
        if (als_change) {
            max_color_adj_light = std::max({max_color_adj_light, color_adj_light});
            min_color_adj_light = std::min({min_color_adj_light, color_adj_light});
            if (abs(color_correction - prev_color_correction) > 0.01f/std::pow(brightness, 0.45f)) {
                float tmp = color_correction > prev_color_correction ? color_adj_light - min_color_adj_light
                            : color_adj_light - max_color_adj_light;
                als_color_change_light_offset += tmp;
                tmp = (color_adj_light > als_change_light) ? als_change_light_diff : als_change_light_diff
                            * std::pow(color_adj_light/als_change_light, 2);
                // Compensate for the change in als_change_light_diff_local due to change in color_adj_light value
                // that occurs due to screen color change
                als_color_change_light_offset += als_change_light_diff_local - als_color_change_light_offset_local - tmp;
                als_color_change_light = color_adj_light;
                max_color_adj_light = 0.0f;
                min_color_adj_light = color_adj_light;
            }
        } else {
            als_color_change_light_offset = 0.0f;
        }
        als_color_change_light_offset_local = (color_adj_light > als_color_change_light) ? als_color_change_light_offset
                    : als_color_change_light_offset * std::pow(color_adj_light/als_color_change_light, 2);

        float tmp = als_change ? light : prev_raw_light;
        light_frac = (tmp > max_lux) ? 1.0f : tmp/((float) max_lux);
        change_threshold = light_frac == 0 ? 1.0f : (0.1f/std::pow(light_frac, 0.5f)) * (1/(3 - 2*color_correction));
        change = (light == 0 || prev_raw_light == 0) ? 0.1f : als_change ? (prev_raw_light - light)/light
                : (light - prev_raw_light)/prev_raw_light;
        if (!first_run && light > prev_raw_light && abs(change) > change_threshold) {
            if (!als_change && brightness > prev_brightness && change > 0) {
                als_change_light = color_adj_light;
                als_change_light_diff = color_adj_light - prev_color_adj_light;
                als_change_frac = brightness;
                als_change = true;
            }
            max_color_adj_light = min_color_adj_light;
            min_color_adj_light = color_adj_light;
        } else if (als_change && (light < 60 || brightness < 0.07f || (brightness < als_change_frac && light < prev_raw_light
                    && change > change_threshold && brightness < prev_brightness))) {
            als_change = false;
            als_change_light_diff = 0.0f;
            als_change_light = 0.0f;
            als_color_change_light_offset_local = 0.0f;
            max_color_adj_light = 0.0f;
            min_color_adj_light = 0.0f;
        }
        first_run = false;
        als_change_light_diff_local = (color_adj_light > als_change_light) ? als_change_light_diff : als_change_light_diff
                    * std::pow(color_adj_light/als_change_light, 2);
        als_change_light_diff_local += als_color_change_light_offset_local;
        als_change_light_diff_local = abs(als_change_light_diff_local) > color_adj_light*0.95f ? (als_change_light_diff_local < 0 ? -color_adj_light*0.95f
                    : color_adj_light*0.95f) : als_change_light_diff_local > 2000 ? 2000 : als_change_light_diff_local;
        als_change_light_diff_local = (als_change_light_diff_local < 0) ? std::max({-0.1f*color_adj_light, als_change_light_diff_local})
                    : als_change_light_diff_local;
        if (color_adj_light - als_change_light_diff_local > light) {
            als_change_light_diff_local = 0;
        }
        prev_color_adj_light = color_adj_light;
        prev_raw_light = light;
        prev_brightness = brightness;
        prev_color_correction = color_correction;
    }

    ALOGD("Raw: %f Color: %f Corrected: %f correction: %f brightness: %d als_change_light: %f als_color_change_light: %f change: %f change_threshold: %f als_: %d", light, prev_color_adj_light, (prev_color_adj_light - als_change_light_diff_local), color_correction, screen_brightness, als_change_light_diff, als_color_change_light_offset, change, change_threshold, als_change ? 1:0);
    light = (prev_color_adj_light - als_change_light_diff_local);
}

}  // namespace implementation
}  // namespace V2_1
}  // namespace sensors
}  // namespace hardware
}  // namespace android
