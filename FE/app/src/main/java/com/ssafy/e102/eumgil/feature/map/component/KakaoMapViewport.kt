package com.ssafy.e102.eumgil.feature.map.component

import android.content.Context
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ssafy.e102.eumgil.R
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.GestureType
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.ssafy.e102.eumgil.feature.map.model.MapCoordinate
import kotlinx.coroutines.delay

@Composable
internal fun KakaoMapViewport(
    state: MapViewportUiState,
    onMarkerClick: (String) -> Unit,
    onCameraMoveEnd: (MapCoordinate, Int, Boolean) -> Unit,
    onMapClick: (MapCoordinate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var reloadGeneration by remember { mutableIntStateOf(0) }
    var isRendererRestarting by remember { mutableStateOf(false) }

    LaunchedEffect(reloadGeneration, isRendererRestarting) {
        if (!isRendererRestarting) return@LaunchedEffect
        delay(KAKAO_RENDERER_RESTART_DELAY_MILLIS)
        isRendererRestarting = false
    }

    if (isRendererRestarting) {
        MapRendererFallbackOverlay(
            title = stringResource(id = R.string.map_viewport_title_renderer_loading),
            description = stringResource(id = R.string.map_viewport_description_renderer_loading),
            isLoading = true,
            modifier = modifier,
        )
        return
    }

    key(reloadGeneration) {
        val controller = remember(reloadGeneration) { KakaoMapViewportController() }
        val rendererFailure = controller.rendererFailure

        LaunchedEffect(controller, controller.rendererStatus) {
            if (controller.rendererStatus != KakaoRendererStatus.Initializing) return@LaunchedEffect
            delay(KAKAO_RENDERER_READY_TIMEOUT_MILLIS)
            controller.markRendererTimedOut()
        }

        DisposableEffect(lifecycleOwner, controller) {
            val observer =
                LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> controller.setLifecycleResumed(true)
                        Lifecycle.Event.ON_PAUSE -> controller.setLifecycleResumed(false)
                        Lifecycle.Event.ON_DESTROY -> controller.finish()
                        else -> Unit
                    }
                }
            lifecycleOwner.lifecycle.addObserver(observer)
            controller.setLifecycleResumed(
                lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED),
            )
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                controller.finish()
            }
        }

        Box(modifier = modifier) {
            AndroidView(
                factory = { context ->
                    controller.bind(
                        context = context,
                        initialState = state,
                        onMarkerClick = onMarkerClick,
                        onCameraMoveEnd = onCameraMoveEnd,
                        onMapClick = onMapClick,
                    )
                },
                modifier = Modifier.fillMaxSize(),
                update = {
                    controller.render(
                        state = state,
                        onMarkerClick = onMarkerClick,
                        onCameraMoveEnd = onCameraMoveEnd,
                        onMapClick = onMapClick,
                    )
                },
            )

            if (controller.rendererStatus != KakaoRendererStatus.Ready) {
                val isRendererError = rendererFailure != null
                MapRendererFallbackOverlay(
                    title =
                        if (isRendererError) {
                            stringResource(id = R.string.map_viewport_title_renderer_error)
                        } else {
                            stringResource(id = R.string.map_viewport_title_renderer_loading)
                        },
                    description =
                        if (isRendererError) {
                            stringResource(id = R.string.map_viewport_description_renderer_error)
                        } else {
                            stringResource(id = R.string.map_viewport_description_renderer_loading)
                        },
                    actionLabel =
                        if (isRendererError) {
                            stringResource(id = R.string.map_viewport_retry)
                        } else {
                            null
                        },
                    onActionClick =
                        if (isRendererError) {
                            {
                                controller.finish()
                                isRendererRestarting = true
                                reloadGeneration += 1
                            }
                        } else {
                            null
                        },
                    isLoading = !isRendererError,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

private class KakaoMapViewportController {
    var rendererStatus by mutableStateOf(KakaoRendererStatus.Initializing)
        private set
    var rendererFailure by mutableStateOf<KakaoRendererFailure?>(null)
        private set

    private var mapView: MapView? = null
    private var kakaoMap: KakaoMap? = null
    private var latestState: MapViewportUiState? = null
    private var markerClickHandler: ((String) -> Unit)? = null
    private var cameraMoveEndHandler: ((MapCoordinate, Int, Boolean) -> Unit)? = null
    private var mapClickHandler: ((MapCoordinate) -> Unit)? = null
    private var lastRenderedCameraRequestId: Long? = null
    private var lastRenderedCameraTarget: com.ssafy.e102.eumgil.feature.map.model.MapCameraTarget? = null
    private var lastRenderedMarkers: List<KakaoMarkerRenderState> = emptyList()
    private var isStarted = false
    private var isFinished = false
    private var isLifecycleResumed = false
    private var hasMapLifecycleResumed = false
    private var lifecycleDispatchRetryCount = 0
    private val attachStateListener =
        object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                (view as? MapView)?.let(::startMap)
                syncLifecycleToMapView(reason = "view-attached")
            }

            override fun onViewDetachedFromWindow(view: View) = Unit
        }

    fun bind(
        context: Context,
        initialState: MapViewportUiState,
        onMarkerClick: (String) -> Unit,
        onCameraMoveEnd: (MapCoordinate, Int, Boolean) -> Unit,
        onMapClick: (MapCoordinate) -> Unit,
    ): MapView {
        latestState = initialState
        markerClickHandler = onMarkerClick
        cameraMoveEndHandler = onCameraMoveEnd
        mapClickHandler = onMapClick

        return mapView ?: MapView(context).also { createdMapView ->
            Log.i(KAKAO_MAP_LOG_TAG, "Creating Kakao MapView instance")
            createdMapView.addOnAttachStateChangeListener(attachStateListener)
            mapView = createdMapView
            if (createdMapView.isAttachedToWindow) {
                startMap(createdMapView)
            }
            syncLifecycleToMapView(reason = "bind")
        }
    }

    fun render(
        state: MapViewportUiState,
        onMarkerClick: (String) -> Unit,
        onCameraMoveEnd: (MapCoordinate, Int, Boolean) -> Unit,
        onMapClick: (MapCoordinate) -> Unit,
    ) {
        latestState = state
        markerClickHandler = onMarkerClick
        cameraMoveEndHandler = onCameraMoveEnd
        mapClickHandler = onMapClick
        renderIntoMapIfReady()
    }

    fun setLifecycleResumed(isResumed: Boolean) {
        isLifecycleResumed = isResumed
        syncLifecycleToMapView(
            reason =
                if (isResumed) {
                    "lifecycle-resume"
                } else {
                    "lifecycle-pause"
                },
        )
    }

    fun finish() {
        if (isFinished) return
        Log.i(KAKAO_MAP_LOG_TAG, "Finishing Kakao map renderer controller")
        isFinished = true
        isStarted = false
        kakaoMap = null
        hasMapLifecycleResumed = false
        lifecycleDispatchRetryCount = 0
        mapView?.removeOnAttachStateChangeListener(attachStateListener)
        mapView?.finish()
        mapView = null
        rendererStatus = KakaoRendererStatus.Initializing
        rendererFailure = null
        lastRenderedCameraRequestId = null
        lastRenderedCameraTarget = null
        lastRenderedMarkers = emptyList()
    }

    fun markRendererTimedOut() {
        if (isFinished || rendererStatus != KakaoRendererStatus.Initializing || kakaoMap != null) return

        val failure = createKakaoRendererTimeoutFailure()
        rendererFailure = failure
        rendererStatus = KakaoRendererStatus.Error
        hasMapLifecycleResumed = false
        Log.e(
            KAKAO_MAP_LOG_TAG,
            "Kakao map renderer timed out before ready: ${failure.debugSummary}",
        )
    }

    private fun startMap(createdMapView: MapView) {
        if (isStarted || isFinished) return

        isStarted = true
        Log.i(KAKAO_MAP_LOG_TAG, "Starting Kakao map renderer")
        createdMapView.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() {
                    if (isFinished) {
                        Log.i(KAKAO_MAP_LOG_TAG, "Ignoring Kakao map destroy callback after controller finish")
                        return
                    }

                    val destroyFailure =
                        resolveKakaoRendererFailureAfterUnexpectedDestroy(
                            existingFailure = rendererFailure,
                        )
                    rendererFailure = destroyFailure
                    rendererStatus = KakaoRendererStatus.Error
                    kakaoMap = null
                    hasMapLifecycleResumed = false
                    lifecycleDispatchRetryCount = 0
                    lastRenderedCameraRequestId = null
                    lastRenderedCameraTarget = null
                    lastRenderedMarkers = emptyList()
                    Log.w(
                        KAKAO_MAP_LOG_TAG,
                        "Kakao map renderer destroyed before interactive recovery: ${destroyFailure.debugSummary}",
                    )
                }

                override fun onMapError(error: Exception) {
                    val failure = createKakaoRendererFailure(error)
                    rendererFailure = failure
                    rendererStatus = KakaoRendererStatus.Error
                    hasMapLifecycleResumed = false
                    Log.e(
                        KAKAO_MAP_LOG_TAG,
                        "Kakao map renderer failed before ready: ${failure.debugSummary}",
                        error,
                    )
                }

                override fun onMapResumed() {
                    hasMapLifecycleResumed = true
                    lifecycleDispatchRetryCount = 0
                    Log.d(KAKAO_MAP_LOG_TAG, "Kakao map lifecycle resumed")
                }

                override fun onMapPaused() {
                    hasMapLifecycleResumed = false
                    lifecycleDispatchRetryCount = 0
                    Log.d(KAKAO_MAP_LOG_TAG, "Kakao map lifecycle paused")
                }
            },
            object : KakaoMapReadyCallback() {
                override fun onMapReady(readyMap: KakaoMap) {
                    kakaoMap = readyMap
                    rendererFailure = null
                    rendererStatus = KakaoRendererStatus.Ready
                    val cameraTarget =
                        latestState?.cameraTarget
                            ?: com.ssafy.e102.eumgil.feature.map.model.MapCameraTarget.DefaultBusan
                    Log.i(
                        KAKAO_MAP_LOG_TAG,
                        "Kakao map ready ${createKakaoCameraDebugSummary(cameraTarget)}",
                    )
                    readyMap.setPoiClickable(false)
                    readyMap.setOnLabelClickListener { _, _, label ->
                        (label.getTag() as? String)?.let { markerId ->
                            markerClickHandler?.invoke(markerId)
                            true
                        } ?: false
                    }
                    readyMap.setOnViewportClickListener { _, position, _ ->
                        mapClickHandler?.invoke(
                            MapCoordinate(
                                latitude = position.latitude,
                                longitude = position.longitude,
                            ),
                        )
                    }
                    readyMap.setOnCameraMoveEndListener { _, cameraPosition, gestureType ->
                        val movedCenter =
                            MapCoordinate(
                                latitude = cameraPosition.position.latitude,
                                longitude = cameraPosition.position.longitude,
                            )
                        lastRenderedCameraTarget =
                            syncRenderedKakaoCameraTarget(
                                previousTarget = lastRenderedCameraTarget,
                                latestStateTarget = latestState?.cameraTarget,
                                center = movedCenter,
                                zoomLevel = cameraPosition.zoomLevel,
                            )
                        cameraMoveEndHandler?.invoke(
                            movedCenter,
                            cameraPosition.zoomLevel,
                            gestureType.isUserDrivenCameraMove(),
                        )
                    }
                    renderIntoMapIfReady()
                    syncLifecycleToMapView(reason = "map-ready")
                }

                override fun getPosition(): LatLng {
                    val cameraTarget =
                        latestState?.cameraTarget
                            ?: com.ssafy.e102.eumgil.feature.map.model.MapCameraTarget.DefaultBusan
                    return LatLng.from(
                        cameraTarget.center.latitude,
                        cameraTarget.center.longitude,
                    )
                }

                override fun getZoomLevel(): Int {
                    val cameraTarget =
                        latestState?.cameraTarget
                            ?: com.ssafy.e102.eumgil.feature.map.model.MapCameraTarget.DefaultBusan
                    return createKakaoCameraRenderState(cameraTarget).zoomLevel
                }
            },
        )
    }

    private fun renderIntoMapIfReady() {
        val readyMap = kakaoMap ?: return
        val state = latestState ?: return

        syncCamera(readyMap = readyMap, state = state)
        syncMarkers(readyMap = readyMap, state = state)
    }

    private fun syncLifecycleToMapView(reason: String) {
        val boundMapView = mapView ?: return
        val lifecycleCommand =
            resolveKakaoMapLifecycleCommand(
                isLifecycleResumed = isLifecycleResumed,
                hasMapView = true,
                isAttachedToWindow = boundMapView.isAttachedToWindow,
                isStarted = isStarted,
                isFinished = isFinished,
                hasResumedLifecycle = hasMapLifecycleResumed,
            )
        if (lifecycleCommand == KakaoMapLifecycleCommand.NONE) {
            Log.d(
                KAKAO_MAP_LOG_TAG,
                "Skipping map lifecycle sync reason=$reason started=$isStarted finished=$isFinished resumed=$isLifecycleResumed attached=${boundMapView.isAttachedToWindow} surface=${boundMapView.surfaceView != null} ready=${kakaoMap != null} mapResumed=$hasMapLifecycleResumed",
            )
            return
        }

        boundMapView.post {
            if (mapView !== boundMapView) return@post
            val replayCommand =
                resolveKakaoMapLifecycleCommand(
                    isLifecycleResumed = isLifecycleResumed,
                    hasMapView = true,
                    isAttachedToWindow = boundMapView.isAttachedToWindow,
                    isStarted = isStarted,
                    isFinished = isFinished,
                    hasResumedLifecycle = hasMapLifecycleResumed,
                )
            when (replayCommand) {
                KakaoMapLifecycleCommand.RESUME -> {
                    Log.d(KAKAO_MAP_LOG_TAG, "Dispatching map resume reason=$reason")
                    runCatching { boundMapView.resume() }
                        .onSuccess {
                            lifecycleDispatchRetryCount = 0
                        }
                        .onFailure { error ->
                            if (scheduleLifecycleRetry(boundMapView, reason, "resume", error)) return@onFailure
                            val failure = createKakaoRendererFailure(error)
                            rendererFailure = failure
                            rendererStatus = KakaoRendererStatus.Error
                            Log.e(
                                KAKAO_MAP_LOG_TAG,
                                "Kakao map resume failed reason=$reason ${failure.debugSummary}",
                                error,
                            )
                        }
                }

                KakaoMapLifecycleCommand.PAUSE -> {
                    Log.d(KAKAO_MAP_LOG_TAG, "Dispatching map pause reason=$reason")
                    runCatching { boundMapView.pause() }
                        .onSuccess {
                            lifecycleDispatchRetryCount = 0
                        }
                        .onFailure { error ->
                            if (scheduleLifecycleRetry(boundMapView, reason, "pause", error)) return@onFailure
                            Log.w(KAKAO_MAP_LOG_TAG, "Kakao map pause failed reason=$reason", error)
                        }
                }

                KakaoMapLifecycleCommand.NONE -> Unit
            }
        }
    }

    private fun scheduleLifecycleRetry(
        boundMapView: MapView,
        reason: String,
        action: String,
        error: Throwable? = null,
    ): Boolean {
        if (lifecycleDispatchRetryCount >= MAX_LIFECYCLE_DISPATCH_RETRIES) {
            return false
        }
        lifecycleDispatchRetryCount += 1
        if (error != null) {
            Log.w(
                KAKAO_MAP_LOG_TAG,
                "Retrying map $action dispatch reason=$reason attempt=$lifecycleDispatchRetryCount",
                error,
            )
        } else {
            Log.d(
                KAKAO_MAP_LOG_TAG,
                "Waiting for map $action reason=$reason attempt=$lifecycleDispatchRetryCount attached=${boundMapView.isAttachedToWindow} surface=${boundMapView.surfaceView != null}",
            )
        }
        boundMapView.postDelayed(
            {
                if (mapView !== boundMapView) return@postDelayed
                syncLifecycleToMapView(
                    reason = "$reason-retry$lifecycleDispatchRetryCount",
                )
            },
            LIFECYCLE_DISPATCH_RETRY_DELAY_MILLIS,
        )
        return true
    }

    private fun syncCamera(
        readyMap: KakaoMap,
        state: MapViewportUiState,
    ) {
        val currentTarget = state.cameraTarget
        val cameraState = createKakaoCameraRenderState(currentTarget)
        if (lastRenderedCameraRequestId == cameraState.requestId) return
        val cameraUpdate =
            CameraUpdateFactory.newCenterPosition(
                LatLng.from(cameraState.latitude, cameraState.longitude),
                cameraState.zoomLevel,
            )
        if (shouldAnimateKakaoCameraTransition(previousTarget = lastRenderedCameraTarget, nextTarget = currentTarget)) {
            readyMap.moveCamera(
                cameraUpdate,
                CameraAnimation.from(KAKAO_ZOOM_CAMERA_ANIMATION_DURATION_MILLIS),
            )
        } else {
            readyMap.moveCamera(cameraUpdate)
        }
        lastRenderedCameraRequestId = cameraState.requestId
        lastRenderedCameraTarget = currentTarget
        Log.d(
            KAKAO_MAP_LOG_TAG,
            "Camera synced ${createKakaoCameraDebugSummary(currentTarget)}",
        )
    }

    private fun syncMarkers(
        readyMap: KakaoMap,
        state: MapViewportUiState,
    ) {
        val markerRenderStates =
            createKakaoMarkerRenderStates(
                markerOverlayState = state.markerOverlayState,
                selectedMarkerId = state.selectedMarkerId,
                currentLocation = state.currentLocation,
                selectedMapPinCoordinate = state.selectedMapPinCoordinate,
            )
        if (lastRenderedMarkers == markerRenderStates) return

        val labelManager = readyMap.labelManager ?: return
        labelManager.removeAllLabelLayer()
        val layer =
            labelManager.addLayer(
                LabelLayerOptions
                    .from(KAKAO_MARKER_LAYER_ID)
                    .setClickable(true),
            ) ?: return

        markerRenderStates.forEach { marker ->
            layer.addLabel(
                LabelOptions
                    .from(
                        marker.markerId,
                        LatLng.from(marker.latitude, marker.longitude),
                    )
                    .setStyles(
                        LabelStyle
                            .from(marker.iconResId)
                            .apply {
                                if (marker.anchorPointX != null && marker.anchorPointY != null) {
                                    setAnchorPoint(marker.anchorPointX, marker.anchorPointY)
                                }
                            },
                    )
                    .setClickable(true)
                    .setRank(marker.rank)
                    .apply {
                        marker.clickTargetId?.let(::setTag)
                    },
            )
        }
        lastRenderedMarkers = markerRenderStates
        Log.d(
            KAKAO_MAP_LOG_TAG,
            "Markers synced ${
                createKakaoMarkerDebugSummary(
                    markerOverlayState = state.markerOverlayState,
                    renderedMarkers = markerRenderStates,
                    selectedMarkerId = state.selectedMarkerId,
                )
            }",
        )
    }
}

private enum class KakaoRendererStatus {
    Initializing,
    Ready,
    Error,
}

private const val KAKAO_MARKER_LAYER_ID = "eumgil-map-markers"
private const val KAKAO_MAP_LOG_TAG = "KakaoMapViewport"
private const val MAX_LIFECYCLE_DISPATCH_RETRIES = 30
private const val LIFECYCLE_DISPATCH_RETRY_DELAY_MILLIS = 50L
private const val KAKAO_RENDERER_RESTART_DELAY_MILLIS = 220L
private const val KAKAO_RENDERER_READY_TIMEOUT_MILLIS = 4_000L

private fun GestureType.isUserDrivenCameraMove(): Boolean = this != GestureType.Unknown
