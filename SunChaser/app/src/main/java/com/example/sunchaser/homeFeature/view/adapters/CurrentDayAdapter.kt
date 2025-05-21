package com.example.sunchaser.homeFeature.view.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sunchaser.databinding.ItemCurrentDayBinding
import com.example.sunchaser.homeFeature.view.listener.OnCurrentDayClickListener
import com.example.sunchaser.model.weatherPojo.ForecastEntity


class CurrentDayAdapter (private val onCurrentDayClickListener: OnCurrentDayClickListener):ListAdapter<ForecastEntity,CurrentDayAdapter.ForecastViewHolder>(CurrDailyForecastDiffCallback())
{

    lateinit var binding: ItemCurrentDayBinding

    class ForecastViewHolder(val binding: ItemCurrentDayBinding) : RecyclerView.ViewHolder(binding.root)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val inflater = LayoutInflater.from(parent.context)
         binding = ItemCurrentDayBinding.inflate(inflater, parent, false)
        return ForecastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        val forecast = currentForecast ?: return // Safety check

        holder.binding.tvTemperature.text="${forecast.temp.toInt()}°C"
        holder.binding.tvWeatherDesc.text= forecast.weatherDescription.replaceFirstChar { it.uppercase() }
        holder.binding.tvHumidity.text="${forecast.humidity}%"
        holder.binding.tvWindSpeed.text = "${forecast.windSpeed} km/h"
        holder.binding.tvPressure.text="${forecast.pressure} hPa"
        Glide.with(holder.itemView.context)
            .load("https://openweathermap.org/img/w/${forecast.weatherIcon}.png")
            .into(holder.binding.ivWeatherIcon)

    }
    override fun getItemCount(): Int = if (currentForecast != null) 1 else 0
    // Cache single forecast to avoid recomputing
    private var currentForecast: ForecastEntity? = null
    // Update data and limit to one forecast
    override fun submitList(list: List<ForecastEntity>?) {
        super.submitList(list)
        currentForecast = list?.firstOrNull()
    }

}

class CurrDailyForecastDiffCallback: DiffUtil.ItemCallback<ForecastEntity>()
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


