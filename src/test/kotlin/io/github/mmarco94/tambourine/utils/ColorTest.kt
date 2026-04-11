package io.github.mmarco94.tambourine.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.tuple
import io.kotest.datatest.withData
import io.kotest.matchers.ranges.shouldBeIn
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.float
import io.kotest.property.checkAll

class ColorTest : FunSpec({
    context("hueDistance") {
        withData(
            tuple(10f, 20f, 10f),
            tuple(10f, 5f, 5f),
            tuple(10f, 100f, 90f),
            tuple(10f, 180f, 170f),
            tuple(10f, 190f, 180f),
            tuple(10f, 200f, 170f),
            tuple(10f, 250f, 120f),
            tuple(10f, 350f, 20f),
        ) { (h1, h2, expected) ->
            val c1 = HSLColor(h1, 0f, 0f)
            val c2 = HSLColor(h2, 0f, 0f)
            c1.hueDistance(c2) shouldBe expected
            c2.hueDistance(c1) shouldBe expected
        }
        test("Always between 0 and 180") {
            checkAll(
                Arb.float(0f, 360f, includeNaNs = false),
                Arb.float(0f, 360f, includeNaNs = false),
            ) { h1, h2 ->
                val c1 = HSLColor(h1, 0f, 0f)
                val c2 = HSLColor(h2, 0f, 0f)
                c1.hueDistance(c2) shouldBeIn 0f..180f
            }
        }
    }

})
