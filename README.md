# premium-helper


#### 1. Add Premium Helper library to the project
1. Edit root build.gradle:
```dsl
allprojects {
    repositories {
     ..
        maven {
            name = "GithubPackages"
            url = uri("https://maven.pkg.github.com/AppBoosty/premium-helper")
                credentials {
                    username = 'app-boosty'
                    password = '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'
                }
        }
        //Required by AppLovin mintegral mediator library
        maven { url 'https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea' }
        maven { url = uri("https://artifact.bytedance.com/repository/pangle") }
    }
}
```
2. Edit local build.gradle:
```dsl
// will not cache SNAPSHOT builds
configurations.all {
    resolutionStrategy.cacheChangingModulesFor 0, 'seconds'
}

dependencies {
    ..
    // $premiumHelperVersion
    // 2.x.x for master branch build
    // or 2.x.x-SNAPSHOT for develop branch build
    implementation "com.appboosty.premiumhelper:premiumhelper:$premiumHelperVersion"
}
```
<br/>
Here is the list of dependencies added in premium-helper which you need to remove from the app dependencies to avoid conflicts:

```dsl
dependencies {
    api 'com.google.android.play:core-ktx:1.8.1'

    api 'androidx.core:core-ktx:1.6.0'
    api 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2'

    api 'androidx.lifecycle:lifecycle-runtime-ktx:2.3.1'
    api 'androidx.lifecycle:lifecycle-process:2.3.1'
    api 'android.arch.lifecycle:runtime:1.1.1'
    api 'android.arch.lifecycle:extensions:1.1.1'
    api 'androidx.lifecycle:lifecycle-common-java8:2.3.1'

    api 'com.android.billingclient:billing:4.0.0'
    api 'com.android.billingclient:billing-ktx:4.0.0'

    api 'com.google.firebase:firebase-config-ktx:21.0.0'
    api 'com.google.firebase:firebase-core:19.0.0'
    api 'com.google.firebase:firebase-messaging-ktx:22.0.0'
    api 'com.google.firebase:firebase-crashlytics:18.2.0'

    api 'com.jakewharton.threetenabp:threetenabp:1.3.0'
    
    api 'androidx.work:work-runtime-ktx:2.5.0'

    api 'com.squareup.retrofit2:retrofit:2.6.1'
    api 'com.squareup.retrofit2:converter-gson:2.6.1'
   
    api 'com.squareup.okhttp3:okhttp:3.12.12'
    api 'com.squareup.okhttp3:logging-interceptor:3.12.12'
    
    api 'com.android.installreferrer:installreferrer:2.2'

    api 'io.reactivex.rxjava2:rxjava:2.2.10'
    api 'io.reactivex.rxjava2:rxandroid:2.1.1'
    api 'org.jetbrains.kotlinx:kotlinx-coroutines-rx2:1.4.2'

    api 'com.jakewharton.timber:timber:4.7.1'

    // Ads
    api 'com.google.android.gms:play-services-ads:20.2.0'
    api 'com.google.ads.mediation:facebook:6.5.1.0'
    api 'com.google.ads.mediation:applovin:10.3.1.0'
    api 'com.google.ads.mediation:adcolony:4.5.0.0'
    api 'androidx.annotation:annotation:1.2.0'
    api 'com.google.ads.mediation:fyber:7.8.3.0'
    api 'com.google.ads.mediation:ironsource:7.1.7.0'

    // Logs
    api 'org.slf4j:slf4j-api:1.7.30'
    api 'com.github.tony19:logback-android:2.0.0'
}
```

#### 2.  Configuration and initialization

Configure premium helper in the Application `onCreate()` method and call `initialize()` to start initialization.
Premium Helper library is initialized only on main application process. Please check that the main process name is not overridden in the AndroidManifest.xml `<application>` with `android:process`.

```kotlin
    override fun onCreate() {
        super.onCreate()

        Premium.initialize(this@SampleApp,
            PremiumHelperConfiguration.Builder(BuildConfig.DEBUG)
                .mainActivityClass(MainActivity::class.java) // Mandatory: specify MainActivity class so that it could be started from Splash/StartLikePro activities
                .configureMainOffer(getString(R.string.default_main_sku_name)) // Main offer default SKU name
                .configureOneTimeOffer("test_premium_v1_030", ""test_premium_v1_030_yearly"") // One-time offer SKUs
                .startLikeProActivityLayout(R.layout.activity_start_like_pro_x_to_close)
                .relaunchPremiumActivityLayout(R.layout.activity_relaunch_premium)
                .relaunchOneTimeActivityLayout(R.layout.activity_relaunch_one_time)
                .rateDialogMode(RateHelper.RateMode.NONE)
                .termsAndConditionsUrl("https://appboosty.com/Sample/Terms")     // Mandatory
                .privacyPolicyUrl("https://appboosty.com/Sample/Privacy")        // Mandatory
                // The advertisement config can be set in two ways. This will not affect the app behaviour
                 //1) Passing both configurations together
                .adManagerConfiguration(admobConfig, applovinConfig)
                 //2) Passing each config to different function
                .admobConfiguration(admobConfig)
                .applovinConfiguration(applovinConfig)
                .setInterstitialMuted(false)
                // Optional: configure push message handler
                .pushMessageListener(PushMessageHandler())
                .setFlurryApiKey("<Flurry API key>")
                .build())
        }
    }
```
#### Mandatory configuration
Configuration option|Description
-------------|------------------
startLikeProActivityLayout|Set layout id for Start Like Pro (Onboarding) activity
relaunchPremiumActivityLayout|Set layout id for Relaunch activity
relaunchOneTimeActivityLayout|Set layout id for Relaunch One Time activity
mainActivityClass|Set class name of main application activity that will be started after the splash screen
configureMainOffer|Configure default value for main offer SKU
adManagerConfiguration|Set configuration for advertisements
termsAndConditionsUrl|Set 'Terms & Conditions' URL
privacyPolicyUrl|Set 'Privacy Policy' URL

