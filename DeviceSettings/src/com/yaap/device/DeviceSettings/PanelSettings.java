/*
* Copyright (C) 2018 The OmniROM Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package com.yaap.device.DeviceSettings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.android.internal.util.yaap.FileUtils;

public class PanelSettings extends PreferenceFragment implements RadioGroup.OnCheckedChangeListener {
    ViewPager viewPager;
    LinearLayout sliderDotspanel;
    private int dotscount;
    private ImageView[] dots;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RadioGroup mRadioGroup = (RadioGroup) view.findViewById(R.id.radio_group);
        int checkedButtonId = R.id.off_mode;
        if (NaturalModeSwitch.isCurrentlyEnabled(getContext())) {
            checkedButtonId = R.id.natural_mode;
        } else if (VividModeSwitch.isCurrentlyEnabled(getContext())) {
            checkedButtonId = R.id.vivid_mode;
        } else if (DCIModeSwitch.isCurrentlyEnabled(getContext())) {
            checkedButtonId = R.id.dci_mode;
        } else if (SRGBModeSwitch.isCurrentlyEnabled(getContext())) {
            checkedButtonId = R.id.srgb_mode;
        } else if (WideColorModeSwitch.isCurrentlyEnabled(getContext())) {
            checkedButtonId = R.id.wide_color_mode;
        }
        mRadioGroup.check(checkedButtonId);
        mRadioGroup.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
	final View rootView = inflater.inflate(R.layout.panel_modes, container, false);

	viewPager = (ViewPager) rootView.findViewById(R.id.viewPager);
        sliderDotspanel = (LinearLayout) rootView.findViewById(R.id.SliderDots);
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getActivity());
        viewPager.setAdapter(viewPagerAdapter);

        dotscount = viewPagerAdapter.getCount();
        dots = new ImageView[dotscount];

        for(int i = 0; i < dotscount; i++){
            dots[i] = new ImageView(getActivity());
            dots[i].setImageDrawable(ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.inactive_dot));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(8, 0, 8, 0);
            sliderDotspanel.addView(dots[i], params);
        }
        dots[0].setImageDrawable(ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.active_dot));

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {

                for(int i = 0; i< dotscount; i++){
                    dots[i].setImageDrawable(ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.inactive_dot));
                }
                dots[position].setImageDrawable(ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.active_dot));
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
	return rootView;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor edit = sharedPrefs.edit();
        if (checkedId == R.id.srgb_mode) {
            Utils.writeValue(NaturalModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_NATURAL_SWITCH, false);
            Utils.writeValue(VividModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_VIVID_SWITCH, false);
            Utils.writeValue(DCIModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_DCI_SWITCH, false);
            Utils.writeValue(WideColorModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_WIDECOLOR_SWITCH, false);
            Utils.writeValue(SRGBModeSwitch.getFile(), "1");
            edit.putBoolean(DeviceSettings.KEY_SRGB_SWITCH, true);
        } else if (checkedId == R.id.dci_mode) {
            Utils.writeValue(NaturalModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_NATURAL_SWITCH, false);
            Utils.writeValue(VividModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_VIVID_SWITCH, false);
            Utils.writeValue(SRGBModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_SRGB_SWITCH, false);
            Utils.writeValue(WideColorModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_WIDECOLOR_SWITCH, false);
            Utils.writeValue(DCIModeSwitch.getFile(), "1");
            edit.putBoolean(DeviceSettings.KEY_DCI_SWITCH, true);
        } else if (checkedId == R.id.natural_mode) {
            Utils.writeValue(VividModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_VIVID_SWITCH, false);
            Utils.writeValue(SRGBModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_SRGB_SWITCH, false);
            Utils.writeValue(WideColorModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_WIDECOLOR_SWITCH, false);
            Utils.writeValue(DCIModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_DCI_SWITCH, false);
            Utils.writeValue(NaturalModeSwitch.getFile(), "1");
            edit.putBoolean(DeviceSettings.KEY_NATURAL_SWITCH, true);
        } else if (checkedId == R.id.vivid_mode) {
            Utils.writeValue(NaturalModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_NATURAL_SWITCH, false);
            Utils.writeValue(SRGBModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_SRGB_SWITCH, false);
            Utils.writeValue(WideColorModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_WIDECOLOR_SWITCH, false);
            Utils.writeValue(DCIModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_DCI_SWITCH, false);
            Utils.writeValue(VividModeSwitch.getFile(), "1");
            edit.putBoolean(DeviceSettings.KEY_VIVID_SWITCH, true);
        } else if (checkedId == R.id.wide_color_mode) {
            Utils.writeValue(NaturalModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_NATURAL_SWITCH, false);
            Utils.writeValue(VividModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_VIVID_SWITCH, false);
            Utils.writeValue(DCIModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_DCI_SWITCH, false);
            Utils.writeValue(SRGBModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_SRGB_SWITCH, false);
            Utils.writeValue(WideColorModeSwitch.getFile(), "1");
            edit.putBoolean(DeviceSettings.KEY_WIDECOLOR_SWITCH, true);
        } else if (checkedId == R.id.off_mode) {
            Utils.writeValue(NaturalModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_NATURAL_SWITCH, false);
            Utils.writeValue(VividModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_VIVID_SWITCH, false);
            Utils.writeValue(DCIModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_DCI_SWITCH, false);
            Utils.writeValue(SRGBModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_SRGB_SWITCH, false);
            Utils.writeValue(WideColorModeSwitch.getFile(), "0");
            edit.putBoolean(DeviceSettings.KEY_WIDECOLOR_SWITCH, false);
        }
        edit.apply();
    }
}
