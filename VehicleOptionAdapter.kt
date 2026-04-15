package com.example.letsgo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.letsgo.models.VehicleFareItem

class VehicleOptionAdapter(
    private var items: List<VehicleFareItem>,
    private val onClick: (VehicleFareItem) -> Unit
) : RecyclerView.Adapter<VehicleOptionAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvVehicleName)
        val tvFare: TextView = view.findViewById(R.id.tvVehicleFare)
        init {
            view.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onClick(items[pos])
                }
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        // inflate a small item layout — create this layout (below) if you don't have one
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_vehicle_option, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val it = items[position]
        holder.tvName.text = it.vehicle.name
        // use totalFare from FareBreakdown (rounded)
        holder.tvFare.text = "₹${"%.0f".format(it.fare.totalFare)}"
    }

    override fun getItemCount(): Int = items.size

    // called from activity to update list
    fun updateData(newList: List<VehicleFareItem>) {
        items = newList
        notifyDataSetChanged()
    }
}
