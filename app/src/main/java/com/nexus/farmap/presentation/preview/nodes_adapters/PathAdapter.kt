package com.nexus.farmap.presentation.preview.nodes_adapters

import androidx.lifecycle.LifecycleCoroutineScope
import com.nexus.farmap.domain.hit_test.OrientatedPosition
import com.nexus.farmap.presentation.common.helpers.DrawerHelper
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArNode


class PathAdapter(
    drawerHelper: DrawerHelper,
    previewView: ArSceneView,
    bufferSize: Int,
    scope: LifecycleCoroutineScope,
) : NodesAdapter<OrientatedPosition>(drawerHelper, previewView, bufferSize, scope) {

    override suspend fun onInserted(item: OrientatedPosition): ArNode = drawerHelper.placeArrow(item, previewView)

    override suspend fun onRemoved(item: OrientatedPosition, node: ArNode) = drawerHelper.removeArrowWithAnim(node)
}