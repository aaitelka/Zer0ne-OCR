# Compose Desktop ProGuard rules

# Keep the main class
-keep class ma.zer0ne.ocr.MainKt { *; }

# Keep Compose runtime
-keep class androidx.compose.** { *; }
-keep class org.jetbrains.skia.** { *; }
-keep class org.jetbrains.skiko.** { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class ma.zer0ne.ocr.**$$serializer { *; }
-keepclassmembers class ma.zer0ne.ocr.** {
    *** Companion;
}
-keepclasseswithmembers class ma.zer0ne.ocr.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Ktor
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }

# Keep Apache POI and PDFBox
-keep class org.apache.poi.** { *; }
-keep class org.apache.pdfbox.** { *; }

# Keep service loaders
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep Logback
-keep class ch.qos.logback.** { *; }
-keep class org.slf4j.** { *; }

# General rules
-dontwarn org.apache.**
-dontwarn javax.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
