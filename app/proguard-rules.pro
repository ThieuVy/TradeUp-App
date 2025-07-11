# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ==================================
# === Stripe Android SDK Rules ===
# ==================================

# Keep all classes in the com.stripe package and their members.
# This is the most important rule to resolve ClassNotFoundException issues.
-keep class com.stripe.** { *; }
-keep interface com.stripe.** { *; }

# Keep all classes that implement Parcelable and their required CREATOR field.
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# Keep methods that are called via reflection by the GSON library (used by Stripe).
-keepclassmembers,allowobfuscation class com.stripe.android.** {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Keep constructors of model classes.
-keepclassmembers class com.stripe.android.model.** {
  public <init>(...);
}
-keepclassmembers class com.stripe.android.cards.** {
  public <init>(...);
}

# ========== SỬA LỖI ==========
#
# CÁC DÒNG GÂY RA LỖI CÚ PHÁP ĐÃ ĐƯỢC XÓA BỎ
#
# ===============================

# Additional rules to ensure nothing is missed.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses