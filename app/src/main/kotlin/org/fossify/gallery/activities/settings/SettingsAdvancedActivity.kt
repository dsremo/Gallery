package org.fossify.gallery.activities.settings

import android.content.Intent
import android.os.Bundle
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.beGoneIf
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.formatSize
import org.fossify.commons.extensions.getProperSize
import org.fossify.commons.extensions.isExternalStorageManager
import org.fossify.commons.helpers.isQPlus
import org.fossify.commons.helpers.isRPlus
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.models.RadioItem
import org.fossify.gallery.R
import org.fossify.gallery.activities.ExcludedFoldersActivity
import org.fossify.gallery.activities.HiddenFoldersActivity
import org.fossify.gallery.activities.IncludedFoldersActivity
import org.fossify.gallery.activities.SimpleActivity
import org.fossify.gallery.databinding.ActivitySettingsAdvancedBinding
import org.fossify.gallery.dialogs.GrantAllFilesDialog
import org.fossify.gallery.dialogs.ManageBottomActionsDialog
import org.fossify.gallery.extensions.config
import org.fossify.gallery.extensions.handleExcludedFolderPasswordProtection
import org.fossify.commons.extensions.handleHiddenFolderPasswordProtection
import org.fossify.gallery.extensions.handleMediaManagementPrompt
import org.fossify.gallery.helpers.DEFAULT_BOTTOM_ACTIONS
import org.fossify.gallery.helpers.PRIORITY_COMPROMISE
import org.fossify.gallery.helpers.PRIORITY_SPEED
import org.fossify.gallery.helpers.PRIORITY_VALIDITY
import org.fossify.gallery.helpers.ROTATE_BY_ASPECT_RATIO
import org.fossify.gallery.helpers.ROTATE_BY_DEVICE_ROTATION
import org.fossify.gallery.helpers.ROTATE_BY_SYSTEM_SETTING

class SettingsAdvancedActivity : SimpleActivity() {
    private val binding by viewBinding(ActivitySettingsAdvancedBinding::inflate)

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

        wireSwitchRow(binding.settingsAnimateGifsHolder, binding.settingsAnimateGifs,
            { config.animateGifs }) { config.animateGifs = it }

        wireSwitchRow(binding.settingsSearchAllFilesHolder, binding.settingsSearchAllFiles,
            { config.searchAllFilesByDefault }) { config.searchAllFilesByDefault = it }

        setupShowHiddenItems()

        wireSwitchRow(binding.settingsDsremoShowSmartAlbumsHolder, binding.settingsDsremoShowSmartAlbums,
            { config.dsremoShowSmartAlbums }) { config.dsremoShowSmartAlbums = it }

        setupFileLoadingPriority()
        setupScreenRotation()

        wireSwitchRow(binding.settingsHideSystemUiHolder, binding.settingsHideSystemUi,
            { config.hideSystemUI }) { config.hideSystemUI = it }

        wireSwitchRow(binding.settingsKeepScreenOnFullscreenPhotosHolder, binding.settingsKeepScreenOnFullscreenPhotos,
            { config.keepScreenOn }) { config.keepScreenOn = it }

        wireSwitchRow(binding.settingsAllowPhotoGesturesHolder, binding.settingsAllowPhotoGestures,
            { config.allowPhotoGestures }) { config.allowPhotoGestures = it }

        wireSwitchRow(binding.settingsAllowDownGestureHolder, binding.settingsAllowDownGesture,
            { config.allowDownGesture }) { config.allowDownGesture = it }

        setupAllowZoomingImages()

        wireSwitchRow(binding.settingsAllowRotatingWithGesturesHolder, binding.settingsAllowRotatingWithGestures,
            { config.allowRotatingWithGestures }) { config.allowRotatingWithGestures = it }

        wireSwitchRow(binding.settingsAllowOneToOneZoomHolder, binding.settingsAllowOneToOneZoom,
            { config.allowOneToOneZoom }) { config.allowOneToOneZoom = it }

        wireSwitchRow(binding.settingsAllowInstantChangeHolder, binding.settingsAllowInstantChange,
            { config.allowInstantChange }) { config.allowInstantChange = it }

        setupManageIncludedFolders()
        setupManageExcludedFolders()
        setupManageHiddenFolders()
        setupManageBottomActions()

        wireSwitchRow(binding.settingsDeleteEmptyFoldersHolder, binding.settingsDeleteEmptyFolders,
            { config.deleteEmptyFolders }) { config.deleteEmptyFolders = it }

        setupKeepLastModified()

        wireSwitchRow(binding.settingsSkipDeleteConfirmationHolder, binding.settingsSkipDeleteConfirmation,
            { config.skipDeleteConfirmation }) { config.skipDeleteConfirmation = it }

