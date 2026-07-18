# Tell Shell ProGuard Rules

# Keep Shizuku API
-keep class rikka.shizuku.** { *; }
-keep class rikka.sui.** { *; }

# Keep Gson
-keepattributes Signature
-keep class com.test.network.** { *; }
-keep class com.tellshell.app.network.** { *; }

# Keep DeepSeek API response models
-keepclassmembers class com.tellshell.app.network.** {
    <fields>;
}
