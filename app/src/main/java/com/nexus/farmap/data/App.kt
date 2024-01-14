package com.nexus.farmap.data

import android.app.Application
import androidx.room.Room
import com.nexus.farmap.data.data_source.GraphDatabase
import com.nexus.farmap.data.ml.classification.TextAnalyzer
import com.nexus.farmap.data.pathfinding.AStarImpl
import com.nexus.farmap.data.repository.GraphImpl
import com.nexus.farmap.domain.tree.Tree
import com.nexus.farmap.domain.use_cases.*

class App : Application() {
    private lateinit var database: GraphDatabase

    private lateinit var repository: GraphImpl
    private lateinit var tree: Tree

    lateinit var findWay: FindWay

    private lateinit var pathfinder: AStarImpl

    lateinit var hitTest: HitTest

    private lateinit var objectDetector: TextAnalyzer
    lateinit var analyzeImage: AnalyzeImage

    lateinit var getDestinationDesc: GetDestinationDesc

    lateinit var smoothPath: SmoothPath

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = Room.databaseBuilder(this, GraphDatabase::class.java, DATABASE_NAME)
            //.createFromAsset(DATABASE_DIR)
            .allowMainThreadQueries().build()

        repository = GraphImpl()
        tree = Tree(repository)

        smoothPath = SmoothPath()

        pathfinder = AStarImpl()
        findWay = FindWay(pathfinder)

        hitTest = HitTest()

        objectDetector = TextAnalyzer()
        analyzeImage = AnalyzeImage(objectDetector)

        getDestinationDesc = GetDestinationDesc()

    }

    fun getDatabase(): GraphDatabase {
        return database
    }

    fun getTree(): Tree {
        return tree
    }

    companion object {
        var instance: App? = null
        const val DATABASE_NAME = "nodes"
        const val DATABASE_DIR = "database/nodes.db"
        const val ADMIN_MODE = "ADMIN"
        const val USER_MODE = "USER"
        var mode = ADMIN_MODE
    }
}