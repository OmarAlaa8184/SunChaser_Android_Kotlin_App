package com.example.sunchaser.favoriteFeature.view.adapters


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sunchaser.databinding.ItemFavoriteBinding
import com.example.sunchaser.model.weatherPojo.ForecastEntity

class FavoriteAdapter(private val onDeleteClick: (ForecastEntity) -> Unit ,private val onItemClick: (ForecastEntity) -> Unit):
    ListAdapter<ForecastEntity, FavoriteAdapter.ViewHolder>(FavoriteDiffCallback())
{
        lateinit var binding:ItemFavoriteBinding

     class ViewHolder(val binding: ItemFavoriteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        val inflater:LayoutInflater=LayoutInflater.from(parent.context)
        binding = ItemFavoriteBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int)
    {
        val forecast = getItem(position)
        holder.binding.tvCity.text="${forecast.cityName}, ${forecast.country}"
        holder.binding.tvCoordinates.text="Lat: ${forecast.latitude}, Lon: ${forecast.longitude}"
        holder.binding.btnDelete.setOnClickListener{
            onDeleteClick(forecast)
        }
       holder.binding.root.setOnClickListener{
           onItemClick(forecast)
      }

    }
}

class FavoriteDiffCallback : DiffUtil.ItemCallback<ForecastEntity>() {
    override fun areItemsTheSame(oldItem: ForecastEntity, newItem: ForecastEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ForecastEntity, newItem: ForecastEntity): Boolean {
        return oldItem == newItem
    }
}