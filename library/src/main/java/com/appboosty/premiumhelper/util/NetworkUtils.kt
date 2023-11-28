package com.appboosty.premiumhelper.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket


object NetworkUtils {

    private val IPV4_REGEX = "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$".toRegex()

    @RequiresApi(Build.VERSION_CODES.P)
    private fun loadPrivateDnsName(context: Context): String? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return null
        val linkProperties = cm.getLinkProperties(network) ?: return null
        return linkProperties.privateDnsServerName
    }

    fun getPrivateDNS(context: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            loadPrivateDnsName(context) ?: ""
        } else {
            "not supported"
        }
    }
    @Suppress("DEPRECATION")
    fun isVPNActive(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val info = cm.getNetworkInfo(ConnectivityManager.TYPE_VPN)
            info?.isConnected ?: false
        } else {
            val network = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(network)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ?: false
        }
    }

    fun getVPNDetails(): VPNDetails? {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val info = interfaces.nextElement()
            if (info.isUp && info.isPointToPoint) {
                info.interfaceAddresses.forEach { ip ->
                    ip.address.hostAddress?.let { hostIp ->
                        if (IPV4_REGEX.matches(hostIp))
                            return VPNDetails(hostIp)
                    }
                }
            }
        }
        return null
    }

    @Suppress("DEPRECATION")
    fun isInternetConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val activeNetworkInfo = cm.activeNetworkInfo
            return activeNetworkInfo?.isConnected ?: false
        } else {
            val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }

    fun isHostReachable(host: String, port: Int = 443, timeout: Int = 1000): Boolean {
        val socket = Socket()
        return try {
            socket.connect(InetSocketAddress(host, port), timeout)
            true
        } catch (e: Exception) {
           // Timber.e(e)
            false
        } finally {
            socket.close()
        }
    }
    data class VPNDetails(val ip: String)
}
