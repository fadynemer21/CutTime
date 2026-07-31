# Keep useful stack-trace metadata while allowing R8 to optimize production code.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Firestore creates this model reflectively via DocumentSnapshot.toObject().
-keep class com.fadynemer.cutime.model.UserProfile {
    public <init>();
    <fields>;
}

# Firebase and WorkManager provide their own consumer rules. The CuTime
# MessagingService and Worker are also manifest/initializer referenced, but these
# explicit rules document that they are runtime entry points.
-keep class com.fadynemer.cutime.notifications.CuTimeMessagingService { *; }
-keep class com.fadynemer.cutime.notifications.AppointmentReminderWorker { *; }
