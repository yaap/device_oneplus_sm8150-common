/*
* Copyright (C) 2021 Yet Another AOSP Project
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
import android.hardware.display.ColorDisplayManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.yaap.device.DeviceSettings.ModeSwitch.*;

import java.util.Locale;

public class PanelSettings extends PreferenceFragment implements RadioGroup.OnCheckedChangeListener,
        SeekBar.OnSeekBarChangeListener {
    private static final int COLOR_CHANNEL_RED = 0;
    private static final int COLOR_CHANNEL_GREEN = 1;
    private static final int COLOR_CHANNEL_BLUE = 2;

    ViewPager viewPager;
    LinearLayout sliderDotspanel;
    private int dotscount;
    private ImageView[] dots;
    private ColorDisplayManager mColorDisplayManager;

    private SeekBar mRedPref;
    private SeekBar mGreenPref;
    private SeekBar mBluePref;
    private TextView mRedText;
    private TextView mGreenText;
    private TextView mBlueText;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RadioGroup mRadioGroup = view.findViewById(R.id.radio_group);
        int checkedButtonId = R.id.off_mode;
        if (NaturalModeSwitch.isCurrentlyEnabled()) {
            checkedButtonId = R.id.natural_mode;
        } else if (VividModeSwitch.isCurrentlyEnabled()) {
            checkedButtonId = R.id.vivid_mode;
        } else if (DCIModeSwitch.isCurrentlyEnabled()) {
            checkedButtonId = R.id.dci_mode;
        } else if (SRGBModeSwitch.isCurrentlyEnabled()) {
            checkedButtonId = R.id.srgb_mode;
        } else if (WideColorModeSwitch.isCurrentlyEnabled()) {
            checkedButtonId = R.id.wide_color_mode;
        }
        mRadioGroup.check(checkedButtonId);
        mRadioGroup.setOnCheckedChangeListener(this);

        if (ColorDisplayManager.isColorTransformAccelerated(getContext())) {
            mColorDisplayManager = getContext().getSystemService(ColorDisplayManager.class);
            mRedPref = view.findViewById(R.id.color_balance_red);
            mRedPref.setOnSeekBarChangeListener(this);
            mGreenPref = view.findViewById(R.id.color_balance_green);
            mGreenPref.setOnSeekBarChangeListener(this);
            mBluePref = view.findViewById(R.id.color_balance_blue);
            mBluePref.setOnSeekBarChangeListener(this);

            mRedText = view.findViewById(R.id.color_balance_red_percent);
            mGreenText = view.findViewById(R.id.color_balance_green_percent);
            mBlueText = view.findViewById(R.id.color_balance_blue_percent);

            mRedPref.setProgress(mColorDisplayManager.getColorBalanceChannel(COLOR_CHANNEL_RED));
            mGreenPref.setProgress(mColorDisplayManager.getColorBalanceChannel(COLOR_CHANNEL_GREEN));
            mBluePref.setProgress(mColorDisplayManager.getColorBalanceChannel(COLOR_CHANNEL_BLUE));
        } else {
            LinearLayout slidersCategory = view.findViewById(R.id.rgb_category);
            slidersCategory.setVisibility(View.GONE);
            slidersCategory.setEnabled(false);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        String percent = String.format(Locale.US, "%d%%",
                Math.round((progress - 25) / 2.3));
        if (seekBar == mRedPref) {
            mColorDisplayManager.setColorBalanceChannel(COLOR_CHANNEL_RED, progress);
            mRedText.setText(percent);
        } else if (seekBar == mGreenPref) {
            mColorDisplayManager.setColorBalanceChannel(COLOR_CHANNEL_GREEN, progress);
            mGreenText.setText(percent);
        } else if (seekBar == mBluePref) {
            mColorDisplayManager.setColorBalanceChannel(COLOR_CHANNEL_BLUE, progress);
            mBlueText.setText(percent);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) { }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.panel_modes, container, false);

        viewPager = rootView.findViewById(R.id.viewPager);
        sliderDotspanel = rootView.findViewById(R.id.SliderDots);
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getActivity());
        viewPager.setAdapter(viewPagerAdapter);

        dotscount = viewPagerAdapter.getCount();
        dots = new ImageView[dotscount];

        for (int i = 0; i < dotscount; i++) {
            dots[i] = new ImageView(getActivity());
            dots[i].setImageDrawable(ContextCompat.getDrawable(getActivity().getApplicationContext(),
                    R.drawable.inactive_dot));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(8, 0, 8, 0);
            sliderDotspanel.addView(dots[i], params);
        }
        dots[0].setImageDrawable(ContextCompat.getDrawable(getActivity().getApplicationContext(),
                R.drawable.active_dot));

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {

                for(int i = 0; i< dotscount; i++){
                    dots[i].setImageDrawable(ContextCompat.getDrawable(
                            getActivity().getApplicationContext(), R.drawable.inactive_dot));
                }
                dots[position].setImageDrawable(ContextCompat.getDrawable(
                        getActivity().getApplicationContext(), R.drawable.active_dot));
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

        boolean natural = checkedId == R.id.natural_mode;
        Utils.writeValue(NaturalModeSwitch.getFile(), natural ? "1" : "0");
        edit.putBoolean(DeviceSettings.KEY_NATURAL_SWITCH, natural);

        boolean vivid = checkedId == R.id.vivid_mode;
        Utils.writeValue(VividModeSwitch.getFile(), vivid ? "1" : "0");
        edit.putBoolean(DeviceSettings.KEY_VIVID_SWITCH, vivid);

        boolean dci = checkedId == R.id.dci_mode;
        Utils.writeValue(DCIModeSwitch.getFile(), dci ? "1" : "0");
        edit.putBoolean(DeviceSettings.KEY_DCI_SWITCH, dci);

        boolean wide = checkedId == R.id.wide_color_mode;
        Utils.writeValue(WideColorModeSwitch.getFile(), wide ? "1" : "0");
        edit.putBoolean(DeviceSettings.KEY_WIDECOLOR_SWITCH, wide);

        boolean srgb = checkedId == R.id.srgb_mode;
        Utils.writeValue(SRGBModeSwitch.getFile(), srgb ? "1" : "0");
        edit.putBoolean(DeviceSettings.KEY_SRGB_SWITCH, srgb);

        edit.apply();
    }


}
