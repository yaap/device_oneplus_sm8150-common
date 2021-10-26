/*
   Copyright (c) 2020, The LineageOS Project

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions are
   met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

   THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
   WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
   ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
   BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
   CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
   SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
   BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
   WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
   OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
   IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <android-base/properties.h>
#define _REALLY_INCLUDE_SYS__SYSTEM_PROPERTIES_H_
#include <stdio.h>
#include <stdlib.h>
#include <sys/sysinfo.h>
#include <sys/system_properties.h>
#include <sys/_system_properties.h>

#include "property_service.h"
#include "vendor_init.h"

using android::base::GetProperty;

void property_override(char const prop[], char const value[]) {
  prop_info *pi;

  pi = (prop_info *)__system_property_find(prop);
  if (pi)
    __system_property_update(pi, value, strlen(value));
  else
    __system_property_add(prop, strlen(prop), value, strlen(value));
}

void property_override_dual(char const system_prop[], char const vendor_prop[], char const value[]) {
    property_override(system_prop, value);
    property_override(vendor_prop, value);
}

void property_override_multi(char const system_prop[], char const vendor_prop[],char const bootimage_prop[], char const value[]) {
    property_override(system_prop, value);
    property_override(vendor_prop, value);
    property_override(bootimage_prop, value);
}

void load_dalvikvm_properties() {
  struct sysinfo sys;
  sysinfo(&sys);
  if (sys.totalram > 8192ull * 1024 * 1024) {
    // from - phone-xhdpi-12288-dalvik-heap.mk
    property_override("dalvik.vm.heapstartsize", "24m");
    property_override("dalvik.vm.heapgrowthlimit", "384m");
    property_override("dalvik.vm.heaptargetutilization", "0.42");
    property_override("dalvik.vm.heapmaxfree", "56m");
    }
  else if(sys.totalram > 6144ull * 1024 * 1024) {
    // from - phone-xhdpi-8192-dalvik-heap.mk
    property_override("dalvik.vm.heapstartsize", "24m");
    property_override("dalvik.vm.heapgrowthlimit", "256m");
    property_override("dalvik.vm.heaptargetutilization", "0.46");
    property_override("dalvik.vm.heapmaxfree", "48m");
    }
  else {
    // from - phone-xhdpi-6144-dalvik-heap.mk
    property_override("dalvik.vm.heapstartsize", "16m");
    property_override("dalvik.vm.heapgrowthlimit", "256m");
    property_override("dalvik.vm.heaptargetutilization", "0.5");
    property_override("dalvik.vm.heapmaxfree", "32m");
  }
  property_override("dalvik.vm.heapsize", "512m");
  property_override("dalvik.vm.heapminfree", "8m");
}

void vendor_load_properties() {
  int prj_version = stoi(android::base::GetProperty("ro.boot.prj_version", ""));
  int project_name = stoi(android::base::GetProperty("ro.boot.project_name", ""));
  int rf_version = stoi(android::base::GetProperty("ro.boot.rf_version", ""));
  switch(project_name){
    case 18857:
      /* OnePlus 7 */
      switch (rf_version){
        case 1:
          /* China */
          property_override("ro.product.model", "GM1900");
          break;
        case 3:
          /* India*/
          property_override("ro.product.model", "GM1901");
          break;
        case 4:
          /* Europe */
          property_override("ro.product.model", "GM1903");
          break;
        case 5:
          /* Global / US Unlocked */
          property_override("ro.product.model", "GM1905");
          break;
      }
      break;
    case 18821:
      /* OnePlus 7 Pro */
      switch (rf_version){
        case 1:
          /* China */
          property_override("ro.product.model", "GM1910");
          break;
        case 3:
          /* India */
          property_override("ro.product.model", "GM1911");
          break;
        case 4:
          /* Europe */
          property_override("ro.product.model", "GM1913");
          break;
        case 5:
          /* Global / US Unlocked */
          property_override("ro.product.model", "GM1917");
          break;
      }
      break;
    case 18831:
      /* OnePlus 7 Pro T-Mobile */
      property_override("ro.product.model", "GM1915");
      break;
    case 18865:
      /* OnePlus 7T */
      switch (rf_version){
        case 1:
          /* China */
          property_override("ro.product.model", "HD1900");
          break;
        case 3:
          /* India */
          property_override("ro.product.model", "HD1901");
          break;
        case 4:
          /* Europe */
          property_override("ro.product.model", "HD1903");
          break;
        case 5:
          /* Global / US Unlocked */
          property_override("ro.product.model", "HD1905");
          break;
      }
      break;
    case 19863:
      /* OnePlus 7T T-Mobile */
      property_override("ro.product.model", "HD1907");
      break;
    case 19801:
      /* OnePlus 7T Pro */
      switch (rf_version){
        case 1:
          /* China */
          property_override("ro.product.model", "HD1910");
          break;
        case 3:
          /* India */
          property_override("ro.product.model", "HD1911");
          break;
        case 4:
          /* Europe */
          property_override("ro.product.model", "HD1913");
          break;
        case 5:
          /* Global / US Unlocked */
          property_override("ro.product.model", "HD1917");
          break;
      }
      break;
    case 19861:
      /* OnePlus 7T Pro NR */
      property_override("persist.radio.multisim.config", "ssss");
      property_override("vendor.product.device", "hotdogg");
      property_override("ro.product.model", "HD1925");
      break;
    }

    property_override("vendor.boot.prj_version", std::to_string(prj_version).c_str());
    property_override_dual("vendor.rf.version", "vendor.boot.rf_version", std::to_string(rf_version).c_str());

  // dalvikvm props
  load_dalvikvm_properties();
}
