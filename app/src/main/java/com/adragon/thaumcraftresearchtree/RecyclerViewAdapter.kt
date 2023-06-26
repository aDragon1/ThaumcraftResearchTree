package com.adragon.thaumcraftresearchtree

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

@Suppress("ClassName")
class RecyclerViewAdapter(private var names: List<String>) :
    RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder>() {

    private var mListener: onItemClickListener = object : onItemClickListener {}
    private var mLongListener: onItemLongClickListener = object : onItemLongClickListener {}

    interface onItemClickListener {
        fun onItemClick(position: Int) {

        }
    }

    interface onItemLongClickListener {
        fun onItemLongClick(position: Int): Boolean {

            return true
        }
    }

    fun setOnItemClickListener(listener: onItemClickListener) {
        mListener = listener
    }

    @Suppress("unused")
    fun setOnItemLongClickListener(listener: onItemLongClickListener) {
        mLongListener = listener
    }

    class MyViewHolder(
        itemView: View,
        listener: onItemClickListener,
        longListener: onItemLongClickListener,
    ) :
        RecyclerView.ViewHolder(itemView) {
        val itemTextView: TextView = itemView.findViewById(R.id.itemTextView)

        init {
            itemView.setOnLongClickListener { longListener.onItemLongClick(adapterPosition) }
            itemView.setOnClickListener { listener.onItemClick(adapterPosition) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_view, parent, false)
        return MyViewHolder(itemView, mListener, mLongListener)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.itemTextView.text = names[position]
    }

    override fun getItemCount(): Int = names.size

    /*
        Todo
          Remove notifyDataSetChanged and do smt like that example
            https://stackoverflow.com/questions/30398247/how-to-filter-a-recyclerview-with-a-searchview
     */
    @SuppressLint("NotifyDataSetChanged")
    fun update(list: List<String>) {
        names = list
        notifyDataSetChanged()
    }

}