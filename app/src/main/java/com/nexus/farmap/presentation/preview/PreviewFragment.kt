package com.nexus.farmap.presentation.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nexus.farmap.R
import com.nexus.farmap.data.App
import com.nexus.farmap.databinding.FragmentPreviewBinding
import com.nexus.farmap.domain.tree.TreeNode
import com.nexus.farmap.presentation.LabelObject
import com.nexus.farmap.presentation.common.helpers.DrawerHelper
import com.nexus.farmap.presentation.preview.nodes_adapters.PathAdapter
import com.nexus.farmap.presentation.preview.nodes_adapters.TreeAdapter
import com.nexus.farmap.presentation.preview.state.PathState
import com.nexus.farmap.presentation.preview.MainShareModel
import com.nexus.farmap.presentation.preview.MainUiEvent
import com.nexus.farmap.presentation.preview.MainEvent

import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.*

import com.uchuhimo.collections.MutableBiMap
import com.uchuhimo.collections.mutableBiMapOf
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.node.ArNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PreviewFragment : Fragment() {

    private val mainModel: MainShareModel by activityViewModels()


    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!

    private val drawerHelper = DrawerHelper(this)

    private var endPlacingJob: Job? = null
    private var startPlacingJob: Job? = null
    private var wayBuildingJob: Job? = null
    private var treeBuildingJob: Job? = null
    private var currentPathState: PathState? = null
    private var lastPositionTime = 0L
    private lateinit var pathAdapter: PathAdapter
    private lateinit var treeAdapter: TreeAdapter


    private var lastConfObject: LabelObject? = null
    private var confObjectJob: Job? = null
    private val treeNodesToModels: MutableBiMap<TreeNode, ArNode> = mutableBiMapOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreviewBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.sceneView.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        binding.sceneView.onPause(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //TODO process pause and resume
        pathAdapter = PathAdapter(
            drawerHelper,
            binding.sceneView,
            VIEWABLE_PATH_NODES,
            viewLifecycleOwner.lifecycleScope
        )

        treeAdapter = TreeAdapter(
            drawerHelper,
            binding.sceneView,
            DEFAULT_BUFFER_SIZE,
            viewLifecycleOwner.lifecycleScope
        )

        binding.sceneView.apply {
            planeRenderer.isVisible = true
            instructions.enabled = false
            onArFrame = { frame ->
                onDrawFrame(frame)
            }
            onTouch = {node, _ ->
                if (App.mode == App.ADMIN_MODE) {
                    node?.let { it ->
                        if (!mainModel.linkPlacementMode.value) {
                            selectNode(it as ArNode)
                        } else {
                            val treeNode = mainModel.selectedNode.value
                            treeAdapter.getArNode(treeNode)?.let { node1 ->
                                linkNodes(node1, it as ArNode)
                            }
                        }
                    }
                }
                true
            }
//            onTouchEvent = { pickHitResult, _ ->
//                if (App.mode == App.ADMIN_MODE) {
//                    pickHitResult.node?.let { node ->
//                        if (!mainModel.linkPlacementMode.value) {
//                            selectNode(node)
//                        } else {
//                            val treeNode = mainModel.selectedNode.value
//                            treeNodesToModels[treeNode]?.let { node1 ->
//                                linkNodes(node1, node)
//                            }
//                        }
//                    }
//                }
//                true
//            }
            onArSessionFailed = { exception ->
                val message = when (exception) {
                    is UnavailableArcoreNotInstalledException,
                    is UnavailableUserDeclinedInstallationException -> getString(R.string.install_arcode)
                    is UnavailableApkTooOldException -> getString(R.string.update_arcode)
                    is UnavailableSdkTooOldException -> getString(R.string.update_app)
                    is UnavailableDeviceNotCompatibleException -> getString(R.string.no_arcore_support)
                    is CameraNotAvailableException -> getString(R.string.camera_not_available)
                    is SecurityException -> getString(R.string.provide_camera_permission)
                    else -> getString(R.string.failed_to_create_session)
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }

        selectNode(null)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                mainModel.pathState.collectLatest { pathState ->
                    //In ADMIN_MODE, all entry labels are drawn automatically, so another redraw from this function
                    //will cause node
                 //   if (App.mode == App.USER_MODE) {

                        if (currentPathState?.endEntry != pathState.endEntry) {
                            endPlacingJob?.cancel()
                            currentPathState?.endEntry?.let { end ->
                                treeNodesToModels[end]?.let {
                                    drawerHelper.removeNode(it)
                                }
                            }
                            endPlacingJob = viewLifecycleOwner.lifecycleScope.launch {
                                pathState.endEntry?.let { end ->
                                    treeNodesToModels[end] = drawerHelper.drawNode(
                                        end,
                                        binding.sceneView,
                                    )
                                }
                            }
                        }
                        if (currentPathState?.startEntry != pathState.startEntry) {
                            startPlacingJob?.cancel()
                            currentPathState?.startEntry?.let { start ->
                                treeNodesToModels[start]?.let {
                                    drawerHelper.removeNode(it)
                                }
                            }
                            startPlacingJob = viewLifecycleOwner.lifecycleScope.launch {
                                pathState.startEntry?.let { start ->
                                    treeNodesToModels[start] = drawerHelper.drawNode(
                                        start,
                                        binding.sceneView,
                                    )
                                }
                            }
                        }
           //         }
                    currentPathState = pathState
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                mainModel.mainUiEvents.collectLatest { uiEvent ->
                    when (uiEvent) {
                        is MainUiEvent.InitSuccess -> {
                           // onInitializeSuccess()
                        }
                        is MainUiEvent.InitFailed -> {
                            showSnackbar(getString(R.string.init_failed))
                        }
                        is MainUiEvent.NodeCreated -> {
                            showSnackbar(getString(R.string.node_created))
                        }
                        is MainUiEvent.LinkCreated -> {
                            viewLifecycleOwner.lifecycleScope.launch {
                                treeAdapter.createLink(uiEvent.node1, uiEvent.node2)
                                showSnackbar(getString(R.string.link_created))
                            }
                        }
                        is MainUiEvent.NodeDeleted -> {
                            showSnackbar(getString(R.string.node_deleted))
                        }
                        is MainUiEvent.PathNotFound -> {
                            showSnackbar(getString(R.string.no_path))
                        }
                        is MainUiEvent.EntryAlreadyExists -> {
                            showSnackbar(getString(R.string.entry_already_exists))
                        }
                        else -> {}
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                mainModel.confirmationObject.collectLatest { confObject ->
                    confObjectJob?.cancel()
                    confObjectJob = viewLifecycleOwner.lifecycleScope.launch {
                        lastConfObject?.node?.let {
                            drawerHelper.removeNode(it)
                        }
                        confObject?.let {
                            it.node = drawerHelper.placeLabel(
                                confObject.label,
                                confObject.pos,
                                binding.sceneView
                            )
                            lastConfObject = it
                        }
                    }
                }
                }
            }
        }


    private fun onDrawFrame(frame: ArFrame) {

        val camera = frame.camera

        // Handle tracking failures.
        if (camera.trackingState != TrackingState.TRACKING) {
            return
        }

        mainModel.onEvent(
            MainEvent.NewFrame(frame)
        )

        val userPos = Float3(
            frame.camera.displayOrientedPose.tx(),
            frame.camera.displayOrientedPose.ty(),
            frame.camera.displayOrientedPose.tz()
        )

        if (System.currentTimeMillis() - lastPositionTime > POSITION_DETECT_DELAY){
            lastPositionTime = System.currentTimeMillis()
            changeViewablePath(userPos)
            if (App.mode == App.ADMIN_MODE) {
                changeViewableTree(userPos)
                mainModel.selectedNode.value?.let { node ->
                    checkSelectedNode(node)
                }
            }
        }
    }
    
    private fun selectNode(node: ArNode?){
        //User selected entry can be stored in PreviewFragment nodes map,
        // if this node displayed as PathState start or end
        val treeNode = treeNodesToModels.inverse[node] ?: treeAdapter.getTreeNode(node)
        mainModel.onEvent(MainEvent.NewSelectedNode(treeNode))
    }

    private fun checkSelectedNode(treeNode: TreeNode){
        if (treeAdapter.getArNode(treeNode) == null) {
            mainModel.onEvent(MainEvent.NewSelectedNode(null))
        }
    }

    private fun linkNodes(node1: ArNode, node2: ArNode){
        viewLifecycleOwner.lifecycleScope.launch {
            val path1: TreeNode? = treeAdapter.getTreeNode(node1)
            val path2: TreeNode? = treeAdapter.getTreeNode(node2)

            if (path1 != null && path2 != null){
                mainModel.onEvent(MainEvent.LinkNodes(path1, path2))
            }
        }
    }

    private fun changeViewablePath(userPosition: Float3){
        wayBuildingJob?.cancel()
        wayBuildingJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            val nodes = currentPathState?.path?.getNearNodes(
                number = VIEWABLE_PATH_NODES,
                position = userPosition
            )
            pathAdapter.commit(nodes ?: listOf())
        }
    }

    //ONLY FOR ADMIN MODE
    private fun changeViewableTree(userPosition: Float3){
        if (treeBuildingJob?.isCompleted == true || treeBuildingJob?.isCancelled == true || treeBuildingJob == null) {
            treeBuildingJob?.cancel()
            treeBuildingJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                val nodes = mainModel.treeDiffUtils.getNearNodes(
                    radius = VIEWABLE_ADMIN_NODES,
                    position = userPosition
                )
                treeAdapter.commit(nodes)
            }
        }

    }

//    private fun reDrawPath(path: Path?, wayNodes: MutableList<ArNode>){
//        wayBuildingJob?.cancel()
//        wayBuildingJob = viewLifecycleOwner.lifecycleScope.launch {
//            drawerHelper.drawWay(
//                path,
//                wayNodes,
//                binding.sceneView
//            )
//        }
//    }



//    private suspend fun onInitializeSuccess() {
//        if (App.mode == App.ADMIN_MODE) {
//            drawerHelper.drawTree(
//                mainModel.tree,
//                treeNodesToModels,
//                modelsToLinkModels,
//                binding.sceneView
//            )
//        }
//    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.sceneView, message, Snackbar.LENGTH_SHORT)
            .show()
    }

    companion object {
        //how many path nodes will be displayed at the moment
        const val VIEWABLE_PATH_NODES = 32
        //distance of viewable nodes for admin mode
        const val VIEWABLE_ADMIN_NODES = 8f
        //how often the check for path and tree redraw will be
        const val POSITION_DETECT_DELAY = 100L
        //image crop percentage for recognition
        val DESIRED_CROP = Pair(0, 0)
    }
}