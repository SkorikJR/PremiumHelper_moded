package com.appboosty.sampleapp;

import android.Manifest;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.ads.nativead.NativeAd;
import com.appboosty.ads.config.PHAdSize;
import com.appboosty.ads.nativead.PHNativeAdView;
import com.appboosty.permissions.PermissionRequester;
import com.appboosty.premiumhelper.Premium;
import com.appboosty.premiumhelper.PremiumHelper;
import com.appboosty.premiumhelper.util.PHResult;
import com.appboosty.sample.R;

import org.jetbrains.annotations.NotNull;

import io.reactivex.disposables.CompositeDisposable;


public class SampleJavaActivity extends AppCompatActivity {

    private final CompositeDisposable disposables = new CompositeDisposable();

    private PHNativeAdView nativeAdView;
    private ViewGroup adContainer2;

    private final PermissionRequester permissionRequester = new PermissionRequester(this, Manifest.permission.CAMERA)
            .onGranted(requester -> {
                Toast.makeText(SampleJavaActivity.this, "Permission ${it.permission} granted", Toast.LENGTH_SHORT).show();
            })
            .onDenied(requester -> {
                Toast.makeText(SampleJavaActivity.this, "Permission ${it.permission} denied", Toast.LENGTH_SHORT).show();
            })
            .onRationale(requester -> {
                requester.showRationale(
                        "Permission needed",
                        "This application needs permissions to work correctly", "Ok"
                );
            })
            .onPermanentlyDenied((requester, canShowSettingsDialog) -> {
                if (canShowSettingsDialog) {
                    requester.showOpenSettingsDialog(
                            "Permission needed",
                            "This application needs permissions to work correctly",
                            "Go to settings",
                            "Later"
                    );
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_java);

        nativeAdView = findViewById(R.id.native_ad_1);
        adContainer2 = findViewById(R.id.ad_container_2);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Sample Java Activity");

        loadBanner();
        loadNativeAd();

        PremiumHelper.getInstance().observePurchaseResultRx().subscribe( result -> {

        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        permissionRequester.request();
    }

    private void loadBanner() {
        disposables.add(Premium.Ads.Rx.loadBanner(PHAdSize.BANNER)
                .subscribe(
                        result -> {
                            if (result instanceof PHResult.Success) {
                                adContainer2.addView(((PHResult.Success<View>) result).getValue());
                            } else {
                                Toast.makeText(SampleJavaActivity.this, "Failed to load banner", Toast.LENGTH_SHORT).show();
                            }
                        },
                        error -> Toast.makeText(SampleJavaActivity.this, "Failed to load banner", Toast.LENGTH_SHORT).show()
                ));
    }

    private void loadNativeAd() {
        disposables.add(Premium.Ads.Rx.loadAndGetNativeAd()
                .subscribe(
                        result -> {
                            if (result instanceof PHResult.Success) {
                                nativeAdView.bindView(((PHResult.Success<NativeAd>) result).getValue());
                            } else {
                                Toast.makeText(SampleJavaActivity.this, "Failed to load native ad", Toast.LENGTH_SHORT).show();
                            }
                        },
                        error -> Toast.makeText(SampleJavaActivity.this, "Failed to load native ad", Toast.LENGTH_SHORT).show()
                ));
    }

    @Override
    protected void onDestroy() {
        disposables.dispose();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull @NotNull MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}