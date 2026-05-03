package com.ssafy.e102.eumgil.feature.report

interface NetworkMonitor {
    fun isOnline(): Boolean
}

object DefaultNetworkMonitor : NetworkMonitor {
    override fun isOnline(): Boolean = true
}
