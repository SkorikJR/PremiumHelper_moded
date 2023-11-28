package com.appboosty.sampleapp.models

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appboosty.ads.PhLoadAdError
import com.appboosty.ads.nativead.PhNativeAdLoadListener
import com.appboosty.ads.nativead.PhNativeAdViewBinder
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.util.PHResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class CommonNativeAdViewModel : ViewModel()  {

    companion object{
        private val TAG = CommonNativeAdViewModel::class.java.simpleName
    }

    private val _nativeAdView = MutableLiveData<View?>()
    val nativeAdView : LiveData<View?> = _nativeAdView

    fun loadAd(binder: PhNativeAdViewBinder){
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = PremiumHelper.getInstance().loadNativeAdsCommon(binder, object: PhNativeAdLoadListener{
                    override fun onAdFailedToLoad(error: PhLoadAdError) {
                        Timber.e("onAdFailedToLoad()-> Error: $error")
                    }

                    override fun onAdLoaded(adView: View) {
                        Timber.d("onAdLoaded()-> called")
                    }
                })
                if(result is PHResult.Success) {
                    _nativeAdView.postValue(result.value)
                }else {
                    _nativeAdView.postValue(null)
                }
            }catch (ex: Exception){
                Timber.e("Failed to load ad: ${ex.message}")
                _nativeAdView.postValue(null)
            }
        }
    }
}