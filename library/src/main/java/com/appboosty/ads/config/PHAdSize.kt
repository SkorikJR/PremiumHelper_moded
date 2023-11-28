package com.appboosty.ads.config

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdSize
import com.appboosty.premiumhelper.util.PremiumHelperUtils
import kotlin.math.roundToInt

class PHAdSize internal constructor(val sizeType: SizeType, val width: Int = 0, val height: Int = 0) {

    enum class SizeType {
        BANNER, FULL_BANNER, LARGE_BANNER, LEADERBOARD, MEDIUM_RECTANGLE, WIDE_SKYSCRAPER, FLUID, ADAPTIVE, ADAPTIVE_ANCHORED
    }

    companion object {

        @JvmField val BANNER = PHAdSize(SizeType.BANNER)
        @JvmField val FULL_BANNER = PHAdSize(SizeType.FULL_BANNER)
        @JvmField val LARGE_BANNER = PHAdSize(SizeType.LARGE_BANNER)
        @JvmField val LEADERBOARD = PHAdSize(SizeType.LEADERBOARD)
        @JvmField val MEDIUM_RECTANGLE = PHAdSize(SizeType.MEDIUM_RECTANGLE)
        @JvmField val WIDE_SKYSCRAPER = PHAdSize(SizeType.WIDE_SKYSCRAPER)
        @JvmField val FLUID = PHAdSize(SizeType.FLUID)

        @JvmStatic
        fun adaptiveBanner(width: Int, maxHeight: Int = 0) = PHAdSize(SizeType.ADAPTIVE, width, maxHeight)

        @JvmStatic
        fun adaptiveBanner(activity: Activity, width: Int = PremiumHelperUtils.getScreenWidthDp(activity), maxHeight: Int = 0) = PHAdSize(SizeType.ADAPTIVE, width, maxHeight)

        @JvmStatic
        @JvmOverloads
        fun adaptiveBanner(activity: Activity, maxHeight: Int = 0) = PHAdSize(SizeType.ADAPTIVE, PremiumHelperUtils.getScreenWidthDp(activity), maxHeight)

        @JvmStatic
        fun adaptiveAnchoredBanner(width: Int) = PHAdSize(SizeType.ADAPTIVE_ANCHORED, width)

        @JvmStatic
        fun adaptiveAnchoredBanner(activity: Activity) = PHAdSize(SizeType.ADAPTIVE_ANCHORED, PremiumHelperUtils.getScreenWidthDp(activity))
    }

    fun asAdSize(context: Context): AdSize {
        return when(sizeType) {
            SizeType.BANNER -> AdSize.BANNER
            SizeType.FULL_BANNER -> AdSize.FULL_BANNER
            SizeType.LARGE_BANNER -> AdSize.LARGE_BANNER
            SizeType.LEADERBOARD -> AdSize.LEADERBOARD
            SizeType.MEDIUM_RECTANGLE -> AdSize.MEDIUM_RECTANGLE
            SizeType.WIDE_SKYSCRAPER -> AdSize.WIDE_SKYSCRAPER
            SizeType.FLUID -> AdSize.FLUID
            SizeType.ADAPTIVE -> {
                if (height > 0) {
                    AdSize.getInlineAdaptiveBannerAdSize(width, height)
                } else  {
                    AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(context, width)
                }
            }
            SizeType.ADAPTIVE_ANCHORED -> {
                getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width)
            }
        }
    }

    private fun getCurrentOrientationAnchoredAdaptiveBannerAdSize(context: Context, width: Int): AdSize {
        val height = when {
            width > 655 -> width / 728.0F * 90.0F
            width > 632 -> 81F
            width > 526 -> width / 468.0F * 60.0F
            width > 432 -> 68F
            else -> width / 6.5F
        }.roundToInt()

        val maxHeight = PremiumHelperUtils.getScreenHeightDp(context, 0)
        val minHeight = 50

        return AdSize(width, height.coerceIn(minHeight, maxHeight))
    }

}