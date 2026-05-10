package com.ssafy.e102.eumgil.feature.map.component

import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
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
import com.ssafy.e102.eumgil.core.model.FacilityCategory
import com.ssafy.e102.eumgil.feature.map.model.MapCoordinate
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs

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

            if (controller.rendererStatus == KakaoRendererStatus.Ready) {
                controller.projectedFacilityMarkerOverlays.forEach { overlay ->
                    MapProjectedFacilityMarkerOverlay(
                        overlay = overlay,
                        onClick = onMarkerClick,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                controller.projectedMarkerOverlays.forEach { overlay ->
                    MapProjectedMarkerOverlay(
                        overlay = overlay,
                        contentDescription =
                            overlay.resolveContentDescription(
                                selectedDestinationName = state.selectedDestinationName,
                            ),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

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
    var projectedFacilityMarkerOverlays by mutableStateOf<List<KakaoFacilityMarkerOverlay>>(emptyList())
        private set
    var projectedMarkerOverlays by mutableStateOf<List<KakaoProjectedMarkerOverlay>>(emptyList())
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
    private var lastDispatchedMapTapCoordinate: MapCoordinate? = null
    private var lastDispatchedMapTapUptimeMillis: Long = 0L
    private var isCameraMoveInProgress = false
    private var projectedMarkerTrackingRunnable: Runnable? = null
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
        stopProjectedMarkerTracking()
        mapView?.finish()
        mapView = null
        rendererStatus = KakaoRendererStatus.Initializing
        rendererFailure = null
        projectedFacilityMarkerOverlays = emptyList()
        projectedMarkerOverlays = emptyList()
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
                    stopProjectedMarkerTracking()
                    projectedFacilityMarkerOverlays = emptyList()
                    projectedMarkerOverlays = emptyList()
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
                            Log.i(KAKAO_MAP_LOG_TAG, "Label tapped markerId=$markerId")
                            markerClickHandler?.invoke(markerId)
                            true
                        } ?: false
                    }
                    readyMap.setOnTerrainClickListener { _, position, _ ->
                        dispatchMapTap(source = "terrain", position = position, hasPoi = false)
                    }
                    readyMap.setOnMapClickListener { _, position, _, poi ->
                        if (poi == null) {
                            dispatchMapTap(source = "map", position = position, hasPoi = false)
                        } else {
                            Log.d(
                                KAKAO_MAP_LOG_TAG,
                                "Ignoring map click with poi lat=${position.latitude.toLogCoordinate()} lng=${position.longitude.toLogCoordinate()}",
                            )
                        }
                    }
                    readyMap.setOnCameraMoveStartListener { _, _ ->
                        startProjectedMarkerTracking()
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
                        stopProjectedMarkerTracking()
                        updateProjectedMarkerOverlays(readyMap = readyMap, state = latestState)
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
        updateProjectedMarkerOverlays(readyMap = readyMap, state = state)
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
            )
        if (lastRenderedMarkers == markerRenderStates) return

        readyMap.labelManager?.removeAllLabelLayer()
        lastRenderedMarkers = markerRenderStates
        Log.d(
            KAKAO_MAP_LOG_TAG,
            buildString {
                append("Markers synced ")
                append(
                    createKakaoMarkerDebugSummary(
                        markerOverlayState = state.markerOverlayState,
                        renderedMarkers = markerRenderStates,
                        selectedMarkerId = state.selectedMarkerId,
                    ),
                )
                state.selectedMapPinCoordinate?.let { coordinate ->
                    append(" pin=")
                    append(coordinate.latitude.toLogCoordinate())
                    append(",")
                    append(coordinate.longitude.toLogCoordinate())
                }
            },
        )
    }

    private fun dispatchMapTap(
        source: String,
        position: LatLng,
        hasPoi: Boolean,
    ) {
        val coordinate =
            MapCoordinate(
                latitude = position.latitude,
                longitude = position.longitude,
            )
        val previousCoordinate = lastDispatchedMapTapCoordinate
        val now = SystemClock.elapsedRealtime()
        val isDuplicate =
            previousCoordinate != null &&
                now - lastDispatchedMapTapUptimeMillis <= KAKAO_MAP_TAP_DEDUP_WINDOW_MILLIS &&
                abs(previousCoordinate.latitude - coordinate.latitude) <= KAKAO_MAP_TAP_DEDUP_COORDINATE_EPSILON &&
                abs(previousCoordinate.longitude - coordinate.longitude) <= KAKAO_MAP_TAP_DEDUP_COORDINATE_EPSILON
        Log.i(
            KAKAO_MAP_LOG_TAG,
            "Map tap source=$source lat=${coordinate.latitude.toLogCoordinate()} lng=${coordinate.longitude.toLogCoordinate()} poi=$hasPoi duplicate=$isDuplicate",
        )
        if (isDuplicate) return
        lastDispatchedMapTapCoordinate = coordinate
        lastDispatchedMapTapUptimeMillis = now
        mapClickHandler?.invoke(coordinate)
    }

    private fun updateProjectedMarkerOverlays(
        readyMap: KakaoMap,
        state: MapViewportUiState?,
    ) {
        projectedFacilityMarkerOverlays =
            state?.let { currentState ->
                createKakaoFacilityMarkerOverlays(
                    markerOverlayState = currentState.markerOverlayState,
                    selectedMarkerId = currentState.selectedMarkerId,
                ) { coordinate ->
                    readyMap
                        .toScreenPoint(
                            LatLng.from(
                                coordinate.latitude,
                                coordinate.longitude,
                            ),
                        )?.let { point ->
                            KakaoMapScreenPoint(x = point.x, y = point.y)
                        }
                }
            } ?: emptyList()

        val projectedMarkers =
            createKakaoProjectedMarkerRenderStates(
                currentLocation = state?.currentLocation,
                selectedDestinationCoordinate = state?.selectedDestinationCoordinate,
                selectedMapPinCoordinate = state?.selectedMapPinCoordinate,
            )
        projectedMarkerOverlays =
            createKakaoProjectedMarkerOverlays(projectedMarkers) { coordinate ->
                readyMap
                    .toScreenPoint(
                        LatLng.from(
                            coordinate.latitude,
                            coordinate.longitude,
                        ),
                    )?.let { point ->
                        KakaoMapScreenPoint(x = point.x, y = point.y)
                    }
            }
    }

    private fun startProjectedMarkerTracking() {
        val boundMapView = mapView ?: return
        val readyMap = kakaoMap ?: return
        isCameraMoveInProgress = true
        if (projectedMarkerTrackingRunnable != null) return

        val trackingRunnable =
            object : Runnable {
                override fun run() {
                    if (!isCameraMoveInProgress || isFinished || mapView !== boundMapView || kakaoMap !== readyMap) {
                        projectedMarkerTrackingRunnable = null
                        return
                    }
                    updateProjectedMarkerOverlays(readyMap = readyMap, state = latestState)
                    boundMapView.postOnAnimation(this)
                }
            }
        projectedMarkerTrackingRunnable = trackingRunnable
        boundMapView.postOnAnimation(trackingRunnable)
    }

    private fun stopProjectedMarkerTracking() {
        isCameraMoveInProgress = false
        val boundMapView = mapView ?: run {
            projectedMarkerTrackingRunnable = null
            return
        }
        projectedMarkerTrackingRunnable?.let(boundMapView::removeCallbacks)
        projectedMarkerTrackingRunnable = null
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
private const val KAKAO_MAP_TAP_DEDUP_WINDOW_MILLIS = 250L
private const val KAKAO_MAP_TAP_DEDUP_COORDINATE_EPSILON = 0.000001
private const val KAKAO_MARKER_LAYER_Z_ORDER = 100

private fun GestureType.isUserDrivenCameraMove(): Boolean = this != GestureType.Unknown

private fun Double.toLogCoordinate(): String = String.format(Locale.US, "%.6f", this)

private data class ProjectedFacilityMarkerPalette(
    val container: Color,
    val border: Color,
    val content: Color,
)

@Composable
private fun MapProjectedFacilityMarkerOverlay(
    overlay: KakaoFacilityMarkerOverlay,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val markerSize = overlay.sizeDp.dp
    val markerSizePx = with(density) { markerSize.roundToPx() }
    val isBrailleBlock = overlay.categoryType.category == FacilityCategory.BRAILLE_BLOCK
    val palette = projectedFacilityMarkerPalette(overlay.categoryType.category)
    val iconSize =
        when (overlay.categoryType.category) {
            FacilityCategory.ELEVATOR -> 16.dp
            FacilityCategory.BRAILLE_BLOCK -> 15.dp
            else -> 14.dp
        }

    Box(modifier = modifier.zIndex(overlay.zIndex)) {
        Surface(
            modifier =
                Modifier
                    .offset {
                        IntOffset(
                            x = overlay.screenPoint.x - (markerSizePx / 2),
                            y = overlay.screenPoint.y - (markerSizePx / 2),
                        )
                    }.size(markerSize)
                    .graphicsLayer {
                        rotationZ = if (isBrailleBlock) 45f else 0f
                    }.semantics {
                        contentDescription = overlay.contentDescription
                    }.clickable {
                        onClick(overlay.clickTargetId)
                    },
            shape =
                if (isBrailleBlock) {
                    RoundedCornerShape(10.dp)
                } else {
                    CircleShape
                },
            color = palette.container,
            border = BorderStroke(if (overlay.isSelected) 2.dp else 1.dp, if (overlay.isSelected) Color.White else palette.border),
            shadowElevation = if (overlay.isSelected) 10.dp else 6.dp,
        ) {
            Box {
                Icon(
                    painter = painterResource(id = projectedFacilityMarkerGlyphResId(overlay.categoryType.category)),
                    contentDescription = null,
                    tint = palette.content,
                    modifier =
                        Modifier
                            .size(iconSize)
                            .offset {
                                IntOffset(
                                    x = (markerSizePx - with(density) { iconSize.roundToPx() }) / 2,
                                    y = (markerSizePx - with(density) { iconSize.roundToPx() }) / 2,
                                )
                            }
                            .graphicsLayer {
                                rotationZ = if (isBrailleBlock) -45f else 0f
                            },
                )
            }
        }
    }
}

@Composable
private fun MapProjectedMarkerOverlay(
    overlay: KakaoProjectedMarkerOverlay,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val markerSize = overlay.sizeDp.dp
    val markerWidthPx = with(density) { markerSize.roundToPx() }
    val markerHeightPx = markerWidthPx
    Box(modifier = modifier.zIndex(overlay.zIndex)) {
        Image(
            painter = painterResource(id = overlay.iconResId),
            contentDescription = contentDescription,
            modifier =
                Modifier
                    .offset {
                        IntOffset(
                            x = overlay.screenPoint.x - (markerWidthPx * overlay.anchorPointX).toInt(),
                            y = overlay.screenPoint.y - (markerHeightPx * overlay.anchorPointY).toInt(),
                        )
                    }
                    .size(markerSize),
        )
    }
}

@Composable
private fun KakaoProjectedMarkerOverlay.resolveContentDescription(
    selectedDestinationName: String?,
): String =
    when (kind) {
        KakaoProjectedMarkerKind.CURRENT_LOCATION ->
            stringResource(id = R.string.navigation_map_marker_current)

        KakaoProjectedMarkerKind.SELECTED_DESTINATION ->
            selectedDestinationName
                ?: stringResource(id = R.string.map_viewport_description_selected)

        KakaoProjectedMarkerKind.SELECTED_MAP_PIN ->
            stringResource(id = R.string.map_viewport_description_selected)
    }

private fun projectedFacilityMarkerPalette(category: FacilityCategory): ProjectedFacilityMarkerPalette =
    when (category) {
        FacilityCategory.TOILET ->
            ProjectedFacilityMarkerPalette(
                container = Color(0xFF00897B),
                border = Color(0xFFBFEDE7),
                content = Color.White,
            )

        FacilityCategory.ELEVATOR ->
            ProjectedFacilityMarkerPalette(
                container = Color(0xFF5E7A2F),
                border = Color(0xFFDDE8C8),
                content = Color.White,
            )

        FacilityCategory.CHARGING_STATION ->
            ProjectedFacilityMarkerPalette(
                container = Color(0xFF9C5F00),
                border = Color(0xFFF1D6AA),
                content = Color.White,
            )

        FacilityCategory.FOOD_CAFE,
        FacilityCategory.RESTAURANT,
        ->
            ProjectedFacilityMarkerPalette(
                container = Color(0xFFD96A39),
                border = Color(0xFFF7D3C3),
                content = Color.White,
            )

        FacilityCategory.TOURIST_SPOT,
        FacilityCategory.TOURIST_ATTRACTION,
        ->
            ProjectedFacilityMarkerPalette(
                container = Color(0xFF1976D2),
                border = Color(0xFFC7E0FF),
                content = Color.White,
            )

        FacilityCategory.ACCOMMODATION ->
            ProjectedFacilityMarkerPalette(
                container = Color(0xFF8D6E63),
                border = Color(0xFFE5D4CD),
                content = Color.White,
            )

        FacilityCategory.HEALTHCARE ->
            ProjectedFacilityMarkerPalette(
                container = Color(0xFFC62828),
                border = Color(0xFFF5C4C4),
                content = Color.White,
            )

        FacilityCategory.WELFARE ->
            ProjectedFacilityMarkerPalette(
                container = Color(0xFF2E7D6B),
                border = Color(0xFFC7E7DE),
                content = Color.White,
            )

        FacilityCategory.PUBLIC_OFFICE ->
            ProjectedFacilityMarkerPalette(
                container = Color(0xFF546E7A),
                border = Color(0xFFD1DADF),
                content = Color.White,
            )

        FacilityCategory.BRAILLE_BLOCK ->
            ProjectedFacilityMarkerPalette(
                container = Color(0xFF7A5A1D),
                border = Color(0xFFF0DEB7),
                content = Color.White,
            )

        FacilityCategory.OTHER ->
            ProjectedFacilityMarkerPalette(
                container = Color(0xFF2563EB),
                border = Color(0xFFDBEAFE),
                content = Color.White,
            )
    }

@androidx.annotation.DrawableRes
private fun projectedFacilityMarkerGlyphResId(category: FacilityCategory): Int =
    when (category) {
        FacilityCategory.TOILET -> R.drawable.ic_place_restroom
        FacilityCategory.ELEVATOR -> R.drawable.ic_lowvision_category_elevator
        FacilityCategory.CHARGING_STATION -> R.drawable.ic_place_charging
        FacilityCategory.FOOD_CAFE -> R.drawable.ic_place_cafe
        FacilityCategory.TOURIST_SPOT -> R.drawable.ic_nav_facility
        FacilityCategory.ACCOMMODATION -> R.drawable.ic_place_accommodation
        FacilityCategory.HEALTHCARE -> R.drawable.ic_place_healthcare
        FacilityCategory.WELFARE -> R.drawable.ic_place_welfare
        FacilityCategory.PUBLIC_OFFICE -> R.drawable.ic_place_public_office
        FacilityCategory.BRAILLE_BLOCK -> R.drawable.ic_route_tactile_blocks
        FacilityCategory.RESTAURANT -> R.drawable.ic_place_restaurant
        FacilityCategory.TOURIST_ATTRACTION -> R.drawable.ic_nav_facility
        FacilityCategory.OTHER -> R.drawable.ic_nav_facility
    }
