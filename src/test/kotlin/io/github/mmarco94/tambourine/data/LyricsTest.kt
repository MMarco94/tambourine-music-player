package io.github.mmarco94.tambourine.data

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LyricsTest : FunSpec({
    context("toSynchronizedLine") {
        withData(
            LrcFile.Entry("", "", null) to null,
            LrcFile.Entry("", "", "asd") to null,
            LrcFile.Entry("12", "34", null) to null,
            LrcFile.Entry("12", "34", "asd") to 12.minutes + 34.seconds,
            LrcFile.Entry("", "56", "asd") to 56.seconds,
            LrcFile.Entry("1", "2.34", "asd") to 1.minutes + 2.seconds + 340.milliseconds,
            LrcFile.Entry("1", "a.34", "asd") to null,
            LrcFile.Entry("1", "3.a", "asd") to 1.minutes + 3.seconds,
            LrcFile.Entry("1", "3.", "asd") to 1.minutes + 3.seconds,
            LrcFile.Entry("1", "3", "asd") to 1.minutes + 3.seconds,
        ) { (entry, expected) ->
            entry.toSynchronizedLine(0)?.start shouldBe expected
            if (expected != null) {
                entry.toSynchronizedLine(100)?.start shouldBe expected - 100.milliseconds
                entry.toSynchronizedLine(-300)?.start shouldBe expected + 300.milliseconds
            }
        }
    }
})
