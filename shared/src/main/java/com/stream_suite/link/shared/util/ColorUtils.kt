/*
 * Copyright (C) 2020 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stream_suite.link.shared.util

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.EdgeEffect
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.stream_suite.link.shared.R
import com.stream_suite.link.shared.activity.AbstractSettingsActivity
import com.stream_suite.link.shared.data.ColorSet
import com.stream_suite.link.shared.data.Settings
import com.stream_suite.link.shared.data.pojo.BaseTheme
import java.util.*
import kotlin.math.pow

/**
 * Helper class for working with colors.
 */
object ColorUtils {

    fun getRandomMaterialColor(context: Context): ColorSet {
        val num = (Math.random() * (16 + 1)).toInt()

        when (num) {
            0 -> return ColorSet.RED(context)
            1 -> return ColorSet.PINK(context)
            2 -> return ColorSet.PURPLE(context)
            3 -> return ColorSet.DEEP_PURPLE(context)
            4 -> return ColorSet.INDIGO(context)
            5 -> return ColorSet.BLUE(context)
            6 -> return ColorSet.LIGHT_BLUE(context)
            7 -> return ColorSet.CYAN(context)
            8 -> return ColorSet.GREEN(context)
            9 -> return ColorSet.LIGHT_GREEN(context)
            10 -> return ColorSet.AMBER(context)
            11 -> return ColorSet.ORANGE(context)
            12 -> return ColorSet.DEEP_ORANGE(context)
            13 -> return ColorSet.BROWN(context)
            14 -> return ColorSet.GREY(context)
            15 -> return ColorSet.BLUE_GREY(context)
            16 -> return ColorSet.TEAL(context)
            17,
                //return ColorSet.Companion.LIME(context);
            18,
                //return ColorSet.Companion.YELLOW(context);
            19 -> return ColorSet.TEAL(context)
        //return ColorSet.Companion.WHITE(context);
            else -> return ColorSet.TEAL(context)
        }
    }

    /**
     * Converts a color integer into it's hex equivalent.
     */
    fun convertToHex(color: Int): String {
        return String.format("#%06X", 0xFFFFFF and color)
    }

    /**
     * Adjusts the status bar color depending on whether you are on a phone or tablet.
     *
     * @param color    the color to change to.
     * @param activity the activity to find the views in.
     */
    fun adjustStatusBarColor(toolbarColor: Int, statusBarColor: Int, activity: Activity?) {
        var statusBarColor = statusBarColor
        var toolbarColor = toolbarColor
        if (activity == null) {
            return
        }

        if (Settings.useGlobalThemeColor) {
            statusBarColor = Settings.mainColorSet.colorDark
            toolbarColor = Settings.mainColorSet.color
        }

        statusBarColor = ActivityUtils.possiblyOverrideColorSelection(activity, statusBarColor)
        toolbarColor = ActivityUtils.possiblyOverrideColorSelection(activity, toolbarColor)

        if (!activity.resources.getBoolean(R.bool.pin_drawer)) {
            ActivityUtils.setStatusBarColor(activity, statusBarColor)
        } else {
            val status = activity.findViewById<View>(R.id.status_bar)

            if (status != null) {
                status.backgroundTintList = ColorStateList.valueOf(statusBarColor)
            }
        }

        ActivityUtils.setUpLightStatusBar(activity, toolbarColor)
    }

    /**
     * Sets the cursor color for an edit text to the supplied color. Reflection is required here,
     * unfortunately.
     *
     * @param editText the edit text to change.
     * @param color    the color of the new cursor.
     */
    fun setCursorDrawableColor(editText: EditText, color: Int) {
        val cursorColor = if (Settings.useGlobalThemeColor) {
            Settings.mainColorSet.colorAccent
        } else {
            color
        }

        try {
            if (AndroidVersionUtil.isAndroidQ) {
                editText.textCursorDrawable?.colorFilter = PorterDuffColorFilter(cursorColor, PorterDuff.Mode.SRC_IN)
                return
            }

            val fCursorDrawableRes = TextView::class.java.getDeclaredField("mCursorDrawableRes")
            fCursorDrawableRes.isAccessible = true
            val mCursorDrawableRes = fCursorDrawableRes.getInt(editText)
            val fEditor = TextView::class.java.getDeclaredField("mEditor")
            fEditor.isAccessible = true
            val editor = fEditor.get(editText)
            val clazz = editor.javaClass
            val fCursorDrawable = clazz.getDeclaredField("mCursorDrawable")
            fCursorDrawable.isAccessible = true
            val drawables = arrayOfNulls<Drawable>(2)
            drawables[0] = editText.context.getDrawable(mCursorDrawableRes)
            drawables[1] = editText.context.getDrawable(mCursorDrawableRes)
            drawables[0]?.setColorFilter(cursorColor, PorterDuff.Mode.SRC_IN)
            drawables[1]?.setColorFilter(cursorColor, PorterDuff.Mode.SRC_IN)
            fCursorDrawable.set(editor, drawables)
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        }
    }

