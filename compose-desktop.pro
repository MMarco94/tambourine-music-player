-dontwarn javax.annotation.**
-dontwarn kotlinx.serialization.**
-keep public class io.github.mmarco94.tambourine.** { *; }
# Logs
-dontwarn ch.qos.logback.**
-keep public class org.slf4j.** { *; }
-keep public class ch.qos.logback.** { *; }
# Audio
-keep public class org.jaudiotagger.** { *; }
-keep public class com.tagtraum.ffsampledsp.** { *; }
# DBus
-keep public class org.freedesktop.dbus.** { *; }
-keep public class * implements org.freedesktop.dbus.interfaces.DBusInterface { *; }
-keep public class * extends org.freedesktop.dbus.messages.Message { *; }
# Flow is wrongly removed
-keep class kotlinx.coroutines.flow.** { *; }