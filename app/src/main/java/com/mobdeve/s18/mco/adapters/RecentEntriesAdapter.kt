package com.mobdeve.s18.mco.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mobdeve.s18.mco.R
import com.mobdeve.s18.mco.databinding.ItemEntryCardBinding
import com.mobdeve.s18.mco.models.JournalEntry
import com.mobdeve.s18.mco.utils.DateUtils

class RecentEntriesAdapter(
    private val onEntryClick: (JournalEntry) -> Unit
) : ListAdapter<JournalEntry, RecentEntriesAdapter.EntryViewHolder>(EntryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val binding = ItemEntryCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EntryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        holder.bind(getItem(position))
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

    private class EntryDiffCallback : DiffUtil.ItemCallback<JournalEntry>() {
        override fun areItemsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
            return oldItem == newItem
        }
    }
}