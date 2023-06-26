@file:Suppress("SameParameterValue")

package com.adragon.thaumcraftresearchtree

import android.content.Intent
import android.content.res.AssetManager
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    data class Research(
        var key: String,
        var name: String,
        var name_ru: String,
        var dependencies: MutableList<String>,
        var dependencies_ru: MutableList<String>,
        var category: String,
        var category_ru: String,
    )

    private lateinit var recyclerView: RecyclerView
    private lateinit var editText: EditText
    private val pathToResearches = "allResearchStuff/researches"
    private val pathToTranslates = "allResearchStuff/researches"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recyclerView = findViewById(R.id.recyclerView)
        editText = findViewById(R.id.editText)

        /*
         Todo
          Somehow to get away from singleton (or learn how to use it) (make Graph class(obj?) parceble),
            also - reorganize whole stuff in case to free researches list
                and pass to InfoActivity only graph or only neighbors list
         */
        /*
          TODO
            Store graph, researches(?) and translatedMap in somewhere in case of better performance
                (Recalculate everything on app start is kinda slow)
         */
        SingletonResearchesInfo.mapOfTranslate = mutableMapOf()
        SingletonResearchesInfo.graph = Graph()
        SingletonResearchesInfo.researches = mutableListOf()

        val graph = SingletonResearchesInfo.graph
        val researches = SingletonResearchesInfo.researches

        processFilesInFolder(assets, pathToResearches, researches)
        translateResearches(assets, pathToTranslates, researches)

        getGraph(researches, graph)

        val map = SingletonResearchesInfo.mapOfTranslate

        /*
         1) Create list named keys
            (it contains translated values,
                if map contains current graph.vertices.key (always contain),
                    else find current g.v.key in researches list and add research.key
                        if list don't contains it (g.v.key) - just add it to list )

        2) Create list named editedList,
             which contains value of keys list and
                it changes when editText changes ( when user search smt)
         */
        val editedList = mutableListOf<String>()
        val keys = graph.getVertices().keys.toList()
            .map { map[it] ?: (researches.find { curRes -> curRes.name == it }?.key ?: it) }
        editedList.addAll(keys)

        /* TODO
            Sort adapter list based on bfs-value (from current to "gotthaumonomicon") */

        val adapter = RecyclerViewAdapter(keys)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        adapter.setOnItemClickListener(object : RecyclerViewAdapter.onItemClickListener {
            override fun onItemClick(position: Int) {
                val intent = Intent(applicationContext, InfoActivity::class.java)
                intent.putExtra("clickedName_ru", editedList[position])
                startActivity(intent)
            }
        })

        /*
        Update editedList when user search smt and updater adapter.values
         */
        editText.addTextChangedListener {
            val eText = editText.text.toString().lowercase()
            editedList.clear()
            editedList.addAll(keys.filter { it.lowercase().contains(eText) })
            adapter.update(editedList)
        }
    }

    private fun translateResearches(
        am: AssetManager,
        folderPath: String,
        researches: List<Research>,
    ) = try {

        fun getTranslateName(
            map: MutableMap<String, String>,
            name: String,
            fileLines: List<String>,
        ): String {
            if (map.contains(name)) return map[name]!!
            val info = fileLines.firstOrNull { fl -> fl.contains(name) }
            val nameRu = (info?.split('=')?.getOrNull(1) ?: "").trim()
            if (nameRu.isNotBlank()) map[name] = nameRu
            return nameRu
        }

        fun getTranslateCategory(
            map: MutableMap<String, String>,
            category: String,
            fileLines: List<String>,
        ): String {
            if (map.contains(category)) return map[category]!!
            val info = fileLines.firstOrNull { fl -> fl.contains("tc.research_category.$category") }
            val nameRu = (info?.split('=')?.getOrNull(1) ?: "").trim()
            if (nameRu.isNotBlank()) map[category] = nameRu
            return nameRu
        }

        am.list(folderPath)?.forEach { filePath ->
            val stream = am.open("$folderPath/$filePath")
            val buffer = ByteArray(stream.available())

            stream.read(buffer)
            stream.close()

            val alreadyTranslatedMap = SingletonResearchesInfo.mapOfTranslate

            val fileLines = String(buffer).split('\n')

            researches.forEach {
                it.name_ru = getTranslateName(alreadyTranslatedMap, it.name, fileLines)

                it.dependencies.forEach { dep ->
                    it.dependencies_ru.add(getTranslateName(alreadyTranslatedMap, dep, fileLines))
                }
                it.category_ru = getTranslateCategory(alreadyTranslatedMap, it.category, fileLines)
            }
        }
        researches.filter { it.name_ru.isBlank() }.forEach { it.name_ru = it.key }
    } catch (e: Exception) {
        println("$e $folderPath")
    }

    private fun processFilesInFolder(
        am: AssetManager,
        folderPath: String,
        resultList: MutableList<Research>,
    ) {

        fun parseFileDirToResearch(am: AssetManager, filePath: String): List<Research> {
            fun getResearch(entry: JSONObject): Research {
                val setToRemove =
                    setOf('1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '!', '~', '@')

                fun String.removeAll(charactersToRemove: Set<Char>): String =
                    filterNot { charactersToRemove.contains(it) }

                fun check(key: String): String = try {
                    entry.getString(key).removeAll(setToRemove)
                } catch (e: org.json.JSONException) {
                    ""
                }

                val key = check("key").trim()
                val name = check("name").trim()
                val category = check("category")

                val parents = try {
                    entry.getJSONArray("parents")
                } catch (e: org.json.JSONException) {
                    JSONArray()
                }
                val parentsList = mutableListOf<String>()
                for (j in 0 until parents.length())
                    parentsList.add(parents.getString(j).removeAll(setToRemove).trim())

                return Research(key, name, "", parentsList, mutableListOf(), category, "")
            }

            val list = mutableListOf<Research>()
            try {
                //  Create open stream on json file and read it to buffer
                val stream = am.open(filePath)
                val buffer = ByteArray(stream.available())

                stream.read(buffer)
                stream.close()

                val json = JSONObject(String(buffer))
                val entries = json.getJSONArray("entries")

                for (i in 0 until entries.length()) {
                    val currentObj = entries.getJSONObject(i)
                    val research = getResearch(currentObj)
                    if (research.name.isBlank() && research.key.isBlank()) continue
                    list.add(research)
                }
            } catch (e: Exception) {
                println("$e $filePath")
            }
            return list
        }

        fun updateResearchDependencies(researches: List<Research>) =
            researches.forEach { curRes ->
                for (i in curRes.dependencies.indices)
                    curRes.dependencies[i] =
                        researches.find { it.key == curRes.dependencies[i] }?.name
                            ?: curRes.dependencies[i]
            }


        am.list(folderPath)?.forEach {
            val filePath = if (folderPath.isNotEmpty()) "$folderPath/$it" else it
            if (it.contains('.'))
                resultList.addAll(parseFileDirToResearch(am, filePath))
            else processFilesInFolder(am, filePath, resultList)
        }
        updateResearchDependencies(resultList)

    }

    private fun getGraph(researchList: List<Research>, g: Graph) {
        g.run {
            researchList.forEach {
                addVertex(it.name)
                it.dependencies.forEach { depIt ->
                    addVertex(depIt)
                    connect(it.name, depIt)
                }
            }
        }
    }
}