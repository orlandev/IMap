package com.orlandev.imapbox

import HeaderSection
import SantiagoCard
import SantiagoSceneryItem
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.annotation.ExperimentalCoilApi
import com.inmersoft.santiago.R
import com.inmersoft.santiago.data.entity.SantiagoSceneryEntity
import com.inmersoft.santiago.data.entity.toMapPoint
import com.inmersoft.santiago.extensions.extractNames
import com.inmersoft.santiago.ui.SharedViewModel
import com.orlandev.imapbox.comp.GenericMap
import com.inmersoft.santiago.ui.navigation.NavigationRoute
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.orlandev.imapbox.NORMAL_ZOOM_START
import com.orlandev.imapbox.ZOOM_POINT_CLICKED

@ExperimentalCoilApi
@Composable
fun MapScreen(sharedViewModel: SharedViewModel, navController: NavHostController, sceneryId: Int?) {

    val isDarkMode = isSystemInDarkTheme()
    val sceneries = sharedViewModel.allSceneryEntity.collectAsState(initial = null)
    val cardInfoScenerySelected = remember {
        mutableStateOf<SantiagoSceneryEntity?>(null)
    }

    val mapMoveListener = object : OnMoveListener {
        override fun onMove(detector: MoveGestureDetector): Boolean {
            cardInfoScenerySelected.value = null
            return false
        }

        override fun onMoveBegin(detector: MoveGestureDetector) {
            cardInfoScenerySelected.value = null
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {
            cardInfoScenerySelected.value = null
        }


    }

    Box(modifier = Modifier.fillMaxSize()) {
        sceneries.value?.let { allSceneries ->

            var mapZoomer= NORMAL_ZOOM_START
            val sceneryFromDetails = allSceneries.firstOrNull() { it.id == sceneryId }
            if (sceneryId != -1) {
                if (sceneryFromDetails != null) {
                    cardInfoScenerySelected.value = sceneryFromDetails
                    mapZoomer= ZOOM_POINT_CLICKED
                }
            }

            GenericMap(
                allSceneries.map { it.toMapPoint() },
                startPoint = sceneryFromDetails?.toMapPoint(),
                zoomStart = mapZoomer,
                isDark = isDarkMode,
                onMoveListener = mapMoveListener
            ) { sceneryPointClickedId ->
                val scenerySelected = allSceneries.firstOrNull() { it.id == sceneryPointClickedId }
                cardInfoScenerySelected.value = scenerySelected
            }
        }
        AnimatedVisibility(
            enter = slideInVertically(
                initialOffsetY = { 100 }
            ) + fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .padding(bottom = 57.dp)
                .align(Alignment.BottomCenter),
            visible = cardInfoScenerySelected.value != null
        ) {
            cardInfoScenerySelected.value?.let { currentSelectedScenery ->
                SantiagoCard(
                    headerContent = {
                        HeaderSection(leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.map_location),
                                contentDescription = null
                            )
                        }, text = currentSelectedScenery.scenary_name.extractNames("-")[0])
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(12.dp)
                ) {
                    SantiagoSceneryItem(
                        modifier = Modifier
                            .fillMaxSize(), currentScenery = currentSelectedScenery
                    ) {
                        sharedViewModel.onNavigateToSceneryDetails(currentSelectedScenery.id)
                        navController.navigate(NavigationRoute.DetailScreen.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            // on the back stack as users select items
                            //popUpTo(navController.graph.findStartDestination().id) {
                            //     saveState = true
                            //}
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                }
            }
        }
    }
}
