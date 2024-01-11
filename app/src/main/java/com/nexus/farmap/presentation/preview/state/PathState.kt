package com.nexus.farmap.presentation.preview.state

import com.nexus.farmap.domain.pathfinding.Path
import com.nexus.farmap.domain.tree.TreeNode

data class PathState(
    val startEntry: TreeNode.Entry? = null, val endEntry: TreeNode.Entry? = null, val path: Path? = null
)
