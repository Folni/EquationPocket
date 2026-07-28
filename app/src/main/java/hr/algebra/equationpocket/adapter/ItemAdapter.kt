package hr.algebra.equationpocket.adapter

import android.content.ContentUris
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import hr.algebra.equationpocket.ITEM_POS
import hr.algebra.equationpocket.ItemPagerActivity
import hr.algebra.equationpocket.PRODUCT_PROVIDER_CONTENT_URI
import hr.algebra.equationpocket.R
import hr.algebra.equationpocket.framework.startActivity
import hr.algebra.equationpocket.model.Item
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import java.io.File

class ItemAdapter(
    private val context: Context,
    private val items: MutableList<Item>
) : RecyclerView.Adapter<ItemAdapter.ViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.item, parent, false) // layout 'item.xml' mora postojati
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)

        holder.itemView.setOnClickListener {
            context.startActivity<ItemPagerActivity>(ITEM_POS, position)
        }

        holder.itemView.setOnLongClickListener {
            deleteItem(position)
            true
        }
    }

    private fun deleteItem(position: Int) {
        val item = items[position]
        context.contentResolver.delete(
            ContentUris.withAppendedId(PRODUCT_PROVIDER_CONTENT_URI, item._id!!),
            null,
            null
        )
        if (item.picturePath.isNotEmpty()) {
            File(item.picturePath).delete()
        }

        items.removeAt(position)
        notifyItemRemoved(position) // Bolje nego notifyDataSetChanged za performanse
    }

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val tvItem = itemView.findViewById<TextView>(R.id.tvItem)
        private val ivItem = itemView.findViewById<ImageView>(R.id.ivItem)
        private val tvPrice = itemView.findViewById<TextView>(R.id.tvPrice)
        private val tvRating = itemView.findViewById<TextView>(R.id.tvRating)

        fun bind(item: Item){
            tvItem.text = item.title

            tvPrice?.text = "${item.price} €"
            tvRating?.text = "⭐ ${item.rate} (${item.count})"

            Picasso.get()
                .load(File(item.picturePath))
                .error(R.drawable.placeholder_image)
                .transform(RoundedCornersTransformation(50, 5))
                .into(ivItem)
        }
    }
}