#### Optional configuration
Configuration option|Description
-------------|------------------
configureOneTimeOffer|Configure default value for one-time relaunch offer SKU
startLikeProTextNoTrial|The startLikeProTextTrial and startLikeProTextNoTrial ids are used to override default strings for purchase button title.<br />If offer sku has a trial period the startLikeProTextTrial text is displayed on the purchase button.<br />If offer sku has a no trial period the startLikeProTextNoTrial text is displayed on the purchase button.
startLikeProTextTrial|See `startLikeProTextNoTrial` description
pushMessageListener|Listener for receiving push messages.
analyticsEventPrefix|Prefix for analytics events.
rateDialogMode|Set default mode for showing rate dialog.
showOnboardingInterstitial|Set default value for showing interstitial after onboarding (true by default)
showExitConfirmationAds|Set default value for showing exit confirmation dialog with native ad (false by default)
showTrialOnCta|Show trial period in CTA buttons title on purchase screens (false by default)
setFlurryApiKey|Flurry API key in case flurry is enabled
consent_request_enabled|Show consent request if required by AdMob (true by default)
#### Test configuration
Configuration option|Description
-------------|------------------
useTestAds|Use test ad unit ids (AdMob only. For AppLovin use testAdvertisingIds API in adManagerConfig)
useTestLayouts|Use test layouts when Start Like Pro or Relaunch layouts are missing (true by default)

Initialization is asynchronous. The splash screen is used to wait for initialization completion.

#### 3. Splash activity

Here are the options you have for setting up splash screen in the app:
##### 1. Use default activity from premium-helper
Premium-helper has default splash screen activity defined as `PHSplashActivity`. This activity has all the logic for app initialization as well as basic splash screen layout.
To use it just add this section to the app `AndroidManifest.xml` file:
```xml
        <activity android:name="com.appboosty.premiumhelper.ui.splash.PHSplashActivity"
                  android:theme="@style/PhSplashTheme">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
```
Do not forget to remove `MAIN\LAUNCHER` filter from the previous launcher activity.

You can override default colors for splash screen background and title by defining these attributes in your custom splash theme:
```xml
    <style name="SplashTheme" parent="@style/PhSplashTheme">
        ...
        <item name="ph_splash_title_color">@color/colorSplashTitle</item>
        <item name="ph_splash_background_color">@color/colorSplashBackground</item>
    </style>
```

##### 2. Use default activity from premium-helper with your custom layout
You can keep the default activity but use your own layout. To do this define your splash screen layout and call it `ph_activity_splash.xml`. Do not forget to add the splash activity to the manifest (see option 1 above).

##### 3. Use custom splash screen activity
You can define your own splash activity extending the `PHSplashActivity` and add it to the manifest with the `MAIN\LAUNCHER` intent filter.

```kotlin
class SplashActivity : PHSplashActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)
    }

    // Override only when you need custom conditions for showing StartLikePro activity
    override fun shouldShowStartLikePro(): Boolean {
        return !Premium.hasActivePurchase()
    }

    // Override when you need to handle premium helper init completion event and/or override the default navigation logic
    protected open fun onPremiumHelperInitialized(result: PHResult<Boolean>) {
      ...
    }

}
```

If your application needs to check if the MainActivity was started from SplashScreen please check the `PHSplashActivity.FLAG_FROM_SPLASH` flag in the intent.

```kotlin
    val startedFromSplash = getIntent().getBooleanExtra(PHSplashActivity.FLAG_FROM_SPLASH, false)
```

Please pay attention to text and background colors when overriding default splash layout and theme. Premium-helper provides normal and `-night` values for `ph_` colors.
So if you change the default text/background color define `-night` values or update background/text color as well to avoid light text on light background.


#### 4. StartLikePro activity

