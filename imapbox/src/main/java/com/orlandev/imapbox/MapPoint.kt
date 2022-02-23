package com.orlandev.imapbox

import com.inmersoft.santiago.extensions.toHashCode
import com.mapbox.geojson.Point

data class MapPoint(
    val sceneryID: Int,
    val location: Point,
    val placeText: String,
)

//Return -1 if not found
fun List<MapPoint>.findByHashCodeOrNull(hashToSearch: String): MapPoint? {
    this.forEach {
        val hash = "${it.location}${it.placeText}".toHashCode()
        if (hash == hashToSearch) {
            return it
        }
    }
    return null
}
