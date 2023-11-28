package com.appboosty.ads

import com.google.android.gms.ads.nativead.NativeAd

data class PhNativeAd(val nativeAd: Any?){

    val headline : String?
        get(){
            return when(nativeAd){
                is NativeAd -> nativeAd.headline
                else -> null
            }
        }

    fun destroy(){
        when(nativeAd){
           is NativeAd -> nativeAd.destroy()
        }
    }
}