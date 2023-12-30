package com.nexus.farmap.domain.use_cases

import com.nexus.farmap.domain.pathfinding.Path
import com.nexus.farmap.domain.pathfinding.Pathfinder
import com.nexus.farmap.domain.tree.Tree

class FindWay(
    private val pathfinder: Pathfinder
) {

    suspend operator fun invoke(
        from: String,
        to: String,
        tree: Tree
    ): Path? {
        return pathfinder.findWay(from, to, tree)
    }
}