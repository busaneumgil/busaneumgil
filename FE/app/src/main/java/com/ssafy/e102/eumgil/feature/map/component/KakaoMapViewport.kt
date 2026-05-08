package com.ssafy.e102.eumgil.feature.map.component

import android.content.Context
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions

@Composable
internal fun KakaoMapViewport(
    state: MapViewportUiState,
    onMarkerClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { KakaoMapViewportController() }
    val rendererFailure = controller.rendererFailure

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
                )
            },
            modifier = Modifier.fillMaxSize(),
            update = {
                controller.render(
                    state = state,
                    onMarkerClick = onMarkerClick,
                )
            },
        )

        if (controller.rendererStatus != KakaoRendererStatus.Ready) {
            MapFallbackSurface(
                markerOverlayState = state.markerOverlayState,
                overlayState = state.overlayState,
                regionLabel = state.regionLabel,
                statusLabel =
                    if (rendererFailure != null) {
                        stringResource(id = R.string.map_viewport_status_renderer_error)
                    } else {
                        state.statusLabel
                    },
                title = state.title,
                description =
                    if (rendererFailure != null) {
                        stringResource(id = R.string.map_viewport_description_renderer_error)
                    } else {
                        state.description
                    },
                supportingText =
                    if (rendererFailure != null) {
                        stringResource(
                            id = R.string.map_viewport_supporting_renderer_error,
                            rendererFailure.debugSummary,
                        )
                    } else {
                        state.supportingText
                    },
                onMarkerClick = onMarkerClick,
                modifier = Modifier.fillMaxSize(),
            )
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
    private var lastRenderedCameraRequestId: Long? = null
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
    ): MapView {
        latestState = initialState
        markerClickHandler = onMarkerClick

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
    ) {
        latestState = state
        markerClickHandler = onMarkerClick
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
        lastRenderedMarkers = emptyList()
    }

    private fun startMap(createdMapView: MapView) {
        if (isStarted || isFinished) return

        isStarted = true
        Log.i(KAKAO_MAP_LOG_TAG, "Starting Kakao map renderer")
        createdMapView.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() {
                    rendererStatus = KakaoRendererStatus.Initializing
                    rendererFailure = null
                    kakaoMap = null
                    hasMapLifecycleResumed = false
                    lifecycleDispatchRetryCount = 0
                    lastRenderedCameraRequestId = null
                    lastRenderedMarkers = emptyList()
                    Log.i(KAKAO_MAP_LOG_TAG, "Kakao map renderer destroyed")
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
        val cameraState = createKakaoCameraRenderState(state.cameraTarget)
        if (lastRenderedCameraRequestId == cameraState.requestId) return

        readyMap.moveCamera(
            CameraUpdateFactory.newCenterPosition(
                LatLng.from(cameraState.latitude, cameraState.longitude),
                cameraState.zoomLevel,
            ),
        )
        lastRenderedCameraRequestId = cameraState.requestId
        Log.d(
            KAKAO_MAP_LOG_TAG,
            "Camera synced ${createKakaoCameraDebugSummary(state.cameraTarget)}",
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
                    .setStyles(marker.iconResId)
                    .setClickable(true)
                    .setRank(marker.rank)
                    .setTag(marker.markerId),
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
