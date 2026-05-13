# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Security rules
-keep class com.klentahn.plexyaudiobooks.BuildConfig { *; }

# Keep Room generated code
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class * implements androidx.room.Entity

# SQLCipher
-keep class net.sqlcipher.** { *; }
-keep class net.zetetic.** { *; }

# Moshi
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}

# Retrofit
-keepattributes Signature, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# General Keep for models to avoid Moshi/Room issues if not fully annotated
-keep @com.squareup.moshi.JsonClass class *
-keep class com.klentahn.plexyaudiobooks.data.model.** { *; }
-keep class com.klentahn.plexyaudiobooks.data.local.db.** { *; }