StartLikePro activity is displayed on first app start.
* If you want to show "X" button on top left on StartLikePro screen, copy sample layout:
* - [activity_start_like_pro_x_to_close.xml](https://github.com/AppBoosty/premium-helper/blob/develop/app/src/main/res/layout/activity_start_like_pro_x_to_close.xml)
    *If you want to use "Or try limited" button, copy sample layout below:
* - [activity_start_like_pro_or_try_limited.xml](https://github.com/AppBoosty/premium-helper/blob/develop/app/src/main/res/layout/activity_start_like_pro_or_try_limited.xml)


* Configure layout in premium-helper initialize:

``` kotlin
        Premium.initialize(this@SampleApp,
            PremiumHelperConfiguration.Builder(BuildConfig.DEBUG)
                ...
                .startLikeProActivityLayout(R.layout.activity_start_like_pro_x_to_close)
                .startLikeProTextTrial(R.string.trial) // Optional
                .startLikeProTextNoTrial(R.string.recover_now) // Optional
                ...
                .build()
```
The `startLikeProTextTrial` and `startLikeProTextNoTrial` ids are used to override default strings for purchase button title.
If offer sku has a trial period the `startLikeProTextTrial` text is displayed on the purchase button.
If offer sku has a no trial period the `startLikeProTextNoTrial` text is displayed on the purchase button.
Default values are defined in premium-helper
<br/>
You can do A/B testing of onboarding layouts. Add all layouts as parameters:
```kotlin
        Premium.initialize(this@SampleApp,
            PremiumHelperConfiguration.Builder(BuildConfig.DEBUG)
                ...
                .startLikeProActivityLayout(R.layout.onboarding_default, R.layout.onboarding_test_a, R.layout.onboarding_test_b)
                ...
                .build()
```
You can select the layout variant by setting the `onboarding_layout_variant` remote config / toto configuration parameter (0 - first layout, etc).
Onboarding layout may contain a close button with the `@id\start_like_pro_close_button` view id.

#### 5. Intro Activity

Intro activity is a special activity which will be opened by the premium-helper before opening main activity.
Intro will be shown until you set the intro complete flag by calling `Premium.setIntroComplete()`.
It is your responsibility to open the main activity from Intro activity once user completes the intro.
You can configure an Intro in premium-helper initializer:
```kotlin
        Premium.initialize(this@SampleApp,
            PremiumHelperConfiguration.Builder(BuildConfig.DEBUG)
                ...
                .introActivityClass(IntroActivity::class.java)
                ...
                .build())
```

#### 6. MainActivity

Configure main activity class name in premium-helper initializer:
```kotlin
        Premium.initialize(this@SampleApp,
            PremiumHelperConfiguration.Builder(BuildConfig.DEBUG)
                ...
                .mainActivityClass(MainActivity::class.java)
                ...
                .build())
```

If the application is using custom themes that you want to use for relaunch activity you need to implement `AppThemeProvider` interface in the Main Activity and return current theme.

``` kotlin
        class MainActivity : AppCompatActivity(), AppThemeProvider {
        
            override fun getCustomTheme(): Int {
                return currentAppTheme
            }
        
        
        }

```

#### 7. Relaunch and Relaunch One Time activity

Important: Please make sure that you are using AppCompatActivity as the base class for your activities. Otherwise relaunch logic will not function.

Relaunch activity is showing a premium offering on app start or when user initiates upgrade from the app (e.g. clicks Upgrade button).

There are two ways to set the 'application start' event:
- The application `cold start` when the Application object is created
- The application resume from the background.

The relaunch is shown either on `cold start` or on application resume based on the `show_relaunch_on_resume` parameter value.

Relaunch starts showing from the second app start and is shown:
- on day 1: 3 times
- on day 2: 2 times
- from day 3: every 3rd day
  If one-time relaunch is available it is showing on every app launch starting from `ONETIME_START_SESSION` parameter value.

Relaunch page on app start is shown by premium helper when needed. If the one-time offer is configured the one-time relaunch will be shown (with the one-time offer expiration countdown)
To show premium offering in other scenarios (e.g. user taps Upgrade button) call `premiumHelper.showPremiumOffering()` method. You can pass the `source` parameter for analytics
and custom `theme` (when custom Dark theme is used).
If you need to show premium offering from service (TileService or other that supports starting activities) use `premiumHelper.showPremiumOfferingNewTask()` method.

* Copy sample layouts: 
  https://github.com/AppBoosty/premium-helper/blob/develop/app/src/main/res/layout/activity_relaunch_premium.xml , https://github.com/AppBoosty/premium-helper/blob/develop/app/src/main/res/layout/activity_relaunch_premium.xml
  https://github.com/AppBoosty/premium-helper/blob/develop/app/src/main/res/layout/activity_relaunch_premium.xml , https://github.com/AppBoosty/premium-helper/blob/develop/app/src/main/res/layout/activity_relaunch_premium_one_time.xml

* Configure layout in premium-helper initialize:

``` kotlin
        Premium.initialize(this@SampleApp,
            PremiumHelperConfiguration.Builder(BuildConfig.DEBUG)
                ...
                .relaunchPremiumActivityLayout(R.layout.activity_relaunch_premium)
                .relaunchOneTimeActivityLayout(R.layout.activity_relaunch_one_time)
                ...
                .build()
```

If you need to get the callback when all the relaunch actions are complete you should implement `OnRelaunchListener` in the MainActivity. Premium-helper will invoke `onRelaunchComplete()` callback once relaunch is done.
If `show_relaunch_on_resume` feature is enabled the relaunch could start from any activity (when app is resumed from background). In such case premium-helper will try to deliver
callback to current activity (if it implements `OnRelaunchListener`) and then will deliver it to the MainActivity when it is resumed.
On relaunch callback will be called immediately on MainActivity resume if the app is PURCHASED.

Note: Sometimes you want to avoid showing the relaunch and interstitial ads after app restart (e.g. after app accessibility access is granted the app auto-restarts). For such scenarios please use `ignoreNextAppStart()`
method to disable all premium-helper logic on next app start.

Note: If you want to make sure that relaunch screen (premium offer, interstitial or rate) is never shown on certain app screen you should implement `NoRelaunchActivity` interface in the activity.
<br/><br/>
You can do A/B testing of relaunch layouts. Add all layouts as parameters:
```kotlin
        Premium.initialize(this@SampleApp,
            PremiumHelperConfiguration.Builder(BuildConfig.DEBUG)
                ...
                .relaunchPremiumActivityLayout(R.layout.relaunch_default, R.layout.relaunch_test_a, R.layout.relaunch_test_b)
                .relaunchOneTimeActivityLayout(R.layout.relaunch_onetime_default, R.layout.relaunch_onetime_test_a, R.layout.relaunch_onetime_test_b)
                ...
                .build()
```
You can select the layout variant by setting the `relaunch_layout_variant`/`relaunch_onetime_layout_variant` remote config / toto configuration parameter (0 - first layout, etc).

##### Relaunch and Start Like A Pro UI styling
The style of the Relaunch and Start Like A Pro screens is defined in the base `PhPremiumOfferingTheme`.
Attribute|Description
---------|-----------
`premium_offer_header_logo_color`|Tint color for premium logo image
`premium_offer_primary_text_color`|Main text color (titles)
`premium_offer_secondary_text_color`|Secondary text color (subtitles)
`premium_offer_feature_icon_color`|Tint color for feature icons and check marks
`premium_offer_background_color`|Background color
`premium_offer_close_button_color`|Close button color
`premium_offer_countdown_text_color`|One-time offer countdown timer color
`premium_offer_cta_button_color`|CTA button color
`premium_offer_cta_button_text_color`|CTA button text color
`premium_offer_cta_button_shape`|CTA button shape
`premium_offer_text_alpha`|	Alpha of 'X' button and the text below the 'Purchase' button

To adjust the style in your application:
1. Define custom style with the `PhPremiumOfferingTheme` parent theme and override the attributes as needed:
```xml
    <style name="CustomPremiumOfferingTheme" parent="PhPremiumOfferingTheme">
        <item name="premium_offer_cta_button_color">@null</item>
        <item name="premium_offer_cta_button_shape">@drawable/cta_button_shape</item>
        <item name="premium_offer_cta_button_text_color">@android:color/black</item>
        <item name="premium_offer_feature_icon_color">@android:color/holo_red_dark</item>
        <item name="premium_offer_countdown_text_color">@android:color/holo_red_dark</item>
        <item name="premium_offer_text_alpha">0.6</item>
    </style>
```
2. Set your custom theme in main app theme:
```xml
    <style name="AppTheme" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        ...
        <item name="premium_offering_style">@style/CustomPremiumOfferingTheme</item>
    </style>
```
3. Note for Android 16 CTA button stying.</br>
If the application has `minSdk 16` you will need to do the following to customize the CTA button style:
  - Copy `drawable-v16/ph_cta_button_bg.xml` to `drawable-v16/` in the app.
  - Define drawable with the needed shape and color (see `ph_cta_button_shape.xml` as sample)
  - Use the new shape drawable in the `drawable-v16/ph_cta_button_bg.xml` file.

#### 8. RateDialog

Rate dialog is shown by Premium Helper after relaunch and interstitial on every 3rd app launch starting from `RATE_US_SESSION_START`parameter value.
The `RATE_US_MODE` parameter controls the type of the rate request presented to the user: validate intent dialog or in-app review.

To manually show rate dialog call `premiumHelper.showRateDialog()` to app rate intent dialog or `premiumHelper.showInAppReview()` for in-app review dialog. Rate intent dialog will use default layout defined in Premium Helper library. You can supply custom rate intent dialog layout which must define the following ids:

```
@id/rate_dialog_dismiss_button
@id/rate_dialog_positive_button
@id/rate_dialog_negative_button
```

Please see sample layout: https://github.com/AppBoosty/premium-helper/blob/develop/app/src/main/res/layout/custom_rate_dialog.xml

Configure layout in premium-helper initialize:
``` kotlin
        Premium.initialize(this@SampleApp,
            PremiumHelperConfiguration.Builder(BuildConfig.DEBUG)
                ...
                .rateDialogLayout(R.layout.rate_dialog)
                .rateDialogMode(RateHelper.RateMode.NONE)
                .rateSessionStart(2)
                ...
                .build()
```

You can set up the rate mode and when the rate dialog starts showing  (see section 9: `rate_us_mode` and `rateus_session_start`). Set `rateus_session_start` to 0 for showing rate UI on first app start.

#### 9. Happy Moment

Happy moment is a moment when user gets a positive experience from using the app (shares a edited image, recovers deleted file etc). We can use this moment and convert
it to a good app rating. To do this call `Premium.onHappyMoment()` method. Premium helper will try to show rate UI and interstitial ad based on the `happy_moment` configuration parameter.</br>
By default it will try to show rate UI configured with the `rate_mode` parameter and interstitial ad. The other options are: rate dialog, in-app review, rate dialog followed by interstitial ad, in-app review followed by interstitial ad.</br>
Use `delay` parameter to postpone the happy moment UI.</br>

You can apply rate UI capping to happy moment with the `.setHappyMomentCapping()` method in premium-helper configuration.
By default capping is reset on every app start. To keep it between app start use `GLOBAL` type when configuring happy moment capping.<br />
If you want to skip rate UI for first calls of the happy moment feature you can configure it with the `.setHappyMomentSkipFirst(skip)` method in premium-helper configuration.
Happy moment skip and capping rules are applied only to rate UI, happy moment interstitial ads are capped with interstitial capping.

Use `showHappyMomentOnNextActivity()` method when you want to show happy moment UI on next activity.

#### 10. Purchase

To check if app is purchased call `premiumHelper.preferences.hasActivePurchase()`.
If you need to start purchase flow call `premiumHelper.launchPurchaseFlow()`. Note that you need to fetch the `Offer` first by calling `premiumHelper.getOffer()`
For test purposes you can define offers yourself with the `addDebugMainOffer()` and `addDebugOneTimeOffer()` in DEBUG build.

``` kotlin

            premiumHelper.addDebugMainOffer("test_premium_v1_trial_7d_yearly", "$123" )
            premiumHelper.addDebugOneTimeOffer("test_premium_v1_trial_7d_yearly", "$123", "test_premium_v1_trial_7d", "$456")

```
These test offers can be purchased on StartLikePro/Relaunch pages. Test purchase is done inside Premium Helper library, Google Play is not involved. Test purchase will persist until app reinstall.

If the app requires other apps install for unlocking premium features you can list package names of these apps:
```kotlin
     setPremiumPackages("ringtone.maker.premium", "ringtone.maker.pro")
```   
and add permissions to query the package to the `AndroidManifest.xml` file:
```xml
    <queries>
        <package android:name="ringtone.maker.premium" />
        <package android:name="ringtone.maker.pro" />
    </queries>
```

#### 11. RemoteConfig and Preferences

You should use RemoteConfig and Preferences from the premium helper: `premiumHelper.configuration`, `premiumHelper.preferences`
You can override the remote config values for test purposes by calling `configuration.overrideDebugValue(key, value)`

Here are the options you can configure with Remote Config:

Remote key|Default Value|Description
----------|-------------|-----------
`main_sku`|-|Default name for main app offer SKU
`onetime_offer_sku`|-|Default name for one-time relaunch offer SKU
`onetime_offer_strikethrough_sku`|-|Default name for one-time relaunch offer strikethrough SKU
`onetime_start_session`|3|Premium Helper will start showing one-time premium offer (if sku's are defined) only from specified session. Otherwise regular relaunch offer is shown.
`rate_us_mode`|`validate_intent`|How to ask user for rating. Values: <br />`none`: No rating UI will be shown. App should show rate dialog manually.<br />`all`: Google Play in-app review dialog is used for app rating.<br />`validate_intent`: Validate intent dialog is shown first. In-app review dialog is shown if user selected positive rate option.
`rateus_session_start`|3|Premium Helper will show rate dialog on app start from the specified session.
`show_interstitial_onboarding_basic`|true|Show interstitial ad after onboarding on first app start.
`show_relaunch_on_resume`|true|Show relaunch on app resume or on cold start.
`show_ad_on_app_exit`|false|Show advertisement when user exit the app.
`interstitial_capping_type`|session|Capping type for interstitial ads. Set global to keep capping timing between app starts.
`interstitial_capping_seconds`|0|Capping for interstitial ads in seconds. Default 0 - no capping.
`happy_moment_capping_seconds`|0|Capping for happy moment in seconds. Default 0 - no capping.
`happy_moment_capping_type`|session|Capping type for happy moment. Set global to keep capping timing between app starts.
`happy_moment_skip_first`|0|Skip rate UI for the chosen number of first happy moment calls. Default 0 - no skip.
`show_trial_on_cta`|false|Show trial period on CTA button in purchase screens.
`ads_provider`|admob| Select ads provider (AdMob/AppLovin Max)
`ad_unit_admob_banner`|-|Ad unit ID for banner ads (Admob).
`ad_unit_admob_interstitial`|-|Ad unit ID for interstitial ads (Admob).
`ad_unit_admob_native`|-|Ad unit ID for native ads (Admob).
`ad_unit_admob_rewarded`|-|Ad unit ID for rewarded ads (Admob).
`ad_unit_admob_native_exit`|-|Ad unit ID for native exit ad (Admob).
`ad_unit_admob_banner_exit`|-|Ad unit ID for banner exit ad (Admob).
`ad_unit_applovin_banner`|-|Ad unit ID for banner ads (AppLovin).
`ad_unit_applovin_interstitial`|-|Ad unit ID for interstitial ads (AppLovin).
`ad_unit_applovin_mrec_banner`|-|Ad unit ID for medium rectangle ads (AppLovin).
`ad_unit_applovin_native`|-|Ad unit ID for native ads (AppLovin).
`ad_unit_applovin_rewarded`|-|Ad unit ID for rewarded ads (AppLovin).
`ad_unit_applovin_native_exit`|-|Ad unit ID for native exit ad (AppLovin).
`ad_unit_applovin_banner_exit`|-|Ad unit ID for banner exit ad (AppLovin).
`terms_url`|-|Url for Terms & Conditions
`privacy_url`|-|Url for Privacy policy
`toto_enabled`|true|Enable the Toto init feature
`happy_moment`|-|Configure happy moment behavior. Choose between: `none`, `default`, `in_app_review`, `validate_intent`, `in_app_review_with_ad`, `validate_intent_with_ad`
`interstitial_muted`|false|Start interstitial ads muted.
`premium_packages`|-|Comma-separated list of premium app package names. The app is considered as premium as long as any of these packages is installed.
`disable_relaunch_premium_offering`|false|Disable premium offering screen on relaunch.
`disable_onboarding_premium_offering`|false|Disable StartLikeAPro screen on first app start.
`onboarding_layout_variant`|0|Onboarding layout variant for A/B testing.
`relaunch_layout_variant`|0|Relaunch layout variant for A/B testing.
`relaunch_onetime_layout_variant`|0|Relaunch OneTime layout variant for A/B testing.
`show_contact_support_dialog`|true|Show Contact Support activity when calling `Premium.sendEmail()`
`prevent_ad_fraud`|false|Enable Interstitial Ad Fraud protection.
`prevent_ad_fraud_timeout_seconds`|10|Ad fraud protection timeout
`max_update_requests`|2|How many times the app retry in-app update request. Set to `-1` to disable in-app updates.

#### 12. Analytics

Premium Helper has an instance of the analytics (Blytics library) which sends analytics events to various analytics platforms (Firebase, Flurry).
See `premium-helper\Analytics.kt` for API reference. Default events are triggered automatically by Premium Helper.

To enable support of the analytics platforms please add the following dependencies:

```dsl
    // Firebase Analytics
    implementation 'com.google.firebase:firebase-analytics:18.0.0'
```

```dsl
    // Flurry Analytics
    implementation 'com.flurry.android:analytics:14.1.0'
```
Also you need to provide Flurry API key with PremiumHelper Initialization.

```kotlin
 Premium.initialize(this, new PremiumHelperConfiguration.Builder(BuildConfig.DEBUG)
                .mainActivityClass(MainActivity.class)
               //............
                .setFlurryApiKey("<Your flurry API key>")
                .build());
```

You can send your own events via premium-helper using `Premium.analytics.sendEvent(...)`.<br/>
Use `Premium.analytics.onFeatureUsed(feature)` event to track app features usage via analytics.<br/><br/>
Premium-helper adds following parameters to each analytics event:<br/>
`session` - number of current analytics session (sessions stops when app is closed/sent to background)<br/>
`days_since_install` - Number of days passed since app install<br/>
`occurrence` - event counter (number of times this event was sent)<br/>

#### 13. Advertisements

Premium Helper can show interstitial, banner, native and rewarded ads. It automatically shows interstitial as a part of the app relaunch logic after showing relaunch offer (if available).

##### Ads configuration

To configure ads create AdManagerConfiguration and pass it to premium-helper initializer:

```kotlin

        val admobConfig = adManagerConfig {
            bannerAd("ca-app-pub-3940256099942544/6300978111")
            interstitialAd("ca-app-pub-3940256099942544/1033173712")
            nativeAd("ca-app-pub-3940256099942544/2247696110")
            rewardedAd("ca-app-pub-3940256099942544/5224354917")
        }
        
        val applovinConfig = adManagerConfig {
            bannerAd("11111")
            bannerMRecAd("2222")
            interstitialAd("3333")
            rewardedAd("4444")
            nativeAd("5555")
            exitBannerAd("2222") //Please provide MRECT adunit id and not regular banner adunit id
            if(BuildConfig.DEBUG) testAdvertisingIds("0987654321","1234567890")
        }

        Premium.initialize(this@SampleApplication, PremiumHelperConfiguration.Builder(BuildConfig.DEBUG)
            
            // ...

            .adManagerConfiguration(admobConfig, applovinConfig)
            .build())
```

AdManagerConfiguration in Java:

```java
        AdManagerConfiguration adConfig = new AdManagerConfiguration.Builder()
                .bannerAd("ca-app-pub-3940256099942544/6300978111")
                .interstitialAd("ca-app-pub-3940256099942544/1033173712")
                .rewardedAd("ca-app-pub-3940256099942544/5224354917")
                .nativeAd("ca-app-pub-3940256099942544/2247696110")
                .build();

```
Use `.useTestAds(BuildConfig.DEBUG)` method in premium helper configuration to use test ads (will work on debug build only).

##### Banners
Banners can be added with the `PhShimmerBannerAdView`.
```xml
    <com.appboosty.ads.PhShimmerBannerAdView
        android:id="@+id/banner_container"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:shimmer_base_color="#FFFFFF"
        app:shimmer_highlight_color="#b3b3b3"
        app:banner_size="adaptive_anchored" />
```
and `PhShimmerNativeAdView` in the xml layout
```xml
    <com.appboosty.ads.PhShimmerNativeAdView
        android:id="@+id/banner_container"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:shimmer_base_color="#FFFFFF"
        app:shimmer_highlight_color="#b3b3b3"
        app:transition_animation_duration="300"
        app:native_ad_size="small" />
```

`layout_height` - use `wrap_content` for default size. When using adaptive banner you can specify the exact height of the banner you need.</br>
`shimmer_base_color`/`shimmer_highlight_color` - set shimmer loading colors<br>
`banner_size` - set which type of banner you want to see: banner, full_banner, large_banner, leaderboard, medium_rectangle, wide_skyscraper, fluid, adaptive, adaptive_anchored<br>
`native_size` - set which type of native ad you want to see: small, medium<br>
You can also set adUnitId and size via setter method `setAdunitId()`.<br>
`transition_animation_duration` - set the duration of transition animation. If set to 0 the transition animation will be disabled.<br>
`PhShimmerBannerAdView` and `PhShimmerNativeAdView` support ad loading listener that you can set with `setAdLoadingListener()` to get callback on ad loading success and failure.<br>
Both `PhShimmerBannerAdView` and `PhShimmerNativeAdView` can be removed using `removeAd()` or re-added using `addAd()` methods.<br>


##### Interstitial ads
Interstitial ads are shown by premium-helper on app start. If you want to show the interstitial ad manually in other place of the app call `showInterstitialAd(activity: Activity, callback: FullScreenContentCallback? = null)` method.<br />
If you need to check if interstitial ad is loaded and ready to be shown use `Premium.Ads.isInterstitialLoaded()` method.<br />
Capping is applied to all interstitial ads shown via premium-helper. You can control the capping interval by setting the capping time in seconds with `.setInterstitialCapping(capping)` method in configuration builder.
By default capping is reset on every app start. To keep it between app start use `GLOBAL` type when configuring capping.<br />
To show interstitial ad without capping use `showInterstitialAdWithoutCapping()` method.<br />
By default interstitial ads are started with sound, you can change it by setting the `interstitial_muted` parameter value to true.
Use `showInterstitialAdOnNextActivity()` method when you need to show interstitial ad on next activity. For example:
```kotlin
  fun onBackPressed() {
      Premium.Ads.showInterstitialAdOnNextActivity(this)
      super.onBackPressed()
  }
```
In Android 13 onBackPressed() method was deprecated and new API `onBackPressedDispatcher` was introduced.<br>
Please refer to migration guide to get more info: https://www.droidcon.com/2022/12/05/migrate-the-deprecated-onbackpressed-function-android-13/

##### Rewarded ads
To show rewarded ad you need to load it first with the `loadRewardedAd(listener: AdListener?)` method and then show it with `showRewardedAd(activity: Activity, rewardedAdCallback: OnUserEarnedRewardListener)`
<br /><br /><br />
Note: when adding AdMob support do not forget to add APPLICATION ID to the AndroidManifest file:

```xml
    <meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" android:value="@string/admob_app_id"/>
```

##### Show ad on app exit
Premium-helper can show native ad when user exit the app with the back button from main app activity. To use this feature inside the app:

1. Enable it in premium-helper initialize: `.showExitConfirmationAds(true)`

``` kotlin
        Premium.initialize(this@SampleApp,
            PremiumHelperConfiguration.Builder(BuildConfig.DEBUG)
                ...
                .showExitConfirmationAds(true)
                ...
                .build()
```

2. Call to PremiumHelperUtils.addOnMainActivityExitHandler from main application activity:

```kotlin
     override fun onCreate(savedInstanceState: Bundle?) {
       //..............
        PremiumHelperUtils.addOnMainActivityExitHandler(this)
       //.................
    }
```
In Android 13 onBackPressed() method was deprecated and new API onBackPressedDispatcher was introduced.
Please refer to migration guide to get more info: https://www.droidcon.com/2022/12/05/migrate-the-deprecated-onbackpressed-function-android-13/<br>

To disable native or banner ads on exit configure it with the `AdManager.AD_DISABLED` instead of ad unit id:
``` kotlin
        Premium.initialize(this@SampleApp,
            PremiumHelperConfiguration.Builder(BuildConfig.DEBUG)
                ...
                .exitNativeAd(AdManager.AD_DISABLED)
                ...
                .build()
```
##### Exit Ads and transparent navigation bar
If the app draws content under navigation and system bars the exit ads view may not show correctly.
1. Make sure the exit ads activity layout does not use `fitsSystemWindows=true` flag for any of the views.
2. Add the insets listener to set up the top and bottom paddings for status and navigation bars:
```java
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ViewGroup bottomAdView = findViewById(R.id.bottom_ad_view);
        ViewCompat.setOnApplyWindowInsetsListener(buttonBarsView, (v, insets) -> {
            if (insets.hasInsets()) {
                ViewCompat.setOnApplyWindowInsetsListener(buttonBarsView, null);
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                buttonBarsView.setPadding(0, systemBars.top, 0, 0);
                bottomAdView.setPadding(0, 0, 0, systemBars.bottom);
            }

            return insets;
        });
    }
```


#### 14. RxJava API

You can call Kotlin suspend methods from Java project using following methods with RxJava API:

```kotlin

    public fun getOfferRx(remoteKey: String): Single<PHResult<Offer>>
    public fun loadBannerRx(bannerSize: PHAdSize): Single<PHResult<View>>
    public fun waitForInitCompleteRx(): Single<PHResult<Boolean>>
    public fun launchBillingFlowRx(@NonNull activity: Activity, @NonNull offer: Offer): Observable<PurchaseResult>
    public fun observePurchaseStatusRx(): Observable<Boolean>
    public fun getActivePurchasesRx(): Single<PHResult<List<ActivePurchase>>>
    public fun hasHistoryPurchasesRx(): Single<PHResult<Boolean>>
    public fun consumeAllRx(): Single<PHResult<Int>>
    public fun loadNativeAdmobAdRx(count: Int = 1): Single<PHResult<Boolean>> //Deprecated
    public fun getNativeAdmobAdRx(): Single<NativeAd>  //Deprecated
    public fun loadAndGetNativeAdmobAdRx(): Single<PHResult<NativeAd>>  //Depricated
    public fun loadNativeAdsCommonRx(...): Single<PHResult<View>
    
    
```

See sample code of RxJava API usage here: https://github.com/AppBoosty/premium-helper/blob/develop/app/src/main/java/com/appboosty/sampleapp/SampleJavaActivity.java

Premium helper calls all Rx methods with `.observeOn(AndroidSchedulers.mainThread())` so the result is sent to the main thread.

#### 15. Testing with Premium Helper

All test methods will work only with the `isDebugMode=true` in the `PremiumHelperConfiguration`.

##### Start Like Pro, Relaunch page with test layouts

By default premium-helper will use debug layouts for Start Like Pro and Relaunch pages if they are missing.
Debug layouts are only available for DEBUG builds. If you do not set layouts for Start Like Pro and Relaunch the RELEASE version will crash.
To disable debug layouts use `PremiumHelperConfiguration.useTestLayouts(false)` method when initializing premium-helper.

##### Ads
Use the `PremiumHelperConfiguration.useTestAds(testAds: Boolean)` method to show ads with test unit ids.

##### Offers and purchases
You can test the app without Google Play products. To do this add debug offers.
For the main offer used on the Start Like Pro and Relaunch pages: `addDebugMainOffer(sku: String, price: String)`
For the one-time relaunch offer: `addDebugOneTimeOffer(one_time_sku: String, one_time_price: String, one_time_strike_sku: String, one_time_strike_price: String)`

To test the one-time relaunch you must define the debug offers with `addDebugOneTimeOffer(...)`. By default one-time relaunch is shown from the third app start, you can override this behavior overriding the `onetime_start_session` remote parameter.


##### Remote Config
You can override Remote Config parameter values for test purposes with the `overrideDebugValue(key: String, value: Any)` method.


#### 16. Other

##### Notifications and Widget
To get correct `source` value in the analytics `App_open` event when starting the app with intent from notification or widget add the flag to the intent:
```kotlin
        // When starting from Notification
        intent.putExtra(PremiumHelper.FLAG_FROM_NOTIFICATION, true)

        // When starting from Widget
        intent.putExtra(PremiumHelper.FLAG_FROM_WIDGET, true)

```

If you want to skip relaunch / interstitial screens on the activity started from notification, widget or shortcut add this flag:

```kotlin
        intent.putExtra(PremiumHelper.FLAG_SHOW_RELAUNCH, false)
```

For shortcut add extras to the intent in shortcuts.xml file:

```xml
    <shortcut
        ...
        <intent
            ...
            <extra
                android:name="shortcut"
                android:value="true" />

            <!-- To SKIP Relaunch -->
            <extra
                android:name="show_relaunch"
                android:value="false" />
```

##### Support email
To send email to app support with device info and logs you can use `ContactSupport.sendEmail(activity: Activity, email: String, emailVip: String?)` method.<br />
You can use an optional email for VIP support which will only be used when the app has premium subscription.<br />
The `ContactSupport.sendEmail()` method will open Contact Support activity to enter the message. If you want to override activity theme add ContactSupportActivity to your AndroidManifest:
```xml
        <activity
            android:name="com.appboosty.premiumhelper.ui.support.ContactSupportActivity"
            tools:replace="android:theme"
            android:theme="@style/<CUSTOM_THEME>"
            android:windowSoftInputMode="adjustResize"
            android:screenOrientation="portrait" />
```
Please refer to the sample app styles.xml file for the theme override example.<br />
You can use the `SHOW_CONTACT_SUPPORT_DIALOG` parameter to disable the Contact Support activity.<br />

##### Privacy Policy & Terms and Conditions
To show 'Privacy Policy' or 'Terms and Conditions' you can use `showPrivacyPolicy(activity: Activity)` or `showTermsAndConditions(activity: Activity)`


##### Utility methods
You can use some useful methods defined in `Premium.Utils`
`openApplicationSettings()` to open application settings page.
`openGooglePlay()` to open Google Play page of the app.
`openUrl()` to open browser for viewing the URL.
`shareMyApp()` to share current app url.

##### Application backup

Premium-helper stores internal configuration and parameters in the shared preferences files.
These files are excluded from auto-backup in the `ph_backup_rules.xml` file.<br />
If application defines it's own rules with the `android:fullBackupContent="..."` in
AndroidManifest file, please
1. Add the following rules:
```xml
    <exclude domain="sharedpref" path="premium_helper_data.xml" />
    <exclude domain="sharedpref" path="toto_configuration.xml" />
    <exclude domain="sharedpref" path="com.appboosty.blyics.counters.global.xml" />
```
2. Add  add `tools:replace="android:fullBackupContent"` to `<application>` element in AndroidManifest.xml.

##### Premium Preferences

You can use Preferences implementation from premium-helper to set preference as a premium feature. It will display lock icon and guide user to the app purchase screen.<br />
See sample here: https://github.com/AppBoosty/premium-helper/blob/develop/app/src/main/res/xml/root_preferences.xml

##### Requesting permissions

You can use `PermissionRequester` and `MultiplePermissionsRequester` classes for requesting required permissions.

1. Define permission requester in your activity:

```kotlin
    private val permissionRequester =
        MultiplePermissionsRequester(this, arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA))

            .onGranted {
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()            }
            .onDenied { _, result ->
                Toast.makeText(this, "Permissions ${result.entries.filterNot { it.value }.map { it.key }.toList()} denied", Toast.LENGTH_SHORT).show()
            }
            .onRationale { requester, result ->
                requester.showRationale(
                    "Permission needed",
                    "This application needs permissions to work correctly", "Ok"
                )
            }
            .onPermanentlyDenied { requester, result, canShowSettingsDialog ->
                if (canShowSettingsDialog) {
                    requester.showOpenSettingsDialog(
                        "Permission needed",
                        "This application needs permissions to work correctly",
                        "Go to settings",
                        "Later"
                    )
                }
            }
```
2. Request permissions:

```kotlin
    override fun onRelaunchComplete(isFirstAppStart: Boolean) {
        permissionRequester.request()
    }

```

3. Check permissions:
You should always use `PermissionUtils.hasPermission()` method to check if permission is granted.
This method checks android version and always returns `true` for `WRITE_EXTERNAL_STORAGE` permission
if Android SDK is greater than 28.   

##### Day / Night themes
You can force day/night theme in the app with these utility methods:
```kotlin
    Premium.Utils.setDayMode()
    Premium.Utils.setNightMode()
```

##### Lint rules

Premium-helper library has a built-in lint rule `InvalidPremiumHelperConfiguration`. Add the rule to the build.gradle file to check premium-helper configuration issues on release build:
```
lintOptions {
        abortOnError false
        fatal 'InvalidPremiumHelperConfiguration'
    }
```


##### Interstitial Ad Fraud

By default the `showInterstitial()` method will wait for the interstitial ad loading if it is not available immediately.
This could break Google Ad policy for showing interstitial ads (https://support.google.com/admob/answer/6201362).
This can be avoided by enabling the ad fraud protection with the `preventAdFraud(true)` configuration method.
When enabled Ad Fraud protection will allow showing Interstitial Ad only if it is already loaded and available immediately.
It also adds extra splash screen delay to wait for first interstitial ad loading.

##### In-app updates

Premium helper checks for in-app update availability on app start after showing interstitial ad and when no relaunch offer is shown. If user declines the update
the in-app update is requested again until max attempts (`max_update_requests`) is exceeded.
You can set `max_update_requests` to `-1` to disable the in-app updates feature. 


#### Callback for 'on paid impression' event.
When host application need to be informed about 'on paid impression' events it can set a listener. The callback will be called 
each time AdMob/AppLovin ADs generate this event.
The API is:
```
PremiumHelper.getInstance().setExternalOnPaidImpressionListener(object:
            PaidImpressionListener{
            override fun onPaidImpression(params: Bundle) {

            }
        })
```   

#### Purchase result listener

Host applicatrion can observe/subscribe to purchase status update events.
The API:<br> 
Kotlin
```
        CoroutineScope(Dispatchers.Default).launch {
            PremiumHelper.getInstance().observePurchaseResult().collect { result->

            }
        }
```
Java
```
        PremiumHelper.getInstance().observePurchaseResultRx().subscribe( result -> {

        });
```



