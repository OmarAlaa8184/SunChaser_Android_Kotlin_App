package com.example.sunchaser.alertFeature.view.adapters

import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sunchaser.alertFeature.viewmodel.AlertViewModel
import com.example.sunchaser.databinding.ItemAlertBinding
import com.example.sunchaser.model.weatherPojo.Alert
import com.example.sunchaser.model.weatherPojo.Settings
import com.example.sunchaser.model.weatherPojo.SettingsManager
import java.util.Date
import java.util.Locale


class AlertsAdapter(private val viewModel: AlertViewModel, private val onToggle: (Int, Boolean) -> Unit, private val onDeleteClick: (Alert) -> Unit):
    ListAdapter<Alert, AlertsAdapter.ViewHolder>(AlertDiffCallback())
{

    lateinit var binding: ItemAlertBinding

    class ViewHolder(val binding: ItemAlertBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): ViewHolder
    {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        binding = ItemAlertBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int)
    {
        val alert = getItem(position)
        val context=holder.itemView.context
        /*holder.binding.tvLocation.text= "Date: ${alert.date}"
        holder.binding.tvTimeRange.text="From ${Date(alert.startTime)} to ${Date(alert.endTime)}"
        holder.binding.tvAlertType.text="Type: ${alert.type.name}"
        holder.binding.switchActive.isChecked = alert.isActive

        holder.binding.btnDelete.setOnClickListener{
            onDeleteClick(alert.id)
        }
        holder.binding.switchActive.setOnCheckedChangeListener { _, isChecked ->
            onToggle(alert.id, isChecked)
        }*/

        holder.binding.tvLocation.text=alert.locationName
        holder.binding.tvTimeRange.text="${SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(alert.startTime))} " +
                "to ${SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(alert.endTime))}"

        holder.binding.tvAlertType.text= when (alert.alertType)
        {
            Alert.AlertType.NOTIFICATION -> "Notification"
            Alert.AlertType.ALARM_SOUND -> "Alarm Sound"
        }
        viewModel.temperatures.observe(context as LifecycleOwner) { tempMap ->
            val temp = tempMap[alert.id] ?: 0f
            // Get temperature unit from settings
            val settings = SettingsManager.currentSettings.value ?: Settings()
            val unitLabel = when (settings.temperatureUnit) {
                "Kelvin" -> "K"
                "Fahrenheit" -> "°F"
                else -> "°C"
            }
            holder.binding.tvLocation.text = "${alert.locationName}, ${temp.toInt()}$unitLabel"
        }

        holder.binding.btnDelete.setOnClickListener {
            onDeleteClick(alert)
        }
//        holder.binding.switchActive.isChecked = alert.isActive
//
//        holder.binding.switchActive.setOnCheckedChangeListener { _, isChecked ->
//            onToggle(alert.id, isChecked)
//        }




    }
}


class AlertDiffCallback : DiffUtil.ItemCallback<Alert>() {

    override fun areItemsTheSame(oldItem: Alert, newItem: Alert): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Alert, newItem: Alert): Boolean {
        return oldItem == newItem
    }
}
