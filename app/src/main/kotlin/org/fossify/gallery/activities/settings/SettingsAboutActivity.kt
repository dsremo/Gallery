package org.fossify.gallery.activities.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.gallery.BuildConfig
import org.fossify.gallery.activities.SimpleActivity
import org.fossify.gallery.databinding.ActivitySettingsAboutBinding

class SettingsAboutActivity : SimpleActivity() {
    private val binding by viewBinding(ActivitySettingsAboutBinding::inflate)

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

        binding.settingsAboutVersion.text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

        binding.settingsAboutSourceHolder.setOnClickListener {
            openUrl("https://github.com/dsremo/Gallery")
        }
        binding.settingsAboutReportHolder.setOnClickListener {
            openUrl("https://github.com/dsremo/Gallery/issues")
        }
        binding.settingsAboutFeedbackHolder.setOnClickListener {
            val subject = "Gallery feedback (${BuildConfig.VERSION_NAME})"
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:feedback@dsremo.com")
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }
            try {
                startActivity(emailIntent)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
        binding.settingsAboutLicenseHolder.setOnClickListener {
            openUrl("https://www.gnu.org/licenses/gpl-3.0.html")
        }
        binding.settingsAboutThirdPartyHolder.setOnClickListener {
            openUrl("https://github.com/dsremo/Gallery/blob/main/THIRD_PARTY_LICENSES.md")
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }
}
