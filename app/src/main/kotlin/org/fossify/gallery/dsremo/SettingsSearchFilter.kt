package org.fossify.gallery.dsremo

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Generic filter for Fossify-style Settings layouts. The Settings XML pattern across all 6
 * Fossify forks is a hand-written `<LinearLayout>` with section labels (TextView using
 * `@style/SettingsSectionLabelStyle`) interleaved with row holders (RelativeLayout/
 * ConstraintLayout using `@style/SettingsHolderTextViewOneLinerStyle`).
 *
 * Algorithm:
 *   - Walk every direct child of [container].
 *   - For each child that's a section label (resourceName ends with "_section_label"):
 *       mark as section header; will be re-evaluated after all its rows are processed.
 *   - For each non-section-label child:
 *       extract all TextView texts inside; if any matches [query] (case-insensitive
 *       substring), keep visible; else hide.
 *   - After the walk, hide section labels whose rows below (until next section label
 *     or end of container) are all hidden.
 *
 * Empty query → show everything. The same helper applies to messages/contacts/calendar/
 * gallery/clock/phone — just copy this file (the package + import path differ per fork).
 */
object SettingsSearchFilter {

    fun apply(container: LinearLayout, query: String) {
        val needle = query.trim().lowercase()
        val children = mutableListOf<View>()
        for (i in 0 until container.childCount) children.add(container.getChildAt(i))

        // Pass 1: hide/show rows
        for (child in children) {
            if (isSectionLabel(child)) continue
            if (child.id == android.R.id.empty) continue
            val visible = needle.isEmpty() || matches(child, needle)
            child.visibility = if (visible) View.VISIBLE else View.GONE
        }

        // Pass 2: hide section labels whose rows are all GONE
        var sectionIdx = -1
        var sectionHasVisibleRow = false
        for (i in children.indices) {
            val child = children[i]
            if (isSectionLabel(child)) {
                // Finalize previous section
                if (sectionIdx >= 0) {
                    children[sectionIdx].visibility = if (sectionHasVisibleRow) View.VISIBLE else View.GONE
                }
                sectionIdx = i
                sectionHasVisibleRow = needle.isEmpty()
            } else {
                if (child.visibility == View.VISIBLE) sectionHasVisibleRow = true
            }
        }
        if (sectionIdx >= 0) {
            children[sectionIdx].visibility = if (sectionHasVisibleRow) View.VISIBLE else View.GONE
        }
    }

    private fun isSectionLabel(view: View): Boolean {
        if (view !is TextView) return false
        val name = try { view.resources.getResourceEntryName(view.id) } catch (_: Throwable) { return false }
        return name.endsWith("_section_label")
    }

    private fun matches(row: View, needle: String): Boolean {
        if (row is TextView) {
            return row.text?.toString()?.lowercase()?.contains(needle) == true
        }
        if (row is ViewGroup) {
            for (i in 0 until row.childCount) {
                if (matches(row.getChildAt(i), needle)) return true
            }
        }
        return false
    }
}
