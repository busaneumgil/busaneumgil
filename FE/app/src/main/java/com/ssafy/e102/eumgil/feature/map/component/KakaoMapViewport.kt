package com.ssafy.e102.eumgil.feature.map.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.appcompat.content.res.AppCompatResources
import com.ssafy.e102.eumgil.R
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.GestureType
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.CompetitionType
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelManager
import com.kakao.vectormap.label.OrderingType
import com.kakao.vectormap.route.RouteLineOptions
import com.kakao.vectormap.route.RouteLineSegment
import com.kakao.vectormap.route.RouteLineStyle
import com.ssafy.e102.eumgil.core.model.FacilityCategory
import com.ssafy.e102.eumgil.feature.map.MapTapClickType
import com.ssafy.e102.eumgil.feature.map.MapTapPayload
import com.ssafy.e102.eumgil.feature.map.model.MapCoordinate
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun KakaoMapViewport(
    state: MapViewportUiState,
    onMarkerClick: (String) -> Unit,
    onCameraMoveEnd: (MapCoordinate, Int, Boolean) -> Unit,
    onMapClick: (MapTapPayload) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var reloadGeneration by remember { mutableIntStateOf(0) }
    var isRendererRestarting by remember { mutableStateOf(false) }
    var attemptedAutomaticRecoveryCount by remember { mutableIntStateOf(0) }

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

        LaunchedEffect(controller, rendererFailure, attemptedAutomaticRecoveryCount) {
            val failure = rendererFailure ?: return@LaunchedEffect
            if (
                !shouldAutoRestartKakaoRenderer(
                    failure = failure,
                    attemptedAutomaticRecoveryCount = attemptedAutomaticRecoveryCount,
                )
            ) {
                return@LaunchedEffect
            }

            controller.finish()
            attemptedAutomaticRecoveryCount += 1
            isRendererRestarting = true
            reloadGeneration += 1
        }

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
                controller.projectedMarkerOverlays.forEach { overlay ->
                    MapProjectedMarkerOverlay(
                        overlay = overlay,
                        contentDescription =
                            overlay.resolveContentDescription(
                                selectedDestinationName = state.selectedDestinationName,
                            ),
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
                                attemptedAutomaticRecoveryCount = 0
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
    var projectedMarkerOverlays by mutableStateOf<List<KakaoProjectedMarkerOverlay>>(emptyList())
        private set

    private var mapView: MapView? = null
    private var kakaoMap: KakaoMap? = null
    private var latestState: MapViewportUiState? = null
    private var markerClickHandler: ((String) -> Unit)? = null
    private var cameraMoveEndHandler: ((MapCoordinate, Int, Boolean) -> Unit)? = null
    private var mapClickHandler: ((MapTapPayload) -> Unit)? = null
    private var facilityMarkerStyleCache: KakaoFacilityMarkerStyleCache? = null
    private var lastRenderedCameraRequestId: Long? = null
    private var lastRenderedCameraTarget: com.ssafy.e102.eumgil.feature.map.model.MapCameraTarget? = null
    private var lastRenderedRouteCameraSignature: Int? = null
    private var lastRenderedMarkers: List<KakaoMarkerRenderState> = emptyList()
    private var lastRenderedRouteLines: List<KakaoRouteLineRenderState> = emptyList()
    private var lastDispatchedMapTapCoordinate: MapCoordinate? = null
    private var lastDispatchedMapTapUptimeMillis: Long = 0L
    private var lastDispatchedMarkerTapId: String? = null
    private var lastDispatchedMarkerTapUptimeMillis: Long = 0L
    private var lastSuppressedTerrainTapCoordinate: MapCoordinate? = null
    private var lastSuppressedTerrainTapUptimeMillis: Long = 0L
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
        onMapClick: (MapTapPayload) -> Unit,
    ): MapView {
        latestState = initialState
        markerClickHandler = onMarkerClick
        cameraMoveEndHandler = onCameraMoveEnd
        mapClickHandler = onMapClick

        return mapView ?: MapView(context).also { createdMapView ->
            Log.i(KAKAO_MAP_LOG_TAG, "Creating Kakao MapView instance")
            createdMapView.addOnAttachStateChangeListener(attachStateListener)
            mapView = createdMapView
            facilityMarkerStyleCache = KakaoFacilityMarkerStyleCache(context = context)
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
        onMapClick: (MapTapPayload) -> Unit,
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
        projectedMarkerOverlays = emptyList()
        facilityMarkerStyleCache?.clear()
        facilityMarkerStyleCache = null
        lastRenderedCameraRequestId = null
        lastRenderedCameraTarget = null
        lastRenderedRouteCameraSignature = null
        lastRenderedMarkers = emptyList()
        lastRenderedRouteLines = emptyList()
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
                    readyMap.setPoiClickable(true)
                    readyMap.setOnLabelClickListener { _, _, label ->
                        ((label.getTag() as? String) ?: label.labelId)?.let { markerId ->
                            dispatchMarkerTap(
                                markerId = markerId,
                                position = label.position,
                            )
                            true
                        } ?: false
                    }
                    readyMap.setOnPoiClickListener { _, position, layerId, poiId ->
                        if (layerId == KAKAO_MARKER_LAYER_ID && poiId.isNotBlank()) {
                            dispatchMarkerTap(
                                markerId = poiId,
                                position = position,
                            )
                        } else if (poiId.isNotBlank()) {
                            Log.d(
                                KAKAO_MAP_LOG_TAG,
                                "Skipping external POI tap without nameHint providerPlaceId=$poiId",
                            )
                        }
                    }
                    readyMap.setOnTerrainClickListener { _, position, _ ->
                        dispatchMapTap(
                            source = "terrain",
                            position = position,
                            clickType = MapTapClickType.ADDRESS,
                        )
                    }
                    readyMap.setOnMapClickListener { _, position, _, poi ->
                        if (poi?.isPoi == true && poi.layerId == KAKAO_MARKER_LAYER_ID && poi.poiId.isNotBlank()) {
                            dispatchMarkerTap(
                                markerId = poi.poiId,
                                position = position,
                            )
                        } else if (poi?.isPoi == true && poi.poiId.isNotBlank()) {
                            dispatchExternalPoiTap(
                                position = position,
                                providerPlaceId = poi.poiId,
                                nameHint = poi.name,
                            )
                        } else if (poi == null) {
                            dispatchMapTap(source = "map", position = position, clickType = MapTapClickType.ADDRESS)
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
        syncRouteLines(readyMap = readyMap, state = state)
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
        val routeCameraState = createKakaoRouteCameraRenderState(state.overlayState)
        if (routeCameraState != null) {
            if (
                lastRenderedCameraRequestId == cameraState.requestId &&
                lastRenderedRouteCameraSignature == routeCameraState.signature
            ) {
                return
            }
            val routePoints =
                routeCameraState.points
                    .map { point -> LatLng.from(point.latitude, point.longitude) }
                    .toTypedArray()
            readyMap.moveCamera(
                CameraUpdateFactory.fitMapPoints(routePoints, KAKAO_ROUTE_CAMERA_PADDING),
            )
            lastRenderedCameraRequestId = cameraState.requestId
            lastRenderedCameraTarget = currentTarget
            lastRenderedRouteCameraSignature = routeCameraState.signature
            Log.d(
                KAKAO_MAP_LOG_TAG,
                "Route camera fitted points=${routePoints.size} requestId=${cameraState.requestId}",
            )
            return
        }
        lastRenderedRouteCameraSignature = null
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

    private fun syncRouteLines(
        readyMap: KakaoMap,
        state: MapViewportUiState,
    ) {
        val routeLineStates = createKakaoRouteLineRenderStates(state.overlayState.polylines)
        if (lastRenderedRouteLines == routeLineStates) return

        val routeLineManager = readyMap.routeLineManager ?: return
        routeLineManager.clearAll()
        if (routeLineStates.isNotEmpty()) {
            val routeLineLayer =
                routeLineManager.addLayer(
                    KAKAO_ROUTE_LINE_LAYER_ID,
                    KAKAO_ROUTE_LINE_LAYER_Z_ORDER,
                ) ?: return
            routeLineStates.forEach { routeLine ->
                val points =
                    routeLine.points.map { point ->
                        LatLng.from(point.latitude, point.longitude)
                    }
                val style =
                    RouteLineStyle.from(
                        routeLine.lineWidth,
                        routeLine.lineColor,
                        routeLine.strokeWidth,
                        routeLine.strokeColor,
                    )
                val segment = RouteLineSegment.from(points, style)
                routeLineLayer.addRouteLine(
                    RouteLineOptions
                        .from(routeLine.routeLineId, segment)
                        .setZOrder(routeLine.zOrder),
                )
            }
        }
        lastRenderedRouteLines = routeLineStates
        Log.d(
            KAKAO_MAP_LOG_TAG,
            "Route lines synced count=${routeLineStates.size}",
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

        val labelManager = readyMap.labelManager ?: return
        val markerStyleCache = facilityMarkerStyleCache ?: return
        labelManager.removeAllLabelLayer()
        if (markerRenderStates.isNotEmpty()) {
            val markerLayer =
                labelManager.addLayer(
                    LabelLayerOptions
                        .from(KAKAO_MARKER_LAYER_ID)
                        .setCompetitionType(CompetitionType.None)
                        .setOrderingType(OrderingType.Rank)
                        .setClickable(true)
                        .setZOrder(KAKAO_MARKER_LAYER_Z_ORDER),
                ) ?: return
            markerLayer.setClickable(true)
            markerRenderStates
                .sortedWith(compareByDescending<KakaoMarkerRenderState> { it.rank }.thenBy { it.markerId })
                    .forEach { marker ->
                        val labelStyles = markerStyleCache.stylesFor(labelManager, marker)
                        val label =
                            markerLayer.addLabel(
                                LabelOptions
                                    .from(
                                        marker.markerId,
                                        LatLng.from(marker.latitude, marker.longitude),
                                    ).setStyles(labelStyles)
                                    .setRank(marker.rank)
                                    .setClickable(marker.clickTargetId != null)
                                    .setTag(marker.clickTargetId ?: marker.markerId),
                            ) ?: return@forEach
                        marker.clickTargetId?.let(label::setTag)
                        label.setClickable(marker.clickTargetId != null)
                    }
        }
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
        clickType: MapTapClickType,
        providerPlaceId: String? = null,
        nameHint: String? = null,
    ) {
        val coordinate =
            MapCoordinate(
                latitude = position.latitude,
                longitude = position.longitude,
            )
        val now = SystemClock.elapsedRealtime()
        val suppressedByMarkerTap =
            source == "terrain" &&
                isSuppressedByRecentMarkerTap(coordinate = coordinate, now = now)
        val previousCoordinate = lastDispatchedMapTapCoordinate
        val isDuplicate =
            previousCoordinate != null &&
                now - lastDispatchedMapTapUptimeMillis <= KAKAO_MAP_TAP_DEDUP_WINDOW_MILLIS &&
                abs(previousCoordinate.latitude - coordinate.latitude) <= KAKAO_MAP_TAP_DEDUP_COORDINATE_EPSILON &&
                abs(previousCoordinate.longitude - coordinate.longitude) <= KAKAO_MAP_TAP_DEDUP_COORDINATE_EPSILON
        if (suppressedByMarkerTap) return
        if (isDuplicate) return
        lastDispatchedMapTapCoordinate = coordinate
        lastDispatchedMapTapUptimeMillis = now
        mapClickHandler?.invoke(
            MapTapPayload(
                coordinate = coordinate,
                clickType = clickType,
                provider = if (clickType == MapTapClickType.POI) KAKAO_PROVIDER_NAME else null,
                providerPlaceId = providerPlaceId,
                nameHint = nameHint?.takeIf { it.isNotBlank() },
            ),
        )
    }

    private fun dispatchExternalPoiTap(
        position: LatLng,
        providerPlaceId: String,
        nameHint: String?,
    ) {
        dispatchMapTap(
            source = "poi",
            position = position,
            clickType = MapTapClickType.POI,
            providerPlaceId = providerPlaceId,
            nameHint = nameHint,
        )
    }

    private fun dispatchMarkerTap(
        markerId: String,
        position: LatLng,
    ) {
        val now = SystemClock.elapsedRealtime()
        val isDuplicate =
            lastDispatchedMarkerTapId == markerId &&
                now - lastDispatchedMarkerTapUptimeMillis <= KAKAO_MARKER_TAP_DEDUP_WINDOW_MILLIS
        if (isDuplicate) return

        lastDispatchedMarkerTapId = markerId
        lastDispatchedMarkerTapUptimeMillis = now
        lastSuppressedTerrainTapCoordinate =
            MapCoordinate(
                latitude = position.latitude,
                longitude = position.longitude,
            )
        lastSuppressedTerrainTapUptimeMillis = now
        markerClickHandler?.invoke(markerId)
    }

    private fun isSuppressedByRecentMarkerTap(
        coordinate: MapCoordinate,
        now: Long,
    ): Boolean {
        val previousCoordinate = lastSuppressedTerrainTapCoordinate ?: return false
        return now - lastSuppressedTerrainTapUptimeMillis <= KAKAO_MARKER_TAP_DEDUP_WINDOW_MILLIS &&
            abs(previousCoordinate.latitude - coordinate.latitude) <= KAKAO_MAP_TAP_DEDUP_COORDINATE_EPSILON &&
            abs(previousCoordinate.longitude - coordinate.longitude) <= KAKAO_MAP_TAP_DEDUP_COORDINATE_EPSILON
    }

    private fun updateProjectedMarkerOverlays(
        readyMap: KakaoMap,
        state: MapViewportUiState?,
    ) {
        val projectedMarkers =
            createKakaoProjectedMarkerRenderStates(
                currentLocation = state?.currentLocation,
                selectedDestinationCoordinate = state?.selectedDestinationCoordinate,
                selectedMapPinCoordinate = state?.selectedMapPinCoordinate,
                overlayPoints = state?.overlayState?.points.orEmpty(),
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
private const val KAKAO_ROUTE_LINE_LAYER_ID = "eumgil-route-lines"
private const val KAKAO_PROVIDER_NAME = "KAKAO"
private const val KAKAO_MAP_LOG_TAG = "KakaoMapViewport"
private const val MAX_LIFECYCLE_DISPATCH_RETRIES = 30
private const val LIFECYCLE_DISPATCH_RETRY_DELAY_MILLIS = 50L
private const val KAKAO_RENDERER_RESTART_DELAY_MILLIS = 220L
private const val KAKAO_RENDERER_READY_TIMEOUT_MILLIS = 4_000L
private const val KAKAO_MAP_TAP_DEDUP_WINDOW_MILLIS = 250L
private const val KAKAO_MARKER_TAP_DEDUP_WINDOW_MILLIS = 250L
private const val KAKAO_MAP_TAP_DEDUP_COORDINATE_EPSILON = 0.000001
private const val KAKAO_MARKER_LAYER_Z_ORDER = 1000
private const val KAKAO_ROUTE_LINE_LAYER_Z_ORDER = 900
private const val KAKAO_ROUTE_CAMERA_PADDING = 84

private fun GestureType.isUserDrivenCameraMove(): Boolean = this != GestureType.Unknown

private fun Double.toLogCoordinate(): String = String.format(Locale.US, "%.6f", this)

@Composable
private fun MapProjectedMarkerOverlay(
    overlay: KakaoProjectedMarkerOverlay,
    contentDescription: String?,
) {
    val density = LocalDensity.current
    val markerSize = overlay.sizeDp.dp
    val markerWidthPx = with(density) { markerSize.roundToPx() }
    val markerHeightPx = markerWidthPx
    val markerModifier =
        Modifier
            .zIndex(overlay.zIndex)
            .offset {
                IntOffset(
                    x = overlay.screenPoint.x - (markerWidthPx * overlay.anchorPointX).toInt(),
                    y = overlay.screenPoint.y - (markerHeightPx * overlay.anchorPointY).toInt(),
                )
            }
            .size(markerSize)

    if (overlay.kind == KakaoProjectedMarkerKind.ROUTE_SEGMENT_JUNCTION) {
        Box(
            modifier = markerModifier,
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
            ) {}
            Surface(
                modifier = Modifier.size((overlay.sizeDp * 0.58f).dp),
                shape = CircleShape,
                color = Color(0xFF2A7BFF),
                border = BorderStroke(1.dp, Color(0xFF0F4FC6)),
            ) {}
        }
        return
    }

    Image(
        painter = painterResource(id = overlay.iconResId),
        contentDescription = contentDescription,
        modifier = markerModifier,
    )
}

@Composable
private fun KakaoProjectedMarkerOverlay.resolveContentDescription(
    selectedDestinationName: String?,
): String? =
    when (kind) {
        KakaoProjectedMarkerKind.CURRENT_LOCATION ->
            stringResource(id = R.string.navigation_map_marker_current)

        KakaoProjectedMarkerKind.SELECTED_DESTINATION ->
            selectedDestinationName
                ?: stringResource(id = R.string.map_viewport_description_selected)

        KakaoProjectedMarkerKind.SELECTED_MAP_PIN ->
            stringResource(id = R.string.map_viewport_description_selected)

        KakaoProjectedMarkerKind.ROUTE_ORIGIN ->
            stringResource(id = R.string.navigation_map_marker_origin)

        KakaoProjectedMarkerKind.ROUTE_DESTINATION ->
            stringResource(id = R.string.navigation_map_marker_destination)

        KakaoProjectedMarkerKind.ROUTE_SEGMENT_JUNCTION -> null
    }

private class KakaoFacilityMarkerStyleCache(
    private val context: Context,
) {
    private val densityBucket = resolveDensityBucket(context.resources.displayMetrics.densityDpi)
    private val bitmapCache = mutableMapOf<KakaoFacilityMarkerBitmapCacheKey, Bitmap>()
    private val stylesCache = mutableMapOf<KakaoFacilityMarkerBitmapCacheKey, LabelStyles>()

    fun stylesFor(
        labelManager: LabelManager,
        marker: KakaoMarkerRenderState,
    ): LabelStyles {
        val key =
            KakaoFacilityMarkerBitmapCacheKey(
                category = marker.category,
                isSelected = marker.isSelected,
                densityBucket = densityBucket,
            )
        return stylesCache.getOrPut(key) {
            val styles =
                LabelStyles.from(
                    key.styleId,
                    LabelStyle
                        .from(bitmapFor(marker, key))
                        .setApplyDpScale(false)
                        .setAnchorPoint(marker.anchorPointX, marker.anchorPointY),
                )
            labelManager.addLabelStyles(styles) ?: styles
        }
    }

    fun clear() {
        stylesCache.clear()
        bitmapCache.clear()
    }

    private fun bitmapFor(
        marker: KakaoMarkerRenderState,
        key: KakaoFacilityMarkerBitmapCacheKey,
    ): Bitmap =
        bitmapCache.getOrPut(key) {
            createFacilityMarkerBitmap(
                category = marker.category,
                glyphResId = marker.glyphResId,
                isSelected = marker.isSelected,
                sizeDp = marker.sizeDp,
            )
        }

    private fun createFacilityMarkerBitmap(
        category: FacilityCategory,
        glyphResId: Int,
        isSelected: Boolean,
        sizeDp: Int,
    ): Bitmap {
        val sizePx = dpToPx(sizeDp.toFloat())
        val borderWidthPx = dpToPx(if (isSelected) 2f else 1f).coerceAtLeast(1f)
        val glyphSizePx = dpToPx(resolveFacilityMarkerGlyphSizeDp(category).toFloat())
        val bitmapSizePx = sizePx.roundToInt()
        val glyphSizeIntPx = glyphSizePx.roundToInt()
        val outerRect = RectF(0f, 0f, sizePx, sizePx)
        val innerRect = RectF(borderWidthPx, borderWidthPx, sizePx - borderWidthPx, sizePx - borderWidthPx)
        val palette = facilityMarkerPalette(category)
        val bitmap = Bitmap.createBitmap(bitmapSizePx, bitmapSizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val outerPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = if (isSelected) FACILITY_MARKER_SELECTED_RING_COLOR else palette.borderColor
            }
        val innerPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = palette.containerColor
            }

        if (category == FacilityCategory.BRAILLE_BLOCK) {
            val radiusPx = dpToPx(FACILITY_MARKER_BRAILLE_CORNER_RADIUS_DP)
            canvas.save()
            canvas.rotate(45f, sizePx / 2f, sizePx / 2f)
            canvas.drawRoundRect(outerRect, radiusPx, radiusPx, outerPaint)
            canvas.drawRoundRect(innerRect, radiusPx, radiusPx, innerPaint)
            canvas.restore()
        } else {
            val outerRadius = sizePx / 2f
            canvas.drawCircle(outerRadius, outerRadius, outerRadius, outerPaint)
            canvas.drawCircle(outerRadius, outerRadius, outerRadius - borderWidthPx, innerPaint)
        }

        val glyphDrawable =
            AppCompatResources
                .getDrawable(context, glyphResId)
                ?.mutate()
                ?: return bitmap
        DrawableCompat.setTint(glyphDrawable, palette.contentColor)
        val glyphLeft = ((sizePx - glyphSizePx) / 2f).toInt()
        val glyphTop = ((sizePx - glyphSizePx) / 2f).toInt()
        glyphDrawable.bounds =
            Rect(
                glyphLeft,
                glyphTop,
                glyphLeft + glyphSizeIntPx,
                glyphTop + glyphSizeIntPx,
            )
        glyphDrawable.draw(canvas)
        return bitmap
    }

    private fun dpToPx(dp: Float): Float = dp * context.resources.displayMetrics.density
}

private fun facilityMarkerPalette(category: FacilityCategory): KakaoFacilityMarkerPalette =
    when (category) {
        FacilityCategory.TOILET ->
            KakaoFacilityMarkerPalette(
                containerColor = 0xFF00897B.toInt(),
                borderColor = 0xFFBFEDE7.toInt(),
                contentColor = 0xFFFFFFFF.toInt(),
            )

        FacilityCategory.ELEVATOR ->
            KakaoFacilityMarkerPalette(
                containerColor = 0xFF5E7A2F.toInt(),
                borderColor = 0xFFDDE8C8.toInt(),
                contentColor = 0xFFFFFFFF.toInt(),
            )

        FacilityCategory.CHARGING_STATION ->
            KakaoFacilityMarkerPalette(
                containerColor = 0xFF9C5F00.toInt(),
                borderColor = 0xFFF1D6AA.toInt(),
                contentColor = 0xFFFFFFFF.toInt(),
            )

        FacilityCategory.FOOD_CAFE,
        FacilityCategory.RESTAURANT,
        ->
            KakaoFacilityMarkerPalette(
                containerColor = 0xFFD96A39.toInt(),
                borderColor = 0xFFF7D3C3.toInt(),
                contentColor = 0xFFFFFFFF.toInt(),
            )

        FacilityCategory.TOURIST_SPOT,
        FacilityCategory.TOURIST_ATTRACTION,
        ->
            KakaoFacilityMarkerPalette(
                containerColor = 0xFF1976D2.toInt(),
                borderColor = 0xFFC7E0FF.toInt(),
                contentColor = 0xFFFFFFFF.toInt(),
            )

        FacilityCategory.ACCOMMODATION ->
            KakaoFacilityMarkerPalette(
                containerColor = 0xFF8D6E63.toInt(),
                borderColor = 0xFFE5D4CD.toInt(),
                contentColor = 0xFFFFFFFF.toInt(),
            )

        FacilityCategory.HEALTHCARE ->
            KakaoFacilityMarkerPalette(
                containerColor = 0xFFC62828.toInt(),
                borderColor = 0xFFF5C4C4.toInt(),
                contentColor = 0xFFFFFFFF.toInt(),
            )

        FacilityCategory.WELFARE ->
            KakaoFacilityMarkerPalette(
                containerColor = 0xFF2E7D6B.toInt(),
                borderColor = 0xFFC7E7DE.toInt(),
                contentColor = 0xFFFFFFFF.toInt(),
            )

        FacilityCategory.PUBLIC_OFFICE ->
            KakaoFacilityMarkerPalette(
                containerColor = 0xFF546E7A.toInt(),
                borderColor = 0xFFD1DADF.toInt(),
                contentColor = 0xFFFFFFFF.toInt(),
            )

        FacilityCategory.BRAILLE_BLOCK ->
            KakaoFacilityMarkerPalette(
                containerColor = 0xFF7A5A1D.toInt(),
                borderColor = 0xFFF0DEB7.toInt(),
                contentColor = 0xFFFFFFFF.toInt(),
            )

        FacilityCategory.OTHER ->
            KakaoFacilityMarkerPalette(
                containerColor = 0xFF2563EB.toInt(),
                borderColor = 0xFFDBEAFE.toInt(),
                contentColor = 0xFFFFFFFF.toInt(),
            )
    }

private fun resolveFacilityMarkerGlyphSizeDp(category: FacilityCategory): Int =
    when (category) {
        FacilityCategory.ELEVATOR -> 16
        FacilityCategory.BRAILLE_BLOCK -> 15
        else -> 14
    }

private fun resolveDensityBucket(densityDpi: Int): Int =
    when {
        densityDpi >= DisplayMetrics.DENSITY_XXXHIGH -> DisplayMetrics.DENSITY_XXXHIGH
        densityDpi >= DisplayMetrics.DENSITY_XXHIGH -> DisplayMetrics.DENSITY_XXHIGH
        densityDpi >= DisplayMetrics.DENSITY_XHIGH -> DisplayMetrics.DENSITY_XHIGH
        densityDpi >= DisplayMetrics.DENSITY_HIGH -> DisplayMetrics.DENSITY_HIGH
        else -> DisplayMetrics.DENSITY_MEDIUM
    }

private data class KakaoFacilityMarkerBitmapCacheKey(
    val category: FacilityCategory,
    val isSelected: Boolean,
    val densityBucket: Int,
) {
    val styleId: String
        get() = "facility-${category.name.lowercase(Locale.US)}-${if (isSelected) "selected" else "normal"}-$densityBucket"
}

private data class KakaoFacilityMarkerPalette(
    val containerColor: Int,
    val borderColor: Int,
    val contentColor: Int,
)

private const val FACILITY_MARKER_SELECTED_RING_COLOR = -0x1
private const val FACILITY_MARKER_BRAILLE_CORNER_RADIUS_DP = 10f
