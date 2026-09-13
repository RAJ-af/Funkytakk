# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep annotations and generic signatures
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes SourceFile, LineNumberTable

# Keep Moshi models and generated JSON adapters
-keepclasseswithmembers class * {
    @com.squareup.moshi.JsonClass <methods>;
    @com.squareup.moshi.JsonClass <fields>;
}
-keep class com.example.** { *; }
-keep class * extends com.squareup.moshi.JsonAdapter { *; }

# Keep Retrofit interface methods
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep Firebase models and credentials
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.firestore.* <fields>;
}

