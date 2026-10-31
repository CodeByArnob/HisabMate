# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Room Database entities and DAOs intact during R8/ProGuard optimization
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
-keepclassmembers class com.example.data.local.** { *; }

# Keep BroadcastReceivers for local notifications and reboot rescheduling
-keep class com.example.util.ReminderNotificationReceiver { *; }
-keep class com.example.util.ReminderBootAndTimeReceiver { *; }

# Preserve source file and line numbers for readable crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