        setupClearCache()
    }

    private fun setupShowHiddenItems() {
        binding.settingsShowHiddenItems.isChecked = config.showHiddenMedia
        if (isRPlus() && !isExternalStorageManager()) {
            binding.settingsShowHiddenItems.text = "${getString(org.fossify.commons.R.string.show_hidden_items)} (${getString(org.fossify.commons.R.string.no_permission)})"
        } else {
            binding.settingsShowHiddenItems.setText(org.fossify.commons.R.string.show_hidden_items)
        }
        binding.settingsShowHiddenItemsHolder.setOnClickListener {
            if (isRPlus() && !isExternalStorageManager()) {
                GrantAllFilesDialog(this)
            } else if (config.showHiddenMedia) {
                toggleHiddenItems()
            } else {
                handleHiddenFolderPasswordProtection { toggleHiddenItems() }
            }
        }
    }

    private fun toggleHiddenItems() {
        binding.settingsShowHiddenItems.toggle()
        config.showHiddenMedia = binding.settingsShowHiddenItems.isChecked
    }

    private fun setupFileLoadingPriority() {
        binding.settingsFileLoadingPriorityHolder.beGoneIf(isRPlus() && !isExternalStorageManager())
        binding.settingsFileLoadingPriority.text = getFileLoadingPriorityText()
        binding.settingsFileLoadingPriorityHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(PRIORITY_SPEED, getString(R.string.speed)),
                RadioItem(PRIORITY_COMPROMISE, getString(R.string.compromise)),
                RadioItem(PRIORITY_VALIDITY, getString(R.string.avoid_showing_invalid_files))
            )
            RadioGroupDialog(this, items, config.fileLoadingPriority) {
                config.fileLoadingPriority = it as Int
                binding.settingsFileLoadingPriority.text = getFileLoadingPriorityText()
            }
        }
    }

    private fun getFileLoadingPriorityText() = getString(
        when (config.fileLoadingPriority) {
            PRIORITY_SPEED -> R.string.speed
            PRIORITY_COMPROMISE -> R.string.compromise
            else -> R.string.avoid_showing_invalid_files
        }
    )

    private fun setupScreenRotation() {
        binding.settingsScreenRotation.text = getScreenRotationText()
        binding.settingsScreenRotationHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(ROTATE_BY_SYSTEM_SETTING, getString(R.string.screen_rotation_system_setting)),
                RadioItem(ROTATE_BY_DEVICE_ROTATION, getString(R.string.screen_rotation_device_rotation)),
                RadioItem(ROTATE_BY_ASPECT_RATIO, getString(R.string.screen_rotation_aspect_ratio))
            )
            RadioGroupDialog(this, items, config.screenRotation) {
                config.screenRotation = it as Int
                binding.settingsScreenRotation.text = getScreenRotationText()
            }
        }
    }

    private fun getScreenRotationText() = getString(
        when (config.screenRotation) {
            ROTATE_BY_SYSTEM_SETTING -> R.string.screen_rotation_system_setting
            ROTATE_BY_DEVICE_ROTATION -> R.string.screen_rotation_device_rotation
            else -> R.string.screen_rotation_aspect_ratio
        }
    )

    private fun setupAllowZoomingImages() {
        binding.settingsAllowZoomingImages.isChecked = config.allowZoomingImages
        updateDeepZoomToggleButtons()
        binding.settingsAllowZoomingImagesHolder.setOnClickListener {
            binding.settingsAllowZoomingImages.toggle()
            config.allowZoomingImages = binding.settingsAllowZoomingImages.isChecked
            updateDeepZoomToggleButtons()
        }
    }

    private fun updateDeepZoomToggleButtons() {
        binding.settingsAllowRotatingWithGesturesHolder.beVisibleIf(config.allowZoomingImages)
        binding.settingsAllowOneToOneZoomHolder.beVisibleIf(config.allowZoomingImages)
    }

    private fun setupManageIncludedFolders() {
        if (isRPlus() && !isExternalStorageManager()) {
            binding.settingsManageIncludedFolders.text = "${getString(R.string.manage_included_folders)} (${getString(org.fossify.commons.R.string.no_permission)})"
        } else {
            binding.settingsManageIncludedFolders.setText(R.string.manage_included_folders)
        }
        binding.settingsManageIncludedFoldersHolder.setOnClickListener {
            if (isRPlus() && !isExternalStorageManager()) {
                GrantAllFilesDialog(this)
            } else {
                startActivity(Intent(this, IncludedFoldersActivity::class.java))
            }
        }
    }

    private fun setupManageExcludedFolders() {
        binding.settingsManageExcludedFoldersHolder.setOnClickListener {
            handleExcludedFolderPasswordProtection {
                startActivity(Intent(this, ExcludedFoldersActivity::class.java))
            }
        }
    }

    private fun setupManageHiddenFolders() {
        binding.settingsManageHiddenFoldersHolder.beGoneIf(isQPlus())
        binding.settingsManageHiddenFoldersHolder.setOnClickListener {
            handleHiddenFolderPasswordProtection {
                startActivity(Intent(this, HiddenFoldersActivity::class.java))
            }
        }
    }

    private fun setupManageBottomActions() {
        binding.settingsManageBottomActionsHolder.setOnClickListener {
            ManageBottomActionsDialog(this) {
                if (config.visibleBottomActions == 0) {
                    config.visibleBottomActions = DEFAULT_BOTTOM_ACTIONS
                }
            }
        }
    }

    private fun setupKeepLastModified() {
        binding.settingsKeepLastModified.isChecked = config.keepLastModified
        binding.settingsKeepLastModifiedHolder.setOnClickListener {
            handleMediaManagementPrompt {
                binding.settingsKeepLastModified.toggle()
                config.keepLastModified = binding.settingsKeepLastModified.isChecked
            }
        }
    }

    private fun setupClearCache() {
        ensureBackgroundThread {
            val size = cacheDir.getProperSize(true).formatSize()
            runOnUiThread { binding.settingsClearCacheSize.text = size }
        }
        binding.settingsClearCacheHolder.setOnClickListener {
            ensureBackgroundThread {
                cacheDir.deleteRecursively()
                runOnUiThread {
                    binding.settingsClearCacheSize.text = cacheDir.getProperSize(true).formatSize()
                }
            }
        }
    }
}
