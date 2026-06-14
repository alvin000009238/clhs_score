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
-dontwarn org.jsoup.**

# WorkManager stores worker class names in its database. Keep the app worker name
# stable without preventing R8 from shrinking WorkManager and its transitive deps.
-keepnames class com.clhs.score.reminders.GradeReminderWorker
-keepclassmembers class com.clhs.score.reminders.GradeReminderWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
