package com.nexus.farmap.domain.pathfinding

import com.nexus.farmap.domain.tree.Tree

interface Pathfinder {

    suspend fun findWay(
        from: String, to: String, tree: Tree
    ): Path?

}