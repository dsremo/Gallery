package org.fossify.gallery.activities.settings

import android.view.View
import org.fossify.commons.views.MyMaterialSwitch

fun wireSwitchRow(
    holder: View,
    switch: MyMaterialSwitch,
    getValue: () -> Boolean,
    setValue: (Boolean) -> Unit
) {
    switch.isChecked = getValue()
    holder.setOnClickListener {
        switch.toggle()
        setValue(switch.isChecked)
    }
}
