package com.appboosty.ads.nativead

import android.content.Context
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes

class PhNativeAdViewBinder private constructor(
    val context: Context,
    // val theme: Int,
    @LayoutRes val layoutResourceId: Int,
    @IdRes val adContainerViewId: Int,
    @IdRes val titleTextViewId: Int,
    @IdRes val advertiserTextViewId: Int,
    @IdRes val bodyTextViewId: Int,
    @IdRes val iconImageViewId: Int,
    @IdRes val iconContentViewId: Int,
    @IdRes val optionsContentViewGroupId: Int,
    @IdRes val optionsContentFrameLayoutId: Int,
    @IdRes val mediaContentViewGroupId: Int,
    @IdRes val callToActionButtonId: Int,
    @IdRes val ratingBarId: Int = 0,
    @IdRes var shimmerViewId: Int,
    val templateType: String
) {

    data class Builder(val context: Context) {
        @LayoutRes private var layoutResourceId: Int = 0
        @IdRes private var titleTextViewId: Int = 0
        @IdRes private var adContainerViewId: Int = 0
        @IdRes private var advertiserTextViewId: Int = 0
        @IdRes private var bodyTextViewId: Int = 0
        @IdRes private var iconImageViewId: Int = 0
        @IdRes private var iconContentViewId: Int = 0
        @IdRes private var optionsContentViewGroupId: Int = 0
        @IdRes private var optionsContentFrameLayoutId: Int = 0
        @IdRes private var mediaContentViewGroupId: Int = 0
        @IdRes private var callToActionButtonId: Int = 0
        @IdRes private var ratingBarId: Int = 0
        @IdRes private var shimmerViewId: Int = 0
        private var templateType: String = ""

        fun build() = PhNativeAdViewBinder(
            context, layoutResourceId, adContainerViewId, titleTextViewId, advertiserTextViewId,
            bodyTextViewId, iconImageViewId, iconContentViewId, optionsContentViewGroupId,
            optionsContentFrameLayoutId, mediaContentViewGroupId,
            callToActionButtonId, ratingBarId, shimmerViewId, templateType
        )

        fun setMainViewResourceId(@LayoutRes resourceID: Int): Builder {
            layoutResourceId = resourceID
            return this
        }

        fun setAdContainerViewId(@IdRes adContainerViewId: Int): Builder {
            this.adContainerViewId = adContainerViewId
            return this
        }

        fun setShimmerViewId(@IdRes shimmerViewId: Int): Builder {
            this.shimmerViewId = shimmerViewId
            return this
        }

        fun setTitleTextViewId(@IdRes titleTextViewId: Int): Builder {
            this.titleTextViewId = titleTextViewId
            return this
        }

        fun setAdvertiserTextViewId(@IdRes advertiserTextViewId: Int): Builder {
            this.advertiserTextViewId = advertiserTextViewId
            return this
        }

        fun setBodyTextViewId(@IdRes bodyTextViewId: Int): Builder {
            this.bodyTextViewId = bodyTextViewId
            return this
        }

        fun setIconImageViewId(@IdRes iconImageViewId: Int = 0): Builder {
            this.iconImageViewId = iconImageViewId
            return this
        }

        fun setIconContentViewId(@IdRes iconContentViewId: Int): Builder {
            this.iconContentViewId = iconContentViewId
            return this
        }

        fun setOptionsContentViewGroupId(@IdRes optionsContentViewGroupId: Int): Builder {
            this.optionsContentViewGroupId = optionsContentViewGroupId
            return this
        }

        fun setRatingBarViewId(@IdRes ratingBarId: Int): Builder {
            this.ratingBarId = ratingBarId
            return this
        }

        fun setMediaContentViewGroupId(@IdRes mediaContentViewGroupId: Int): Builder {
            this.mediaContentViewGroupId = mediaContentViewGroupId
            return this
        }


        fun setCallToActionButtonId(@IdRes callToActionButtonId: Int): Builder {
            this.callToActionButtonId = callToActionButtonId
            return this
        }

        fun setTemplateType(templateType: String): Builder {
            this.templateType = templateType
            return this
        }
    }
}