# Add project specific ProGuard rules here.

# Keep data model classes used for RSS parsing (constructed via reflection-free code, but safe to keep)
-keep class com.mohan.news.data.** { *; }

# OkHttp / okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keepattributes Signature
-keepattributes *Annotation*

# Coil
-dontwarn coil.**

# Keep line numbers for readable crash traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
