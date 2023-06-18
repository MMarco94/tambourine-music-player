package io.github.mmarco94.tambourine.utils

class AppendOnlyList<T> private constructor(
    private val internalList: ArrayList<T>
) : List<T> by internalList {
    constructor() : this(ArrayList())

    fun add(element: T) {
        internalList.add(element)
    }

    // It is guaranteed for the returned list to be "immutable" from the perspective of the user
    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
        require(fromIndex in 0 until size)
        require(toIndex in 0..size)
        require(fromIndex <= toIndex)
        return object : AbstractList<T>() {
            override val size: Int = toIndex - fromIndex

            override fun get(index: Int): T {
                require(index in indices)
                return internalList[fromIndex + index]
            }
        }
    }
}