package com.nexus.farmap.presentation.preview

import com.nexus.farmap.domain.tree.TreeNode
import com.google.ar.core.Anchor

sealed interface MainUiEvent {
    object InitSuccess: MainUiEvent
    object InitFailed: MainUiEvent
    object PathNotFound: MainUiEvent
    object EntryAlreadyExists: MainUiEvent
    object EntryCreated: MainUiEvent
    class NodeCreated(val treeNode: TreeNode, val anchor: Anchor? = null): MainUiEvent
    class LinkCreated(val node1: TreeNode, val node2: TreeNode): MainUiEvent
    class NodeDeleted(val node: TreeNode): MainUiEvent
}