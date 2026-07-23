# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep AntiBan class
-keep class com.systemhelper.AntiBan { *; }

# Keep NativeHelper class
-keep class com.systemhelper.NativeHelper { *; }
-keep class com.systemhelper.NativeHelper$PlayerInfo { *; }

# Obfuscate everything else
-repackageclasses ''
-allowaccessmodification
