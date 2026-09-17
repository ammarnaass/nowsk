package com.example.data.qibla

import kotlin.math.*

object QiblaCalculator {
    const val KAABA_LATITUDE = 21.4225241
    const val KAABA_LONGITUDE = 39.8261818

    /**
     * Calculates the Qibla angle (azimuth from True North clockwise in degrees: 0..360)
     */
    fun calculateQiblaAngle(userLat: Double, userLon: Double): Double {
        val phi1 = Math.toRadians(userLat)
        val phi2 = Math.toRadians(KAABA_LATITUDE)
        val deltaLambda = Math.toRadians(KAABA_LONGITUDE - userLon)

        val y = sin(deltaLambda)
        val x = cos(phi1) * tan(phi2) - sin(phi1) * cos(deltaLambda)

        var qibla = Math.toDegrees(atan2(y, x))
        qibla = (qibla + 360.0) % 360.0
        return qibla
    }

    /**
     * Calculates distance to Kaaba in kilometers using Haversine formula
     */
    fun calculateDistanceToKaabaKm(userLat: Double, userLon: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(KAABA_LATITUDE - userLat)
        val dLon = Math.toRadians(KAABA_LONGITUDE - userLon)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(userLat)) * cos(Math.toRadians(KAABA_LATITUDE)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}
