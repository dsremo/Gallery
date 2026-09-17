package org.fossify.gallery.activities.settings

import android.os.Bundle
import org.fossify.commons.dialogs.ChangeDateTimeFormatDialog
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.helpers.isTiramisuPlus
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.gallery.activities.SimpleActivity
import org.fossify.gallery.databinding.ActivitySettingsAppearanceBinding
import org.fossify.gallery.extensions.config
import org.fossify.gallery.helpers.ColorModeHelper
import java.util.Locale
import kotlin.system.exitProcess

class SettingsAppearanceActivity : SimpleActivity() {
    private val binding by viewBinding(ActivitySettingsAppearanceBinding::inflate)

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

        binding.settingsColorCustomizationHolder.setOnClickListener { startCustomizationActivity() }

        binding.settingsUseEnglishHolder.beVisibleIf(
            (config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus()
        )
        binding.settingsUseEnglish.isChecked = config.useEnglish
        binding.settingsUseEnglishHolder.setOnClickListener {
            binding.settingsUseEnglish.toggle()
            config.useEnglish = binding.settingsUseEnglish.isChecked
            exitProcess(0)
        }

        binding.settingsLanguage.text = Locale.getDefault().displayLanguage
        binding.settingsLanguageHolder.beVisibleIf(isTiramisuPlus())
        binding.settingsLanguageHolder.setOnClickListener { launchChangeAppLanguageIntent() }

        binding.settingsChangeDateTimeFormatHolder.setOnClickListener {
            ChangeDateTimeFormatDialog(this) {}
        }

        wireSwitchRow(
            binding.settingsBlackBackgroundHolder,
            binding.settingsBlackBackground,
            { config.blackBackground }
        ) { config.blackBackground = it }

        binding.settingsUltraHdrRenderingHolder.beVisibleIf(ColorModeHelper.isGainmapSupported())
        wireSwitchRow(
            binding.settingsUltraHdrRenderingHolder,
            binding.settingsUltraHdrRendering,
            { config.ultraHdrRendering }
        ) { config.ultraHdrRendering = it }

        wireSwitchRow(
            binding.settingsShowThumbnailFileTypesHolder,
            binding.settingsShowThumbnailFileTypes,
            { config.showThumbnailFileTypes }
        ) { config.showThumbnailFileTypes = it }

        wireSwitchRow(
            binding.settingsFileRoundedCornersHolder,
            binding.settingsFileRoundedCorners,
            { config.fileRoundedCorners }
        ) { config.fileRoundedCorners = it }
    }
}
