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
import com.example.sunchaser.model.weatherPojo.Settings
import com.example.sunchaser.model.weatherPojo.SettingsManager


/*class CurrentDayAdapter (private val onCurrentDayClickListener: OnCurrentDayClickListener):ListAdapter<ForecastEntity,CurrentDayAdapter.ForecastViewHolder>(CurrDailyForecastDiffCallback())
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

}*/

class CurrentDayAdapter(private val onCurrentDayClickListener: OnCurrentDayClickListener) : ListAdapter<ForecastEntity, CurrentDayAdapter.ForecastViewHolder>(CurrDailyForecastDiffCallback()) {
    lateinit var binding: ItemCurrentDayBinding

    class ForecastViewHolder(val binding: ItemCurrentDayBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        binding = ItemCurrentDayBinding.inflate(inflater, parent, false)
        return ForecastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        val forecast = currentForecast ?: return // Safety check
        val settings = SettingsManager.currentSettings.value ?: Settings()

        // Map temperature unit
        val tempUnit = when (settings.temperatureUnit) {
            "Kelvin" -> "K"
            "Fahrenheit" -> "°F"
            else -> "°C"
        }

        // Convert temperature (assuming API returns Kelvin)
//        val displayTemp = when (settings.temperatureUnit) {
//            "Celsius" -> forecast.temp - 273.15 // Kelvin to Celsius
//            "Fahrenheit" -> (forecast.temp - 273.15) * 9 / 5 + 32 // Kelvin to Fahrenheit
//            else -> forecast.temp // Kelvin
//        }
        val displayTemp = forecast.temp


        // Map wind speed unit
        val windUnit = if (settings.windSpeedUnit == "miles/hour") "mph" else "m/s"
        // Convert wind speed (assuming API returns meters/sec)
        val displayWindSpeed = if (settings.windSpeedUnit == "miles/hour") {
            forecast.windSpeed * 2.23694 // meters/sec to miles/hour
        } else {
            forecast.windSpeed // meters/sec
        }

        holder.binding.tvTemperature.text = "${displayTemp.toInt()}$tempUnit"
        holder.binding.tvWeatherDesc.text = forecast.weatherDescription.replaceFirstChar { it.uppercase() }
        holder.binding.tvHumidity.text = "${forecast.humidity}%"
        holder.binding.tvWindSpeed.text = "${displayWindSpeed.toInt()} $windUnit"
        holder.binding.tvPressure.text = "${forecast.pressure} hPa"
        Glide.with(holder.itemView.context)
            .load("https://openweathermap.org/img/w/${forecast.weatherIcon}.png")
            .into(holder.binding.ivWeatherIcon)
    }

    override fun getItemCount(): Int = if (currentForecast != null) 1 else 0

    // Cache single forecast to avoid recomputing
    private var currentForecast: ForecastEntity? = null

    override fun submitList(list: List<ForecastEntity>?) {
        super.submitList(list)
        currentForecast = list?.firstOrNull()
    }
}

class CurrDailyForecastDiffCallback : DiffUtil.ItemCallback<ForecastEntity>() {
    override fun areItemsTheSame(oldItem: ForecastEntity, newItem: ForecastEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ForecastEntity, newItem: ForecastEntity): Boolean {
        return oldItem.dt == newItem.dt &&
                oldItem.temp == newItem.temp &&
                oldItem.weatherIcon == newItem.weatherIcon &&
                oldItem.weatherDescription == newItem.weatherDescription
    }
}