    /**
     * Set the color of the handles when you select text in a
     * [android.widget.EditText] or other view that extends [TextView].
     *
     * @param view
     * The [TextView] or a [View] that extends [TextView].
     * @param color
     * The color to set for the text handles
     */
    fun colorTextSelectionHandles(view: TextView, color: Int) {
        val handleColor = if (Settings.useGlobalThemeColor) {
            Settings.mainColorSet.colorAccent
        } else {
            color
        }

        try {
            if (AndroidVersionUtil.isAndroidQ) {
                val colorFilter = PorterDuffColorFilter(handleColor, PorterDuff.Mode.SRC_IN)
                view.textSelectHandle?.colorFilter = colorFilter
                view.textSelectHandleLeft?.colorFilter = colorFilter
                view.textSelectHandleRight?.colorFilter = colorFilter
                return
            }

            val editorField = TextView::class.java.getDeclaredField("mEditor")
            if (!editorField.isAccessible) {
                editorField.isAccessible = true
            }

            val editor = editorField.get(view)
            val editorClass = editor.javaClass

            val handleNames = arrayOf("mSelectHandleLeft", "mSelectHandleRight", "mSelectHandleCenter")
            val resNames = arrayOf("mTextSelectHandleLeftRes", "mTextSelectHandleRightRes", "mTextSelectHandleRes")

            for (i in handleNames.indices) {
                val handleField = editorClass.getDeclaredField(handleNames[i])
                if (!handleField.isAccessible) {
                    handleField.isAccessible = true
                }

                var handleDrawable: Drawable? = handleField.get(editor) as Drawable?

                if (handleDrawable == null) {
                    val resField = TextView::class.java.getDeclaredField(resNames[i])
                    if (!resField.isAccessible) {
                        resField.isAccessible = true
                    }
                    val resId = resField.getInt(view)
                    handleDrawable = view.resources.getDrawable(resId)
                }

                if (handleDrawable != null) {
                    val drawable = handleDrawable.mutate()
                    drawable.setColorFilter(handleColor, PorterDuff.Mode.SRC_IN)
                    handleField.set(editor, drawable)
                }
            }
        } catch (e: Exception) {
        }
    }

    /**
     * Changes the overscroll highlight effect on a recyclerview to be the given color.
     */
    fun changeRecyclerOverscrollColors(recyclerView: RecyclerView, color: Int) {
        val colorWithGlobalCalculated = if (Settings.useGlobalThemeColor) {
            Settings.mainColorSet.color
        } else {
            color
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var invoked = false

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                // only invoke this once
                if (invoked) {
                    return
                } else {
                    invoked = true
                }

                val clazz = RecyclerView::class.java

                try {
                    for (name in arrayOf("ensureTopGlow", "ensureBottomGlow", "ensureLeftGlow", "ensureRightGlow")) {
                        val method = clazz.getDeclaredMethod(name)
                        method.isAccessible = true
                        method.invoke(recyclerView)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    for (name in arrayOf("mTopGlow", "mBottomGlow", "mLeftGlow", "mRightGlow")) {
                        val field = clazz.getDeclaredField(name)
                        field.isAccessible = true
                        val edge = field.get(recyclerView)
                        (edge as EdgeEffect).color = colorWithGlobalCalculated
                    }
                } catch (e: Exception) {
                    e.printStackTrace()

                    try {
                        for (name in arrayOf("mTopGlow", "mBottomGlow", "mLeftGlow", "mRightGlow")) {
                            val field = clazz.getDeclaredField(name)
                            field.isAccessible = true
                            val edge = field.get(recyclerView)
                            val fEdgeEffect = edge.javaClass.getDeclaredField("mEdgeEffect")
                            fEdgeEffect.isAccessible = true
                            (edge as EdgeEffect).color = colorWithGlobalCalculated
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }

                }

            }
        })
    }

