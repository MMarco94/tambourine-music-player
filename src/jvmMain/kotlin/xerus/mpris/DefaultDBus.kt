package xerus.mpris

import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.types.DBusMapType
import org.freedesktop.dbus.types.Variant
import java.security.InvalidParameterException
import java.util.*
import kotlin.reflect.KClass


fun Any.variant(): Variant<*> {
	if (this is Variant<*>)
		return this
	if (this is Map<*, *>)
		throw InvalidParameterException("Map.variant has to be used for Maps")
	return Variant(this)
}

inline fun <reified K, reified V> Map<K, V>.variant() =
	Variant(this, DBusMapType(K::class.java, V::class.java))

fun <K : Any, V : Any> Map<K, V>.variant(
	keyClass: KClass<K>,
	valueClass: KClass<V>
) =
	Variant(this, DBusMapType(keyClass.java, valueClass.java))

interface DefaultDBus : Properties {

	override fun isRemote() = false

	override fun <A : Any> Get(interface_name: String, property_name: String): A {
		val value = GetAll(interface_name)[property_name]
			?: throw InvalidParameterException("Property $property_name for $interface_name not found")
		logger.debug("Request of $property_name from $interface_name yielded $value")
		@Suppress("UNCHECKED_CAST")
		return value as A
	}

	override fun <A : Any> Set(interface_name: String, property_name: String, value: A) {
		GetAll(interface_name)[property_name] = value.variant()
		propertyChanged(interface_name, property_name)
	}

	/** returns a new [Properties.PropertiesChanged] signal */
	fun propertyChanged(interface_name: String, property_name: String) =
		Properties.PropertiesChanged(
			objectPath,
			interface_name,
			Collections.singletonMap<String, Variant<*>>(
				property_name,
				Get<Any>(interface_name, property_name).variant()
			),
			Collections.emptyList()
		)

}
