package com.adragon.thaumcraftresearchtree

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class InfoActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var currentItemNameTextView: TextView
    private lateinit var currentItemCategoryTextView: TextView
    private lateinit var toolbar: Toolbar
    private lateinit var toolbarTitleTextView: TextView
    val delim = " -> "

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.info_view)

        currentItemNameTextView = findViewById(R.id.infoNameTextView)
        currentItemCategoryTextView = findViewById(R.id.infoCategoryTextView)
        recyclerView = findViewById(R.id.infoViewRecyclerView)

        toolbar = findViewById(R.id.toolbar)
        toolbarTitleTextView = findViewById(R.id.toolbar_title)
        title = ""
        setSupportActionBar(toolbar)
        toolbar.setBackgroundColor(17170499)

        try {
            /* Get research stuff from singleton object */
            val graph = SingletonResearchesInfo.graph
            val researches = SingletonResearchesInfo.researches

            /*   Map consists of research.name to research.name_ru */
            val mapOfTranslate = SingletonResearchesInfo.mapOfTranslate

            /*  Get clicked item text and set it on textView */
            val nameRu = intent.getStringExtra("clickedName_ru") ?: ""
            currentItemNameTextView.text = mapOfTranslate[nameRu] ?: nameRu

            /* get path (null for first invoke) and set it */
            val pathText = intent.getStringExtra("path") ?: (mapOfTranslate[nameRu] ?: nameRu)
            toolbarTitleTextView.text = pathText

            /* get category and set it */
            val category = researches.find { it.name_ru == nameRu }?.category_ru
            currentItemCategoryTextView.text = category


            /*    Set recyclerView stuff */
            recyclerView.layoutManager = LinearLayoutManager(this)
            val neighbors = graph.neighbors(researches.find { it.name_ru == nameRu }?.name ?: "")
                .ifEmpty { listOf() }
            val translatedNeighbors = mutableListOf<String>()

            neighbors.forEach { translatedNeighbors.add(mapOfTranslate[it] ?: it) }

            val adapter =
                RecyclerViewAdapter(translatedNeighbors.ifEmpty { listOf("Элементов нет") })
            recyclerView.adapter = adapter

            adapter.apply {
                setOnItemClickListener(object : RecyclerViewAdapter.onItemClickListener {
                    override fun onItemClick(position: Int) {
                        if (neighbors.isEmpty()) return
                        val clickedItem = neighbors[position]
                        val intent = Intent(applicationContext, InfoActivity::class.java)
                        intent.putExtra(
                            "clickedName_ru",
                            mapOfTranslate[clickedItem] ?: clickedItem
                        )
                        intent.putExtra(
                            "path",
                            "${toolbarTitleTextView.text} $delim ${mapOfTranslate[clickedItem] ?: clickedItem}"
                        )
                        startActivity(intent)
                    }
                })
                /* TODO Add popup dialog onLongItemClick to show neighbors of longClicked item (like msg in Tg)       */
//                setOnItemLongClickListener(object : RecyclerViewAdapter.onItemLongClickListener {
//                    override fun onItemLongClick(position: Int): Boolean {
//                        if (neighbors.isEmpty()) return false
//                        val clickedItem = neighbors[position]
//                        val clickNeighbors = graph.neighbors(researches.find { clickedItem== nameRu }?.name ?: "")
//                            .ifEmpty { listOf() }
//                        val clickTranslatedNeighbors = mutableListOf<String>()
//
//                        clickNeighbors.forEach { clickTranslatedNeighbors.add(mapOfTranslate[it] ?: it) }
//                        println()
//                        return true
//                    }
//                })
            }
        } catch (e: Exception) {
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = RecyclerViewAdapter(listOf(e.toString()))
        }
    }
}