    /**
     * Changes the window background to black if applicable
     */
    fun checkBlackBackground(activity: Activity) {
        if (Settings.baseTheme === BaseTheme.BLACK) {
            val background = activity.window.decorView.background
            if (background is ColorDrawable) {
                if (background.color == Color.BLACK) {
                    return
                }
            }

            activity.window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        }
    }

    fun getColors(context: Context): List<ColorSet> {
        val colors = ArrayList<ColorSet>()
        colors.add(ColorSet.RED(context))
        colors.add(ColorSet.PINK(context))
        colors.add(ColorSet.PURPLE(context))
        colors.add(ColorSet.DEEP_PURPLE(context))
        colors.add(ColorSet.INDIGO(context))
        colors.add(ColorSet.PULSE_BLUE(context))
        colors.add(ColorSet.BLUE(context))
        colors.add(ColorSet.LIGHT_BLUE(context))
        colors.add(ColorSet.CYAN(context))
        colors.add(ColorSet.TEAL(context))
        colors.add(ColorSet.GREEN(context))
        colors.add(ColorSet.DEFAULT(context))
        colors.add(ColorSet.YELLOW(context))
        colors.add(ColorSet.AMBER(context))
        colors.add(ColorSet.ORANGE(context))
        colors.add(ColorSet.DEEP_ORANGE(context))
        colors.add(ColorSet.BLACK(context))
        colors.add(ColorSet.BLUE_GREY(context))
        colors.add(ColorSet.GREY(context))
        colors.add(ColorSet.WHITE(context))
        return colors
    }

    fun animateToolbarColor(activity: Activity, originalColor: Int, newColor: Int) {
        val drawable = ColorDrawable(originalColor)
        var actionBar: ActionBar? = null
        var toolbar: Toolbar? = null

        try {
            if (activity is AbstractSettingsActivity) {
                toolbar = activity.toolbar
                toolbar!!.setBackgroundColor(originalColor)
            } else {
                actionBar = (activity as AppCompatActivity).supportActionBar
                actionBar!!.setBackgroundDrawable(drawable)
            }
        } catch (e: Exception) {
        }

        val animator = ValueAnimator.ofArgb(originalColor, newColor)
        animator.duration = 200
        animator.addUpdateListener { valueAnimator ->
            val color = valueAnimator.animatedValue as Int
            if (toolbar != null) {
                toolbar.setBackgroundColor(color)
            } else {
                drawable.color = color
                actionBar?.setBackgroundDrawable(drawable)
            }
        }

        animator.start()
    }

    fun animateStatusBarColor(activity: Activity, originalColor: Int, newColor: Int, finalToolbarColor: Int = newColor) {
        val animator = ValueAnimator.ofArgb(originalColor, newColor)
        animator.duration = 200
        animator.addUpdateListener { valueAnimator ->
            val color = valueAnimator.animatedValue as Int
            if (activity.window != null) {
                ActivityUtils.setStatusBarColor(activity, color, finalToolbarColor)
            }
        }
        animator.start()
    }

    // Modeled after WCAG 2.0 Success Criterion 1.4.3
    // https://www.w3.org/TR/2008/REC-WCAG20-20081211/#visual-audio-contrast-contrast
    fun isColorDark(color: Int): Boolean {
        val red = getSRGB(Color.red(color))
        val green = getSRGB(Color.green(color))
        val blue = getSRGB(Color.blue(color))

        // Compute the relative luminance of the color
        // https://www.w3.org/TR/WCAG20/#relativeluminancedef
        val luminance = (0.2126 * red + 0.7152 * green + 0.0722 * blue)

        // Determine color based on the contrast ratio
        // https://www.w3.org/TR/WCAG20/#contrast-ratiodef
        return luminance  < 0.35
    }

    private fun getSRGB(component: Int): Double {
        val sRGB = component.toDouble() / 255
        return if (sRGB <= 0.03928) {
            sRGB / 12.92
        }
        else {
            ((sRGB + 0.055) / 1.055).pow(2.4)
        }
    }
}
