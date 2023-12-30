package com.nexus.farmap.data.pathfinding

import com.nexus.farmap.data.App
import com.nexus.farmap.domain.pathfinding.Path
import com.nexus.farmap.domain.pathfinding.Pathfinder
import com.nexus.farmap.domain.tree.Tree

class AStarImpl: Pathfinder {

    private val smoothPath = App.instance!!.smoothPath

    override suspend fun findWay(from: String, to: String, tree: Tree): Path? {

        val finalNode = AStarNode(tree.getEntry(to)!!, null)
        val initialNode = AStarNode(tree.getEntry(from)!!, finalNode)

        val openList: MutableList<AStarNode> = mutableListOf()
        val closedSet: MutableSet<AStarNode> = mutableSetOf()

        openList.add(initialNode)

        while (openList.isNotEmpty()) {
            val currentNode = getNextAStarNode(openList)
            openList.remove(currentNode)
            closedSet.add(currentNode)

            if (currentNode == finalNode) {

                return Path(
                    smoothPath(
                        getPath(currentNode).map { aStarNode ->
                            aStarNode.node
                        }
                    )
                )
            } else {
                addAdjacentNodes(currentNode, openList, closedSet, finalNode, tree)
            }
        }
        return null

    }

    private fun getPath(node: AStarNode): List<AStarNode> {
        var currentNode = node
        val path: MutableList<AStarNode> = mutableListOf()
        path.add(currentNode)
        while (currentNode.parent != null) {
            path.add(0, currentNode.parent!!)
            currentNode = currentNode.parent!!
        }
        return path
    }

    private fun addAdjacentNodes(
        currentNode: AStarNode,
        openList: MutableList<AStarNode>,
        closedSet: Set<AStarNode>,
        finalNode: AStarNode,
        tree: Tree
    ) {
        currentNode.node.neighbours.forEach { nodeId ->
            tree.getNode(nodeId)?.let { node ->
                val nodeClosed = closedSet.find { it.node.id == nodeId }
                val nodeOpen = openList.find { it.node.id == nodeId }
                if ( nodeClosed == null && nodeOpen == null) {
                    checkNode(currentNode, AStarNode(node, finalNode), openList, closedSet)
                }
                else if (nodeOpen != null && nodeClosed == null) {
                    checkNode(currentNode, nodeOpen, openList, closedSet)
                }
            }

        }
    }

    //всем нодам устанавливается сложность в 1, можно ее убрать по идее
    private fun checkNode(
        parentNode: AStarNode,
        node: AStarNode,
        openList: MutableList<AStarNode>,
        closedSet: Set<AStarNode>
    ) {
        if (!closedSet.contains(node)) {
            if (!openList.contains(node)) {
                node.setNodeData(parentNode, 1f)
                openList.add(node)
            } else {
                node.checkBetterPath(parentNode, 1f)
            }
        }
    }


    private fun getNextAStarNode(openList: List<AStarNode>): AStarNode {
        return openList.sortedBy { node -> node.f }[0]

    }






}