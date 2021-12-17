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

import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.yaap.device.DeviceSettings.ModeSwitch.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class PanelSettings extends PreferenceFragment implements RadioGroup.OnCheckedChangeListener,
        SeekBar.OnSeekBarChangeListener {
    private static final int COLOR_CHANNEL_RED = 0;
    private static final int COLOR_CHANNEL_GREEN = 1;
    private static final int COLOR_CHANNEL_BLUE = 2;
    private static final int DOT_INDICATOR_SIZE = 12;
    private static final int DOT_INDICATOR_LEFT_PADDING = 6;
    private static final int DOT_INDICATOR_RIGHT_PADDING = 6;
    private static final String PAGE_VIEWER_SELECTION_INDEX = "page_viewer_selection_index";

    private View mViewArrowPrevious;
    private View mViewArrowNext;
    private ViewPager mViewPager;
    private ColorDisplayManager mColorDisplayManager;

    private ArrayList<View> mPageList;
    private ImageView[] mDotIndicators;
    private View[] mViewPagerImages;

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

        if (savedInstanceState != null) {
            final int selectedPosition = savedInstanceState.getInt(PAGE_VIEWER_SELECTION_INDEX);
            mViewPager.setCurrentItem(selectedPosition);
            updateIndicator(selectedPosition);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putInt(PAGE_VIEWER_SELECTION_INDEX, mViewPager.getCurrentItem());
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
        addViewPager(rootView);
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

    private ArrayList<Integer> getViewPagerResource() {
        return new ArrayList<>(
                Arrays.asList(
                        R.layout.color_mode_view1,
                        R.layout.color_mode_view2,
                        R.layout.color_mode_view3));
    }

    private void addViewPager(View rootView) {
        final ArrayList<Integer> tmpviewPagerList = getViewPagerResource();
        mViewPager = rootView.findViewById(R.id.viewpager);

        mViewPagerImages = new View[3];
        for (int idx = 0; idx < tmpviewPagerList.size(); idx++) {
            mViewPagerImages[idx] =
                    getLayoutInflater().inflate(tmpviewPagerList.get(idx), null /* root */);
        }

        mPageList = new ArrayList<>();
        mPageList.add(mViewPagerImages[0]);
        mPageList.add(mViewPagerImages[1]);
        mPageList.add(mViewPagerImages[2]);

        mViewPager.setAdapter(new ColorPagerAdapter(mPageList));

        mViewArrowPrevious = rootView.findViewById(R.id.arrow_previous);
        mViewArrowPrevious.setOnClickListener(v -> {
            final int previousPos = mViewPager.getCurrentItem() - 1;
            mViewPager.setCurrentItem(previousPos, true);
        });

        mViewArrowNext = rootView.findViewById(R.id.arrow_next);
        mViewArrowNext.setOnClickListener(v -> {
            final int nextPos = mViewPager.getCurrentItem() + 1;
            mViewPager.setCurrentItem(nextPos, true);
        });

        mViewPager.addOnPageChangeListener(createPageListener());

        final ViewGroup viewGroup = rootView.findViewById(R.id.viewGroup);
        mDotIndicators = new ImageView[mPageList.size()];
        for (int i = 0; i < mPageList.size(); i++) {
            final ImageView imageView = new ImageView(getContext());
            final ViewGroup.MarginLayoutParams lp =
                    new ViewGroup.MarginLayoutParams(DOT_INDICATOR_SIZE, DOT_INDICATOR_SIZE);
            lp.setMargins(DOT_INDICATOR_LEFT_PADDING, 0, DOT_INDICATOR_RIGHT_PADDING, 0);
            imageView.setLayoutParams(lp);
            mDotIndicators[i] = imageView;

            viewGroup.addView(mDotIndicators[i]);
        }

        updateIndicator(mViewPager.getCurrentItem());
    }

    private ViewPager.OnPageChangeListener createPageListener() {
        return new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(
                    int position, float positionOffset, int positionOffsetPixels) {
                if (positionOffset != 0) {
                    for (int idx = 0; idx < mPageList.size(); idx++)
                        mViewPagerImages[idx].setVisibility(View.VISIBLE);
                } else {
                    updateIndicator(position);
                }
            }

            @Override
            public void onPageSelected(int position) {}

            @Override
            public void onPageScrollStateChanged(int state) {}
        };
    }

    private void updateIndicator(int position) {
        for (int i = 0; i < mPageList.size(); i++) {
            if (position == i) {
                mDotIndicators[i].setBackgroundResource(
                        R.drawable.ic_color_page_indicator_focused);

                mViewPagerImages[i].setVisibility(View.VISIBLE);
            } else {
                mDotIndicators[i].setBackgroundResource(
                        R.drawable.ic_color_page_indicator_unfocused);

                mViewPagerImages[i].setVisibility(View.INVISIBLE);
            }
        }

        if (position == 0) {
            mViewArrowPrevious.setVisibility(View.INVISIBLE);
            mViewArrowNext.setVisibility(View.VISIBLE);
        } else if (position == (mPageList.size() - 1)) {
            mViewArrowPrevious.setVisibility(View.VISIBLE);
            mViewArrowNext.setVisibility(View.INVISIBLE);
        } else {
            mViewArrowPrevious.setVisibility(View.VISIBLE);
            mViewArrowNext.setVisibility(View.VISIBLE);
        }
    }

    private static class ColorPagerAdapter extends PagerAdapter {
        private final ArrayList<View> mPageViewList;

        ColorPagerAdapter(ArrayList<View> pageViewList) {
            mPageViewList = pageViewList;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (mPageViewList.get(position) != null) {
                container.removeView(mPageViewList.get(position));
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(mPageViewList.get(position));
            return mPageViewList.get(position);
        }

        @Override
        public int getCount() {
            return mPageViewList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return object == view;
        }
    }
}
