package com.appboosty.ads

import android.content.Context
import androidx.core.os.bundleOf
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.network.NetworkStateMonitor
import com.appboosty.premiumhelper.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

object AdsErrorReporter {

    private val mutex = Mutex()
    fun reportAdErrorAsync(context: Context, adType: String, adsError: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                val isNetworkAvailable = NetworkUtils.isInternetConnected(context)
                val unreachableDomains =
                    if (isNetworkAvailable) NetworkStateMonitor.getInstance()
                        .getUnavailableDomains().filter { it.isNotEmpty() }
                        .joinToString(separator = ",") else ""
                val privateDNSName = NetworkUtils.getPrivateDNS(context)
                val availableDomainRatio = if (isNetworkAvailable) NetworkStateMonitor.getInstance()
                    .getAvailableDomainsRatio() else 0
                val errorMessage = adsError ?: "unknown"
                val isVpnActive = NetworkUtils.isVPNActive(context)
                val isConsentFormShown = PremiumHelper.getInstance().adManager.consentManager.isConsentFormShown
               // val vpnIp = if (isVpnActive) NetworkUtils.getVPNDetails()?.ip ?: "" else ""
                val params = bundleOf(
                    "is_network_available" to isNetworkAvailable,
                    "unreachable_domains" to unreachableDomains,
                    "private_dns_name" to privateDNSName,
                    "ad_error" to errorMessage,
                    "is_vpn_active" to isVpnActive,
                   // "vpn_ip" to vpnIp,
                    "ad_type" to adType,
                    "available_domain_ratio" to availableDomainRatio,
                    "consent_form_shown" to isConsentFormShown,
                    "ads_provider" to PremiumHelper.getInstance().getCurrentAdsProvider().name
                )

                Timber.d(params.toString())
                PremiumHelper.getInstance().analytics.onAdLoadError(params)
            }
        }

    }
}