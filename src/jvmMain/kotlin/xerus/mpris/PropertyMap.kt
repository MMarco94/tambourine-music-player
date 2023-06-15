package xerus.mpris

import org.freedesktop.dbus.types.Variant

class PropertyMap private constructor(private val map: MutableMap<String, Variant<*>>) :
    MutableMap<String, Variant<*>> by map {

    constructor(initializer: PropertyMap.() -> Unit) : this(HashMap()) {
        initializer(this)
    }

    constructor(vararg elements: Pair<String, Variant<*>>) : this(mutableMapOf(*elements))

    override val entries: MutableSet<MutableMap.MutableEntry<String, Variant<*>>>
        get() = refresh().entries
    override val values: MutableCollection<Variant<*>>
        get() = refresh().values

    override fun containsValue(value: Variant<*>) = refresh().containsValue(value)

    private val mapCallable = HashMap<String, () -> Any?>()


    fun refresh(): MutableMap<String, Variant<*>> {
        mapCallable.forEach { key, cal -> refresh(key, cal) }
        return map
    }

    private fun refresh(key: String, cal: (() -> Any?)? = mapCallable[key]): Any? {
        val value = (cal ?: return null).invoke()
        value?.variant()?.let { map[key] = it } ?: map.remove(key)
        return value
    }

    override operator fun get(key: String) =
        refresh(key)?.variant() ?: map[key]

    fun getValue(key: String): Any = get(key)!!.value

    operator fun set(key: String, value: Any) = put(key, value)

    fun put(key: String, value: Any) {
        map[key] = value.variant()
    }

    fun put(key: String, cal: () -> Any?) {
        mapCallable[key] = cal
        refresh(key)
    }

    override fun toString(): String {
        return "PropertyMap$map"
    }

}
