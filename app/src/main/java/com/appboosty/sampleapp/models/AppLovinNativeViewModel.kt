package com.appboosty.sampleapp.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.appboosty.ads.applovin.AppLovinNativeAdWrapper
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.util.PHResult
import com.appboosty.premiumhelper.util.error
import com.appboosty.premiumhelper.util.successValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class AppLovinNativeViewModel : ViewModel() {

    companion object{
        private val TAG = AppLovinNativeViewModel::class.java.simpleName
    }

    private var currentLoader: MaxNativeAdLoader? = null

    private val _maxAdLiveData = MutableLiveData<AppLovinNativeAdWrapper?>()
    val maxAdLiveData : LiveData<AppLovinNativeAdWrapper?> = _maxAdLiveData


    fun loadAd(){
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = PremiumHelper.getInstance().loadNativeAppLovinAd()
                if(result is PHResult.Success) {
                    currentLoader = result.value.adLoader
                    _maxAdLiveData.postValue(result.successValue)
                }else {
                    Timber.tag(TAG).e("Failed to load ad: ${result.error}")
                    _maxAdLiveData.postValue(null)
                }
            }catch (ex: Exception){
                Timber.tag(TAG).e("Failed to load ad: ${ex.message}")
                _maxAdLiveData.postValue(null)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentLoader?.destroy()
    }
}