package com.nexus.farmap.data.repository

import com.nexus.farmap.data.App
import com.nexus.farmap.data.model.TreeNodeDto
import com.nexus.farmap.domain.repository.GraphRepository
import com.nexus.farmap.domain.tree.TreeNode
import com.nexus.farmap.domain.use_cases.convert
import com.nexus.farmap.domain.use_cases.opposite
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.toFloat3
import io.github.sceneview.math.toOldQuaternion
import io.github.sceneview.math.toVector3

class GraphImpl : GraphRepository {

    private val dao = App.instance?.getDatabase()?.graphDao!!

    override suspend fun getNodes(): List<TreeNodeDto> {
        return dao.getNodes() ?: listOf()
    }

    override suspend fun insertNodes(
        nodes: List<TreeNode>, translocation: Float3, rotation: Quaternion, pivotPosition: Float3
    ) {
        val transNodes = nodes.toMutableList()
        val undoTranslocation = translocation * -1f
        val undoQuaternion = rotation.opposite()
        dao.insertNodes(transNodes.map { node ->
            when (node) {
                is TreeNode.Entry -> {
                    TreeNodeDto.fromTreeNode(
                        node = node, position = undoPositionConvert(
                            node.position, undoTranslocation, undoQuaternion, pivotPosition
                        ), forwardVector = node.forwardVector.convert(undoQuaternion)
                    )
                }

                is TreeNode.Path -> {
                    TreeNodeDto.fromTreeNode(
                        node = node, position = undoPositionConvert(
                            node.position, undoTranslocation, undoQuaternion, pivotPosition
                        )
                    )

                }
            }
        })
    }

    override suspend fun deleteNodes(nodes: List<TreeNode>) {
        dao.deleteNodes(nodes.map { node -> TreeNodeDto.fromTreeNode(node) })
    }

    override suspend fun updateNodes(
        nodes: List<TreeNode>, translocation: Float3, rotation: Quaternion, pivotPosition: Float3
    ) {
        val transNodes = nodes.toMutableList()
        val undoTranslocation = translocation * -1f
        val undoQuarterion = rotation.opposite()
        dao.updateNodes(transNodes.map { node ->
            when (node) {
                is TreeNode.Entry -> {
                    TreeNodeDto.fromTreeNode(
                        node = node, position = undoPositionConvert(
                            node.position, undoTranslocation, undoQuarterion, pivotPosition
                        ), forwardVector = node.forwardVector.convert(undoQuarterion)
                    )
                }

                is TreeNode.Path -> {
                    TreeNodeDto.fromTreeNode(
                        node = node, position = undoPositionConvert(
                            node.position, undoTranslocation, undoQuarterion, pivotPosition
                        )
                    )
                }
            }
        })
    }

    override suspend fun clearNodes() {
        dao.clearNodes()
    }

    private fun undoPositionConvert(
        position: Float3, translocation: Float3, quaternion: Quaternion, pivotPosition: Float3
    ): Float3 {
        return (com.google.ar.sceneform.math.Quaternion.rotateVector(
            quaternion.toOldQuaternion(), (position - pivotPosition - translocation).toVector3()
        ).toFloat3() + pivotPosition)
    }

}