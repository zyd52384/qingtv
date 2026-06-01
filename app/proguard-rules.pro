# Add project specific ProGuard rules here.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class com.qingkan.tv.model.** { *; }
-keep class com.qingkan.tv.data.** { *; }
