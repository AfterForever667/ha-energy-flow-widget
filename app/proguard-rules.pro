# kotlinx.serialization keeps generated serializers via @Serializable companions.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class dev.dph.energyflow.** {
    *** Companion;
}
-keepclasseswithmembers class dev.dph.energyflow.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp platform shims referenced reflectively on some JVMs.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
