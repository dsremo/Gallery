package org.fossify.gallery.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.fossify.gallery.databinding.ItemSettingsCategoryBinding

data class SettingsCategory(
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

class SettingsCategoriesAdapter(
    private val categories: List<SettingsCategory>
) : RecyclerView.Adapter<SettingsCategoriesAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemSettingsCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.binding.settingsCategoryTitle.text = category.title
        holder.binding.settingsCategorySubtitle.text = category.subtitle
        holder.binding.settingsCategoryHolder.setOnClickListener { category.onClick() }
    }

    override fun getItemCount(): Int = categories.size

    class CategoryViewHolder(val binding: ItemSettingsCategoryBinding) :
        RecyclerView.ViewHolder(binding.root)
}
