package io.github.mmarco94.tambourine.data

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth

class PartialDateTest : FunSpec({
    context("parse") {
        withData(
            nameFn = { "'${it.first}'" },
            "" to null,
            "asd" to null,
            "2020" to PartialDate.Year(2020),
            "asd--" to null,
            "asd-123" to null,
            "2020-asd" to null,
            "2020-asd-asd" to null,
            "2020-1-asd" to null,
            "2020-asd-1" to null,
            "2020-0" to null,
            "2020-13" to null,
            "2020-01" to PartialDate.Month(YearMonth(2020, Month.JANUARY)),
            "2020-12" to PartialDate.Month(YearMonth(2020, Month.DECEMBER)),
            "2020-00-02" to null,
            "2020-13-02" to null,
            "2020-01-02" to PartialDate.Date(LocalDate(2020, Month.JANUARY, 2)),
            "2020-12-02" to PartialDate.Date(LocalDate(2020, Month.DECEMBER, 2)),
            "2020-12-00" to null,
        ) { (str, expected) ->
            PartialDate.parse(str) shouldBe expected
        }
    }
})
