package com.nexus.farmap.domain.repository

import com.nexus.farmap.data.model.TreeNodeDto
import com.nexus.farmap.domain.tree.TreeNode
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion

interface GraphRepository {

    suspend fun getNodes(): List<TreeNodeDto>

    suspend fun insertNodes(
        nodes: List<TreeNode>, translocation: Float3, rotation: Quaternion, pivotPosition: Float3
    )

    suspend fun deleteNodes(nodes: List<TreeNode>)

    suspend fun updateNodes(
        nodes: List<TreeNode>, translocation: Float3, rotation: Quaternion, pivotPosition: Float3
    )

    suspend fun clearNodes()

}