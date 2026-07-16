# Add project specific ProGuard rules here.
# Keep chess model classes readable in crash reports until release hardening is done.
-keep class com.checkmatey.core.chess.** { *; }
