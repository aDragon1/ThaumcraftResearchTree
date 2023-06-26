package com.adragon.thaumcraftresearchtree

object SingletonResearchesInfo {
    lateinit var graph: Graph
    lateinit var researches: MutableList<MainActivity.Research>
    lateinit var mapOfTranslate: MutableMap<String, String>
}

class Graph {
    data class Vertex(val name: String) {
        val neighbors = mutableSetOf<Vertex>()
    }

    private val vertices = mutableMapOf<String, Vertex>()


    private operator fun get(name: String) =
        vertices[name] ?: throw IllegalArgumentException("vertex $name doesn't exist")


    fun getVertices() = vertices

    fun addVertex(name: String) {
        if (!vertices.contains(name))
            vertices[name] = Vertex(name)
    }

    //        The neighbors of base vertex is dependency's
    private fun connect(first: Vertex, second: Vertex) {
        first.neighbors.add(second)
//        second.neighbors.add(first)
    }

    fun connect(from: String, to: String) = connect(this[from], this[to])

    fun neighbors(name: String) = vertices[name]?.neighbors?.map { it.name } ?: listOf()

    @Suppress("unused")
    fun bfs(start: String, finish: String): Int = bfs(this[start], this[finish])

    private fun bfs(start: Vertex, finish: Vertex): Int {
        val queue = ArrayDeque<Vertex>()
        val visited = mutableSetOf<Vertex>()
        val route = mutableListOf<String>()
        queue.add(start)
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            route.add(next.name)
//            if (next == finish) return route
            for (neighbor in next.neighbors) {
                if (neighbor in visited) continue
                visited += neighbor
                queue.add(neighbor)
                if (neighbor == finish) return route.apply { add(finish.name) }.size  // Stop traversal if finish vertex is encountered
            }
        }
        return 0
    }
}