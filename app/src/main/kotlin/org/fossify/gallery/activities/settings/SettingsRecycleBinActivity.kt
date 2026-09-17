package org.fossify.gallery.activities.settings

import android.content.Intent
import android.os.Bundle
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.formatSize
import org.fossify.commons.extensions.getProperSize
import org.fossify.commons.extensions.recycleBinPath
import org.fossify.gallery.extensions.showRecycleBinEmptyingDialog
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.helpers.sumByLong
import org.fossify.commons.models.RadioItem
import org.fossify.gallery.activities.MediaActivity
import org.fossify.gallery.activities.SimpleActivity
import org.fossify.gallery.databinding.ActivitySettingsRecycleBinBinding
import org.fossify.gallery.extensions.config
import org.fossify.gallery.extensions.emptyTheRecycleBin
import org.fossify.gallery.extensions.mediaDB
import org.fossify.gallery.helpers.DIRECTORY
import org.fossify.gallery.helpers.RECYCLE_BIN
import java.io.File

class SettingsRecycleBinActivity : SimpleActivity() {
    private val binding by viewBinding(ActivitySettingsRecycleBinBinding::inflate)
    private var mRecycleBinContentSize = 0L

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
        setupUseRecycleBin()
        setupShowRecycleBin()
        setupShowRecycleBinLast()
        setupEmptyRecycleBin()
        setupOpenRecycleBin()
        setupTrashRetention()
        updateRecycleBinButtons()
    }

    private fun setupUseRecycleBin() {
        binding.settingsUseRecycleBin.isChecked = config.useRecycleBin
        binding.settingsUseRecycleBinHolder.setOnClickListener {
            binding.settingsUseRecycleBin.toggle()
            config.useRecycleBin = binding.settingsUseRecycleBin.isChecked
            updateRecycleBinButtons()
        }
    }

    private fun setupShowRecycleBin() {
        binding.settingsShowRecycleBin.isChecked = config.showRecycleBinAtFolders
        binding.settingsShowRecycleBinHolder.setOnClickListener {
            binding.settingsShowRecycleBin.toggle()
            config.showRecycleBinAtFolders = binding.settingsShowRecycleBin.isChecked
            updateRecycleBinButtons()
        }
    }

    private fun setupShowRecycleBinLast() {
        binding.settingsShowRecycleBinLast.isChecked = config.showRecycleBinLast
        binding.settingsShowRecycleBinLastHolder.setOnClickListener {
            binding.settingsShowRecycleBinLast.toggle()
            config.showRecycleBinLast = binding.settingsShowRecycleBinLast.isChecked
            if (config.showRecycleBinLast) {
                config.removePinnedFolders(setOf(RECYCLE_BIN))
            }
        }
    }

    private fun updateRecycleBinButtons() {
        binding.settingsShowRecycleBinLastHolder.visibility =
            if (config.useRecycleBin && config.showRecycleBinAtFolders) android.view.View.VISIBLE else android.view.View.GONE
        binding.settingsEmptyRecycleBinHolder.visibility =
            if (config.useRecycleBin) android.view.View.VISIBLE else android.view.View.GONE
        binding.settingsShowRecycleBinHolder.visibility =
            if (config.useRecycleBin) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun setupEmptyRecycleBin() {
        ensureBackgroundThread {
            try {
                mRecycleBinContentSize = mediaDB.getDeletedMedia().sumByLong { medium ->
                    val size = medium.size
                    if (size == 0L) {
                        val path = medium.path.removePrefix(RECYCLE_BIN).prependIndent(recycleBinPath)
                        File(path).length()
                    } else {
                        size
                    }
                }
            } catch (ignored: Exception) {
            }
            runOnUiThread {
                binding.settingsEmptyRecycleBinSize.text = mRecycleBinContentSize.formatSize()
            }
        }

        binding.settingsEmptyRecycleBinHolder.setOnClickListener {
            if (mRecycleBinContentSize == 0L) {
                toast(org.fossify.commons.R.string.recycle_bin_empty)
            } else {
                showRecycleBinEmptyingDialog {
                    emptyTheRecycleBin()
                    mRecycleBinContentSize = 0L
                    binding.settingsEmptyRecycleBinSize.text = 0L.formatSize()
                }
            }
        }
    }

    private fun setupOpenRecycleBin() {
        binding.settingsOpenRecycleBinHolder.setOnClickListener {
            Intent(this, MediaActivity::class.java).apply {
                putExtra(DIRECTORY, RECYCLE_BIN)
                startActivity(this)
            }
        }
    }

    private fun setupTrashRetention() {
        binding.settingsTrashRetention.text = formatRetention(config.dsremoTrashRetentionDays)
        binding.settingsTrashRetentionHolder.setOnClickListener {
            val options = arrayListOf(7, 14, 30, 60, 90, 180, 365, 0)
            val items = options.map { RadioItem(it, formatRetention(it)) } as ArrayList<RadioItem>
            RadioGroupDialog(this, items, config.dsremoTrashRetentionDays) { picked ->
                config.dsremoTrashRetentionDays = picked as Int
                binding.settingsTrashRetention.text = formatRetention(config.dsremoTrashRetentionDays)
            }
        }
    }

    private fun formatRetention(days: Int): String =
        if (days <= 0) "Never" else "$days days"
}
