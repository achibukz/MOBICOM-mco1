package com.mobdeve.s18.mco.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mobdeve.s18.mco.R
import com.mobdeve.s18.mco.databinding.ItemEntryCardBinding
import com.mobdeve.s18.mco.databinding.ItemJournalSectionHeaderBinding
import com.mobdeve.s18.mco.models.JournalEntry
import com.mobdeve.s18.mco.utils.DateUtils

class JournalAdapter(
    private val onEntryClick: (JournalEntry) -> Unit
) : ListAdapter<JournalAdapter.JournalItem, RecyclerView.ViewHolder>(JournalItemDiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ENTRY = 1
    }

    sealed class JournalItem {
        data class Header(val monthYear: String) : JournalItem()
        data class Entry(val entry: JournalEntry) : JournalItem()
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is JournalItem.Header -> TYPE_HEADER
            is JournalItem.Entry -> TYPE_ENTRY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemJournalSectionHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                HeaderViewHolder(binding)
            }
            TYPE_ENTRY -> {
                val binding = ItemEntryCardBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                EntryViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is JournalItem.Header -> (holder as HeaderViewHolder).bind(item.monthYear)
            is JournalItem.Entry -> (holder as EntryViewHolder).bind(item.entry)
        }
    }

    inner class HeaderViewHolder(
        private val binding: ItemJournalSectionHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(monthYear: String) {
            binding.tvSectionHeader.text = monthYear
        }
    }

    inner class EntryViewHolder(
        private val binding: ItemEntryCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: JournalEntry) {
            binding.tvTitle.text = entry.title
            binding.tvDate.text = DateUtils.formatDateTime(entry.timestamp)
            binding.chipLocation.text = entry.address ?: "Unknown Location"

            // Load first photo as thumbnail
            if (entry.photos.isNotEmpty()) {
                Glide.with(binding.ivThumbnail.context)
                    .load(entry.photos.first().uriString) // FIX: Changed .uri to .uriString
                    .placeholder(R.color.secondary)
                    .error(R.color.secondary)
                    .centerCrop()
                    .into(binding.ivThumbnail)
            } else {
                binding.ivThumbnail.setImageResource(R.color.secondary)
            }

            binding.root.setOnClickListener {
                onEntryClick(entry)
            }
        }
    }

    private class JournalItemDiffCallback : DiffUtil.ItemCallback<JournalItem>() {
        override fun areItemsTheSame(oldItem: JournalItem, newItem: JournalItem): Boolean {
            return when {
                oldItem is JournalItem.Header && newItem is JournalItem.Header ->
                    oldItem.monthYear == newItem.monthYear
                oldItem is JournalItem.Entry && newItem is JournalItem.Entry ->
                    oldItem.entry.id == newItem.entry.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: JournalItem, newItem: JournalItem): Boolean {
            return oldItem == newItem
        }
    }
}