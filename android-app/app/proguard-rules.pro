# Keep encryption classes
-keep class com.catlover.app.security.** { *; }
-keep class javax.crypto.** { *; }

# Socket.IO
-keep class io.socket.** { *; }
-keep class okhttp3.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
