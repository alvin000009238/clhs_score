# ProGuard / R8 rules for SchoolGrades

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# Google Tink / security-crypto
-dontwarn com.google.errorprone.annotations.**
-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite {
  <fields>;
}

# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.clhs.score.**$$serializer { *; }
-keepclassmembers class com.clhs.score.** {
    *** Companion;
}
-keepclasseswithmembers class com.clhs.score.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Jsoup
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# Glance and Widgets
-keep class androidx.glance.** { *; }
-keep class com.clhs.score.widget.** { *; }

# WorkManager + Room + SQLite (transitive dep from Firebase Messaging)
-keep class androidx.work.** { *; }
-keep class androidx.room.** { *; }
-keep class androidx.sqlite.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }
