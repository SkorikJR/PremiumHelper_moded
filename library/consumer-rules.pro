-keep class com.appboosty.premiumhelper.toto.** { *; }
-keep class com.appboosty.ads.config.**  { *; }
-keep class com.appboosty.premiumhelper.util.ActivePurchaseInfo  { *; }
-keep class com.appboosty.premiumhelper.configuration.appconfig.PremiumHelperConfiguration  { *; }
-keepclassmembers enum * { *; }

-keep public class org.slf4j.** { *; }
-keep public class ch.** { *; }

-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

-dontwarn org.codehaus.mojo.animal_sniffer.*

-keep class android.support.v8.renderscript.** { *; }
-keep class androidx.renderscript.** { *; }