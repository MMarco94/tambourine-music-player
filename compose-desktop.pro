-dontwarn javax.annotation.**
-dontwarn kotlinx.serialization.**
-keep public class io.github.mmarco94.tambourine.** { *; }
# Logs
-dontwarn org.tinylog.**
-dontwarn io.github.oshai.kotlinlogging.logback.**
-dontwarn com.oracle.svm.core.annotate.**
-keep public class org.slf4j.** { *; }
-keep public class org.tinylog.** { *; }
# Audio
-keep public class org.jaudiotagger.** { *; }
-keep public class com.tagtraum.ffsampledsp.** { *; }
# DBus
-keep public class org.freedesktop.dbus.** { *; }
-keep public class * implements org.freedesktop.dbus.interfaces.DBusInterface { *; }
-keep public class * extends org.freedesktop.dbus.messages.Message { *; }
# Flow is wrongly removed
-keep class kotlinx.coroutines.flow.** { *; }