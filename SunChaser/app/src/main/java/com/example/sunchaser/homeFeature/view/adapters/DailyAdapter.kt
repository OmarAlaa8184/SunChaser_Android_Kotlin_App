package com.example.sunchaser.homeFeature.view.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sunchaser.homeFeature.view.listener.OnDailyClickListener
import com.example.sunchaser.databinding.ItemDailyForecastBinding
import com.example.sunchaser.model.weatherPojo.DailyForecast
import com.example.sunchaser.model.weatherPojo.ForecastEntity
import com.example.sunchaser.model.weatherPojo.toDailyForecasts



class DailyAdapter(private val onDailyClickListener: OnDailyClickListener ):ListAdapter<ForecastEntity,DailyAdapter.ForecastViewHolder>(DailyForecastDiffCallback())
{
    lateinit var binding: ItemDailyForecastBinding

    class ForecastViewHolder(var binding:ItemDailyForecastBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder
    {
        val inflater: LayoutInflater =parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding=ItemDailyForecastBinding.inflate(inflater,parent,false)
        return ForecastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int)
    {
        val dailyForecasts=currentList.toDailyForecasts()

        if (position >= dailyForecasts.size) return  // Safety check

        val dailyForecast=dailyForecasts[position]

        holder.binding.tvDay.text=dailyForecast.date
        holder.binding.tvDailyTemp.text="${dailyForecast.avgTemp.toInt()}°C"
        Glide.with(holder.itemView.context)
            .load("https://openweathermap.org/img/w/${dailyForecast.weather.icon}.png")
            .into(holder.binding.ivDailyIcon)
    }

    override fun getItemCount(): Int
    {
        return currentList.toDailyForecasts().size
    }

    // Cache daily forecasts to avoid recomputing in onBindViewHolder
    private var dailyForecasts: List<DailyForecast> = emptyList()

    // Update data and compute daily forecasts
    override fun submitList(list: List<ForecastEntity>?) {
        super.submitList(list)
        dailyForecasts = list?.toDailyForecasts() ?: emptyList()
    }

}

class DailyForecastDiffCallback:DiffUtil.ItemCallback<ForecastEntity>()
{
    override fun areItemsTheSame(oldItem: ForecastEntity, newItem: ForecastEntity): Boolean {

        return oldItem.id==newItem.id
    }

    override fun areContentsTheSame(oldItem: ForecastEntity, newItem: ForecastEntity): Boolean {
       // return oldItem==newItem

        return oldItem.dt == newItem.dt &&
                oldItem.temp == newItem.temp &&
                oldItem.weatherIcon == newItem.weatherIcon &&
                oldItem.weatherDescription == newItem.weatherDescription
    }

}

