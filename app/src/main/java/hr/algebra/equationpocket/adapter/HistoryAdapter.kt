package hr.algebra.equationpocket.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import hr.algebra.equationpocket.R
import hr.algebra.equationpocket.model.HistoryEntry

class HistoryAdapter(
    private val historyList: List<HistoryEntry>
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInputs: TextView = view.findViewById(R.id.tvHistoryInputs)
        val tvResult: TextView = view.findViewById(R.id.tvHistoryResult)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.history_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = historyList[position]
        holder.tvInputs.text = entry.inputs
        holder.tvResult.text = entry.result
    }

    override fun getItemCount(): Int = historyList.size
}