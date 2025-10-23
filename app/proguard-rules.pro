-keepattributes *Annotation*
-keepclassmembers class ** {
    @org.jetbrains.annotations.NotNull <methods>;
}
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**