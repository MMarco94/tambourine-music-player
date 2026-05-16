package io.github.mmarco94.tambourine.data

import io.github.mmarco94.tambourine.utils.letIfPositive
import io.github.mmarco94.tambourine.utils.takeIfPositive
import io.github.mmarco94.tambourine.utils.toPositiveIntOrMinusOne
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaYearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    .withLocale(Locale.getDefault())

private val yearMonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

sealed interface PartialDate {
    val yearInt: Int
    val year: Year

    fun oldest(another: PartialDate): PartialDate
    fun newest(another: PartialDate): PartialDate

    sealed interface WithMonth : PartialDate {
        val yearMonth: YearMonth
        val month: Month
    }

    data class Year(override val yearInt: Int) : PartialDate {
        override val year = this
        private val formatted = yearInt.toString()

        override fun oldest(another: PartialDate): PartialDate {
            return if (this.yearInt <= another.yearInt) {
                this
            } else {
                another
            }
        }

        override fun newest(another: PartialDate): PartialDate {
            return if (this.yearInt >= another.yearInt) {
                this
            } else {
                another
            }
        }

        override fun toString() = formatted
    }

    data class Month(override val yearMonth: YearMonth, override val year: Year) : PartialDate, WithMonth {
        private val javaYearMonth = yearMonth.toJavaYearMonth()

        override val yearInt = year.yearInt
        override val month = this

        private val formatted: String = yearMonthFormatter.format(javaYearMonth)

        override fun oldest(another: PartialDate): PartialDate {
            return when (another) {
                is Year -> another.oldest(this)
                is WithMonth -> if (this.yearMonth <= another.yearMonth) {
                    this
                } else {
                    another
                }
            }
        }

        override fun newest(another: PartialDate): PartialDate {
            return when (another) {
                is Year -> another.newest(this)
                is WithMonth -> if (this.yearMonth >= another.yearMonth) {
                    this
                } else {
                    another
                }
            }
        }

        override fun toString() = formatted
    }

    data class Date(val date: LocalDate, override val month: Month) : PartialDate, WithMonth {
        private val javaDate = date.toJavaLocalDate()

        override val yearInt = month.yearInt
        override val yearMonth = month.yearMonth
        override val year = month.year

        private val formatted: String = dateFormatter.format(javaDate)

        override fun oldest(another: PartialDate): PartialDate {
            return when (another) {
                is Date -> if (this.date <= another.date) {
                    this
                } else {
                    another
                }

                else -> another.oldest(this)
            }
        }

        override fun newest(another: PartialDate): PartialDate {
            return when (another) {
                is Date -> if (this.date >= another.date) {
                    this
                } else {
                    another
                }

                else -> another.newest(this)
            }
        }

        override fun toString() = formatted
    }

    companion object {
        suspend fun parse(string: String, cache: PartialDateParserCache): PartialDate? {
            return cache.use { cache ->
                val ios = string.indexOf('-')
                if (ios < 0) {
                    // Year
                    return@use string.toPositiveIntOrMinusOne().letIfPositive { cache.year(it) }
                }

                val ios2 = string.indexOf('-', startIndex = ios + 1)
                if (ios2 < 0) {
                    // Year-Month
                    val year = string.toPositiveIntOrMinusOne(start = 0, end = ios).takeIfPositive { return@use null }
                    val month = string.toPositiveIntOrMinusOne(start = ios + 1).takeIfPositive { return@use null }
                    return@use cache.month(year, month)
                }

                val timeStart = string.indexOf('T', startIndex = ios2 + 1).takeIfPositive { string.length }

                val year = string.toPositiveIntOrMinusOne(start = 0, end = ios).takeIfPositive { return@use null }
                val month =
                    string.toPositiveIntOrMinusOne(start = ios + 1, end = ios2).takeIfPositive { return@use null }
                val day =
                    string.toPositiveIntOrMinusOne(start = ios2 + 1, end = timeStart).takeIfPositive { return@use null }

                cache.date(year, month, day)
            }
        }
    }
}

class PartialDateParserCache {
    private val mutex = Mutex()
    private val cache = Cache()

    suspend fun <T> use(block: (Cache) -> T): T {
        return mutex.withLock {
            block(cache)
        }
    }

    class Cache {
        private val yearCache = mutableMapOf<Int, PartialDate.Year>()
        private val monthCache = mutableMapOf<Int, PartialDate.Month>()
        private val dateCache = mutableMapOf<Int, PartialDate.Date>()
        fun year(year: Int): PartialDate.Year {
            return yearCache.getOrPut(year) { PartialDate.Year(year) }
        }

        fun month(year: Int, month: Int): PartialDate.Month? {
            if (month !in 1..12) return null
            val key = year * 12 + month
            return monthCache.getOrPut(key) {
                PartialDate.Month(YearMonth(year, month), year(year))
            }
        }

        fun date(year: Int, month: Int, day: Int): PartialDate.Date? {
            val partialDateMonth = month(year, month) ?: return null
            if (day !in 1..partialDateMonth.yearMonth.numberOfDays) return null
            val key = year * 12 * 40 + month * 40 + day
            return dateCache.getOrPut(key) {
                PartialDate.Date(LocalDate(year, month, day), partialDateMonth)
            }
        }
    }
}

data class PartialDateRange(
    val oldest: PartialDate,
    val newest: PartialDate,
) {

    override fun toString(): String {
        return if (oldest == newest) {
            oldest.toString()
        } else if (newest is PartialDate.WithMonth && oldest is PartialDate.WithMonth && newest.yearMonth == oldest.yearMonth) {
            newest.month.toString()
        } else if (newest.yearInt == oldest.yearInt) {
            newest.year.toString()
        } else {
            "${oldest.year} - ${newest.year}"
        }
    }

    companion object {
        fun <T> of(collection: Collection<T>, f: (T) -> PartialDate?): PartialDateRange? {
            var oldest: PartialDate? = null
            var newest: PartialDate? = null
            collection.forEach {
                val date = f(it)
                if (date != null) {
                    oldest = oldest?.oldest(date) ?: date
                    newest = newest?.newest(date) ?: date
                }
            }
            return if (oldest != null && newest != null) {
                PartialDateRange(oldest, newest)
            } else {
                null
            }
        }
    }
}

val COMPARE_OLDEST = Comparator<PartialDate?> { a, b ->
    if (a != null && b != null) {
        val oldest = a.oldest(b)
        when (a) {
            b -> 0
            oldest -> -1
            else -> 1
        }
    } else if (a == null && b == null) {
        return@Comparator 0
    } else if (b == null) {
        return@Comparator -1
    } else {
        return@Comparator 1
    }
}

val COMPARE_NEWEST = Comparator<PartialDate?> { a, b ->
    if (a != null && b != null) {
        val newest = a.newest(b)
        when (a) {
            b -> 0
            newest -> -1
            else -> 1
        }
    } else if (a == null && b == null) {
        return@Comparator 0
    } else if (b == null) {
        return@Comparator -1
    } else {
        return@Comparator 1
    }
}
