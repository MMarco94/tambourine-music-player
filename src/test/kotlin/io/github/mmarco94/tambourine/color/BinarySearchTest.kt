package io.github.mmarco94.tambourine.color

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BinarySearchTest : FunSpec({
    test("binarySearch") {
        binarySearch(listOf(0, 1, 2, 3), 3, true) shouldBe 3
        binarySearch(listOf(0, 1, 2, 3), 3, false) shouldBe 4
        binarySearch(listOf(0, 1, 2, 3, 3, 3), 3, true) shouldBe 3
        binarySearch(listOf(0, 1, 2, 3, 3, 3), 3, false) shouldBe 6
        binarySearch(listOf(0, 1, 2, 3, 3, 3, 4), 3, true) shouldBe 3
        binarySearch(listOf(0, 1, 2, 3, 3, 3, 4), 3, false) shouldBe 6
        binarySearch(listOf(40), 0, false) shouldBe 0
        binarySearch(listOf(0, 40), 0, false) shouldBe 1
        binarySearch(listOf(0, 0, 0, 40), 0, false) shouldBe 3
        binarySearch(listOf(0, 1000), 500, true) shouldBe 1
        binarySearch(listOf(0, 1000), 500, false) shouldBe 1
    }

})

private fun binarySearch(list: List<Int>, value: Int, preferStart: Boolean): Int {
    return binarySearch(0, list.size - 1, preferStart) {
        list[it].compareTo(value)
    }
}
