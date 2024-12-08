import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fruitrecognitionapp.ListItem
import com.example.fruitrecognitionapp.R

class NutritionAdapter(private val items: List<ListItem>) : RecyclerView.Adapter<NutritionAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.itemTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d("NutritionAdapter", "Binding item: ${items[position]}")
        holder.textView.text = items[position].text
    }

    override fun getItemCount(): Int = items.size
}
