package com.appboosty.ads.applovin

import android.os.Bundle
import androidx.core.os.bundleOf
import com.applovin.mediation.MaxAd
import com.google.android.gms.ads.AdValue

object AppLovinRevenueHelper {

    fun convertParameters(ad: MaxAd): Bundle {
        val revenue = ad.revenue
        val networkName = ad.networkName
        val adUnitId = ad.adUnitId

        return bundleOf(
            "valuemicros" to (revenue * 1000000).toLong(),
            "value" to revenue.toFloat(),
            "currency" to "USD",
            "precision" to convertToAdMobType(ad.revenuePrecision),
            "adunitid" to adUnitId,
            "mediation" to "applovin",
            "ad_format" to ad.format.label,
            "network" to (networkName ?: "unknown")
        )
    }

    private fun convertToAdMobType(precision: String): Int {
        return when (precision) {
            "exact" -> AdValue.PrecisionType.PRECISE
            "publisher_defined" -> AdValue.PrecisionType.PUBLISHER_PROVIDED
            "estimated" -> AdValue.PrecisionType.ESTIMATED
            else -> AdValue.PrecisionType.UNKNOWN
        }
    }
}