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

# Giữ lại tất cả các lớp trong package com.stripe và các thành viên của chúng.
# Đây là quy tắc quan trọng nhất để giải quyết lỗi ClassNotFoundException.
-keep class com.stripe.** { *; }
-keep interface com.stripe.** { *; }

# Giữ lại tất cả các lớp implement Parcelable và trường CREATOR bắt buộc của chúng.
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# Giữ lại các phương thức được gọi qua reflection bởi thư viện GSON (dùng bởi Stripe).
-keepclassmembers,allowobfuscation class com.stripe.android.** {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Giữ lại các constructor của các lớp model.
-keepclassmembers class com.stripe.android.model.** {
  public <init>(...);
}
-keepclassmembers class com.stripe.android.cards.** {
  public <init>(...);
}

# ========== BẮT ĐẦU PHẦN SỬA LỖI ==========
#
# CÁC DÒNG GÂY LỖI CÚ PHÁP ĐÃ ĐƯỢC XÓA BỎ
# -keep @com.stripe.android.core.networking.StripeRequest$Method { *; }
# -keep @com.stripe.android.core.networking.StripeRequest$MimeType { *; }
#
# =========================================

# Quy tắc bổ sung để đảm bảo không có gì bị thiếu.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses