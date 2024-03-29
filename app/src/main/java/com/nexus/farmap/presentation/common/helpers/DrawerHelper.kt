package com.nexus.farmap.presentation.common.helpers

import android.graphics.Color
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nexus.farmap.R
import com.nexus.farmap.domain.hit_test.OrientatedPosition
import com.nexus.farmap.domain.tree.TreeNode
import com.google.ar.core.Anchor
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.uchuhimo.collections.MutableBiMap
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.ar.scene.destroy
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.math.toNewQuaternion
import io.github.sceneview.math.toVector3
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch


class DrawerHelper(
    private val fragment: Fragment,
) {

    private var labelScale = Scale(0.15f, 0.075f, 0.15f)
    private var arrowScale = Scale(0.5f, 0.5f, 0.5f)
    private var labelAnimationDelay = 2L
    private var arrowAnimationDelay = 2L
    private var labelAnimationPart = 10
    private var arrowAnimationPart = 15

    private val animationJobs = mutableMapOf<ArNode, Job>()

    suspend fun drawNode(
        treeNode: TreeNode, surfaceView: ArSceneView, anchor: Anchor? = null
    ): ArNode {
        return when (treeNode) {
            is TreeNode.Path -> {
                drawPath(treeNode, surfaceView, anchor)
            }

            is TreeNode.Entry -> {
                drawEntry(treeNode, surfaceView, anchor)
            }
        }

    }

    suspend fun removeLink(
        pair: Pair<ArNode, ArNode>, modelsToLinkModels: MutableBiMap<Pair<ArNode, ArNode>, ArNode>
    ) {
        modelsToLinkModels[pair]?.destroy()
        modelsToLinkModels.remove(pair)
    }

    private suspend fun drawPath(
        treeNode: TreeNode.Path, surfaceView: ArSceneView, anchor: Anchor? = null
    ): ArNode {
        val modelNode = ArNode()
        modelNode.loadModel(
            context = fragment.requireContext(),
            glbFileLocation = "models/nexus.glb",
        )
        modelNode.position = treeNode.position
        modelNode.modelScale = Scale(0.1f)
        if (anchor != null) {
            modelNode.anchor = anchor
        } else {
            modelNode.anchor = modelNode.createAnchor()
        }

        modelNode.model?.let {
            it.isShadowCaster = false
            it.isShadowReceiver = false
        }

        surfaceView.addChild(modelNode)

        return modelNode
    }

    private suspend fun drawEntry(
        treeNode: TreeNode.Entry, surfaceView: ArSceneView, anchor: Anchor? = null
    ): ArNode {
        return placeLabel(
            treeNode.number, OrientatedPosition(treeNode.position, treeNode.forwardVector), surfaceView
        )
    }

    suspend fun placeLabel(
        label: String, pos: OrientatedPosition, surfaceView: ArSceneView, anchor: Anchor? = null
    ): ArNode = placeRend(
        label = label, pos = pos, surfaceView = surfaceView, scale = labelScale, anchor = anchor
    )

    suspend fun placeArrow(
        pos: OrientatedPosition, surfaceView: ArSceneView
    ): ArNode = placeRend(
        pos = pos, surfaceView = surfaceView, scale = arrowScale
    )

    private suspend fun placeRend(
        label: String? = null, pos: OrientatedPosition, surfaceView: ArSceneView, scale: Scale, anchor: Anchor? = null
    ): ArNode {
        var node: ArNode? = null
        ViewRenderable.builder()
            .setView(fragment.requireContext(), if (label != null) R.layout.text_sign else R.layout.route_node)
            .setSizer { Vector3(0f, 0f, 0f) }.setVerticalAlignment(ViewRenderable.VerticalAlignment.CENTER)
            .setHorizontalAlignment(ViewRenderable.HorizontalAlignment.CENTER).build()
            .thenAccept { renderable: ViewRenderable ->
                renderable.let {
                    it.isShadowCaster = false
                    it.isShadowReceiver = false
                }
                if (label != null) {
                    val cardView = renderable.view as CardView
                    val textView: TextView = cardView.findViewById(R.id.signTextView)
                    textView.text = label
                }
                val textNode = ArNode().apply {
                    setModel(
                        renderable = renderable
                    )
                    model
                    position = Position(pos.position.x, pos.position.y, pos.position.z)
                    quaternion = pos.orientation
                    if (anchor != null) {
                        this.anchor = anchor
                    } else {
                        this.anchor = this.createAnchor()
                    }
                }
                surfaceView.addChild(textNode)
                node = textNode
                textNode.animateViewAppear(
                    scale,
                    if (label != null) labelAnimationDelay else arrowAnimationDelay,
                    if (label != null) labelAnimationPart else arrowAnimationPart
                )
            }.await()

        return node!!
    }

    fun removeNode(node: ArNode) {
        node.destroy()
        node.anchor?.destroy()
        animationJobs[node]?.cancel()
    }

    fun removeArrowWithAnim(node: ArNode) {
        node.model as ViewRenderable? ?: throw Exception("No view renderable")
        node.animateViewDisappear(arrowScale, arrowAnimationDelay, arrowAnimationPart)
    }

    fun drawLine(
        from: ArNode,
        to: ArNode,
        modelsToLinkModels: MutableBiMap<Pair<ArNode, ArNode>, ArNode>,
        surfaceView: ArSceneView
    ) {

        val fromVector = from.position.toVector3()
        val toVector = to.position.toVector3()

        // Compute a line's length
        val lineLength = Vector3.subtract(fromVector, toVector).length()

        // Prepare a color
        val colorOrange = Color(Color.parseColor("#ffffff"))

        // 1. make a material by the color
        MaterialFactory.makeOpaqueWithColor(fragment.requireContext(), colorOrange).thenAccept { material: Material? ->
                // 2. make a model by the material
                val model = ShapeFactory.makeCylinder(
                    0.01f, lineLength, Vector3(0f, lineLength / 2, 0f), material
                )

                model.isShadowCaster = false
                model.isShadowReceiver = false

                // 3. make node
                val node = ArNode()
                node.setModel(model)
                node.parent = from
                surfaceView.addChild(node)

                // 4. set rotation
                val difference = Vector3.subtract(toVector, fromVector)
                val directionFromTopToBottom = difference.normalized()
                val rotationFromAToB: Quaternion = Quaternion.lookRotation(
                    directionFromTopToBottom, Vector3.up()
                )

                val rotation = Quaternion.multiply(
                    rotationFromAToB, Quaternion.axisAngle(Vector3(1.0f, 0.0f, 0.0f), 270f)
                )

                node.modelQuaternion = rotation.toNewQuaternion()
                node.position = from.position

                modelsToLinkModels[Pair(from, to)] = node
            }
    }

    private fun ArNode.animateViewAppear(targetScale: Scale, delay: Long, part: Int) {
        animateView(true, targetScale, delay, part, end = null)
    }

    private fun ArNode.animateViewDisappear(targetScale: Scale, delay: Long, part: Int) {
        animateView(false, targetScale, delay, part) {
            removeNode(it)
        }
    }

    private fun ArNode.animateView(
        appear: Boolean, targetScale: Scale, delay: Long, part: Int, end: ((ArNode) -> Unit)?
    ) {
        val renderable = this.model as ViewRenderable? ?: throw Exception("No view renderable")
        var size = renderable.sizer.getSize(renderable.view)
        val xPart = targetScale.x / part
        val yPart = targetScale.y / part
        val zPart = targetScale.z / part
        animationJobs[this]?.cancel()
        animationJobs[this] = fragment.viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                if (size.x >= targetScale.toVector3().x && appear) {
                    break
                } else if (size.x <= 0 && !appear) {
                    break
                }
                renderable.sizer = ViewSizer {
                    if (appear) size.addConst(xPart, yPart, zPart)
                    else size.addConst(xPart, yPart, zPart, -1)

                }
                delay(delay)
                size = renderable.sizer.getSize(renderable.view)
            }
            if (end != null) {
                end(this@animateView)
            }
        }
    }

    private fun Vector3.addConst(xValue: Float, yValue: Float, zValue: Float, modifier: Int = 1): Vector3 {
        return Vector3(
            x + xValue * modifier, y + yValue * modifier, z + zValue * modifier
        )
    }
}