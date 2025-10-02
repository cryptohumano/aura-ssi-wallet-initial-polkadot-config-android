package com.aura.substratecryptotest.data.services

import com.aura.substratecryptotest.data.models.MilestoneDetails
import kotlin.math.*

/**
 * Servicio para calcular estadísticas GPS de milestones
 */
class GpsCalculatorService {

    /**
     * Calcula la distancia entre dos puntos GPS usando fórmula de Haversine
     */
    fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val R = 6371000.0 // Radio de la Tierra en metros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    /**
     * Calcula la distancia total recorrida en una expedición
     */
    fun calculateTotalDistance(milestoneDetails: List<MilestoneDetails>): Double {
        if (milestoneDetails.size < 2) return 0.0

        var totalDistance = 0.0
        for (i in 1 until milestoneDetails.size) {
            val prev = milestoneDetails[i - 1]
            val current = milestoneDetails[i]
            
            if (prev.gpsData != null && current.gpsData != null) {
                val distance = calculateDistance(
                    prev.gpsData.latitude, prev.gpsData.longitude,
                    current.gpsData.latitude, current.gpsData.longitude
                )
                totalDistance += distance
            }
        }
        return totalDistance
    }

    /**
     * Calcula la velocidad promedio entre dos milestones
     */
    fun calculateAverageSpeed(
        milestone1: MilestoneDetails,
        milestone2: MilestoneDetails
    ): Double? {
        if (milestone1.gpsData == null || milestone2.gpsData == null) return null

        val distance = calculateDistance(
            milestone1.gpsData.latitude, milestone1.gpsData.longitude,
            milestone2.gpsData.latitude, milestone2.gpsData.longitude
        )

        val timeDiff = milestone2.milestone.timestamp.time - milestone1.milestone.timestamp.time
        if (timeDiff <= 0) return null

        val timeInHours = timeDiff / (1000.0 * 60 * 60) // Convertir a horas
        return distance / (timeInHours * 1000) // km/h
    }

    /**
     * Calcula el desnivel total (ascenso + descenso)
     */
    fun calculateTotalElevationGain(milestoneDetails: List<MilestoneDetails>): Double {
        if (milestoneDetails.size < 2) return 0.0

        var totalGain = 0.0
        for (i in 1 until milestoneDetails.size) {
            val prev = milestoneDetails[i - 1]
            val current = milestoneDetails[i]
            
            if (prev.gpsData?.altitude != null && current.gpsData?.altitude != null) {
                val elevationDiff = current.gpsData.altitude - prev.gpsData.altitude
                if (elevationDiff > 0) {
                    totalGain += elevationDiff
                }
            }
        }
        return totalGain
    }

    /**
     * Calcula el desnivel total (ascenso + descenso)
     */
    fun calculateTotalElevationLoss(milestoneDetails: List<MilestoneDetails>): Double {
        if (milestoneDetails.size < 2) return 0.0

        var totalLoss = 0.0
        for (i in 1 until milestoneDetails.size) {
            val prev = milestoneDetails[i - 1]
            val current = milestoneDetails[i]
            
            if (prev.gpsData?.altitude != null && current.gpsData?.altitude != null) {
                val elevationDiff = current.gpsData.altitude - prev.gpsData.altitude
                if (elevationDiff < 0) {
                    totalLoss += abs(elevationDiff)
                }
            }
        }
        return totalLoss
    }

    /**
     * Calcula la altitud máxima y mínima
     */
    fun calculateAltitudeRange(milestoneDetails: List<MilestoneDetails>): Pair<Double?, Double?> {
        val altitudes = milestoneDetails
            .mapNotNull { it.gpsData?.altitude }
            .filter { it > 0 }

        if (altitudes.isEmpty()) return Pair(null, null)

        return Pair(altitudes.maxOrNull(), altitudes.minOrNull())
    }

    /**
     * Calcula el tiempo total de la expedición
     */
    fun calculateTotalTime(milestoneDetails: List<MilestoneDetails>): Long? {
        if (milestoneDetails.size < 2) return null

        val first = milestoneDetails.first().milestone.timestamp
        val last = milestoneDetails.last().milestone.timestamp

        return last.time - first.time
    }

    /**
     * Genera estadísticas completas de la expedición
     */
    fun generateExpeditionStats(milestoneDetails: List<MilestoneDetails>): ExpeditionStats {
        val totalDistance = calculateTotalDistance(milestoneDetails)
        val totalTime = calculateTotalTime(milestoneDetails)
        val totalGain = calculateTotalElevationGain(milestoneDetails)
        val totalLoss = calculateTotalElevationLoss(milestoneDetails)
        val altitudeRange = calculateAltitudeRange(milestoneDetails)

        val averageSpeed = if (totalTime != null && totalTime > 0) {
            val timeInHours = totalTime / (1000.0 * 60 * 60)
            totalDistance / (timeInHours * 1000) // km/h
        } else null

        return ExpeditionStats(
            totalDistance = totalDistance,
            totalTime = totalTime,
            averageSpeed = averageSpeed,
            totalElevationGain = totalGain,
            totalElevationLoss = totalLoss,
            maxAltitude = altitudeRange.first,
            minAltitude = altitudeRange.second,
            milestoneCount = milestoneDetails.size
        )
    }
}

/**
 * Estadísticas de una expedición
 */
data class ExpeditionStats(
    val totalDistance: Double, // metros
    val totalTime: Long?, // milisegundos
    val averageSpeed: Double?, // km/h
    val totalElevationGain: Double, // metros
    val totalElevationLoss: Double, // metros
    val maxAltitude: Double?, // metros
    val minAltitude: Double?, // metros
    val milestoneCount: Int
) {
    fun getFormattedDistance(): String {
        return when {
            totalDistance >= 1000 -> "${String.format("%.2f", totalDistance / 1000)} km"
            else -> "${String.format("%.0f", totalDistance)} m"
        }
    }

    fun getFormattedTime(): String {
        if (totalTime == null) return "N/A"
        val hours = totalTime / (1000 * 60 * 60)
        val minutes = (totalTime % (1000 * 60 * 60)) / (1000 * 60)
        return "${hours}h ${minutes}m"
    }

    fun getFormattedSpeed(): String {
        return if (averageSpeed != null) {
            "${String.format("%.1f", averageSpeed)} km/h"
        } else "N/A"
    }

    fun getFormattedElevationGain(): String {
        return "${String.format("%.0f", totalElevationGain)} m"
    }

    fun getFormattedElevationLoss(): String {
        return "${String.format("%.0f", totalElevationLoss)} m"
    }

    fun getFormattedMaxAltitude(): String {
        return if (maxAltitude != null) {
            "${String.format("%.0f", maxAltitude)} m"
        } else "N/A"
    }

    fun getFormattedMinAltitude(): String {
        return if (minAltitude != null) {
            "${String.format("%.0f", minAltitude)} m"
        } else "N/A"
    }
}



