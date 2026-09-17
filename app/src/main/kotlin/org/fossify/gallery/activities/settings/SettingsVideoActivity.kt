package org.fossify.gallery.activities.settings

import android.os.Bundle
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.models.RadioItem
import org.fossify.gallery.R
import org.fossify.gallery.activities.SimpleActivity
import org.fossify.gallery.databinding.ActivitySettingsVideoBinding
import org.fossify.gallery.extensions.config
import org.fossify.gallery.helpers.VIDEO_PLAYER_APP
import org.fossify.gallery.helpers.VIDEO_PLAYER_SYSTEM

class SettingsVideoActivity : SimpleActivity() {
    private val binding by viewBinding(ActivitySettingsVideoBinding::inflate)

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
        wireSwitchRow(
            binding.settingsAutoplayVideosHolder,
            binding.settingsAutoplayVideos,
            { config.autoplayVideos }
        ) { config.autoplayVideos = it }

        wireSwitchRow(
            binding.settingsRememberLastVideoPositionHolder,
            binding.settingsRememberLastVideoPosition,
            { config.rememberLastVideoPosition }
        ) { config.rememberLastVideoPosition = it }

        wireSwitchRow(
            binding.settingsMaxBrightnessHolder,
            binding.settingsMaxBrightness,
            { config.maxBrightness }
        ) { config.maxBrightness = it }

        wireSwitchRow(
            binding.settingsMuteVideosHolder,
            binding.settingsMuteVideos,
            { config.muteVideos }
        ) { config.muteVideos = it }

        wireSwitchRow(
            binding.settingsAllowVideoGesturesHolder,
            binding.settingsAllowVideoGestures,
            { config.allowVideoGestures }
        ) { config.allowVideoGestures = it }

        wireSwitchRow(
            binding.settingsShowExtendedDetailsHolder,
            binding.settingsShowExtendedDetails,
            { config.showExtendedDetails }
        ) { config.showExtendedDetails = it }

        wireSwitchRow(
            binding.settingsOpenVideosOnSeparateScreenHolder,
            binding.settingsOpenVideosOnSeparateScreen,
            { config.gestureVideoPlayer }
        ) { config.gestureVideoPlayer = it }

        binding.settingsOnVideoTap.text = getVideoPlayerTypeText()
        binding.settingsOnVideoTapHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(VIDEO_PLAYER_APP, getString(R.string.open_in_app_player)),
                RadioItem(VIDEO_PLAYER_SYSTEM, getString(R.string.open_system_default_player))
            )
            RadioGroupDialog(this, items, config.videoPlayerType) {
                config.videoPlayerType = it as Int
                binding.settingsOnVideoTap.text = getVideoPlayerTypeText()
            }
        }
    }

    private fun getVideoPlayerTypeText() = getString(
        when (config.videoPlayerType) {
            VIDEO_PLAYER_APP -> R.string.open_in_app_player
            else -> R.string.open_system_default_player
        }
    )
}
