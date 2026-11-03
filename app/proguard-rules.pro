# Room, Coil, DataStore and Compose ship their own consumer rules.
-keepattributes *Annotation*

# Category keys are stored explicitly as strings, but keep the enum intact anyway.
-keep enum com.hawatraders.manager.data.db.Category { *; }

# Tesseract OCR (JNI)
-keep class com.googlecode.tesseract.android.** { *; }
