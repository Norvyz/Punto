# Add project specific ProGuard rules here.
# Keep rules will be added when features are implemented.

# Rive: conservar las clases públicas de la librería (JNI)
-keep class app.rive.** { *; }
-keep class app.rive.runtime.kotlin.** { *; }
