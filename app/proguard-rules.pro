# Default Proguard Rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class com.example.data.** { *; }
-keep class com.example.storage.** { *; }
