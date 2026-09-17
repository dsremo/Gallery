package org.fossify.gallery.activities.settings

import android.os.Bundle
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.gallery.R
import org.fossify.gallery.activities.SimpleActivity
import org.fossify.gallery.databinding.ActivitySettingsGridBinding
import org.fossify.gallery.dialogs.ChangeFileThumbnailStyleDialog
import org.fossify.gallery.dialogs.ChangeFolderThumbnailStyleDialog
import org.fossify.gallery.extensions.config
import org.fossify.gallery.helpers.FOLDER_MEDIA_CNT_NONE
import org.fossify.gallery.helpers.FOLDER_STYLE_SQUARE

class SettingsGridActivity : SimpleActivity() {
    private val binding by viewBinding(ActivitySettingsGridBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupEdgeToEdge(
            padTopSystem = listOf(binding.settingsAppbar),
            padBottomSystem = listOf(binding.settingsNestedScrollview)
        )
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.settingsAppbar, NavigationIcon.Arrow)

        binding.settingsDisplayFileNamesHolder.visibility = android.view.View.GONE
        config.displayFileNames = false

        binding.settingsFolderMediaCount.isChecked = config.showFolderMediaCount != FOLDER_MEDIA_CNT_NONE
        binding.settingsFolderMediaCountHolder.setOnClickListener {
            binding.settingsFolderMediaCount.toggle()
            config.showFolderMediaCount = if (binding.settingsFolderMediaCount.isChecked) {
                org.fossify.gallery.helpers.FOLDER_MEDIA_CNT_LINE
            } else {
                FOLDER_MEDIA_CNT_NONE
            }
        }

        wireSwitchRow(
            binding.settingsCropThumbnailsHolder,
            binding.settingsCropThumbnails,
            { config.cropThumbnails }
        ) { config.cropThumbnails = it }

        binding.settingsFolderThumbnailStyle.text = getFolderStyleText()
        binding.settingsFolderThumbnailStyleHolder.setOnClickListener {
            ChangeFolderThumbnailStyleDialog(this) {
                binding.settingsFolderThumbnailStyle.text = getFolderStyleText()
            }
        }

        binding.settingsFileThumbnailStyleHolder.setOnClickListener {
            ChangeFileThumbnailStyleDialog(this)
        }

        wireSwitchRow(
            binding.settingsHideExtendedDetailsHolder,
            binding.settingsHideExtendedDetails,
            { config.hideExtendedDetails }
        ) { config.hideExtendedDetails = it }
    }

    private fun getFolderStyleText() = getString(
        when (config.folderStyle) {
            FOLDER_STYLE_SQUARE -> R.string.square
            else -> R.string.rounded_corners
        }
    )
}
