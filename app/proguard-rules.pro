# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep annotations, Kotlin metadata, and generic signatures (Crashlytics stacktraces + Moshi reflection)
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keepattributes SourceFile, LineNumberTable
-keep class kotlin.Metadata { *; }

# Keep Moshi models, Kotlin adapters, and @Json annotations
-keepclasseswithmembers class * {
    @com.squareup.moshi.JsonClass <methods>;
    @com.squareup.moshi.JsonClass <fields>;
}
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
    @com.squareup.moshi.Json <methods>;
}
-keep class com.example.** { *; }
-keep class * extends com.squareup.moshi.JsonAdapter { *; }
-keep class com.squareup.moshi.** { *; }

# Keep Retrofit & OkHttp / Okio
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keepattributes Exceptions
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Keep Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# Google Tink (E2EE Chat Vault & HPKE)
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# SQLCipher (Encrypted Local Chat DB)
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# Agora Voice SDK
-keep class io.agora.** { *; }
-dontwarn io.agora.**

# Firebase (Auth, Firestore, Messaging, Crashlytics, Analytics, App Check)
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.firestore.* <fields>;
}
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Coil & Telephoto Image Loading
-keep class coil.** { *; }
-keep class me.saket.telephoto.** { *; }
-dontwarn coil.**

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Biometric & Lottie
-keep class androidx.biometric.** { *; }
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# Jetpack Compose & Navigation Runtime
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Strip verbose and debug logs in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

