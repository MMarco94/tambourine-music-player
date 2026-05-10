package io.github.mmarco94.tambourine.data

import kotlinx.datetime.*
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

    data class Month(override val yearMonth: YearMonth) : PartialDate, WithMonth {
        private val javaYearMonth = yearMonth.toJavaYearMonth()
        override val yearInt = yearMonth.year
        override val year = Year(yearInt)
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

    data class Date(val date: LocalDate) : PartialDate, WithMonth {
        private val javaDate = date.toJavaLocalDate()

        override val yearInt = date.year
        override val yearMonth = date.yearMonth
        override val month = Month(yearMonth)
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
        fun parse(string: String): PartialDate? {
            // Starting with year, the most common use-case
            string.toIntOrNull()?.let { return Year(it) }

            try {
                return Date(LocalDate.parse(string))
            } catch (_: IllegalArgumentException) {
            }

            try {
                return Month(YearMonth.parse(string))
            } catch (_: IllegalArgumentException) {
            }

            return null
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