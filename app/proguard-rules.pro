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
-dontwarn javax.annotation.Nullable

-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep interface retrofit2.** { *; }
-keep class org.withus.app.remote.ApiService { *; }
-dontwarn retrofit2.**


# Kakao SDK 난독화 방지
-keep class com.kakao.sdk.**.model.** { *; }
-keep interface com.kakao.sdk.**.Api { *; }

# 에러 코드 관련 Enum 및 필드 유지 (NoSuchFieldException 방지)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Retrofit & OkHttp 관련 (네트워크 인터페이스 보호)
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep interface retrofit2.** { *; }
-keep class com.yeogijeogi.withus.data.remote.** { *; } # ApiService 패키지 경로

# 특정 필드 TokenNotFound 관련 추정 클래스 보호
# (정확한 클래스를 알 수 없을 때 카카오 관련 모델 전체를 유지)
-keep class com.kakao.sdk.auth.model.** { *; }
