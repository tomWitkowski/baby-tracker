-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Room entities (used via reflection by Room)
-keep class com.babytracker.data.db.entity.** { *; }

# Sync data classes (serialised manually via org.json — keep field names)
-keep class com.babytracker.data.sync.SyncMessage { *; }

# Google Play Billing
-keep class com.android.billingclient.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
