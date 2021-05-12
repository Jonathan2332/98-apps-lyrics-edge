package a98apps.lyricsedge.view.adapter

import a98apps.lyricsedge.R
import a98apps.lyricsedge.constants.Api
import a98apps.lyricsedge.databinding.FragmentCacheItemBinding
import a98apps.lyricsedge.model.CacheManager
import a98apps.lyricsedge.view.listener.RecyclerClickListener
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import kotlin.collections.ArrayList

class LyricCacheRecyclerViewAdapter(private val values: MutableList<CacheManager>, private val listener: RecyclerClickListener) : RecyclerView.Adapter<LyricCacheRecyclerViewAdapter.ViewHolder>(), Filterable
{
    private var valuesFull: List<CacheManager> = values.toList()

    private val selectedItems: MutableList<Int> = mutableListOf()
    private val selectedCacheIds: MutableList<String> = mutableListOf()
    private val selectedMusicIds: MutableList<String> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        return ViewHolder(FragmentCacheItemBinding.inflate(LayoutInflater.from(parent.context), parent, false), listener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int)
    {
        val item = values[position]
        holder.id.text = item.id.toString()
        holder.musicName.text = item.musicName
        holder.artistName.text = item.artistName
        holder.type.text = when(item.type)
        {
            Api.RESULT_TYPE_EXACT -> holder.type.context.getString(R.string.text_exact)
            Api.RESULT_TYPE_APPROX -> holder.type.context.getString(R.string.text_approximate)
            else -> holder.type.context.getString(android.R.string.unknownName)
        }

        if(selectedItems.contains(position))
        {
            val context = holder.root.context
            holder.root.setBackgroundColor(context.getColor(R.color.blue_selected_item))
        }
        else holder.root.addRipple()

    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(binding: FragmentCacheItemBinding, private val listener: RecyclerClickListener) : RecyclerView.ViewHolder(binding.root), View.OnLongClickListener, View.OnClickListener
    {
        val root = binding.itemRoot
        val id: TextView = binding.itemNumber
        val musicName: TextView = binding.itemName
        val artistName: TextView = binding.itemArtist
        val type: TextView = binding.itemType

        init
        {
            binding.root.setOnClickListener(this)
            binding.root.setOnLongClickListener(this)
        }

        override fun onClick(v: View?)
        {
            val cacheId = values[absoluteAdapterPosition].id.toString()
            val musicId = values[absoluteAdapterPosition].musicId.toString()
            listener.onItemClick(absoluteAdapterPosition, cacheId, musicId)
        }

        override fun onLongClick(v: View?): Boolean
        {
            val cacheId = values[absoluteAdapterPosition].id.toString()
            val musicId = values[absoluteAdapterPosition].musicId.toString()
            listener.onItemLongClick(absoluteAdapterPosition, cacheId, musicId)
            return true
        }
    }

    private fun View.addRipple() = with(TypedValue()) {
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
        setBackgroundResource(resourceId)
    }

    fun handleSelectedItem(position: Int, cacheId: String, musicId: String)
    {
        if(!selectedItems.contains(position))
        {
            selectedItems.add(position)
            selectedCacheIds.add(cacheId)
            selectedMusicIds.add(musicId)
            notifyItemChanged(position)
        }
        else
        {
            selectedItems.remove(position)
            selectedCacheIds.remove(cacheId)
            selectedMusicIds.remove(musicId)
            notifyItemChanged(position)
        }
    }

    //returns the only item selected for sharing
    fun getSelectedItem(): CacheManager
    {
        return values[selectedItems[0]]
    }

    fun getSelectedCacheIds(): MutableList<String>
    {
        return selectedCacheIds
    }

    fun getSelectedMusicIds(): MutableList<String>
    {
        return selectedMusicIds
    }

    fun selectAllItems()
    {
        selectedItems.clear()
        selectedCacheIds.clear()
        selectedMusicIds.clear()

        for(i in 0 until itemCount)
        {
            selectedItems.add(i)
            selectedCacheIds.add(values[i].id.toString())
            selectedMusicIds.add(values[i].musicId.toString())
        }
    }

    fun selectedItemsCount(): Int
    {
        return selectedItems.size
    }

    fun clearSelectedItems()
    {
        selectedItems.clear()
        selectedCacheIds.clear()
        selectedMusicIds.clear()
        values.clear()
        values.addAll(valuesFull.toMutableList())
        notifyDataSetChanged()
    }

    fun updateItems(newValues: MutableList<CacheManager>)
    {
        values.clear()
        values.addAll(newValues)
        valuesFull = newValues.toList()
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter
    {
        return searchFilter
    }

    private val searchFilter: Filter = object : Filter()
    {
        override fun performFiltering(constraint: CharSequence?): FilterResults
        {
            val filteredResult: MutableList<CacheManager> = ArrayList()
            if(constraint == null || constraint.isEmpty())
            {
                filteredResult.addAll(valuesFull)
            }
            else
            {
                val locale = Locale.getDefault()
                val pattern = constraint.toString().lowercase(locale).trim()

                for(cache in valuesFull)
                {
                    if(cache.artistName?.lowercase(locale)?.contains(pattern) == true || cache.musicName?.lowercase(locale)?.contains(pattern) == true)
                    {
                        filteredResult.add(cache)
                    }
                }
            }

            val filterResults = FilterResults()
            filterResults.values = filteredResult

            return filterResults
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?)
        {
            if(results?.values != null)
            {
                values.clear()
                @Suppress("UNCHECKED_CAST")
                val filteredResult: List<CacheManager> = results.values as List<CacheManager>
                values.addAll(filteredResult)
                notifyDataSetChanged()
            }
        }
    }
}