package com.appboosty.sampleapp

import android.content.Context
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.nativead.NativeAd
import com.appboosty.ads.nativead.PHNativeAdView
import com.appboosty.premiumhelper.PremiumHelper
import com.appboosty.premiumhelper.util.PHResult
import com.appboosty.sample.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NativeAdsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_native_ads)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Native ads preview"

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SampleAdapter(this)

//        lifecycleScope.launchWhenCreated {
//            PremiumHelper.getInstance().adManager.loadNativeAd(10)
//        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == android.R.id.home) {
            finish()
        }

        return super.onOptionsItemSelected(item)
    }
}


class SampleAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val ITEM = 0
    private val AD = 1

    private val nativeAds = SparseArray<NativeAd>()

    private val inflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when (viewType) {
            ITEM -> {
                val view = inflater.inflate(R.layout.sample_list_item, parent, false)
                SampleViewHolder(view)
            }
            AD -> {
                val view = PHNativeAdView(context)
                AdViewHolder(view)
            }
            else -> error("")
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        if (holder is SampleViewHolder) {
            holder.title.text = "List Item #$position"
        } else if (holder is AdViewHolder) {
            if (nativeAds.get(position) != null) {
                (holder.itemView as PHNativeAdView).bindView(nativeAds.get(position))
            } else {
                (context as LifecycleOwner).lifecycleScope.launch {
                    val result = PremiumHelper.getInstance().loadAndGetNativeAdmobAd()

                    if (result is PHResult.Success) {
                        val ad = result.value
                        nativeAds.put(position, ad)
                        withContext(Dispatchers.Main) {
                            (holder.itemView as PHNativeAdView).bindView(ad)
                        }
                    }
                }

            }
        }

    }

//    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
//
//        if (holder is AdViewHolder) {
//            val ad = (holder.itemView as PHNativeAdView).nativeAd
//
//            if (ad != null) {
//                nativeAds.put(holder.adapterPosition, ad)
//            }
//        }
//
//        super.onViewRecycled(holder)
//    }

    override fun getItemViewType(position: Int): Int {
        return if (position > 0 && position % 3 == 0) AD else ITEM
    }

    override fun getItemCount(): Int {
        return 100
    }

    class SampleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.text1)
    }

    class AdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    }

}