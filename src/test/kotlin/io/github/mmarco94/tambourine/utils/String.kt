package io.github.mmarco94.tambourine.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.tuple
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import kotlin.String

class String : FunSpec({
    context("substringTrimmed") {
        context("full string") {
            withData(
                nameFn = { it.first },
                "asd" to "asd",
                "    asd" to "asd",
                "asd    " to "asd",
                "    asd    " to "asd",
            ) { (input, expected) ->
                input.substringTrimmed(0, input.length) shouldBe expected
            }
        }
        context("full string") {
            withData(
                nameFn = { it.a },
                tuple("asd", 0, 0, ""),
                tuple("asd", 0, 3, "asd"),
                tuple("asd   ", 0, 3, "asd"),
                tuple("asd   ", 2, 3, "d"),
                tuple("   asd   ", 1, 5, "as"),
            ) { (str, start, end, expected) ->
                str.substringTrimmed(start, end) shouldBe expected
            }
        }
    }
    context("forAllLines") {
        withData(
            nameFn = { "'${it.first}'" },
            "" to emptyList(),
            "\n" to emptyList(),
            "\n\n" to emptyList(),
            "\n\n\n\n" to emptyList(),
            "a" to listOf("a"),
            "a\n" to listOf("a"),
            "\na" to listOf("a"),
            "\na\n" to listOf("a"),
            "a\nb" to listOf("a", "b"),
            "a\n\nb" to listOf("a", "b"),
            "\n\na\n\nb\n\n" to listOf("a", "b"),
            "a\nb\nc" to listOf("a", "b", "c"),
        ) { (input, expected) ->
            val lines = mutableListOf<String>()
            input.forAllLines { start, end ->
                lines.add(input.substring(start, end))
            }
            lines shouldBe expected
        }
    }
})
