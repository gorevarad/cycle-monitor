# Add project specific ProGuard rules here.
# See https://developer.android.com/build/shrink-code for details.

# Room generates code at compile time; keep annotations it relies on for reflection-free access.
-keep class com.cyclemonitor.app.data.db.** { *; }
