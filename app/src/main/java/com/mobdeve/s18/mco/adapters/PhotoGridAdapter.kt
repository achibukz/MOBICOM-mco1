package com.mobdeve.s18.mco.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mobdeve.s18.mco.databinding.ItemPhotoThumbBinding
import com.mobdeve.s18.mco.models.EntryPhoto

class PhotoGridAdapter(
    private val onRemovePhoto: (EntryPhoto) -> Unit
) : ListAdapter<EntryPhoto, PhotoGridAdapter.PhotoViewHolder>(PhotoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoThumbBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PhotoViewHolder(
        private val binding: ItemPhotoThumbBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: EntryPhoto) {
            Glide.with(binding.ivPhoto.context)
                .load(photo.uriString) // FIX: Use uriString (Glide handles this automatically)
                .centerCrop()
                .into(binding.ivPhoto)

            binding.btnRemove.setOnClickListener {
                onRemovePhoto(photo)
            }
        }
    }

    private class PhotoDiffCallback : DiffUtil.ItemCallback<EntryPhoto>() {
        override fun areItemsTheSame(oldItem: EntryPhoto, newItem: EntryPhoto): Boolean {
            // FIX: Compare the string properties
            return oldItem.uriString == newItem.uriString
        }

        override fun areContentsTheSame(oldItem: EntryPhoto, newItem: EntryPhoto): Boolean {
            return oldItem == newItem
        }
    }
}