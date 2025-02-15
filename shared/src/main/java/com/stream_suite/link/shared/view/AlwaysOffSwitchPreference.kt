package com.stream_suite.link.shared.view

import android.content.Context
import android.util.AttributeSet
import androidx.preference.SwitchPreference

class AlwaysOffSwitchPreference : SwitchPreference {

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)

    init {
        isChecked = false
    }
}
