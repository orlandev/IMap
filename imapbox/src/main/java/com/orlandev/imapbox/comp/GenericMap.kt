package com.orlandev.imapbox.comp

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.orlandev.imapbox.MapPoint
import com.orlandev.imapbox.ZOOM_POINT_CLICKED
import com.orlandev.imapbox.findByHashCodeOrNull
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import kotlinx.coroutines.launch

@Composable
fun GenericMap(
    listOfPoints: List<MapPoint>,
    zoomStart: Double,
    isDark: Boolean,
    startPoint: MapPoint?,
    mapIcon: Bitmap,
    onMoveListener: OnMoveListener,
    onPointAnnotationClickListener: (Int) -> Unit
) {
    val map = rememberMapboxViewWithLifecycle()
    map.gestures.addOnMoveListener(onMoveListener = onMoveListener)
    MapboxMapContainer(
        map = map,
        listOfPoints,
        startPoint = startPoint,
        zoom = zoomStart,
        isDark = isDark,
        mapIcon = mapIcon,
        onPointAnnotationClickListener = {
            if (it.textField != null) {
                val hashCode = "${it.point}${it.textField}"
                val sceneryResult = listOfPoints.findByHashCodeOrNull(hashCode)
                if (sceneryResult != null) {
                    Log.d("SMAP", "OK SCENERY RESUL IS NOT NULL")
                    map.getMapboxMap().centerTo(
                        lat = sceneryResult.location.latitude(),
                        lng = sceneryResult.location.longitude(),
                        zoom = ZOOM_POINT_CLICKED
                    )
                    onPointAnnotationClickListener(sceneryResult.sceneryID)
                }
            }
            false
        }
    )
}

@Composable()
private fun MapboxMapContainer(
    map: MapView,
    listOfPoints: List<MapPoint>,
    zoom: Double,
    isDark: Boolean,
    startPoint: MapPoint?, mapIcon: Bitmap,
    onPointAnnotationClickListener: OnPointAnnotationClickListener
) {

    val (isMapInitialized, setMapInitialized) = remember(map) { mutableStateOf(false) }

    LaunchedEffect(map, isMapInitialized) {
        if (!isMapInitialized) {
            val mbxMap = map.getMapboxMap()
            mbxMap.loadStyleUri(if (isDark) Style.DARK else Style.OUTDOORS) {
                when {
                    startPoint != null -> {
                        mbxMap.centerTo(
                            lat = startPoint.location.latitude(),
                            lng = startPoint.location.longitude(),
                            zoom = zoom
                        )
                    }
                    else -> {
                        mbxMap.centerTo(
                            lat = listOfPoints[0].location.latitude(),
                            lng = listOfPoints[0].location.longitude(),
                            zoom = zoom
                        )
                    }
                }
                setMapInitialized(true)
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    if (isMapInitialized) {
        AndroidView(factory = { map }) { mapView ->
            coroutineScope.launch {
                val mbxMap = mapView.getMapboxMap()
                if (listOfPoints.isNotEmpty()) {
                    when {
                        startPoint != null -> {
                            mbxMap.centerTo(
                                lat = startPoint.location.latitude(),
                                lng = startPoint.location.longitude(),
                                zoom = zoom
                            )
                        }
                        else -> {
                            mbxMap.centerTo(
                                lat = listOfPoints[0].location.latitude(),
                                lng = listOfPoints[0].location.longitude(),
                                zoom = zoom
                            )
                        }
                    }
                    map.addPointAnnotation(
                        listOfPoints,
                        bitmapMapIcon = mapIcon,
                        onPointAnnotationClickListener = onPointAnnotationClickListener
                    )
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

    }
}


@Composable
private fun rememberMapboxViewWithLifecycle(): MapView {
    val context = LocalContext.current

    val opt = MapInitOptions(context, plugins = MapInitOptions.defaultPluginList)
    val map = remember { MapView(context, opt) }

    val observer = rememberMapboxViewLifecycleObserver(map)
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle) {
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    return map
}

@Composable
private fun rememberMapboxViewLifecycleObserver(map: MapView): LifecycleEventObserver {
    return remember(map) {
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> map.onStart()
                Lifecycle.Event.ON_STOP -> map.onStop()
                Lifecycle.Event.ON_DESTROY -> map.onDestroy()
                else -> Unit // nop
            }
        }
    }
}


private fun MapboxMap.centerTo(lat: Double, lng: Double, zoom: Double) {
    val point = Point.fromLngLat(lng, lat)

    val camera = CameraOptions.Builder()
        .center(point)
        .zoom(zoom)
        .build()

    val animations = MapAnimationOptions.mapAnimationOptions {
        duration(1000)
    }
    this.flyTo(cameraOptions = camera, animationOptions = animations)

    //setCamera(camera)
}

private fun MapView.addPointAnnotation(
    listOfPoints: List<MapPoint>,
    bitmapMapIcon: Bitmap,
    onPointAnnotationClickListener: OnPointAnnotationClickListener
) {
    val annotationApi = this.annotations
    val pointAnnotationManager = annotationApi.createPointAnnotationManager(this)

// Set options for the resulting line layer.
    val pointAnnotationOptions = mutableListOf<PointAnnotationOptions>()

    listOfPoints.forEach { currentPoint ->
        pointAnnotationOptions.add(
            PointAnnotationOptions()
                .withPoint(currentPoint.location)
                .withTextField(currentPoint.placeText)
                .withTextOffset(listOf(0.0, 2.5))
                .withTextSize(14.0)
                .withTextColor(Color.BLACK)
                .withTextHaloWidth(0.8)
                .withTextHaloColor(Color.WHITE)
                .withIconSize(1.3)
                // Style the line that will be added to the map.
                .withIconImage(bitmapMapIcon)
        )
    }
    pointAnnotationManager.create(pointAnnotationOptions)
    pointAnnotationManager.addClickListener(onPointAnnotationClickListener)
}
