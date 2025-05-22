package com.example.sunchaser.homeFeature.view.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sunchaser.databinding.ItemStatisticBinding
import com.example.sunchaser.model.weatherPojo.StatisticItem


class StatisticsAdapter : ListAdapter<StatisticItem, StatisticsAdapter.ViewHolder>(StatisticDiffCallback())
{

     lateinit var binding: ItemStatisticBinding

    class ViewHolder(val binding: ItemStatisticBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        val inflater = LayoutInflater.from(parent.context)
        binding = ItemStatisticBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            tvTitle.text = item.title
            tvValue.text = item.value
        }
    }
}

class StatisticDiffCallback : DiffUtil.ItemCallback<StatisticItem>()
{
    override fun areItemsTheSame(oldItem: StatisticItem, newItem: StatisticItem): Boolean
    {
        return oldItem.title == newItem.title
    }

    override fun areContentsTheSame(oldItem: StatisticItem, newItem: StatisticItem): Boolean {
        return oldItem == newItem
    }

}
