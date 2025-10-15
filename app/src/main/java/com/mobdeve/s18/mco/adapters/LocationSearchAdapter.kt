package com.mobdeve.s18.mco.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s18.mco.R
import com.mobdeve.s18.mco.utils.LocationSearchUtils

class LocationSearchAdapter(
    private val onLocationClick: (LocationSearchUtils.Companion.SearchResult) -> Unit
) : ListAdapter<LocationSearchUtils.Companion.SearchResult, LocationSearchAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_location_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLocationName: TextView = itemView.findViewById(R.id.tvLocationName)
        private val tvLocationDetails: TextView = itemView.findViewById(R.id.tvLocationDetails)

        fun bind(searchResult: LocationSearchUtils.Companion.SearchResult) {
            tvLocationName.text = searchResult.name.ifBlank { "Unknown Location" }
            tvLocationDetails.text = searchResult.displayName

            itemView.setOnClickListener {
                onLocationClick(searchResult)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<LocationSearchUtils.Companion.SearchResult>() {
        override fun areItemsTheSame(
            oldItem: LocationSearchUtils.Companion.SearchResult,
            newItem: LocationSearchUtils.Companion.SearchResult
        ): Boolean {
            return oldItem.latitude == newItem.latitude && oldItem.longitude == newItem.longitude
        }

        override fun areContentsTheSame(
            oldItem: LocationSearchUtils.Companion.SearchResult,
            newItem: LocationSearchUtils.Companion.SearchResult
        ): Boolean {
            return oldItem == newItem
        }
    }
}
