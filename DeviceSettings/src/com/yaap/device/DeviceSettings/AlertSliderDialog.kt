/*
 * Copyright (C) 2019 CypherOS
 * Copyright (C) 2014-2020 Paranoid Android
 * Copyright (C) 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.yaap.device.DeviceSettings

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import com.yaap.device.DeviceSettings.R

/**
 * View with some logging to show that its being run.
 */
class AlertSliderDialog(context: Context) : Dialog(context, R.style.alert_slider_theme) {
    private val dialogView by lazy { findViewById<LinearLayout>(R.id.alert_slider_dialog) }
    private val frameView by lazy { findViewById<ViewGroup>(R.id.alert_slider_view) }
    private val iconView by lazy { findViewById<ImageView>(R.id.alert_slider_icon) }
    private val textView by lazy { findViewById<TextView>(R.id.alert_slider_text) }

    init {
        window!!.requestFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                    or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        window.addPrivateFlags(WindowManager.LayoutParams.PRIVATE_FLAG_TRUSTED_OVERLAY)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        window.setType(WindowManager.LayoutParams.TYPE_VOLUME_OVERLAY)
        window.attributes = window.attributes.apply {
            format = PixelFormat.TRANSLUCENT
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            title = TAG
        }

        setCanceledOnTouchOutside(false)
        setContentView(R.layout.alert_slider_dialog)
    }

    fun setState(position: Int, ringerMode: Int) {
        window!!.attributes = window.attributes.apply {
            gravity = Gravity.TOP or Gravity.RIGHT

            val f = context.resources.getFraction(R.fraction.alert_slider_dialog_y, 1, 1)
            val h = context.resources.getDimension(R.dimen.alert_slider_dialog_height).toInt()
            val hv = h + dialogView.paddingTop + dialogView.paddingBottom

            x = context.resources.displayMetrics.widthPixels / 100
            y = ((context.resources.displayMetrics.heightPixels * f) - (hv * 0.5)).toInt()

            when (position) {
                Constants.POSITION_TOP -> {
                    y -= (h * 0.5).toInt()
                }
                Constants.POSITION_BOTTOM -> {
                    y += (h * 0.5).toInt()
                }
                else -> {}
            }
        }

        frameView.setBackgroundResource(when (position) {
            Constants.POSITION_TOP -> R.drawable.alert_slider_top
            Constants.POSITION_MIDDLE -> R.drawable.alert_slider_middle
            else -> R.drawable.alert_slider_bottom
        })

        iconView.setImageResource(when (ringerMode) {
            Constants.KEY_VALUE_SILENT -> R.drawable.ic_volume_ringer_mute
            Constants.KEY_VALUE_VIBRATE -> R.drawable.ic_volume_ringer_vibrate
            Constants.KEY_VALUE_NORMAL -> R.drawable.ic_volume_ringer
            Constants.KEY_VALUE_PRIORTY_ONLY -> R.drawable.ic_notifications_alert
            Constants.KEY_VALUE_TOTAL_SILENCE -> R.drawable.ic_notifications_silence
            else -> R.drawable.ic_info
        })

        textView.setText(when (ringerMode) {
            Constants.KEY_VALUE_SILENT -> R.string.notification_slider_mode_silent
            Constants.KEY_VALUE_VIBRATE -> R.string.notification_slider_mode_vibrate
            Constants.KEY_VALUE_NORMAL -> R.string.notification_slider_mode_none
            Constants.KEY_VALUE_PRIORTY_ONLY -> R.string.notification_slider_mode_priority_only
            Constants.KEY_VALUE_TOTAL_SILENCE -> R.string.notification_slider_mode_total_silence
            else -> R.string.notification_slider_mode_none
        })
    }

    companion object {
        private const val TAG = "AlertSliderDialog"
    }
}
