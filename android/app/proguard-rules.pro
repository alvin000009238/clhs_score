# ProGuard / R8 rules for SchoolGrades

# Google Tink / security-crypto
-dontwarn com.google.errorprone.annotations.**
-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite {
  <fields>;
}

# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.AnnotationsKt

# WorkManager stores worker class names in its database. Keep the app worker name
# stable without preventing R8 from shrinking WorkManager and its transitive deps.
-keepnames class com.clhs.score.reminders.GradeReminderWorker
-keepclassmembers class com.clhs.score.reminders.GradeReminderWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
