package org.fossify.gallery.activities.settings

import android.os.Bundle
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.dialogs.SecurityDialog
import org.fossify.commons.extensions.beGoneIf
import org.fossify.commons.extensions.isExternalStorageManager
import org.fossify.commons.helpers.isRPlus
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.PROTECTION_FINGERPRINT
import org.fossify.commons.helpers.SHOW_ALL_TABS
import org.fossify.commons.views.MyMaterialSwitch
import org.fossify.gallery.activities.SimpleActivity
import org.fossify.gallery.databinding.ActivitySettingsPrivacyBinding
import org.fossify.gallery.extensions.config

class SettingsPrivacyActivity : SimpleActivity() {
    private val binding by viewBinding(ActivitySettingsPrivacyBinding::inflate)

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

        setupPasswordRow(
            binding.settingsAppPasswordProtection,
            { config.isAppPasswordProtectionOn },
            { config.appPasswordHash },
            { config.appProtectionType }
        ) { newHash, newType ->
            val current = config.isAppPasswordProtectionOn
            config.isAppPasswordProtectionOn = !current
            config.appPasswordHash = if (current) "" else newHash
            config.appProtectionType = newType
            binding.settingsAppPasswordProtection.isChecked = !current
            confirmProtection(config.isAppPasswordProtectionOn, config.appProtectionType)
        }.also { onClick ->
            binding.settingsAppPasswordProtectionHolder.setOnClickListener { onClick() }
        }

        binding.settingsHiddenItemPasswordProtectionHolder.beGoneIf(isRPlus() && !isExternalStorageManager())
        setupPasswordRow(
            binding.settingsHiddenItemPasswordProtection,
            { config.isHiddenPasswordProtectionOn },
            { config.hiddenPasswordHash },
            { config.hiddenProtectionType }
        ) { newHash, newType ->
            val current = config.isHiddenPasswordProtectionOn
            config.isHiddenPasswordProtectionOn = !current
            config.hiddenPasswordHash = if (current) "" else newHash
            config.hiddenProtectionType = newType
            binding.settingsHiddenItemPasswordProtection.isChecked = !current
            confirmProtection(config.isHiddenPasswordProtectionOn, config.hiddenProtectionType)
        }.also { onClick ->
            binding.settingsHiddenItemPasswordProtectionHolder.setOnClickListener { onClick() }
        }

        binding.settingsExcludedItemPasswordProtectionHolder.beGoneIf(isRPlus() && !isExternalStorageManager())
        setupPasswordRow(
            binding.settingsExcludedItemPasswordProtection,
            { config.isExcludedPasswordProtectionOn },
            { config.excludedPasswordHash },
            { config.excludedProtectionType }
        ) { newHash, newType ->
            val current = config.isExcludedPasswordProtectionOn
            config.isExcludedPasswordProtectionOn = !current
            config.excludedPasswordHash = if (current) "" else newHash
            config.excludedProtectionType = newType
            binding.settingsExcludedItemPasswordProtection.isChecked = !current
            confirmProtection(config.isExcludedPasswordProtectionOn, config.excludedProtectionType)
        }.also { onClick ->
            binding.settingsExcludedItemPasswordProtectionHolder.setOnClickListener { onClick() }
        }

        setupPasswordRow(
            binding.settingsFileDeletionPasswordProtection,
            { config.isDeletePasswordProtectionOn },
            { config.deletePasswordHash },
            { config.deleteProtectionType }
        ) { newHash, newType ->
            val current = config.isDeletePasswordProtectionOn
            config.isDeletePasswordProtectionOn = !current
            config.deletePasswordHash = if (current) "" else newHash
            config.deleteProtectionType = newType
            binding.settingsFileDeletionPasswordProtection.isChecked = !current
            confirmProtection(config.isDeletePasswordProtectionOn, config.deleteProtectionType)
        }.also { onClick ->
            binding.settingsFileDeletionPasswordProtectionHolder.setOnClickListener { onClick() }
        }

        wireSwitchRow(
            binding.settingsDsremoBlurSensitiveHolder,
            binding.settingsDsremoBlurSensitive,
            { config.dsremoBlurSensitiveThumbnails }
        ) { config.dsremoBlurSensitiveThumbnails = it }

        wireSwitchRow(
            binding.settingsDsremoShowSystemHiddenHolder,
            binding.settingsDsremoShowSystemHidden,
            { config.dsremoShowSystemHidden }
        ) { config.dsremoShowSystemHidden = it }

        wireSwitchRow(
            binding.settingsDsremoStripExifOnShareHolder,
            binding.settingsDsremoStripExifOnShare,
            { config.dsremoStripExifOnShare }
        ) { config.dsremoStripExifOnShare = it }
    }

    private fun setupPasswordRow(
        switch: MyMaterialSwitch,
        getEnabled: () -> Boolean,
        getHash: () -> String,
        getType: () -> Int,
        onSuccess: (String, Int) -> Unit
    ): () -> Unit {
        switch.isChecked = getEnabled()
        return {
            val tabToShow = if (getEnabled()) getType() else SHOW_ALL_TABS
            SecurityDialog(this, getHash(), tabToShow) { newHash, newType, success ->
                if (success) onSuccess(newHash, newType)
            }
        }
    }

    private fun confirmProtection(enabled: Boolean, protectionType: Int) {
        if (enabled) {
            val messageId = if (protectionType == PROTECTION_FINGERPRINT) {
                org.fossify.commons.R.string.fingerprint_setup_successfully
            } else {
                org.fossify.commons.R.string.protection_setup_successfully
            }
            ConfirmationDialog(this, "", messageId, org.fossify.commons.R.string.ok, 0) { }
        }
    }
}
