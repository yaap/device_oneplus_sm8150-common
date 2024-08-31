/*
 * Copyright (C) 2019 CypherOS
 * Copyright (C) 2014-2020 Paranoid Android
 * Copyright (C) 2023 The LineageOS Project
 * Copyright (C) 2023 Yet Another AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.yaap.device.DeviceSettings

import android.animation.Animator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.view.animation.OvershootInterpolator
import android.view.Gravity
import android.view.Surface
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
class AlertSliderDialog(private var context: Context) : Dialog(context, R.style.alert_slider_theme) {
    private val dialogView by lazy { findViewById<LinearLayout>(R.id.alert_slider_dialog) }
    private val frameView by lazy { findViewById<ViewGroup>(R.id.alert_slider_view) }
    private val iconView by lazy { findViewById<ImageView>(R.id.alert_slider_icon) }
    private val textView by lazy { findViewById<TextView>(R.id.alert_slider_text) }

    private val rotation: Int = context.getDisplay().getRotation()
    private val isLand: Boolean = rotation != Surface.ROTATION_0
    private val isLeft = context.resources.getBoolean(R.bool.alert_slider_dialog_left)
    private val length: Int
    private val xPos: Int
    private val yPos: Int

    private var isAnimating = false
    private var animator = ValueAnimator()

    init {
        // window init
        getWindow()?.let {
            it.requestFeature(Window.FEATURE_NO_TITLE)
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            it.addFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
            it.addPrivateFlags(WindowManager.LayoutParams.PRIVATE_FLAG_TRUSTED_OVERLAY)
            it.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
            it.setType(WindowManager.LayoutParams.TYPE_VOLUME_OVERLAY)

            it.attributes = it.attributes.apply {
                format = PixelFormat.TRANSLUCENT
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                title = TAG
            }
        }

        setCanceledOnTouchOutside(false)
        setContentView(R.layout.alert_slider_dialog)

        // position calculations
        val fraction = context.resources.getFraction(R.fraction.alert_slider_dialog_y, 1, 1)
        val widthPixels = context.resources.displayMetrics.widthPixels
        val heightPixels = context.resources.displayMetrics.heightPixels
        val pads = dialogView!!.paddingTop * 2 // equal paddings in all 4 directions
        length = if (isLand) context.resources.getDimension(R.dimen.alert_slider_dialog_width).toInt()
                 else context.resources.getDimension(R.dimen.alert_slider_dialog_height).toInt()
        val hv = (length + pads) * 0.5

        xPos = if (isLand) (widthPixels * fraction - hv).toInt()
               else if (isLeft) 0
               else widthPixels / 100
        yPos = if (isLand) (if (isLeft) (widthPixels / 100) else 0)
               else (heightPixels * fraction - hv).toInt()

        getWindow()?.let {
            it.attributes = it.attributes.apply {
                gravity = when(rotation) {
                    Surface.ROTATION_0 ->
                        if (isLeft) (Gravity.TOP or Gravity.LEFT)
                        else (Gravity.TOP or Gravity.RIGHT)
                    Surface.ROTATION_90 ->
                        if (isLeft) (Gravity.BOTTOM or Gravity.LEFT)
                        else (Gravity.TOP or Gravity.LEFT)
                    Surface.ROTATION_270 ->
                        if (isLeft) (Gravity.TOP or Gravity.RIGHT)
                        else (Gravity.BOTTOM or Gravity.RIGHT)
                    else ->
                        if (isLeft) (Gravity.TOP or Gravity.LEFT)
                        else (Gravity.TOP or Gravity.RIGHT)
                }

                x = xPos
                y = yPos
            }
        }
    }

    @Synchronized
    fun setState(position: Int, ringerMode: Int) {
        val delta = length * when(position) {
            Constants.POSITION_TOP -> -1
            Constants.POSITION_BOTTOM -> 1
            else -> 0 // Constants.POSITION_MIDDLE
        }
        var endX = xPos
        var endY = yPos
        if (isLand) endX += delta
        else endY += delta
        if (isShowing()) {
            animatePosition(endX, endY, position, ringerMode)
        } else {
            applyOnStart(ringerMode)
            applyOnEnd(endX, endY, position)
        }
    }

    @Synchronized
    private fun animatePosition(endX: Int, endY: Int, position: Int, ringerMode: Int) {
        if (isAnimating) animator.cancel()
        animator = ValueAnimator()
        animator.setDuration(100)
        animator.setInterpolator(OvershootInterpolator())
        getWindow()?.let {
            animator.setValues(
                PropertyValuesHolder.ofInt("x", it.attributes.x, endX),
                PropertyValuesHolder.ofInt("y", it.attributes.y, endY)
            )
        }
        animator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
            override fun onAnimationUpdate(animation: ValueAnimator) {
                getWindow()?.let {
                    it.attributes = it.attributes.apply {
                        x = animation.getAnimatedValue("x") as Int
                        y = animation.getAnimatedValue("y") as Int
                    }
                }
            }
        })
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                isAnimating = true
                applyOnStart(ringerMode)
            }

            override fun onAnimationEnd(animation: Animator) {
                applyOnEnd(endX, endY, position)
                isAnimating = false
            }

            override fun onAnimationCancel(animation: Animator) { }
            override fun onAnimationRepeat(animation: Animator) { }
        })
        animator.start()
    }

    private fun applyOnStart(ringerMode: Int) {
        sIconResMap.get(ringerMode)?.let {
            iconView!!.setImageResource(it)
        } ?: {
            iconView!!.setImageResource(R.drawable.ic_info)
        }

        sTextResMap.get(ringerMode)?.let {
            textView!!.setText(it)
        } ?: {
            textView!!.setText(R.string.notification_slider_mode_none)
        }
    }

    private fun applyOnEnd(endX: Int, endY: Int, position: Int) {
        if (isLeft) {
            frameView!!.setBackgroundResource(
                when (rotation) {
                    Surface.ROTATION_90 -> sBackgroundResMapLeft90.get(position)!!
                    Surface.ROTATION_270 -> sBackgroundResMapLeft270.get(position)!!
                    else -> sBackgroundResMapLeft.get(position)!! // Surface.ROTATION_0
                }
            )
        } else {
            frameView!!.setBackgroundResource(
                when (rotation) {
                    Surface.ROTATION_90 -> sBackgroundResMap90.get(position)!!
                    Surface.ROTATION_270 -> sBackgroundResMap270.get(position)!!
                    else -> sBackgroundResMap.get(position)!! // Surface.ROTATION_0
                }
            )
        }
        getWindow()?.let {
            it.attributes = it.attributes.apply {
                x = endX
                y = endY
            }
        }
    }

    companion object {
        private const val TAG = "AlertSliderDialog"

        private val sBackgroundResMap = hashMapOf(
            Constants.POSITION_TOP to R.drawable.alert_slider_top,
            Constants.POSITION_MIDDLE to R.drawable.alert_slider_middle,
            Constants.POSITION_BOTTOM to R.drawable.alert_slider_bottom
        )

        private val sBackgroundResMapLeft = hashMapOf(
            Constants.POSITION_TOP to R.drawable.alert_slider_top_270,
            Constants.POSITION_MIDDLE to R.drawable.alert_slider_middle,
            Constants.POSITION_BOTTOM to R.drawable.alert_slider_bottom_90
        )

        private val sBackgroundResMap90 = hashMapOf(
            Constants.POSITION_TOP to R.drawable.alert_slider_top_90,
            Constants.POSITION_MIDDLE to R.drawable.alert_slider_middle,
            Constants.POSITION_BOTTOM to R.drawable.alert_slider_bottom_90
        )

        private val sBackgroundResMapLeft90 = hashMapOf(
            Constants.POSITION_TOP to R.drawable.alert_slider_bottom_270,
            Constants.POSITION_MIDDLE to R.drawable.alert_slider_middle,
            Constants.POSITION_BOTTOM to R.drawable.alert_slider_top_270
        )

        private val sBackgroundResMap270 = hashMapOf(
            Constants.POSITION_TOP to R.drawable.alert_slider_top_270,
            Constants.POSITION_MIDDLE to R.drawable.alert_slider_middle,
            Constants.POSITION_BOTTOM to R.drawable.alert_slider_bottom_270
        )

        private val sBackgroundResMapLeft270 = hashMapOf(
            Constants.POSITION_TOP to R.drawable.alert_slider_bottom_90,
            Constants.POSITION_MIDDLE to R.drawable.alert_slider_middle,
            Constants.POSITION_BOTTOM to R.drawable.alert_slider_top_90
        )

        private val sIconResMap = hashMapOf(
            Constants.KEY_VALUE_SILENT to R.drawable.ic_volume_ringer_mute,
            Constants.KEY_VALUE_VIBRATE to R.drawable.ic_volume_ringer_vibrate,
            Constants.KEY_VALUE_NORMAL to R.drawable.ic_volume_ringer,
            Constants.KEY_VALUE_PRIORTY_ONLY to R.drawable.ic_notifications_alert,
            Constants.KEY_VALUE_TOTAL_SILENCE to R.drawable.ic_notifications_silence
        )

        private val sTextResMap = hashMapOf(
            Constants.KEY_VALUE_SILENT to R.string.notification_slider_mode_silent,
            Constants.KEY_VALUE_VIBRATE to R.string.notification_slider_mode_vibrate,
            Constants.KEY_VALUE_NORMAL to R.string.notification_slider_mode_none,
            Constants.KEY_VALUE_PRIORTY_ONLY to R.string.notification_slider_mode_priority_only,
            Constants.KEY_VALUE_TOTAL_SILENCE to R.string.notification_slider_mode_total_silence
        )
    }
}
