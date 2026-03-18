# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep JNI Methods
-keep class com.mahavtaar.vibecoder.llm.LlamaJni { *; }

# Keep JavascriptInterfaces
-keepclassmembers class com.mahavtaar.vibecoder.editor.EditorViewModel {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class com.mahavtaar.vibecoder.ui.terminal.TerminalViewModel {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Room DAOs and Entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Hilt & Coroutines
-keep,allowobfuscation,allowshrinking class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keep,allowobfuscation,allowshrinking class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keep,allowobfuscation,allowshrinking class kotlinx.coroutines.android.AndroidDispatcherFactory {}

# Ktor Serialization
-keep class io.ktor.** { *; }
-keep class kotlinx.serialization.** { *; }
