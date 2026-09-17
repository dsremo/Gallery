package org.fossify.gallery.activities

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.gallery.activities.settings.SettingsAboutActivity
import org.fossify.gallery.activities.settings.SettingsAdvancedActivity
import org.fossify.gallery.activities.settings.SettingsAppearanceActivity
import org.fossify.gallery.activities.settings.SettingsBackupActivity
import org.fossify.gallery.activities.settings.SettingsGridActivity
import org.fossify.gallery.activities.settings.SettingsPrivacyActivity
import org.fossify.gallery.activities.settings.SettingsRecycleBinActivity
import org.fossify.gallery.activities.settings.SettingsVideoActivity
import org.fossify.gallery.adapters.SettingsCategoriesAdapter
import org.fossify.gallery.adapters.SettingsCategory
import org.fossify.gallery.databinding.ActivitySettingsBinding

class SettingsActivity : SimpleActivity() {
    private val binding by viewBinding(ActivitySettingsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupEdgeToEdge(
            padTopSystem = listOf(binding.settingsAppbar),
            padBottomSystem = listOf(binding.settingsCategoriesList)
        )

        val categories = listOf(
            SettingsCategory(
                title = "Appearance & display",
                subtitle = "Theme, colors, language, font size, thumbnail types"
            ) { openCategory(SettingsAppearanceActivity::class.java) },
            SettingsCategory(
                title = "Thumbnails & grid",
                subtitle = "Column count, file names, media count, album cover style"
            ) { openCategory(SettingsGridActivity::class.java) },
            SettingsCategory(
                title = "Video player",
                subtitle = "Autoplay, remember position, mute, brightness"
            ) { openCategory(SettingsVideoActivity::class.java) },
            SettingsCategory(
                title = "Privacy & security",
                subtitle = "App lock, blur sensitive, hide from other apps, strip EXIF"
            ) { openCategory(SettingsPrivacyActivity::class.java) },
            SettingsCategory(
                title = "Backup & sync",
                subtitle = "dsremo cloud backup and restore"
            ) { openCategory(SettingsBackupActivity::class.java) },
            SettingsCategory(
                title = "Recycle bin",
                subtitle = "Enable, auto-empty, pin, open"
            ) { openCategory(SettingsRecycleBinActivity::class.java) },
            SettingsCategory(
                title = "Advanced",
                subtitle = "GIF animation, whole-word search, date-taken, cache"
            ) { openCategory(SettingsAdvancedActivity::class.java) },
            SettingsCategory(
                title = "About",
                subtitle = "Version, feedback, license"
            ) { openCategory(SettingsAboutActivity::class.java) }
        )

        binding.settingsCategoriesList.layoutManager = LinearLayoutManager(this)
        binding.settingsCategoriesList.adapter = SettingsCategoriesAdapter(categories)
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.settingsAppbar, NavigationIcon.Arrow)
    }

    private fun openCategory(target: Class<*>) {
        startActivity(Intent(this, target))
    }
}
