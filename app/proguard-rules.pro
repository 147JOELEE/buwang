# ProGuard rules for BuWang App

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Room entities
-keep class com.buwang.app.data.local.entity.** { *; }

# Keep data classes used for serialization
-keep class com.buwang.app.data.remote.dto.** { *; }
-keep class com.buwang.app.domain.model.** { *; }

# Keep Retrofit interfaces
-keep,allowobfuscation interface com.buwang.app.data.remote.api.** { *; }

# Keep Tink
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
