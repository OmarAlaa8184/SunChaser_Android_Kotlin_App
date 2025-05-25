package com.example.sunchaser.alertFeature.view.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sunchaser.databinding.ItemAlertBinding
import com.example.sunchaser.model.weatherPojo.Alert
import java.util.Date


class AlertsAdapter(private val onToggle: (Int, Boolean) -> Unit, private val onDeleteClick: (Alert) -> Unit):
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
        holder.binding.tvTimeRange.text="${Date(alert.startTime)} to ${Date(alert.endTime)}"
        holder.binding.tvAlertType.text= when (alert.alertType)
        {
            Alert.AlertType.NOTIFICATION -> "Notification"
            Alert.AlertType.ALARM_SOUND -> "Alarm Sound"
        }
//        holder.binding.switchActive.isChecked = alert.isActive
//
//        holder.binding.switchActive.setOnCheckedChangeListener { _, isChecked ->
//            onToggle(alert.id, isChecked)
//        }

        holder.binding.btnDelete.setOnClickListener {
            onDeleteClick(alert)
        }


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
