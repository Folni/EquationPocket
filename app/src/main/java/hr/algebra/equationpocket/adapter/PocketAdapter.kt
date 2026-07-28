package hr.algebra.equationpocket.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import hr.algebra.equationpocket.R
import hr.algebra.equationpocket.model.Pocket

class PocketAdapter(
    private var pockets: List<Pocket>,
    private val onPocketClick: (Pocket) -> Unit,
    private val onPocketLongClick: (Pocket) -> Unit
) : RecyclerView.Adapter<PocketAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.pocket_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pocket = pockets[position]
        holder.bind(pocket)
        holder.itemView.setOnClickListener { onPocketClick(pocket) }
        holder.itemView.setOnLongClickListener {
            onPocketLongClick(pocket)
            true
        }
    }

    override fun getItemCount(): Int = pockets.size

    fun updateData(newPockets: List<Pocket>) {
        pockets = newPockets
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPocketName: TextView = itemView.findViewById(R.id.tvPocketName)

        fun bind(pocket: Pocket) {
            tvPocketName.text = pocket.name
        }
    }
}
