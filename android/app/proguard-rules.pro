# ProGuard / R8 rules for SchoolGrades

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# Google Tink / security-crypto
-dontwarn com.google.errorprone.annotations.**

# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
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

# Keep data classes used for JSON parsing
-keep class com.clhs.score.data.** { *; }
