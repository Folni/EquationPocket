package hr.algebra.equationpocket.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import hr.algebra.equationpocket.R
import hr.algebra.equationpocket.model.Equation

class EquationAdapter(
    private var equations: List<Equation>,
    private val onEquationClick: (Equation) -> Unit,
    private val onEquationLongClick: (Equation) -> Unit
) : RecyclerView.Adapter<EquationAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.equation_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val equation = equations[position]
        holder.bind(equation)
        holder.itemView.setOnClickListener { onEquationClick(equation) }
        holder.itemView.setOnLongClickListener {
            onEquationLongClick(equation)
            true
        }
    }

    override fun getItemCount(): Int = equations.size

    fun updateData(newEquations: List<Equation>) {
        equations = newEquations
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvEquationName)
        private val tvFormula: TextView = itemView.findViewById(R.id.tvEquationFormula)
        private val ivFavorite: android.widget.ImageView = itemView.findViewById(R.id.ivFavorite)

        fun bind(equation: Equation) {
            tvName.text = equation.name
            tvFormula.text = equation.formula
            ivFavorite.visibility = if (equation.isFavorite) View.VISIBLE else View.GONE
        }
    }
}
