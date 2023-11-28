package com.appboosty.premiumhelper.network

import android.annotation.SuppressLint
import android.content.Context
import com.appboosty.premiumhelper.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.lang.IllegalArgumentException
import kotlin.coroutines.resume

class NetworkStateMonitor private constructor(private val context: Context) {

    companion object {

        @SuppressLint("StaticFieldLeak")
        private var instance: NetworkStateMonitor? = null

        @Synchronized
        fun getInstance(context: Context? = null): NetworkStateMonitor {
            if (instance == null && context == null)
                throw IllegalArgumentException("On first call the context can't be null")
            return instance ?: run {
                instance = NetworkStateMonitor(context!!)
                instance!!
            }
        }

        private const val NETWORK_STATES_TTL = 60000 * 30L //Half a hour

        private val adSenseServiceDomains = listOf(
            "adsense.google.com",
            "adservice.google.ca",
            "adservice.google.co.in",
            "adservice.google.co.kr",
            "adservice.google.co.uk",
            "adservice.google.co.za",
            "adservice.google.com",
            "adservice.google.com.ar",
            "adservice.google.com.au",
            "adservice.google.com.br",
            "adservice.google.com.co",
            "adservice.google.com.gt",
            "adservice.google.com.mx",
            "adservice.google.com.pe",
            "adservice.google.com.ph",
            "adservice.google.com.pk",
            "adservice.google.com.tr",
            "adservice.google.com.tw",
            "adservice.google.com.vn",
            "adservice.google.de",
            "adservice.google.dk",
            "adservice.google.es",
            "adservice.google.fr",
            "adservice.google.nl",
            "adservice.google.no",
            "adservice.google.pl",
            "adservice.google.ru",
            "adservice.google.vg",
            "app-measurement.com",
            //"dai.google.com",
            "doubleclick.com",
            "doubleclick.net",
            "doubleclickbygoogle.com",
            "googleadservices.com",
            //"googlesyndication-cn.com",
            //"googlesyndication.com",
            //"googletagservices.com",
            //"gstaticadssl.l.google.com",
            //"s0-2mdn-net.l.google.com"
        )
        private val appLovinAdServiceDomains = listOf(
            "ms.applvn.com",
            "applovin.com"
        )
    }

    private var recentNetworkState = PhNetworkState(0, hashMapOf())

    @Synchronized
    private fun updateNetworkState() {
        val domainStatuses = hashMapOf<String, Boolean>()
        adSenseServiceDomains.forEach { domain ->
            domainStatuses[domain] = NetworkUtils.isHostReachable(domain)
        }
        appLovinAdServiceDomains.forEach { domain ->
            domainStatuses[domain] = NetworkUtils.isHostReachable(domain)
        }
        recentNetworkState = PhNetworkState(
            System.currentTimeMillis(),
            domainStatuses,
            NetworkUtils.isVPNActive(context),
            NetworkUtils.getPrivateDNS(context)
        )
        Timber.d("Status update of ad domains finished")
    }

    suspend fun getUnavailableDomains(): List<String> {
        return suspendCancellableCoroutine { cont ->
            if (System.currentTimeMillis() - recentNetworkState.timestamp < NETWORK_STATES_TTL &&
                recentNetworkState.matchNetworkConfiguration(context)) {
                if (cont.isActive) {
                    cont.resume(recentNetworkState.hostsStatus.filter { !it.value }.map { it.key })
                }
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    updateNetworkState()
                    if (cont.isActive) {
                        cont.resume(recentNetworkState.hostsStatus.filter { !it.value }
                            .map { it.key })
                    }
                }
            }
        }
    }

    suspend fun getAvailableDomainsRatio(): Int {
        if (recentNetworkState.hostsStatus.isEmpty()) return 100
        val unavailableDomains = getUnavailableDomains()
        return 100 - (unavailableDomains.size.toFloat() / recentNetworkState.hostsStatus.size * 100).toInt()
    }


    data class PhNetworkState(
        val timestamp: Long,
        val hostsStatus: HashMap<String, Boolean>,
        val vpnActive: Boolean = false,
        val privateDNS: String = ""
    ){
        fun matchNetworkConfiguration(context: Context): Boolean{
            return vpnActive == NetworkUtils.isVPNActive(context) && privateDNS == NetworkUtils.getPrivateDNS(context)
        }
    }
}