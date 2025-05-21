package com.example.sunchaser.homeFeature.view.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sunchaser.homeFeature.view.listener.OnHourlyForecastClickListener
import com.example.sunchaser.databinding.ItemHourlyForecastBinding
import com.example.sunchaser.model.weatherPojo.ForecastEntity
import com.example.sunchaser.model.weatherPojo.toHourlyFormat


class HourlyAdapter(private val onForecastClickListener: OnHourlyForecastClickListener) : ListAdapter<ForecastEntity, HourlyAdapter.ForecastViewHolder>(HourlyForecastDiffCallback())
{

    lateinit var binding: ItemHourlyForecastBinding

    class ForecastViewHolder(var binding: ItemHourlyForecastBinding ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val inflater = LayoutInflater.from(parent.context)
         binding = ItemHourlyForecastBinding.inflate(inflater, parent, false)
        return ForecastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int)
    {
        if (position >= hourlyForecasts.size) return // Safety check

        val forecast = hourlyForecasts[position]

        holder.binding.tvHour.text=forecast.dt.toHourlyFormat()
        holder.binding.tvHourlyTemp.text="${forecast.temp.toInt()}°C"
        Glide.with(holder.itemView.context)
            .load("https://openweathermap.org/img/w/${forecast.weatherIcon}.png")
          //  .load(forecast.weatherIcon.let { "https://openweathermap.org/img/w/$it.png" })
            .into(holder.binding.ivHourlyIcon)
    }
    override fun getItemCount(): Int = hourlyForecasts.size

    // Cache hourly forecasts to avoid recomputing
    private var hourlyForecasts: List<ForecastEntity> = emptyList()

    // Update data and limit to 8 forecasts
    override fun submitList(list: List<ForecastEntity>?) {
        super.submitList(list)
        hourlyForecasts = list?.take(8) ?: emptyList()
    }
}

class HourlyForecastDiffCallback:DiffUtil.ItemCallback<ForecastEntity>()
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
