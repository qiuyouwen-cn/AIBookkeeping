# Add project specific ProGuard rules here.
-keepattributes Signature
-keepattributes *Annotation*

# Keep Gson
-keep class com.google.gson.** { *; }

# Keep data models
-keep class com.ai.bookkeeping.model.** { *; }
